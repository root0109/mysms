package com.mysms.people.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.domain.EducationHistoryEntity;
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
 * Plain-JDBC access to people.education_history (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class EducationHistoryRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("personId", "r.person_id", ValueType.UUID),
            field("qualificationId", "r.qualification_id", ValueType.UUID),
            field("institutionName", "r.institution_name", ValueType.STRING),
            field("location", "r.location", ValueType.STRING),
            field("degreeType", "r.degree_type", ValueType.STRING),
            field("major", "r.major", ValueType.STRING),
            field("gradeType", "r.grade_type", ValueType.STRING),
            field("grade", "r.grade", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE),
            field("endDate", "r.end_date", ValueType.DATE),
            field("certification", "r.certification", ValueType.STRING),
            field("minor", "r.minor", ValueType.STRING),
            field("studyMode", "r.study_mode", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<EducationHistoryEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", EducationHistoryEntity::getCreatedAt),
            Map.entry("updateTime", EducationHistoryEntity::getUpdatedAt),
            Map.entry("sortOrder", EducationHistoryEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.person_id, r.qualification_id, r.institution_name, r.location, r.degree_type,
                   r.major, r.grade_type, r.grade, r.start_date, r.end_date, r.certification, r.minor, r.study_mode,
                   r.sort_order, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM people.education_history r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO people.education_history
            (id, tenant_id, person_id, qualification_id, institution_name, location, degree_type, major, grade_type,
             grade, start_date, end_date, certification, minor, study_mode, sort_order, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :personId, :qualificationId, :institutionName, :location, :degreeType, :major,
                    :gradeType, :grade, :startDate, :endDate, :certification, :minor, :studyMode,
                    COALESCE(:sortOrder, 0), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, person_id, qualification_id, institution_name, location, degree_type, major,
                      grade_type, grade, start_date, end_date, certification, minor, study_mode, sort_order, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE people.education_history
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE people.education_history
               SET qualification_id = :qualificationId,
                   institution_name = :institutionName,
                   location = :location,
                   degree_type = :degreeType,
                   major = :major,
                   grade_type = :gradeType,
                   grade = :grade,
                   start_date = :startDate,
                   end_date = :endDate,
                   certification = :certification,
                   minor = :minor,
                   study_mode = :studyMode,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUALIFICATION_ID = """
            SELECT EXISTS (SELECT 1 FROM people.qualification WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public EducationHistoryRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<EducationHistoryEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(EducationHistoryRepository::map).optional();
    }

    public Page<EducationHistoryEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<EducationHistoryEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(EducationHistoryRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), EducationHistoryEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public EducationHistoryEntity insert(EducationHistoryEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId())
                .param("qualificationId", e.getQualificationId())
                .param("institutionName", e.getInstitutionName())
                .param("location", e.getLocation())
                .param("degreeType", e.getDegreeType())
                .param("major", e.getMajor())
                .param("gradeType", e.getGradeType())
                .param("grade", e.getGrade())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("certification", e.getCertification())
                .param("minor", e.getMinor())
                .param("studyMode", e.getStudyMode())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(EducationHistoryRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(EducationHistoryEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("qualificationId", e.getQualificationId())
                .param("institutionName", e.getInstitutionName())
                .param("location", e.getLocation())
                .param("degreeType", e.getDegreeType())
                .param("major", e.getMajor())
                .param("gradeType", e.getGradeType())
                .param("grade", e.getGrade())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("certification", e.getCertification())
                .param("minor", e.getMinor())
                .param("studyMode", e.getStudyMode())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(EducationHistoryEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced people.person row exists (live). */
    public boolean existsPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PERSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced people.qualification row exists (live). */
    public boolean existsQualificationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUALIFICATION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.person_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, EducationHistoryEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static EducationHistoryEntity map(ResultSet rs, int row) throws SQLException {
        return EducationHistoryEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .qualificationId(rs.getObject("qualification_id", UUID.class))
                .institutionName(rs.getString("institution_name"))
                .location(rs.getString("location"))
                .degreeType(rs.getString("degree_type"))
                .major(rs.getString("major"))
                .gradeType(rs.getString("grade_type"))
                .grade(rs.getString("grade"))
                .startDate(rs.getObject("start_date", LocalDate.class))
                .endDate(rs.getObject("end_date", LocalDate.class))
                .certification(rs.getString("certification"))
                .minor(rs.getString("minor"))
                .studyMode(rs.getString("study_mode"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
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
