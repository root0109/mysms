package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.NoticeEntity;
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
 * Plain-JDBC access to comms.notice: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class NoticeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("audience", "r.audience", ValueType.STRING),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("backgroundId", "r.background_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("body", "r.body", ValueType.STRING),
            field("attachmentFileId", "r.attachment_file_id", ValueType.UUID),
            field("publishAt", "r.publish_at", ValueType.TIMESTAMP),
            field("expireAt", "r.expire_at", ValueType.TIMESTAMP),
            field("isPublished", "r.is_published", ValueType.BOOLEAN),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("showInCalendar", "r.show_in_calendar", ValueType.BOOLEAN),
            field("allowReplies", "r.allow_replies", ValueType.BOOLEAN),
            field("discussionTopicId", "r.discussion_topic_id", ValueType.UUID),
            field("isClosed", "r.is_closed", ValueType.BOOLEAN),
            field("isDeactivated", "r.is_deactivated", ValueType.BOOLEAN),
            field("authorComment", "r.author_comment", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<NoticeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", NoticeEntity::getCreatedAt),
            Map.entry("updateTime", NoticeEntity::getUpdatedAt),
            Map.entry("title", NoticeEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.audience, r.class_section_id,
                   r.background_id, r.title, r.body, r.attachment_file_id, r.publish_at, r.expire_at, r.is_published,
                   r.cohort_id, r.show_in_calendar, r.allow_replies, r.discussion_topic_id, r.is_closed,
                   r.is_deactivated, r.author_comment, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM comms.notice r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.notice
            (id, tenant_id, institution_id, academic_year_id, audience, class_section_id, background_id, title, body,
             attachment_file_id, publish_at, expire_at, cohort_id, show_in_calendar, allow_replies,
             discussion_topic_id, is_closed, is_deactivated, author_comment, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :audience, :classSectionId, :backgroundId,
                    :title, :body, :attachmentFileId, :publishAt, :expireAt, :cohortId,
                    COALESCE(:showInCalendar, false), COALESCE(:allowReplies, false), :discussionTopicId,
                    COALESCE(:isClosed, false), COALESCE(:isDeactivated, false), :authorComment, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, audience, class_section_id, background_id, title,
                      body, attachment_file_id, publish_at, expire_at, is_published, cohort_id, show_in_calendar,
                      allow_replies, discussion_topic_id, is_closed, is_deactivated, author_comment, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.notice
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.notice
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   audience = :audience,
                   class_section_id = :classSectionId,
                   background_id = :backgroundId,
                   title = :title,
                   body = :body,
                   attachment_file_id = :attachmentFileId,
                   publish_at = :publishAt,
                   expire_at = :expireAt,
                   cohort_id = :cohortId,
                   show_in_calendar = :showInCalendar,
                   allow_replies = :allowReplies,
                   discussion_topic_id = :discussionTopicId,
                   is_closed = :isClosed,
                   is_deactivated = :isDeactivated,
                   author_comment = :authorComment,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_BACKGROUND_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.notice_background WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_NOTICE_AUDIENCE_NOTICE_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.notice_audience WHERE tenant_id = :tenantId
                AND notice_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_NOTICE_VERSION_NOTICE_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.notice_version WHERE tenant_id = :tenantId
                AND notice_id = :id AND deleted_at IS NULL)
            """;

    /** comms.PublishNotice: allowed while is_published is false and is_closed is false. */
    private static final String TRANSITION_PUBLISH_NOTICE = """
            UPDATE comms.notice
               SET is_published = true,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** comms.UnpublishNotice: allowed while is_published is true. */
    private static final String TRANSITION_UNPUBLISH_NOTICE = """
            UPDATE comms.notice
               SET is_published = false,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** comms.DeactivateNotice: allowed while is_closed is false. */
    private static final String TRANSITION_DEACTIVATE_NOTICE = """
            UPDATE comms.notice
               SET is_closed = true,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** comms.ReactivateNotice: allowed while is_closed is true. */
    private static final String TRANSITION_REACTIVATE_NOTICE = """
            UPDATE comms.notice
               SET is_closed = false,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public NoticeRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<NoticeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(NoticeRepository::map).optional();
    }

    public Page<NoticeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<NoticeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(NoticeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), NoticeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public NoticeEntity insert(NoticeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("audience", e.getAudience())
                .param("classSectionId", e.getClassSectionId())
                .param("backgroundId", e.getBackgroundId())
                .param("title", e.getTitle())
                .param("body", e.getBody())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("publishAt", e.getPublishAt())
                .param("expireAt", e.getExpireAt())
                .param("cohortId", e.getCohortId())
                .param("showInCalendar", e.getShowInCalendar())
                .param("allowReplies", e.getAllowReplies())
                .param("discussionTopicId", e.getDiscussionTopicId())
                .param("isClosed", e.getIsClosed())
                .param("isDeactivated", e.getIsDeactivated())
                .param("authorComment", e.getAuthorComment())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(NoticeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(NoticeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("audience", e.getAudience())
                .param("classSectionId", e.getClassSectionId())
                .param("backgroundId", e.getBackgroundId())
                .param("title", e.getTitle())
                .param("body", e.getBody())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("publishAt", e.getPublishAt())
                .param("expireAt", e.getExpireAt())
                .param("cohortId", e.getCohortId())
                .param("showInCalendar", e.getShowInCalendar())
                .param("allowReplies", e.getAllowReplies())
                .param("discussionTopicId", e.getDiscussionTopicId())
                .param("isClosed", e.getIsClosed())
                .param("isDeactivated", e.getIsDeactivated())
                .param("authorComment", e.getAuthorComment())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(NoticeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced comms.notice_background row exists (live). */
    public boolean existsBackgroundId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_BACKGROUND_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live comms.notice_audience rows still reference the row. */
    public boolean referencedByNoticeAudienceNoticeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_NOTICE_AUDIENCE_NOTICE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live comms.notice_version rows still reference the row. */
    public boolean referencedByNoticeVersionNoticeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_NOTICE_VERSION_NOTICE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** comms.PublishNotice; 0 rows means the row changed (or vanished) since it was read. */
    public int publishNotice(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_PUBLISH_NOTICE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** comms.UnpublishNotice; 0 rows means the row changed (or vanished) since it was read. */
    public int unpublishNotice(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_UNPUBLISH_NOTICE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** comms.DeactivateNotice; 0 rows means the row changed (or vanished) since it was read. */
    public int deactivateNotice(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_DEACTIVATE_NOTICE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** comms.ReactivateNotice; 0 rows means the row changed (or vanished) since it was read. */
    public int reactivateNotice(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REACTIVATE_NOTICE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, NoticeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static NoticeEntity map(ResultSet rs, int row) throws SQLException {
        return NoticeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .audience(rs.getString("audience"))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .backgroundId(rs.getObject("background_id", UUID.class))
                .title(rs.getString("title"))
                .body(rs.getString("body"))
                .attachmentFileId(rs.getObject("attachment_file_id", UUID.class))
                .publishAt(rs.getObject("publish_at", OffsetDateTime.class))
                .expireAt(rs.getObject("expire_at", OffsetDateTime.class))
                .isPublished(rs.getObject("is_published", Boolean.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .showInCalendar(rs.getObject("show_in_calendar", Boolean.class))
                .allowReplies(rs.getObject("allow_replies", Boolean.class))
                .discussionTopicId(rs.getObject("discussion_topic_id", UUID.class))
                .isClosed(rs.getObject("is_closed", Boolean.class))
                .isDeactivated(rs.getObject("is_deactivated", Boolean.class))
                .authorComment(rs.getString("author_comment"))
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
