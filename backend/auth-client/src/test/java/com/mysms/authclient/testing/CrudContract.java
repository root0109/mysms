package com.mysms.authclient.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The standard-operation contract of every resource of a module, checked end to end over HTTP against real
 * PostgreSQL (JAVA-CODING-RULES §12): create, read, list with filter, partial update under If-Match, soft delete, the
 * 403/404/409/412/428 answers, tenant isolation and the institution data scope. The resources and their sample
 * payloads come from {@code crud-spec.json} on the module's component-test classpath, generated from the contract and
 * the schema together with the code under test (tools/backend/crud); nothing is mocked.
 */
public final class CrudContract {

    /** The institution every tenant-wide sample row belongs to. */
    public static final UUID INSTITUTION = UUID.fromString("0196f000-0000-7000-8000-00000000a001");
    private static final UUID OUTSIDE_INSTITUTION = UUID.fromString("0196f000-0000-7000-8000-00000000a002");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final TestTokens tokens;
    private final IntSupplier port;
    private final BiConsumer<UUID, List<String>> sql;
    private final UUID tenant = UUID.randomUUID();
    private final UUID otherTenant = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();

    /**
     * @param tokens the key pair the service under test trusts
     * @param port   the service's HTTP port
     * @param sql    runs fixture statements as the application role inside a transaction bound to the given tenant
     */
    public CrudContract(TestTokens tokens, IntSupplier port, BiConsumer<UUID, List<String>> sql) {
        this.tokens = tokens;
        this.port = port;
        this.sql = sql;
    }

    /** One container of ordered checks per resource in the spec. */
    public List<DynamicNode> tests(String specResource) {
        JsonNode spec = read(specResource);
        List<DynamicNode> containers = new ArrayList<>();
        for (JsonNode resource : spec.path("resources")) {
            containers.add(DynamicContainer.dynamicContainer(resource.path("name").asString(), checks(resource)));
        }
        return containers;
    }

    private List<DynamicTest> checks(JsonNode r) {
        State s = new State(r);
        List<DynamicTest> t = new ArrayList<>();
        t.add(DynamicTest.dynamicTest("fixtures", () -> sql.accept(tenant, strings(r.path("fixtures")))));
        if (s.has("Create") && s.r.path("publicCreate").asBoolean()) {
            t.add(DynamicTest.dynamicTest("anyone may create it (a public form)", () -> createInPublic(s)));
        } else if (s.has("Create")) {
            t.add(DynamicTest.dynamicTest("create needs the permission", () -> createWithoutPermission(s)));
        }
        if (s.has("Create")) {
            t.add(DynamicTest.dynamicTest("validateOnly writes nothing", () -> validateOnly(s)));
            t.add(DynamicTest.dynamicTest("create", () -> create(s)));
            if (r.path("duplicateConflicts").asBoolean()) {
                t.add(DynamicTest.dynamicTest("a duplicate is 409", () -> duplicate(s)));
            }
        } else {
            t.add(DynamicTest.dynamicTest("seed", () -> seed(s)));
        }
        if (s.has("Get")) {
            t.add(DynamicTest.dynamicTest("get", () -> get(s)));
        }
        if (s.has("List")) {
            t.add(DynamicTest.dynamicTest("list and filter", () -> list(s)));
        }
        if (s.global()) {
            t.add(DynamicTest.dynamicTest("a tenant administrator cannot change it", () -> tenantCannotWrite(s)));
        } else {
            t.add(DynamicTest.dynamicTest("other tenants never see the row", () -> otherTenant(s)));
            t.add(DynamicTest.dynamicTest("institution scope", () -> scope(s)));
        }
        if (s.has("Update")) {
            t.add(DynamicTest.dynamicTest("update under If-Match", () -> update(s)));
        }
        for (JsonNode transition : r.path("transitions")) {
            t.add(DynamicTest.dynamicTest(transition.path("operation").asString(), () -> transition(s, transition)));
        }
        for (JsonNode secret : r.path("secrets")) {
            if (secret.hasNonNull("rotated")) {
                t.add(DynamicTest.dynamicTest("replace the secret " + secret.path("field").asString(),
                        () -> rotate(s, secret)));
            }
        }
        if (s.has("Delete")) {
            t.add(DynamicTest.dynamicTest("soft delete under If-Match", () -> delete(s)));
        }
        return t;
    }

    // ------------------------------------------------------------------------------------------------- the checks

    /** A public form: a signed-in tenant user without any permission may submit one, and it is stored globally. */
    private void createInPublic(State s) {
        Reply reply = call("POST", s.collection(), token(tenant, List.of("*"), "nothing.at.all"), s.createBody());
        assertThat(reply.status).as(reply.text).isEqualTo(201);
    }

    private void createWithoutPermission(State s) {
        Reply reply = call("POST", s.collection(), token(tenant, List.of("*"), "nothing.at.all"), s.createBody());
        assertThat(reply.status).as(reply.text).isEqualTo(403);
    }

    private void validateOnly(State s) {
        Reply reply = call("POST", s.collection() + "?validateOnly=true", admin(s), s.createBody());
        assertThat(reply.status).as(reply.text).isEqualTo(200);
        if (s.has("Get") && reply.body.hasNonNull("id")) {
            Reply after = call("GET", s.item(reply.body.path("id").asString()), admin(s), null);
            assertThat(after.status).as("validateOnly must not persist").isEqualTo(404);
        }
    }

    private void create(State s) {
        String key = UUID.randomUUID().toString();
        Reply reply = call("POST", s.collection(), admin(s), s.createBody(), "Idempotency-Key", key);
        assertThat(reply.status).as(reply.text).isEqualTo(201);
        if (s.global()) {
            // platform staff have no tenant, so no kept response (it is encrypted per tenant): a repeat runs again,
            // and a unique key refuses it rather than storing a second row
            if (s.r.path("duplicateConflicts").asBoolean()) {
                Reply again = call("POST", s.collection(), admin(s), s.createBody(), "Idempotency-Key", key);
                assertThat(again.status).as(again.text).isEqualTo(409);
            }
        } else {
            replayed(s, reply, key);
        }
        assertThat(reply.etag()).isEqualTo("\"1\"");
        s.id = reply.body.path("id").asString();
        assertThat(UUID.fromString(s.id)).isNotNull();
        if (s.has("Get")) {
            assertThat(reply.header("Location")).isEqualTo(s.item(s.id));
        }
        assertEchoes(s.echoed(), reply.body);
        assertSecrets(s, reply.body);
        List<String> checks = new ArrayList<>();
        for (JsonNode secret : s.r.path("secrets")) {
            checks.add(secret.path("check").asString().replace("{id}", s.id)
                    .replace("{plain}", s.r.path("create").path(secret.path("field").asString()).asString()));
        }
        sql.accept(tenant, checks);
    }

    /** A new secret replaces the old one: stored as a fresh envelope, shown only by its new last four. */
    private void rotate(State s, JsonNode secret) {
        String field = secret.path("field").asString();
        String value = secret.path("rotated").asString();
        int version = s.version();
        ObjectNode body = JSON.createObjectNode().put(field, value);
        Reply done = call("PATCH", s.item(s.id) + "?updateMask=" + field, admin(s), body.toString(), "If-Match",
                "\"" + version + "\"");
        assertThat(done.status).as(done.text).isEqualTo(200);
        s.known = version + 1;
        assertThat(done.body.has(field)).as("plaintext of " + field).isFalse();
        if (secret.hasNonNull("masked")) {
            assertThat(done.body.path(secret.path("masked").asString()).asString())
                    .isEqualTo("••••" + value.substring(value.length() - 4));
        }
        sql.accept(tenant, List.of(secret.path("check").asString().replace("{id}", s.id).replace("{plain}", value)));
    }

    /** A secret is never returned; its display form is "••••" and its last four characters. */
    private static void assertSecrets(State s, JsonNode row) {
        for (JsonNode secret : s.r.path("secrets")) {
            assertThat(row.has(secret.path("field").asString())).as("plaintext of " + secret.path("field"))
                    .isFalse();
            if (secret.hasNonNull("masked")) {
                assertThat(row.path(secret.path("masked").asString()).asString()).as("masked form")
                        .isEqualTo(secret.path("mask").asString());
            }
        }
    }

    /** The same request and key get the same answer back without a second row; the kept answer is encrypted. */
    private void replayed(State s, Reply reply, String key) {
        Reply replayed = call("POST", s.collection(), admin(s), s.createBody(), "Idempotency-Key", key);
        assertThat(replayed.status).as(replayed.text).isEqualTo(201);
        assertThat(replayed.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(replayed.body.path("id").asString()).isEqualTo(reply.body.path("id").asString());
        assertThat(replayed.etag()).isEqualTo(reply.etag());
        assertThat(replayed.header("Location")).isEqualTo(reply.header("Location"));
        Reply reused = call("POST", s.collection(), admin(s), s.createBody() + " ", "Idempotency-Key", key);
        assertThat(reused.status).as("a key is for one request only: " + reused.text).isEqualTo(422);
        assertThat(reused.body.path("code").asString()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        String module = s.r.path("table").asString().split("\\.")[0];
        sql.accept(tenant, List.of("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM " + module + ".idempotency_key WHERE "
                + "idempotency_key = '" + key + "' AND response_status = 201 AND response_body LIKE 'v1.%') OR EXISTS "
                + "(SELECT 1 FROM " + module + ".idempotency_key WHERE idempotency_key = '" + key + "' AND strpos("
                + "response_body, '" + reply.body.path("id").asString() + "') > 0) THEN RAISE EXCEPTION "
                + "'kept response missing or not encrypted'; END IF; END $$"));
    }

    private void duplicate(State s) {
        Reply reply = call("POST", s.collection(), admin(s), s.createBody());
        assertThat(reply.status).as(reply.text).isEqualTo(409);
    }

    private void seed(State s) {
        s.id = s.r.path("seedId").asString();
        sql.accept(tenant, strings(s.r.path("seed")));
    }

    private void get(State s) {
        Reply reply = call("GET", s.item(s.id), admin(s), null);
        assertThat(reply.status).as(reply.text).isEqualTo(200);
        assertThat(reply.body.path("id").asString()).isEqualTo(s.id);
        assertThat(reply.etag()).isEqualTo("\"" + reply.body.path("etag").asString().replace("\"", "") + "\"");
        if (s.has("Create")) {
            assertEchoes(s.echoed(), reply.body);
            assertSecrets(s, reply.body);
        }
        Reply masked = call("GET", s.item(s.id) + "?fields=id", admin(s), null);
        assertThat(masked.status).isEqualTo(200);
        assertThat(masked.body.propertyNames()).containsExactly("id");
        assertThat(call("GET", s.item(UUID.randomUUID().toString()), admin(s), null).status).isEqualTo(404);
    }

    private void list(State s) {
        Reply all = call("GET", s.listing("pageSize=500", null), admin(s), null);
        assertThat(all.status).as(all.text).isEqualTo(200);
        assertThat(ids(all.body)).contains(s.id);
        Reply filtered = call("GET", s.listing("", "id = \"" + s.id + "\""), admin(s), null);
        assertThat(filtered.status).as(filtered.text).isEqualTo(200);
        assertThat(ids(filtered.body)).containsExactly(s.id);
        assertThat(filtered.body.path("totalSize").asInt()).isEqualTo(1);
        Reply page = call("GET", s.listing("pageSize=1&orderBy=" + encode("createTime desc"), null), admin(s),
                null);
        assertThat(page.status).as(page.text).isEqualTo(200);
        assertThat(page.body.path("items").size()).isEqualTo(1);
        Reply badFilter = call("GET", s.listing("", "noSuchField = 1"), admin(s), null);
        assertThat(badFilter.status).isEqualTo(400);
        if (s.r.has("partition")) {
            Reply unbounded = call("GET", s.collection() + "?pageSize=500", admin(s), null);
            assertThat(unbounded.status).as("a partitioned list needs its range: " + unbounded.text).isEqualTo(400);
            assertThat(unbounded.text).contains("INVALID_FILTER").contains(s.r.path("partition").asString());
            Reply onlyId = call("GET", s.collection() + "?filter=" + encode("id = \"" + s.id + "\""), admin(s), null);
            assertThat(onlyId.status).isEqualTo(400);
        }
    }

    private void otherTenant(State s) {
        String stranger = token(otherTenant, List.of("*"), List.of("*"), "*");
        if (s.has("Get")) {
            assertThat(call("GET", s.item(s.id), stranger, null).status).isEqualTo(404);
        }
        if (s.has("List")) {
            Reply list = call("GET", s.listing("pageSize=500", null), stranger, null);
            assertThat(list.status).as(list.text).isIn(200, 404);
            if (list.status == 200) {
                assertThat(ids(list.body)).doesNotContain(s.id);
            }
        }
    }

    private void scope(State s) {
        String scope = s.r.path("scope").asString();
        String inside = token(tenant, List.of(INSTITUTION.toString()), "*");
        String outside = token(tenant, List.of(OUTSIDE_INSTITUTION.toString()), "*");
        // a tenant-wide grant of other permissions (here: read only) never widens the write permissions
        String mixed = token(tenant, List.of("*", OUTSIDE_INSTITUTION.toString()), List.of("*.read.never"), "*");
        if (s.has("Delete") && !"self".equals(scope)) {
            int status = call("DELETE", s.item(s.id), mixed, null, "If-Match", "\"" + s.version() + "\"").status;
            assertThat(status).as("mixed grants must not reach this row").isIn(403, 404);
        }
        if ("self".equals(scope)) {
            // an institution is its own scope: a new one is outside every school-level caller
            if (s.has("Get")) {
                assertThat(call("GET", s.item(s.id), inside, null).status).isEqualTo(404);
            }
            if (s.has("Create")) {
                assertThat(call("POST", s.collection(), inside, s.createBody()).status).isEqualTo(403);
            }
            return;
        }
        if (s.r.has("ownerOnly")) {
            ownerOnly(s, UUID.fromString(s.r.path("ownerOnly").asString()));
            return;
        }
        if ("tenant".equals(scope)) {
            if (s.has("Get")) {
                assertThat(call("GET", s.item(s.id), outside, null).status).as("tenant-level rows are readable")
                        .isEqualTo(200);
            }
            if (s.has("Create")) {
                Reply create = call("POST", s.collection(), inside, s.createBody());
                assertThat(create.status).as("tenant-level rows change only through a tenant-wide grant: " + create.text)
                        .isEqualTo(403);
            }
            if (s.has("Delete")) {
                assertThat(call("DELETE", s.item(s.id), inside, null, "If-Match", "\"" + s.version() + "\"").status)
                        .isEqualTo(403);
            }
            return;
        }
        if (s.has("Get")) {
            assertThat(call("GET", s.item(s.id), inside, null).status).isEqualTo(200);
            assertThat(call("GET", s.item(s.id), outside, null).status).as("outside the caller's institutions")
                    .isEqualTo(404);
        }
        if (s.has("List")) {
            Reply list = call("GET", s.listing("pageSize=500", null), outside, null);
            if (list.status == 200) {
                assertThat(ids(list.body)).doesNotContain(s.id);
            } else {
                assertThat(list.status).as(list.text).isEqualTo(404);
            }
        }
        if (s.has("Create")) {
            Reply create = call("POST", s.collection(), outside, s.createBody());
            assertThat(create.status).as(create.text).isIn(403, 404);
        }
        if (s.has("Delete")) {
            assertThat(call("DELETE", s.item(s.id), outside, null, "If-Match", "\"" + s.version() + "\"").status)
                    .isEqualTo(404);
        }
    }

    private void update(State s) {
        JsonNode change = s.r.path("update");
        String body = change.isObject() && !change.isEmpty() ? change.toString() : "{}";
        int version = s.version();
        Reply noMatch = call("PATCH", s.item(s.id), admin(s), body);
        assertThat(noMatch.status).as(noMatch.text).isEqualTo(428);
        Reply stale = call("PATCH", s.item(s.id), admin(s), body, "If-Match", "\"" + (version + 7) + "\"");
        assertThat(stale.status).as(stale.text).isEqualTo(412);
        Reply dry = call("PATCH", s.item(s.id) + "?validateOnly=true", admin(s), body, "If-Match", "\"" + version + "\"");
        assertThat(dry.status).as(dry.text).isEqualTo(200);
        assertThat(s.version()).as("validateOnly must not persist").isEqualTo(version);
        Reply done = call("PATCH", s.item(s.id), admin(s), body, "If-Match", "\"" + version + "\"");
        assertThat(done.status).as(done.text).isEqualTo(200);
        assertThat(done.etag()).isEqualTo("\"" + (version + 1) + "\"");
        s.known = version + 1;
        assertEchoes(change, done.body);
        Reply unknown = call("PATCH", s.item(s.id) + "?updateMask=noSuchField", admin(s), body, "If-Match",
                "\"" + (version + 1) + "\"");
        assertThat(unknown.status).as(unknown.text).isEqualTo(400);
    }

    /**
     * One state transition: the permission and the data scope apply as for any write; from the guarded state (set
     * up in SQL) the step answers 200, sets its values and moves the version on by one; a stale body etag is 412;
     * a repeat answers 200 again only when the new state still satisfies the guard, otherwise 409 and nothing
     * changes.
     */
    private void transition(State s, JsonNode t) {
        String path = t.path("path").asString().replace("{id}", s.id);
        String body = t.path("body").toString();
        boolean ifMatch = t.path("ifMatch").asBoolean();
        List<String> prepare = new ArrayList<>();
        for (JsonNode statement : t.path("prepare")) {
            prepare.add(statement.asString().replace("{id}", s.id));
        }
        sql.accept(tenant, prepare);
        int version = s.version();
        String[] current = ifMatch ? new String[] {"If-Match", "\"" + version + "\""} : new String[0];
        assertThat(call("POST", path, token(tenant, List.of("*"), List.of("*"), "nothing.at.all"), body, current)
                .status).as("the permission is required").isEqualTo(403);
        String scope = s.r.path("scope").asString();
        if ("tenant".equals(scope)) {
            assertThat(call("POST", path, token(tenant, List.of(INSTITUTION.toString()), "*"), body, current).status)
                    .as("a tenant-level row changes only through a tenant-wide grant").isEqualTo(403);
        } else if ("self".equals(scope)) {
            assertThat(call("POST", path, token(tenant, List.of(INSTITUTION.toString()), "*"), body, current).status)
                    .isEqualTo(404);
        } else {
            assertThat(call("POST", path, token(tenant, List.of(OUTSIDE_INSTITUTION.toString()), "*"), body, current)
                    .status).as("outside the caller's institutions").isEqualTo(404);
        }
        assertThat(call("POST", t.path("path").asString().replace("{id}", UUID.randomUUID().toString()), admin(s),
                body, current).status).isEqualTo(404);
        if (ifMatch) {
            assertThat(call("POST", path, admin(s), body).status).as("If-Match is required").isEqualTo(428);
            assertThat(call("POST", path, admin(s), body, "If-Match", "\"" + (version + 7) + "\"").status)
                    .as("a stale If-Match").isEqualTo(412);
        }
        if (t.path("etag").asBoolean()) {
            ObjectNode stale = ((ObjectNode) JSON.readTree(body)).put("etag", "\"" + (version + 7) + "\"");
            Reply refused = call("POST", path, admin(s), stale.toString(), current);
            assertThat(refused.status).as(refused.text).isEqualTo(412);
        }
        assertThat(s.version()).as("refused calls change nothing").isEqualTo(version);
        Reply done = call("POST", path, admin(s), body, current);
        assertThat(done.status).as(done.text).isEqualTo(200);
        assertThat(done.etag()).isEqualTo("\"" + (version + 1) + "\"");
        s.known = version + 1;
        assertThat(done.body.path("id").asString()).isEqualTo(s.id);
        for (Map.Entry<String, JsonNode> set : t.path("sets").properties()) {
            JsonNode actual = done.body.path(set.getKey());
            if (set.getValue().isNull()) {
                assertThat(actual.isNull() || actual.isMissingNode()).as(set.getKey() + " is cleared").isTrue();
            } else {
                assertThat(actual).as(set.getKey()).isEqualTo(set.getValue());
            }
        }
        for (JsonNode stamp : t.path("stamps")) {
            assertThat(done.body.hasNonNull(stamp.asString())).as(stamp.asString() + " is set").isTrue();
        }
        String[] next = ifMatch ? new String[] {"If-Match", "\"" + (version + 1) + "\""} : new String[0];
        Reply again = call("POST", path, admin(s), body, next);
        if (satisfies(t.path("guard"), done.body)) {
            assertThat(again.status).as(again.text).isEqualTo(200);
            s.known = version + 2;
        } else {
            assertThat(again.status).as("the step is not allowed from the new state: " + again.text).isEqualTo(409);
            assertThat(again.body.path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
            assertThat(s.version()).as("a refused step changes nothing").isEqualTo(version + 1);
        }
    }

    /** Whether the row satisfies a guard (field to allowed values; a missing boolean counts as false). */
    private static boolean satisfies(JsonNode guard, JsonNode row) {
        for (Map.Entry<String, JsonNode> field : guard.properties()) {
            JsonNode value = row.path(field.getKey());
            boolean ok = false;
            for (JsonNode allowed : field.getValue()) {
                if (allowed.isBoolean()) {
                    ok |= allowed.asBoolean() == (value.isBoolean() && value.asBoolean());
                } else {
                    ok |= allowed.asString().equals(value.asString());
                }
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private void delete(State s) {
        assertThat(call("DELETE", s.item(s.id), admin(s), null).status).isEqualTo(428);
        int version = s.version();
        assertThat(call("DELETE", s.item(s.id), admin(s), null, "If-Match", "\"" + (version + 7) + "\"").status)
                .isEqualTo(412);
        Reply done = call("DELETE", s.item(s.id), admin(s), null, "If-Match", "\"" + version + "\"");
        assertThat(done.status).as(done.text).isEqualTo(204);
        if (s.has("Get")) {
            assertThat(call("GET", s.item(s.id), admin(s), null).status).isEqualTo(404);
        }
        if (s.has("List")) {
            Reply filtered = call("GET", s.listing("", "id = \"" + s.id + "\""), admin(s), null);
            assertThat(ids(filtered.body)).isEmpty();
        }
    }

    // ------------------------------------------------------------------------------------------------- helpers

    private final class State {
        private final JsonNode r;
        private String id;
        /** The version last seen in a response, for resources served without Get and List. */
        private int known = 1;
        private String range;

        State(JsonNode r) {
            this.r = r;
        }

        /** A global table (no tenant): platform staff write it; tenant users at most read it. */
        boolean global() {
            return "global".equals(r.path("scope").asString());
        }

        boolean has(String op) {
            for (JsonNode o : r.path("ops")) {
                if (op.equals(o.asString())) {
                    return true;
                }
            }
            return false;
        }

        String collection() {
            return r.path("collection").asString();
        }

        /**
         * The collection URL with the query and the filter; a partitioned resource's filter also carries a range of
         * 30 days either side of the row's partition key, read through Get (the server refuses lists without one).
         */
        String listing(String query, String filter) {
            String range = range();
            String where = filter == null ? range : range.isEmpty() ? filter : "(" + filter + ") AND " + range;
            String url = collection() + "?" + query;
            if (!where.isEmpty()) {
                url += (query.isEmpty() ? "" : "&") + "filter=" + encode(where);
            }
            return url;
        }

        /** Computed once, while the row still exists (the delete check lists after the row is gone). */
        private String range() {
            if (!r.has("partition")) {
                return "";
            }
            if (range == null) {
                range = readRange();
            }
            return range;
        }

        private String readRange() {
            String field = r.path("partition").asString();
            Reply row = call("GET", item(id), admin(this), null);
            assertThat(row.status).as(row.text).isEqualTo(200);
            String value = row.body.path(field).asString();
            String from;
            String to;
            if (value.length() == 10) {
                LocalDate day = LocalDate.parse(value);
                from = day.minusDays(30).toString();
                to = day.plusDays(30).toString();
            } else {
                OffsetDateTime at = OffsetDateTime.parse(value);
                from = at.minusDays(30).toString();
                to = at.plusDays(30).toString();
            }
            return field + " >= \"" + from + "\" AND " + field + " <= \"" + to + "\"";
        }

        String item(String id) {
            return r.path("item").asString().replace("{id}", id);
        }

        String createBody() {
            return r.path("create").toString();
        }

        /** The create body without its write-only secrets, which are never echoed. */
        JsonNode echoed() {
            ObjectNode copy = ((ObjectNode) r.path("create")).deepCopy();
            for (JsonNode secret : r.path("secrets")) {
                copy.remove(secret.path("field").asString());
            }
            return copy;
        }

        /** The row's current version, read through the API (or the seed when the resource has no Get). */
        int version() {
            if (has("Get")) {
                Reply reply = call("GET", item(id), admin(this), null);
                assertThat(reply.status).as(reply.text).isEqualTo(200);
                return Integer.parseInt(reply.etag().replace("\"", ""));
            }
            if (!has("List")) {
                return known;
            }
            Reply list = call("GET", listing("", "id = \"" + id + "\""), admin(this), null);
            return Integer.parseInt(list.body.path("items").path(0).path("etag").asString().replace("\"", ""));
        }
    }

    private record Reply(int status, JsonNode body, String text, HttpResponse<String> raw) {
        String etag() {
            return raw.headers().firstValue("ETag").orElse(null);
        }

        String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }
    }

    private Reply call(String method, String path, String token, String body, String... headers) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port.getAsInt() + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.header("Authorization", "Bearer " + token);
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        try {
            HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
            return new Reply(response.statusCode(), json, method + " " + path + " -> " + response.statusCode() + " "
                    + response.body(), response);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * The module's long-running operations (GetOperation, ListOperations, CancelOperation) over rows of its own
     * operation table: a caller sees and cancels only what they started, a tenant-wide grant covers the tenant, a
     * finished operation cannot be cancelled, the permission is required, and another tenant sees nothing.
     */
    public List<DynamicNode> operationTests(String module) {
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();
        UUID done = UUID.randomUUID();
        String base = "/v1/" + module + "/operations";
        String read = module + ".operation.read";
        String cancel = module + ".operation.cancel_operation";
        return List.of(DynamicTest.dynamicTest("operations: own, tenant-wide and cancel", () -> {
            String insert = "INSERT INTO " + module + ".operation (id, tenant_id, operation_name, status, progress_percent,"
                    + " started_by_user_id, started_at, finished_at, result_entity_type, result_entity_id, created_at,"
                    + " updated_at, row_version) VALUES ('%s', :tenant, '%s', '%s', %s, '%s', now() - interval '%d minutes',"
                    + " %s, %s, %s, now(), now(), 1)";
            UUID file = UUID.randomUUID();
            sql.accept(tenant, List.of(
                    insert.formatted(mine, "report.run", "running", "40", user, 3, "NULL", "NULL", "NULL"),
                    insert.formatted(theirs, "import.rows", "running", "NULL", UUID.randomUUID(), 2, "NULL", "NULL",
                            "NULL"),
                    insert.formatted(done, "report.run", "succeeded", "100", user, 1, "now()", "'file'",
                            "'" + file + "'")));
            String own = token(tenant, List.of(INSTITUTION.toString()), read, cancel);
            Reply listed = call("GET", base, own, null);
            assertThat(listed.status).as(listed.text).isEqualTo(200);
            List<String> ids = new ArrayList<>();
            listed.body.path("items").forEach(item -> ids.add(item.path("operationId").asString()));
            assertThat(ids).as("own operations, newest first").containsExactly(done.toString(), mine.toString());
            JsonNode finished = listed.body.path("items").get(0);
            assertThat(finished.path("module").asString()).isEqualTo(module);
            assertThat(finished.path("status").asString()).isEqualTo("succeeded");
            assertThat(finished.path("resultFileId").asString()).isEqualTo(file.toString());
            assertThat(finished.hasNonNull("doneTime")).isTrue();
            assertThat(call("GET", base + "?kind=import.rows", own, null).body.path("items")).isEmpty();
            assertThat(call("GET", base + "/" + theirs, own, null).status).as("someone else's").isEqualTo(404);
            assertThat(call("GET", base + "?kind=import.rows", admin(), null).body.path("items")).hasSize(1);
            assertThat(call("GET", base + "/" + theirs, admin(), null).status).isEqualTo(200);
            assertThat(call("GET", base, token(UUID.randomUUID(), List.of("*"), List.of("*"), "*"), null)
                    .body.path("items")).as("another tenant").isEmpty();
            assertThat(call("GET", base, token(tenant, List.of("*"), "nothing.at.all"), null).status).isEqualTo(403);

            assertThat(call("POST", base + "/" + theirs + ":cancel", own, null).status).isEqualTo(404);
            Reply cancelled = call("POST", base + "/" + mine + ":cancel", own, null);
            assertThat(cancelled.status).as(cancelled.text).isEqualTo(202);
            assertThat(cancelled.body.path("status").asString()).isEqualTo("cancelled");
            assertThat(cancelled.body.hasNonNull("doneTime")).isTrue();
            Reply again = call("POST", base + "/" + mine + ":cancel", own, null);
            assertThat(again.status).isEqualTo(409);
            assertThat(again.body.path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
            assertThat(call("POST", base + "/" + done + ":cancel", own, null).status).isEqualTo(409);
            assertThat(call("POST", base + "/" + theirs + ":cancel", admin(), null).status).isEqualTo(202);
        }));
    }

    /** A tenant-wide administrator holding every permission. */
    /**
     * Rows reachable only by their owner (the parent user) or with the permission held tenant-wide, such as sign-in
     * sessions: another user with the permission at institution level gets 404, the owner gets the rows.
     */
    private void ownerOnly(State s, UUID owner) {
        String stranger = token(tenant, List.of("*"), "*");
        String self = tokenOf(owner, tenant, List.of("*"), List.of(), "*");
        if (s.has("Get")) {
            assertThat(call("GET", s.item(s.id), stranger, null).status).as("another user's row").isEqualTo(404);
            assertThat(call("GET", s.item(s.id), self, null).status).as("the owner's own row").isEqualTo(200);
        }
        if (s.has("List")) {
            assertThat(call("GET", s.listing("pageSize=500", null), stranger, null).status).isEqualTo(404);
            Reply own = call("GET", s.listing("pageSize=500", null), self, null);
            assertThat(own.status).as(own.text).isEqualTo(200);
            assertThat(ids(own.body)).contains(s.id);
        }
        if (s.has("Delete")) {
            assertThat(call("DELETE", s.item(s.id), stranger, null, "If-Match", "\"" + s.version() + "\"").status)
                    .as("another user cannot end it").isEqualTo(s.global() ? 403 : 404);
        }
    }

    /** Every permission: a tenant administrator, or platform staff for a global table. */
    private String admin(State s) {
        return s.global() ? tokens.platformOperator(user, "*") : admin();
    }

    /** Every permission in the test tenant. */
    private String admin() {
        return token(tenant, List.of("*"), List.of("*"), "*");
    }

    /** A global table is written only by platform staff: even a tenant administrator is refused (the scheme). */
    private void tenantCannotWrite(State s) {
        String tenantAdmin = token(tenant, List.of("*"), List.of("*"), "*");
        if (s.has("Create") && !s.r.path("publicCreate").asBoolean()) {
            assertThat(call("POST", s.collection(), tenantAdmin, s.createBody()).status).isEqualTo(403);
        }
        if (s.has("Delete")) {
            assertThat(call("DELETE", s.item(s.id), tenantAdmin, null, "If-Match", "\"" + s.version() + "\"").status)
                    .isEqualTo(403);
        }
    }

    private String token(UUID tenantId, List<String> institutions, String... permissions) {
        return token(tenantId, institutions, List.of(), permissions);
    }

    private String token(UUID tenantId, List<String> institutions, List<String> tenantWide, String... permissions) {
        return tokenOf(user, tenantId, institutions, tenantWide, permissions);
    }

    private String tokenOf(UUID subject, UUID tenantId, List<String> institutions, List<String> tenantWide,
                           String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tperms", tenantWide);
        claims.put("sub", subject.toString());
        claims.put("tid", tenantId.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("ins", institutions);
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    /** Every field sent comes back with the same value (numbers compared as decimals, instants as instants). */
    private static void assertEchoes(JsonNode sent, JsonNode received) {
        if (!sent.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = ((ObjectNode) sent).properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode expected = field.getValue();
            JsonNode actual = received.path(field.getKey());
            if (expected.isNull()) {
                assertThat(actual.isMissingNode() || actual.isNull()).as(field.getKey()).isTrue();
            } else if (expected.isNumber() || looksNumeric(expected) && looksNumeric(actual)) {
                assertThat(new BigDecimal(actual.asString())).as(field.getKey())
                        .isEqualByComparingTo(new BigDecimal(expected.asString()));
            } else if (expected.isString() && expected.asString().matches("\\d{4}-\\d{2}-\\d{2}T.*")) {
                assertThat(Instant.parse(actual.asString())).as(field.getKey())
                        .isEqualTo(Instant.parse(expected.asString()));
            } else if (expected.isString() && expected.asString().matches("\\d{2}:\\d{2}")) {
                assertThat(actual.asString()).as(field.getKey()).startsWith(expected.asString());
            } else {
                assertThat(actual).as(field.getKey()).isEqualTo(expected);
            }
        }
    }

    private static boolean looksNumeric(JsonNode node) {
        return node.isNumber() || node.isString() && node.asString().matches("-?\\d+(\\.\\d+)?");
    }

    private static List<String> ids(JsonNode list) {
        List<String> ids = new ArrayList<>();
        for (JsonNode item : list.path("items")) {
            ids.add(item.path("id").asString());
        }
        return ids;
    }

    private static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        for (JsonNode node : array) {
            out.add(node.asString());
        }
        return out;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static JsonNode read(String resource) {
        try (InputStream in = CrudContract.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing " + resource);
            }
            return JSON.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
