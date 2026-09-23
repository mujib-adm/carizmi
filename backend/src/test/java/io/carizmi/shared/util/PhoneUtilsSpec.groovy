package io.carizmi.shared.util

import spock.lang.Specification
import spock.lang.Unroll

class PhoneUtilsSpec extends Specification {

    @Unroll
    def "test - isUsPhoneNumber: Valid formats [phone: #phone]"() {
        expect:
        PhoneUtils.isUsPhoneNumber(phone)

        where:
        phone << [
                "6126550830",
                "612-655-0830",
                "(612) 655-0830",
                "(612)-655-0830",
                "(612)655-0830",
                "612.655.0830",
                "612 655 0830",
                "+1-612-655-0830",
                "+1 612-655-0830",
                "+1 (612) 655-0830",
                "1-612-655-0830",
                "16126550830",
                "+16126550830",
                "  612-655-0830  "
        ]
    }

    @Unroll
    def "test - isUsPhoneNumber: Invalid formats [phone: #phone]"() {
        expect:
        !PhoneUtils.isUsPhoneNumber(phone)

        where:
        phone << [
                null,
                "",
                "   ",
                "123",
                "1004",
                "612-655-083",       // 9 digits
                "612-655-08301",     // 11 digits without valid US country code
                "10000000000000000000",
                "John",
                "John Smith",
                "612-ABC-0830",
                "++1-612-655-0830"
        ]
    }

    def "test - parse: Correctly breaks down valid phone into parts"() {
        when: "Parsing a formatted phone"
        Optional<PhoneUtils.PhoneParts> partsOpt = PhoneUtils.parse("(612) 655-0830")

        then: "Parts are present and match components"
        partsOpt.isPresent()
        PhoneUtils.PhoneParts parts = partsOpt.get()
        parts.area() == "612"
        parts.prefix() == "655"
        parts.line() == "0830"
        parts.toDigits() == "6126550830"
        parts.toHyphenated() == "612-655-0830"
        parts.toParenthesized() == "(612) 655-0830"
        parts.toParenthesizedHyphen() == "(612)-655-0830"
        parts.toParenthesizedNoSpace() == "(612)655-0830"
        parts.toDotSeparated() == "612.655.0830"
        parts.toSpaceSeparated() == "612 655 0830"

        and: "allFormats contains all expected index-friendly representations"
        List<String> formats = parts.allFormats()
        formats.contains("612-655-0830")
        formats.contains("6126550830")
        formats.contains("(612) 655-0830")
        formats.contains("(612)-655-0830")
        formats.contains("(612)655-0830")
        formats.contains("612.655.0830")
        formats.contains("612 655 0830")
        formats.contains("+1-612-655-0830")
        formats.contains("1-612-655-0830")
    }

    def "test - extractDigits: Extracts 10 digits or empty string"() {
        expect:
        PhoneUtils.extractDigits("+1 (612) 655-0830") == "6126550830"
        PhoneUtils.extractDigits("612-655-0830") == "6126550830"
        PhoneUtils.extractDigits("invalid") == ""
        PhoneUtils.extractDigits(null) == ""
    }

    def "test - format: Standardizes valid phone to XXX-XXX-XXXX"() {
        expect:
        PhoneUtils.format("6126550830") == "612-655-0830"
        PhoneUtils.format("(612) 655-0830") == "612-655-0830"
        PhoneUtils.format("+1-612-655-0830") == "612-655-0830"
        PhoneUtils.format("not-a-phone") == "not-a-phone"
        PhoneUtils.format(null) == ""
    }
}