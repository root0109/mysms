package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokensTest {

    @Test
    void tokensCarryTheirTenantAndAreUnique() {
        UUID tenant = UUID.randomUUID();
        String first = RefreshTokens.generate(tenant);
        String second = RefreshTokens.generate(tenant);
        assertThat(first).isNotEqualTo(second).startsWith(tenant + ".");
        assertThat(RefreshTokens.tenantOf(first)).contains(tenant);
    }

    @Test
    void malformedTokensHaveNoTenant() {
        String valid = RefreshTokens.generate(UUID.randomUUID());
        assertThat(RefreshTokens.tenantOf(null)).isEmpty();
        assertThat(RefreshTokens.tenantOf("")).isEmpty();
        assertThat(RefreshTokens.tenantOf(valid + "x")).isEmpty();
        assertThat(RefreshTokens.tenantOf(valid.replace('.', '-'))).isEmpty();
        assertThat(RefreshTokens.tenantOf("zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz" + valid.substring(36))).isEmpty();
        assertThat(RefreshTokens.tenantOf(valid.substring(0, 37) + "!" + valid.substring(38))).isEmpty();
        assertThat(RefreshTokens.tenantOf(valid.toUpperCase(java.util.Locale.ROOT))).isEmpty();
    }

    @Test
    void onlyAStableSha256IsStored() {
        String token = RefreshTokens.generate(UUID.randomUUID());
        assertThat(RefreshTokens.hash(token)).hasSize(64).matches("[0-9a-f]{64}").isEqualTo(RefreshTokens.hash(token))
                .doesNotContain(token.substring(37));
    }
}
