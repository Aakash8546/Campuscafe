package com.campuscafe.backend.category.service;

import com.campuscafe.backend.category.dto.CategoryResponse;
import com.campuscafe.backend.category.dto.CreateCategoryRequest;
import com.campuscafe.backend.category.dto.UpdateCategoryRequest;
import com.campuscafe.backend.category.mapper.CategoryMapper;
import com.campuscafe.backend.category.repository.CategoryRepository;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.product.Category;
import com.campuscafe.backend.exception.*;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final CategoryMapper categoryMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();


        if (categoryRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateCategoryException("Category already exists with name: " + request.getName());
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

        Category category = Category.builder()
                .name(request.getName())
                .merchant(merchant)
                .active(true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        List<Category> categories = categoryRepository.findByMerchantId(currentUser.getMerchantId());
        return categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        // Tenant check
        if (!category.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this category");
        }

        return categoryMapper.toResponse(category);
    }

    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));


        if (!category.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this category");
        }


        if (categoryRepository.existsByNameAndMerchantIdAndIdNot(request.getName(), merchantId, id)) {
            throw new DuplicateCategoryException("Category already exists with name: " + request.getName());
        }

        category.setName(request.getName());
        category.setActive(request.getActive());

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    public void deleteCategory(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));


        if (!category.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this category");
        }

        categoryRepository.delete(category);
    }
}
