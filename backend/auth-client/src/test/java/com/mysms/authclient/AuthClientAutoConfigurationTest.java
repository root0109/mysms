package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.error.ProblemResponseWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AuthClientAutoConfigurationTest {

    @Test
    void wiresTheEdge() {
        AuthClientAutoConfiguration config = new AuthClientAutoConfiguration();
        AuthClientProperties properties = new AuthClientProperties(TestTokens.ISSUER, null,
                new TestTokens().publicKeyPem(), null, List.of("master"), null);
        OperationTable table = config.operationTable(properties);
        assertThat(table.size()).isEqualTo(20);
        TokenVerifier verifier = config.tokenVerifier(properties);
        assertThat(config.authFilter(table, verifier, config.permissionChecker(),
                new ProblemResponseWriter(JsonMapper.builder().build()), properties)).isNotNull();
        assertThat(config.bearerPropagationInterceptor()).isNotNull();
    }
}
