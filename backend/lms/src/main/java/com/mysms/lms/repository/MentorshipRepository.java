package com.mysms.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.MentorshipEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to lms.mentorship (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class MentorshipRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("mentorUserId", "r.mentor_user_id", ValueType.UUID),
            field("menteeUserId", "r.mentee_user_id", ValueType.UUID),
            field("validFrom", "r.valid_from", ValueType.DATE),
            field("validTo", "r.valid_to", ValueType.DATE),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<MentorshipEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", MentorshipEntity::getCreatedAt),
            Map.entry("updateTime", MentorshipEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.mentor_user_id, r.mentee_user_id, r.valid_from, r.valid_to, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.mentorship r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.mentorship
            (id, tenant_id, mentor_user_id, mentee_user_id, valid_from, valid_to, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :mentorUserId, :menteeUserId, :validFrom, :validTo, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, mentor_user_id, mentee_user_id, valid_from, valid_to, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.mentorship
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.mentorship
               SET mentor_user_id = :mentorUserId,
                   mentee_user_id = :menteeUserId,
                   valid_from = :validFrom,
                   valid_to = :validTo,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String IX_MENTORSHIP_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.mentorship
                            WHERE tenant_id = :tenantId
                              AND mentor_user_id = :mentorUserId AND mentee_user_id = :menteeUserId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;

    private final JdbcClient jdbc;

    public MentorshipRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<MentorshipEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(MentorshipRepository::map).optional();
    }

    public Page<MentorshipEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<MentorshipEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(MentorshipRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), MentorshipEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public MentorshipEntity insert(MentorshipEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("mentorUserId", e.getMentorUserId())
                .param("menteeUserId", e.getMenteeUserId())
                .param("validFrom", e.getValidFrom())
                .param("validTo", e.getValidTo())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(MentorshipRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(MentorshipEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("mentorUserId", e.getMentorUserId())
                .param("menteeUserId", e.getMenteeUserId())
                .param("validFrom", e.getValidFrom())
                .param("validTo", e.getValidTo())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(MentorshipEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** ix_mentorship_1: mentor_user_id, mentee_user_id where (deleted_at IS NULL). */
    public boolean takenMentorUserIdMenteeUserId1(MentorshipEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(IX_MENTORSHIP_1_TAKEN)
                .param("mentorUserId", e.getMentorUserId())
                .param("menteeUserId", e.getMenteeUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, MentorshipEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static MentorshipEntity map(ResultSet rs, int row) throws SQLException {
        return MentorshipEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .mentorUserId(rs.getObject("mentor_user_id", UUID.class))
                .menteeUserId(rs.getObject("mentee_user_id", UUID.class))
                .validFrom(rs.getObject("valid_from", LocalDate.class))
                .validTo(rs.getObject("valid_to", LocalDate.class))
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
