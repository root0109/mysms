package com.mysms.learning.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.ReleaseRuleEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Plain-JDBC access to lms.release_rule: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ReleaseRuleRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("sourceContentItemId", "r.source_content_item_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("stopIfTrue", "r.stop_if_true", ValueType.BOOLEAN),
            field("isDisabled", "r.is_disabled", ValueType.BOOLEAN),
            field("reassignSource", "r.reassign_source", ValueType.BOOLEAN),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("copiedFromId", "r.copied_from_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ReleaseRuleEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ReleaseRuleEntity::getCreatedAt),
            Map.entry("updateTime", ReleaseRuleEntity::getUpdatedAt),
            Map.entry("sortOrder", ReleaseRuleEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.course_id, r.cohort_id, r.source_content_item_id, r.title, r.stop_if_true,
                   r.is_disabled, r.reassign_source, r.sort_order, r.copied_from_id, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.release_rule r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** lms.EnableReleaseRule: allowed while the rule is disabled. */
    private static final String STEP_ENABLE = """
            UPDATE lms.release_rule
               SET is_disabled = false, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** lms.DisableReleaseRule: allowed while the rule is enabled. */
    private static final String STEP_DISABLE = """
            UPDATE lms.release_rule
               SET is_disabled = true, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** The rules whose source is an item, in evaluation order. */
    private static final String OF_SOURCE = SELECT_LIVE
            + " AND r.source_content_item_id = :itemId ORDER BY r.sort_order, r.id";
    /** The enabled rules that release an item, or the chapter it is in. */
    private static final String RELEASING = SELECT_LIVE + """
             AND NOT r.is_disabled
             AND EXISTS (SELECT 1 FROM lms.release_rule_target t WHERE t.tenant_id = :tenantId
                            AND t.release_rule_id = r.id AND t.deleted_at IS NULL
                            AND (t.content_item_id = :itemId OR t.chapter_id = :chapterId))
            ORDER BY r.sort_order, r.id
            """;
    private static final String SET_ENABLED_FOR_SOURCE = """
            UPDATE lms.release_rule
               SET is_disabled = :disabled, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND source_content_item_id = :itemId AND deleted_at IS NULL
               AND is_disabled <> :disabled
            """;
    private static final String PLACE = """
            UPDATE lms.release_rule
               SET sort_order = :sortOrder, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String INSERT = """
            INSERT INTO lms.release_rule
            (id, tenant_id, course_id, cohort_id, source_content_item_id, title, stop_if_true, is_disabled,
             reassign_source, sort_order, copied_from_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :cohortId, :sourceContentItemId, :title, COALESCE(:stopIfTrue, false),
                    COALESCE(:isDisabled, false), COALESCE(:reassignSource, false), COALESCE(:sortOrder, 0),
                    :copiedFromId, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, course_id, cohort_id, source_content_item_id, title, stop_if_true, is_disabled,
                      reassign_source, sort_order, copied_from_id, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.release_rule
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.release_rule
               SET course_id = :courseId,
                   cohort_id = :cohortId,
                   source_content_item_id = :sourceContentItemId,
                   title = :title,
                   stop_if_true = :stopIfTrue,
                   is_disabled = :isDisabled,
                   reassign_source = :reassignSource,
                   sort_order = :sortOrder,
                   copied_from_id = :copiedFromId,
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
            SELECT EXISTS (SELECT 1 FROM lms.release_rule WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_SOURCE_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_RULE_COPIED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule WHERE tenant_id = :tenantId
                AND copied_from_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_RULE_CONDITION_RELEASE_RULE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule_condition WHERE tenant_id = :tenantId
                AND release_rule_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_RULE_TARGET_RELEASE_RULE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule_target WHERE tenant_id = :tenantId
                AND release_rule_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.course_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public ReleaseRuleRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Enables (true) or disables the rule under its version; 0 rows means it changed since it was read. */
    public int setEnabled(UUID id, boolean enabled, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(enabled ? STEP_ENABLE : STEP_DISABLE).param("id", id).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    /** The item's rules (it is their source), in evaluation order. */
    public List<ReleaseRuleEntity> ofSource(UUID itemId) {
        return jdbc.sql(OF_SOURCE).param("itemId", itemId).query(ReleaseRuleRepository::map).list();
    }

    /** The enabled rules that release the item or its chapter, in evaluation order. */
    public List<ReleaseRuleEntity> releasing(UUID itemId, UUID chapterId) {
        return jdbc.sql(RELEASING).param("itemId", itemId).param("chapterId", chapterId)
                .query(ReleaseRuleRepository::map).list();
    }

    /** Enables or disables every rule whose source is the item; answers how many changed. */
    public int setEnabledForSource(UUID itemId, boolean enabled, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_ENABLED_FOR_SOURCE).param("itemId", itemId).param("disabled", !enabled).param("now", now)
                .param("actor", actor).update();
    }

    /** Puts the rules in the listed order (0, 1, 2…). */
    public void place(List<UUID> ids, OffsetDateTime now, UUID actor) {
        for (int i = 0; i < ids.size(); i++) {
            jdbc.sql(PLACE).param("id", ids.get(i)).param("sortOrder", i).param("now", now).param("actor", actor)
                    .update();
        }
    }

    public Optional<ReleaseRuleEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ReleaseRuleRepository::map).optional();
    }

    public Page<ReleaseRuleEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ReleaseRuleEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ReleaseRuleRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ReleaseRuleEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ReleaseRuleEntity insert(ReleaseRuleEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("courseId", e.getCourseId())
                .param("cohortId", e.getCohortId())
                .param("sourceContentItemId", e.getSourceContentItemId())
                .param("title", e.getTitle())
                .param("stopIfTrue", e.getStopIfTrue())
                .param("isDisabled", e.getIsDisabled())
                .param("reassignSource", e.getReassignSource())
                .param("sortOrder", e.getSortOrder())
                .param("copiedFromId", e.getCopiedFromId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ReleaseRuleRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ReleaseRuleEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("courseId", e.getCourseId())
                .param("cohortId", e.getCohortId())
                .param("sourceContentItemId", e.getSourceContentItemId())
                .param("title", e.getTitle())
                .param("stopIfTrue", e.getStopIfTrue())
                .param("isDisabled", e.getIsDisabled())
                .param("reassignSource", e.getReassignSource())
                .param("sortOrder", e.getSortOrder())
                .param("copiedFromId", e.getCopiedFromId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ReleaseRuleEntity e, int expectedVersion) {
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

    /** Whether the referenced lms.release_rule row exists (live). */
    public boolean existsCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COPIED_FROM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsSourceContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SOURCE_CONTENT_ITEM_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.release_rule rows still reference the row. */
    public boolean referencedByReleaseRuleCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_RULE_COPIED_FROM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_rule_condition rows still reference the row. */
    public boolean referencedByReleaseRuleConditionReleaseRuleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_RULE_CONDITION_RELEASE_RULE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_rule_target rows still reference the row. */
    public boolean referencedByReleaseRuleTargetReleaseRuleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_RULE_TARGET_RELEASE_RULE_ID).param("id", id)
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

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static final String OF_COURSE = SELECT_LIVE + " AND r.course_id = :courseId ORDER BY r.sort_order";

    /** The course template's live rules, in order. */
    public List<ReleaseRuleEntity> ofCourse(UUID courseId) {
        return jdbc.sql(OF_COURSE).param("courseId", courseId).query(ReleaseRuleRepository::map).list();
    }

    private static List<Object> sortValues(SortSpec sort, ReleaseRuleEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ReleaseRuleEntity map(ResultSet rs, int row) throws SQLException {
        return ReleaseRuleEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .sourceContentItemId(rs.getObject("source_content_item_id", UUID.class))
                .title(rs.getString("title"))
                .stopIfTrue(rs.getObject("stop_if_true", Boolean.class))
                .isDisabled(rs.getObject("is_disabled", Boolean.class))
                .reassignSource(rs.getObject("reassign_source", Boolean.class))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .copiedFromId(rs.getObject("copied_from_id", UUID.class))
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
