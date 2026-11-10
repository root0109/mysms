package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.ChapterEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to lms.chapter (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class ChapterRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("isScormPackage", "r.is_scorm_package", ValueType.BOOLEAN),
            field("scormPackageFileId", "r.scorm_package_file_id", ValueType.UUID),
            field("scormPackagePath", "r.scorm_package_path", ValueType.STRING),
            field("scormManifestFile", "r.scorm_manifest_file", ValueType.STRING),
            field("scormLaunchFile", "r.scorm_launch_file", ValueType.STRING),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("parentChapterId", "r.parent_chapter_id", ValueType.UUID),
            field("description", "r.description", ValueType.STRING),
            field("chapterNumber", "r.chapter_number", ValueType.INTEGER),
            field("startDay", "r.start_day", ValueType.INTEGER),
            field("dueDay", "r.due_day", ValueType.INTEGER),
            field("startsAt", "r.starts_at", ValueType.TIMESTAMP),
            field("endsAt", "r.ends_at", ValueType.TIMESTAMP),
            field("allowDelete", "r.allow_delete", ValueType.BOOLEAN),
            field("allowEditDetails", "r.allow_edit_details", ValueType.BOOLEAN),
            field("allowEditDates", "r.allow_edit_dates", ValueType.BOOLEAN),
            field("copiedFromId", "r.copied_from_id", ValueType.UUID),
            field("isOriginal", "r.is_original", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ChapterEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ChapterEntity::getCreatedAt),
            Map.entry("updateTime", ChapterEntity::getUpdatedAt),
            Map.entry("title", ChapterEntity::getTitle),
            Map.entry("sortOrder", ChapterEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.course_id, r.title, r.sort_order, r.is_scorm_package, r.scorm_package_file_id,
                   r.scorm_package_path, r.scorm_manifest_file, r.scorm_launch_file, r.cohort_id, r.parent_chapter_id,
                   r.description, r.chapter_number, r.start_day, r.start_time, r.due_day, r.due_time, r.starts_at,
                   r.ends_at, r.allow_delete, r.allow_edit_details, r.allow_edit_dates, r.copied_from_id,
                   r.is_original, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.chapter r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.chapter
            (id, tenant_id, course_id, title, sort_order, is_scorm_package, scorm_package_file_id,
             scorm_package_path, scorm_manifest_file, scorm_launch_file, cohort_id, parent_chapter_id, description,
             chapter_number, start_day, start_time, due_day, due_time, starts_at, ends_at, allow_delete,
             allow_edit_details, allow_edit_dates, copied_from_id, is_original, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :title, :sortOrder, COALESCE(:isScormPackage, false),
                    :scormPackageFileId, :scormPackagePath, :scormManifestFile, :scormLaunchFile, :cohortId,
                    :parentChapterId, :description, :chapterNumber, :startDay, :startTime, :dueDay, :dueTime,
                    :startsAt, :endsAt, COALESCE(:allowDelete, true), COALESCE(:allowEditDetails, true),
                    COALESCE(:allowEditDates, true), :copiedFromId, COALESCE(:isOriginal, true), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, course_id, title, sort_order, is_scorm_package, scorm_package_file_id,
                      scorm_package_path, scorm_manifest_file, scorm_launch_file, cohort_id, parent_chapter_id,
                      description, chapter_number, start_day, start_time, due_day, due_time, starts_at, ends_at,
                      allow_delete, allow_edit_details, allow_edit_dates, copied_from_id, is_original, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.chapter
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.chapter
               SET title = :title,
                   sort_order = :sortOrder,
                   is_scorm_package = :isScormPackage,
                   scorm_package_file_id = :scormPackageFileId,
                   scorm_package_path = :scormPackagePath,
                   scorm_manifest_file = :scormManifestFile,
                   scorm_launch_file = :scormLaunchFile,
                   cohort_id = :cohortId,
                   parent_chapter_id = :parentChapterId,
                   description = :description,
                   chapter_number = :chapterNumber,
                   start_day = :startDay,
                   start_time = :startTime,
                   due_day = :dueDay,
                   due_time = :dueTime,
                   starts_at = :startsAt,
                   ends_at = :endsAt,
                   allow_delete = :allowDelete,
                   allow_edit_details = :allowEditDetails,
                   allow_edit_dates = :allowEditDates,
                   copied_from_id = :copiedFromId,
                   is_original = :isOriginal,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COPIED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PARENT_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CHAPTER_COPIED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId
                AND copied_from_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CHAPTER_PARENT_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId
                AND parent_chapter_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_IMPORT_EXPORT_JOB_TARGET_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.import_export_job WHERE tenant_id = :tenantId
                AND target_chapter_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LEARNING_OBJECTIVE_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.learning_objective WHERE tenant_id = :tenantId
                AND chapter_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LESSON_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson WHERE tenant_id = :tenantId
                AND chapter_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LESSON_PROGRESS_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson_progress WHERE tenant_id = :tenantId
                AND chapter_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_GRANT_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_grant WHERE tenant_id = :tenantId
                AND chapter_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_RULE_TARGET_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule_target WHERE tenant_id = :tenantId
                AND chapter_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public ChapterRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ChapterEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ChapterRepository::map).optional();
    }

    public Page<ChapterEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ChapterEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ChapterRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ChapterEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ChapterEntity insert(ChapterEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("courseId", e.getCourseId())
                .param("title", e.getTitle())
                .param("sortOrder", e.getSortOrder())
                .param("isScormPackage", e.getIsScormPackage())
                .param("scormPackageFileId", e.getScormPackageFileId())
                .param("scormPackagePath", e.getScormPackagePath())
                .param("scormManifestFile", e.getScormManifestFile())
                .param("scormLaunchFile", e.getScormLaunchFile())
                .param("cohortId", e.getCohortId())
                .param("parentChapterId", e.getParentChapterId())
                .param("description", e.getDescription())
                .param("chapterNumber", e.getChapterNumber())
                .param("startDay", e.getStartDay())
                .param("startTime", e.getStartTime())
                .param("dueDay", e.getDueDay())
                .param("dueTime", e.getDueTime())
                .param("startsAt", e.getStartsAt())
                .param("endsAt", e.getEndsAt())
                .param("allowDelete", e.getAllowDelete())
                .param("allowEditDetails", e.getAllowEditDetails())
                .param("allowEditDates", e.getAllowEditDates())
                .param("copiedFromId", e.getCopiedFromId())
                .param("isOriginal", e.getIsOriginal())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ChapterRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ChapterEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("title", e.getTitle())
                .param("sortOrder", e.getSortOrder())
                .param("isScormPackage", e.getIsScormPackage())
                .param("scormPackageFileId", e.getScormPackageFileId())
                .param("scormPackagePath", e.getScormPackagePath())
                .param("scormManifestFile", e.getScormManifestFile())
                .param("scormLaunchFile", e.getScormLaunchFile())
                .param("cohortId", e.getCohortId())
                .param("parentChapterId", e.getParentChapterId())
                .param("description", e.getDescription())
                .param("chapterNumber", e.getChapterNumber())
                .param("startDay", e.getStartDay())
                .param("startTime", e.getStartTime())
                .param("dueDay", e.getDueDay())
                .param("dueTime", e.getDueTime())
                .param("startsAt", e.getStartsAt())
                .param("endsAt", e.getEndsAt())
                .param("allowDelete", e.getAllowDelete())
                .param("allowEditDetails", e.getAllowEditDetails())
                .param("allowEditDates", e.getAllowEditDates())
                .param("copiedFromId", e.getCopiedFromId())
                .param("isOriginal", e.getIsOriginal())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ChapterEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.chapter row exists (live). */
    public boolean existsCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COPIED_FROM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.chapter row exists (live). */
    public boolean existsParentChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PARENT_CHAPTER_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.chapter rows still reference the row. */
    public boolean referencedByChapterCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CHAPTER_COPIED_FROM_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.chapter rows still reference the row. */
    public boolean referencedByChapterParentChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CHAPTER_PARENT_CHAPTER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.import_export_job rows still reference the row. */
    public boolean referencedByImportExportJobTargetChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_IMPORT_EXPORT_JOB_TARGET_CHAPTER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.learning_objective rows still reference the row. */
    public boolean referencedByLearningObjectiveChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LEARNING_OBJECTIVE_CHAPTER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.lesson rows still reference the row. */
    public boolean referencedByLessonChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LESSON_CHAPTER_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.lesson_progress rows still reference the row. */
    public boolean referencedByLessonProgressChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LESSON_PROGRESS_CHAPTER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_grant rows still reference the row. */
    public boolean referencedByReleaseGrantChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_GRANT_CHAPTER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_rule_target rows still reference the row. */
    public boolean referencedByReleaseRuleTargetChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_RULE_TARGET_CHAPTER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through course_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.course_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ChapterEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ChapterEntity map(ResultSet rs, int row) throws SQLException {
        return ChapterEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .title(rs.getString("title"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .isScormPackage(rs.getObject("is_scorm_package", Boolean.class))
                .scormPackageFileId(rs.getObject("scorm_package_file_id", UUID.class))
                .scormPackagePath(rs.getString("scorm_package_path"))
                .scormManifestFile(rs.getString("scorm_manifest_file"))
                .scormLaunchFile(rs.getString("scorm_launch_file"))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .parentChapterId(rs.getObject("parent_chapter_id", UUID.class))
                .description(rs.getString("description"))
                .chapterNumber(rs.getObject("chapter_number", Integer.class))
                .startDay(rs.getObject("start_day", Integer.class))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .dueDay(rs.getObject("due_day", Integer.class))
                .dueTime(rs.getObject("due_time", LocalTime.class))
                .startsAt(rs.getObject("starts_at", OffsetDateTime.class))
                .endsAt(rs.getObject("ends_at", OffsetDateTime.class))
                .allowDelete(rs.getObject("allow_delete", Boolean.class))
                .allowEditDetails(rs.getObject("allow_edit_details", Boolean.class))
                .allowEditDates(rs.getObject("allow_edit_dates", Boolean.class))
                .copiedFromId(rs.getObject("copied_from_id", UUID.class))
                .isOriginal(rs.getObject("is_original", Boolean.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
