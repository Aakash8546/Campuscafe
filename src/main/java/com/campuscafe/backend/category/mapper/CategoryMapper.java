package com.campuscafe.backend.category.mapper;

import com.campuscafe.backend.category.dto.CategoryResponse;
import com.campuscafe.backend.domain.product.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}
