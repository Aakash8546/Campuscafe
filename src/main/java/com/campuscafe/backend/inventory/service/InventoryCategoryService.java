package com.campuscafe.backend.inventory.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.inventory.InventoryCategory;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.inventory.dto.InventoryCategoryResponse;
import com.campuscafe.backend.inventory.dto.CreateInventoryCategoryRequest;
import com.campuscafe.backend.inventory.dto.UpdateInventoryCategoryRequest;
import com.campuscafe.backend.inventory.mapper.InventoryCategoryMapper;
import com.campuscafe.backend.inventory.repository.InventoryCategoryRepository;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryCategoryService {

    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final MerchantRepository merchantRepository;
    private final InventoryCategoryMapper categoryMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public InventoryCategoryResponse createCategory(CreateInventoryCategoryRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        if (inventoryCategoryRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateInventoryCategoryException("Inventory category already exists with name: " + request.getName());
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

        InventoryCategory category = InventoryCategory.builder()
                .name(request.getName())
                .merchant(merchant)
                .build();

        InventoryCategory savedCategory = inventoryCategoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<InventoryCategoryResponse> getAllCategories() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        List<InventoryCategory> categories = inventoryCategoryRepository.findByMerchantId(currentUser.getMerchantId());
        return categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryCategoryResponse getCategoryById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        InventoryCategory category = inventoryCategoryRepository.findById(id)
                .orElseThrow(() -> new InventoryCategoryNotFoundException("Inventory category not found with id: " + id));

        if (!category.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this inventory category");
        }

        return categoryMapper.toResponse(category);
    }

    public InventoryCategoryResponse updateCategory(Long id, UpdateInventoryCategoryRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        InventoryCategory category = inventoryCategoryRepository.findById(id)
                .orElseThrow(() -> new InventoryCategoryNotFoundException("Inventory category not found with id: " + id));

        if (!category.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory category");
        }

        if (inventoryCategoryRepository.existsByNameAndMerchantIdAndIdNot(request.getName(), merchantId, id)) {
            throw new DuplicateInventoryCategoryException("Inventory category already exists with name: " + request.getName());
        }

        category.setName(request.getName());
        InventoryCategory updatedCategory = inventoryCategoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    public void deleteCategory(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        InventoryCategory category = inventoryCategoryRepository.findById(id)
                .orElseThrow(() -> new InventoryCategoryNotFoundException("Inventory category not found with id: " + id));

        if (!category.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this inventory category");
        }

        inventoryCategoryRepository.delete(category);
    }
}
