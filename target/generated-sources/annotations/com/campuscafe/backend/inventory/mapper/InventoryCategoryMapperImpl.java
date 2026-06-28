package com.campuscafe.backend.inventory.mapper;

import com.campuscafe.backend.domain.inventory.InventoryCategory;
import com.campuscafe.backend.inventory.dto.InventoryCategoryResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-28T12:07:22+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class InventoryCategoryMapperImpl implements InventoryCategoryMapper {

    @Override
    public InventoryCategoryResponse toResponse(InventoryCategory category) {
        if ( category == null ) {
            return null;
        }

        InventoryCategoryResponse.InventoryCategoryResponseBuilder inventoryCategoryResponse = InventoryCategoryResponse.builder();

        inventoryCategoryResponse.id( category.getId() );
        inventoryCategoryResponse.name( category.getName() );
        inventoryCategoryResponse.createdAt( category.getCreatedAt() );
        inventoryCategoryResponse.updatedAt( category.getUpdatedAt() );

        return inventoryCategoryResponse.build();
    }
}
