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
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(16, first.migrationsExecuted);
        assertTrue(columnExists("t_product_sku", "sku_code"));
        assertTrue(columnExists("t_stock", "sku_id"));
        assertTrue(columnExists("t_order", "tracking_no"));
        assertTrue(columnExists("t_order", "merchant_note"));
        assertTrue(tableExists("t_shipping_rule"));
        assertTrue(tableExists("t_ai_setting"));

        MigrateResult second = flyway.migrate();
        assertEquals(0, second.migrationsExecuted);
        flyway.validate();
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
