package org.sofumar.portal.integration.test.api

import org.sofumar.portal.data.dto.UserDto
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.integration.base.BaseIntegrationSpecification
import org.sofumar.portal.integration.constants.ApiEndpoints
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class AuthApiITSpec extends BaseIntegrationSpecification {

    def setup() {
        login()
    }

    def "test /auth/register - 200/401"() {
        given:
        UserDto newUser = new UserDto(
                firstName: "New",
                lastName: "User",
                email: "newuser@test.com",
                username: "newuser",
                password: "Security123!"
        )

        when: "unauthenticated registration attempt"
        // Note: restTemplate is stateless, so not passing authHeaders makes it anonymous
        ResponseEntity<GlobalResponse> anonResponse = restTemplate.postForEntity(ApiEndpoints.Auth.REGISTER, newUser, GlobalResponse)

        then: "status is 401 (not logged in)"
        anonResponse.statusCode == HttpStatus.UNAUTHORIZED

        when: "admin attempts to register a new user"
        ResponseEntity<GlobalResponse> successResponse = restTemplate.exchange(
                ApiEndpoints.Auth.REGISTER,
                HttpMethod.POST,
                new HttpEntity<>(newUser, authHeaders),
                GlobalResponse
        )

        then: "status is 200 OK"
        successResponse.statusCode == HttpStatus.OK
    }

    def "test /auth/login - 200/401"() {
        given:
        Map<String, String> invalidLogin = [username: "admin", password: "wrongpassword"]

        when: "logging in with valid credentials"
        ResponseEntity<GlobalResponse> successResponse = restTemplate.postForEntity(ApiEndpoints.Auth.LOGIN, adminDefaultCreds, GlobalResponse)

        then: "status is 200 and cookies are set"
        successResponse.statusCode == HttpStatus.OK
        List<String> cookies = extractCookies(successResponse)
        cookies.any { it.contains("access_token") }
        cookies.any { it.contains("refresh_token") }

        when: "logging in with invalid credentials"
        ResponseEntity<GlobalResponse> failResponse = restTemplate.postForEntity(ApiEndpoints.Auth.LOGIN, invalidLogin, GlobalResponse)

        then: "status is 401"
        failResponse.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "test /auth/refresh - 200"() {
        given:
        // We need the refresh token from the headers
        HttpHeaders refreshHeaders = getAuthenticatedHeaders(rawCookies.findAll { it.contains("refresh_token") })

        when: "refreshing the token"
        ResponseEntity<GlobalResponse> refreshResponse = restTemplate.exchange(ApiEndpoints.Auth.REFRESH, HttpMethod.POST, new HttpEntity<>(refreshHeaders), GlobalResponse)

        then: "status is 200 and new cookies are issued"
        refreshResponse.statusCode == HttpStatus.OK
        List<String> newCookies = extractCookies(refreshResponse)
        newCookies.any { it.contains("access_token") }
        newCookies.any { it.contains("refresh_token") }
    }

    def "test /auth/profile - 200/401"() {
        when: "requesting profile with valid session"
        ResponseEntity<GlobalResponse> profileResponse = restTemplate.exchange(ApiEndpoints.Auth.PROFILE, HttpMethod.GET, new HttpEntity<>(authHeaders), GlobalResponse)

        then: "status is 200 and user info matches"
        profileResponse.statusCode == HttpStatus.OK
        profileResponse.body.responseData.username == adminDefaultCreds.get("username")

        when: "requesting profile without session"
        ResponseEntity<GlobalResponse> anonResponse = restTemplate.getForEntity(ApiEndpoints.Auth.PROFILE, GlobalResponse)

        then: "status is 401"
        anonResponse.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "test /auth/password-update 200 & Post-Update 401"() {
        given:
        PasswordUpdateRequestDto updateReq = new PasswordUpdateRequestDto(
                oldPassword: adminDefaultCreds.get("password"),
                newPassword: "NewSecret123!",
                confirmPassword: "NewSecret123!"
        )

        when: "update password"
        ResponseEntity<GlobalResponse> updateResponse = restTemplate.exchange(ApiEndpoints.Auth.PASSWORD_UPDATE, HttpMethod.POST, new HttpEntity<>(updateReq, authHeaders), GlobalResponse)

        then:
        updateResponse.statusCode == HttpStatus.OK
        extractCookies(updateResponse).any { it.contains("Max-Age=0") }

        when: "attempting to use the OLD session"
        ResponseEntity<GlobalResponse> profileResponse = restTemplate.exchange(ApiEndpoints.Auth.PROFILE, HttpMethod.GET, new HttpEntity<>(authHeaders), GlobalResponse)

        then: "status is 401 because the session was invalidated after password change"
        profileResponse.statusCode == HttpStatus.UNAUTHORIZED

        cleanup: "restore admin password to original value"
        ResponseEntity<GlobalResponse> resetLogin = restTemplate.postForEntity(
                ApiEndpoints.Auth.LOGIN, [username: adminDefaultCreds.get("username"), password: "NewSecret123!"], GlobalResponse)
        if (resetLogin.statusCode == HttpStatus.OK) {
            HttpHeaders resetHeaders = getAuthenticatedHeaders(extractCookies(resetLogin))
            PasswordUpdateRequestDto resetReq = new PasswordUpdateRequestDto(
                    oldPassword: "NewSecret123!",
                    newPassword: adminDefaultCreds.get("password"),
                    confirmPassword: adminDefaultCreds.get("password")
            )
            restTemplate.exchange(ApiEndpoints.Auth.PASSWORD_UPDATE, HttpMethod.POST,
                    new HttpEntity<>(resetReq, resetHeaders), GlobalResponse)
        }
    }

    def "test /auth/logout - Blacklisting Verification"() {
        when: "logging out using setup session"
        ResponseEntity<String> logoutResponse = restTemplate.exchange(ApiEndpoints.Auth.LOGOUT, HttpMethod.POST, new HttpEntity<>(authHeaders), String)

        then: "status is 200 and cookies are cleared"
        logoutResponse.statusCode == HttpStatus.OK
        List<String> clearCookies = extractCookies(logoutResponse)
        clearCookies.any { it.contains("Max-Age=0") }

        when: "attempting to use the blacklisted session after logout"
        ResponseEntity<GlobalResponse> profileResponse = restTemplate.exchange(ApiEndpoints.Auth.PROFILE, HttpMethod.GET, new HttpEntity<>(authHeaders), GlobalResponse)

        then: "status is 401 because token is revoked"
        profileResponse.statusCode == HttpStatus.UNAUTHORIZED
    }
}