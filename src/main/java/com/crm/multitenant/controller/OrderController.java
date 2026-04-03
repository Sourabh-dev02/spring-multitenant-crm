package com.crm.multitenant.controller;

import com.crm.multitenant.domain.OrderStatus;
import com.crm.multitenant.dto.OrderRequest;
import com.crm.multitenant.dto.OrderResponse;
import com.crm.multitenant.repository.projection.OrderSummary;
import com.crm.multitenant.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderSummary>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.listSummaries(pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<OrderResponse>> listByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orderService.listByCustomer(customerId, pageable));
    }

    // this endpoint triggers the EntityGraph - loads everything in one query
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    @PostMapping("/bulk/status")
    public ResponseEntity<Integer> bulkUpdateStatus(
            @RequestBody List<Long> ids,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.bulkUpdateStatus(ids, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        orderService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
