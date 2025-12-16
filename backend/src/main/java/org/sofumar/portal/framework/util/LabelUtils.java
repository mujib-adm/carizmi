package org.sofumar.portal.framework.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LabelUtils {

    // Regex to split camelCase boundaries
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([a-z])([A-Z])");

    public static String toLabel(String field) {
        if (field == null || field.isBlank()) {
            return "";
        }

        // Step 1: Replace underscores with spaces
        String result = field.replace("_", " ");

        // Step 2: Insert spaces before camelCase boundaries
        Matcher matcher = CAMEL_CASE_PATTERN.matcher(result);
        result = matcher.replaceAll("$1 $2");

        // Step 3: Capitalize each word
        StringBuilder label = new StringBuilder();
        for (String word : result.split(" ")) {
            if (!word.isBlank()) {
                label.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        // Trim trailing space
        return label.toString().trim();
    }

    // Demo
    public static void main(String[] args) {
        System.out.println(toLabel("email"));          // Email
        System.out.println(toLabel("firstName"));      // First Name
        System.out.println(toLabel("userAccountId"));  // User Account Id
        System.out.println(toLabel("postal_code"));    // Postal Code
    }
}