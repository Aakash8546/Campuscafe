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
import com.campuscafe.backend.inventory.specification.InventoryItemSpecification;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.NotificationRepository;
import com.campuscafe.backend.repository.UserRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    private final InventoryItemMapper itemMapper;
    private final InventoryTransactionMapper transactionMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private User getCurrentUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user details not found"));
    }

    public InventoryItemResponse createItem(CreateInventoryItemRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        if (inventoryItemRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateInventoryItemException("Inventory item already exists with name: " + request.getName());
        }

        InventoryCategory category = inventoryCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InventoryCategoryNotFoundException("Inventory category not found with id: " + request.getCategoryId()));

        if (!category.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory category");
        }

        if (request.getMaxStock().compareTo(request.getMinStock()) < 0) {
            throw new InventoryValidationException("Max stock must be greater than or equal to min stock");
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

        InventoryItem item = InventoryItem.builder()
                .name(request.getName())
                .unit(request.getUnit())
                .currentStock(request.getCurrentStock())
                .minStock(request.getMinStock())
                .maxStock(request.getMaxStock())
                .category(category)
                .merchant(merchant)
                .build();

        InventoryItem savedItem = inventoryItemRepository.save(item);

        checkLowStockAndNotify(savedItem, merchant);

        return itemMapper.toResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemSummaryResponse> getAllItems(String name, Long categoryId, Boolean lowStock, Pageable pageable) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Specification<InventoryItem> spec = Specification.where(InventoryItemSpecification.withMerchantId(merchantId));

        if (name != null) {
            spec = spec.and(InventoryItemSpecification.withName(name));
        }
        if (categoryId != null) {
            spec = spec.and(InventoryItemSpecification.withCategoryId(categoryId));
        }
        if (lowStock != null) {
            spec = spec.and(InventoryItemSpecification.withLowStock(lowStock));
        }

        Page<InventoryItem> items = inventoryItemRepository.findAll(spec, pageable);
        return items.map(itemMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getItemById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found with id: " + id));

        if (!item.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this inventory item");
        }

        return itemMapper.toResponse(item);
    }

    public InventoryItemResponse updateItem(Long id, UpdateInventoryItemRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found with id: " + id));

        if (!item.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory item");
        }

        if (inventoryItemRepository.existsByNameAndMerchantIdAndIdNot(request.getName(), merchantId, id)) {
            throw new DuplicateInventoryItemException("Inventory item already exists with name: " + request.getName());
        }

        InventoryCategory category = inventoryCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InventoryCategoryNotFoundException("Inventory category not found with id: " + request.getCategoryId()));

        if (!category.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory category");
        }

        if (request.getMaxStock().compareTo(request.getMinStock()) < 0) {
            throw new InventoryValidationException("Max stock must be greater than or equal to min stock");
        }

        item.setName(request.getName());
        item.setUnit(request.getUnit());
        item.setMinStock(request.getMinStock());
        item.setMaxStock(request.getMaxStock());
        item.setCategory(category);

        InventoryItem updatedItem = inventoryItemRepository.save(item);

        checkLowStockAndNotify(updatedItem, item.getMerchant());

        return itemMapper.toResponse(updatedItem);
    }

    public void deleteItem(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found with id: " + id));

        if (!item.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this inventory item");
        }

        inventoryItemRepository.delete(item);
    }

    public InventoryItemResponse stockIn(StockInRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        InventoryItem item = inventoryItemRepository.findById(request.getInventoryItemId())
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found with id: " + request.getInventoryItemId()));

        if (!item.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory item");
        }

        item.setCurrentStock(item.getCurrentStock().add(request.getQuantity()));
        InventoryItem savedItem = inventoryItemRepository.save(item);

        // Save transaction log
        User userEntity = getCurrentUserEntity(currentUser.getUserId());
        InventoryTransaction transaction = InventoryTransaction.builder()
                .inventoryItem(savedItem)
                .type(InventoryTransactionType.STOCK_IN)
                .quantity(request.getQuantity())
                .remarks(request.getRemarks())
                .createdBy(userEntity)
                .build();
        inventoryTransactionRepository.save(transaction);

        checkLowStockAndNotify(savedItem, item.getMerchant());

        return itemMapper.toResponse(savedItem);
    }

    public InventoryItemResponse stockOut(StockOutRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        InventoryItem item = inventoryItemRepository.findById(request.getInventoryItemId())
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found with id: " + request.getInventoryItemId()));

        if (!item.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory item");
        }

        if (item.getCurrentStock().compareTo(request.getQuantity()) < 0) {
            throw new InsufficientStockException("Insufficient stock. Available: " + item.getCurrentStock() + ", requested: " + request.getQuantity());
        }

        item.setCurrentStock(item.getCurrentStock().subtract(request.getQuantity()));
        InventoryItem savedItem = inventoryItemRepository.save(item);

        // Save transaction log
        User userEntity = getCurrentUserEntity(currentUser.getUserId());
        InventoryTransaction transaction = InventoryTransaction.builder()
                .inventoryItem(savedItem)
                .type(InventoryTransactionType.STOCK_OUT)
                .quantity(request.getQuantity())
                .remarks(request.getRemarks())
                .createdBy(userEntity)
                .build();
        inventoryTransactionRepository.save(transaction);

        checkLowStockAndNotify(savedItem, item.getMerchant());

        return itemMapper.toResponse(savedItem);
    }

    public InventoryItemResponse adjustStock(StockAdjustmentRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        InventoryItem item = inventoryItemRepository.findById(request.getInventoryItemId())
                .orElseThrow(() -> new InventoryItemNotFoundException("Inventory item not found with id: " + request.getInventoryItemId()));

        if (!item.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this inventory item");
        }

        BigDecimal oldStock = item.getCurrentStock();
        BigDecimal newStock = request.getNewQuantity();

        item.setCurrentStock(newStock);
        InventoryItem savedItem = inventoryItemRepository.save(item);

        // Save transaction log (using absolute change quantity)
        BigDecimal change = newStock.subtract(oldStock).abs();
        User userEntity = getCurrentUserEntity(currentUser.getUserId());
        InventoryTransaction transaction = InventoryTransaction.builder()
                .inventoryItem(savedItem)
                .type(InventoryTransactionType.ADJUSTMENT)
                .quantity(change)
                .remarks(request.getRemarks())
                .createdBy(userEntity)
                .build();
        inventoryTransactionRepository.save(transaction);

        checkLowStockAndNotify(savedItem, item.getMerchant());

        return itemMapper.toResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public InventoryDashboardResponse getDashboardMetrics() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        long totalItems = inventoryItemRepository.countByMerchantId(merchantId);
        long lowStockItems = inventoryItemRepository.countLowStockItems(merchantId);
        long totalCategories = inventoryCategoryRepository.countByMerchantId(merchantId);

        return InventoryDashboardResponse.builder()
                .totalItems(totalItems)
                .lowStockItems(lowStockItems)
                .totalCategories(totalCategories)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getTransactionHistory(Pageable pageable) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Page<InventoryTransaction> transactions = inventoryTransactionRepository
                .findByInventoryItemMerchantId(merchantId, pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    private void checkLowStockAndNotify(InventoryItem item, Merchant merchant) {
        if (item.getCurrentStock().compareTo(item.getMinStock()) <= 0) {
            String title = "Low Stock Alert: " + item.getName();
            boolean exists = notificationRepository.existsByMerchantIdAndTypeAndTitleAndReadStatus(
                    merchant.getId(), NotificationType.LOW_STOCK, title, false
            );
            if (!exists) {
                Notification notification = Notification.builder()
                        .merchant(merchant)
                        .type(NotificationType.LOW_STOCK)
                        .title(title)
                        .message(String.format("Inventory item '%s' is running low. Current stock: %s %s (Min threshold: %s %s).",
                                item.getName(), item.getCurrentStock(), item.getUnit(), item.getMinStock(), item.getUnit()))
                        .readStatus(false)
                        .build();
                notificationRepository.save(notification);
            }
        }
    }
}
