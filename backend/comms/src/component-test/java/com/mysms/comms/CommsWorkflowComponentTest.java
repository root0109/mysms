package com.mysms.comms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.events.GreetingRequestHandler;
import com.mysms.comms.events.GuardianLinkSync;
import com.mysms.comms.events.StaffRecordSync;
import com.mysms.comms.events.StudentRecordSync;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
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
 * comms' workflow operations over HTTP against real PostgreSQL, a real SMTP server (GreenMail) and real signed
 * tokens: each answers for the caller's own rows only (another user's row is 404 whatever their grants), outside the
 * caller's institutions is 404, a malformed request 400 or 422, a stale body etag 412, a state the step does not
 * start from 409. iam and the platform do not run in a module test, so what needs them answers 503 and writes nothing.
 */
@SpringBootTest(classes = CommsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommsWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final GreenMail SMTP = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SCHOOL = UUID.fromString("0196f000-0000-7000-8000-00000000a001");
    private static final UUID OTHER_SCHOOL = UUID.fromString("0196f000-0000-7000-8000-00000000a002");
    private static final String YEAR = "0196f000-0000-7000-8000-00000000e001";
    private static final String STALE = "\"99\"";
    private static final String STALE_BODY = "{\"etag\":\"\\\"99\\\"\"}";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(CommsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private GreetingRequestHandler greetings;

    @Autowired
    private StudentRecordSync studentRecords;

    @Autowired
    private StaffRecordSync staffRecords;

    @Autowired
    private GuardianLinkSync guardianLinks;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SMTP.start();
        DATABASE.register(registry, "comms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.comms.smtp.host", () -> "127.0.0.1");
        registry.add("mysms.comms.smtp.port", () -> SMTP.getSmtp().getPort());
        registry.add("mysms.comms.smtp.from", () -> "school@mysms.test");
        registry.add("mysms.comms.smtp.starttls-required", () -> "false");
    }

    @AfterAll
    static void stopSmtp() {
        SMTP.stop();
    }

    // ---- callers, calls and rows

    private record Reply(int status, JsonNode body) {

        String code() {
            return body.path("code").asString();
        }

        List<String> ids(String list) {
            List<String> ids = new ArrayList<>();
            body.path(list).forEach(item -> ids.add(item.path("id").asString()));
            return ids;
        }
    }

    /** A user of the test tenant with comms' permissions in the school, or tenant-wide in every school. */
    private static String user(UUID user, boolean tenantWide) {
        return token(user, List.of("comms.*"), List.of(tenantWide ? "*" : SCHOOL.toString()), tenantWide);
    }

    private static String token(UUID user, List<String> permissions, List<String> institutions, boolean tenantWide) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", TENANT.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", tenantWide ? permissions : List.of());
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        request.header("Authorization", "Bearer " + token);
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isEmpty() ? JSON.nullNode()
                : JSON.readTree(response.body()));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Runs statements in the test tenant; {@code :tenant} is its id. */
    private void sql(String... statements) {
        write(() -> {
            for (String statement : statements) {
                database.jdbc().sql(statement).param("tenant", TENANT).update();
            }
            return null;
        });
    }

    private <T> T write(Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(TENANT, null),
                () -> TestTransactions.of(database).write(work::get));
    }

    private String value(String query) {
        return write(() -> database.jdbc().sql(query).query(String.class).optional().orElse(null));
    }

    private int count(String query) {
        return write(() -> database.jdbc().sql(query).query(Integer.class).single());
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    // ---- the in-app inbox

    private String notification(UUID recipient, String extra) {
        String id = id();
        sql(("INSERT INTO comms.in_app_notification (id, tenant_id, recipient_user_id, notification_type_id, title,"
                + " created_at, archived_at) VALUES ('%s', :tenant, '%s', '0196f0c0-6000-7000-8000-000000000001',"
                + " 'Hello', now() - interval '1 hour', %s)").formatted(id, recipient, extra));
        return id;
    }

    private Reply inbox(String token) throws Exception {
        Instant now = Instant.now();
        String range = "createTime >= \"" + now.minusSeconds(86_400) + "\" AND createTime <= \"" + now.plusSeconds(60)
                + "\"";
        return call("GET", "/v1/comms/notifications?filter=" + encode(range), token, null);
    }

    @Test
    void theInboxIsTheCallersOwn() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String mine = notification(me, "NULL");
        String second = notification(me, "NULL");
        String archived = notification(me, "now()");
        String theirs = notification(other, "NULL");
        String token = user(me, true);

        Reply unbounded = call("GET", "/v1/comms/notifications", token, null);
        assertThat(unbounded.status()).as("a partitioned list needs its range").isEqualTo(400);
        assertThat(unbounded.code()).isEqualTo("INVALID_FILTER");
        Reply listed = inbox(token);
        assertThat(listed.status()).as(listed.body().toString()).isEqualTo(200);
        assertThat(listed.ids("data")).containsExactlyInAnyOrder(mine, second);
        assertThat(call("GET", "/v1/comms/notifications/" + archived, token, null).status())
                .as("an archived notification is still the owner's").isEqualTo(200);
        assertThat(call("GET", "/v1/comms/notifications/" + theirs, token, null).status())
                .as("another account's, even for a tenant administrator").isEqualTo(404);

        Reply read = call("POST", "/v1/comms/notifications/" + mine + ":markRead", token, "{}");
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        assertThat(read.body().path("readAt").isMissingNode()).isFalse();
        assertThat(call("POST", "/v1/comms/notifications/" + mine + ":markRead", token, "{}").body().path("readAt"))
                .as("reading again keeps the first time").isEqualTo(read.body().path("readAt"));
        assertThat(call("POST", "/v1/comms/notifications/" + theirs + ":markRead", token, "{}").status()).isEqualTo(404);

        assertThat(call("POST", "/v1/comms/notifications:markAllRead", token, "{}").status()).isEqualTo(204);
        assertThat(value("SELECT count(*) FILTER (WHERE read_at IS NULL) || '/' || count(*) FROM"
                + " comms.in_app_notification WHERE recipient_user_id IN ('" + me + "', '" + other + "')"))
                .as("only the caller's inbox is marked (the archived one stays unread)").isEqualTo("2/4");

        assertThat(call("POST", "/v1/comms/notifications:archive", token,
                "{\"notificationIds\":[\"" + second + "\",\"" + theirs + "\"]}").status()).isEqualTo(204);
        assertThat(inbox(token).ids("data")).containsExactly(mine);
        assertThat(value("SELECT (archived_at IS NULL)::text FROM comms.in_app_notification WHERE id = '" + theirs + "'"))
                .as("another account's notification is not archived").isEqualTo("true");
        assertThat(call("POST", "/v1/comms/notifications:archive", token, "{}").status()).isEqualTo(204);
        assertThat(inbox(token).ids("data")).as("without ids every read one is archived").isEmpty();
        assertThat(call("POST", "/v1/comms/notifications:archive", token, "{\"notificationIds\":[\"x\"]}").status())
                .isIn(400, 422);
    }

    // ---- posts, the feed and reactions

    /** A class page of the school; members are added with {@link #member}. */
    private String page(String school) {
        String category = id();
        String page = id();
        sql("INSERT INTO comms.page_category (id, tenant_id, institution_id, name, code, row_version) VALUES ('"
                        + category + "', :tenant, '" + school + "', 'Classes', 'C" + category.substring(0, 8) + "', 1)",
                "INSERT INTO comms.class_page (id, tenant_id, institution_id, page_category_id, name, description,"
                        + " row_version) VALUES ('" + page + "', :tenant, '" + school + "', '" + category
                        + "', 'Class 5A', 'The class page', 1)");
        return page;
    }

    private String member(String page, UUID user) {
        String id = id();
        sql("INSERT INTO comms.class_page_member (id, tenant_id, class_page_id, user_id, row_version) VALUES ('" + id
                + "', :tenant, '" + page + "', '" + user + "', 1)");
        return id;
    }

    /** A post of the school by the author: on the page when one is named, school-wide otherwise. */
    private String post(UUID author, String page, String status, String school) {
        String id = id();
        sql("INSERT INTO comms.post (id, tenant_id, institution_id, academic_year_id, class_page_id, author_user_id,"
                + " body, status, posted_at, row_version) VALUES ('" + id + "', :tenant, '" + school + "', '" + YEAR
                + "', " + (page == null ? "NULL" : "'" + page + "'") + ", '" + author + "', 'News', '" + status
                + "', CASE WHEN '" + status + "' = 'posted' THEN now() END, 1)");
        return id;
    }

    private String comment(String post, UUID author) {
        String id = id();
        sql("INSERT INTO comms.post_comment (id, tenant_id, post_id, author_user_id, body, row_version) VALUES ('" + id
                + "', :tenant, '" + post + "', '" + author + "', 'Well done', 1)");
        return id;
    }

    @Test
    void theFeedAndEveryPostReadKeepAClassPagesPostsOnIt() throws Exception {
        UUID me = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        String token = user(me, true);
        String ours = page(SCHOOL.toString());
        String theirs = page(SCHOOL.toString());
        member(ours, me);
        member(ours, teacher);
        member(theirs, teacher);
        String onOurs = post(teacher, ours, "posted", SCHOOL.toString());
        String onTheirs = post(teacher, theirs, "posted", SCHOOL.toString());
        String schoolWide = post(teacher, null, "posted", SCHOOL.toString());
        String draft = post(teacher, null, "draft", SCHOOL.toString());
        String elsewhere = post(teacher, null, "posted", OTHER_SCHOOL.toString());
        String myDraft = post(me, ours, "draft", SCHOOL.toString());

        Reply feed = call("GET", "/v1/comms/posts:feed", user(me, false), null);
        assertThat(feed.status()).as(feed.body().toString()).isEqualTo(200);
        assertThat(feed.ids("data")).containsExactlyInAnyOrder(onOurs, schoolWide)
                .doesNotContain(onTheirs, draft, elsewhere, myDraft);
        assertThat(call("GET", "/v1/comms/posts:feed?classPageId=" + ours, token, null).ids("data"))
                .containsExactly(onOurs);
        assertThat(call("GET", "/v1/comms/posts:feed?classPageId=" + theirs, token, null).ids("data")).isEmpty();
        assertThat(call("GET", "/v1/comms/posts:feed?classSectionId=" + id(), token, null).ids("data"))
                .as("comms does not know who is in which class: closed").isEmpty();

        // the leak this release closes: GetPost, ListPosts and the comments keep a class page's posts on it
        assertThat(call("GET", "/v1/comms/posts/" + onTheirs, token, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/comms/posts/" + onOurs, token, null).status()).isEqualTo(200);
        assertThat(call("GET", "/v1/comms/posts/" + onTheirs, user(teacher, false), null).status()).isEqualTo(200);
        assertThat(call("GET", "/v1/comms/posts?size=500", token, null).ids("data"))
                .contains(onOurs, schoolWide, draft, myDraft).doesNotContain(onTheirs);
        String note = comment(onTheirs, teacher);
        assertThat(call("GET", "/v1/comms/posts/" + onTheirs + "/comments", token, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/comms/posts/" + onTheirs + "/comments/" + note, token, null).status())
                .isEqualTo(404);
        assertThat(call("GET", "/v1/comms/posts/" + onTheirs + "/comments", user(teacher, false), null).ids("data"))
                .containsExactly(note);

        Reply saved = call("POST", "/v1/comms/posts/" + schoolWide + "/reactions", token, "{\"isSaved\":true}");
        assertThat(saved.status()).as(saved.body().toString()).isEqualTo(201);
        assertThat(call("GET", "/v1/comms/posts:feed?savedOnly=true", token, null).ids("data"))
                .containsExactly(schoolWide);
        assertThat(call("GET", "/v1/comms/posts:feed?savedOnly=true", user(teacher, false), null).ids("data"))
                .as("a bookmark is the caller's own").isEmpty();
    }

    @Test
    void onlyTheAuthorSubmitsTheirDraft() throws Exception {
        UUID me = UUID.randomUUID();
        String page = page(SCHOOL.toString());
        member(page, me);
        String draft = post(me, page, "draft", SCHOOL.toString());
        String token = user(me, false);

        assertThat(call("POST", "/v1/comms/posts/" + draft + ":submit", user(UUID.randomUUID(), true), "{}").status())
                .as("another user's draft").isEqualTo(404);
        assertThat(call("POST", "/v1/comms/posts/" + draft + ":submit", token, STALE_BODY).status())
                .isEqualTo(412);
        Reply submitted = call("POST", "/v1/comms/posts/" + draft + ":submit", token, "{\"etag\":\"\\\"1\\\"\"}");
        assertThat(submitted.status()).as(submitted.body().toString()).isEqualTo(200);
        assertThat(submitted.body().path("status").asString()).isEqualTo("pending");
        Reply again = call("POST", "/v1/comms/posts/" + draft + ":submit", token, "{}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("INVALID_STATE_TRANSITION");

        String left = post(me, page, "draft", SCHOOL.toString());
        sql("UPDATE comms.class_page_member SET is_active = false WHERE user_id = '" + me + "'");
        assertThat(call("POST", "/v1/comms/posts/" + left + ":submit", token, "{}").status())
                .as("an author who left the page no longer posts to it").isEqualTo(403);
        String outside = post(me, null, "draft", OTHER_SCHOOL.toString());
        assertThat(call("POST", "/v1/comms/posts/" + outside + ":submit", token, "{}").status()).isEqualTo(404);
    }

    @Test
    void aReactionIsTheCallersOwnAndOnlyOnWhatTheyMayRead() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String token = user(me, false);
        String admin = user(other, true);
        String page = page(SCHOOL.toString());
        member(page, me);
        String readable = post(other, page, "posted", SCHOOL.toString());
        String hidden = post(other, page(SCHOOL.toString()), "posted", SCHOOL.toString());
        String base = "/v1/comms/posts/" + readable + "/reactions";

        Reply created = call("POST", base, token, "{\"reaction\":\"like\",\"userId\":\"" + other + "\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        assertThat(created.body().path("userId").asString()).as("the user comes from the token").isEqualTo(me.toString());
        String reaction = created.body().path("id").asString();
        assertThat(call("POST", base, token, "{\"reaction\":\"like\"}").status()).isEqualTo(409);
        assertThat(call("POST", "/v1/comms/posts/" + hidden + "/reactions", token, "{\"reaction\":\"like\"}").status())
                .as("a post of a page the caller is not on").isEqualTo(404);
        assertThat(call("POST", base, token, "{\"reaction\":\"love\"}").status()).isIn(400, 422);
        assertThat(call("POST", base + "?validateOnly=true", user(UUID.randomUUID(), true), "{\"reaction\":\"like\"}")
                .status())
                .as("a non-member does not reach the page's post").isEqualTo(404);

        assertThat(call("GET", base + "/" + reaction, token, null).status()).isEqualTo(200);
        assertThat(call("GET", base + "/" + reaction, admin, null).status()).as("another user's").isEqualTo(404);
        assertThat(call("GET", base, token, null).ids("data")).containsExactly(reaction);
        assertThat(call("GET", "/v1/comms/posts/" + hidden + "/reactions", token, null).status()).isEqualTo(404);

        assertThat(call("PATCH", base + "/" + reaction, token, "{\"reaction\":\"unlike\"}").status()).isEqualTo(428);
        assertThat(call("PATCH", base + "/" + reaction, token, "{\"reaction\":\"unlike\"}", "If-Match", STALE).status())
                .isEqualTo(412);
        assertThat(call("PATCH", base + "/" + reaction, admin, "{\"reaction\":\"unlike\"}", "If-Match", "\"1\"")
                .status()).isEqualTo(404);
        Reply changed = call("PATCH", base + "/" + reaction, token, "{\"reaction\":\"unlike\",\"isSaved\":true}",
                "If-Match", "\"1\"");
        assertThat(changed.status()).as(changed.body().toString()).isEqualTo(200);
        assertThat(changed.body().path("reaction").asString()).isEqualTo("unlike");
        assertThat(changed.body().path("isSaved").asBoolean()).isTrue();
        assertThat(call("DELETE", base + "/" + reaction, admin, null, "If-Match", "\"2\"").status()).isEqualTo(404);
        assertThat(call("DELETE", base + "/" + reaction, token, null, "If-Match", "\"2\"").status()).isEqualTo(204);
        assertThat(call("GET", base + "/" + reaction, token, null).status()).isEqualTo(404);

        String note = comment(readable, other);
        String hiddenNote = comment(hidden, other);
        Reply liked = call("POST", "/v1/comms/comment-reactions", token, "{\"commentId\":\"" + note
                + "\",\"reaction\":\"like\"}");
        assertThat(liked.status()).as(liked.body().toString()).isEqualTo(201);
        assertThat(liked.body().path("userId").asString()).isEqualTo(me.toString());
        String onComment = liked.body().path("id").asString();
        assertThat(call("POST", "/v1/comms/comment-reactions", token, "{\"commentId\":\"" + note
                + "\",\"reaction\":\"like\"}").status()).isEqualTo(409);
        assertThat(call("POST", "/v1/comms/comment-reactions", token, "{\"commentId\":\"" + hiddenNote
                + "\",\"reaction\":\"like\"}").status()).isEqualTo(404);
        assertThat(call("GET", "/v1/comms/comment-reactions", token, null).ids("data")).containsExactly(onComment);
        assertThat(call("GET", "/v1/comms/comment-reactions", admin, null).ids("data")).isEmpty();
        assertThat(call("GET", "/v1/comms/comment-reactions/" + onComment, admin, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/comms/comment-reactions/" + reaction, token, null).status())
                .as("a reaction to a post is not a comment reaction").isEqualTo(404);
        assertThat(call("PATCH", "/v1/comms/comment-reactions/" + onComment, token, "{\"isActive\":false}",
                "If-Match", STALE).status()).isEqualTo(412);
        assertThat(call("PATCH", "/v1/comms/comment-reactions/" + onComment, token, "{\"isActive\":false}",
                "If-Match", "\"1\"").body().path("isActive").asBoolean()).isFalse();
        assertThat(call("DELETE", "/v1/comms/comment-reactions/" + onComment, token, null, "If-Match", "\"2\"")
                .status()).isEqualTo(204);
    }

    // ---- authorship: who wrote a post or a comment comes from the token

    /** A user of the school who writes posts and comments but does not moderate them (no post approval). */
    private static String writer(UUID user) {
        return token(user, List.of("comms.post.read", "comms.post.create", "comms.post.update", "comms.post.delete",
                "comms.post_comment.*", "comms.post_audience_class.*"), List.of(SCHOOL.toString()), false);
    }

    @Test
    void aPostIsWrittenAsTheCallerAndOnlyTheAuthorChangesIt() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        String page = page(SCHOOL.toString());
        member(page, me);
        member(page, other);
        String body = "{\"institutionId\":\"" + SCHOOL + "\",\"academicYearId\":\"" + YEAR + "\",\"classPageId\":\""
                + page + "\",\"authorUserId\":\"" + other + "\",\"body\":\"Trip on Friday\"}";

        Reply created = call("POST", "/v1/comms/posts", writer(me), body);
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        assertThat(created.body().path("authorUserId").asString()).as("the author comes from the token")
                .isEqualTo(me.toString());
        String post = created.body().path("id").asString();
        assertThat(value("SELECT author_user_id::text FROM comms.post WHERE id = '" + post + "'"))
                .isEqualTo(me.toString());
        assertThat(call("POST", "/v1/comms/posts", writer(outsider), body).status())
                .as("a class page the caller is not a member of").isEqualTo(403);

        String item = "/v1/comms/posts/" + post;
        assertThat(call("PATCH", item, writer(other), "{\"body\":\"Mine now\"}", "If-Match", "\"1\"").status())
                .as("another member's post").isEqualTo(404);
        assertThat(call("DELETE", item, writer(other), null, "If-Match", "\"1\"").status()).isEqualTo(404);
        assertThat(call("PATCH", item + "?updateMask=authorUserId", writer(me), "{}", "If-Match", "\"1\"").status())
                .as("the author is not writable").isEqualTo(400);
        Reply edited = call("PATCH", item, writer(me), "{\"body\":\"Trip on Monday\",\"authorUserId\":\"" + other
                + "\"}", "If-Match", "\"1\"");
        assertThat(edited.status()).as(edited.body().toString()).isEqualTo(200);
        assertThat(edited.body().path("authorUserId").asString()).isEqualTo(me.toString());
        assertThat(call("PATCH", item, writer(me), "{\"classPageId\":\"" + page(SCHOOL.toString()) + "\"}",
                "If-Match", "\"2\"").status()).as("moved to a page the author is not on").isEqualTo(403);

        String audience = item + "/audience-classes";
        String section = "{\"postId\":\"" + post + "\",\"classSectionId\":\"" + id() + "\"}";
        assertThat(call("POST", audience, writer(other), section).status()).as("another author's post")
                .isEqualTo(404);
        Reply addressed = call("POST", audience, writer(me), section);
        assertThat(addressed.status()).as(addressed.body().toString()).isEqualTo(201);
        String audienceClass = audience + "/" + addressed.body().path("id").asString();
        assertThat(call("DELETE", audienceClass, writer(other), null, "If-Match", "\"1\"").status()).isEqualTo(404);
        assertThat(call("GET", audience, writer(outsider), null).status()).as("a page's post to a non-member")
                .isEqualTo(404);
        assertThat(call("GET", audienceClass, writer(outsider), null).status()).isEqualTo(404);
        assertThat(call("DELETE", audienceClass, writer(me), null, "If-Match", "\"1\"").status()).isEqualTo(204);

        assertThat(call("DELETE", item, writer(me), null, "If-Match", "\"2\"").status()).isEqualTo(204);
    }

    @Test
    void aCommentIsWrittenAsTheCallerAndOnlyTheAuthorChangesIt() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String page = page(SCHOOL.toString());
        member(page, me);
        member(page, other);
        String post = post(me, page, "posted", SCHOOL.toString());
        String comments = "/v1/comms/posts/" + post + "/comments";

        Reply created = call("POST", comments, writer(other), "{\"postId\":\"" + post + "\",\"authorUserId\":\"" + me
                + "\",\"body\":\"See you there\"}");
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        assertThat(created.body().path("authorUserId").asString()).as("the author comes from the token")
                .isEqualTo(other.toString());
        String comment = comments + "/" + created.body().path("id").asString();
        assertThat(call("POST", comments, writer(UUID.randomUUID()), "{\"body\":\"Let me in\"}").status())
                .as("a class page's post to a non-member").isEqualTo(404);

        assertThat(call("PATCH", comment, writer(me), "{\"body\":\"Not coming\"}", "If-Match", "\"1\"").status())
                .as("the post's author does not edit another's comment").isEqualTo(404);
        assertThat(call("DELETE", comment, writer(me), null, "If-Match", "\"1\"").status()).isEqualTo(404);
        assertThat(call("PATCH", comment + "?updateMask=authorUserId", writer(other), "{}", "If-Match", "\"1\"")
                .status()).as("the author is not writable").isEqualTo(400);
        Reply edited = call("PATCH", comment, writer(other), "{\"body\":\"See you at nine\"}", "If-Match", "\"1\"");
        assertThat(edited.status()).as(edited.body().toString()).isEqualTo(200);
        assertThat(edited.body().path("authorUserId").asString()).isEqualTo(other.toString());

        assertThat(call("DELETE", comment, user(UUID.randomUUID(), false), null, "If-Match", "\"2\"").status())
                .as("a moderator of the school removes it").isEqualTo(204);
    }

    // ---- class pages, groups and tasks

    @Test
    void aMemberFollowsTheirPageAndNobodyElseCan() throws Exception {
        UUID me = UUID.randomUUID();
        String page = page(SCHOOL.toString());
        String membership = member(page, me);
        String token = user(me, false);

        Reply followed = call("POST", "/v1/comms/class-pages/" + page + ":follow", token, "{\"follow\":true}");
        assertThat(followed.status()).as(followed.body().toString()).isEqualTo(200);
        assertThat(followed.body().path("id").asString()).isEqualTo(membership);
        assertThat(followed.body().path("isFollowing").asBoolean()).isTrue();
        assertThat(call("POST", "/v1/comms/class-pages/" + page + ":follow", token, "{\"follow\":true}").body()
                .path("etag")).as("following again changes nothing").isEqualTo(followed.body().path("etag"));
        assertThat(call("POST", "/v1/comms/class-pages/" + page + ":follow", token, "{\"follow\":false}").body()
                .path("isFollowing").asBoolean()).isFalse();
        assertThat(call("POST", "/v1/comms/class-pages/" + page + ":follow", user(UUID.randomUUID(), true),
                "{\"follow\":true}").status()).as("following never makes a member").isEqualTo(404);
        assertThat(count("SELECT count(*) FROM comms.class_page_member WHERE class_page_id = '" + page + "'"))
                .isEqualTo(1);
        String outside = page(OTHER_SCHOOL.toString());
        member(outside, me);
        assertThat(call("POST", "/v1/comms/class-pages/" + outside + ":follow", token, "{\"follow\":true}").status())
                .isEqualTo(404);
        assertThat(call("POST", "/v1/comms/class-pages/" + page + ":follow", token, "{}").status()).isIn(400, 422);
    }

    private String group(String school) {
        String id = id();
        sql("INSERT INTO comms.audience_group (id, tenant_id, institution_id, name, row_version) VALUES ('" + id
                + "', :tenant, " + (school == null ? "NULL" : "'" + school + "'") + ", 'Chess club', 1)");
        return id;
    }

    @Test
    void membersAreAddedOnceToAGroupOfTheCallersSchool() throws Exception {
        String token = user(UUID.randomUUID(), false);
        String group = group(SCHOOL.toString());
        String first = id();
        String second = id();

        Reply added = call("POST", "/v1/comms/audience-groups/" + group + ":addMembers", token,
                "{\"userIds\":[\"" + first + "\",\"" + first + "\"],\"memberType\":\"student\"}");
        assertThat(added.status()).as(added.body().toString()).isEqualTo(200);
        assertThat(added.body().path("items")).hasSize(1);
        Reply again = call("POST", "/v1/comms/audience-groups/" + group + ":addMembers", token,
                "{\"userIds\":[\"" + first + "\",\"" + second + "\"],\"memberType\":\"teacher\"}");
        assertThat(again.body().path("items")).hasSize(2);
        assertThat(count("SELECT count(*) FROM comms.audience_group_member WHERE group_id = '" + group + "'"))
                .isEqualTo(2);
        assertThat(call("POST", "/v1/comms/audience-groups/" + group + ":addMembers", token,
                "{\"userIds\":[],\"memberType\":\"student\"}").status()).isEqualTo(422);
        assertThat(call("POST", "/v1/comms/audience-groups/" + group(OTHER_SCHOOL.toString()) + ":addMembers", token,
                "{\"userIds\":[\"" + first + "\"],\"memberType\":\"student\"}").status()).isEqualTo(404);
        assertThat(call("POST", "/v1/comms/audience-groups/" + group(null) + ":addMembers", token,
                "{\"userIds\":[\"" + first + "\"],\"memberType\":\"student\"}").status())
                .as("a tenant-level group changes only through a tenant-wide grant").isEqualTo(403);
    }

    private String task(String school, String mode) {
        String id = id();
        sql("INSERT INTO comms.task (id, tenant_id, institution_id, owner_user_id, title, task_category, due_at,"
                + " reminder_option, checklist, assignment_mode, row_version) VALUES ('" + id + "', :tenant, '" + school
                + "', '" + UUID.randomUUID() + "', 'Collect forms', 'group', now() + interval '1 day',"
                + " 'one_day_before', '[]', '" + mode + "', 1)");
        return id;
    }

    @Test
    void anOpenTaskIsClaimedOnceBySomeoneItIsAssignedTo() throws Exception {
        UUID me = UUID.randomUUID();
        UUID colleague = UUID.randomUUID();
        String group = group(SCHOOL.toString());
        sql("INSERT INTO comms.audience_group_member (id, tenant_id, group_id, user_id, row_version) VALUES ('" + id()
                + "', :tenant, '" + group + "', '" + me + "', 1)", "INSERT INTO comms.audience_group_member (id,"
                + " tenant_id, group_id, user_id, row_version) VALUES ('" + id() + "', :tenant, '" + group + "', '"
                + colleague + "', 1)");
        String task = task(SCHOOL.toString(), "open");
        sql("INSERT INTO comms.task_assignee (id, tenant_id, task_id, assignee_type, group_id, row_version) VALUES ('"
                + id() + "', :tenant, '" + task + "', 'group', '" + group + "', 1)");

        assertThat(call("POST", "/v1/comms/tasks/" + task + ":claim", user(UUID.randomUUID(), false), "{}").status())
                .as("not assigned to the caller").isEqualTo(403);
        Reply claimed = call("POST", "/v1/comms/tasks/" + task + ":claim", user(me, false), "{}");
        assertThat(claimed.status()).as(claimed.body().toString()).isEqualTo(200);
        assertThat(claimed.body().path("items").get(0).path("claimedByUserId").asString()).isEqualTo(me.toString());
        assertThat(call("POST", "/v1/comms/tasks/" + task + ":claim", user(me, false), "{}").status())
                .as("claiming again is harmless").isEqualTo(200);
        Reply late = call("POST", "/v1/comms/tasks/" + task + ":claim", user(colleague, false), "{}");
        assertThat(late.status()).isEqualTo(409);
        assertThat(late.code()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(call("POST", "/v1/comms/tasks/" + task(SCHOOL.toString(), "individual") + ":claim",
                user(me, false), "{}").status()).as("only an open task is claimed").isEqualTo(409);
        assertThat(call("POST", "/v1/comms/tasks/" + task(OTHER_SCHOOL.toString(), "open") + ":claim",
                user(me, false), "{}").status()).isEqualTo(404);
    }

    // ---- the calendar

    private String event(String audience, UUID owner, String category, String school) {
        String id = id();
        sql("INSERT INTO comms.event (id, tenant_id, institution_id, audience, owner_user_id, title, category,"
                + " start_at, end_at, status, row_version) VALUES ('" + id + "', :tenant, '" + school + "', '"
                + audience + "', " + (owner == null ? "NULL" : "'" + owner + "'") + ", 'Event " + audience + "', '"
                + category + "', '2026-03-10T09:00:00Z', '2026-03-10T10:00:00Z', 'active', 1)");
        return id;
    }

    private String occurrence(String event, UUID user, String start) {
        String id = id();
        sql("INSERT INTO comms.event_occurrence (id, tenant_id, event_id, user_id, start_at, row_version) VALUES ('"
                + id + "', :tenant, '" + event + "', " + (user == null ? "NULL" : "'" + user + "'") + ", '" + start
                + "', 1)");
        return id;
    }

    @Test
    void theCalendarIsTheCallersAndTheirPersonalEventsTheirsAlone() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String token = user(me, false);
        String school = event("school", null, "meeting", SCHOOL.toString());
        String holiday = event("school", null, "holiday", SCHOOL.toString());
        String elsewhere = event("school", null, "meeting", OTHER_SCHOOL.toString());
        String classEvent = event("class", null, "exam", SCHOOL.toString());
        String invited = event("class", null, "exam", SCHOOL.toString());
        sql("INSERT INTO comms.event_attendee (id, tenant_id, event_id, user_id, row_version) VALUES ('" + id()
                + "', :tenant, '" + invited + "', '" + me + "', 1)");
        String personal = event("personal", me, "other", SCHOOL.toString());
        String mine = occurrence(personal, null, "2026-03-11T08:00:00Z");
        String theirsEvent = event("personal", other, "other", SCHOOL.toString());
        String theirs = occurrence(theirsEvent, null, "2026-03-11T08:00:00Z");
        String window = "?from=" + encode("2026-03-01T00:00:00Z") + "&to=" + encode("2026-04-01T00:00:00Z");

        Reply calendar = call("GET", "/v1/comms/me/calendar" + window, token, null);
        assertThat(calendar.status()).as(calendar.body().toString()).isEqualTo(200);
        assertThat(calendar.ids("data")).containsExactlyInAnyOrder(school, holiday, invited, mine)
                .doesNotContain(elsewhere, classEvent, theirs);
        assertThat(call("GET", "/v1/comms/me/calendar" + window + "&itemTypes=holiday", token, null).ids("data"))
                .containsExactly(holiday);
        Reply paged = call("GET", "/v1/comms/me/calendar" + window + "&size=2", token, null);
        assertThat(paged.ids("data")).hasSize(2);
        Reply next = call("GET", paged.body().path("paging").path("next").asString().replaceFirst("^https?://[^/]+",
                ""), token, null);
        assertThat(next.ids("data")).hasSize(2).doesNotContainAnyElementsOf(paged.ids("data"));
        assertThat(call("GET", "/v1/comms/me/calendar", token, null).status()).isEqualTo(400);
        assertThat(call("GET", "/v1/comms/me/calendar?from=" + encode("2026-03-01T00:00:00Z") + "&to="
                + encode("2027-06-01T00:00:00Z"), token, null).status()).as("at most 366 days").isEqualTo(400);
        assertThat(call("GET", "/v1/comms/me/calendar" + window + "&itemTypes=birthday", token, null).status())
                .isEqualTo(400);

        String path = "/v1/comms/events/" + personal + "/occurrences/" + mine + ":complete";
        assertThat(call("POST", "/v1/comms/events/" + theirsEvent + "/occurrences/" + theirs + ":complete",
                user(me, true), "{}").status()).as("another person's personal event").isEqualTo(404);
        assertThat(call("POST", path, token, STALE_BODY).status()).isEqualTo(412);
        Reply done = call("POST", path, token, "{}");
        assertThat(done.status()).as(done.body().toString()).isEqualTo(200);
        assertThat(done.body().path("responseStatus").asString()).isEqualTo("completed");
        assertThat(call("POST", path, token, "{}").status()).isEqualTo(409);
        assertThat(call("GET", "/v1/comms/me/calendar" + window + "&itemTypes=occurrence", token, null).body()
                .path("data").get(0).path("isCompleted").asBoolean()).isTrue();
        String schoolOccurrence = occurrence(school, null, "2026-03-12T08:00:00Z");
        assertThat(call("POST", "/v1/comms/events/" + school + "/occurrences/" + schoolOccurrence + ":complete",
                token, "{}").status()).as("only a personal event is completed").isEqualTo(404);
    }

    @Test
    void anExportNeedsThePlatformAndWritesNothingWithoutIt() throws Exception {
        UUID me = UUID.randomUUID();
        event("personal", me, "other", SCHOOL.toString());
        String body = "{\"provider\":\"google\",\"from\":\"2026-03-01T00:00:00Z\",\"to\":\"2026-04-01T00:00:00Z\"}";

        Reply refused = call("POST", "/v1/comms/me/calendar:export", user(me, false), body);
        assertThat(refused.status()).as(refused.body().toString()).isEqualTo(503);
        assertThat(count("SELECT count(*) FROM comms.calendar_export WHERE user_id = '" + me + "'")).isZero();
        assertThat(call("POST", "/v1/comms/me/calendar:export", user(me, false),
                body.replace("2026-04-01", "2026-02-01")).status()).as("to before from").isEqualTo(400);
        assertThat(call("POST", "/v1/comms/me/calendar:export", user(me, false), "{\"provider\":\"google\"}")
                .status()).isIn(400, 422);
    }

    // ---- conversations and feedback

    @Test
    void aConversationIsReadMarkedAndHiddenByItsParticipantsOnly() throws Exception {
        UUID me = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        String conversation = id();
        String fromTeacher = id();
        String fromMe = id();
        sql("INSERT INTO comms.conversation (id, tenant_id, institution_id, conversation_type, created_by,"
                + " row_version) VALUES ('" + conversation + "', :tenant, '" + SCHOOL + "', 'direct', '" + teacher
                + "', 1)");
        for (UUID user : List.of(me, teacher)) {
            sql("INSERT INTO comms.conversation_participant (id, tenant_id, conversation_id, user_id, row_version)"
                    + " VALUES ('" + id() + "', :tenant, '" + conversation + "', '" + user + "', 1)");
        }
        sql("INSERT INTO comms.conversation_message (id, tenant_id, conversation_id, sender_user_id, body, created_at,"
                + " row_version) VALUES ('" + fromTeacher + "', :tenant, '" + conversation + "', '" + teacher
                + "', 'Hello', now() - interval '1 minute', 1)", "INSERT INTO comms.conversation_message (id,"
                + " tenant_id, conversation_id, sender_user_id, body, row_version) VALUES ('" + fromMe + "', :tenant, '"
                + conversation + "', '" + me + "', 'Hi', 1)");
        String messages = "/v1/comms/conversations/" + conversation + "/messages";
        String token = user(me, false);
        String stranger = user(outsider, true);

        // the leak this release closes: only whoever opened a conversation and its participants reach it
        assertThat(call("GET", "/v1/comms/conversations/" + conversation, token, null).status()).isEqualTo(200);
        assertThat(call("GET", "/v1/comms/conversations/" + conversation, stranger, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/comms/conversations?size=500", stranger, null).ids("data"))
                .doesNotContain(conversation);
        assertThat(call("GET", "/v1/comms/conversations?size=500", token, null).ids("data")).contains(conversation);
        assertThat(call("GET", messages, stranger, null).status()).isEqualTo(404);
        assertThat(call("GET", messages + "/" + fromMe, stranger, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/comms/conversations/" + conversation + "/participants", stranger, null).status())
                .isEqualTo(404);
        assertThat(call("POST", messages, stranger, "{\"conversationId\":\"" + conversation + "\",\"senderUserId\":\""
                + outsider + "\",\"body\":\"Let me in\"}").status()).isEqualTo(404);

        assertThat(call("POST", "/v1/comms/conversations/" + conversation + ":markRead", token, "{}").status())
                .isEqualTo(204);
        assertThat(value("SELECT (last_read_at IS NOT NULL)::text FROM comms.conversation_participant WHERE user_id = '"
                + me + "'")).isEqualTo("true");
        assertThat(value("SELECT (last_read_at IS NULL)::text FROM comms.conversation_participant WHERE user_id = '"
                + teacher + "'")).as("only the caller's mark moves").isEqualTo("true");
        assertThat(call("POST", "/v1/comms/conversations/" + conversation + ":markRead", stranger, "{}").status())
                .isEqualTo(404);

        assertThat(call("POST", "/v1/comms/messages/" + fromTeacher + ":hideForMe", stranger, "{}").status())
                .isEqualTo(404);
        assertThat(call("POST", "/v1/comms/messages/" + fromTeacher + ":hideForMe", token, "{}").status())
                .isEqualTo(204);
        assertThat(call("POST", "/v1/comms/messages/" + fromTeacher + ":hideForMe", token, "{}").status())
                .as("hiding again changes nothing").isEqualTo(204);
        assertThat(call("POST", "/v1/comms/messages/" + fromMe + ":hideForMe", token, "{}").status()).isEqualTo(204);
        assertThat(call("GET", messages, token, null).ids("data")).isEmpty();
        assertThat(call("GET", messages, user(teacher, false), null).ids("data"))
                .as("the other participant still sees both").containsExactlyInAnyOrder(fromTeacher, fromMe);
        assertThat(call("POST", "/v1/comms/messages/" + id() + ":hideForMe", token, "{}").status()).isEqualTo(404);
    }

    @Test
    void aFeedbackMessageIsMarkedForwardBySomeoneOtherThanItsSender() throws Exception {
        UUID guardian = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        String thread = id();
        String message = id();
        sql("INSERT INTO comms.feedback_thread (id, tenant_id, institution_id, guardian_id, student_id,"
                + " assigned_to_user_id, row_version) VALUES ('" + thread + "', :tenant, '" + SCHOOL + "', '"
                + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '" + teacher + "', 1)",
                "INSERT INTO comms.feedback_message (id, tenant_id, thread_id, sender_user_id, category, message,"
                        + " row_version) VALUES ('" + message + "', :tenant, '" + thread + "', '" + guardian
                        + "', 'complaint', 'The bus was late', 1)");
        String path = "/v1/comms/feedback-messages/" + message + ":markReviewStatus";
        String token = user(teacher, false);

        assertThat(call("POST", path, user(guardian, false), "{\"reviewStatus\":\"seen\"}").status())
                .as("the sender").isEqualTo(403);
        assertThat(call("POST", path, token, "{\"reviewStatus\":\"done\"}").status()).isIn(400, 422);
        Reply seen = call("POST", path, token, "{\"reviewStatus\":\"seen\"}");
        assertThat(seen.status()).as(seen.body().toString()).isEqualTo(200);
        assertThat(seen.body().path("reviewStatus").asString()).isEqualTo("seen");
        assertThat(call("POST", path, token, "{\"reviewStatus\":\"seen\"}").status()).isEqualTo(409);
        assertThat(call("POST", path, token, "{\"reviewStatus\":\"action_taken\"}").body().path("reviewStatus")
                .asString()).isEqualTo("action_taken");
        assertThat(call("POST", path, token(teacher, List.of("comms.*"), List.of(OTHER_SCHOOL.toString()), false),
                "{\"reviewStatus\":\"action_taken\"}").status()).isEqualTo(404);
    }

    @Test
    void aConversationMessageIsSentAsTheCallerAndOnlyTheSenderChangesIt() throws Exception {
        UUID me = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        String conversation = id();
        sql("INSERT INTO comms.conversation (id, tenant_id, institution_id, conversation_type, created_by,"
                + " row_version) VALUES ('" + conversation + "', :tenant, '" + SCHOOL + "', 'direct', '" + teacher
                + "', 1)");
        for (UUID user : List.of(me, teacher)) {
            sql("INSERT INTO comms.conversation_participant (id, tenant_id, conversation_id, user_id, row_version)"
                    + " VALUES ('" + id() + "', :tenant, '" + conversation + "', '" + user + "', 1)");
        }
        String messages = "/v1/comms/conversations/" + conversation + "/messages";

        Reply sent = call("POST", messages, user(me, false), "{\"conversationId\":\"" + conversation
                + "\",\"senderUserId\":\"" + teacher + "\",\"body\":\"Running late\"}");
        assertThat(sent.status()).as(sent.body().toString()).isEqualTo(201);
        assertThat(sent.body().path("senderUserId").asString()).as("the sender comes from the token")
                .isEqualTo(me.toString());
        String message = messages + "/" + sent.body().path("id").asString();
        assertThat(value("SELECT sender_user_id::text FROM comms.conversation_message WHERE id = '"
                + sent.body().path("id").asString() + "'")).isEqualTo(me.toString());

        assertThat(call("PATCH", message, user(teacher, true), "{\"body\":\"On time\"}", "If-Match", "\"1\"").status())
                .as("another participant's message, whatever their grants").isEqualTo(404);
        assertThat(call("DELETE", message, user(teacher, true), null, "If-Match", "\"1\"").status()).isEqualTo(404);
        assertThat(call("GET", message, user(teacher, false), null).status()).as("participants still read it")
                .isEqualTo(200);
        assertThat(call("PATCH", message + "?updateMask=senderUserId", user(me, false), "{}", "If-Match", "\"1\"")
                .status()).as("the sender is not writable").isEqualTo(400);
        Reply edited = call("PATCH", message, user(me, false), "{\"body\":\"Running very late\"}", "If-Match", "\"1\"");
        assertThat(edited.status()).as(edited.body().toString()).isEqualTo(200);
        assertThat(call("DELETE", message, user(me, false), null, "If-Match", "\"2\"").status()).isEqualTo(204);
    }

    @Test
    void aFeedbackMessageIsSentAsTheCallerAndOnlyTheSchoolSideMarksIt() throws Exception {
        UUID guardian = UUID.randomUUID();
        UUID teacher = UUID.randomUUID();
        String thread = id();
        sql("INSERT INTO comms.feedback_thread (id, tenant_id, institution_id, guardian_id, student_id,"
                + " assigned_to_user_id, row_version) VALUES ('" + thread + "', :tenant, '" + SCHOOL + "', '"
                + UUID.randomUUID() + "', '" + UUID.randomUUID() + "', '" + teacher + "', 1)");
        String messages = "/v1/comms/feedback-threads/" + thread + "/messages";
        String school = user(teacher, false);
        String parent = user(guardian, false);

        Reply sent = call("POST", messages, parent, "{\"threadId\":\"" + thread + "\",\"senderUserId\":\"" + teacher
                + "\",\"category\":\"complaint\",\"message\":\"The bus was late\"}");
        assertThat(sent.status()).as(sent.body().toString()).isEqualTo(201);
        assertThat(sent.body().path("senderUserId").asString()).as("the sender comes from the token")
                .isEqualTo(guardian.toString());
        String message = messages + "/" + sent.body().path("id").asString();

        assertThat(call("PATCH", message, school, "{\"message\":\"Nothing happened\"}", "If-Match", "\"1\"").status())
                .as("the sender's words are not the school's to edit").isEqualTo(404);
        assertThat(call("DELETE", message, school, null, "If-Match", "\"1\"").status()).isEqualTo(404);
        assertThat(call("PATCH", message, parent, "{\"reviewStatus\":\"action_taken\"}", "If-Match", "\"1\"")
                .status()).as("the sender does not mark it").isEqualTo(403);
        assertThat(call("PATCH", message + "?updateMask=senderUserId", parent, "{}", "If-Match", "\"1\"").status())
                .as("the sender is not writable").isEqualTo(400);
        Reply marked = call("PATCH", message, school, "{\"reviewStatus\":\"seen\",\"deletedByReceiver\":true}",
                "If-Match", "\"1\"");
        assertThat(marked.status()).as(marked.body().toString()).isEqualTo(200);
        assertThat(marked.body().path("reviewStatus").asString()).isEqualTo("seen");
        assertThat(marked.body().path("deletedByReceiver").asBoolean()).isTrue();
        assertThat(call("PATCH", message, school, "{\"reviewStatus\":\"new\"}", "If-Match", "\"2\"").status())
                .as("the handling state does not move back").isEqualTo(409);
        Reply edited = call("PATCH", message, parent, "{\"message\":\"The bus was very late\"}", "If-Match", "\"2\"");
        assertThat(edited.status()).as(edited.body().toString()).isEqualTo(200);
        assertThat(edited.body().path("senderUserId").asString()).isEqualTo(guardian.toString());
        assertThat(call("DELETE", message, parent, null, "If-Match", "\"3\"").status()).isEqualTo(204);
    }

    /** hr's event of a staff record of the school with a login account. */
    private void staffOf(UUID account, UUID school) {
        UUID staff = UUID.randomUUID();
        deliver("hr", "hr.staff.created", "hr.staff", staff, "{\"id\":\"" + staff + "\",\"userId\":\"" + account
                + "\",\"personId\":\"" + UUID.randomUUID() + "\",\"institutionId\":\"" + school
                + "\",\"rowVersion\":1}", staffRecords::apply);
    }

    /** sis's events of a student of the school and a guardian (record, person, account) linked to it. */
    private void guardianOf(UUID guardianRecord, UUID guardianPerson, UUID guardianAccount, UUID student, UUID school) {
        deliver("sis", "sis.student.created", "sis.student", student, "{\"id\":\"" + student + "\",\"personId\":\""
                + UUID.randomUUID() + "\",\"institutionId\":\"" + school + "\",\"rowVersion\":1}",
                studentRecords::apply);
        UUID link = UUID.randomUUID();
        deliver("sis", "sis.student_guardian.created", "sis.student_guardian", link, "{\"id\":\"" + link
                + "\",\"studentId\":\"" + student + "\",\"studentPersonId\":\"" + UUID.randomUUID()
                + "\",\"guardianId\":\"" + guardianRecord + "\",\"guardianPersonId\":\"" + guardianPerson
                + "\",\"guardianUserId\":" + (guardianAccount == null ? "null" : "\"" + guardianAccount + "\"")
                + ",\"isLegalGuardian\":true,\"hasCustody\":true,\"status\":\"active\",\"rowVersion\":1}",
                guardianLinks::apply);
    }

    /** A guardian's token: the feedback permissions a parent role holds, in the school, as the account and person. */
    private static String parent(UUID account, UUID person) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", account.toString());
        claims.put("pid", person.toString());
        claims.put("tid", TENANT.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("comms.feedback_thread.read", "comms.feedback_thread.create",
                "comms.feedback_message.read", "comms.feedback_message.create"));
        claims.put("tperms", List.of());
        claims.put("ins", List.of(SCHOOL.toString()));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    @Test
    void aFeedbackThreadIsItsGuardiansItsHandlersAndTheSchoolSidesOnly() throws Exception {
        UUID recordA = UUID.randomUUID();
        UUID personA = UUID.randomUUID();
        UUID accountA = UUID.randomUUID();
        UUID childA = UUID.randomUUID();
        UUID recordB = UUID.randomUUID();
        UUID personB = UUID.randomUUID();
        UUID childB = UUID.randomUUID();
        guardianOf(recordA, personA, accountA, childA, SCHOOL);
        guardianOf(recordB, personB, null, childB, SCHOOL);
        UUID handler = UUID.randomUUID();
        String threadA = id();
        String threadB = id();
        for (String[] row : List.of(new String[] {threadA, recordA.toString(), childA.toString()},
                new String[] {threadB, recordB.toString(), childB.toString()})) {
            sql("INSERT INTO comms.feedback_thread (id, tenant_id, institution_id, guardian_id, student_id,"
                    + " assigned_to_user_id, row_version) VALUES ('" + row[0] + "', :tenant, '" + SCHOOL + "', '"
                    + row[1] + "', '" + row[2] + "', '" + handler + "', 1)");
        }
        String guardianA = parent(accountA, personA);
        String guardianB = parent(UUID.randomUUID(), personB);
        String reader = token(UUID.randomUUID(), List.of("comms.feedback_thread.read", "comms.feedback_message.read",
                "comms.feedback_message.create"), List.of(SCHOOL.toString()), false);
        String schoolSide = token(UUID.randomUUID(), List.of("comms.feedback_thread.read",
                "comms.feedback_message.read", "comms.feedback_message.mark_feedback_message"),
                List.of(SCHOOL.toString()), false);
        String messageOf = "{\"category\":\"complaint\",\"message\":\"About my child\"}";

        assertThat(call("GET", "/v1/comms/feedback-threads?size=500", guardianA, null).ids("data"))
                .as("the guardian's own thread only").containsExactly(threadA);
        assertThat(call("GET", "/v1/comms/feedback-threads/" + threadB, guardianA, null).status())
                .as("another guardian's complaint").isEqualTo(404);
        assertThat(call("GET", "/v1/comms/feedback-threads/" + threadB + "/messages", guardianA, null).status())
                .isEqualTo(404);
        assertThat(call("POST", "/v1/comms/feedback-threads/" + threadB + "/messages", guardianA, messageOf).status())
                .as("nor writes into it").isEqualTo(404);
        assertThat(call("GET", "/v1/comms/feedback-threads/" + threadB, guardianB, null).status())
                .as("the guardian by their person").isEqualTo(200);
        assertThat(call("POST", "/v1/comms/feedback-threads/" + threadA + "/messages", guardianA, messageOf).status())
                .isEqualTo(201);

        assertThat(call("GET", "/v1/comms/feedback-threads?size=500", reader, null).ids("data"))
                .as("the read permission alone reaches nobody's thread").isEmpty();
        assertThat(call("GET", "/v1/comms/feedback-threads/" + threadA + "/messages", reader, null).status())
                .isEqualTo(404);
        assertThat(call("POST", "/v1/comms/feedback-threads/" + threadA + "/messages", reader, messageOf).status())
                .isEqualTo(404);
        assertThat(call("GET", "/v1/comms/feedback-threads?size=500", schoolSide, null).ids("data"))
                .as("the school side of the institution").contains(threadA, threadB);
        assertThat(call("GET", "/v1/comms/feedback-threads/" + threadA + "/messages", user(handler, false), null)
                .status()).as("the handler").isEqualTo(200);

        String opening = "{\"institutionId\":\"" + SCHOOL + "\",\"guardianId\":\"%s\",\"studentId\":\"%s\","
                + "\"assignedToUserId\":\"" + handler + "\"}";
        assertThat(call("POST", "/v1/comms/feedback-threads", guardianA, opening.formatted(recordB, childB)).status())
                .as("not as another guardian").isEqualTo(403);
        assertThat(call("POST", "/v1/comms/feedback-threads", guardianA, opening.formatted(recordA, childB)).status())
                .as("nor about someone else's child").isEqualTo(403);
        Reply opened = call("POST", "/v1/comms/feedback-threads", guardianA, opening.formatted(recordA, childA));
        assertThat(opened.status()).as(opened.body().toString()).isEqualTo(201);
    }

    @Test
    void onlyTheOpenerManagesWhoTakesPartAndEachMovesOnlyTheirOwnMark() throws Exception {
        UUID opener = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID colleague = UUID.randomUUID();
        UUID elsewhere = UUID.randomUUID();
        staffOf(opener, SCHOOL);
        staffOf(member, SCHOOL);
        staffOf(colleague, SCHOOL);
        staffOf(elsewhere, OTHER_SCHOOL);
        String conversation = id();
        sql("INSERT INTO comms.conversation (id, tenant_id, institution_id, conversation_type, created_by,"
                + " row_version) VALUES ('" + conversation + "', :tenant, '" + SCHOOL + "', 'group', '" + opener
                + "', 1)");
        String participants = "/v1/comms/conversations/" + conversation + "/participants";
        String owner = user(opener, false);
        String other = user(member, false);
        Reply added = call("POST", participants, owner, "{\"userId\":\"" + member + "\"}");
        assertThat(added.status()).as(added.body().toString()).isEqualTo(201);
        String memberRow = participants + "/" + added.body().path("id").asString();

        assertThat(call("POST", participants, other, "{\"userId\":\"" + colleague + "\"}").status())
                .as("a participant does not add people").isEqualTo(403);
        Reply stranger = call("POST", participants, owner, "{\"userId\":\"" + elsewhere + "\"}");
        assertThat(stranger.status()).as("an account of another school").isEqualTo(422);
        assertThat(stranger.body().path("errors").get(0).path("field").asString()).isEqualTo("userId");
        assertThat(call("POST", participants, owner, "{\"userId\":\"" + UUID.randomUUID() + "\"}").status())
                .as("an account comms does not know").isEqualTo(422);
        Reply colleagueAdded = call("POST", participants, owner, "{\"userId\":\"" + colleague + "\"}");
        String colleagueRow = participants + "/" + colleagueAdded.body().path("id").asString();

        assertThat(call("PATCH", colleagueRow, other, "{\"lastReadAt\":\"2026-01-01T00:00:00Z\"}", "If-Match",
                "\"1\"").status()).as("another's read mark").isEqualTo(404);
        assertThat(call("PATCH", colleagueRow, owner, "{\"lastReadAt\":\"2026-01-01T00:00:00Z\"}", "If-Match",
                "\"1\"").status()).as("not even the opener's to move").isEqualTo(404);
        assertThat(call("PATCH", memberRow, other, "{\"userId\":\"" + colleague + "\"}", "If-Match", "\"1\"")
                .status()).as("a row is never repointed").isEqualTo(422);
        Reply marked = call("PATCH", memberRow, other, "{\"lastReadAt\":\"2026-01-01T00:00:00Z\"}", "If-Match",
                "\"1\"");
        assertThat(marked.status()).as(marked.body().toString()).isEqualTo(200);
        assertThat(call("PATCH", "/v1/comms/conversations/" + conversation, other, "{\"title\":\"Mine now\"}",
                "If-Match", "\"1\"").status()).as("only the opener changes the conversation").isEqualTo(403);
        assertThat(call("DELETE", colleagueRow, other, null, "If-Match", "\"1\"").status())
                .as("a participant removes nobody else").isEqualTo(404);
        assertThat(call("DELETE", colleagueRow, owner, null, "If-Match", "\"1\"").status())
                .as("the opener removes someone").isEqualTo(204);
        assertThat(call("DELETE", memberRow, other, null, "If-Match", "\"2\"").status()).as("and anyone leaves")
                .isEqualTo(204);
    }

    @Test
    void mailGoesOnlyToTheSchoolsAccountsAndOutsideWithinItsLimits() throws Exception {
        UUID me = UUID.randomUUID();
        UUID elsewhere = UUID.randomUUID();
        UUID pupil = UUID.randomUUID();
        staffOf(elsewhere, OTHER_SCHOOL);
        String token = user(me, false);
        String compose = "/v1/comms/mailbox-messages:compose";

        Reply outsider = call("POST", compose, token, "{\"subject\":\"S\",\"body\":\"B\",\"toUserIds\":[\""
                + elsewhere + "\"]}");
        assertThat(outsider.status()).as("an account of another school").isEqualTo(422);
        assertThat(outsider.body().path("errors").get(0).path("field").asString()).isEqualTo("toUserIds[0]");
        assertThat(call("POST", compose, token, "{\"subject\":\"S\",\"body\":\"B\",\"toUserIds\":[\""
                + UUID.randomUUID() + "\"]}").status()).as("an account comms does not know").isEqualTo(422);
        Reply about = call("POST", compose, token, "{\"subject\":\"S\",\"body\":\"B\",\"toAddresses\":"
                + "[\"a@example.org\"],\"aboutStudentId\":\"" + pupil + "\"}");
        assertThat(about.status()).as("a student not of the school").isEqualTo(422);
        assertThat(about.body().path("errors").get(0).path("field").asString()).isEqualTo("aboutStudentId");
        List<String> many = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            many.add("\"bulk" + i + "@example.org\"");
        }
        assertThat(call("POST", compose, token, "{\"subject\":\"S\",\"body\":\"B\",\"toAddresses\":["
                + String.join(",", many) + "]}").status()).as("at most 10 outside addresses a mail").isEqualTo(422);
        sql("INSERT INTO comms.mailbox_message (id, tenant_id, institution_id, owner_user_id, folder, to_addresses,"
                + " delivery_status, row_version) SELECT gen_random_uuid(), :tenant, '" + SCHOOL + "', '" + me
                + "', 'sent', 'x@example.org', 'delivered', 1 FROM generate_series(1, 50)");
        Reply limited = call("POST", compose, token, "{\"subject\":\"S\",\"body\":\"B\",\"toAddresses\":"
                + "[\"one-more@example.org\"]}");
        assertThat(limited.status()).as("at most 50 such mails a day").isEqualTo(429);
        assertThat(count("SELECT count(*) FROM comms.mailbox_message WHERE owner_user_id = '" + me + "'"))
                .as("nothing written").isEqualTo(50);
    }

    // ---- mail and delivery

    private static MimeMessage mailTo(String address) throws Exception {
        assertThat(SMTP.waitForIncomingEmail(10_000, 1)).isTrue();
        for (MimeMessage mail : SMTP.getReceivedMessages()) {
            if (mail.getAllRecipients()[0].toString().equals(address)) {
                return mail;
            }
        }
        throw new AssertionError("no mail to " + address);
    }

    @Test
    void composedMailLandsInEachMailboxAndGoesOutByEmail() throws Exception {
        UUID me = UUID.randomUUID();
        UUID colleague = UUID.randomUUID();
        staffOf(colleague, SCHOOL);
        String token = user(me, false);
        String address = "parent-" + UUID.randomUUID() + "@example.org";

        Reply sent = call("POST", "/v1/comms/mailbox-messages:compose", token, "{\"subject\":\"Trip\",\"body\":\"On"
                + " Friday\",\"toUserIds\":[\"" + colleague + "\"],\"toAddresses\":[\"" + address + "\"]}");
        assertThat(sent.status()).as(sent.body().toString()).isEqualTo(200);
        assertThat(sent.body().path("folder").asString()).isEqualTo("sent");
        assertThat(sent.body().path("ownerUserId").asString()).isEqualTo(me.toString());
        assertThat(sent.body().path("deliveryStatus").asString()).isEqualTo("delivered");
        MimeMessage mail = mailTo(address);
        assertThat(mail.getSubject()).isEqualTo("Trip");
        assertThat(GreenMailUtil.getBody(mail)).contains("On Friday");
        String inbox = value("SELECT id::text FROM comms.mailbox_message WHERE owner_user_id = '" + colleague + "'");
        assertThat(value("SELECT folder FROM comms.mailbox_message WHERE id = '" + inbox + "'")).isEqualTo("inbox");

        String mine = user(colleague, false);
        assertThat(call("GET", "/v1/comms/mailbox-messages/" + inbox, token, null).status())
                .as("the sender does not read the recipient's copy").isEqualTo(404);
        assertThat(call("POST", "/v1/comms/mailbox-messages/" + inbox + ":markRead", token, "{}").status())
                .isEqualTo(404);
        Reply read = call("POST", "/v1/comms/mailbox-messages/" + inbox + ":markRead", mine, "{}");
        assertThat(read.status()).as(read.body().toString()).isEqualTo(200);
        assertThat(read.body().path("readAt").isMissingNode()).isFalse();
        assertThat(call("POST", "/v1/comms/mailbox-messages/" + inbox + ":markRead", mine, "{}").body().path("etag"))
                .as("reading again changes nothing").isEqualTo(read.body().path("etag"));
        assertThat(call("GET", "/v1/comms/mailbox-messages?size=500", mine, null).ids("data")).containsExactly(inbox);

        assertThat(call("POST", "/v1/comms/mailbox-messages:compose", token, "{\"subject\":\"S\",\"body\":\"B\"}")
                .status()).as("no recipient").isEqualTo(422);
        assertThat(call("POST", "/v1/comms/mailbox-messages:compose", token, "{\"subject\":\"S\",\"body\":\"B\","
                + "\"toUserIds\":[\"" + colleague + "\"],\"attachmentFileIds\":[\"" + id() + "\"]}").status())
                .as("attachments are not offered yet").isEqualTo(422);
        assertThat(call("POST", "/v1/comms/mailbox-messages:compose", token(me, List.of("comms.*"),
                List.of(SCHOOL.toString(), OTHER_SCHOOL.toString()), false), "{\"subject\":\"S\",\"body\":\"B\","
                + "\"toUserIds\":[\"" + colleague + "\"]}").status()).as("which school is it from?").isEqualTo(422);
    }

    private String outbound(String channel, String status, String school, String recipient) {
        String id = id();
        sql("INSERT INTO comms.outbound_message (id, tenant_id, institution_id, message_kind, channel, sender,"
                + " recipient, subject, body, status, row_version) VALUES ('" + id + "', :tenant, '" + school
                + "', 'notification', '" + channel + "', 'school', '" + recipient + "', 'Bus', 'The bus is late', '"
                + status + "', 1)");
        return id;
    }

    @Test
    void messagesAreCancelledWhileQueuedAndRetriedAfterAFailure() throws Exception {
        String token = user(UUID.randomUUID(), false);
        String queued = outbound("email", "queued", SCHOOL.toString(), "a@example.org");
        String address = "retry-" + UUID.randomUUID() + "@example.org";
        String failed = outbound("email", "failed", SCHOOL.toString(), address);

        assertThat(call("POST", "/v1/comms/outbound-messages/" + queued + ":cancel", token,
                STALE_BODY).status()).isEqualTo(412);
        Reply cancelled = call("POST", "/v1/comms/outbound-messages/" + queued + ":cancel", token, "{}");
        assertThat(cancelled.status()).as(cancelled.body().toString()).isEqualTo(200);
        assertThat(cancelled.body().path("status").asString()).isEqualTo("cancelled");
        assertThat(call("POST", "/v1/comms/outbound-messages/" + queued + ":cancel", token, "{}").status())
                .isEqualTo(409);
        assertThat(call("POST", "/v1/comms/outbound-messages/" + queued + ":retry", token, "{}").status())
                .as("only a failed message is retried").isEqualTo(409);
        assertThat(call("POST", "/v1/comms/outbound-messages/"
                + outbound("email", "queued", OTHER_SCHOOL.toString(), "b@example.org") + ":cancel", token, "{}")
                .status()).isEqualTo(404);

        Reply retried = call("POST", "/v1/comms/outbound-messages/" + failed + ":retry", token, "{}");
        assertThat(retried.status()).as(retried.body().toString()).isEqualTo(200);
        assertThat(retried.body().path("status").asString()).isEqualTo("delivered");
        assertThat(mailTo(address).getSubject()).isEqualTo("Bus");
        Reply sms = call("POST", "/v1/comms/outbound-messages/"
                + outbound("sms", "failed", SCHOOL.toString(), "9800000000") + ":retry", token, "{}");
        assertThat(sms.status()).as("no SMS provider").isEqualTo(503);
        assertThat(sms.code()).isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void sendingRefusesWhatThisReleaseCannotDeliverAndWritesNothing() throws Exception {
        String token = user(UUID.randomUUID(), false);
        String send = "/v1/comms/outbound-messages:send";
        String key = "Idempotency-Key";
        int before = count("SELECT count(*) FROM comms.outbound_message");

        Reply sms = call("POST", send, token, "{\"channel\":\"sms\",\"recipients\":{\"userIds\":[\"" + id()
                + "\"]},\"messageKind\":\"notification\",\"body\":\"Hi\"}", key, id());
        assertThat(sms.status()).isEqualTo(503);
        assertThat(sms.code()).isEqualTo("NOT_CONFIGURED");
        Reply classes = call("POST", send, token, "{\"channel\":\"email\",\"recipients\":{\"classSectionIds\":[\""
                + id() + "\"],\"includeGuardians\":true},\"messageKind\":\"notification\",\"body\":\"Hi\","
                + "\"scheduledAt\":\"2030-01-01T00:00:00Z\"}", key, id());
        assertThat(classes.status()).isEqualTo(422);
        List<String> fields = new ArrayList<>();
        classes.body().path("errors").forEach(error -> fields.add(error.path("field").asString()));
        assertThat(fields).contains("recipients.classSectionIds", "recipients.includeGuardians", "scheduledAt");
        assertThat(call("POST", send, token, "{\"channel\":\"email\",\"recipients\":{\"userIds\":[\"" + id()
                + "\"]},\"messageKind\":\"notification\"}", key, id()).status()).as("no text").isEqualTo(422);
        assertThat(call("POST", send, token, "{\"channel\":\"email\",\"recipients\":{\"audienceGroupIds\":[\""
                + group(OTHER_SCHOOL.toString()) + "\"]},\"messageKind\":\"notification\",\"body\":\"Hi\"}", key, id())
                .status()).as("a group outside the caller's school").isEqualTo(422);
        assertThat(call("POST", send, token, "{\"channel\":\"email\",\"recipients\":{\"userIds\":[\"" + id()
                + "\"]},\"messageKind\":\"notification\",\"body\":\"Hi\"}").status())
                .as("the key is required").isEqualTo(400);
        Reply noIam = call("POST", send, token, "{\"channel\":\"email\",\"recipients\":{\"userIds\":[\"" + id()
                + "\"]},\"messageKind\":\"notification\",\"body\":\"Hi\"}", key, id());
        assertThat(noIam.status()).as("the addresses come from iam").isEqualTo(503);
        assertThat(count("SELECT count(*) FROM comms.outbound_message")).isEqualTo(before);
    }

    @Test
    void aTestMessageIsOnlyForTheTenantsChannelAdministrator() throws Exception {
        String email = id();
        String disabled = id();
        String chat = id();
        sql("INSERT INTO comms.notification_channel (id, tenant_id, code, row_version) VALUES ('" + email
                + "', :tenant, 'channel.email', 1)", "INSERT INTO comms.notification_channel (id, tenant_id, code,"
                + " is_enabled, row_version) VALUES ('" + disabled + "', :tenant, 'channel.sms', false, 1)",
                "INSERT INTO comms.notification_channel (id, tenant_id, code, row_version) VALUES ('" + chat
                        + "', :tenant, 'channel.whatsapp', 1)");
        String admin = user(UUID.randomUUID(), true);
        String body = "{\"to\":\"me@example.org\"}";

        assertThat(call("POST", "/v1/comms/notification-channels/" + email + ":sendTest", user(UUID.randomUUID(),
                false), body).status()).as("a school-level grant").isEqualTo(403);
        assertThat(call("POST", "/v1/comms/notification-channels/" + disabled + ":sendTest", admin, body).status())
                .isEqualTo(409);
        assertThat(call("POST", "/v1/comms/notification-channels/" + chat + ":sendTest", admin, body).status())
                .isEqualTo(422);
        Reply noIam = call("POST", "/v1/comms/notification-channels/" + email + ":sendTest", admin, body);
        assertThat(noIam.status()).as("the caller's own address comes from iam").isEqualTo(503);
        assertThat(call("POST", "/v1/comms/notification-channels/" + id() + ":sendTest", admin, body).status())
                .isEqualTo(404);
    }

    // ---- greetings from people

    private void deliver(String source, String type, String aggregate, UUID id, String data, EventHandler handler) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), source, type, Optional.of(TENANT), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        assertThat(new InboxProcessor(database, Clock.systemUTC()).deliver(event, handler))
                .isEqualTo(InboxProcessor.Outcome.PROCESSED);
    }

    private static String greeting(UUID greeting, UUID person, String template) {
        return "{\"id\":\"" + greeting + "\",\"personId\":\"" + person + "\",\"templateId\":\"" + template
                + "\",\"channel\":\"email\"}";
    }

    @Test
    void aGreetingReachesTheInboxOfEachAccountCommsKnowsOfThePerson() throws Exception {
        UUID person = UUID.randomUUID();
        UUID account = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        deliver("sis", "sis.student.created", "sis.student", student, "{\"id\":\"" + student + "\",\"userId\":\""
                + account + "\",\"personId\":\"" + person + "\",\"rowVersion\":1}", studentRecords::apply);
        String template = id();
        sql("INSERT INTO comms.message_template (id, tenant_id, channel, name, subject, body, row_version) VALUES ('"
                + template + "', :tenant, 'in_app', 'Birthday', 'Happy birthday!', 'Have a wonderful day.', 1)");
        UUID greeting = UUID.randomUUID();

        deliver("people", "people.greeting.requested", "people.greeting", greeting, greeting(greeting, person,
                template), greetings::deliver);

        JsonNode inbox = inbox(user(account, false)).body().path("data");
        assertThat(inbox).hasSize(1);
        assertThat(inbox.get(0).path("title").asString()).isEqualTo("Happy birthday!");
        assertThat(inbox.get(0).path("body").asString()).isEqualTo("Have a wonderful day.");
        assertThat(inbox.get(0).path("entityType").asString()).isEqualTo("people.greeting");
        assertThat(inbox.get(0).path("entityId").asString()).isEqualTo(greeting.toString());

        UUID staff = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        deliver("people", "people.greeting.requested", "people.greeting", unknown, greeting(unknown, staff, template),
                greetings::deliver);
        assertThat(count("SELECT count(*) FROM comms.in_app_notification WHERE entity_id = '" + unknown + "'"))
                .as("comms holds no account of a staff member or guardian yet").isZero();

        deliver("people", "people.greeting.requested", "people.greeting", greeting, greeting(greeting, person,
                template), greetings::deliver);
        assertThat(count("SELECT count(*) FROM comms.in_app_notification WHERE entity_id = '" + greeting + "'"))
                .as("the same greeting delivered again tells nobody twice").isEqualTo(1);

        UUID missing = UUID.randomUUID();
        deliver("people", "people.greeting.requested", "people.greeting", missing, greeting(missing, person, id()),
                greetings::deliver);
        assertThat(count("SELECT count(*) FROM comms.in_app_notification WHERE entity_id = '" + missing + "'"))
                .isZero();
        assertThat(value("SELECT payload FROM comms.outbox_event WHERE event_type = 'comms.greeting_delivery.rejected'"
                + " AND aggregate_id = '" + missing + "'")).as("people is told to take it back")
                .contains("TEMPLATE_UNAVAILABLE");
        EventEnvelope malformed = new EventEnvelope(UUID.randomUUID(), "people", "people.greeting.requested",
                Optional.of(TENANT), "people.greeting", missing, Instant.now(), "{\"id\":\"" + UUID.randomUUID()
                + "\"}", Optional.of(TRACE));
        assertThatThrownBy(() -> greetings.deliver(malformed)).isInstanceOf(RejectedEventException.class);
    }
}
