package com.crm.multitenant.repository;

import com.crm.multitenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySchemaName(String schemaName);

    Optional<Tenant> findByName(String name);

    boolean existsBySchemaName(String schemaName);
}
