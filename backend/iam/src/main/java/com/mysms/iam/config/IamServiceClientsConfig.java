package com.mysms.iam.config;

import com.mysms.iam.business.ServiceClients;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The service clients of the deployment ({@code mysms.iam.service-clients.<module>.secret-sha256} and
 * {@code .permissions}; as environment variables {@code MYSMS_IAM_SERVICECLIENTS_<MODULE>_SECRETSHA256} and
 * {@code _PERMISSIONS}, by Spring's relaxed binding) and the service-token lifetime
 * ({@code mysms.iam.token.service-token-ttl}, else {@code MYSMS_IAM_SERVICE_TOKEN_TTL}, default 5 minutes). None
 * configured = no client can obtain a service token.
 */
@Configuration(proxyBeanMethods = false)
public class IamServiceClientsConfig {

    @Bean
    public ServiceClients serviceClients(Environment environment,
            @Value("${mysms.iam.token.service-token-ttl}") Duration ttl) {
        Map<String, ServiceClients.Client> clients = new HashMap<>();
        Binder.get(environment)
                .bind("mysms.iam.service-clients", Bindable.mapOf(String.class, ServiceClients.Client.class))
                .ifBound(clients::putAll);
        return new ServiceClients(clients, ttl);
    }
}
