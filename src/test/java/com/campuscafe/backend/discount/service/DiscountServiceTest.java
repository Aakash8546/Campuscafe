package com.campuscafe.backend.discount.service;

import com.campuscafe.backend.discount.dto.CreateDiscountRequest;
import com.campuscafe.backend.discount.dto.DiscountResponse;
import com.campuscafe.backend.discount.dto.UpdateDiscountRequest;
import com.campuscafe.backend.discount.mapper.DiscountMapper;
import com.campuscafe.backend.discount.repository.DiscountRepository;
import com.campuscafe.backend.domain.discount.Discount;
import com.campuscafe.backend.domain.discount.enums.DiscountType;
import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.AccessDeniedException;
import com.campuscafe.backend.exception.DiscountNotFoundException;
import com.campuscafe.backend.exception.DuplicateDiscountException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private DiscountMapper discountMapper;

    @InjectMocks
    private DiscountService discountService;

    private Merchant merchant;
    private User adminUser;
    private Discount percentageDiscount;
    private Discount flatDiscount;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Cafe A").email("cafeA@test.com").verified(true).build();
        merchant.setId(1L);

        Role adminRole = Role.builder().name("ADMIN").build();
        adminUser = User.builder().email("admin@cafeA.com").role(adminRole).merchant(merchant).active(true).build();
        adminUser.setId(1L);

        percentageDiscount = Discount.builder()
                .merchant(merchant)
                .name("10% OFF")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .active(true)
                .build();
        percentageDiscount.setId(100L);

        flatDiscount = Discount.builder()
                .merchant(merchant)
                .name("Flat 50")
                .discountType(DiscountType.FLAT)
                .value(new BigDecimal("50.00"))
                .active(true)
                .build();
        flatDiscount.setId(200L);
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
    void testCreatePercentageDiscount_Success() {
        setupSecurityContext(adminUser);

        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .name("10% OFF")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .active(true)
                .build();

        when(discountRepository.existsByMerchantIdAndNameIgnoreCase(1L, "10% OFF")).thenReturn(false);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(discountRepository.save(any(Discount.class))).thenReturn(percentageDiscount);

        DiscountResponse response = DiscountResponse.builder()
                .id(100L)
                .name("10% OFF")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .active(true)
                .build();
        when(discountMapper.toResponse(any(Discount.class))).thenReturn(response);

        DiscountResponse result = discountService.createDiscount(request);

        assertNotNull(result);
        assertEquals("10% OFF", result.getName());
        assertEquals(DiscountType.PERCENTAGE, result.getDiscountType());
        assertEquals(new BigDecimal("10.00"), result.getValue());

        verify(discountRepository, times(1)).save(any(Discount.class));
    }

    @Test
    void testCreateDiscount_DuplicateName_ThrowsDuplicateDiscountException() {
        setupSecurityContext(adminUser);

        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .name("10% OFF")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .build();

        when(discountRepository.existsByMerchantIdAndNameIgnoreCase(1L, "10% OFF")).thenReturn(true);

        assertThrows(DuplicateDiscountException.class, () -> discountService.createDiscount(request));
    }

    @Test
    void testCreatePercentageDiscount_InvalidValue_ThrowsIllegalArgumentException() {
        setupSecurityContext(adminUser);

        CreateDiscountRequest requestTooHigh = CreateDiscountRequest.builder()
                .name("Super Promo")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("101.00"))
                .build();

        CreateDiscountRequest requestTooLow = CreateDiscountRequest.builder()
                .name("Super Promo")
                .discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.ZERO)
                .build();

        when(discountRepository.existsByMerchantIdAndNameIgnoreCase(1L, "Super Promo")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> discountService.createDiscount(requestTooHigh));
        assertThrows(IllegalArgumentException.class, () -> discountService.createDiscount(requestTooLow));
    }

    @Test
    void testCreateFlatDiscount_InvalidValue_ThrowsIllegalArgumentException() {
        setupSecurityContext(adminUser);

        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .name("Super Promo")
                .discountType(DiscountType.FLAT)
                .value(new BigDecimal("-5.00"))
                .build();

        when(discountRepository.existsByMerchantIdAndNameIgnoreCase(1L, "Super Promo")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> discountService.createDiscount(request));
    }

    @Test
    void testGetDiscounts_Success() {
        setupSecurityContext(adminUser);

        when(discountRepository.findByMerchantId(1L)).thenReturn(List.of(percentageDiscount, flatDiscount));
        when(discountMapper.toResponseList(anyList())).thenReturn(List.of(
                DiscountResponse.builder().id(100L).name("10% OFF").build(),
                DiscountResponse.builder().id(200L).name("Flat 50").build()
        ));

        List<DiscountResponse> result = discountService.getDiscounts();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetDiscountById_NotFound_ThrowsDiscountNotFoundException() {
        setupSecurityContext(adminUser);

        when(discountRepository.findByIdAndMerchantId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(DiscountNotFoundException.class, () -> discountService.getDiscountById(999L));
    }

    @Test
    void testUpdateDiscount_Success() {
        setupSecurityContext(adminUser);

        UpdateDiscountRequest request = UpdateDiscountRequest.builder()
                .name("20% OFF")
                .discountType(DiscountType.PERCENTAGE)
                .value(new BigDecimal("20.00"))
                .active(true)
                .build();

        when(discountRepository.findByIdAndMerchantId(100L, 1L)).thenReturn(Optional.of(percentageDiscount));
        when(discountRepository.existsByMerchantIdAndNameIgnoreCaseAndIdNot(1L, "20% OFF", 100L)).thenReturn(false);
        when(discountRepository.save(any(Discount.class))).thenReturn(percentageDiscount);

        DiscountResponse response = DiscountResponse.builder().id(100L).name("20% OFF").active(true).build();
        when(discountMapper.toResponse(any(Discount.class))).thenReturn(response);

        DiscountResponse result = discountService.updateDiscount(100L, request);

        assertNotNull(result);
        assertEquals("20% OFF", result.getName());
    }

    @Test
    void testDeactivateDiscount_Success() {
        setupSecurityContext(adminUser);

        when(discountRepository.findByIdAndMerchantId(100L, 1L)).thenReturn(Optional.of(percentageDiscount));
        when(discountRepository.save(any(Discount.class))).thenReturn(percentageDiscount);

        DiscountResponse response = DiscountResponse.builder().id(100L).active(false).build();
        when(discountMapper.toResponse(any(Discount.class))).thenReturn(response);

        DiscountResponse result = discountService.deactivateDiscount(100L);

        assertNotNull(result);
        assertFalse(result.getActive());
        assertFalse(percentageDiscount.getActive());
    }
}
