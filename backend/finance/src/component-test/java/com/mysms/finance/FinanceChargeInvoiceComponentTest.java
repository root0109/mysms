package com.mysms.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.events.StaffRecordSync;
import com.mysms.finance.events.StudentRecordSync;
import com.mysms.messaging.events.EventEnvelope;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * finance.RaiseChargeInvoice over HTTP against real PostgreSQL as the application role: a one-line draft for a charge
 * another module computed, one per source, inside the caller's institutions. Each test works in a tenant of its own;
 * org does not run here, so a charge without a named currency is 503.
 */
@SpringBootTest(classes = FinanceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FinanceChargeInvoiceComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String CREATE = "finance.invoice.create";
    private static final String PATH = "/v1/finance/invoices:raiseCharge";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(FinanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private StudentRecordSync studentRecords;

    @Autowired
    private StaffRecordSync staffRecords;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "finance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of its own tenant and a librarian who may raise its invoices. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID currency = UUID.randomUUID();
        final UUID student = UUID.randomUUID();
        final String librarian = token(tenant, institution.toString(), CREATE, "finance.invoice.read");

        School() {
            sql("INSERT INTO finance.student_record (tenant_id, student_id, person_id, institution_id, source_version,"
                    + " synced_at) VALUES (:tenantId, '" + student + "', '" + UUID.randomUUID() + "', '" + institution
                    + "', 1, now())");
        }

        String charge(UUID source, String amount) {
            return "{\"institutionId\":\"" + institution + "\",\"studentId\":\"" + student
                    + "\",\"billToName\":\" A borrower \",\"description\":\"Library fine: 4 days late\",\"amount\":\""
                    + amount + "\",\"currencyId\":\"" + currency + "\",\"dueDate\":\"2026-12-31\",\"sourceType\":"
                    + "\"library.loan\",\"sourceId\":\"" + source + "\"}";
        }

        List<String> column(String query) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(query).query(String.class)
                            .list()));
        }

        void sql(String statement) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).update()));
        }
    }

    private Reply raise(String body, String token) throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                .POST(HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token).build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static String token(UUID tenant, String institution, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", "*".equals(institution) ? List.of(permissions) : List.of());
        claims.put("ins", List.of(institution));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    @Test
    void aChargeBecomesAOneLineDraftOncePerSource() throws Exception {
        School school = new School();
        UUID loan = UUID.randomUUID();

        Reply raised = raise(school.charge(loan, "10.00"), school.librarian);
        assertThat(raised.status()).as(raised.text()).isEqualTo(200);
        JsonNode invoice = raised.body();
        assertThat(invoice.path("status").asString()).isEqualTo("draft");
        assertThat(invoice.path("total").asString()).isEqualTo("10.00");
        assertThat(invoice.path("subtotal").asString()).isEqualTo("10.00");
        assertThat(invoice.path("amountPaid").asString()).isEqualTo("0.00");
        assertThat(invoice.path("billToName").asString()).isEqualTo("A borrower");
        assertThat(invoice.path("currencyId").asString()).isEqualTo(school.currency.toString());
        assertThat(invoice.path("invoiceNumber").asString()).startsWith("DRAFT-");
        String id = invoice.path("id").asString();
        assertThat(school.column("SELECT description || '|' || unit_price || '|' || line_total FROM"
                + " finance.invoice_line WHERE tenant_id = :tenantId AND invoice_id = '" + id + "'"))
                .containsExactly("Library fine: 4 days late|10.00|10.00");

        Reply again = raise(school.charge(loan, "10.00"), school.librarian);
        assertThat(again.body().path("id").asString()).as("the same source answers its invoice").isEqualTo(id);
        assertThat(school.column("SELECT count(*)::text FROM finance.invoice WHERE tenant_id = :tenantId"))
                .containsExactly("1");
        Reply posted = raise(school.charge(UUID.randomUUID(), "7.25"), school.librarian);
        assertThat(posted.body().path("id").asString()).as("another source, another invoice").isNotEqualTo(id);

        School other = new School();
        Reply sameSourceElsewhere = raise(other.charge(loan, "10.00"), other.librarian);
        assertThat(sameSourceElsewhere.status()).as("ids are per tenant").isEqualTo(200);
        assertThat(sameSourceElsewhere.body().path("id").asString()).isNotEqualTo(id);
    }

    @Test
    void onlyTheInstitutionsStudentsAndTheirOrItsStaffsAccountsAreBilled() throws Exception {
        School school = new School();
        School other = new School();
        UUID staffAccount = UUID.randomUUID();
        school.sql("INSERT INTO finance.staff_record (tenant_id, staff_id, user_id, institution_id, source_version,"
                + " synced_at) VALUES (:tenantId, '" + UUID.randomUUID() + "', '" + staffAccount + "', '"
                + school.institution + "', 1, now())");
        String unknown = school.charge(UUID.randomUUID(), "5").replace(school.student.toString(),
                UUID.randomUUID().toString());
        Reply stranger = raise(unknown, school.librarian);
        assertThat(stranger.status()).as("a student finance does not know: " + stranger.text()).isEqualTo(422);
        assertThat(stranger.body().path("errors").get(0).path("field").asString()).isEqualTo("studentId");
        school.sql("INSERT INTO finance.student_record (tenant_id, student_id, person_id, institution_id,"
                + " source_version, synced_at) VALUES (:tenantId, '" + other.student + "', '" + UUID.randomUUID()
                + "', '" + other.institution + "', 1, now())");
        Reply elsewhere = raise(school.charge(UUID.randomUUID(), "5").replace(school.student.toString(),
                other.student.toString()), school.librarian);
        assertThat(elsewhere.status()).as("another school's student").isEqualTo(422);

        String toAccount = school.charge(UUID.randomUUID(), "5").replaceAll("\"studentId\":\"[^\"]+\"",
                "\"billToUserId\":\"" + staffAccount + "\"");
        Reply staff = raise(toAccount, school.librarian);
        assertThat(staff.status()).as("an account of the school's staff: " + staff.text()).isEqualTo(200);
        assertThat(staff.body().path("billToUserId").asString()).isEqualTo(staffAccount.toString());
        Reply anyone = raise(toAccount.replace(staffAccount.toString(), UUID.randomUUID().toString()),
                school.librarian);
        assertThat(anyone.status()).isEqualTo(422);
        assertThat(anyone.body().path("errors").get(0).path("field").asString()).isEqualTo("billToUserId");
        assertThat(school.column("SELECT count(*)::text FROM finance.invoice WHERE tenant_id = :tenantId"))
                .containsExactly("1");
    }

    @Test
    void theBilledPartiesAreKnownFromSisAndHrEvents() throws Exception {
        School school = new School();
        UUID student = UUID.randomUUID();
        UUID studentAccount = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        UUID staffAccount = UUID.randomUUID();
        assertThat(deliver(school, studentRecords::apply, "sis", "sis.student", "created", student, "{\"id\":\""
                + student + "\",\"userId\":\"" + studentAccount + "\",\"personId\":\"" + UUID.randomUUID()
                + "\",\"institutionId\":\"" + school.institution + "\",\"rowVersion\":1}")).isTrue();
        String staffEvent = "{\"id\":\"" + staff + "\",\"userId\":\"" + staffAccount + "\",\"personId\":\""
                + UUID.randomUUID() + "\",\"institutionId\":\"" + school.institution + "\",\"rowVersion\":";
        assertThat(deliver(school, staffRecords::apply, "hr", "hr.staff", "created", staff, staffEvent + "1}"))
                .isTrue();
        String byStudent = school.charge(UUID.randomUUID(), "3").replace(school.student.toString(), student.toString());
        assertThat(raise(byStudent, school.librarian).status()).as("the student sis announced").isEqualTo(200);
        String byAccount = school.charge(UUID.randomUUID(), "3").replaceAll("\"studentId\":\"[^\"]+\"",
                "\"billToUserId\":\"" + studentAccount + "\"");
        assertThat(raise(byAccount, school.librarian).status()).as("the student's own account").isEqualTo(200);

        assertThat(deliver(school, staffRecords::apply, "hr", "hr.staff", "deleted", staff, staffEvent + "2}"))
                .isTrue();
        assertThat(deliver(school, staffRecords::apply, "hr", "hr.staff", "updated", staff, staffEvent + "1}"))
                .as("a late event changes nothing").isFalse();
        String toStaff = school.charge(UUID.randomUUID(), "3").replaceAll("\"studentId\":\"[^\"]+\"",
                "\"billToUserId\":\"" + staffAccount + "\"");
        assertThat(raise(toStaff, school.librarian).status()).as("a deleted staff record bills nobody")
                .isEqualTo(422);
    }

    private boolean deliver(School school, java.util.function.Function<EventEnvelope, Boolean> handler,
            String module, String aggregate, String verb, UUID id, String data) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(school.tenant, null),
                () -> TestTransactions.of(database).write(() -> handler.apply(new EventEnvelope(UUID.randomUUID(),
                        module, aggregate + "." + verb, Optional.of(school.tenant), aggregate, id, Instant.now(), data,
                        Optional.empty()))));
    }

    @Test
    void aSourceAnswersItsInvoiceOnlyWhileItIsLiveAndTheSameCharge() throws Exception {
        School school = new School();
        UUID loan = UUID.randomUUID();
        String id = raise(school.charge(loan, "10.00"), school.librarian).body().path("id").asString();

        Reply changed = raise(school.charge(loan, "12.50"), school.librarian);
        assertThat(changed.status()).as("a changed fine is not silently kept: " + changed.text()).isEqualTo(409);
        school.sql("UPDATE finance.invoice SET status = 'cancelled' WHERE id = '" + id + "'");
        Reply cancelled = raise(school.charge(loan, "10.00"), school.librarian);
        assertThat(cancelled.status()).as("nor a cancelled invoice answered as raised").isEqualTo(409);
        school.sql("UPDATE finance.invoice SET status = 'draft', deleted_at = now() WHERE id = '" + id + "'");
        assertThat(raise(school.charge(loan, "10.00"), school.librarian).status()).as("nor a deleted one")
                .isEqualTo(409);
    }

    @Test
    void raisesAtTheSameTimeEndWithOneInvoiceForBoth() throws Exception {
        School school = new School();
        UUID loan = UUID.randomUUID();
        int raises = 6;
        ExecutorService pool = Executors.newFixedThreadPool(raises);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Reply>> replies = new ArrayList<>();
            for (int i = 0; i < raises; i++) {
                replies.add(pool.submit(() -> {
                    start.await();
                    return raise(school.charge(loan, "10.00"), school.librarian);
                }));
            }
            start.countDown();
            Set<String> ids = new HashSet<>();
            for (Future<Reply> reply : replies) {
                Reply done = reply.get(60, TimeUnit.SECONDS);
                assertThat(done.status()).as(done.text()).isEqualTo(200);
                ids.add(done.body().path("id").asString());
            }
            assertThat(ids).as("every raise answers the one invoice").hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(school.column("SELECT count(*)::text FROM finance.invoice_line WHERE tenant_id = :tenantId"))
                .containsExactly("1");
    }

    @Test
    void theChargeIsCheckedAndRaisedOnlyInsideTheCallersInstitutions() throws Exception {
        School school = new School();
        UUID loan = UUID.randomUUID();

        assertThat(raise(school.charge(loan, "0"), school.librarian).status()).isEqualTo(422);
        assertThat(raise(school.charge(loan, "1.005"), school.librarian).status()).isEqualTo(422);
        assertThat(raise(school.charge(loan, "-3"), school.librarian).status()).isEqualTo(422);
        Reply unbilled = raise(school.charge(loan, "5").replaceAll("\"studentId\":\"[^\"]+\",", ""),
                school.librarian);
        assertThat(unbilled.status()).isEqualTo(422);
        assertThat(unbilled.body().path("errors").get(0).path("field").asString()).isEqualTo("studentId");
        assertThat(raise(school.charge(loan, "5").replace("library.loan", "Library Loan"), school.librarian).status())
                .isEqualTo(422);
        assertThat(raise(school.charge(loan, "5"), token(school.tenant, school.institution.toString(),
                "finance.invoice.read")).status()).isEqualTo(403);
        assertThat(raise(school.charge(loan, "5"), token(school.tenant, UUID.randomUUID().toString(), CREATE))
                .status()).as("another school's office").isEqualTo(403);
        Reply withoutCurrency = raise(school.charge(loan, "5").replaceAll(",\"currencyId\":\"[^\"]+\"", ""),
                school.librarian);
        assertThat(withoutCurrency.status()).as("the institution's currency comes from org: " + withoutCurrency.text())
                .isEqualTo(503);
        assertThat(school.column("SELECT count(*)::text FROM finance.invoice WHERE tenant_id = :tenantId"))
                .containsExactly("0");
    }
}
