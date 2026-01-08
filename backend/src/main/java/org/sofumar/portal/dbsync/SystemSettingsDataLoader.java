package org.sofumar.portal.dbsync;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.data.vo.SystemSettingsVO;
import org.sofumar.portal.repo.SystemSettingsRepository;
import org.sofumar.portal.repo.jpaspec.SystemSettingsSpecifications;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

/**
 * Synchronizes the database systemsettings table with the definitions in
 * systemsettings-data.json on startup.
 */
@Component
@RequiredArgsConstructor
public class SystemSettingsDataLoader implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsDataLoader.class);

    private final SystemSettingsRepository systemSettingsRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting System Settings Data Synchronization...");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        ClassPathResource resource = new ClassPathResource("systemsettings-data.json");

        if (!resource.exists()) {
            logger.info("systemsettings-data.json not found! Skipping sync.");
            return;
        }

        JsonNode rootNode;
        try (InputStream inputStream = resource.getInputStream()) {
            rootNode = mapper.readTree(inputStream);
        }

        for (Map.Entry<String, JsonNode> field : rootNode.properties()) {
            String settingType = field.getKey();
            JsonNode items = field.getValue();

            if (items.isArray()) {
                for (JsonNode item : items) {
                    syncSetting(settingType, item);
                }
            }
        }
        logger.info("System Settings Data Synchronization Completed.");
    }

    private void syncSetting(String settingType, JsonNode item) {
        String key = item.get("key").asText();
        String value = item.get("value").asText();
        LocalDate effectiveDate = item.has("effectiveDate") && !item.get("effectiveDate").isNull() 
                ? LocalDate.parse(item.get("effectiveDate").asText()) : null;
        boolean active = !item.has("active") || item.get("active").asBoolean();

        Specification<SystemSettingsVO> spec = SystemSettingsSpecifications.isSettingType(settingType)
                .and(SystemSettingsSpecifications.isSettingKey(key));
        SystemSettingsVO existing = systemSettingsRepository.findOne(spec).orElse(null);

        if (existing != null) {
            // Update if changed
            boolean changed = !existing.getSettingValue().equals(value)
                    || (existing.getEffectiveDate() == null && effectiveDate != null)
                    || (existing.getEffectiveDate() != null && !existing.getEffectiveDate().equals(effectiveDate))
                    || existing.isActive() != active;

            if (changed) {
                existing.setSettingValue(value);
                existing.setEffectiveDate(effectiveDate);
                existing.setActive(active);
                systemSettingsRepository.save(existing);
                logger.info("Updated system setting: {} - {}", settingType, key);
            }
        } else {
            // Insert new
            SystemSettingsVO newSetting = new SystemSettingsVO();
            newSetting.setSettingType(settingType);
            newSetting.setSettingKey(key);
            newSetting.setSettingValue(value);
            newSetting.setEffectiveDate(effectiveDate);
            newSetting.setActive(active);
            systemSettingsRepository.save(newSetting);
            logger.info("Created system setting: {} - {}", settingType, key);
        }
    }
}