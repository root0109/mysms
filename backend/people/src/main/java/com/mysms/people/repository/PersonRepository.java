package com.mysms.people.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.domain.PersonEntity;
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
 * Plain-JDBC access to people.person (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class PersonRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("honorificId", "r.honorific_id", ValueType.UUID),
            field("firstName", "r.first_name", ValueType.STRING),
            field("middleName", "r.middle_name", ValueType.STRING),
            field("lastName", "r.last_name", ValueType.STRING),
            field("displayName", "r.display_name", ValueType.STRING),
            field("dateOfBirth", "r.date_of_birth", ValueType.DATE),
            field("gender", "r.gender", ValueType.STRING),
            field("bloodGroup", "r.blood_group", ValueType.STRING),
            field("maritalStatus", "r.marital_status", ValueType.STRING),
            field("birthPlace", "r.birth_place", ValueType.STRING),
            field("nativePlace", "r.native_place", ValueType.STRING),
            field("nationalityCountryId", "r.nationality_country_id", ValueType.UUID),
            field("motherTongue", "r.mother_tongue", ValueType.STRING),
            field("religion", "r.religion", ValueType.STRING),
            field("socialCategory", "r.social_category", ValueType.STRING),
            field("socialSubCategory", "r.social_sub_category", ValueType.STRING),
            field("nationalIdType", "r.national_id_type", ValueType.STRING),
            field("visaInfo", "r.visa_info", ValueType.STRING),
            field("primaryEmail", "r.primary_email", ValueType.STRING),
            field("primaryPhone", "r.primary_phone", ValueType.STRING),
            field("alternatePhone", "r.alternate_phone", ValueType.STRING),
            field("photoFileId", "r.photo_file_id", ValueType.UUID),
            field("remarks", "r.remarks", ValueType.STRING),
            field("workPhone", "r.work_phone", ValueType.STRING),
            field("websiteUrl", "r.website_url", ValueType.STRING),
            field("aboutMe", "r.about_me", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PersonEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PersonEntity::getCreatedAt),
            Map.entry("updateTime", PersonEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.honorific_id, r.first_name, r.middle_name, r.last_name, r.display_name,
                   r.date_of_birth, r.gender, r.blood_group, r.marital_status, r.birth_place, r.native_place,
                   r.nationality_country_id, r.mother_tongue, r.religion, r.social_category, r.social_sub_category,
                   r.national_id_type, r.national_id_enc, r.visa_info, r.primary_email, r.primary_phone,
                   r.alternate_phone, r.photo_file_id, r.remarks, r.work_phone, r.website_url, r.about_me,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM people.person r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO people.person
            (id, tenant_id, honorific_id, first_name, middle_name, last_name, display_name, date_of_birth, gender,
             blood_group, marital_status, birth_place, native_place, nationality_country_id, mother_tongue, religion,
             social_category, social_sub_category, national_id_type, national_id_enc, visa_info, primary_email,
             primary_phone, alternate_phone, photo_file_id, remarks, work_phone, website_url, about_me, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :honorificId, :firstName, :middleName, :lastName, :displayName, :dateOfBirth,
                    :gender, :bloodGroup, :maritalStatus, :birthPlace, :nativePlace, :nationalityCountryId,
                    :motherTongue, :religion, :socialCategory, :socialSubCategory, :nationalIdType, :nationalIdEnc,
                    :visaInfo, :primaryEmail, :primaryPhone, :alternatePhone, :photoFileId, :remarks, :workPhone,
                    :websiteUrl, :aboutMe, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, honorific_id, first_name, middle_name, last_name, display_name, date_of_birth,
                      gender, blood_group, marital_status, birth_place, native_place, nationality_country_id,
                      mother_tongue, religion, social_category, social_sub_category, national_id_type, national_id_enc,
                      visa_info, primary_email, primary_phone, alternate_phone, photo_file_id, remarks, work_phone,
                      website_url, about_me, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE people.person
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE people.person
               SET honorific_id = :honorificId,
                   first_name = :firstName,
                   middle_name = :middleName,
                   last_name = :lastName,
                   display_name = :displayName,
                   date_of_birth = :dateOfBirth,
                   gender = :gender,
                   blood_group = :bloodGroup,
                   marital_status = :maritalStatus,
                   birth_place = :birthPlace,
                   native_place = :nativePlace,
                   nationality_country_id = :nationalityCountryId,
                   mother_tongue = :motherTongue,
                   religion = :religion,
                   social_category = :socialCategory,
                   social_sub_category = :socialSubCategory,
                   national_id_type = :nationalIdType,
                   national_id_enc = :nationalIdEnc,
                   visa_info = :visaInfo,
                   primary_email = :primaryEmail,
                   primary_phone = :primaryPhone,
                   alternate_phone = :alternatePhone,
                   photo_file_id = :photoFileId,
                   remarks = :remarks,
                   work_phone = :workPhone,
                   website_url = :websiteUrl,
                   about_me = :aboutMe,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_HONORIFIC_ID = """
            SELECT EXISTS (SELECT 1 FROM people.honorific WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CERTIFICATION_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.certification WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONSENT_RECORD_GIVEN_BY_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.consent_record WHERE tenant_id = :tenantId
                AND given_by_person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONSENT_RECORD_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.consent_record WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EDUCATION_HISTORY_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.education_history WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EMERGENCY_CONTACT_CONTACT_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.emergency_contact WHERE tenant_id = :tenantId
                AND contact_person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EMERGENCY_CONTACT_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.emergency_contact WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PERSON_ADDRESS_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person_address WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PERSON_DOCUMENT_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person_document WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PERSON_SKILL_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person_skill WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PERSON_SOCIAL_LINK_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person_social_link WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PUBLIC_PROFILE_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.public_profile WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_WORK_EXPERIENCE_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.work_experience WHERE tenant_id = :tenantId
                AND person_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public PersonRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PersonEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PersonRepository::map).optional();
    }

    public Page<PersonEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PersonEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PersonRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PersonEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PersonEntity insert(PersonEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("honorificId", e.getHonorificId())
                .param("firstName", e.getFirstName())
                .param("middleName", e.getMiddleName())
                .param("lastName", e.getLastName())
                .param("displayName", e.getDisplayName())
                .param("dateOfBirth", e.getDateOfBirth())
                .param("gender", e.getGender())
                .param("bloodGroup", e.getBloodGroup())
                .param("maritalStatus", e.getMaritalStatus())
                .param("birthPlace", e.getBirthPlace())
                .param("nativePlace", e.getNativePlace())
                .param("nationalityCountryId", e.getNationalityCountryId())
                .param("motherTongue", e.getMotherTongue())
                .param("religion", e.getReligion())
                .param("socialCategory", e.getSocialCategory())
                .param("socialSubCategory", e.getSocialSubCategory())
                .param("nationalIdType", e.getNationalIdType())
                .param("nationalIdEnc", e.getNationalIdEnc())
                .param("visaInfo", e.getVisaInfo())
                .param("primaryEmail", e.getPrimaryEmail())
                .param("primaryPhone", e.getPrimaryPhone())
                .param("alternatePhone", e.getAlternatePhone())
                .param("photoFileId", e.getPhotoFileId())
                .param("remarks", e.getRemarks())
                .param("workPhone", e.getWorkPhone())
                .param("websiteUrl", e.getWebsiteUrl())
                .param("aboutMe", e.getAboutMe())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PersonRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PersonEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("honorificId", e.getHonorificId())
                .param("firstName", e.getFirstName())
                .param("middleName", e.getMiddleName())
                .param("lastName", e.getLastName())
                .param("displayName", e.getDisplayName())
                .param("dateOfBirth", e.getDateOfBirth())
                .param("gender", e.getGender())
                .param("bloodGroup", e.getBloodGroup())
                .param("maritalStatus", e.getMaritalStatus())
                .param("birthPlace", e.getBirthPlace())
                .param("nativePlace", e.getNativePlace())
                .param("nationalityCountryId", e.getNationalityCountryId())
                .param("motherTongue", e.getMotherTongue())
                .param("religion", e.getReligion())
                .param("socialCategory", e.getSocialCategory())
                .param("socialSubCategory", e.getSocialSubCategory())
                .param("nationalIdType", e.getNationalIdType())
                .param("nationalIdEnc", e.getNationalIdEnc())
                .param("visaInfo", e.getVisaInfo())
                .param("primaryEmail", e.getPrimaryEmail())
                .param("primaryPhone", e.getPrimaryPhone())
                .param("alternatePhone", e.getAlternatePhone())
                .param("photoFileId", e.getPhotoFileId())
                .param("remarks", e.getRemarks())
                .param("workPhone", e.getWorkPhone())
                .param("websiteUrl", e.getWebsiteUrl())
                .param("aboutMe", e.getAboutMe())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PersonEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced people.honorific row exists (live). */
    public boolean existsHonorificId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_HONORIFIC_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live people.certification rows still reference the row. */
    public boolean referencedByCertificationPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CERTIFICATION_PERSON_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live people.consent_record rows still reference the row. */
    public boolean referencedByConsentRecordGivenByPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONSENT_RECORD_GIVEN_BY_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.consent_record rows still reference the row. */
    public boolean referencedByConsentRecordPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONSENT_RECORD_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.education_history rows still reference the row. */
    public boolean referencedByEducationHistoryPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EDUCATION_HISTORY_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.emergency_contact rows still reference the row. */
    public boolean referencedByEmergencyContactContactPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EMERGENCY_CONTACT_CONTACT_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.emergency_contact rows still reference the row. */
    public boolean referencedByEmergencyContactPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EMERGENCY_CONTACT_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.person_address rows still reference the row. */
    public boolean referencedByPersonAddressPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PERSON_ADDRESS_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.person_document rows still reference the row. */
    public boolean referencedByPersonDocumentPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PERSON_DOCUMENT_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.person_skill rows still reference the row. */
    public boolean referencedByPersonSkillPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PERSON_SKILL_PERSON_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live people.person_social_link rows still reference the row. */
    public boolean referencedByPersonSocialLinkPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PERSON_SOCIAL_LINK_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.public_profile rows still reference the row. */
    public boolean referencedByPublicProfilePersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PUBLIC_PROFILE_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live people.work_experience rows still reference the row. */
    public boolean referencedByWorkExperiencePersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_WORK_EXPERIENCE_PERSON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, PersonEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PersonEntity map(ResultSet rs, int row) throws SQLException {
        return PersonEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .honorificId(rs.getObject("honorific_id", UUID.class))
                .firstName(rs.getString("first_name"))
                .middleName(rs.getString("middle_name"))
                .lastName(rs.getString("last_name"))
                .displayName(rs.getString("display_name"))
                .dateOfBirth(rs.getObject("date_of_birth", LocalDate.class))
                .gender(rs.getString("gender"))
                .bloodGroup(rs.getString("blood_group"))
                .maritalStatus(rs.getString("marital_status"))
                .birthPlace(rs.getString("birth_place"))
                .nativePlace(rs.getString("native_place"))
                .nationalityCountryId(rs.getObject("nationality_country_id", UUID.class))
                .motherTongue(rs.getString("mother_tongue"))
                .religion(rs.getString("religion"))
                .socialCategory(rs.getString("social_category"))
                .socialSubCategory(rs.getString("social_sub_category"))
                .nationalIdType(rs.getString("national_id_type"))
                .nationalIdEnc(rs.getString("national_id_enc"))
                .visaInfo(rs.getString("visa_info"))
                .primaryEmail(rs.getString("primary_email"))
                .primaryPhone(rs.getString("primary_phone"))
                .alternatePhone(rs.getString("alternate_phone"))
                .photoFileId(rs.getObject("photo_file_id", UUID.class))
                .remarks(rs.getString("remarks"))
                .workPhone(rs.getString("work_phone"))
                .websiteUrl(rs.getString("website_url"))
                .aboutMe(rs.getString("about_me"))
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
