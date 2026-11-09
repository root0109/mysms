package com.mysms.authclient;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Forwards the caller's bearer token on SDK calls made while serving a request, so the called module applies the same
 * identity, tenant and permissions (the tenant is always taken from the token, never from the request).
 */
public final class BearerPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        AuthContextHolder.token().ifPresent(token -> request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        return execution.execute(request, body);
    }
}
