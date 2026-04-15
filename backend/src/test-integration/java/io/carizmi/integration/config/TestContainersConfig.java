package io.carizmi.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@EnableConfigurationProperties(TestcontainersProperties.class)
public class TestContainersConfig {

    @Bean
    @ServiceConnection
    @SuppressWarnings({"resource", "rawtypes"})
    MySQLContainer mysqlContainer(TestcontainersProperties props) {
        return new MySQLContainer<>(props.mysql().image())
                .withDatabaseName(props.mysql().dbName())
                .withUsername(props.mysql().username())
                .withPassword(props.mysql().password())
                .withReuse(true);
    }

    @Bean
    @ServiceConnection(name = "redis")
    @SuppressWarnings({"resource", "rawtypes"})
    GenericContainer redisContainer(TestcontainersProperties props) {
        return new GenericContainer<>(DockerImageName.parse(props.redis().image()))
                .withExposedPorts(props.redis().port())
                .withReuse(true);
    }
}