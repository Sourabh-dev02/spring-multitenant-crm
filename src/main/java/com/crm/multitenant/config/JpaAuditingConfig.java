package com.crm.multitenant.config;

import com.crm.multitenant.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables JPA auditing for createdAt/updatedAt/createdBy/updatedBy fields.
 *
 * Keeping this in its own config class (separate from the main app class)
 * makes it easier to exclude during testing without wiring up the whole context.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "tenantAuditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> tenantAuditorAware() {
        // pull the username from our thread-local context
        // in a real service this would come from SecurityContextHolder JWT claims
        return () -> Optional.ofNullable(TenantContext.getCurrentUser())
                .filter(u -> !u.isBlank())
                .or(() -> Optional.of("system")); // default for background jobs / seeding
    }
}
