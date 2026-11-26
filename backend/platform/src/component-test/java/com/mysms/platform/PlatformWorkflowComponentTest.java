package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.platform.PlatformCalls.Reply;
import com.mysms.platform.config.PlatformDatabaseConfig;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
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
 * The platform's workflows over HTTP on real PostgreSQL with real signed tokens: subscription payments proved by the
 * checkout signature and by the gateway's signed webhook (real HMAC signatures, idempotent), cancellation, the audit
 * trail's access rules, tags and themes' own rules, branding, enquiry follow-up, demo-data clearance, my storage,
 * search and reports. The gateway's API and the stock-photo service point at a closed local port (503 UNAVAILABLE);
 * no file store and no master address are configured (503 where they are needed).
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String KEY_SECRET = "platform-key-secret";
    private static final String WEBHOOK_SECRET = "platform-webhook-secret";
    private static final String CLOSED = "http://127.0.0.1:9";
    private static final UUID TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f2001");
    private static final UUID OTHER_TENANT = UUID.fromString("0190b0a0-0000-7000-8000-0000000f2002");
    private static final UUID SCHOOL = UUID.fromString("0190b0a0-0000-7000-8000-0000000f2003");
    private static final String CHECKOUT = "platform.tenant_subscription.start_subscription_checkout";
    private static final String CANCEL = "platform.tenant_subscription.cancel_subscription";
    private static final String SUBSCRIPTION_READ = "platform.tenant_subscription.read";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private PlatformCalls http;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "platform", "master", "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.platform.razorpay.url", () -> CLOSED);
        registry.add("mysms.platform.razorpay.key-id", () -> "rzp_test_platform");
        registry.add("mysms.platform.razorpay.key-secret", () -> KEY_SECRET);
        registry.add("mysms.platform.razorpay.webhook-secret", () -> WEBHOOK_SECRET);
        registry.add("mysms.platform.stock-images.url", () -> CLOSED);
        registry.add("mysms.platform.stock-images.api-key", () -> "pexels-test-key");
    }

    @BeforeEach
    void tenants() {
        http = new PlatformCalls(TOKENS, () -> port, database);
        http.tenant(TENANT, "flow-a");
        http.tenant(OTHER_TENANT, "flow-b");
    }

    // ---------------------------------------------------------------------------------------------- subscriptions

    @Test
    void theCheckoutSignatureAloneActivatesNothingTheSettledPaymentDoesOnce() {
        UUID subscription = pendingSubscription(TENANT, null, "order_Confirm1", 12);
        String admin = http.admin(TENANT, UUID.randomUUID(), CHECKOUT, SUBSCRIPTION_READ);
        String path = "/v1/platform/tenant-subscriptions/" + subscription + ":confirmCheckout";
        Reply forged = http.call("POST", path, admin, confirm("order_Confirm1", "pay_1", "00ff"));
        assertThat(forged.status()).as(forged.text()).isEqualTo(422);
        assertThat(forged.text()).contains("INVALID_SIGNATURE");
        Reply otherOrder = http.call("POST", path, admin, confirm("order_Other", "pay_1",
                hmac(KEY_SECRET, "order_Other|pay_1")));
        assertThat(otherOrder.status()).as("a proof for another order").isEqualTo(422);
        String school = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), CHECKOUT);
        assertThat(http.call("POST", path, school, confirm("order_Confirm1", "pay_1",
                hmac(KEY_SECRET, "order_Confirm1|pay_1"))).status())
                .as("a tenant-wide subscription changes only through a tenant-wide grant").isEqualTo(403);

        Reply signed = http.call("POST", path, admin, confirm("order_Confirm1", "pay_1",
                hmac(KEY_SECRET, "order_Confirm1|pay_1")));
        assertThat(signed.status()).as(signed.text()).isEqualTo(200);
        assertThat(signed.body().path("status").asString())
                .as("the gateway (not reachable here) has not reported the payment captured").isEqualTo("pending");
        assertThat(state(subscription)).isEqualTo("pending:1");
        byte[] paid = orderPaid("order_Confirm1", TENANT, subscription);
        assertThat(webhook(paid, hmac(WEBHOOK_SECRET, paid)).status()).isEqualTo(204);
        Reply done = http.call("POST", path, admin, confirm("order_Confirm1", "pay_1",
                hmac(KEY_SECRET, "order_Confirm1|pay_1")));
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(done.body().path("status").asString()).isEqualTo("active");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(done.body().path("startDate").asString()).isEqualTo(today.toString());
        assertThat(done.body().path("endDate").asString()).isEqualTo(today.plusMonths(12).minusDays(1).toString());
        assertThat(done.body().path("amountPaid").asString()).isEqualTo("1200.00");
        assertThat(done.etag()).isEqualTo("\"2\"");
        Reply repeat = http.call("POST", path, admin, confirm("order_Confirm1", "pay_1",
                hmac(KEY_SECRET, "order_Confirm1|pay_1")));
        assertThat(repeat.status()).isEqualTo(200);
        assertThat(repeat.etag()).as("activated once").isEqualTo("\"2\"");
        assertThat(http.call("POST", path, http.admin(OTHER_TENANT, UUID.randomUUID(), CHECKOUT),
                confirm("order_Confirm1", "pay_1", hmac(KEY_SECRET, "order_Confirm1|pay_1"))).status())
                .isEqualTo(404);

        UUID renewal = pendingSubscription(TENANT, null, "order_Renew", 1);
        byte[] renewalPaid = orderPaid("order_Renew", TENANT, renewal);
        assertThat(webhook(renewalPaid, hmac(WEBHOOK_SECRET, renewalPaid)).status()).isEqualTo(204);
        Reply renewed = http.call("GET", "/v1/platform/tenant-subscriptions/" + renewal, admin, null);
        assertThat(renewed.body().path("startDate").asString()).as("a renewal starts after the paid period")
                .isEqualTo(today.plusMonths(12).toString());
    }

    @Test
    void theGatewaysSignedWebhookActivatesIdempotently() {
        UUID subscription = pendingSubscription(TENANT, SCHOOL, "order_Hook1", 6);
        byte[] paid = orderPaid("order_Hook1", TENANT, subscription);
        Reply unsigned = webhook(paid, "deadbeef");
        assertThat(unsigned.status()).as(unsigned.text()).isEqualTo(401);
        assertThat(state(subscription)).isEqualTo("pending:1");
        Reply tampered = webhook(orderPaid("order_Hook1", TENANT, subscription, "netbanking"),
                hmac(WEBHOOK_SECRET, paid));
        assertThat(tampered.status()).as("the signature covers these exact bytes").isEqualTo(401);

        Reply done = webhook(paid, hmac(WEBHOOK_SECRET, paid));
        assertThat(done.status()).as(done.text()).isEqualTo(204);
        assertThat(state(subscription)).isEqualTo("active:2");
        assertThat(http.query(TENANT, "SELECT payment_mode || ' ' || payer_email FROM platform.tenant_subscription "
                + "WHERE id = :id", Map.of("id", subscription), String.class)).isEqualTo("upi parent@example.org");
        assertThat(webhook(paid, hmac(WEBHOOK_SECRET, paid)).status()).isEqualTo(204);
        assertThat(state(subscription)).as("a redelivery changes nothing").isEqualTo("active:2");
        Reply read = http.call("GET", "/v1/platform/tenant-subscriptions/" + subscription,
                http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), SUBSCRIPTION_READ), null);
        assertThat(read.body().has("payerEmail")).as("payer contact is kept, never returned").isFalse();

        byte[] other = "{\"event\":\"payment.failed\",\"payload\":{}}".getBytes(StandardCharsets.UTF_8);
        assertThat(webhook(other, hmac(WEBHOOK_SECRET, other)).status()).isEqualTo(204);
        byte[] stranger = orderPaid("order_X", UUID.randomUUID(), UUID.randomUUID());
        assertThat(webhook(stranger, hmac(WEBHOOK_SECRET, stranger)).status()).as("not a tenant of this platform")
                .isEqualTo(204);
        byte[] wrongOrder = orderPaid("order_Wrong", TENANT, subscription);
        assertThat(webhook(wrongOrder, hmac(WEBHOOK_SECRET, wrongOrder)).status()).isEqualTo(204);
        byte[] noNotes = ("{\"event\":\"order.paid\",\"payload\":{\"order\":{\"entity\":{\"id\":\"order_N\"}}}}")
                .getBytes(StandardCharsets.UTF_8);
        assertThat(webhook(noNotes, hmac(WEBHOOK_SECRET, noNotes)).status())
                .as("an order the platform did not open is acknowledged, so the gateway stops redelivering it")
                .isEqualTo(204);
        byte[] otherNotes = ("{\"event\":\"order.paid\",\"payload\":{\"order\":{\"entity\":{\"id\":\"order_O\","
                + "\"notes\":{\"tenantId\":\"not-an-id\",\"invoice\":\"INV-7\"}}}}}").getBytes(StandardCharsets.UTF_8);
        assertThat(webhook(otherNotes, hmac(WEBHOOK_SECRET, otherNotes)).status()).isEqualTo(204);
        byte[] notJson = "not json".getBytes(StandardCharsets.UTF_8);
        assertThat(webhook(notJson, hmac(WEBHOOK_SECRET, notJson)).status()).isEqualTo(400);

        UUID cancelled = pendingSubscription(TENANT, null, "order_Late", 1);
        http.sql(TENANT, "UPDATE platform.tenant_subscription SET status = 'cancelled' WHERE id = :id",
                Map.of("id", cancelled));
        byte[] late = orderPaid("order_Late", TENANT, cancelled);
        assertThat(webhook(late, hmac(WEBHOOK_SECRET, late)).status()).isEqualTo(204);
        assertThat(http.query(TENANT, "SELECT status || ':' || gateway_status FROM platform.tenant_subscription "
                + "WHERE id = :id", Map.of("id", cancelled), String.class))
                .as("money for a cancelled subscription is kept for a refund").isEqualTo("cancelled:paid_after_cancel");
    }

    @Test
    void checkoutNeedsAPricedPlanAndTheGateway() {
        String admin = http.admin(TENANT, UUID.randomUUID(), CHECKOUT);
        UUID plan = plan("gold", "1200.00", null, true);
        Reply unreachable = checkout(admin, plan);
        assertThat(unreachable.status()).as(unreachable.text()).isEqualTo(503);
        assertThat(unreachable.body().path("code").asString()).isEqualTo("UNAVAILABLE");
        assertThat(http.query(TENANT, "SELECT count(*) FROM platform.tenant_subscription WHERE plan_id = :plan",
                Map.of("plan", plan), Long.class)).as("nothing is recorded without a gateway order").isZero();
        Reply priced = checkout(admin, plan("silver", "100.00", UUID.randomUUID(), true));
        assertThat(priced.status()).as("the plan's currency needs master, which this test does not run").isEqualTo(503);
        assertThat(priced.body().path("code").asString()).isEqualTo("UNAVAILABLE");
        assertThat(checkout(admin, plan("free", "0.00", null, true)).status()).isEqualTo(422);
        assertThat(checkout(admin, plan("old", "10.00", null, false)).status()).isEqualTo(422);
        assertThat(checkout(admin, UUID.randomUUID()).status()).isEqualTo(422);
        Reply noKey = http.call("POST", "/v1/platform/tenant-subscriptions:checkout", admin,
                "{\"planId\":\"" + plan + "\"}");
        assertThat(noKey.status()).as("money operations need an Idempotency-Key").isEqualTo(400);
        assertThat(checkout(http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), CHECKOUT), plan)
                .status()).isEqualTo(403);
    }

    @Test
    void cancellingKeepsAnActiveSubscriptionUntilItsEndDate() {
        UUID subscription = pendingSubscription(TENANT, null, "order_Cancel", 1);
        http.sql(TENANT, "UPDATE platform.tenant_subscription SET status = 'active', end_date = DATE '2027-01-31' "
                + "WHERE id = :id", Map.of("id", subscription));
        String admin = http.admin(TENANT, UUID.randomUUID(), CANCEL);
        Reply done = http.call("POST", "/v1/platform/tenant-subscriptions/" + subscription + ":cancel", admin,
                "{\"reason\":\"Moving to another plan\"}");
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(done.body().path("status").asString()).isEqualTo("cancelled");
        assertThat(done.body().path("endDate").asString()).isEqualTo("2027-01-31");
        assertThat(http.query(TENANT, "SELECT count(*) FROM platform.outbox_event WHERE aggregate_id = :id AND "
                + "event_type = 'platform.tenant_subscription.cancel'", Map.of("id", subscription), Long.class))
                .isEqualTo(1L);
    }

    // ---------------------------------------------------------------------------------------------- audit trail

    @Test
    void theAuditTrailIsReadByTenantAdministratorsOnly() {
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();
        audit(TENANT, mine, "2026-09-10 10:00:00+00");
        audit(OTHER_TENANT, theirs, "2026-09-10 10:00:00+00");
        String range = PlatformCalls.encode("occurredAt >= \"2026-09-01T00:00:00Z\" AND occurredAt < "
                + "\"2026-10-01T00:00:00Z\"");
        String admin = http.admin(TENANT, UUID.randomUUID(), "platform.audit_log.read");
        Reply list = http.call("GET", "/v1/platform/audit-logs?filter=" + range, admin, null);
        assertThat(list.status()).as(list.text()).isEqualTo(200);
        assertThat(list.body().path("data").findValuesAsString("id")).contains(mine.toString())
                .doesNotContain(theirs.toString());
        JsonNode first = list.body().path("data").get(0);
        assertThat(first.has("etag")).as("an immutable record has no version").isFalse();
        assertThat(first.path("subjectType").asString()).isEqualTo("platform.tag");
        assertThat(first.path("description").asString()).isEqualTo("update platform.tag");
        Reply unbounded = http.call("GET", "/v1/platform/audit-logs", admin, null);
        assertThat(unbounded.status()).isEqualTo(400);
        assertThat(unbounded.body().path("code").asString()).isEqualTo("INVALID_FILTER");
        assertThat(http.call("GET", "/v1/platform/audit-logs?filter=" + PlatformCalls.encode(
                "occurredAt >= \"2025-01-01T00:00:00Z\" AND occurredAt < \"2026-10-01T00:00:00Z\""), admin, null)
                .status()).as("at most 366 days").isEqualTo(400);
        Reply one = http.call("GET", "/v1/platform/audit-logs/" + mine, admin, null);
        assertThat(one.status()).isEqualTo(200);
        assertThat(one.body().path("properties").path("changedFields").get(0).asString()).isEqualTo("name");
        assertThat(http.call("GET", "/v1/platform/audit-logs/" + theirs, admin, null).status()).isEqualTo(404);
        String school = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()), "platform.audit_log.read");
        assertThat(http.call("GET", "/v1/platform/audit-logs?filter=" + range, school, null).status())
                .as("the trail spans every institution").isEqualTo(403);
        assertThat(http.call("GET", "/v1/platform/audit-logs/" + mine, school, null).status()).isEqualTo(403);
        assertThat(http.call("GET", "/v1/platform/audit-logs?filter=" + range,
                http.admin(TENANT, UUID.randomUUID(), "platform.tag.read"), null).status()).isEqualTo(403);
        Reply page = http.call("GET", "/v1/platform/audit-logs?size=1&filter=" + range, admin, null);
        assertThat(page.body().path("data").size()).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------- tags, themes

    @Test
    void aTagInUseIsNotDeletedAndTranslationsAreNotSilentlyDropped() {
        String admin = http.admin(TENANT, UUID.randomUUID(), "platform.tag.create", "platform.tag.delete",
                "platform.tag.update", "platform.tag.read");
        Reply translated = http.call("POST", "/v1/platform/tags", admin,
                "{\"name\":\"Art\",\"slug\":\"art\",\"translations\":{\"hi\":{\"name\":\"कला\"}}}");
        assertThat(translated.status()).isEqualTo(422);
        assertThat(translated.text()).contains("translations");
        assertThat(http.call("POST", "/v1/platform/tags", admin, "{\"name\":\"Art\",\"slug\":\"Art Class\"}")
                .status()).isEqualTo(422);
        Reply tag = http.call("POST", "/v1/platform/tags", admin, "{\"name\":\"Art\",\"slug\":\"art\"}");
        assertThat(tag.status()).as(tag.text()).isEqualTo(201);
        assertThat(tag.body().path("tagType").asString()).isEqualTo("general");
        assertThat(http.call("POST", "/v1/platform/tags", admin, "{\"name\":\"Other\",\"slug\":\"art\","
                + "\"tagType\":\"post\"}").status()).as("slugs are unique per tag type").isEqualTo(201);
        String id = tag.body().path("id").asString();
        http.sql(TENANT, "INSERT INTO platform.tag_assignment (id, tenant_id, tag_id, entity_type, entity_id, "
                + "row_version) VALUES (:id, :tenant, :tag, 'lms.course', :entity, 1)", Map.of("id", UUID.randomUUID(),
                "tenant", TENANT, "tag", UUID.fromString(id), "entity", UUID.randomUUID()));
        Reply blocked = http.call("DELETE", "/v1/platform/tags/" + id, admin, null, "If-Match", "\"1\"");
        assertThat(blocked.status()).isEqualTo(409);
        assertThat(blocked.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
        Reply search = http.call("GET", "/v1/platform/search?query=art&entityTypes=platform.tag",
                http.admin(TENANT, UUID.randomUUID(), "platform.tenant.global_search", "platform.tag.read"), null);
        assertThat(search.status()).as(search.text()).isEqualTo(200);
        assertThat(search.body().path("data").findValuesAsString("entityId")).contains(id);
        assertThat(http.call("GET", "/v1/platform/search?query=art",
                http.admin(TENANT, UUID.randomUUID(), "platform.tenant.global_search"), null).body().path("data")
                .findValuesAsString("entityId")).as("tags need their read permission").doesNotContain(id);
        String searcher = http.admin(TENANT, UUID.randomUUID(), "platform.tenant.global_search");
        assertThat(http.call("GET", "/v1/platform/search?query=a", searcher, null).status()).isEqualTo(400);
        assertThat(http.call("GET", "/v1/platform/search?query=art&entityTypes=sis.student", searcher, null).status())
                .as("other modules' records are not the platform's to search").isEqualTo(400);
        assertThat(http.call("GET", "/v1/platform/search?query=art&page=999", searcher, null).status())
                .isEqualTo(400);
    }

    @Test
    void themeTokensAreTheOverridableOnesOnly() {
        String admin = http.admin(TENANT, UUID.randomUUID(), "platform.theme.create", "platform.theme.update");
        Reply intent = http.call("POST", "/v1/platform/themes", admin,
                "{\"name\":\"Loud\",\"tokens\":{\"intent.danger\":\"#FF0000\",\"color.primary\":\"red\","
                        + "\"font.family\":\"Comic Sans\"}}");
        assertThat(intent.status()).as(intent.text()).isEqualTo(422);
        assertThat(intent.text()).contains("tokens.intent.danger").contains("tokens.color.primary")
                .contains("tokens.font.family");
        assertThat(http.call("POST", "/v1/platform/themes", admin, "{\"name\":\"Odd\",\"tokens\":[1]}").status())
                .isEqualTo(422);
        Reply theme = http.call("POST", "/v1/platform/themes", admin, "{\"name\":\"Calm\",\"tokens\":"
                + "{\"color.primary\":\"#2563eb\",\"color.primary.dark\":\"#93C5FD\",\"radius.step\":\"1\"}}");
        assertThat(theme.status()).as(theme.text()).isEqualTo(201);
        String id = theme.body().path("id").asString();
        Reply cleared = http.call("PATCH", "/v1/platform/themes/" + id, admin, "{\"tokens\":null}",
                "If-Match", "\"1\"");
        assertThat(cleared.status()).as(cleared.text()).isEqualTo(200);
        assertThat(cleared.body().path("tokens").size()).isZero();
        Reply recoloured = http.call("PATCH", "/v1/platform/themes/" + id, admin,
                "{\"tokens\":{\"color.secondary\":\"#0D9488\"}}", "If-Match", "\"2\"");
        assertThat(recoloured.body().path("tokens").path("color.secondary").asString()).isEqualTo("#0D9488");
        assertThat(http.call("GET", "/v1/platform/themes/" + id, null, null).status())
                .as("a guest has no tenant: no theme").isEqualTo(404);
        Reply guestList = http.call("GET", "/v1/platform/themes", null, null);
        assertThat(guestList.status()).isEqualTo(200);
        assertThat(guestList.body().path("data").size()).isZero();
        Reply mine = http.call("GET", "/v1/platform/themes/" + id,
                http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString())), null);
        assertThat(mine.status()).as("every user of the tenant reads its themes").isEqualTo(200);
        assertThat(http.call("GET", "/v1/platform/themes/" + id, http.admin(OTHER_TENANT, UUID.randomUUID()), null)
                .status()).isEqualTo(404);
    }

    // ---------------------------------------------------------------------------------------------- branding, staff

    @Test
    void brandingIsPublicAndPresentationalOnly() {
        Reply branding = http.call("GET", "/v1/platform/branding?tenantCode=flow-a&institutionId=" + SCHOOL, null,
                null);
        assertThat(branding.status()).as(branding.text()).isEqualTo(200);
        assertThat(branding.body().propertyNames()).containsExactlyInAnyOrder("tenantName", "locale", "timezone");
        assertThat(http.call("GET", "/v1/platform/branding?tenantCode=nobody", null, null).status()).isEqualTo(404);
        http.sqlGlobal("INSERT INTO platform.tenant (id, code, name, status) VALUES (:id, 'flow-closed', 'Closed', "
                + "'closed') ON CONFLICT (id) DO NOTHING", Map.of("id", UUID.randomUUID()));
        assertThat(http.call("GET", "/v1/platform/branding?tenantCode=flow-closed", null, null).status())
                .isEqualTo(404);
    }

    @Test
    void platformStaffMarkEnquiriesContacted() {
        UUID enquiry = UUID.randomUUID();
        http.sqlGlobal("INSERT INTO platform.sales_enquiry (id, contact_name, email, phone, organisation_name, "
                + "designation, row_version) VALUES (:id, 'A. Rao', 'a.rao@example.org', '9000000000', "
                + "'Sunrise School', 'Principal', 1)", Map.of("id", enquiry));
        String path = "/v1/platform/sales-enquiries/" + enquiry + ":markContacted";
        assertThat(http.call("POST", path, http.admin(TENANT, UUID.randomUUID(),
                "platform.sales_enquiry.mark_enquiry_contacted"), "{}").status())
                .as("a tenant user's token can never call it").isEqualTo(403);
        String staff = http.staff(UUID.randomUUID(), "platform.sales_enquiry.mark_enquiry_contacted");
        Reply done = http.call("POST", path, staff, "{\"reason\":\" Called back; demo booked \"}");
        assertThat(done.status()).as(done.text()).isEqualTo(200);
        assertThat(done.body().path("status").asString()).isEqualTo("contacted");
        assertThat(done.etag()).isEqualTo("\"2\"");
        Reply again = http.call("POST", path, staff, "{}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(http.call("POST", "/v1/platform/sales-enquiries/" + UUID.randomUUID() + ":markContacted", staff,
                "{}").status()).isEqualTo(404);
    }

    @Test
    void clearingDemoDataIsAnnouncedToTheModulesThatHoldIt() {
        String staff = http.staff(UUID.randomUUID(), "platform.tenant.clear_demo_data");
        Reply done = http.call("POST", "/v1/platform/tenants/" + TENANT + ":clearDemoData", staff, "{}");
        assertThat(done.status()).as(done.text()).isEqualTo(202);
        assertThat(done.body().path("kind").asString()).isEqualTo("platform.ClearDemoData");
        assertThat(done.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(http.query(TENANT, "SELECT count(*) FROM platform.outbox_event WHERE aggregate_id = :tenant AND "
                + "event_type = 'platform.tenant.clearDemoData' AND payload LIKE :operation", Map.of("tenant", TENANT,
                "operation", "%" + done.body().path("operationId").asString() + "%"), Long.class)).isEqualTo(1L);
        assertThat(http.call("POST", "/v1/platform/tenants/" + UUID.randomUUID() + ":clearDemoData", staff, "{}")
                .status()).isEqualTo(404);
        assertThat(http.call("POST", "/v1/platform/tenants/" + TENANT + ":clearDemoData",
                http.admin(TENANT, UUID.randomUUID(), "platform.tenant.clear_demo_data"), "{}").status())
                .isEqualTo(403);
        UUID closed = UUID.randomUUID();
        http.sqlGlobal("INSERT INTO platform.tenant (id, code, name, status) VALUES (:id, 'flow-gone', 'Gone', "
                + "'closed') ON CONFLICT (id) DO NOTHING", Map.of("id", closed));
        assertThat(http.call("POST", "/v1/platform/tenants/" + closed + ":clearDemoData", staff, "{}").status())
                .isEqualTo(409);
    }

    // ---------------------------------------------------------------------------------------------- reports, photos

    @Test
    void reportsListWhatTheCallerMayRunAndNeedTheStore() {
        String school = http.token(TENANT, UUID.randomUUID(), List.of(SCHOOL.toString()),
                "platform.tenant.list_report_definitions", "platform.tenant.run_report", "platform.audit_log.read");
        Reply none = http.call("GET", "/v1/platform/reports", school, null);
        assertThat(none.status()).isEqualTo(200);
        assertThat(none.body().path("data").size()).as("reports span the tenant: tenant-wide grants only").isZero();
        assertThat(http.call("POST", "/v1/platform/reports:run", school, "{\"reportCode\":\"platform.audit_log\","
                + "\"format\":\"csv\",\"parameters\":{\"from\":\"2026-10-01\",\"to\":\"2026-10-02\"}}").status())
                .isEqualTo(403);
        String admin = http.admin(TENANT, UUID.randomUUID(), "platform.tenant.list_report_definitions",
                "platform.tenant.run_report", "platform.audit_log.read");
        Reply mine = http.call("GET", "/v1/platform/reports?module=platform", admin, null);
        assertThat(mine.body().path("data").findValuesAsString("reportCode")).containsExactly("platform.audit_log");
        assertThat(mine.body().path("data").get(0).path("parameters").path("required").size()).isEqualTo(2);
        assertThat(http.call("GET", "/v1/platform/reports?module=sis", admin, null).body().path("data").size())
                .isZero();
        assertThat(http.call("POST", "/v1/platform/reports:run", admin, "{\"reportCode\":\"sis.students\","
                + "\"format\":\"csv\"}").status()).isEqualTo(422);
        assertThat(http.call("POST", "/v1/platform/reports:run", admin, "{\"reportCode\":\"platform.audit_log\","
                + "\"format\":\"csv\",\"parameters\":{\"from\":\"yesterday\",\"to\":\"2026-10-02\"}}").status())
                .isEqualTo(422);
        Reply noStore = http.call("POST", "/v1/platform/reports:run", admin, "{\"reportCode\":\"platform.audit_log\","
                + "\"format\":\"csv\",\"parameters\":{\"from\":\"2026-10-01\",\"to\":\"2026-10-02\"}}");
        assertThat(noStore.status()).as(noStore.text()).isEqualTo(503);
        assertThat(noStore.body().path("code").asString()).isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void myStorageAndProvidersThatCannotAnswer() {
        UUID user = UUID.randomUUID();
        String token = http.token(TENANT, user, List.of(SCHOOL.toString()),
                "platform.storage_quota.get_my_storage_usage", "platform.tenant.search_stock_images",
                "platform.file_object.create_upload_url");
        http.sql(TENANT, "INSERT INTO platform.storage_quota (id, tenant_id, user_id, quota_bytes, used_bytes, "
                + "row_version) VALUES (:id, :tenant, :user, 5000, 1200, 1)",
                Map.of("id", UUID.randomUUID(), "tenant", TENANT, "user", user));
        Reply mine = http.call("GET", "/v1/platform/me/storage", token, null);
        assertThat(mine.status()).as(mine.text()).isEqualTo(200);
        assertThat(mine.body().path("quotaBytes").asString()).isEqualTo("5000");
        assertThat(mine.body().path("usedBytes").asString()).isEqualTo("1200");
        assertThat(http.call("GET", "/v1/platform/me/storage",
                http.token(OTHER_TENANT, user, List.of("*"), "platform.storage_quota.get_my_storage_usage"), null)
                .status()).as("the same user id in another tenant has no quota there").isEqualTo(404);
        Reply photos = http.call("GET", "/v1/platform/stock-images?query=science%20lab", token, null);
        assertThat(photos.status()).as(photos.text()).isEqualTo(503);
        assertThat(photos.body().path("code").asString()).isEqualTo("UNAVAILABLE");
        Reply upload = http.call("POST", "/v1/platform/files:createUploadUrl", token,
                "{\"fileName\":\"a.pdf\",\"mimeType\":\"application/pdf\",\"sizeBytes\":\"3\"}");
        assertThat(upload.status()).isEqualTo(503);
        assertThat(upload.body().path("code").asString()).isEqualTo("NOT_CONFIGURED");
    }

    // ---------------------------------------------------------------------------------------------- helpers

    private void audit(UUID tenant, UUID id, String at) {
        http.sql(tenant, "INSERT INTO platform.audit_log (id, tenant_id, log_name, description, event, subject_type, "
                + "subject_id, causer_type, causer_id, properties, occurred_at) VALUES (:id, :tenant, 'platform', "
                + "'update platform.tag', 'update', 'platform.tag', :subject, 'user', :causer, "
                + "CAST('{\"changedFields\":[\"name\"]}' AS jsonb), CAST(:at AS timestamptz))",
                Map.of("id", id, "tenant", tenant, "subject", UUID.randomUUID(), "causer", UUID.randomUUID(),
                        "at", at));
    }

    private UUID plan(String code, String price, UUID currency, boolean active) {
        UUID id = UUID.randomUUID();
        Map<String, Object> params = new HashMap<>(Map.of("id", id, "code", code + "-" + id,
                "price", new BigDecimal(price), "active", active));
        params.put("currency", currency);
        http.sqlGlobal("INSERT INTO platform.saas_plan (id, code, display_name, billing_cycle_months, price, "
                + "currency_id, is_active, row_version) VALUES (:id, :code, 'Plan', 12, :price, "
                + "CAST(:currency AS uuid), :active, 1)", params);
        return id;
    }

    private Reply checkout(String token, UUID plan) {
        return http.call("POST", "/v1/platform/tenant-subscriptions:checkout", token, "{\"planId\":\"" + plan + "\"}",
                "Idempotency-Key", UUID.randomUUID().toString());
    }

    /** A pending subscription as StartSubscriptionCheckout leaves it (its order opened at the gateway). */
    private UUID pendingSubscription(UUID tenant, UUID institution, String order, int months) {
        UUID id = UUID.randomUUID();
        Map<String, Object> params = new HashMap<>(Map.of("id", id, "tenant", tenant,
                "user", UUID.randomUUID(), "order", order, "months", months));
        params.put("institution", institution);
        http.sql(tenant, "INSERT INTO platform.tenant_subscription (id, tenant_id, institution_id, "
                + "requested_by_user_id, status, price, billing_cycle_months, gateway_name, gateway_txn_ref, "
                + "gateway_status, row_version) VALUES (:id, :tenant, CAST(:institution AS uuid), :user, 'pending', "
                + "1200.00, :months, 'razorpay', :order, 'created', 1)", params);
        return id;
    }

    private String state(UUID subscription) {
        return http.query(TENANT, "SELECT status || ':' || row_version FROM platform.tenant_subscription "
                + "WHERE id = :id", Map.of("id", subscription), String.class);
    }

    private Reply webhook(byte[] body, String signature) {
        return http.send("POST", "/v1/platform/tenant-subscriptions:webhook", null, body,
                "X-Razorpay-Signature", signature);
    }

    private static String confirm(String order, String payment, String signature) {
        return "{\"gatewayOrderId\":\"" + order + "\",\"gatewayPaymentId\":\"" + payment
                + "\",\"gatewaySignature\":\"" + signature + "\"}";
    }

    private static byte[] orderPaid(String order, UUID tenant, UUID subscription) {
        return orderPaid(order, tenant, subscription, "upi");
    }

    /** Razorpay's order.paid webhook body, with the notes the platform put on the order. */
    private static byte[] orderPaid(String order, UUID tenant, UUID subscription, String method) {
        return ("{\"entity\":\"event\",\"event\":\"order.paid\",\"payload\":{\"payment\":{\"entity\":{\"id\":"
                + "\"pay_W1\",\"order_id\":\"" + order + "\",\"amount\":120000,\"currency\":\"INR\",\"status\":"
                + "\"captured\",\"method\":\"" + method + "\",\"email\":\"parent@example.org\",\"contact\":"
                + "\"+919000000000\"}},\"order\":{\"entity\":{\"id\":\"" + order + "\",\"amount\":120000,"
                + "\"amount_paid\":120000,\"status\":\"paid\",\"receipt\":\"" + subscription + "\",\"notes\":{"
                + "\"tenantId\":\"" + tenant + "\",\"subscriptionId\":\"" + subscription + "\"}}}}}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String hmac(String secret, String data) {
        return hmac(secret, data.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmac(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
