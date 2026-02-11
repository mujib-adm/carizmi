package org.sofumar.portal.security

import org.sofumar.portal.constants.Role
import org.sofumar.portal.core.vo.UserVO
import org.springframework.security.core.GrantedAuthority
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class SofumarUserDetailsSpec extends Specification {

    @Subject
    SofumarUserDetails userDetails

    def "test - getAuthorities: Should return prefixed role when role exists"() {
        given: "A UserVO with a role"
        Role role = Role.ADMIN
        UserVO userVO = new UserVO(role: role)
        userDetails = new SofumarUserDetails(userVO)

        when: "Authorities are retrieved"
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities()

        then: "No mock interactions occurred"
        0 * _

        and: "The authority is prefixed with ROLE_"
        authorities.size() == 1
        authorities.iterator().next().authority == "ROLE_ADMIN"
    }

    def "test - getAuthorities: Should return empty list when role is null"() {
        given: "A UserVO without a role"
        UserVO userVO = new UserVO(role: null)
        userDetails = new SofumarUserDetails(userVO)

        when: "Authorities are retrieved"
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities()

        then: "No mock interactions occurred"
        0 * _

        and: "The authorities list is empty"
        authorities.isEmpty()
    }

    def "test - getPassword: Should return password from VO"() {
        given: "A UserVO with a password"
        String password = "encodedPassword"
        UserVO userVO = new UserVO(password: password)
        userDetails = new SofumarUserDetails(userVO)

        when: "Password is retrieved"
        String result = userDetails.getPassword()

        then: "No mock interactions occurred"
        0 * _

        and: "The result matches the VO password"
        result == password
    }

    def "test - getUsername: Should return username from VO"() {
        given: "A UserVO with a username"
        String username = "testuser"
        UserVO userVO = new UserVO(username: username)
        userDetails = new SofumarUserDetails(userVO)

        when: "Username is retrieved"
        String result = userDetails.getUsername()

        then: "No mock interactions occurred"
        0 * _

        and: "The result matches the VO username"
        result == username
    }

    @Unroll
    def "test - isAccountNonLocked: Should return #expected for lockoutTime #lockoutTimeDescription"() {
        given: "A UserVO with a specific lockoutTime"
        UserVO userVO = new UserVO(lockoutTime: lockoutTime)
        userDetails = new SofumarUserDetails(userVO)

        when: "Checking if account is non-locked"
        boolean result = userDetails.isAccountNonLocked()

        then: "No mock interactions occurred"
        0 * _

        and: "The result matches expectation"
        result == expected

        where:
        lockoutTimeDescription  | lockoutTime                          | expected
        "null (never locked)"   | null                                 | true
        "active (10 mins ago)"  | LocalDateTime.now().minusMinutes(10) | false
        "expired (16 mins ago)" | LocalDateTime.now().minusMinutes(16) | true
    }

    @Unroll
    def "test - isEnabled: Should return #expected for active=#isActive and role=#role"() {
        given: "A UserVO with state and role"
        UserVO userVO = new UserVO(active: isActive, role: role)
        userDetails = new SofumarUserDetails(userVO)

        when: "Checking if account is enabled"
        boolean result = userDetails.isEnabled()

        then: "No mock interactions occurred"
        0 * _

        and: "The result matches expectation"
        result == expected

        where:
        isActive | role           | expected
        true     | Role.ADMIN     | true
        true     | Role.MANAGER   | true
        true     | Role.MEMBER    | true
        false    | Role.ADMIN     | false
        true     | Role.ANONYMOUS | false
        true     | null           | false
        false    | null           | false
    }

    def "test - getAccountNonExpired and isCredentialsNonExpired: Should always return true"() {
        given: "Any userDetails"
        UserVO userVO = new UserVO()
        userDetails = new SofumarUserDetails(userVO)

        when: "Boolean flags retrieved"
        boolean nonExpired = userDetails.isAccountNonExpired()
        boolean credNonExpired = userDetails.isCredentialsNonExpired()

        then: "No interactions"
        0 * _

        and: "Defaults are true"
        nonExpired
        credNonExpired
    }
}