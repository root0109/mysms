package com.mysms.students.admission.repository;

/**
 * The statements of admission's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code AdmissionPrivacyTaskHandler} through privacy-client: per table, the ids of the rows of the applications whose
 * applicant is the subject (soft-deleted rows included, since soft delete is not erasure) and the statement that
 * erases one row. An application is a request to join, not the admission register (sis keeps that), so the whole
 * application goes: what the schema requires gets a fixed placeholder, the rest is cleared, the row soft-deleted; its
 * number, dates, placement and fee fields stay for the cycle's counts.
 */
public final class PrivacyErasureRepository {

    public static final String GUARDIAN_ROWS = """
            SELECT g.id
              FROM admission.application_guardian g
              JOIN admission.application a ON a.id = g.application_id AND a.tenant_id = :tenantId
             WHERE g.tenant_id = :tenantId AND a.applicant_person_id = :subjectId
             ORDER BY g.id
            """;
    public static final String ERASE_GUARDIAN = """
            UPDATE admission.application_guardian
               SET name = 'Erased', qualification_id = NULL, designation = NULL, occupation = NULL,
                   organisation = NULL, annual_income = NULL, mobile = NULL, email = NULL, national_id_enc = NULL,
                   photo_file_id = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String EMERGENCY_CONTACT_ROWS = """
            SELECT c.id
              FROM admission.application_emergency_contact c
              JOIN admission.application a ON a.id = c.application_id AND a.tenant_id = :tenantId
             WHERE c.tenant_id = :tenantId AND a.applicant_person_id = :subjectId
             ORDER BY c.id
            """;
    public static final String ERASE_EMERGENCY_CONTACT = """
            UPDATE admission.application_emergency_contact
               SET contact_number = 'Erased', relationship = 'Erased', deleted_at = COALESCE(deleted_at, :now),
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String APPLICATION_ROWS = """
            SELECT id FROM admission.application WHERE tenant_id = :tenantId AND applicant_person_id = :subjectId
             ORDER BY id
            """;
    /** The required name, birth date and gender get fixed placeholders; every other detail of the applicant goes. */
    public static final String ERASE_APPLICATION = """
            UPDATE admission.application
               SET honorific_id = NULL, full_name = 'Erased applicant', first_name = NULL, middle_name = NULL,
                   last_name = NULL, date_of_birth = DATE '1900-01-01', gender = 'other', birth_place = NULL,
                   nationality = NULL, religion = NULL, social_category = NULL, mother_tongue = NULL,
                   identification_marks = NULL, national_id_enc = NULL, blood_group = NULL, height_cm = NULL,
                   weight_kg = NULL, photo_file_id = NULL, email = NULL, phone = NULL, mobile = NULL,
                   address_line1 = NULL, address_line2 = NULL, city_name = NULL, postal_code = NULL,
                   state_province_id = NULL, country_id = NULL, permanent_address = NULL,
                   communication_address = NULL, previous_institution = NULL, previous_course = NULL,
                   previous_result = NULL, previous_board = NULL, board_registration_number = NULL,
                   reason_for_leaving = NULL, language_choice = NULL, stream_choice = NULL, has_siblings = NULL,
                   has_medical_history = NULL, medical_details = NULL, has_extracurricular = NULL,
                   extracurricular_details = NULL, transport_mode = NULL, transport_details = NULL,
                   family_business = NULL, family_income = NULL, remarks = NULL, payment_status_note = NULL,
                   deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
