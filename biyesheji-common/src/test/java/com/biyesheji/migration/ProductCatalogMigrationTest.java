package com.biyesheji.migration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogMigrationTest {
    @Test
    void catalogMigrationUsesAnAutoIncrementPrimaryKeyBeforeBackfill() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V8__add_product_catalog.sql"));

        assertTrue(sql.indexOf("AUTO_INCREMENT") < sql.indexOf("INSERT INTO t_product_catalog"));
    }
}
