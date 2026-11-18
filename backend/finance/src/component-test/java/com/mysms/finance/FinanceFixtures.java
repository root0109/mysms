package com.mysms.finance;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the finance workflow component tests share: signed tokens, calls over HTTP to the running service, and the
 * rows a test starts from (fee heads, plans, concessions), written with plain SQL as the application role inside the
 * test's own tenant.
 */
final class FinanceFixtures {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final UUID INR = UUID.fromString("0196f000-0000-7000-8000-0000000fc001");
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    private final TestTokens tokens;
    private final ModuleDatabase database;
    private final int port;

    FinanceFixtures(TestTokens tokens, ModuleDatabase database, int port) {
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

    /** The fee office of one institution: every finance permission there. */
    String office(UUID tenant, UUID institution) {
        return token(UUID.randomUUID(), tenant, List.of(institution.toString()), List.of(), "finance.*");
    }

    /** A family member (student or guardian) of one institution: the reads and the online payment, nothing more. */
    String family(UUID tenant, UUID institution, UUID user) {
        return token(user, tenant, List.of(institution.toString()), List.of(), "finance.invoice.get_fee_statement",
                "finance.invoice.read", "finance.student_fee_due.read", "finance.payment.read",
                "finance.payment.quote_checkout", "finance.payment.start_checkout", "finance.refund.read");
    }

    Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    /** A POST carrying a fresh Idempotency-Key. */
    Reply post(String path, String token, Object body) throws Exception {
        return call("POST", path, token, json(body), "Idempotency-Key", UUID.randomUUID().toString());
    }

    static Reply expect(Reply reply, int status) {
        assertThat(reply.status()).as(reply.text()).isEqualTo(status);
        return reply;
    }

    static String json(Object value) {
        return JSON.writeValueAsString(value);
    }

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

    /** Executes one statement in the tenant. */
    void sql(UUID tenant, String sql, Object... namedParams) {
        inTenant(tenant, () -> {
            var statement = database.jdbc().sql(sql);
            for (int i = 0; i < namedParams.length; i += 2) {
                statement = statement.param((String) namedParams[i], namedParams[i + 1]);
            }
            return statement.update();
        });
    }

    UUID feeHead(UUID tenant, UUID institution, String name, String defaultAmount) {
        UUID id = UUID.randomUUID();
        sql(tenant, "INSERT INTO finance.fee_head (id, tenant_id, institution_id, code, name, default_amount, "
                + "currency_id, row_version) VALUES (:id, :tenantId, :institution, :code, :name, :amount, :currency, 1)",
                "id", id, "institution", institution, "code", "H" + id.toString().substring(0, 8), "name", name,
                "amount", defaultAmount == null ? null : new BigDecimal(defaultAmount), "currency", INR);
        return id;
    }

    UUID plan(UUID tenant, UUID institution, String discountPercent) {
        UUID id = UUID.randomUUID();
        sql(tenant, "INSERT INTO finance.fee_plan (id, tenant_id, institution_id, code, name, schedule_basis, "
                + "discount_percent, row_version) VALUES (:id, :tenantId, :institution, :code, 'Annual plan', "
                + "'fixed_days', :discount, 1)", "id", id, "institution", institution,
                "code", "P" + id.toString().substring(0, 8),
                "discount", discountPercent == null ? null : new BigDecimal(discountPercent));
        return id;
    }

    UUID installment(UUID tenant, UUID plan, int sequence, int dueDays, String sharePercent) {
        UUID id = UUID.randomUUID();
        sql(tenant, "INSERT INTO finance.fee_plan_installment (id, tenant_id, fee_plan_id, sequence, due_days, "
                + "share_percent, row_version) VALUES (:id, :tenantId, :plan, :sequence, :days, :share, 1)",
                "id", id, "plan", plan, "sequence", sequence, "days", dueDays, "share", new BigDecimal(sharePercent));
        return id;
    }

    void component(UUID tenant, UUID installment, UUID feeHead, int sequence, String sharePercent) {
        sql(tenant, "INSERT INTO finance.fee_plan_installment_component (id, tenant_id, installment_id, fee_head_id, "
                + "sequence, share_percent, row_version) VALUES (:id, :tenantId, :installment, :head, :sequence, :share, "
                + "1)", "id", UUID.randomUUID(), "installment", installment, "head", feeHead, "sequence", sequence,
                "share", new BigDecimal(sharePercent));
    }

    void concession(UUID tenant, UUID institution, UUID student, UUID year, UUID feeHead, String type, String value) {
        sql(tenant, "INSERT INTO finance.fee_concession (id, tenant_id, institution_id, student_id, academic_year_id, "
                + "fee_head_id, concession_type, concession_value, row_version) VALUES (:id, :tenantId, :institution, "
                + ":student, :year, :head, :type, :value, 1)", "id", UUID.randomUUID(), "institution", institution,
                "student", student, "year", year, "head", feeHead, "type", type, "value", new BigDecimal(value));
    }

    /** Runs a fee run for the students over HTTP and answers the finished operation. */
    JsonNode generate(String token, UUID plan, UUID year, String startDate, UUID... students) throws Exception {
        Reply reply = expect(post("/v1/finance/student-fee-dues:generate", token, Map.of("feePlanId", plan,
                "academicYearId", year, "studentIds", List.of(students), "startDate", startDate)), 202);
        assertThat(reply.body().path("status").asString()).isEqualTo("succeeded");
        return reply.body().path("result");
    }

    /** The student's dues as amount|discount|net|due date, keyed by fee head and installment ("<head>#<sequence>"). */
    Map<String, String> dues(UUID tenant, UUID student, Map<UUID, String> heads) {
        Map<String, String> found = new HashMap<>();
        inTenant(tenant, () -> {
            database.jdbc().sql("SELECT d.fee_head_id, i.sequence, d.amount, d.discount_amount, d.net_amount, "
                    + "d.due_date FROM finance.student_fee_due d JOIN finance.fee_plan_installment i "
                    + "ON i.tenant_id = :tenantId AND i.id = d.installment_id WHERE d.tenant_id = :tenantId "
                    + "AND d.student_id = :student AND d.deleted_at IS NULL").param("student", student)
                    .query((rs, row) -> found.put(heads.get(rs.getObject(1, UUID.class)) + "#" + rs.getInt(2),
                            rs.getBigDecimal(3) + "|" + rs.getBigDecimal(4) + "|" + rs.getBigDecimal(5) + "|"
                                    + rs.getObject(6, LocalDate.class)))
                    .list();
            return null;
        });
        return found;
    }

    /** The id of the student's due for the head in the installment. */
    UUID due(UUID tenant, UUID student, UUID feeHead, int sequence) {
        return value(tenant, "SELECT d.id FROM finance.student_fee_due d JOIN finance.fee_plan_installment i "
                + "ON i.tenant_id = :tenantId AND i.id = d.installment_id WHERE d.tenant_id = :tenantId "
                + "AND d.student_id = :student AND d.fee_head_id = :head AND i.sequence = :sequence "
                + "AND d.status <> 'cancelled'", UUID.class, "student", student, "head", feeHead, "sequence", sequence);
    }
}
