package com.mysms.platform.org;

import com.mysms.platform.PlatformApplication;
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
import com.mysms.platform.PlatformTenants;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.org.config.OrgDatabaseConfig;
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
 * do the academic year's steps, the terms it generates and the public school finder.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
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
    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase platform;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "org", "platform", "master");
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
        PlatformTenants.register(platform, tenant);
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
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("org/crud-spec.json")) {
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
        String[] ids = new String[2];
        for (int i = 0; i < 2; i++) {
            // one active year per institution: each year is its own school's
            fixtures(TENANT, List.of("INSERT INTO org.institution (tenant_id, code, name, row_version, id) VALUES "
                    + "(:tenant, 'CLOSE" + i + "', 'Close school', 1, '0196f000-0000-7000-8000-00000000c00" + i
                    + "') ON CONFLICT (id) DO NOTHING"));
            Answer year = rest("POST", "/v1/org/academic-years", ADMIN, "{\"institutionId\":"
                    + "\"0196f000-0000-7000-8000-00000000c00" + i + "\",\"name\":\"Year " + i
                    + "\",\"startDate\":\"2027-0" + (i + 1) + "-01\",\"endDate\":\"2027-0" + (i + 1) + "-20\"}");
            assertThat(year.status()).as(year.body().toString()).isEqualTo(201);
            ids[i] = year.body().path("id").asString();
        }
        MethodDescriptor close = rpc("CloseAcademicYear");
        // planned years cannot be closed: 409 on both
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + ids[0] + ":close", ADMIN, "{}"),
                grpc(close, set(request(close), "academic_year_id", ids[0]).build(), ADMIN));
        fixtures(TENANT, List.of("UPDATE org.academic_year SET status = 'active' WHERE tenant_id = :tenant AND id IN ('"
                + ids[0] + "', '" + ids[1] + "')"));
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
    void activatingAYearAndGeneratingItsTermsAnswerLikeRest() throws Exception {
        String[] years = new String[2];
        for (int i = 0; i < 2; i++) {
            String institution = "0196f000-0000-7000-8000-00000000e00" + i;
            fixtures(TENANT, List.of("INSERT INTO org.institution (tenant_id, code, name, row_version, id) VALUES "
                    + "(:tenant, 'ACTV" + i + "', 'Activation school " + i + "', 1, '" + institution
                    + "') ON CONFLICT (id) DO NOTHING"));
            Answer year = rest("POST", "/v1/org/academic-years", ADMIN, "{\"institutionId\":\"" + institution
                    + "\",\"name\":\"2027-28\",\"startDate\":\"2027-04-01\",\"endDate\":\"2028-03-31\","
                    + "\"termStructure\":\"two_semesters_quarterly\"}");
            assertThat(year.status()).as(year.body().toString()).isEqualTo(201);
            years[i] = year.body().path("id").asString();
        }
        MethodDescriptor activate = rpc("ActivateAcademicYear");
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + years[0] + ":activate", NOBODY,
                "{}"), grpc(activate, set(request(activate), "academic_year_id", years[0]).build(), NOBODY));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + years[0] + ":activate", STRANGER,
                "{}"), grpc(activate, set(request(activate), "academic_year_id", years[0]).build(), STRANGER));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + years[0] + ":activate", ADMIN,
                        "{\"etag\":\"\\\"99\\\"\"}"),
                grpc(activate, set(set(request(activate), "academic_year_id", years[0]), "etag", "\"99\"").build(),
                        ADMIN));
        Answer byRest = rest("POST", "/v1/org/academic-years/" + years[0] + ":activate", ADMIN, "{}");
        Answer byGrpc = grpc(activate, set(request(activate), "academic_year_id", years[1]).build(), ADMIN);
        assertThat(byRest.status()).as(byRest.body().toString()).isEqualTo(200);
        assertThat(byGrpc.status()).isEqualTo(200);
        assertThat(byGrpc.body().path("academicYear").path("status").asString()).isEqualTo("STATUS_ACTIVE");
        GrpcParity.assertSameResource(rest("GET", "/v1/org/academic-years/" + years[1], ADMIN, null).body(),
                byGrpc.body().path("academicYear"));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + years[0] + ":activate", ADMIN,
                "{}"), grpc(activate, set(request(activate), "academic_year_id", years[1]).build(), ADMIN));

        MethodDescriptor generate = rpc("GenerateTerms");
        Answer termsByRest = rest("POST", "/v1/org/academic-years/" + years[0] + ":generateTerms", ADMIN, "{}");
        Answer termsByGrpc = grpc(generate, set(request(generate), "academic_year_id", years[1]).build(), ADMIN);
        assertThat(termsByRest.status()).as(termsByRest.body().toString()).isEqualTo(200);
        assertThat(termsByGrpc.status()).isEqualTo(200);
        assertThat(termsByGrpc.body().path("items").findValuesAsString("name"))
                .isEqualTo(termsByRest.body().path("items").findValuesAsString("name"));
        assertThat(termsByGrpc.body().path("items").findValuesAsString("startDate"))
                .isEqualTo(termsByRest.body().path("items").findValuesAsString("startDate"));
        Answer again = grpc(generate, set(request(generate), "academic_year_id", years[1]).build(), ADMIN);
        assertThat(again.body().path("items").findValuesAsString("id"))
                .as("a re-run answers the terms it made").isEqualTo(termsByGrpc.body().path("items")
                        .findValuesAsString("id"));
        String unknown = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + unknown + ":generateTerms",
                ADMIN, "{}"), grpc(generate, set(request(generate), "academic_year_id", unknown).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/org/academic-years/" + years[0] + ":generateTerms",
                NOBODY, "{}"), grpc(generate, set(request(generate), "academic_year_id", years[0]).build(), NOBODY));
        assertThat(grpc(activate, set(request(activate), "academic_year_id", unknown).build(), null).status())
                .as("authentication first, as on REST").isEqualTo(401);
    }

    /** The public school finder answers a guest alike over both transports. */
    @Test
    void theSchoolFinderAnswersAGuestLikeRest() throws Exception {
        fixtures(TENANT, List.of("INSERT INTO org.tenant_directory (tenant_id, tenant_code, status, source_version)"
                + " VALUES (:tenant, 'parity-finder', 'active', 1) ON CONFLICT (tenant_id) DO NOTHING"));
        for (String name : List.of("Paritown East", "Paritown West")) {
            Answer created = rest("POST", "/v1/org/institutions", ADMIN, "{\"code\":\"PT" + name.length() + name.charAt(9)
                    + "\",\"name\":\"" + name + "\"}");
            assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
            fixtures(TENANT, List.of("INSERT INTO org.finder_listing (setting_value_id, tenant_id, institution_id,"
                    + " is_listed, source_version, synced_at) VALUES (gen_random_uuid(), :tenant, '"
                    + created.body().path("id").asString() + "', true, 1, now())"));
        }
        MethodDescriptor find = rpc("FindInstitutions");
        Answer overRest = rest("GET", "/v1/org/institutions:find?query=paritown", null, null);
        Answer overGrpc = grpc(find, set(request(find), "query", "paritown").build(), null);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        assertThat(overGrpc.status()).isEqualTo(200);
        assertThat(overGrpc.body().path("items").findValuesAsString("name"))
                .isEqualTo(overRest.body().path("data").findValuesAsString("name"))
                .containsExactly("Paritown East", "Paritown West");
        assertThat(overGrpc.body().path("items").findValuesAsString("tenantCode")).containsOnly("parity-finder");
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/org/institutions:find?query=pa", null, null),
                grpc(find, set(request(find), "query", "pa").build(), null));
    }
}
