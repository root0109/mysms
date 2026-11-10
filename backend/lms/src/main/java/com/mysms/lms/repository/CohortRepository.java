package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.CohortEntity;
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
 * Plain-JDBC access to lms.cohort (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class CohortRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("classSubjectId", "r.class_subject_id", ValueType.UUID),
            field("batchId", "r.batch_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("details", "r.details", ValueType.STRING),
            field("categoryId", "r.category_id", ValueType.UUID),
            field("medium", "r.medium", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE),
            field("endDate", "r.end_date", ValueType.DATE),
            field("timezone", "r.timezone", ValueType.STRING),
            field("seatCount", "r.seat_count", ValueType.INTEGER),
            field("isPublished", "r.is_published", ValueType.BOOLEAN),
            field("allowSelfEnrollment", "r.allow_self_enrollment", ValueType.BOOLEAN),
            field("allowFutureAccess", "r.allow_future_access", ValueType.BOOLEAN),
            field("isPaid", "r.is_paid", ValueType.BOOLEAN),
            field("price", "r.price", ValueType.DECIMAL),
            field("currencyId", "r.currency_id", ValueType.UUID),
            field("priceUsd", "r.price_usd", ValueType.DECIMAL),
            field("showLiveClass", "r.show_live_class", ValueType.BOOLEAN),
            field("evaluationEnabled", "r.evaluation_enabled", ValueType.BOOLEAN),
            field("evaluationEndDate", "r.evaluation_end_date", ValueType.DATE),
            field("certificationEnabled", "r.certification_enabled", ValueType.BOOLEAN),
            field("conferencingProvider", "r.conferencing_provider", ValueType.STRING),
            field("conferencingAccountId", "r.conferencing_account_id", ValueType.UUID),
            field("timetableTemplateId", "r.timetable_template_id", ValueType.UUID),
            field("confirmationTemplateId", "r.confirmation_template_id", ValueType.UUID),
            field("metaImageFileId", "r.meta_image_file_id", ValueType.UUID),
            field("introVideoFileId", "r.intro_video_file_id", ValueType.UUID),
            field("customComponent", "r.custom_component", ValueType.STRING),
            field("customScript", "r.custom_script", ValueType.STRING),
            field("publishNotificationSent", "r.publish_notification_sent", ValueType.BOOLEAN),
            field("code", "r.code", ValueType.STRING),
            field("academicTermId", "r.academic_term_id", ValueType.UUID),
            field("programId", "r.program_id", ValueType.UUID),
            field("externalRef", "r.external_ref", ValueType.STRING),
            field("lifecycleStatus", "r.lifecycle_status", ValueType.STRING),
            field("staffAccessFrom", "r.staff_access_from", ValueType.TIMESTAMP),
            field("staffAccessUntil", "r.staff_access_until", ValueType.TIMESTAMP),
            field("studentAccessFrom", "r.student_access_from", ValueType.TIMESTAMP),
            field("studentAccessUntil", "r.student_access_until", ValueType.TIMESTAMP),
            field("hasSchedule", "r.has_schedule", ValueType.BOOLEAN),
            field("staffCanEditSchedule", "r.staff_can_edit_schedule", ValueType.BOOLEAN),
            field("gradeDisplay", "r.grade_display", ValueType.STRING),
            field("gradingSchemeId", "r.grading_scheme_id", ValueType.UUID),
            field("enrollmentOpensAt", "r.enrollment_opens_at", ValueType.TIMESTAMP),
            field("enrollmentClosesAt", "r.enrollment_closes_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CohortEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CohortEntity::getCreatedAt),
            Map.entry("updateTime", CohortEntity::getUpdatedAt),
            Map.entry("title", CohortEntity::getTitle),
            Map.entry("startDate", CohortEntity::getStartDate));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.class_subject_id, r.batch_id, r.academic_year_id, r.institution_id, r.title,
                   r.description, r.details, r.category_id, r.medium, r.start_date, r.end_date, r.start_time,
                   r.end_time, r.timezone, r.seat_count, r.is_published, r.allow_self_enrollment,
                   r.allow_future_access, r.is_paid, r.price, r.currency_id, r.price_usd, r.show_live_class,
                   r.evaluation_enabled, r.evaluation_end_date, r.certification_enabled, r.conferencing_provider,
                   r.conferencing_account_id, r.timetable_template_id, r.confirmation_template_id,
                   r.meta_image_file_id, r.intro_video_file_id, r.custom_component, r.custom_script,
                   r.publish_notification_sent, r.code, r.academic_term_id, r.program_id, r.external_ref,
                   r.lifecycle_status, r.staff_access_from, r.staff_access_until, r.student_access_from,
                   r.student_access_until, r.has_schedule, r.staff_can_edit_schedule, r.grade_display,
                   r.grading_scheme_id, r.enrollment_opens_at, r.enrollment_closes_at, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.cohort r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.cohort
            (id, tenant_id, class_subject_id, batch_id, academic_year_id, institution_id, title, description,
             details, category_id, medium, start_date, end_date, start_time, end_time, timezone, seat_count,
             allow_self_enrollment, allow_future_access, is_paid, price, currency_id, price_usd, show_live_class,
             evaluation_enabled, evaluation_end_date, certification_enabled, conferencing_provider,
             conferencing_account_id, timetable_template_id, confirmation_template_id, meta_image_file_id,
             intro_video_file_id, custom_component, custom_script, publish_notification_sent, code, academic_term_id,
             program_id, external_ref, lifecycle_status, staff_access_from, staff_access_until, student_access_from,
             student_access_until, has_schedule, staff_can_edit_schedule, grade_display, grading_scheme_id,
             enrollment_opens_at, enrollment_closes_at, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :classSubjectId, :batchId, :academicYearId, :institutionId, :title, :description,
                    :details, :categoryId, COALESCE(:medium, 'online'::character varying), :startDate, :endDate,
                    :startTime, :endTime, :timezone, :seatCount, COALESCE(:allowSelfEnrollment, false),
                    COALESCE(:allowFutureAccess, true), COALESCE(:isPaid, false), :price, :currencyId, :priceUsd,
                    COALESCE(:showLiveClass, false), COALESCE(:evaluationEnabled, false), :evaluationEndDate,
                    COALESCE(:certificationEnabled, false), :conferencingProvider, :conferencingAccountId,
                    :timetableTemplateId, :confirmationTemplateId, :metaImageFileId, :introVideoFileId,
                    :customComponent, :customScript, COALESCE(:publishNotificationSent, false), :code,
                    :academicTermId, :programId, :externalRef,
                    COALESCE(:lifecycleStatus, 'draft'::character varying), :staffAccessFrom, :staffAccessUntil,
                    :studentAccessFrom, :studentAccessUntil, COALESCE(:hasSchedule, false),
                    COALESCE(:staffCanEditSchedule, false), COALESCE(:gradeDisplay, 'points'::character varying),
                    :gradingSchemeId, :enrollmentOpensAt, :enrollmentClosesAt, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, class_subject_id, batch_id, academic_year_id, institution_id, title, description,
                      details, category_id, medium, start_date, end_date, start_time, end_time, timezone, seat_count,
                      is_published, allow_self_enrollment, allow_future_access, is_paid, price, currency_id, price_usd,
                      show_live_class, evaluation_enabled, evaluation_end_date, certification_enabled,
                      conferencing_provider, conferencing_account_id, timetable_template_id, confirmation_template_id,
                      meta_image_file_id, intro_video_file_id, custom_component, custom_script,
                      publish_notification_sent, code, academic_term_id, program_id, external_ref, lifecycle_status,
                      staff_access_from, staff_access_until, student_access_from, student_access_until, has_schedule,
                      staff_can_edit_schedule, grade_display, grading_scheme_id, enrollment_opens_at,
                      enrollment_closes_at, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.cohort
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.cohort
               SET class_subject_id = :classSubjectId,
                   batch_id = :batchId,
                   academic_year_id = :academicYearId,
                   institution_id = :institutionId,
                   title = :title,
                   description = :description,
                   details = :details,
                   category_id = :categoryId,
                   medium = :medium,
                   start_date = :startDate,
                   end_date = :endDate,
                   start_time = :startTime,
                   end_time = :endTime,
                   timezone = :timezone,
                   seat_count = :seatCount,
                   allow_self_enrollment = :allowSelfEnrollment,
                   allow_future_access = :allowFutureAccess,
                   is_paid = :isPaid,
                   price = :price,
                   currency_id = :currencyId,
                   price_usd = :priceUsd,
                   show_live_class = :showLiveClass,
                   evaluation_enabled = :evaluationEnabled,
                   evaluation_end_date = :evaluationEndDate,
                   certification_enabled = :certificationEnabled,
                   conferencing_provider = :conferencingProvider,
                   conferencing_account_id = :conferencingAccountId,
                   timetable_template_id = :timetableTemplateId,
                   confirmation_template_id = :confirmationTemplateId,
                   meta_image_file_id = :metaImageFileId,
                   intro_video_file_id = :introVideoFileId,
                   custom_component = :customComponent,
                   custom_script = :customScript,
                   publish_notification_sent = :publishNotificationSent,
                   code = :code,
                   academic_term_id = :academicTermId,
                   program_id = :programId,
                   external_ref = :externalRef,
                   lifecycle_status = :lifecycleStatus,
                   staff_access_from = :staffAccessFrom,
                   staff_access_until = :staffAccessUntil,
                   student_access_from = :studentAccessFrom,
                   student_access_until = :studentAccessUntil,
                   has_schedule = :hasSchedule,
                   staff_can_edit_schedule = :staffCanEditSchedule,
                   grade_display = :gradeDisplay,
                   grading_scheme_id = :gradingSchemeId,
                   enrollment_opens_at = :enrollmentOpensAt,
                   enrollment_closes_at = :enrollmentClosesAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CATEGORY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.category WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONFERENCING_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.conferencing_account WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_TIMETABLE_TEMPLATE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_template WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ACCESS_LOG_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.access_log WHERE tenant_id = :tenantId AND cohort_id = :id)
            """;
    private static final String REFERENCED_BY_ANNOTATION_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.annotation WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_GROUP_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_BOOKMARK_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.bookmark WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CERTIFICATE_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CERTIFICATE_EVALUATION_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate_evaluation WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CERTIFICATE_REQUEST_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.certificate_request WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CHAPTER_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_COURSE_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_course WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_ENROLLMENT_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_enrollment WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_FEEDBACK_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_feedback WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_GROUP_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_group WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_HOLIDAY_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_holiday WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_SCHEDULE_SLOT_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_schedule_slot WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_SESSION_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_session WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COHORT_TEACHER_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_teacher WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_RATING_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item_rating WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_VIEW_SESSION_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_view_session WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ENROLLMENT_SOURCE_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.enrollment WHERE tenant_id = :tenantId
                AND source_cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ENROLLMENT_REQUEST_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.enrollment_request WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_GRADEBOOK_ENTRY_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.gradebook_entry WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_GRADING_CATEGORY_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.grading_category WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_IMPORT_EXPORT_JOB_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.import_export_job WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LIVE_CLASS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.live_class WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PLAGIARISM_COHORT_LINK_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.plagiarism_cohort_link WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_SUBMISSION_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_RULE_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STUDENT_ALERT_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.student_alert WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TIMETABLE_ENTRY_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_entry WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TIMETABLE_LEGEND_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_legend WHERE tenant_id = :tenantId
                AND cohort_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public CohortRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CohortEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CohortRepository::map).optional();
    }

    public Page<CohortEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CohortEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CohortRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CohortEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CohortEntity insert(CohortEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("classSubjectId", e.getClassSubjectId())
                .param("batchId", e.getBatchId())
                .param("academicYearId", e.getAcademicYearId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("details", e.getDetails())
                .param("categoryId", e.getCategoryId())
                .param("medium", e.getMedium())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("timezone", e.getTimezone())
                .param("seatCount", e.getSeatCount())
                .param("allowSelfEnrollment", e.getAllowSelfEnrollment())
                .param("allowFutureAccess", e.getAllowFutureAccess())
                .param("isPaid", e.getIsPaid())
                .param("price", e.getPrice())
                .param("currencyId", e.getCurrencyId())
                .param("priceUsd", e.getPriceUsd())
                .param("showLiveClass", e.getShowLiveClass())
                .param("evaluationEnabled", e.getEvaluationEnabled())
                .param("evaluationEndDate", e.getEvaluationEndDate())
                .param("certificationEnabled", e.getCertificationEnabled())
                .param("conferencingProvider", e.getConferencingProvider())
                .param("conferencingAccountId", e.getConferencingAccountId())
                .param("timetableTemplateId", e.getTimetableTemplateId())
                .param("confirmationTemplateId", e.getConfirmationTemplateId())
                .param("metaImageFileId", e.getMetaImageFileId())
                .param("introVideoFileId", e.getIntroVideoFileId())
                .param("customComponent", e.getCustomComponent())
                .param("customScript", e.getCustomScript())
                .param("publishNotificationSent", e.getPublishNotificationSent())
                .param("code", e.getCode())
                .param("academicTermId", e.getAcademicTermId())
                .param("programId", e.getProgramId())
                .param("externalRef", e.getExternalRef())
                .param("lifecycleStatus", e.getLifecycleStatus())
                .param("staffAccessFrom", e.getStaffAccessFrom())
                .param("staffAccessUntil", e.getStaffAccessUntil())
                .param("studentAccessFrom", e.getStudentAccessFrom())
                .param("studentAccessUntil", e.getStudentAccessUntil())
                .param("hasSchedule", e.getHasSchedule())
                .param("staffCanEditSchedule", e.getStaffCanEditSchedule())
                .param("gradeDisplay", e.getGradeDisplay())
                .param("gradingSchemeId", e.getGradingSchemeId())
                .param("enrollmentOpensAt", e.getEnrollmentOpensAt())
                .param("enrollmentClosesAt", e.getEnrollmentClosesAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CohortRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CohortEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("classSubjectId", e.getClassSubjectId())
                .param("batchId", e.getBatchId())
                .param("academicYearId", e.getAcademicYearId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("details", e.getDetails())
                .param("categoryId", e.getCategoryId())
                .param("medium", e.getMedium())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("timezone", e.getTimezone())
                .param("seatCount", e.getSeatCount())
                .param("allowSelfEnrollment", e.getAllowSelfEnrollment())
                .param("allowFutureAccess", e.getAllowFutureAccess())
                .param("isPaid", e.getIsPaid())
                .param("price", e.getPrice())
                .param("currencyId", e.getCurrencyId())
                .param("priceUsd", e.getPriceUsd())
                .param("showLiveClass", e.getShowLiveClass())
                .param("evaluationEnabled", e.getEvaluationEnabled())
                .param("evaluationEndDate", e.getEvaluationEndDate())
                .param("certificationEnabled", e.getCertificationEnabled())
                .param("conferencingProvider", e.getConferencingProvider())
                .param("conferencingAccountId", e.getConferencingAccountId())
                .param("timetableTemplateId", e.getTimetableTemplateId())
                .param("confirmationTemplateId", e.getConfirmationTemplateId())
                .param("metaImageFileId", e.getMetaImageFileId())
                .param("introVideoFileId", e.getIntroVideoFileId())
                .param("customComponent", e.getCustomComponent())
                .param("customScript", e.getCustomScript())
                .param("publishNotificationSent", e.getPublishNotificationSent())
                .param("code", e.getCode())
                .param("academicTermId", e.getAcademicTermId())
                .param("programId", e.getProgramId())
                .param("externalRef", e.getExternalRef())
                .param("lifecycleStatus", e.getLifecycleStatus())
                .param("staffAccessFrom", e.getStaffAccessFrom())
                .param("staffAccessUntil", e.getStaffAccessUntil())
                .param("studentAccessFrom", e.getStudentAccessFrom())
                .param("studentAccessUntil", e.getStudentAccessUntil())
                .param("hasSchedule", e.getHasSchedule())
                .param("staffCanEditSchedule", e.getStaffCanEditSchedule())
                .param("gradeDisplay", e.getGradeDisplay())
                .param("gradingSchemeId", e.getGradingSchemeId())
                .param("enrollmentOpensAt", e.getEnrollmentOpensAt())
                .param("enrollmentClosesAt", e.getEnrollmentClosesAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CohortEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.category row exists (live). */
    public boolean existsCategoryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CATEGORY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.conferencing_account row exists (live). */
    public boolean existsConferencingAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONFERENCING_ACCOUNT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced lms.timetable_template row exists (live). */
    public boolean existsTimetableTemplateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_TIMETABLE_TEMPLATE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.access_log rows still reference the row. */
    public boolean referencedByAccessLogCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ACCESS_LOG_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.annotation rows still reference the row. */
    public boolean referencedByAnnotationCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ANNOTATION_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.assignment_group rows still reference the row. */
    public boolean referencedByAssignmentGroupCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_GROUP_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.bookmark rows still reference the row. */
    public boolean referencedByBookmarkCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_BOOKMARK_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.certificate rows still reference the row. */
    public boolean referencedByCertificateCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CERTIFICATE_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.certificate_evaluation rows still reference the row. */
    public boolean referencedByCertificateEvaluationCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CERTIFICATE_EVALUATION_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.certificate_request rows still reference the row. */
    public boolean referencedByCertificateRequestCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CERTIFICATE_REQUEST_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.chapter rows still reference the row. */
    public boolean referencedByChapterCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CHAPTER_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.cohort_course rows still reference the row. */
    public boolean referencedByCohortCourseCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_COURSE_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.cohort_enrollment rows still reference the row. */
    public boolean referencedByCohortEnrollmentCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_ENROLLMENT_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.cohort_feedback rows still reference the row. */
    public boolean referencedByCohortFeedbackCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_FEEDBACK_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.cohort_group rows still reference the row. */
    public boolean referencedByCohortGroupCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_GROUP_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.cohort_holiday rows still reference the row. */
    public boolean referencedByCohortHolidayCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_HOLIDAY_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.cohort_schedule_slot rows still reference the row. */
    public boolean referencedByCohortScheduleSlotCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_SCHEDULE_SLOT_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.cohort_session rows still reference the row. */
    public boolean referencedByCohortSessionCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_SESSION_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.cohort_teacher rows still reference the row. */
    public boolean referencedByCohortTeacherCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COHORT_TEACHER_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.content_item_rating rows still reference the row. */
    public boolean referencedByContentItemRatingCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_RATING_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_view_session rows still reference the row. */
    public boolean referencedByContentViewSessionCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_VIEW_SESSION_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.enrollment rows still reference the row. */
    public boolean referencedByEnrollmentSourceCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ENROLLMENT_SOURCE_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.enrollment_request rows still reference the row. */
    public boolean referencedByEnrollmentRequestCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ENROLLMENT_REQUEST_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum rows still reference the row. */
    public boolean referencedByForumCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.gradebook_entry rows still reference the row. */
    public boolean referencedByGradebookEntryCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_GRADEBOOK_ENTRY_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.grading_category rows still reference the row. */
    public boolean referencedByGradingCategoryCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_GRADING_CATEGORY_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.import_export_job rows still reference the row. */
    public boolean referencedByImportExportJobCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_IMPORT_EXPORT_JOB_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.live_class rows still reference the row. */
    public boolean referencedByLiveClassCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LIVE_CLASS_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.plagiarism_cohort_link rows still reference the row. */
    public boolean referencedByPlagiarismCohortLinkCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PLAGIARISM_COHORT_LINK_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_submission rows still reference the row. */
    public boolean referencedByQuizSubmissionCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_SUBMISSION_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_rule rows still reference the row. */
    public boolean referencedByReleaseRuleCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_RULE_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.student_alert rows still reference the row. */
    public boolean referencedByStudentAlertCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STUDENT_ALERT_COHORT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.timetable_entry rows still reference the row. */
    public boolean referencedByTimetableEntryCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TIMETABLE_ENTRY_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.timetable_legend rows still reference the row. */
    public boolean referencedByTimetableLegendCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TIMETABLE_LEGEND_COHORT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, CohortEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CohortEntity map(ResultSet rs, int row) throws SQLException {
        return CohortEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .classSubjectId(rs.getObject("class_subject_id", UUID.class))
                .batchId(rs.getObject("batch_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .details(rs.getString("details"))
                .categoryId(rs.getObject("category_id", UUID.class))
                .medium(rs.getString("medium"))
                .startDate(rs.getObject("start_date", LocalDate.class))
                .endDate(rs.getObject("end_date", LocalDate.class))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .endTime(rs.getObject("end_time", LocalTime.class))
                .timezone(rs.getString("timezone"))
                .seatCount(rs.getObject("seat_count", Integer.class))
                .isPublished(rs.getObject("is_published", Boolean.class))
                .allowSelfEnrollment(rs.getObject("allow_self_enrollment", Boolean.class))
                .allowFutureAccess(rs.getObject("allow_future_access", Boolean.class))
                .isPaid(rs.getObject("is_paid", Boolean.class))
                .price(rs.getBigDecimal("price"))
                .currencyId(rs.getObject("currency_id", UUID.class))
                .priceUsd(rs.getBigDecimal("price_usd"))
                .showLiveClass(rs.getObject("show_live_class", Boolean.class))
                .evaluationEnabled(rs.getObject("evaluation_enabled", Boolean.class))
                .evaluationEndDate(rs.getObject("evaluation_end_date", LocalDate.class))
                .certificationEnabled(rs.getObject("certification_enabled", Boolean.class))
                .conferencingProvider(rs.getString("conferencing_provider"))
                .conferencingAccountId(rs.getObject("conferencing_account_id", UUID.class))
                .timetableTemplateId(rs.getObject("timetable_template_id", UUID.class))
                .confirmationTemplateId(rs.getObject("confirmation_template_id", UUID.class))
                .metaImageFileId(rs.getObject("meta_image_file_id", UUID.class))
                .introVideoFileId(rs.getObject("intro_video_file_id", UUID.class))
                .customComponent(rs.getString("custom_component"))
                .customScript(rs.getString("custom_script"))
                .publishNotificationSent(rs.getObject("publish_notification_sent", Boolean.class))
                .code(rs.getString("code"))
                .academicTermId(rs.getObject("academic_term_id", UUID.class))
                .programId(rs.getObject("program_id", UUID.class))
                .externalRef(rs.getString("external_ref"))
                .lifecycleStatus(rs.getString("lifecycle_status"))
                .staffAccessFrom(rs.getObject("staff_access_from", OffsetDateTime.class))
                .staffAccessUntil(rs.getObject("staff_access_until", OffsetDateTime.class))
                .studentAccessFrom(rs.getObject("student_access_from", OffsetDateTime.class))
                .studentAccessUntil(rs.getObject("student_access_until", OffsetDateTime.class))
                .hasSchedule(rs.getObject("has_schedule", Boolean.class))
                .staffCanEditSchedule(rs.getObject("staff_can_edit_schedule", Boolean.class))
                .gradeDisplay(rs.getString("grade_display"))
                .gradingSchemeId(rs.getObject("grading_scheme_id", UUID.class))
                .enrollmentOpensAt(rs.getObject("enrollment_opens_at", OffsetDateTime.class))
                .enrollmentClosesAt(rs.getObject("enrollment_closes_at", OffsetDateTime.class))
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
