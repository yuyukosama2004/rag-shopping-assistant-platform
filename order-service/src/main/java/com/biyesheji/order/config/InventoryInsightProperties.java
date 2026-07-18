package com.biyesheji.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "inventory.insight")
public class InventoryInsightProperties {
    private int lowStockThreshold = 5;
    private int deadStockMinAgeDays = 30;
    private int deadStockWindowDays = 90;
    private int overstockMinAvailable = 10;
    private int overstockCoverDays = 90;
    private String timeZone = "Asia/Shanghai";
}
