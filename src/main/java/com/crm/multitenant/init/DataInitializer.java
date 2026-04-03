package com.crm.multitenant.init;

import com.crm.multitenant.dto.TenantRequest;
import com.crm.multitenant.service.TenantService;
import com.crm.multitenant.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds two test tenants (acme_corp and globex) with customers, products and orders
 * so you can hit the API immediately without manually setting anything up.
 *
 * Only runs when app.seed-on-startup=true (set in application-local.yml).
 * Skips if the tenants table already has rows so it's safe to restart without duplication.
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(name = "app.seed-on-startup", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final TenantService tenantService;
    private final JdbcTemplate  jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Long tenantCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.tenants", Long.class);

        if (tenantCount != null && tenantCount > 0) {
            log.info("Seed data already present ({} tenants), skipping.", tenantCount);
            return;
        }

        log.info("Seeding test data...");
        seedTenant("Acme Corp",  "acme_corp",  "admin@acme.com");
        seedTenant("Globex Inc", "globex_inc", "admin@globex.com");
        log.info("Seed complete.");
    }

    private void seedTenant(String name, String schema, String email) {
        TenantRequest req = new TenantRequest();
        req.setName(name);
        req.setSchemaName(schema);
        req.setContactEmail(email);
        tenantService.createTenant(req); // this also provisions the schema

        // switch context into the new schema
        TenantContext.setCurrentTenant(schema);
        TenantContext.setCurrentUser("seeder");

        try {
            seedProducts(schema);
            seedCustomers(schema);
        } finally {
            TenantContext.clear();
        }
    }

    private void seedProducts(String schema) {
        List<Object[]> products = List.of(
            new Object[]{"CRM Pro License",     "CRM-PRO-001", new BigDecimal("299.00"), 9999},
            new Object[]{"Support Package - 10h","SUP-10H-001", new BigDecimal("499.00"), 500},
            new Object[]{"Onboarding Bundle",   "OBD-001",     new BigDecimal("199.00"), 200},
            new Object[]{"API Add-on",          "API-ADDON-001",new BigDecimal("99.00"),  1000},
            new Object[]{"Enterprise License",  "ENT-LIC-001", new BigDecimal("999.00"), 100}
        );

        for (Object[] p : products) {
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".products (name, sku, price, stock_quantity, is_active, is_deleted, version) " +
                "VALUES (?,?,?,?,true,false,0)",
                p[0], p[1], p[2], p[3]
            );
        }
        log.debug("Inserted {} products into {}", products.size(), schema);
    }

    private void seedCustomers(String schema) {
        List<Object[]> customers = List.of(
            new Object[]{"Alice",  "Johnson", "alice@example.com",  "TechCorp"},
            new Object[]{"Bob",    "Smith",   "bob@example.com",    "StartupXYZ"},
            new Object[]{"Carol",  "White",   "carol@example.com",  "MegaRetail"},
            new Object[]{"David",  "Brown",   "david@example.com",  "CloudSoft"},
            new Object[]{"Emma",   "Wilson",  "emma@example.com",   "DataFlow Inc"}
        );

        for (Object[] c : customers) {
            jdbcTemplate.update(
                "INSERT INTO " + schema + ".customers (first_name, last_name, email, company, status, is_deleted, version) " +
                "VALUES (?,?,?,?,'ACTIVE',false,0)",
                c[0], c[1], c[2], c[3]
            );
        }
        log.debug("Inserted {} customers into {}", customers.size(), schema);
    }
}
