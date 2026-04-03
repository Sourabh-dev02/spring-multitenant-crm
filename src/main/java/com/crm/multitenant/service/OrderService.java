package com.crm.multitenant.service;

import com.crm.multitenant.domain.Order;
import com.crm.multitenant.domain.OrderItem;
import com.crm.multitenant.domain.OrderStatus;
import com.crm.multitenant.domain.Product;
import com.crm.multitenant.dto.OrderRequest;
import com.crm.multitenant.dto.OrderResponse;
import com.crm.multitenant.exception.BusinessException;
import com.crm.multitenant.exception.ResourceNotFoundException;
import com.crm.multitenant.mapper.OrderMapper;
import com.crm.multitenant.repository.CustomerRepository;
import com.crm.multitenant.repository.OrderRepository;
import com.crm.multitenant.repository.ProductRepository;
import com.crm.multitenant.repository.projection.OrderSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository    orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository  productRepository;
    private final OrderMapper        orderMapper;

    public Page<OrderSummary> listSummaries(Pageable pageable) {
        return orderRepository.findAllSummaries(pageable);
    }

    public Page<OrderResponse> listByCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Uses the EntityGraph to load order + items + products in a single query.
     * Critical for the order detail endpoint - without this it's N+1 city.
     */
    public OrderResponse findById(Long id) {
        return orderRepository.findWithItemsById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        var customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));

        Order order = new Order();
        order.setCustomer(customer);
        order.setNotes(request.getNotes());

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

            if (!product.isActive()) {
                throw new BusinessException("Product is not active: " + product.getSku());
            }
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getSku());
            }

            // decrement stock atomically - prevents overselling if two orders come in simultaneously
            int updated = productRepository.decrementStock(product.getId(), itemReq.getQuantity());
            if (updated == 0) {
                throw new BusinessException("Stock ran out while processing order for: " + product.getSku());
            }

            order.addItem(new OrderItem(product, itemReq.getQuantity()));
        }

        order = orderRepository.save(order);
        log.info("Order {} created for customer {}", order.getId(), customer.getId());
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        // simple state machine - prevent backward transitions
        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public int bulkUpdateStatus(List<Long> ids, OrderStatus status) {
        return orderRepository.bulkUpdateStatus(ids, status);
    }

    @Transactional
    public void softDelete(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        if (order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.SHIPPED) {
            throw new BusinessException("Cannot delete an order that is already " + order.getStatus());
        }
        order.setDeleted(true);
        orderRepository.save(order);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // just a representative rule set - expand this in a real product
        boolean invalid = switch (current) {
            case DELIVERED, CANCELLED, REFUNDED -> true; // terminal states
            case SHIPPED -> next != OrderStatus.DELIVERED && next != OrderStatus.REFUNDED;
            default -> false;
        };
        if (invalid) {
            throw new BusinessException("Cannot transition order from " + current + " to " + next);
        }
    }
}
