package org.sofumar.portal.security;

import lombok.RequiredArgsConstructor;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.core.vo.UserVO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SofumarUserDetailsService implements UserDetailsService {

    private final User user;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserVO userVO = user.findUserForAuthentication(username);
        if (userVO == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return new SofumarUserDetails(userVO);
    }
}