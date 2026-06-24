package com.campuscafe.backend.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCategoryResponse {
    private Long id;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
}
