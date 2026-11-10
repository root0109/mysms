package com.mysms.master;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.master.config.MasterDatabaseConfig;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * The master contract end to end over HTTP against real PostgreSQL: auth at the edge, validation, uniqueness,
 * ETag/If-Match, update masks, keyset paging, filters, read masks, soft delete, audit + events in the outbox.
 */
@SpringBootTest(classes = MasterApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MasterApiComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String OPERATOR = TOKENS.platformOperator(UUID.randomUUID(),
            "master.country.create", "master.country.update", "master.country.delete",
            "master.state_province.create", "master.state_province.update", "master.state_province.delete",
            "master.city.create", "master.city.update", "master.city.delete",
            "master.currency.create", "master.currency.update", "master.currency.delete");

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(MasterDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "master");
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

    private static String q(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String createCountry(String iso2, String name) throws Exception {
        Reply created = call("POST", "/v1/master/countries", OPERATOR, "{\"iso2\":\"" + iso2 + "\",\"name\":\"" + name + "\"}");
        assertThat(created.status()).as(created.raw().body()).isEqualTo(201);
        return created.body().path("id").asString();
    }

    @Test
    @Order(1)
    void createsAndReadsCountries() throws Exception {
        Reply empty = call("GET", "/v1/master/countries", null, null);
        assertThat(empty.status()).isEqualTo(200);
        assertThat(empty.body().path("data")).isEmpty();
        assertThat(empty.body().path("paging").path("size").asInt()).isZero();
        assertThat(empty.body().path("paging").has("next")).isFalse();

        Reply created = call("POST", "/v1/master/countries", OPERATOR,
                "{\"iso2\":\"in\",\"iso3\":\"ind\",\"name\":\"India\",\"shortName\":\"IN\",\"phonePrefix\":\"+91\","
                        + "\"id\":\"00000000-0000-0000-0000-000000000000\",\"etag\":\"\\\"99\\\"\"}");
        assertThat(created.status()).isEqualTo(201);
        String id = created.body().path("id").asString();
        assertThat(UUID.fromString(id).version()).isEqualTo(7);
        assertThat(created.raw().headers().firstValue("Location")).contains("/v1/master/countries/" + id);
        assertThat(created.etag()).isEqualTo("\"1\"");
        assertThat(created.body().path("iso2").asString()).isEqualTo("IN");
        assertThat(created.body().path("iso3").asString()).isEqualTo("IND");
        assertThat(created.body().path("isActive").asBoolean()).isTrue();
        assertThat(created.body().path("createdBy").isMissingNode()).isFalse();

        Reply read = call("GET", "/v1/master/countries/" + id, null, null);
        assertThat(read.status()).isEqualTo(200);
        assertThat(read.etag()).isEqualTo("\"1\"");
        assertThat(read.body().path("name").asString()).isEqualTo("India");
        Reply masked = call("GET", "/v1/master/countries/" + id + "?fields=" + q("name,iso2"), null, null);
        assertThat(masked.body().properties()).extracting(java.util.Map.Entry::getKey).containsExactlyInAnyOrder("name", "iso2");
        assertThat(call("GET", "/v1/master/countries/" + id + "?fields=secret", null, null).status()).isEqualTo(400);
        assertThat(call("GET", "/v1/master/countries/" + UUID.randomUUID(), null, null).status()).isEqualTo(404);
    }

    @Test
    @Order(2)
    void rejectsInvalidAndDuplicateCountries() throws Exception {
        Reply duplicate = call("POST", "/v1/master/countries", OPERATOR, "{\"iso2\":\"IN\",\"name\":\"Other\"}");
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.body().path("code").asString()).isEqualTo("DUPLICATE");
        Reply tooLong = call("POST", "/v1/master/countries", OPERATOR, "{\"iso2\":\"XYZ\",\"name\":\"X\"}");
        assertThat(tooLong.status()).isEqualTo(422);
        assertThat(tooLong.body().path("errors").get(0).path("field").asString()).isEqualTo("iso2");
        Reply pattern = call("POST", "/v1/master/countries", OPERATOR, "{\"iso2\":\"1A\",\"name\":\"X\"}");
        assertThat(pattern.status()).isEqualTo(422);
        assertThat(pattern.body().path("code").asString()).isEqualTo("VALIDATION_FAILED");
        assertThat(call("POST", "/v1/master/countries", OPERATOR, "{\"iso2\":\"XX\"}").status()).isEqualTo(422);
        assertThat(call("POST", "/v1/master/countries", OPERATOR, "{nope").status()).isEqualTo(400);
        Reply dryRun = call("POST", "/v1/master/countries?validateOnly=true", OPERATOR, "{\"iso2\":\"ZZ\",\"name\":\"Dry\"}");
        assertThat(dryRun.status()).isEqualTo(200);
        assertThat(call("GET", "/v1/master/countries?filter=" + q("iso2 = \"ZZ\""), null, null).body().path("data")).isEmpty();
    }

    @Test
    @Order(3)
    void edgeEnforcesSchemeAndPermission() throws Exception {
        String tenantUser = TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID(), "master.country.create");
        assertThat(call("POST", "/v1/master/countries", tenantUser, "{\"name\":\"X\"}").status()).isEqualTo(403);
        String readOnlyOperator = TOKENS.platformOperator(UUID.randomUUID());
        Reply denied = call("POST", "/v1/master/countries", readOnlyOperator, "{\"name\":\"X\"}");
        assertThat(denied.status()).isEqualTo(403);
        assertThat(denied.body().path("code").asString()).isEqualTo("PERMISSION_DENIED");
        assertThat(call("POST", "/v1/master/countries", null, "{\"name\":\"X\"}").status()).isEqualTo(401);
        // a reference list is public; a signed-in tenant user may read it too
        assertThat(call("GET", "/v1/master/countries", tenantUser, null).status()).isEqualTo(200);
    }

    @Test
    @Order(4)
    void updatesUnderIfMatchWithMasks() throws Exception {
        String id = call("GET", "/v1/master/countries?filter=" + q("iso2 = \"IN\""), null, null)
                .body().path("data").get(0).path("id").asString();
        String path = "/v1/master/countries/" + id;
        assertThat(call("PATCH", path, OPERATOR, "{\"name\":\"Bharat\"}").status()).isEqualTo(428);
        Reply stale = call("PATCH", path, OPERATOR, "{\"name\":\"Bharat\"}", "If-Match", "\"7\"");
        assertThat(stale.status()).isEqualTo(412);
        assertThat(stale.body().path("code").asString()).isEqualTo("ETAG_MISMATCH");
        Reply renamed = call("PATCH", path, OPERATOR, "{\"name\":\"Bharat\"}", "If-Match", "\"1\"");
        assertThat(renamed.status()).isEqualTo(200);
        assertThat(renamed.etag()).isEqualTo("\"2\"");
        assertThat(renamed.body().path("shortName").asString()).isEqualTo("IN");
        Reply cleared = call("PATCH", path + "?updateMask=shortName", OPERATOR, "{}", "If-Match", "\"2\"");
        assertThat(cleared.status()).isEqualTo(200);
        assertThat(cleared.body().path("shortName").isMissingNode()).isTrue();
        assertThat(cleared.body().path("name").asString()).isEqualTo("Bharat");
        Reply required = call("PATCH", path + "?updateMask=name", OPERATOR, "{}", "If-Match", "\"3\"");
        assertThat(required.status()).isEqualTo(422);
        assertThat(call("PATCH", path + "?updateMask=id", OPERATOR, "{}", "If-Match", "\"3\"").status()).isEqualTo(400);
        Reply dryRun = call("PATCH", path + "?validateOnly=true", OPERATOR, "{\"name\":\"Dry\"}", "If-Match", "\"3\"");
        assertThat(dryRun.status()).isEqualTo(200);
        assertThat(call("GET", path, null, null).body().path("name").asString()).isEqualTo("Bharat");
    }

    @Test
    @Order(5)
    void pagesFiltersAndOrders() throws Exception {
        createCountry("FR", "France");
        createCountry("DE", "Germany");
        createCountry("JP", "Japan");
        Reply first = call("GET", "/v1/master/countries?size=2&sort=name,desc", null, null);
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.body().path("data")).extracting(n -> n.path("name").asString()).containsExactly("Japan", "Germany");
        JsonNode firstPaging = first.body().path("paging");
        assertThat(firstPaging.path("page").asInt()).isEqualTo(1);
        assertThat(firstPaging.path("size").asInt()).isEqualTo(2);
        assertThat(firstPaging.path("first").asString()).isEqualTo("/v1/master/countries?page=1&size=2&sort=name,desc");
        assertThat(firstPaging.has("previous")).isFalse();
        String nextLink = firstPaging.path("next").asString();
        assertThat(nextLink).startsWith("/v1/master/countries?page=2&size=2&sort=name,desc&cursor=");
        // the next link continues by cursor (keyset) after Germany
        Reply second = call("GET", nextLink, null, null);
        assertThat(second.body().path("data")).extracting(n -> n.path("name").asString()).containsExactly("France", "Bharat");
        assertThat(second.body().path("paging").has("next")).isFalse();
        assertThat(second.body().path("paging").path("previous").asString())
                .isEqualTo("/v1/master/countries?page=1&size=2&sort=name,desc");
        // a page number without a cursor reads by offset: the same rows
        Reply byOffset = call("GET", "/v1/master/countries?page=2&size=2&sort=name,desc", null, null);
        assertThat(byOffset.body().path("data")).extracting(n -> n.path("name").asString())
                .containsExactly("France", "Bharat");
        // the cursor belongs to its sort and page
        String cursor = nextLink.substring(nextLink.indexOf("cursor=") + "cursor=".length());
        assertThat(call("GET", "/v1/master/countries?page=2&size=2&cursor=" + cursor, null, null).status()).isEqualTo(400);
        assertThat(call("GET", "/v1/master/countries?page=3&size=2&sort=name,desc&cursor=" + cursor, null, null)
                .status()).isEqualTo(400);
        // several sort criteria: repeated parameters or one comma list read the same
        Reply repeated = call("GET", "/v1/master/countries?sort=sortOrder,desc&sort=name", null, null);
        Reply joined = call("GET", "/v1/master/countries?sort=sortOrder,desc,name,asc", null, null);
        assertThat(repeated.status()).as(repeated.raw().body()).isEqualTo(200);
        assertThat(repeated.body().path("data")).isEqualTo(joined.body().path("data"));
        Reply filtered = call("GET", "/v1/master/countries?filter=" + q("name : \"an\" AND isActive = true"), null, null);
        assertThat(filtered.body().path("data")).extracting(n -> n.path("name").asString())
                .containsExactlyInAnyOrder("France", "Germany", "Japan");
        Reply badFilter = call("GET", "/v1/master/countries?filter=" + q("secret = 1"), null, null);
        assertThat(badFilter.status()).isEqualTo(400);
        assertThat(badFilter.body().path("code").asString()).isEqualTo("INVALID_FILTER");
        assertThat(call("GET", "/v1/master/countries?sort=phonePrefix", null, null).status()).isEqualTo(400);
        assertThat(call("GET", "/v1/master/countries?size=0", null, null).status()).isEqualTo(400);
        assertThat(call("GET", "/v1/master/countries?size=501", null, null).status()).isEqualTo(400);
    }

    @Test
    @Order(6)
    void statesAndCitiesKeepTheirCountry() throws Exception {
        String india = call("GET", "/v1/master/countries?filter=" + q("iso2 = \"IN\""), null, null)
                .body().path("data").get(0).path("id").asString();
        String france = call("GET", "/v1/master/countries?filter=" + q("iso2 = \"FR\""), null, null)
                .body().path("data").get(0).path("id").asString();
        Reply orphan = call("POST", "/v1/master/states", OPERATOR,
                "{\"countryId\":\"" + UUID.randomUUID() + "\",\"name\":\"Nowhere\"}");
        assertThat(orphan.status()).isEqualTo(422);
        assertThat(orphan.body().path("errors").get(0).path("field").asString()).isEqualTo("countryId");
        Reply state = call("POST", "/v1/master/states", OPERATOR,
                "{\"countryId\":\"" + india + "\",\"name\":\"Karnataka\",\"code\":\"KA\"}");
        assertThat(state.status()).isEqualTo(201);
        String stateId = state.body().path("id").asString();
        assertThat(call("POST", "/v1/master/states", OPERATOR,
                "{\"countryId\":\"" + india + "\",\"name\":\"Karnataka\"}").status()).isEqualTo(409);
        assertThat(call("POST", "/v1/master/states", OPERATOR,
                "{\"countryId\":\"" + india + "\",\"name\":\"Other\",\"code\":\"KA\"}").status()).isEqualTo(409);
        Reply mismatch = call("POST", "/v1/master/cities", OPERATOR,
                "{\"countryId\":\"" + france + "\",\"stateProvinceId\":\"" + stateId + "\",\"name\":\"Bengaluru\"}");
        assertThat(mismatch.status()).isEqualTo(422);
        Reply city = call("POST", "/v1/master/cities", OPERATOR,
                "{\"countryId\":\"" + india + "\",\"stateProvinceId\":\"" + stateId + "\",\"name\":\"Bengaluru\"}");
        assertThat(city.status()).isEqualTo(201);
        assertThat(call("GET", "/v1/master/cities?filter=" + q("countryId = \"" + india + "\""), null, null)
                .body().path("data")).hasSize(1);

        Reply countryNow = call("GET", "/v1/master/countries/" + india, null, null);
        Reply blocked = call("DELETE", "/v1/master/countries/" + india, OPERATOR, null, "If-Match", countryNow.etag());
        assertThat(blocked.status()).isEqualTo(409);
        assertThat(blocked.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
        Reply stateBlocked = call("DELETE", "/v1/master/states/" + stateId, OPERATOR, null, "If-Match", state.etag());
        assertThat(stateBlocked.status()).isEqualTo(409);

        String cityId = city.body().path("id").asString();
        Reply renamed = call("PATCH", "/v1/master/cities/" + cityId, OPERATOR, "{\"name\":\"Bangalore\"}",
                "If-Match", city.etag());
        assertThat(renamed.status()).isEqualTo(200);
        assertThat(call("DELETE", "/v1/master/cities/" + cityId, OPERATOR, null, "If-Match", city.etag()).status())
                .isEqualTo(412);
        assertThat(call("DELETE", "/v1/master/cities/" + cityId, OPERATOR, null, "If-Match", renamed.etag()).status())
                .isEqualTo(204);
        Reply stateRead = call("GET", "/v1/master/states/" + stateId, null, null);
        Reply stateRenamed = call("PATCH", "/v1/master/states/" + stateId, OPERATOR, "{\"code\":\"kar\"}", "If-Match",
                stateRead.etag());
        assertThat(stateRenamed.status()).isEqualTo(200);
        assertThat(call("GET", "/v1/master/states?sort=createTime,desc", null, null).status()).isEqualTo(200);
        assertThat(call("DELETE", "/v1/master/states/" + stateId, OPERATOR, null, "If-Match", stateRenamed.etag()).status())
                .isEqualTo(204);
        assertThat(call("DELETE", "/v1/master/countries/" + india, OPERATOR, null, "If-Match", countryNow.etag()).status())
                .isEqualTo(204);
        assertThat(call("GET", "/v1/master/countries/" + india, null, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/master/cities/" + cityId, null, null).status()).isEqualTo(404);
    }

    @Test
    @Order(7)
    void currenciesValidateCodesAndUnits() throws Exception {
        Reply inr = call("POST", "/v1/master/currencies", OPERATOR,
                "{\"code\":\"inr\",\"name\":\"Indian rupee\",\"symbol\":\"₹\"}");
        assertThat(inr.status()).isEqualTo(201);
        assertThat(inr.body().path("code").asString()).isEqualTo("INR");
        assertThat(inr.body().path("minorUnits").asInt()).isEqualTo(2);
        assertThat(call("POST", "/v1/master/currencies", OPERATOR, "{\"code\":\"INR\",\"name\":\"Again\"}").status())
                .isEqualTo(409);
        assertThat(call("POST", "/v1/master/currencies", OPERATOR, "{\"code\":\"US\",\"name\":\"Short\"}").status())
                .isEqualTo(422);
        assertThat(call("POST", "/v1/master/currencies", OPERATOR, "{\"code\":\"USD\",\"name\":\"Dollar\",\"minorUnits\":9}")
                .status()).isEqualTo(422);
        String id = inr.body().path("id").asString();
        Reply updated = call("PATCH", "/v1/master/currencies/" + id, OPERATOR, "{\"minorUnits\":0,\"isActive\":false}",
                "If-Match", inr.etag());
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.body().path("isActive").asBoolean()).isFalse();
        assertThat(call("GET", "/v1/master/currencies?sort=code", null, null).body().path("data")).hasSize(1);
        assertThat(call("DELETE", "/v1/master/currencies/" + id, OPERATOR, null, "If-Match", updated.etag()).status())
                .isEqualTo(204);
    }

    @Test
    @Order(8)
    void everyChangeLeftAuditAndDomainEventsInTheOutbox() {
        Integer audits = database.jdbc().sql("SELECT count(*) FROM master.outbox_event WHERE event_type = 'audit.recorded'")
                .query(Integer.class).single();
        Integer created = database.jdbc()
                .sql("SELECT count(*) FROM master.outbox_event WHERE event_type = 'master.country.created'")
                .query(Integer.class).single();
        Integer deleted = database.jdbc().sql("SELECT count(*) FROM master.outbox_event WHERE event_type LIKE '%.deleted'")
                .query(Integer.class).single();
        assertThat(created).isEqualTo(4);
        assertThat(deleted).isEqualTo(4);
        assertThat(audits).isGreaterThanOrEqualTo(15);
        String payload = database.jdbc().sql(
                "SELECT payload FROM master.outbox_event WHERE event_type = 'audit.recorded' ORDER BY id LIMIT 1")
                .query(String.class).single();
        assertThat(payload).contains("\"action\":\"create\"", "\"entityType\":\"master.country\"", "\"actorUserId\":\"");
    }
}
