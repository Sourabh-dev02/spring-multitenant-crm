package com.crm.multitenant.dto;

import com.crm.multitenant.domain.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String notes;
    private List<OrderItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    @Data
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String sku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
