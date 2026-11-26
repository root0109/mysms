package com.mysms.students.careers.repository;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.domain.CandidatePreferredIndustryEntity;
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
 * Plain-JDBC access to careers.candidate_preferred_industry: live rows only, keyset paging, and the row_version guard
 * on every update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CandidatePreferredIndustryRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("candidatePreferenceId", "r.candidate_preference_id", ValueType.UUID),
            field("industryId", "r.industry_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CandidatePreferredIndustryEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CandidatePreferredIndustryEntity::getCreatedAt),
            Map.entry("updateTime", CandidatePreferredIndustryEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.candidate_preference_id, r.industry_id, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM careers.candidate_preferred_industry r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO careers.candidate_preferred_industry
            (id, tenant_id, candidate_preference_id, industry_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :candidatePreferenceId, :industryId, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, candidate_preference_id, industry_id, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE careers.candidate_preferred_industry
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE careers.candidate_preferred_industry
               SET industry_id = :industryId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_CANDIDATE_PREFERRED_INDUSTRY_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM careers.candidate_preferred_industry
                            WHERE tenant_id = :tenantId
                              AND candidate_preference_id = :candidatePreferenceId AND industry_id = :industryId AND id
                                  <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_CANDIDATE_PREFERENCE_ID = """
            SELECT EXISTS (SELECT 1 FROM careers.candidate_preference WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_INDUSTRY_ID = """
            SELECT EXISTS (SELECT 1 FROM careers.industry WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public CandidatePreferredIndustryRepository(@Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CandidatePreferredIndustryEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CandidatePreferredIndustryRepository::map).optional();
    }

    public Page<CandidatePreferredIndustryEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CandidatePreferredIndustryEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CandidatePreferredIndustryRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CandidatePreferredIndustryEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CandidatePreferredIndustryEntity insert(CandidatePreferredIndustryEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("candidatePreferenceId", e.getCandidatePreferenceId())
                .param("industryId", e.getIndustryId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CandidatePreferredIndustryRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CandidatePreferredIndustryEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("industryId", e.getIndustryId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CandidatePreferredIndustryEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_candidate_preferred_industry_1: candidate_preference_id, industry_id where (deleted_at IS NULL). */
    public boolean takenCandidatePreferenceIdIndustryId1(CandidatePreferredIndustryEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_CANDIDATE_PREFERRED_INDUSTRY_1_TAKEN)
                .param("candidatePreferenceId", e.getCandidatePreferenceId())
                .param("industryId", e.getIndustryId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced careers.candidate_preference row exists (live). */
    public boolean existsCandidatePreferenceId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CANDIDATE_PREFERENCE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced careers.industry row exists (live). */
    public boolean existsIndustryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_INDUSTRY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.candidate_preference_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, CandidatePreferredIndustryEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CandidatePreferredIndustryEntity map(ResultSet rs, int row) throws SQLException {
        return CandidatePreferredIndustryEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .candidatePreferenceId(rs.getObject("candidate_preference_id", UUID.class))
                .industryId(rs.getObject("industry_id", UUID.class))
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
