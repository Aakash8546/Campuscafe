package com.campuscafe.backend.product.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.domain.product.ProductRecipe;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.product.dto.RecipeRequest;
import com.campuscafe.backend.product.dto.RecipeResponse;
import com.campuscafe.backend.product.repository.ProductRecipeRepository;
import com.campuscafe.backend.product.repository.ProductRepository;
import com.campuscafe.backend.inventory.repository.InventoryItemRepository;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.domain.user.User;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRecipeServiceTest {

    @Mock
    private ProductRecipeRepository productRecipeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private ProductRecipeService productRecipeService;

    private Merchant merchant;
    private User adminUser;
    private Product product;
    private InventoryItem inventoryItem;
    private ProductRecipe recipe;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().email("owner@campuscafe.com").verified(true).active(true).build();
        merchant.setId(1L);

        adminUser = User.builder().merchant(merchant).name("Admin").email("owner@campuscafe.com").active(true).build();
        adminUser.setId(1L);

        product = Product.builder().merchant(merchant).name("Coffee").price(BigDecimal.TEN).available(true).build();
        product.setId(10L);

        inventoryItem = InventoryItem.builder().merchant(merchant).name("Milk").unit("LITER").currentStock(BigDecimal.TEN).build();
        inventoryItem.setId(20L);

        recipe = ProductRecipe.builder().merchant(merchant).product(product).inventoryItem(inventoryItem).quantityRequired(BigDecimal.ONE).build();
        recipe.setId(100L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testAddRecipeItem_Success() {
        setupSecurityContext(adminUser);

        RecipeRequest request = RecipeRequest.builder()
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.ONE)
                .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(inventoryItem));
        when(productRecipeRepository.existsByProductIdAndInventoryItemId(10L, 20L)).thenReturn(false);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(productRecipeRepository.save(any(ProductRecipe.class))).thenReturn(recipe);

        RecipeResponse result = productRecipeService.addRecipeItem(10L, request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(10L, result.getProductId());
        assertEquals(20L, result.getInventoryItemId());
        assertEquals(BigDecimal.ONE, result.getQuantityRequired());
    }

    @Test
    void testAddRecipeItem_Duplicate_ThrowsException() {
        setupSecurityContext(adminUser);

        RecipeRequest request = RecipeRequest.builder()
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.ONE)
                .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(inventoryItem));
        when(productRecipeRepository.existsByProductIdAndInventoryItemId(10L, 20L)).thenReturn(true);

        assertThrows(DuplicateInventoryItemException.class, () -> productRecipeService.addRecipeItem(10L, request));
    }

    @Test
    void testGetRecipe_Success() {
        setupSecurityContext(adminUser);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRecipeRepository.findByProductIdAndMerchantId(10L, 1L)).thenReturn(List.of(recipe));

        List<RecipeResponse> result = productRecipeService.getRecipe(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    void testUpdateRecipe_Success() {
        setupSecurityContext(adminUser);

        RecipeRequest request = RecipeRequest.builder()
                .inventoryItemId(20L)
                .quantityRequired(BigDecimal.TEN)
                .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRecipeRepository.findByIdAndProductIdAndMerchantId(100L, 10L, 1L)).thenReturn(Optional.of(recipe));
        when(inventoryItemRepository.findById(20L)).thenReturn(Optional.of(inventoryItem));
        when(productRecipeRepository.existsByProductIdAndInventoryItemIdAndIdNot(10L, 20L, 100L)).thenReturn(false);
        when(productRecipeRepository.save(any(ProductRecipe.class))).thenReturn(recipe);

        RecipeResponse result = productRecipeService.updateRecipe(10L, 100L, request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    void testDeleteRecipe_Success() {
        setupSecurityContext(adminUser);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRecipeRepository.findByIdAndProductIdAndMerchantId(100L, 10L, 1L)).thenReturn(Optional.of(recipe));

        assertDoesNotThrow(() -> productRecipeService.deleteRecipe(10L, 100L));
        verify(productRecipeRepository, times(1)).delete(recipe);
    }
}
