package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * The processing register against real PostgreSQL: the seeded catalogue is listed with each activity's purpose, basis
 * and categories, filters by legal basis, needs its own permission, and never lets an activity that reaches minors
 * serve a purpose forbidden for minors (design §5.1's rule P1, held by the seed until the manifests' build check).
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProcessingActivityComponentTest {

    /** The activities privacy's baseline seeds (design §8). */
    private static final int REGISTER = 18;
    private static final String ACTIVITIES = "/v1/privacy/processing-activities";
    private static final String READ = "privacy.processing_activity.read";
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PrivacyDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private PrivacyCalls calls;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void connect() {
        calls = new PrivacyCalls(port, TOKENS);
    }

    @Test
    void theRegisterListsEveryActivityWithItsPurposeBasisAndCategories() throws Exception {
        PrivacyCalls.Reply list = calls.get(ACTIVITIES + "?size=500", calls.administrator(UUID.randomUUID(), READ));

        assertThat(list.status()).as(list.body().toString()).isEqualTo(200);
        assertThat(list.body().path("data")).hasSize(REGISTER);
        JsonNode marketing = byCode(list.body(), "comms.marketing_broadcast");
        assertThat(marketing.path("module").asString()).isEqualTo("comms");
        assertThat(marketing.path("purposeCode").asString()).isEqualTo("marketing_comms");
        assertThat(marketing.path("legalBasis").asString()).isEqualTo("consent");
        assertThat(marketing.path("targetsMinors").asBoolean()).isFalse();
        assertThat(texts(marketing.path("categories"))).containsExactly("contact");
        assertThat(texts(byCode(list.body(), "frontoffice.visitor_log").path("categories")))
                .containsExactly("government_id", "identity", "photo_media");
    }

    @Test
    void theRegisterFiltersByLegalBasisAndNeedsItsOwnPermission() throws Exception {
        PrivacyCalls.Reply consent = calls.get(ACTIVITIES + "?size=500&filter=legalBasis%20%3D%20%22consent%22",
                calls.administrator(UUID.randomUUID(), READ));
        assertThat(consent.status()).as(consent.body().toString()).isEqualTo(200);
        assertThat(consent.body().path("data")).hasSize(12);
        assertThat(consent.body().path("data").findValuesAsString("legalBasis")).containsOnly("consent");

        assertThat(calls.get(ACTIVITIES, calls.administrator(UUID.randomUUID(), "privacy.tenant_purpose.read"))
                .status()).isEqualTo(403);
    }

    @Test
    void theRegisterExportsAsCsvOneRowPerActivity() throws Exception {
        PrivacyCalls.Reply export = calls.get(ACTIVITIES + ":export", calls.administrator(UUID.randomUUID(), READ));

        assertThat(export.status()).as(export.body().toString()).isEqualTo(200);
        assertThat(export.text("fileName")).matches("processing-register-\\d{4}-\\d{2}-\\d{2}\\.csv");
        assertThat(export.body().path("activities").asInt()).isEqualTo(REGISTER);
        List<String> rows = List.of(export.text("csv").split("\r\n"));
        assertThat(rows).hasSize(REGISTER + 1);
        assertThat(rows.get(0)).isEqualTo("activity_code,module,purpose,legal_basis,education_exemption_clause,"
                + "targets_minors,audiences,data_categories,recipients,cross_border,retention_policy");
        assertThat(rows).contains("comms.marketing_broadcast,comms,marketing_comms,consent,,false,student; guardian,"
                + "contact,,false,");
        assertThat(calls.get(ACTIVITIES + ":export", calls.administrator(UUID.randomUUID(),
                "privacy.tenant_purpose.read")).status()).isEqualTo(403);
    }

    @Test
    void noActivityThatReachesMinorsServesAPurposeForbiddenForMinors() {
        int violations = TestTransactions.of(database).readWithoutTenant(() -> database.jdbc().sql("""
                SELECT count(*)
                  FROM privacy.processing_activity a
                  JOIN privacy.purpose p ON p.id = a.purpose_id
                 WHERE a.targets_minors AND p.forbidden_for_minors
                """).query(Integer.class).single());
        assertThat(violations).isZero();
    }

    private static List<String> texts(JsonNode array) {
        List<String> texts = new ArrayList<>();
        array.forEach(item -> texts.add(item.asString()));
        return texts;
    }

    private static JsonNode byCode(JsonNode list, String code) {
        for (JsonNode item : list.path("data")) {
            if (code.equals(item.path("code").asString())) {
                return item;
            }
        }
        throw new AssertionError(code + " is not listed in " + list);
    }
}
