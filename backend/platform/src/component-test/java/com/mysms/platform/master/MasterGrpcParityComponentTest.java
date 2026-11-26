package com.mysms.platform.master;

import com.mysms.platform.PlatformApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.FieldMask;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.GrpcParity.Answer;
import com.mysms.core.testkit.GrpcParityContract;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.master.v1.Country;
import com.mysms.master.v1.CountryServiceGrpc;
import com.mysms.master.v1.CreateCountryRequest;
import com.mysms.master.v1.CreateStateProvinceRequest;
import com.mysms.master.v1.DeleteCountryRequest;
import com.mysms.master.v1.GetCountryRequest;
import com.mysms.master.v1.MasterProto;
import com.mysms.master.v1.StateProvince;
import com.mysms.master.v1.StateProvinceServiceGrpc;
import com.mysms.master.v1.UpdateCountryRequest;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Every RPC of master's four gRPC services against the same operation over REST, on real PostgreSQL: the same resource
 * and the same refusals on both transports, and the generated stubs work against the resolvers.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=MasterGrpcParityComponentTest")
class MasterGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String OPERATOR = TOKENS.platformOperator(UUID.randomUUID(), "master.*");
    private static final String TENANT_USER = TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID(), "master.*");
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "master", "platform", "org");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    private Channel channel() {
        if (channel == null) {
            channel = GrpcParity.channel("MasterGrpcParityComponentTest");
        }
        return channel;
    }

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

    private String countryId(String iso2) throws Exception {
        Answer created = rest("POST", "/v1/master/countries", OPERATOR, "{\"iso2\":\"" + iso2 + "\",\"name\":\"Land "
                + iso2 + "\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return created.body().path("id").asString();
    }

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        String country = countryId("ZQ");
        // master is global reference data: no other tenant sees other rows, so there is no stranger to refuse
        GrpcParityContract contract = new GrpcParityContract(MasterProto.getDescriptor(), this::rest, this::channel,
                bridge, new GrpcParityContract.Callers(OPERATOR, TENANT_USER, null));
        List<GrpcParityContract.Resource> resources = List.of(
                resource("Country", "/v1/master/countries", "{\"iso2\":\"QA\",\"iso3\":\"QAX\",\"name\":\"Qland\"}",
                        "{\"shortName\":\"Q\",\"isActive\":false}", true),
                resource("StateProvince", "/v1/master/states",
                        "{\"countryId\":\"" + country + "\",\"code\":\"QS\",\"name\":\"Q State\"}",
                        "{\"name\":\"Q State 2\"}", false),
                resource("City", "/v1/master/cities", "{\"countryId\":\"" + country + "\",\"name\":\"Q City\"}",
                        "{\"name\":\"Q City 2\"}", false),
                resource("Currency", "/v1/master/currencies", "{\"code\":\"QQQ\",\"name\":\"Q Dollar\",\"minorUnits\":3}",
                        "{\"symbol\":\"Q$\"}", true));
        return resources.stream().map(r -> (DynamicNode) DynamicTest.dynamicTest(r.name(), () -> contract.verify(r)))
                .toList();
    }

    private static GrpcParityContract.Resource resource(String name, String collection, String create, String update,
                                                        boolean duplicateConflicts) {
        return new GrpcParityContract.Resource(name, collection, (ObjectNode) JSON.readTree(create),
                (ObjectNode) JSON.readTree(update), duplicateConflicts);
    }

    /** The generated stubs against the resolvers, with what only a typed client shows. */
    @Test
    void theGeneratedStubsWorkAgainstTheResolvers() throws Exception {
        CountryServiceGrpc.CountryServiceBlockingStub countries = CountryServiceGrpc.newBlockingStub(
                GrpcParity.as(channel(), OPERATOR));
        Country created = countries.createCountry(CreateCountryRequest.newBuilder()
                .setCountry(Country.newBuilder().setIso2("qb").setName("Qbland").setIsActive(true)).build()).getCountry();
        assertThat(created.getIso2()).as("normalized by the same service").isEqualTo("QB");
        assertThat(created.getIsActive()).isTrue();
        assertThat(created.getEtag()).isEqualTo("\"1\"");
        assertThat(UUID.fromString(created.getId()).version()).isEqualTo(7);

        // a flag switched off: proto3 cannot send false, the update mask names it
        Country inactive = countries.updateCountry(UpdateCountryRequest.newBuilder()
                .setCountry(Country.newBuilder().setId(created.getId()).setEtag(created.getEtag()))
                .setUpdateMask(FieldMask.newBuilder().addPaths("is_active")).build()).getCountry();
        assertThat(inactive.getIsActive()).isFalse();
        assertThat(inactive.getName()).as("unmasked fields keep their values").isEqualTo("Qbland");
        assertThat(rest("GET", "/v1/master/countries/" + created.getId(), null, null).body().path("isActive")
                .asBoolean()).isFalse();
        // anonymous reads, like REST
        assertThat(CountryServiceGrpc.newBlockingStub(channel()).getCountry(GetCountryRequest.newBuilder()
                .setId(created.getId()).build()).getCountry().getEtag()).isEqualTo("\"2\"");

        // a country with states cannot be deleted: 409 on both
        StateProvinceServiceGrpc.newBlockingStub(GrpcParity.as(channel(), OPERATOR)).createStateProvince(
                CreateStateProvinceRequest.newBuilder().setStateProvince(StateProvince.newBuilder()
                        .setCountryId(created.getId()).setName("Qb State")).build());
        StatusRuntimeException refused = catchStatus(() -> countries.deleteCountry(DeleteCountryRequest.newBuilder()
                .setId(created.getId()).setEtag("\"2\"").build()));
        assertThat(refused.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        GrpcParity.assertSameRefusal(rest("DELETE", "/v1/master/countries/" + created.getId(), OPERATOR, null,
                "If-Match", "\"2\""), GrpcParity.refusal(refused));

        // writes are platform staff's: a tenant user's token is refused, no token is unauthenticated
        StatusRuntimeException tenant = catchStatus(() -> CountryServiceGrpc.newBlockingStub(
                GrpcParity.as(channel(), TENANT_USER)).createCountry(CreateCountryRequest.newBuilder()
                .setCountry(Country.newBuilder().setIso2("QC").setName("Qc")).build()));
        assertThat(tenant.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
        StatusRuntimeException anonymous = catchStatus(() -> CountryServiceGrpc.newBlockingStub(channel())
                .createCountry(CreateCountryRequest.newBuilder().setCountry(Country.newBuilder().setIso2("QC")
                        .setName("Qc")).build()));
        assertThat(anonymous.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    private static StatusRuntimeException catchStatus(Runnable call) {
        try {
            call.run();
        } catch (StatusRuntimeException e) {
            return e;
        }
        throw new AssertionError("the call succeeded");
    }
}
