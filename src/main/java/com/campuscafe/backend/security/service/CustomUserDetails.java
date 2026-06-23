package com.campuscafe.backend.security.service;

import com.campuscafe.backend.domain.user.Permission;
import com.campuscafe.backend.domain.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
            if (user.getRole().getPermissions() != null) {
                for (Permission permission : user.getRole().getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getName()));
                }
            }
        }
        return authorities;
    }

    public Long getUserId() {
        return user.getId();
    }

    public Long getMerchantId() {
        return user.getMerchant() != null ? user.getMerchant().getId() : null;
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getRole() {
        return user.getRole() != null ? user.getRole().getName() : null;
    }

    public Set<String> getPermissions() {
        if (user.getRole() == null || user.getRole().getPermissions() == null) {
            return Collections.emptySet();
        }
        return user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getActive() && user.getMerchant().getActive();
    }
}
