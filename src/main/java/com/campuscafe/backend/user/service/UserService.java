package com.campuscafe.backend.user.service;

import com.campuscafe.backend.domain.merchant.Merchant;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    private CustomUserDetails getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public UserResponse createUser(CreateUserRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        
        // Verify if role exists
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + request.getRole()));

        // Check global email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }

        // Fetch merchant of current authenticated user
        Merchant merchant = merchantRepository.findById(currentUser.getMerchantId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user's merchant not found"));

        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .merchant(merchant)
                .active(true)
                .build();

        User savedUser = userRepository.save(newUser);
        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> getAllUsers() {
        CustomUserDetails currentUser = getAuthenticatedUser();
        List<User> users = userRepository.findByMerchantId(currentUser.getMerchantId());
        return users.stream()
                .map(userMapper::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Multi-tenant boundary check
        if (!user.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this user");
        }

        return userMapper.toResponse(user);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Multi-tenant boundary check
        if (!user.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this user");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + request.getRole()));

        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRole(role);

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    public UserResponse deactivateUser(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Multi-tenant boundary check
        if (!user.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this user");
        }

        user.setActive(false);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    public UserResponse activateUser(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Multi-tenant boundary check
        if (!user.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this user");
        }

        user.setActive(true);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        CustomUserDetails currentUser = getAuthenticatedUser();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // Multi-tenant boundary check
        if (!user.getMerchant().getId().equals(currentUser.getMerchantId())) {
            throw new AccessDeniedException("You do not have access to this user");
        }

        userRepository.delete(user);
    }
}
