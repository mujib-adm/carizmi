package org.sofumar.portal.framework.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionParserUtils {
    private static final Pattern DUPLICATE_ENTRY_PATTERN = Pattern.compile("Duplicate entry '(.+?)' for key '(.+?)'");

    static void main(String[] args) {
        String message = "Duplicate entry 'test@mail.com' for key 'users.UK6dotkott2kjsp8vw4d0m25fb7'";

        Matcher matcher = DUPLICATE_ENTRY_PATTERN.matcher(message);
        if (matcher.find()) {
            String value = matcher.group(1);       // test@mail.com
            String constraint = matcher.group(2);  // users.UK6dotkott2kjsp8vw4d0m25fb7

            System.out.println("Value: " + value);
            System.out.println("Constraint: " + constraint);
        } else {
            System.out.println("Message format not recognized.");
        }
    }

    public static boolean isDuplicateEntry(String message) {
        return message != null && DUPLICATE_ENTRY_PATTERN.matcher(message).matches();
    }

    public static Optional<DuplicateEntryInfo> parseDuplicateEntry(String message) {
        Matcher matcher = DUPLICATE_ENTRY_PATTERN.matcher(message);
        if (matcher.find()) {
            return Optional.of(new DuplicateEntryInfo(matcher.group(1), matcher.group(2)));
        }
        return Optional.empty();
    }

    public record DuplicateEntryInfo(String value, String constraint) {}

}
