package com.mysms.lms.business;

import com.mysms.core.error.UnavailableException;
import com.mysms.core.json.JsonMappers;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Zoom's REST API with a Server-to-Server OAuth app per tenant: an account-credentials token per call, then meetings
 * created, read, ended and reported on for the account's own user ("me"). Unconfigured (blank API address) the
 * adapter is never chosen.
 */
@Component
public class ZoomMeetingProvider implements MeetingProvider {

    static final String PROVIDER = "zoom";
    /** Zoom documents a start URL as valid for two hours. */
    static final Duration START_LINK_LIFETIME = Duration.ofHours(2);
    private static final String UNREACHABLE = "The zoom meeting provider could not answer.";
    private static final DateTimeFormatter ZOOM_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final int REPORT_PAGE = 300;

    private final URI api;
    private final URI oauth;
    private final Duration timeout;
    private final Clock clock;
    private final HttpClient http;
    private final JsonMapper json = JsonMappers.create();

    public ZoomMeetingProvider(@Value("${mysms.lms.conferencing.zoom.api-url}") String api,
            @Value("${mysms.lms.conferencing.zoom.oauth-url}") String oauth,
            @Value("${mysms.lms.conferencing.timeout}") Duration timeout, Clock clock) {
        this.api = api == null || api.isBlank() ? null : URI.create(api.endsWith("/") ? api : api + "/");
        this.oauth = oauth == null || oauth.isBlank() ? null : URI.create(oauth);
        this.timeout = timeout;
        this.clock = clock;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean configured() {
        return api != null && oauth != null;
    }

    @Override
    public Meeting create(Account account, MeetingRequest request) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("auto_recording", request.autoRecording() == null ? "none" : request.autoRecording());
        settings.put("join_before_host", false);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topic", request.topic());
        body.put("type", 2);
        body.put("start_time", ZOOM_TIME.format(request.startsAt().withOffsetSameInstant(ZoneOffset.UTC)));
        body.put("timezone", request.timezone());
        body.put("duration", request.durationMinutes());
        if (request.agenda() != null) {
            body.put("agenda", request.agenda());
        }
        body.put("settings", settings);
        JsonNode meeting = send(account, "POST", "users/me/meetings", body);
        return new Meeting(meeting.path("id").asString(), meeting.path("uuid").asString(),
                meeting.path("join_url").asString(), meeting.path("start_url").asString(),
                meeting.path("password").asString(null));
    }

    @Override
    public StartLink startLink(Account account, String meetingId) {
        JsonNode meeting = send(account, "GET", "meetings/" + segment(meetingId), null);
        return new StartLink(meeting.path("start_url").asString(), clock.instant().plus(START_LINK_LIFETIME));
    }

    @Override
    public void end(Account account, String meetingId) {
        send(account, "PUT", "meetings/" + segment(meetingId) + "/status", Map.of("action", "end"));
    }

    @Override
    public List<Attendance> attendance(Account account, String meetingUuid) {
        List<Attendance> attended = new ArrayList<>();
        String next = "";
        do {
            String path = "past_meetings/" + segment(meetingUuid) + "/participants?page_size=" + REPORT_PAGE
                    + (next.isEmpty() ? "" : "&next_page_token=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
            JsonNode page = send(account, "GET", path, null);
            for (JsonNode participant : page.path("participants")) {
                OffsetDateTime joined = OffsetDateTime.parse(participant.path("join_time").asString());
                OffsetDateTime left = participant.hasNonNull("leave_time")
                        ? OffsetDateTime.parse(participant.path("leave_time").asString()) : null;
                attended.add(new Attendance(participant.path("user_email").asString(null), joined, left,
                        participant.path("duration").asInt(0) / 60));
            }
            next = page.path("next_page_token").asString("");
        } while (!next.isEmpty());
        return attended;
    }

    private JsonNode send(Account account, String method, String path, Object body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(api.resolve(path)).timeout(timeout)
                .header("Authorization", "Bearer " + token(account)).header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        }
        return answer(request.build());
    }

    private String token(Account account) {
        String basic = Base64.getEncoder().encodeToString((account.clientId() + ":" + account.clientSecret())
                .getBytes(StandardCharsets.UTF_8));
        URI uri = URI.create(oauth + "?grant_type=account_credentials&account_id="
                + URLEncoder.encode(account.externalAccountId(), StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).header("Authorization", "Basic " + basic)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        String token = answer(request).path("access_token").asString("");
        if (token.isEmpty()) {
            throw new UnavailableException(UNREACHABLE);
        }
        return token;
    }

    /** Any refusal, fault or unreadable answer is 503: the caller changed nothing and may retry. */
    private JsonNode answer(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException unreachable) {
            throw new UnavailableException(UNREACHABLE);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new UnavailableException(UNREACHABLE);
        }
        if (response.statusCode() >= 300) {
            throw new UnavailableException(UNREACHABLE);
        }
        try {
            return json.readTree(response.body() == null || response.body().isBlank() ? "{}" : response.body());
        } catch (JacksonException notJson) {
            throw new UnavailableException(UNREACHABLE);
        }
    }

    /** Meeting ids and UUIDs go into the path; a UUID with '/' must be encoded twice (Zoom's rule). */
    static String segment(String id) {
        String once = URLEncoder.encode(id, StandardCharsets.UTF_8);
        return id.startsWith("/") || id.contains("//") ? URLEncoder.encode(once, StandardCharsets.UTF_8) : once;
    }
}
