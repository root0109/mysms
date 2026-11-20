package com.mysms.core.trace;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First filter of every request: continues the caller's W3C trace (or starts one), binds the call context and the
 * logging MDC (traceId), and returns the trace id as `X-Trace-Id` so clients can quote it (problem+json carries it too).
 * It also opens the request's {@link RequestCost} and logs a warning when the request ran more statements or remote
 * calls than one request should (a likely N+1).
 */
public final class TraceFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    public static final String TRACEPARENT = "traceparent";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    private static final Logger LOG = LoggerFactory.getLogger(TraceFilter.class);

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TraceContext trace = TraceContext.parse(request.getHeader(TRACEPARENT))
                .map(TraceContext::newChild)
                .orElseGet(TraceContext::newRoot);
        TraceHolder.set(trace);
        CallContextHolder.set(CallContext.anonymous(trace.traceId()));
        MDC.put(MDC_TRACE_ID, trace.traceId());
        response.setHeader(TRACE_ID_HEADER, trace.traceId());
        RequestCost.Count cost = RequestCost.open();
        try {
            chain.doFilter(request, response);
        } finally {
            cost.close();
            if (cost.aboveWarning()) {
                LOG.warn("{} {} ran {} SQL statements and {} calls to other modules; look for a query or call per row",
                        request.getMethod(), request.getRequestURI(), cost.statements(), cost.remoteCalls());
            }
            MDC.remove(MDC_TRACE_ID);
            CallContextHolder.clear();
            TraceHolder.clear();
        }
    }
}
