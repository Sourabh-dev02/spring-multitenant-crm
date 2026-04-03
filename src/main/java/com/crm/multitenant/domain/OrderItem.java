package com.crm.multitenant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single line on an order: X units of product Y at price Z.
 *
 * We snapshot the unit price at order time so historical orders aren't
 * affected if a product price changes later. That's why unitPrice is
 * stored here rather than read from Product.
 */
@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor
@SequenceGenerator(name = "base_seq", sequenceName = "order_item_seq", allocationSize = 50)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // price captured at the moment this line was added - independent of current product.price
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public OrderItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice(); // snapshot current price
    }
}
