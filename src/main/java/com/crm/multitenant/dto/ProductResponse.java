package com.crm.multitenant.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
