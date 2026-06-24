package com.campuscafe.backend.inventory.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.inventory.InventoryCategory;
import com.campuscafe.backend.domain.inventory.InventoryItem;
import com.campuscafe.backend.domain.inventory.InventoryTransaction;
import com.campuscafe.backend.domain.inventory.enums.InventoryTransactionType;
import com.campuscafe.backend.domain.notification.Notification;
import com.campuscafe.backend.domain.notification.enums.NotificationType;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.inventory.dto.*;
import com.campuscafe.backend.inventory.mapper.*;
import com.campuscafe.backend.inventory.repository.*;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.NotificationRepository;
import com.campuscafe.backend.repository.UserRepository;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryCategoryRepository inventoryCategoryRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private InventoryItemMapper itemMapper;

    @Mock
    private InventoryTransactionMapper transactionMapper;

    @InjectMocks
    private InventoryItemService itemService;

    private Merchant merchant;
    private Merchant otherMerchant;
    private InventoryCategory category;
    private InventoryCategory otherCategory;
    private User adminUser;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Merchant A").email("merchantA@test.com").verified(true).build();
        merchant.setId(1L);

        otherMerchant = Merchant.builder().cafeName("Merchant B").email("merchantB@test.com").verified(true).build();
        otherMerchant.setId(2L);

        category = InventoryCategory.builder().name("Raw Material").merchant(merchant).build();
        category.setId(1L);

        otherCategory = InventoryCategory.builder().name("Packaging").merchant(otherMerchant).build();
        otherCategory.setId(2L);

        com.campuscafe.backend.domain.user.Role adminRole = com.campuscafe.backend.domain.user.Role.builder().name("ADMIN").build();
        adminUser = User.builder()
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
    void testCreateItem_Success() {
        setupSecurityContext(adminUser);
        CreateInventoryItemRequest request = CreateInventoryItemRequest.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("2.000"))
                .maxStock(new BigDecimal("20.000"))
                .categoryId(1L)
                .build();

        when(inventoryItemRepository.existsByNameAndMerchantId("Milk", 1L)).thenReturn(false);
        when(inventoryCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));

        InventoryItem savedItem = InventoryItem.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("2.000"))
                .maxStock(new BigDecimal("20.000"))
                .category(category)
                .merchant(merchant)
                .build();
        savedItem.setId(10L);
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(savedItem);

        InventoryItemResponse response = InventoryItemResponse.builder()
                .id(10L)
                .name("Milk")
                .currentStock(new BigDecimal("10.000"))
                .build();
        when(itemMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        InventoryItemResponse result = itemService.createItem(request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Milk", result.getName());
        verify(inventoryItemRepository, times(1)).save(any(InventoryItem.class));
    }

    @Test
    void testCreateItem_DuplicateName_ThrowsDuplicateInventoryItemException() {
        setupSecurityContext(adminUser);
        CreateInventoryItemRequest request = CreateInventoryItemRequest.builder().name("Milk").build();

        when(inventoryItemRepository.existsByNameAndMerchantId("Milk", 1L)).thenReturn(true);

        assertThrows(DuplicateInventoryItemException.class, () -> itemService.createItem(request));
    }

    @Test
    void testCreateItem_InvalidMaxStock_ThrowsInventoryValidationException() {
        setupSecurityContext(adminUser);
        CreateInventoryItemRequest request = CreateInventoryItemRequest.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("5.000"))
                .maxStock(new BigDecimal("2.000"))
                .categoryId(1L)
                .build();

        when(inventoryItemRepository.existsByNameAndMerchantId("Milk", 1L)).thenReturn(false);
        when(inventoryCategoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(InventoryValidationException.class, () -> itemService.createItem(request));
    }

    @Test
    void testStockIn_Success() {
        setupSecurityContext(adminUser);
        StockInRequest request = StockInRequest.builder()
                .inventoryItemId(10L)
                .quantity(new BigDecimal("5.000"))
                .remarks("Buy stock")
                .build();

        InventoryItem item = InventoryItem.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("2.000"))
                .maxStock(new BigDecimal("20.000"))
                .merchant(merchant)
                .build();
        item.setId(10L);

        when(inventoryItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(item);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        InventoryItemResponse response = InventoryItemResponse.builder()
                .id(10L)
                .currentStock(new BigDecimal("15.000"))
                .build();
        when(itemMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        InventoryItemResponse result = itemService.stockIn(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("15.000"), result.getCurrentStock());
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
    }

    @Test
    void testStockOut_Success() {
        setupSecurityContext(adminUser);
        StockOutRequest request = StockOutRequest.builder()
                .inventoryItemId(10L)
                .quantity(new BigDecimal("4.000"))
                .remarks("Consume stock")
                .build();

        InventoryItem item = InventoryItem.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("2.000"))
                .maxStock(new BigDecimal("20.000"))
                .merchant(merchant)
                .build();
        item.setId(10L);

        when(inventoryItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(item);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        InventoryItemResponse response = InventoryItemResponse.builder()
                .id(10L)
                .currentStock(new BigDecimal("6.000"))
                .build();
        when(itemMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        InventoryItemResponse result = itemService.stockOut(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("6.000"), result.getCurrentStock());
    }

    @Test
    void testStockOut_InsufficientStock_ThrowsInsufficientStockException() {
        setupSecurityContext(adminUser);
        StockOutRequest request = StockOutRequest.builder()
                .inventoryItemId(10L)
                .quantity(new BigDecimal("15.000"))
                .build();

        InventoryItem item = InventoryItem.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("2.000"))
                .maxStock(new BigDecimal("20.000"))
                .merchant(merchant)
                .build();
        item.setId(10L);

        when(inventoryItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(InsufficientStockException.class, () -> itemService.stockOut(request));
    }

    @Test
    void testStockAdjustment_Success() {
        setupSecurityContext(adminUser);
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .inventoryItemId(10L)
                .newQuantity(new BigDecimal("12.000"))
                .remarks("Correction")
                .build();

        InventoryItem item = InventoryItem.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("2.000"))
                .maxStock(new BigDecimal("20.000"))
                .merchant(merchant)
                .build();
        item.setId(10L);

        when(inventoryItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(item);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        InventoryItemResponse response = InventoryItemResponse.builder()
                .id(10L)
                .currentStock(new BigDecimal("12.000"))
                .build();
        when(itemMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        InventoryItemResponse result = itemService.adjustStock(request);

        assertNotNull(result);
        assertEquals(new BigDecimal("12.000"), result.getCurrentStock());
    }

    @Test
    void testLowStockNotificationTriggered() {
        setupSecurityContext(adminUser);
        StockOutRequest request = StockOutRequest.builder()
                .inventoryItemId(10L)
                .quantity(new BigDecimal("9.000"))
                .remarks("Consume stock")
                .build();

        InventoryItem item = InventoryItem.builder()
                .name("Milk")
                .unit("LITER")
                .currentStock(new BigDecimal("10.000"))
                .minStock(new BigDecimal("2.000"))
                .maxStock(new BigDecimal("20.000"))
                .merchant(merchant)
                .build();
        item.setId(10L);

        when(inventoryItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenReturn(item);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When stock becomes 1.000 (which is <= minStock 2.000), it should trigger low stock notice
        when(notificationRepository.existsByMerchantIdAndTypeAndTitleAndReadStatus(
                eq(1L), eq(NotificationType.LOW_STOCK), anyString(), eq(false)
        )).thenReturn(false);

        itemService.stockOut(request);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
