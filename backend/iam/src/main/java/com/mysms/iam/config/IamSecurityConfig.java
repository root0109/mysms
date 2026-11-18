package com.mysms.iam.config;

import com.mysms.iam.business.PasswordHasher;
import com.mysms.iam.business.TokenIssuer;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The issuer side of iam: password hashing and the token signer (refuses to start without a valid signing key).
 */
@Configuration(proxyBeanMethods = false)
public class IamSecurityConfig {

    /**
     * iam's own settings. Each is read from its property ({@code mysms.iam.*}) and otherwise from its environment
     * variable; the issuer is the one every module verifies ({@code mysms.auth.issuer}) unless iam is given its own.
     */
    @Bean
    public IamProperties iamProperties(
            @Value("${mysms.iam.token.issuer}") String issuer,
            @Value("${mysms.iam.token.signing-key}") String signingKey,
            @Value("${mysms.iam.token.key-id}") String keyId,
            @Value("${mysms.iam.token.access-token-ttl}") Duration accessTokenTtl,
            @Value("${mysms.iam.token.refresh-token-ttl}") Duration refreshTokenTtl,
            // key rotation: the retiring key's public half stays in the JWKS until its tokens expire
            @Value("${mysms.iam.token.previous-public-key}") String previousPublicKey,
            // first start of a new installation: the tenant and its administrator, created when all three are set
            @Value("${mysms.iam.bootstrap.tenant-code}") String tenantCode,
            @Value("${mysms.iam.bootstrap.admin-username}") String adminUsername,
            @Value("${mysms.iam.bootstrap.admin-password}") String adminPassword) {
        return new IamProperties(
                new IamProperties.Token(issuer, signingKey, keyId, accessTokenTtl, refreshTokenTtl, previousPublicKey),
                new IamProperties.Bootstrap(tenantCode, adminUsername, adminPassword));
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }

    @Bean
    public TokenIssuer tokenIssuer(IamProperties properties, Clock clock) {
        return TokenIssuer.from(properties.token(), clock);
    }
}
