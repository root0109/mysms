package com.mysms.core.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.json.JsonMappers;
import com.mysms.core.testkit.TestDatabase;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;

/**
 * A sibling area's operation called in process with a {@code request_id}, against real PostgreSQL as the application
 * role: run once and replay the answer, refuse a reused key, keep a refusal but not a server failure, refuse a kept
 * row it cannot read back, and run every time without a tenant.
 */
class InProcessIdempotencyComponentTest {

    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000000c1");
    private static final UUID USER = UUID.fromString("0190b0a0-0000-7000-8000-0000000000cf");
    private static final String OPERATION = "coretest.CreateWidget";
    private static final FieldCipher CIPHER = FieldCipher.fromSettings(
            "k1:" + Base64.getEncoder().encodeToString(new byte[32]), null);

    /** The callee's answer. */
    public record Widget(String id, String name) {
    }

    private static TestDatabase database;
    private static ModuleDatabase db;
    private static IdempotencyLedger ledger;
    private static InProcessIdempotency calls;
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
        ledger = new IdempotencyLedger(List.of(new IdempotencyStore(db, Clock.systemUTC())), CIPHER,
                JsonMappers.create(), Clock.systemUTC());
        calls = new InProcessIdempotency(ledger, JsonMappers.create());
    }

    @AfterAll
    static void close() {
        db.close();
        database.close();
    }

    private static <T> T asCaller(Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736")
                .withIdentity(TENANT, USER), work);
    }

    private Widget create(UUID key, String name) {
        return calls.call(OPERATION, key, Map.of("name", name), Widget.class, () -> {
            runs.incrementAndGet();
            if ("refused".equals(name)) {
                throw new ValidationFailedException(List.of(new FieldError("name", "is refused", "REFUSED")));
            }
            if ("boom".equals(name)) {
                throw new IllegalStateException("failed on the server");
            }
            return new Widget(UUID.randomUUID().toString(), name);
        });
    }

    @Test
    void aKeyRunsOnceAndReplaysTheAnswerAndAReusedKeyIsRefused() {
        UUID key = UUID.randomUUID();
        Widget first = asCaller(() -> create(key, "Pump"));
        Widget again = asCaller(() -> create(key, "Pump"));
        assertThat(again).as("the kept answer").isEqualTo(first);
        assertThat(runs.get()).as("ran once").isEqualTo(1);

        assertThatThrownBy(() -> asCaller(() -> create(key, "Valve"))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(e.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        });
        assertThat(runs.get()).isEqualTo(1);
    }

    @Test
    void aRefusalIsKeptAndAServerFailureFreesTheKey() {
        UUID refusedKey = UUID.randomUUID();
        assertThatThrownBy(() -> asCaller(() -> create(refusedKey, "refused")))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> asCaller(() -> create(refusedKey, "refused"))).as("the kept refusal")
                .isInstanceOfSatisfying(InProcessIdempotency.KeptRefusal.class, e -> {
                    assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                    assertThat(e.errors()).singleElement().satisfies(error -> {
                        assertThat(error.field()).isEqualTo("name");
                        assertThat(error.code()).isEqualTo("REFUSED");
                    });
                });
        assertThat(runs.get()).isEqualTo(1);

        UUID failedKey = UUID.randomUUID();
        assertThatThrownBy(() -> asCaller(() -> create(failedKey, "boom"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> asCaller(() -> create(failedKey, "boom"))).as("run again")
                .isInstanceOf(IllegalStateException.class);
        assertThat(runs.get()).isEqualTo(3);
    }

    @Test
    void aKeptRowInAnotherFormIsRefusedAsAReusedKey() {
        UUID key = UUID.randomUUID();
        asCaller(() -> {
            IdempotencyLedger.Ticket ticket = ledger.claim(ledger.storeFor(OPERATION).orElseThrow(), OPERATION, key,
                    TENANT, USER, IdempotencyLedger.hash("CALL\n" + OPERATION + "\n" + USER + "\n",
                            JsonMappers.create().writeValueAsBytes(Map.of("name", "Pump"))));
            ledger.complete(ticket, HttpStatus.OK.value(), Map.of("message", "AAAA"));
            return ticket;
        });

        assertThatThrownBy(() -> asCaller(() -> create(key, "Pump"))).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(e.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        });
        assertThat(runs.get()).isZero();
    }

    @Test
    void withoutATenantOrAStoreEveryCallRuns() {
        UUID key = UUID.randomUUID();
        CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"), () -> create(key, "Pump"));
        CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"), () -> create(key, "Pump"));
        asCaller(() -> calls.call("master.CreateWidget", key, Map.of(), Widget.class, () -> {
            runs.incrementAndGet();
            return new Widget("1", "x");
        }));
        assertThat(runs.get()).isEqualTo(3);
    }
}
