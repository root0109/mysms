package com.mysms.privacyclient;

import com.mysms.authclient.AuthClientAutoConfiguration;
import com.mysms.authclient.ServiceTokenProvider;
import java.time.Duration;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Wires the consent checker into a module that depends on privacy-client: privacy's decision service over gRPC
 * ({@code grpc.client.privacy-service.*}; in the monolith privacy's in-process server), within
 * {@code mysms.privacy-client.deadline}, behind the per-process cache of {@code mysms.privacy-client.cache-ttl}. Both
 * keys are the consuming module's configuration (its application.properties), like every other key. The legal holds
 * a module's {@link PrivacyTaskHandler} asks before it erases come from privacy's legal-hold service on the same
 * channel, within {@code mysms.privacy-client.holds-deadline}.
 */
@AutoConfiguration(after = AuthClientAutoConfiguration.class)
public class PrivacyClientAutoConfiguration {

    /** The gRPC client name of privacy ({@code grpc.client.privacy-service.*}), as every caller of privacy names it. */
    static final String PRIVACY_SERVICE = "privacy-service";

    @Bean
    @ConditionalOnMissingBean
    public ConsentDecisionSource consentDecisionSource(GrpcChannelFactory channels, ServiceTokenProvider serviceTokens,
            @Value("${mysms.privacy-client.deadline}") Duration deadline) {
        return new PrivacyDecisionGrpcClient(channels.createChannel(PRIVACY_SERVICE), serviceTokens, deadline);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsentChecker consentChecker(ConsentDecisionSource source,
            @Value("${mysms.privacy-client.cache-ttl}") Duration ttl) {
        return new CachingConsentChecker(source, ttl);
    }

    @Bean
    @ConditionalOnMissingBean
    public LegalHolds legalHolds(GrpcChannelFactory channels, ServiceTokenProvider serviceTokens,
            @Value("${mysms.privacy-client.holds-deadline}") Duration deadline) {
        return new LegalHoldGrpcClient(channels.createChannel(PRIVACY_SERVICE), serviceTokens, deadline);
    }
}
