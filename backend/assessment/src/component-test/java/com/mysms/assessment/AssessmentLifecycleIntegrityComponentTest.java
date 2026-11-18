package com.mysms.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.ExamResultEntity;
import com.mysms.assessment.repository.ExamResultRepository;
import com.mysms.assessment.repository.MarksheetRepository;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Marks aggregated results immutable and revokes publication metadata during marksheet cancellation/reset. */
@SpringBootTest(classes = AssessmentApplication.class)
class AssessmentLifecycleIntegrityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(AssessmentDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private ExamResultRepository examResults;

    @Autowired
    private MarksheetRepository marksheets;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @Test
    void recordMarksSqlCannotChangeAnExamResultAlreadyLinkedToAMarksheet() {
        Fixture fixture = seed();
        CallContext context = CallContext.anonymous(TRACE).withIdentity(fixture.tenantId(), null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            ExamResultEntity current = ExamResultEntity.builder().id(fixture.examResultId())
                    .marks(new BigDecimal("80.00")).resultStatus("pass").rowVersion(0).build();
            int updated = examResults.recordMarks(current, 0, "present", new BigDecimal("90.00"),
                    new BigDecimal("100.00"), "pass", null, fixture.actor(), OffsetDateTime.now(), "correction");

            assertThat(updated).isZero();
            assertThat(database.jdbc().sql("SELECT marks FROM assessment.exam_result "
                    + "WHERE tenant_id = :tenantId AND id = :id").param("id", fixture.examResultId())
                    .query(BigDecimal.class).single()).isEqualByComparingTo("80.00");
            assertThat(database.jdbc().sql("SELECT row_version FROM assessment.exam_result "
                    + "WHERE tenant_id = :tenantId AND id = :id").param("id", fixture.examResultId())
                    .query(Integer.class).single()).isZero();
            return null;
        }));
    }

    @Test
    void cancellationAndResetClearPublicationAndGuardianVisibility() {
        Fixture fixture = seed();
        CallContext context = CallContext.anonymous(TRACE).withIdentity(fixture.tenantId(), null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            OffsetDateTime now = OffsetDateTime.now();
            assertThat(marksheets.cancelMarksheet(fixture.marksheetId(), 0, now, fixture.actor())).isEqualTo(1);
            assertPublicationRevoked(fixture.marksheetId());

            database.jdbc().sql("UPDATE assessment.marksheet SET published_at = :publishedAt, "
                            + "published_by_user_id = :actor, visible_to_guardians = true "
                            + "WHERE tenant_id = :tenantId AND id = :id")
                    .param("publishedAt", now).param("actor", fixture.actor()).param("id", fixture.marksheetId()).update();
            assertThat(marksheets.resetMarksheet(fixture.marksheetId(), 1, now.plusSeconds(1), fixture.actor()))
                    .isEqualTo(1);
            assertPublicationRevoked(fixture.marksheetId());
            return null;
        }));
    }

    private Fixture seed() {
        Fixture fixture = new Fixture(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID());
        CallContext context = CallContext.anonymous(TRACE).withIdentity(fixture.tenantId(), null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            database.jdbc().sql("INSERT INTO assessment.exam_type (id, tenant_id, code, name) "
                            + "VALUES (:id, :tenantId, :code, 'Lifecycle test')")
                    .param("id", fixture.examTypeId()).param("code", UUID.randomUUID().toString().substring(0, 8)).update();
            database.jdbc().sql("INSERT INTO assessment.exam_session "
                            + "(id, tenant_id, institution_id, exam_type_id, code, name, grade_level_id, start_date, end_date) "
                            + "VALUES (:id, :tenantId, :institution, :examTypeId, :code, 'Lifecycle test', :gradeLevel, "
                            + ":startDate, :endDate)")
                    .param("id", fixture.examSessionId()).param("institution", UUID.randomUUID())
                    .param("examTypeId", fixture.examTypeId())
                    .param("code", UUID.randomUUID().toString().substring(0, 8))
                    .param("gradeLevel", UUID.randomUUID()).param("startDate", LocalDate.parse("2026-01-01"))
                    .param("endDate", LocalDate.parse("2026-01-01")).update();
            database.jdbc().sql("INSERT INTO assessment.result_template "
                            + "(id, tenant_id, exam_session_id, name, result_date) VALUES (:id, :tenantId, :session, "
                            + "'Lifecycle test', :resultDate)")
                    .param("id", fixture.templateId()).param("session", fixture.examSessionId())
                    .param("resultDate", LocalDate.parse("2026-01-02")).update();
            database.jdbc().sql("INSERT INTO assessment.marksheet "
                            + "(id, tenant_id, exam_session_id, result_template_id, name, generated_on, "
                            + "generated_by_user_id, status, published_at, published_by_user_id, visible_to_guardians) "
                            + "VALUES (:id, :tenantId, :session, :template, 'Lifecycle test', :generatedOn, :actor, "
                            + "'validated', :publishedAt, :actor, true)")
                    .param("id", fixture.marksheetId()).param("session", fixture.examSessionId())
                    .param("template", fixture.templateId()).param("generatedOn", LocalDate.parse("2026-01-02"))
                    .param("actor", fixture.actor()).param("publishedAt", OffsetDateTime.now()).update();
            database.jdbc().sql("INSERT INTO assessment.marksheet_line "
                            + "(id, tenant_id, marksheet_id, student_id, result_status, generated_on) "
                            + "VALUES (:id, :tenantId, :marksheet, :student, 'pass', :generatedOn)")
                    .param("id", fixture.lineId()).param("marksheet", fixture.marksheetId())
                    .param("student", fixture.studentId()).param("generatedOn", LocalDate.parse("2026-01-02")).update();
            database.jdbc().sql("INSERT INTO assessment.exam "
                            + "(id, tenant_id, exam_session_id, subject_id, code, name, start_at, end_at, total_marks, "
                            + "pass_marks, status) VALUES (:id, :tenantId, :session, :subject, :code, 'Lifecycle test', "
                            + ":startAt, :endAt, 100, 50, 'result_updated')")
                    .param("id", fixture.examId()).param("session", fixture.examSessionId())
                    .param("subject", UUID.randomUUID()).param("code", UUID.randomUUID().toString().substring(0, 8))
                    .param("startAt", OffsetDateTime.parse("2026-01-01T09:00:00Z"))
                    .param("endAt", OffsetDateTime.parse("2026-01-01T10:00:00Z")).update();
            database.jdbc().sql("INSERT INTO assessment.exam_result "
                            + "(id, tenant_id, exam_id, student_id, attendance_status, marks, max_marks, grade_label, "
                            + "grade_point, credits, result_status, marksheet_line_id) VALUES (:id, :tenantId, :exam, "
                            + ":student, 'present', 80, 100, 'A', 4, 3, 'pass', :line)")
                    .param("id", fixture.examResultId()).param("exam", fixture.examId())
                    .param("student", fixture.studentId()).param("line", fixture.lineId()).update();
            return null;
        }));
        return fixture;
    }

    private void assertPublicationRevoked(UUID marksheetId) {
        Boolean revoked = database.jdbc().sql("SELECT published_at IS NULL AND published_by_user_id IS NULL "
                        + "AND visible_to_guardians = false FROM assessment.marksheet "
                        + "WHERE tenant_id = :tenantId AND id = :id")
                .param("id", marksheetId).query(Boolean.class).single();
        assertThat(revoked).isTrue();
    }

    private record Fixture(UUID tenantId, UUID examTypeId, UUID examSessionId, UUID templateId, UUID marksheetId,
            UUID lineId, UUID examId, UUID examResultId, UUID studentId, UUID actor) { }
}
