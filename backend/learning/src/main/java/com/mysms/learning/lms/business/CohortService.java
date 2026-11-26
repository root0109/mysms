package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.authclient.AuthContext;
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
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.Cohort;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.api.model.CohortCourse;
import com.mysms.learning.lms.api.model.CohortCourseUpdate;
import com.mysms.learning.lms.api.model.CohortFeedback;
import com.mysms.learning.lms.api.model.CohortFeedbackUpdate;
import com.mysms.learning.lms.api.model.CohortGroup;
import com.mysms.learning.lms.api.model.CohortGroupUpdate;
import com.mysms.learning.lms.api.model.CohortHoliday;
import com.mysms.learning.lms.api.model.CohortHolidayUpdate;
import com.mysms.learning.lms.api.model.CohortScheduleSlot;
import com.mysms.learning.lms.api.model.CohortScheduleSlotUpdate;
import com.mysms.learning.lms.api.model.CohortSession;
import com.mysms.learning.lms.api.model.CohortSessionUpdate;
import com.mysms.learning.lms.api.model.CohortTeacher;
import com.mysms.learning.lms.api.model.CohortTeacherUpdate;
import com.mysms.learning.lms.api.model.CohortUpdate;
import com.mysms.learning.lms.api.model.Forum;
import com.mysms.learning.lms.api.model.ForumUpdate;
import com.mysms.learning.lms.api.model.PlagiarismCohortLink;
import com.mysms.learning.lms.api.model.PlagiarismCohortLinkUpdate;
import com.mysms.learning.lms.api.model.UnreadCounts;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.CohortCourseEntity;
import com.mysms.learning.lms.domain.CohortEntity;
import com.mysms.learning.lms.domain.CohortFeedbackEntity;
import com.mysms.learning.lms.domain.CohortGroupEntity;
import com.mysms.learning.lms.domain.CohortHolidayEntity;
import com.mysms.learning.lms.domain.CohortScheduleSlotEntity;
import com.mysms.learning.lms.domain.CohortSessionEntity;
import com.mysms.learning.lms.domain.CohortTeacherEntity;
import com.mysms.learning.lms.domain.ForumEntity;
import com.mysms.learning.lms.domain.PlagiarismCohortLinkEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.CohortCourseMapper;
import com.mysms.learning.lms.mapper.CohortFeedbackMapper;
import com.mysms.learning.lms.mapper.CohortGroupMapper;
import com.mysms.learning.lms.mapper.CohortHolidayMapper;
import com.mysms.learning.lms.mapper.CohortMapper;
import com.mysms.learning.lms.mapper.CohortScheduleSlotMapper;
import com.mysms.learning.lms.mapper.CohortSessionMapper;
import com.mysms.learning.lms.mapper.CohortTeacherMapper;
import com.mysms.learning.lms.mapper.ForumMapper;
import com.mysms.learning.lms.mapper.PlagiarismCohortLinkMapper;
import com.mysms.learning.lms.repository.CohortCourseRepository;
import com.mysms.learning.lms.repository.CohortFeedbackRepository;
import com.mysms.learning.lms.repository.CohortGroupRepository;
import com.mysms.learning.lms.repository.CohortHolidayRepository;
import com.mysms.learning.lms.repository.CohortRepository;
import com.mysms.learning.lms.repository.CohortScheduleSlotRepository;
import com.mysms.learning.lms.repository.CohortSessionRepository;
import com.mysms.learning.lms.repository.CohortTeacherRepository;
import com.mysms.learning.lms.repository.ForumRepository;
import com.mysms.learning.lms.repository.PlagiarismCohortLinkRepository;
import com.mysms.learning.lms.repository.LearningActivityRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.learning.lms.api.model.CohortActivity;
import com.mysms.learning.lms.api.model.CohortActivityUpdate;
import com.mysms.learning.lms.domain.ContentItemEntity;
import com.mysms.learning.lms.mapper.CohortActivityMapper;
import com.mysms.learning.lms.repository.ContentItemRepository;
import com.mysms.learning.lms.repository.CohortActivityRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import com.mysms.core.error.ConflictException;
import com.mysms.core.id.UuidV7;
import com.mysms.learning.lms.api.model.ChapterList;
import com.mysms.learning.lms.mapper.ChapterMapper;
import com.mysms.learning.lms.repository.ChapterRepository;
import com.mysms.learning.lms.repository.CohortOutlineRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Business service of lms.cohort and of the records that exist only as part of a Cohort (CohortCourse, CohortFeedback,
 * CohortGroup, CohortHoliday, CohortScheduleSlot, CohortSession, CohortTeacher, Forum, PlagiarismCohortLink): audit and
 * domain event commit in the same transaction as every change. Rows outside the caller's institutions are invisible;
 * tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class CohortService extends AppTransactionManager {

    public static final String AGGREGATE = CohortEntity.TABLE;
    static final String PERMISSION_CREATE = "lms.cohort.create";
    static final String PERMISSION_UPDATE = "lms.cohort.update";
    static final String PERMISSION_DELETE = "lms.cohort.delete";

    public static final String COHORT_COURSE_AGGREGATE = CohortCourseEntity.TABLE;
    static final String COHORT_COURSE_PERMISSION_GET = "lms.cohort_course.read";
    static final String COHORT_COURSE_PERMISSION_CREATE = "lms.cohort_course.create";
    static final String COHORT_COURSE_PERMISSION_UPDATE = "lms.cohort_course.update";
    static final String COHORT_COURSE_PERMISSION_DELETE = "lms.cohort_course.delete";

    public static final String COHORT_FEEDBACK_AGGREGATE = CohortFeedbackEntity.TABLE;
    static final String COHORT_FEEDBACK_PERMISSION_LIST = "lms.cohort_feedback.read";
    static final String COHORT_FEEDBACK_PERMISSION_GET = "lms.cohort_feedback.read";
    static final String COHORT_FEEDBACK_PERMISSION_CREATE = "lms.cohort_feedback.create";
    static final String COHORT_FEEDBACK_PERMISSION_UPDATE = "lms.cohort_feedback.update";
    static final String COHORT_FEEDBACK_PERMISSION_DELETE = "lms.cohort_feedback.delete";

    public static final String COHORT_GROUP_AGGREGATE = CohortGroupEntity.TABLE;
    static final String COHORT_GROUP_PERMISSION_LIST = "lms.cohort_group.read";
    static final String COHORT_GROUP_PERMISSION_GET = "lms.cohort_group.read";
    static final String COHORT_GROUP_PERMISSION_CREATE = "lms.cohort_group.create";
    static final String COHORT_GROUP_PERMISSION_UPDATE = "lms.cohort_group.update";
    static final String COHORT_GROUP_PERMISSION_DELETE = "lms.cohort_group.delete";

    public static final String COHORT_HOLIDAY_AGGREGATE = CohortHolidayEntity.TABLE;
    static final String COHORT_HOLIDAY_PERMISSION_LIST = "lms.cohort_holiday.read";
    static final String COHORT_HOLIDAY_PERMISSION_GET = "lms.cohort_holiday.read";
    static final String COHORT_HOLIDAY_PERMISSION_CREATE = "lms.cohort_holiday.create";
    static final String COHORT_HOLIDAY_PERMISSION_UPDATE = "lms.cohort_holiday.update";
    static final String COHORT_HOLIDAY_PERMISSION_DELETE = "lms.cohort_holiday.delete";

    public static final String COHORT_SCHEDULE_SLOT_AGGREGATE = CohortScheduleSlotEntity.TABLE;
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_LIST = "lms.cohort_schedule_slot.read";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_GET = "lms.cohort_schedule_slot.read";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_CREATE = "lms.cohort_schedule_slot.create";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_UPDATE = "lms.cohort_schedule_slot.update";
    static final String COHORT_SCHEDULE_SLOT_PERMISSION_DELETE = "lms.cohort_schedule_slot.delete";

    public static final String COHORT_SESSION_AGGREGATE = CohortSessionEntity.TABLE;
    static final String COHORT_SESSION_PERMISSION_LIST = "lms.cohort_session.read";
    static final String COHORT_SESSION_PERMISSION_GET = "lms.cohort_session.read";
    static final String COHORT_SESSION_PERMISSION_CREATE = "lms.cohort_session.create";
    static final String COHORT_SESSION_PERMISSION_UPDATE = "lms.cohort_session.update";
    static final String COHORT_SESSION_PERMISSION_DELETE = "lms.cohort_session.delete";

    public static final String COHORT_TEACHER_AGGREGATE = CohortTeacherEntity.TABLE;
    static final String COHORT_TEACHER_PERMISSION_LIST = "lms.cohort_teacher.read";
    static final String COHORT_TEACHER_PERMISSION_GET = "lms.cohort_teacher.read";
    static final String COHORT_TEACHER_PERMISSION_CREATE = "lms.cohort_teacher.create";
    static final String COHORT_TEACHER_PERMISSION_UPDATE = "lms.cohort_teacher.update";
    static final String COHORT_TEACHER_PERMISSION_DELETE = "lms.cohort_teacher.delete";

    public static final String FORUM_AGGREGATE = ForumEntity.TABLE;
    static final String FORUM_PERMISSION_LIST = "lms.forum.read";
    static final String FORUM_PERMISSION_GET = "lms.forum.read";
    static final String FORUM_PERMISSION_CREATE = "lms.forum.create";
    static final String FORUM_PERMISSION_UPDATE = "lms.forum.update";
    static final String FORUM_PERMISSION_DELETE = "lms.forum.delete";

    public static final String PLAGIARISM_COHORT_LINK_AGGREGATE = PlagiarismCohortLinkEntity.TABLE;
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_LIST = "lms.plagiarism_cohort_link.read";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_GET = "lms.plagiarism_cohort_link.read";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_CREATE = "lms.plagiarism_cohort_link.create";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_UPDATE = "lms.plagiarism_cohort_link.update";
    static final String PLAGIARISM_COHORT_LINK_PERMISSION_DELETE = "lms.plagiarism_cohort_link.delete";

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
    private final CohortAccess access;
    private final ContentItemRepository contentItemRepository;
    private final CohortActivityRepository cohortActivityRepository;
    private final CohortActivityMapper cohortActivityMapper;
    private final CohortOutlineRepository outlineRepository;
    private final PlatformFiles files;
    private final ChapterRepository chapterRepository;
    private final ChapterMapper chapterMapper;
    private final LearningActivityRepository activity;
    private final PublicReads publicReads;

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
            PlagiarismCohortLinkMapper plagiarismCohortLinkMapper,
            CohortAccess access,
            ContentItemRepository contentItemRepository,
            CohortActivityRepository cohortActivityRepository,
            CohortActivityMapper cohortActivityMapper,
            CohortOutlineRepository outlineRepository,
            PlatformFiles files,
            ChapterRepository chapterRepository,
            ChapterMapper chapterMapper,
            LearningActivityRepository activity, PublicReads publicReads) {
        super(database, transactions);
        this.publicReads = publicReads;
        this.outlineRepository = outlineRepository;
        this.files = files;
        this.chapterRepository = chapterRepository;
        this.chapterMapper = chapterMapper;
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
        this.access = access;
        this.contentItemRepository = contentItemRepository;
        this.cohortActivityRepository = cohortActivityRepository;
        this.cohortActivityMapper = cohortActivityMapper;
        this.activity = activity;
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Cohort updateCohort(UUID id, String ifMatch, CohortUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", Patches.value(changes.getStartTime()), wire);
        WireValues.checkTime("endTime", Patches.value(changes.getEndTime()), wire);
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
            List<String> fields = apply(entity, changes);
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

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(CohortEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(CohortEntity entity, CohortUpdate changes) {
        Patches patch = new Patches();
        patch.apply("classSubjectId", changes.getClassSubjectId(), entity::setClassSubjectId);
        patch.apply("batchId", changes.getBatchId(), entity::setBatchId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("details", changes.getDetails(), entity::setDetails);
        patch.apply("categoryId", changes.getCategoryId(), entity::setCategoryId);
        patch.apply("medium", changes.getMedium(), v -> entity.setMedium(v == null ? null : v.getValue()));
        patch.apply("startDate", changes.getStartDate(), entity::setStartDate);
        patch.apply("endDate", changes.getEndDate(), entity::setEndDate);
        patch.apply("startTime", changes.getStartTime(), v -> entity.setStartTime(WireValues.time(v)));
        patch.apply("endTime", changes.getEndTime(), v -> entity.setEndTime(WireValues.time(v)));
        patch.apply("timezone", changes.getTimezone(), entity::setTimezone);
        patch.apply("seatCount", changes.getSeatCount(), entity::setSeatCount);
        patch.apply("allowSelfEnrollment", changes.getAllowSelfEnrollment(), entity::setAllowSelfEnrollment);
        patch.apply("allowFutureAccess", changes.getAllowFutureAccess(), entity::setAllowFutureAccess);
        patch.apply("isPaid", changes.getIsPaid(), entity::setIsPaid);
        patch.apply("price", changes.getPrice(), v -> entity.setPrice(WireValues.decimal(v)));
        patch.apply("currencyId", changes.getCurrencyId(), entity::setCurrencyId);
        patch.apply("priceUsd", changes.getPriceUsd(), v -> entity.setPriceUsd(WireValues.decimal(v)));
        patch.apply("showLiveClass", changes.getShowLiveClass(), entity::setShowLiveClass);
        patch.apply("evaluationEnabled", changes.getEvaluationEnabled(), entity::setEvaluationEnabled);
        patch.apply("evaluationEndDate", changes.getEvaluationEndDate(), entity::setEvaluationEndDate);
        patch.apply("certificationEnabled", changes.getCertificationEnabled(), entity::setCertificationEnabled);
        patch.apply("conferencingProvider", changes.getConferencingProvider(),
                                                v -> entity.setConferencingProvider(v == null ? null : v.getValue()));
        patch.apply("conferencingAccountId", changes.getConferencingAccountId(), entity::setConferencingAccountId);
        patch.apply("timetableTemplateId", changes.getTimetableTemplateId(), entity::setTimetableTemplateId);
        patch.apply("confirmationTemplateId", changes.getConfirmationTemplateId(), entity::setConfirmationTemplateId);
        patch.apply("metaImageFileId", changes.getMetaImageFileId(), entity::setMetaImageFileId);
        patch.apply("introVideoFileId", changes.getIntroVideoFileId(), entity::setIntroVideoFileId);
        patch.apply("customComponent", changes.getCustomComponent(), entity::setCustomComponent);
        patch.apply("customScript", changes.getCustomScript(), entity::setCustomScript);
        patch.apply("publishNotificationSent", changes.getPublishNotificationSent(), entity::setPublishNotificationSent);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("academicTermId", changes.getAcademicTermId(), entity::setAcademicTermId);
        patch.apply("programId", changes.getProgramId(), entity::setProgramId);
        patch.apply("externalRef", changes.getExternalRef(), entity::setExternalRef);
        patch.apply("lifecycleStatus", changes.getLifecycleStatus(),
                                                v -> entity.setLifecycleStatus(v == null ? null : v.getValue()));
        patch.apply("staffAccessFrom", changes.getStaffAccessFrom(), entity::setStaffAccessFrom);
        patch.apply("staffAccessUntil", changes.getStaffAccessUntil(), entity::setStaffAccessUntil);
        patch.apply("studentAccessFrom", changes.getStudentAccessFrom(), entity::setStudentAccessFrom);
        patch.apply("studentAccessUntil", changes.getStudentAccessUntil(), entity::setStudentAccessUntil);
        patch.apply("hasSchedule", changes.getHasSchedule(), entity::setHasSchedule);
        patch.apply("staffCanEditSchedule", changes.getStaffCanEditSchedule(), entity::setStaffCanEditSchedule);
        patch.apply("gradeDisplay", changes.getGradeDisplay(), v -> entity.setGradeDisplay(v == null ? null : v.getValue()));
        patch.apply("gradingSchemeId", changes.getGradingSchemeId(), entity::setGradingSchemeId);
        patch.apply("enrollmentOpensAt", changes.getEnrollmentOpensAt(), entity::setEnrollmentOpensAt);
        patch.apply("enrollmentClosesAt", changes.getEnrollmentClosesAt(), entity::setEnrollmentClosesAt);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CohortCourse updateCohortCourse(UUID cohortId, UUID id, String ifMatch, CohortCourseUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_COURSE_PERMISSION_UPDATE);
            CohortCourseEntity entity = loadVisibleCohortCourse(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCohortCourse(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfCohortCourse(CohortCourseEntity entity) {
        return cohortCourseRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortCourse(CohortCourseEntity entity, CohortCourseUpdate changes) {
        Patches patch = new Patches();
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("evaluatorId", changes.getEvaluatorId(), entity::setEvaluatorId);
        return patch.applied();
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
        entity.setUserId(OwnRows.caller());
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CohortFeedback updateCohortFeedback(UUID cohortId, UUID id, String ifMatch, CohortFeedbackUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_FEEDBACK_PERMISSION_UPDATE);
            CohortFeedbackEntity entity = loadVisibleCohortFeedback(scope, cohortId, id);
            OwnRows.require(entity.getUserId(), COHORT_FEEDBACK_AGGREGATE, id);
            scope.requireWritable(institutionOfCohortFeedback(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCohortFeedback(entity, changes);
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
            OwnRows.require(entity.getUserId(), COHORT_FEEDBACK_AGGREGATE, id);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfCohortFeedback(CohortFeedbackEntity entity) {
        return cohortFeedbackRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortFeedback(CohortFeedbackEntity entity, CohortFeedbackUpdate changes) {
        Patches patch = new Patches();
        patch.apply("feedback", changes.getFeedback(), entity::setFeedback);
        patch.apply("contentRating", changes.getContentRating(), v -> entity.setContentRating(WireValues.decimal(v)));
        patch.apply("teacherRating", changes.getTeacherRating(), v -> entity.setTeacherRating(WireValues.decimal(v)));
        patch.apply("valueRating", changes.getValueRating(), v -> entity.setValueRating(WireValues.decimal(v)));
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CohortGroup updateCohortGroup(UUID cohortId, UUID id, String ifMatch, CohortGroupUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_GROUP_PERMISSION_UPDATE);
            CohortGroupEntity entity = loadVisibleCohortGroup(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortGroup(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCohortGroup(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfCohortGroup(CohortGroupEntity entity) {
        return cohortGroupRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortGroup(CohortGroupEntity entity, CohortGroupUpdate changes) {
        Patches patch = new Patches();
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("isDisabled", changes.getIsDisabled(), entity::setIsDisabled);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CohortHoliday updateCohortHoliday(UUID cohortId, UUID id, String ifMatch, CohortHolidayUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_HOLIDAY_PERMISSION_UPDATE);
            CohortHolidayEntity entity = loadVisibleCohortHoliday(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortHoliday(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCohortHoliday(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfCohortHoliday(CohortHolidayEntity entity) {
        return cohortHolidayRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortHoliday(CohortHolidayEntity entity, CohortHolidayUpdate changes) {
        Patches patch = new Patches();
        patch.apply("holidayId", changes.getHolidayId(), entity::setHolidayId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("startDate", changes.getStartDate(), entity::setStartDate);
        patch.apply("endDate", changes.getEndDate(), entity::setEndDate);
        patch.apply("isIncluded", changes.getIsIncluded(), entity::setIsIncluded);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CohortScheduleSlot updateCohortScheduleSlot(UUID cohortId, UUID id, String ifMatch,
            CohortScheduleSlotUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", Patches.value(changes.getStartTime()), wire);
        WireValues.checkTime("endTime", Patches.value(changes.getEndTime()), wire);
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
            List<String> fields = applyCohortScheduleSlot(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfCohortScheduleSlot(CohortScheduleSlotEntity entity) {
        return cohortScheduleSlotRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortScheduleSlot(CohortScheduleSlotEntity entity, CohortScheduleSlotUpdate changes) {
        Patches patch = new Patches();
        patch.apply("slotType", changes.getSlotType(), v -> entity.setSlotType(v == null ? null : v.getValue()));
        patch.apply("cohortTeacherId", changes.getCohortTeacherId(), entity::setCohortTeacherId);
        patch.apply("weekday", changes.getWeekday(), v -> entity.setWeekday(v == null ? null : v.getValue()));
        patch.apply("startTime", changes.getStartTime(), v -> entity.setStartTime(WireValues.time(v)));
        patch.apply("endTime", changes.getEndTime(), v -> entity.setEndTime(WireValues.time(v)));
        patch.apply("venue", changes.getVenue(), entity::setVenue);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CohortSession updateCohortSession(UUID cohortId, UUID id, String ifMatch, CohortSessionUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_SESSION_PERMISSION_UPDATE);
            CohortSessionEntity entity = loadVisibleCohortSession(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortSession(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCohortSession(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfCohortSession(CohortSessionEntity entity) {
        return cohortSessionRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortSession(CohortSessionEntity entity, CohortSessionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("startsAt", changes.getStartsAt(), entity::setStartsAt);
        patch.apply("endsAt", changes.getEndsAt(), entity::setEndsAt);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("venue", changes.getVenue(), entity::setVenue);
        patch.apply("isCancelled", changes.getIsCancelled(), entity::setIsCancelled);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CohortTeacher updateCohortTeacher(UUID cohortId, UUID id, String ifMatch, CohortTeacherUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_TEACHER_PERMISSION_UPDATE);
            CohortTeacherEntity entity = loadVisibleCohortTeacher(scope, cohortId, id);
            scope.requireWritable(institutionOfCohortTeacher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCohortTeacher(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfCohortTeacher(CohortTeacherEntity entity) {
        return cohortTeacherRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortTeacher(CohortTeacherEntity entity, CohortTeacherUpdate changes) {
        Patches patch = new Patches();
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("roleId", changes.getRoleId(), entity::setRoleId);
        patch.apply("roleLabel", changes.getRoleLabel(), entity::setRoleLabel);
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("hasOfficeHours", changes.getHasOfficeHours(), entity::setHasOfficeHours);
        patch.apply("removedAt", changes.getRemovedAt(), entity::setRemovedAt);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Forum updateForum(UUID cohortId, UUID id, String ifMatch, ForumUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FORUM_PERMISSION_UPDATE);
            ForumEntity entity = loadVisibleForum(scope, cohortId, id);
            scope.requireWritable(institutionOfForum(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyForum(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfForum(ForumEntity entity) {
        return forumRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyForum(ForumEntity entity, ForumUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("earlyAccessFrom", changes.getEarlyAccessFrom(), entity::setEarlyAccessFrom);
        patch.apply("opensAt", changes.getOpensAt(), entity::setOpensAt);
        patch.apply("closesAt", changes.getClosesAt(), entity::setClosesAt);
        patch.apply("lateAccessUntil", changes.getLateAccessUntil(), entity::setLateAccessUntil);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public PlagiarismCohortLink updatePlagiarismCohortLink(UUID cohortId, UUID id, String ifMatch,
            PlagiarismCohortLinkUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PLAGIARISM_COHORT_LINK_PERMISSION_UPDATE);
            PlagiarismCohortLinkEntity entity = loadVisiblePlagiarismCohortLink(scope, cohortId, id);
            scope.requireWritable(institutionOfPlagiarismCohortLink(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyPlagiarismCohortLink(entity, changes);
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

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOfPlagiarismCohortLink(PlagiarismCohortLinkEntity entity) {
        return plagiarismCohortLinkRepository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyPlagiarismCohortLink(PlagiarismCohortLinkEntity entity, PlagiarismCohortLinkUpdate changes) {
        Patches patch = new Patches();
        patch.apply("plagiarismAccountId", changes.getPlagiarismAccountId(), entity::setPlagiarismAccountId);
        patch.apply("accountRef", changes.getAccountRef(), entity::setAccountRef);
        patch.apply("providerClassRef", changes.getProviderClassRef(), entity::setProviderClassRef);
        patch.apply("providerClassTitle", changes.getProviderClassTitle(), entity::setProviderClassTitle);
        patch.apply("providerClassEndAt", changes.getProviderClassEndAt(), entity::setProviderClassEndAt);
        patch.apply("providerAssignmentRef", changes.getProviderAssignmentRef(), entity::setProviderAssignmentRef);
        patch.apply("providerAssignmentTitle", changes.getProviderAssignmentTitle(), entity::setProviderAssignmentTitle);
        patch.apply("providerAssignmentStartAt", changes.getProviderAssignmentStartAt(), entity::setProviderAssignmentStartAt);
        patch.apply("providerAssignmentDueAt", changes.getProviderAssignmentDueAt(), entity::setProviderAssignmentDueAt);
        patch.apply("providerAssignmentInstructions", changes.getProviderAssignmentInstructions(),
                                                entity::setProviderAssignmentInstructions);
        patch.apply("studentReportAccess", changes.getStudentReportAccess(), entity::setStudentReportAccess);
        patch.apply("teacherEmail", changes.getTeacherEmail(), entity::setTeacherEmail);
        patch.apply("teacherFirstName", changes.getTeacherFirstName(), entity::setTeacherFirstName);
        patch.apply("teacherLastName", changes.getTeacherLastName(), entity::setTeacherLastName);
        patch.apply("classCreated", changes.getClassCreated(), entity::setClassCreated);
        patch.apply("assignmentCreated", changes.getAssignmentCreated(), entity::setAssignmentCreated);
        return patch.applied();
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

    // ---- Cohort: reads for members, the workflow steps and the student access window ----

    static final String PERMISSION_READ = "lms.cohort.read";
    static final String PERMISSION_PUBLISH = "lms.cohort.publish_cohort";
    static final String PERMISSION_UNPUBLISH = "lms.cohort.unpublish_cohort";
    static final String PERMISSION_ACTIVATE = "lms.cohort.activate_cohort";
    static final String PERMISSION_DEACTIVATE = "lms.cohort.deactivate_cohort";
    static final String PERMISSION_ARCHIVE = "lms.cohort.archive_cohort";
    static final String PERMISSION_SET_STUDENT_ACCESS = "lms.cohort.set_student_access";
    static final String COHORT_COURSE_PERMISSION_LIST = "lms.cohort_course.read";
    /** Fields the guest projections leave out, so the public cannot filter on them either. */
    static final Set<String> COHORT_GUEST_HIDDEN = Set.of("createdBy", "updatedBy", "externalRef", "customScript",
            "conferencingAccountId", "confirmationTemplateId", "publishNotificationSent");
    static final Set<String> COHORT_COURSE_GUEST_HIDDEN = Set.of("evaluatorId", "createdBy", "updatedBy");
    private static final String STATE_REFUSED = "The cohort cannot be changed this way in its current state.";
    private static final String ROW_CHANGED = "The cohort changed since it was read; read it again.";

    /**
     * lms.GetCohort: for a member holding lms.cohort.read the cohort as they may see it (a hidden draft is 404); for
     * the public an open cohort ({@link #openToGuests}) in the guest projection.
     */
    public Cohort getCohort(UUID id) {
        if (publicReads.asMember(PERMISSION_READ)) {
            return doInReadTransaction(() -> mapper.toDto(access.visible(id, PERMISSION_READ)));
        }
        return doInReadTransaction(() -> guestProjection(mapper.toDto(openCohort(id))));
    }

    /** A cohort the public may see: published, open for self-enrolment and not ended (404 otherwise). */
    private CohortEntity openCohort(UUID id) {
        LocalDate today = LocalDate.now(clock);
        return repository.findById(id).filter(c -> Boolean.TRUE.equals(c.getIsPublished())
                && Boolean.TRUE.equals(c.getAllowSelfEnrollment()) && !c.getEndDate().isBefore(today))
                .orElseThrow(() -> new ResourceNotFoundException(CohortAccess.COHORT, id));
    }

    /** The public's view of a cohort: its page, not its internal references. */
    static Cohort guestProjection(Cohort cohort) {
        cohort.setCreatedBy(null);
        cohort.setUpdatedBy(null);
        cohort.setExternalRef(null);
        cohort.setCustomScript(null);
        cohort.setConferencingAccountId(null);
        cohort.setConfirmationTemplateId(null);
        cohort.setPublishNotificationSent(null);
        return cohort;
    }

    /**
     * lms.ListCohorts, member path: the cohorts of the caller's read scope that are theirs to see — every cohort where
     * they manage cohorts, elsewhere published cohorts and the ones they teach or study in.
     */
    public Page<Cohort> listCohorts(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CohortRepository.SORT_FIELDS, CohortRepository.DEFAULT_ORDER,
                CohortRepository.ID_COLUMN);
        if (!publicReads.asMember(PERMISSION_READ)) {
            SqlFragment open = FilterParser.parse(filter, PublicReads.guestFields(CohortRepository.FILTER_FIELDS,
                    COHORT_GUEST_HIDDEN)).and(CohortRepository.openToGuests(LocalDate.now(clock)));
            PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
            return doInReadTransaction(() -> repository.list(page, open).map(mapper::toDto)
                    .map(CohortService::guestProjection));
        }
        AuthContext caller = CohortAccess.member();
        DataScope manage = caller.hasPermission(PERMISSION_UPDATE) ? DataScope.of(caller, PERMISSION_UPDATE)
                : new DataScope(false, Set.of(), false);
        SqlFragment where = FilterParser.parse(filter, CohortRepository.FILTER_FIELDS)
                .and(CohortRepository.scope(DataScope.of(caller, PERMISSION_READ)))
                .and(CohortRepository.visibleToMember(manage, caller.userId()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /**
     * lms.ListCohortCourses: the courses of a cohort the member may see; for the public the published courses of an
     * open cohort, without who arranged them.
     */
    public Page<CohortCourse> listCohortCourses(UUID cohortId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        boolean member = publicReads.asMember(COHORT_COURSE_PERMISSION_LIST);
        SortSpec sort = SortSpec.parse(sortBy, CohortCourseRepository.SORT_FIELDS, CohortCourseRepository.DEFAULT_ORDER,
                CohortCourseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortCourseRepository.FILTER_FIELDS)
                .and(CohortCourseRepository.ofParent(cohortId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        if (!member) {
            SqlFragment open = FilterParser.parse(filter, PublicReads.guestFields(
                    CohortCourseRepository.FILTER_FIELDS, COHORT_COURSE_GUEST_HIDDEN))
                    .and(CohortCourseRepository.ofParent(cohortId)).and(CohortCourseRepository.PUBLISHED_COURSE);
            return doInReadTransaction(() -> {
                openCohort(cohortId);
                return cohortCourseRepository.list(page, open).map(cohortCourseMapper::toDto).map(course -> {
                    course.setEvaluatorId(null);
                    course.setCreatedBy(null);
                    course.setUpdatedBy(null);
                    return course;
                });
            });
        }
        return doInReadTransaction(() -> {
            access.visible(cohortId, COHORT_COURSE_PERMISSION_LIST);
            return cohortCourseRepository.list(page, where).map(cohortCourseMapper::toDto);
        });
    }

    /** lms.PublishCohort: into the catalogue; allowed while unpublished and neither suspended, completed nor archived. */
    public Cohort publishCohort(UUID id, String etag) {
        return step(id, etag, PERMISSION_PUBLISH, "publish", List.of("isPublished"), null,
                row -> !Boolean.TRUE.equals(row.getIsPublished())
                        && List.of("draft", "active").contains(row.getLifecycleStatus()),
                (row, now, actor) -> repository.publish(id, row.getRowVersion(), now, actor));
    }

    /** lms.UnpublishCohort: out of the catalogue; allowed while published. */
    public Cohort unpublishCohort(UUID id, String etag) {
        return step(id, etag, PERMISSION_UNPUBLISH, "unpublish", List.of("isPublished"), null,
                row -> Boolean.TRUE.equals(row.getIsPublished()),
                (row, now, actor) -> repository.unpublish(id, row.getRowVersion(), now, actor));
    }

    /** lms.ActivateCohort: opens the cohort to its members; allowed from draft or suspended. */
    public Cohort activateCohort(UUID id, String etag) {
        return step(id, etag, PERMISSION_ACTIVATE, "activate", List.of("lifecycleStatus"), null,
                row -> List.of("draft", "suspended").contains(row.getLifecycleStatus()),
                (row, now, actor) -> repository.setLifecycle(id, row.getRowVersion(), "active", now, actor));
    }

    /** lms.DeactivateCohort: suspends an active cohort; the reason is kept with the audit record. */
    public Cohort deactivateCohort(UUID id, String etag, String reason) {
        return step(id, etag, PERMISSION_DEACTIVATE, "deactivate", List.of("lifecycleStatus"), reason,
                row -> "active".equals(row.getLifecycleStatus()),
                (row, now, actor) -> repository.setLifecycle(id, row.getRowVersion(), "suspended", now, actor));
    }

    /** lms.ArchiveCohort: a finished cohort becomes read-only and leaves the catalogue; allowed unless draft. */
    public Cohort archiveCohort(UUID id, String etag) {
        return step(id, etag, PERMISSION_ARCHIVE, "archive", List.of("lifecycleStatus", "isPublished"), null,
                row -> List.of("active", "suspended", "completed").contains(row.getLifecycleStatus()),
                (row, now, actor) -> repository.archive(id, row.getRowVersion(), now, actor));
    }

    /**
     * lms.SetStudentAccess: the window in which students reach the cohort (either end may be left open). The window
     * must not end before it starts (422), and an archived cohort is read-only (409).
     */
    public Cohort setStudentAccess(UUID id, OffsetDateTime from, OffsetDateTime until) {
        if (from != null && until != null && until.isBefore(from)) {
            throw new ValidationFailedException(List.of(new FieldError("studentAccessUntil",
                    "must not be before studentAccessFrom", "INVALID_RANGE")));
        }
        return step(id, null, PERMISSION_SET_STUDENT_ACCESS, "set_student_access",
                List.of("studentAccessFrom", "studentAccessUntil"), null,
                row -> !"archived".equals(row.getLifecycleStatus()),
                (row, now, actor) -> repository.setStudentAccess(id, row.getRowVersion(), from, until, now, actor));
    }

    /** One guarded UPDATE of a step: what changes, under the row_version the guard was checked on. */
    @FunctionalInterface
    private interface StepWrite {
        int apply(CohortEntity row, OffsetDateTime now, UUID actor);
    }

    /**
     * A cohort step: the row under the step's permission scope (404), writable (403), the
     * body etag when sent (412), the guard on the row as read (409), one guarded UPDATE (0 rows → 412), the audit
     * record and the {@code lms.cohort.<step>} event in the same transaction.
     */
    private Cohort step(UUID id, String etag, String permission, String step, List<String> changed, String reason,
            Predicate<CohortEntity> guard, StepWrite write) {
        DataScope scope = DataScope.current(permission);
        OptionalInt expected = Etags.bodyVersion(etag);
        return doInWriteTransaction(() -> {
            CohortEntity row = loadVisible(scope, id);
            scope.requireWritable(institutionOf(row));
            if (expected.isPresent() && expected.getAsInt() != row.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!guard.test(row)) {
                throw new InvalidStateException(STATE_REFUSED);
            }
            if (write.apply(row, now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException(ROW_CHANGED);
            }
            audit.recordTransition(AGGREGATE, id, step, changed, reason);
            outbox.publish(AGGREGATE + "." + step, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    // ---- CohortActivity: a record of the Cohort aggregate (stored as the cohort's own lms.content_item) ----

    public static final String COHORT_ACTIVITY_AGGREGATE = "lms.cohort_activity";
    static final String COHORT_ACTIVITY_PERMISSION_LIST = "lms.cohort_activity.read";
    static final String COHORT_ACTIVITY_PERMISSION_GET = "lms.cohort_activity.read";
    static final String COHORT_ACTIVITY_PERMISSION_CREATE = "lms.cohort_activity.create";
    static final String COHORT_ACTIVITY_PERMISSION_UPDATE = "lms.cohort_activity.update";
    static final String COHORT_ACTIVITY_PERMISSION_DELETE = "lms.cohort_activity.delete";

    public CohortActivity getCohortActivity(UUID cohortId, UUID id) {
        return doInReadTransaction(() -> cohortActivityMapper.toDto(
                loadVisibleCohortActivity(DataScope.current(COHORT_ACTIVITY_PERMISSION_GET), cohortId, id)));
    }

    public Page<CohortActivity> listCohortActivities(UUID cohortId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CohortActivityRepository.SORT_FIELDS,
                CohortActivityRepository.DEFAULT_ORDER, CohortActivityRepository.ID_COLUMN);
        DataScope scope = DataScope.current(COHORT_ACTIVITY_PERMISSION_LIST);
        SqlFragment where = FilterParser.parse(filter, CohortActivityRepository.FILTER_FIELDS)
                .and(CohortActivityRepository.ofCohort(cohortId))
                .and(CohortActivityRepository.scope(scope));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + cohortId, clock);
        return doInReadTransaction(() -> contentItemRepository.list(page, where).map(cohortActivityMapper::toDto));
    }

    /**
     * Places one quiz, assignment or exercise in the cohort: exactly one reference (422), an existing one (422), not
     * already placed (409); with validateOnly every check runs and nothing is written.
     */
    public CohortActivity createCohortActivity(UUID cohortId, CohortActivity request, boolean validateOnly) {
        validator.validate(request);
        ContentItemEntity entity = ContentItemEntity.builder().cohortId(cohortId).quizId(request.getQuizId())
                .assignmentId(request.getAssignmentId()).exerciseId(request.getExerciseId()).build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COHORT_ACTIVITY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = repository.findById(cohortId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, cohortId));
            if (!scope.canRead(institutionOf(cohort))) {
                throw new ResourceNotFoundException(AGGREGATE, cohortId);
            }
            assertValidCohortActivity(entity);
            scope.requireWritable(institutionOf(cohort));
            assertUniqueCohortActivity(entity);
            if (validateOnly) {
                return cohortActivityMapper.toDto(entity);
            }
            ContentItemEntity stored = contentItemRepository.insert(entity);
            audit.recordCreate(COHORT_ACTIVITY_AGGREGATE, stored.getId());
            outbox.publish(COHORT_ACTIVITY_AGGREGATE + LmsEvents.CREATED, COHORT_ACTIVITY_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return cohortActivityMapper.toDto(stored);
        });
    }

    /** Partial update under If-Match: the activity may point at another quiz, assignment or exercise. */
    public CohortActivity updateCohortActivity(UUID cohortId, UUID id, String ifMatch, CohortActivityUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_ACTIVITY_PERMISSION_UPDATE);
            ContentItemEntity entity = loadVisibleCohortActivity(scope, cohortId, id);
            scope.requireWritable(repository.findById(cohortId).flatMap(this::institutionOf));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCohortActivity(entity, changes);
            validator.validate(cohortActivityMapper.toDto(entity));
            assertValidCohortActivity(entity);
            assertUniqueCohortActivity(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return cohortActivityMapper.toDto(entity);
            }
            if (contentItemRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COHORT_ACTIVITY_AGGREGATE, id, fields);
            outbox.publish(COHORT_ACTIVITY_AGGREGATE + LmsEvents.UPDATED, COHORT_ACTIVITY_AGGREGATE, id,
                    LmsEvents.payload(id));
            return cohortActivityMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while grades, submissions or release rules still reference the item. */
    public void deleteCohortActivity(UUID cohortId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COHORT_ACTIVITY_PERMISSION_DELETE);
            ContentItemEntity entity = loadVisibleCohortActivity(scope, cohortId, id);
            scope.requireWritable(repository.findById(cohortId).flatMap(this::institutionOf));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (contentItemRepository.referencedByGradebookEntryContentItemId(id)
                    || contentItemRepository.referencedByQuizSubmissionContentItemId(id)
                    || contentItemRepository.referencedByAssignmentSubmissionContentItemId(id)
                    || contentItemRepository.referencedByAssignmentGroupContentItemId(id)
                    || contentItemRepository.referencedByForumTopicContentItemId(id)
                    || contentItemRepository.referencedByReleaseRuleSourceContentItemId(id)
                    || contentItemRepository.referencedByReleaseRuleTargetContentItemId(id)
                    || contentItemRepository.referencedByReleaseGrantContentItemId(id)
                    || contentItemRepository.referencedByStudentAlertContentItemId(id)
                    || contentItemRepository.referencedByItemOverrideContentItemId(id)
                    || contentItemRepository.referencedByRubricAssessmentContentItemId(id)
                    || contentItemRepository.referencedByReassignmentLogContentItemId(id)) {
                throw new InvalidStateException(
                        "The cohort activity already has grades, submissions or release rules; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (contentItemRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COHORT_ACTIVITY_AGGREGATE, id);
            outbox.publish(COHORT_ACTIVITY_AGGREGATE + LmsEvents.DELETED, COHORT_ACTIVITY_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live activity of this cohort, if the caller may see the cohort: anything else is 404. */
    private ContentItemEntity loadVisibleCohortActivity(DataScope scope, UUID cohortId, UUID id) {
        ContentItemEntity entity = contentItemRepository.findById(id)
                .filter(row -> cohortId.equals(row.getCohortId()) && row.getChapterId() == null
                        && CohortActivityRepository.KINDS.contains(row.getItemKind()))
                .orElseThrow(() -> new ResourceNotFoundException(COHORT_ACTIVITY_AGGREGATE, id));
        Optional<CohortEntity> cohort = repository.findById(cohortId);
        if (cohort.isEmpty() || !scope.canRead(institutionOf(cohort.get()))) {
            throw new ResourceNotFoundException(COHORT_ACTIVITY_AGGREGATE, id);
        }
        return entity;
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCohortActivity(ContentItemEntity entity, CohortActivityUpdate changes) {
        Patches patch = new Patches();
        patch.apply("quizId", changes.getQuizId(), entity::setQuizId);
        patch.apply("assignmentId", changes.getAssignmentId(), entity::setAssignmentId);
        patch.apply("exerciseId", changes.getExerciseId(), entity::setExerciseId);
        return patch.applied();
    }

    /**
     * Exactly one of quiz, assignment and exercise, and it must exist (422 with every failing field); the item kind
     * of the stored row follows from the reference.
     */
    private void assertValidCohortActivity(ContentItemEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        long references = Stream.of(entity.getQuizId(), entity.getAssignmentId(),
                entity.getExerciseId()).filter(Objects::nonNull).count();
        if (references != 1) {
            errors.add(new FieldError("quizId", "exactly one of quizId, assignmentId and exerciseId is required",
                    "EXACTLY_ONE"));
        }
        if (entity.getQuizId() != null && !contentItemRepository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (entity.getAssignmentId() != null && !contentItemRepository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (entity.getExerciseId() != null && !contentItemRepository.existsExerciseId(entity.getExerciseId())) {
            errors.add(new FieldError("exerciseId", "exercise does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        entity.setItemKind(entity.getQuizId() != null ? "quiz" : entity.getAssignmentId() != null ? "assignment"
                : "exercise");
    }

    private void assertUniqueCohortActivity(ContentItemEntity entity) {
        if (cohortActivityRepository.taken(entity.getId(), entity.getCohortId(), entity.getQuizId(),
                entity.getAssignmentId(), entity.getExerciseId())) {
            throw new DuplicateResourceException("The quiz, assignment or exercise is already an activity of the cohort.");
        }
    }

    // ---- Cohort: the syllabus as a file ----

    static final String PERMISSION_SYLLABUS = "lms.cohort.get_cohort_syllabus";

    /**
     * lms.GetCohortSyllabus: the cohort's outline with its dates as a PDF file of the caller, downloaded through a
     * URL signed now (503 while platform cannot). Any cohort member may read it; pdf is the one format made.
     */
    public DownloadLink getCohortSyllabus(UUID cohortId, String format) {
        if (format != null && !"pdf".equals(format)) {
            throw new ValidationFailedException(List.of(new FieldError("format", "this syllabus is made as pdf",
                    "AllowedValues")));
        }
        record Syllabus(String title, List<CohortOutlineRepository.SyllabusRow> rows) {
        }
        Syllabus syllabus = doInReadTransaction(() -> {
            CohortEntity cohort = access.visible(cohortId, PERMISSION_SYLLABUS);
            return new Syllabus(cohort.getTitle(), outlineRepository.syllabus(cohortId));
        });
        List<String> lines = new ArrayList<>();
        lines.add(syllabus.title() == null ? "Cohort syllabus" : syllabus.title());
        lines.add("");
        String chapter = null;
        for (CohortOutlineRepository.SyllabusRow row : syllabus.rows()) {
            if (!java.util.Objects.equals(chapter, row.chapter())) {
                chapter = row.chapter();
                lines.add(chapter == null ? "" : chapter);
            }
            StringBuilder line = new StringBuilder("  - ");
            line.append(row.title() == null ? row.itemKind() : row.title());
            if (row.visibleFrom() != null) {
                line.append(", from ").append(row.visibleFrom().toLocalDate());
            }
            if (row.dueAt() != null) {
                line.append(", due ").append(row.dueAt().toLocalDate());
            }
            if (row.isGraded() && row.points() != null) {
                line.append(", ").append(row.points().toPlainString()).append(" points");
            }
            lines.add(line.toString());
        }
        UUID fileId = files.write("cohort-syllabus-" + cohortId + ".pdf", "application/pdf",
                Pdf.render("Syllabus", lines));
        return files.link(fileId, "cohortId");
    }

    // ---- Cohort: copying a course's outline into the cohort ----

    static final String PERMISSION_COPY_OUTLINE = "lms.cohort.copy_course_outline_to_cohort";
    private static final int OUTLINE_PAGE = 500;

    /**
     * lms.CopyCourseOutlineToCohort: copies the course's template (all chapters or the chosen ones, with their content
     * items and learning objectives, the course's grading categories and the release rules of the copied items) into
     * the cohort. Day offsets become dates from {@code shiftDatesFrom} (default: the cohort's start date) in the
     * cohort's time zone. A chapter the cohort already has a copy of is not copied again, so the step can be repeated
     * to pick up chapters added later. The course must be one the cohort teaches (422); an archived cohort is 409.
     * Answers the chapters copied.
     */
    public ChapterList copyCourseOutlineToCohort(UUID cohortId, UUID courseId, List<UUID> chapterIds,
            LocalDate shiftDatesFrom) {
        DataScope scope = DataScope.current(PERMISSION_COPY_OUTLINE);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = repository.findForUpdate(cohortId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, cohortId));
            if (!scope.canRead(institutionOf(cohort))) {
                throw new ResourceNotFoundException(AGGREGATE, cohortId);
            }
            scope.requireWritable(institutionOf(cohort));
            if ("archived".equals(cohort.getLifecycleStatus())) {
                throw new InvalidStateException("An archived cohort is read-only.");
            }
            if (courseId == null) {
                throw new ValidationFailedException(List.of(new FieldError("courseId", "must not be null",
                        "NotNull")));
            }
            if (!outlineRepository.isCourseOfCohort(cohortId, courseId)) {
                throw new ValidationFailedException(List.of(new FieldError("courseId",
                        "is not a course of the cohort", "NOT_FOUND")));
            }
            CohortOutlineRepository.Copy copy = new CohortOutlineRepository.Copy(cohortId,
                    shiftDatesFrom == null ? cohort.getStartDate() : shiftDatesFrom, zoneOf(cohort), now(),
                    Actor.current().orElse(null));
            CohortOutlineRepository.Pairs chapters = pairs(outlineRepository.templateChapters(courseId, cohortId,
                    chapterIds));
            if (chapters.isEmpty()) {
                return new ChapterList(List.of());
            }
            outlineRepository.copyChapters(copy, chapters);
            outlineRepository.copyGradingCategories(copy,
                    pairs(outlineRepository.templateGradingCategories(courseId, cohortId)));
            CohortOutlineRepository.Pairs items = pairs(outlineRepository.templateItems(chapters.olds()));
            outlineRepository.copyItems(copy, items, chapters);
            outlineRepository.copyObjectives(copy, pairs(outlineRepository.templateObjectives(chapters.olds())),
                    chapters);
            CohortOutlineRepository.Pairs rules = pairs(outlineRepository.templateRules(courseId, items.olds()));
            outlineRepository.copyRules(copy, rules, items);
            outlineRepository.copyConditions(copy, pairs(outlineRepository.templateConditions(rules.olds())), rules);
            outlineRepository.copyTargets(copy,
                    pairs(outlineRepository.templateTargets(rules.olds(), items.olds(), chapters.olds())), rules, items,
                    chapters);
            audit.recordTransition(AGGREGATE, cohortId, "copy_course_outline", List.of("chapters", "contentItems"),
                    null);
            outbox.publish(AGGREGATE + ".copy_course_outline", AGGREGATE, cohortId, Map.of("id", cohortId.toString(),
                    "courseId", courseId.toString(), "chapters", chapters.news().length, "items",
                    items.news().length));
            SortSpec sort = SortSpec.parse(List.of("sortOrder"), ChapterRepository.SORT_FIELDS,
                    ChapterRepository.DEFAULT_ORDER, ChapterRepository.ID_COLUMN);
            SqlFragment copied = new SqlFragment("r.id = ANY(:copiedChapters)", Map.of("copiedChapters",
                    chapters.news()));
            return new ChapterList(chapterRepository.list(PageRequest.of(1, OUTLINE_PAGE, null, sort, "outline",
                    clock), copied).items().stream().map(chapterMapper::toDto).toList());
        });
    }

    /** Old ids with a fresh UUIDv7 each, in order. */
    private CohortOutlineRepository.Pairs pairs(List<UUID> olds) {
        UUID[] news = new UUID[olds.size()];
        for (int i = 0; i < news.length; i++) {
            news[i] = UuidV7.next(clock);
        }
        return new CohortOutlineRepository.Pairs(olds.toArray(UUID[]::new), news);
    }

    /** The cohort's time zone, which day offsets are dated in (409 when it is not a known zone). */
    private static String zoneOf(CohortEntity cohort) {
        try {
            return ZoneId.of(cohort.getTimezone()).getId();
        } catch (DateTimeException unknown) {
            throw new ConflictException("INVALID_TIMEZONE", "The cohort's time zone is not a known zone.");
        }
    }
    /**
     * lms.GetUnreadCounts: the caller's unread forum posts across their cohorts, in all and per cohort.
     * Notifications and mail are 0 until comms serves its notification and mailbox reads.
     */
    public UnreadCounts getUnreadCounts() {
        UUID user = AuthContext.require().userId();
        return doInReadTransaction(() -> {
            List<LearningActivityRepository.CohortUnread> unread =
                    activity.unreadPosts(user, activity.memberCohorts(user));
            UnreadCounts counts = new UnreadCounts(
                    unread.stream().mapToInt(LearningActivityRepository.CohortUnread::posts).sum(), 0, 0);
            Map<String, Integer> perCohort = new LinkedHashMap<>();
            unread.forEach(u -> perCohort.put(u.cohortId().toString(), u.posts()));
            counts.setPerCohort(perCohort);
            return counts;
        });
    }
}
