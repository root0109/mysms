package com.mysms.finance;

import static com.mysms.finance.FinanceFixtures.INR;
import static com.mysms.finance.FinanceFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.finance.FinanceFixtures.Reply;
import com.mysms.finance.config.FinanceDatabaseConfig;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
 * Money through the gateway, over HTTP against real PostgreSQL: the gateway's signed notifications record a payment
 * once (a redelivery or a second event of the same payment changes nothing), a bad signature or another amount is
 * refused, a notification of an order MySMS does not know is acknowledged without effect, and a refund settles from
 * its notification. The gateway itself is never called: its address points at a closed port, so a checkout or a
 * gateway refund stops at 503 — a checkout writes nothing; a refund that never reached the gateway fails and releases
 * its amount; one whose request may have reached it (a timeout, recorded here as the service leaves it) stays
 * initiated while the gateway cannot be asked, and a retry with the same key answers it without asking for a second
 * refund. master is not running either, so a checkout's currency is 503 too.
 *
 * <p>The invoice: one Tuition due of 1000.00 (no discount), posted; the order of its checkout is 100000 paise.
 */
@SpringBootTest(classes = FinanceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String WEBHOOK = "/v1/finance/payment-gateways/razorpay:webhook";
    private static final String WEBHOOK_SECRET = "whsec-test-5678";
    private static final String PAYMENTS = "/v1/finance/payments/";
    private static final String CHECKOUT = "/v1/finance/checkout:";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(FinanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private FinanceFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;
    private UUID account;
    private UUID invoice;
    private UUID parent;
    private UUID student;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "finance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.finance.razorpay-url", () -> "http://127.0.0.1:9/v1/");
        registry.add("mysms.finance.razorpay-timeout", () -> "PT2S");
    }

    @BeforeEach
    void school() throws Exception {
        fx = new FinanceFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
        parent = UUID.randomUUID();
        UUID tuition = fx.feeHead(tenant, institution, "Tuition", "1000.00");
        UUID plan = fx.plan(tenant, institution, null);
        fx.component(tenant, fx.installment(tenant, plan, 1, 0, "100.000"), tuition, 1, "100.000");
        student = UUID.randomUUID();
        fx.generate(admin, plan, UUID.randomUUID(), "2026-04-01", student);
        JsonNode draft = expect(fx.post("/v1/finance/invoices:fromDues", admin, Map.of("studentId", student,
                "dueIds", List.of(fx.due(tenant, student, tuition, 1)), "billToName", "Asha Rao")), 200).body();
        invoice = UUID.fromString(draft.path("id").asString());
        expect(fx.post("/v1/finance/invoices/" + invoice + ":post", admin, Map.of()), 200);
        account = UUID.fromString(expect(fx.post("/v1/finance/payment-gateway-accounts", admin, Map.of(
                "institutionId", institution, "gatewayName", "razorpay", "displayName", "Main account",
                "keyId", "rzp_test_key", "keySecret", "key-secret-1234", "webhookSecret", WEBHOOK_SECRET,
                "isDefault", true)), 201).body().path("id").asString());
    }

    @Test
    void aSignedCaptureRecordsThePaymentOnceAndAllocatesIt() throws Exception {
        UUID payment = initiated("order_A1", "1000.00", 100000L);
        String capture = captured("pay_A1", "order_A1", 100000L, payment);

        expect(webhook(capture, sign(capture), "evt_1"), 204);
        JsonNode received = expect(fx.call("GET", PAYMENTS + payment, admin, null), 200).body();
        assertThat(received.path("status").asString()).isEqualTo("received");
        assertThat(received.path("gatewayPaymentId").asString()).isEqualTo("pay_A1");
        assertThat(received.path("allocations").findValuesAsString("amount")).containsExactly("1000.00");
        JsonNode paid = expect(fx.call("GET", "/v1/finance/invoices/" + invoice, admin, null), 200).body();
        assertThat(paid.path("status").asString()).isEqualTo("paid");
        assertThat(paid.path("amountPaid").asString()).isEqualTo("1000.00");

        expect(webhook(capture, sign(capture), "evt_1"), 204);
        String orderPaid = capture.replace("payment.captured", "order.paid");
        expect(webhook(orderPaid, sign(orderPaid), "evt_2"), 204);
        assertThat(count("finance.payment_allocation")).as("the money counts once").isEqualTo(1L);
        assertThat(count("finance.gateway_event")).isEqualTo(2L);
        JsonNode family = expect(fx.call("GET", PAYMENTS + payment, fx.family(tenant, institution, parent), null),
                200).body();
        assertThat(family.path("status").asString()).as("the payer reads their own payment").isEqualTo("received");
    }

    @Test
    void anUnsignedOrMismatchedOrForeignNotificationRecordsNothing() throws Exception {
        UUID payment = initiated("order_B1", "1000.00", 100000L);
        String capture = captured("pay_B1", "order_B1", 100000L, payment);
        expect(webhook(capture, sign(capture.replace("100000", "1")), "evt_3"), 401);
        expect(webhook(capture, null, "evt_3"), 401);

        String cheaper = captured("pay_B1", "order_B1", 99900L, payment);
        Reply mismatch = expect(webhook(cheaper, sign(cheaper), "evt_4"), 422);
        assertThat(mismatch.text()).contains("GATEWAY_AMOUNT_MISMATCH");
        assertThat(fx.value(tenant, "SELECT processing_error FROM finance.gateway_event WHERE tenant_id = :tenantId "
                + "AND gateway_event_id = 'evt_4'", String.class)).isEqualTo("AMOUNT_MISMATCH");
        expect(webhook(cheaper, sign(cheaper), "evt_4"), 204);
        assertThat(status(payment)).isEqualTo("initiated");

        String stranger = capture.replace(tenant.toString(), UUID.randomUUID().toString());
        expect(webhook(stranger, sign(stranger), "evt_5"), 204);
        String unknownOrder = captured("pay_X", "order_unknown", 100000L, payment);
        expect(webhook(unknownOrder, sign(unknownOrder), "evt_6"), 204);
        expect(webhook("{\"event\":\"payment.captured\"}", sign("{\"event\":\"payment.captured\"}"), "evt_7"), 204);
        expect(webhook("not json", "x", "evt_8"), 400);
        expect(webhook(capture, sign(capture), null), 400);
        expect(fx.call("POST", "/v1/finance/payment-gateways/stripe:webhook", null, capture), 404);
        assertThat(status(payment)).isEqualTo("initiated");
        assertThat(count("finance.payment_allocation")).isZero();

        String failed = capture.replace("payment.captured", "payment.failed").replace("\"captured\"", "\"failed\"");
        expect(webhook(failed, sign(failed), "evt_9"), 204);
        assertThat(status(payment)).isEqualTo("failed");
    }

    @Test
    void aGatewayRefundThatNeverReachedTheGatewayFailsAndReleasesItsAmount() throws Exception {
        UUID payment = received("order_C0", "pay_C0", "evt_C0");

        String key = UUID.randomUUID().toString();
        String body = FinanceFixtures.json(Map.of("amount", "400.00", "reason", "Withdrawn"));
        Reply unreachable = fx.call("POST", PAYMENTS + payment + ":refund", admin, body, "Idempotency-Key", key);
        assertThat(unreachable.status()).as("no connection to the gateway: " + unreachable.text()).isEqualTo(503);
        assertThat(fx.value(tenant, "SELECT status FROM finance.refund WHERE tenant_id = :tenantId", String.class))
                .as("nothing reached the gateway, so nothing was refunded").isEqualTo("failed");
        JsonNode retried = expect(fx.call("POST", PAYMENTS + payment + ":refund", admin, body,
                "Idempotency-Key", key), 200).body();
        assertThat(retried.path("status").asString()).isEqualTo("failed");
        assertThat(count("finance.refund")).as("a retry never opens a second refund").isEqualTo(1L);
        Reply whole = fx.post(PAYMENTS + payment + ":refund", admin, Map.of("amount", "1000.00"));
        assertThat(whole.status()).as("the failed 400.00 is not committed: " + whole.text()).isEqualTo(503);
        assertThat(fx.value(tenant, "SELECT COUNT(*) FROM finance.refund WHERE tenant_id = :tenantId "
                + "AND status = 'failed'", Long.class)).isEqualTo(2L);
        assertThat(status(payment)).isEqualTo("received");
    }

    @Test
    void aGatewayRefundWithoutAnAnswerStaysInitiatedIsNotRepeatedAndSettlesFromItsNotification() throws Exception {
        UUID payment = received("order_C1", "pay_C1", "evt_10");
        UUID key = UUID.randomUUID();
        UUID refund = initiatedRefund(payment, "400.00", key);

        String body = FinanceFixtures.json(Map.of("amount", "400.00", "reason", "Withdrawn"));
        JsonNode retried = expect(fx.call("POST", PAYMENTS + payment + ":refund", admin, body,
                "Idempotency-Key", key.toString()), 200).body();
        assertThat(retried.path("id").asString()).isEqualTo(refund.toString());
        assertThat(retried.path("status").asString()).as("the gateway cannot say yet").isEqualTo("initiated");
        assertThat(count("finance.refund")).as("a retry never refunds twice").isEqualTo(1L);
        Reply over = fx.post(PAYMENTS + payment + ":refund", admin, Map.of("amount", "600.01"));
        assertThat(over.status()).as("400.00 is being refunded: " + over.text()).isEqualTo(422);

        String processed = refundNotification("refund.processed", "rfnd_C1", "pay_C1", 40000, refund);
        expect(webhook(processed, sign(processed), "evt_11"), 204);
        JsonNode settled = expect(fx.call("GET", "/v1/finance/refunds/" + refund, admin, null), 200).body();
        assertThat(settled.path("status").asString()).isEqualTo("processed");
        assertThat(settled.path("gatewayRefundId").asString()).isEqualTo("rfnd_C1");
        JsonNode invoiceNow = expect(fx.call("GET", "/v1/finance/invoices/" + invoice, admin, null), 200).body();
        assertThat(invoiceNow.path("amountPaid").asString()).isEqualTo("600.00");
        assertThat(invoiceNow.path("status").asString()).isEqualTo("partially_paid");

        // a refund whose first request got no answer, settled by its notification before the client retries
        UUID lateKey = UUID.randomUUID();
        UUID late = initiatedRefund(payment, "100.00", lateKey);
        String lateProcessed = refundNotification("refund.processed", "rfnd_C2", "pay_C1", 10000, late);
        expect(webhook(lateProcessed, sign(lateProcessed), "evt_12"), 204);
        String stale = FinanceFixtures.json(Map.of("amount", "100.00", "etag", "\"1\""));
        JsonNode again = expect(fx.call("POST", PAYMENTS + payment + ":refund", admin, stale,
                "Idempotency-Key", lateKey.toString()), 200).body();
        assertThat(again.path("id").asString()).as("a retry answers its refund, whatever the payment's etag now")
                .isEqualTo(late.toString());
        assertThat(again.path("status").asString()).isEqualTo("processed");
    }

    @Test
    void aRefundMadeAtTheGatewayAloneIsPublishedNotBooked() throws Exception {
        UUID payment = received("order_E1", "pay_E1", "evt_E1");
        String outside = json(Map.of("event", "refund.processed", "payload", Map.of("refund", Map.of("entity",
                Map.of("id", "rfnd_E1", "payment_id", "pay_E1", "amount", 25000, "status", "processed",
                        "notes", Map.of("mysms_tenant", tenant.toString()))))));
        expect(webhook(outside, sign(outside), "evt_E2"), 204);
        assertThat(count("finance.refund")).isZero();
        assertThat(fx.value(tenant, "SELECT payload FROM finance.outbox_event WHERE tenant_id = :tenantId "
                + "AND event_type = 'finance.payment.external_refund_seen'", String.class))
                .contains(payment.toString()).contains("rfnd_E1").contains("25000");
        JsonNode untouched = expect(fx.call("GET", "/v1/finance/invoices/" + invoice, admin, null), 200).body();
        assertThat(untouched.path("amountPaid").asString()).isEqualTo("1000.00");
        expect(webhook(outside, sign(outside), "evt_E2"), 204);
        assertThat(fx.value(tenant, "SELECT COUNT(*) FROM finance.outbox_event WHERE tenant_id = :tenantId "
                + "AND event_type = 'finance.payment.external_refund_seen'", Long.class)).as("once per event")
                .isEqualTo(1L);
    }

    @Test
    void aRefundGivesBackThePartOfAPaymentNoAllocationHoldsFirst() throws Exception {
        UUID online = initiated("order_D1", "1000.00", 100000L);
        expect(fx.post("/v1/finance/payments:record", admin, Map.of("payerName", "Asha Rao", "billingName",
                "Asha Rao", "amount", "400.00", "currencyId", INR, "paymentMethod", "cash",
                "allocations", List.of(Map.of("invoiceId", invoice, "amount", "400.00")))), 201);
        String capture = captured("pay_D1", "order_D1", 100000L, online);
        expect(webhook(capture, sign(capture), "evt_D1"), 204);
        JsonNode received = expect(fx.call("GET", PAYMENTS + online, admin, null), 200).body();
        assertThat(received.path("allocations").findValuesAsString("amount"))
                .as("600.00 were still owed; 400.00 of the money came in beyond it").containsExactly("600.00");

        JsonNode surplus = expect(fx.post(PAYMENTS + online + ":refund", admin, Map.of("amount", "400.00",
                "refundMethod", "bank_transfer")), 200).body();
        assertThat(surplus.path("status").asString()).isEqualTo("processed");
        JsonNode paid = expect(fx.call("GET", "/v1/finance/invoices/" + invoice, admin, null), 200).body();
        assertThat(paid.path("amountPaid").asString()).as("the surplus went back, not the invoice's money")
                .isEqualTo("1000.00");
        assertThat(paid.path("status").asString()).isEqualTo("paid");
        JsonNode statement = expect(fx.call("GET", "/v1/finance/students/" + student + "/fee-statement", admin,
                null), 200).body();
        assertThat(statement.path("totalPaid").asString()).isEqualTo("1000.00");
        assertThat(statement.path("balance").asString()).isEqualTo("0.00");

        expect(fx.post(PAYMENTS + online + ":refund", admin, Map.of("amount", "100.00", "refundMethod", "cash")),
                200);
        JsonNode partly = expect(fx.call("GET", "/v1/finance/invoices/" + invoice, admin, null), 200).body();
        assertThat(partly.path("amountPaid").asString()).as("beyond the surplus, the slice gives back")
                .isEqualTo("900.00");
        assertThat(partly.path("status").asString()).isEqualTo("partially_paid");
        expect(fx.post("/v1/finance/payments:record", admin, Map.of("payerName", "Asha Rao", "billingName",
                "Asha Rao", "amount", "100.01", "currencyId", INR, "paymentMethod", "cash",
                "allocations", List.of(Map.of("invoiceId", invoice, "amount", "100.01")))), 422);
    }

    @Test
    void aCheckoutWithoutTheGatewayOrMasterWritesNothing() throws Exception {
        Map<String, Object> start = Map.of("purpose", "invoice", "targetId", invoice, "billingName", "Asha Rao",
                "payerConsent", true);
        Reply unavailable = fx.post(CHECKOUT + "start", admin, start);
        assertThat(unavailable.status()).as(unavailable.text()).isEqualTo(503);
        expect(fx.post(CHECKOUT + "quote", admin, Map.of("purpose", "invoice", "targetId", invoice)), 503);
        assertThat(count("finance.payment")).isZero();

        expect(fx.post(CHECKOUT + "start", admin, Map.of("purpose", "invoice", "targetId", invoice,
                "billingName", "Asha Rao", "payerConsent", false)), 422);
        expect(fx.post(CHECKOUT + "start", admin, Map.of("purpose", "lms_course", "targetId", invoice,
                "billingName", "Asha Rao", "payerConsent", true)), 422);
        Reply noKey = fx.call("POST", CHECKOUT + "start", admin, json(start));
        assertThat(noKey.status()).isEqualTo(400);
        expect(fx.post(CHECKOUT + "start", fx.family(tenant, institution, parent), start), 503);
        expect(fx.post(CHECKOUT + "start", admin, Map.of("purpose", "invoice", "targetId", UUID.randomUUID(),
                "billingName", "Asha Rao", "payerConsent", true)), 404);

        fx.sql(tenant, "UPDATE finance.payment_gateway_account SET is_active = false WHERE tenant_id = :tenantId "
                + "AND id = :id", "id", account);
        Reply notSetUp = fx.post(CHECKOUT + "start", admin, start);
        assertThat(notSetUp.status()).isEqualTo(503);
        assertThat(notSetUp.text()).contains("PAYMENT_GATEWAY_NOT_CONFIGURED");
        assertThat(count("finance.payment")).isZero();
    }

    // ---- helpers ----

    /** A checkout's payment as StartCheckout leaves it: initiated, for the invoice, with its gateway order. */
    private UUID initiated(String orderId, String amount, long minor) {
        UUID id = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO finance.payment (id, tenant_id, institution_id, payer_user_id, purpose, invoice_id, "
                + "gateway_name, gateway_order_id, original_amount, amount, amount_with_tax, currency_id, billing_name, "
                + "payer_consent, status, payment_method, gateway_account_id, gateway_currency, gateway_amount_minor, "
                + "row_version) VALUES (:id, :tenantId, :institution, :parent, 'invoice', :invoice, 'razorpay', :order, "
                + ":amount, :amount, :amount, :currency, 'Asha Rao', true, 'initiated', 'gateway', :account, 'INR', "
                + ":minor, 1)", "id", id, "institution", institution, "parent", parent, "invoice", invoice,
                "order", orderId, "amount", new java.math.BigDecimal(amount), "currency", INR, "account", account,
                "minor", minor);
        return id;
    }

    /** A checkout's payment of 1000.00 for the invoice, received through a signed capture. */
    private UUID received(String orderId, String gatewayPaymentId, String eventId) throws Exception {
        UUID payment = initiated(orderId, "1000.00", 100000L);
        String capture = captured(gatewayPaymentId, orderId, 100000L, payment);
        expect(webhook(capture, sign(capture), eventId), 204);
        return payment;
    }

    /**
     * A gateway refund as the service leaves it when its request to the gateway timed out: initiated under the
     * request's Idempotency-Key, its amount committed (the edge keeps no 503 answer, so a retry reaches the service).
     */
    private UUID initiatedRefund(UUID payment, String amount, UUID key) {
        UUID id = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO finance.refund (id, tenant_id, payment_id, amount, reason, status, refund_method, "
                + "request_key, row_version) VALUES (:id, :tenantId, :payment, :amount, 'Withdrawn', 'initiated', "
                + "'gateway', :key, 1)", "id", id, "payment", payment, "amount", new java.math.BigDecimal(amount),
                "key", key);
        return id;
    }

    private String refundNotification(String event, String gatewayRefundId, String gatewayPaymentId, long minor,
            UUID refund) {
        return json(Map.of("event", event, "payload", Map.of("refund", Map.of("entity", Map.of("id", gatewayRefundId,
                "payment_id", gatewayPaymentId, "amount", minor, "status", event.substring("refund.".length()),
                "notes", Map.of("mysms_tenant", tenant.toString(), "mysms_refund", refund.toString()))))));
    }

    private String captured(String paymentId, String orderId, long minor, UUID payment) {
        return json(Map.of("event", "payment.captured", "payload", Map.of("payment", Map.of("entity", Map.of(
                "id", paymentId, "order_id", orderId, "amount", minor, "currency", "INR", "status", "captured",
                "method", "upi", "notes", Map.of("mysms_tenant", tenant.toString(),
                        "mysms_payment", payment.toString()))))));
    }

    private Reply webhook(String body, String signature, String eventId) throws Exception {
        List<String> headers = new java.util.ArrayList<>();
        if (signature != null) {
            headers.addAll(List.of("X-Razorpay-Signature", signature));
        }
        if (eventId != null) {
            headers.addAll(List.of("X-Razorpay-Event-Id", eventId));
        }
        return fx.call("POST", WEBHOOK, null, body, headers.toArray(String[]::new));
    }

    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private String status(UUID payment) {
        return fx.value(tenant, "SELECT status FROM finance.payment WHERE tenant_id = :tenantId AND id = :id",
                String.class, "id", payment);
    }

    private long count(String table) {
        return fx.value(tenant, "SELECT COUNT(*) FROM " + table + " WHERE tenant_id = :tenantId", Long.class);
    }

    private static String json(Object value) {
        return FinanceFixtures.json(value);
    }
}
