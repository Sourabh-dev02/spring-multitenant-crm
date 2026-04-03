package com.crm.multitenant.controller;

import com.crm.multitenant.dto.TenantRequest;
import com.crm.multitenant.dto.TenantResponse;
import com.crm.multitenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only endpoints for tenant management.
 * In production these would be behind a separate admin role guard,
 * not exposed on the same port as the tenant APIs.
 */
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantRequest request) {
        TenantResponse response = tenantService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> listAll() {
        return ResponseEntity.ok(tenantService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        tenantService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
