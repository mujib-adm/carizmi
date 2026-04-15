package io.carizmi.integration.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "testcontainers")
public record TestcontainersProperties(Mysql mysql, Redis redis) {
    public record Mysql(String image, @JsonProperty("db-name") String dbName, String username, String password) { }
    public record Redis(String image, Integer port) { }
}