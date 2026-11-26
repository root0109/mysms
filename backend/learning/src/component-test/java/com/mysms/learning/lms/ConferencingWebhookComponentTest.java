package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static com.mysms.learning.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
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

/**
 * Zoom callbacks over HTTP against real PostgreSQL: the live class behind the meeting takes the effect — ended,
 * participant joined and left, recording ready — only when the signature verifies with the meeting's
 * conferencing-account secret, and only inside the tenant the event names.
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConferencingWebhookComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String PATH = "/v1/lms/integrations/zoom:webhook";
    private static final String SECRET = "zoom-client-secret";
    private static final String MEETING = "912345678";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private FieldCipher cipher;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID liveClass;
    private UUID host;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.secrets.keys", () -> LmsFixtures.SECRETS);
        registry.add("mysms.secrets.active-key", () -> "test");
    }

    @BeforeEach
    void meeting() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        host = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.conferencing_account (id, tenant_id, provider, account_name, owner_user_id, "
                        + "client_secret_enc) VALUES (:id, :tenantId, 'zoom', 'School Zoom', :owner, :enc)",
                "id", account, "owner", host,
                "enc", cipher.encrypt(SECRET, tenant, "lms.conferencing_account.client_secret_enc"));
        liveClass = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.live_class (id, tenant_id, title, class_date, start_time, duration_minutes, "
                        + "timezone, host_user_id, conferencing_provider, conferencing_account_id, meeting_id) "
                        + "VALUES (:id, :tenantId, 'Algebra live', :date, :time, 60, 'UTC', :host, 'zoom', :account, "
                        + ":meeting)",
                "id", liveClass, "date", LocalDate.parse("2026-11-12"), "time", LocalTime.parse("09:00"),
                "host", host, "account", account, "meeting", MEETING);
    }

    private LmsFixtures.Reply post(String body, String secret) throws Exception {
        long now = System.currentTimeMillis() / 1000;
        return fx.call("POST", PATH, null, body, "X-Signature", sign(secret, now, body),
                "X-Timestamp", Long.toString(now));
    }

    private static String sign(String secret, long timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    private String event(String type, String participant) {
        return """
                {"event":"%s","payload":{"object":{"id":"%s",
                 "end_time":"2026-11-12T10:30:00Z","share_url":"https://zoom.us/rec/share/abc",
                 "tracking_fields":[{"field":"mysms_tenant","value":"%s"}]%s}}}
                """.formatted(type, MEETING, tenant, participant == null ? "" : ",\"participant\":" + participant);
    }

    @Test
    void theMeetingLifecycleLandsOnTheLiveClassAndItsParticipants() throws Exception {
        UUID student = UUID.randomUUID();
        expect(post(event("meeting.participant_joined", "{\"customer_key\":\"" + student
                + "\",\"join_time\":\"2026-11-12T09:00:05Z\"}"), SECRET), 204);
        assertThat(fx.value(tenant, "SELECT attended FROM lms.live_class_participant WHERE live_class_id = :id "
                + "AND user_id = :user", Boolean.class, "id", liveClass, "user", student)).isTrue();
        expect(post(event("meeting.participant_joined", "{\"customer_key\":\"" + host
                + "\",\"join_time\":\"2026-11-12T09:00:00Z\"}"), SECRET), 204);
        assertThat(fx.value(tenant, "SELECT host_joined FROM lms.live_class WHERE id = :id", Boolean.class,
                "id", liveClass)).isTrue();
        expect(post(event("meeting.participant_left", "{\"customer_key\":\"" + student
                + "\",\"leave_time\":\"2026-11-12T09:45:05Z\"}"), SECRET), 204);
        // joined 09:00:05, left 09:45:05: 45 minutes, worked by hand
        assertThat(fx.value(tenant, "SELECT duration_minutes FROM lms.live_class_participant WHERE user_id = :user",
                Integer.class, "user", student)).isEqualTo(45);
        expect(post(event("meeting.ended", null), SECRET), 204);
        assertThat(fx.value(tenant, "SELECT ended_at FROM lms.live_class WHERE id = :id", OffsetDateTime.class,
                "id", liveClass)).isEqualTo(OffsetDateTime.parse("2026-11-12T10:30:00Z"));
        expect(post(event("recording.completed", null), SECRET), 204);
        assertThat(fx.value(tenant, "SELECT recording_url FROM lms.live_class WHERE id = :id", String.class,
                "id", liveClass)).isEqualTo("https://zoom.us/rec/share/abc");
    }

    @Test
    void aRepeatedEndedEventKeepsTheFirstEndTime() throws Exception {
        expect(post(event("meeting.ended", null), SECRET), 204);
        expect(post("""
                {"event":"meeting.ended","payload":{"object":{"id":"%s","end_time":"2026-11-12T11:00:00Z",
                 "tracking_fields":[{"field":"mysms_tenant","value":"%s"}]}}}
                """.formatted(MEETING, tenant), SECRET), 204);
        assertThat(fx.value(tenant, "SELECT ended_at FROM lms.live_class WHERE id = :id", OffsetDateTime.class,
                "id", liveClass)).isEqualTo(OffsetDateTime.parse("2026-11-12T10:30:00Z"));
    }

    @Test
    void theWrongSignatureProviderMeetingOrTenantIsRefused() throws Exception {
        expect(post(event("meeting.ended", null), "wrong-secret"), 401);
        long now = System.currentTimeMillis() / 1000;
        String body = event("meeting.ended", null);
        expect(fx.call("POST", PATH, null, body, "X-Signature", sign(SECRET, now - 600, body),
                "X-Timestamp", Long.toString(now - 600)), 401);
        expect(post("not json at all", SECRET), 401);
        expect(post(event("meeting.created", null), SECRET), 204); // an event lms does not act on
        // an unknown meeting is answered like a bad signature: the answer never tells which meetings exist
        LmsFixtures.Reply unknown = expect(post("""
                {"event":"meeting.ended","payload":{"object":{"id":"other-meeting",
                 "tracking_fields":[{"field":"mysms_tenant","value":"%s"}]}}}
                """.formatted(tenant), "any-secret"), 401);
        LmsFixtures.Reply unverified = expect(post(event("meeting.ended", null), "any-secret"), 401);
        assertThat(unknown.body().path("code")).isEqualTo(unverified.body().path("code"));
        assertThat(unknown.body().path("detail")).isEqualTo(unverified.body().path("detail"));
        expect(fx.call("POST", "/v1/lms/integrations/google_meet:webhook", null, event("meeting.ended", null),
                "X-Signature", "x", "X-Timestamp", "1"), 404);
        // the same meeting id claimed under another tenant finds nothing there
        expect(post("""
                {"event":"meeting.ended","payload":{"object":{"id":"%s",
                 "tracking_fields":[{"field":"mysms_tenant","value":"%s"}]}}}
                """.formatted(MEETING, UUID.randomUUID()), SECRET), 401);
        assertThat(fx.value(tenant, "SELECT ended_at FROM lms.live_class WHERE id = :id", OffsetDateTime.class,
                "id", liveClass)).isNull();
    }
}
