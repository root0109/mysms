package com.mysms.identity.people.repository;

/**
 * The statements of people's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code PeoplePrivacyTaskHandler} through privacy-client: per table, the ids of a subject's rows (soft-deleted ones
 * included: soft delete is not erasure) and the statement that erases one row. A person's own row is anonymised and
 * kept (other modules refer to it); every other row is cleared and soft-deleted. Text columns the schema requires
 * get a fixed placeholder; nothing of the old value remains.
 */
public final class PrivacyErasureRepository {

    public static final String PERSON_ADDRESS_ROWS = """
            SELECT id FROM people.person_address WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ERASE_PERSON_ADDRESS = """
            UPDATE people.person_address
               SET line1 = NULL, line2 = NULL, city_id = NULL, city_name = NULL, state_province_id = NULL,
                   state_name = NULL, country_id = NULL, postal_code = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String EMERGENCY_CONTACT_ROWS = """
            SELECT id FROM people.emergency_contact WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /** contact_name and phone are required while no contact person is named: fixed placeholders. */
    public static final String ERASE_EMERGENCY_CONTACT = """
            UPDATE people.emergency_contact
               SET contact_person_id = NULL, contact_name = 'Erased', relationship = NULL, phone = '0',
                   deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String EDUCATION_HISTORY_ROWS = """
            SELECT id FROM people.education_history WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ERASE_EDUCATION_HISTORY = """
            UPDATE people.education_history
               SET qualification_id = NULL, institution_name = 'Erased', location = NULL, degree_type = NULL,
                   major = NULL, grade_type = NULL, grade = NULL, start_date = NULL, end_date = NULL,
                   certification = NULL, minor = NULL, study_mode = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String WORK_EXPERIENCE_ROWS = """
            SELECT id FROM people.work_experience WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ERASE_WORK_EXPERIENCE = """
            UPDATE people.work_experience
               SET title = 'Erased', company = 'Erased', location = 'Erased', to_date = NULL, description = NULL,
                   company_website = NULL, industry = NULL, responsibilities = NULL,
                   deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String CERTIFICATION_ROWS = """
            SELECT id FROM people.certification WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ERASE_CERTIFICATION = """
            UPDATE people.certification
               SET name = 'Erased', issuing_organization = 'Erased', expiration_date = NULL, description = NULL,
                   deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String PERSON_SKILL_ROWS = """
            SELECT id FROM people.person_skill WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ERASE_PERSON_SKILL = """
            UPDATE people.person_skill
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String PERSON_DOCUMENT_ROWS = """
            SELECT id FROM people.person_document WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /** The file stays referenced until platform's file purge runs on the erasure (design §9.1, still to do). */
    public static final String ERASE_PERSON_DOCUMENT = """
            UPDATE people.person_document
               SET name = 'Erased', status = 'archived', deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String PUBLIC_PROFILE_ROWS = """
            SELECT id FROM people.public_profile WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ERASE_PUBLIC_PROFILE = """
            UPDATE people.public_profile
               SET headline = NULL, profession = NULL, persona_category = NULL, cover_image_file_id = NULL,
                   city = NULL, country_id = NULL, college = NULL, branch = NULL, linkedin_url = NULL,
                   github_url = NULL, twitter_url = NULL, medium_url = NULL, deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String PERSON_SOCIAL_LINK_ROWS = """
            SELECT id FROM people.person_social_link WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    public static final String ERASE_PERSON_SOCIAL_LINK = """
            UPDATE people.person_social_link
               SET handle = 'erased', deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String GREETING_ROWS = """
            SELECT id FROM people.greeting WHERE tenant_id = :tenantId AND person_id = :subjectId ORDER BY id
            """;
    /** A greeting holds nothing but whom, when and how; erasing it is its soft delete. */
    public static final String ERASE_GREETING = """
            UPDATE people.greeting
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String PERSON_ROWS = """
            SELECT id FROM people.person WHERE tenant_id = :tenantId AND id = :subjectId
            """;
    /** The person is kept, anonymised: 'Erased person' and no identifying value; the secret envelope cleared. */
    public static final String ANONYMISE_PERSON = """
            UPDATE people.person
               SET honorific_id = NULL, first_name = NULL, middle_name = NULL, last_name = NULL,
                   display_name = 'Erased person', date_of_birth = NULL, gender = NULL, blood_group = NULL,
                   marital_status = NULL, birth_place = NULL, native_place = NULL, nationality_country_id = NULL,
                   mother_tongue = NULL, religion = NULL, social_category = NULL, social_sub_category = NULL,
                   national_id_type = NULL, national_id_enc = NULL, visa_info = NULL, primary_email = NULL,
                   primary_phone = NULL, alternate_phone = NULL, photo_file_id = NULL, remarks = NULL,
                   work_phone = NULL, website_url = NULL, about_me = NULL, updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
