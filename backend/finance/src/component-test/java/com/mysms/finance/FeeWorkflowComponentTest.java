package com.mysms.finance;

import static com.mysms.finance.FinanceFixtures.INR;
import static com.mysms.finance.FinanceFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.DataScope;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.finance.FinanceFixtures.Reply;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.domain.InvoiceEntity;
import com.mysms.finance.repository.InvoiceRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * The fee workflow over HTTP against real PostgreSQL as the application role: a fee run priced from a plan, invoices
 * raised from dues, posted and cancelled, counter payments allocated exactly, refunds, the fee statement, who may read
 * what, and two payments or two refunds racing for the same money. sis is not running here, so the family's own
 * reads stop at 503 after every local check.
 *
 * <p>The school's plan: Tuition (default 30000.00) and Lab (6000.00), so a total of 36000.00; installment 1 (40 %,
 * due at the start) shares Tuition 75 % and Lab 25 %, installment 2 (60 %, 90 days later) is all Tuition; the plan
 * takes 5 % off every due. From 2026-04-01:
 * <pre>
 *   installment 1: 14400.00 → Tuition 10800.00 (5 %: 540.00), Lab 3600.00 (5 %: 180.00)     due 2026-04-01
 *   installment 2: 21600.00 → Tuition 21600.00 (5 %: 1080.00)                               due 2026-06-30
 *   S1, no concession:            nets 10260.00, 3420.00, 20520.00                     (34200.00)
 *   S2, 10 % off Tuition:         Tuition 10260.00 − 1026.00 → 9234.00, 20520.00 − 2052.00 → 18468.00, Lab 3420.00
 *   S3, 12000.00 off any head:    Tuition#1 10260.00 → 0.00 (1740.00 left), Lab 3420.00 − 1740.00 → 1680.00,
 *                                 Tuition#2 20520.00 (budget spent)
 * </pre>
 */
@SpringBootTest(classes = FinanceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeeWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String DUES = "/v1/finance/student-fee-dues";
    private static final String INVOICES = "/v1/finance/invoices";
    private static final String PAYMENTS = "/v1/finance/payments";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(FinanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private InvoiceRepository invoices;

    private FinanceFixtures fx;
    private UUID tenant;
    private UUID institution;
    private UUID year;
    private String admin;
    private UUID tuition;
    private UUID lab;
    private UUID plan;
    private Map<UUID, String> heads;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "finance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void school() {
        fx = new FinanceFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        year = UUID.randomUUID();
        admin = fx.admin(tenant);
        tuition = fx.feeHead(tenant, institution, "Tuition", "30000.00");
        lab = fx.feeHead(tenant, institution, "Lab", "6000.00");
        heads = Map.of(tuition, "Tuition", lab, "Lab");
        plan = fx.plan(tenant, institution, "5.00");
        UUID first = fx.installment(tenant, plan, 1, 0, "40.000");
        UUID second = fx.installment(tenant, plan, 2, 90, "60.000");
        fx.component(tenant, first, tuition, 1, "75.000");
        fx.component(tenant, first, lab, 2, "25.000");
        fx.component(tenant, second, tuition, 1, "100.000");
    }

    @Test
    void aFeeRunPricesInstallmentsThePlanDiscountAndConcessions() throws Exception {
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID s3 = UUID.randomUUID();
        fx.concession(tenant, institution, s2, year, tuition, "percent", "10");
        fx.concession(tenant, institution, s3, year, null, "amount", "12000.00");

        JsonNode run = fx.generate(admin, plan, year, "2026-04-01", s1, s2, s3);
        assertThat(run.path("students").asInt()).isEqualTo(3);
        assertThat(run.path("dues").asInt()).isEqualTo(9);
        assertThat(run.path("kept").asInt()).isZero();

        assertThat(fx.dues(tenant, s1, heads)).containsOnly(
                Map.entry("Tuition#1", "10800.00|540.00|10260.00|2026-04-01"),
                Map.entry("Lab#1", "3600.00|180.00|3420.00|2026-04-01"),
                Map.entry("Tuition#2", "21600.00|1080.00|20520.00|2026-06-30"));
        assertThat(fx.dues(tenant, s2, heads)).containsOnly(
                Map.entry("Tuition#1", "10800.00|1566.00|9234.00|2026-04-01"),
                Map.entry("Lab#1", "3600.00|180.00|3420.00|2026-04-01"),
                Map.entry("Tuition#2", "21600.00|3132.00|18468.00|2026-06-30"));
        assertThat(fx.dues(tenant, s3, heads)).containsOnly(
                Map.entry("Tuition#1", "10800.00|10800.00|0.00|2026-04-01"),
                Map.entry("Lab#1", "3600.00|1920.00|1680.00|2026-04-01"),
                Map.entry("Tuition#2", "21600.00|1080.00|20520.00|2026-06-30"));

        JsonNode again = fx.generate(admin, plan, year, "2026-04-01", s1, s2, s3);
        assertThat(again.path("dues").asInt()).as("a slot already charged is kept").isZero();
        assertThat(again.path("kept").asInt()).isEqualTo(3);
        assertThat(fx.value(tenant, "SELECT COUNT(*) FROM finance.student_fee_due WHERE tenant_id = :tenantId",
                Long.class)).isEqualTo(9L);
    }

    @Test
    void aTotalOverrideIsSharedOutExactlyToTheCent() throws Exception {
        UUID student = UUID.randomUUID();
        UUID thirds = fx.plan(tenant, institution, null);
        UUID a = fx.installment(tenant, thirds, 1, 0, "33.333");
        UUID b = fx.installment(tenant, thirds, 2, 30, "33.333");
        UUID c = fx.installment(tenant, thirds, 3, 60, "33.334");
        fx.component(tenant, a, tuition, 1, "100.000");
        fx.component(tenant, b, tuition, 1, "100.000");
        fx.component(tenant, c, tuition, 1, "100.000");
        Reply reply = expect(fx.post(DUES + ":generate", admin, Map.of("feePlanId", thirds, "academicYearId", year,
                "studentIds", List.of(student), "startDate", "2026-04-01", "totalAmount", "1000.00")), 202);
        assertThat(reply.body().path("result").path("dues").asInt()).isEqualTo(3);
        // 1000.00 × 33.333 % = 333.33 twice, × 33.334 % = 333.34: exact, nothing left over
        assertThat(fx.dues(tenant, student, heads)).containsOnly(
                Map.entry("Tuition#1", "333.33|0.00|333.33|2026-04-01"),
                Map.entry("Tuition#2", "333.33|0.00|333.33|2026-05-01"),
                Map.entry("Tuition#3", "333.34|0.00|333.34|2026-05-31"));
    }

    @Test
    void aFeeRunIsRefusedForAPlanThatCannotBeChargedOrOutsideTheCallersSchool() throws Exception {
        UUID broken = fx.plan(tenant, institution, null);
        UUID only = fx.installment(tenant, broken, 1, 0, "90.000");
        fx.component(tenant, only, fx.feeHead(tenant, institution, "Bus", null), 1, "100.000");
        Reply refused = expect(fx.post(DUES + ":generate", admin, Map.of("feePlanId", broken, "academicYearId", year,
                "studentIds", List.of(UUID.randomUUID()), "startDate", "2026-04-01")), 422);
        assertThat(refused.text()).contains("installment shares must total 100").contains("totalAmount");

        String elsewhere = fx.office(tenant, UUID.randomUUID());
        expect(fx.post(DUES + ":generate", elsewhere, Map.of("feePlanId", plan, "academicYearId", year,
                "studentIds", List.of(UUID.randomUUID()), "startDate", "2026-04-01")), 422);
        expect(fx.post(DUES + ":generate", admin, Map.of("feePlanId", plan, "academicYearId", year,
                "startDate", "2026-04-01")), 422);
        Reply noKey = fx.call("POST", DUES + ":generate", admin, FinanceFixtures.json(Map.of("feePlanId", plan,
                "academicYearId", year, "studentIds", List.of(UUID.randomUUID()), "startDate", "2026-04-01")));
        assertThat(noKey.status()).as("the Idempotency-Key is required").isEqualTo(400);
        Reply roster = fx.post(DUES + ":generate", admin, Map.of("feePlanId", plan, "academicYearId", year,
                "classSectionId", UUID.randomUUID(), "startDate", "2026-04-01"));
        assertThat(roster.status()).as("a class roster is sis', which is not running: " + roster.text())
                .isEqualTo(503);
        assertThat(fx.value(tenant, "SELECT COUNT(*) FROM finance.student_fee_due WHERE tenant_id = :tenantId",
                Long.class)).isZero();
    }

    @Test
    void invoicesAreRaisedFromDuesCancelledAndPosted() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID t1 = fx.due(tenant, s1, tuition, 1);
        UUID l1 = fx.due(tenant, s1, lab, 1);

        JsonNode draft = fromDues(s1, t1, l1);
        assertThat(draft.path("status").asString()).isEqualTo("draft");
        assertThat(draft.path("invoiceNumber").asString()).startsWith("DRAFT-");
        assertThat(draft.path("subtotal").asString()).isEqualTo("14400.00");
        assertThat(draft.path("discountTotal").asString()).isEqualTo("720.00");
        assertThat(draft.path("total").asString()).isEqualTo("13680.00");
        assertThat(draft.path("billToName").asString()).isEqualTo("Asha Rao");
        String id = draft.path("id").asString();
        JsonNode lines = expect(fx.call("GET", INVOICES + "/" + id + "/lines", admin, null), 200).body();
        assertThat(lines.path("data").findValuesAsString("description"))
                .containsExactlyInAnyOrder("Tuition · installment 1", "Lab · installment 1");
        assertThat(expect(fx.call("GET", DUES + "/" + t1, admin, null), 200).body().path("status").asString())
                .isEqualTo("invoiced");

        expect(fx.post(INVOICES + ":fromDues", admin, Map.of("studentId", s1, "dueIds", List.of(t1),
                "billToName", "Asha Rao")), 409);
        expect(fx.post(INVOICES + ":fromDues", admin, Map.of("studentId", UUID.randomUUID(),
                "dueIds", List.of(fx.due(tenant, s1, tuition, 2)), "billToName", "Asha Rao")), 422);
        Reply nameless = fx.post(INVOICES + ":fromDues", admin, Map.of("studentId", s1,
                "dueIds", List.of(fx.due(tenant, s1, tuition, 2))));
        assertThat(nameless.status()).as("the name comes from sis, which is not running").isEqualTo(503);

        JsonNode cancelled = expect(fx.post(INVOICES + "/" + id + ":cancel", admin,
                Map.of("reason", "Wrong dues")), 200).body();
        assertThat(cancelled.path("status").asString()).isEqualTo("cancelled");
        assertThat(expect(fx.call("GET", DUES + "/" + t1, admin, null), 200).body().path("status").asString())
                .as("the dues are released").isEqualTo("draft");
        expect(fx.post(INVOICES + "/" + id + ":post", admin, Map.of()), 409);

        JsonNode again = fromDues(s1, t1, l1);
        String againId = again.path("id").asString();
        expect(fx.post(INVOICES + "/" + againId + ":post", admin, Map.of("etag", "\"9\"")), 412);
        JsonNode posted = expect(fx.post(INVOICES + "/" + againId + ":post", admin, Map.of()), 200).body();
        assertThat(posted.path("status").asString()).isEqualTo("posted");
        assertThat(posted.path("invoiceNumber").asString()).isEqualTo("INV-000001");
        expect(fx.post(INVOICES + "/" + againId + ":post", admin, Map.of()), 409);
        expect(fx.post(INVOICES + "/" + againId + ":cancel", admin, Map.of()), 409);
        expect(fx.call("PATCH", INVOICES + "/" + againId, admin, "{\"billToName\":\"Other\"}", "If-Match",
                posted.path("etag").asString()), 409);

        JsonNode second = fromDues(s1, fx.due(tenant, s1, tuition, 2));
        assertThat(expect(fx.post(INVOICES + "/" + second.path("id").asString() + ":post", admin, Map.of()), 200)
                .body().path("invoiceNumber").asString()).isEqualTo("INV-000002");

        JsonNode empty = expect(fx.post(INVOICES, admin, Map.of("institutionId", institution, "studentId", s1,
                "billToName", "Asha Rao", "invoiceDate", "2026-04-01", "currencyId", INR)), 201).body();
        expect(fx.post(INVOICES + "/" + empty.path("id").asString() + ":post", admin, Map.of()), 422);
        expect(fx.post(INVOICES, admin, Map.of("institutionId", institution, "billToName", "Nobody",
                "invoiceDate", "2026-04-01", "currencyId", INR)), 422);
    }

    @Test
    void aCounterPaymentIsAllocatedExactlyAndTheStatementAddsUp() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID invoice = postedInvoice(s1);
        UUID t2 = fx.due(tenant, s1, tuition, 2);

        expect(fx.post(PAYMENTS + ":record", admin, counter(s1, "15000.00",
                slice("invoiceId", invoice, "10000.00"), slice("studentFeeDueId", t2, "4000.00"))), 422);
        expect(fx.post(PAYMENTS + ":record", admin, counter(s1, "3680.01",
                slice("invoiceId", invoice, "3680.01"))), 201);
        Reply over = fx.post(PAYMENTS + ":record", admin, counter(s1, "10000.00",
                slice("invoiceId", invoice, "10000.00")));
        assertThat(over.status()).as(over.text()).isEqualTo(422);
        assertThat(over.text()).contains("OVER_ALLOCATED").contains("9999.99");
        expect(fx.post(PAYMENTS + ":record", admin, counter(s1, "10.00",
                slice("studentFeeDueId", fx.due(tenant, s1, tuition, 1), "10.00"))), 409);

        JsonNode payment = expect(fx.post(PAYMENTS + ":record", admin, counter(s1, "15000.00",
                slice("invoiceId", invoice, "9999.99"), slice("studentFeeDueId", t2, "5000.01"))), 201).body();
        assertThat(payment.path("status").asString()).isEqualTo("received");
        assertThat(payment.path("purpose").asString()).isEqualTo("fees");
        assertThat(payment.path("receiptNumber").asString()).isEqualTo("RCT-000002");
        assertThat(payment.path("allocations").findValuesAsString("amount")).containsExactly("9999.99", "5000.01");
        JsonNode paid = expect(fx.call("GET", INVOICES + "/" + invoice, admin, null), 200).body();
        assertThat(paid.path("amountPaid").asString()).isEqualTo("13680.00");
        assertThat(paid.path("status").asString()).isEqualTo("paid");

        JsonNode statement = expect(fx.call("GET", "/v1/finance/students/" + s1 + "/fee-statement", admin, null),
                200).body();
        // owed: the posted invoice 13680.00 + the due not on it 20520.00; paid: 3680.01 + 9999.99 + 5000.01
        assertThat(statement.path("totalDue").asString()).isEqualTo("34200.00");
        assertThat(statement.path("totalPaid").asString()).isEqualTo("18680.01");
        assertThat(statement.path("balance").asString()).isEqualTo("15519.99");
        assertThat(statement.path("currencyId").asString()).isEqualTo(INR.toString());
        assertThat(statement.path("dues").size()).isEqualTo(3);
        assertThat(statement.path("invoices").size()).isEqualTo(1);
        assertThat(statement.path("payments").size()).isEqualTo(2);
        expect(fx.call("GET", "/v1/finance/students/" + s1 + "/fee-statement?academicYearId=" + UUID.randomUUID(),
                admin, null), 404);
    }

    @Test
    void refundsArePeeledOffTheNewestSliceNeverExceedThePaymentAndAreSafeToRetry() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID invoice = postedInvoice(s1);
        UUID t2 = fx.due(tenant, s1, tuition, 2);
        JsonNode payment = expect(fx.post(PAYMENTS + ":record", admin, counter(s1, "15000.00",
                slice("invoiceId", invoice, "10000.00"), slice("studentFeeDueId", t2, "5000.00"))), 201).body();
        String refund = PAYMENTS + "/" + payment.path("id").asString() + ":refund";

        String key = UUID.randomUUID().toString();
        String body = FinanceFixtures.json(Map.of("amount", "6000.00", "refundMethod", "cash", "reason", "Left"));
        JsonNode first = expect(fx.call("POST", refund, admin, body, "Idempotency-Key", key), 200).body();
        assertThat(first.path("status").asString()).isEqualTo("processed");
        Reply retried = expect(fx.call("POST", refund, admin, body, "Idempotency-Key", key), 200);
        assertThat(retried.body().path("id").asString()).isEqualTo(first.path("id").asString());
        assertThat(fx.value(tenant, "SELECT COUNT(*) FROM finance.refund WHERE tenant_id = :tenantId", Long.class))
                .isEqualTo(1L);
        // 6000.00 back: the due's 5000.00 slice (listed last) first, then 1000.00 of the invoice's slice
        JsonNode invoiceNow = expect(fx.call("GET", INVOICES + "/" + invoice, admin, null), 200).body();
        assertThat(invoiceNow.path("amountPaid").asString()).isEqualTo("9000.00");
        assertThat(invoiceNow.path("status").asString()).isEqualTo("partially_paid");
        JsonNode statement = expect(fx.call("GET", "/v1/finance/students/" + s1 + "/fee-statement", admin, null),
                200).body();
        assertThat(statement.path("totalPaid").asString()).isEqualTo("9000.00");
        assertThat(statement.path("balance").asString()).isEqualTo("25200.00");

        Reply over = fx.post(refund, admin, Map.of("amount", "9000.01", "refundMethod", "cash"));
        assertThat(over.status()).as(over.text()).isEqualTo(422);
        assertThat(over.text()).contains("OVER_REFUND");
        expect(fx.post(refund, admin, Map.of("refundMethod", "gateway")), 422);
        JsonNode rest = expect(fx.post(refund, admin, Map.of("refundMethod", "bank_transfer")), 200).body();
        assertThat(rest.path("amount").asString()).isEqualTo("9000.00");
        JsonNode refunded = expect(fx.call("GET", PAYMENTS + "/" + payment.path("id").asString(), admin, null), 200)
                .body();
        assertThat(refunded.path("status").asString()).isEqualTo("refunded");
        invoiceNow = expect(fx.call("GET", INVOICES + "/" + invoice, admin, null), 200).body();
        assertThat(invoiceNow.path("amountPaid").asString()).isEqualTo("0.00");
        assertThat(invoiceNow.path("status").asString()).isEqualTo("posted");
        expect(fx.post(refund, admin, Map.of("refundMethod", "cash")), 409);

        JsonNode refunds = expect(fx.call("GET", "/v1/finance/refunds?filter=" + encode("paymentId = \""
                + payment.path("id").asString() + "\""), admin, null), 200).body();
        assertThat(refunds.path("data").findValuesAsString("amount")).containsExactlyInAnyOrder("6000.00", "9000.00");
    }

    @Test
    void twoPaymentsForOneDueAndTwoRefundsOfOnePaymentNeverSpendTheSameMoney() throws Exception {
        UUID s2 = UUID.randomUUID();
        fx.concession(tenant, institution, s2, year, tuition, "percent", "10");
        fx.generate(admin, plan, year, "2026-04-01", s2);
        UUID t2 = fx.due(tenant, s2, tuition, 2);
        List<Reply> payments = race(2, () -> fx.post(PAYMENTS + ":record", admin, counter(s2, "10000.00",
                slice("studentFeeDueId", t2, "10000.00"))));
        assertThat(payments).extracting(Reply::status).containsExactlyInAnyOrder(201, 422);
        String paid = payments.stream().filter(r -> r.status() == 201).findFirst().orElseThrow().body().path("id")
                .asString();

        List<Reply> refunds = race(2, () -> fx.post(PAYMENTS + "/" + paid + ":refund", admin,
                Map.of("amount", "6000.00", "refundMethod", "cash")));
        assertThat(refunds).extracting(Reply::status).containsExactlyInAnyOrder(200, 422);
        assertThat(fx.value(tenant, "SELECT SUM(amount) FROM finance.refund WHERE tenant_id = :tenantId",
                java.math.BigDecimal.class)).isEqualByComparingTo("6000.00");
    }

    @Test
    void familiesReadOnlyTheirOwnAndOtherSchoolsAndTenantsSeeNothing() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID invoice = postedInvoice(s1);
        UUID parent = UUID.randomUUID();
        Map<String, Object> byParent = counter(s1, "100.00", slice("invoiceId", invoice, "100.00"));
        byParent.put("payerUserId", parent);
        JsonNode payment = expect(fx.post(PAYMENTS + ":record", admin, byParent), 201).body();
        String paymentId = payment.path("id").asString();

        String family = fx.family(tenant, institution, parent);
        Reply statement = fx.call("GET", "/v1/finance/students/" + s1 + "/fee-statement", family, null);
        assertThat(statement.status()).as("only sis can say whose student it is: " + statement.text())
                .isEqualTo(503);
        expect(fx.call("GET", INVOICES + "/" + invoice, family, null), 503);
        expect(fx.call("GET", PAYMENTS + "/" + paymentId, family, null), 200);
        String stranger = fx.family(tenant, institution, UUID.randomUUID());
        expect(fx.call("GET", PAYMENTS + "/" + paymentId, stranger, null), 404);
        JsonNode theirs = expect(fx.call("GET", PAYMENTS + "?size=500", family, null), 200).body();
        assertThat(theirs.path("data").size()).isEqualTo(1);
        assertThat(theirs.path("data").get(0).path("id").asString()).isEqualTo(paymentId);
        assertThat(expect(fx.call("GET", PAYMENTS + "?size=500", stranger, null), 200).body().path("data").size())
                .isZero();

        String otherSchool = fx.office(tenant, UUID.randomUUID());
        Reply elsewhere = fx.call("GET", "/v1/finance/students/" + s1 + "/fee-statement", otherSchool, null);
        assertThat(elsewhere.status()).as("the student's records lie outside this office's reach, so only sis can say "
                + "whether the student is the clerk's own: " + elsewhere.text()).isEqualTo(503);
        expect(fx.call("GET", INVOICES + "/" + invoice, otherSchool, null), 404);
        expect(fx.call("GET", PAYMENTS + "/" + paymentId, otherSchool, null), 404);
        assertThat(expect(fx.call("GET", DUES + "?size=500", otherSchool, null), 200).body().path("data").size())
                .isZero();
        expect(fx.post(PAYMENTS + "/" + paymentId + ":refund", otherSchool, Map.of("refundMethod", "cash")), 404);

        UUID otherTenant = UUID.randomUUID();
        String foreign = fx.admin(otherTenant);
        expect(fx.call("GET", "/v1/finance/students/" + s1 + "/fee-statement", foreign, null), 404);
        expect(fx.call("GET", INVOICES + "/" + invoice, foreign, null), 404);
        expect(fx.call("GET", PAYMENTS + "/" + paymentId, foreign, null), 404);

        String office = fx.office(tenant, institution);
        assertThat(expect(fx.call("GET", DUES + "?size=500", office, null), 200).body().path("data").size())
                .isEqualTo(3);
        JsonNode own = expect(fx.call("GET", "/v1/finance/students/" + s1 + "/fee-statement", office, null), 200)
                .body();
        assertThat(own.path("totalPaid").asString()).isEqualTo("100.00");
        assertThat(own.path("payments").get(0).has("taxIdentity")).isFalse();
        assertThat(own.path("payments").get(0).path("payerUserId").asString())
                .as("the fee office reads every payment of its institution whole").isEqualTo(parent.toString());
        assertThat(own.path("payments").get(0).path("billingName").asString()).isEqualTo("Asha Rao");
    }

    @Test
    void aUserBilledDirectlyReadsAndPaysTheirOwnInvoice() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID invoice = postedInvoice(s1);
        UUID billed = UUID.randomUUID();
        // an invoice billed to a user rather than a student (an adult learner, a course buyer)
        fx.sql(tenant, "UPDATE finance.invoice SET student_id = NULL, bill_to_user_id = :user WHERE tenant_id = "
                + ":tenantId AND id = :id", "user", billed, "id", invoice);
        String user = fx.family(tenant, institution, billed);

        JsonNode theirs = expect(fx.call("GET", INVOICES + "/" + invoice, user, null), 200).body();
        assertThat(theirs.path("billToUserId").asString()).isEqualTo(billed.toString());
        // the list's filter for a caller outside the fee office (over HTTP it first asks sis for their children)
        SortSpec order = SortSpec.parse(null, InvoiceRepository.SORT_FIELDS, InvoiceRepository.DEFAULT_ORDER,
                InvoiceRepository.ID_COLUMN);
        DataScope noOffice = new DataScope(false, Set.of(), false);
        assertThat(fx.inTenant(tenant, () -> invoices.list(PageRequest.of(1, 50, null, order, null, Clock.systemUTC()),
                InvoiceRepository.reachableOrOwn(noOffice, Set.of(), billed)).items()))
                .extracting(InvoiceEntity::getId).containsExactly(invoice);
        assertThat(fx.inTenant(tenant, () -> invoices.list(PageRequest.of(1, 50, null, order, null, Clock.systemUTC()),
                InvoiceRepository.reachableOrOwn(noOffice, Set.of(), UUID.randomUUID())).items())).isEmpty();
        Reply quote = fx.post("/v1/finance/checkout:quote", user, Map.of("purpose", "invoice", "targetId", invoice));
        assertThat(quote.status()).as("theirs to pay; the currency is master's, not running here: " + quote.text())
                .isEqualTo(503);
        Reply start = fx.post("/v1/finance/checkout:start", user, Map.of("purpose", "invoice", "targetId", invoice,
                "billingName", "Asha Rao", "payerConsent", true));
        assertThat(start.status()).as(start.text()).isEqualTo(503);
        assertThat(start.text()).contains("PAYMENT_GATEWAY_NOT_CONFIGURED");

        String someoneElse = fx.family(tenant, institution, UUID.randomUUID());
        Reply notTheirs = fx.call("GET", INVOICES + "/" + invoice, someoneElse, null);
        assertThat(notTheirs.status()).as("billed to nobody they own; no student to ask sis about").isEqualTo(404);
        expect(fx.post("/v1/finance/checkout:quote", someoneElse, Map.of("purpose", "invoice", "targetId", invoice)),
                404);
    }

    @Test
    void aDuesInstitutionStaysWhenAPaymentIsRecordedAgainstItMeanwhile() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID t2 = fx.due(tenant, s1, tuition, 2);
        String etag = expect(fx.call("GET", DUES + "/" + t2, admin, null), 200).body().path("etag").asString();
        CountDownLatch paying = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // a counter payment as finance.RecordPayment writes it: the due locked, the payment and its allocation
            // written, the commit still to come
            Future<Object> payment = pool.submit(() -> fx.inTenant(tenant, () -> {
                database.jdbc().sql("SELECT id FROM finance.student_fee_due WHERE tenant_id = :tenantId AND id = :id "
                        + "FOR UPDATE").param("id", t2).query(UUID.class).single();
                UUID paymentId = UUID.randomUUID();
                database.jdbc().sql("INSERT INTO finance.payment (id, tenant_id, institution_id, purpose, "
                        + "original_amount, amount, amount_with_tax, currency_id, billing_name, payer_name, "
                        + "payer_consent, status, payment_method, row_version) VALUES (:id, :tenantId, :institution, "
                        + "'fees', 100, 100, 100, :currency, 'Asha Rao', 'Asha Rao', false, 'received', 'cash', 1)")
                        .param("id", paymentId).param("institution", institution).param("currency", INR).update();
                database.jdbc().sql("INSERT INTO finance.payment_allocation (id, tenant_id, payment_id, "
                        + "student_fee_due_id, amount, currency_id, row_version) VALUES (:id, :tenantId, :payment, "
                        + ":due, 100, :currency, 1)").param("id", UUID.randomUUID()).param("payment", paymentId)
                        .param("due", t2).param("currency", INR).update();
                paying.countDown();
                await(commit);
                return null;
            }));
            assertThat(paying.await(30, TimeUnit.SECONDS)).isTrue();
            Future<Reply> move = pool.submit(() -> fx.call("PATCH", DUES + "/" + t2, admin,
                    FinanceFixtures.json(Map.of("institutionId", UUID.randomUUID())), "If-Match", etag));
            assertWaiting(move);
            commit.countDown();
            payment.get(30, TimeUnit.SECONDS);
            Reply moved = move.get(30, TimeUnit.SECONDS);
            assertThat(moved.status()).as("the payment was taken at the due's institution: " + moved.text())
                    .isEqualTo(409);
        } finally {
            commit.countDown();
            pool.shutdownNow();
        }
        assertThat(expect(fx.call("GET", DUES + "/" + t2, admin, null), 200).body().path("institutionId")
                .asString()).isEqualTo(institution.toString());
    }

    @Test
    void remindersArePublishedForOpenInvoicesForCommsToDeliver() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID invoice = postedInvoice(s1);
        UUID unknown = UUID.randomUUID();
        JsonNode sent = expect(fx.post(INVOICES + ":sendReminders", admin, Map.of("institutionId", institution,
                "invoiceIds", List.of(invoice, unknown), "channel", "sms")), 200).body();
        assertThat(sent.path("queued").asInt()).isEqualTo(1);
        assertThat(sent.path("skipped").asInt()).isEqualTo(1);
        assertThat(fx.value(tenant, "SELECT payload FROM finance.outbox_event WHERE tenant_id = :tenantId "
                + "AND event_type = 'finance.fee_reminder.requested'", String.class))
                .contains(invoice.toString()).contains(s1.toString()).contains("sms");
        expect(fx.post(INVOICES + ":sendReminders", fx.office(tenant, UUID.randomUUID()),
                Map.of("institutionId", institution, "channel", "email")), 403);
    }

    @Test
    void aDueChangesOnlyWhileItIsNotBilled() throws Exception {
        UUID s1 = UUID.randomUUID();
        fx.generate(admin, plan, year, "2026-04-01", s1);
        UUID t2 = fx.due(tenant, s1, tuition, 2);
        JsonNode due = expect(fx.call("GET", DUES + "/" + t2, admin, null), 200).body();
        JsonNode moved = expect(fx.call("PATCH", DUES + "/" + t2, admin, "{\"dueDate\":\"2026-07-15\"}",
                "If-Match", due.path("etag").asString()), 200).body();
        assertThat(moved.path("dueDate").asString()).isEqualTo("2026-07-15");
        fromDues(s1, t2);
        String billed = expect(fx.call("GET", DUES + "/" + t2, admin, null), 200).body().path("etag").asString();
        expect(fx.call("PATCH", DUES + "/" + t2, admin, "{\"dueDate\":\"2026-08-01\"}", "If-Match", billed), 409);
    }

    // ---- helpers ----

    private JsonNode fromDues(UUID student, UUID... dues) throws Exception {
        return expect(fx.post(INVOICES + ":fromDues", admin, Map.of("studentId", student, "dueIds", List.of(dues),
                "billToName", "Asha Rao")), 200).body();
    }

    /** The posted invoice of the student's installment-1 dues (13680.00). */
    private UUID postedInvoice(UUID student) throws Exception {
        JsonNode draft = fromDues(student, fx.due(tenant, student, tuition, 1), fx.due(tenant, student, lab, 1));
        String id = draft.path("id").asString();
        expect(fx.post(INVOICES + "/" + id + ":post", admin, Map.of()), 200);
        return UUID.fromString(id);
    }

    private static Map<String, Object> slice(String target, UUID id, String amount) {
        return Map.of(target, id, "amount", amount);
    }

    @SafeVarargs
    private Map<String, Object> counter(UUID student, String amount, Map<String, Object>... slices) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("payerName", "Parent of " + student.toString().substring(0, 4));
        body.put("billingName", "Asha Rao");
        body.put("amount", amount);
        body.put("currencyId", INR);
        body.put("paymentMethod", "cash");
        body.put("allocations", List.of(slices));
        return body;
    }

    /**
     * Asserts the request is held up: unanswered after two seconds while the due is locked (it answers in
     * milliseconds otherwise). The service's sessions are not visible to the test's role, so the wait is timed.
     */
    private static void assertWaiting(Future<Reply> request) throws Exception {
        try {
            Reply early = request.get(2, TimeUnit.SECONDS);
            throw new AssertionError("answered while the due was locked: " + early.status() + " " + early.text());
        } catch (java.util.concurrent.TimeoutException waiting) {
            // still waiting for the due's lock
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private static List<Reply> race(int parties, Callable<Reply> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(parties);
        try {
            List<Future<Reply>> futures = new ArrayList<>();
            for (int i = 0; i < parties; i++) {
                futures.add(pool.submit(call));
            }
            List<Reply> replies = new ArrayList<>();
            for (Future<Reply> future : futures) {
                replies.add(future.get());
            }
            return replies;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
