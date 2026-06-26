package com.campuscafe.backend.discount.mapper;

import com.campuscafe.backend.discount.dto.DiscountResponse;
import com.campuscafe.backend.domain.discount.Discount;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DiscountMapper {

    DiscountResponse toResponse(Discount discount);

    List<DiscountResponse> toResponseList(List<Discount> discounts);
}
