package com.campuscafe.backend.product.service;

import com.campuscafe.backend.category.repository.CategoryRepository;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.product.Category;
import com.campuscafe.backend.domain.product.Product;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.product.dto.*;
import com.campuscafe.backend.product.mapper.ProductMapper;
import com.campuscafe.backend.product.repository.ProductRepository;
import com.campuscafe.backend.product.specification.ProductSpecification;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final ProductMapper productMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        // Unique product name validation within merchant
        if (productRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateProductException("Product already exists with name: " + request.getName());
        }

        // Fetch and validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (!category.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this category");
        }

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .merchant(merchant)
                .available(true)
                .build();

        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(String name, Long categoryId, Boolean available, Pageable pageable) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Specification<Product> spec = Specification.where(ProductSpecification.withMerchantId(merchantId));

        if (name != null) {
            spec = spec.and(ProductSpecification.withName(name));
        }
        if (categoryId != null) {
            spec = spec.and(ProductSpecification.withCategoryId(categoryId));
        }

        // Cashier visibility rule: Cashiers can only see available products
        if (currentUser.getRole().equals("CASHIER")) {
            spec = spec.and(ProductSpecification.withAvailable(true));
        } else if (available != null) {
            spec = spec.and(ProductSpecification.withAvailable(available));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        // Tenant check
        if (!product.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this product");
        }

        // Cashier check: Cashier cannot see unavailable products
        if (currentUser.getRole().equals("CASHIER") && !product.getAvailable()) {
            throw new AccessDeniedException("You do not have access to this product");
        }

        return productMapper.toResponse(product);
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        // Tenant check
        if (!product.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this product");
        }

        // Unique product name validation
        if (productRepository.existsByNameAndMerchantIdAndIdNot(request.getName(), merchantId, id)) {
            throw new DuplicateProductException("Product already exists with name: " + request.getName());
        }

        // Fetch and validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + request.getCategoryId()));

        if (!category.getMerchant().getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have access to this category");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    public ProductResponse toggleAvailability(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        // Tenant check
        if (!product.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this product");
        }

        product.setAvailable(!product.getAvailable());
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        // Tenant check
        if (!product.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this product");
        }

        productRepository.delete(product);
    }
}
