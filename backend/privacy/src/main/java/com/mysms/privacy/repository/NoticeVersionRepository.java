package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.NoticeVersionEntity;
import com.mysms.privacy.model.GrievanceOfficer;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain-JDBC access to privacy.notice_version (no soft delete: versions are kept as consent evidence). The grievance
 * contact is jsonb, written through {@code CAST(:x AS jsonb)} and read back as JSON text; the purpose codes are a
 * varchar array. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class NoticeVersionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("version", "r.version", ValueType.INTEGER),
            field("status", "r.status", ValueType.STRING),
            field("requiresReconsent", "r.requires_reconsent", ValueType.BOOLEAN),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("version", "r.version", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "version";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<NoticeVersionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("version", NoticeVersionEntity::getVersion),
            Map.entry("createTime", NoticeVersionEntity::getCreatedAt),
            Map.entry("updateTime", NoticeVersionEntity::getUpdatedAt));

    private static final String SELECT = """
            SELECT r.id, r.tenant_id, r.notice_id, r.version, r.status, r.requires_reconsent, r.purpose_codes,
                   r.grievance_contact::text AS grievance_contact, r.published_at, r.superseded_by_id,
                   r.document_file_id, r.created_at, r.created_by, r.updated_at, r.updated_by, r.row_version
              FROM privacy.notice_version r
             WHERE r.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND r.id = :id";
    private static final String FIND_ACTIVE = SELECT + " AND r.notice_id = :noticeId AND r.status = 'active'";
    /** The latest publication of a version asking everyone again (a retired one still counts: its ask stands). */
    private static final String RECONSENT_SINCE = """
            SELECT r.published_at
              FROM privacy.notice_version r
             WHERE r.tenant_id = :tenantId
               AND r.notice_id = :noticeId AND r.requires_reconsent AND r.status <> 'draft'
             ORDER BY r.published_at DESC
             LIMIT 1
            """;
    /** The live notices among those named that have an active version, with that version. */
    private static final String ACTIVE_NOTICES = """
            SELECT n.id AS notice_id, n.code, r.id
              FROM privacy.notice_version r
              JOIN privacy.notice n ON n.tenant_id = :tenantId AND n.id = r.notice_id
             WHERE r.tenant_id = :tenantId
               AND r.notice_id = ANY(CAST(:noticeIds AS uuid[])) AND r.status = 'active' AND n.deleted_at IS NULL
            """;
    private static final String NEXT_VERSION = """
            SELECT COALESCE(MAX(version), 0) + 1 FROM privacy.notice_version
             WHERE tenant_id = :tenantId AND notice_id = :noticeId
            """;
    private static final String INSERT = """
            INSERT INTO privacy.notice_version
            (id, tenant_id, notice_id, version, status, requires_reconsent, purpose_codes, grievance_contact,
             document_file_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :noticeId, :version, :status, :requiresReconsent, CAST(:purposeCodes AS varchar[]),
                    CAST(:grievanceContact AS jsonb), :documentFileId, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, notice_id, version, status, requires_reconsent, purpose_codes,
                      grievance_contact::text AS grievance_contact, published_at, superseded_by_id, document_file_id,
                      created_at, created_by, updated_at, updated_by, row_version
            """;
    private static final String UPDATE = """
            UPDATE privacy.notice_version
               SET requires_reconsent = :requiresReconsent,
                   purpose_codes = CAST(:purposeCodes AS varchar[]),
                   grievance_contact = CAST(:grievanceContact AS jsonb),
                   document_file_id = :documentFileId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'draft'
            """;
    private static final String PUBLISH = """
            UPDATE privacy.notice_version
               SET status = 'active',
                   published_at = :publishedAt,
                   updated_at = :publishedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'draft'
            """;
    private static final String RETIRE = """
            UPDATE privacy.notice_version
               SET status = 'retired',
                   superseded_by_id = :supersededById,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'active'
            """;
    private static final String ACTIVE_PURPOSE_CODES = """
            SELECT p.code FROM privacy.purpose p WHERE p.status = 'active' AND p.code = ANY(CAST(:codes AS varchar[]))
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public NoticeVersionRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper json) {
        this.jdbc = database.jdbc();
        this.json = json;
    }

    public Optional<NoticeVersionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(this::map).optional();
    }

    /** The notice's active version, if one has been published. */
    public Optional<NoticeVersionEntity> findActive(UUID noticeId) {
        return jdbc.sql(FIND_ACTIVE).param("noticeId", noticeId).query(this::map).optional();
    }

    /**
     * When the notice last published a version that requires consent again: a grant recorded before then no longer
     * counts. Empty when no published version of the notice has asked.
     */
    public Optional<OffsetDateTime> reconsentSince(UUID noticeId) {
        return jdbc.sql(RECONSENT_SINCE).param("noticeId", noticeId)
                .query((rs, row) -> rs.getObject("published_at", OffsetDateTime.class)).optional();
    }

    /**
     * The notices among {@code noticeIds} a decision can be recorded under — live, with an active version — by notice
     * id: what the consent centre needs to open a purpose's notice.
     */
    public Map<UUID, ActiveNotice> activeNotices(Collection<UUID> noticeIds) {
        return jdbc.sql(ACTIVE_NOTICES).param("noticeIds", noticeIds.toArray(UUID[]::new))
                .query((rs, row) -> new ActiveNotice(rs.getObject("notice_id", UUID.class), rs.getString("code"),
                        rs.getObject("id", UUID.class)))
                .list().stream().collect(Collectors.toMap(ActiveNotice::noticeId, Function.identity()));
    }

    public Page<NoticeVersionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<NoticeVersionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(this::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), NoticeVersionEntity::getId);
    }

    /** The number the notice's next version takes; uq_notice_version_number refuses a concurrent duplicate. */
    public int nextVersion(UUID noticeId) {
        return jdbc.sql(NEXT_VERSION).param("noticeId", noticeId).query(Integer.class).single();
    }

    /** Inserts the row and returns it as stored. */
    public NoticeVersionEntity insert(NoticeVersionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("noticeId", e.getNoticeId())
                .param("version", e.getVersion())
                .param("status", e.getStatus())
                .param("requiresReconsent", e.getRequiresReconsent())
                .param("purposeCodes", e.getPurposeCodes().toArray(String[]::new))
                .param("grievanceContact", json.writeValueAsString(e.getGrievanceContact()))
                .param("documentFileId", e.getDocumentFileId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(this::map).single();
    }

    /** Updates a draft under the ETag guard; 0 rows means the row changed (or was published) since it was read. */
    public int update(NoticeVersionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("requiresReconsent", e.getRequiresReconsent())
                .param("purposeCodes", e.getPurposeCodes().toArray(String[]::new))
                .param("grievanceContact", json.writeValueAsString(e.getGrievanceContact()))
                .param("documentFileId", e.getDocumentFileId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** draft → active under the ETag guard. */
    public int publish(UUID id, int expectedVersion, OffsetDateTime publishedAt, UUID actor) {
        return jdbc.sql(PUBLISH)
                .param("id", id)
                .param("publishedAt", publishedAt)
                .param("updatedBy", actor)
                .param("expected", expectedVersion)
                .update();
    }

    /** active → retired, naming the version that supersedes it, under the ETag guard. */
    public int retire(UUID id, int expectedVersion, UUID supersededById, OffsetDateTime at, UUID actor) {
        return jdbc.sql(RETIRE)
                .param("id", id)
                .param("supersededById", supersededById)
                .param("updatedAt", at)
                .param("updatedBy", actor)
                .param("expected", expectedVersion)
                .update();
    }

    /** The given codes that name active purposes of the catalogue (privacy.purpose, global). */
    public Set<String> activePurposeCodes(List<String> codes) {
        return Set.copyOf(jdbc.sql(ACTIVE_PURPOSE_CODES).param("codes", codes.toArray(String[]::new))
                .query(String.class).list());
    }

    /** Children of the path's parent notice. */
    public static SqlFragment ofParent(UUID noticeId) {
        return new SqlFragment("r.notice_id = :parentId", Map.of("parentId", noticeId));
    }

    private static List<Object> sortValues(SortSpec sort, NoticeVersionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private NoticeVersionEntity map(ResultSet rs, int row) throws SQLException {
        return NoticeVersionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .noticeId(rs.getObject("notice_id", UUID.class))
                .version(rs.getObject("version", Integer.class))
                .status(rs.getString("status"))
                .requiresReconsent(rs.getObject("requires_reconsent", Boolean.class))
                .purposeCodes(strings(rs.getArray("purpose_codes")))
                .grievanceContact(json.readValue(rs.getString("grievance_contact"), GrievanceOfficer.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .supersededById(rs.getObject("superseded_by_id", UUID.class))
                .documentFileId(rs.getObject("document_file_id", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static List<String> strings(Array array) throws SQLException {
        return List.of((String[]) array.getArray());
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }

    /**
     * A notice a decision can be recorded under.
     *
     * @param noticeId   the notice
     * @param code       its code within the tenant (what GetMyNotice takes)
     * @param versionId  its active version
     */
    public record ActiveNotice(UUID noticeId, String code, UUID versionId) {
    }
}
