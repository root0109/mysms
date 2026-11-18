package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access of the e-book workflows: the teacher's curation of its related materials (their initial rank and
 * the e-book's seed list), the ordered materials a student is shown, the read-only personal rankings
 * (lms.related_material_rank with its items), and the e-book's conversion state.
 */
@Repository
public class EbookCurationRepository {

    /** The e-book as its workflows need it. */
    public record EbookHead(UUID id, UUID institutionId, String title, UUID sourceFileId, UUID fileId,
                            String conversionJobRef, boolean showRelatedMaterials, int rowVersion) {
    }

    /** One material in the order it is shown, with its position. */
    public record Shown(UUID id, int position) {
    }

    /** One personal ranking, with its materials in rank order as comma-separated ids. */
    public record RankRow(UUID id, UUID userId, UUID ebookId, UUID courseId, UUID fileId, String rankedMaterialIds,
                          String subjectTags, OffsetDateTime createdAt, UUID createdBy, OffsetDateTime updatedAt,
                          UUID updatedBy, int rowVersion) {
    }

    /** Ranking order: newest first. */
    public static final Map<String, FieldSpec> RANK_SORT_FIELDS = Map.of(
            "createTime", new FieldSpec("createTime", "r.created_at", ValueType.TIMESTAMP));
    public static final Map<String, FieldSpec> RANK_FILTER_FIELDS = Map.of(
            "userId", new FieldSpec("userId", "r.user_id", ValueType.UUID),
            "courseId", new FieldSpec("courseId", "r.course_id", ValueType.UUID),
            "fileId", new FieldSpec("fileId", "r.file_id", ValueType.UUID));
    /** Shown order: the position, then the id. */
    public static final Map<String, FieldSpec> SHOWN_SORT_FIELDS = Map.of(
            "position", new FieldSpec("position", "pos", ValueType.INTEGER));

    private static final String HEAD = """
            SELECT id, institution_id, title, source_file_id, file_id, conversion_job_ref, show_related_materials,
                   row_version
              FROM lms.ebook WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String HEAD_FOR_UPDATE = HEAD + " FOR UPDATE";
    private static final String HEAD_BY_JOB = """
            SELECT id, institution_id, title, source_file_id, file_id, conversion_job_ref, show_related_materials,
                   row_version
              FROM lms.ebook WHERE tenant_id = :tenantId AND conversion_job_ref = :job AND deleted_at IS NULL
               FOR UPDATE
            """;
    private static final String MATERIALS = """
            SELECT id FROM lms.related_material
             WHERE tenant_id = :tenantId AND ebook_id = :ebookId AND deleted_at IS NULL
            """;
    private static final String CLEAR_RANKS = """
            UPDATE lms.related_material SET initial_rank = NULL, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND ebook_id = :ebookId AND initial_rank IS NOT NULL AND deleted_at IS NULL
            """;
    private static final String SET_RANK = """
            UPDATE lms.related_material SET initial_rank = :rank, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String SET_SEED = """
            UPDATE lms.ebook SET related_material_seed = :seed, show_related_materials = :show, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String COHORT_INSTITUTION = """
            SELECT institution_id FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String COHORT_EXISTS = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String MY_RANK = """
            SELECT id FROM lms.related_material_rank
             WHERE tenant_id = :tenantId AND user_id = :userId AND ebook_id = :ebookId
               AND course_id IS NOT DISTINCT FROM :courseId AND file_id IS NOT DISTINCT FROM :fileId
               AND deleted_at IS NULL
             ORDER BY updated_at DESC, id DESC LIMIT 1
            """;
    private static final String RANKED = """
            SELECT * FROM (SELECT m.id, i.position AS pos FROM lms.related_material_rank_item i
                             JOIN lms.related_material m ON m.id = i.related_material_id AND m.tenant_id = :tenantId
                                  AND m.deleted_at IS NULL
                            WHERE i.tenant_id = :tenantId AND i.related_material_rank_id = :rankId
                              AND i.deleted_at IS NULL) r
            """;
    private static final String CURATED = """
            SELECT * FROM (SELECT m.id, COALESCE(m.initial_rank, 2147483647) AS pos FROM lms.related_material m
                            WHERE m.tenant_id = :tenantId AND m.ebook_id = :ebookId AND m.deleted_at IS NULL
                              AND (m.initial_rank IS NOT NULL OR NOT EXISTS (
                                   SELECT 1 FROM lms.related_material c WHERE c.tenant_id = :tenantId
                                      AND c.ebook_id = m.ebook_id AND c.initial_rank IS NOT NULL
                                      AND c.deleted_at IS NULL))) r
            """;
    private static final String RANK_SELECT = """
            SELECT r.id, r.user_id, r.ebook_id, r.course_id, r.file_id, r.subject_tags, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.row_version,
                   COALESCE((SELECT string_agg(CAST(i.related_material_id AS text), ',' ORDER BY i.position)
                               FROM lms.related_material_rank_item i
                              WHERE i.tenant_id = :tenantId AND i.related_material_rank_id = r.id
                                AND i.deleted_at IS NULL), '') AS ranked_material_ids
              FROM lms.related_material_rank r
             WHERE r.tenant_id = :tenantId AND r.ebook_id = :ebookId AND r.deleted_at IS NULL
            """;
    private static final String RANK_BY_ID = RANK_SELECT + " AND r.id = :id";
    private static final String OWN_RANKS = "r.user_id = :rankViewer";
    private static final String SET_CONVERSION = """
            UPDATE lms.ebook SET conversion_job_ref = :job, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String CONVERTED = """
            UPDATE lms.ebook SET file_id = :fileId, conversion_job_ref = NULL, updated_at = :now,
                   updated_by = NULL, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public EbookCurationRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<EbookHead> head(UUID id) {
        return jdbc.sql(HEAD).param("id", id).query(EbookCurationRepository::head).optional();
    }

    public Optional<EbookHead> lock(UUID id) {
        return jdbc.sql(HEAD_FOR_UPDATE).param("id", id).query(EbookCurationRepository::head).optional();
    }

    /** The e-book waiting for the conversion job, locked. */
    public Optional<EbookHead> lockByJob(String job) {
        return jdbc.sql(HEAD_BY_JOB).param("job", job).query(EbookCurationRepository::head).optional();
    }

    public List<UUID> materials(UUID ebookId) {
        return jdbc.sql(MATERIALS).param("ebookId", ebookId).query(UUID.class).list();
    }

    /** Sets the curation: the given materials ranked 1..n in that order, every other material unranked. */
    public void curate(UUID ebookId, List<UUID> ordered, OffsetDateTime now, UUID actor) {
        jdbc.sql(CLEAR_RANKS).param("ebookId", ebookId).param("now", now).param("actor", actor).update();
        for (int i = 0; i < ordered.size(); i++) {
            jdbc.sql(SET_RANK).param("id", ordered.get(i)).param("rank", i + 1).param("now", now)
                    .param("actor", actor).update();
        }
    }

    public int setSeed(UUID id, int expected, String seed, boolean show, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_SEED).param("id", id).param("expected", expected).param("seed", seed).param("show", show)
                .param("now", now).param("actor", actor).update();
    }

    public boolean cohortExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(COHORT_EXISTS).param("id", id).query(Boolean.class).single());
    }

    public Optional<UUID> cohortInstitution(UUID id) {
        return jdbc.sql(COHORT_INSTITUTION).param("id", id).query(UUID.class).optional();
    }

    public Optional<UUID> myRank(UUID userId, UUID ebookId, UUID courseId, UUID fileId) {
        return jdbc.sql(MY_RANK).param("userId", userId).param("ebookId", ebookId).param("courseId", courseId)
                .param("fileId", fileId).query(UUID.class).optional();
    }

    /** The materials shown, in order: the given personal ranking's, else the curation (all, when none is curated). */
    public Page<Shown> shown(PageRequest page, UUID ebookId, Optional<UUID> rankId) {
        SqlFragment keyset = page.keysetPredicate();
        String sql = (rankId.isPresent() ? RANKED : CURATED) + " WHERE " + keyset.sql() + " ORDER BY "
                + page.orderByClause() + page.limitClause();
        var statement = jdbc.sql(sql).params(keyset.params()).params(page.limitParams());
        statement = rankId.isPresent() ? statement.param("rankId", rankId.get()) : statement.param("ebookId", ebookId);
        List<Shown> rows = statement.query((rs, n) -> new Shown(rs.getObject("id", UUID.class), rs.getInt("pos")))
                .list();
        return page.toPage(rows, r -> List.of(r.position()), Shown::id);
    }

    /**
     * The e-book's personal rankings: all of them for {@code everyone}, else only the viewer's own.
     */
    public Page<RankRow> ranks(PageRequest page, UUID ebookId, SqlFragment filter, boolean everyone, UUID viewer) {
        SqlFragment where = filter.and(page.keysetPredicate());
        if (!everyone) {
            where = where.and(new SqlFragment(OWN_RANKS, Map.of("rankViewer", viewer)));
        }
        String sql = RANK_SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RankRow> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams()).param("ebookId", ebookId)
                .query(EbookCurationRepository::rank).list();
        return page.toPage(rows, r -> List.of(r.createdAt()), RankRow::id);
    }

    public Optional<RankRow> rank(UUID ebookId, UUID id) {
        return jdbc.sql(RANK_BY_ID).param("ebookId", ebookId).param("id", id).query(EbookCurationRepository::rank)
                .optional();
    }

    public int startConversion(UUID id, int expected, String job, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_CONVERSION).param("id", id).param("expected", expected).param("job", job)
                .param("now", now).param("actor", actor).update();
    }

    public int converted(UUID id, int expected, UUID fileId, OffsetDateTime now) {
        return jdbc.sql(CONVERTED).param("id", id).param("expected", expected).param("fileId", fileId)
                .param("now", now).update();
    }

    private static EbookHead head(ResultSet rs, int n) throws SQLException {
        return new EbookHead(rs.getObject("id", UUID.class), rs.getObject("institution_id", UUID.class),
                rs.getString("title"), rs.getObject("source_file_id", UUID.class),
                rs.getObject("file_id", UUID.class), rs.getString("conversion_job_ref"),
                rs.getBoolean("show_related_materials"), rs.getInt("row_version"));
    }

    private static RankRow rank(ResultSet rs, int n) throws SQLException {
        return new RankRow(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("ebook_id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getObject("file_id", UUID.class), rs.getString("ranked_material_ids"),
                rs.getString("subject_tags"), rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("created_by", UUID.class), rs.getObject("updated_at", OffsetDateTime.class),
                rs.getObject("updated_by", UUID.class), rs.getInt("row_version"));
    }
}
