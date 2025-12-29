package org.sofumar.portal.dbsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.data.vo.ReferenceVO;
import org.sofumar.portal.repo.ReferenceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;

/**
 * Synchronizes the database reference table with the definitions in
 * reference-data.json on startup.
 */
@Component
@RequiredArgsConstructor
public class ReferenceDataLoader implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceDataLoader.class);

    private final ReferenceRepository referenceRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting Reference Data Synchronization...");

        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("reference-data.json");

        if (!resource.exists()) {
            logger.info("reference-data.json not found! Skipping sync.");
            return;
        }

        JsonNode rootNode;
        try (InputStream inputStream = resource.getInputStream()) {
            rootNode = mapper.readTree(inputStream);
        }

        for (Map.Entry<String, JsonNode> field : rootNode.properties()) {
            String referenceName = field.getKey();
            JsonNode items = field.getValue();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    syncReference(referenceName, item);
                }
            }
        }
        logger.info("Reference Data Synchronization Completed.");
    }

    private void syncReference(String referenceName, JsonNode item) {
        String code = item.get("code").asText();
        String display = item.get("display").asText();
        boolean active = !item.has("active") || item.get("active").asBoolean();

        ReferenceVO existing = referenceRepository.findByReferenceNameAndReferenceCode(referenceName, code);

        if (existing != null) {
            // Update if changed
            if (!existing.getReferenceDisplay().equals(display) || existing.isActive() != active) {
                existing.setReferenceDisplay(display);
                existing.setActive(active);
                referenceRepository.save(existing);
                logger.info("Updated reference: {} - {}", referenceName, code);
            }
        } else {
            // Insert new
            ReferenceVO newRef = new ReferenceVO();
            newRef.setReferenceName(referenceName);
            newRef.setReferenceCode(code);
            newRef.setReferenceDisplay(display);
            newRef.setActive(active);
            referenceRepository.save(newRef);
            logger.info("Created reference: {} - {}", referenceName, code);
        }
    }
}