package com.mysms.people;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.people.config.PeopleDatabaseConfig;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The caller's own person record end to end over HTTP against real PostgreSQL: the record is the token's `pid`,
 * only contact fields change, internal remarks and the national id never leave, ETags guard updates, row-level
 * security keeps tenants apart.
 */
@SpringBootTest(classes = PeopleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.name=" + PeopleApplication.CONFIG_NAME)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PeopleApiComponentTest {

    private static final String NATIONAL_ID = "P1234567890";

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PeopleDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private FieldCipher cipher;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, HttpResponse<String> raw) {
        String etag() {
            return raw.headers().firstValue("ETag").orElse(null);
        }
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Reply(response.statusCode(), json, response);
    }

    private static String token(UUID tenant, UUID person, String... permissions) {
        Map<String, Object> claims = new java.util.HashMap<>(Map.of("sub", USER.toString(), "tid", tenant.toString(),
                "aud", "mysms", "perms", List.of(permissions)));
        if (person != null) {
            claims.put("pid", person.toString());
        }
        return TOKENS.custom(claims, Instant.now().plusSeconds(600));
    }

    private void person(UUID tenant, UUID id, String name) {
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> database.tx().write(() -> database.jdbc().sql("""
                INSERT INTO people.person (id, tenant_id, display_name, first_name, remarks, national_id_type,
                                           national_id_enc, primary_email, row_version)
                VALUES (:id, :tenant, :name, :name, 'internal note', 'PASSPORT', :nationalId, 'old@example.org', 1)
                """).param("id", id).param("tenant", tenant).param("name", name)
                .param("nationalId", cipher.encrypt(NATIONAL_ID, tenant, "people.person.national_id_enc")).update()));
    }

    @Test
    @Order(1)
    void readsTheCallersOwnRecordOnly() throws Exception {
        person(TENANT, PERSON, "Asha Rao");
        String me = token(TENANT, PERSON, "people.person.read_mine");
        Reply mine = call("GET", "/v1/people/me", me, null);
        assertThat(mine.status()).isEqualTo(200);
        assertThat(mine.etag()).isEqualTo("\"1\"");
        assertThat(mine.raw().headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(mine.body().path("id").asString()).isEqualTo(PERSON.toString());
        assertThat(mine.body().path("displayName").asString()).isEqualTo("Asha Rao");
        assertThat(mine.body().path("nationalIdType").asString()).isEqualTo("PASSPORT");
        assertThat(mine.body().has("remarks")).isFalse();
        assertThat(mine.body().has("nationalId")).isFalse();
        assertThat(mine.raw().body()).doesNotContain("v1.test.").doesNotContain(NATIONAL_ID)
                .doesNotContain("internal note");
        Reply masked = call("GET", "/v1/people/me?fields=displayName", me, null);
        assertThat(masked.body().has("primaryEmail")).isFalse();

        assertThat(call("GET", "/v1/people/me", token(TENANT, null, "people.person.read_mine"), null).status())
                .as("an account without a person record").isEqualTo(404);
        assertThat(call("GET", "/v1/people/me", token(TENANT, UUID.randomUUID(), "people.person.read_mine"), null)
                .status()).isEqualTo(404);
        assertThat(call("GET", "/v1/people/me", token(OTHER_TENANT, PERSON, "people.person.read_mine"), null).status())
                .as("row-level security: another tenant never sees the row").isEqualTo(404);
        assertThat(call("GET", "/v1/people/me", token(TENANT, PERSON), null).status()).isEqualTo(403);
        assertThat(call("GET", "/v1/people/me", null, null).status()).isEqualTo(401);
    }

    @Test
    @Order(2)
    void updatesOnlyContactFieldsUnderIfMatch() throws Exception {
        String me = token(TENANT, PERSON, "people.person.read_mine", "people.person.update_mine");
        String body = "{\"primaryEmail\":\" asha@example.org \",\"websiteUrl\":\"https://asha.example.org\","
                + "\"aboutMe\":\"Physics teacher\",\"primaryPhone\":\"+91 98765 43210\"}";
        assertThat(call("PATCH", "/v1/people/me", me, body).status()).isEqualTo(428);
        assertThat(call("PATCH", "/v1/people/me", me, body, "If-Match", "\"9\"").status()).isEqualTo(412);
        Reply dryRun = call("PATCH", "/v1/people/me?validateOnly=true", me, body, "If-Match", "\"1\"");
        assertThat(dryRun.status()).isEqualTo(200);
        assertThat(call("GET", "/v1/people/me", me, null).etag()).isEqualTo("\"1\"");
        Reply updated = call("PATCH", "/v1/people/me", me, body, "If-Match", "\"1\"");
        assertThat(updated.status()).as(updated.raw().body()).isEqualTo(200);
        assertThat(updated.etag()).isEqualTo("\"2\"");
        assertThat(updated.body().path("primaryEmail").asString()).isEqualTo("asha@example.org");
        assertThat(updated.body().path("websiteUrl").asString()).isEqualTo("https://asha.example.org");
        assertThat(updated.body().has("remarks")).isFalse();
        // names are not self-editable: an unknown property is ignored, the display name stays
        Reply name = call("PATCH", "/v1/people/me", me, "{\"displayName\":\"Someone Else\"}", "If-Match", "\"2\"");
        assertThat(name.body().path("displayName").asString()).isEqualTo("Asha Rao");
        String etag = name.etag();
        Reply cleared = call("PATCH", "/v1/people/me?updateMask=aboutMe", me, "{}", "If-Match", etag);
        assertThat(cleared.status()).isEqualTo(200);
        assertThat(cleared.body().has("aboutMe")).isFalse();
        assertThat(cleared.body().path("primaryEmail").asString()).isEqualTo("asha@example.org");
        Reply script = call("PATCH", "/v1/people/me", me, "{\"websiteUrl\":\"javascript:alert(1)\"}", "If-Match",
                cleared.etag());
        assertThat(script.status()).isEqualTo(422);
        assertThat(script.body().path("errors").get(0).path("field").asString()).isEqualTo("websiteUrl");
        Reply tooLong = call("PATCH", "/v1/people/me", me, "{\"primaryPhone\":\"" + "1".repeat(21) + "\"}", "If-Match",
                cleared.etag());
        assertThat(tooLong.status()).isIn(400, 422);
        assertThat(call("PATCH", "/v1/people/me", token(TENANT, PERSON, "people.person.read_mine"),
                "{\"aboutMe\":\"x\"}", "If-Match", cleared.etag()).status()).isEqualTo(403);
        List<String> events = CallContextHolder.callAs(
                CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(TENANT, null),
                () -> database.tx().read(() -> database.jdbc().sql("SELECT event_type FROM people.outbox_event ORDER BY id")
                        .query(String.class).list()));
        assertThat(events).contains("people.person.updated", "audit.recorded");
    }

    @Test
    @Order(3)
    void personRegisterMasksSecretsAndOtherOperationsAnswer501() throws Exception {
        // the people register (standard operations) shows a national id only masked; never the plaintext or envelope
        Reply listed = call("GET", "/v1/people/persons?filter=" + URLEncoder.encode("id = \"" + PERSON + "\"",
                StandardCharsets.UTF_8), token(TENANT, PERSON, "people.person.read"), null);
        assertThat(listed.status()).as(listed.raw().body()).isEqualTo(200);
        assertThat(listed.body().path("items").get(0).path("nationalIdMasked").asString()).isEqualTo("••••7890");
        assertThat(listed.raw().body()).doesNotContain(NATIONAL_ID).doesNotContain("v1.test.");
        Reply greetings = call("POST", "/v1/people/persons:sendGreetings", token(TENANT, PERSON,
                "people.person.send_greetings"), "{\"personIds\":[\"" + PERSON + "\"],\"templateId\":\"" + PERSON
                + "\",\"channel\":\"sms\"}");
        assertThat(greetings.status()).isEqualTo(501);
    }
}
