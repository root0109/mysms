package com.mysms.lms;

import static com.mysms.lms.LmsFixtures.expect;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.lms.LmsFixtures.Reply;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * E-book curation and the materials a student is shown, and the restore of a cohort archive, over HTTP against real
 * PostgreSQL: a teacher's curation orders and switches on the recommendations, a student sees them in that order,
 * personal rankings are read by their student or an e-book officer, and GetJobPackage / RestoreCohortArchive guard
 * their jobs' states.
 */
@SpringBootTest(classes = LmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EbookCurationComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(LmsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    private LmsFixtures fx;
    private UUID tenant;
    private UUID institution;
    private String admin;
    private UUID ebook;
    private UUID first;
    private UUID second;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "lms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    @BeforeEach
    void school() {
        fx = new LmsFixtures(TOKENS, database, port);
        tenant = UUID.randomUUID();
        institution = UUID.randomUUID();
        admin = fx.admin(tenant);
        ebook = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.ebook (id, tenant_id, institution_id, title) "
                + "VALUES (:id, :tenantId, :institution, 'Algebra')", "id", ebook, "institution", institution);
        first = material("https://a.example.com");
        second = material("https://b.example.com");
    }

    private UUID material(String url) {
        UUID material = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.related_material (id, tenant_id, ebook_id, url) "
                + "VALUES (:id, :tenantId, :ebook, :url)", "id", material, "ebook", ebook, "url", url);
        return material;
    }

    @Test
    void curationOrdersTheMaterialsAndStudentsSeeThemInThatOrder() throws Exception {
        String curate = "/v1/lms/ebooks/" + ebook + ":publishCuration";
        Reply curated = expect(fx.call("POST", curate, admin,
                "{\"relatedMaterialIds\":[\"" + second + "\",\"" + first + "\"]}"), 200);
        assertThat(curated.body().path("items")).hasSize(2);
        assertThat(fx.value(tenant, "SELECT show_related_materials FROM lms.ebook WHERE id = :id", Boolean.class,
                "id", ebook)).isTrue();
        String student = fx.member(tenant, institution, "lms.ebook.get_my_related_materials");
        Reply mine = expect(fx.call("GET", "/v1/lms/ebooks/" + ebook + ":myRelatedMaterials", student, null), 200);
        assertThat(mine.body().path("data")).hasSize(2);
        assertThat(mine.body().path("data").get(0).path("id").asString()).isEqualTo(second.toString());
        assertThat(mine.body().path("data").get(1).path("id").asString()).isEqualTo(first.toString());

        expect(fx.call("POST", curate, admin, "{\"relatedMaterialIds\":[\"" + UUID.randomUUID() + "\"]}"), 422);
        expect(fx.call("POST", curate, fx.member(tenant, institution, "lms.ebook.read"),
                "{\"relatedMaterialIds\":[]}"), 403);
        expect(fx.call("POST", "/v1/lms/ebooks/" + UUID.randomUUID() + ":publishCuration", admin,
                "{\"relatedMaterialIds\":[]}"), 404);
        expect(fx.call("POST", curate, fx.admin(UUID.randomUUID()), "{\"relatedMaterialIds\":[]}"), 404);
    }

    @Test
    void personalRankingsAreReadByTheirStudentOrAnEbookOfficer() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID rank = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.related_material_rank (id, tenant_id, user_id, ebook_id) "
                + "VALUES (:id, :tenantId, :user, :ebook)", "id", rank, "user", studentId, "ebook", ebook);
        String owner = fx.user(studentId, tenant, institution, "lms.related_material_rank.read");
        String other = fx.member(tenant, institution, "lms.related_material_rank.read");
        String base = "/v1/lms/ebooks/" + ebook + "/related-material-ranks";
        assertThat(expect(fx.call("GET", base, owner, null), 200).body().path("data")).hasSize(1);
        assertThat(expect(fx.call("GET", base, other, null), 200).body().path("data")).isEmpty();
        assertThat(expect(fx.call("GET", base, admin, null), 200).body().path("data")).hasSize(1);
        expect(fx.call("GET", base + "/" + rank, owner, null), 200);
        expect(fx.call("GET", base + "/" + rank, other, null), 404);
        expect(fx.call("GET", base + "/" + rank, admin, null), 200);
    }

    @Test
    void aFinishedCohortArchiveIsRestoredOnce() throws Exception {
        UUID cohort = fx.cohort(tenant, institution);
        fx.sql(tenant, "UPDATE lms.cohort SET lifecycle_status = 'archived' WHERE id = :id", "id", cohort);
        UUID job = UUID.randomUUID();
        fx.sql(tenant, "INSERT INTO lms.import_export_job (id, tenant_id, institution_id, direction, package_format, "
                        + "scope, cohort_id, requested_by, requested_on, status) VALUES (:id, :tenantId, :institution, "
                        + "'export', 'cohort_archive', 'cohort', :cohort, :by, current_date, 'complete')",
                "id", job, "institution", institution, "cohort", cohort, "by", UUID.randomUUID());
        String restore = "/v1/lms/import-export-jobs/" + job + ":restore";
        expect(fx.call("POST", restore, fx.member(tenant, institution, "lms.import_export_job.read"), "{}"), 403);
        Reply restored = expect(fx.call("POST", restore, admin, "{}"), 200);
        assertThat(restored.body().path("unarchiveStatus").asString()).isEqualTo("complete");
        assertThat(fx.value(tenant, "SELECT lifecycle_status FROM lms.cohort WHERE id = :id", String.class,
                "id", cohort)).isEqualTo("completed");
        expect(fx.call("POST", restore, admin, "{}"), 409);
        expect(fx.call("POST", "/v1/lms/import-export-jobs/" + UUID.randomUUID() + ":restore", admin, "{}"), 404);

        // the package link: no package on this job (409); a job with one stops at the file store (503)
        expect(fx.call("GET", "/v1/lms/import-export-jobs/" + job + ":package", admin, null), 409);
        fx.sql(tenant, "UPDATE lms.import_export_job SET package_file_id = :file WHERE id = :id",
                "file", UUID.randomUUID(), "id", job);
        expect(fx.call("GET", "/v1/lms/import-export-jobs/" + job + ":package", admin, null), 503);
    }
}
