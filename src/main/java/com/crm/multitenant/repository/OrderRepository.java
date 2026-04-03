package com.crm.multitenant.repository;

import com.crm.multitenant.domain.Order;
import com.crm.multitenant.domain.OrderStatus;
import com.crm.multitenant.repository.projection.OrderSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Load an order with all its items and the products in those items in one go.
     * Equivalent to: SELECT o, i, p FROM orders o JOIN order_items i JOIN products p
     * This is the fix for the N+1 on the order detail endpoint.
     */
    @EntityGraph(value = "Order.withItems")
    Optional<Order> findWithItemsById(Long id);

    /**
     * Projection query for the order list page - avoids loading line items
     * when we just need a summary row.
     */
    @Query("SELECT o.id as id, o.status as status, o.totalAmount as totalAmount, " +
           "o.createdAt as createdAt, " +
           "c.firstName as customerFirstName, c.lastName as customerLastName, c.email as customerEmail " +
           "FROM Order o JOIN o.customer c WHERE o.deleted = false")
    Page<OrderSummary> findAllSummaries(Pageable pageable);

    // revenue reporting query
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customer.id = :customerId AND o.deleted = false")
    BigDecimal sumTotalByCustomer(@Param("customerId") Long customerId);

    // bulk status update - useful for batch cancellation workflows
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id IN :ids AND o.deleted = false")
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("status") OrderStatus status);
}
