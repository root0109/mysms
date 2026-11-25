package com.mysms.people;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
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
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.v1.PeopleProto;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
 * Every RPC of people's four gRPC services against the same operation over REST, on real PostgreSQL: the same resource
 * and the same refusals over both transports, the caller's own person record, a blood group's irregular value, and an
 * institution's birthdays and greetings. Three resources are driven here directly, because the parity contract
 * derives a List RPC name they do not have.
 */
@SpringBootTest(classes = PeopleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=PeopleGrpcParityComponentTest")
class PeopleGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, null, List.of("people.*"));
    private static final String NOBODY = token(TENANT, null, List.of());
    private static final String STRANGER = token(OTHER_TENANT, null, List.of("people.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(PeopleDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @BeforeAll
    static void consents() {
        PublicProfileConsents.allowSpecPerson();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "people");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @AfterAll
    static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    /** A tenant-wide caller holding the permissions, linked to the person when one is given (the {@code pid} claim). */
    private static String token(UUID tenant, UUID person, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", permissions);
        claims.put("ins", List.of("*"));
        if (person != null) {
            claims.put("pid", person.toString());
        }
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
            channel = GrpcParity.channel("PeopleGrpcParityComponentTest");
        }
        return channel;
    }

    /** Calls an RPC with the request given as protobuf JSON (lowerCamelCase names, enum value names). */
    private Answer grpc(String rpc, String token, String requestJson) throws Exception {
        MethodDescriptor method = contract().rpc(rpc);
        DynamicMessage.Builder request = GrpcParityContract.request(method);
        JsonFormat.parser().merge(requestJson, request);
        return GrpcParity.call(channel(), method, request.build(), token);
    }

    private void fixtures(UUID tenant, List<String> statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            statements.forEach(statement -> database.jdbc().sql(statement).param("tenant", tenant).update());
            return null;
        }));
    }

    private static String quoted(String value) {
        return JSON.writeValueAsString(value);
    }

    // ------------------------------------------------------------------ the standard operations, resource by resource

    private GrpcParityContract contract() {
        return new GrpcParityContract(PeopleProto.getDescriptor(), this::rest, this::channel, bridge,
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

    // ------------------------------------------------------------------ the caller's own record, irregular values, 501

    @Test
    void theCallersOwnPersonReadsAndChangesTheSameOverBoth() throws Exception {
        UUID person = UUID.randomUUID();
        fixtures(TENANT, List.of("INSERT INTO people.person (id, tenant_id, display_name, first_name, row_version)"
                + " VALUES ('" + person + "', :tenant, 'Asha Rao', 'Asha', 1)"));
        String me = token(TENANT, person, List.of("people.person.read_mine", "people.person.update_mine"));
        Answer mine = rest("GET", "/v1/people/me", me, null);
        Answer mineGrpc = grpc("GetMyPerson", me, "{}");
        assertThat(mine.status()).as(mine.body().toString()).isEqualTo(200);
        assertThat(mineGrpc.status()).as(mineGrpc.code()).isEqualTo(200);
        GrpcParity.assertSameResource(mine.body(), mineGrpc.body().path("person"));
        // no linked person, a person that does not exist, no permission, no token: refused alike
        for (String caller : List.of(token(TENANT, null, List.of("people.person.read_mine")),
                token(TENANT, UUID.randomUUID(), List.of("people.person.read_mine")), NOBODY)) {
            GrpcParityContract.assertSameAnswer(rest("GET", "/v1/people/me", caller, null),
                    grpc("GetMyPerson", caller, "{}"));
        }
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/people/me", null, null), grpc("GetMyPerson", null, "{}"));

        String body = "{\"aboutMe\":\"Teaches physics\"}";
        String update = "{\"person\":{\"aboutMe\":\"Teaches physics\"},\"updateMask\":\"aboutMe\",\"etag\":%s}";
        GrpcParityContract.assertSameAnswer(rest("PATCH", "/v1/people/me", me, body),
                grpc("UpdateMyPerson", me, update.formatted("\"\"")));
        GrpcParityContract.assertSameAnswer(rest("PATCH", "/v1/people/me", me, body, "If-Match", "\"9\""),
                grpc("UpdateMyPerson", me, update.formatted("\"9\"")));
        GrpcParityContract.assertSameAnswer(rest("PATCH", "/v1/people/me", NOBODY, body, "If-Match", "\"1\""),
                grpc("UpdateMyPerson", NOBODY, update.formatted("\"1\"")));
        Answer dryRun = grpc("UpdateMyPerson", me, "{\"person\":{\"aboutMe\":\"Dry run\"},\"validateOnly\":true,"
                + "\"etag\":\"1\"}");
        assertThat(dryRun.status()).as(dryRun.code()).isEqualTo(200);
        assertThat(rest("GET", "/v1/people/me", me, null).body().path("etag").asString())
                .as("a dry run changes nothing").isEqualTo("\"1\"");
        Answer updated = grpc("UpdateMyPerson", me, update.formatted(quoted(mine.body().path("etag").asString())));
        assertThat(updated.status()).as(updated.code()).isEqualTo(200);
        Answer reread = rest("GET", "/v1/people/me", me, null);
        assertThat(reread.body().path("aboutMe").asString()).isEqualTo("Teaches physics");
        GrpcParity.assertSameResource(reread.body(), updated.body().path("person"));
    }

    @Test
    void aBloodGroupTravelsAsItsOwnValueOnEachTransport() throws Exception {
        String admin = token(UUID.randomUUID(), null, List.of("people.*"));
        Answer created = grpc("CreatePerson", admin, "{\"person\":{\"displayName\":\"Blood group\","
                + "\"bloodGroup\":\"BLOOD_GROUP_A1B_NEGATIVE\"}}");
        assertThat(created.status()).as(created.code()).isEqualTo(200);
        String id = created.body().path("person").path("id").asString();
        assertThat(created.body().path("person").path("bloodGroup").asString()).isEqualTo("BLOOD_GROUP_A1B_NEGATIVE");
        Answer read = rest("GET", "/v1/people/persons/" + id, admin, null);
        assertThat(read.body().path("bloodGroup").asString()).isEqualTo("A1B-");
        Answer patched = rest("PATCH", "/v1/people/persons/" + id, admin, "{\"bloodGroup\":\"O+\"}", "If-Match",
                read.body().path("etag").asString());
        assertThat(patched.status()).as(patched.body().toString()).isEqualTo(200);
        assertThat(grpc("GetPerson", admin, "{\"id\":\"" + id + "\"}").body().path("person").path("bloodGroup").asString())
                .isEqualTo("BLOOD_GROUP_O_POSITIVE");
    }

    /** The birthdays and greetings answer and refuse alike over both transports. */
    @Test
    void birthdaysAndGreetingsAnswerLikeRest() throws Exception {
        String person = UUID.randomUUID().toString();
        String institution = "0196f000-0000-7000-8000-00000000b001";
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        fixtures(TENANT, List.of(
                "INSERT INTO people.person (id, tenant_id, display_name, date_of_birth, row_version) VALUES ('"
                        + person + "', :tenant, 'Parity pupil', '" + today.minusYears(12) + "', 1)",
                "INSERT INTO people.member_link (tenant_id, record_id, record_type, person_id, institution_id,"
                        + " source_version, synced_at) VALUES (:tenant, '" + UUID.randomUUID() + "', 'student', '"
                        + person + "', '" + institution + "', 1, now())"));
        String absent = UUID.randomUUID().toString();

        String window = "fromDate=" + today.minusDays(1) + "&toDate=" + today.plusDays(1);
        Answer birthdays = rest("GET", "/v1/people/persons:birthdays?institutionId=" + institution + "&" + window,
                ADMIN, null);
        Answer birthdaysGrpc = grpc("ListBirthdays", ADMIN, "{\"institutionId\":\"" + institution + "\",\"fromDate\":\""
                + today.minusDays(1) + "\",\"toDate\":\"" + today.plusDays(1) + "\"}");
        assertThat(birthdays.status()).as(birthdays.body().toString()).isEqualTo(200);
        assertThat(birthdaysGrpc.body().path("items").findValuesAsString("displayName"))
                .isEqualTo(birthdays.body().path("data").findValuesAsString("displayName")).containsExactly("Parity pupil");
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/people/persons:birthdays?institutionId=" + institution
                        + "&fromDate=" + today + "&toDate=" + today.minusDays(1), ADMIN, null),
                grpc("ListBirthdays", ADMIN, "{\"institutionId\":\"" + institution + "\",\"fromDate\":\"" + today
                        + "\",\"toDate\":\"" + today.minusDays(1) + "\"}"));

        String template = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO people.greeting_template (tenant_id, template_id, is_active,"
                + " source_version, synced_at) VALUES (:tenant, '" + template + "', true, 1, now())"));
        Answer sent = rest("POST", "/v1/people/persons:sendGreetings", ADMIN, "{\"personIds\":[\"" + person
                + "\"],\"templateId\":\"" + template + "\",\"channel\":\"sms\"}");
        Answer sentGrpc = grpc("SendGreetings", ADMIN, "{\"personIds\":[\"" + person + "\"],\"templateId\":\"" + template
                + "\",\"channel\":\"CHANNEL_EMAIL\"}");
        assertThat(sent.status()).as(sent.body().toString()).isEqualTo(200);
        assertThat(sent.body().path("queued").asInt()).isEqualTo(1);
        assertThat(sentGrpc.body().path("result").path("queued").asInt()).isEqualTo(1);
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/people/persons:sendGreetings", ADMIN, "{\"personIds\":[\""
                        + absent + "\"],\"templateId\":\"" + template + "\",\"channel\":\"sms\"}"),
                grpc("SendGreetings", ADMIN, "{\"personIds\":[\"" + absent + "\"],\"templateId\":\"" + template
                        + "\",\"channel\":\"CHANNEL_SMS\"}"));
        assertThat(grpc("SendGreetings", null, "{}").status()).as("authentication first, as on REST").isEqualTo(401);
    }
}
