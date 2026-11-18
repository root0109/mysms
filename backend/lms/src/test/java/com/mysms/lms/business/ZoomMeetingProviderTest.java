package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.UnavailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The Zoom adapter's request and answer mapping against a JDK HttpServer standing in for Zoom. */
class ZoomMeetingProviderTest {

    private static HttpServer zoom;
    private static final Map<String, String> ANSWERS = new ConcurrentHashMap<>();
    private static final List<String> CALLS = new ArrayList<>();
    private static final MeetingProvider.Account ACCOUNT = new MeetingProvider.Account("acc-1", "client", "secret");

    private ZoomMeetingProvider provider;

    @BeforeAll
    static void start() throws IOException {
        zoom = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        zoom.createContext("/", ZoomMeetingProviderTest::answer);
        zoom.start();
    }

    @AfterAll
    static void stop() {
        zoom.stop(0);
    }

    @BeforeEach
    void fresh() {
        ANSWERS.clear();
        CALLS.clear();
        ANSWERS.put("POST /oauth", "{\"access_token\":\"token-1\"}");
        String base = "http://127.0.0.1:" + zoom.getAddress().getPort();
        provider = new ZoomMeetingProvider(base + "/v2/", base + "/oauth", Duration.ofSeconds(5),
                Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC));
    }

    private static void answer(HttpExchange exchange) throws IOException {
        String key = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
        synchronized (CALLS) {
            CALLS.add(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        }
        String body = ANSWERS.get(key);
        byte[] bytes = (body == null ? "{}" : body).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(body == null ? 404 : 200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String call(String startsWith) {
        synchronized (CALLS) {
            return CALLS.stream().filter(made -> made.startsWith(startsWith)).findFirst().orElseThrow();
        }
    }

    @Test
    void unconfiguredWithoutAddresses() {
        assertThat(new ZoomMeetingProvider(" ", "", Duration.ofSeconds(1), Clock.systemUTC()).configured()).isFalse();
        assertThat(provider.configured()).isTrue();
        assertThat(provider.provider()).isEqualTo("zoom");
    }

    @Test
    void createSendsZoomsMeetingShapeAndReadsTheAnswer() {
        ANSWERS.put("POST /v2/users/me/meetings", "{\"id\":91001,\"uuid\":\"u==1\",\"join_url\":\"https://z/j\","
                + "\"start_url\":\"https://z/s\",\"password\":\"pw\"}");
        MeetingProvider.Meeting meeting = provider.create(ACCOUNT, new MeetingProvider.MeetingRequest("Algebra",
                "Agenda", OffsetDateTime.parse("2026-11-20T14:30:00+05:30"), "Asia/Kolkata", 45, null));
        assertThat(meeting).isEqualTo(new MeetingProvider.Meeting("91001", "u==1", "https://z/j", "https://z/s",
                "pw"));
        String token = call("POST /oauth");
        assertThat(token).contains("grant_type=account_credentials").contains("account_id=acc-1");
        String create = call("POST /v2/users/me/meetings");
        assertThat(create).contains("\"topic\":\"Algebra\"").contains("\"type\":2")
                .contains("\"start_time\":\"2026-11-20T09:00:00Z\"").contains("\"timezone\":\"Asia/Kolkata\"")
                .contains("\"duration\":45").contains("\"auto_recording\":\"none\"")
                .contains("\"join_before_host\":false");
    }

    @Test
    void startLinkCarriesZoomsTwoHourLifetime() {
        ANSWERS.put("GET /v2/meetings/91001", "{\"start_url\":\"https://z/s2\"}");
        MeetingProvider.StartLink link = provider.startLink(ACCOUNT, "91001");
        assertThat(link.url()).isEqualTo("https://z/s2");
        assertThat(link.expiresAt()).isEqualTo(Instant.parse("2026-11-12T12:00:00Z"));
    }

    @Test
    void endPutsTheEndAction() {
        ANSWERS.put("PUT /v2/meetings/91001/status", "{}");
        provider.end(ACCOUNT, "91001");
        assertThat(call("PUT /v2/meetings/91001/status")).contains("\"action\":\"end\"");
    }

    @Test
    void attendancePagesThroughTheReportAndMapsSeconds() {
        ANSWERS.put("GET /v2/past_meetings/u1/participants",
                "{\"participants\":[{\"user_email\":\"a@x.in\",\"join_time\":\"2026-11-12T09:00:00Z\","
                        + "\"leave_time\":\"2026-11-12T09:30:00Z\",\"duration\":1800}],\"next_page_token\":\"\"}");
        List<MeetingProvider.Attendance> attended = provider.attendance(ACCOUNT, "u1");
        assertThat(attended).containsExactly(new MeetingProvider.Attendance("a@x.in",
                OffsetDateTime.parse("2026-11-12T09:00:00Z"), OffsetDateTime.parse("2026-11-12T09:30:00Z"), 30));
        assertThat(call("GET /v2/past_meetings/u1/participants")).contains("page_size=300");
    }

    @Test
    void doubleEncodesAMeetingUuidWithSlashes() {
        assertThat(ZoomMeetingProvider.segment("a//b")).isEqualTo("a%252F%252Fb");
        assertThat(ZoomMeetingProvider.segment("plain")).isEqualTo("plain");
    }

    @Test
    void anyRefusalIsUnavailable() {
        assertThatThrownBy(() -> provider.startLink(ACCOUNT, "absent")).isInstanceOf(UnavailableException.class);
        ANSWERS.put("POST /oauth", "{\"error\":\"denied\"}");
        assertThatThrownBy(() -> provider.end(ACCOUNT, "91001")).isInstanceOf(UnavailableException.class);
    }
}
