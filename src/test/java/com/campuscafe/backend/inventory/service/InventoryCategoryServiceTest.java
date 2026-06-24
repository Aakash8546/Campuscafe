package com.campuscafe.backend.inventory.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.inventory.InventoryCategory;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.inventory.dto.*;
import com.campuscafe.backend.inventory.mapper.InventoryCategoryMapper;
import com.campuscafe.backend.inventory.repository.InventoryCategoryRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryCategoryServiceTest {

    @Mock
    private InventoryCategoryRepository categoryRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private InventoryCategoryMapper categoryMapper;

    @InjectMocks
    private InventoryCategoryService categoryService;

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
        setupSecurityContext(adminUser);
        CreateInventoryCategoryRequest request = CreateInventoryCategoryRequest.builder().name("Packaging").build();

        when(categoryRepository.existsByNameAndMerchantId("Packaging", 1L)).thenReturn(false);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

        InventoryCategory savedCategory = InventoryCategory.builder().name("Packaging").merchant(merchant).build();
        savedCategory.setId(5L);
        when(categoryRepository.save(any(InventoryCategory.class))).thenReturn(savedCategory);

        InventoryCategoryResponse response = InventoryCategoryResponse.builder().id(5L).name("Packaging").build();
        when(categoryMapper.toResponse(any(InventoryCategory.class))).thenReturn(response);

        InventoryCategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Packaging", result.getName());
        verify(categoryRepository, times(1)).save(any(InventoryCategory.class));
    }

    @Test
    void testCreateCategory_DuplicateName_ThrowsDuplicateInventoryCategoryException() {
        setupSecurityContext(adminUser);
        CreateInventoryCategoryRequest request = CreateInventoryCategoryRequest.builder().name("Packaging").build();

        when(categoryRepository.existsByNameAndMerchantId("Packaging", 1L)).thenReturn(true);

        assertThrows(DuplicateInventoryCategoryException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any(InventoryCategory.class));
    }

    @Test
    void testGetCategoryById_Success() {
        setupSecurityContext(adminUser);
        InventoryCategory category = InventoryCategory.builder().name("Others").merchant(merchant).build();
        category.setId(6L);

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));

        InventoryCategoryResponse response = InventoryCategoryResponse.builder().id(6L).name("Others").build();
        when(categoryMapper.toResponse(category)).thenReturn(response);

        InventoryCategoryResponse result = categoryService.getCategoryById(6L);

        assertNotNull(result);
        assertEquals(6L, result.getId());
    }

    @Test
    void testGetCategoryById_CrossTenant_ThrowsAccessDeniedException() {
        setupSecurityContext(adminUser);
        InventoryCategory category = InventoryCategory.builder().name("Others").merchant(otherMerchant).build();
        category.setId(6L);

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));

        assertThrows(AccessDeniedException.class, () -> categoryService.getCategoryById(6L));
    }

    @Test
    void testUpdateCategory_Success() {
        setupSecurityContext(adminUser);
        InventoryCategory category = InventoryCategory.builder().name("Others").merchant(merchant).build();
        category.setId(6L);

        UpdateInventoryCategoryRequest request = UpdateInventoryCategoryRequest.builder().name("Miscellaneous").build();

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndMerchantIdAndIdNot("Miscellaneous", 1L, 6L)).thenReturn(false);
        when(categoryRepository.save(any(InventoryCategory.class))).thenReturn(category);

        InventoryCategoryResponse response = InventoryCategoryResponse.builder().id(6L).name("Miscellaneous").build();
        when(categoryMapper.toResponse(any(InventoryCategory.class))).thenReturn(response);

        InventoryCategoryResponse result = categoryService.updateCategory(6L, request);

        assertNotNull(result);
        assertEquals("Miscellaneous", result.getName());
    }

    @Test
    void testDeleteCategory_Success() {
        setupSecurityContext(adminUser);
        InventoryCategory category = InventoryCategory.builder().name("Others").merchant(merchant).build();
        category.setId(6L);

        when(categoryRepository.findById(6L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(6L);

        verify(categoryRepository, times(1)).delete(category);
    }
}
