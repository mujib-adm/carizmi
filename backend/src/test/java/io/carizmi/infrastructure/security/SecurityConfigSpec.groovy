package io.carizmi.infrastructure.security

import jakarta.servlet.Filter
import io.carizmi.domain.identity.security.handler.RestAuthenticationFailureHandler
import io.carizmi.domain.identity.security.handler.RestAuthenticationSuccessHandler
import io.carizmi.domain.identity.security.filters.JsonAuthenticationFilter
import io.carizmi.infrastructure.filter.JwtAuthenticationFilter
import io.carizmi.infrastructure.filter.RequestsRateLimitFilter
import io.carizmi.infrastructure.security.RestAccessDeniedHandler
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import spock.lang.Specification

class SecurityConfigSpec extends Specification {

    SecurityConfig securityConfig = new SecurityConfig()

    def "test - filterChain: should configure http security with perimeter requestMatchers and build successfully"() {
        given: "Mocked HttpSecurity and dependencies"
        HttpSecurity http = Mock(HttpSecurity)
        JwtAuthenticationFilter jwtFilter = Mock()
        RequestsRateLimitFilter rateLimitFilter = Mock()
        JsonAuthenticationFilter jsonFilter = Mock()
        RestAccessDeniedHandler accessDeniedHandler = Mock()
        RequestMatcher requestMatcher = Mock(RequestMatcher)
        DefaultSecurityFilterChain expectedChain = new DefaultSecurityFilterChain(requestMatcher, [])
        AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry registry = Mock()
        ExceptionHandlingConfigurer exceptionConfig = Mock()
        
        // Create distinct AuthorizedUrl mocks to verify correct associations
        AuthorizeHttpRequestsConfigurer.AuthorizedUrl urlPermitAll = Mock()
        AuthorizeHttpRequestsConfigurer.AuthorizedUrl urlAnyRequest = Mock()

        // Stub methods to return registry for chaining
        urlPermitAll.permitAll() >> registry
        urlAnyRequest.authenticated() >> registry

        when: "The target method executed"
        SecurityFilterChain result = securityConfig.filterChain(http, jwtFilter, rateLimitFilter, jsonFilter, accessDeniedHandler)

        then: "The request matchers are correctly configured for perimeter security"
        // Verify configuration methods and return values to support chaining
        1 * http.csrf(_) >> http
        1 * http.cors(_) >> http
        1 * http.sessionManagement(_) >> http
        // Capture lambda and execute it
        1 * http.authorizeHttpRequests(_) >> { args ->
            Customizer<AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry> lambda =
                    args[0] as Customizer<AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry>
            lambda.customize(registry)
            return http
        }
        3 * http.addFilterBefore(_ as Filter, _ as Class<? extends Filter>) >> http
        1 * http.headers(_ as Customizer<HeadersConfigurer<HttpSecurity>>) >> http
        1 * exceptionConfig.authenticationEntryPoint(_ as JsonAuthenticationEntryPoint) >> exceptionConfig
        1 * exceptionConfig.accessDeniedHandler(accessDeniedHandler)
        // 1. Preflight -> permitAll
        1 * registry.requestMatchers(HttpMethod.OPTIONS, "/**") >> urlPermitAll
        // 2. Public endpoints -> permitAll
        1 * registry.requestMatchers(
                "/auth/login",
                "/auth/refresh",
                "/v3/api-docs/**",
                "/swagger-ui/index.html",
                "/actuator/health"
        ) >> urlPermitAll

        2 * urlPermitAll.permitAll() >> registry
        // 3. Catch-all: Any other request must be authenticated
        1 * registry.anyRequest() >> urlAnyRequest
        1 * urlAnyRequest.authenticated() >> registry

        1 * http.exceptionHandling(_) >> { args ->
            Customizer<ExceptionHandlingConfigurer<HttpSecurity>> customizer = args[0] as Customizer<ExceptionHandlingConfigurer<HttpSecurity>>
            customizer.customize(exceptionConfig)
            return http
        }
        1 * http.build() >> expectedChain
        0 * _

        and: "The method completes without exceptions"
        noExceptionThrown()
        result == expectedChain
    }

    def "test - authenticationManager: bean should delegate to AuthenticationConfiguration"() {

        given: "AuthenticationConfiguration mock"
        AuthenticationConfiguration authenticationConfiguration = Mock(AuthenticationConfiguration)
        AuthenticationManager expectedManager = Mock(AuthenticationManager)

        when: "The target method executed"
        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration)

        then: "The expected calls are made"
        1 * authenticationConfiguration.getAuthenticationManager() >> expectedManager
        0 * _

        and: "The expected result"
        result == expectedManager
        result instanceof AuthenticationManager
        noExceptionThrown()
    }

    def "test - jsonAuthenticationFilter: bean should configure filter correctly"() {

        given: "Dependencies for JsonAuthenticationFilter"
        AuthenticationManager authenticationManager = Mock(AuthenticationManager)
        RestAuthenticationSuccessHandler successHandler = Mock(RestAuthenticationSuccessHandler)
        RestAuthenticationFailureHandler failureHandler = Mock(RestAuthenticationFailureHandler)

        when: "The target method executed"
        JsonAuthenticationFilter result =
                securityConfig.jsonAuthenticationFilter(authenticationManager, successHandler, failureHandler)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        // Verify internal state using reflection since getters might be missing or protected
        ReflectionTestUtils.getField(result, "authenticationManager") == authenticationManager
        ReflectionTestUtils.getField(result, "successHandler") == successHandler
        ReflectionTestUtils.getField(result, "failureHandler") == failureHandler
        noExceptionThrown()
    }

    def "test - passwordEncoder: bean should return BCryptPasswordEncoder"() {

        given: "No dependencies required"

        when: "The target method executed"
        PasswordEncoder result = securityConfig.passwordEncoder()

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        result != null
        result instanceof BCryptPasswordEncoder
        result.matches("password", result.encode("password"))
        noExceptionThrown()
    }

    def "test - corsConfigurationSource: should build proper configuration from allowedOrigins"() {

        given: "Custom allowed origins configured"
        String origin1 = "http://test1.com"
        String origin2 = "http://test2.com"
        String[] origins = [origin1, origin2] as String[]
        securityConfig.@allowedOrigins = origins

        // Need a request to resolve the configuration
        // UrlBasedCorsConfigurationSource matches path
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any/path")

        when: "The target method executed"
        CorsConfigurationSource source = securityConfig.corsConfigurationSource()
        CorsConfiguration config = source.getCorsConfiguration(request)

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        config != null
        config.getAllowedOrigins().containsAll([origin1, origin2])
        config.getAllowedMethods().containsAll(["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"])
        config.getAllowedHeaders().containsAll(["Authorization", "Content-Type", "Accept", "X-Requested-With"])
        config.getExposedHeaders().containsAll(["Authorization", "Link"])
        config.getAllowCredentials() == true
        config.getMaxAge() == 3600L
        noExceptionThrown()
    }

    def "test - corsFilter: should register configuration for all paths"() {

        given: "Custom allowed origin"
        String origin = "http://cors.com"
        String[] origins = [origin] as String[]
        securityConfig.@allowedOrigins = origins

        when: "The target method executed"
        CorsFilter filter = securityConfig.corsFilter()

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        filter != null
        filter.getClass() == CorsFilter
        noExceptionThrown()
    }

    def "test - logCorsConfiguration: should handle empty or null origins without throwing exception"() {
        given: "Empty or null origins"
        securityConfig.@allowedOrigins = origins as String[]

        when: "The target method executed"
        securityConfig.logCorsConfiguration()

        then: "No exceptions"
        0 * _

        and:
        noExceptionThrown()

        where:
        origins << [null, []]
    }

    def "test - logCorsConfiguration: should handle valid origins without throwing exception"() {
        given: "Valid origins"
        securityConfig.@allowedOrigins = ["http://localhost:3000"] as String[]

        when: "The target method executed"
        securityConfig.logCorsConfiguration()

        then: "No exceptions"
        0 * _

        and:
        noExceptionThrown()
    }
}