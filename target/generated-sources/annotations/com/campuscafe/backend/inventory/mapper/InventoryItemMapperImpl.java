package com.campuscafe.backend.inventory.mapper;

import com.campuscafe.backend.domain.inventory.InventoryCategory;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.inventory.dto.InventoryItemResponse;
import com.campuscafe.backend.inventory.dto.InventoryItemSummaryResponse;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-24T01:12:09+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class InventoryItemMapperImpl implements InventoryItemMapper {

    @Autowired
    private InventoryCategoryMapper inventoryCategoryMapper;

    @Override
    public InventoryItemResponse toResponse(InventoryItem item) {
        if ( item == null ) {
            return null;
        }

        InventoryItemResponse.InventoryItemResponseBuilder inventoryItemResponse = InventoryItemResponse.builder();

        inventoryItemResponse.id( item.getId() );
        inventoryItemResponse.name( item.getName() );
        inventoryItemResponse.unit( item.getUnit() );
        inventoryItemResponse.currentStock( item.getCurrentStock() );
        inventoryItemResponse.minStock( item.getMinStock() );
        inventoryItemResponse.maxStock( item.getMaxStock() );
        inventoryItemResponse.category( inventoryCategoryMapper.toResponse( item.getCategory() ) );
        inventoryItemResponse.createdAt( item.getCreatedAt() );
        inventoryItemResponse.updatedAt( item.getUpdatedAt() );

        return inventoryItemResponse.build();
    }

    @Override
    public InventoryItemSummaryResponse toSummaryResponse(InventoryItem item) {
        if ( item == null ) {
            return null;
        }

        InventoryItemSummaryResponse.InventoryItemSummaryResponseBuilder inventoryItemSummaryResponse = InventoryItemSummaryResponse.builder();

        inventoryItemSummaryResponse.categoryName( itemCategoryName( item ) );
        inventoryItemSummaryResponse.id( item.getId() );
        inventoryItemSummaryResponse.name( item.getName() );
        inventoryItemSummaryResponse.unit( item.getUnit() );
        inventoryItemSummaryResponse.currentStock( item.getCurrentStock() );
        inventoryItemSummaryResponse.minStock( item.getMinStock() );
        inventoryItemSummaryResponse.maxStock( item.getMaxStock() );

        return inventoryItemSummaryResponse.build();
    }

    private String itemCategoryName(InventoryItem inventoryItem) {
        if ( inventoryItem == null ) {
            return null;
        }
        InventoryCategory category = inventoryItem.getCategory();
        if ( category == null ) {
            return null;
        }
        String name = category.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
