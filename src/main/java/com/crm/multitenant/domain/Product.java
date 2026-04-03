package com.crm.multitenant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * Product catalog entry for a tenant. Things like SaaS subscriptions, consulting
 * hours, physical goods - whatever the tenant is selling.
 *
 * Caching this with READ_WRITE because product lists don't change often
 * and get hit constantly when displaying order forms.
 */
@Entity
@Table(name = "products",
    indexes = @Index(name = "idx_product_sku", columnList = "sku", unique = true)
)
@Getter @Setter @NoArgsConstructor
@SQLRestriction("is_deleted = false")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product extends BaseEntity {

    @SequenceGenerator(name = "base_seq", sequenceName = "product_seq", allocationSize = 50)

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    // storing price in BigDecimal to avoid floating-point rounding nightmares
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Product(String name, String sku, BigDecimal price) {
        this.name = name;
        this.sku = sku;
        this.price = price;
    }
}
