package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.error.ProblemResponseWriter;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AuthClientAutoConfigurationTest {

    @Test
    void wiresTheEdge() {
        AuthClientAutoConfiguration config = new AuthClientAutoConfiguration();
        AuthClientProperties properties = new AuthClientProperties(TestTokens.ISSUER, null,
                new TestTokens().publicKeyPem(), null, List.of("master"), null, false);
        OperationTable table = config.operationTable(properties);
        assertThat(table.size()).isEqualTo(20);
        TokenVerifier verifier = config.tokenVerifier(properties);
        assertThat(config.authFilter(table, config.edgeAuthorizer(verifier, config.permissionChecker(),
                new StaticListableBeanFactory().getBeanProvider(TenantCodeResolver.class), Clock.systemUTC()),
                new ProblemResponseWriter(JsonMapper.builder().build()), properties)).isNotNull();
        assertThat(config.grpcCallerInterceptor(java.time.Duration.ofSeconds(3))).isNotNull();
    }
}
