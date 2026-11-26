package com.mysms.learning.lms.business;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Google's OAuth 2.0 authorization endpoint for calendar access: the link attempt is an authorization-code request
 * with offline access, so the callback can hold a refresh token. Unconfigured (blank client id) the adapter is never
 * chosen.
 */
@Component
public class GoogleCalendarProvider implements CalendarProvider {

    public static final String PROVIDER = "google";

    private final String authUrl;
    private final String clientId;
    private final String scope;

    public GoogleCalendarProvider(@Value("${mysms.lms.calendar.google.auth-url}") String authUrl,
            @Value("${mysms.lms.calendar.google.client-id}") String clientId,
            @Value("${mysms.lms.calendar.google.scope}") String scope) {
        this.authUrl = authUrl;
        this.clientId = clientId;
        this.scope = scope;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean configured() {
        return authUrl != null && !authUrl.isBlank() && clientId != null && !clientId.isBlank();
    }

    @Override
    public URI authorizeUrl(String redirectUri, String state) {
        return URI.create(authUrl + "?response_type=code&access_type=offline&prompt=consent"
                + "&client_id=" + encode(clientId) + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode(scope) + "&state=" + encode(state));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
