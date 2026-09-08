package com.nivya.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nivya.role.RoleType;
import com.nivya.user.entity.User;
import com.nivya.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Authenticated Spring Security UserDetails representation.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String uuid;
    private final String name;
    private final String email;
    @JsonIgnore
    private final String password;
    private final RoleType role;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String uuid, String name, String email, String password,
                         RoleType role, UserStatus status, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().getAuthority())
        );

        return new UserPrincipal(
                user.getId(),
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                authorities
        );
    }

    public Long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public RoleType getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
