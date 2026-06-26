package com.campuscafe.backend.inventory.mapper;

import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.domain.inventory.InventoryTransaction;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.inventory.dto.InventoryTransactionResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-26T11:45:07+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class InventoryTransactionMapperImpl implements InventoryTransactionMapper {

    @Override
    public InventoryTransactionResponse toResponse(InventoryTransaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        InventoryTransactionResponse.InventoryTransactionResponseBuilder inventoryTransactionResponse = InventoryTransactionResponse.builder();

        inventoryTransactionResponse.inventoryItemId( transactionInventoryItemId( transaction ) );
        inventoryTransactionResponse.inventoryItemName( transactionInventoryItemName( transaction ) );
        inventoryTransactionResponse.createdByEmail( transactionCreatedByEmail( transaction ) );
        inventoryTransactionResponse.id( transaction.getId() );
        inventoryTransactionResponse.quantity( transaction.getQuantity() );
        if ( transaction.getType() != null ) {
            inventoryTransactionResponse.type( transaction.getType().name() );
        }
        inventoryTransactionResponse.remarks( transaction.getRemarks() );
        inventoryTransactionResponse.createdAt( transaction.getCreatedAt() );

        return inventoryTransactionResponse.build();
    }

    private Long transactionInventoryItemId(InventoryTransaction inventoryTransaction) {
        if ( inventoryTransaction == null ) {
            return null;
        }
        InventoryItem inventoryItem = inventoryTransaction.getInventoryItem();
        if ( inventoryItem == null ) {
            return null;
        }
        Long id = inventoryItem.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String transactionInventoryItemName(InventoryTransaction inventoryTransaction) {
        if ( inventoryTransaction == null ) {
            return null;
        }
        InventoryItem inventoryItem = inventoryTransaction.getInventoryItem();
        if ( inventoryItem == null ) {
            return null;
        }
        String name = inventoryItem.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String transactionCreatedByEmail(InventoryTransaction inventoryTransaction) {
        if ( inventoryTransaction == null ) {
            return null;
        }
        User createdBy = inventoryTransaction.getCreatedBy();
        if ( createdBy == null ) {
            return null;
        }
        String email = createdBy.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }
}
