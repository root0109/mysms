package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.ConsentRecordEntity;
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
 * Plain-JDBC access to privacy.consent_record. A row's decision never changes and no row is deleted: the only updates
 * end a live decision (superseded, withdrawn) or verify it, each under the row_version guard and only from the state
 * it leaves. "Live" is neither withdrawn nor superseded; whether it has lapsed is the consent rule's to judge. Every
 * statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ConsentRecordRepository {

    /**
     * A giver's live decision as the consent centre shows it, with the number of the notice version it was given
     * under.
     */
    public record GivenConsent(UUID id, UUID subjectPersonId, String purposeCode, String scope, String decision,
                               OffsetDateTime validFrom, LocalDate validUntil, String channel, int rowVersion,
                               int noticeVersion) {
    }

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("subjectPersonId", "r.subject_person_id", ValueType.UUID),
            field("giverPersonId", "r.giver_person_id", ValueType.UUID),
            field("giverCapacity", "r.giver_capacity", ValueType.STRING),
            field("purposeCode", "r.purpose_code", ValueType.STRING),
            field("scope", "r.scope", ValueType.STRING),
            field("decision", "r.decision", ValueType.STRING),
            field("channel", "r.channel", ValueType.STRING),
            field("verificationStatus", "r.verification_status", ValueType.STRING),
            field("validFrom", "r.valid_from", ValueType.TIMESTAMP),
            field("withdrawnAt", "r.withdrawn_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("validFrom", "r.valid_from", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ConsentRecordEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("validFrom", ConsentRecordEntity::getValidFrom),
            Map.entry("createTime", ConsentRecordEntity::getCreatedAt),
            Map.entry("updateTime", ConsentRecordEntity::getUpdatedAt));

    private static final String SELECT = """
            SELECT r.id, r.tenant_id, r.subject_person_id, r.giver_person_id, r.giver_capacity, r.purpose_code, r.scope,
                   r.decision, r.notice_version_id, r.notice_language, r.valid_from, r.valid_until, r.withdrawn_at,
                   r.superseded_by_id, r.channel, r.verification_status, r.verification_method, r.verified_by_user_id,
                   r.verified_at, r.recorded_by_user_id, r.evidence_file_id, r.external_artefact_id, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.row_version
              FROM privacy.consent_record r
             WHERE r.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND r.id = :id";
    private static final String LIVE = """
               AND r.subject_person_id = :subjectPersonId AND r.purpose_code = :purposeCode
               AND r.withdrawn_at IS NULL AND r.superseded_by_id IS NULL
            """;
    private static final String CURRENT = SELECT + LIVE + " ORDER BY r.valid_from, r.id";
    private static final String LOCK_LIVE = SELECT + LIVE + " AND r.giver_person_id = :giverPersonId FOR UPDATE";
    private static final String GIVEN_BY = """
            SELECT r.id, r.subject_person_id, r.purpose_code, r.scope, r.decision, r.valid_from, r.valid_until,
                   r.channel, r.row_version, v.version AS notice_version
              FROM privacy.consent_record r
              JOIN privacy.notice_version v ON v.tenant_id = :tenantId AND v.id = r.notice_version_id
             WHERE r.tenant_id = :tenantId
               AND r.giver_person_id = :giverPersonId AND r.withdrawn_at IS NULL AND r.superseded_by_id IS NULL
             ORDER BY r.subject_person_id, r.purpose_code
            """;
    private static final String INSERT = """
            INSERT INTO privacy.consent_record
            (id, tenant_id, subject_person_id, giver_person_id, giver_capacity, purpose_code, scope, decision,
             notice_version_id, notice_language, valid_from, valid_until, channel, verification_status,
             verification_method, verified_by_user_id, verified_at, recorded_by_user_id, evidence_file_id, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :subjectPersonId, :giverPersonId, :giverCapacity, :purposeCode, :scope, :decision,
                    :noticeVersionId, :noticeLanguage, :validFrom, :validUntil, :channel, :verificationStatus,
                    :verificationMethod, :verifiedByUserId, :verifiedAt, :recordedByUserId, :evidenceFileId, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            """;
    private static final String SUPERSEDE = """
            UPDATE privacy.consent_record
               SET superseded_by_id = :supersededById,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND withdrawn_at IS NULL AND superseded_by_id IS NULL
            """;
    private static final String WITHDRAW = """
            UPDATE privacy.consent_record
               SET withdrawn_at = :withdrawnAt,
                   updated_at = :withdrawnAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND withdrawn_at IS NULL AND superseded_by_id IS NULL
            """;
    private static final String VERIFY = """
            UPDATE privacy.consent_record
               SET verification_status = 'verified',
                   verification_method = :verificationMethod,
                   verified_by_user_id = :verifiedByUserId,
                   verified_at = :verifiedAt,
                   updated_at = :verifiedAt,
                   updated_by = :verifiedByUserId,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND verification_status = 'unverified'
               AND withdrawn_at IS NULL AND superseded_by_id IS NULL
            """;

    private final JdbcClient jdbc;

    public ConsentRecordRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ConsentRecordEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ConsentRecordRepository::map).optional();
    }

    public Page<ConsentRecordEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ConsentRecordEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ConsentRecordRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ConsentRecordEntity::getId);
    }

    /** Every giver's live decision about the subject and purpose (entitled or not: the consent rule decides). */
    public List<ConsentRecordEntity> current(UUID subjectPersonId, String purposeCode) {
        return jdbc.sql(CURRENT).param("subjectPersonId", subjectPersonId).param("purposeCode", purposeCode)
                .query(ConsentRecordRepository::map).list();
    }

    /** The giver's live decision about the subject and purpose, its row locked until the transaction ends. */
    public Optional<ConsentRecordEntity> lockLive(UUID subjectPersonId, String purposeCode, UUID giverPersonId) {
        return jdbc.sql(LOCK_LIVE).param("subjectPersonId", subjectPersonId).param("purposeCode", purposeCode)
                .param("giverPersonId", giverPersonId).query(ConsentRecordRepository::map).optional();
    }

    /** Every live decision the person gave, about anyone, with its notice version number. */
    public List<GivenConsent> givenBy(UUID giverPersonId) {
        return jdbc.sql(GIVEN_BY).param("giverPersonId", giverPersonId)
                .query(ConsentRecordRepository::given).list();
    }

    public void insert(ConsentRecordEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("subjectPersonId", e.getSubjectPersonId())
                .param("giverPersonId", e.getGiverPersonId())
                .param("giverCapacity", e.getGiverCapacity())
                .param("purposeCode", e.getPurposeCode())
                .param("scope", e.getScope())
                .param("decision", e.getDecision())
                .param("noticeVersionId", e.getNoticeVersionId())
                .param("noticeLanguage", e.getNoticeLanguage())
                .param("validFrom", e.getValidFrom())
                .param("validUntil", e.getValidUntil())
                .param("channel", e.getChannel())
                .param("verificationStatus", e.getVerificationStatus())
                .param("verificationMethod", e.getVerificationMethod())
                .param("verifiedByUserId", e.getVerifiedByUserId())
                .param("verifiedAt", e.getVerifiedAt())
                .param("recordedByUserId", e.getRecordedByUserId())
                .param("evidenceFileId", e.getEvidenceFileId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** live → superseded by the giver's newer decision; 0 rows means the row changed since it was read. */
    public int supersede(UUID id, int expectedVersion, UUID supersededById, OffsetDateTime at, UUID actor) {
        return jdbc.sql(SUPERSEDE).param("id", id).param("supersededById", supersededById).param("updatedAt", at)
                .param("updatedBy", actor).param("expected", expectedVersion).update();
    }

    /** live → withdrawn; 0 rows means the row changed since it was read. */
    public int withdraw(UUID id, int expectedVersion, OffsetDateTime at, UUID actor) {
        return jdbc.sql(WITHDRAW).param("id", id).param("withdrawnAt", at).param("updatedBy", actor)
                .param("expected", expectedVersion).update();
    }

    /** unverified → verified by a staff member; 0 rows means the row changed since it was read. */
    public int verify(UUID id, int expectedVersion, String method, UUID verifier, OffsetDateTime at) {
        return jdbc.sql(VERIFY).param("id", id).param("verificationMethod", method).param("verifiedByUserId", verifier)
                .param("verifiedAt", at).param("expected", expectedVersion).update();
    }

    private static List<Object> sortValues(SortSpec sort, ConsentRecordEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static GivenConsent given(ResultSet rs, int row) throws SQLException {
        return new GivenConsent(rs.getObject("id", UUID.class), rs.getObject("subject_person_id", UUID.class),
                rs.getString("purpose_code"), rs.getString("scope"), rs.getString("decision"),
                rs.getObject("valid_from", OffsetDateTime.class), rs.getObject("valid_until", LocalDate.class),
                rs.getString("channel"), rs.getInt("row_version"), rs.getInt("notice_version"));
    }

    private static ConsentRecordEntity map(ResultSet rs, int row) throws SQLException {
        return ConsentRecordEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .subjectPersonId(rs.getObject("subject_person_id", UUID.class))
                .giverPersonId(rs.getObject("giver_person_id", UUID.class))
                .giverCapacity(rs.getString("giver_capacity"))
                .purposeCode(rs.getString("purpose_code"))
                .scope(rs.getString("scope"))
                .decision(rs.getString("decision"))
                .noticeVersionId(rs.getObject("notice_version_id", UUID.class))
                .noticeLanguage(rs.getString("notice_language"))
                .validFrom(rs.getObject("valid_from", OffsetDateTime.class))
                .validUntil(rs.getObject("valid_until", LocalDate.class))
                .withdrawnAt(rs.getObject("withdrawn_at", OffsetDateTime.class))
                .supersededById(rs.getObject("superseded_by_id", UUID.class))
                .channel(rs.getString("channel"))
                .verificationStatus(rs.getString("verification_status"))
                .verificationMethod(rs.getString("verification_method"))
                .verifiedByUserId(rs.getObject("verified_by_user_id", UUID.class))
                .verifiedAt(rs.getObject("verified_at", OffsetDateTime.class))
                .recordedByUserId(rs.getObject("recorded_by_user_id", UUID.class))
                .evidenceFileId(rs.getObject("evidence_file_id", UUID.class))
                .externalArtefactId(rs.getString("external_artefact_id"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
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
