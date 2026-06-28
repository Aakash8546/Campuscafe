package com.campuscafe.backend.discount.mapper;

import com.campuscafe.backend.discount.dto.DiscountResponse;
import com.campuscafe.backend.domain.discount.Discount;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-28T12:07:22+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class DiscountMapperImpl implements DiscountMapper {

    @Override
    public DiscountResponse toResponse(Discount discount) {
        if ( discount == null ) {
            return null;
        }

        DiscountResponse.DiscountResponseBuilder discountResponse = DiscountResponse.builder();

        discountResponse.id( discount.getId() );
        discountResponse.name( discount.getName() );
        discountResponse.discountType( discount.getDiscountType() );
        discountResponse.value( discount.getValue() );
        discountResponse.maxDiscount( discount.getMaxDiscount() );
        discountResponse.active( discount.getActive() );

        return discountResponse.build();
    }

    @Override
    public List<DiscountResponse> toResponseList(List<Discount> discounts) {
        if ( discounts == null ) {
            return null;
        }

        List<DiscountResponse> list = new ArrayList<DiscountResponse>( discounts.size() );
        for ( Discount discount : discounts ) {
            list.add( toResponse( discount ) );
        }

        return list;
    }
}
