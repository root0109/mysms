package com.mysms.platform.org;

import com.mysms.platform.PlatformApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.platform.org.events.FinderListingSync;
import com.mysms.platform.PlatformTenants;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.events.TenantDirectorySync;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * org.FindInstitutions over HTTP against real PostgreSQL, without a token: active schools of active tenants that opted
 * in, across tenants, by a part of the name or the whole code, with the tenant's sign-in code and nothing else; the
 * directory follows every change of an institution, platform's tenant events and iam's events of the opt-in setting.
 * Each test names schools of its own.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrgSchoolFinderComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String FIND = "/v1/org/institutions:find?query=";

    @LocalServerPort
    private int port;

    @Autowired
    private TenantDirectorySync tenants;

    @Autowired
    private FinderListingSync listings;

    @Autowired
    @Qualifier(OrgDatabaseConfig.DATABASE)
    private ModuleDatabase database;
    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase platform;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "org", "platform", "master");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {

        List<String> names() {
            return body.path("data").findValuesAsString("name");
        }
    }

    /** A tenant platform announced, with an administrator who manages its schools. */
    private final class Tenant {
        final UUID id = UUID.randomUUID();
        final String code;
        final String admin;

        Tenant(String code, String status) {
            this.code = code;
            PlatformTenants.register(platform, id);
            announce("created", status, 1);
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", UUID.randomUUID().toString());
            claims.put("tid", id.toString());
            claims.put("aud", "mysms");
            claims.put("perms", List.of("org.*"));
            claims.put("tperms", List.of("org.*"));
            claims.put("ins", List.of("*"));
            admin = TOKENS.custom(claims, Instant.now().plusSeconds(900));
        }

        boolean announce(String verb, String status, int version) {
            return tenants.apply(new EventEnvelope(UUID.randomUUID(), "platform", "platform.tenant." + verb,
                    Optional.empty(), "platform.tenant", id, Instant.now(), "{\"id\":\"" + id + "\",\"code\":\"" + code
                    + "\",\"status\":\"" + status + "\",\"rowVersion\":" + version + "}", Optional.empty()));
        }

        /** A school of the tenant, created over the API, that opted in to the finder; answers its id and etag. */
        String[] school(String code, String name) throws Exception {
            String[] school = unlistedSchool(code, name);
            assertThat(setting("created", UUID.randomUUID(), school[0], FinderListingSync.SETTING, "true", 1)).isTrue();
            return school;
        }

        /** A school of the tenant, created over the API, whose administrators never opted in. */
        String[] unlistedSchool(String code, String name) throws Exception {
            Reply created = call("POST", "/v1/org/institutions", "{\"code\":\"" + code + "\",\"name\":\"" + name
                    + "\"}", admin);
            assertThat(created.status()).as(created.text()).isEqualTo(201);
            return new String[] {created.body().path("id").asString(), created.body().path("etag").asString()};
        }

        /** An event of an institution's setting value, as iam publishes it (IamEvents.settingValue). */
        boolean setting(String verb, UUID valueId, String institution, String key, String value, int version) {
            return listings.apply(new EventEnvelope(UUID.randomUUID(), "iam", "iam.setting_value." + verb,
                    Optional.of(id), "iam.setting_value", valueId, Instant.now(), "{\"id\":\"" + valueId
                    + "\",\"settingKey\":\"" + key + "\",\"scopeType\":\"institution\",\"scopeId\":\"" + institution
                    + "\",\"value\":\"" + value + "\",\"rowVersion\":" + version + "}", Optional.empty()));
        }
    }

    private Reply call(String method, String path, String body, String token, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
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
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private Reply find(String query) throws Exception {
        return call("GET", FIND + URLEncoder.encode(query, StandardCharsets.UTF_8), null, null);
    }

    @Test
    void aGuestFindsActiveSchoolsOfActiveTenantsAcrossTenantsWithTheirCode() throws Exception {
        Tenant greenwood = new Tenant("greenwood-" + UUID.randomUUID().toString().substring(0, 6), "active");
        Tenant meadow = new Tenant("meadow-" + UUID.randomUUID().toString().substring(0, 6), "active");
        Tenant closed = new Tenant("shut-" + UUID.randomUUID().toString().substring(0, 6), "suspended");
        greenwood.school("QW1", "Quillwood High");
        meadow.school("QW2", "Quillwood Primary");
        closed.school("QW3", "Quillwood Academy");

        Reply found = find("quillwood");
        assertThat(found.status()).as(found.text()).isEqualTo(200);
        assertThat(found.names()).containsExactly("Quillwood High", "Quillwood Primary");
        JsonNode card = found.body().path("data").get(0);
        assertThat(card.path("tenantCode").asString()).isEqualTo(greenwood.code);
        assertThat(card.path("institutionId").asString()).isNotEmpty();
        assertThat(card.has("email")).isFalse();
        assertThat(find("qw2").names()).as("the whole code, in any case").containsExactly("Quillwood Primary");
        assertThat(find("qw").status()).as("three characters at least").isEqualTo(422);
        assertThat(find("%%%").names()).as("wildcards are text").isEmpty();

        assertThat(closed.announce("updated", "active", 2)).isTrue();
        assertThat(find("quillwood").names()).hasSize(3);
        assertThat(closed.announce("updated", "suspended", 1)).as("a late event changes nothing").isFalse();
        assertThat(closed.announce("deleted", "active", 3)).isTrue();
        assertThat(find("quillwood").names()).as("a deleted tenant is closed").hasSize(2);
    }

    @Test
    void aSchoolIsFoundOnlyWhileItsAdministratorsHaveOptedIn() throws Exception {
        Tenant tenant = new Tenant("opt-" + UUID.randomUUID().toString().substring(0, 6), "active");
        String school = tenant.unlistedSchool("OP1", "Ospreyfield School")[0];
        assertThat(find("ospreyfield").names()).as("off by default").isEmpty();

        UUID value = UUID.randomUUID();
        assertThat(tenant.setting("created", value, school, FinderListingSync.SETTING, "true", 1)).isTrue();
        assertThat(find("ospreyfield").names()).as("on").containsExactly("Ospreyfield School");
        assertThat(tenant.setting("updated", value, school, FinderListingSync.SETTING, "false", 2)).isTrue();
        assertThat(find("ospreyfield").names()).as("switched off again").isEmpty();
        assertThat(tenant.setting("updated", value, school, FinderListingSync.SETTING, "true", 1))
                .as("a late event changes nothing").isFalse();
        assertThat(tenant.setting("updated", value, school, FinderListingSync.SETTING, "true", 3)).isTrue();
        assertThat(find("ospreyfield").names()).containsExactly("Ospreyfield School");
        assertThat(tenant.setting("deleted", value, school, FinderListingSync.SETTING, "true", 4)).isTrue();
        assertThat(find("ospreyfield").names()).as("a deleted value: the default, off, applies").isEmpty();

        assertThat(tenant.setting("created", UUID.randomUUID(), school, "ui.theme", "true", 1))
                .as("another setting changes nothing").isFalse();
        Tenant other = new Tenant("oth-" + UUID.randomUUID().toString().substring(0, 6), "active");
        assertThat(other.setting("created", UUID.randomUUID(), school, FinderListingSync.SETTING, "true", 1)).isTrue();
        assertThat(find("ospreyfield").names()).as("another tenant cannot list this school").isEmpty();
    }

    @Test
    void aSchoolsEventsCarryItsOwnTimeZoneAndVersion() throws Exception {
        Tenant tenant = new Tenant("zone-" + UUID.randomUUID().toString().substring(0, 6), "active");
        String[] school = tenant.unlistedSchool("TZ1", "Zone Academy");
        Reply zoned = call("PATCH", "/v1/org/institutions/" + school[0], "{\"timezone\":\"Asia/Kolkata\"}",
                tenant.admin, "If-Match", school[1]);
        assertThat(zoned.status()).as(zoned.text()).isEqualTo(200);

        JsonNode created = event(tenant.id, "org.institution.created", school[0]);
        assertThat(created.has("timezone")).as("the tenant's zone").isFalse();
        assertThat(created.path("rowVersion").asInt()).isEqualTo(1);
        JsonNode updated = event(tenant.id, "org.institution.updated", school[0]);
        assertThat(updated.path("timezone").asString()).isEqualTo("Asia/Kolkata");
        assertThat(updated.path("rowVersion").asInt()).isEqualTo(2);
    }

    @Test
    void aHolidaysEventsCarryTheRowAndItsVersionForHrmsCopyOfTheCalendar() throws Exception {
        Tenant tenant = new Tenant("hol-" + UUID.randomUUID().toString().substring(0, 6), "active");
        String[] school = tenant.unlistedSchool("HO1", "Holiday Academy");
        Reply created = call("POST", "/v1/org/holidays", "{\"institutionId\":\"" + school[0]
                + "\",\"title\":\"Founders Day\",\"startDate\":\"2026-08-14\",\"endDate\":\"2026-08-15\"}",
                tenant.admin);
        assertThat(created.status()).as(created.text()).isEqualTo(201);
        String id = created.body().path("id").asString();
        Reply tenantWide = call("PATCH", "/v1/org/holidays/" + id, "{\"institutionId\":null}", tenant.admin,
                "If-Match", created.body().path("etag").asString());
        assertThat(tenantWide.status()).as(tenantWide.text()).isEqualTo(200);
        JsonNode widened = event(tenant.id, "org.holiday.updated", id);
        assertThat(widened.has("institutionId")).as("a holiday of the whole tenant").isFalse();
        assertThat(widened.path("rowVersion").asInt()).isEqualTo(2);
        // re-sending a field with its current value is a change like any other: the row is announced again (what
        // DEPLOYMENT §5.7 asks of an install whose holidays predate hrms's copy); an empty PATCH is not
        Reply resaved = call("PATCH", "/v1/org/holidays/" + id, "{\"title\":\"Founders Day\"}", tenant.admin,
                "If-Match", tenantWide.body().path("etag").asString());
        assertThat(resaved.status()).as(resaved.text()).isEqualTo(200);
        JsonNode announced = event(tenant.id, "org.holiday.updated", id);
        assertThat(announced.path("rowVersion").asInt()).isEqualTo(3);
        assertThat(announced.path("title").asString()).isEqualTo("Founders Day");
        assertThat(call("DELETE", "/v1/org/holidays/" + id, null, tenant.admin, "If-Match",
                resaved.body().path("etag").asString()).status()).isEqualTo(204);

        JsonNode added = event(tenant.id, "org.holiday.created", id);
        assertThat(added.path("institutionId").asString()).isEqualTo(school[0]);
        assertThat(added.path("title").asString()).isEqualTo("Founders Day");
        assertThat(added.path("startDate").asString()).isEqualTo("2026-08-14");
        assertThat(added.path("endDate").asString()).isEqualTo("2026-08-15");
        assertThat(added.path("rowVersion").asInt()).isEqualTo(1);
        assertThat(event(tenant.id, "org.holiday.deleted", id).path("rowVersion").asInt()).isEqualTo(4);
    }

    private JsonNode event(UUID tenant, String type, String institution) {
        String payload = CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736")
                .withIdentity(tenant, null), () -> TestTransactions.of(database).read(() -> database.jdbc()
                .sql("SELECT payload FROM org.outbox_event WHERE tenant_id = :tenantId AND event_type = :type"
                        + " AND aggregate_id = :id ORDER BY id DESC LIMIT 1").param("type", type)
                .param("id", UUID.fromString(institution))
                .query(String.class).single()));
        return JSON.readTree(payload);
    }

    @Test
    void theDirectoryFollowsEveryChangeOfASchool() throws Exception {
        Tenant tenant = new Tenant("vale-" + UUID.randomUUID().toString().substring(0, 6), "active");
        String[] school = tenant.school("VX1", "Vexley Grammar");
        assertThat(find("vexley").names()).containsExactly("Vexley Grammar");

        Reply renamed = call("PATCH", "/v1/org/institutions/" + school[0], "{\"name\":\"Vexley College\"}",
                tenant.admin, "If-Match", school[1]);
        assertThat(renamed.status()).as(renamed.text()).isEqualTo(200);
        assertThat(find("vexley").names()).containsExactly("Vexley College");
        Reply inactive = call("PATCH", "/v1/org/institutions/" + school[0], "{\"status\":\"inactive\"}", tenant.admin,
                "If-Match", renamed.body().path("etag").asString());
        assertThat(inactive.status()).as(inactive.text()).isEqualTo(200);
        assertThat(find("vexley").names()).as("an inactive school is not found").isEmpty();
        Reply active = call("PATCH", "/v1/org/institutions/" + school[0], "{\"status\":\"active\"}", tenant.admin,
                "If-Match", inactive.body().path("etag").asString());
        assertThat(find("vexley").names()).containsExactly("Vexley College");
        assertThat(call("DELETE", "/v1/org/institutions/" + school[0], null, tenant.admin, "If-Match",
                active.body().path("etag").asString()).status()).isEqualTo(204);
        assertThat(find("vexley").names()).as("nor a deleted one").isEmpty();
    }

    @Test
    void resultsComePageByPageAndACountryNarrowsThemThroughMaster() throws Exception {
        Tenant tenant = new Tenant("zed-" + UUID.randomUUID().toString().substring(0, 6), "active");
        String[] north = tenant.school("ZB1", "Zorbel North");
        tenant.school("ZB2", "Zorbel South");
        tenant.school("ZB3", "Zorbel West");

        Reply first = call("GET", FIND + "zorbel&size=2", null, null);
        assertThat(first.names()).containsExactly("Zorbel North", "Zorbel South");
        String next = first.body().path("paging").path("next").asString();
        assertThat(call("GET", next, null, null).names()).containsExactly("Zorbel West");
        assertThat(call("GET", FIND + "zorbel&page=2&size=2&cursor=nonsense", null, null).status()).isEqualTo(400);
        assertThat(call("GET", next.replace("zorbel", "zorbe"), null, null).status())
                .as("a cursor belongs to its query").isEqualTo(400);
        assertThat(call("GET", FIND + "zorbel", null, tenant.admin).names()).as("signed in, the same answer")
                .hasSize(3);
        Reply india = call("POST", "/v1/master/countries", "{\"iso2\":\"IN\",\"name\":\"India\"}",
                TOKENS.platformOperator(UUID.randomUUID(), "master.country.create"));
        assertThat(india.status()).as(india.text()).isEqualTo(201);
        Reply placed = call("PATCH", "/v1/org/institutions/" + north[0], "{\"countryId\":\""
                + india.body().path("id").asString() + "\"}", tenant.admin, "If-Match", north[1]);
        assertThat(placed.status()).as(placed.text()).isEqualTo(200);
        Reply inIndia = call("GET", FIND + "zorbel&countryCode=in", null, null);
        assertThat(inIndia.status()).as(inIndia.text()).isEqualTo(200);
        assertThat(inIndia.names()).as("master (in process) names the country; only its school is found")
                .containsExactly("Zorbel North");
        Reply inNepal = call("GET", FIND + "zorbel&countryCode=NP", null, null);
        assertThat(inNepal.status()).as(inNepal.text()).isEqualTo(200);
        assertThat(inNepal.names()).as("a country master does not know finds nothing").isEmpty();
        assertThat(call("GET", FIND + "zorbel&countryCode=India", null, null).status()).isEqualTo(422);
    }
}
