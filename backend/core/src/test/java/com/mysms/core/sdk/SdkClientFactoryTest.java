package com.mysms.core.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mysms.core.trace.TraceContext;
import com.mysms.core.trace.TraceHolder;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;

class SdkClientFactoryTest {

    interface PingApi {
        @GetExchange("/v1/ping")
        String ping();
    }

    @AfterEach
    void tearDown() {
        TraceHolder.clear();
    }

    @Test
    void createsClientsThatContinueTheTrace() {
        TraceContext trace = TraceContext.newRoot();
        TraceHolder.set(trace);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://finance.local/v1/ping"))
                .andExpect(header("traceparent", Matchers.startsWith("00-" + trace.traceId() + "-")))
                .andExpect(header("X-Extra", "yes"))
                .andRespond(withSuccess("pong", MediaType.TEXT_PLAIN));
        SdkClientFactory factory = new SdkClientFactory(builder, List.of(new TracePropagationInterceptor(),
                (request, body, execution) -> {
                    request.getHeaders().set("X-Extra", "yes");
                    return execution.execute(request, body);
                }));
        assertThat(factory.create(PingApi.class, "http://finance.local").ping()).isEqualTo("pong");
        server.verify();
    }

    @Test
    void startsATraceWhenNoneIsBound() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://x.local/v1/ping")).andExpect(header("traceparent", Matchers.matchesRegex(
                "^00-[0-9a-f]{32}-[0-9a-f]{16}-01$"))).andRespond(withSuccess("pong", MediaType.TEXT_PLAIN));
        new SdkClientFactory(builder, List.of(new TracePropagationInterceptor())).create(PingApi.class, "http://x.local").ping();
        server.verify();
    }
}
