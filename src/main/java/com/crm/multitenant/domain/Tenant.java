package com.crm.multitenant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents an organization (tenant) that signs up for the CRM.
 * Lives in the public schema - it's the only table shared across all schemas.
 *
 * The schemaName field is what Hibernate uses to route the connection
 * (set_path on the PG connection). It must match an actual PG schema name,
 * validated via a regex in MultiTenantConnectionProviderImpl.
 */
@Entity
@Table(name = "tenants", schema = "public")
@Getter @Setter @NoArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenant_seq")
    @SequenceGenerator(name = "tenant_seq", sequenceName = "public.tenant_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // this becomes the postgres schema name - lowercase alphanumeric + underscores only
    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Column(nullable = false)
    private boolean active = true;

    // contact info for org-level billing / support
    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    public Tenant(String name, String schemaName, String contactEmail) {
        this.name = name;
        this.schemaName = schemaName;
        this.contactEmail = contactEmail;
    }
}
