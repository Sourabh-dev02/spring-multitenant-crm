package com.crm.multitenant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A sales order placed by a customer. totalAmount is kept as a stored field
 * (recalculated when items change) rather than a derived/formula field so we
 * can sort and filter on it in the DB without re-summing.
 *
 * The @NamedEntityGraph here handles the most common access pattern: fetching
 * an order along with its line items and the products in those items. Without
 * this you'd hit an N+1 every time you display an order detail page.
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_status", columnList = "status")
    }
)
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "Order.withItems",
        attributeNodes = {
            @NamedAttributeNode("customer"),
            @NamedAttributeNode(value = "orderItems", subgraph = "items-with-product")
        },
        subgraphs = @NamedSubgraph(
            name = "items-with-product",
            attributeNodes = @NamedAttributeNode("product")
        )
    )
})
@Getter @Setter @NoArgsConstructor
@SQLRestriction("is_deleted = false")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Order extends BaseEntity {

    @SequenceGenerator(name = "base_seq", sequenceName = "order_seq", allocationSize = 50)

    // not nullable - every order must belong to a customer
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    public void removeItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
