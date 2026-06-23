package com.campuscafe.backend.product.mapper;

import com.campuscafe.backend.category.mapper.CategoryMapper;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.product.dto.ProductResponse;
import com.campuscafe.backend.product.dto.ProductSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "categoryName", source = "category.name")
    ProductSummaryResponse toSummaryResponse(Product product);
}
