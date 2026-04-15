package io.carizmi.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

@Configuration
public class BeanConfig {
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return new DefaultBearerTokenResolver();
    }
}