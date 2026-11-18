package com.mysms.authclient;

import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.error.Problems;
import com.mysms.core.grpc.CallBinding;
import com.mysms.core.trace.TraceFilter;
import com.mysms.core.web.IdempotencyFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * The servlet edge: the route must be a contract operation (else 404), then {@link EdgeAuthorizer} decides as on gRPC,
 * and only an admitted caller is bound for the request. Controllers and services never parse tokens or check
 * permissions inline.
 */
public final class AuthFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = TraceFilter.ORDER + 10;
    public static final String INSTITUTION_HEADER = "X-Institution-Id";
    /** The tenant a service principal's call acts in (ignored for people: their tenant is the token's). */
    public static final String TENANT_HEADER = "X-Tenant-Id";
    /** The school a guest reads; ignored when the request carries a token. */
    public static final String TENANT_CODE_HEADER = "X-Tenant-Code";

    private final OperationTable operations;
    private final EdgeAuthorizer authorizer;
    private final ProblemResponseWriter problems;
    private final List<PathPattern> infrastructurePaths;

    /**
     * @param infrastructurePaths non-contract endpoints served without a token (health, info), as path patterns
     */
    public AuthFilter(OperationTable operations, EdgeAuthorizer authorizer, ProblemResponseWriter problems,
                      List<String> infrastructurePaths) {
        this.operations = operations;
        this.authorizer = authorizer;
        this.problems = problems;
        PathPatternParser parser = new PathPatternParser();
        this.infrastructurePaths = infrastructurePaths.stream().map(parser::parse).toList();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        PathContainer container = PathContainer.parsePath(path);
        if (infrastructurePaths.stream().anyMatch(p -> p.matches(container))) {
            chain.doFilter(request, response);
            return;
        }
        Optional<OperationRule> rule = operations.match(request.getMethod(), path);
        if (rule.isEmpty()) {
            problems.write(response, HttpStatus.NOT_FOUND, "NOT_FOUND", "No such operation.");
            return;
        }
        // a contract operation without a handler is then answered 501 (not served yet), never a misleading 404
        request.setAttribute(Problems.OPERATION_ATTRIBUTE, rule.get().operationId());
        if (rule.get().idempotency() != null) {
            request.setAttribute(IdempotencyFilter.ATTRIBUTE, rule.get().idempotency());
        }
        EdgeDecision decision = authorizer.decide(rule.get(), request.getHeader(HttpHeaders.AUTHORIZATION),
                request.getHeader(INSTITUTION_HEADER), request.getHeader(TENANT_HEADER),
                request.getHeader(TENANT_CODE_HEADER));
        if (decision instanceof EdgeDecision.Refused refused) {
            if (refused.challenge()) {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
            }
            problems.write(response, refused.status(), refused.code(), refused.detail());
            return;
        }
        try (CallBinding.Scope scope = EdgeIdentity.bind((EdgeDecision.Admitted) decision)) {
            chain.doFilter(request, response);
        }
    }
}
