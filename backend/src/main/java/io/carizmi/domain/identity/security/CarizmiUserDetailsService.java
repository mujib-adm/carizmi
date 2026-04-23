package io.carizmi.domain.identity.security;

import lombok.RequiredArgsConstructor;
import io.carizmi.domain.identity.service.User;
import io.carizmi.domain.identity.model.UserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarizmiUserDetailsService implements UserDetailsService {

    private final User user;

    @Value("${app.security.lockout-duration-minutes:15}")
    private long lockoutDurationMinutes;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserVO userVO = user.findUserForAuthentication(username);
        if (userVO == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new CarizmiUserDetails(userVO, lockoutDurationMinutes);
    }
}