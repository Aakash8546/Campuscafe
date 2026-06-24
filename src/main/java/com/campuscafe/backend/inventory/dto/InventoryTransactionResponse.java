package com.campuscafe.backend.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private BigDecimal quantity;
    private String type;
    private String remarks;
    private String createdByEmail;
    private Instant createdAt;
}
