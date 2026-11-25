package com.mysms.demoseed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class DemoSettingsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Test
    void readsPlainSettingsFromPropertiesAndCredentialsFromTheEnvironmentOnly() {
        Properties properties = new Properties();
        properties.setProperty("mysms.demo.baseUrl", "https://sms.example.org");
        properties.setProperty("mysms.demo.action", "remove");
        properties.setProperty("MYSMS_DEMO_ADMIN_PASSWORD", "from-a-property");

        DemoSettings settings = DemoSettings.from(properties, Map.of("MYSMS_DEMO_ADMIN_USERNAME", "admin",
                "MYSMS_DEMO_ADMIN_PASSWORD", "admin-secret-value", "MYSMS_DEMO_PLATFORM_TOKEN", "token"), TODAY);

        assertThat(settings.baseUrl()).isEqualTo(URI.create("https://sms.example.org"));
        assertThat(settings.action()).isEqualTo(DemoSettings.Action.REMOVE);
        assertThat(settings.tenantCode()).isEqualTo("demo-20261118");
        assertThat(settings.adminPassword()).isEqualTo("admin-secret-value");
        assertThat(settings.personaPassword()).isEmpty();
        assertThat(settings.studentsPerClass()).isEqualTo(15);
        assertThat(settings.manifestDirectory()).isEqualTo(Path.of("demo-manifests"));
        assertThat(settings.toString()).doesNotContain("admin-secret-value").doesNotContain("token,");
    }

    @Test
    void refusesAnythingButADemoTenantAndAnEmptySchool() {
        assertThatThrownBy(() -> settings("school-1", 15)).hasMessageContaining("demo-");
        assertThatThrownBy(() -> settings("demo-", 15)).hasMessageContaining("demo-");
        assertThatThrownBy(() -> settings("demo-x", 0)).hasMessageContaining("studentsPerClass");
        Properties missing = new Properties();
        assertThatThrownBy(() -> DemoSettings.from(missing, Map.of(), TODAY)).hasMessageContaining("baseUrl");
    }

    private static DemoSettings settings(String code, int students) {
        return new DemoSettings(URI.create("http://localhost"), DemoSettings.Action.SEED, code, "", "", "", "",
                students, 0, 1L, Path.of("."), TODAY);
    }
}
