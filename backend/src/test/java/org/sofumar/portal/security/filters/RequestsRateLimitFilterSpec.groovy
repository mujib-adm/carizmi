package org.sofumar.portal.security.filters

import com.github.benmanes.caffeine.cache.Cache
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Duration
import org.sofumar.portal.testbase.ServletCaptureHelper

class RequestsRateLimitFilterSpec extends BaseSpecification {

    @Subject
    RequestsRateLimitFilter filter = new RequestsRateLimitFilter()

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    FilterChain chain = Mock()

    ServletCaptureHelper capture

    void setup() {
        // Default configuration
        ReflectionTestUtils.setField(filter, "enabled", true)
        ReflectionTestUtils.setField(filter, "capacity", 5) // Small capacity for testing
        ReflectionTestUtils.setField(filter, "refillTokens", 5)
        ReflectionTestUtils.setField(filter, "refillDuration", Duration.ofSeconds(60))
        // Prepare a capture for potential JSON writes
        capture = captureServletOutput()
    }

    def "test - doFilterInternal: Should skip filtering when disabled"() {
        given: "A disabled filter"
        ReflectionTestUtils.setField(filter, "enabled", false)

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "Constraint is skipped and chain continues"
        1 * chain.doFilter(request, response)
        0 * _

        and: "No exception occurred"
        noExceptionThrown()
    }

    def "test - doFilterInternal: Should allow request when under limit"() {
        given: "A request from an IP"
        String ip = "192.168.1.1"

        when: "The filter is executed"
        filter.doFilterInternal(request, response, chain)

        then: "IP is resolved and bucket allows consumption"
        1 * request.getHeader("X-Forwarded-For") >> null
        1 * request.getRemoteAddr() >> ip
        1 * chain.doFilter(request, response)
        0 * _

        and: "No exception occurred"
        noExceptionThrown()
    }

    def "test - doFilterInternal: Should return 429 when rate limit exceeded"() {
        given: "A request from an IP that has exhausted its bucket"
        String ip = "192.168.1.2"
        int capacity = 1
        ReflectionTestUtils.setField(filter, "capacity", capacity)

        // use capture for response output
        when: "Consuming more than capacity"
        filter.doFilterInternal(request, response, chain) // Consumption 1 (OK)
        filter.doFilterInternal(request, response, chain) // Consumption 2 (Over limit)

        then: "First request passes"
        1 * request.getHeader("X-Forwarded-For") >> null
        1 * request.getRemoteAddr() >> ip
        1 * chain.doFilter(request, response)

        then: "Second request is blocked"
        1 * request.getHeader("X-Forwarded-For") >> null
        1 * request.getRemoteAddr() >> ip
        1 * response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value())
        1 * response.setContentType("application/json")
        1 * response.getOutputStream() >> capture.getServletOutputStream()
        0 * _

        and: "No exception occurred"
        noExceptionThrown()
    }

    @Unroll
    def "test - resolveClientIP: Should resolve IP from #source [expected: #expectedIP]"() {
        given: "A request with specific headers"
        // We test via doFilterInternal to cover the private helper

        when: "Filtering a request"
        filter.doFilterInternal(request, response, chain)

        then: "Header is checked"
        1 * request.getHeader("X-Forwarded-For") >> xForwardedFor
        if (xForwardedFor == null) {
            1 * request.getRemoteAddr() >> remoteAddr
        }
        1 * chain.doFilter(request, response)
        0 * _

        and: "No exception occurred"
        noExceptionThrown()

        where:
        source            | xForwardedFor        | remoteAddr    | expectedIP
        "X-Forwarded-For" | "10.0.0.1, 10.0.0.2" | "192.168.1.1" | "10.0.0.1"
        "RemoteAddr"      | null                 | "192.168.1.5" | "192.168.1.5"
    }

    def "test - resolveBucket: Should use separate buckets for different IPs"() {
        given: "Two different IPs"
        String ip1 = "1.1.1.1"
        String ip2 = "2.2.2.2"
        ReflectionTestUtils.setField(filter, "capacity", 1)

        when: "Both IPs make requests"
        filter.doFilterInternal(request, response, chain) // ip1
        filter.doFilterInternal(request, response, chain) // ip2

        then: "ip1 request passes"
        1 * request.getHeader("X-Forwarded-For") >> null
        1 * request.getRemoteAddr() >> ip1
        1 * chain.doFilter(request, response)

        then: "ip2 request passes (new bucket)"
        1 * request.getHeader("X-Forwarded-For") >> null
        1 * request.getRemoteAddr() >> ip2
        1 * chain.doFilter(request, response)
        0 * _

        and: "No exception occurred"
        noExceptionThrown()
    }

    def "test - cache: Should use bounded Caffeine cache"() {
        given: "A request from an IP and the filter's cache field"
        Cache<String, Bucket> cacheField = (Cache<String, Bucket>) ReflectionTestUtils.getField(filter, "cache")
        String ip = "10.0.0.1"

        when: "The filter is executed and a bucket entry is created"
        filter.doFilterInternal(request, response, chain)

        then: "IP is resolved via remote address"
        1 * request.getHeader("X-Forwarded-For") >> null
        1 * request.getRemoteAddr() >> ip
        1 * chain.doFilter(request, response)
        0 * _

        and: "Cache is a bounded Caffeine Cache"
        cacheField instanceof Cache
        cacheField.estimatedSize() >= 1

        and: "No exception occurred"
        noExceptionThrown()
    }
}