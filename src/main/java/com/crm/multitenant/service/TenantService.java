package com.crm.multitenant.service;

import com.crm.multitenant.domain.Tenant;
import com.crm.multitenant.dto.TenantRequest;
import com.crm.multitenant.dto.TenantResponse;
import com.crm.multitenant.exception.ConflictException;
import com.crm.multitenant.exception.ResourceNotFoundException;
import com.crm.multitenant.mapper.TenantMapper;
import com.crm.multitenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles tenant lifecycle - creation, activation, deactivation.
 *
 * Tenant provisioning creates a real postgres schema and runs the tenant DDL
 * inside it. This is a one-time operation per tenant, so it lives in its own
 * service rather than being mixed with business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper     tenantMapper;
    private final JdbcTemplate     jdbcTemplate;

    @Transactional
    public TenantResponse createTenant(TenantRequest request) {
        if (tenantRepository.existsBySchemaName(request.getSchemaName())) {
            throw new ConflictException("Schema name already taken: " + request.getSchemaName());
        }

        Tenant tenant = tenantMapper.toEntity(request);
        tenant = tenantRepository.save(tenant);

        provisionSchema(request.getSchemaName());

        log.info("Tenant '{}' created with schema '{}'", tenant.getName(), tenant.getSchemaName());
        return tenantMapper.toResponse(tenant);
    }

    @Cacheable(value = "tenants", key = "#id")
    @Transactional(readOnly = true)
    public TenantResponse findById(Long id) {
        return tenantRepository.findById(id)
                .map(tenantMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> findAll() {
        return tenantRepository.findAll().stream()
                .map(tenantMapper::toResponse)
                .toList();
    }

    @CacheEvict(value = "tenants", key = "#id")
    @Transactional
    public void deactivate(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        tenant.setActive(false);
        tenantRepository.save(tenant);
        log.info("Tenant '{}' deactivated", tenant.getName());
    }

    /**
     * Creates the schema and all tables needed for a new tenant.
     * This runs DDL so it must be called outside of a JPA transaction
     * (DDL auto-commits in postgres). We use JdbcTemplate directly here.
     *
     * Schema names are already validated by the TenantRequest @Pattern annotation,
     * but we double-check here to be absolutely sure nothing unsafe gets through.
     */
    private void provisionSchema(String schemaName) {
        if (!schemaName.matches("[a-z][a-z0-9_]{2,62}")) {
            throw new IllegalArgumentException("Unsafe schema name rejected: " + schemaName);
        }

        log.debug("Provisioning schema: {}", schemaName);

        // CREATE SCHEMA IF NOT EXISTS is idempotent - safe to re-run
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        // set search_path so subsequent DDL lands in the right schema
        jdbcTemplate.execute("SET search_path TO " + schemaName);

        jdbcTemplate.execute("""
            CREATE SEQUENCE IF NOT EXISTS customer_seq START 1 INCREMENT 50;
            """);
        jdbcTemplate.execute("""
            CREATE SEQUENCE IF NOT EXISTS product_seq START 1 INCREMENT 50;
            """);
        jdbcTemplate.execute("""
            CREATE SEQUENCE IF NOT EXISTS order_seq START 1 INCREMENT 50;
            """);
        jdbcTemplate.execute("""
            CREATE SEQUENCE IF NOT EXISTS order_item_seq START 1 INCREMENT 50;
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS customers (
                id           BIGINT PRIMARY KEY DEFAULT nextval('customer_seq'),
                version      BIGINT NOT NULL DEFAULT 0,
                first_name   VARCHAR(100) NOT NULL,
                last_name    VARCHAR(100) NOT NULL,
                email        VARCHAR(255) NOT NULL UNIQUE,
                phone        VARCHAR(20),
                company      VARCHAR(200),
                status       VARCHAR(20) NOT NULL DEFAULT 'PROSPECT',
                is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
                created_at   TIMESTAMPTZ,
                updated_at   TIMESTAMPTZ,
                created_by   VARCHAR(255),
                updated_by   VARCHAR(255)
            )""");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id             BIGINT PRIMARY KEY DEFAULT nextval('product_seq'),
                version        BIGINT NOT NULL DEFAULT 0,
                name           VARCHAR(200) NOT NULL,
                description    VARCHAR(2000),
                sku            VARCHAR(50) NOT NULL UNIQUE,
                price          NUMERIC(12,2) NOT NULL,
                stock_quantity INTEGER NOT NULL DEFAULT 0,
                is_active      BOOLEAN NOT NULL DEFAULT TRUE,
                is_deleted     BOOLEAN NOT NULL DEFAULT FALSE,
                created_at     TIMESTAMPTZ,
                updated_at     TIMESTAMPTZ,
                created_by     VARCHAR(255),
                updated_by     VARCHAR(255)
            )""");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS orders (
                id           BIGINT PRIMARY KEY DEFAULT nextval('order_seq'),
                version      BIGINT NOT NULL DEFAULT 0,
                customer_id  BIGINT NOT NULL REFERENCES customers(id),
                status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                total_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
                notes        VARCHAR(500),
                is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
                created_at   TIMESTAMPTZ,
                updated_at   TIMESTAMPTZ,
                created_by   VARCHAR(255),
                updated_by   VARCHAR(255)
            )""");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS order_items (
                id         BIGINT PRIMARY KEY DEFAULT nextval('order_item_seq'),
                version    BIGINT NOT NULL DEFAULT 0,
                order_id   BIGINT NOT NULL REFERENCES orders(id),
                product_id BIGINT NOT NULL REFERENCES products(id),
                quantity   INTEGER NOT NULL,
                unit_price NUMERIC(12,2) NOT NULL,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMPTZ,
                updated_at TIMESTAMPTZ,
                created_by VARCHAR(255),
                updated_by VARCHAR(255)
            )""");

        // reset to public
        jdbcTemplate.execute("SET search_path TO public");

        log.info("Schema '{}' provisioned successfully", schemaName);
    }
}
