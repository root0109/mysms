package com.mysms.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.events.PayrollReferenceCleanup;
import com.mysms.payroll.events.StaffMemberSync;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The payslip workflow over HTTP against real PostgreSQL as the application role: payroll.GeneratePayslips writes one
 * payslip per staff member with the amounts worked out by hand here, is safe to repeat and to race, and
 * payroll.MarkPayslipPaid records the payment once. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = PayrollApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PayslipRunComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PAYSLIPS = "/v1/payroll/payslips";
    private static final String GENERATE = "payroll.payslip.generate_payslips";
    private static final String MARK_PAID = "payroll.payslip.mark_payslip_paid";
    private static final String CANCEL = "payroll.payslip.cancel_payslip";
    private static final String[] OFFICER = {GENERATE, MARK_PAID, CANCEL, "payroll.payslip.read",
        "payroll.payslip_line.read", "payroll.operation.read", "payroll.payroll_payment.read"};

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PayrollDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private StaffMemberSync staffMembers;

    @Autowired
    private PayrollReferenceCleanup cleanup;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "payroll");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /**
     * One school of one tenant with the pay components Basic and HRA (earnings), PF (deduction) and the employer's
     * EPF share (employer-defined), a salary template that uses them, and a payroll officer.
     */
    private final class School {
        final UUID tenant;
        final UUID institution = UUID.randomUUID();
        final UUID template = UUID.randomUUID();
        final UUID account = UUID.randomUUID();
        final UUID officerUser = UUID.randomUUID();
        final String officer;
        private final Map<String, UUID> templateComponents = new HashMap<>();
        /** The tenant's pay components by code and its one calculation basis (shared by its schools). */
        private final Map<String, UUID> payComponents;
        private final UUID category;

        /**
         * One school of a new tenant with the pay components Basic and HRA (earnings), PF (deduction) and the
         * employer's EPF share (employer-defined).
         */
        School() {
            this.tenant = UUID.randomUUID();
            this.payComponents = new HashMap<>();
            this.category = UUID.randomUUID();
            this.officer = token(officerUser, tenant, List.of(institution.toString()), OFFICER);
            sql("INSERT INTO payroll.pay_category (id, tenant_id, name, code) VALUES ('" + category
                    + "', :tenantId, 'Fixed amount', 'FIXED')");
            String[][] components = {{"BASIC", "Basic pay", "earning", "1"}, {"HRA", "House rent", "earning", "2"},
                {"PF", "Provident fund", "deduction", "3"}, {"EPF", "Employer PF share", "defined", "4"}};
            for (String[] component : components) {
                UUID payComponent = UUID.randomUUID();
                payComponents.put(component[0], payComponent);
                sql("INSERT INTO payroll.pay_component (id, tenant_id, code, name, component_type, sort_order)"
                        + " VALUES ('" + payComponent + "', :tenantId, '" + component[0] + "', '" + component[1]
                        + "', '" + component[2] + "', " + component[3] + ")");
            }
            open();
        }

        /** Another school of the same tenant, with the tenant's pay components and a template of its own. */
        School(School sibling) {
            this.tenant = sibling.tenant;
            this.payComponents = sibling.payComponents;
            this.category = sibling.category;
            this.officer = token(officerUser, tenant, List.of(institution.toString()), OFFICER);
            open();
        }

        /** The school's salary template with every pay component, and the account salaries are paid from. */
        private void open() {
            sql("INSERT INTO payroll.salary_template (id, tenant_id, institution_id, name) VALUES ('" + template
                            + "', :tenantId, '" + institution + "', 'Teaching staff')",
                    "INSERT INTO payroll.disbursement_account (id, tenant_id, institution_id, managed_by_user_id, name,"
                            + " account_key, account_number_enc, bank_routing_code) VALUES ('" + account
                            + "', :tenantId, '" + institution + "', '" + officerUser
                            + "', 'Salary account', 'SAL', 'sealed', 'TEST0000001')");
            payComponents.forEach((code, payComponent) -> {
                UUID templateComponent = UUID.randomUUID();
                templateComponents.put(code, templateComponent);
                sql("INSERT INTO payroll.salary_template_component (id, tenant_id, salary_template_id,"
                        + " pay_component_id, pay_category_id) VALUES ('" + templateComponent + "', :tenantId, '"
                        + template + "', '" + payComponent + "', '" + category + "')");
            });
        }

        void sql(String... statements) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        for (String statement : statements) {
                            database.jdbc().sql(statement).update();
                        }
                        return null;
                    }));
        }

        <T> T query(String statement, Class<T> type) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).query(type)
                            .single()));
        }

        /** A staff salary from the date with component amounts given as code, amount pairs; answers the staff id. */
        UUID salary(UUID staff, String effectiveDate, String... amounts) {
            UUID salary = UUID.randomUUID();
            sql("INSERT INTO payroll.staff_salary (id, tenant_id, staff_id, salary_template_id, gross_salary,"
                    + " effective_date) VALUES ('" + salary + "', :tenantId, '" + staff + "', '" + template
                    + "', 1, '" + effectiveDate + "')");
            for (int i = 0; i < amounts.length; i += 2) {
                sql("INSERT INTO payroll.staff_salary_component (tenant_id, staff_salary_id,"
                        + " salary_template_component_id, amount) VALUES (:tenantId, '" + salary + "', '"
                        + templateComponents.get(amounts[i]) + "', " + amounts[i + 1] + ")");
            }
            return staff;
        }

        Reply generate(String token, String periodStart, String periodEnd, UUID... staff) throws Exception {
            StringBuilder body = new StringBuilder("{\"institutionId\":\"" + institution + "\",\"periodStart\":\""
                    + periodStart + "\",\"periodEnd\":\"" + periodEnd + "\"");
            if (staff.length > 0) {
                List<String> ids = new ArrayList<>();
                for (UUID id : staff) {
                    ids.add("\"" + id + "\"");
                }
                body.append(",\"staffIds\":[").append(String.join(",", ids)).append("]");
            }
            return call("POST", PAYSLIPS + ":generate", token, body + "}", "Idempotency-Key",
                    UUID.randomUUID().toString());
        }

        /** The staff member's payslips that count (not cancelled), oldest period first. */
        List<JsonNode> payslips(UUID staff) throws Exception {
            Reply list = call("GET", PAYSLIPS + "?filter=" + encode("staffId = \"" + staff
                    + "\" AND status != \"cancelled\""), officer, null);
            assertThat(list.status()).as(list.text()).isEqualTo(200);
            List<JsonNode> rows = new ArrayList<>();
            list.body().path("data").forEach(rows::add);
            rows.sort((a, b) -> a.path("periodStart").asString().compareTo(b.path("periodStart").asString()));
            return rows;
        }

        /** The payslip's lines as "CODE type amount", in the order the payslip lists them. */
        List<String> lines(String payslip) throws Exception {
            Reply list = call("GET", PAYSLIPS + "/" + payslip + "/lines", officer, null);
            assertThat(list.status()).as(list.text()).isEqualTo(200);
            List<String> lines = new ArrayList<>();
            for (JsonNode line : list.body().path("data")) {
                lines.add(line.path("componentCode").asString() + " " + line.path("componentType").asString() + " "
                        + line.path("amount").asString());
            }
            return lines;
        }

        Reply markPaid(String payslip, String token, String extra) throws Exception {
            return call("POST", PAYSLIPS + "/" + payslip + ":markPaid", token, "{\"disbursementAccountId\":\""
                    + account + "\",\"paymentMethod\":\"bank_transfer\",\"transactionDate\":\"2026-02-02\"" + extra
                    + "}");
        }

        /** Delivers an hr.staff event of the record (its account and row version) to payroll; whether it changed. */
        boolean staffEvent(String verb, UUID staff, UUID user, int version) {
            String data = "{\"id\":\"" + staff + "\",\"userId\":" + (user == null ? "null" : "\"" + user + "\"")
                    + ",\"personId\":null,\"institutionId\":\"" + institution + "\",\"rowVersion\":" + version
                    + "}";
            EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "hr", "hr.staff." + verb, Optional.of(tenant),
                    "hr.staff", staff, Instant.now(), data, Optional.of(TRACE));
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        if (verb.equals("deleted")) {
                            cleanup.handle(event);
                            return query("SELECT is_deleted FROM payroll.staff_member WHERE tenant_id = :tenantId"
                                    + " AND staff_id = '" + staff + "'", Boolean.class);
                        }
                        return staffMembers.apply(event);
                    }));
        }

        long count(String table, String where) {
            return query("SELECT count(*) FROM payroll." + table + " WHERE tenant_id = :tenantId AND " + where,
                    Long.class);
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token);
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static String token(UUID user, UUID tenant, List<String> institutions, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", institutions.contains("*") ? List.of("*") : List.of());
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private static List<String> skipped(Reply run) {
        List<String> skipped = new ArrayList<>();
        for (JsonNode staff : run.body().path("result").path("skipped")) {
            skipped.add(staff.path("staffId").asString() + " " + staff.path("reason").asString());
        }
        return skipped;
    }

    @Test
    void aRunWritesOnePayslipPerStaffMemberWithItsLinesAndTotalsAndARepeatedRunKeepsThem() throws Exception {
        School school = new School();
        UUID asha = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "30000.00", "HRA", "12000.50", "PF",
                "3600.00", "EPF", "1800.00");
        UUID ravi = school.salary(UUID.randomUUID(), "2025-06-01", "BASIC", "18000.00", "PF", "2160.00");

        Reply run = school.generate(school.officer, "2026-01-01", "2026-01-31");
        assertThat(run.status()).as(run.text()).isEqualTo(202);
        assertThat(run.body().path("module").asString()).isEqualTo("payroll");
        assertThat(run.body().path("kind").asString()).isEqualTo("payroll.GeneratePayslips");
        assertThat(run.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(run.body().path("progressPercent").asInt()).isEqualTo(100);
        assertThat(run.body().path("doneTime").asString()).isNotEmpty();
        assertThat(run.body().path("result").path("generated").asInt()).isEqualTo(2);
        assertThat(run.body().path("result").path("kept").asInt()).isZero();
        assertThat(run.body().path("result").path("skippedCount").asInt()).isZero();

        Reply polled = call("GET", "/v1/payroll/operations/" + run.body().path("operationId").asString(),
                school.officer, null);
        assertThat(polled.status()).as(polled.text()).isEqualTo(200);
        assertThat(polled.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(polled.body().path("result").path("generated").asInt()).isEqualTo(2);

        JsonNode ashas = school.payslips(asha).get(0);
        assertThat(ashas.path("status").asString()).isEqualTo("unpaid");
        assertThat(ashas.path("institutionId").asString()).isEqualTo(school.institution.toString());
        assertThat(ashas.path("periodStart").asString()).isEqualTo("2026-01-01");
        assertThat(ashas.path("periodEnd").asString()).isEqualTo("2026-01-31");
        // 30000.00 + 12000.50; the employer's 1800.00 is listed and counts in neither total
        assertThat(ashas.path("grossAmount").asString()).isEqualTo("42000.50");
        assertThat(ashas.path("deductionTotal").asString()).isEqualTo("3600.00");
        // 42000.50 - 3600.00
        assertThat(ashas.path("netAmount").asString()).isEqualTo("38400.50");
        assertThat(ashas.path("payPercentage").asString()).isEqualTo("100.00");
        assertThat(ashas.path("leaveDeduction").asString()).isEqualTo("0.00");
        assertThat(school.lines(ashas.path("id").asString())).containsExactly("BASIC earning 30000.00",
                "HRA earning 12000.50", "PF deduction 3600.00", "EPF defined 1800.00");
        JsonNode ravis = school.payslips(ravi).get(0);
        assertThat(ravis.path("grossAmount").asString()).isEqualTo("18000.00");
        // 18000.00 - 2160.00
        assertThat(ravis.path("netAmount").asString()).isEqualTo("15840.00");
        assertThat(List.of(ashas.path("payslipNumber").asString(), ravis.path("payslipNumber").asString()))
                .as("numbered from the institution's series").containsExactlyInAnyOrder("PS-000001", "PS-000002");
        assertThat(school.count("outbox_event", "event_type = 'payroll.payslip.created'")).isEqualTo(2);
        assertThat(school.count("outbox_event", "event_type = 'audit.recorded' AND aggregate_type = 'payroll.payslip'"))
                .isEqualTo(2);
        assertThat(school.count("outbox_event", "event_type = 'payroll.operation.succeeded'")).isEqualTo(1);
        // the end of the run is audited like any change, by the caller who started it
        assertThat(school.count("outbox_event", "event_type = 'audit.recorded' AND aggregate_type = 'payroll.operation'"))
                .as("started and finished").isEqualTo(2);
        assertThat(school.query("SELECT count(*) FROM payroll.operation WHERE tenant_id = :tenantId AND updated_by = '"
                + school.officerUser + "' AND status = 'succeeded'", Long.class)).isEqualTo(1);

        Reply again = school.generate(school.officer, "2026-01-01", "2026-01-31");
        assertThat(again.status()).as(again.text()).isEqualTo(202);
        assertThat(again.body().path("result").path("generated").asInt()).isZero();
        assertThat(again.body().path("result").path("kept").asInt()).isEqualTo(2);
        assertThat(school.payslips(asha)).hasSize(1);
        assertThat(school.payslips(asha).get(0).path("etag").asString()).as("the kept payslip is untouched")
                .isEqualTo(ashas.path("etag").asString());
        assertThat(school.count("payslip_line", "deleted_at IS NULL")).isEqualTo(6);

        // the next month is a new payslip with the next number
        Reply february = school.generate(school.officer, "2026-02-01", "2026-02-28", asha);
        assertThat(february.body().path("result").path("generated").asInt()).as(february.text()).isEqualTo(1);
        assertThat(school.payslips(asha)).hasSize(2);
        assertThat(school.payslips(asha).get(1).path("payslipNumber").asString()).isEqualTo("PS-000003");
        assertThat(school.payslips(ravi)).as("only the named staff member was paid").hasSize(1);
    }

    @Test
    void aSalaryChangeInsideThePeriodIsPaidByTheDayAndRoundedHalfUpToTheCent() throws Exception {
        School school = new School();
        UUID asha = UUID.randomUUID();
        school.salary(asha, "2025-04-01", "BASIC", "30000.00", "HRA", "12000.50", "PF", "3600.00");
        school.salary(asha, "2026-01-16", "BASIC", "36000.00", "HRA", "14400.00", "PF", "4320.00", "EPF", "1800.00");
        UUID joiner = school.salary(UUID.randomUUID(), "2026-01-17", "BASIC", "25000.00", "PF", "2500.00");
        UUID later = school.salary(UUID.randomUUID(), "2026-02-01", "BASIC", "25000.00");

        Reply run = school.generate(school.officer, "2026-01-01", "2026-01-31");
        assertThat(run.body().path("result").path("generated").asInt()).as(run.text()).isEqualTo(2);

        // January has 31 days: the old salary pays 15 of them (1-15), the new one 16 (16-31)
        JsonNode payslip = school.payslips(asha).get(0);
        assertThat(school.lines(payslip.path("id").asString())).containsExactly(
                "BASIC earning 14516.13",    // 30000.00 x 15 / 31 = 14516.129...
                "HRA earning 5806.69",       // 12000.50 x 15 / 31 = 5806.693...
                "PF deduction 1741.94",      // 3600.00 x 15 / 31 = 1741.935...
                "BASIC earning 18580.65",    // 36000.00 x 16 / 31 = 18580.645...
                "HRA earning 7432.26",       // 14400.00 x 16 / 31 = 7432.258...
                "PF deduction 2229.68",      // 4320.00 x 16 / 31 = 2229.677...
                "EPF defined 929.03");       // 1800.00 x 16 / 31 = 929.032...
        // 14516.13 + 5806.69 + 18580.65 + 7432.26
        assertThat(payslip.path("grossAmount").asString()).isEqualTo("46335.73");
        // 1741.94 + 2229.68
        assertThat(payslip.path("deductionTotal").asString()).isEqualTo("3971.62");
        // 46335.73 - 3971.62
        assertThat(payslip.path("netAmount").asString()).isEqualTo("42364.11");

        // a first salary from the 17th pays 15 of the 31 days
        JsonNode joined = school.payslips(joiner).get(0);
        assertThat(school.lines(joined.path("id").asString())).containsExactly(
                "BASIC earning 12096.77",    // 25000.00 x 15 / 31 = 12096.774...
                "PF deduction 1209.68");     // 2500.00 x 15 / 31 = 1209.677...
        assertThat(joined.path("netAmount").asString()).isEqualTo("10887.09");
        assertThat(school.payslips(later)).as("a salary that starts after the period pays nothing in it").isEmpty();
    }

    @Test
    void staffWhoCannotBePaidAreSkippedWithTheReasonAndACancelledPayslipIsGeneratedAgain() throws Exception {
        School school = new School();
        UUID paid = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "1000.00");
        UUID bare = school.salary(UUID.randomUUID(), "2025-04-01");
        UUID indebted = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "100.00", "PF", "250.50");
        UUID even = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "100.00", "PF", "100.00");
        UUID stranger = UUID.randomUUID();

        Reply run = school.generate(school.officer, "2026-01-01", "2026-01-31", paid, bare, indebted, even, stranger);
        assertThat(run.status()).as(run.text()).isEqualTo(202);
        assertThat(run.body().path("result").path("generated").asInt()).isEqualTo(2);
        assertThat(run.body().path("result").path("skippedCount").asInt()).isEqualTo(3);
        assertThat(skipped(run)).containsExactlyInAnyOrder(stranger + " NO_SALARY", bare + " NO_COMPONENTS",
                indebted + " NEGATIVE_NET");
        assertThat(school.payslips(even).get(0).path("netAmount").asString()).isEqualTo("0.00");
        assertThat(school.payslips(indebted)).isEmpty();

        // a period that shares days with a payslip that counts would pay those days twice
        Reply overlapping = school.generate(school.officer, "2026-01-15", "2026-01-31", paid);
        assertThat(skipped(overlapping)).as(overlapping.text()).containsExactly(paid + " OVERLAPPING_PERIOD");
        assertThat(school.payslips(paid)).hasSize(1);

        // a cancelled payslip no longer counts: the next run writes a new one
        JsonNode first = school.payslips(paid).get(0);
        Reply cancelled = call("POST", PAYSLIPS + "/" + first.path("id").asString() + ":cancel", school.officer,
                "{\"reason\":\"Wrong salary\"}");
        assertThat(cancelled.status()).as(cancelled.text()).isEqualTo(200);
        school.sql("UPDATE payroll.staff_salary_component SET amount = 1200.00 WHERE tenant_id = :tenantId"
                + " AND staff_salary_id IN (SELECT id FROM payroll.staff_salary WHERE tenant_id = :tenantId"
                + " AND staff_id = '" + paid + "')");
        Reply rerun = school.generate(school.officer, "2026-01-01", "2026-01-31", paid);
        assertThat(rerun.body().path("result").path("generated").asInt()).as(rerun.text()).isEqualTo(1);
        JsonNode second = school.payslips(paid).get(0);
        assertThat(second.path("id").asString()).isNotEqualTo(first.path("id").asString());
        assertThat(second.path("netAmount").asString()).isEqualTo("1200.00");
    }

    @Test
    void aRunIsRefusedWithoutThePermissionOutsideTheCallersInstitutionsAndForAnImpossiblePeriod() throws Exception {
        School school = new School();
        UUID asha = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "1000.00");
        String reader = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()),
                "payroll.payslip.read");
        assertThat(school.generate(reader, "2026-01-01", "2026-01-31").status()).isEqualTo(403);
        String elsewhere = token(UUID.randomUUID(), school.tenant, List.of(UUID.randomUUID().toString()), OFFICER);
        Reply outside = school.generate(elsewhere, "2026-01-01", "2026-01-31");
        assertThat(outside.status()).as(outside.text()).isEqualTo(403);

        Reply reversed = school.generate(school.officer, "2026-01-31", "2026-01-01");
        assertThat(reversed.status()).as(reversed.text()).isEqualTo(422);
        assertThat(reversed.body().path("errors").get(0).path("field").asString()).isEqualTo("periodEnd");
        assertThat(school.generate(school.officer, "2026-01-01", "2026-02-01").status())
                .as("two calendar months").isEqualTo(422);
        assertThat(school.generate(school.officer, "2026-01-20", "2026-02-10").status())
                .as("a short period across two months").isEqualTo(422);
        assertThat(school.generate(school.officer, "2026-01-01", "2026-01-31").status())
                .as("31 days").isEqualTo(202);
        Reply noPeriod = call("POST", PAYSLIPS + ":generate", school.officer,
                "{\"institutionId\":\"" + school.institution + "\"}", "Idempotency-Key", UUID.randomUUID().toString());
        assertThat(noPeriod.status()).as(noPeriod.text()).isEqualTo(422);
        Reply noKey = call("POST", PAYSLIPS + ":generate", school.officer, "{\"institutionId\":\""
                + school.institution + "\",\"periodStart\":\"2026-03-01\",\"periodEnd\":\"2026-03-31\"}");
        assertThat(noKey.status()).as("a bulk money operation needs an Idempotency-Key: " + noKey.text())
                .isEqualTo(400);

        // another tenant's officer, even with the same institution id in scope, pays nobody of this tenant
        String foreign = token(UUID.randomUUID(), UUID.randomUUID(), List.of(school.institution.toString()), OFFICER);
        Reply theirs = school.generate(foreign, "2026-03-01", "2026-03-31", asha);
        assertThat(theirs.status()).as(theirs.text()).isEqualTo(202);
        assertThat(theirs.body().path("result").path("generated").asInt()).isZero();
        assertThat(skipped(theirs)).containsExactly(asha + " NO_SALARY");
        assertThat(school.payslips(asha)).as("January only").hasSize(1);
        Reply theirOperation = call("GET", "/v1/payroll/operations/" + theirs.body().path("operationId").asString(),
                school.officer, null);
        assertThat(theirOperation.status()).as("an operation of another tenant").isEqualTo(404);
    }

    @Test
    void runsAtTheSameTimeWriteEachPayslipOnce() throws Exception {
        School school = new School();
        List<UUID> staff = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            staff.add(school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "1000.00", "PF", "100.00"));
        }
        List<CompletableFuture<Reply>> runs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            runs.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return school.generate(school.officer, "2026-01-01", "2026-01-31");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));
        }
        int generated = 0;
        for (CompletableFuture<Reply> run : runs) {
            Reply reply = run.join();
            assertThat(reply.status()).as(reply.text()).isEqualTo(202);
            assertThat(reply.body().path("result").path("skippedCount").asInt()).as(reply.text()).isZero();
            generated += reply.body().path("result").path("generated").asInt();
        }
        assertThat(generated).as("each payslip was generated by exactly one of the runs").isEqualTo(12);
        assertThat(school.count("payslip", "deleted_at IS NULL")).isEqualTo(12);
        assertThat(school.count("payslip_line", "deleted_at IS NULL")).isEqualTo(24);
        assertThat(school.query("SELECT count(DISTINCT payslip_number) FROM payroll.payslip"
                + " WHERE tenant_id = :tenantId", Long.class)).isEqualTo(12);
        for (UUID member : staff) {
            assertThat(school.payslips(member)).hasSize(1);
        }
    }

    @Test
    void markingPaidRecordsThePaymentOnceAndThePayslipNoLongerChanges() throws Exception {
        School school = new School();
        UUID asha = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "30000.00", "PF", "3600.00");
        school.generate(school.officer, "2026-01-01", "2026-01-31");
        JsonNode payslip = school.payslips(asha).get(0);
        String id = payslip.path("id").asString();

        Reply paid = school.markPaid(id, school.officer, ",\"referenceNumber\":\"  UTR-778899  \",\"etag\":\""
                + payslip.path("etag").asString().replace("\"", "\\\"") + "\"");
        assertThat(paid.status()).as(paid.text()).isEqualTo(200);
        assertThat(paid.body().path("status").asString()).isEqualTo("paid");
        assertThat(paid.body().path("netAmount").asString()).isEqualTo("26400.00");

        Reply payments = call("GET", "/v1/payroll/payroll-payments?filter=" + encode("payslipId = \"" + id + "\""),
                school.officer, null);
        assertThat(payments.status()).as(payments.text()).isEqualTo(200);
        assertThat(payments.body().path("data")).hasSize(1);
        JsonNode payment = payments.body().path("data").get(0);
        // 30000.00 - 3600.00
        assertThat(payment.path("amount").asString()).isEqualTo("26400.00");
        assertThat(payment.path("staffId").asString()).isEqualTo(asha.toString());
        assertThat(payment.path("institutionId").asString()).isEqualTo(school.institution.toString());
        assertThat(payment.path("disbursementAccountId").asString()).isEqualTo(school.account.toString());
        assertThat(payment.path("paymentMethod").asString()).isEqualTo("bank_transfer");
        assertThat(payment.path("transactionDate").asString()).isEqualTo("2026-02-02");
        assertThat(payment.path("referenceNumber").asString()).isEqualTo("UTR-778899");
        assertThat(payment.path("transactionNumber").asString()).isEqualTo("PP-000001");
        assertThat(school.count("outbox_event", "event_type = 'payroll.payslip.markPaid'")).isEqualTo(1);
        assertThat(school.count("outbox_event", "event_type = 'payroll.payroll_payment.created'")).isEqualTo(1);

        assertThat(school.markPaid(id, school.officer, "").status()).as("paid already").isEqualTo(409);
        Reply cancel = call("POST", PAYSLIPS + "/" + id + ":cancel", school.officer, "{}");
        assertThat(cancel.status()).as("a paid payslip is not cancelled: " + cancel.text()).isEqualTo(409);
        assertThat(school.count("payroll_payment", "deleted_at IS NULL")).isEqualTo(1);
        Reply rerun = school.generate(school.officer, "2026-01-01", "2026-01-31");
        assertThat(rerun.body().path("result").path("kept").asInt()).as("a paid payslip is kept").isEqualTo(1);
        assertThat(school.payslips(asha).get(0).path("etag").asString())
                .isEqualTo(paid.body().path("etag").asString());
    }

    @Test
    void markingPaidIsRefusedForTheWrongCallerVersionAccountOrState() throws Exception {
        School school = new School();
        UUID asha = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "30000.00");
        UUID even = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "100.00", "PF", "100.00");
        school.generate(school.officer, "2026-01-01", "2026-01-31");
        String id = school.payslips(asha).get(0).path("id").asString();

        String reader = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()),
                "payroll.payslip.read");
        assertThat(school.markPaid(id, reader, "").status()).isEqualTo(403);
        String elsewhere = token(UUID.randomUUID(), school.tenant, List.of(UUID.randomUUID().toString()), OFFICER);
        assertThat(school.markPaid(id, elsewhere, "").status()).as("outside the caller's institutions")
                .isEqualTo(404);
        String foreign = token(UUID.randomUUID(), UUID.randomUUID(), List.of("*"), OFFICER);
        assertThat(school.markPaid(id, foreign, "").status()).as("another tenant").isEqualTo(404);
        assertThat(school.markPaid(UUID.randomUUID().toString(), school.officer, "").status()).isEqualTo(404);

        assertThat(school.markPaid(id, school.officer, ",\"etag\":\"\\\"99\\\"\"").status()).as("a stale etag")
                .isEqualTo(412);
        Reply noAccount = call("POST", PAYSLIPS + "/" + id + ":markPaid", school.officer,
                "{\"paymentMethod\":\"cash\",\"transactionDate\":\"2026-02-02\"}");
        assertThat(noAccount.status()).as(noAccount.text()).isEqualTo(422);
        Reply unknownAccount = call("POST", PAYSLIPS + "/" + id + ":markPaid", school.officer,
                "{\"disbursementAccountId\":\"" + UUID.randomUUID()
                        + "\",\"paymentMethod\":\"cash\",\"transactionDate\":\"2026-02-02\"}");
        assertThat(unknownAccount.status()).as(unknownAccount.text()).isEqualTo(422);
        assertThat(unknownAccount.body().path("errors").get(0).path("field").asString())
                .isEqualTo("disbursementAccountId");
        UUID otherSchoolsAccount = UUID.randomUUID();
        school.sql("INSERT INTO payroll.disbursement_account (id, tenant_id, institution_id, managed_by_user_id, name,"
                + " account_key, account_number_enc, bank_routing_code) VALUES ('" + otherSchoolsAccount
                + "', :tenantId, '" + UUID.randomUUID() + "', '" + school.officerUser
                + "', 'Other school', 'OTH', 'sealed', 'TEST0000002')");
        Reply wrongAccount = call("POST", PAYSLIPS + "/" + id + ":markPaid", school.officer,
                "{\"disbursementAccountId\":\"" + otherSchoolsAccount
                        + "\",\"paymentMethod\":\"cash\",\"transactionDate\":\"2026-02-02\"}");
        assertThat(wrongAccount.status()).as("an account of another institution: " + wrongAccount.text())
                .isEqualTo(422);
        assertThat(school.markPaid(id, school.officer, ",\"referenceNumber\":\"" + "9".repeat(81) + "\"").status())
                .as("a reference longer than the payment keeps").isEqualTo(422);
        assertThat(school.payslips(asha).get(0).path("status").asString()).as("nothing changed").isEqualTo("unpaid");
        assertThat(school.count("payroll_payment", "deleted_at IS NULL")).isZero();

        // a payslip with nothing to pay is settled without a payment
        String nothingToPay = school.payslips(even).get(0).path("id").asString();
        Reply settled = school.markPaid(nothingToPay, school.officer, "");
        assertThat(settled.status()).as(settled.text()).isEqualTo(200);
        assertThat(settled.body().path("status").asString()).isEqualTo("paid");
        assertThat(school.count("payroll_payment", "deleted_at IS NULL")).isZero();

        // a cancelled payslip is not paid
        call("POST", PAYSLIPS + "/" + id + ":cancel", school.officer, "{}");
        assertThat(school.markPaid(id, school.officer, "").status()).as("cancelled").isEqualTo(409);

        // paid twice at the same moment: one payment
        school.generate(school.officer, "2026-01-01", "2026-01-31", asha);
        String fresh = school.payslips(asha).get(0).path("id").asString();
        List<CompletableFuture<Reply>> attempts = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            attempts.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return school.markPaid(fresh, school.officer, "");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));
        }
        List<Integer> statuses = attempts.stream().map(attempt -> attempt.join().status()).toList();
        assertThat(statuses.stream().filter(status -> status == 200).count()).as(statuses.toString()).isEqualTo(1);
        assertThat(statuses).allMatch(status -> status == 200 || status == 409 || status == 412);
        assertThat(school.count("payroll_payment", "deleted_at IS NULL AND payslip_id = '" + fresh + "'"))
                .isEqualTo(1);
    }

    @Test
    void twoHalvesOfAMonthPayTheMonthOnceAndADayPaysADay() throws Exception {
        School school = new School();
        UUID asha = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "30000.00", "HRA", "100.01", "PF",
                "0.03");

        Reply first = school.generate(school.officer, "2026-01-01", "2026-01-15");
        assertThat(first.body().path("result").path("generated").asInt()).as(first.text()).isEqualTo(1);
        Reply second = school.generate(school.officer, "2026-01-16", "2026-01-31");
        assertThat(second.body().path("result").path("generated").asInt()).as(second.text()).isEqualTo(1);
        List<JsonNode> halves = school.payslips(asha);
        assertThat(halves).hasSize(2);
        // January has 31 days: 15 of them, then 16
        assertThat(school.lines(halves.get(0).path("id").asString())).containsExactly(
                "BASIC earning 14516.13",    // 30000.00 x 15 / 31 = 14516.129...
                "HRA earning 48.39",         // 100.01 x 15 / 31 = 48.391...
                "PF deduction 0.01");        // 0.03 x 15 / 31 = 0.0145...
        assertThat(school.lines(halves.get(1).path("id").asString())).containsExactly(
                "BASIC earning 15483.87",    // 30000.00 x 16 / 31 = 15483.870...
                "HRA earning 51.62",         // 100.01 x 16 / 31 = 51.618...
                "PF deduction 0.02");        // 0.03 x 16 / 31 = 0.0154...
        // 14516.13 + 48.39 - 0.01 and 15483.87 + 51.62 - 0.02: together the month's 30100.01 - 0.03, once
        assertThat(halves.get(0).path("netAmount").asString()).isEqualTo("14564.51");
        assertThat(halves.get(1).path("netAmount").asString()).isEqualTo("15535.47");

        // February 2026 has 28 days: one day of 30000.00 is 1071.43 (1071.428...)
        UUID ravi = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "30000.00");
        school.generate(school.officer, "2026-02-10", "2026-02-10", ravi);
        assertThat(school.lines(school.payslips(ravi).get(0).path("id").asString()))
                .containsExactly("BASIC earning 1071.43");
    }

    @Test
    void aStaffMemberWhoMovesSchoolInTheMonthIsPaidByEachSchoolForItsOwnDays() throws Exception {
        School first = new School();
        School second = new School(first);
        UUID asha = UUID.randomUUID();
        first.salary(asha, "2025-04-01", "BASIC", "31000.00", "PF", "3100.00");
        second.salary(asha, "2026-01-16", "BASIC", "62000.00");

        // the second school pays 16 to 31 January with its own salary only: 62000.00 x 16 / 31 = 32000.00
        Reply theirs = second.generate(second.officer, "2026-01-01", "2026-01-31");
        assertThat(theirs.body().path("result").path("generated").asInt()).as(theirs.text()).isEqualTo(1);
        JsonNode payslip = second.payslips(asha).get(0);
        assertThat(second.lines(payslip.path("id").asString())).containsExactly("BASIC earning 32000.00");
        assertThat(payslip.path("institutionId").asString()).isEqualTo(second.institution.toString());

        // the first school's run for January does not pay someone who left it; its part is a period of its own
        Reply january = first.generate(first.officer, "2026-01-01", "2026-01-31", asha);
        assertThat(skipped(january)).containsExactly(asha + " NO_SALARY");
    }

    @Test
    void aStaffMemberReadsOnlyTheirOwnPayslipsWhileTheOfficerReadsTheSchools() throws Exception {
        School school = new School();
        UUID asha = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "30000.00", "PF", "3600.00");
        UUID ravi = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "18000.00");
        school.generate(school.officer, "2026-01-01", "2026-01-31");
        String ashas = school.payslips(asha).get(0).path("id").asString();
        String ravis = school.payslips(ravi).get(0).path("id").asString();
        UUID ashaUser = UUID.randomUUID();
        assertThat(school.staffEvent("created", asha, ashaUser, 1)).isTrue();
        String teacher = token(ashaUser, school.tenant, List.of(school.institution.toString()),
                "payroll.payslip.read", "payroll.payslip_line.read");

        assertThat(ids(call("GET", PAYSLIPS, teacher, null))).containsExactly(ashas);
        assertThat(ids(call("GET", PAYSLIPS + "?filter=" + encode("institutionId = \"" + school.institution + "\""),
                teacher, null))).containsExactly(ashas);
        assertThat(call("GET", PAYSLIPS + "/" + ashas, teacher, null).status()).isEqualTo(200);
        assertThat(call("GET", PAYSLIPS + "/" + ravis, teacher, null).status()).as("a colleague's").isEqualTo(404);
        assertThat(ids(call("GET", PAYSLIPS + "/" + ashas + "/lines", teacher, null))).hasSize(2);
        assertThat(ids(call("GET", PAYSLIPS + "/" + ravis + "/lines", teacher, null))).as("a colleague's").isEmpty();
        String ravisLine = ids(call("GET", PAYSLIPS + "/" + ravis + "/lines", school.officer, null)).get(0);
        assertThat(call("GET", PAYSLIPS + "/" + ravis + "/lines/" + ravisLine, teacher, null).status()).isEqualTo(404);
        assertThat(call("GET", PAYSLIPS + "/" + ravis + "/lines/" + ravisLine, school.officer, null).status())
                .isEqualTo(200);
        assertThat(ids(call("GET", PAYSLIPS, school.officer, null))).containsExactlyInAnyOrder(ashas, ravis);

        // the same account in another tenant reads nothing of this one
        String foreign = token(ashaUser, UUID.randomUUID(), List.of(school.institution.toString()),
                "payroll.payslip.read");
        assertThat(ids(call("GET", PAYSLIPS, foreign, null))).isEmpty();
        assertThat(call("GET", PAYSLIPS + "/" + ashas, foreign, null).status()).isEqualTo(404);
    }

    @Test
    void theAccountOfAStaffRecordFollowsHrsEventsInVersionOrderAndADeletionIsFinal() throws Exception {
        School school = new School();
        UUID asha = school.salary(UUID.randomUUID(), "2025-04-01", "BASIC", "30000.00");
        school.generate(school.officer, "2026-01-01", "2026-01-31");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        String firstToken = token(first, school.tenant, List.of(school.institution.toString()), "payroll.payslip.read");
        String secondToken = token(second, school.tenant, List.of(school.institution.toString()),
                "payroll.payslip.read");

        assertThat(school.staffEvent("updated", asha, second, 2)).isTrue();
        assertThat(school.staffEvent("created", asha, first, 1)).as("a late, older event").isFalse();
        assertThat(school.staffEvent("updated", asha, second, 2)).as("a repeated event").isFalse();
        assertThat(ids(call("GET", PAYSLIPS, secondToken, null))).hasSize(1);
        assertThat(ids(call("GET", PAYSLIPS, firstToken, null))).isEmpty();

        assertThat(school.staffEvent("deleted", asha, second, 3)).isTrue();
        assertThat(school.staffEvent("updated", asha, second, 4)).as("nothing after the deletion").isFalse();
        assertThat(ids(call("GET", PAYSLIPS, secondToken, null))).isEmpty();
        assertThat(ids(call("GET", PAYSLIPS, school.officer, null))).as("the officer still reads it").hasSize(1);

        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> school.staffEvent("created", other, UUID.randomUUID(), 0))
                .isInstanceOf(RejectedEventException.class);
        assertThat(school.count("staff_member", "staff_id = '" + other + "'")).isZero();
    }

    private static List<String> ids(Reply list) {
        assertThat(list.status()).as(list.text()).isEqualTo(200);
        List<String> ids = new ArrayList<>();
        list.body().path("data").forEach(row -> ids.add(row.path("id").asString()));
        return ids;
    }
}
