package io.carizmi.domain.platform.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.domain.platform.service.Reference;
import io.carizmi.domain.platform.model.ReferenceVO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Loads reference data from a JSON file on startup.
 */
@Component
@RequiredArgsConstructor
public class ReferenceDataLoader implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceDataLoader.class);
    private static final String REFERENCE_JSON_FILE = "data/reference-data.json";
    private static final String CODE = "code";
    private static final String DISPLAY = "display";

    private final Reference reference;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting Reference Data Synchronization...");

        ClassPathResource resource = new ClassPathResource(REFERENCE_JSON_FILE);

        if (!resource.exists()) {
            logger.warn("{} not found in classpath. Skipping sync.", REFERENCE_JSON_FILE);
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode rootNode = objectMapper.readTree(inputStream);

        for (Map.Entry<String, JsonNode> entry : rootNode.properties()) {
            String referenceName = entry.getKey();
            JsonNode items = entry.getValue();

                if (items.isArray()) {
                for (JsonNode item : items) {
                    sync(referenceName, item);
                    }
                }
            }
            logger.info("Reference Data Synchronization Completed.");
        } catch (Exception e) {
            logger.error("Failed to load reference data", e);
        }
    }

    private void sync(String referenceName, JsonNode item) {
        ReferenceVO newReferenceVO = new ReferenceVO();
        newReferenceVO.setReferenceName(referenceName);
        if (item.has(CODE)) newReferenceVO.setReferenceCode(item.get(CODE).asText());
        if (item.has(DISPLAY)) newReferenceVO.setReferenceDisplay(item.get(DISPLAY).asText());
        if (item.has(FieldConstants.ACTIVE)) newReferenceVO.setActive(item.get(FieldConstants.ACTIVE).asBoolean(true));
        
        sync(newReferenceVO);
    }

    private void sync(ReferenceVO newReferenceVO) {
        String name = newReferenceVO.getReferenceName();
        String code = newReferenceVO.getReferenceCode();
        
        if (name == null || code == null) {
            logger.warn("Skipping invalid reference: [{}] {}", name, code);
            return;
        }

        Optional<ReferenceVO> existingReferenceVOOpt = reference.findByNameAndCode(name, code);

        if (existingReferenceVOOpt.isPresent()) {
            ReferenceVO existingReferenceVO = existingReferenceVOOpt.get();
            boolean changed = false;

            // Update display if it has changed
            if (StringUtils.isNotBlank(newReferenceVO.getReferenceDisplay()) && !newReferenceVO.getReferenceDisplay().equals(existingReferenceVO.getReferenceDisplay())) {
                existingReferenceVO.setReferenceDisplay(newReferenceVO.getReferenceDisplay());
                changed = true;
            }

            // Update active status if it has changed
            if (newReferenceVO.isActive() != existingReferenceVO.isActive()) {
                existingReferenceVO.setActive(newReferenceVO.isActive());
                changed = true;
            }

            if (changed) {
                logger.info("Updating existing reference: [{}] {}", name, code);
                reference.update(existingReferenceVO);
            }
        } else {
            logger.info("Creating new reference: [{}] {}", name, code);
            reference.add(newReferenceVO);
        }
    }
}