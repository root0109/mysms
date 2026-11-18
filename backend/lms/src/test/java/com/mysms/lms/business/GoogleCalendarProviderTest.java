package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;

/** The Google calendar adapter's authorization URL and the configuration switch. */
class GoogleCalendarProviderTest {

    private static final String AUTH = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";

    @Test
    void unconfiguredWithoutAClientId() {
        assertThat(new GoogleCalendarProvider(AUTH, "", SCOPE).configured()).isFalse();
        assertThat(new GoogleCalendarProvider("", "id", SCOPE).configured()).isFalse();
        GoogleCalendarProvider configured = new GoogleCalendarProvider(AUTH, "id-1", SCOPE);
        assertThat(configured.configured()).isTrue();
        assertThat(configured.provider()).isEqualTo("google");
    }

    @Test
    void choosingAnUnconfiguredProviderIs503() {
        CalendarProviders none = new CalendarProviders(java.util.List.of(new GoogleCalendarProvider(AUTH, "", SCOPE)));
        assertThatThrownBy(() -> none.of("google")).isInstanceOf(CalendarNotConfiguredException.class)
                .hasMessageContaining("google");
    }

    @Test
    void authorizeUrlCarriesOfflineCodeRequestWithEscapedParameters() {
        URI url = new GoogleCalendarProvider(AUTH, "id 1", SCOPE)
                .authorizeUrl("https://app.example/cb?x=1", "state-9");
        assertThat(url.toString()).startsWith(AUTH + "?response_type=code&access_type=offline&prompt=consent")
                .contains("&client_id=id+1").contains("&redirect_uri=https%3A%2F%2Fapp.example%2Fcb%3Fx%3D1")
                .contains("&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fcalendar.events")
                .contains("&state=state-9");
    }
}
