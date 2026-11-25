package com.mysms.people;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.privacyclient.ActivityCode;
import com.mysms.privacyclient.testing.TestConsents;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A public profile rests on its person's consent (design PRIVACY-AND-CONSENT-CENTRE §8, activity
 * people.public_profile), end to end over HTTP against real PostgreSQL with privacy's answers from
 * {@link TestConsents}: no profile is created without the consent; another user sees it only with the consent, its own
 * person always; while privacy cannot answer nothing is created or shown.
 */
@SpringBootTest(classes = PeopleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicProfileConsentComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final ActivityCode PUBLIC_PROFILE = new ActivityCode("people.public_profile");
    private static final String[] PROFILES = {"people.public_profile.create", "people.public_profile.read",
        "people.public_profile.update"};

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PeopleDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body) {
    }

    private Reply send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if ("PATCH".equals(method)) {
            // the profiles written directly are at row version 1
            request.header("If-Match", "\"1\"");
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isEmpty() ? JSON.createObjectNode()
                : JSON.readTree(response.body()));
    }

    /** A user of the tenant holding the permissions tenant-wide; with a person when {@code person} is given. */
    private static String user(UUID tenant, UUID person) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("amr", List.of("pwd"));
        claims.put("perms", List.of(PROFILES));
        claims.put("tperms", List.of(PROFILES));
        claims.put("ins", List.of("*"));
        if (person != null) {
            claims.put("pid", person.toString());
        }
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    /** A user holding the permissions through a role scoped to institutions, not tenant-wide. */
    private static String scoped(UUID tenant) {
        return TOKENS.tenantUser(tenant, UUID.randomUUID(), PROFILES);
    }

    /** A person of the tenant, and (when {@code profile} is given) a public profile of theirs written directly. */
    private UUID person(UUID tenant, UUID profile) {
        UUID person = UUID.randomUUID();
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> {
                    database.jdbc().sql("INSERT INTO people.person (id, tenant_id, display_name, row_version)"
                            + " VALUES (:id, :tenant, 'Asha Rao', 1)").param("id", person).param("tenant", tenant)
                            .update();
                    if (profile != null) {
                        database.jdbc().sql("INSERT INTO people.public_profile (id, tenant_id, person_id, headline,"
                                + " row_version) VALUES (:id, :tenant, :person, 'Alumna', 1)").param("id", profile)
                                .param("tenant", tenant).param("person", person).update();
                    }
                    return null;
                }));
        return person;
    }

    private static String profiles(UUID person) {
        return "/v1/people/persons/" + person + "/public-profile";
    }

    @Test
    void aProfileIsCreatedOnlyWithThePersonsConsent() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID refusing = person(tenant, null);
        UUID consenting = person(tenant, null);
        TestConsents.allow(PUBLIC_PROFILE, consenting);
        String admin = user(tenant, null);

        Reply refused = send("POST", profiles(refusing), admin, "{\"headline\":\"Alumna\"}");
        assertThat(refused.status()).as(refused.body().toString()).isEqualTo(422);
        assertThat(refused.body().path("errors").get(0).path("field").asString()).isEqualTo("personId");
        assertThat(refused.body().path("errors").get(0).path("code").asString()).isEqualTo("CONSENT_REQUIRED");

        Reply created = send("POST", profiles(consenting), admin, "{\"headline\":\"Alumna\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        assertThat(send("GET", profiles(consenting) + "/" + created.body().path("id").asString(), admin, null)
                .status()).isEqualTo(200);
        assertThat(send("POST", profiles(UUID.randomUUID()), admin, "{\"headline\":\"Alumna\"}").status())
                .as("an unknown person is not found, whatever the consent").isEqualTo(404);
        assertThat(send("POST", profiles(refusing), scoped(tenant), "{\"headline\":\"Alumna\"}").status())
                .as("a caller who may not create tenant-level data learns nothing of the consent").isEqualTo(403);
    }

    @Test
    void withoutConsentOnlyThePersonSeesTheirProfile() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID profile = UUID.randomUUID();
        UUID owner = person(tenant, profile);
        String path = profiles(owner) + "/" + profile;
        assertThat(send("GET", path, user(tenant, null), null).status())
                .as("hidden from others, as if absent").isEqualTo(404);
        assertThat(send("PATCH", path + "?updateMask=headline", user(tenant, null), "{\"headline\":\"Changed\"}")
                .status()).as("an update answers the profile, so it is hidden too").isEqualTo(404);
        Reply own = send("GET", path, user(tenant, owner), null);
        assertThat(own.status()).as(own.body().toString()).isEqualTo(200);
        assertThat(own.body().path("headline").asString()).isEqualTo("Alumna");
    }

    @Test
    void whilePrivacyCannotAnswerNothingIsCreatedOrShown() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID profile = UUID.randomUUID();
        UUID owner = person(tenant, profile);
        TestConsents.unavailableFor(owner);
        String admin = user(tenant, null);
        assertThat(send("GET", profiles(owner) + "/" + profile, admin, null).status()).isEqualTo(503);
        assertThat(send("GET", profiles(owner) + "/" + UUID.randomUUID(), admin, null).status())
                .as("a missing profile answers the same: the outage reveals no profile").isEqualTo(503);
        assertThat(send("POST", profiles(owner), admin, "{\"headline\":\"Again\"}").status()).isEqualTo(503);
    }
}
