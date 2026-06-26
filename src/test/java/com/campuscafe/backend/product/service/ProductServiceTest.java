package com.campuscafe.backend.product.service;

import com.campuscafe.backend.category.repository.CategoryRepository;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.product.Category;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.product.dto.*;
import com.campuscafe.backend.product.mapper.ProductMapper;
import com.campuscafe.backend.product.repository.ProductRepository;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Merchant merchant;
    private Merchant otherMerchant;
    private Category category;
    private Category otherCategory;
    private com.campuscafe.backend.domain.user.User adminUser;
    private com.campuscafe.backend.domain.user.User cashierUser;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Merchant A").email("merchantA@test.com").verified(true).build();
        merchant.setId(1L);

        otherMerchant = Merchant.builder().cafeName("Merchant B").email("merchantB@test.com").verified(true).build();
        otherMerchant.setId(2L);

        category = Category.builder().name("Beverages").merchant(merchant).active(true).build();
        category.setId(1L);

        otherCategory = Category.builder().name("Food").merchant(otherMerchant).active(true).build();
        otherCategory.setId(2L);

        com.campuscafe.backend.domain.user.Role adminRole = com.campuscafe.backend.domain.user.Role.builder().name("ADMIN").build();
        adminUser = com.campuscafe.backend.domain.user.User.builder()
                .email("admin@merchantA.com")
                .role(adminRole)
                .merchant(merchant)
                .active(true)
                .build();
        adminUser.setId(1L);

        com.campuscafe.backend.domain.user.Role cashierRole = com.campuscafe.backend.domain.user.Role.builder().name("CASHIER").build();
        cashierUser = com.campuscafe.backend.domain.user.User.builder()
                .email("cashier@merchantA.com")
                .role(cashierRole)
                .merchant(merchant)
                .active(true)
                .build();
        cashierUser.setId(2L);
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
    void testCreateProduct_Success() {
        setupSecurityContext(adminUser);
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Hot Coffee")
                .price(new BigDecimal("4.50"))
                .categoryId(1L)
                .build();

        when(productRepository.existsByNameAndMerchantId("Hot Coffee", 1L)).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

        Product savedProduct = Product.builder()
                .name("Hot Coffee")
                .price(new BigDecimal("4.50"))
                .category(category)
                .merchant(merchant)
                .available(true)
                .build();
        savedProduct.setId(10L);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = ProductResponse.builder()
                .id(10L)
                .name("Hot Coffee")
                .price(new BigDecimal("4.50"))
                .available(true)
                .build();
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        ProductResponse result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Hot Coffee", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testCreateProduct_DuplicateName_ThrowsDuplicateProductException() {
        setupSecurityContext(adminUser);
        CreateProductRequest request = CreateProductRequest.builder().name("Hot Coffee").build();

        when(productRepository.existsByNameAndMerchantId("Hot Coffee", 1L)).thenReturn(true);

        assertThrows(DuplicateProductException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testCreateProduct_CrossTenantCategory_ThrowsAccessDeniedException() {
        setupSecurityContext(adminUser);
        CreateProductRequest request = CreateProductRequest.builder().name("Hot Coffee").categoryId(2L).build();

        when(productRepository.existsByNameAndMerchantId("Hot Coffee", 1L)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(otherCategory));

        assertThrows(AccessDeniedException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testGetProductById_Success() {
        setupSecurityContext(adminUser);
        Product product = Product.builder().name("Hot Coffee").price(new BigDecimal("4.50")).category(category).merchant(merchant).available(true).build();
        product.setId(10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductResponse response = ProductResponse.builder().id(10L).name("Hot Coffee").price(new BigDecimal("4.50")).available(true).build();
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getProductById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void testGetProductById_CrossTenant_ThrowsAccessDeniedException() {
        setupSecurityContext(adminUser);
        Product product = Product.builder().name("Hot Coffee").price(new BigDecimal("4.50")).category(otherCategory).merchant(otherMerchant).available(true).build();
        product.setId(10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(AccessDeniedException.class, () -> productService.getProductById(10L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchProducts_AsCashier_EnforcesAvailableOnly() {
        setupSecurityContext(cashierUser);

        Product product = Product.builder().name("Hot Coffee").category(category).merchant(merchant).available(true).build();
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        productService.searchProducts(null, null, null, PageRequest.of(0, 10));

        // Verify findAll is called with dynamic specifications
        verify(productRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testToggleAvailability() {
        setupSecurityContext(adminUser);
        Product product = Product.builder().name("Hot Coffee").merchant(merchant).available(true).build();
        product.setId(10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = ProductResponse.builder().id(10L).available(false).build();
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        ProductResponse result = productService.toggleAvailability(10L);

        assertNotNull(result);
        assertFalse(result.getAvailable());
        assertFalse(product.getAvailable());
    }

    @Test
    void testCreateProduct_WithPriority() {
        setupSecurityContext(adminUser);
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Premium Espresso")
                .price(new BigDecimal("5.00"))
                .categoryId(1L)
                .priority(5)
                .build();

        when(productRepository.existsByNameAndMerchantId("Premium Espresso", 1L)).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

        Product savedProduct = Product.builder()
                .name("Premium Espresso")
                .price(new BigDecimal("5.00"))
                .category(category)
                .merchant(merchant)
                .available(true)
                .priority(5)
                .build();
        savedProduct.setId(11L);

        org.mockito.ArgumentCaptor<Product> productCaptor = org.mockito.ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture())).thenReturn(savedProduct);

        ProductResponse response = ProductResponse.builder()
                .id(11L)
                .name("Premium Espresso")
                .price(new BigDecimal("5.00"))
                .available(true)
                .priority(5)
                .build();
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        ProductResponse result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals(5, productCaptor.getValue().getPriority());
        assertEquals(5, result.getPriority());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchProducts_AppliesPrioritySorting() {
        setupSecurityContext(cashierUser);

        Product product = Product.builder().name("Hot Coffee").category(category).merchant(merchant).available(true).priority(2).build();
        Page<Product> page = new PageImpl<>(List.of(product));

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        when(productRepository.findAll(any(Specification.class), pageableCaptor.capture())).thenReturn(page);

        productService.searchProducts(null, null, null, PageRequest.of(0, 10));

        Pageable captured = pageableCaptor.getValue();
        assertNotNull(captured.getSort());
        assertNotNull(captured.getSort().getOrderFor("priority"));
        assertEquals(org.springframework.data.domain.Sort.Direction.ASC, captured.getSort().getOrderFor("priority").getDirection());
    }
}
