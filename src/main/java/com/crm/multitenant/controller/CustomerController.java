package com.crm.multitenant.controller;

import com.crm.multitenant.dto.CustomerRequest;
import com.crm.multitenant.dto.CustomerResponse;
import com.crm.multitenant.repository.projection.CustomerSummary;
import com.crm.multitenant.service.CustomerService;
import com.crm.multitenant.tenant.TenantContext;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Returns a lightweight summary page (no orders loaded).
     * Default sort is newest first - most useful for a CRM dashboard.
     */
    @GetMapping
    public ResponseEntity<Page<CustomerSummary>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(customerService.listSummaries(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CustomerResponse>> search(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(customerService.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        customerService.softDelete(id, TenantContext.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    // bulk delete endpoint - handy for admin cleanup or GDPR wipe requests
    @DeleteMapping("/bulk")
    public ResponseEntity<Integer> bulkSoftDelete(
            @RequestBody List<Long> ids) {
        int count = customerService.bulkSoftDelete(ids, TenantContext.getCurrentUser());
        return ResponseEntity.ok(count);
    }
}
