package com.campuscafe.backend.inventory.mapper;

import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.inventory.dto.InventoryItemResponse;
import com.campuscafe.backend.inventory.dto.InventoryItemSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {InventoryCategoryMapper.class})
public interface InventoryItemMapper {

    InventoryItemResponse toResponse(InventoryItem item);

    @Mapping(target = "categoryName", source = "category.name")
    InventoryItemSummaryResponse toSummaryResponse(InventoryItem item);
}
