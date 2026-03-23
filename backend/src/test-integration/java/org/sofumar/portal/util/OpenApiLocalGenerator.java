package org.sofumar.portal.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.integration.config.TestContainersConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Automates the extraction of the OpenAPI specification locally during the Maven build lifecycle.
 * Utilizes TestContainers to avoid requiring a running local database.
 */
public class OpenApiLocalGenerator {
    private static final Logger log = LoggerFactory.getLogger(OpenApiLocalGenerator.class);

    public static void main(String[] args) {
        log.info("Starting OpenApiLocalGenerator to extract OpenAPI spec...");
        
        SpringApplication app = new SpringApplication(org.sofumar.portal.PortalApplication.class, TestContainersConfig.class);
        app.setAdditionalProfiles("test");
        ConfigurableApplicationContext context = app.run("--server.port=0", "--spring.docker.compose.enabled=false");

        try {
            String port = context.getEnvironment().getProperty("local.server.port");
            String url = "http://localhost:" + port + "/v3/api-docs";

            log.info("Fetching OpenAPI specification from {}", url);
            RestTemplate restTemplate = new RestTemplate();
            String openApiJson = restTemplate.getForObject(url, String.class);

            if (openApiJson == null || openApiJson.isEmpty()) {
                throw new IllegalStateException("Received empty OpenAPI specification");
            }

            ObjectMapper mapper = new ObjectMapper();
            Object jsonObject = mapper.readValue(openApiJson, Object.class);
            String formattedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

            Path outputPath = Paths.get("../frontend/src/api/openapi.json");
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, formattedJson);

            log.info("Successfully generated OpenAPI specification at {}", outputPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to generate OpenAPI specification", e);
            throw new RuntimeException(e);
        } finally {
            log.info("Shutting down OpenApi generator context...");
            SpringApplication.exit(context, () -> 0);
        }
    }
}