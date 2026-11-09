package com.mysms.authclient;

import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.core.error.ProblemResponseWriter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Wires the edge filter and the auth facade into every module (JAVA-CODING-RULES §16). */
@AutoConfiguration
@EnableConfigurationProperties(AuthClientProperties.class)
public class AuthClientAutoConfiguration {

    @Bean
    public OperationTable operationTable(AuthClientProperties properties) {
        return OperationTable.load(properties.modules());
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenVerifier tokenVerifier(AuthClientProperties properties) {
        return JwtTokenVerifier.from(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionChecker permissionChecker() {
        return new ClaimsPermissionChecker();
    }

    @Bean
    public AuthFilter authFilter(OperationTable operations, TokenVerifier verifier, PermissionChecker permissions,
                                 ProblemResponseWriter problems, AuthClientProperties properties) {
        return new AuthFilter(operations, verifier, permissions, problems, properties.infrastructurePaths());
    }

    @Bean
    public BearerPropagationInterceptor bearerPropagationInterceptor() {
        return new BearerPropagationInterceptor();
    }
}
