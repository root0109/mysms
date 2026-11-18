package com.mysms.lms;

import static com.mysms.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.lms.LmsFixtures.Reply;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The question bank over HTTP against real PostgreSQL: SearchQuestionBank's matching, type filter, quiz exclusion and
 * tenant isolation, and ImportQuestions' refusals — an unsupported format, a missing pool, a school-level grant, and
 * the platform file store not running here (503, nothing imported).
 */
@SpringBootTest(classes = LmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuestionBankComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String SEARCH = "/v1/lms/questions:search";
    private static final String IMPORT = "/v1/lms/questions:import";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private LmsFixtures fx;
    private UUID tenant;
    private String admin;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void school() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        admin = fx.admin(tenant);
    }

    private UUID question(String type, String text) {
        UUID question = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.question (id, tenant_id, question_type, question_text, marks) "
                + "VALUES (:id, :tenantId, :type, :text, 1)", "id", question, "type", type, "text", text);
        return question;
    }

    @Test
    void searchMatchesTextFiltersTypesAndExcludesAQuizsQuestions() throws Exception {
        UUID pythagoras = question("choices", "State the Pythagorean theorem");
        question("open_ended", "Describe Pythagoras of Samos");
        question("choices", "Name the capital of France");
        Reply byText = expect(fx.call("GET", SEARCH + "?query=pythagor", admin, null), 200);
        assertThat(byText.body().path("data")).hasSize(2);
        Reply byType = expect(fx.call("GET", SEARCH + "?query=pythagor&questionTypes=choices", admin, null), 200);
        assertThat(byType.body().path("data")).hasSize(1);
        assertThat(byType.body().path("data").get(0).path("id").asString()).isEqualTo(pythagoras.toString());
        UUID quiz = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.quiz (id, tenant_id, title, passing_percentage, total_marks) "
                + "VALUES (:id, :tenantId, 'Quiz', 50, 10)", "id", quiz);
        fx.sql(tenant, "INSERT INTO lms.quiz_question (id, tenant_id, quiz_id, question_id, marks) "
                + "VALUES (:id, :tenantId, :quiz, :question, 1)", "id", UUID.randomUUID(), "quiz", quiz,
                "question", pythagoras);
        Reply excluded = expect(fx.call("GET", SEARCH + "?query=pythagor&excludeQuizId=" + quiz, admin, null), 200);
        assertThat(excluded.body().path("data")).hasSize(1);
        assertThat(excluded.body().path("data").get(0).path("id").asString()).isNotEqualTo(pythagoras.toString());
        expect(fx.call("GET", SEARCH + "?questionTypes=nonsense", admin, null), 422);
        expect(fx.call("GET", SEARCH, fx.member(tenant, UUID.randomUUID(), "lms.ebook.read"), null), 403);
        Reply stranger = expect(fx.call("GET", SEARCH + "?query=pythagor", fx.admin(UUID.randomUUID()), null), 200);
        assertThat(stranger.body().path("data")).isEmpty();
    }

    @Test
    void importRefusesWhatItMustAndStopsAtTheMissingFileStore() throws Exception {
        String base = "{\"fileId\":\"" + UUID.randomUUID() + "\"";
        Reply qti = expect(fx.call("POST", IMPORT, admin, base + ",\"format\":\"qti\"}"), 422);
        assertThat(qti.body().path("errors").get(0).path("field").asString()).isEqualTo("format");
        expect(fx.call("POST", IMPORT, fx.member(tenant, UUID.randomUUID(), "lms.question.import_questions"),
                base + ",\"format\":\"gift\"}"), 403);
        expect(fx.call("POST", IMPORT, admin, base + ",\"format\":\"gift\",\"questionPoolId\":\""
                + UUID.randomUUID() + "\"}"), 422);
        // the file itself lives in platform's store, which is not running here
        expect(fx.call("POST", IMPORT, admin, base + ",\"format\":\"gift\"}"), 503);
        assertThat(fx.value(tenant, "SELECT count(*) FROM lms.question", Integer.class)).isZero();
    }
}
