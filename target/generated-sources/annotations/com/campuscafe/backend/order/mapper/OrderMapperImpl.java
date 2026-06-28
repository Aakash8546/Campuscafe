package com.campuscafe.backend.order.mapper;

import com.campuscafe.backend.domain.order.Order;
import com.campuscafe.backend.domain.order.OrderItem;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.domain.product.ProductVariant;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.order.dto.OrderDetailsResponse;
import com.campuscafe.backend.order.dto.OrderItemResponse;
import com.campuscafe.backend.order.dto.OrderResponse;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-28T13:36:02+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Override
    public OrderResponse toResponse(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderResponse.OrderResponseBuilder orderResponse = OrderResponse.builder();

        orderResponse.id( order.getId() );
        orderResponse.orderNumber( order.getOrderNumber() );
        orderResponse.billSerialNumber( order.getBillSerialNumber() );
        if ( order.getStatus() != null ) {
            orderResponse.status( order.getStatus().name() );
        }
        if ( order.getSource() != null ) {
            orderResponse.source( order.getSource().name() );
        }
        orderResponse.subtotal( order.getSubtotal() );
        orderResponse.discountAmount( order.getDiscountAmount() );
        orderResponse.finalAmount( order.getFinalAmount() );
        if ( order.getPaymentMethod() != null ) {
            orderResponse.paymentMethod( order.getPaymentMethod().name() );
        }
        orderResponse.createdAt( order.getCreatedAt() );

        return orderResponse.build();
    }

    @Override
    public OrderDetailsResponse toDetailsResponse(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderDetailsResponse.OrderDetailsResponseBuilder orderDetailsResponse = OrderDetailsResponse.builder();

        orderDetailsResponse.createdByEmail( orderCreatedByEmail( order ) );
        orderDetailsResponse.createdByName( orderCreatedByName( order ) );
        orderDetailsResponse.id( order.getId() );
        orderDetailsResponse.orderNumber( order.getOrderNumber() );
        orderDetailsResponse.billSerialNumber( order.getBillSerialNumber() );
        if ( order.getStatus() != null ) {
            orderDetailsResponse.status( order.getStatus().name() );
        }
        if ( order.getSource() != null ) {
            orderDetailsResponse.source( order.getSource().name() );
        }
        orderDetailsResponse.subtotal( order.getSubtotal() );
        orderDetailsResponse.discountAmount( order.getDiscountAmount() );
        orderDetailsResponse.finalAmount( order.getFinalAmount() );
        if ( order.getPaymentMethod() != null ) {
            orderDetailsResponse.paymentMethod( order.getPaymentMethod().name() );
        }
        orderDetailsResponse.items( orderItemListToOrderItemResponseList( order.getItems() ) );
        orderDetailsResponse.createdAt( order.getCreatedAt() );
        orderDetailsResponse.updatedAt( order.getUpdatedAt() );

        return orderDetailsResponse.build();
    }

    @Override
    public OrderItemResponse toItemResponse(OrderItem item) {
        if ( item == null ) {
            return null;
        }

        OrderItemResponse.OrderItemResponseBuilder orderItemResponse = OrderItemResponse.builder();

        orderItemResponse.productId( itemProductId( item ) );
        orderItemResponse.productName( itemProductName( item ) );
        orderItemResponse.variantId( itemVariantId( item ) );
        orderItemResponse.variantName( item.getVariantName() );
        orderItemResponse.id( item.getId() );
        orderItemResponse.quantity( item.getQuantity() );
        orderItemResponse.unitPrice( item.getUnitPrice() );
        orderItemResponse.subtotal( item.getSubtotal() );
        orderItemResponse.instructions( item.getInstructions() );

        return orderItemResponse.build();
    }

    private String orderCreatedByEmail(Order order) {
        if ( order == null ) {
            return null;
        }
        User createdBy = order.getCreatedBy();
        if ( createdBy == null ) {
            return null;
        }
        String email = createdBy.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }

    private String orderCreatedByName(Order order) {
        if ( order == null ) {
            return null;
        }
        User createdBy = order.getCreatedBy();
        if ( createdBy == null ) {
            return null;
        }
        String name = createdBy.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    protected List<OrderItemResponse> orderItemListToOrderItemResponseList(List<OrderItem> list) {
        if ( list == null ) {
            return null;
        }

        List<OrderItemResponse> list1 = new ArrayList<OrderItemResponse>( list.size() );
        for ( OrderItem orderItem : list ) {
            list1.add( toItemResponse( orderItem ) );
        }

        return list1;
    }

    private Long itemProductId(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Product product = orderItem.getProduct();
        if ( product == null ) {
            return null;
        }
        Long id = product.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String itemProductName(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Product product = orderItem.getProduct();
        if ( product == null ) {
            return null;
        }
        String name = product.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Long itemVariantId(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        ProductVariant variant = orderItem.getVariant();
        if ( variant == null ) {
            return null;
        }
        Long id = variant.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
