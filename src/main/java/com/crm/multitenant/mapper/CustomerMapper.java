package com.crm.multitenant.mapper;

import com.crm.multitenant.domain.Customer;
import com.crm.multitenant.dto.CustomerRequest;
import com.crm.multitenant.dto.CustomerResponse;
import org.mapstruct.*;

/**
 * MapStruct generates the implementation at compile time - no reflection,
 * no runtime overhead. The generated class lives in target/generated-sources.
 *
 * componentModel = "spring" makes the impl a @Component so we can @Autowire it normally.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "orders", ignore = true)
    Customer toEntity(CustomerRequest request);

    // for updates: apply the request fields on top of the existing entity
    // NullValuePropertyMappingStrategy.IGNORE means null fields in the request don't overwrite existing values
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "orders", ignore = true)
    void updateEntity(CustomerRequest request, @MappingTarget Customer customer);
}
