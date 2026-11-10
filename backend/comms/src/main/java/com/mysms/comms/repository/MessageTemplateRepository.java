package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.MessageTemplateEntity;
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
 * Plain-JDBC access to comms.message_template (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class MessageTemplateRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("channel", "r.channel", ValueType.STRING),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("subject", "r.subject", ValueType.STRING),
            field("body", "r.body", ValueType.STRING),
            field("externalTemplateId", "r.external_template_id", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<MessageTemplateEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", MessageTemplateEntity::getCreatedAt),
            Map.entry("updateTime", MessageTemplateEntity::getUpdatedAt),
            Map.entry("name", MessageTemplateEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.channel, r.code, r.name, r.subject, r.body,
                   r.external_template_id, r.is_active, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM comms.message_template r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.message_template
            (id, tenant_id, institution_id, channel, code, name, subject, body, external_template_id, is_active,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :channel, :code, :name, :subject, :body, :externalTemplateId,
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, channel, code, name, subject, body, external_template_id,
                      is_active, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.message_template
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.message_template
               SET institution_id = :institutionId,
                   channel = :channel,
                   code = :code,
                   name = :name,
                   subject = :subject,
                   body = :body,
                   external_template_id = :externalTemplateId,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_OUTBOUND_MESSAGE_TEMPLATE_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.outbound_message WHERE tenant_id = :tenantId
                AND template_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public MessageTemplateRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<MessageTemplateEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(MessageTemplateRepository::map).optional();
    }

    public Page<MessageTemplateEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<MessageTemplateEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(MessageTemplateRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), MessageTemplateEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public MessageTemplateEntity insert(MessageTemplateEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("channel", e.getChannel())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("subject", e.getSubject())
                .param("body", e.getBody())
                .param("externalTemplateId", e.getExternalTemplateId())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(MessageTemplateRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(MessageTemplateEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("channel", e.getChannel())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("subject", e.getSubject())
                .param("body", e.getBody())
                .param("externalTemplateId", e.getExternalTemplateId())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(MessageTemplateEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live comms.outbound_message rows still reference the row. */
    public boolean referencedByOutboundMessageTemplateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_OUTBOUND_MESSAGE_TEMPLATE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, MessageTemplateEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static MessageTemplateEntity map(ResultSet rs, int row) throws SQLException {
        return MessageTemplateEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .channel(rs.getString("channel"))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .subject(rs.getString("subject"))
                .body(rs.getString("body"))
                .externalTemplateId(rs.getString("external_template_id"))
                .isActive(rs.getObject("is_active", Boolean.class))
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
