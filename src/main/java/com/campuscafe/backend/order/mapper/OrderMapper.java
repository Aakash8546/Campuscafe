package com.campuscafe.backend.order.mapper;

import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.OrderItem;
import com.campuscafe.backend.order.dto.OrderDetailsResponse;
import com.campuscafe.backend.order.dto.OrderItemResponse;
import com.campuscafe.backend.order.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "createdByEmail", source = "createdBy.email")
    @Mapping(target = "createdByName", source = "createdBy.name")
    OrderDetailsResponse toDetailsResponse(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "variantId", source = "variant.id")
    @Mapping(target = "variantName", source = "variantName")
    OrderItemResponse toItemResponse(OrderItem item);
}
