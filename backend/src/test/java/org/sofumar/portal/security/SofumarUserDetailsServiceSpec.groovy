package org.sofumar.portal.security

import org.sofumar.portal.core.businesslogic.User
import org.sofumar.portal.core.vo.UserVO
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import spock.lang.Specification

class SofumarUserDetailsServiceSpec extends Specification {

    User user = Mock()
    SofumarUserDetailsService userDetailsService = new SofumarUserDetailsService(user)

    def "loadUserByUsername - should return UserDetails when user found"() {
        given:
        String username = "testuser"
        String password = "encodedPassword"
        UserVO userVO = new UserVO(username: username, password: password)

        when:
        UserDetails userDetails = userDetailsService.loadUserByUsername(username)

        then:
        1 * user.findUserForAuthentication(username) >> userVO
        0 * _

        and:
        userDetails != null
        userDetails.username == username
        userDetails.password == password
    }

    def "loadUserByUsername - should throw UsernameNotFoundException when user not found"() {
        given:
        String username = "nonexistent"

        when:
        userDetailsService.loadUserByUsername(username)

        then:
        1 * user.findUserForAuthentication(username) >> null
        0 * _

        and:
        thrown(UsernameNotFoundException)
    }
}