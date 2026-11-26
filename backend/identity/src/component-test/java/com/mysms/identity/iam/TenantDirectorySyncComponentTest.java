package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.events.TenantDirectorySync;
import com.mysms.identity.iam.repository.TenantDirectoryRepository;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real PostgreSQL: iam's tenant directory follows platform.tenant from its events — the newest row version wins, a late
 * or repeated event changes nothing, a deleted tenant is closed, and a malformed event is rejected (parked).
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantDirectorySyncComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private TenantDirectorySync sync;

    @Autowired
    private TenantDirectoryRepository directory;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam", "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
    }

    private static EventEnvelope event(UUID tenant, String verb, String code, String status, int version) {
        String data = "{\"id\":\"" + tenant + "\",\"rowVersion\":" + version
                + (code == null ? "" : ",\"code\":\"" + code + "\"") + ",\"status\":\"" + status + "\"}";
        return new EventEnvelope(UUID.randomUUID(), "platform", "platform.tenant." + verb, Optional.empty(),
                "platform.tenant", tenant, Instant.now(), data, Optional.empty());
    }

    private TenantDirectoryRepository.Entry entry(UUID tenant) {
        return TestTransactions.of(database).readWithoutTenant(() -> directory.find(tenant)).orElseThrow();
    }

    @Test
    void theDirectoryFollowsTheNewestTenantVersionAndClosesADeletedTenant() {
        UUID tenant = UUID.randomUUID();
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        inbox.deliver(event(tenant, "created", "greenfield", "active", 1), sync::apply);
        assertThat(entry(tenant)).isEqualTo(new TenantDirectoryRepository.Entry("greenfield", "active", 1));
        assertThat(TestTransactions.of(database).readWithoutTenant(() -> directory.findActiveTenant("GreenField")))
                .contains(tenant);

        assertThat(sync.apply(event(tenant, "updated", "greenfield", "suspended", 3))).isTrue();
        assertThat(sync.apply(event(tenant, "updated", "greenfield", "active", 2)))
                .as("a late event never restores an older status").isFalse();
        assertThat(sync.apply(event(tenant, "updated", "greenfield", "suspended", 3))).as("a repeat").isFalse();
        assertThat(entry(tenant)).isEqualTo(new TenantDirectoryRepository.Entry("greenfield", "suspended", 3));
        assertThat(TestTransactions.of(database).readWithoutTenant(() -> directory.findActiveTenant("greenfield")))
                .as("a suspended tenant cannot sign in").isEmpty();

        assertThat(sync.apply(event(tenant, "deleted", "greenfield", "active", 4))).isTrue();
        assertThat(entry(tenant).status()).isEqualTo("closed");

        assertThatThrownBy(() -> sync.apply(event(UUID.randomUUID(), "created", null, "active", 1)))
                .isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(event(UUID.randomUUID(), "created", "x", "dormant", 1)))
                .isInstanceOf(RejectedEventException.class);
        assertThat(sync.module()).isEqualTo("iam");
        assertThat(sync.handlers()).containsOnlyKeys("platform.tenant.created", "platform.tenant.updated",
                "platform.tenant.deleted");
    }
}
