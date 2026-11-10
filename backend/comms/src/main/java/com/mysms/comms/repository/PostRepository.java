package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.PostEntity;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
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
 * Plain-JDBC access to comms.post (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class PostRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("classPageId", "r.class_page_id", ValueType.UUID),
            field("authorUserId", "r.author_user_id", ValueType.UUID),
            field("body", "r.body", ValueType.STRING),
            field("visibility", "r.visibility", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("scheduledFor", "r.scheduled_for", ValueType.TIMESTAMP),
            field("postedAt", "r.posted_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PostEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PostEntity::getCreatedAt),
            Map.entry("updateTime", PostEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.class_page_id, r.author_user_id, r.body,
                   r.visibility, r.status, r.scheduled_for, r.posted_at, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM comms.post r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.post
            (id, tenant_id, institution_id, academic_year_id, class_page_id, author_user_id, body, visibility,
             scheduled_for, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :classPageId, :authorUserId, :body,
                    COALESCE(:visibility, 'all_classes'::character varying), :scheduledFor, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, class_page_id, author_user_id, body, visibility,
                      status, scheduled_for, posted_at, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.post
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.post
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   class_page_id = :classPageId,
                   author_user_id = :authorUserId,
                   body = :body,
                   visibility = :visibility,
                   scheduled_for = :scheduledFor,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CLASS_PAGE_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.class_page WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_POST_AUDIENCE_CLASS_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.post_audience_class WHERE tenant_id = :tenantId
                AND post_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_POST_COMMENT_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.post_comment WHERE tenant_id = :tenantId
                AND post_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_REACTION_POST_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.reaction WHERE tenant_id = :tenantId
                AND post_id = :id AND deleted_at IS NULL)
            """;

    /** comms.ApprovePost: allowed while status is pending. */
    private static final String TRANSITION_APPROVE_POST = """
            UPDATE comms.post
               SET status = 'posted',
                   posted_at = :now,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** comms.CancelPost: allowed while status is draft or pending or posted. */
    private static final String TRANSITION_CANCEL_POST = """
            UPDATE comms.post
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public PostRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PostEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PostRepository::map).optional();
    }

    public Page<PostEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PostEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PostRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PostEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PostEntity insert(PostEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("classPageId", e.getClassPageId())
                .param("authorUserId", e.getAuthorUserId())
                .param("body", e.getBody())
                .param("visibility", e.getVisibility())
                .param("scheduledFor", e.getScheduledFor())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PostRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PostEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("classPageId", e.getClassPageId())
                .param("authorUserId", e.getAuthorUserId())
                .param("body", e.getBody())
                .param("visibility", e.getVisibility())
                .param("scheduledFor", e.getScheduledFor())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PostEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced comms.class_page row exists (live). */
    public boolean existsClassPageId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CLASS_PAGE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live comms.post_audience_class rows still reference the row. */
    public boolean referencedByPostAudienceClassPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_POST_AUDIENCE_CLASS_POST_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live comms.post_comment rows still reference the row. */
    public boolean referencedByPostCommentPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_POST_COMMENT_POST_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live comms.reaction rows still reference the row. */
    public boolean referencedByReactionPostId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_REACTION_POST_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** comms.ApprovePost; 0 rows means the row changed (or vanished) since it was read. */
    public int approvePost(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_APPROVE_POST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** comms.CancelPost; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelPost(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_POST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, PostEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PostEntity map(ResultSet rs, int row) throws SQLException {
        return PostEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .classPageId(rs.getObject("class_page_id", UUID.class))
                .authorUserId(rs.getObject("author_user_id", UUID.class))
                .body(rs.getString("body"))
                .visibility(rs.getString("visibility"))
                .status(rs.getString("status"))
                .scheduledFor(rs.getObject("scheduled_for", OffsetDateTime.class))
                .postedAt(rs.getObject("posted_at", OffsetDateTime.class))
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
