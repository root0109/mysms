package com.mysms.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** The per-request statement and remote-call counts that expose an N+1. */
class RequestCostTest {

    @Test
    void countsNestAndNothingIsCountedOutsideAnOpenCount() {
        RequestCost.statement();
        RequestCost.remoteCall();
        try (RequestCost.Count outer = RequestCost.open()) {
            RequestCost.statement();
            try (RequestCost.Count inner = RequestCost.open()) {
                RequestCost.statement();
                RequestCost.remoteCall();
                assertThat(inner.statements()).isEqualTo(1);
                assertThat(inner.remoteCalls()).isEqualTo(1);
            }
            RequestCost.statement();
            assertThat(outer.statements()).isEqualTo(3);
            assertThat(outer.remoteCalls()).isEqualTo(1);
        }
        try (RequestCost.Count fresh = RequestCost.open()) {
            assertThat(fresh.statements()).isZero();
            assertThat(fresh.remoteCalls()).isZero();
        }
    }

    @Test
    void aCountPastTheLimitsIsAWarning() {
        try (RequestCost.Count count = RequestCost.open()) {
            for (int i = 0; i < RequestCost.WARN_STATEMENTS; i++) {
                RequestCost.statement();
            }
            assertThat(count.aboveWarning()).isFalse();
            RequestCost.statement();
            assertThat(count.aboveWarning()).isTrue();
        }
        try (RequestCost.Count count = RequestCost.open()) {
            for (int i = 0; i <= RequestCost.WARN_REMOTE_CALLS; i++) {
                RequestCost.remoteCall();
            }
            assertThat(count.aboveWarning()).isTrue();
        }
    }

    @Test
    void theTraceFilterCountsEachRequestOnItsOwn() throws Exception {
        AtomicInteger counted = new AtomicInteger();
        try (RequestCost.Count test = RequestCost.open()) {
            new TraceFilter().doFilter(new MockHttpServletRequest("GET", "/v1/master/countries"),
                    new MockHttpServletResponse(), new MockFilterChain() {
                        @Override
                        public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                            try (RequestCost.Count probe = RequestCost.open()) {
                                RequestCost.statement();
                                RequestCost.statement();
                                counted.set(probe.statements());
                            }
                        }
                    });
            assertThat(counted.get()).isEqualTo(2);
            assertThat(test.statements()).isEqualTo(2);
            RequestCost.statement();
            assertThat(test.statements()).isEqualTo(3);
        }
    }
}
