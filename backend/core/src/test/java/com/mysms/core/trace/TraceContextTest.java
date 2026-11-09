package com.mysms.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.context.CallContextHolder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceContextTest {

    private static final String PARENT = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    @Test
    void parsesAndFormatsTraceparent() {
        TraceContext context = TraceContext.parse(PARENT).orElseThrow();
        assertThat(context.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(context.spanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(context.sampled()).isTrue();
        assertThat(context.toHeader()).isEqualTo(PARENT);
        TraceContext child = context.newChild();
        assertThat(child.traceId()).isEqualTo(context.traceId());
        assertThat(child.spanId()).isNotEqualTo(context.spanId()).hasSize(16);
        assertThat(TraceContext.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00").orElseThrow()
                .toHeader()).endsWith("-00");
    }

    @Test
    void ignoresMalformedOrZeroIds() {
        assertThat(TraceContext.parse(null)).isEmpty();
        assertThat(TraceContext.parse("garbage")).isEmpty();
        assertThat(TraceContext.parse("00-" + "0".repeat(32) + "-00f067aa0ba902b7-01")).isEmpty();
        assertThat(TraceContext.parse("00-4bf92f3577b34da6a3ce929d0e0e4736-" + "0".repeat(16) + "-01")).isEmpty();
        assertThat(TraceContext.newRoot().traceId()).hasSize(32);
    }

    @Test
    void filterContinuesTheTraceBindsTheContextAndCleansUp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/master/countries");
        request.addHeader(TraceFilter.TRACEPARENT, PARENT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();
        new TraceFilter().doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seen.set(CallContextHolder.require().traceId());
                assertThat(TraceHolder.current()).isPresent();
            }
        });
        assertThat(seen.get()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(response.getHeader(TraceFilter.TRACE_ID_HEADER)).isEqualTo(seen.get());
        assertThat(CallContextHolder.current()).isEmpty();
        assertThat(TraceHolder.current()).isEmpty();
        assertThat(new TraceFilter().getOrder()).isEqualTo(TraceFilter.ORDER);
    }

    @Test
    void filterStartsANewTraceWithoutHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new TraceFilter().doFilter(new MockHttpServletRequest("GET", "/x"), response, new MockFilterChain());
        assertThat(response.getHeader(TraceFilter.TRACE_ID_HEADER)).hasSize(32);
    }
}
