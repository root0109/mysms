package com.mysms.org;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.org.config.OrgDatabaseConfig;
import com.mysms.org.v1.OrgProto;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every RPC of org's six gRPC services against the same operation over REST, on real PostgreSQL: each resource of
 * crud-spec.json gives the same resource and the same refusals (403, 404, 409, 412, 428, 422) over both transports, as
 * do the academic year's close transition and the operations this release does not serve (501).
 */
@SpringBootTest(classes = OrgApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=OrgGrpcParityComponentTest")
class OrgGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("org.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("org.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(OrgDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /** A tenant-wide administrator of the tenant holding the permissions (every institution, tenant-level rows). */
    private static String token(UUID tenant, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", permissions);
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    // ------------------------------------------------------------------ the two transports

    private Answer rest(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
        return new Answer(response.statusCode(), response.statusCode() >= 400 ? json.path("code").asString() : null,
                json);
    }

    private Channel channel() {
        if (channel == null) {
            channel = GrpcParity.channel("OrgGrpcParityComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, DynamicMessage request, String token) {
        return GrpcParity.call(channel(), rpc, request, token);
    }

    private MethodDescriptor rpc(String name) {
        return contract().rpc(name);
    }

    private static DynamicMessage.Builder request(MethodDescriptor rpc) {
        return GrpcParityContract.request(rpc);
    }

    private static DynamicMessage.Builder set(DynamicMessage.Builder builder, String field, Object value) {
        return GrpcParityContract.set(builder, field, value);
    }

    private void fixtures(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }

    // ------------------------------------------------------------------ the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(OrgProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(ADMIN, NOBODY, STRANGER));
    }

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("crud-spec.json")) {
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, resource.fixtures());
                fixtures(OTHER_TENANT, resource.fixtures());
                contract().verify(resource);
            }));
        }
        return tests;
    }

    // ------------------------------------------------------------------ the workflow and the operations not served

    @Test
    void closingAnAcademicYearAnswersLikeRest() throws Exception {
        fixtures(TENANT, List.of("INSERT INTO org.institution (tenant_id, code, name, row_version, id) VALUES (:tenant,"
                + " 'CLOSE1', 'Close school', 1, '0196f000-0000-7000-8000-00000000c001') ON CONFLICT (id) DO NOTHING"));
        String[] ids = new String[2];
        for (int i = 0; i < 2; i++) {
            Answer year = rest("POST", "/v1/org/academic-years", ADMIN, "{\"institutionId\":"
                    + "\"0196f000-0000-7000-8000-00000000c001\",\"name\":\"Year " + i + "\",\"startDate\":\"2027-0" + (i + 1)
                    + "-01\",\"endDate\":\"2027-0" + (i + 1) + "-20\"}");
            assertThat(year.status()).as(year.body().toString()).isEqualTo(201);
            ids[i] = year.body().path("id").asString();
        }
        MethodDescriptor close = rpc("CloseAcademicYear");
        // planned years cannot be closed: 409 on both
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + ids[0] + ":close", ADMIN, "{}"),
                grpc(close, set(request(close), "academic_year_id", ids[0]).build(), ADMIN));
        fixtures(TENANT, List.of("UPDATE org.academic_year SET status = 'active' WHERE tenant_id = :tenant"));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + ids[0] + ":close", ADMIN,
                        "{\"etag\":\"\\\"99\\\"\"}"),
                grpc(close, set(set(request(close), "academic_year_id", ids[0]), "etag", "\"99\"").build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + ids[0] + ":close", NOBODY, "{}"),
                grpc(close, set(request(close), "academic_year_id", ids[0]).build(), NOBODY));
        Answer closedRest = rest("POST", "/v1/org/academic-years/" + ids[0] + ":close", ADMIN, "{}");
        Answer closedGrpc = grpc(close, set(request(close), "academic_year_id", ids[1]).build(), ADMIN);
        assertThat(closedRest.status()).isEqualTo(200);
        assertThat(closedGrpc.status()).isEqualTo(200);
        JsonNode grpcYear = closedGrpc.body().path("academicYear");
        assertThat(grpcYear.path("status").asString()).isEqualTo("STATUS_CLOSED");
        GrpcParity.assertSameResource(rest("GET", "/v1/org/academic-years/" + ids[1], ADMIN, null).body(), grpcYear);
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + ids[1] + ":close", ADMIN, "{}"),
                grpc(close, set(request(close), "academic_year_id", ids[1]).build(), ADMIN));
    }

    /**
     * The active institutions of the tenant a service call names, by name, over both transports: inactive and other
     * tenants' institutions never appear, and a person's token or a service without the permission is refused.
     */
    @Test
    void activeInstitutionsAreListedForServicesOnly() throws Exception {
        fixtures(TENANT, List.of(
                "INSERT INTO org.institution (tenant_id, code, name, row_version, id) VALUES (:tenant, 'ACT-B', "
                        + "'Beta school', 1, '0196f000-0000-7000-8000-00000000a0b1') ON CONFLICT (id) DO NOTHING",
                "INSERT INTO org.institution (tenant_id, code, name, row_version, id) VALUES (:tenant, 'ACT-A', "
                        + "'Alpha school', 1, '0196f000-0000-7000-8000-00000000a0a1') ON CONFLICT (id) DO NOTHING",
                "INSERT INTO org.institution (tenant_id, code, name, status, row_version, id) VALUES (:tenant, "
                        + "'ACT-C', 'Closed school', 'inactive', 1, '0196f000-0000-7000-8000-00000000a0c1') "
                        + "ON CONFLICT (id) DO NOTHING"));
        fixtures(OTHER_TENANT, List.of("INSERT INTO org.institution (tenant_id, code, name, row_version, id) VALUES "
                + "(:tenant, 'ACT-X', 'Another tenant school', 1, '0196f000-0000-7000-8000-00000000a0d1') "
                + "ON CONFLICT (id) DO NOTHING"));
        String service = TOKENS.service("iam", "org.institution.list_active");
        String path = "/v1/org/institutions:active?filter=code%20%3A%20%22ACT-%22";
        Answer listed = rest("GET", path, service, null, "X-Tenant-Id", TENANT.toString());
        assertThat(listed.status()).as(listed.body().toString()).isEqualTo(200);
        assertThat(listed.body().path("data").findValuesAsString("name")).containsExactly("Alpha school", "Beta school");

        MethodDescriptor active = rpc("ListActiveInstitutions");
        Metadata tenant = new Metadata();
        tenant.put(Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER), TENANT.toString());
        Channel inTenant = ClientInterceptors.intercept(channel(), MetadataUtils.newAttachHeadersInterceptor(tenant));
        Answer overGrpc = GrpcParity.call(inTenant, active, set(request(active), "filter", "code : \"ACT-\"").build(),
                service);
        assertThat(overGrpc.status()).isEqualTo(200);
        assertThat(overGrpc.body().path("items").findValuesAsString("name")).containsExactly("Alpha school",
                "Beta school");
        GrpcParity.assertSameResource(listed.body().path("data").get(0), overGrpc.body().path("items").get(0));

        GrpcParityContract.assertSameAnswer(rest("GET", path, ADMIN, null), grpc(active, request(active).build(), ADMIN));
        String unpermitted = TOKENS.service("iam");
        GrpcParityContract.assertSameAnswer(rest("GET", path, unpermitted, null, "X-Tenant-Id", TENANT.toString()),
                GrpcParity.call(inTenant, active, request(active).build(), unpermitted));
        assertThat(rest("GET", path, ADMIN, null).status()).as("a person's token").isEqualTo(403);
        assertThat(rest("GET", path, null, null).status()).isEqualTo(401);
    }

    @Test
    void theOperationsThisReleaseDoesNotServeAre501OnBoth() throws Exception {
        String year = UUID.randomUUID().toString();
        MethodDescriptor activate = rpc("ActivateAcademicYear");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + year + ":activate", ADMIN, "{}"),
                grpc(activate, set(request(activate), "academic_year_id", year).build(), ADMIN));
        MethodDescriptor generate = rpc("GenerateTerms");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + year + ":generateTerms", ADMIN, "{}"),
                grpc(generate, set(request(generate), "academic_year_id", year).build(), ADMIN));
        MethodDescriptor find = rpc("FindInstitutions");
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/org/institutions:find?query=abc", null, null),
                grpc(find, set(request(find), "query", "abc").build(), null));
        assertThat(grpc(activate, set(request(activate), "academic_year_id", year).build(), null).status())
                .as("authentication first, as on REST").isEqualTo(401);
    }
}
