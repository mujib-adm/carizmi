package io.carizmi.domain.identity.security

import io.carizmi.domain.identity.service.User
import io.carizmi.domain.identity.model.UserVO
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

class CarizmiUserDetailsServiceSpec extends Specification {

    User user = Mock()
    CarizmiUserDetailsService userDetailsService = new CarizmiUserDetailsService(user)

    void setup() {
        ReflectionTestUtils.setField(userDetailsService, "lockoutDurationMinutes", 15L)
    }

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