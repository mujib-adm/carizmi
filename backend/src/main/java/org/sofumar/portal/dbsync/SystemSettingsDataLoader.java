package org.sofumar.portal.dbsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.core.businesslogic.SystemSetting;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads system settings from a JSON file on startup.
 */
@Component
@RequiredArgsConstructor
public class SystemSettingsDataLoader implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsDataLoader.class);
    private static final String SETTINGS_JSON_FILE = "data/systemsettings-data.json";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private final SystemSetting systemSetting;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting System Settings Data Synchronization...");

        ClassPathResource resource = new ClassPathResource(SETTINGS_JSON_FILE);

        if (!resource.exists()) {
            logger.warn("{} not found in classpath. Skipping sync.", SETTINGS_JSON_FILE);
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode rootNode = objectMapper.readTree(inputStream);
            
        for (Map.Entry<String, JsonNode> entry : rootNode.properties()) {
            String settingType = entry.getKey();
            JsonNode items = entry.getValue();

                if (items.isArray()) {
                for (JsonNode item : items) {
                    sync(settingType, item);
                    }
                }
            }
            logger.info("System Settings Data Synchronization Completed.");
        } catch (Exception e) {
            logger.error("Failed to load system settings", e);
        }
    }

    private void sync(String settingType, JsonNode item) {
        SystemSettingsVO newSettingVO = new SystemSettingsVO();
        newSettingVO.setSettingType(settingType);
        if (item.has(KEY)) newSettingVO.setSettingKey(item.get(KEY).asText());
        if (item.has(VALUE)) newSettingVO.setSettingValue(item.get(VALUE).asText());
        if (item.has(FieldConstants.ACTIVE)) newSettingVO.setActive(item.get(FieldConstants.ACTIVE).asBoolean(true));
        
        if (item.has(FieldConstants.EFFECTIVE_DATE) && !item.get(FieldConstants.EFFECTIVE_DATE).isNull()) {
            try {
                newSettingVO.setEffectiveDate(LocalDate.parse(item.get(FieldConstants.EFFECTIVE_DATE).asText()));
            } catch (Exception e) {
                logger.warn("Invalid date format for setting {}: {}", newSettingVO.getSettingKey(), item.get(FieldConstants.EFFECTIVE_DATE).asText());
            }
        }
        
        sync(newSettingVO);
    }

    private void sync(SystemSettingsVO newSettingVO) {
        String type = newSettingVO.getSettingType();
        String key = newSettingVO.getSettingKey();
        
        if (type == null || key == null) {
            logger.warn("Skipping invalid setting: [{}] {}", type, key);
            return;
        }

        Optional<SystemSettingsVO> existingSettingOpt = systemSetting.findByTypeAndKey(type, key);

        if (existingSettingOpt.isPresent()) {
            SystemSettingsVO existingSettingVO = existingSettingOpt.get();
            boolean changed = false;
            
            // Update value if changed
            if (StringUtils.isNotBlank(newSettingVO.getSettingValue()) && !newSettingVO.getSettingValue().equals(existingSettingVO.getSettingValue())) {
                existingSettingVO.setSettingValue(newSettingVO.getSettingValue());
                changed = true;
            }
            
            // Update effective date if changed
            if (newSettingVO.getEffectiveDate() != null && !newSettingVO.getEffectiveDate().equals(existingSettingVO.getEffectiveDate())) {
                existingSettingVO.setEffectiveDate(newSettingVO.getEffectiveDate());
                changed = true;
            }
            
            // Update active status if changed
            if (newSettingVO.isActive() != existingSettingVO.isActive()) {
                existingSettingVO.setActive(newSettingVO.isActive());
                changed = true;
            }

            if (changed) {
                logger.info("Updating existing setting: [{}] {}", type, key);
                systemSetting.update(existingSettingVO);
            }
        } else {
            logger.info("Creating new setting: [{}] {}", type, key);
            systemSetting.add(newSettingVO);
        }
    }
}