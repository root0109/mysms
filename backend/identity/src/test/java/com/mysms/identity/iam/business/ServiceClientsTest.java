package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceClientsTest {

    private static String sha256(String secret) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void authenticatesOnlyTheConfiguredSecretOfTheClient() throws Exception {
        ServiceClients clients = new ServiceClients(Map.of("finance", new ServiceClients.Client(sha256("s-finance"),
                List.of("org.institution.read"))), Duration.ofMinutes(5));
        assertThat(clients.authenticate("finance", "s-finance")).map(ServiceClients.Client::permissions)
                .contains(List.of("org.institution.read"));
        assertThat(clients.authenticate("finance", "s-other")).isEmpty();
        assertThat(clients.authenticate("platform", "s-finance")).isEmpty();
        assertThat(clients.authenticate(null, "s-finance")).isEmpty();
        assertThat(clients.authenticate("finance", null)).isEmpty();
        assertThat(clients.tokenTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(new ServiceClients.Client(sha256("x"), null).permissions()).isEmpty();
    }

    @Test
    void anAreaWithAnUnderscoreIsAClientInEverySpellingItsConfigurationCanTake() throws Exception {
        for (String configured : List.of("time_attendance", "time-attendance", "timeattendance")) {
            ServiceClients clients = new ServiceClients(Map.of(configured, new ServiceClients.Client(sha256("s-ta"),
                    List.of("hr.staff.read"))), Duration.ofMinutes(5));
            assertThat(clients.authenticate("time_attendance", "s-ta")).as(configured)
                    .map(ServiceClients.Client::permissions).contains(List.of("hr.staff.read"));
            assertThat(clients.authenticate("time_attendance", "s-other")).as(configured).isEmpty();
            assertThat(clients.authenticate("time-attendance", "s-ta")).as("a client id is an area code").isEmpty();
        }
        String hash = sha256("s");
        assertThatThrownBy(() -> new ServiceClients(Map.of("time_attendance", new ServiceClients.Client(hash, List.of()),
                "timeattendance", new ServiceClients.Client(hash, List.of())), Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("configured twice");
    }

    @Test
    void refusesAMalformedConfiguration() throws Exception {
        String hash = sha256("s");
        assertThatThrownBy(() -> new ServiceClients(Map.of("Finance", new ServiceClients.Client(hash, List.of())),
                Duration.ofMinutes(5))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ServiceClients(Map.of("finance", new ServiceClients.Client("plain-secret",
                List.of())), Duration.ofMinutes(5))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ServiceClients(Map.of("_finance", new ServiceClients.Client(hash, List.of())),
                Duration.ofMinutes(5))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ServiceClients(Map.of(), Duration.ZERO)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ServiceClients(Map.of(), Duration.ofHours(2))).isInstanceOf(IllegalStateException.class);
    }
}
