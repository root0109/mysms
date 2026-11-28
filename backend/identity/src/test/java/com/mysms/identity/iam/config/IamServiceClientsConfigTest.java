package com.mysms.identity.iam.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.identity.iam.business.ServiceClients;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class IamServiceClientsConfigTest {

    private static String sha256(String secret) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
    }

    private static ServiceClients bind(PropertySource<?> source) {
        AbstractEnvironment environment = new AbstractEnvironment() { }; // only the given source, no real environment
        environment.getPropertySources().addFirst(source);
        return new IamServiceClientsConfig().serviceClients(environment, Duration.ofMinutes(5));
    }

    @Test
    void anUnderscoreAreaBindsFromAnEnvironmentVariableAndFromProperties() throws Exception {
        String hash = sha256("s-ta");
        // Spring maps environment variables only for a source of this name
        ServiceClients fromEnvironment = bind(new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
                "MYSMS_IAM_SERVICECLIENTS_TIMEATTENDANCE_SECRETSHA256", hash,
                "MYSMS_IAM_SERVICECLIENTS_TIMEATTENDANCE_PERMISSIONS", "hr.staff.read")));
        assertThat(fromEnvironment.authenticate("time_attendance", "s-ta")).map(ServiceClients.Client::permissions)
                .contains(List.of("hr.staff.read"));
        for (String key : List.of("time_attendance", "time-attendance")) {
            ServiceClients fromProperties = bind(new MapPropertySource("properties",
                    Map.of("mysms.iam.service-clients." + key + ".secret-sha256", hash)));
            assertThat(fromProperties.authenticate("time_attendance", "s-ta")).as(key).isPresent();
        }
    }
}
