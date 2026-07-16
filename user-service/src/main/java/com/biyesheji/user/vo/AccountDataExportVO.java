package com.biyesheji.user.vo;

import com.biyesheji.entity.Address;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AccountDataExportVO {
    private LocalDateTime generatedAt;
    private Profile profile;
    private List<Address> addresses;
    private List<ExportedOrder> orders;

    @Data
    @Builder
    public static class Profile {
        private Long id;
        private String username;
        private String nickname;
        private String phone;
        private String email;
        private String avatar;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class ExportedOrder {
        private String orderNo;
        private BigDecimal productAmount;
        private BigDecimal shippingFee;
        private BigDecimal totalAmount;
        private String shippingRuleName;
        private String shippingMethod;
        private String paymentMethod;
        private Integer status;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String shippingCarrier;
        private String trackingNo;
        private LocalDateTime payTime;
        private LocalDateTime processingAt;
        private LocalDateTime shippedAt;
        private LocalDateTime cancelTime;
        private LocalDateTime createdAt;
        private List<ExportedOrderItem> items;
        private List<ExportedRefund> refunds;
    }

    @Data
    @Builder
    public static class ExportedOrderItem {
        private Long productId;
        private Long skuId;
        private String skuCode;
        private String skuSpecJson;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    public static class ExportedRefund {
        private BigDecimal amount;
        private String reason;
        private String status;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
    }
}
