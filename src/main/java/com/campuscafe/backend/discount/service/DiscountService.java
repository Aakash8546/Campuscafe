package com.campuscafe.backend.discount.service;

import com.campuscafe.backend.discount.dto.CreateDiscountRequest;
import com.campuscafe.backend.discount.dto.DiscountResponse;
import com.campuscafe.backend.discount.dto.UpdateDiscountRequest;
import com.campuscafe.backend.discount.mapper.DiscountMapper;
import com.campuscafe.backend.discount.repository.DiscountRepository;
import com.campuscafe.backend.domain.discount.Discount;
import com.campuscafe.backend.domain.discount.enums.DiscountType;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.DiscountNotFoundException;
import com.campuscafe.backend.exception.DuplicateDiscountException;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final MerchantRepository merchantRepository;
    private final DiscountMapper discountMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    private void validateDiscountValue(DiscountType type, BigDecimal value) {
        if (type == DiscountType.PERCENTAGE) {
            if (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage discount value must be between 1 and 100");
            }
        } else if (type == DiscountType.FLAT) {
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Flat discount value must be greater than zero");
            }
        }
    }

    public DiscountResponse createDiscount(CreateDiscountRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        if (discountRepository.existsByMerchantIdAndNameIgnoreCase(merchantId, request.getName())) {
            throw new DuplicateDiscountException("Discount with name '" + request.getName() + "' already exists");
        }

        validateDiscountValue(request.getDiscountType(), request.getValue());

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new AccessDeniedException("Merchant not found"));

        Discount discount = Discount.builder()
                .merchant(merchant)
                .name(request.getName())
                .discountType(request.getDiscountType())
                .value(request.getValue())
                .maxDiscount(request.getMaxDiscount())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Discount saved = discountRepository.save(discount);
        return discountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getDiscounts() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        List<Discount> discounts = discountRepository.findByMerchantId(merchantId);
        return discountMapper.toResponseList(discounts);
    }

    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Discount discount = discountRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new DiscountNotFoundException("Discount not found with id: " + id));

        return discountMapper.toResponse(discount);
    }

    public DiscountResponse updateDiscount(Long id, UpdateDiscountRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Discount discount = discountRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new DiscountNotFoundException("Discount not found with id: " + id));

        if (discountRepository.existsByMerchantIdAndNameIgnoreCaseAndIdNot(merchantId, request.getName(), id)) {
            throw new DuplicateDiscountException("Discount with name '" + request.getName() + "' already exists");
        }

        validateDiscountValue(request.getDiscountType(), request.getValue());

        discount.setName(request.getName());
        discount.setDiscountType(request.getDiscountType());
        discount.setValue(request.getValue());
        discount.setMaxDiscount(request.getMaxDiscount());
        discount.setActive(request.getActive());

        Discount updated = discountRepository.save(discount);
        return discountMapper.toResponse(updated);
    }

    public DiscountResponse activateDiscount(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Discount discount = discountRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new DiscountNotFoundException("Discount not found with id: " + id));

        discount.setActive(true);
        Discount updated = discountRepository.save(discount);
        return discountMapper.toResponse(updated);
    }

    public DiscountResponse deactivateDiscount(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        Long merchantId = currentUser.getMerchantId();

        Discount discount = discountRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new DiscountNotFoundException("Discount not found with id: " + id));

        discount.setActive(false);
        Discount updated = discountRepository.save(discount);
        return discountMapper.toResponse(updated);
    }
}
