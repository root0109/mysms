package com.mysms.core.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.IdempotencyStore;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.error.Problems;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.core.trace.TraceFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Idempotency-Key machinery against real PostgreSQL as the non-owner application role (row-level security on):
 * run once, replay the same request, refuse a reused key, hold a running request, retry after a server failure,
 * keep the response only encrypted, and leave every request it does not apply to alone.
 */
class IdempotencyFilterComponentTest {

    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000000a1");
    private static final UUID OTHER_TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000000a2");
    private static final UUID USER = UUID.fromString("0190b0a0-0000-7000-8000-0000000000ee");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final FieldCipher CIPHER = FieldCipher.fromSettings(
            "k1:" + Base64.getEncoder().encodeToString(new byte[32]), null);

    private static TestDatabase database;
    private static ModuleDatabase db;
    private static IdempotencyFilter filter;
    private final AtomicInteger runs = new AtomicInteger();

    @BeforeAll
    static void open() {
        database = TestDatabase.create();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("mysms.coretest.datasource.url", database.jdbcUrl())
                .withProperty("mysms.coretest.datasource.username", database.username())
                .withProperty("mysms.coretest.datasource.password", database.password())
                .withProperty("mysms.coretest.datasource.migrate-username", database.migrateUsername())
                .withProperty("mysms.coretest.datasource.migrate-password", database.migratePassword());
        db = ModuleDatabase.open("coretest", environment, true);
        filter = new IdempotencyFilter(List.of(new IdempotencyStore(db, Clock.systemUTC())), CIPHER,
                new ProblemResponseWriter(JSON), JSON, Clock.systemUTC());
    }

    @AfterAll
    static void close() {
        db.close();
        database.close();
    }

    @AfterEach
    void clear() {
        CallContextHolder.clear();
    }

    private static MockHttpServletRequest post(String key, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/coretest/items");
        request.setAttribute(Problems.OPERATION_ATTRIBUTE, "coretest.CreateItem");
        request.setAttribute(IdempotencyFilter.ATTRIBUTE, "optional");
        if (key != null) {
            request.addHeader(IdempotencyFilter.HEADER, key);
        }
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    /** The controller: echoes the body it can still read, counts how often it ran. */
    private FilterChain created(int status) {
        return (req, res) -> {
            runs.incrementAndGet();
            String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            HttpServletResponse http = (HttpServletResponse) res;
            http.setStatus(status);
            http.setHeader("Location", "/v1/coretest/items/1");
            http.setHeader("ETag", "\"1\"");
            http.setContentType("application/json");
            http.getOutputStream().write(("{\"echo\":" + body + ",\"run\":" + runs.get() + "}")
                    .getBytes(StandardCharsets.UTF_8));
        };
    }

    private MockHttpServletResponse send(MockHttpServletRequest request, FilterChain chain) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private static void as(UUID tenant) {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, USER));
    }

    @Test
    void theFirstRequestRunsAndTheSameRequestIsAnsweredFromTheStore() throws Exception {
        as(TENANT);
        String key = UUID.randomUUID().toString();
        MockHttpServletResponse first = send(post(key, "{\"a\":1}"), created(201));
        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(first.getContentAsString()).isEqualTo("{\"echo\":{\"a\":1},\"run\":1}");
        MockHttpServletResponse again = send(post(key, "{\"a\":1}"), created(201));
        assertThat(runs.get()).as("the retry did not run again").isEqualTo(1);
        assertThat(again.getStatus()).isEqualTo(201);
        assertThat(again.getContentAsString()).isEqualTo(first.getContentAsString());
        assertThat(again.getHeader("Location")).isEqualTo("/v1/coretest/items/1");
        assertThat(again.getHeader("ETag")).isEqualTo("\"1\"");
        assertThat(again.getHeader("Idempotent-Replayed")).isEqualTo("true");
        String stored = TestTransactions.of(db).read(() -> db.jdbc().sql("SELECT response_body FROM coretest.idempotency_key "
                + "WHERE tenant_id = :tenantId AND idempotency_key = :key").param("key", UUID.fromString(key))
                .query(String.class).single());
        assertThat(stored).as("kept only encrypted").startsWith("v1.k1.").doesNotContain("echo");

        MockHttpServletResponse reused = send(post(key, "{\"a\":2}"), created(201));
        assertThat(reused.getStatus()).isEqualTo(422);
        assertThat(reused.getContentAsString()).contains("IDEMPOTENCY_KEY_REUSED");
        // the same key in another tenant is another key
        as(OTHER_TENANT);
        assertThat(send(post(key, "{\"a\":2}"), created(201)).getStatus()).isEqualTo(201);
        assertThat(runs.get()).isEqualTo(2);
    }

    @Test
    void aRunningRequestHoldsItsKeyAndAServerFailureFreesIt() throws Exception {
        as(TENANT);
        UUID key = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        IdempotencyStore store = new IdempotencyStore(db, Clock.systemUTC());
        String hash = "0".repeat(64);
        assertThat(store.claim(key, USER, "coretest.CreateItem", hash, now, now.plusHours(1), now.minusMinutes(2))
                .outcome()).isEqualTo(IdempotencyStore.Outcome.RUN);
        // a claim of the same key by a request with another body while it runs: in progress is only for the same
        // request, so this is a reuse
        MockHttpServletResponse other = send(post(key.toString(), "{}"), created(201));
        assertThat(other.getStatus()).isEqualTo(422);
        IdempotencyStore.Claim running = store.claim(key, USER, "coretest.CreateItem", hash, now, now.plusHours(1),
                now.minusMinutes(2));
        assertThat(running.outcome()).isEqualTo(IdempotencyStore.Outcome.IN_PROGRESS);
        // a lock older than the stale limit is taken over
        assertThat(store.claim(key, USER, "coretest.CreateItem", hash, now.plusMinutes(5), now.plusHours(1),
                now.plusMinutes(3)).outcome()).isEqualTo(IdempotencyStore.Outcome.RUN);

        String failing = UUID.randomUUID().toString();
        MockHttpServletResponse failed = send(post(failing, "{\"b\":1}"), created(503));
        assertThat(failed.getStatus()).isEqualTo(503);
        MockHttpServletResponse retried = send(post(failing, "{\"b\":1}"), created(201));
        assertThat(retried.getStatus()).as("a server failure is not kept: the retry runs").isEqualTo(201);
        assertThat(runs.get()).isEqualTo(2);

        String throwing = UUID.randomUUID().toString();
        assertThatThrownBy(() -> send(post(throwing, "{}"), (req, res) -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(send(post(throwing, "{}"), created(201)).getStatus()).isEqualTo(201);

        String duplicate = UUID.randomUUID().toString();
        assertThat(send(post(duplicate, "{}"), created(409)).getStatus()).isEqualTo(409);
        assertThat(send(post(duplicate, "{}"), created(201)).getStatus()).as("a 4xx answer is kept").isEqualTo(409);

        String throttled = UUID.randomUUID().toString();
        assertThat(send(post(throttled, "{}"), created(429)).getStatus()).isEqualTo(429);
        assertThat(send(post(throttled, "{}"), created(201)).getStatus()).as("try-again answers are not kept")
                .isEqualTo(201);
    }

    @Test
    void aLargeResponseIsAnsweredButNotKeptAndExpiredKeysArePurged() throws Exception {
        as(TENANT);
        String key = UUID.randomUUID().toString();
        FilterChain large = (req, res) -> {
            runs.incrementAndGet();
            HttpServletResponse http = (HttpServletResponse) res;
            http.setStatus(201);
            http.setHeader("Location", "/v1/coretest/items/9");
            http.getOutputStream().write(new byte[IdempotencyFilter.MAX_STORED_BODY + 1]);
        };
        MockHttpServletResponse first = send(post(key, "{}"), large);
        assertThat(first.getContentAsByteArray()).hasSize(IdempotencyFilter.MAX_STORED_BODY + 1);
        MockHttpServletResponse replay = send(post(key, "{}"), large);
        assertThat(runs.get()).isEqualTo(1);
        assertThat(replay.getStatus()).isEqualTo(201);
        assertThat(replay.getHeader("Location")).isEqualTo("/v1/coretest/items/9");
        assertThat(replay.getContentAsByteArray()).as("only status and headers are kept").isEmpty();

        // a key past its 24 hours is removed when the tenant claims another key
        TestTransactions.of(db).write(() -> db.jdbc()
                .sql("UPDATE coretest.idempotency_key SET expires_at = now() - interval '1 hour' "
                        + "WHERE tenant_id = :tenantId AND idempotency_key = :key")
                .param("key", UUID.fromString(key)).update());
        send(post(UUID.randomUUID().toString(), "{}"), created(201));
        assertThat(TestTransactions.of(db).read(() -> db.jdbc().sql("SELECT count(*) FROM coretest.idempotency_key "
                + "WHERE tenant_id = :tenantId AND idempotency_key = :key").param("key", UUID.fromString(key))
                .query(Integer.class).single())).isZero();
    }

    @Test
    void requestsItDoesNotApplyToPassThrough() throws Exception {
        as(TENANT);
        assertThat(send(post(null, "{}"), created(201)).getHeader("Idempotent-Replayed")).isNull();
        assertThat(send(post("not-a-uuid", "{}"), created(201)).getStatus()).isEqualTo(201);
        MockHttpServletRequest undeclared = post(UUID.randomUUID().toString(), "{}");
        undeclared.removeAttribute(IdempotencyFilter.ATTRIBUTE);
        assertThat(send(undeclared, created(201)).getStatus()).isEqualTo(201);
        MockHttpServletRequest get = post(UUID.randomUUID().toString(), "");
        get.setMethod("GET");
        assertThat(send(get, created(200)).getStatus()).isEqualTo(200);
        MockHttpServletRequest otherModule = post(UUID.randomUUID().toString(), "{}");
        otherModule.setAttribute(Problems.OPERATION_ATTRIBUTE, "elsewhere.CreateThing");
        assertThat(send(otherModule, created(201)).getStatus()).isEqualTo(201);
        assertThat(runs.get()).isEqualTo(5);
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        String key = UUID.randomUUID().toString();
        send(post(key, "{}"), created(201));
        send(post(key, "{}"), created(201));
        assertThat(runs.get()).as("without a tenant nothing is kept").isEqualTo(7);
        assertThat(filter.getOrder()).isGreaterThan(TraceFilter.ORDER);

        as(TENANT);
        MockHttpServletRequest huge = post(UUID.randomUUID().toString(), "");
        huge.setContent(new byte[IdempotencyFilter.MAX_BODY + 1]);
        assertThat(send(huge, created(201)).getStatus()).isEqualTo(413);
        assertThat(JSON.readTree(send(post(UUID.randomUUID().toString(), "{\"x\":\"é\"}"), created(201))
                .getContentAsByteArray()).path("run").asInt()).isEqualTo(8);
    }

    @Test
    void anOperationThatRequiresAKeyRefusesARequestWithoutOne() throws Exception {
        as(TENANT);
        MockHttpServletRequest missing = post(null, "{}");
        missing.setAttribute(IdempotencyFilter.ATTRIBUTE, IdempotencyFilter.REQUIRED);
        MockHttpServletResponse refused = send(missing, created(201));
        assertThat(refused.getStatus()).isEqualTo(400);
        assertThat(JSON.readTree(refused.getContentAsByteArray()).path("errors").toString()
                + refused.getContentAsString()).contains("INVALID_ARGUMENT");
        MockHttpServletRequest blank = post(" ", "{}");
        blank.setAttribute(IdempotencyFilter.ATTRIBUTE, IdempotencyFilter.REQUIRED);
        assertThat(send(blank, created(201)).getStatus()).isEqualTo(400);
        assertThat(runs.get()).isZero();
        MockHttpServletRequest given = post(UUID.randomUUID().toString(), "{}");
        given.setAttribute(IdempotencyFilter.ATTRIBUTE, IdempotencyFilter.REQUIRED);
        assertThat(send(given, created(201)).getStatus()).isEqualTo(201);
        assertThat(runs.get()).isEqualTo(1);
    }
}
