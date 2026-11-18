package com.mysms.frontoffice.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.frontoffice.domain.CallLogEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to frontoffice.call_log: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CallLogRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("direction", "r.direction", ValueType.STRING),
            field("purpose", "r.purpose", ValueType.STRING),
            field("callerName", "r.caller_name", ValueType.STRING),
            field("incomingNumber", "r.incoming_number", ValueType.STRING),
            field("outgoingNumber", "r.outgoing_number", ValueType.STRING),
            field("callDate", "r.call_date", ValueType.DATE),
            field("description", "r.description", ValueType.STRING),
            field("loggedByUserId", "r.logged_by_user_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CallLogEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CallLogEntity::getCreatedAt),
            Map.entry("updateTime", CallLogEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.direction, r.purpose, r.caller_name,
                   r.incoming_number, r.outgoing_number, r.call_date, r.start_time, r.end_time, r.duration_seconds,
                   r.description, r.logged_by_user_id, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM frontoffice.call_log r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO frontoffice.call_log
            (id, tenant_id, institution_id, academic_year_id, direction, purpose, caller_name, incoming_number,
             outgoing_number, call_date, start_time, end_time, duration_seconds, description, logged_by_user_id,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :direction, :purpose, :callerName,
                    :incomingNumber, :outgoingNumber, :callDate, :startTime, :endTime, :durationSeconds,
                    :description, :loggedByUserId, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, direction, purpose, caller_name, incoming_number,
                      outgoing_number, call_date, start_time, end_time, duration_seconds, description,
                      logged_by_user_id, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE frontoffice.call_log
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE frontoffice.call_log
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   direction = :direction,
                   purpose = :purpose,
                   caller_name = :callerName,
                   incoming_number = :incomingNumber,
                   outgoing_number = :outgoingNumber,
                   call_date = :callDate,
                   start_time = :startTime,
                   end_time = :endTime,
                   duration_seconds = :durationSeconds,
                   description = :description,
                   logged_by_user_id = :loggedByUserId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public CallLogRepository(@Qualifier(FrontofficeDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CallLogEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CallLogRepository::map).optional();
    }

    public Page<CallLogEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CallLogEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CallLogRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CallLogEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CallLogEntity insert(CallLogEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("direction", e.getDirection())
                .param("purpose", e.getPurpose())
                .param("callerName", e.getCallerName())
                .param("incomingNumber", e.getIncomingNumber())
                .param("outgoingNumber", e.getOutgoingNumber())
                .param("callDate", e.getCallDate())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("durationSeconds", e.getDurationSeconds())
                .param("description", e.getDescription())
                .param("loggedByUserId", e.getLoggedByUserId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CallLogRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CallLogEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("direction", e.getDirection())
                .param("purpose", e.getPurpose())
                .param("callerName", e.getCallerName())
                .param("incomingNumber", e.getIncomingNumber())
                .param("outgoingNumber", e.getOutgoingNumber())
                .param("callDate", e.getCallDate())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("durationSeconds", e.getDurationSeconds())
                .param("description", e.getDescription())
                .param("loggedByUserId", e.getLoggedByUserId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CallLogEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, CallLogEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CallLogEntity map(ResultSet rs, int row) throws SQLException {
        return CallLogEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .direction(rs.getString("direction"))
                .purpose(rs.getString("purpose"))
                .callerName(rs.getString("caller_name"))
                .incomingNumber(rs.getString("incoming_number"))
                .outgoingNumber(rs.getString("outgoing_number"))
                .callDate(rs.getObject("call_date", LocalDate.class))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .endTime(rs.getObject("end_time", LocalTime.class))
                .durationSeconds(rs.getObject("duration_seconds", Integer.class))
                .description(rs.getString("description"))
                .loggedByUserId(rs.getObject("logged_by_user_id", UUID.class))
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
