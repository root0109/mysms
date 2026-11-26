package com.mysms.identity.iam;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.IamAuthzGrpcClient;
import com.mysms.authclient.RolePermissionSource;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.iam.v1.GetRolePermissionsRequest;
import com.mysms.iam.v1.IamAuthzServiceGrpc;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * iam's role-data service over a real gRPC socket, as every other module's permissions filter calls it (auth-client's
 * {@link IamAuthzGrpcClient}, the caller's own token): a tenant user reads the codes of their own roles and the codes
 * every role holds; another role, another tenant, a service or platform token, an expired token and no token are
 * refused.
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IamRoleDataGrpcComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final int GRPC_PORT = freePort();
    private static final String TENANT_CODE = "roledata";
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private IamHttp iam;

    @Autowired
    private RolePermissionSource source;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam", "people");
        registry.add("grpc.server.port", () -> GRPC_PORT);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> TENANT_CODE);
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> "bootstrap-admin-secret-9");
    }

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @BeforeEach
    void connect() {
        iam = new IamHttp(port, database, TENANT_CODE);
        if (channel == null) {
            channel = ManagedChannelBuilder.forAddress("localhost", GRPC_PORT).usePlaintext().build();
        }
    }

    @AfterAll
    static void close() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /** The raw call, to see each refusal's status. */
    private static Status.Code call(String token, UUID tenant, UUID... roles) {
        GetRolePermissionsRequest.Builder request = GetRolePermissionsRequest.newBuilder().setTenantId(tenant.toString());
        for (UUID role : roles) {
            request.addRoleIds(role.toString());
        }
        IamAuthzServiceGrpc.IamAuthzServiceBlockingStub stub = IamAuthzServiceGrpc.newBlockingStub(channel);
        try {
            (token == null ? stub : stub.withCallCredentials(new Bearer(token))).getRolePermissions(request.build());
            return Status.Code.OK;
        } catch (StatusRuntimeException e) {
            return e.getStatus().getCode();
        }
    }

    private static final class Bearer extends CallCredentials {

        private final String token;

        Bearer(String token) {
            this.token = token;
        }

        @Override
        public void applyRequestMetadata(RequestInfo info, Executor executor, MetadataApplier applier) {
            Metadata headers = new Metadata();
            headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
            applier.apply(headers);
        }
    }

    @Test
    void theProcessServingIamReadsItsOwnRoleDataNotItselfOverGrpc() {
        assertThat(source).isInstanceOfSatisfying(com.mysms.authclient.testing.TestRoleData.class, test ->
                assertThat(test.iam()).as("behind the component tests' role fixtures")
                        .isInstanceOf(com.mysms.identity.iam.business.LocalRolePermissions.class));
    }

    @Test
    void aTenantUserReadsTheirOwnRolesOnlyAndEveryOtherCallerIsRefused() throws Exception {
        iam.user("roledatauser", null, "iam.role.read");
        UUID stranger = iam.role("roledata-other", "iam.role.update");
        String token = IamHttp.access(iam.signIn("roledatauser", IamHttp.PASSWORD));
        JsonNode claims = IamHttp.claims(token);
        UUID own = UUID.fromString(claims.path("rgr").get(0).asString().split("@")[0]);
        UUID tenant = iam.tenant();

        RolePermissionSource.RolePermissions answer =
                new IamAuthzGrpcClient(channel).rolePermissions(tenant, Set.of(own), token);
        assertThat(answer.codesByRole().get(own)).containsExactly("iam.role.read");
        assertThat(answer.commonCodes()).isNotEmpty();
        assertThat(answer.version()).isEqualTo(claims.path("azv").asLong());

        assertThat(call(token, tenant, stranger)).as("a role the token does not hold")
                .isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(call(token, tenant, own, stranger)).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(call(token, UUID.randomUUID(), own)).as("another tenant").isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(call(null, tenant, own)).as("no token").isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(call(TOKENS.service("lms", "iam.role.read"), tenant, own)).as("a service token")
                .isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(call(TOKENS.platformOperator(UUID.randomUUID(), "*"), tenant, own)).as("platform staff")
                .isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(call(TOKENS.tenantUser(tenant, UUID.randomUUID(), "*"), tenant, own))
                .as("a token without role claims holds no role").isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(call(TOKENS.tenantUser(tenant, UUID.randomUUID()), tenant)).as("but reads the common codes")
                .isEqualTo(Status.Code.OK);
        Map<String, Object> expired = new HashMap<>();
        expired.put("sub", UUID.randomUUID().toString());
        expired.put("aud", "mysms");
        expired.put("tid", tenant.toString());
        expired.put("rgr", List.of(own + "@tenant"));
        expired.put("azv", 0);
        assertThat(call(TOKENS.custom(expired, Instant.now().minusSeconds(600)), tenant, own)).as("an expired token")
                .isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThatThrownBy(() -> new IamAuthzGrpcClient(channel).rolePermissions(tenant, Set.of(stranger), token))
                .as("the filter's client turns any refusal into 503")
                .isInstanceOf(com.mysms.core.error.UnavailableException.class);
    }
}
