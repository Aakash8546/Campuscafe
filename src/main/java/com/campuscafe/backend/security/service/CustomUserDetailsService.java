package com.campuscafe.backend.security.service;

import com.campuscafe.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.campuscafe.backend.domain.user.User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        
        // Eagerly initialize lazy loaded associations inside the active transaction
        if (user.getRole() != null) {
            user.getRole().getName();
            if (user.getRole().getPermissions() != null) {
                user.getRole().getPermissions().size();
            }
        }
        if (user.getMerchant() != null) {
            user.getMerchant().getId();
        }
        
        return new CustomUserDetails(user);
    }
}
