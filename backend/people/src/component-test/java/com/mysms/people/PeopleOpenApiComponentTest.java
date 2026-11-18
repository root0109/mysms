package com.mysms.people;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.testkit.OpenApiDocuments;
import com.mysms.core.testkit.TestDatabase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The OpenAPI document of the people service, written by springdoc from its contract interfaces and kept as the build
 * artifact {@value #OPEN_API_OUTPUT_LOCATION}, from which the clients' TypeScript SDK is generated.
 */
@SpringBootTest(classes = PeopleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PeopleOpenApiComponentTest {

    /** Where the document is written, relative to the module. */
    private static final String OPEN_API_OUTPUT_LOCATION = "target/openapi/api-docs.yml";
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "people");
        registry.add("mysms.auth.api-docs", () -> true);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @Test
    void writesItsOpenApiDocumentFromTheCode() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs.yaml"))
                .header("Accept", "application/vnd.oai.openapi")
                .header("Authorization", "Bearer " + TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID()))
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        String document = OpenApiDocuments.checked("people", response.body());
        assertThat(document).contains("operationId: people.CreateCertification").contains("x-permission:");

        Path output = Path.of(OPEN_API_OUTPUT_LOCATION);
        Files.createDirectories(output.getParent());
        Files.writeString(output, document, StandardCharsets.UTF_8);
        assertThat(output).as("the OpenAPI document is written").isNotEmptyFile();
    }
}
