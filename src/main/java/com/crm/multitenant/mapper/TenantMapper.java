package com.crm.multitenant.mapper;

import com.crm.multitenant.domain.Tenant;
import com.crm.multitenant.dto.TenantRequest;
import com.crm.multitenant.dto.TenantResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    TenantResponse toResponse(Tenant tenant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    Tenant toEntity(TenantRequest request);
}
