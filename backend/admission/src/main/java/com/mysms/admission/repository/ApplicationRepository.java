package com.mysms.admission.repository;
import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.domain.ApplicationEntity;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to admission.application: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ApplicationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("admissionCycleId", "r.admission_cycle_id", ValueType.UUID),
            field("applicationNumber", "r.application_number", ValueType.STRING),
            field("applicationDate", "r.application_date", ValueType.TIMESTAMP),
            field("admissionDate", "r.admission_date", ValueType.DATE),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("gradeLevelId", "r.grade_level_id", ValueType.UUID),
            field("sectionId", "r.section_id", ValueType.UUID),
            field("programId", "r.program_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("batchId", "r.batch_id", ValueType.UUID),
            field("applicantPersonId", "r.applicant_person_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("isExistingStudent", "r.is_existing_student", ValueType.BOOLEAN),
            field("honorificId", "r.honorific_id", ValueType.UUID),
            field("fullName", "r.full_name", ValueType.STRING),
            field("firstName", "r.first_name", ValueType.STRING),
            field("middleName", "r.middle_name", ValueType.STRING),
            field("lastName", "r.last_name", ValueType.STRING),
            field("dateOfBirth", "r.date_of_birth", ValueType.DATE),
            field("gender", "r.gender", ValueType.STRING),
            field("birthPlace", "r.birth_place", ValueType.STRING),
            field("nationality", "r.nationality", ValueType.STRING),
            field("religion", "r.religion", ValueType.STRING),
            field("socialCategory", "r.social_category", ValueType.STRING),
            field("motherTongue", "r.mother_tongue", ValueType.STRING),
            field("identificationMarks", "r.identification_marks", ValueType.STRING),
            field("bloodGroup", "r.blood_group", ValueType.STRING),
            field("heightCm", "r.height_cm", ValueType.DECIMAL),
            field("weightKg", "r.weight_kg", ValueType.DECIMAL),
            field("photoFileId", "r.photo_file_id", ValueType.UUID),
            field("email", "r.email", ValueType.STRING),
            field("phone", "r.phone", ValueType.STRING),
            field("mobile", "r.mobile", ValueType.STRING),
            field("addressLine1", "r.address_line1", ValueType.STRING),
            field("addressLine2", "r.address_line2", ValueType.STRING),
            field("cityName", "r.city_name", ValueType.STRING),
            field("postalCode", "r.postal_code", ValueType.STRING),
            field("stateProvinceId", "r.state_province_id", ValueType.UUID),
            field("countryId", "r.country_id", ValueType.UUID),
            field("permanentAddress", "r.permanent_address", ValueType.STRING),
            field("communicationAddress", "r.communication_address", ValueType.STRING),
            field("previousInstitution", "r.previous_institution", ValueType.STRING),
            field("previousCourse", "r.previous_course", ValueType.STRING),
            field("previousResult", "r.previous_result", ValueType.STRING),
            field("previousBoard", "r.previous_board", ValueType.STRING),
            field("boardRegistrationNumber", "r.board_registration_number", ValueType.STRING),
            field("reasonForLeaving", "r.reason_for_leaving", ValueType.STRING),
            field("languageChoice", "r.language_choice", ValueType.STRING),
            field("streamChoice", "r.stream_choice", ValueType.STRING),
            field("hasSiblings", "r.has_siblings", ValueType.BOOLEAN),
            field("hasMedicalHistory", "r.has_medical_history", ValueType.BOOLEAN),
            field("medicalDetails", "r.medical_details", ValueType.STRING),
            field("hasExtracurricular", "r.has_extracurricular", ValueType.BOOLEAN),
            field("extracurricularDetails", "r.extracurricular_details", ValueType.STRING),
            field("transportMode", "r.transport_mode", ValueType.STRING),
            field("transportDetails", "r.transport_details", ValueType.STRING),
            field("familyBusiness", "r.family_business", ValueType.STRING),
            field("familyIncome", "r.family_income", ValueType.DECIMAL),
            field("applicationFee", "r.application_fee", ValueType.DECIMAL),
            field("discountPercent", "r.discount_percent", ValueType.DECIMAL),
            field("feePlanId", "r.fee_plan_id", ValueType.UUID),
            field("feeStartDate", "r.fee_start_date", ValueType.DATE),
            field("paymentDueDate", "r.payment_due_date", ValueType.DATE),
            field("paymentStatus", "r.payment_status", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("remarks", "r.remarks", ValueType.STRING),
            field("paymentStatusNote", "r.payment_status_note", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** The office's notes on an application: never filtered on by an applicant (they would probe them). */
    public static final Set<String> OFFICE_NOTE_FIELDS = Set.of("remarks", "paymentStatusNote");
    /** What an applicant may filter their own applications by: everything but the office's notes. */
    public static final Map<String, FieldSpec> APPLICANT_FILTER_FIELDS = FILTER_FIELDS.entrySet().stream()
            .filter(field -> !OFFICE_NOTE_FIELDS.contains(field.getKey()))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ApplicationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ApplicationEntity::getCreatedAt),
            Map.entry("updateTime", ApplicationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.admission_cycle_id, r.application_number,
                   r.application_date, r.admission_date, r.academic_year_id, r.grade_level_id, r.section_id,
                   r.program_id, r.course_id, r.batch_id, r.applicant_person_id, r.student_id, r.is_existing_student,
                   r.honorific_id, r.full_name, r.first_name, r.middle_name, r.last_name, r.date_of_birth, r.gender,
                   r.birth_place, r.nationality, r.religion, r.social_category, r.mother_tongue,
                   r.identification_marks, r.national_id_enc, r.blood_group, r.height_cm, r.weight_kg,
                   r.photo_file_id, r.email, r.phone, r.mobile, r.address_line1, r.address_line2, r.city_name,
                   r.postal_code, r.state_province_id, r.country_id, r.permanent_address, r.communication_address,
                   r.previous_institution, r.previous_course, r.previous_result, r.previous_board,
                   r.board_registration_number, r.reason_for_leaving, r.language_choice, r.stream_choice,
                   r.has_siblings, r.has_medical_history, r.medical_details, r.has_extracurricular,
                   r.extracurricular_details, r.transport_mode, r.transport_details, r.family_business,
                   r.family_income, r.application_fee, r.discount_percent, r.fee_plan_id, r.fee_start_date,
                   r.payment_due_date, r.payment_status, r.status, r.remarks, r.payment_status_note, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM admission.application r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO admission.application
            (id, tenant_id, institution_id, admission_cycle_id, application_number, application_date, admission_date,
             academic_year_id, grade_level_id, section_id, program_id, course_id, batch_id, applicant_person_id,
             student_id, is_existing_student, honorific_id, full_name, first_name, middle_name, last_name,
             date_of_birth, gender, birth_place, nationality, religion, social_category, mother_tongue,
             identification_marks, national_id_enc, blood_group, height_cm, weight_kg, photo_file_id, email, phone,
             mobile, address_line1, address_line2, city_name, postal_code, state_province_id, country_id,
             permanent_address, communication_address, previous_institution, previous_course, previous_result,
             previous_board, board_registration_number, reason_for_leaving, language_choice, stream_choice,
             has_siblings, has_medical_history, medical_details, has_extracurricular, extracurricular_details,
             transport_mode, transport_details, family_business, family_income, application_fee, discount_percent,
             fee_plan_id, fee_start_date, payment_due_date, payment_status, remarks, payment_status_note, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :admissionCycleId, :applicationNumber,
                    COALESCE(:applicationDate, now()), :admissionDate, :academicYearId, :gradeLevelId, :sectionId,
                    :programId, :courseId, :batchId, :applicantPersonId, :studentId,
                    COALESCE(:isExistingStudent, false), :honorificId, :fullName, :firstName, :middleName, :lastName,
                    :dateOfBirth, :gender, :birthPlace, :nationality, :religion, :socialCategory, :motherTongue,
                    :identificationMarks, :nationalIdEnc, :bloodGroup, :heightCm, :weightKg, :photoFileId, :email,
                    :phone, :mobile, :addressLine1, :addressLine2, :cityName, :postalCode, :stateProvinceId,
                    :countryId, :permanentAddress, :communicationAddress, :previousInstitution, :previousCourse,
                    :previousResult, :previousBoard, :boardRegistrationNumber, :reasonForLeaving, :languageChoice,
                    :streamChoice, :hasSiblings, :hasMedicalHistory, :medicalDetails, :hasExtracurricular,
                    :extracurricularDetails, :transportMode, :transportDetails, :familyBusiness, :familyIncome,
                    :applicationFee, :discountPercent, :feePlanId, :feeStartDate, :paymentDueDate,
                    COALESCE(:paymentStatus, 'unpaid'::character varying), :remarks, :paymentStatusNote, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, admission_cycle_id, application_number, application_date,
                      admission_date, academic_year_id, grade_level_id, section_id, program_id, course_id, batch_id,
                      applicant_person_id, student_id, is_existing_student, honorific_id, full_name, first_name,
                      middle_name, last_name, date_of_birth, gender, birth_place, nationality, religion,
                      social_category, mother_tongue, identification_marks, national_id_enc, blood_group, height_cm,
                      weight_kg, photo_file_id, email, phone, mobile, address_line1, address_line2, city_name,
                      postal_code, state_province_id, country_id, permanent_address, communication_address,
                      previous_institution, previous_course, previous_result, previous_board, board_registration_number,
                      reason_for_leaving, language_choice, stream_choice, has_siblings, has_medical_history,
                      medical_details, has_extracurricular, extracurricular_details, transport_mode, transport_details,
                      family_business, family_income, application_fee, discount_percent, fee_plan_id, fee_start_date,
                      payment_due_date, payment_status, status, remarks, payment_status_note, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE admission.application
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE admission.application
               SET institution_id = :institutionId,
                   admission_cycle_id = :admissionCycleId,
                   application_number = :applicationNumber,
                   application_date = :applicationDate,
                   admission_date = :admissionDate,
                   academic_year_id = :academicYearId,
                   grade_level_id = :gradeLevelId,
                   section_id = :sectionId,
                   program_id = :programId,
                   course_id = :courseId,
                   batch_id = :batchId,
                   applicant_person_id = :applicantPersonId,
                   student_id = :studentId,
                   is_existing_student = :isExistingStudent,
                   honorific_id = :honorificId,
                   full_name = :fullName,
                   first_name = :firstName,
                   middle_name = :middleName,
                   last_name = :lastName,
                   date_of_birth = :dateOfBirth,
                   gender = :gender,
                   birth_place = :birthPlace,
                   nationality = :nationality,
                   religion = :religion,
                   social_category = :socialCategory,
                   mother_tongue = :motherTongue,
                   identification_marks = :identificationMarks,
                   national_id_enc = :nationalIdEnc,
                   blood_group = :bloodGroup,
                   height_cm = :heightCm,
                   weight_kg = :weightKg,
                   photo_file_id = :photoFileId,
                   email = :email,
                   phone = :phone,
                   mobile = :mobile,
                   address_line1 = :addressLine1,
                   address_line2 = :addressLine2,
                   city_name = :cityName,
                   postal_code = :postalCode,
                   state_province_id = :stateProvinceId,
                   country_id = :countryId,
                   permanent_address = :permanentAddress,
                   communication_address = :communicationAddress,
                   previous_institution = :previousInstitution,
                   previous_course = :previousCourse,
                   previous_result = :previousResult,
                   previous_board = :previousBoard,
                   board_registration_number = :boardRegistrationNumber,
                   reason_for_leaving = :reasonForLeaving,
                   language_choice = :languageChoice,
                   stream_choice = :streamChoice,
                   has_siblings = :hasSiblings,
                   has_medical_history = :hasMedicalHistory,
                   medical_details = :medicalDetails,
                   has_extracurricular = :hasExtracurricular,
                   extracurricular_details = :extracurricularDetails,
                   transport_mode = :transportMode,
                   transport_details = :transportDetails,
                   family_business = :familyBusiness,
                   family_income = :familyIncome,
                   application_fee = :applicationFee,
                   discount_percent = :discountPercent,
                   fee_plan_id = :feePlanId,
                   fee_start_date = :feeStartDate,
                   payment_due_date = :paymentDueDate,
                   payment_status = :paymentStatus,
                   remarks = :remarks,
                   payment_status_note = :paymentStatusNote,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_APPLICATION_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM admission.application
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND application_number = :applicationNumber
                              AND id <> :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_ADMISSION_CYCLE_ID = """
            SELECT EXISTS (SELECT 1 FROM admission.admission_cycle WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_APPLICATION_EMERGENCY_CONTACT_APPLICATION_ID = """
            SELECT EXISTS (SELECT 1 FROM admission.application_emergency_contact WHERE tenant_id = :tenantId
                AND application_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_APPLICATION_GUARDIAN_APPLICATION_ID = """
            SELECT EXISTS (SELECT 1 FROM admission.application_guardian WHERE tenant_id = :tenantId
                AND application_id = :id AND deleted_at IS NULL)
            """;
    /**
     * A step of the application's lifecycle: the new status, and the two dates a step may stamp — the application date
     * on submitting, the admission date (when still empty) on confirming admission.
     */
    private static final String TRANSITION = """
            UPDATE admission.application
               SET status = :status,
                   application_date = COALESCE(CAST(:applicationDate AS timestamptz), application_date),
                   admission_date = COALESCE(admission_date, CAST(:admissionDate AS date)),
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** The applications that count against a cycle's number of places: handed in and neither refused nor withdrawn. */
    private static final String COUNT_IN_CYCLE = """
            SELECT count(*) FROM admission.application
             WHERE tenant_id = :tenantId
               AND admission_cycle_id = :admissionCycleId AND deleted_at IS NULL
               AND status NOT IN ('draft', 'rejected', 'cancelled')
            """;
    /** Enrolment, first half: the person the applicant has become, kept so a retry never creates a second one. */
    private static final String RECORD_APPLICANT_PERSON = """
            UPDATE admission.application
               SET applicant_person_id = :personId,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'admitted' AND deleted_at IS NULL
            """;
    /** Enrolment, second half: the student, kept while the application is still admitted (a half-finished enrolment). */
    private static final String RECORD_STUDENT = """
            UPDATE admission.application
               SET student_id = :studentId,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'admitted' AND deleted_at IS NULL
            """;
    /** admission.EnrollApplicant, the last step: done only for an admitted application that has its student. */
    private static final String COMPLETE_ENROLLMENT = """
            UPDATE admission.application
               SET status = 'done',
                   section_id = COALESCE(CAST(:sectionId AS uuid), section_id),
                   batch_id = COALESCE(CAST(:batchId AS uuid), batch_id),
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'admitted' AND student_id IS NOT NULL
               AND deleted_at IS NULL
            """;
    private static final String IN_SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;
    private static final String REACH = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds)
                 OR (r.created_by = :applicant AND r.admission_cycle_id IS NOT NULL))
            """;

    private final JdbcClient jdbc;

    public ApplicationRepository(@Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ApplicationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ApplicationRepository::map).optional();
    }

    public Page<ApplicationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ApplicationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ApplicationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ApplicationEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ApplicationEntity insert(ApplicationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("admissionCycleId", e.getAdmissionCycleId())
                .param("applicationNumber", e.getApplicationNumber())
                .param("applicationDate", e.getApplicationDate())
                .param("admissionDate", e.getAdmissionDate())
                .param("academicYearId", e.getAcademicYearId())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("sectionId", e.getSectionId())
                .param("programId", e.getProgramId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("applicantPersonId", e.getApplicantPersonId())
                .param("studentId", e.getStudentId())
                .param("isExistingStudent", e.getIsExistingStudent())
                .param("honorificId", e.getHonorificId())
                .param("fullName", e.getFullName())
                .param("firstName", e.getFirstName())
                .param("middleName", e.getMiddleName())
                .param("lastName", e.getLastName())
                .param("dateOfBirth", e.getDateOfBirth())
                .param("gender", e.getGender())
                .param("birthPlace", e.getBirthPlace())
                .param("nationality", e.getNationality())
                .param("religion", e.getReligion())
                .param("socialCategory", e.getSocialCategory())
                .param("motherTongue", e.getMotherTongue())
                .param("identificationMarks", e.getIdentificationMarks())
                .param("nationalIdEnc", e.getNationalIdEnc())
                .param("bloodGroup", e.getBloodGroup())
                .param("heightCm", e.getHeightCm())
                .param("weightKg", e.getWeightKg())
                .param("photoFileId", e.getPhotoFileId())
                .param("email", e.getEmail())
                .param("phone", e.getPhone())
                .param("mobile", e.getMobile())
                .param("addressLine1", e.getAddressLine1())
                .param("addressLine2", e.getAddressLine2())
                .param("cityName", e.getCityName())
                .param("postalCode", e.getPostalCode())
                .param("stateProvinceId", e.getStateProvinceId())
                .param("countryId", e.getCountryId())
                .param("permanentAddress", e.getPermanentAddress())
                .param("communicationAddress", e.getCommunicationAddress())
                .param("previousInstitution", e.getPreviousInstitution())
                .param("previousCourse", e.getPreviousCourse())
                .param("previousResult", e.getPreviousResult())
                .param("previousBoard", e.getPreviousBoard())
                .param("boardRegistrationNumber", e.getBoardRegistrationNumber())
                .param("reasonForLeaving", e.getReasonForLeaving())
                .param("languageChoice", e.getLanguageChoice())
                .param("streamChoice", e.getStreamChoice())
                .param("hasSiblings", e.getHasSiblings())
                .param("hasMedicalHistory", e.getHasMedicalHistory())
                .param("medicalDetails", e.getMedicalDetails())
                .param("hasExtracurricular", e.getHasExtracurricular())
                .param("extracurricularDetails", e.getExtracurricularDetails())
                .param("transportMode", e.getTransportMode())
                .param("transportDetails", e.getTransportDetails())
                .param("familyBusiness", e.getFamilyBusiness())
                .param("familyIncome", e.getFamilyIncome())
                .param("applicationFee", e.getApplicationFee())
                .param("discountPercent", e.getDiscountPercent())
                .param("feePlanId", e.getFeePlanId())
                .param("feeStartDate", e.getFeeStartDate())
                .param("paymentDueDate", e.getPaymentDueDate())
                .param("paymentStatus", e.getPaymentStatus())
                .param("remarks", e.getRemarks())
                .param("paymentStatusNote", e.getPaymentStatusNote())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ApplicationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ApplicationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("admissionCycleId", e.getAdmissionCycleId())
                .param("applicationNumber", e.getApplicationNumber())
                .param("applicationDate", e.getApplicationDate())
                .param("admissionDate", e.getAdmissionDate())
                .param("academicYearId", e.getAcademicYearId())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("sectionId", e.getSectionId())
                .param("programId", e.getProgramId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("applicantPersonId", e.getApplicantPersonId())
                .param("studentId", e.getStudentId())
                .param("isExistingStudent", e.getIsExistingStudent())
                .param("honorificId", e.getHonorificId())
                .param("fullName", e.getFullName())
                .param("firstName", e.getFirstName())
                .param("middleName", e.getMiddleName())
                .param("lastName", e.getLastName())
                .param("dateOfBirth", e.getDateOfBirth())
                .param("gender", e.getGender())
                .param("birthPlace", e.getBirthPlace())
                .param("nationality", e.getNationality())
                .param("religion", e.getReligion())
                .param("socialCategory", e.getSocialCategory())
                .param("motherTongue", e.getMotherTongue())
                .param("identificationMarks", e.getIdentificationMarks())
                .param("nationalIdEnc", e.getNationalIdEnc())
                .param("bloodGroup", e.getBloodGroup())
                .param("heightCm", e.getHeightCm())
                .param("weightKg", e.getWeightKg())
                .param("photoFileId", e.getPhotoFileId())
                .param("email", e.getEmail())
                .param("phone", e.getPhone())
                .param("mobile", e.getMobile())
                .param("addressLine1", e.getAddressLine1())
                .param("addressLine2", e.getAddressLine2())
                .param("cityName", e.getCityName())
                .param("postalCode", e.getPostalCode())
                .param("stateProvinceId", e.getStateProvinceId())
                .param("countryId", e.getCountryId())
                .param("permanentAddress", e.getPermanentAddress())
                .param("communicationAddress", e.getCommunicationAddress())
                .param("previousInstitution", e.getPreviousInstitution())
                .param("previousCourse", e.getPreviousCourse())
                .param("previousResult", e.getPreviousResult())
                .param("previousBoard", e.getPreviousBoard())
                .param("boardRegistrationNumber", e.getBoardRegistrationNumber())
                .param("reasonForLeaving", e.getReasonForLeaving())
                .param("languageChoice", e.getLanguageChoice())
                .param("streamChoice", e.getStreamChoice())
                .param("hasSiblings", e.getHasSiblings())
                .param("hasMedicalHistory", e.getHasMedicalHistory())
                .param("medicalDetails", e.getMedicalDetails())
                .param("hasExtracurricular", e.getHasExtracurricular())
                .param("extracurricularDetails", e.getExtracurricularDetails())
                .param("transportMode", e.getTransportMode())
                .param("transportDetails", e.getTransportDetails())
                .param("familyBusiness", e.getFamilyBusiness())
                .param("familyIncome", e.getFamilyIncome())
                .param("applicationFee", e.getApplicationFee())
                .param("discountPercent", e.getDiscountPercent())
                .param("feePlanId", e.getFeePlanId())
                .param("feeStartDate", e.getFeeStartDate())
                .param("paymentDueDate", e.getPaymentDueDate())
                .param("paymentStatus", e.getPaymentStatus())
                .param("remarks", e.getRemarks())
                .param("paymentStatusNote", e.getPaymentStatusNote())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ApplicationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_application_1: institution_id, application_number, among live rows. */
    public boolean takenInstitutionIdApplicationNumber1(ApplicationEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_APPLICATION_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("applicationNumber", e.getApplicationNumber())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced admission.admission_cycle row exists (live). */
    public boolean existsAdmissionCycleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ADMISSION_CYCLE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live admission.application_emergency_contact rows still reference the row. */
    public boolean referencedByApplicationEmergencyContactApplicationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_APPLICATION_EMERGENCY_CONTACT_APPLICATION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live admission.application_guardian rows still reference the row. */
    public boolean referencedByApplicationGuardianApplicationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_APPLICATION_GUARDIAN_APPLICATION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /**
     * One lifecycle step; 0 rows means the row changed (or vanished) since it was read.
     *
     * @param applicationDate the submission time, or null to keep the application date
     * @param admissionDate   the admission date for an application that has none yet, or null
     */
    public int transition(UUID id, int expected, String status, OffsetDateTime applicationDate,
            LocalDate admissionDate, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION)
                .param("id", id)
                .param("expected", expected)
                .param("status", status)
                .param("applicationDate", applicationDate, Types.TIMESTAMP_WITH_TIMEZONE)
                .param("admissionDate", admissionDate, Types.DATE)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** How many applications of the cycle count against its number of places. */
    public long countInCycle(UUID admissionCycleId) {
        return jdbc.sql(COUNT_IN_CYCLE).param("admissionCycleId", admissionCycleId).query(Long.class).single();
    }

    /** Keeps the applicant's person on an admitted application; 0 rows means the row changed since it was read. */
    public int recordApplicantPerson(UUID id, int expected, UUID personId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RECORD_APPLICANT_PERSON)
                .param("id", id)
                .param("expected", expected)
                .param("personId", personId)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Keeps the student on an admitted application; 0 rows means the row changed since it was read. */
    public int recordStudent(UUID id, int expected, UUID studentId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RECORD_STUDENT)
                .param("id", id)
                .param("expected", expected)
                .param("studentId", studentId)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Marks an admitted application with a student done; 0 rows means it changed, or has no student. */
    public int completeEnrollment(UUID id, int expected, UUID sectionId, UUID batchId, OffsetDateTime now,
            UUID actor) {
        return jdbc.sql(COMPLETE_ENROLLMENT)
                .param("id", id)
                .param("expected", expected)
                .param("sectionId", sectionId, Types.OTHER)
                .param("batchId", batchId, Types.OTHER)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only — without their own applications elsewhere. */
    public static SqlFragment inScope(DataScope scope) {
        return new SqlFragment(IN_SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /**
     * Rows of the caller's institutions, and the applications the caller started themselves in an admission cycle (an
     * applicant's own, whatever their institutions).
     */
    public static SqlFragment reach(DataScope scope, UUID applicant) {
        return new SqlFragment(REACH,
                Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray(), "applicant", applicant));
    }

    private static List<Object> sortValues(SortSpec sort, ApplicationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ApplicationEntity map(ResultSet rs, int row) throws SQLException {
        return ApplicationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .admissionCycleId(rs.getObject("admission_cycle_id", UUID.class))
                .applicationNumber(rs.getString("application_number"))
                .applicationDate(rs.getObject("application_date", OffsetDateTime.class))
                .admissionDate(rs.getObject("admission_date", LocalDate.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .gradeLevelId(rs.getObject("grade_level_id", UUID.class))
                .sectionId(rs.getObject("section_id", UUID.class))
                .programId(rs.getObject("program_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .batchId(rs.getObject("batch_id", UUID.class))
                .applicantPersonId(rs.getObject("applicant_person_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .isExistingStudent(rs.getObject("is_existing_student", Boolean.class))
                .honorificId(rs.getObject("honorific_id", UUID.class))
                .fullName(rs.getString("full_name"))
                .firstName(rs.getString("first_name"))
                .middleName(rs.getString("middle_name"))
                .lastName(rs.getString("last_name"))
                .dateOfBirth(rs.getObject("date_of_birth", LocalDate.class))
                .gender(rs.getString("gender"))
                .birthPlace(rs.getString("birth_place"))
                .nationality(rs.getString("nationality"))
                .religion(rs.getString("religion"))
                .socialCategory(rs.getString("social_category"))
                .motherTongue(rs.getString("mother_tongue"))
                .identificationMarks(rs.getString("identification_marks"))
                .nationalIdEnc(rs.getString("national_id_enc"))
                .bloodGroup(rs.getString("blood_group"))
                .heightCm(rs.getBigDecimal("height_cm"))
                .weightKg(rs.getBigDecimal("weight_kg"))
                .photoFileId(rs.getObject("photo_file_id", UUID.class))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .mobile(rs.getString("mobile"))
                .addressLine1(rs.getString("address_line1"))
                .addressLine2(rs.getString("address_line2"))
                .cityName(rs.getString("city_name"))
                .postalCode(rs.getString("postal_code"))
                .stateProvinceId(rs.getObject("state_province_id", UUID.class))
                .countryId(rs.getObject("country_id", UUID.class))
                .permanentAddress(rs.getString("permanent_address"))
                .communicationAddress(rs.getString("communication_address"))
                .previousInstitution(rs.getString("previous_institution"))
                .previousCourse(rs.getString("previous_course"))
                .previousResult(rs.getString("previous_result"))
                .previousBoard(rs.getString("previous_board"))
                .boardRegistrationNumber(rs.getString("board_registration_number"))
                .reasonForLeaving(rs.getString("reason_for_leaving"))
                .languageChoice(rs.getString("language_choice"))
                .streamChoice(rs.getString("stream_choice"))
                .hasSiblings(rs.getObject("has_siblings", Boolean.class))
                .hasMedicalHistory(rs.getObject("has_medical_history", Boolean.class))
                .medicalDetails(rs.getString("medical_details"))
                .hasExtracurricular(rs.getObject("has_extracurricular", Boolean.class))
                .extracurricularDetails(rs.getString("extracurricular_details"))
                .transportMode(rs.getString("transport_mode"))
                .transportDetails(rs.getString("transport_details"))
                .familyBusiness(rs.getString("family_business"))
                .familyIncome(rs.getBigDecimal("family_income"))
                .applicationFee(rs.getBigDecimal("application_fee"))
                .discountPercent(rs.getBigDecimal("discount_percent"))
                .feePlanId(rs.getObject("fee_plan_id", UUID.class))
                .feeStartDate(rs.getObject("fee_start_date", LocalDate.class))
                .paymentDueDate(rs.getObject("payment_due_date", LocalDate.class))
                .paymentStatus(rs.getString("payment_status"))
                .status(rs.getString("status"))
                .remarks(rs.getString("remarks"))
                .paymentStatusNote(rs.getString("payment_status_note"))
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
