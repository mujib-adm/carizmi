package io.carizmi.tooling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility to generate SystemSettingConstants.java from systemsettings-data.json.
 * Run this main method to regenerate constants when the JSON definition
 * changes.
 */
public class SystemSettingConstantsGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingConstantsGenerator.class);

    private static final String RESOURCE_PATH = "src/main/resources/data/systemsettings-data.json";
    private static final String OUTPUT_PATH = "src/main/java/io/carizmi/constants/SystemSettingConstants.java";
    private static final String PACKAGE_NAME = "io.carizmi.constants";

    public static void main(String[] args) {
        try {
            generate();
        } catch (IOException e) {
            logger.error(e.toString());
        }
    }

    public static void generate() throws IOException {
        File jsonFile = new File(RESOURCE_PATH);
        if (!jsonFile.exists()) {
            jsonFile = new File("backend/" + RESOURCE_PATH);
            if (!jsonFile.exists()) {
                logger.error("Could not find systemsettings-data.json at " + RESOURCE_PATH + " or backend/" + RESOURCE_PATH);
                return;
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonFile);

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(PACKAGE_NAME).append(";\n\n");
        sb.append("/**\n");
        sb.append(" * Auto-generated constants from systemsettings-data.json.\n");
        sb.append(" * DO NOT MODIFY MANUALLY. Run SystemSettingConstantsGenerator to regenerate.\n");
        sb.append(" */\n");
        sb.append("public final class SystemSettingConstants {\n\n");

        for (Map.Entry<String, JsonNode> field : rootNode.properties()) {
            String settingName = field.getKey();
            JsonNode values = field.getValue();

            String className = camelToSnake(settingName).toUpperCase();

            sb.append("    public static final class ").append(className).append(" {\n");
            sb.append("        public static final String TYPE = \"").append(settingName).append("\";\n");

            if (values.isArray()) {
                for (JsonNode item : values) {
                    if (item.has("key")) {
                        String keyValue = item.get("key").asText();
                        sb.append("        public static final String ").append(keyValue).append(" = \"").append(keyValue).append("\";\n");
                    }
                }
            }
            sb.append("    }\n\n");
        }

        sb.append("}\n");

        File outputFile = new File(OUTPUT_PATH);
        if (!outputFile.getParentFile().exists()) {
            outputFile = new File("backend/" + OUTPUT_PATH);
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(sb.toString());
            logger.info("Successfully generated {}", outputFile.getAbsolutePath());
        }
    }

    private static String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2");
    }
}