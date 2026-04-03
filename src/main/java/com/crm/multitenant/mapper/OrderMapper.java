package com.crm.multitenant.mapper;

import com.crm.multitenant.domain.Order;
import com.crm.multitenant.domain.OrderItem;
import com.crm.multitenant.dto.OrderResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "customerId",   source = "customer.id")
    @Mapping(target = "customerName", expression = "java(order.getCustomer().getFirstName() + \" \" + order.getCustomer().getLastName())")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId",   source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "sku",         source = "product.sku")
    @Mapping(target = "lineTotal",   expression = "java(item.getLineTotal())")
    OrderResponse.OrderItemResponse toItemResponse(OrderItem item);
}
