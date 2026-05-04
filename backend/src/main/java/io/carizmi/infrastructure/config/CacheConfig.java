package io.carizmi.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Application-level cache configuration.
 *
 * <p>Provides a simple {@link ConcurrentMapCacheManager} as the primary cache manager,
 * keeping Redis exclusively for JWT token storage. This is appropriate for single-instance
 * deployments with low-volume data.</p>
 *
 * <h2>Scaling Path</h2>
 * <p>If the application transitions to multi-instance deployments, replace this
 * with a Redis-backed cache manager using JSON serialization (e.g., Jackson2)
 * instead of JDK serialization.</p>
 */
@Configuration
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("systemSettings", "referenceData");
    }
}