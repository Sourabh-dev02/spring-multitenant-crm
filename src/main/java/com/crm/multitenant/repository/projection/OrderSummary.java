package com.crm.multitenant.repository.projection;

import com.crm.multitenant.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight projection for order list views.
 * Joins just enough from customer to show who placed the order.
 */
public interface OrderSummary {
    Long getId();
    OrderStatus getStatus();
    BigDecimal getTotalAmount();
    Instant getCreatedAt();
    String getCustomerFirstName();
    String getCustomerLastName();
    String getCustomerEmail();
}
