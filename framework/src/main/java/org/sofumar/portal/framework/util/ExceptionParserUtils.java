package org.sofumar.portal.framework.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExceptionParserUtils {
    private static final Pattern DUPLICATE_ENTRY_PATTERN = Pattern.compile("Duplicate entry '(.+?)' for key '(.+?)'");

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