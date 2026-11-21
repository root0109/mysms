package com.mysms.authclient;

import com.mysms.core.error.UnavailableException;
import com.mysms.iam.v1.GetRolePermissionsRequest;
import com.mysms.iam.v1.GetRolePermissionsResponse;
import com.mysms.iam.v1.IamAuthzServiceGrpc;
import com.mysms.iam.v1.RolePermissionCodes;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * The role data source of every process that does not serve iam (Strategy of {@link RolePermissionSource}): iam's
 * {@code IamAuthzService.GetRolePermissions} over gRPC ({@code grpc.client.iam-service}, a verbatim copy of
 * iam_authz.proto), as the reference access-control library's filter falls back to its role service. The call carries
 * the caller's own token, so iam answers only about that caller's tenant and roles; any failure is
 * {@link UnavailableException}, which the edge turns into a refusal (fail closed).
 */
public final class IamAuthzGrpcClient implements RolePermissionSource {

    private final IamAuthzServiceGrpc.IamAuthzServiceBlockingStub stub;

    public IamAuthzGrpcClient(Channel channel) {
        this.stub = IamAuthzServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public RolePermissions rolePermissions(UUID tenant, Set<UUID> roleIds, String bearerToken) {
        GetRolePermissionsRequest.Builder request = GetRolePermissionsRequest.newBuilder().setTenantId(tenant.toString());
        roleIds.forEach(role -> request.addRoleIds(role.toString()));
        try {
            GetRolePermissionsResponse answer =
                    stub.withCallCredentials(new Bearer(bearerToken)).getRolePermissions(request.build());
            Map<UUID, Set<String>> codesByRole = new HashMap<>();
            for (RolePermissionCodes role : answer.getRolesList()) {
                codesByRole.put(UUID.fromString(role.getRoleId()), Set.copyOf(role.getPermissionCodesList()));
            }
            return new RolePermissions(answer.getAuthzVersion(), codesByRole,
                    Set.copyOf(answer.getCommonPermissionCodesList()));
        } catch (StatusRuntimeException e) {
            throw new UnavailableException("iam's role data could not be read (" + e.getStatus().getCode() + ").");
        } catch (RuntimeException e) {
            // an answer that cannot be read is no answer: refuse, never admit
            throw new UnavailableException("iam's role data could not be read.");
        }
    }

    /** The caller's bearer token on the call; with credentials of its own the caller interceptor adds none. */
    private static final class Bearer extends CallCredentials {

        private final String token;

        Bearer(String token) {
            this.token = token;
        }

        @Override
        public void applyRequestMetadata(RequestInfo info, Executor executor, MetadataApplier applier) {
            Metadata headers = new Metadata();
            headers.put(GrpcAuthInterceptor.AUTHORIZATION, EdgeAuthorizer.BEARER_PREFIX + token);
            applier.apply(headers);
        }
    }
}
