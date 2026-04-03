package com.crm.multitenant.config;

import com.crm.multitenant.tenant.MultiTenantConnectionProviderImpl;
import com.crm.multitenant.tenant.TenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up Hibernate's multi-tenancy support.
 *
 * Using HibernatePropertiesCustomizer lets us hook into Spring Boot's
 * auto-configuration rather than replacing it entirely. Less boilerplate,
 * same result.
 */
@Configuration
@RequiredArgsConstructor
public class HibernateMultiTenancyConfig {

    private final MultiTenantConnectionProviderImpl connectionProvider;
    private final TenantIdentifierResolver          tenantIdentifierResolver;

    @Bean
    public HibernatePropertiesCustomizer hibernateMultiTenancyCustomizer() {
        return props -> {
            // plugging in our connection provider automatically signals Hibernate
            // to use SCHEMA-based multi-tenancy (no need to set hibernate.multiTenancy)
            props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);

            // L2 cache via JCache - EhCache 3 underneath (see ehcache.xml for region config)
            props.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, true);
            props.put(AvailableSettings.USE_QUERY_CACHE, true);
            props.put(AvailableSettings.CACHE_REGION_FACTORY, "jcache");
            props.put("hibernate.javax.cache.provider",
                    "org.ehcache.jsr107.EhcacheCachingProvider");
            props.put("hibernate.javax.cache.uri", "classpath:ehcache.xml");
        };
    }
}
