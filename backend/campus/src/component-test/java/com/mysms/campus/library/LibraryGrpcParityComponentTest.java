package com.mysms.campus.library;

import com.mysms.campus.CampusApplication;
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
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.library.v1.LibraryProto;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import tools.jackson.databind.node.ObjectNode;

/**
 * Every RPC of library's ten gRPC services against the same REST operation, on real PostgreSQL: for each resource of
 * crud-spec.json the create, get, list, update and delete give the same resource over both transports, and both refuse
 * alike — 403 without the permission, 404 for an absent row and another tenant's row, 409 for a duplicate, 412 for a
 * stale etag, 428 without one, 422 for an invalid body; every state transition of crud-spec.json (a reservation's
 * accept and reject, a purchase request's submit, accept and reject) with its refusals, the loan and reservation
 * steps, invoicing a fine (refused alike; finance and people do not run here), and a keyed create replayed by its
 * {@code request_id}, the same way.
 */
@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = GrpcParity.IN_PROCESS_SERVER + "=LibraryGrpcParityComponentTest")
class LibraryGrpcParityComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final String ADMIN = token(TENANT, List.of("library.*"));
    private static final String NOBODY = token(TENANT, List.of());
    private static final String STRANGER = token(OTHER_TENANT, List.of("library.*"));
    private static ManagedChannel channel;

    @LocalServerPort
    private int port;


    @Autowired
    private ProtoBridge bridge;

    @Autowired
    @Qualifier(LibraryDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "library", "transport", "frontoffice", "facility");
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

    // ---- the two transports

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
            channel = GrpcParity.channel("LibraryGrpcParityComponentTest");
        }
        return channel;
    }

    private Answer grpc(MethodDescriptor rpc, DynamicMessage request, String token) {
        return GrpcParity.call(channel(), rpc, request, token);
    }

    private MethodDescriptor rpc(String name) {
        return contract(ADMIN).rpc(name);
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

    private static JsonNode spec() throws Exception {
        return JSON.readTree(LibraryGrpcParityComponentTest.class.getClassLoader().getResourceAsStream("library/crud-spec.json"));
    }

    private static List<String> fixturesOf(JsonNode resource) {
        List<String> fixtures = new ArrayList<>();
        resource.path("fixtures").forEach(statement -> fixtures.add(statement.asString()));
        return fixtures;
    }

    // ---- the standard operations, resource by resource

    private GrpcParityContract contract(String admin) {
        return new GrpcParityContract(LibraryProto.getDescriptor(), this::rest, this::channel, bridge,
                new GrpcParityContract.Callers(admin, NOBODY, STRANGER));
    }

    @TestFactory
    List<DynamicNode> everyStandardRpcAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (GrpcParityContract.Resource resource : GrpcParityContract.resources("library/crud-spec.json")) {
            tests.add(DynamicTest.dynamicTest(resource.name(), () -> {
                fixtures(TENANT, resource.fixtures());
                fixtures(OTHER_TENANT, resource.fixtures());
                // an administrator of their own: a reservation is its requester's, one open request per title
                contract(token(TENANT, List.of("library.*"))).verify(resource);
            }));
        }
        return tests;
    }

    // ---- the state transitions

    @TestFactory
    List<DynamicNode> everyStateTransitionAnswersLikeRest() throws Exception {
        List<DynamicNode> tests = new ArrayList<>();
        for (JsonNode resource : spec().path("resources")) {
            for (JsonNode transition : resource.path("transitions")) {
                tests.add(DynamicTest.dynamicTest(transition.path("operation").asString(),
                        () -> transitionAnswersLikeRest(resource, transition)));
            }
        }
        assertThat(tests).as("accept and reject a reservation; submit, accept and reject a purchase request").hasSize(5);
        return tests;
    }

    /**
     * One transition on two rows of the resource, one per transport: refused alike outside its guard state (409), for
     * a stale etag (412), without the permission (403), for another tenant's row and an absent row (404) and a
     * malformed id (400); then done on both, giving the row REST reads; and refused alike once done (409).
     */
    private void transitionAnswersLikeRest(JsonNode resource, JsonNode transition) throws Exception {
        fixtures(TENANT, fixturesOf(resource));
        String collection = resource.path("collection").asString();
        String table = resource.path("table").asString();
        String operation = transition.path("operation").asString();
        MethodDescriptor rpc = rpc(operation.substring(operation.indexOf('.') + 1));
        String target = transition.path("sets").path("status").asString();
        ObjectNode body = (ObjectNode) transition.path("body");
        String[] ids = new String[2];
        for (int i = 0; i < 2; i++) {
            // each row by a requester of its own: one open reservation per requester and title
            Answer created = rest("POST", collection, token(TENANT, List.of("library.*")),
                    resource.path("create").toString());
            assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
            ids[i] = created.body().path("id").asString();
        }
        fixtures(TENANT, List.of("UPDATE " + table + " SET status = '" + target + "' WHERE tenant_id = :tenant AND id = '"
                + ids[0] + "'"));
        sameAnswer(transition, rpc, ids[0], body, null, ADMIN, 409);
        for (String id : ids) {
            List<String> prepare = new ArrayList<>();
            transition.path("prepare").forEach(statement -> prepare.add(statement.asString().replace("{id}", id)
                    + " AND tenant_id = :tenant"));
            fixtures(TENANT, prepare);
        }
        sameAnswer(transition, rpc, ids[0], body, "\"99\"", ADMIN, 412);
        sameAnswer(transition, rpc, ids[0], body, null, NOBODY, 403);
        sameAnswer(transition, rpc, ids[0], body, null, STRANGER, 404);
        sameAnswer(transition, rpc, UUID.randomUUID().toString(), body, null, ADMIN, 404);
        sameAnswer(transition, rpc, "not-a-uuid", body, null, ADMIN, 400);

        Answer overRest = rest("POST", path(transition, ids[0]), ADMIN, body.toString());
        Answer overGrpc = grpc(rpc, transitionRequest(rpc, ids[1], body, null), ADMIN);
        assertThat(overRest.status()).as(overRest.body().toString()).isEqualTo(200);
        assertThat(overGrpc.status()).as("%s: %s", operation, overGrpc.code()).isEqualTo(200);
        assertThat(overRest.body().path("status").asString()).isEqualTo(target);
        JsonNode row = overGrpc.body().path(rpc.getOutputType().getFields().get(0).getJsonName());
        assertThat(row.path("status").asString()).isEqualTo("STATUS_" + target.toUpperCase(Locale.ROOT));
        GrpcParity.assertSameResource(rest("GET", collection + "/" + ids[1], ADMIN, null).body(), row);
        if (!transition.path("guard").path("status").toString().contains("\"" + target + "\"")) {
            sameAnswer(transition, rpc, ids[0], body, null, ADMIN, 409);
            sameAnswer(transition, rpc, ids[1], body, null, ADMIN, 409);
        }
    }

    private static String path(JsonNode transition, String id) {
        return transition.path("path").asString().replace("{id}", id);
    }

    /** The transition's request: the path id (the request's first field), the REST body's fields, the etag if any. */
    private static DynamicMessage transitionRequest(MethodDescriptor rpc, String id, ObjectNode body, String etag) {
        DynamicMessage.Builder request = request(rpc);
        request.setField(rpc.getInputType().getFields().get(0), id);
        body.properties().forEach(field -> set(request, field.getKey(), field.getValue().asString()));
        if (etag != null) {
            set(request, "etag", etag);
        }
        return request.build();
    }

    private void sameAnswer(JsonNode transition, MethodDescriptor rpc, String id, ObjectNode body, String etag,
                            String token, int expected) throws Exception {
        ObjectNode restBody = body.deepCopy();
        if (etag != null) {
            restBody.put("etag", etag);
        }
        Answer overRest = rest("POST", path(transition, id), token, restBody.toString());
        assertThat(overRest.status()).as("%s over REST: %s", rpc.getName(), overRest.body()).isEqualTo(expected);
        GrpcParityContract.assertSameAnswer(overRest, grpc(rpc, transitionRequest(rpc, id, body, etag), token));
    }

    // ---- the operations not served, and a replay

    /** A copy of a fresh title out on a student's card since ten days ago (due four days ago); answers the loan. */
    private String outOnLoan() {
        String media = UUID.randomUUID().toString();
        String type = UUID.randomUUID().toString();
        String card = UUID.randomUUID().toString();
        String unit = UUID.randomUUID().toString();
        String loan = UUID.randomUUID().toString();
        java.time.LocalDate issued = java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(10);
        fixtures(TENANT, List.of(
                "INSERT INTO library.media (id, tenant_id, institution_id, title, row_version) VALUES ('" + media
                        + "', :tenant, '0196f000-0000-7000-8000-00000000a001', 'Parity title', 1)",
                "INSERT INTO library.library_card_type (id, tenant_id, name, max_items, loan_days, fine_per_day, code,"
                        + " row_version) VALUES ('" + type + "', :tenant, 'Parity " + type + "', 2, 6, 1.25, 'P"
                        + type.substring(0, 8) + "', 1)",
                "INSERT INTO library.library_card (id, tenant_id, institution_id, card_number, card_type_id,"
                        + " holder_type, person_id, student_id, issue_date, row_version) VALUES ('" + card
                        + "', :tenant, '0196f000-0000-7000-8000-00000000a001', 'PC-" + card + "', '" + type
                        + "', 'student', '" + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '2026-01-10', 1)",
                "INSERT INTO library.media_unit (id, tenant_id, media_id, accession_number, status, row_version) VALUES"
                        + " ('" + unit + "', :tenant, '" + media + "', 'PA-" + unit + "', 'issued', 1)",
                "INSERT INTO library.loan (id, tenant_id, media_unit_id, media_id, library_card_id, issued_on, due_on,"
                        + " status, row_version) VALUES ('" + loan + "', :tenant, '" + unit + "', '" + media + "', '"
                        + card + "', '" + issued + "', '" + issued.plusDays(6) + "', 'issued', 1)"));
        return loan;
    }

    @Test
    void theLoanAndReservationStepsAnswerAndRefuseLikeRest() throws Exception {
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        Map<String, String> loanSteps = Map.of("RequestLoanReturn", ":requestReturn", "ReturnLoan", ":return",
                "MarkLoanLost", ":markLost");
        for (Map.Entry<String, String> step : loanSteps.entrySet()) {
            MethodDescriptor rpc = rpc(step.getKey());
            boolean returning = "ReturnLoan".equals(step.getKey());
            String body = returning ? "{\"returnedOn\":\"" + today + "\"}" : "{}";
            String overRestLoan = outOnLoan();
            String overGrpcLoan = outOnLoan();
            DynamicMessage.Builder request = set(request(rpc), "loan_id", overGrpcLoan);
            if (returning) {
                set(request, "returned_on", today);
            }
            Answer byRest = rest("POST", "/v1/library/loans/" + overRestLoan + step.getValue(), ADMIN, body);
            Answer byGrpc = grpc(rpc, request.build(), ADMIN);
            assertThat(byRest.status()).as(step.getKey() + " " + byRest.body()).isEqualTo(200);
            assertThat(byGrpc.status()).as(step.getKey() + " " + byGrpc.code()).isEqualTo(200);
            assertThat(byGrpc.body().path("loan").path("fineAmount").asString()).as(step.getKey())
                    .isEqualTo(byRest.body().path("fineAmount").asString());
            GrpcParity.assertSameResource(rest("GET", "/v1/library/loans/" + overGrpcLoan, ADMIN, null).body(),
                    byGrpc.body().path("loan"));
            if (!returning) {
                GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/loans/" + overRestLoan
                        + step.getValue(), ADMIN, body), grpc(rpc, request.build(), ADMIN));
            }
            String absent = UUID.randomUUID().toString();
            DynamicMessage.Builder unknown = set(request(rpc), "loan_id", absent);
            if (returning) {
                set(unknown, "returned_on", today);
            }
            GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/loans/" + absent + step.getValue(), ADMIN,
                    body), grpc(rpc, unknown.build(), ADMIN));
            GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/loans/" + overRestLoan + step.getValue(),
                    NOBODY, body), grpc(rpc, request.build(), NOBODY));
        }

        MethodDescriptor again = rpc("RequestReservationAgain");
        String[] reservations = new String[2];
        String title = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO library.media (id, tenant_id, institution_id, title, row_version) VALUES"
                + " ('" + title + "', :tenant, '0196f000-0000-7000-8000-00000000a001', 'Reserved title', 1)"));
        for (int i = 0; i < 2; i++) {
            reservations[i] = UUID.randomUUID().toString();
            fixtures(TENANT, List.of("INSERT INTO library.reservation (id, tenant_id, media_id, requested_by_user_id,"
                    + " date_from, date_to, status, row_version) VALUES ('" + reservations[i] + "', :tenant, '"
                    + title + "', '" + UUID.randomUUID() + "', '" + today + "', '" + today
                    + "', 'rejected', 1)"));
        }
        Answer againByRest = rest("POST", "/v1/library/reservations/" + reservations[0] + ":requestAgain", ADMIN,
                "{}");
        Answer againByGrpc = grpc(again, set(request(again), "reservation_id", reservations[1]).build(), ADMIN);
        assertThat(againByRest.status()).as(againByRest.body().toString()).isEqualTo(200);
        assertThat(againByGrpc.status()).isEqualTo(200);
        GrpcParity.assertSameResource(rest("GET", "/v1/library/reservations/" + reservations[1], ADMIN, null).body(),
                againByGrpc.body().path("reservation"));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/reservations/" + reservations[0]
                + ":requestAgain", ADMIN, "{}"), grpc(again, set(request(again), "reservation_id", reservations[1])
                .build(), ADMIN));
        assertThat(grpc(again, set(request(again), "reservation_id", reservations[0]).build(), null).status())
                .as("authentication first, as on REST").isEqualTo(401);
    }

    /** A school-level member holding a reservation's standard operations but none of the staff steps. */
    private static String member(UUID user) {
        List<String> permissions = List.of("library.reservation.read", "library.reservation.create",
                "library.reservation.update", "library.reservation.delete");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", TENANT.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", List.of());
        claims.put("ins", List.of(SCHOOL));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private static final String SCHOOL = "0196f000-0000-7000-8000-00000000a001";

    /**
     * The requester of a reservation is the caller over gRPC as over REST, whatever the request names, and another
     * member's reservation is refused alike.
     */
    @Test
    void aReservationsRequesterComesFromTheTokenLikeRest() throws Exception {
        UUID me = UUID.randomUUID();
        String mine = member(me);
        String theirs = member(UUID.randomUUID());
        String title = UUID.randomUUID().toString();
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        fixtures(TENANT, List.of("INSERT INTO library.media (id, tenant_id, institution_id, title, row_version) VALUES"
                + " ('" + title + "', :tenant, '" + SCHOOL + "', 'Wanted title', 1)"));

        MethodDescriptor create = rpc("CreateReservation");
        DynamicMessage.Builder creating = request(create);
        JsonFormat.parser().merge("{\"requestId\":\"" + UUID.randomUUID() + "\",\"reservation\":{\"mediaId\":\""
                + title + "\",\"requestedByUserId\":\"" + UUID.randomUUID() + "\",\"dateFrom\":\"" + today
                + "\",\"dateTo\":\"" + today + "\"}}", creating);
        Answer created = grpc(create, creating.build(), mine);
        assertThat(created.status()).as(created.code()).isEqualTo(200);
        JsonNode reservation = created.body().path("reservation");
        assertThat(reservation.path("requestedByUserId").asString()).as("the requester comes from the token")
                .isEqualTo(me.toString());
        String item = "/v1/library/reservations/" + reservation.path("id").asString();
        GrpcParity.assertSameResource(rest("GET", item, mine, null).body(), reservation);

        MethodDescriptor get = rpc("GetReservation");
        GrpcParityContract.assertSameAnswer(rest("GET", item, theirs, null),
                grpc(get, set(request(get), "id", reservation.path("id").asString()).build(), theirs));
        MethodDescriptor update = rpc("UpdateReservation");
        DynamicMessage.Builder editing = request(update);
        JsonFormat.parser().merge("{\"id\":\"" + reservation.path("id").asString()
                + "\",\"reference\":{\"value\":\"Mine\"},\"etag\":\"\\\"1\\\"\"}", editing);
        Answer refused = rest("PATCH", item, theirs, "{\"reference\":\"Mine\"}", "If-Match", "\"1\"");
        assertThat(refused.status()).as("another member's reservation").isEqualTo(404);
        GrpcParityContract.assertSameAnswer(refused, grpc(update, editing.build(), theirs));
    }

    /** Another member's loan and card are refused over gRPC as over REST; the borrower's own are answered alike. */
    @Test
    void aMembersLoansAndCardsAreTheirOwnLikeRest() throws Exception {
        UUID borrower = UUID.randomUUID();
        UUID holder = UUID.randomUUID();
        String card = UUID.randomUUID().toString();
        String loan = UUID.randomUUID().toString();
        String title = UUID.randomUUID().toString();
        String unit = UUID.randomUUID().toString();
        String type = UUID.randomUUID().toString();
        fixtures(TENANT, List.of("INSERT INTO library.media (id, tenant_id, institution_id, title, row_version) VALUES"
                + " ('" + title + "', :tenant, '" + SCHOOL + "', 'Lent title', 1)",
                "INSERT INTO library.media_unit (id, tenant_id, media_id, accession_number, status, row_version) VALUES"
                        + " ('" + unit + "', :tenant, '" + title + "', 'P-" + unit + "', 'issued', 1)",
                "INSERT INTO library.library_card_type (id, tenant_id, name, max_items, loan_days, code, row_version)"
                        + " VALUES ('" + type + "', :tenant, 'Parity', 2, 14, 'P" + type.substring(0, 6) + "', 1)",
                "INSERT INTO library.library_card (id, tenant_id, institution_id, card_number, card_type_id,"
                        + " holder_type, person_id, staff_id, issue_date, row_version) VALUES ('" + card + "', :tenant, '"
                        + SCHOOL + "', 'P-" + card.substring(0, 8) + "', '" + type + "', 'staff', '" + holder + "', '"
                        + UUID.randomUUID() + "', '2026-01-10', 1)",
                "INSERT INTO library.loan (id, tenant_id, media_unit_id, media_id, library_card_id, borrower_user_id,"
                        + " issued_on, status, row_version) VALUES ('" + loan + "', :tenant, '" + unit + "', '" + title
                        + "', '" + card + "', '" + borrower + "', '2026-01-10', 'issued', 1)"));
        String mine = reader(borrower, holder);
        String theirs = reader(UUID.randomUUID(), UUID.randomUUID());

        MethodDescriptor getLoan = rpc("GetLoan");
        Answer refused = rest("GET", "/v1/library/loans/" + loan, theirs, null);
        assertThat(refused.status()).as("another member's loan").isEqualTo(404);
        GrpcParityContract.assertSameAnswer(refused, grpc(getLoan, set(request(getLoan), "id", loan).build(), theirs));
        Answer own = rest("GET", "/v1/library/loans/" + loan, mine, null);
        assertThat(own.status()).isEqualTo(200);
        GrpcParityContract.assertSameAnswer(own, grpc(getLoan, set(request(getLoan), "id", loan).build(), mine));
        MethodDescriptor getCard = rpc("GetLibraryCard");
        Answer otherCard = rest("GET", "/v1/library/library-cards/" + card, theirs, null);
        assertThat(otherCard.status()).as("another person's card").isEqualTo(404);
        GrpcParityContract.assertSameAnswer(otherCard, grpc(getCard, set(request(getCard), "id", card).build(),
                theirs));
        GrpcParityContract.assertSameAnswer(rest("GET", "/v1/library/library-cards/" + card, mine, null),
                grpc(getCard, set(request(getCard), "id", card).build(), mine));
    }

    /** A school-level member who may read loans and cards but holds none of the staff permissions. */
    private static String reader(UUID user, UUID person) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("pid", person.toString());
        claims.put("tid", TENANT.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("library.loan.read", "library.library_card.read"));
        claims.put("tperms", List.of());
        claims.put("ins", List.of(SCHOOL));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    /** Invoicing a fine needs finance and people, which do not run here: the refusals before the call, and the 503. */
    @Test
    void invoicingAFineIsRefusedAlikeOverBoth() throws Exception {
        MethodDescriptor invoice = rpc("InvoiceLoanFine");
        String out = outOnLoan();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/loans/" + out + ":invoiceFine", ADMIN, "{}"),
                grpc(invoice, set(request(invoice), "loan_id", out).build(), ADMIN));
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        assertThat(rest("POST", "/v1/library/loans/" + out + ":return", ADMIN, "{\"returnedOn\":\"" + today + "\"}")
                .body().path("fineAmount").asString()).isEqualTo("5.00");
        Answer unavailable = rest("POST", "/v1/library/loans/" + out + ":invoiceFine", ADMIN, "{}");
        assertThat(unavailable.status()).as(unavailable.body().toString()).isEqualTo(503);
        GrpcParityContract.assertSameAnswer(unavailable, grpc(invoice, set(request(invoice), "loan_id", out).build(),
                ADMIN));
        String absent = UUID.randomUUID().toString();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/loans/" + absent + ":invoiceFine", ADMIN,
                "{}"), grpc(invoice, set(request(invoice), "loan_id", absent).build(), ADMIN));
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/loans/" + out + ":invoiceFine", NOBODY, "{}"),
                grpc(invoice, set(request(invoice), "loan_id", out).build(), NOBODY));
    }

    @Test
    void aKeyedCreateIsReplayedByItsRequestIdLikeRestsIdempotencyKey() throws Exception {
        MethodDescriptor create = rpc("CreateAuthor");
        DynamicMessage.Builder author = DynamicMessage.newBuilder(create.getInputType().findFieldByName("author")
                .getMessageType());
        String key = UUID.randomUUID().toString();
        DynamicMessage first = set(set(request(create), "author", set(author, "name", "Replayed author").build()),
                "request_id", key).build();
        Answer created = grpc(create, first, ADMIN);
        Answer replayed = grpc(create, first, ADMIN);
        assertThat(created.status()).as(String.valueOf(created.code())).isEqualTo(200);
        assertThat(replayed.body()).as("the first answer, not a second author").isEqualTo(created.body());
        Answer listed = rest("GET", "/v1/library/authors?filter=name%20%3D%20%22Replayed%20author%22", ADMIN, null);
        assertThat(listed.body().path("data")).hasSize(1);

        String restKey = UUID.randomUUID().toString();
        Answer overRest = rest("POST", "/v1/library/authors", ADMIN, "{\"name\":\"Replayed over REST\"}",
                "Idempotency-Key", restKey);
        assertThat(overRest.status()).isEqualTo(201);
        assertThat(rest("POST", "/v1/library/authors", ADMIN, "{\"name\":\"Replayed over REST\"}", "Idempotency-Key",
                restKey).body().path("id")).isEqualTo(overRest.body().path("id"));
        // the key reused for another request is refused alike
        DynamicMessage other = set(set(request(create), "author", set(author, "name", "Another author").build()),
                "request_id", key).build();
        GrpcParityContract.assertSameAnswer(rest("POST", "/v1/library/authors", ADMIN, "{\"name\":\"Another author\"}",
                "Idempotency-Key", restKey), grpc(create, other, ADMIN));
        assertThat(grpc(create, other, ADMIN).code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }
}
