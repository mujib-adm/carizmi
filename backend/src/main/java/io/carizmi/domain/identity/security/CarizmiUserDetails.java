package io.carizmi.domain.identity.security;

import lombok.Getter;
import io.carizmi.shared.constants.Role;
import io.carizmi.domain.identity.model.UserVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public class CarizmiUserDetails implements UserDetails {

    private final UserVO userVO;
    private final long lockoutDurationMinutes;

    public CarizmiUserDetails(UserVO userVO, long lockoutDurationMinutes) {
        this.userVO = userVO;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (userVO.getRole() == null) {
            return Collections.emptyList();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + userVO.getRole().name()));
    }

    @Override
    public String getPassword() {
        return userVO.getPassword();
    }

    @Override
    public String getUsername() {
        return userVO.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        if (userVO.getLockoutTime() == null) {
            return true;
        }
        LocalDateTime lockExpiration = userVO.getLockoutTime().plusMinutes(lockoutDurationMinutes);
        return LocalDateTime.now().isAfter(lockExpiration);
    }

    @Override
    public boolean isEnabled() {
        return userVO.isActive() && userVO.getRole() != null && userVO.getRole() != Role.ANONYMOUS;
    }
}