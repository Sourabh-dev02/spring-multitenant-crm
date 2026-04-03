package com.crm.multitenant.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which tenant (schema) to use for the current operation.
 * Hibernate calls this before every DB interaction to figure out the connection to use.
 */
@Component("tenantIdentifierResolver")
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    // Fall back to public when no tenant is set (e.g. actuator health checks, startup)
    public static final String DEFAULT_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_SCHEMA;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // true = Hibernate will validate that an existing session belongs to the current tenant
        // prevents accidental cross-tenant session reuse
        return true;
    }
}
