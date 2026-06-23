package io.carizmi.integration.tooling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.integration.config.TestContainersConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Automates the extraction of the OpenAPI specification locally during the Maven build lifecycle.
 * Utilizes TestContainers to avoid requiring a running local database.
 */
public class OpenApiLocalGenerator {
    private static final Logger log = LoggerFactory.getLogger(OpenApiLocalGenerator.class);

    public static void main(String[] args) {
        log.info("Starting OpenApiLocalGenerator to extract OpenAPI spec...");
        
        SpringApplication app = new SpringApplication(io.carizmi.CarizmiApplication.class, TestContainersConfig.class);
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

            ObjectMapper mapper = JsonMapper.builder().build();
            Object jsonObject = mapper.readValue(openApiJson, Object.class);

            // Normalize the server URL to avoid contract drift caused by random ephemeral ports.
            // Spring Boot starts on port 0 (random), so the generated URL is different every run.
            if (jsonObject instanceof Map<?, ?> root) {
                Object servers = root.get("servers");
                if (servers instanceof List<?> serverList && !serverList.isEmpty()) {
                    Object firstServer = serverList.getFirst();
                    if (firstServer instanceof Map<?, ?> serverMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mutableServer = (Map<String, Object>) serverMap;
                        mutableServer.put("url", "http://localhost:8080");
                    }
                }
            }

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