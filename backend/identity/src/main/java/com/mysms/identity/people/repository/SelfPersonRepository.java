package com.mysms.identity.people.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.people.config.PeopleDatabaseConfig;
import com.mysms.identity.people.domain.SelfPersonEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to people.person. Row-level security limits every statement to the bound tenant. The self-service
 * update writes only the contact and self-description columns, under the row_version guard.
 */
@Repository
public class SelfPersonRepository {

    private static final String FIND_BY_ID = """
            SELECT r.id, r.tenant_id, r.honorific_id, r.first_name, r.middle_name, r.last_name, r.display_name,
                   r.date_of_birth, r.gender, r.blood_group, r.marital_status, r.birth_place, r.native_place,
                   r.nationality_country_id, r.mother_tongue, r.religion, r.social_category, r.social_sub_category,
                   r.national_id_type, r.visa_info, r.primary_email, r.primary_phone, r.alternate_phone,
                   r.photo_file_id, r.remarks, r.work_phone, r.website_url, r.about_me, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM people.person r
             WHERE r.tenant_id = :tenantId
               AND r.id = :id AND r.deleted_at IS NULL
            """;
    private static final String UPDATE_SELF = """
            UPDATE people.person
               SET primary_email = :primaryEmail,
                   primary_phone = :primaryPhone,
                   alternate_phone = :alternatePhone,
                   work_phone = :workPhone,
                   website_url = :websiteUrl,
                   about_me = :aboutMe,
                   mother_tongue = :motherTongue,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public SelfPersonRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SelfPersonEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SelfPersonRepository::map).optional();
    }

    /** Writes the self-editable columns; 0 rows means the row changed (or vanished) since it was read. */
    public int updateSelfEditable(SelfPersonEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE_SELF)
                .param("id", e.getId())
                .param("primaryEmail", e.getPrimaryEmail())
                .param("primaryPhone", e.getPrimaryPhone())
                .param("alternatePhone", e.getAlternatePhone())
                .param("workPhone", e.getWorkPhone())
                .param("websiteUrl", e.getWebsiteUrl())
                .param("aboutMe", e.getAboutMe())
                .param("motherTongue", e.getMotherTongue())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static SelfPersonEntity map(ResultSet rs, int row) throws SQLException {
        return SelfPersonEntity.builder()
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
}
