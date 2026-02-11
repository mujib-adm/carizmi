package org.sofumar.portal.security;

import lombok.Getter;
import org.sofumar.portal.constants.Role;
import org.sofumar.portal.core.vo.UserVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public class SofumarUserDetails implements UserDetails {

    private final UserVO userVO;

    public SofumarUserDetails(UserVO userVO) {
        this.userVO = userVO;
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
        // Lock lasts for 15 minutes
        long lockoutDurationMinutes = 15;
        java.time.LocalDateTime lockExpiration = userVO.getLockoutTime().plusMinutes(lockoutDurationMinutes);
        return java.time.LocalDateTime.now().isAfter(lockExpiration);
    }

    @Override
    public boolean isEnabled() {
        return userVO.isActive() && userVO.getRole() != null && userVO.getRole() != Role.ANONYMOUS;
    }
}