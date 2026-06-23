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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Merchant merchant;
    private Merchant otherMerchant;
    private com.campuscafe.backend.domain.user.User adminUser;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Merchant A").email("merchantA@test.com").verified(true).build();
        merchant.setId(1L);

        otherMerchant = Merchant.builder().cafeName("Merchant B").email("merchantB@test.com").verified(true).build();
        otherMerchant.setId(2L);

        com.campuscafe.backend.domain.user.Role adminRole = com.campuscafe.backend.domain.user.Role.builder().name("ADMIN").build();
        adminUser = com.campuscafe.backend.domain.user.User.builder()
                .email("admin@merchantA.com")
                .role(adminRole)
                .merchant(merchant)
                .active(true)
                .build();
        adminUser.setId(1L);

        setupSecurityContext(adminUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(com.campuscafe.backend.domain.user.User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateCategory_Success() {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Beverages").build();

        when(categoryRepository.existsByNameAndMerchantId("Beverages", 1L)).thenReturn(false);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

        Category savedCategory = Category.builder().name("Beverages").merchant(merchant).active(true).build();
        savedCategory.setId(5L);
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = CategoryResponse.builder().id(5L).name("Beverages").active(true).build();
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Beverages", result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void testCreateCategory_DuplicateName_ThrowsDuplicateCategoryException() {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Beverages").build();

        when(categoryRepository.existsByNameAndMerchantId("Beverages", 1L)).thenReturn(true);

        assertThrows(DuplicateCategoryException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void testGetCategoryById_Success() {
        Category category = Category.builder().name("Desserts").merchant(merchant).active(true).build();
        category.setId(6L);

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));
        
        CategoryResponse response = CategoryResponse.builder().id(6L).name("Desserts").active(true).build();
        when(categoryMapper.toResponse(category)).thenReturn(response);

        CategoryResponse result = categoryService.getCategoryById(6L);

        assertNotNull(result);
        assertEquals(6L, result.getId());
    }

    @Test
    void testGetCategoryById_CrossTenant_ThrowsAccessDeniedException() {
        Category category = Category.builder().name("Desserts").merchant(otherMerchant).active(true).build();
        category.setId(6L);

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));

        assertThrows(AccessDeniedException.class, () -> categoryService.getCategoryById(6L));
    }

    @Test
    void testUpdateCategory_Success() {
        Category category = Category.builder().name("Desserts").merchant(merchant).active(true).build();
        category.setId(6L);

        UpdateCategoryRequest request = UpdateCategoryRequest.builder().name("Sweet Desserts").active(false).build();

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndMerchantIdAndIdNot("Sweet Desserts", 1L, 6L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse response = CategoryResponse.builder().id(6L).name("Sweet Desserts").active(false).build();
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

        CategoryResponse result = categoryService.updateCategory(6L, request);

        assertNotNull(result);
        assertFalse(result.getActive());
        assertEquals("Sweet Desserts", result.getName());
    }

    @Test
    void testDeleteCategory_Success() {
        Category category = Category.builder().name("Desserts").merchant(merchant).active(true).build();
        category.setId(6L);

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(6L);

        verify(categoryRepository, times(1)).delete(category);
    }
}
