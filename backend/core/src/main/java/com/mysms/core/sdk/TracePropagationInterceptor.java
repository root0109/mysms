package com.mysms.core.sdk;

import com.mysms.core.trace.TraceContext;
import com.mysms.core.trace.TraceFilter;
import com.mysms.core.trace.TraceHolder;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Continues the current trace on every outbound SDK call (new span, same trace id). */
public final class TracePropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        TraceContext next = TraceHolder.current().map(TraceContext::newChild).orElseGet(TraceContext::newRoot);
        request.getHeaders().set(TraceFilter.TRACEPARENT, next.toHeader());
        return execution.execute(request, body);
    }
}
