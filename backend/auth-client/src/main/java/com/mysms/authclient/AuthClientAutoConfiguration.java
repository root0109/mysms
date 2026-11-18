package com.mysms.authclient;

import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.grpc.GrpcEdgeAutoConfiguration;
import com.mysms.core.grpc.GrpcStatuses;
import com.mysms.core.imports.ImportIdentity;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Wires the edge filter and the auth facade into every module. */
@AutoConfiguration(after = GrpcEdgeAutoConfiguration.class)
@EnableConfigurationProperties(AuthClientProperties.class)
public class AuthClientAutoConfiguration {

    @Bean
    public OperationTable operationTable(AuthClientProperties properties) {
        return OperationTable.load(properties.modules());
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenVerifier tokenVerifier(AuthClientProperties properties) {
        return JwtTokenVerifier.from(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public PermissionChecker permissionChecker() {
        return new ClaimsPermissionChecker();
    }

    /**
     * The one authentication and authorization decision of both edges (servlet filter and gRPC interceptor). A module
     * serving guests provides a {@link TenantCodeResolver} (its iam client); in the monolith several modules do and
     * any one answers for all, since each asks the same iam.
     */
    @Bean
    public EdgeAuthorizer edgeAuthorizer(TokenVerifier verifier, PermissionChecker permissions,
                                         ObjectProvider<TenantCodeResolver> resolvers, Clock clock) {
        TenantCodes tenantCodes = resolvers.orderedStream().findFirst()
                .map(resolver -> new TenantCodes(resolver, clock)).orElse(null);
        return new EdgeAuthorizer(verifier, permissions, tenantCodes);
    }

    @Bean
    public AuthFilter authFilter(OperationTable operations, EdgeAuthorizer authorizer, ProblemResponseWriter problems,
                                 AuthClientProperties properties) {
        return new AuthFilter(operations, authorizer, problems, properties.infrastructurePaths());
    }

    /** The gRPC edge: a {@code @GrpcGlobalServerInterceptor} of the starter's server, inside core's error mapping. */
    @Bean
    public GrpcAuthInterceptor grpcAuthInterceptor(OperationTable operations, EdgeAuthorizer authorizer,
                                                   GrpcStatuses statuses) {
        return new GrpcAuthInterceptor(operations, authorizer, statuses);
    }

    /**
     * What every gRPC call to another module carries: the caller's token, institution and tenant, the trace, and a
     * deadline ({@code mysms.grpc.client.deadline}, else MYSMS_GRPC_CLIENT_DEADLINE, 10 s by default).
     */
    @Bean
    public GrpcCallerInterceptor grpcCallerInterceptor(
            @Value("${mysms.grpc.client.deadline:${MYSMS_GRPC_CLIENT_DEADLINE:10s}}") Duration deadline) {
        return new GrpcCallerInterceptor(deadline);
    }

    /**
     * The module's service identity for gRPC calls no person makes: iam's token endpoint ({@code mysms.auth.token-uri},
     * else MYSMS_AUTH_TOKEN_URI) and each module's client secret ({@code mysms.<module>.service-client.secret}, else
     * MYSMS_&lt;MODULE&gt;_SERVICE_CLIENT_SECRET). Unconfigured, a service call answers 503.
     */
    @Bean
    public ServiceTokenProvider serviceTokenProvider(Environment environment, JsonMapper jsonMapper, Clock clock) {
        return new ServiceTokenProvider(environment.getProperty("mysms.auth.token-uri"),
                module -> environment.getProperty("mysms." + module + ".service-client.secret"), jsonMapper, clock);
    }

    /** The importing user's narrowed identity the rows of an import batch are created under. */
    @Bean
    public ImportIdentity importIdentity() {
        return new ImportCallerIdentity();
    }
}
