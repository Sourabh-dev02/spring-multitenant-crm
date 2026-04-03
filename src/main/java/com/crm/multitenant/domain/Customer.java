package com.crm.multitenant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * A customer belongs to exactly one tenant (enforced by the schema boundary,
 * not a foreign key). No multi-tenant discriminator column needed here because
 * the schema itself is the boundary.
 *
 * @SQLRestriction replaces the old @Where annotation. It automatically appends
 * "is_deleted = false" to every query on this entity, so soft-deleted records
 * are invisible unless you bypass Hibernate with native SQL.
 */
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customer_email", columnList = "email"),
        @Index(name = "idx_customer_status", columnList = "status")
    }
)
@Getter @Setter @NoArgsConstructor
@SQLRestriction("is_deleted = false")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SequenceGenerator(name = "base_seq", sequenceName = "customer_seq", allocationSize = 50)
public class Customer extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.PROSPECT;

    // using lazy here - we almost never need orders when listing customers
    // use EntityGraph or fetch join when you actually need them
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Order> orders = new ArrayList<>();

    public Customer(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // convenience - keeps bidirectional relationship consistent
    public void addOrder(Order order) {
        orders.add(order);
        order.setCustomer(this);
    }
}
