package com.campuscafe.backend.inventory.mapper;

import com.campuscafe.backend.domain.inventory.InventoryTransaction;
import com.campuscafe.backend.inventory.dto.InventoryTransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryTransactionMapper {

    @Mapping(target = "inventoryItemId", source = "inventoryItem.id")
    @Mapping(target = "inventoryItemName", source = "inventoryItem.name")
    @Mapping(target = "createdByEmail", source = "createdBy.email")
    InventoryTransactionResponse toResponse(InventoryTransaction transaction);
}
