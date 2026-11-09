package com.mysms.authclient;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.error.Problems;
import com.mysms.core.trace.TraceFilter;
import com.mysms.core.web.IdempotencyFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Every authentication and authorization check of a request, in one place at the edge (JAVA-CODING-RULES §16):
 * the route must be a contract operation; the bearer token is verified locally; the token's kind must match the
 * operation's security scheme (tenant user vs platform staff); `X-Institution-Id` must lie within the caller's scopes;
 * the operation's x-permission must be granted. Only then is {@link AuthContext} bound and the request passed on.
 * Controllers and services never parse tokens or headers and never check permissions inline.
 */
public final class AuthFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = TraceFilter.ORDER + 10;
    public static final String INSTITUTION_HEADER = "X-Institution-Id";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PERMISSION_DENIED = "PERMISSION_DENIED";

    private final OperationTable operations;
    private final TokenVerifier verifier;
    private final PermissionChecker permissions;
    private final ProblemResponseWriter problems;
    private final List<PathPattern> infrastructurePaths;

    /**
     * @param infrastructurePaths non-contract endpoints served without a token (health, info), as path patterns
     */
    public AuthFilter(OperationTable operations, TokenVerifier verifier, PermissionChecker permissions,
                      ProblemResponseWriter problems, List<String> infrastructurePaths) {
        this.operations = operations;
        this.verifier = verifier;
        this.permissions = permissions;
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
        String header = request.getHeader("Authorization");
        VerifiedToken token = null;
        if (header != null) {
            if (!header.startsWith(BEARER_PREFIX) || header.length() == BEARER_PREFIX.length()) {
                unauthorized(response, InvalidTokenException.UNAUTHENTICATED, "Authorization must be a bearer token.");
                return;
            }
            try {
                token = verifier.verify(header.substring(BEARER_PREFIX.length()).trim());
            } catch (InvalidTokenException e) {
                unauthorized(response, e.code(), e.getMessage());
                return;
            }
        }
        if (token == null) {
            if (rule.get().isPublic()) {
                chain.doFilter(request, response);
            } else {
                unauthorized(response, InvalidTokenException.UNAUTHENTICATED, "A bearer token is required.");
            }
            return;
        }
        Optional<AuthContext> caller = authorize(rule.get(), token, request, response);
        if (caller.isEmpty()) {
            return;
        }
        proceedAs(caller.get(), header.substring(BEARER_PREFIX.length()).trim(), token.authMethods(), request, response,
                chain);
    }

    /** Scheme, institution and permission checks; writes the 400/403 itself and returns empty when refused. */
    private Optional<AuthContext> authorize(OperationRule rule, VerifiedToken token, HttpServletRequest request,
                                            HttpServletResponse response) throws IOException {
        AuthContext context = token.context();
        if (!rule.isPublic()) {
            boolean schemeAllowed = context.platformOperator()
                    ? rule.schemes().contains(OperationRule.PLATFORM_ADMIN)
                    : rule.schemes().contains(OperationRule.BEARER) && context.tenantId().isPresent()
                            && token.audiences().contains(AuthClientProperties.TENANT_AUDIENCE);
            if (!schemeAllowed) {
                problems.write(response, HttpStatus.FORBIDDEN, PERMISSION_DENIED,
                        "This operation is not available to this kind of account.");
                return Optional.empty();
            }
        }
        String institution = request.getHeader(INSTITUTION_HEADER);
        if (institution != null) {
            UUID selected;
            try {
                selected = UUID.fromString(institution.trim());
            } catch (IllegalArgumentException e) {
                problems.write(response, HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", INSTITUTION_HEADER + " is not a UUID.");
                return Optional.empty();
            }
            if (!context.coversInstitution(selected)) {
                problems.write(response, HttpStatus.FORBIDDEN, PERMISSION_DENIED,
                        "The institution is outside the caller's scope.");
                return Optional.empty();
            }
            context = context.withActiveInstitution(selected);
        }
        if (!rule.isPublic() && !permissions.isPermitted(context, rule.permission())) {
            problems.write(response, HttpStatus.FORBIDDEN, PERMISSION_DENIED,
                    "Missing permission " + rule.permission() + ".");
            return Optional.empty();
        }
        return Optional.of(context);
    }

    private void proceedAs(AuthContext context, String rawToken, Set<String> authMethods, HttpServletRequest request,
                           HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        Optional<CallContext> before = CallContextHolder.current();
        String traceId = before.map(CallContext::traceId).orElseGet(() -> UUID.randomUUID().toString());
        CallContextHolder.set(CallContext.anonymous(traceId).withIdentity(context.tenantId().orElse(null), context.userId()));
        AuthContextHolder.set(context, rawToken, authMethods);
        context.tenantId().ifPresent(t -> MDC.put("tenantId", t.toString()));
        MDC.put("userId", context.userId().toString());
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("userId");
            AuthContextHolder.clear();
            before.ifPresentOrElse(CallContextHolder::set, CallContextHolder::clear);
        }
    }

    private void unauthorized(HttpServletResponse response, String code, String detail) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        problems.write(response, HttpStatus.UNAUTHORIZED, code, detail);
    }
}
