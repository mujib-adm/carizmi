package io.carizmi.shared.util;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reusable utility for 10-digit US phone numbers.
 * Centralizes detection, parsing, extraction, and standard formatting.
 */
public final class PhoneUtils {

    /**
     * Regex matching standard 10-digit US phone numbers with optional country code (+1 or 1)
     * and standard delimiter variations (hyphens, spaces, dots, parentheses).
     */
    public static final String US_PHONE_REGEX = "^(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})$";
    public static final Pattern US_PHONE_PATTERN = Pattern.compile(US_PHONE_REGEX);

    private PhoneUtils() {
        // Utility class — no instantiation
    }

    /**
     * Returns true if the given input matches a valid 10-digit US phone number format.
     */
    public static boolean isUsPhoneNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        return US_PHONE_PATTERN.matcher(input.trim()).matches();
    }

    /**
     * Parses the given input into its area code, prefix, and line components.
     *
     * @param input the raw phone string
     * @return an {@link Optional} containing {@link PhoneParts} if valid, or {@code Optional.empty()}
     */
    public static Optional<PhoneParts> parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = US_PHONE_PATTERN.matcher(input.trim());
        if (matcher.matches()) {
            return Optional.of(new PhoneParts(matcher.group(1), matcher.group(2), matcher.group(3)));
        }
        return Optional.empty();
    }

    /**
     * Extracts only the 10 numeric digits if valid, or returns empty string.
     */
    public static String extractDigits(String input) {
        return parse(input).map(PhoneParts::toDigits).orElse("");
    }

    /**
     * Formats the given input to the standard database representation (XXX-XXX-XXXX).
     */
    public static String format(String input) {
        return parse(input).map(PhoneParts::toHyphenated).orElse(input != null ? input.trim() : "");
    }

    /**
     * Represents the parsed components of a 10-digit US phone number.
     * Provides helper methods to generate candidate exact representations for index-friendly lookups.
     */
    public record PhoneParts(String area, String prefix, String line) {

        public String toDigits() {
            return area + prefix + line;
        }

        public String toHyphenated() {
            return area + "-" + prefix + "-" + line;
        }

        public String toParenthesized() {
            return "(" + area + ") " + prefix + "-" + line;
        }

        public String toParenthesizedHyphen() {
            return "(" + area + ")-" + prefix + "-" + line;
        }

        public String toParenthesizedNoSpace() {
            return "(" + area + ")" + prefix + "-" + line;
        }

        public String toDotSeparated() {
            return area + "." + prefix + "." + line;
        }

        public String toSpaceSeparated() {
            return area + " " + prefix + " " + line;
        }

        /**
         * Returns all standard candidate formats for exact equality comparison,
         * maximizing database index utilization on varchar phone columns.
         */
        public List<String> allFormats() {
            return List.of(
                    toHyphenated(),
                    toDigits(),
                    toParenthesized(),
                    toParenthesizedHyphen(),
                    toParenthesizedNoSpace(),
                    toDotSeparated(),
                    toSpaceSeparated(),
                    "+1-" + toHyphenated(),
                    "+1 " + toHyphenated(),
                    "1-" + toHyphenated()
            );
        }
    }
}