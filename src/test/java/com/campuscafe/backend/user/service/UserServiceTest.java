package com.campuscafe.backend.user.service;

import com.campuscafe.backend.domain.merchant.Merchant;
import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.Role;
import com.campuscafe.backend.domain.user.User;
import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.repository.MerchantRepository;
import com.campuscafe.backend.repository.RoleRepository;
import com.campuscafe.backend.repository.UserRepository;
import com.campuscafe.backend.security.service.CustomUserDetails;
import com.campuscafe.backend.user.dto.CreateUserRequest;
import com.campuscafe.backend.user.dto.UpdateUserRequest;
import com.campuscafe.backend.user.dto.UserResponse;
import com.campuscafe.backend.user.dto.UserSummaryResponse;
import com.campuscafe.backend.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private Merchant merchant;
    private Merchant otherMerchant;
    private Role adminRole;
    private Role managerRole;
    private User adminUser;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().cafeName("Merchant A").email("merchantA@test.com").verified(true).build();
        merchant.setId(1L);

        otherMerchant = Merchant.builder().cafeName("Merchant B").email("merchantB@test.com").verified(true).build();
        otherMerchant.setId(2L);

        Permission viewPerm = Permission.builder().id(1L).name("USER_VIEW").build();
        Permission createPerm = Permission.builder().id(2L).name("USER_CREATE").build();

        adminRole = Role.builder().id(1L).name("ADMIN").permissions(Set.of(viewPerm, createPerm)).build();
        managerRole = Role.builder().id(2L).name("MANAGER").permissions(Set.of(viewPerm)).build();

        adminUser = User.builder()
                .email("admin@merchantA.com")
                .password("encodedPassword")
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
    void testCreateUser_Success() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New Manager")
                .email("manager@merchantA.com")
                .phone("1234567890")
                .password("Password@123")
                .role("MANAGER")
                .build();

        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchant));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        User savedUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(managerRole)
                .merchant(merchant)
                .active(true)
                .build();
        savedUser.setId(2L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        UserResponse response = UserResponse.builder()
                .id(2L)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role("MANAGER")
                .active(true)
                .build();
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("MANAGER", result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("New Manager")
                .email("manager@merchantA.com")
                .phone("1234567890")
                .password("Password@123")
                .role("MANAGER")
                .build();

        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_RoleNotFound() {
        CreateUserRequest request = CreateUserRequest.builder()
                .role("INVALID_ROLE")
                .build();

        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> userService.createUser(request));
    }

    @Test
    void testGetAllUsers_Success() {
        User user1 = User.builder().email("user1@merchantA.com").merchant(merchant).role(managerRole).active(true).build();
        user1.setId(2L);
        
        when(userRepository.findByMerchantId(1L)).thenReturn(List.of(adminUser, user1));
        
        UserSummaryResponse summary = UserSummaryResponse.builder().id(2L).email("user1@merchantA.com").role("MANAGER").active(true).build();
        when(userMapper.toSummaryResponse(any(User.class))).thenReturn(summary);

        List<UserSummaryResponse> results = userService.getAllUsers();

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(userRepository, times(1)).findByMerchantId(1L);
    }

    @Test
    void testGetUserById_Success() {
        User targetUser = User.builder().email("target@merchantA.com").merchant(merchant).role(managerRole).active(true).build();
        targetUser.setId(3L);

        when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));
        
        UserResponse response = UserResponse.builder().id(3L).email("target@merchantA.com").role("MANAGER").active(true).build();
        when(userMapper.toResponse(targetUser)).thenReturn(response);

        UserResponse result = userService.getUserById(3L);

        assertNotNull(result);
        assertEquals(3L, result.getId());
    }

    @Test
    void testGetUserById_CrossTenantAccess_ThrowsAccessDeniedException() {
        User crossUser = User.builder().email("cross@merchantB.com").merchant(otherMerchant).role(managerRole).active(true).build();
        crossUser.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(crossUser));

        assertThrows(AccessDeniedException.class, () -> userService.getUserById(10L));
    }

    @Test
    void testDeactivateUser_Success() {
        User targetUser = User.builder().email("target@merchantA.com").merchant(merchant).role(managerRole).active(true).build();
        targetUser.setId(3L);

        when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        UserResponse response = UserResponse.builder().id(3L).email("target@merchantA.com").role("MANAGER").active(false).build();
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        UserResponse result = userService.deactivateUser(3L);

        assertNotNull(result);
        assertFalse(result.getActive());
        assertFalse(targetUser.getActive());
    }

    @Test
    void testDeleteUser_Success() {
        User targetUser = User.builder().email("target@merchantA.com").merchant(merchant).role(managerRole).active(true).build();
        targetUser.setId(3L);

        when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));

        userService.deleteUser(3L);

        verify(userRepository, times(1)).delete(targetUser);
    }
}
