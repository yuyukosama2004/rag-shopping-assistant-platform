package com.biyesheji.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46")
            .withDatabaseName("biyesheji")
            .withUsername("test")
            .withPassword("test-password");

    @Test
    void upgradesTheLegacySchemaAndIsIdempotent() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(findLegacySchema()));
        }

        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateMigrationNaming(true)
                .load();

        MigrateResult first = flyway.migrate();
        assertEquals(18, first.migrationsExecuted);
        assertTrue(columnExists("t_product_sku", "sku_code"));
        assertTrue(columnExists("t_stock", "sku_id"));
        assertTrue(columnExists("t_order", "tracking_no"));
        assertTrue(columnExists("t_order", "merchant_note"));
        assertTrue(tableExists("t_shipping_rule"));
        assertTrue(tableExists("t_ai_setting"));
        assertTrue(tableExists("t_order_notification_outbox"));
        assertTrue(columnExists("t_stock_ledger", "event_key"));
        assertTrue(columnExists("t_stock_ledger", "before_total"));
        assertTrue(columnExists("t_stock_ledger", "after_total"));
        assertTrue(columnExists("t_stock_ledger", "before_locked"));
        assertTrue(columnExists("t_stock_ledger", "after_locked"));
        assertStockLedgerCompatibilityAndUniqueness();

        MigrateResult second = flyway.migrate();
        assertEquals(0, second.migrationsExecuted);
        flyway.validate();
    }

    private void assertStockLedgerCompatibilityAndUniqueness() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            try (PreparedStatement legacyInsert = connection.prepareStatement("""
                    INSERT INTO t_stock_ledger
                        (id, sku_id, action, quantity, before_available, after_available)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                legacyInsert.setLong(1, 9001L);
                legacyInsert.setLong(2, 101L);
                legacyInsert.setString(3, "MANUAL_ADJUST");
                legacyInsert.setInt(4, 1);
                legacyInsert.setInt(5, 4);
                legacyInsert.setInt(6, 5);
                assertEquals(1, legacyInsert.executeUpdate());
            }
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT event_key, before_total FROM t_stock_ledger WHERE id = ?")) {
                query.setLong(1, 9001L);
                try (ResultSet result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertNull(result.getString("event_key"));
                    assertNull(result.getObject("before_total"));
                }
            }

            insertLedgerEvent(connection, 9002L, "ORDER_RESERVE:ORDER-1:101");
            assertThrows(SQLException.class,
                    () -> insertLedgerEvent(connection, 9003L, "ORDER_RESERVE:ORDER-1:101"));
        }
    }

    private void insertLedgerEvent(Connection connection, long id, String eventKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO t_stock_ledger
                    (id, sku_id, action, event_key, quantity, before_available, after_available)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, id);
            statement.setLong(2, 101L);
            statement.setString(3, "ORDER_RESERVE");
            statement.setString(4, eventKey);
            statement.setInt(5, 1);
            statement.setInt(6, 5);
            statement.setInt(7, 4);
            statement.executeUpdate();
        }
    }

    private static Path findLegacySchema() {
        Path rootRelative = Path.of("sql", "init.sql");
        if (Files.isRegularFile(rootRelative)) return rootRelative;
        Path moduleRelative = Path.of("..", "sql", "init.sql");
        if (Files.isRegularFile(moduleRelative)) return moduleRelative;
        throw new IllegalStateException("Cannot locate sql/init.sql");
    }

    private boolean tableExists(String table) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             ResultSet result = connection.getMetaData().getTables(
                     MYSQL.getDatabaseName(), null, table, new String[]{"TABLE"})) {
            return result.next();
        }
    }

    private boolean columnExists(String table, String column) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             ResultSet result = connection.getMetaData().getColumns(
                     MYSQL.getDatabaseName(), null, table, column)) {
            return result.next();
        }
    }
}
