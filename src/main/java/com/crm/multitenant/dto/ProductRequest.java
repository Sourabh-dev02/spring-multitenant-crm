package com.crm.multitenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 50)
    private String sku;

    @NotNull
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull
    private Integer stockQuantity = 0;
}
