package com.mysms.iam.config;

import com.mysms.core.sdk.SdkClientFactory;
import com.mysms.iam.business.PasswordHasher;
import com.mysms.iam.business.TokenIssuer;
import com.mysms.sdk.people.api.PersonApi;
import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The issuer side of iam: password hashing, the token signer (refuses to start without a valid signing key) and
 * the people client GetMe uses for the caller's person record.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IamProperties.class)
public class IamSecurityConfig {

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
