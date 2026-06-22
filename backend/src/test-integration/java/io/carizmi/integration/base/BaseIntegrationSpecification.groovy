package io.carizmi.integration.base

import io.carizmi.CarizmiApplication
import io.carizmi.framework.data.response.GlobalResponse
import io.carizmi.integration.config.TestContainersConfig
import io.carizmi.integration.constants.ApiEndpoints
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import spock.lang.Specification

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = [CarizmiApplication, TestContainersConfig]
)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationSpecification extends Specification {

    @Value('${admin.default.username}')
    private String username
    @Value('${admin.default.password}')
    private String password

    protected HttpHeaders authHeaders
    protected List<String> rawCookies
    protected Map<String, String> adminDefaultCreds

    @Autowired
    TestRestTemplate restTemplate

    def setup() {
        adminDefaultCreds = [username: username, password: password] as Map<String, String>
    }

    protected void login(Map creds = adminDefaultCreds) {
        ResponseEntity<GlobalResponse> response = restTemplate.postForEntity(ApiEndpoints.Auth.LOGIN, creds, GlobalResponse)
        if (response.statusCode != HttpStatus.OK) {
            throw new IllegalStateException("Login failed for user ${creds.username}")
        }
        rawCookies = extractCookies(response)
        authHeaders = getAuthenticatedHeaders(rawCookies)
    }

    protected List<String> extractCookies(ResponseEntity<?> response) {
        return response.headers.get("Set-Cookie") ?: []
    }

    protected HttpHeaders getAuthenticatedHeaders(List<String> cookies) {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        if (cookies) {
            String cleanedCookies = cookies.collect { it.split(";")[0] }.join("; ")
            headers.add("Cookie", cleanedCookies)
        }
        return headers
    }
}