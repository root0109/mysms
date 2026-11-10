package com.mysms.iam.business;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.iam.config.IamProperties;
import java.time.Clock;
import org.junit.jupiter.api.Test;

/** The bootstrap refuses unusable settings before it touches anything (the database path is component-tested). */
class IamBootstrapTest {

    private static IamBootstrap bootstrap(String code, String password) {
        return new IamBootstrap(new IamProperties(null, null, new IamProperties.Bootstrap(code, "admin", password)),
                null, null, null, null, null, null, null, Clock.systemUTC());
    }

    @Test
    void refusesABadTenantCode() {
        assertThatThrownBy(() -> bootstrap("Not A Code!", "long enough secret").bootstrap())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("tenant-code");
        assertThatThrownBy(() -> bootstrap("x", "long enough secret").bootstrap())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("tenant-code");
    }

    @Test
    void refusesAShortPassword() {
        assertThatThrownBy(() -> bootstrap("acme", "short").bootstrap())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("at least 12");
    }
}
