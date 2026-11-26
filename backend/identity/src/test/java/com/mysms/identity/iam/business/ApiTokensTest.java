package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiTokensTest {

    @Test
    void apiTokensAreToldApartFromRefreshTokensAndCarryTheirTenant() {
        UUID tenant = UUID.randomUUID();
        String token = ApiTokens.generate(tenant);
        assertThat(ApiTokens.recognises(token)).isTrue();
        assertThat(ApiTokens.tenantOf(token)).contains(tenant);
        assertThat(ApiTokens.recognises(RefreshTokens.generate(tenant))).isFalse();
        assertThat(ApiTokens.recognises(null)).isFalse();
        assertThat(ApiTokens.tenantOf("mysms_pat_garbage")).isEmpty();
        assertThat(ApiTokens.tenantOf(RefreshTokens.generate(tenant))).isEmpty();
    }
}
