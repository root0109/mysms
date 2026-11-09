package com.mysms.core.sdk;

import java.util.List;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Creates the generated SDK clients (HTTP service interfaces in the `sdk` module) a module uses to call another
 * module — over HTTP even when both run in the monolith, so there is one code path (JAVA-CODING-RULES §2). Every
 * client propagates the trace and, through interceptors contributed by auth-client, the caller's credentials.
 */
public final class SdkClientFactory {

    private final RestClient.Builder builder;
    private final List<ClientHttpRequestInterceptor> interceptors;

    public SdkClientFactory(RestClient.Builder builder, List<ClientHttpRequestInterceptor> interceptors) {
        this.builder = builder;
        this.interceptors = List.copyOf(interceptors);
    }

    public <T> T create(Class<T> api, String baseUrl) {
        RestClient client = builder.clone()
                .baseUrl(baseUrl)
                .requestInterceptors(list -> list.addAll(interceptors))
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build().createClient(api);
    }
}
