package org.sofumar.portal.security.config;

import jakarta.annotation.PostConstruct;
import org.sofumar.portal.security.handler.RestAuthenticationFailureHandler;
import org.sofumar.portal.security.handler.RestAuthenticationSuccessHandler;
import org.sofumar.portal.security.filters.JsonAuthenticationFilter;
import org.sofumar.portal.security.filters.JwtAuthenticationFilter;
import org.sofumar.portal.security.filters.RequestsRateLimitFilter;
import org.sofumar.portal.security.handler.RestAccessDeniedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.cors.allowed-origins:}")
    private String[] allowedOrigins;

    @PostConstruct
    public void logCorsConfiguration() {
        if (allowedOrigins == null || allowedOrigins.length == 0) {
            log.warn("CORS: No allowed origins configured. Frontend access may be blocked.");
        } else {
            log.info("CORS: Allowed origins initialized: {}", Arrays.toString(allowedOrigins));
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           RequestsRateLimitFilter requestsRateLimitFilter,
                                           JsonAuthenticationFilter jsonAuthenticationFilter,
                                           RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // integrate with the CorsConfigurationSource bean; global CorsFilter still ensures headers on errors
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // permit preflight globally
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // public endpoints
                        .requestMatchers("/auth/login", "/auth/refresh", "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
                        // catch-all: any other request must be authenticated.
                        // Fine-grained role checks are handled at the Method (Controller) level.
                        .anyRequest().authenticated()
                )
                // ensure JWT filter does not block preflight; JwtAuthenticationFilter should skip OPTIONS
                .addFilterBefore(requestsRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:"
                )))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    /**
     * Global CorsFilter with highest precedence.
     * Ensures CORS headers are applied before any security or error handling.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildCorsConfiguration());
        return new CorsFilter(source);
    }

    /**
     * CorsConfigurationSource kept for Spring Security .cors(...) integration.
     * This is useful if other parts of Spring expect a CorsConfigurationSource bean.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildCorsConfiguration());
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public JsonAuthenticationFilter jsonAuthenticationFilter(AuthenticationManager authenticationManager,
                                                             RestAuthenticationSuccessHandler successHandler,
                                                             RestAuthenticationFailureHandler failureHandler) {
        JsonAuthenticationFilter filter = new JsonAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setAuthenticationFailureHandler(failureHandler);
        filter.setFilterProcessesUrl("/auth/login");
        return filter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();

        // Use explicit origins when allowCredentials is true
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization", "Link"));
        config.setAllowCredentials(true);

        // Cache preflight for 1 hour to reduce preflight traffic
        config.setMaxAge(3600L);

        return config;
    }
}