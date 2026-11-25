package com.mysms.library;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.library.config.LibraryDatabaseConfig;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The loan and reservation workflows over HTTP against real PostgreSQL as the application role: the borrower drops a
 * copy for return, the librarian checks it in with the card type's fine (idempotently) or marks it lost, a refused or
 * cancelled reservation is asked for again — each inside the caller's reach, or the borrower's own. Each test works in
 * a tenant of its own.
 */
@SpringBootTest(classes = LibraryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LibraryLoanWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String RETURN = "library.loan.return_loan";
    private static final String LOST = "library.loan.mark_loan_lost";
    private static final String REQUEST_RETURN = "library.loan.request_loan_return";
    private static final String AGAIN = "library.reservation.request_reservation_again";
    /** A member's reservation rights: every standard operation, no staff step. */
    private static final String[] RESERVATION_MEMBER = {"library.reservation.read", "library.reservation.create",
        "library.reservation.update", "library.reservation.delete"};

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LibraryDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "library");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school library of its own tenant: a title, a card type fining 2.50 a day, a student's card, a librarian. */
    private final class Library {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID media = UUID.randomUUID();
        final UUID cardType = UUID.randomUUID();
        final UUID card = UUID.randomUUID();
        final UUID borrower = UUID.randomUUID();
        final String librarian = token(tenant, UUID.randomUUID(), institution.toString(), RETURN, LOST,
                REQUEST_RETURN, AGAIN, "library.loan.invoice_loan_fine");
        final String student = token(tenant, borrower, UUID.randomUUID().toString(), REQUEST_RETURN, AGAIN);

        Library() {
            sql("INSERT INTO library.media (id, tenant_id, institution_id, title, row_version) VALUES ('" + media
                    + "', :tenantId, '" + institution + "', 'A title', 1)");
            sql("INSERT INTO library.library_card_type (id, tenant_id, name, max_items, loan_days, fine_per_day, code,"
                    + " row_version) VALUES ('" + cardType + "', :tenantId, 'Student', 2, 14, 2.50, 'STU', 1)");
            sql("INSERT INTO library.library_card (id, tenant_id, institution_id, card_number, card_type_id,"
                    + " holder_type, person_id, student_id, issue_date, row_version) VALUES ('" + card + "', :tenantId, '"
                    + institution + "', 'C-1', '" + cardType + "', 'student', '" + UUID.randomUUID() + "', '"
                    + UUID.randomUUID() + "', '2026-01-10', 1)");
        }

        /** A copy out on loan since the given day, due 14 days later; answers the loan's id. */
        UUID loan(LocalDate issuedOn) {
            UUID unit = UUID.randomUUID();
            UUID loan = UUID.randomUUID();
            sql("INSERT INTO library.media_unit (id, tenant_id, media_id, accession_number, status, row_version) VALUES"
                    + " ('" + unit + "', :tenantId, '" + media + "', 'A-" + unit + "', 'issued', 1)");
            sql("INSERT INTO library.loan (id, tenant_id, media_unit_id, media_id, library_card_id, borrower_user_id,"
                    + " issued_on, due_on, status, row_version) VALUES ('" + loan + "', :tenantId, '" + unit + "', '"
                    + media + "', '" + card + "', '" + borrower + "', '" + issuedOn + "', '" + issuedOn.plusDays(14)
                    + "', 'issued', 1)");
            return loan;
        }

        String value(String query) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(query).query(String.class)
                            .single()));
        }

        void sql(String statement) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).update()));
        }
    }

    private Reply post(String path, String body, String token) throws Exception {
        return send("POST", path, body, token);
    }

    private Reply send(String method, String path, String body, String token, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static List<String> ids(Reply list) {
        List<String> ids = new ArrayList<>();
        list.body().path("data").forEach(item -> ids.add(item.path("id").asString()));
        return ids;
    }

    private Reply loanStep(UUID loan, String step, String body, String token) throws Exception {
        return post("/v1/library/loans/" + loan + ":" + step, body, token);
    }

    private static String token(UUID tenant, UUID user, String institution, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", "*".equals(institution) ? List.of(permissions) : List.of());
        claims.put("ins", List.of(institution));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private static String member(UUID tenant, UUID user, UUID person, String institution, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("pid", person.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", List.of());
        claims.put("ins", List.of(institution));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    @Test
    void aLateReturnIsFinedByTheCardTypeOnceAndTheCopyIsBackOnTheShelf() throws Exception {
        Library library = new Library();
        LocalDate issued = today().minusDays(30);
        UUID loan = library.loan(issued);
        String returnedOn = issued.plusDays(18).toString();

        Reply returned = loanStep(loan, "return", "{\"returnedOn\":\"" + returnedOn + "\",\"etag\":\"\\\"1\\\"\"}",
                library.librarian);
        assertThat(returned.status()).as(returned.text()).isEqualTo(200);
        assertThat(returned.body().path("status").asString()).isEqualTo("returned");
        assertThat(returned.body().path("fineAmount").asString()).as("4 days late at 2.50").isEqualTo("10.00");
        assertThat(library.value("SELECT status FROM library.media_unit WHERE id = (SELECT media_unit_id FROM"
                + " library.loan WHERE id = '" + loan + "')")).isEqualTo("available");

        Reply again = loanStep(loan, "return", "{\"returnedOn\":\"" + returnedOn + "\",\"etag\":\"\\\"1\\\"\"}",
                library.librarian);
        assertThat(again.status()).as("idempotent: " + again.text()).isEqualTo(200);
        assertThat(again.body().path("etag").asString()).isEqualTo(returned.body().path("etag").asString());
        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + issued.plusDays(19) + "\"}", library.librarian)
                .status()).as("returned on another day").isEqualTo(409);
        assertThat(library.value("SELECT count(*)::text FROM library.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'library.loan.return'")).isEqualTo("1");

        UUID onTime = library.loan(today().minusDays(3));
        assertThat(loanStep(onTime, "return", "{\"returnedOn\":\"" + today() + "\"}", library.librarian).body()
                .path("fineAmount").asString()).isEqualTo("0.00");
    }

    @Test
    void theReturnDateIsCheckedAndTheLibrarianNeedsTheLoanInReach() throws Exception {
        Library library = new Library();
        LocalDate issued = today().minusDays(5);
        UUID loan = library.loan(issued);

        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + issued.minusDays(1) + "\"}", library.librarian)
                .status()).isEqualTo(422);
        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + today().plusDays(1) + "\"}", library.librarian)
                .status()).isEqualTo(422);
        assertThat(loanStep(loan, "return", "{}", library.librarian).status()).isEqualTo(422);
        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + today() + "\",\"etag\":\"\\\"9\\\"\"}",
                library.librarian).status()).isEqualTo(412);
        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + today() + "\"}", library.student).status())
                .as("a borrower does not check in").isEqualTo(403);
        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + today() + "\"}",
                token(library.tenant, UUID.randomUUID(), UUID.randomUUID().toString(), RETURN)).status())
                .as("another school's librarian").isEqualTo(404);
        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + today() + "\"}",
                token(UUID.randomUUID(), UUID.randomUUID(), "*", RETURN)).status()).isEqualTo(404);
    }

    @Test
    void aMemberReadsOnlyTheirOwnLoansAndCardsAndStaffReadTheirSchools() throws Exception {
        Library library = new Library();
        UUID loan = library.loan(today().minusDays(2));
        UUID holder = UUID.fromString(library.value("SELECT person_id::text FROM library.library_card WHERE id = '"
                + library.card + "'"));
        String school = library.institution.toString();
        String[] reads = {"library.loan.read", "library.library_card.read"};
        String borrower = member(library.tenant, library.borrower, UUID.randomUUID(), school, reads);
        String byPerson = member(library.tenant, UUID.randomUUID(), holder, school, reads);
        String classmate = member(library.tenant, UUID.randomUUID(), UUID.randomUUID(), school, reads);
        String librarian = member(library.tenant, UUID.randomUUID(), UUID.randomUUID(), school, "library.loan.read",
                RETURN, "library.library_card.read", "library.library_card.update");
        String elsewhere = member(library.tenant, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID().toString(),
                "library.loan.read", RETURN, "library.library_card.read", "library.library_card.update");
        String loanPath = "/v1/library/loans/" + loan;
        String cardPath = "/v1/library/library-cards/" + library.card;

        assertThat(send("GET", loanPath, null, classmate, "X-Institution-Id", school).status())
                .as("another member's loan").isEqualTo(404);
        assertThat(ids(send("GET", "/v1/library/loans", null, classmate))).doesNotContain(loan.toString());
        assertThat(send("GET", cardPath, null, classmate).status()).as("another member's card").isEqualTo(404);
        assertThat(ids(send("GET", "/v1/library/library-cards", null, classmate)))
                .doesNotContain(library.card.toString());
        assertThat(send("GET", loanPath, null, borrower).status()).as("lent to the account").isEqualTo(200);
        assertThat(ids(send("GET", "/v1/library/loans", null, borrower))).containsExactly(loan.toString());
        assertThat(send("GET", loanPath, null, byPerson).status()).as("on the person's card").isEqualTo(200);
        assertThat(ids(send("GET", "/v1/library/loans", null, byPerson))).containsExactly(loan.toString());
        assertThat(send("GET", cardPath, null, byPerson).status()).as("the person's own card").isEqualTo(200);
        assertThat(ids(send("GET", "/v1/library/library-cards", null, byPerson)))
                .containsExactly(library.card.toString());
        assertThat(send("GET", loanPath, null, librarian).status()).as("staff of the school").isEqualTo(200);
        assertThat(ids(send("GET", "/v1/library/loans", null, librarian))).contains(loan.toString());
        assertThat(send("GET", cardPath, null, librarian).status()).isEqualTo(200);
        assertThat(send("GET", loanPath, null, elsewhere).status()).as("staff of another school").isEqualTo(404);
        assertThat(send("GET", cardPath, null, elsewhere).status()).isEqualTo(404);
    }

    @Test
    void theBorrowerDropsTheirOwnCopyForReturnAndNobodyElsesOutsideReach() throws Exception {
        Library library = new Library();
        UUID loan = library.loan(today().minusDays(2));

        String stranger = token(library.tenant, UUID.randomUUID(), UUID.randomUUID().toString(), REQUEST_RETURN);
        assertThat(loanStep(loan, "requestReturn", "{}", stranger).status()).isEqualTo(404);
        Reply dropped = loanStep(loan, "requestReturn", "{}", library.student);
        assertThat(dropped.status()).as(dropped.text()).isEqualTo(200);
        assertThat(dropped.body().path("status").asString()).isEqualTo("return_pending");
        assertThat(loanStep(loan, "requestReturn", "{}", library.student).status()).isEqualTo(409);
        Reply checkedIn = loanStep(loan, "return", "{\"returnedOn\":\"" + today() + "\"}", library.librarian);
        assertThat(checkedIn.body().path("status").asString()).as("a pending return is checked in")
                .isEqualTo("returned");

        UUID another = library.loan(today().minusDays(2));
        assertThat(loanStep(another, "requestReturn", "{}", library.librarian).status())
                .as("a librarian in reach drops it too").isEqualTo(200);
    }

    @Test
    void aLostCopyKeepsTheOverdueFineAndIsMarkedLost() throws Exception {
        Library library = new Library();
        UUID loan = library.loan(today().minusDays(20));

        Reply lost = loanStep(loan, "markLost", "{\"reason\":\"Left on the bus\"}", library.librarian);
        assertThat(lost.status()).as(lost.text()).isEqualTo(200);
        assertThat(lost.body().path("status").asString()).isEqualTo("lost");
        assertThat(lost.body().path("fineAmount").asString()).as("6 days overdue").isEqualTo("15.00");
        assertThat(library.value("SELECT status FROM library.media_unit WHERE id = (SELECT media_unit_id FROM"
                + " library.loan WHERE id = '" + loan + "')")).isEqualTo("lost");
        assertThat(library.value("SELECT payload FROM library.outbox_event WHERE tenant_id = :tenantId AND"
                + " event_type = 'audit.recorded' AND aggregate_id = '" + loan + "'")).contains("Left on the bus");
        assertThat(loanStep(loan, "markLost", "{}", library.librarian).status()).isEqualTo(409);
        assertThat(loanStep(loan, "return", "{\"returnedOn\":\"" + today() + "\"}", library.librarian).status())
                .isEqualTo(409);
        assertThat(loanStep(library.loan(today()), "markLost", "{\"reason\":\"" + "x".repeat(501) + "\"}",
                library.librarian).status()).isEqualTo(422);
        assertThat(loanStep(library.loan(today()), "markLost", "{}", library.student).status()).isEqualTo(403);
    }

    @Test
    void aRefusedReservationIsAskedForAgainByItsRequester() throws Exception {
        Library library = new Library();
        UUID reservation = UUID.randomUUID();
        library.sql("INSERT INTO library.reservation (id, tenant_id, media_id, requested_by_user_id, date_from,"
                + " date_to, status, row_version) VALUES ('" + reservation + "', :tenantId, '" + library.media + "', '"
                + library.borrower + "', '" + today() + "', '" + today().plusDays(10) + "', 'rejected', 1)");
        String path = "/v1/library/reservations/" + reservation + ":requestAgain";

        assertThat(post(path, "{}", token(library.tenant, UUID.randomUUID(), UUID.randomUUID().toString(), AGAIN))
                .status()).as("someone else's request outside reach").isEqualTo(404);
        Reply again = post(path, "{\"etag\":\"\\\"1\\\"\"}", library.student);
        assertThat(again.status()).as(again.text()).isEqualTo(200);
        assertThat(again.body().path("status").asString()).isEqualTo("requested");
        assertThat(post(path, "{}", library.student).status()).as("already requested").isEqualTo(409);

        UUID stale = UUID.randomUUID();
        UUID otherTitle = UUID.randomUUID();
        library.sql("INSERT INTO library.media (id, tenant_id, institution_id, title, row_version) VALUES ('"
                + otherTitle + "', :tenantId, '" + library.institution + "', 'Another title', 1)");
        library.sql("INSERT INTO library.reservation (id, tenant_id, media_id, requested_by_user_id, date_from,"
                + " date_to, status, row_version) VALUES ('" + stale + "', :tenantId, '" + otherTitle + "', '"
                + library.borrower + "', '2025-01-01', '2025-01-05', 'cancelled', 1)");
        assertThat(post("/v1/library/reservations/" + stale + ":requestAgain", "{}", library.student).status())
                .as("the wanted dates have passed").isEqualTo(422);

        UUID duplicate = UUID.randomUUID();
        library.sql("INSERT INTO library.reservation (id, tenant_id, media_id, requested_by_user_id, date_from,"
                + " date_to, status, row_version) VALUES ('" + duplicate + "', :tenantId, '" + library.media + "', '"
                + library.borrower + "', '" + today() + "', '" + today().plusDays(10) + "', 'cancelled', 1)");
        assertThat(post("/v1/library/reservations/" + duplicate + ":requestAgain", "{}", library.librarian).status())
                .as("the requester already has an open request for the title").isEqualTo(409);
    }

    @Test
    void aReservationIsTheRequestersOwnUnlessLibraryStaffReachIt() throws Exception {
        Library library = new Library();
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID somebody = UUID.randomUUID();
        String school = library.institution.toString();
        String mine = token(library.tenant, me, school, RESERVATION_MEMBER);
        String theirs = token(library.tenant, other, school, RESERVATION_MEMBER);
        String staff = token(library.tenant, UUID.randomUUID(), school, "library.reservation.read",
                "library.reservation.create", "library.reservation.accept_reservation");
        String body = "{\"mediaId\":\"" + library.media + "\",\"requestedByUserId\":\"" + other
                + "\",\"requestedByPersonId\":\"" + somebody + "\",\"dateFrom\":\"" + today()
                + "\",\"dateTo\":\"" + today().plusDays(5) + "\"}";

        Reply created = send("POST", "/v1/library/reservations", body, mine);
        assertThat(created.status()).as(created.text()).isEqualTo(201);
        assertThat(created.body().path("requestedByUserId").asString()).as("the requester comes from the token")
                .isEqualTo(me.toString());
        assertThat(created.body().has("requestedByPersonId")).as("a member reserves for no one else").isFalse();
        String id = created.body().path("id").asString();
        assertThat(library.value("SELECT requested_by_user_id::text FROM library.reservation WHERE id = '" + id + "'"))
                .isEqualTo(me.toString());
        String item = "/v1/library/reservations/" + id;

        assertThat(send("GET", item, null, theirs).status()).as("another member's reservation").isEqualTo(404);
        assertThat(ids(send("GET", "/v1/library/reservations?size=500", null, theirs))).doesNotContain(id);
        assertThat(send("PATCH", item, "{\"reference\":\"Mine\"}", theirs, "If-Match", "\"1\"").status())
                .isEqualTo(404);
        assertThat(send("DELETE", item, null, theirs, "If-Match", "\"1\"").status()).isEqualTo(404);
        assertThat(ids(send("GET", "/v1/library/reservations?size=500", null, mine))).containsExactly(id);
        assertThat(send("GET", item, null, staff).status()).as("library staff of the school").isEqualTo(200);
        assertThat(ids(send("GET", "/v1/library/reservations?size=500", null, staff))).contains(id);

        assertThat(send("PATCH", item + "?updateMask=requestedByUserId", "{}", mine, "If-Match", "\"1\"").status())
                .as("the requester is not writable").isEqualTo(400);
        Reply edited = send("PATCH", item, "{\"reference\":\"R-1\",\"requestedByPersonId\":\"" + somebody + "\"}",
                mine, "If-Match", "\"1\"");
        assertThat(edited.status()).as(edited.text()).isEqualTo(200);
        assertThat(edited.body().path("requestedByUserId").asString()).isEqualTo(me.toString());
        assertThat(edited.body().has("requestedByPersonId")).isFalse();

        Reply forSomebody = send("POST", "/v1/library/reservations", body, staff);
        assertThat(forSomebody.status()).as(forSomebody.text()).isEqualTo(201);
        assertThat(forSomebody.body().path("requestedByPersonId").asString()).as("staff reserve for a person")
                .isEqualTo(somebody.toString());
        assertThat(forSomebody.body().path("requestedByUserId").asString()).isNotEqualTo(other.toString());

        assertThat(send("DELETE", item, null, mine, "If-Match", "\"2\"").status()).isEqualTo(204);
    }

    @Test
    void aFineIsInvoicedOnlyOnceTheCopyIsBackOrLostAndThroughFinance() throws Exception {
        Library library = new Library();
        String invoice = "invoiceFine";
        UUID out = library.loan(today().minusDays(30));
        assertThat(loanStep(out, invoice, "{}", library.librarian).status()).as("the copy is still out")
                .isEqualTo(409);
        UUID onTime = library.loan(today().minusDays(2));
        loanStep(onTime, "return", "{\"returnedOn\":\"" + today() + "\"}", library.librarian);
        assertThat(loanStep(onTime, invoice, "{}", library.librarian).status()).as("no fine").isEqualTo(409);
        assertThat(loanStep(out, invoice, "{}", library.student).status()).isEqualTo(403);
        assertThat(loanStep(out, invoice, "{}", token(library.tenant, UUID.randomUUID(),
                UUID.randomUUID().toString(), "library.loan.invoice_loan_fine")).status()).isEqualTo(404);

        loanStep(out, "markLost", "{}", library.librarian);
        Reply unavailable = loanStep(out, invoice, "{}", token(library.tenant, UUID.randomUUID(),
                library.institution.toString(), "library.loan.invoice_loan_fine"));
        assertThat(unavailable.status()).as("people and finance do not run here: " + unavailable.text())
                .isEqualTo(503);
        assertThat(unavailable.text()).doesNotContain("people.v1").doesNotContain("finance.v1");
        assertThat(library.value("SELECT coalesce(invoice_id::text, 'none') FROM library.loan WHERE id = '" + out
                + "'")).isEqualTo("none");
    }
}
