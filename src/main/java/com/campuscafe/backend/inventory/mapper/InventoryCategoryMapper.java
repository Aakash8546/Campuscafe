package com.campuscafe.backend.inventory.mapper;

import com.campuscafe.backend.domain.inventory.InventoryCategory;
import com.campuscafe.backend.inventory.dto.InventoryCategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryCategoryMapper {
    InventoryCategoryResponse toResponse(InventoryCategory category);
}
