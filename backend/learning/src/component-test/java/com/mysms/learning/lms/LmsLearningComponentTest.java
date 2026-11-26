package com.mysms.learning.lms;

import com.mysms.learning.LearningApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
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
 * The learning side of lms against real PostgreSQL as the non-owner role: public reads in the guest projection,
 * course steps and delete, lessons and notes, enrolment and completion, structure moves, release rules, and the
 * operations whose provider is not configured here (503).
 */
@SpringBootTest(classes = LearningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LmsLearningComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SCHOOL = UUID.fromString("0196f000-0000-7000-8000-00000000b001");
    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID OTHER_STUDENT = UUID.randomUUID();
    private static final String AUTHOR = token(TENANT, TEACHER, List.of("lms.*"));
    private static final String LEARNER = token(TENANT, STUDENT, List.of("lms.course.enroll_in_course",
            "lms.lesson.mark_lesson_complete", "lms.lesson.track_video_watch", "lms.lesson_note.*",
            "lms.enrollment.get_student_dashboard", "lms.learning_program.enroll_in_learning_program",
            "lms.chapter.save_scorm_progress", "lms.chapter.get_scorm_launch", "lms.content_item.record_item_view",
            "lms.content_item.get_release_status", "lms.settings.send_contact_message"));
    private static final String OTHER_LEARNER = token(TENANT, OTHER_STUDENT, List.of("lms.lesson_note.*",
            "lms.course.enroll_in_course", "lms.content_item.record_item_view"));

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private static String token(UUID tenant, UUID user, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", permissions);
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private record Reply(int status, JsonNode body) {

        String code() {
            return body.path("code").asString();
        }

        String text(String field) {
            return body.path(field).asString();
        }
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
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
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()));
    }

    private void sql(String... statements) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(TENANT, null);
        CallContextHolder.callAs(context, () -> TestTransactions.of(database).write(() -> {
            for (String statement : statements) {
                database.jdbc().sql(statement).update();
            }
            return null;
        }));
    }

    private int count(String query) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(TENANT, null);
        return CallContextHolder.callAs(context, () -> TestTransactions.of(database).read(() ->
                database.jdbc().sql(query).query(Integer.class).single()));
    }

    /** A course of the school, published (and listed in the catalogue) or a draft, with one chapter. */
    private record Course(UUID id, UUID chapter) {
    }

    private Course course(boolean published, String title) throws Exception {
        Reply created = call("POST", "/v1/lms/courses", AUTHOR, """
                {"title":"%s","institutionId":"%s","authorComments":"internal note","shortIntroduction":"About %s"}
                """.formatted(title, SCHOOL, title));
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        UUID id = UUID.fromString(created.text("id"));
        if (published) {
            sql("UPDATE lms.course SET status = 'approved', lifecycle_status = 'published', is_published = true,"
                    + " is_in_catalog = true, catalog_status = 'published', row_version = row_version + 1"
                    + " WHERE id = '" + id + "'");
        }
        Reply chapter = call("POST", "/v1/lms/courses/" + id + "/chapters", AUTHOR,
                "{\"title\":\"Chapter one\",\"sortOrder\":0}");
        assertThat(chapter.status()).as(chapter.body().toString()).isEqualTo(201);
        return new Course(id, UUID.fromString(chapter.text("id")));
    }

    private UUID lesson(UUID chapter, int sortOrder, boolean preview, String video) throws Exception {
        Reply created = call("POST", "/v1/lms/lessons", AUTHOR, """
                {"chapterId":"%s","title":"Lesson %d","sortOrder":%d,"includeInPreview":%b,"body":"The body",
                 "teacherNotes":"For teachers","content":{"blocks":[{"type":"paragraph","text":"Hi"}]}%s}
                """.formatted(chapter, sortOrder, sortOrder, preview,
                video == null ? "" : ",\"youtubeVideoId\":\"" + video + "\""));
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return UUID.fromString(created.text("id"));
    }

    // ---- the public's reads

    @Test
    void thePublicReadsPublishedCoursesOnlyInTheGuestProjection() throws Exception {
        Course open = course(true, "Open algebra");
        Course draft = course(false, "Draft geometry");
        String stranger = token(TENANT, UUID.randomUUID(), List.of());

        Reply member = call("GET", "/v1/lms/courses/" + draft.id(), AUTHOR, null);
        assertThat(member.status()).isEqualTo(200);
        assertThat(member.text("authorComments")).isEqualTo("internal note");

        Reply seen = call("GET", "/v1/lms/courses/" + open.id(), stranger, null);
        assertThat(seen.status()).isEqualTo(200);
        assertThat(seen.body().has("authorComments")).as("guest projection").isFalse();
        assertThat(seen.body().has("createdBy")).isFalse();
        assertThat(call("GET", "/v1/lms/courses/" + draft.id(), stranger, null).status()).isEqualTo(404);
        List<String> listed = call("GET", "/v1/lms/courses?size=500", stranger, null).body().path("data")
                .findValuesAsString("id");
        assertThat(listed).contains(open.id().toString()).doesNotContain(draft.id().toString());
        assertThat(call("GET", "/v1/lms/courses?size=500", AUTHOR, null).body().path("data").findValuesAsString("id"))
                .contains(open.id().toString(), draft.id().toString());

        assertThat(call("GET", "/v1/lms/courses", null, null).status()).as("a guest who names no school")
                .isEqualTo(404);
        assertThat(call("GET", "/v1/lms/catalog", null, null).status()).isEqualTo(404);
        assertThat(call("GET", "/v1/lms/courses/" + open.id(), token(UUID.randomUUID(), UUID.randomUUID(),
                List.of("lms.*")), null).status()).as("another tenant").isEqualTo(404);

        assertThat(call("GET", "/v1/lms/courses/" + open.id() + "/reviews", stranger, null).status()).isEqualTo(200);
        assertThat(call("GET", "/v1/lms/courses/" + draft.id() + "/previews", stranger, null).status())
                .as("a draft's page is not public").isEqualTo(404);

        Reply outline = call("GET", "/v1/lms/courses/" + open.id() + ":outline", stranger, null);
        assertThat(outline.status()).isEqualTo(200);
        assertThat(outline.body().path("chapters").get(0).path("chapterId").asString())
                .isEqualTo(open.chapter().toString());
        assertThat(outline.body().path("course").has("authorComments")).isFalse();
    }

    @Test
    void theCatalogueListsCoursesInItsWindowWithItsStatisticsAndSettings() throws Exception {
        Course listed = course(true, "Catalogue chemistry");
        Course unlisted = course(true, "Unlisted physics");
        sql("UPDATE lms.course SET is_in_catalog = false, catalog_status = 'draft' WHERE id = '" + unlisted.id() + "'");
        sql("INSERT INTO lms.category (tenant_id, name, code, is_active, row_version) VALUES"
                + " (current_setting('app.tenant_id')::uuid, 'Science', 'sci', true, 1),"
                + " (current_setting('app.tenant_id')::uuid, 'Archived', 'old', false, 1)");
        String stranger = token(TENANT, UUID.randomUUID(), List.of());

        Reply home = call("GET", "/v1/lms/catalog?query=chemistry", stranger, null);
        assertThat(home.status()).as(home.body().toString()).isEqualTo(200);
        assertThat(home.body().path("courses").findValuesAsString("id")).containsExactly(listed.id().toString());
        assertThat(home.body().path("categories").findValuesAsString("code")).contains("sci").doesNotContain("old");
        assertThat(home.body().path("paging").path("page").asInt()).isEqualTo(1);
        assertThat(call("GET", "/v1/lms/catalog?query=physics", stranger, null).body().path("courses")).isEmpty();
        assertThat(call("GET", "/v1/lms/catalog?query=100%25", stranger, null).body().path("courses"))
                .as("LIKE wildcards are taken literally").isEmpty();

        Reply compared = call("GET", "/v1/lms/catalog:compare?courseIds=" + listed.id() + "&courseIds="
                + UUID.randomUUID(), stranger, null);
        assertThat(compared.body().path("data").findValuesAsString("id")).containsExactly(listed.id().toString());

        assertThat(call("GET", "/v1/lms/categories?size=500", stranger, null).body().path("data")
                .findValuesAsString("code")).contains("sci").doesNotContain("old");
        assertThat(call("GET", "/v1/lms/categories?size=500", AUTHOR, null).body().path("data")
                .findValuesAsString("code")).contains("sci", "old");

        Reply statistics = call("GET", "/v1/lms/statistics", stranger, null);
        assertThat(statistics.status()).isEqualTo(200);
        assertThat(statistics.body().path("publishedCourses").asInt()).isGreaterThanOrEqualTo(2);

        Reply settings = call("GET", "/v1/lms/public-settings", stranger, null);
        assertThat(settings.status()).isEqualTo(200);
        assertThat(settings.body().path("allowGuestAccess").asBoolean()).as("the column defaults").isTrue();
        assertThat(settings.text("textDirection")).isEqualTo("auto");
    }

    @Test
    void publishedProgramsArePublicAndAStudentJoinsOnce() throws Exception {
        sql("INSERT INTO lms.learning_program (tenant_id, title, is_published, row_version, id) VALUES"
                + " (current_setting('app.tenant_id')::uuid, 'Data path', true, 1, '0196f000-0000-7000-8000-0000000c0001'),"
                + " (current_setting('app.tenant_id')::uuid, 'Hidden path', false, 1, '0196f000-0000-7000-8000-0000000c0002')");
        String stranger = token(TENANT, UUID.randomUUID(), List.of());
        assertThat(call("GET", "/v1/lms/learning-programs?size=500", stranger, null).body().path("data")
                .findValuesAsString("title")).contains("Data path").doesNotContain("Hidden path");
        assertThat(call("GET", "/v1/lms/learning-programs/0196f000-0000-7000-8000-0000000c0002", stranger, null)
                .status()).isEqualTo(404);

        Reply joined = call("POST", "/v1/lms/learning-programs/0196f000-0000-7000-8000-0000000c0001:enroll", LEARNER,
                "{}");
        assertThat(joined.status()).as(joined.body().toString()).isEqualTo(200);
        assertThat(joined.text("userId")).isEqualTo(STUDENT.toString());
        assertThat(call("POST", "/v1/lms/learning-programs/0196f000-0000-7000-8000-0000000c0001:enroll", LEARNER,
                "{}").status()).isEqualTo(409);
        assertThat(call("POST", "/v1/lms/learning-programs/0196f000-0000-7000-8000-0000000c0002:enroll", LEARNER,
                "{}").status()).as("an unpublished program").isEqualTo(404);
        assertThat(call("GET", "/v1/lms/learning-programs/0196f000-0000-7000-8000-0000000c0001", AUTHOR, null)
                .body().path("memberCount").asInt()).isEqualTo(1);
    }

    // ---- the course's steps and delete

    @Test
    void aCourseIsReviewedBeforeItIsPublishedAndDeletedOnlyWhenNothingHoldsIt() throws Exception {
        Course course = course(false, "Steps biology");
        String base = "/v1/lms/courses/" + course.id();
        assertThat(call("POST", base + ":publish", AUTHOR, "{}").status()).as("not reviewed yet").isEqualTo(409);
        assertThat(call("POST", base + ":submitForReview", AUTHOR, "{}").status()).isEqualTo(200);
        assertThat(call("POST", base + ":approve", AUTHOR, "{}").status()).isEqualTo(200);
        Reply published = call("POST", base + ":publish", AUTHOR, "{}");
        assertThat(published.body().path("isPublished").asBoolean()).isTrue();
        assertThat(published.body().has("publishedOn")).isTrue();
        assertThat(call("POST", base + ":publishCatalogListing", AUTHOR, "{}").body().path("isInCatalog")
                .asBoolean()).isTrue();
        Reply deactivated = call("POST", base + ":deactivate", AUTHOR, "{\"reason\":\"replaced\"}");
        assertThat(deactivated.text("lifecycleStatus")).isEqualTo("deactivated");
        assertThat(call("GET", base, token(TENANT, UUID.randomUUID(), List.of()), null).status())
                .as("a deactivated course leaves the public").isEqualTo(404);
        assertThat(call("POST", base + ":reactivate", AUTHOR, "{}").text("lifecycleStatus")).isEqualTo("published");

        Reply dependencies = call("GET", base + ":dependencies", AUTHOR, null);
        assertThat(dependencies.body().path("canDelete").asBoolean()).isFalse();
        assertThat(dependencies.body().path("blocking").has("published")).isTrue();
        assertThat(dependencies.body().path("blocking").has("lms.chapter")).isTrue();
        String etag = call("GET", base, AUTHOR, null).text("etag");
        assertThat(call("DELETE", base, AUTHOR, null, "If-Match", etag).status()).isEqualTo(409);

        Course empty = course(false, "Empty course");
        String emptyBase = "/v1/lms/courses/" + empty.id();
        String chapterEtag = call("GET", emptyBase + "/chapters/" + empty.chapter(), AUTHOR, null).text("etag");
        assertThat(call("DELETE", emptyBase + "/chapters/" + empty.chapter(), AUTHOR, null, "If-Match", chapterEtag)
                .status()).isEqualTo(204);
        assertThat(call("GET", emptyBase + ":dependencies", AUTHOR, null).body().path("canDelete").asBoolean())
                .isTrue();
        String emptyEtag = call("GET", emptyBase, AUTHOR, null).text("etag");
        assertThat(call("DELETE", emptyBase, AUTHOR, null, "If-Match", emptyEtag).status()).isEqualTo(204);
        assertThat(call("GET", emptyBase, AUTHOR, null).status()).isEqualTo(404);
    }

    // ---- learning

    @Test
    void aStudentEnrolsCompletesLessonsUnderTheRulesAndSeesTheirProgress() throws Exception {
        Course course = course(true, "Learning history");
        UUID video = lesson(course.chapter(), 0, false, "dQw4w9WgXcQ");
        UUID reading = lesson(course.chapter(), 1, true, null);
        String paid = course(true, "Paid art").id().toString();
        sql("UPDATE lms.course SET is_paid = true, price = 10, currency_id = gen_random_uuid() WHERE id = '"
                + paid + "'");

        assertThat(call("POST", "/v1/lms/lessons/" + reading + ":markComplete", LEARNER, "{}").code())
                .isEqualTo("NOT_ENROLLED");
        Reply teaser = call("GET", "/v1/lms/lessons/" + video + ":content", LEARNER, null);
        assertThat(teaser.body().path("canAccess").asBoolean()).as("not enrolled, not a preview").isFalse();
        assertThat(teaser.body().path("lesson").has("body")).isFalse();
        Reply preview = call("GET", "/v1/lms/lessons/" + reading + ":content", LEARNER, null);
        assertThat(preview.body().path("canAccess").asBoolean()).isTrue();
        assertThat(preview.body().path("lesson").has("teacherNotes")).as("never to students").isFalse();
        assertThat(preview.body().path("previousLessonId").asString()).isEqualTo(video.toString());

        Reply enrolled = call("POST", "/v1/lms/courses/" + course.id() + ":enroll", LEARNER, "{}");
        assertThat(enrolled.status()).as(enrolled.body().toString()).isEqualTo(200);
        assertThat(call("POST", "/v1/lms/courses/" + course.id() + ":enroll", LEARNER, "{}").status()).isEqualTo(409);
        assertThat(call("POST", "/v1/lms/courses/" + paid + ":enroll", LEARNER, "{}").code())
                .isEqualTo("PAYMENT_REQUIRED");
        assertThat(call("POST", "/v1/lms/courses/" + course(false, "Draft music").id() + ":enroll", LEARNER, "{}")
                .status()).isEqualTo(404);

        assertThat(call("POST", "/v1/lms/lessons/" + video + ":markComplete", LEARNER, "{}").code())
                .isEqualTo("VIDEO_NOT_WATCHED");
        Reply watched = call("POST", "/v1/lms/lessons/" + video + ":trackVideoWatch", LEARNER,
                "{\"videoSource\":\"dQw4w9WgXcQ\",\"watchSeconds\":\"42.5\"}");
        assertThat(watched.status()).as(watched.body().toString()).isEqualTo(200);
        call("POST", "/v1/lms/lessons/" + video + ":trackVideoWatch", LEARNER,
                "{\"videoSource\":\"dQw4w9WgXcQ\",\"watchSeconds\":\"10\"}");
        assertThat(count("SELECT floor(watch_seconds)::int FROM lms.video_watch WHERE lesson_id = '" + video + "'"))
                .as("the longest time is kept").isEqualTo(42);
        assertThat(call("POST", "/v1/lms/lessons/" + video + ":trackVideoWatch", LEARNER,
                "{\"videoSource\":\"x\",\"watchSeconds\":\"-1\"}").status()).isEqualTo(422);

        Reply done = call("POST", "/v1/lms/lessons/" + video + ":markComplete", LEARNER, "{}");
        assertThat(done.status()).as(done.body().toString()).isEqualTo(200);
        assertThat(done.text("status")).isEqualTo("complete");
        assertThat(call("POST", "/v1/lms/lessons/" + video + ":markComplete", LEARNER, "{}").status())
                .as("a repeat keeps it complete").isEqualTo(200);

        Reply outline = call("GET", "/v1/lms/courses/" + course.id() + ":outline", LEARNER, null);
        assertThat(outline.text("progressPercent")).as("one of two lessons").isEqualTo("50.00");
        assertThat(outline.body().path("chapters").get(0).path("lessons").get(0).path("progressStatus").asString())
                .isEqualTo("complete");
        call("POST", "/v1/lms/lessons/" + reading + ":markComplete", LEARNER, "{}");
        assertThat(count("SELECT progress_percent::int FROM lms.enrollment WHERE course_id = '" + course.id()
                + "' AND user_id = '" + STUDENT + "'")).isEqualTo(100);

        Reply dashboard = call("GET", "/v1/lms/me/dashboard", LEARNER, null);
        assertThat(dashboard.status()).as(dashboard.body().toString()).isEqualTo(200);
        assertThat(dashboard.body().path("streakDays").asInt()).isEqualTo(1);
        assertThat(dashboard.body().path("continueLessonId").asString()).isEqualTo(reading.toString());
        assertThat(dashboard.body().path("enrollments").findValuesAsString("courseId"))
                .contains(course.id().toString());

        Reply distribution = call("GET", "/v1/lms/courses/" + course.id() + ":progressDistribution", AUTHOR, null);
        assertThat(distribution.body().path("data")).hasSize(6);
        assertThat(distribution.body().path("data").get(5).path("students").asInt()).isEqualTo(1);
        assertThat(call("GET", "/v1/lms/courses/" + course.id() + ":progressDistribution", LEARNER, null).status())
                .isEqualTo(403);
    }

    @Test
    void aStudentsNotesAreTheirsAlone() throws Exception {
        Course course = course(true, "Notes literature");
        UUID lesson = lesson(course.chapter(), 0, false, null);
        String notes = "/v1/lms/lessons/" + lesson + "/notes";
        String body = "{\"userId\":\"" + STUDENT + "\",\"color\":\"green\",\"note\":\"Remember this\"}";
        assertThat(call("POST", notes, LEARNER, body).status()).as("not enrolled: the lesson is not theirs to read")
                .isEqualTo(404);
        call("POST", "/v1/lms/courses/" + course.id() + ":enroll", LEARNER, "{}");
        call("POST", "/v1/lms/courses/" + course.id() + ":enroll", OTHER_LEARNER, "{}");
        Reply written = call("POST", notes, LEARNER, body);
        assertThat(written.status()).as(written.body().toString()).isEqualTo(201);
        String note = notes + "/" + written.text("id");
        Reply named = call("POST", notes, LEARNER, "{\"userId\":\"" + OTHER_STUDENT + "\",\"color\":\"red\","
                + "\"note\":\"x\"}");
        assertThat(named.text("userId")).as("the writer is the caller, whatever the body names")
                .isEqualTo(STUDENT.toString());
        assertThat(call("POST", notes, LEARNER, "{\"userId\":\"" + STUDENT + "\",\"color\":\"red\"}").status())
                .as("a note needs text").isEqualTo(422);

        assertThat(call("GET", note, OTHER_LEARNER, null).status()).isEqualTo(404);
        assertThat(call("GET", notes, OTHER_LEARNER, null).body().path("data")).isEmpty();
        assertThat(call("GET", notes, LEARNER, null).body().path("data")).as("both of the caller's notes").hasSize(2);
        assertThat(call("PATCH", note, OTHER_LEARNER, "{\"note\":\"mine now\"}", "If-Match",
                written.text("etag")).status()).isEqualTo(404);
        Reply changed = call("PATCH", note, LEARNER, "{\"note\":\"Remember that\"}", "If-Match", written.text("etag"));
        assertThat(changed.status()).isEqualTo(200);
        assertThat(call("PATCH", note, LEARNER, "{\"note\":\"stale\"}", "If-Match", written.text("etag")).status())
                .isEqualTo(412);
        assertThat(call("DELETE", note, LEARNER, null, "If-Match", changed.text("etag")).status()).isEqualTo(204);
    }

    // ---- the structure editor

    @Test
    void chaptersLessonsAndItemsMoveWithinTheirCourse() throws Exception {
        Course course = course(false, "Structure maths");
        UUID second = UUID.fromString(call("POST", "/v1/lms/courses/" + course.id() + "/chapters", AUTHOR,
                "{\"title\":\"Chapter two\",\"sortOrder\":1}").text("id"));
        Reply reordered = call("POST", "/v1/lms/courses/" + course.id() + ":reorderChapters", AUTHOR,
                "{\"chapterIds\":[\"" + second + "\",\"" + course.chapter() + "\"]}");
        assertThat(reordered.status()).as(reordered.body().toString()).isEqualTo(200);
        assertThat(reordered.body().path("items").findValuesAsString("id"))
                .containsExactly(second.toString(), course.chapter().toString());
        assertThat(call("POST", "/v1/lms/courses/" + course.id() + ":reorderChapters", AUTHOR,
                "{\"chapterIds\":[\"" + second + "\"]}").status()).as("every chapter once").isEqualTo(422);

        Reply nested = call("POST", "/v1/lms/chapters/" + second + ":move", AUTHOR,
                "{\"parentChapterId\":\"" + course.chapter() + "\",\"position\":0}");
        assertThat(nested.status()).as(nested.body().toString()).isEqualTo(200);
        assertThat(call("POST", "/v1/lms/chapters/" + course.chapter() + ":move", AUTHOR,
                "{\"parentChapterId\":\"" + second + "\",\"position\":0}").status())
                .as("not into its own subtree").isEqualTo(422);

        UUID first = lesson(course.chapter(), 0, false, null);
        UUID other = lesson(course.chapter(), 1, false, null);
        Reply moved = call("POST", "/v1/lms/lessons/" + other + ":move", AUTHOR,
                "{\"targetChapterId\":\"" + course.chapter() + "\",\"position\":0}");
        assertThat(moved.body().path("sortOrder").asInt()).isEqualTo(0);
        assertThat(call("GET", "/v1/lms/lessons/" + first, AUTHOR, null).body().path("sortOrder").asInt())
                .as("made room").isEqualTo(1);
        Course elsewhere = course(false, "Elsewhere");
        assertThat(call("POST", "/v1/lms/lessons/" + other + ":move", AUTHOR,
                "{\"targetChapterId\":\"" + elsewhere.chapter() + "\",\"position\":0}").status()).isEqualTo(422);
    }

    @Test
    void releaseRulesAndItemsServeTheirOwnSteps() throws Exception {
        Course course = course(true, "Release design");
        UUID lessonId = lesson(course.chapter(), 0, false, null);
        Reply item = call("POST", "/v1/lms/content-items", AUTHOR, """
                {"chapterId":"%s","courseId":"%s","itemKind":"lesson","lessonId":"%s","title":"Item"}
                """.formatted(course.chapter(), course.id(), lessonId));
        assertThat(item.status()).as(item.body().toString()).isEqualTo(201);
        String itemId = item.text("id");
        UUID gated = lesson(course.chapter(), 1, false, null);
        Reply target = call("POST", "/v1/lms/content-items", AUTHOR, """
                {"chapterId":"%s","courseId":"%s","itemKind":"lesson","lessonId":"%s","title":"Gated"}
                """.formatted(course.chapter(), course.id(), gated));
        String targetId = target.text("id");
        Reply rule = call("POST", "/v1/lms/release-rules", AUTHOR,
                "{\"courseId\":\"" + course.id() + "\",\"sourceContentItemId\":\"" + itemId + "\"}");
        assertThat(rule.status()).as(rule.body().toString()).isEqualTo(201);
        String ruleId = rule.text("id");
        sql("INSERT INTO lms.release_rule_target (tenant_id, release_rule_id, content_item_id, row_version) VALUES"
                + " (current_setting('app.tenant_id')::uuid, '" + ruleId + "', '" + targetId + "', 1)",
                "INSERT INTO lms.release_rule_condition (tenant_id, release_rule_id, operator, condition_type,"
                + " row_version) VALUES (current_setting('app.tenant_id')::uuid, '" + ruleId + "', '=', 'completed', 1)");
        call("POST", "/v1/lms/courses/" + course.id() + ":enroll", LEARNER, "{}");

        Reply held = call("GET", "/v1/lms/content-items/" + targetId + ":releaseStatus", LEARNER, null);
        assertThat(held.status()).as(held.body().toString()).isEqualTo(200);
        assertThat(held.body().path("isReleased").asBoolean()).isFalse();
        assertThat(held.body().path("pendingConditions").get(0).asString()).isEqualTo("completed =");
        Reply granted = call("POST", "/v1/lms/content-items/" + targetId + ":grantAccess", AUTHOR,
                "{\"userIds\":[\"" + STUDENT + "\"],\"grant\":true}");
        assertThat(granted.body().path("items")).hasSize(1);
        assertThat(call("GET", "/v1/lms/content-items/" + targetId + ":releaseStatus", LEARNER, null).body()
                .path("isReleased").asBoolean()).isTrue();
        assertThat(call("GET", "/v1/lms/content-items/" + targetId + ":releaseStatus?userId=" + STUDENT, LEARNER,
                null).status()).as("naming oneself is asking for oneself").isEqualTo(200);
        assertThat(call("GET", "/v1/lms/content-items/" + targetId + ":releaseStatus?userId=" + OTHER_STUDENT,
                LEARNER, null).status()).isEqualTo(403);

        Reply disabled = call("POST", "/v1/lms/content-items/" + itemId + ":setReleaseRulesEnabled", AUTHOR,
                "{\"enabled\":false}");
        assertThat(disabled.body().path("items").get(0).path("isDisabled").asBoolean()).isTrue();
        Reply copies = call("POST", "/v1/lms/release-rules/" + ruleId + ":copyTo", AUTHOR,
                "{\"sourceContentItemIds\":[\"" + targetId + "\"]}");
        assertThat(copies.body().path("items").get(0).path("copiedFromId").asString()).isEqualTo(ruleId);
        assertThat(count("SELECT count(*) FROM lms.release_rule_condition c JOIN lms.release_rule r ON"
                + " r.tenant_id = c.tenant_id AND r.id = c.release_rule_id WHERE r.copied_from_id = '" + ruleId + "'"))
                .as("the conditions come along").isEqualTo(1);
        Reply reordered = call("POST", "/v1/lms/content-items/" + itemId + ":reorderReleaseRules", AUTHOR,
                "{\"releaseRuleIds\":[\"" + ruleId + "\"]}");
        assertThat(reordered.status()).isEqualTo(200);

        Reply viewed = call("POST", "/v1/lms/content-items/" + itemId + ":recordView", LEARNER, "{\"liked\":true}");
        assertThat(viewed.status()).as(viewed.body().toString()).isEqualTo(200);
        call("POST", "/v1/lms/content-items/" + itemId + ":recordView", LEARNER, "{\"liked\":true}");
        Reply again = call("GET", "/v1/lms/content-items/" + itemId, AUTHOR, null);
        assertThat(again.text("viewCount")).isEqualTo("2");
        assertThat(again.text("likeCount")).as("one like per student").isEqualTo("1");
        assertThat(call("POST", "/v1/lms/content-items/" + itemId + ":recordView", OTHER_LEARNER, "{}").status())
                .as("not following the course").isEqualTo(404);

        assertThat(call("POST", "/v1/lms/content-items/" + itemId + ":setVisibility", AUTHOR,
                "{\"isActive\":false}").code()).as("a template item").isEqualTo("NOT_COHORT_ITEM");
        Reply movedItem = call("POST", "/v1/lms/content-items/" + targetId + ":move", AUTHOR,
                "{\"targetChapterId\":\"" + course.chapter() + "\",\"position\":0}");
        assertThat(movedItem.body().path("sortOrder").asInt()).isEqualTo(0);
    }

    // ---- providers this test does not configure

    @Test
    void scormAndContactNeedTheirProviders() throws Exception {
        Course course = course(true, "Scorm safety");
        sql("UPDATE lms.chapter SET is_scorm_package = true, scorm_package_path = 'pkg/1',"
                + " scorm_launch_file = 'index.html' WHERE id = '" + course.chapter() + "'");
        String chapter = "/v1/lms/chapters/" + course.chapter();
        assertThat(call("GET", chapter + ":scormLaunch", LEARNER, null).status()).as("not enrolled").isEqualTo(404);
        call("POST", "/v1/lms/courses/" + course.id() + ":enroll", LEARNER, "{}");
        Reply launch = call("GET", chapter + ":scormLaunch", LEARNER, null);
        assertThat(launch.status()).isEqualTo(503);
        assertThat(launch.code()).isEqualTo("NOT_CONFIGURED");
        Reply saved = call("POST", chapter + ":saveScormProgress", LEARNER,
                "{\"status\":\"partially_complete\",\"runtimeState\":{\"suspendData\":\"abc\",\"location\":\"p2\","
                        + "\"score\":\"80.5\",\"totalTimeSeconds\":120}}");
        assertThat(saved.status()).as(saved.body().toString()).isEqualTo(200);
        assertThat(saved.text("scormLocation")).isEqualTo("p2");
        assertThat(call("POST", chapter + ":saveScormProgress", LEARNER,
                "{\"status\":\"complete\",\"runtimeState\":{\"totalTimeSeconds\":1.5}}").status()).isEqualTo(422);

        Reply contact = call("POST", "/v1/lms/contact", LEARNER, "{\"subject\":\"Hello\",\"content\":\"A question\"}");
        assertThat(contact.status()).isEqualTo(503);
        assertThat(contact.code()).isEqualTo("NOT_CONFIGURED");
    }
}
