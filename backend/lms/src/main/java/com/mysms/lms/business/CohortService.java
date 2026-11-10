package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.Cohort;
import com.mysms.lms.api.model.CohortCourse;
import com.mysms.lms.api.model.CohortCourseUpdate;
import com.mysms.lms.api.model.CohortFeedback;
import com.mysms.lms.api.model.CohortFeedbackUpdate;
import com.mysms.lms.api.model.CohortGroup;
import com.mysms.lms.api.model.CohortGroupUpdate;
import com.mysms.lms.api.model.CohortHoliday;
import com.mysms.lms.api.model.CohortHolidayUpdate;
import com.mysms.lms.api.model.CohortScheduleSlot;
import com.mysms.lms.api.model.CohortScheduleSlotUpdate;
import com.mysms.lms.api.model.CohortSession;
import com.mysms.lms.api.model.CohortSessionUpdate;
import com.mysms.lms.api.model.CohortTeacher;
import com.mysms.lms.api.model.CohortTeacherUpdate;
import com.mysms.lms.api.model.CohortUpdate;
import com.mysms.lms.api.model.Forum;
import com.mysms.lms.api.model.ForumUpdate;
import com.mysms.lms.api.model.PlagiarismCohortLink;
import com.mysms.lms.api.model.PlagiarismCohortLinkUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.CohortCourseEntity;
import com.mysms.lms.domain.CohortEntity;
import com.mysms.lms.domain.CohortFeedbackEntity;
import com.mysms.lms.domain.CohortGroupEntity;
import com.mysms.lms.domain.CohortHolidayEntity;
import com.mysms.lms.domain.CohortScheduleSlotEntity;
import com.mysms.lms.domain.CohortSessionEntity;
import com.mysms.lms.domain.CohortTeacherEntity;
import com.mysms.lms.domain.ForumEntity;
import com.mysms.lms.domain.PlagiarismCohortLinkEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.CohortCourseMapper;
import com.mysms.lms.mapper.CohortFeedbackMapper;
import com.mysms.lms.mapper.CohortGroupMapper;
import com.mysms.lms.mapper.CohortHolidayMapper;
import com.mysms.lms.mapper.CohortMapper;
import com.mysms.lms.mapper.CohortScheduleSlotMapper;
import com.mysms.lms.mapper.CohortSessionMapper;
import com.mysms.lms.mapper.CohortTeacherMapper;
import com.mysms.lms.mapper.ForumMapper;
import com.mysms.lms.mapper.PlagiarismCohortLinkMapper;
import com.mysms.lms.repository.CohortCourseRepository;
import com.mysms.lms.repository.CohortFeedbackRepository;
import com.mysms.lms.repository.CohortGroupRepository;
import com.mysms.lms.repository.CohortHolidayRepository;
import com.mysms.lms.repository.CohortRepository;
import com.mysms.lms.repository.CohortScheduleSlotRepository;
import com.mysms.lms.repository.CohortSessionRepository;
import com.mysms.lms.repository.CohortTeacherRepository;
import com.mysms.lms.repository.ForumRepository;
import com.mysms.lms.repository.PlagiarismCohortLinkRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.cohort (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Cohort
 * are served here too — CohortCourse, CohortFeedback, CohortGroup, CohortHoliday, CohortScheduleSlot, CohortSession,
 * CohortTeacher, Forum, PlagiarismCohortLink.
 */
@Service
public class CohortService extends AppTransactionManager {

    public static final String AGGREGATE = CohortEntity.TABLE;
    static final String PERMISSION_CREATE = "lms.cohort.create";
    static final String PERMISSION_UPDATE = "lms.cohort.update";
    static final String PERMISSION_DELETE = "lms.cohort.delete";
    /** The contract's CohortUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("classSubjectId", "batchId", "academicYearId", "institutionId", "title", "description", "details",
            "categoryId", "medium", "startDate", "endDate", "startTime", "endTime", "timezone", "seatCount",
            "allowSelfEnrollment", "allowFutureAccess", "isPaid", "price", "currencyId", "priceUsd", "showLiveClass",
            "evaluationEnabled", "evaluationEndDate", "certificationEnabled", "conferencingProvider",
            "conferencingAccountId", "timetableTemplateId", "confirmationTemplateId", "metaImageFileId",
            "introVideoFileId", "customComponent", "customScript", "publishNotificationSent", "code", "academicTermId",
            "programId", "externalRef", "lifecycleStatus", "staffAccessFrom", "staffAccessUntil", "studentAccessFrom",
            "studentAccessUntil", "hasSchedule", "staffCanEditSchedule", "gradeDisplay", "gradingSchemeId",
            "enrollmentOpensAt", "enrollmentClosesAt");

    public static final String COHORT_COURSE_AGGREGATE = CohortCourseEntity.TABLE;
    static final String COHORT_COURSE_PERMISSION_GET = "lms.cohort_course.read";
    static final String COHORT_COURSE_PERMISSION_CREATE = "lms.cohort_course.create";
    static final String COHORT_COURSE_PERMISSION_UPDATE = "lms.cohort_course.update";
    static final String COHORT_COURSE_PERMISSION_DELETE = "lms.cohort_course.delete";
    /** The contract's CohortCourseUpdate fields, in apply order. */
    static final List<String> COHORT_COURSE_WRITABLE = List.of("courseId", "evaluatorId");

    public static final String COHORT_FEEDBACK_AGGREGATE = CohortFeedbackEntity.TABLE;
    static final String COHORT_FEEDBACK_PERMISSION_LIST = "lms.cohort_feedback.read";
    static final String COHORT_FEEDBACK_PERMISSION_GET = "lms.cohort_feedback.read";
    static final String COHORT_FEEDBACK_PERMISSION_CREATE = "lms.cohort_feedback.create";
    static final String COHORT_FEEDBACK_PERMISSION_UPDATE = "lms.cohort_feedback.update";
    static final String COHORT_FEEDBACK_PERMISSION_DELETE = "lms.cohort_feedback.delete";
    /** The contract's CohortFeedbackUpdate fields, in apply order. */
    static final List<String> COHORT_FEEDBACK_WRITABLE = List.of("userId", "feedback", "contentRating", "teacherRating",
            "valueRating");

    public static final String COHORT_GROUP_AGGREGATE = CohortGroupEntity.TABLE;
    static final String COHORT_GROUP_PERMISSION_LIST = "lms.cohort_group.read";
    static final String COHORT_GROUP_PERMISSION_GET = "lms.cohort_group.read";
    static final String COHORT_GROUP_PERMISSION_CREATE = "lms.cohort_group.create";
    static final String COHORT_GROUP_PERMISSION_UPDATE = "lms.cohort_group.update";
    static final String COHORT_GROUP_PERMISSION_DELETE = "lms.cohort_group.delete";
    /** The contract's CohortGroupUpdate fields, in apply order. */
    static final List<String> COHORT_GROUP_WRITABLE = List.of("name", "description", "isDisabled");

    public static final String COHORT_HOLIDAY_AGGREGATE = CohortHolidayEntity.TABLE;
    static final String COHORT_HOLIDAY_PERMISSION_LIST = "lms.cohort_holiday.read";
    static final String COHORT_HOLIDAY_PERMISSION_GET = "lms.cohort_holiday.read";
    static final String COHORT_HOLIDAY_PERMISSION_CREATE = "lms.cohort_holiday.create";
    static final String COHORT_HOLIDAY_PERMISSION_UPDATE = "lms.cohort_holiday.update";
    static final String COHORT_HOLIDAY_PERMISSION_DELETE = "lms.cohort_holiday.delete";
    /** The contract's CohortHolidayUpdate fields, in apply order. */
    static final List<String> COHORT_HOLIDAY_WRITABLE = List.of("holidayId", "title", "startDate", "endDate", "isIncluded");

    public static final String COHORT_SCHEDULE_SLOT_AGGREGATE = CohortScheduleSlotEntity.TABLE;
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_LIST = "lms.cohort_schedule_slot.read";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_GET = "lms.cohort_schedule_slot.read";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_CREATE = "lms.cohort_schedule_slot.create";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_UPDATE = "lms.cohort_schedule_slot.update";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_DELETE = "lms.cohort_schedule_slot.delete";
    /** The contract's CohortScheduleSlotUpdate fields, in apply order. */
    static final List<String> COHORT_SCHEDULE_SLOT_WRITABLE =
            List.of("slotType", "cohortTeacherId", "weekday", "startTime", "endTime", "venue");

    public static final String COHORT_SESSION_AGGREGATE = CohortSessionEntity.TABLE;
    static final String COHORT_SESSION_PERMISSION_LIST = "lms.cohort_session.read";
    static final String COHORT_SESSION_PERMISSION_GET = "lms.cohort_session.read";
    static final String COHORT_SESSION_PERMISSION_CREATE = "lms.cohort_session.create";
    static final String COHORT_SESSION_PERMISSION_UPDATE = "lms.cohort_session.update";
    static final String COHORT_SESSION_PERMISSION_DELETE = "lms.cohort_session.delete";
    /** The contract's CohortSessionUpdate fields, in apply order. */
    static final List<String> COHORT_SESSION_WRITABLE = List.of("startsAt", "endsAt", "description", "venue", "isCancelled");

    public static final String COHORT_TEACHER_AGGREGATE = CohortTeacherEntity.TABLE;
    static final String COHORT_TEACHER_PERMISSION_LIST = "lms.cohort_teacher.read";
    static final String COHORT_TEACHER_PERMISSION_GET = "lms.cohort_teacher.read";
    static final String COHORT_TEACHER_PERMISSION_CREATE = "lms.cohort_teacher.create";
    static final String COHORT_TEACHER_PERMISSION_UPDATE = "lms.cohort_teacher.update";
    static final String COHORT_TEACHER_PERMISSION_DELETE = "lms.cohort_teacher.delete";
    /** The contract's CohortTeacherUpdate fields, in apply order. */
    static final List<String> COHORT_TEACHER_WRITABLE =
            List.of("userId", "roleId", "roleLabel", "isActive", "hasOfficeHours", "removedAt");

    public static final String FORUM_AGGREGATE = ForumEntity.TABLE;
    static final String FORUM_PERMISSION_LIST = "lms.forum.read";
    static final String FORUM_PERMISSION_GET = "lms.forum.read";
    static final String FORUM_PERMISSION_CREATE = "lms.forum.create";
    static final String FORUM_PERMISSION_UPDATE = "lms.forum.update";
    static final String FORUM_PERMISSION_DELETE = "lms.forum.delete";
    /** The contract's ForumUpdate fields, in apply order. */
    static final List<String> FORUM_WRITABLE =
            List.of("title", "description", "earlyAccessFrom", "opensAt", "closesAt", "lateAccessUntil");

    public static final String PLAGIARISM_COHORT_LINK_AGGREGATE = PlagiarismCohortLinkEntity.TABLE;
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_LIST = "lms.plagiarism_cohort_link.read";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_GET = "lms.plagiarism_cohort_link.read";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_CREATE = "lms.plagiarism_cohort_link.create";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_UPDATE = "lms.plagiarism_cohort_link.update";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_DELETE = "lms.plagiarism_cohort_link.delete";
    /** The contract's PlagiarismCohortLinkUpdate fields, in apply order. */
    static final List<String> PLAGIARISM_COHORT_LINK_WRITABLE =
            List.of("plagiarismAccountId", "accountRef", "providerClassRef", "providerClassTitle", "providerClassEndAt",
            "providerAssignmentRef", "providerAssignmentTitle", "providerAssignmentStartAt", "providerAssignmentDueAt",
            "providerAssignmentInstructions", "studentReportAccess", "teacherEmail", "teacherFirstName",
            "teacherLastName", "classCreated", "assignmentCreated");

    private final CohortRepository repository;
    private final CohortMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CohortCourseRepository cohortCourseRepository;
    private final CohortCourseMapper cohortCourseMapper;
    private final CohortFeedbackRepository cohortFeedbackRepository;
    private final CohortFeedbackMapper cohortFeedbackMapper;
    private final CohortGroupRepository cohortGroupRepository;
    private final CohortGroupMapper cohortGroupMapper;
    private final CohortHolidayRepository cohortHolidayRepository;
    private final CohortHolidayMapper cohortHolidayMapper;
    private final CohortScheduleSlotRepository cohortScheduleSlotRepository;
    private final CohortScheduleSlotMapper cohortScheduleSlotMapper;
    private final CohortSessionRepository cohortSessionRepository;
    private final CohortSessionMapper cohortSessionMapper;
    private final CohortTeacherRepository cohortTeacherRepository;
    private final CohortTeacherMapper cohortTeacherMapper;
    private final ForumRepository forumRepository;
    private final ForumMapper forumMapper;
    private final PlagiarismCohortLinkRepository plagiarismCohortLinkRepository;
    private final PlagiarismCohortLinkMapper plagiarismCohortLinkMapper;

    public CohortService(CohortRepository repository,
            CohortMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            CohortCourseRepository cohortCourseRepository,
            CohortCourseMapper cohortCourseMapper,
            CohortFeedbackRepository cohortFeedbackRepository,
            CohortFeedbackMapper cohortFeedbackMapper,
            CohortGroupRepository cohortGroupRepository,
            CohortGroupMapper cohortGroupMapper,
            CohortHolidayRepository cohortHolidayRepository,
            CohortHolidayMapper cohortHolidayMapper,
            CohortScheduleSlotRepository cohortScheduleSlotRepository,
            CohortScheduleSlotMapper cohortScheduleSlotMapper,
            CohortSessionRepository cohortSessionRepository,
            CohortSessionMapper cohortSessionMapper,
            CohortTeacherRepository cohortTeacherRepository,
            CohortTeacherMapper cohortTeacherMapper,
            ForumRepository forumRepository,
            ForumMapper forumMapper,
            PlagiarismCohortLinkRepository plagiarismCohortLinkRepository,
            PlagiarismCohortLinkMapper plagiarismCohortLinkMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.cohortCourseRepository = cohortCourseRepository;
        this.cohortCourseMapper = cohortCourseMapper;
        this.cohortFeedbackRepository = cohortFeedbackRepository;
        this.cohortFeedbackMapper = cohortFeedbackMapper;
        this.cohortGroupRepository = cohortGroupRepository;
        this.cohortGroupMapper = cohortGroupMapper;
        this.cohortHolidayRepository = cohortHolidayRepository;
        this.cohortHolidayMapper = cohortHolidayMapper;
        this.cohortScheduleSlotRepository = cohortScheduleSlotRepository;
        this.cohortScheduleSlotMapper = cohortScheduleSlotMapper;
        this.cohortSessionRepository = cohortSessionRepository;
        this.cohortSessionMapper = cohortSessionMapper;
        this.cohortTeacherRepository = cohortTeacherRepository;
        this.cohortTeacherMapper = cohortTeacherMapper;
        this.forumRepository = forumRepository;
        this.forumMapper = forumMapper;
        this.plagiarismCohortLinkRepository = plagiarismCohortLinkRepository;
        this.plagiarismCohortLinkMapper = plagiarismCohortLinkMapper;
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Cohort createCohort(Cohort request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        CohortEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            CohortEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Cohort updateCohort(UUID id, String ifMatch, CohortUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("endTime", changes.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CohortEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohort(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CohortEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAccessLogCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.access_log rows; remove those first.");
            }
            if (repository.referencedByAnnotationCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.annotation rows; remove those first.");
            }
            if (repository.referencedByAssignmentGroupCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.assignment_group rows; remove those first.");
            }
            if (repository.referencedByBookmarkCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.bookmark rows; remove those first.");
            }
            if (repository.referencedByCertificateCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.certificate rows; remove those first.");
            }
            if (repository.referencedByCertificateEvaluationCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.certificate_evaluation rows; remove those first.");
            }
            if (repository.referencedByCertificateRequestCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.certificate_request rows; remove those first.");
            }
            if (repository.referencedByChapterCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.chapter rows; remove those first.");
            }
            if (repository.referencedByCohortCourseCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_course rows; remove those first.");
            }
            if (repository.referencedByCohortEnrollmentCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_enrollment rows; remove those first.");
            }
            if (repository.referencedByCohortFeedbackCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_feedback rows; remove those first.");
            }
            if (repository.referencedByCohortGroupCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_group rows; remove those first.");
            }
            if (repository.referencedByCohortHolidayCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_holiday rows; remove those first.");
            }
            if (repository.referencedByCohortScheduleSlotCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_schedule_slot rows; remove those first.");
            }
            if (repository.referencedByCohortSessionCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_session rows; remove those first.");
            }
            if (repository.referencedByCohortTeacherCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.cohort_teacher rows; remove those first.");
            }
            if (repository.referencedByContentItemCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByContentItemRatingCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.content_item_rating rows; remove those first.");
            }
            if (repository.referencedByContentViewSessionCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.content_view_session rows; remove those first.");
            }
            if (repository.referencedByEnrollmentSourceCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.enrollment rows; remove those first.");
            }
            if (repository.referencedByEnrollmentRequestCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.enrollment_request rows; remove those first.");
            }
            if (repository.referencedByForumCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.forum rows; remove those first.");
            }
            if (repository.referencedByGradebookEntryCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.gradebook_entry rows; remove those first.");
            }
            if (repository.referencedByGradingCategoryCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.grading_category rows; remove those first.");
            }
            if (repository.referencedByImportExportJobCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.import_export_job rows; remove those first.");
            }
            if (repository.referencedByLiveClassCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.live_class rows; remove those first.");
            }
            if (repository.referencedByPlagiarismCohortLinkCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.plagiarism_cohort_link rows; remove those first.");
            }
            if (repository.referencedByQuizSubmissionCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.quiz_submission rows; remove those first.");
            }
            if (repository.referencedByReleaseRuleCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.release_rule rows; remove those first.");
            }
            if (repository.referencedByStudentAlertCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.student_alert rows; remove those first.");
            }
            if (repository.referencedByTimetableEntryCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.timetable_entry rows; remove those first.");
            }
            if (repository.referencedByTimetableLegendCohortId(id)) {
                throw new InvalidStateException(
                        "The cohort is still referenced by lms.timetable_legend rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortEntity loadVisible(DataScope scope, UUID id) {
        CohortEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(CohortEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected CohortUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(CohortEntity entity, CohortUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "classSubjectId" -> entity.setClassSubjectId(changes.getClassSubjectId());
                case "batchId" -> entity.setBatchId(changes.getBatchId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "details" -> entity.setDetails(changes.getDetails());
                case "categoryId" -> entity.setCategoryId(changes.getCategoryId());
                case "medium" -> entity.setMedium(Optional.ofNullable(changes.getMedium()).map(v -> v.getValue())
                        .orElse(null));
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                case "timezone" -> entity.setTimezone(changes.getTimezone());
                case "seatCount" -> entity.setSeatCount(changes.getSeatCount());
                case "allowSelfEnrollment" -> entity.setAllowSelfEnrollment(changes.getAllowSelfEnrollment());
                case "allowFutureAccess" -> entity.setAllowFutureAccess(changes.getAllowFutureAccess());
                case "isPaid" -> entity.setIsPaid(changes.getIsPaid());
                case "price" -> entity.setPrice(WireValues.decimal(changes.getPrice()));
                case "currencyId" -> entity.setCurrencyId(changes.getCurrencyId());
                case "priceUsd" -> entity.setPriceUsd(WireValues.decimal(changes.getPriceUsd()));
                case "showLiveClass" -> entity.setShowLiveClass(changes.getShowLiveClass());
                case "evaluationEnabled" -> entity.setEvaluationEnabled(changes.getEvaluationEnabled());
                case "evaluationEndDate" -> entity.setEvaluationEndDate(changes.getEvaluationEndDate());
                case "certificationEnabled" -> entity.setCertificationEnabled(changes.getCertificationEnabled());
                case "conferencingProvider" -> entity.setConferencingProvider(
                        Optional.ofNullable(changes.getConferencingProvider()).map(v -> v.getValue()).orElse(null));
                case "conferencingAccountId" -> entity.setConferencingAccountId(changes.getConferencingAccountId());
                case "timetableTemplateId" -> entity.setTimetableTemplateId(changes.getTimetableTemplateId());
                case "confirmationTemplateId" -> entity.setConfirmationTemplateId(changes.getConfirmationTemplateId());
                case "metaImageFileId" -> entity.setMetaImageFileId(changes.getMetaImageFileId());
                case "introVideoFileId" -> entity.setIntroVideoFileId(changes.getIntroVideoFileId());
                case "customComponent" -> entity.setCustomComponent(changes.getCustomComponent());
                case "customScript" -> entity.setCustomScript(changes.getCustomScript());
                case "publishNotificationSent" -> entity.setPublishNotificationSent(
                        changes.getPublishNotificationSent());
                case "code" -> entity.setCode(changes.getCode());
                case "academicTermId" -> entity.setAcademicTermId(changes.getAcademicTermId());
                case "programId" -> entity.setProgramId(changes.getProgramId());
                case "externalRef" -> entity.setExternalRef(changes.getExternalRef());
                case "lifecycleStatus" -> entity.setLifecycleStatus(Optional.ofNullable(changes.getLifecycleStatus())
                        .map(v -> v.getValue()).orElse(null));
                case "staffAccessFrom" -> entity.setStaffAccessFrom(changes.getStaffAccessFrom());
                case "staffAccessUntil" -> entity.setStaffAccessUntil(changes.getStaffAccessUntil());
                case "studentAccessFrom" -> entity.setStudentAccessFrom(changes.getStudentAccessFrom());
                case "studentAccessUntil" -> entity.setStudentAccessUntil(changes.getStudentAccessUntil());
                case "hasSchedule" -> entity.setHasSchedule(changes.getHasSchedule());
                case "staffCanEditSchedule" -> entity.setStaffCanEditSchedule(changes.getStaffCanEditSchedule());
                case "gradeDisplay" -> entity.setGradeDisplay(Optional.ofNullable(changes.getGradeDisplay())
                        .map(v -> v.getValue()).orElse(null));
                case "gradingSchemeId" -> entity.setGradingSchemeId(changes.getGradingSchemeId());
                case "enrollmentOpensAt" -> entity.setEnrollmentOpensAt(changes.getEnrollmentOpensAt());
                case "enrollmentClosesAt" -> entity.setEnrollmentClosesAt(changes.getEnrollmentClosesAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CohortEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getMedium() == null) {
            errors.add(new FieldError("medium", "must not be null", "NotNull"));
        }
        if (entity.getStartDate() == null) {
            errors.add(new FieldError("startDate", "must not be null", "NotNull"));
        }
        if (entity.getEndDate() == null) {
            errors.add(new FieldError("endDate", "must not be null", "NotNull"));
        }
        if (entity.getTimezone() == null) {
            errors.add(new FieldError("timezone", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowSelfEnrollment() == null) {
            errors.add(new FieldError("allowSelfEnrollment", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowFutureAccess() == null) {
            errors.add(new FieldError("allowFutureAccess", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPaid() == null) {
            errors.add(new FieldError("isPaid", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("price", WireValues.decimal(entity.getPrice()), 14, 2, errors);
        WireValues.checkDecimal("priceUsd", WireValues.decimal(entity.getPriceUsd()), 14, 2, errors);
        if (!creating && entity.getShowLiveClass() == null) {
            errors.add(new FieldError("showLiveClass", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEvaluationEnabled() == null) {
            errors.add(new FieldError("evaluationEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCertificationEnabled() == null) {
            errors.add(new FieldError("certificationEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPublishNotificationSent() == null) {
            errors.add(new FieldError("publishNotificationSent", "must not be null", "NotNull"));
        }
        if (!creating && entity.getLifecycleStatus() == null) {
            errors.add(new FieldError("lifecycleStatus", "must not be null", "NotNull"));
        }
        if (!creating && entity.getHasSchedule() == null) {
            errors.add(new FieldError("hasSchedule", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStaffCanEditSchedule() == null) {
            errors.add(new FieldError("staffCanEditSchedule", "must not be null", "NotNull"));
        }
        if (!creating && entity.getGradeDisplay() == null) {
            errors.add(new FieldError("gradeDisplay", "must not be null", "NotNull"));
        }
        if (entity.getCategoryId() != null && !repository.existsCategoryId(entity.getCategoryId())) {
            errors.add(new FieldError("categoryId", "category does not exist", "NOT_FOUND"));
        }
        if (entity.getConferencingAccountId() != null
                && !repository.existsConferencingAccountId(entity.getConferencingAccountId())) {
            errors.add(new FieldError("conferencingAccountId", "conferencing account does not exist", "NOT_FOUND"));
        }
        if (entity.getTimetableTemplateId() != null
                && !repository.existsTimetableTemplateId(entity.getTimetableTemplateId())) {
            errors.add(new FieldError("timetableTemplateId", "timetable template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CohortEntity entity) {

    }

    // ---- CohortCourse: a record of the Cohort aggregate ----

    public CohortCourse getCohortCourse(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return cohortCourseMapper.toDto(loadVisibleCohortCourse(DataScope.current(COHORT_COURSE_PERMISSION_GET),
                    cohortId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CohortCourse createCohortCourse(UUID cohortId, CohortCourse request, boolean validateOnly) {
        validator.validate(request);
        CohortCourseEntity entity = cohortCourseMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_COURSE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!cohortCourseRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfCohortCourse(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidCohortCourse(entity, true);
            scope.requireWritable(institutionOfCohortCourse(entity));
            assertUniqueCohortCourse(entity);
            if (validateOnly) {
                return cohortCourseMapper.toDto(entity);
            }
            CohortCourseEntity stored = cohortCourseRepository.insert(entity);
            audit.recordCreate(COHORT_COURSE_AGGREGATE, stored.getId());
            outbox.publish(COHORT_COURSE_AGGREGATE + LmsEvents.CREATED, COHORT_COURSE_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return cohortCourseMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CohortCourse updateCohortCourse(UUID cohortId, UUID id, String ifMatch, CohortCourseUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COHORT_COURSE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_COURSE_PERMISSION_UPDATE);
            CohortCourseEntity entity = loadVisibleCohortCourse(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCohortCourse(entity, changes, fields);
            validator.validate(cohortCourseMapper.toDto(entity));
            assertValidCohortCourse(entity, false);
            scope.requireWritable(institutionOfCohortCourse(entity));
            assertUniqueCohortCourse(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortCourseMapper.toDto(entity);
            }
            if (cohortCourseRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_COURSE_AGGREGATE, id, fields);
            outbox.publish(COHORT_COURSE_AGGREGATE + LmsEvents.UPDATED, COHORT_COURSE_AGGREGATE, id, LmsEvents.payload(id));
            return cohortCourseMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortCourse(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_COURSE_PERMISSION_DELETE);
            CohortCourseEntity entity = loadVisibleCohortCourse(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (cohortCourseRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_COURSE_AGGREGATE, id);
            outbox.publish(COHORT_COURSE_AGGREGATE + LmsEvents.DELETED, COHORT_COURSE_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortCourseEntity loadVisibleCohortCourse(DataScope scope, UUID cohortId, UUID id) {
        CohortCourseEntity entity = cohortCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_COURSE_AGGREGATE, id));
        if (!scope.canRead(institutionOfCohortCourse(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(COHORT_COURSE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfCohortCourse(CohortCourseEntity entity) {
        return cohortCourseRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected CohortCourseUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCohortCourse(CohortCourseEntity entity, CohortCourseUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "evaluatorId" -> entity.setEvaluatorId(changes.getEvaluatorId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCohortCourse(CohortCourseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !cohortCourseRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !cohortCourseRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getEvaluatorId() != null && !cohortCourseRepository.existsEvaluatorId(entity.getEvaluatorId())) {
            errors.add(new FieldError("evaluatorId", "evaluator does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCohortCourse(CohortCourseEntity entity) {
        if (cohortCourseRepository.takenCohortIdCourseId1(entity)) {
            throw new DuplicateResourceException(
                    "A cohort course with the same cohort id and course id already exists.");
        }
    }

    // ---- CohortFeedback: a record of the Cohort aggregate ----

    public CohortFeedback getCohortFeedback(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return cohortFeedbackMapper.toDto(loadVisibleCohortFeedback(DataScope.current(COHORT_FEEDBACK_PERMISSION_GET),
                    cohortId, id));
        });
    }

    public Page<CohortFeedback> listCohortFeedbacks(UUID cohortId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CohortFeedbackRepository.SORT_FIELDS, CohortFeedbackRepository.DEFAULT_ORDER,
                CohortFeedbackRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortFeedbackRepository.FILTER_FIELDS)
                .and(CohortFeedbackRepository.ofParent(cohortId))
                        .and(CohortFeedbackRepository.scope(DataScope.current(COHORT_FEEDBACK_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<CohortFeedbackEntity> rows = cohortFeedbackRepository.list(page, where);
            return rows.map(cohortFeedbackMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CohortFeedback createCohortFeedback(UUID cohortId, CohortFeedback request, boolean validateOnly) {
        validator.validate(request);
        CohortFeedbackEntity entity = cohortFeedbackMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_FEEDBACK_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!cohortFeedbackRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfCohortFeedback(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidCohortFeedback(entity, true);
            scope.requireWritable(institutionOfCohortFeedback(entity));
            assertUniqueCohortFeedback(entity);
            if (validateOnly) {
                return cohortFeedbackMapper.toDto(entity);
            }
            CohortFeedbackEntity stored = cohortFeedbackRepository.insert(entity);
            audit.recordCreate(COHORT_FEEDBACK_AGGREGATE, stored.getId());
            outbox.publish(COHORT_FEEDBACK_AGGREGATE + LmsEvents.CREATED, COHORT_FEEDBACK_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return cohortFeedbackMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CohortFeedback updateCohortFeedback(UUID cohortId, UUID id, String ifMatch, CohortFeedbackUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COHORT_FEEDBACK_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_FEEDBACK_PERMISSION_UPDATE);
            CohortFeedbackEntity entity = loadVisibleCohortFeedback(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortFeedback(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCohortFeedback(entity, changes, fields);
            validator.validate(cohortFeedbackMapper.toDto(entity));
            assertValidCohortFeedback(entity, false);
            scope.requireWritable(institutionOfCohortFeedback(entity));
            assertUniqueCohortFeedback(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortFeedbackMapper.toDto(entity);
            }
            if (cohortFeedbackRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_FEEDBACK_AGGREGATE, id, fields);
            outbox.publish(COHORT_FEEDBACK_AGGREGATE + LmsEvents.UPDATED, COHORT_FEEDBACK_AGGREGATE, id, LmsEvents.payload(id));
            return cohortFeedbackMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortFeedback(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_FEEDBACK_PERMISSION_DELETE);
            CohortFeedbackEntity entity = loadVisibleCohortFeedback(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortFeedback(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (cohortFeedbackRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_FEEDBACK_AGGREGATE, id);
            outbox.publish(COHORT_FEEDBACK_AGGREGATE + LmsEvents.DELETED, COHORT_FEEDBACK_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortFeedbackEntity loadVisibleCohortFeedback(DataScope scope, UUID cohortId, UUID id) {
        CohortFeedbackEntity entity = cohortFeedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_FEEDBACK_AGGREGATE, id));
        if (!scope.canRead(institutionOfCohortFeedback(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(COHORT_FEEDBACK_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfCohortFeedback(CohortFeedbackEntity entity) {
        return cohortFeedbackRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected CohortFeedbackUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCohortFeedback(CohortFeedbackEntity entity, CohortFeedbackUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "feedback" -> entity.setFeedback(changes.getFeedback());
                case "contentRating" -> entity.setContentRating(WireValues.decimal(changes.getContentRating()));
                case "teacherRating" -> entity.setTeacherRating(WireValues.decimal(changes.getTeacherRating()));
                case "valueRating" -> entity.setValueRating(WireValues.decimal(changes.getValueRating()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCohortFeedback(CohortFeedbackEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getFeedback() == null) {
            errors.add(new FieldError("feedback", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("contentRating", WireValues.decimal(entity.getContentRating()), 3, 2, errors);
        WireValues.checkDecimal("teacherRating", WireValues.decimal(entity.getTeacherRating()), 3, 2, errors);
        WireValues.checkDecimal("valueRating", WireValues.decimal(entity.getValueRating()), 3, 2, errors);
        if (entity.getCohortId() != null && !cohortFeedbackRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCohortFeedback(CohortFeedbackEntity entity) {
        if (cohortFeedbackRepository.takenCohortIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A cohort feedback with the same cohort id and user id already exists.");
        }
    }

    // ---- CohortGroup: a record of the Cohort aggregate ----

    public CohortGroup getCohortGroup(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return cohortGroupMapper.toDto(loadVisibleCohortGroup(DataScope.current(COHORT_GROUP_PERMISSION_GET), cohortId, id));
        });
    }

    public Page<CohortGroup> listCohortGroups(UUID cohortId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CohortGroupRepository.SORT_FIELDS, CohortGroupRepository.DEFAULT_ORDER,
                CohortGroupRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortGroupRepository.FILTER_FIELDS)
                .and(CohortGroupRepository.ofParent(cohortId))
                        .and(CohortGroupRepository.scope(DataScope.current(COHORT_GROUP_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<CohortGroupEntity> rows = cohortGroupRepository.list(page, where);
            return rows.map(cohortGroupMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CohortGroup createCohortGroup(UUID cohortId, CohortGroup request, boolean validateOnly) {
        validator.validate(request);
        CohortGroupEntity entity = cohortGroupMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_GROUP_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!cohortGroupRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfCohortGroup(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidCohortGroup(entity, true);
            scope.requireWritable(institutionOfCohortGroup(entity));
            assertUniqueCohortGroup(entity);
            if (validateOnly) {
                return cohortGroupMapper.toDto(entity);
            }
            CohortGroupEntity stored = cohortGroupRepository.insert(entity);
            audit.recordCreate(COHORT_GROUP_AGGREGATE, stored.getId());
            outbox.publish(COHORT_GROUP_AGGREGATE + LmsEvents.CREATED, COHORT_GROUP_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return cohortGroupMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CohortGroup updateCohortGroup(UUID cohortId, UUID id, String ifMatch, CohortGroupUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COHORT_GROUP_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_GROUP_PERMISSION_UPDATE);
            CohortGroupEntity entity = loadVisibleCohortGroup(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortGroup(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCohortGroup(entity, changes, fields);
            validator.validate(cohortGroupMapper.toDto(entity));
            assertValidCohortGroup(entity, false);
            scope.requireWritable(institutionOfCohortGroup(entity));
            assertUniqueCohortGroup(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortGroupMapper.toDto(entity);
            }
            if (cohortGroupRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_GROUP_AGGREGATE, id, fields);
            outbox.publish(COHORT_GROUP_AGGREGATE + LmsEvents.UPDATED, COHORT_GROUP_AGGREGATE, id, LmsEvents.payload(id));
            return cohortGroupMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortGroup(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_GROUP_PERMISSION_DELETE);
            CohortGroupEntity entity = loadVisibleCohortGroup(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortGroup(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (cohortGroupRepository.referencedByForumSectionCohortGroupId(id)) {
                throw new InvalidStateException(
                        "The cohort group is still referenced by lms.forum_section rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (cohortGroupRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_GROUP_AGGREGATE, id);
            outbox.publish(COHORT_GROUP_AGGREGATE + LmsEvents.DELETED, COHORT_GROUP_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortGroupEntity loadVisibleCohortGroup(DataScope scope, UUID cohortId, UUID id) {
        CohortGroupEntity entity = cohortGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_GROUP_AGGREGATE, id));
        if (!scope.canRead(institutionOfCohortGroup(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(COHORT_GROUP_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfCohortGroup(CohortGroupEntity entity) {
        return cohortGroupRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected CohortGroupUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyCohortGroup(CohortGroupEntity entity, CohortGroupUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "name" -> entity.setName(changes.getName());
                case "description" -> entity.setDescription(changes.getDescription());
                case "isDisabled" -> entity.setIsDisabled(changes.getIsDisabled());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCohortGroup(CohortGroupEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsDisabled() == null) {
            errors.add(new FieldError("isDisabled", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !cohortGroupRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCohortGroup(CohortGroupEntity entity) {
        if (cohortGroupRepository.takenCohortIdName1(entity)) {
            throw new DuplicateResourceException("A cohort group with the same cohort id and name already exists.");
        }
    }

    // ---- CohortHoliday: a record of the Cohort aggregate ----

    public CohortHoliday getCohortHoliday(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return cohortHolidayMapper.toDto(loadVisibleCohortHoliday(DataScope.current(COHORT_HOLIDAY_PERMISSION_GET),
                    cohortId, id));
        });
    }

    public Page<CohortHoliday> listCohortHolidays(UUID cohortId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CohortHolidayRepository.SORT_FIELDS, CohortHolidayRepository.DEFAULT_ORDER,
                CohortHolidayRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortHolidayRepository.FILTER_FIELDS)
                .and(CohortHolidayRepository.ofParent(cohortId))
                        .and(CohortHolidayRepository.scope(DataScope.current(COHORT_HOLIDAY_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<CohortHolidayEntity> rows = cohortHolidayRepository.list(page, where);
            return rows.map(cohortHolidayMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CohortHoliday createCohortHoliday(UUID cohortId, CohortHoliday request, boolean validateOnly) {
        validator.validate(request);
        CohortHolidayEntity entity = cohortHolidayMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_HOLIDAY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!cohortHolidayRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfCohortHoliday(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidCohortHoliday(entity, true);
            scope.requireWritable(institutionOfCohortHoliday(entity));
            assertUniqueCohortHoliday(entity);
            if (validateOnly) {
                return cohortHolidayMapper.toDto(entity);
            }
            CohortHolidayEntity stored = cohortHolidayRepository.insert(entity);
            audit.recordCreate(COHORT_HOLIDAY_AGGREGATE, stored.getId());
            outbox.publish(COHORT_HOLIDAY_AGGREGATE + LmsEvents.CREATED, COHORT_HOLIDAY_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return cohortHolidayMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CohortHoliday updateCohortHoliday(UUID cohortId, UUID id, String ifMatch, CohortHolidayUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COHORT_HOLIDAY_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_HOLIDAY_PERMISSION_UPDATE);
            CohortHolidayEntity entity = loadVisibleCohortHoliday(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortHoliday(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCohortHoliday(entity, changes, fields);
            validator.validate(cohortHolidayMapper.toDto(entity));
            assertValidCohortHoliday(entity, false);
            scope.requireWritable(institutionOfCohortHoliday(entity));
            assertUniqueCohortHoliday(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortHolidayMapper.toDto(entity);
            }
            if (cohortHolidayRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_HOLIDAY_AGGREGATE, id, fields);
            outbox.publish(COHORT_HOLIDAY_AGGREGATE + LmsEvents.UPDATED, COHORT_HOLIDAY_AGGREGATE, id, LmsEvents.payload(id));
            return cohortHolidayMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortHoliday(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_HOLIDAY_PERMISSION_DELETE);
            CohortHolidayEntity entity = loadVisibleCohortHoliday(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortHoliday(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (cohortHolidayRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_HOLIDAY_AGGREGATE, id);
            outbox.publish(COHORT_HOLIDAY_AGGREGATE + LmsEvents.DELETED, COHORT_HOLIDAY_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortHolidayEntity loadVisibleCohortHoliday(DataScope scope, UUID cohortId, UUID id) {
        CohortHolidayEntity entity = cohortHolidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_HOLIDAY_AGGREGATE, id));
        if (!scope.canRead(institutionOfCohortHoliday(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(COHORT_HOLIDAY_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfCohortHoliday(CohortHolidayEntity entity) {
        return cohortHolidayRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected CohortHolidayUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCohortHoliday(CohortHolidayEntity entity, CohortHolidayUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "holidayId" -> entity.setHolidayId(changes.getHolidayId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "startDate" -> entity.setStartDate(changes.getStartDate());
                case "endDate" -> entity.setEndDate(changes.getEndDate());
                case "isIncluded" -> entity.setIsIncluded(changes.getIsIncluded());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCohortHoliday(CohortHolidayEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getStartDate() == null) {
            errors.add(new FieldError("startDate", "must not be null", "NotNull"));
        }
        if (entity.getEndDate() == null) {
            errors.add(new FieldError("endDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsIncluded() == null) {
            errors.add(new FieldError("isIncluded", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !cohortHolidayRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCohortHoliday(CohortHolidayEntity entity) {

    }

    // ---- CohortScheduleSlot: a record of the Cohort aggregate ----

    public CohortScheduleSlot getCohortScheduleSlot(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return cohortScheduleSlotMapper.toDto(loadVisibleCohortScheduleSlot(DataScope.current(
                    COHORT_SCHEDULE_SLOT_PERMISSION_GET), cohortId, id));
        });
    }

    public Page<CohortScheduleSlot> listCohortScheduleSlots(UUID cohortId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CohortScheduleSlotRepository.SORT_FIELDS,
                        CohortScheduleSlotRepository.DEFAULT_ORDER,
                CohortScheduleSlotRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortScheduleSlotRepository.FILTER_FIELDS)
                .and(CohortScheduleSlotRepository.ofParent(cohortId))
                        .and(CohortScheduleSlotRepository.scope(DataScope.current(COHORT_SCHEDULE_SLOT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<CohortScheduleSlotEntity> rows = cohortScheduleSlotRepository.list(page, where);
            return rows.map(cohortScheduleSlotMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CohortScheduleSlot createCohortScheduleSlot(UUID cohortId, CohortScheduleSlot request,
            boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        CohortScheduleSlotEntity entity = cohortScheduleSlotMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_SCHEDULE_SLOT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!cohortScheduleSlotRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfCohortScheduleSlot(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidCohortScheduleSlot(entity, true);
            scope.requireWritable(institutionOfCohortScheduleSlot(entity));
            assertUniqueCohortScheduleSlot(entity);
            if (validateOnly) {
                return cohortScheduleSlotMapper.toDto(entity);
            }
            CohortScheduleSlotEntity stored = cohortScheduleSlotRepository.insert(entity);
            audit.recordCreate(COHORT_SCHEDULE_SLOT_AGGREGATE, stored.getId());
            outbox.publish(COHORT_SCHEDULE_SLOT_AGGREGATE + LmsEvents.CREATED, COHORT_SCHEDULE_SLOT_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return cohortScheduleSlotMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CohortScheduleSlot updateCohortScheduleSlot(UUID cohortId, UUID id, String ifMatch,
            CohortScheduleSlotUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COHORT_SCHEDULE_SLOT_WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("endTime", changes.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_SCHEDULE_SLOT_PERMISSION_UPDATE);
            CohortScheduleSlotEntity entity = loadVisibleCohortScheduleSlot(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortScheduleSlot(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCohortScheduleSlot(entity, changes, fields);
            validator.validate(cohortScheduleSlotMapper.toDto(entity));
            assertValidCohortScheduleSlot(entity, false);
            scope.requireWritable(institutionOfCohortScheduleSlot(entity));
            assertUniqueCohortScheduleSlot(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortScheduleSlotMapper.toDto(entity);
            }
            if (cohortScheduleSlotRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_SCHEDULE_SLOT_AGGREGATE, id, fields);
            outbox.publish(COHORT_SCHEDULE_SLOT_AGGREGATE + LmsEvents.UPDATED, COHORT_SCHEDULE_SLOT_AGGREGATE, id,
                    LmsEvents.payload(id));
            return cohortScheduleSlotMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortScheduleSlot(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_SCHEDULE_SLOT_PERMISSION_DELETE);
            CohortScheduleSlotEntity entity = loadVisibleCohortScheduleSlot(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortScheduleSlot(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (cohortScheduleSlotRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_SCHEDULE_SLOT_AGGREGATE, id);
            outbox.publish(COHORT_SCHEDULE_SLOT_AGGREGATE + LmsEvents.DELETED, COHORT_SCHEDULE_SLOT_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortScheduleSlotEntity loadVisibleCohortScheduleSlot(DataScope scope, UUID cohortId, UUID id) {
        CohortScheduleSlotEntity entity = cohortScheduleSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_SCHEDULE_SLOT_AGGREGATE, id));
        if (!scope.canRead(institutionOfCohortScheduleSlot(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(COHORT_SCHEDULE_SLOT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfCohortScheduleSlot(CohortScheduleSlotEntity entity) {
        return cohortScheduleSlotRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected CohortScheduleSlotUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyCohortScheduleSlot(CohortScheduleSlotEntity entity, CohortScheduleSlotUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "slotType" -> entity.setSlotType(Optional.ofNullable(changes.getSlotType()).map(v -> v.getValue())
                        .orElse(null));
                case "cohortTeacherId" -> entity.setCohortTeacherId(changes.getCohortTeacherId());
                case "weekday" -> entity.setWeekday(Optional.ofNullable(changes.getWeekday()).map(v -> v.getValue())
                        .orElse(null));
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                case "venue" -> entity.setVenue(changes.getVenue());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCohortScheduleSlot(CohortScheduleSlotEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSlotType() == null) {
            errors.add(new FieldError("slotType", "must not be null", "NotNull"));
        }
        if (entity.getWeekday() == null) {
            errors.add(new FieldError("weekday", "must not be null", "NotNull"));
        }
        if (entity.getStartTime() == null) {
            errors.add(new FieldError("startTime", "must not be null", "NotNull"));
        }
        if (entity.getEndTime() == null) {
            errors.add(new FieldError("endTime", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !cohortScheduleSlotRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCohortTeacherId() != null
                && !cohortScheduleSlotRepository.existsCohortTeacherId(entity.getCohortTeacherId())) {
            errors.add(new FieldError("cohortTeacherId", "cohort teacher does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCohortScheduleSlot(CohortScheduleSlotEntity entity) {

    }

    // ---- CohortSession: a record of the Cohort aggregate ----

    public CohortSession getCohortSession(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return cohortSessionMapper.toDto(loadVisibleCohortSession(DataScope.current(COHORT_SESSION_PERMISSION_GET),
                    cohortId, id));
        });
    }

    public Page<CohortSession> listCohortSessions(UUID cohortId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CohortSessionRepository.SORT_FIELDS, CohortSessionRepository.DEFAULT_ORDER,
                CohortSessionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortSessionRepository.FILTER_FIELDS)
                .and(CohortSessionRepository.ofParent(cohortId))
                        .and(CohortSessionRepository.scope(DataScope.current(COHORT_SESSION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<CohortSessionEntity> rows = cohortSessionRepository.list(page, where);
            return rows.map(cohortSessionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CohortSession createCohortSession(UUID cohortId, CohortSession request, boolean validateOnly) {
        validator.validate(request);
        CohortSessionEntity entity = cohortSessionMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_SESSION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!cohortSessionRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfCohortSession(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidCohortSession(entity, true);
            scope.requireWritable(institutionOfCohortSession(entity));
            assertUniqueCohortSession(entity);
            if (validateOnly) {
                return cohortSessionMapper.toDto(entity);
            }
            CohortSessionEntity stored = cohortSessionRepository.insert(entity);
            audit.recordCreate(COHORT_SESSION_AGGREGATE, stored.getId());
            outbox.publish(COHORT_SESSION_AGGREGATE + LmsEvents.CREATED, COHORT_SESSION_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return cohortSessionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CohortSession updateCohortSession(UUID cohortId, UUID id, String ifMatch, CohortSessionUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COHORT_SESSION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_SESSION_PERMISSION_UPDATE);
            CohortSessionEntity entity = loadVisibleCohortSession(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortSession(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCohortSession(entity, changes, fields);
            validator.validate(cohortSessionMapper.toDto(entity));
            assertValidCohortSession(entity, false);
            scope.requireWritable(institutionOfCohortSession(entity));
            assertUniqueCohortSession(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortSessionMapper.toDto(entity);
            }
            if (cohortSessionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_SESSION_AGGREGATE, id, fields);
            outbox.publish(COHORT_SESSION_AGGREGATE + LmsEvents.UPDATED, COHORT_SESSION_AGGREGATE, id, LmsEvents.payload(id));
            return cohortSessionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortSession(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_SESSION_PERMISSION_DELETE);
            CohortSessionEntity entity = loadVisibleCohortSession(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortSession(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (cohortSessionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_SESSION_AGGREGATE, id);
            outbox.publish(COHORT_SESSION_AGGREGATE + LmsEvents.DELETED, COHORT_SESSION_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortSessionEntity loadVisibleCohortSession(DataScope scope, UUID cohortId, UUID id) {
        CohortSessionEntity entity = cohortSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_SESSION_AGGREGATE, id));
        if (!scope.canRead(institutionOfCohortSession(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(COHORT_SESSION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfCohortSession(CohortSessionEntity entity) {
        return cohortSessionRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected CohortSessionUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCohortSession(CohortSessionEntity entity, CohortSessionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "startsAt" -> entity.setStartsAt(changes.getStartsAt());
                case "endsAt" -> entity.setEndsAt(changes.getEndsAt());
                case "description" -> entity.setDescription(changes.getDescription());
                case "venue" -> entity.setVenue(changes.getVenue());
                case "isCancelled" -> entity.setIsCancelled(changes.getIsCancelled());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCohortSession(CohortSessionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getStartsAt() == null) {
            errors.add(new FieldError("startsAt", "must not be null", "NotNull"));
        }
        if (entity.getEndsAt() == null) {
            errors.add(new FieldError("endsAt", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsCancelled() == null) {
            errors.add(new FieldError("isCancelled", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !cohortSessionRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCohortSession(CohortSessionEntity entity) {

    }

    // ---- CohortTeacher: a record of the Cohort aggregate ----

    public CohortTeacher getCohortTeacher(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return cohortTeacherMapper.toDto(loadVisibleCohortTeacher(DataScope.current(COHORT_TEACHER_PERMISSION_GET),
                    cohortId, id));
        });
    }

    public Page<CohortTeacher> listCohortTeachers(UUID cohortId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CohortTeacherRepository.SORT_FIELDS, CohortTeacherRepository.DEFAULT_ORDER,
                CohortTeacherRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortTeacherRepository.FILTER_FIELDS)
                .and(CohortTeacherRepository.ofParent(cohortId))
                        .and(CohortTeacherRepository.scope(DataScope.current(COHORT_TEACHER_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<CohortTeacherEntity> rows = cohortTeacherRepository.list(page, where);
            return rows.map(cohortTeacherMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CohortTeacher createCohortTeacher(UUID cohortId, CohortTeacher request, boolean validateOnly) {
        validator.validate(request);
        CohortTeacherEntity entity = cohortTeacherMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_TEACHER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!cohortTeacherRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfCohortTeacher(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidCohortTeacher(entity, true);
            scope.requireWritable(institutionOfCohortTeacher(entity));
            assertUniqueCohortTeacher(entity);
            if (validateOnly) {
                return cohortTeacherMapper.toDto(entity);
            }
            CohortTeacherEntity stored = cohortTeacherRepository.insert(entity);
            audit.recordCreate(COHORT_TEACHER_AGGREGATE, stored.getId());
            outbox.publish(COHORT_TEACHER_AGGREGATE + LmsEvents.CREATED, COHORT_TEACHER_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return cohortTeacherMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CohortTeacher updateCohortTeacher(UUID cohortId, UUID id, String ifMatch, CohortTeacherUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COHORT_TEACHER_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_TEACHER_PERMISSION_UPDATE);
            CohortTeacherEntity entity = loadVisibleCohortTeacher(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortTeacher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCohortTeacher(entity, changes, fields);
            validator.validate(cohortTeacherMapper.toDto(entity));
            assertValidCohortTeacher(entity, false);
            scope.requireWritable(institutionOfCohortTeacher(entity));
            assertUniqueCohortTeacher(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortTeacherMapper.toDto(entity);
            }
            if (cohortTeacherRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_TEACHER_AGGREGATE, id, fields);
            outbox.publish(COHORT_TEACHER_AGGREGATE + LmsEvents.UPDATED, COHORT_TEACHER_AGGREGATE, id, LmsEvents.payload(id));
            return cohortTeacherMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortTeacher(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_TEACHER_PERMISSION_DELETE);
            CohortTeacherEntity entity = loadVisibleCohortTeacher(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortTeacher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (cohortTeacherRepository.referencedByCohortScheduleSlotCohortTeacherId(id)) {
                throw new InvalidStateException(
                        "The cohort teacher is still referenced by lms.cohort_schedule_slot rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (cohortTeacherRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_TEACHER_AGGREGATE, id);
            outbox.publish(COHORT_TEACHER_AGGREGATE + LmsEvents.DELETED, COHORT_TEACHER_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortTeacherEntity loadVisibleCohortTeacher(DataScope scope, UUID cohortId, UUID id) {
        CohortTeacherEntity entity = cohortTeacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_TEACHER_AGGREGATE, id));
        if (!scope.canRead(institutionOfCohortTeacher(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(COHORT_TEACHER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfCohortTeacher(CohortTeacherEntity entity) {
        return cohortTeacherRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected CohortTeacherUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCohortTeacher(CohortTeacherEntity entity, CohortTeacherUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "roleId" -> entity.setRoleId(changes.getRoleId());
                case "roleLabel" -> entity.setRoleLabel(changes.getRoleLabel());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "hasOfficeHours" -> entity.setHasOfficeHours(changes.getHasOfficeHours());
                case "removedAt" -> entity.setRemovedAt(changes.getRemovedAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCohortTeacher(CohortTeacherEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!creating && entity.getHasOfficeHours() == null) {
            errors.add(new FieldError("hasOfficeHours", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !cohortTeacherRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCohortTeacher(CohortTeacherEntity entity) {
        if (cohortTeacherRepository.takenCohortIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A cohort teacher with the same cohort id and user id already exists.");
        }
    }

    // ---- Forum: a record of the Cohort aggregate ----

    public Forum getForum(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return forumMapper.toDto(loadVisibleForum(DataScope.current(FORUM_PERMISSION_GET), cohortId, id));
        });
    }

    public Page<Forum> listForums(UUID cohortId, Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ForumRepository.SORT_FIELDS, ForumRepository.DEFAULT_ORDER,
                ForumRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ForumRepository.FILTER_FIELDS)
                .and(ForumRepository.ofParent(cohortId)).and(ForumRepository.scope(DataScope.current(FORUM_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<ForumEntity> rows = forumRepository.list(page, where);
            return rows.map(forumMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Forum createForum(UUID cohortId, Forum request, boolean validateOnly) {
        validator.validate(request);
        ForumEntity entity = forumMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(FORUM_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!forumRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfForum(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidForum(entity, true);
            scope.requireWritable(institutionOfForum(entity));
            assertUniqueForum(entity);
            if (validateOnly) {
                return forumMapper.toDto(entity);
            }
            ForumEntity stored = forumRepository.insert(entity);
            audit.recordCreate(FORUM_AGGREGATE, stored.getId());
            outbox.publish(FORUM_AGGREGATE + LmsEvents.CREATED, FORUM_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return forumMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Forum updateForum(UUID cohortId, UUID id, String ifMatch, ForumUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, FORUM_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FORUM_PERMISSION_UPDATE);
            ForumEntity entity = loadVisibleForum(scope, cohortId, id);
            scope.requireWritable(institutionOfForum(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyForum(entity, changes, fields);
            validator.validate(forumMapper.toDto(entity));
            assertValidForum(entity, false);
            scope.requireWritable(institutionOfForum(entity));
            assertUniqueForum(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return forumMapper.toDto(entity);
            }
            if (forumRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(FORUM_AGGREGATE, id, fields);
            outbox.publish(FORUM_AGGREGATE + LmsEvents.UPDATED, FORUM_AGGREGATE, id, LmsEvents.payload(id));
            return forumMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteForum(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FORUM_PERMISSION_DELETE);
            ForumEntity entity = loadVisibleForum(scope, cohortId, id);
            scope.requireWritable(institutionOfForum(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (forumRepository.referencedByForumSectionForumId(id)) {
                throw new InvalidStateException(
                        "The forum is still referenced by lms.forum_section rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (forumRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(FORUM_AGGREGATE, id);
            outbox.publish(FORUM_AGGREGATE + LmsEvents.DELETED, FORUM_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ForumEntity loadVisibleForum(DataScope scope, UUID cohortId, UUID id) {
        ForumEntity entity = forumRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(FORUM_AGGREGATE, id));
        if (!scope.canRead(institutionOfForum(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(FORUM_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfForum(ForumEntity entity) {
        return forumRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected ForumUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyForum(ForumEntity entity, ForumUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "earlyAccessFrom" -> entity.setEarlyAccessFrom(changes.getEarlyAccessFrom());
                case "opensAt" -> entity.setOpensAt(changes.getOpensAt());
                case "closesAt" -> entity.setClosesAt(changes.getClosesAt());
                case "lateAccessUntil" -> entity.setLateAccessUntil(changes.getLateAccessUntil());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidForum(ForumEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !forumRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueForum(ForumEntity entity) {

    }

    // ---- PlagiarismCohortLink: a record of the Cohort aggregate ----

    public PlagiarismCohortLink getPlagiarismCohortLink(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> {
            return plagiarismCohortLinkMapper.toDto(loadVisiblePlagiarismCohortLink(DataScope.current(
                    PLAGIARISM_COHORT_LINK_PERMISSION_GET), cohortId, id));
        });
    }

    public Page<PlagiarismCohortLink> listPlagiarismCohortLinks(UUID cohortId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, PlagiarismCohortLinkRepository.SORT_FIELDS,
                        PlagiarismCohortLinkRepository.DEFAULT_ORDER,
                PlagiarismCohortLinkRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PlagiarismCohortLinkRepository.FILTER_FIELDS)
                .and(PlagiarismCohortLinkRepository.ofParent(cohortId))
                        .and(PlagiarismCohortLinkRepository.scope(DataScope.current(PLAGIARISM_COHORT_LINK_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> {
            Page<PlagiarismCohortLinkEntity> rows = plagiarismCohortLinkRepository.list(page, where);
            return rows.map(plagiarismCohortLinkMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PlagiarismCohortLink createPlagiarismCohortLink(UUID cohortId, PlagiarismCohortLink request,
            boolean validateOnly) {
        validator.validate(request);
        PlagiarismCohortLinkEntity entity = plagiarismCohortLinkMapper.toEntity(request);
        entity.setCohortId(cohortId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PLAGIARISM_COHORT_LINK_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!plagiarismCohortLinkRepository.existsCohortId(entity.getCohortId())) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            if (!scope.canRead(institutionOfPlagiarismCohortLink(entity))) {
                throw new ResourceNotFoundException("lms.cohort", entity.getCohortId());
            }
            assertValidPlagiarismCohortLink(entity, true);
            scope.requireWritable(institutionOfPlagiarismCohortLink(entity));
            assertUniquePlagiarismCohortLink(entity);
            if (validateOnly) {
                return plagiarismCohortLinkMapper.toDto(entity);
            }
            PlagiarismCohortLinkEntity stored = plagiarismCohortLinkRepository.insert(entity);
            audit.recordCreate(PLAGIARISM_COHORT_LINK_AGGREGATE, stored.getId());
            outbox.publish(PLAGIARISM_COHORT_LINK_AGGREGATE + LmsEvents.CREATED, PLAGIARISM_COHORT_LINK_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return plagiarismCohortLinkMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PlagiarismCohortLink updatePlagiarismCohortLink(UUID cohortId, UUID id, String ifMatch,
            PlagiarismCohortLinkUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PLAGIARISM_COHORT_LINK_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PLAGIARISM_COHORT_LINK_PERMISSION_UPDATE);
            PlagiarismCohortLinkEntity entity = loadVisiblePlagiarismCohortLink(scope, cohortId, id);
            scope.requireWritable(institutionOfPlagiarismCohortLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyPlagiarismCohortLink(entity, changes, fields);
            validator.validate(plagiarismCohortLinkMapper.toDto(entity));
            assertValidPlagiarismCohortLink(entity, false);
            scope.requireWritable(institutionOfPlagiarismCohortLink(entity));
            assertUniquePlagiarismCohortLink(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return plagiarismCohortLinkMapper.toDto(entity);
            }
            if (plagiarismCohortLinkRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PLAGIARISM_COHORT_LINK_AGGREGATE, id, fields);
            outbox.publish(PLAGIARISM_COHORT_LINK_AGGREGATE + LmsEvents.UPDATED, PLAGIARISM_COHORT_LINK_AGGREGATE, id,
                    LmsEvents.payload(id));
            return plagiarismCohortLinkMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deletePlagiarismCohortLink(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PLAGIARISM_COHORT_LINK_PERMISSION_DELETE);
            PlagiarismCohortLinkEntity entity = loadVisiblePlagiarismCohortLink(scope, cohortId, id);
            scope.requireWritable(institutionOfPlagiarismCohortLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (plagiarismCohortLinkRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PLAGIARISM_COHORT_LINK_AGGREGATE, id);
            outbox.publish(PLAGIARISM_COHORT_LINK_AGGREGATE + LmsEvents.DELETED, PLAGIARISM_COHORT_LINK_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private PlagiarismCohortLinkEntity loadVisiblePlagiarismCohortLink(DataScope scope, UUID cohortId, UUID id) {
        PlagiarismCohortLinkEntity entity = plagiarismCohortLinkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PLAGIARISM_COHORT_LINK_AGGREGATE, id));
        if (!scope.canRead(institutionOfPlagiarismCohortLink(entity)) || !cohortId.equals(entity.getCohortId())) {
            throw new ResourceNotFoundException(PLAGIARISM_COHORT_LINK_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOfPlagiarismCohortLink(PlagiarismCohortLinkEntity entity) {
        return plagiarismCohortLinkRepository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected PlagiarismCohortLinkUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyPlagiarismCohortLink(PlagiarismCohortLinkEntity entity, PlagiarismCohortLinkUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "plagiarismAccountId" -> entity.setPlagiarismAccountId(changes.getPlagiarismAccountId());
                case "accountRef" -> entity.setAccountRef(changes.getAccountRef());
                case "providerClassRef" -> entity.setProviderClassRef(changes.getProviderClassRef());
                case "providerClassTitle" -> entity.setProviderClassTitle(changes.getProviderClassTitle());
                case "providerClassEndAt" -> entity.setProviderClassEndAt(changes.getProviderClassEndAt());
                case "providerAssignmentRef" -> entity.setProviderAssignmentRef(changes.getProviderAssignmentRef());
                case "providerAssignmentTitle" -> entity.setProviderAssignmentTitle(
                        changes.getProviderAssignmentTitle());
                case "providerAssignmentStartAt" -> entity.setProviderAssignmentStartAt(
                        changes.getProviderAssignmentStartAt());
                case "providerAssignmentDueAt" -> entity.setProviderAssignmentDueAt(
                        changes.getProviderAssignmentDueAt());
                case "providerAssignmentInstructions" -> entity.setProviderAssignmentInstructions(
                        changes.getProviderAssignmentInstructions());
                case "studentReportAccess" -> entity.setStudentReportAccess(changes.getStudentReportAccess());
                case "teacherEmail" -> entity.setTeacherEmail(changes.getTeacherEmail());
                case "teacherFirstName" -> entity.setTeacherFirstName(changes.getTeacherFirstName());
                case "teacherLastName" -> entity.setTeacherLastName(changes.getTeacherLastName());
                case "classCreated" -> entity.setClassCreated(changes.getClassCreated());
                case "assignmentCreated" -> entity.setAssignmentCreated(changes.getAssignmentCreated());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidPlagiarismCohortLink(PlagiarismCohortLinkEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getPlagiarismAccountId() == null) {
            errors.add(new FieldError("plagiarismAccountId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStudentReportAccess() == null) {
            errors.add(new FieldError("studentReportAccess", "must not be null", "NotNull"));
        }
        if (!creating && entity.getClassCreated() == null) {
            errors.add(new FieldError("classCreated", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAssignmentCreated() == null) {
            errors.add(new FieldError("assignmentCreated", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !plagiarismCohortLinkRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getPlagiarismAccountId() != null
                && !plagiarismCohortLinkRepository.existsPlagiarismAccountId(entity.getPlagiarismAccountId())) {
            errors.add(new FieldError("plagiarismAccountId", "plagiarism account does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniquePlagiarismCohortLink(PlagiarismCohortLinkEntity entity) {
        if (plagiarismCohortLinkRepository.takenCohortIdPlagiarismAccountId1(entity)) {
            throw new DuplicateResourceException(
                    "A plagiarism cohort link with the same cohort id and plagiarism account id already exists.");
        }
    }
}
