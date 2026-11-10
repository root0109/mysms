package com.mysms.iam.config;

import com.mysms.core.sdk.SdkClientFactory;
import com.mysms.iam.business.PasswordHasher;
import com.mysms.iam.business.TokenIssuer;
import com.mysms.sdk.people.api.PersonApi;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The issuer side of iam: password hashing, the token signer (refuses to start without a valid signing key) and
 * the people client GetMe uses for the caller's person record.
 */
@Configuration(proxyBeanMethods = false)
public class IamSecurityConfig {

    /**
     * iam's own settings. Each is read from its property ({@code mysms.iam.*}) and otherwise from its environment
     * variable; the issuer is the one every module verifies ({@code mysms.auth.issuer}) unless iam is given its own.
     */
    @Bean
    public IamProperties iamProperties(
            @Value("${mysms.iam.token.issuer:${mysms.auth.issuer}}") String issuer,
            @Value("${mysms.iam.token.signing-key:${MYSMS_IAM_SIGNING_KEY:}}") String signingKey,
            @Value("${mysms.iam.token.key-id:${MYSMS_IAM_SIGNING_KEY_ID:}}") String keyId,
            @Value("${mysms.iam.token.access-token-ttl:${MYSMS_IAM_ACCESS_TOKEN_TTL:PT15M}}") Duration accessTokenTtl,
            @Value("${mysms.iam.token.refresh-token-ttl:${MYSMS_IAM_REFRESH_TOKEN_TTL:P30D}}") Duration refreshTokenTtl,
            // key rotation: the retiring key's public half stays in the JWKS until its tokens expire
            @Value("${mysms.iam.token.previous-public-key:${MYSMS_IAM_PREVIOUS_PUBLIC_KEY:}}") String previousPublicKey,
            // where iam.GetMe reads the caller's person record (people, or the monolith); empty = omitted
            @Value("${mysms.iam.people-base-url:${MYSMS_PEOPLE_URL:}}") String peopleBaseUrl,
            // first start of a new installation: the tenant and its administrator, created when all three are set
            @Value("${mysms.iam.bootstrap.tenant-code:${MYSMS_IAM_BOOTSTRAP_TENANT_CODE:}}") String tenantCode,
            @Value("${mysms.iam.bootstrap.admin-username:${MYSMS_IAM_BOOTSTRAP_ADMIN_USERNAME:}}") String adminUsername,
            @Value("${mysms.iam.bootstrap.admin-password:${MYSMS_IAM_BOOTSTRAP_ADMIN_PASSWORD:}}") String adminPassword) {
        return new IamProperties(
                new IamProperties.Token(issuer, signingKey, keyId, accessTokenTtl, refreshTokenTtl, previousPublicKey),
                peopleBaseUrl, new IamProperties.Bootstrap(tenantCode, adminUsername, adminPassword));
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }

    @Bean
    public TokenIssuer tokenIssuer(IamProperties properties, Clock clock) {
        return TokenIssuer.from(properties.token(), clock);
    }

    /** Present only when mysms.iam.people-base-url is set; GetMe then includes the person record. */
    @Bean
    public PeopleClient peopleClient(IamProperties properties, SdkClientFactory clients) {
        String baseUrl = properties.peopleBaseUrl();
        return new PeopleClient(baseUrl == null || baseUrl.isBlank()
                ? Optional.empty()
                : Optional.of(clients.create(PersonApi.class, baseUrl)));
    }

    /** Holder so the optional SDK client can be injected without a nullable bean. */
    public record PeopleClient(Optional<PersonApi> api) {
    }
}
