package com.mysms.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestTransactions;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the assessment workflow component tests share: signed tokens, calls over HTTP to the running service, and the
 * rows a test starts from, written with plain SQL as the application role inside the test's own tenant (the rows the
 * workflow operations then act on through the API).
 */
final class AssessmentFixtures {

    static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    private final TestTokens tokens;
    private final ModuleDatabase database;
    private final int port;

    AssessmentFixtures(TestTokens tokens, ModuleDatabase database, int port) {
        this.tokens = tokens;
        this.database = database;
        this.port = port;
    }

    record Reply(int status, JsonNode body, String text) {
    }

    /** A tenant user's token: the institutions it reaches ("*" for all), its tenant-wide permissions and its own. */
    String token(UUID user, UUID tenant, List<String> institutions, List<String> tenantWide, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", tenantWide);
        claims.put("ins", institutions);
        return tokens.custom(claims, Instant.now().plusSeconds(900));
    }

    /** Every permission in every institution of the tenant. */
    String admin(UUID tenant) {
        return token(UUID.randomUUID(), tenant, List.of("*"), List.of("*"), "*");
    }

    /** The given permissions in one institution only. */
    String member(UUID tenant, UUID institution, String... permissions) {
        return token(UUID.randomUUID(), tenant, List.of(institution.toString()), List.of(), permissions);
    }

    Reply call(String method, String path, String token, String body) throws Exception {
        return call(method, path, token, body, null);
    }

    Reply call(String method, String path, String token, String body, String ifMatch) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (ifMatch != null) {
            request.header("If-Match", ifMatch);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    static Reply expect(Reply reply, int status) {
        assertThat(reply.status()).as(reply.text()).isEqualTo(status);
        return reply;
    }

    static String json(Object value) {
        return JSON.writeValueAsString(value);
    }

    // ---- seeded rows

    /** Runs SQL work in the tenant, as the application role, in one committed transaction. */
    <T> T inTenant(UUID tenant, Supplier<T> work) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        return CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(work::get));
    }

    /** One value read with SQL in the tenant. */
    <T> T value(UUID tenant, String sql, Class<T> type, Object... namedParams) {
        return inTenant(tenant, () -> {
            var statement = database.jdbc().sql(sql);
            for (int i = 0; i < namedParams.length; i += 2) {
                statement = statement.param((String) namedParams[i], namedParams[i + 1]);
            }
            return statement.query(type).optional().orElse(null);
        });
    }

    /** An exam session of the institution (its exam type with it). */
    UUID session(UUID tenant, UUID institution) {
        UUID type = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        inTenant(tenant, () -> {
            database.jdbc().sql("INSERT INTO assessment.exam_type (id, tenant_id, code, name) "
                    + "VALUES (:id, :tenantId, :code, 'Term exams')").param("id", type).param("code", code()).update();
            database.jdbc().sql("INSERT INTO assessment.exam_session (id, tenant_id, institution_id, exam_type_id, code, "
                            + "name, grade_level_id, start_date, end_date) VALUES (:id, :tenantId, :institution, :type, "
                            + ":code, 'Term 1', :gradeLevel, :start, :end)")
                    .param("id", session).param("institution", institution).param("type", type).param("code", code())
                    .param("gradeLevel", UUID.randomUUID()).param("start", LocalDate.parse("2026-03-01"))
                    .param("end", LocalDate.parse("2026-03-10")).update();
            return null;
        });
        return session;
    }

    /** A paper of the session in the given state, marked out of totalMarks with passMarks to pass. */
    UUID exam(UUID tenant, UUID session, String status, String totalMarks, String passMarks) {
        UUID exam = UUID.randomUUID();
        inTenant(tenant, () -> database.jdbc().sql("INSERT INTO assessment.exam (id, tenant_id, exam_session_id, "
                        + "subject_id, class_section_id, code, name, start_at, end_at, total_marks, pass_marks, status) "
                        + "VALUES (:id, :tenantId, :session, :subject, :section, :code, 'Paper', :start, :end, :total, "
                        + ":pass, :status)")
                .param("id", exam).param("session", session).param("subject", UUID.randomUUID())
                .param("section", UUID.randomUUID()).param("code", code())
                .param("start", OffsetDateTime.parse("2026-03-02T09:00:00Z"))
                .param("end", OffsetDateTime.parse("2026-03-02T12:00:00Z"))
                .param("total", new BigDecimal(totalMarks)).param("pass", new BigDecimal(passMarks))
                .param("status", status).update());
        return exam;
    }

    /** A registered attendee as RecordExamMarks leaves one (marks and result status may be null). */
    UUID result(UUID tenant, UUID exam, UUID student, String attendance, String marks, String maxMarks,
            String resultStatus, String credits) {
        UUID result = UUID.randomUUID();
        inTenant(tenant, () -> database.jdbc().sql("INSERT INTO assessment.exam_result (id, tenant_id, exam_id, "
                        + "student_id, attendance_status, marks, max_marks, result_status, credits) VALUES (:id, "
                        + ":tenantId, :exam, :student, :attendance, :marks, :max, :status, :credits)")
                .param("id", result).param("exam", exam).param("student", student).param("attendance", attendance)
                .param("marks", decimal(marks)).param("max", decimal(maxMarks)).param("status", resultStatus)
                .param("credits", decimal(credits)).update());
        return result;
    }

    /** A grading scheme of the institution with the given bands (label, min, max, grade point, passing). */
    UUID scheme(UUID tenant, UUID institution, String method, Object[]... bands) {
        UUID scheme = UUID.randomUUID();
        inTenant(tenant, () -> {
            database.jdbc().sql("INSERT INTO assessment.grading_scheme (id, tenant_id, institution_id, name, "
                            + "scheme_type, grading_method) VALUES (:id, :tenantId, :institution, :name, 'scholastic', "
                            + ":method)")
                    .param("id", scheme).param("institution", institution).param("name", "Scheme " + code())
                    .param("method", method).update();
            for (Object[] band : bands) {
                database.jdbc().sql("INSERT INTO assessment.grading_band (id, tenant_id, grading_scheme_id, min_percent, "
                                + "max_percent, grade_label, grade_point, is_pass) VALUES (:id, :tenantId, :scheme, :min, "
                                + ":max, :label, :point, :pass)")
                        .param("id", UUID.randomUUID()).param("scheme", scheme).param("label", band[0])
                        .param("min", new BigDecimal((String) band[1])).param("max", new BigDecimal((String) band[2]))
                        .param("point", new BigDecimal((String) band[3])).param("pass", band[4]).update();
            }
            return null;
        });
        return scheme;
    }

    /** A draft result template of the session grading with the scheme. */
    UUID template(UUID tenant, UUID session, UUID scheme) {
        UUID template = UUID.randomUUID();
        inTenant(tenant, () -> database.jdbc().sql("INSERT INTO assessment.result_template (id, tenant_id, "
                        + "exam_session_id, grading_scheme_id, name, result_date) VALUES (:id, :tenantId, :session, "
                        + ":scheme, 'Term 1 results', :date)")
                .param("id", template).param("session", session).param("scheme", scheme)
                .param("date", LocalDate.parse("2026-03-20")).update());
        return template;
    }

    static Object[] band(String label, String min, String max, String point, boolean pass) {
        return new Object[] {label, min, max, point, pass};
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static String code() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
