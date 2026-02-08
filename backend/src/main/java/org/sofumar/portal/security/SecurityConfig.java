package org.sofumar.portal.security;

import org.sofumar.portal.constants.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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

    // Make allowed origins configurable via env or application.properties
    @Value("${app.cors.allowed-origins:http://localhost:8081,http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // integrate with the CorsConfigurationSource bean; global CorsFilter still ensures headers on errors
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // permit preflight globally
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // public endpoints
                        .requestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health").permitAll()
                        // authenticated endpoints
                        .requestMatchers("/auth/profile", "/auth/password-update", "/auth/logout").authenticated()

                        // role based

                        // ADMIN only
                        .requestMatchers("/users/**").hasRole(Role.ADMIN.name())
                        // Write operations - ADMIN only
                        .requestMatchers(HttpMethod.PUT, "/settings/**").hasRole(Role.ADMIN.name())

                        // Write operations - ADMIN and MANAGER only
                        .requestMatchers(HttpMethod.POST, "/members/**", "/payments/**", "/expenses/**").hasAnyRole(Role.ADMIN.name(), Role.MANAGER.name())
                        .requestMatchers(HttpMethod.PUT, "/members/**", "/payments/**", "/expenses/**").hasAnyRole(Role.ADMIN.name(), Role.MANAGER.name())
                        .requestMatchers(HttpMethod.DELETE, "/members/**", "/expenses/**").hasAnyRole(Role.ADMIN.name(), Role.MANAGER.name())

                        // Read operations - all authenticated roles
                        .requestMatchers(HttpMethod.GET, "/members/**", "/payments/**", "/expenses/**", "/dashboard/**", "/references/**", "/settings/**").hasAnyRole(Role.ADMIN.name(), Role.MANAGER.name(), Role.MEMBER.name())
                        .anyRequest().authenticated()
                )
                // ensure JWT filter does not block preflight; JwtAuthFilter should skip OPTIONS
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")))
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(e -> e.authenticationEntryPoint(new JsonAuthEntryPoint()));
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

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();

        // Use explicit origins when allowCredentials is true
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization", "Link")); // expose headers your client needs
        config.setAllowCredentials(true);

        // Cache preflight for 1 hour to reduce preflight traffic
        config.setMaxAge(3600L);

        return config;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}