package com.mysms.learning.lms.business;

import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.repository.LearningActivityRepository;
import com.mysms.learning.lms.api.model.ProgressBucket;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Objects;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Value;
import com.mysms.learning.lms.repository.LessonRepository;
import com.mysms.learning.lms.repository.LessonProgressRepository;
import com.mysms.learning.lms.repository.EnrollmentRepository;
import com.mysms.learning.lms.mapper.LessonProgressMapper;
import com.mysms.learning.lms.errors.NotConfiguredException;
import com.mysms.learning.lms.domain.LessonProgressEntity;
import com.mysms.learning.lms.domain.LessonEntity;
import com.mysms.learning.lms.domain.EnrollmentEntity;
import com.mysms.learning.lms.api.model.ScormLaunch;
import com.mysms.learning.lms.api.model.SaveScormProgressRequest;
import com.mysms.learning.lms.api.model.ReorderChaptersRequest;
import com.mysms.learning.lms.api.model.MoveChapterRequest;
import com.mysms.learning.lms.api.model.LessonProgress;
import com.mysms.learning.lms.api.model.CourseOutlineLesson;
import com.mysms.learning.lms.api.model.CourseOutlineChapter;
import com.mysms.learning.lms.api.model.CourseOutline;
import com.mysms.learning.lms.api.model.ChapterList;
import com.mysms.core.error.ConflictException;
import com.mysms.authclient.AuthContext;
import java.util.function.Predicate;
import java.util.Map;
import java.util.LinkedHashMap;
import com.mysms.learning.lms.api.model.Dependencies;
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
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.ApproveEnrollmentRequestRequest;
import com.mysms.learning.lms.api.model.BatchEnrollInCourseRequest;
import com.mysms.learning.lms.api.model.BatchEnrollPreview;
import com.mysms.learning.lms.api.model.BatchEnrollPreviewList;
import com.mysms.learning.lms.api.model.CalendarItem;
import com.mysms.learning.lms.api.model.Certificate;
import com.mysms.learning.lms.api.model.CreateCourseVersionRequest;
import com.mysms.learning.lms.api.model.EvaluatorSlot;
import com.mysms.learning.lms.api.model.TeacherDashboard;
import com.mysms.learning.lms.domain.CertificateEntity;
import com.mysms.learning.lms.domain.CohortEnrollmentEntity;
import com.mysms.learning.lms.domain.ContentItemEntity;
import com.mysms.learning.lms.domain.EvaluatorAvailabilityEntity;
import com.mysms.learning.lms.domain.EvaluatorEntity;
import com.mysms.learning.lms.domain.GradingCategoryEntity;
import com.mysms.learning.lms.domain.LearningObjectiveEntity;
import com.mysms.learning.lms.domain.ReleaseRuleConditionEntity;
import com.mysms.learning.lms.domain.ReleaseRuleTargetEntity;
import com.mysms.learning.lms.mapper.CertificateMapper;
import com.mysms.learning.lms.mapper.CohortMapper;
import com.mysms.learning.lms.mapper.StudentAlertMapper;
import com.mysms.learning.lms.repository.CertificateRepository;
import com.mysms.learning.lms.repository.CertificateRequestRepository;
import com.mysms.learning.lms.repository.CohortCourseRepository;
import com.mysms.learning.lms.repository.CohortEnrollmentRepository;
import com.mysms.learning.lms.repository.CohortRepository;
import com.mysms.learning.lms.repository.ContentItemRepository;
import com.mysms.learning.lms.repository.EvaluatorAvailabilityRepository;
import com.mysms.learning.lms.repository.EvaluatorRepository;
import com.mysms.learning.lms.repository.GradingCategoryRepository;
import com.mysms.learning.lms.repository.LearningObjectiveRepository;
import com.mysms.learning.lms.repository.ReleaseRuleConditionRepository;
import com.mysms.learning.lms.repository.ReleaseRuleRepository;
import com.mysms.learning.lms.repository.ReleaseRuleTargetRepository;
import com.mysms.learning.lms.repository.StudentAlertRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import com.mysms.learning.lms.domain.ReleaseRuleEntity;
import com.mysms.learning.lms.api.model.CatalogBanner;
import com.mysms.learning.lms.api.model.CatalogBannerUpdate;
import com.mysms.learning.lms.api.model.Chapter;
import com.mysms.learning.lms.api.model.ChapterUpdate;
import com.mysms.learning.lms.api.model.CourseAuthorProfile;
import com.mysms.learning.lms.api.model.CourseAuthorProfileUpdate;
import com.mysms.learning.lms.api.model.CourseInterest;
import com.mysms.learning.lms.api.model.CourseInterestUpdate;
import com.mysms.learning.lms.api.model.CourseMentor;
import com.mysms.learning.lms.api.model.CourseMentorUpdate;
import com.mysms.learning.lms.api.model.CoursePreview;
import com.mysms.learning.lms.api.model.CoursePreviewUpdate;
import com.mysms.learning.lms.api.model.CourseReview;
import com.mysms.learning.lms.api.model.CourseReviewUpdate;
import com.mysms.learning.lms.api.model.CourseTeacher;
import com.mysms.learning.lms.api.model.CourseTeacherUpdate;
import com.mysms.learning.lms.api.model.CourseTestimonial;
import com.mysms.learning.lms.api.model.CourseTestimonialUpdate;
import com.mysms.learning.lms.api.model.EnrollmentRequest;
import com.mysms.learning.lms.api.model.EnrollmentRequestUpdate;
import com.mysms.learning.lms.api.model.LmsCourse;
import com.mysms.learning.lms.api.model.LmsCourseUpdate;
import com.mysms.learning.lms.api.model.RejectEnrollmentRequestRequest;
import com.mysms.learning.lms.api.model.RelatedCourse;
import com.mysms.learning.lms.api.model.RelatedCourseUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.CatalogBannerEntity;
import com.mysms.learning.lms.domain.ChapterEntity;
import com.mysms.learning.lms.domain.CourseAuthorProfileEntity;
import com.mysms.learning.lms.domain.CourseInterestEntity;
import com.mysms.learning.lms.domain.CourseMentorEntity;
import com.mysms.learning.lms.domain.CoursePreviewEntity;
import com.mysms.learning.lms.domain.CourseReviewEntity;
import com.mysms.learning.lms.domain.CourseTeacherEntity;
import com.mysms.learning.lms.domain.CourseTestimonialEntity;
import com.mysms.learning.lms.domain.EnrollmentRequestEntity;
import com.mysms.learning.lms.domain.LmsCourseEntity;
import com.mysms.learning.lms.domain.RelatedCourseEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.CatalogBannerMapper;
import com.mysms.learning.lms.mapper.ChapterMapper;
import com.mysms.learning.lms.mapper.CourseAuthorProfileMapper;
import com.mysms.learning.lms.mapper.CourseInterestMapper;
import com.mysms.learning.lms.mapper.CourseMentorMapper;
import com.mysms.learning.lms.mapper.CoursePreviewMapper;
import com.mysms.learning.lms.mapper.CourseReviewMapper;
import com.mysms.learning.lms.mapper.CourseTeacherMapper;
import com.mysms.learning.lms.mapper.CourseTestimonialMapper;
import com.mysms.learning.lms.mapper.EnrollmentRequestMapper;
import com.mysms.learning.lms.mapper.LmsCourseMapper;
import com.mysms.learning.lms.mapper.RelatedCourseMapper;
import com.mysms.learning.lms.repository.CatalogBannerRepository;
import com.mysms.learning.lms.repository.ChapterRepository;
import com.mysms.learning.lms.repository.CourseAuthorProfileRepository;
import com.mysms.learning.lms.repository.CourseInterestRepository;
import com.mysms.learning.lms.repository.CourseMentorRepository;
import com.mysms.learning.lms.repository.CoursePreviewRepository;
import com.mysms.learning.lms.repository.CourseReviewRepository;
import com.mysms.learning.lms.repository.CourseTeacherRepository;
import com.mysms.learning.lms.repository.CourseTestimonialRepository;
import com.mysms.learning.lms.repository.EnrollmentRequestRepository;
import com.mysms.learning.lms.repository.LmsCourseRepository;
import com.mysms.learning.lms.repository.RelatedCourseRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.course and of the records that exist only as part of a LmsCourse (CatalogBanner, Chapter,
 * CourseAuthorProfile, CourseInterest, CourseMentor, CoursePreview, CourseReview, CourseTeacher, CourseTestimonial,
 * EnrollmentRequest, RelatedCourse): audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LmsCourseService extends AppTransactionManager {

    public static final String AGGREGATE = LmsCourseEntity.TABLE;
    static final String PERMISSION_READ = "lms.course.read";
    static final String PERMISSION_DELETE = "lms.course.delete";
    static final String PERMISSION_CHECK_DEPENDENCIES = "lms.course.check_course_dependencies";
    static final String PERMISSION_SUBMIT_FOR_REVIEW = "lms.course.submit_course_for_review";
    static final String PERMISSION_APPROVE = "lms.course.approve_course";
    static final String PERMISSION_PUBLISH = "lms.course.publish_course";
    static final String PERMISSION_UNPUBLISH = "lms.course.unpublish_course";
    static final String PERMISSION_DEACTIVATE = "lms.course.deactivate_course";
    static final String PERMISSION_REACTIVATE = "lms.course.reactivate_course";
    static final String PERMISSION_PUBLISH_CATALOG_LISTING = "lms.course.publish_catalog_listing";
    static final String PERMISSION_HIDE_CATALOG_LISTING = "lms.course.hide_catalog_listing";
    static final String PUBLISHED = "published";
    static final String PERMISSION_GET_OUTLINE = "lms.course.get_course_outline";
    static final String PERMISSION_PROGRESS_DISTRIBUTION = "lms.course.get_course_progress_distribution";
    static final String PERMISSION_REORDER_CHAPTERS = "lms.course.reorder_chapters";
    static final String CHAPTER_PERMISSION_MOVE = "lms.chapter.move_chapter";
    static final String CHAPTER_PERMISSION_SCORM_LAUNCH = "lms.chapter.get_scorm_launch";
    static final String PROGRESS_AGGREGATE = "lms.lesson_progress";
    /** The runtime-state keys a SCORM player saves (lms.SaveScormProgress) and gets back on launch. */
    static final String SCORM_SUSPEND_DATA = "suspendData";
    static final String SCORM_LOCATION = "location";
    static final String SCORM_SCORE = "score";
    static final String SCORM_TOTAL_TIME = "totalTimeSeconds";
    /** lms.lesson_progress.scorm_location is varchar(1000). */
    private static final int MAX_SCORM_LOCATION = 1000;
    static final String PERMISSION_CREATE = "lms.course.create";
    static final String PERMISSION_UPDATE = "lms.course.update";

    public static final String CATALOG_BANNER_AGGREGATE = CatalogBannerEntity.TABLE;
    static final String CATALOG_BANNER_PERMISSION_GET = "lms.catalog_banner.read";
    static final String CATALOG_BANNER_PERMISSION_CREATE = "lms.catalog_banner.create";
    static final String CATALOG_BANNER_PERMISSION_UPDATE = "lms.catalog_banner.update";
    static final String CATALOG_BANNER_PERMISSION_DELETE = "lms.catalog_banner.delete";

    public static final String CHAPTER_AGGREGATE = ChapterEntity.TABLE;
    static final String CHAPTER_PERMISSION_LIST = "lms.chapter.read";
    static final String CHAPTER_PERMISSION_GET = "lms.chapter.read";
    static final String CHAPTER_PERMISSION_CREATE = "lms.chapter.create";
    static final String CHAPTER_PERMISSION_UPDATE = "lms.chapter.update";
    static final String CHAPTER_PERMISSION_DELETE = "lms.chapter.delete";

    public static final String COURSE_AUTHOR_PROFILE_AGGREGATE = CourseAuthorProfileEntity.TABLE;
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_GET = "lms.course_author_profile.read";
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_CREATE = "lms.course_author_profile.create";
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_UPDATE = "lms.course_author_profile.update";
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_DELETE = "lms.course_author_profile.delete";

    public static final String COURSE_INTEREST_AGGREGATE = CourseInterestEntity.TABLE;
    static final String COURSE_INTEREST_PERMISSION_LIST = "lms.course_interest.read";
    static final String COURSE_INTEREST_PERMISSION_GET = "lms.course_interest.read";
    static final String COURSE_INTEREST_PERMISSION_CREATE = "lms.course_interest.create";
    static final String COURSE_INTEREST_PERMISSION_UPDATE = "lms.course_interest.update";
    static final String COURSE_INTEREST_PERMISSION_DELETE = "lms.course_interest.delete";

    public static final String COURSE_MENTOR_AGGREGATE = CourseMentorEntity.TABLE;
    static final String COURSE_MENTOR_PERMISSION_LIST = "lms.course_mentor.read";
    static final String COURSE_MENTOR_PERMISSION_GET = "lms.course_mentor.read";
    static final String COURSE_MENTOR_PERMISSION_CREATE = "lms.course_mentor.create";
    static final String COURSE_MENTOR_PERMISSION_UPDATE = "lms.course_mentor.update";
    static final String COURSE_MENTOR_PERMISSION_DELETE = "lms.course_mentor.delete";

    public static final String COURSE_PREVIEW_AGGREGATE = CoursePreviewEntity.TABLE;
    static final String COURSE_PREVIEW_PERMISSION_GET = "lms.course_preview.read";
    static final String COURSE_PREVIEW_PERMISSION_CREATE = "lms.course_preview.create";
    static final String COURSE_PREVIEW_PERMISSION_UPDATE = "lms.course_preview.update";
    static final String COURSE_PREVIEW_PERMISSION_DELETE = "lms.course_preview.delete";

    public static final String COURSE_REVIEW_AGGREGATE = CourseReviewEntity.TABLE;
    static final String COURSE_REVIEW_PERMISSION_GET = "lms.course_review.read";
    static final String COURSE_REVIEW_PERMISSION_CREATE = "lms.course_review.create";
    static final String COURSE_REVIEW_PERMISSION_UPDATE = "lms.course_review.update";
    static final String COURSE_REVIEW_PERMISSION_DELETE = "lms.course_review.delete";

    public static final String COURSE_TEACHER_AGGREGATE = CourseTeacherEntity.TABLE;
    static final String COURSE_TEACHER_PERMISSION_LIST = "lms.course_teacher.read";
    static final String COURSE_TEACHER_PERMISSION_GET = "lms.course_teacher.read";
    static final String COURSE_TEACHER_PERMISSION_CREATE = "lms.course_teacher.create";
    static final String COURSE_TEACHER_PERMISSION_UPDATE = "lms.course_teacher.update";
    static final String COURSE_TEACHER_PERMISSION_DELETE = "lms.course_teacher.delete";

    public static final String COURSE_TESTIMONIAL_AGGREGATE = CourseTestimonialEntity.TABLE;
    static final String COURSE_TESTIMONIAL_PERMISSION_GET = "lms.course_testimonial.read";
    static final String COURSE_TESTIMONIAL_PERMISSION_CREATE = "lms.course_testimonial.create";
    static final String COURSE_TESTIMONIAL_PERMISSION_UPDATE = "lms.course_testimonial.update";
    static final String COURSE_TESTIMONIAL_PERMISSION_DELETE = "lms.course_testimonial.delete";

    public static final String ENROLLMENT_REQUEST_AGGREGATE = EnrollmentRequestEntity.TABLE;
    static final String ENROLLMENT_REQUEST_PERMISSION_LIST = "lms.enrollment_request.read";
    static final String ENROLLMENT_REQUEST_PERMISSION_GET = "lms.enrollment_request.read";
    static final String ENROLLMENT_REQUEST_PERMISSION_CREATE = "lms.enrollment_request.create";
    static final String ENROLLMENT_REQUEST_PERMISSION_UPDATE = "lms.enrollment_request.update";
    static final String ENROLLMENT_REQUEST_PERMISSION_DELETE = "lms.enrollment_request.delete";
    static final String PERMISSION_REJECT_ENROLLMENT_REQUEST = "lms.enrollment_request.reject_enrollment_request";

    public static final String RELATED_COURSE_AGGREGATE = RelatedCourseEntity.TABLE;
    static final String RELATED_COURSE_PERMISSION_GET = "lms.related_course.read";
    static final String RELATED_COURSE_PERMISSION_CREATE = "lms.related_course.create";
    static final String RELATED_COURSE_PERMISSION_UPDATE = "lms.related_course.update";
    static final String RELATED_COURSE_PERMISSION_DELETE = "lms.related_course.delete";

    private final LmsCourseRepository repository;
    private final LmsCourseMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CatalogBannerRepository catalogBannerRepository;
    private final CatalogBannerMapper catalogBannerMapper;
    private final ChapterRepository chapterRepository;
    private final ChapterMapper chapterMapper;
    private final CourseAuthorProfileRepository courseAuthorProfileRepository;
    private final CourseAuthorProfileMapper courseAuthorProfileMapper;
    private final CourseInterestRepository courseInterestRepository;
    private final CourseInterestMapper courseInterestMapper;
    private final CourseMentorRepository courseMentorRepository;
    private final CourseMentorMapper courseMentorMapper;
    private final CoursePreviewRepository coursePreviewRepository;
    private final CoursePreviewMapper coursePreviewMapper;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewMapper courseReviewMapper;
    private final CourseTeacherRepository courseTeacherRepository;
    private final CourseTeacherMapper courseTeacherMapper;
    private final CourseTestimonialRepository courseTestimonialRepository;
    private final CourseTestimonialMapper courseTestimonialMapper;
    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final EnrollmentRequestMapper enrollmentRequestMapper;
    private final RelatedCourseRepository relatedCourseRepository;
    private final RelatedCourseMapper relatedCourseMapper;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final LessonProgressMapper progressMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final LearningActivityRepository activity;
    private final ContentItemRepository contentItems;
    private final LearningObjectiveRepository objectives;
    private final GradingCategoryRepository gradingCategories;
    private final ReleaseRuleRepository releaseRules;
    private final ReleaseRuleConditionRepository releaseRuleConditions;
    private final ReleaseRuleTargetRepository releaseRuleTargets;
    private final CohortRepository cohorts;
    private final CohortCourseRepository cohortCourses;
    private final CohortEnrollmentRepository cohortEnrollments;
    private final CohortMapper cohortMapper;
    private final CertificateRepository certificates;
    private final CertificateMapper certificateMapper;
    private final CertificateRequestRepository certificateRequests;
    private final EvaluatorRepository evaluators;
    private final EvaluatorAvailabilityRepository evaluatorAvailability;
    private final StudentAlertRepository studentAlerts;
    private final StudentAlertMapper studentAlertMapper;
    private final PlatformFiles files;
    private final String scormContentBaseUrl;
    private final PublicReads publicReads;

    public LmsCourseService(LmsCourseRepository repository,
            LmsCourseMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            CatalogBannerRepository catalogBannerRepository,
            CatalogBannerMapper catalogBannerMapper,
            ChapterRepository chapterRepository,
            ChapterMapper chapterMapper,
            CourseAuthorProfileRepository courseAuthorProfileRepository,
            CourseAuthorProfileMapper courseAuthorProfileMapper,
            CourseInterestRepository courseInterestRepository,
            CourseInterestMapper courseInterestMapper,
            CourseMentorRepository courseMentorRepository,
            CourseMentorMapper courseMentorMapper,
            CoursePreviewRepository coursePreviewRepository,
            CoursePreviewMapper coursePreviewMapper,
            CourseReviewRepository courseReviewRepository,
            CourseReviewMapper courseReviewMapper,
            CourseTeacherRepository courseTeacherRepository,
            CourseTeacherMapper courseTeacherMapper,
            CourseTestimonialRepository courseTestimonialRepository,
            CourseTestimonialMapper courseTestimonialMapper,
            EnrollmentRequestRepository enrollmentRequestRepository,
            EnrollmentRequestMapper enrollmentRequestMapper,
            RelatedCourseRepository relatedCourseRepository,
            RelatedCourseMapper relatedCourseMapper,
            LessonRepository lessonRepository,
            LessonProgressRepository progressRepository,
            LessonProgressMapper progressMapper,
            EnrollmentRepository enrollmentRepository,
            LearningActivityRepository activity,
            ContentItemRepository contentItems,
            LearningObjectiveRepository objectives,
            GradingCategoryRepository gradingCategories,
            ReleaseRuleRepository releaseRules,
            ReleaseRuleConditionRepository releaseRuleConditions,
            ReleaseRuleTargetRepository releaseRuleTargets,
            CohortRepository cohorts,
            CohortCourseRepository cohortCourses,
            CohortEnrollmentRepository cohortEnrollments,
            CohortMapper cohortMapper,
            CertificateRepository certificates,
            CertificateMapper certificateMapper,
            CertificateRequestRepository certificateRequests,
            EvaluatorRepository evaluators,
            EvaluatorAvailabilityRepository evaluatorAvailability,
            StudentAlertRepository studentAlerts,
            StudentAlertMapper studentAlertMapper,
            PlatformFiles files,
            @Value("${mysms.lms.scorm.content-base-url}") String scormContentBaseUrl, PublicReads publicReads) {
        super(database, transactions);
        this.publicReads = publicReads;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.catalogBannerRepository = catalogBannerRepository;
        this.catalogBannerMapper = catalogBannerMapper;
        this.chapterRepository = chapterRepository;
        this.chapterMapper = chapterMapper;
        this.courseAuthorProfileRepository = courseAuthorProfileRepository;
        this.courseAuthorProfileMapper = courseAuthorProfileMapper;
        this.courseInterestRepository = courseInterestRepository;
        this.courseInterestMapper = courseInterestMapper;
        this.courseMentorRepository = courseMentorRepository;
        this.courseMentorMapper = courseMentorMapper;
        this.coursePreviewRepository = coursePreviewRepository;
        this.coursePreviewMapper = coursePreviewMapper;
        this.courseReviewRepository = courseReviewRepository;
        this.courseReviewMapper = courseReviewMapper;
        this.courseTeacherRepository = courseTeacherRepository;
        this.courseTeacherMapper = courseTeacherMapper;
        this.courseTestimonialRepository = courseTestimonialRepository;
        this.courseTestimonialMapper = courseTestimonialMapper;
        this.enrollmentRequestRepository = enrollmentRequestRepository;
        this.enrollmentRequestMapper = enrollmentRequestMapper;
        this.relatedCourseRepository = relatedCourseRepository;
        this.relatedCourseMapper = relatedCourseMapper;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.progressMapper = progressMapper;
        this.enrollmentRepository = enrollmentRepository;
        this.activity = activity;
        this.contentItems = contentItems;
        this.objectives = objectives;
        this.gradingCategories = gradingCategories;
        this.releaseRules = releaseRules;
        this.releaseRuleConditions = releaseRuleConditions;
        this.releaseRuleTargets = releaseRuleTargets;
        this.cohorts = cohorts;
        this.cohortCourses = cohortCourses;
        this.cohortEnrollments = cohortEnrollments;
        this.cohortMapper = cohortMapper;
        this.certificates = certificates;
        this.certificateMapper = certificateMapper;
        this.certificateRequests = certificateRequests;
        this.evaluators = evaluators;
        this.evaluatorAvailability = evaluatorAvailability;
        this.studentAlerts = studentAlerts;
        this.studentAlertMapper = studentAlertMapper;
        this.files = files;
        this.scormContentBaseUrl = scormContentBaseUrl == null ? "" : scormContentBaseUrl.trim();
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LmsCourse createLmsCourse(LmsCourse request, boolean validateOnly) {
        validator.validate(request);
        LmsCourseEntity entity = mapper.toEntity(request);
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
            LmsCourseEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LmsCourse updateLmsCourse(UUID id, String ifMatch, LmsCourseUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LmsCourseEntity entity = loadVisible(scope, id);
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

    // ---- reads the public may make ----

    /**
     * A course. A member holding lms.course.read sees every course within their data scope; the public — a guest
     * naming the school, or a signed-in user without the permission — sees a published, active course only, in the
     * guest projection.
     */
    public LmsCourse getLmsCourse(UUID id) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        return doInReadTransaction(() -> {
            if (member) {
                return mapper.toDto(loadVisible(DataScope.current(PERMISSION_READ), id));
            }
            return guestProjection(mapper.toDto(loadPublic(id)));
        });
    }

    /** The tenant's courses within the caller's scope, page by page; published, active ones only to the public. */
    public Page<LmsCourse> listLmsCourses(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        SortSpec sort = SortSpec.parse(sortBy, LmsCourseRepository.SORT_FIELDS, LmsCourseRepository.DEFAULT_ORDER,
                LmsCourseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, member ? LmsCourseRepository.FILTER_FIELDS
                : PublicReads.guestFields(LmsCourseRepository.FILTER_FIELDS, COURSE_GUEST_HIDDEN));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        if (member) {
            SqlFragment scoped = where.and(LmsCourseRepository.scope(DataScope.current(PERMISSION_READ)));
            return doInReadTransaction(() -> repository.list(page, scoped).map(mapper::toDto));
        }
        return doInReadTransaction(() -> repository.list(page, where.and(LmsCourseRepository.PUBLISHED))
                .map(e -> guestProjection(mapper.toDto(e))));
    }

    /** Fields {@link #guestProjection} leaves out, so the public cannot filter on them either. */
    static final Set<String> COURSE_GUEST_HIDDEN = Set.of("authorComments", "externalRef", "publishNotificationSent",
            "defaultEvaluatorId", "gradingSchemeId", "createdBy", "updatedBy");
    /** Who wrote, created and changed a row: hidden from the public. */
    private static final Set<String> AUTHORS = Set.of("userId", "createdBy", "updatedBy");

    /** The public's view of a course: authoring notes, references and who changed what are internal. */
    static LmsCourse guestProjection(LmsCourse course) {
        course.setAuthorComments(null);
        course.setExternalRef(null);
        course.setPublishNotificationSent(null);
        course.setDefaultEvaluatorId(null);
        course.setGradingSchemeId(null);
        course.setCreatedBy(null);
        course.setUpdatedBy(null);
        return course;
    }

    /** A published, active course (404 otherwise: the public never learns that a draft exists). */
    private LmsCourseEntity loadPublic(UUID id) {
        return repository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getIsPublished()) && PUBLISHED.equals(e.getLifecycleStatus()))
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /**
     * How the caller reads a course's public children (banners, previews, reviews, testimonials, author profiles,
     * related courses): true for a member who sees the course within their scope; false for the public, once the course
     * proved published. Either way an invisible course is 404.
     */
    private boolean readCourseChildren(UUID courseId, String permission) {
        if (publicReads.asMember(permission)) {
            loadVisible(DataScope.current(permission), courseId);
            return true;
        }
        loadPublic(courseId);
        return false;
    }

    /** The catalogue banners of a course. */
    public Page<CatalogBanner> listCatalogBanners(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CatalogBannerRepository.SORT_FIELDS,
                CatalogBannerRepository.DEFAULT_ORDER, CatalogBannerRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CatalogBannerRepository.FILTER_FIELDS)
                .and(CatalogBannerRepository.ofParent(courseId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            boolean member = readCourseChildren(courseId, CATALOG_BANNER_PERMISSION_GET);
            return catalogBannerRepository.list(page, where).map(catalogBannerMapper::toDto)
                    .map(d -> member ? d : withoutAuthors(d));
        });
    }

    /** The preview items of a course (what a visitor may see before enrolling). */
    public Page<CoursePreview> listCoursePreviews(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CoursePreviewRepository.SORT_FIELDS,
                CoursePreviewRepository.DEFAULT_ORDER, CoursePreviewRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CoursePreviewRepository.FILTER_FIELDS)
                .and(CoursePreviewRepository.ofParent(courseId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            boolean member = readCourseChildren(courseId, COURSE_PREVIEW_PERMISSION_GET);
            return coursePreviewRepository.list(page, where).map(coursePreviewMapper::toDto)
                    .map(d -> member ? d : withoutAuthors(d));
        });
    }

    /** The students' reviews of a course; the public neither sees nor filters on who wrote them. */
    public Page<CourseReview> listCourseReviews(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CourseReviewRepository.SORT_FIELDS,
                CourseReviewRepository.DEFAULT_ORDER, CourseReviewRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            boolean member = readCourseChildren(courseId, COURSE_REVIEW_PERMISSION_GET);
            SqlFragment where = FilterParser.parse(filter, member ? CourseReviewRepository.FILTER_FIELDS
                    : PublicReads.guestFields(CourseReviewRepository.FILTER_FIELDS, AUTHORS))
                    .and(CourseReviewRepository.ofParent(courseId));
            return courseReviewRepository.list(page, where).map(courseReviewMapper::toDto)
                    .map(d -> member ? d : withoutAuthors(d));
        });
    }

    /** The testimonials shown on a course's page. */
    public Page<CourseTestimonial> listCourseTestimonials(UUID courseId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CourseTestimonialRepository.SORT_FIELDS,
                CourseTestimonialRepository.DEFAULT_ORDER, CourseTestimonialRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CourseTestimonialRepository.FILTER_FIELDS)
                .and(CourseTestimonialRepository.ofParent(courseId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            boolean member = readCourseChildren(courseId, COURSE_TESTIMONIAL_PERMISSION_GET);
            return courseTestimonialRepository.list(page, where).map(courseTestimonialMapper::toDto)
                    .map(d -> member ? d : withoutAuthors(d));
        });
    }

    /** The author profiles shown on a course's page. */
    public Page<CourseAuthorProfile> listCourseAuthorProfiles(UUID courseId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CourseAuthorProfileRepository.SORT_FIELDS,
                CourseAuthorProfileRepository.DEFAULT_ORDER, CourseAuthorProfileRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CourseAuthorProfileRepository.FILTER_FIELDS)
                .and(CourseAuthorProfileRepository.ofParent(courseId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            boolean member = readCourseChildren(courseId, COURSE_AUTHOR_PROFILE_PERMISSION_GET);
            return courseAuthorProfileRepository.list(page, where).map(courseAuthorProfileMapper::toDto)
                    .map(d -> member ? d : withoutAuthors(d));
        });
    }

    /** The courses recommended alongside a course; the public sees only those that are published themselves. */
    public Page<RelatedCourse> listRelatedCourses(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RelatedCourseRepository.SORT_FIELDS,
                RelatedCourseRepository.DEFAULT_ORDER, RelatedCourseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RelatedCourseRepository.FILTER_FIELDS)
                .and(RelatedCourseRepository.ofParent(courseId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            boolean member = readCourseChildren(courseId, RELATED_COURSE_PERMISSION_GET);
            if (member) {
                return relatedCourseRepository.list(page, where).map(relatedCourseMapper::toDto);
            }
            return relatedCourseRepository.list(page, where.and(RelatedCourseRepository.PUBLISHED_TARGET))
                    .map(relatedCourseMapper::toDto).map(LmsCourseService::withoutAuthors);
        });
    }

    /** The public's view of a course's child row: who wrote, created and changed it is internal. */
    private static <T> T withoutAuthors(T dto) {
        if (dto instanceof CatalogBanner d) {
            d.setCreatedBy(null);
            d.setUpdatedBy(null);
        } else if (dto instanceof CoursePreview d) {
            d.setCreatedBy(null);
            d.setUpdatedBy(null);
        } else if (dto instanceof CourseReview d) {
            d.setUserId(null);
            d.setCreatedBy(null);
            d.setUpdatedBy(null);
        } else if (dto instanceof CourseTestimonial d) {
            d.setCreatedBy(null);
            d.setUpdatedBy(null);
        } else if (dto instanceof CourseAuthorProfile d) {
            d.setCreatedBy(null);
            d.setUpdatedBy(null);
        } else if (dto instanceof RelatedCourse d) {
            d.setCreatedBy(null);
            d.setUpdatedBy(null);
        }
        return dto;
    }

    // ---- the outline: what a course page, a course home and the structure editor show ----

    /**
     * lms.GetCourseOutline: the course's template chapters (each followed by the chapters inside it) with their
     * lessons, for the public (a published course, guest projection) or a member in scope; a signed-in student also
     * gets their progress — the enrolment, its percentage and each lesson's status.
     */
    public CourseOutline getCourseOutline(UUID id) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        Optional<UUID> user = AuthContext.current().map(AuthContext::userId);
        return doInReadTransaction(() -> {
            Optional<EnrollmentEntity> enrolment = user.flatMap(u -> enrollmentRepository.findOwn(id, u));
            LmsCourseEntity course = member ? loadVisible(DataScope.current(PERMISSION_READ), id)
                    : enrolment.isPresent() ? repository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)) : loadPublic(id);
            List<ChapterEntity> chapters = inReadingOrder(chapterRepository.templateOutline(id));
            Map<UUID, List<LessonEntity>> lessons = new LinkedHashMap<>();
            if (!chapters.isEmpty()) {
                lessonRepository.ofChapters(chapters.stream().map(ChapterEntity::getId).toList())
                        .forEach(l -> lessons.computeIfAbsent(l.getChapterId(), k -> new ArrayList<>()).add(l));
            }
            Map<UUID, String> statuses = new LinkedHashMap<>();
            enrolment.ifPresent(e -> progressRepository.ownInCourse(e.getUserId(), id).stream()
                    .filter(p -> p.getLessonId() != null)
                    .forEach(p -> statuses.put(p.getLessonId(), p.getStatus())));
            LmsCourse dto = mapper.toDto(course);
            CourseOutline outline = new CourseOutline(member ? dto : guestProjection(dto));
            outline.setChapters(chapters.stream().map(c -> new CourseOutlineChapter(c.getId(), c.getTitle(),
                    c.getSortOrder(), Boolean.TRUE.equals(c.getIsScormPackage()))
                    .lessons(lessons.getOrDefault(c.getId(), List.of()).stream().map(l -> new CourseOutlineLesson(
                            l.getId(), l.getTitle(), l.getSortOrder(), Boolean.TRUE.equals(l.getIncludeInPreview()))
                            .progressStatus(statuses.get(l.getId()))).toList())).toList());
            enrolment.ifPresent(e -> {
                outline.setEnrollmentId(e.getId());
                outline.setProgressPercent(WireValues.decimal(e.getProgressPercent()));
            });
            return outline;
        });
    }

    /** Depth first: each chapter followed by the chapters inside it, every level in its own order. */
    private static List<ChapterEntity> inReadingOrder(List<ChapterEntity> chapters) {
        Map<UUID, List<ChapterEntity>> children = new LinkedHashMap<>();
        chapters.forEach(c -> children.computeIfAbsent(c.getParentChapterId(), k -> new ArrayList<>()).add(c));
        List<ChapterEntity> ordered = new ArrayList<>();
        appendLevel(children, null, ordered);
        return ordered;
    }

    private static void appendLevel(Map<UUID, List<ChapterEntity>> children, UUID parent, List<ChapterEntity> into) {
        for (ChapterEntity chapter : children.getOrDefault(parent, List.of())) {
            into.add(chapter);
            appendLevel(children, chapter.getId(), into);
        }
    }

    /**
     * lms.ReorderChapters: the course's top-level template chapters in the order given, which must name each of
     * them exactly once (422 otherwise). Answers the chapters in their new order.
     */
    public ChapterList reorderChapters(UUID courseId, ReorderChaptersRequest request) {
        validator.validate(request);
        if (request.getChapterIds().isEmpty()) {
            // gRPC has no absent list; an empty one is refused as REST refuses a missing one
            throw new ValidationFailedException(List.of(new FieldError("chapterIds", "must not be null", "NotNull")));
        }
        DataScope scope = DataScope.current(PERMISSION_REORDER_CHAPTERS);
        return doInWriteTransaction(() -> {
            LmsCourseEntity course = loadVisible(scope, courseId);
            scope.requireWritable(institutionOf(course));
            List<UUID> current = chapterRepository.siblings(courseId, null, null).stream().map(ChapterEntity::getId)
                    .toList();
            List<UUID> wanted = request.getChapterIds();
            if (wanted.size() != current.size() || !new HashSet<>(wanted).equals(new HashSet<>(current))) {
                throw new ValidationFailedException(List.of(new FieldError("chapterIds",
                        "must list each top-level chapter of the course exactly once", "Invalid")));
            }
            chapterRepository.renumber(wanted, null, now(), Actor.current().orElse(null));
            audit.recordTransition(AGGREGATE, courseId, "reorder_chapters", List.of("chapters"), null);
            outbox.publish(AGGREGATE + ".reorder_chapters", AGGREGATE, courseId, LmsEvents.payload(courseId));
            return new ChapterList(chapterRepository.siblings(courseId, null, null).stream()
                    .map(chapterMapper::toDto).toList());
        });
    }

    /**
     * lms.MoveChapter: puts a chapter at a place (0-based) under a parent chapter of the same course and outline
     * (cohort), or at the top level; the siblings there are renumbered around it. A parent that is the chapter itself,
     * inside it, or in another outline is 422.
     */
    public Chapter moveChapter(UUID chapterId, MoveChapterRequest request) {
        validator.validate(request);
        if (request.getPosition() < 0) {
            throw new ValidationFailedException(List.of(new FieldError("position", "must be 0 or more", "Min")));
        }
        DataScope scope = DataScope.current(CHAPTER_PERMISSION_MOVE);
        return doInWriteTransaction(() -> {
            ChapterEntity chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_AGGREGATE, chapterId));
            LmsCourseEntity course = loadVisible(scope, chapter.getCourseId());
            scope.requireWritable(institutionOf(course));
            UUID parent = request.getParentChapterId();
            if (parent != null && !isPlaceFor(chapter, parent)) {
                throw new ValidationFailedException(List.of(new FieldError("parentChapterId",
                        "must be another chapter of the same outline, not inside this one", "Invalid")));
            }
            List<UUID> siblings = new ArrayList<>(chapterRepository.siblings(chapter.getCourseId(),
                    chapter.getCohortId(), parent).stream().map(ChapterEntity::getId).filter(i -> !i.equals(chapterId))
                    .toList());
            siblings.add(Math.min(request.getPosition(), siblings.size()), chapterId);
            chapterRepository.renumber(siblings, parent, now(), Actor.current().orElse(null));
            audit.recordTransition(CHAPTER_AGGREGATE, chapterId, "move", List.of("parentChapterId", "sortOrder"),
                    null);
            outbox.publish(CHAPTER_AGGREGATE + ".move", CHAPTER_AGGREGATE, chapterId, LmsEvents.payload(chapterId));
            return chapterMapper.toDto(chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_AGGREGATE, chapterId)));
        });
    }

    /** Whether the chapter may move under this parent: same course and outline, and not into its own subtree. */
    private boolean isPlaceFor(ChapterEntity chapter, UUID parentId) {
        UUID at = parentId;
        while (at != null) {
            if (at.equals(chapter.getId())) {
                return false;
            }
            Optional<ChapterEntity> step = chapterRepository.findById(at);
            if (step.isEmpty() || !step.get().getCourseId().equals(chapter.getCourseId())
                    || !Objects.equals(step.get().getCohortId(), chapter.getCohortId())) {
                return false;
            }
            at = step.get().getParentChapterId();
        }
        return true;
    }

    // ---- SCORM chapters: the player's launch and saved state ----

    /**
     * lms.GetScormLaunch: the package URL under {@code mysms.lms.scorm.content-base-url} and the caller's saved state.
     * A student must be enrolled (404 otherwise); 409 NOT_SCORM for a plain chapter, 503 NOT_CONFIGURED without a
     * package host.
     */
    public ScormLaunch getScormLaunch(UUID chapterId) {
        UUID user = AuthContext.require().userId();
        return doInReadTransaction(() -> {
            ChapterEntity chapter = scormChapter(chapterId, user, CHAPTER_PERMISSION_SCORM_LAUNCH);
            if (scormContentBaseUrl.isEmpty()) {
                throw new NotConfiguredException("No host serves SCORM packages in this deployment.");
            }
            if (chapter.getScormPackagePath() == null || chapter.getScormLaunchFile() == null) {
                throw new ConflictException("SCORM_NOT_READY", "The chapter's SCORM package has not been extracted.");
            }
            ScormLaunch launch = new ScormLaunch(scormUrl(chapter));
            progressRepository.findOwn(user, chapterId, null).ifPresent(p -> {
                Map<String, Object> state = new LinkedHashMap<>();
                state.put("status", p.getStatus());
                putIfSet(state, SCORM_SUSPEND_DATA, p.getScormSuspendData());
                putIfSet(state, SCORM_LOCATION, p.getScormLocation());
                putIfSet(state, SCORM_SCORE, WireValues.decimal(p.getScormScore()));
                putIfSet(state, SCORM_TOTAL_TIME, p.getScormTotalTimeSeconds());
                launch.setRuntimeState(state);
            });
            return launch;
        });
    }

    /**
     * lms.SaveScormProgress: stores the enrolled student's SCORM runtime state for a chapter (a completed chapter
     * stays complete) and recomputes the enrolment's progress in the same transaction.
     */
    public LessonProgress saveScormProgress(UUID chapterId, SaveScormProgressRequest request) {
        validator.validate(request);
        UUID user = AuthContext.require().userId();
        LessonProgressEntity row = new LessonProgressEntity();
        row.setStatus(request.getStatus().getValue());
        readScormState(request.getRuntimeState(), row);
        row.onCreatingData(Actor.current(), clock);
        row.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        row.setUserId(user);
        row.setChapterId(chapterId);
        row.setIsScormChapter(true);
        return doInWriteTransaction(() -> {
            ChapterEntity chapter = chapterRepository.findById(chapterId)
                    .filter(c -> Boolean.TRUE.equals(c.getIsScormPackage()))
                    .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_AGGREGATE, chapterId));
            if (enrollmentRepository.findOwn(chapter.getCourseId(), user).isEmpty()) {
                throw new ConflictException("NOT_ENROLLED", "Enrol in the course first.");
            }
            row.setCourseId(chapter.getCourseId());
            OffsetDateTime now = now();
            LessonProgressEntity stored = progressRepository.upsert(row, now, user);
            enrollmentRepository.recordProgress(chapter.getCourseId(), user, null, now, user);
            audit.recordTransition(PROGRESS_AGGREGATE, stored.getId(), "save_scorm_progress", List.of("status"), null);
            return progressMapper.toDto(stored);
        });
    }

    /** A SCORM chapter the caller may launch: a member in scope, or a student enrolled in its course. */
    private ChapterEntity scormChapter(UUID chapterId, UUID user, String permission) {
        ChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException(CHAPTER_AGGREGATE, chapterId));
        boolean member = PublicReads.member(PERMISSION_READ)
                && repository.findById(chapter.getCourseId()).map(c -> DataScope.current(permission)
                        .canRead(institutionOf(c))).orElse(false);
        if (!member && enrollmentRepository.findOwn(chapter.getCourseId(), user).isEmpty()) {
            throw new ResourceNotFoundException(CHAPTER_AGGREGATE, chapterId);
        }
        if (!Boolean.TRUE.equals(chapter.getIsScormPackage())) {
            throw new ConflictException("NOT_SCORM", "The chapter is not a SCORM package.");
        }
        return chapter;
    }

    private String scormUrl(ChapterEntity chapter) {
        String base = scormContentBaseUrl.endsWith("/")
                ? scormContentBaseUrl.substring(0, scormContentBaseUrl.length() - 1) : scormContentBaseUrl;
        return base + "/" + encodePath(chapter.getScormPackagePath()) + "/" + encodePath(chapter.getScormLaunchFile());
    }

    /** Each segment of a stored path, URL-encoded (a path never leaves the package by "." or ".." segments). */
    private static String encodePath(String path) {
        return Arrays.stream(path.split("/")).filter(seg -> !seg.isEmpty() && !".".equals(seg) && !"..".equals(seg))
                .map(seg -> URLEncoder.encode(seg, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    private static void putIfSet(Map<String, Object> state, String key, Object value) {
        if (value != null) {
            state.put(key, value);
        }
    }

    /** The saved runtime state, field by field, checked against the columns that keep it (422 otherwise). */
    private static void readScormState(Object runtimeState, LessonProgressEntity row) {
        if (!(runtimeState instanceof Map<?, ?> state)) {
            throw new ValidationFailedException(List.of(new FieldError("runtimeState", "must be a JSON object",
                    "Type")));
        }
        List<FieldError> errors = new ArrayList<>();
        Object suspend = state.get(SCORM_SUSPEND_DATA);
        if (suspend != null) {
            row.setScormSuspendData(String.valueOf(suspend));
        }
        Object location = state.get(SCORM_LOCATION);
        if (location != null) {
            String text = String.valueOf(location);
            if (text.length() > MAX_SCORM_LOCATION) {
                errors.add(new FieldError("runtimeState.location", "at most 1000 characters", "Size"));
            }
            row.setScormLocation(text);
        }
        Object score = state.get(SCORM_SCORE);
        if (score != null) {
            WireValues.checkDecimal("runtimeState.score", String.valueOf(score), 6, 2, errors);
            if (errors.isEmpty()) {
                row.setScormScore(WireValues.decimal(String.valueOf(score)));
            }
        }
        Object time = state.get(SCORM_TOTAL_TIME);
        if (time != null) {
            if (time instanceof Number n && n.longValue() >= 0 && n.longValue() <= Integer.MAX_VALUE
                    && n.doubleValue() == n.longValue()) {
                row.setScormTotalTimeSeconds(n.intValue());
            } else {
                errors.add(new FieldError("runtimeState.totalTimeSeconds", "must be a whole number of seconds",
                        "Type"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * lms.GetCourseProgressDistribution: how many of the course's students are in each progress range (not started,
     * each quarter, complete), for a member who sees the course. The six ranges always come in one page.
     */
    public Page<ProgressBucket> getCourseProgressDistribution(UUID courseId) {
        DataScope scope = DataScope.current(PERMISSION_PROGRESS_DISTRIBUTION);
        return doInReadTransaction(() -> {
            loadVisible(scope, courseId);
            List<ProgressBucket> buckets = activity.progressBuckets(courseId).stream()
                    .map(b -> new ProgressBucket(b.label(), b.students())).toList();
            return new Page<>(buckets, 1, buckets.size(), null);
        });
    }


    // ---- enrolment approval, batch enrolment, versions, slots, certificates, the teacher's home ----

    static final String PERMISSION_APPROVE_ENROLLMENT_REQUEST =
            "lms.enrollment_request.approve_enrollment_request";
    static final String PERMISSION_BATCH_ENROLL = "lms.course.batch_enroll_in_course";
    static final String PERMISSION_CREATE_VERSION = "lms.course.create_course_version";
    static final String PERMISSION_EVALUATOR_SLOTS = "lms.course.get_evaluator_slots";
    static final String PERMISSION_SYLLABUS = "lms.course.get_course_syllabus";
    static final int MAX_BATCH_ENROLL = 500;
    static final int MAX_SLOT_DAYS = 92;
    static final int DASHBOARD_COHORTS = 20;
    static final int DASHBOARD_ITEMS = 10;

    /**
     * lms.ApproveEnrollmentRequest: approves the pending request (decided_by, decided_at) and enrols its student in
     * the named cohort of the course, in one transaction. A cohort of another course is 422; a student already in
     * the cohort keeps their enrolment.
     */
    public EnrollmentRequest approveEnrollmentRequest(UUID courseId, UUID requestId,
            ApproveEnrollmentRequestRequest request) {
        DataScope scope = DataScope.current(PERMISSION_APPROVE_ENROLLMENT_REQUEST);
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            EnrollmentRequestEntity row = enrollmentRequestRepository.findById(requestId)
                    .filter(r -> courseId.equals(r.getCourseId()))
                    .orElseThrow(() -> new ResourceNotFoundException(ENROLLMENT_REQUEST_AGGREGATE, requestId));
            LmsCourseEntity course = loadVisible(scope, courseId);
            scope.requireWritable(institutionOf(course));
            validator.validate(request);
            if (request.getCohortId() == null) {
                throw new ValidationFailedException(List.of(new FieldError("cohortId",
                        "must name the cohort the student joins", "NotNull")));
            }
            if (expected.isPresent() && expected.getAsInt() != row.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"pending".equals(row.getStatus())) {
                throw new InvalidStateException("The enrolment request was already decided.");
            }
            if (cohorts.findById(request.getCohortId()).isEmpty()
                    || !cohortCourses.linked(request.getCohortId(), courseId)) {
                throw new ValidationFailedException(List.of(new FieldError("cohortId",
                        "must be a cohort that teaches the course", "NOT_FOUND")));
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            if (enrollmentRequestRepository.approve(requestId, row.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            if (!cohortEnrollments.exists(request.getCohortId(), row.getUserId())) {
                CohortEnrollmentEntity member = CohortEnrollmentEntity.builder().cohortId(request.getCohortId())
                        .userId(row.getUserId()).isActive(true).isSelfEnrolled(false).confirmationEmailSent(false)
                        .gradePublished(false).hasAccommodation(false).build();
                member.onCreatingData(Actor.current(), clock);
                member.setTenantId(tenant);
                cohortEnrollments.insert(member);
                audit.recordCreate(CohortEnrollmentEntity.TABLE, member.getId());
            }
            audit.recordTransition(ENROLLMENT_REQUEST_AGGREGATE, requestId, "approve",
                    List.of("status", "decidedBy", "decidedAt"), null);
            outbox.publish(ENROLLMENT_REQUEST_AGGREGATE + ".approve", ENROLLMENT_REQUEST_AGGREGATE, requestId,
                    LmsEvents.payload(requestId));
            return enrollmentRequestMapper.toDto(enrollmentRequestRepository.findById(requestId).orElseThrow(
                    () -> new ResourceNotFoundException(ENROLLMENT_REQUEST_AGGREGATE, requestId)));
        });
    }

    /**
     * lms.BatchEnrollInCourse: enrols up to 500 named students in the course, or previews the run (validateOnly).
     * Each student's line says whether they were already enrolled and whether a paid course still waits for payment
     * (the payment ids passed stand for the students' settled payments; without any, a paid course enrols nobody).
     */
    public BatchEnrollPreviewList batchEnrollInCourse(UUID courseId, BatchEnrollInCourseRequest request) {
        validator.validate(request);
        List<UUID> users = request.getUserIds().stream().distinct().toList();
        if (users.isEmpty() || users.size() > MAX_BATCH_ENROLL) {
            throw new ValidationFailedException(List.of(new FieldError("userIds", "name 1 to 500 students", "Size")));
        }
        boolean preview = Boolean.TRUE.equals(request.getValidateOnly());
        boolean paymentsNamed = request.getPaymentIds() != null && !request.getPaymentIds().isEmpty();
        DataScope scope = DataScope.current(PERMISSION_BATCH_ENROLL);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            LmsCourseEntity course = loadVisible(scope, courseId);
            scope.requireWritable(institutionOf(course));
            if (!PUBLISHED.equals(course.getLifecycleStatus())) {
                throw new InvalidStateException("Only a published course takes enrolments.");
            }
            boolean paymentRequired = Boolean.TRUE.equals(course.getIsPaid()) && !paymentsNamed;
            List<BatchEnrollPreview> lines = new ArrayList<>();
            for (UUID user : users) {
                boolean already = enrollmentRepository.findOwn(courseId, user).isPresent();
                lines.add(new BatchEnrollPreview(user, already, !already && paymentRequired));
                if (preview || already || paymentRequired) {
                    continue;
                }
                EnrollmentEntity entity = new EnrollmentEntity();
                entity.setCourseId(courseId);
                entity.setUserId(user);
                entity.setMemberType("student");
                entity.setMemberRole("member");
                entity.setProgressPercent(BigDecimal.ZERO);
                entity.setPurchasedCertificate(Boolean.TRUE.equals(request.getPurchasedCertificate()));
                entity.onCreatingData(Actor.current(), clock);
                entity.setTenantId(tenant);
                EnrollmentEntity stored = enrollmentRepository.insert(entity);
                repository.countEnrollment(courseId);
                audit.recordCreate(EnrollmentEntity.TABLE, stored.getId());
                outbox.publish(EnrollmentEntity.TABLE + LmsEvents.CREATED, EnrollmentEntity.TABLE, stored.getId(),
                        LmsEvents.payload(stored.getId()));
            }
            return new BatchEnrollPreviewList(lines);
        });
    }

    /**
     * lms.CreateCourseVersion: copies the course into its next major or minor version — chapters, lessons, items,
     * objectives, grading categories and release rules with their conditions and targets — as a fresh draft with
     * previous_version_id and root_course_id set. Cohorts, enrolments and reviews stay with the old version.
     */
    public LmsCourse createCourseVersion(UUID courseId, CreateCourseVersionRequest request) {
        validator.validate(request);
        boolean major = request.getVersionKind() == CreateCourseVersionRequest.VersionKindEnum.MAJOR;
        DataScope scope = DataScope.current(PERMISSION_CREATE_VERSION);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            LmsCourseEntity source = loadVisible(scope, courseId);
            scope.requireWritable(institutionOf(source));
            LmsCourseEntity next = nextVersionOf(source, major, request.getAuthorComments());
            next.onCreatingData(Actor.current(), clock);
            next.setTenantId(tenant);
            LmsCourseEntity stored = repository.insert(next);
            copyStructure(source, stored, tenant);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + ".create_version", AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return mapper.toDto(repository.findById(stored.getId()).orElseThrow(
                    () -> new ResourceNotFoundException(AGGREGATE, stored.getId())));
        });
    }

    /** The next version's course row: the source's content, a fresh review, lifecycle and catalogue state. */
    private static LmsCourseEntity nextVersionOf(LmsCourseEntity s, boolean major, String authorComments) {
        return LmsCourseEntity.builder()
                .institutionId(s.getInstitutionId()).title(s.getTitle()).shortIntroduction(s.getShortIntroduction())
                .description(s.getDescription()).imageFileId(s.getImageFileId()).videoUrl(s.getVideoUrl())
                .categoryId(s.getCategoryId()).status("in_progress").isPublished(false).publishedOn(null)
                .isUpcoming(s.getIsUpcoming()).isFeatured(s.getIsFeatured())
                .disableSelfLearning(s.getDisableSelfLearning()).enforceLessonOrder(s.getEnforceLessonOrder())
                .certificationEnabled(s.getCertificationEnabled()).paidCertificate(s.getPaidCertificate())
                .defaultEvaluatorId(s.getDefaultEvaluatorId()).isPaid(s.getIsPaid()).price(s.getPrice())
                .currencyId(s.getCurrencyId()).priceUsd(s.getPriceUsd()).cardGradient(s.getCardGradient())
                .timezone(s.getTimezone()).publishNotificationSent(false).enrollmentCount(0)
                .lessonCount(s.getLessonCount()).averageRating(null).code(s.getCode())
                .stateStandardCode(s.getStateStandardCode()).credits(s.getCredits())
                .prerequisites(s.getPrerequisites()).corequisites(s.getCorequisites())
                .effectiveFrom(s.getEffectiveFrom()).lifecycleStatus("draft").deactivatedOn(null)
                .versionMajor(major ? versionOf(s.getVersionMajor()) + 1 : versionOf(s.getVersionMajor()))
                .versionMinor(major ? 0 : versionOf(s.getVersionMinor()) + 1)
                .previousVersionId(s.getId())
                .rootCourseId(s.getRootCourseId() == null ? s.getId() : s.getRootCourseId())
                .authorComments(authorComments).externalRef(s.getExternalRef())
                .nominalDuration(s.getNominalDuration()).deliveryModality(s.getDeliveryModality())
                .isShared(s.getIsShared()).contentUpdatedAt(null).gradeDisplay(s.getGradeDisplay())
                .gradingSchemeId(s.getGradingSchemeId()).isInCatalog(false)
                .catalogDescription(s.getCatalogDescription()).catalogVisibleFrom(s.getCatalogVisibleFrom())
                .catalogVisibleUntil(s.getCatalogVisibleUntil()).outline(s.getOutline())
                .workloadHours(s.getWorkloadHours()).catalogStatus("draft")
                .build();
    }

    private static int versionOf(Integer version) {
        return version == null ? 1 : version;
    }

    /** Copies the source's template structure under the new version, keeping every cross-reference inside it. */
    private void copyStructure(LmsCourseEntity source, LmsCourseEntity next, UUID tenant) {
        Map<UUID, UUID> chapterIds = new LinkedHashMap<>();
        Map<UUID, UUID> lessonIds = new LinkedHashMap<>();
        Map<UUID, UUID> itemIds = new LinkedHashMap<>();
        List<ChapterEntity> sourceChapters = inReadingOrder(chapterRepository.templateOutline(source.getId()));
        for (ChapterEntity chapter : sourceChapters) {
            UUID oldId = chapter.getId();
            chapter.setCourseId(next.getId());
            chapter.setParentChapterId(chapter.getParentChapterId() == null ? null
                    : chapterIds.get(chapter.getParentChapterId()));
            // a template chapter carries no copied_from_id (ck_chapter_1); the new course row records the lineage
            chapter.setIsOriginal(false);
            chapter.onCreatingData(Actor.current(), clock);
            chapter.setTenantId(tenant);
            chapterIds.put(oldId, chapterRepository.insert(chapter).getId());
        }
        List<UUID> oldChapterIds = List.copyOf(chapterIds.keySet());
        for (LessonEntity lesson : lessonRepository.ofChapters(oldChapterIds)) {
            UUID oldId = lesson.getId();
            lesson.setChapterId(chapterIds.get(lesson.getChapterId()));
            lesson.onCreatingData(Actor.current(), clock);
            lesson.setTenantId(tenant);
            lessonRepository.insert(lesson);
            lessonIds.put(oldId, lesson.getId());
        }
        for (ContentItemEntity item : contentItems.ofChapters(oldChapterIds)) {
            UUID oldId = item.getId();
            item.setChapterId(chapterIds.get(item.getChapterId()));
            item.setCourseId(next.getId());
            if (item.getLessonId() != null) {
                item.setLessonId(lessonIds.getOrDefault(item.getLessonId(), item.getLessonId()));
            }
            item.setCopiedFromId(oldId);
            item.setIsOriginal(false);
            item.setViewCount(0L);
            item.setLikeCount(0L);
            item.setRatingTotal(BigDecimal.ZERO);
            item.setRatingCount(0);
            item.onCreatingData(Actor.current(), clock);
            item.setTenantId(tenant);
            itemIds.put(oldId, contentItems.insert(item).getId());
        }
        for (LearningObjectiveEntity objective : objectives.ofCourse(source.getId())) {
            UUID oldId = objective.getId();
            objective.setCourseId(next.getId());
            objective.setChapterId(objective.getChapterId() == null ? null
                    : chapterIds.get(objective.getChapterId()));
            objective.setCopiedFromId(oldId);
            objective.onCreatingData(Actor.current(), clock);
            objective.setTenantId(tenant);
            objectives.insert(objective);
        }
        for (GradingCategoryEntity category : gradingCategories.ofCourse(source.getId())) {
            category.setCourseId(next.getId());
            category.onCreatingData(Actor.current(), clock);
            category.setTenantId(tenant);
            gradingCategories.insert(category);
        }
        for (ReleaseRuleEntity rule : releaseRules.ofCourse(source.getId())) {
            UUID oldRuleId = rule.getId();
            UUID newSource = itemIds.get(rule.getSourceContentItemId());
            if (newSource == null) {
                continue;
            }
            rule.setCourseId(next.getId());
            rule.setSourceContentItemId(newSource);
            rule.setCopiedFromId(oldRuleId);
            rule.onCreatingData(Actor.current(), clock);
            rule.setTenantId(tenant);
            UUID newRuleId = releaseRules.insert(rule).getId();
            for (ReleaseRuleConditionEntity condition : releaseRuleConditions.ofRules(List.of(oldRuleId))) {
                condition.setReleaseRuleId(newRuleId);
                condition.onCreatingData(Actor.current(), clock);
                condition.setTenantId(tenant);
                releaseRuleConditions.insert(condition);
            }
            for (ReleaseRuleTargetEntity target : releaseRuleTargets.ofRule(oldRuleId)) {
                UUID targetItem = target.getContentItemId() == null ? null
                        : itemIds.get(target.getContentItemId());
                UUID targetChapter = target.getChapterId() == null ? null : chapterIds.get(target.getChapterId());
                if (targetItem == null && targetChapter == null) {
                    continue;
                }
                target.setReleaseRuleId(newRuleId);
                target.setContentItemId(targetItem);
                target.setChapterId(targetChapter);
                target.onCreatingData(Actor.current(), clock);
                target.setTenantId(tenant);
                releaseRuleTargets.insert(target);
            }
        }
    }

    /**
     * lms.GetEvaluatorSlots: the course's default evaluator's free evaluation slots between two dates (at most 92
     * days apart) — their weekly availability expanded day by day, minus their unavailable range and the slots
     * already booked. A course with no default evaluator has none.
     */
    public Page<EvaluatorSlot> getEvaluatorSlots(UUID courseId, LocalDate fromDate, LocalDate toDate,
            Integer pageNumber, Integer pageSize) {
        if (toDate.isBefore(fromDate) || fromDate.plusDays(MAX_SLOT_DAYS).isBefore(toDate)) {
            throw new ValidationFailedException(List.of(new FieldError("toDate",
                    "must lie within 92 days of fromDate", "Range")));
        }
        DataScope scope = DataScope.current(PERMISSION_EVALUATOR_SLOTS);
        return doInReadTransaction(() -> {
            LmsCourseEntity course = loadVisible(scope, courseId);
            List<EvaluatorSlot> slots = course.getDefaultEvaluatorId() == null ? List.of()
                    : freeSlots(course.getDefaultEvaluatorId(), fromDate, toDate);
            int size = pageSize == null ? 50 : pageSize;
            int page = pageNumber == null ? 1 : pageNumber;
            int from = Math.min((page - 1) * size, slots.size());
            return new Page<>(slots.subList(from, Math.min(from + size, slots.size())), page, size, null);
        });
    }

    /** The evaluator's availability expanded over the dates, without booked or unavailable days. */
    private List<EvaluatorSlot> freeSlots(UUID evaluatorId, LocalDate fromDate, LocalDate toDate) {
        EvaluatorEntity evaluator = evaluators.findById(evaluatorId).orElse(null);
        if (evaluator == null) {
            return List.of();
        }
        Map<String, List<EvaluatorAvailabilityEntity>> byWeekday = new LinkedHashMap<>();
        for (EvaluatorAvailabilityEntity window : evaluatorAvailability.ofEvaluator(evaluatorId)) {
            byWeekday.computeIfAbsent(window.getWeekday(), k -> new ArrayList<>()).add(window);
        }
        Set<String> booked = certificateRequests.booked(evaluator.getUserId(), fromDate, toDate).stream()
                .map(b -> b.date() + "T" + b.start())
                .collect(Collectors.toSet());
        List<EvaluatorSlot> slots = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            if (evaluator.getUnavailableFrom() != null && evaluator.getUnavailableTo() != null
                    && !date.isBefore(evaluator.getUnavailableFrom()) && !date.isAfter(evaluator.getUnavailableTo())) {
                continue;
            }
            String weekday = date.getDayOfWeek().name().toLowerCase(Locale.ROOT);
            for (EvaluatorAvailabilityEntity window : byWeekday.getOrDefault(weekday, List.of())) {
                if (!booked.contains(date + "T" + window.getStartTime())) {
                    slots.add(new EvaluatorSlot(evaluator.getUserId(), date,
                            WireValues.time(window.getStartTime()), WireValues.time(window.getEndTime())));
                }
            }
        }
        return slots;
    }

    /**
     * lms.IssueMyCertificate: issues the caller their completion certificate once the course is complete — the
     * course must have certification enabled, a paid certificate must have been bought, and a certificate issued
     * before is simply returned.
     */
    public Certificate issueMyCertificate(UUID courseId) {
        UUID user = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            EnrollmentEntity enrollment = enrollmentRepository.findOwn(courseId, user)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, courseId));
            LmsCourseEntity course = repository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, courseId));
            Optional<CertificateEntity> issued = certificates.findOwnForCourse(user, courseId);
            if (issued.isPresent()) {
                return certificateMapper.toDto(issued.get());
            }
            if (!Boolean.TRUE.equals(course.getCertificationEnabled())) {
                throw new ConflictException("CERTIFICATION_DISABLED", "The course issues no certificates.");
            }
            if (enrollment.getProgressPercent() == null
                    || enrollment.getProgressPercent().compareTo(BigDecimal.valueOf(100)) < 0) {
                throw new ConflictException("COURSE_NOT_COMPLETE", "Complete the course first.");
            }
            if (Boolean.TRUE.equals(course.getPaidCertificate())
                    && !Boolean.TRUE.equals(enrollment.getPurchasedCertificate())) {
                throw new ConflictException("PAYMENT_REQUIRED", "Buy the certificate first.");
            }
            LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
            CertificateEntity entity = CertificateEntity.builder().userId(user).courseId(courseId)
                    .templateCode("course_completion").issueDate(today).isPublished(true).build();
            entity.onCreatingData(Actor.current(), clock);
            entity.setTenantId(tenant);
            entity.setCertificateNumber(("C-" + today.getYear() + "-" + entity.getId()).substring(0, 20));
            CertificateEntity stored = certificates.insert(entity);
            audit.recordCreate(CertificateEntity.TABLE, stored.getId());
            outbox.publish(CertificateEntity.TABLE + LmsEvents.CREATED, CertificateEntity.TABLE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return certificateMapper.toDto(stored);
        });
    }

    /**
     * lms.GetTeacherDashboard: the caller's cohorts, how many submissions wait for a grade, their unread forum
     * posts, the next due items and sessions, and the latest student alerts.
     */
    public TeacherDashboard getTeacherDashboard() {
        UUID user = AuthContext.require().userId();
        return doInReadTransaction(() -> {
            List<UUID> cohortIds = activity.teachingCohorts(user);
            int unread = activity.unreadPosts(user, cohortIds).stream()
                    .mapToInt(LearningActivityRepository.CohortUnread::posts).sum();
            TeacherDashboard dashboard = new TeacherDashboard(activity.toGrade(cohortIds), unread);
            dashboard.setCohorts(cohortIds.stream().limit(DASHBOARD_COHORTS).map(cohorts::findById)
                    .flatMap(Optional::stream).map(cohortMapper::toDto).toList());
            OffsetDateTime now = now();
            dashboard.setUpcomingDue(activity.upcomingDue(cohortIds, now, DASHBOARD_ITEMS).stream()
                    .map(u -> calendarItem(CalendarItem.ItemTypeEnum.DUE_DATE, u)).toList());
            dashboard.setUpcomingSessions(activity.upcomingSessions(cohortIds, now, DASHBOARD_ITEMS).stream()
                    .map(u -> calendarItem(CalendarItem.ItemTypeEnum.COHORT_SESSION, u)).toList());
            dashboard.setAlerts(studentAlerts.ofCohorts(cohortIds, DASHBOARD_ITEMS).stream()
                    .map(studentAlertMapper::toDto).toList());
            return dashboard;
        });
    }

    private static CalendarItem calendarItem(CalendarItem.ItemTypeEnum type, LearningActivityRepository.Upcoming u) {
        CalendarItem item = new CalendarItem(type, u.id(), u.title(), u.startAt());
        item.setEndAt(u.endAt());
        item.setCohortId(u.cohortId());
        item.setContentItemId(u.contentItemId());
        return item;
    }


    /**
     * lms.GetCourseSyllabus: generates the syllabus (HTML; other formats are not generated here) from the outline,
     * the objectives and the grading categories, stores it, and answers its signed short-lived address.
     */
    public DownloadLink getCourseSyllabus(UUID courseId, String format) {
        if (format != null && !"html".equalsIgnoreCase(format)) {
            throw new ValidationFailedException(List.of(new FieldError("format",
                    "only html is generated by this release", "Pattern")));
        }
        DataScope scope = DataScope.current(PERMISSION_SYLLABUS);
        String html = doInReadTransaction(() -> syllabusHtml(loadVisible(scope, courseId)));
        UUID fileId = files.write("syllabus-" + courseId + ".html", "text/html; charset=utf-8",
                html.getBytes(StandardCharsets.UTF_8), AGGREGATE, courseId);
        return files.link(fileId, "courseId");
    }

    /** The syllabus document: title, introduction, objectives, grading categories and the chapter tree. */
    private String syllabusHtml(LmsCourseEntity course) {
        StringBuilder html = new StringBuilder("<!doctype html><html><head><meta charset=\"utf-8\"><title>")
                .append(escape(course.getTitle())).append("</title></head><body><h1>")
                .append(escape(course.getTitle())).append("</h1>");
        if (course.getShortIntroduction() != null) {
            html.append("<p>").append(escape(course.getShortIntroduction())).append("</p>");
        }
        List<LearningObjectiveEntity> courseObjectives = objectives.ofCourse(course.getId());
        if (!courseObjectives.isEmpty()) {
            html.append("<h2>Objectives</h2><ul>");
            courseObjectives.forEach(o -> html.append("<li>").append(escape(o.getObjective())).append("</li>"));
            html.append("</ul>");
        }
        List<GradingCategoryEntity> categories = gradingCategories.ofCourse(course.getId());
        if (!categories.isEmpty()) {
            html.append("<h2>Grading</h2><ul>");
            categories.forEach(c -> html.append("<li>").append(escape(c.getName()))
                    .append(c.getWeightPercent() == null ? "" : " — " + c.getWeightPercent() + "%")
                    .append("</li>"));
            html.append("</ul>");
        }
        html.append("<h2>Outline</h2><ol>");
        List<ChapterEntity> chapters = inReadingOrder(chapterRepository.templateOutline(course.getId()));
        List<UUID> chapterIds = chapters.stream().map(ChapterEntity::getId).toList();
        Map<UUID, List<LessonEntity>> lessons = new LinkedHashMap<>();
        for (LessonEntity lesson : lessonRepository.ofChapters(chapterIds)) {
            lessons.computeIfAbsent(lesson.getChapterId(), k -> new ArrayList<>()).add(lesson);
        }
        for (ChapterEntity chapter : chapters) {
            html.append("<li>").append(escape(chapter.getTitle()));
            List<LessonEntity> own = lessons.getOrDefault(chapter.getId(), List.of());
            if (!own.isEmpty()) {
                html.append("<ul>");
                own.forEach(l -> html.append("<li>").append(escape(l.getTitle())).append("</li>"));
                html.append("</ul>");
            }
            html.append("</li>");
        }
        return html.append("</ol></body></html>").toString();
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---- delete and its dependencies ----

    /**
     * lms.CheckCourseDependencies: what keeps the course from being deleted — every table of this module whose live
     * rows still reference it (cohorts, enrolments, newer versions, chapters, its catalogue banners…), and a published
     * course, which must be unpublished first.
     */
    public Dependencies checkCourseDependencies(UUID id) {
        return doInReadTransaction(() -> {
            LmsCourseEntity entity = loadVisible(DataScope.current(PERMISSION_CHECK_DEPENDENCIES), id);
            Map<String, Boolean> blocking = blockers(entity);
            return new Dependencies(blocking.isEmpty()).blocking(blocking);
        });
    }

    /**
     * Soft delete under If-Match: refused (409) while the course is published or any live row of this module still
     * references it — {@link #checkCourseDependencies} names them.
     */
    public void deleteLmsCourse(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LmsCourseEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            Map<String, Boolean> blocking = blockers(entity);
            if (!blocking.isEmpty()) {
                throw new InvalidStateException("The course is still referenced by " + blocking.keySet().iterator()
                        .next() + " rows; see its dependencies and remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The reasons the course cannot be deleted, in a stable order; empty when it can. */
    private Map<String, Boolean> blockers(LmsCourseEntity entity) {
        UUID id = entity.getId();
        Map<String, Boolean> blocking = new LinkedHashMap<>();
        if (PUBLISHED.equals(entity.getLifecycleStatus())) {
            blocking.put("published", true);
        }
        if (repository.referencedByAssignmentCourseId(id)) {
            blocking.put("lms.assignment", true);
        }
        if (repository.referencedByAssignmentSubmissionCourseId(id)) {
            blocking.put("lms.assignment_submission", true);
        }
        if (repository.referencedByCatalogBannerCourseId(id)) {
            blocking.put("lms.catalog_banner", true);
        }
        if (repository.referencedByCertificateCourseId(id)) {
            blocking.put("lms.certificate", true);
        }
        if (repository.referencedByCertificateEvaluationCourseId(id)) {
            blocking.put("lms.certificate_evaluation", true);
        }
        if (repository.referencedByCertificateRequestCourseId(id)) {
            blocking.put("lms.certificate_request", true);
        }
        if (repository.referencedByChapterCourseId(id)) {
            blocking.put("lms.chapter", true);
        }
        if (repository.referencedByCohortCourseCourseId(id)) {
            blocking.put("lms.cohort_course", true);
        }
        if (repository.referencedByContentItemCourseId(id)) {
            blocking.put("lms.content_item", true);
        }
        if (repository.referencedByCoursePreviousVersionId(id)) {
            blocking.put("lms.course", true);
        }
        if (repository.referencedByCourseRootCourseId(id)) {
            blocking.put("lms.course", true);
        }
        if (repository.referencedByCourseAuthorProfileCourseId(id)) {
            blocking.put("lms.course_author_profile", true);
        }
        if (repository.referencedByCourseInterestCourseId(id)) {
            blocking.put("lms.course_interest", true);
        }
        if (repository.referencedByCourseMentorCourseId(id)) {
            blocking.put("lms.course_mentor", true);
        }
        if (repository.referencedByCoursePreviewCourseId(id)) {
            blocking.put("lms.course_preview", true);
        }
        if (repository.referencedByCourseReviewCourseId(id)) {
            blocking.put("lms.course_review", true);
        }
        if (repository.referencedByCourseTeacherCourseId(id)) {
            blocking.put("lms.course_teacher", true);
        }
        if (repository.referencedByCourseTestimonialCourseId(id)) {
            blocking.put("lms.course_testimonial", true);
        }
        if (repository.referencedByDriveItemCourseId(id)) {
            blocking.put("lms.drive_item", true);
        }
        if (repository.referencedByEnrollmentCourseId(id)) {
            blocking.put("lms.enrollment", true);
        }
        if (repository.referencedByEnrollmentRequestCourseId(id)) {
            blocking.put("lms.enrollment_request", true);
        }
        if (repository.referencedByGradingCategoryCourseId(id)) {
            blocking.put("lms.grading_category", true);
        }
        if (repository.referencedByImportExportJobCourseId(id)) {
            blocking.put("lms.import_export_job", true);
        }
        if (repository.referencedByLearningObjectiveCourseId(id)) {
            blocking.put("lms.learning_objective", true);
        }
        if (repository.referencedByLearningProgramCourseCourseId(id)) {
            blocking.put("lms.learning_program_course", true);
        }
        if (repository.referencedByLessonProgressCourseId(id)) {
            blocking.put("lms.lesson_progress", true);
        }
        if (repository.referencedByQuizCourseId(id)) {
            blocking.put("lms.quiz", true);
        }
        if (repository.referencedByQuizSubmissionCourseId(id)) {
            blocking.put("lms.quiz_submission", true);
        }
        if (repository.referencedByRelatedCourseCourseId(id)) {
            blocking.put("lms.related_course", true);
        }
        if (repository.referencedByRelatedCourseRelatedCourseId(id)) {
            blocking.put("lms.related_course", true);
        }
        if (repository.referencedByRelatedMaterialRankCourseId(id)) {
            blocking.put("lms.related_material_rank", true);
        }
        if (repository.referencedByReleaseRuleCourseId(id)) {
            blocking.put("lms.release_rule", true);
        }
        if (repository.referencedByStudentAlertCourseId(id)) {
            blocking.put("lms.student_alert", true);
        }
        return blocking;
    }

    // ---- the course's guarded steps ----

    /** lms.SubmitCourseForReview: in_progress → under_review. */
    public LmsCourse submitCourseForReview(UUID id, String etag) {
        return courseStep(id, PERMISSION_SUBMIT_FOR_REVIEW, etag, "submit_for_review", null,
                e -> "in_progress".equals(e.getStatus()), LmsCourseRepository.CourseStep.SUBMIT_FOR_REVIEW,
                List.of("status"));
    }

    /** lms.ApproveCourse: under_review → approved. */
    public LmsCourse approveCourse(UUID id, String etag) {
        return courseStep(id, PERMISSION_APPROVE, etag, "approve", null, e -> "under_review".equals(e.getStatus()),
                LmsCourseRepository.CourseStep.APPROVE, List.of("status"));
    }

    /** lms.PublishCourse: draft → published, once the review approved the course. */
    public LmsCourse publishCourse(UUID id, String etag) {
        return courseStep(id, PERMISSION_PUBLISH, etag, "publish", null,
                e -> "draft".equals(e.getLifecycleStatus()) && "approved".equals(e.getStatus()),
                LmsCourseRepository.CourseStep.PUBLISH, List.of("lifecycleStatus", "isPublished", "publishedOn"));
    }

    /** lms.UnpublishCourse: published → draft (hidden from the public; enrolled students keep their access). */
    public LmsCourse unpublishCourse(UUID id, String etag) {
        return courseStep(id, PERMISSION_UNPUBLISH, etag, "unpublish", null,
                e -> PUBLISHED.equals(e.getLifecycleStatus()), LmsCourseRepository.CourseStep.UNPUBLISH,
                List.of("lifecycleStatus", "isPublished"));
    }

    /** lms.DeactivateCourse: published → deactivated (no new cohorts or enrolments); the reason is kept. */
    public LmsCourse deactivateCourse(UUID id, String etag, String reason) {
        return courseStep(id, PERMISSION_DEACTIVATE, etag, "deactivate", reason,
                e -> PUBLISHED.equals(e.getLifecycleStatus()), LmsCourseRepository.CourseStep.DEACTIVATE,
                List.of("lifecycleStatus", "isPublished", "deactivatedOn"));
    }

    /** lms.ReactivateCourse: deactivated → published. */
    public LmsCourse reactivateCourse(UUID id, String etag) {
        return courseStep(id, PERMISSION_REACTIVATE, etag, "reactivate", null,
                e -> "deactivated".equals(e.getLifecycleStatus()), LmsCourseRepository.CourseStep.REACTIVATE,
                List.of("lifecycleStatus", "isPublished", "deactivatedOn"));
    }

    /** lms.PublishCatalogListing: the listing (draft or archived) goes into the catalogue of a published course. */
    public LmsCourse publishCatalogListing(UUID id, String etag) {
        return courseStep(id, PERMISSION_PUBLISH_CATALOG_LISTING, etag, "publish_catalog_listing", null,
                e -> !PUBLISHED.equals(e.getCatalogStatus()) && PUBLISHED.equals(e.getLifecycleStatus()),
                LmsCourseRepository.CourseStep.PUBLISH_CATALOG_LISTING, List.of("catalogStatus", "isInCatalog"));
    }

    /** lms.HideCatalogListing: a published listing is archived and leaves the catalogue. */
    public LmsCourse hideCatalogListing(UUID id, String etag) {
        return courseStep(id, PERMISSION_HIDE_CATALOG_LISTING, etag, "hide_catalog_listing", null,
                e -> PUBLISHED.equals(e.getCatalogStatus()), LmsCourseRepository.CourseStep.HIDE_CATALOG_LISTING,
                List.of("catalogStatus", "isInCatalog"));
    }

    /**
     * Template method of every guarded course step: 404 when invisible, 403 when not writable, 412 on an etag
     * mismatch, 409 when the guard fails on the row as read; then the guarded UPDATE, audit and
     * {@code lms.course.<step>} event in one transaction.
     */
    private LmsCourse courseStep(UUID id, String permission, String etag, String step, String reason,
            Predicate<LmsCourseEntity> guard, LmsCourseRepository.CourseStep write, List<String> changed) {
        DataScope scope = DataScope.current(permission);
        OptionalInt expected = Etags.bodyVersion(etag);
        return doInWriteTransaction(() -> {
            LmsCourseEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!guard.test(entity)) {
                throw new InvalidStateException("The course cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.step(write, id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, step, changed, reason);
            outbox.publish(AGGREGATE + "." + step, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LmsCourseEntity loadVisible(DataScope scope, UUID id) {
        LmsCourseEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(LmsCourseEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(LmsCourseEntity entity, LmsCourseUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("shortIntroduction", changes.getShortIntroduction(), entity::setShortIntroduction);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("imageFileId", changes.getImageFileId(), entity::setImageFileId);
        patch.apply("videoUrl", changes.getVideoUrl(), entity::setVideoUrl);
        patch.apply("categoryId", changes.getCategoryId(), entity::setCategoryId);
        patch.apply("publishedOn", changes.getPublishedOn(), entity::setPublishedOn);
        patch.apply("isUpcoming", changes.getIsUpcoming(), entity::setIsUpcoming);
        patch.apply("isFeatured", changes.getIsFeatured(), entity::setIsFeatured);
        patch.apply("disableSelfLearning", changes.getDisableSelfLearning(), entity::setDisableSelfLearning);
        patch.apply("enforceLessonOrder", changes.getEnforceLessonOrder(), entity::setEnforceLessonOrder);
        patch.apply("certificationEnabled", changes.getCertificationEnabled(), entity::setCertificationEnabled);
        patch.apply("paidCertificate", changes.getPaidCertificate(), entity::setPaidCertificate);
        patch.apply("defaultEvaluatorId", changes.getDefaultEvaluatorId(), entity::setDefaultEvaluatorId);
        patch.apply("isPaid", changes.getIsPaid(), entity::setIsPaid);
        patch.apply("price", changes.getPrice(), v -> entity.setPrice(WireValues.decimal(v)));
        patch.apply("currencyId", changes.getCurrencyId(), entity::setCurrencyId);
        patch.apply("priceUsd", changes.getPriceUsd(), v -> entity.setPriceUsd(WireValues.decimal(v)));
        patch.apply("cardGradient", changes.getCardGradient(), v -> entity.setCardGradient(v == null ? null : v.getValue()));
        patch.apply("timezone", changes.getTimezone(), entity::setTimezone);
        patch.apply("publishNotificationSent", changes.getPublishNotificationSent(), entity::setPublishNotificationSent);
        patch.apply("enrollmentCount", changes.getEnrollmentCount(), entity::setEnrollmentCount);
        patch.apply("lessonCount", changes.getLessonCount(), entity::setLessonCount);
        patch.apply("averageRating", changes.getAverageRating(), v -> entity.setAverageRating(WireValues.decimal(v)));
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("stateStandardCode", changes.getStateStandardCode(), entity::setStateStandardCode);
        patch.apply("credits", changes.getCredits(), v -> entity.setCredits(WireValues.decimal(v)));
        patch.apply("prerequisites", changes.getPrerequisites(), entity::setPrerequisites);
        patch.apply("corequisites", changes.getCorequisites(), entity::setCorequisites);
        patch.apply("effectiveFrom", changes.getEffectiveFrom(), entity::setEffectiveFrom);
        patch.apply("lifecycleStatus", changes.getLifecycleStatus(),
                                                v -> entity.setLifecycleStatus(v == null ? null : v.getValue()));
        patch.apply("deactivatedOn", changes.getDeactivatedOn(), entity::setDeactivatedOn);
        patch.apply("versionMajor", changes.getVersionMajor(), entity::setVersionMajor);
        patch.apply("versionMinor", changes.getVersionMinor(), entity::setVersionMinor);
        patch.apply("previousVersionId", changes.getPreviousVersionId(), entity::setPreviousVersionId);
        patch.apply("rootCourseId", changes.getRootCourseId(), entity::setRootCourseId);
        patch.apply("authorComments", changes.getAuthorComments(), entity::setAuthorComments);
        patch.apply("externalRef", changes.getExternalRef(), entity::setExternalRef);
        patch.apply("nominalDuration", changes.getNominalDuration(), entity::setNominalDuration);
        patch.apply("deliveryModality", changes.getDeliveryModality(),
                                                v -> entity.setDeliveryModality(v == null ? null : v.getValue()));
        patch.apply("isShared", changes.getIsShared(), entity::setIsShared);
        patch.apply("contentUpdatedAt", changes.getContentUpdatedAt(), entity::setContentUpdatedAt);
        patch.apply("gradeDisplay", changes.getGradeDisplay(), v -> entity.setGradeDisplay(v == null ? null : v.getValue()));
        patch.apply("gradingSchemeId", changes.getGradingSchemeId(), entity::setGradingSchemeId);
        patch.apply("isInCatalog", changes.getIsInCatalog(), entity::setIsInCatalog);
        patch.apply("catalogDescription", changes.getCatalogDescription(), entity::setCatalogDescription);
        patch.apply("catalogVisibleFrom", changes.getCatalogVisibleFrom(), entity::setCatalogVisibleFrom);
        patch.apply("catalogVisibleUntil", changes.getCatalogVisibleUntil(), entity::setCatalogVisibleUntil);
        patch.apply("outline", changes.getOutline(), entity::setOutline);
        patch.apply("workloadHours", changes.getWorkloadHours(), entity::setWorkloadHours);
        patch.apply("catalogStatus", changes.getCatalogStatus(), v -> entity.setCatalogStatus(v == null ? null : v.getValue()));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LmsCourseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsUpcoming() == null) {
            errors.add(new FieldError("isUpcoming", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsFeatured() == null) {
            errors.add(new FieldError("isFeatured", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDisableSelfLearning() == null) {
            errors.add(new FieldError("disableSelfLearning", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEnforceLessonOrder() == null) {
            errors.add(new FieldError("enforceLessonOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCertificationEnabled() == null) {
            errors.add(new FieldError("certificationEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPaidCertificate() == null) {
            errors.add(new FieldError("paidCertificate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPaid() == null) {
            errors.add(new FieldError("isPaid", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("price", WireValues.decimal(entity.getPrice()), 14, 2, errors);
        WireValues.checkDecimal("priceUsd", WireValues.decimal(entity.getPriceUsd()), 14, 2, errors);
        if (!creating && entity.getPublishNotificationSent() == null) {
            errors.add(new FieldError("publishNotificationSent", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEnrollmentCount() == null) {
            errors.add(new FieldError("enrollmentCount", "must not be null", "NotNull"));
        }
        if (!creating && entity.getLessonCount() == null) {
            errors.add(new FieldError("lessonCount", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("averageRating", WireValues.decimal(entity.getAverageRating()), 3, 2, errors);
        WireValues.checkDecimal("credits", WireValues.decimal(entity.getCredits()), 6, 2, errors);
        if (!creating && entity.getLifecycleStatus() == null) {
            errors.add(new FieldError("lifecycleStatus", "must not be null", "NotNull"));
        }
        if (!creating && entity.getVersionMajor() == null) {
            errors.add(new FieldError("versionMajor", "must not be null", "NotNull"));
        }
        if (!creating && entity.getVersionMinor() == null) {
            errors.add(new FieldError("versionMinor", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsShared() == null) {
            errors.add(new FieldError("isShared", "must not be null", "NotNull"));
        }
        if (!creating && entity.getGradeDisplay() == null) {
            errors.add(new FieldError("gradeDisplay", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsInCatalog() == null) {
            errors.add(new FieldError("isInCatalog", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCatalogStatus() == null) {
            errors.add(new FieldError("catalogStatus", "must not be null", "NotNull"));
        }
        if (entity.getCategoryId() != null && !repository.existsCategoryId(entity.getCategoryId())) {
            errors.add(new FieldError("categoryId", "category does not exist", "NOT_FOUND"));
        }
        if (entity.getDefaultEvaluatorId() != null
                && !repository.existsDefaultEvaluatorId(entity.getDefaultEvaluatorId())) {
            errors.add(new FieldError("defaultEvaluatorId", "evaluator does not exist", "NOT_FOUND"));
        }
        if (entity.getPreviousVersionId() != null
                && !repository.existsPreviousVersionId(entity.getPreviousVersionId())) {
            errors.add(new FieldError("previousVersionId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getRootCourseId() != null && !repository.existsRootCourseId(entity.getRootCourseId())) {
            errors.add(new FieldError("rootCourseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LmsCourseEntity entity) {

    }

    // ---- CatalogBanner: a record of the LmsCourse aggregate ----

    public CatalogBanner getCatalogBanner(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return catalogBannerMapper.toDto(loadVisibleCatalogBanner(DataScope.current(CATALOG_BANNER_PERMISSION_GET),
                    courseId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CatalogBanner createCatalogBanner(UUID courseId, CatalogBanner request, boolean validateOnly) {
        validator.validate(request);
        CatalogBannerEntity entity = catalogBannerMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CATALOG_BANNER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!catalogBannerRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCatalogBanner(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCatalogBanner(entity, true);
            scope.requireWritable(institutionOfCatalogBanner(entity));
            assertUniqueCatalogBanner(entity);
            if (validateOnly) {
                return catalogBannerMapper.toDto(entity);
            }
            CatalogBannerEntity stored = catalogBannerRepository.insert(entity);
            audit.recordCreate(CATALOG_BANNER_AGGREGATE, stored.getId());
            outbox.publish(CATALOG_BANNER_AGGREGATE + LmsEvents.CREATED, CATALOG_BANNER_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return catalogBannerMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CatalogBanner updateCatalogBanner(UUID courseId, UUID id, String ifMatch, CatalogBannerUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CATALOG_BANNER_PERMISSION_UPDATE);
            CatalogBannerEntity entity = loadVisibleCatalogBanner(scope, courseId, id);
            scope.requireWritable(institutionOfCatalogBanner(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCatalogBanner(entity, changes);
            validator.validate(catalogBannerMapper.toDto(entity));
            assertValidCatalogBanner(entity, false);
            scope.requireWritable(institutionOfCatalogBanner(entity));
            assertUniqueCatalogBanner(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return catalogBannerMapper.toDto(entity);
            }
            if (catalogBannerRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CATALOG_BANNER_AGGREGATE, id, fields);
            outbox.publish(CATALOG_BANNER_AGGREGATE + LmsEvents.UPDATED, CATALOG_BANNER_AGGREGATE, id, LmsEvents.payload(id));
            return catalogBannerMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCatalogBanner(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CATALOG_BANNER_PERMISSION_DELETE);
            CatalogBannerEntity entity = loadVisibleCatalogBanner(scope, courseId, id);
            scope.requireWritable(institutionOfCatalogBanner(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (catalogBannerRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CATALOG_BANNER_AGGREGATE, id);
            outbox.publish(CATALOG_BANNER_AGGREGATE + LmsEvents.DELETED, CATALOG_BANNER_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CatalogBannerEntity loadVisibleCatalogBanner(DataScope scope, UUID courseId, UUID id) {
        CatalogBannerEntity entity = catalogBannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CATALOG_BANNER_AGGREGATE, id));
        if (!scope.canRead(institutionOfCatalogBanner(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(CATALOG_BANNER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCatalogBanner(CatalogBannerEntity entity) {
        return catalogBannerRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCatalogBanner(CatalogBannerEntity entity, CatalogBannerUpdate changes) {
        Patches patch = new Patches();
        patch.apply("placement", changes.getPlacement(), v -> entity.setPlacement(v == null ? null : v.getValue()));
        patch.apply("imageFileId", changes.getImageFileId(), entity::setImageFileId);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCatalogBanner(CatalogBannerEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getPlacement() == null) {
            errors.add(new FieldError("placement", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !catalogBannerRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCatalogBanner(CatalogBannerEntity entity) {

    }

    // ---- Chapter: a record of the LmsCourse aggregate ----

    public Chapter getChapter(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return chapterMapper.toDto(loadVisibleChapter(DataScope.current(CHAPTER_PERMISSION_GET), courseId, id));
        });
    }

    public Page<Chapter> listChapters(UUID courseId, Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ChapterRepository.SORT_FIELDS, ChapterRepository.DEFAULT_ORDER,
                ChapterRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ChapterRepository.FILTER_FIELDS)
                .and(ChapterRepository.ofParent(courseId))
                        .and(ChapterRepository.scope(DataScope.current(CHAPTER_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            Page<ChapterEntity> rows = chapterRepository.list(page, where);
            return rows.map(chapterMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Chapter createChapter(UUID courseId, Chapter request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("dueTime", request.getDueTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        ChapterEntity entity = chapterMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CHAPTER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!chapterRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfChapter(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidChapter(entity, true);
            scope.requireWritable(institutionOfChapter(entity));
            assertUniqueChapter(entity);
            if (validateOnly) {
                return chapterMapper.toDto(entity);
            }
            ChapterEntity stored = chapterRepository.insert(entity);
            audit.recordCreate(CHAPTER_AGGREGATE, stored.getId());
            outbox.publish(CHAPTER_AGGREGATE + LmsEvents.CREATED, CHAPTER_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return chapterMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Chapter updateChapter(UUID courseId, UUID id, String ifMatch, ChapterUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", Patches.value(changes.getStartTime()), wire);
        WireValues.checkTime("dueTime", Patches.value(changes.getDueTime()), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CHAPTER_PERMISSION_UPDATE);
            ChapterEntity entity = loadVisibleChapter(scope, courseId, id);
            scope.requireWritable(institutionOfChapter(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyChapter(entity, changes);
            validator.validate(chapterMapper.toDto(entity));
            assertValidChapter(entity, false);
            scope.requireWritable(institutionOfChapter(entity));
            assertUniqueChapter(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return chapterMapper.toDto(entity);
            }
            if (chapterRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CHAPTER_AGGREGATE, id, fields);
            outbox.publish(CHAPTER_AGGREGATE + LmsEvents.UPDATED, CHAPTER_AGGREGATE, id, LmsEvents.payload(id));
            return chapterMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteChapter(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CHAPTER_PERMISSION_DELETE);
            ChapterEntity entity = loadVisibleChapter(scope, courseId, id);
            scope.requireWritable(institutionOfChapter(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (chapterRepository.referencedByChapterCopiedFromId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.chapter rows; remove those first.");
            }
            if (chapterRepository.referencedByChapterParentChapterId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.chapter rows; remove those first.");
            }
            if (chapterRepository.referencedByImportExportJobTargetChapterId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.import_export_job rows; remove those first.");
            }
            if (chapterRepository.referencedByLearningObjectiveChapterId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.learning_objective rows; remove those first.");
            }
            if (chapterRepository.referencedByLessonChapterId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.lesson rows; remove those first.");
            }
            if (chapterRepository.referencedByLessonProgressChapterId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.lesson_progress rows; remove those first.");
            }
            if (chapterRepository.referencedByReleaseGrantChapterId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.release_grant rows; remove those first.");
            }
            if (chapterRepository.referencedByReleaseRuleTargetChapterId(id)) {
                throw new InvalidStateException(
                        "The chapter is still referenced by lms.release_rule_target rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (chapterRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CHAPTER_AGGREGATE, id);
            outbox.publish(CHAPTER_AGGREGATE + LmsEvents.DELETED, CHAPTER_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ChapterEntity loadVisibleChapter(DataScope scope, UUID courseId, UUID id) {
        ChapterEntity entity = chapterRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(CHAPTER_AGGREGATE, id));
        if (!scope.canRead(institutionOfChapter(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(CHAPTER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfChapter(ChapterEntity entity) {
        return chapterRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyChapter(ChapterEntity entity, ChapterUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("isScormPackage", changes.getIsScormPackage(), entity::setIsScormPackage);
        patch.apply("scormPackageFileId", changes.getScormPackageFileId(), entity::setScormPackageFileId);
        patch.apply("scormPackagePath", changes.getScormPackagePath(), entity::setScormPackagePath);
        patch.apply("scormManifestFile", changes.getScormManifestFile(), entity::setScormManifestFile);
        patch.apply("scormLaunchFile", changes.getScormLaunchFile(), entity::setScormLaunchFile);
        patch.apply("cohortId", changes.getCohortId(), entity::setCohortId);
        patch.apply("parentChapterId", changes.getParentChapterId(), entity::setParentChapterId);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("chapterNumber", changes.getChapterNumber(), entity::setChapterNumber);
        patch.apply("startDay", changes.getStartDay(), entity::setStartDay);
        patch.apply("startTime", changes.getStartTime(), v -> entity.setStartTime(WireValues.time(v)));
        patch.apply("dueDay", changes.getDueDay(), entity::setDueDay);
        patch.apply("dueTime", changes.getDueTime(), v -> entity.setDueTime(WireValues.time(v)));
        patch.apply("startsAt", changes.getStartsAt(), entity::setStartsAt);
        patch.apply("endsAt", changes.getEndsAt(), entity::setEndsAt);
        patch.apply("allowDelete", changes.getAllowDelete(), entity::setAllowDelete);
        patch.apply("allowEditDetails", changes.getAllowEditDetails(), entity::setAllowEditDetails);
        patch.apply("allowEditDates", changes.getAllowEditDates(), entity::setAllowEditDates);
        patch.apply("copiedFromId", changes.getCopiedFromId(), entity::setCopiedFromId);
        patch.apply("isOriginal", changes.getIsOriginal(), entity::setIsOriginal);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidChapter(ChapterEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsScormPackage() == null) {
            errors.add(new FieldError("isScormPackage", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowDelete() == null) {
            errors.add(new FieldError("allowDelete", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowEditDetails() == null) {
            errors.add(new FieldError("allowEditDetails", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowEditDates() == null) {
            errors.add(new FieldError("allowEditDates", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsOriginal() == null) {
            errors.add(new FieldError("isOriginal", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !chapterRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCopiedFromId() != null && !chapterRepository.existsCopiedFromId(entity.getCopiedFromId())) {
            errors.add(new FieldError("copiedFromId", "chapter does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !chapterRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getParentChapterId() != null && !chapterRepository.existsParentChapterId(entity.getParentChapterId())) {
            errors.add(new FieldError("parentChapterId", "chapter does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueChapter(ChapterEntity entity) {

    }

    // ---- CourseAuthorProfile: a record of the LmsCourse aggregate ----

    public CourseAuthorProfile getCourseAuthorProfile(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return courseAuthorProfileMapper.toDto(loadVisibleCourseAuthorProfile(DataScope.current(
                    COURSE_AUTHOR_PROFILE_PERMISSION_GET), courseId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CourseAuthorProfile createCourseAuthorProfile(UUID courseId, CourseAuthorProfile request,
            boolean validateOnly) {
        validator.validate(request);
        CourseAuthorProfileEntity entity = courseAuthorProfileMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_AUTHOR_PROFILE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!courseAuthorProfileRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCourseAuthorProfile(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCourseAuthorProfile(entity, true);
            scope.requireWritable(institutionOfCourseAuthorProfile(entity));
            assertUniqueCourseAuthorProfile(entity);
            if (validateOnly) {
                return courseAuthorProfileMapper.toDto(entity);
            }
            CourseAuthorProfileEntity stored = courseAuthorProfileRepository.insert(entity);
            audit.recordCreate(COURSE_AUTHOR_PROFILE_AGGREGATE, stored.getId());
            outbox.publish(COURSE_AUTHOR_PROFILE_AGGREGATE + LmsEvents.CREATED, COURSE_AUTHOR_PROFILE_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return courseAuthorProfileMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CourseAuthorProfile updateCourseAuthorProfile(UUID courseId, UUID id, String ifMatch,
            CourseAuthorProfileUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_AUTHOR_PROFILE_PERMISSION_UPDATE);
            CourseAuthorProfileEntity entity = loadVisibleCourseAuthorProfile(scope, courseId, id);
            scope.requireWritable(institutionOfCourseAuthorProfile(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCourseAuthorProfile(entity, changes);
            validator.validate(courseAuthorProfileMapper.toDto(entity));
            assertValidCourseAuthorProfile(entity, false);
            scope.requireWritable(institutionOfCourseAuthorProfile(entity));
            assertUniqueCourseAuthorProfile(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return courseAuthorProfileMapper.toDto(entity);
            }
            if (courseAuthorProfileRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_AUTHOR_PROFILE_AGGREGATE, id, fields);
            outbox.publish(COURSE_AUTHOR_PROFILE_AGGREGATE + LmsEvents.UPDATED, COURSE_AUTHOR_PROFILE_AGGREGATE, id,
                    LmsEvents.payload(id));
            return courseAuthorProfileMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourseAuthorProfile(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_AUTHOR_PROFILE_PERMISSION_DELETE);
            CourseAuthorProfileEntity entity = loadVisibleCourseAuthorProfile(scope, courseId, id);
            scope.requireWritable(institutionOfCourseAuthorProfile(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (courseAuthorProfileRepository.referencedByRelatedCourseAuthorProfileId(id)) {
                throw new InvalidStateException(
                        "The course author profile is still referenced by lms.related_course rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (courseAuthorProfileRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_AUTHOR_PROFILE_AGGREGATE, id);
            outbox.publish(COURSE_AUTHOR_PROFILE_AGGREGATE + LmsEvents.DELETED, COURSE_AUTHOR_PROFILE_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CourseAuthorProfileEntity loadVisibleCourseAuthorProfile(DataScope scope, UUID courseId, UUID id) {
        CourseAuthorProfileEntity entity = courseAuthorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_AUTHOR_PROFILE_AGGREGATE, id));
        if (!scope.canRead(institutionOfCourseAuthorProfile(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_AUTHOR_PROFILE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCourseAuthorProfile(CourseAuthorProfileEntity entity) {
        return courseAuthorProfileRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCourseAuthorProfile(CourseAuthorProfileEntity entity, CourseAuthorProfileUpdate changes) {
        Patches patch = new Patches();
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("bio", changes.getBio(), entity::setBio);
        patch.apply("photoFileId", changes.getPhotoFileId(), entity::setPhotoFileId);
        patch.apply("qualification", changes.getQualification(), entity::setQualification);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCourseAuthorProfile(CourseAuthorProfileEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !courseAuthorProfileRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCourseAuthorProfile(CourseAuthorProfileEntity entity) {

    }

    // ---- CourseInterest: a record of the LmsCourse aggregate ----

    public CourseInterest getCourseInterest(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return courseInterestMapper.toDto(loadVisibleCourseInterest(DataScope.current(COURSE_INTEREST_PERMISSION_GET),
                    courseId, id));
        });
    }

    public Page<CourseInterest> listCourseInterests(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CourseInterestRepository.SORT_FIELDS, CourseInterestRepository.DEFAULT_ORDER,
                CourseInterestRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CourseInterestRepository.FILTER_FIELDS)
                .and(CourseInterestRepository.ofParent(courseId))
                        .and(CourseInterestRepository.scope(DataScope.current(COURSE_INTEREST_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            Page<CourseInterestEntity> rows = courseInterestRepository.list(page, where);
            return rows.map(courseInterestMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CourseInterest createCourseInterest(UUID courseId, CourseInterest request, boolean validateOnly) {
        validator.validate(request);
        CourseInterestEntity entity = courseInterestMapper.toEntity(request);
        entity.setUserId(OwnRows.caller());
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_INTEREST_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!courseInterestRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCourseInterest(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCourseInterest(entity, true);
            scope.requireWritable(institutionOfCourseInterest(entity));
            assertUniqueCourseInterest(entity);
            if (validateOnly) {
                return courseInterestMapper.toDto(entity);
            }
            CourseInterestEntity stored = courseInterestRepository.insert(entity);
            audit.recordCreate(COURSE_INTEREST_AGGREGATE, stored.getId());
            outbox.publish(COURSE_INTEREST_AGGREGATE + LmsEvents.CREATED, COURSE_INTEREST_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return courseInterestMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CourseInterest updateCourseInterest(UUID courseId, UUID id, String ifMatch, CourseInterestUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_INTEREST_PERMISSION_UPDATE);
            CourseInterestEntity entity = loadVisibleCourseInterest(scope, courseId, id);
            OwnRows.require(entity.getUserId(), COURSE_INTEREST_AGGREGATE, id);
            scope.requireWritable(institutionOfCourseInterest(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCourseInterest(entity, changes);
            validator.validate(courseInterestMapper.toDto(entity));
            assertValidCourseInterest(entity, false);
            scope.requireWritable(institutionOfCourseInterest(entity));
            assertUniqueCourseInterest(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return courseInterestMapper.toDto(entity);
            }
            if (courseInterestRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_INTEREST_AGGREGATE, id, fields);
            outbox.publish(COURSE_INTEREST_AGGREGATE + LmsEvents.UPDATED, COURSE_INTEREST_AGGREGATE, id, LmsEvents.payload(id));
            return courseInterestMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourseInterest(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_INTEREST_PERMISSION_DELETE);
            CourseInterestEntity entity = loadVisibleCourseInterest(scope, courseId, id);
            OwnRows.require(entity.getUserId(), COURSE_INTEREST_AGGREGATE, id);
            scope.requireWritable(institutionOfCourseInterest(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (courseInterestRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_INTEREST_AGGREGATE, id);
            outbox.publish(COURSE_INTEREST_AGGREGATE + LmsEvents.DELETED, COURSE_INTEREST_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CourseInterestEntity loadVisibleCourseInterest(DataScope scope, UUID courseId, UUID id) {
        CourseInterestEntity entity = courseInterestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_INTEREST_AGGREGATE, id));
        if (!scope.canRead(institutionOfCourseInterest(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_INTEREST_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCourseInterest(CourseInterestEntity entity) {
        return courseInterestRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCourseInterest(CourseInterestEntity entity, CourseInterestUpdate changes) {
        Patches patch = new Patches();
        patch.apply("emailSent", changes.getEmailSent(), entity::setEmailSent);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCourseInterest(CourseInterestEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEmailSent() == null) {
            errors.add(new FieldError("emailSent", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !courseInterestRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCourseInterest(CourseInterestEntity entity) {
        if (courseInterestRepository.takenCourseIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A course interest with the same course id and user id already exists.");
        }
    }

    // ---- CourseMentor: a record of the LmsCourse aggregate ----

    public CourseMentor getCourseMentor(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return courseMentorMapper.toDto(loadVisibleCourseMentor(DataScope.current(COURSE_MENTOR_PERMISSION_GET),
                    courseId, id));
        });
    }

    public Page<CourseMentor> listCourseMentors(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CourseMentorRepository.SORT_FIELDS, CourseMentorRepository.DEFAULT_ORDER,
                CourseMentorRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CourseMentorRepository.FILTER_FIELDS)
                .and(CourseMentorRepository.ofParent(courseId))
                        .and(CourseMentorRepository.scope(DataScope.current(COURSE_MENTOR_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            Page<CourseMentorEntity> rows = courseMentorRepository.list(page, where);
            return rows.map(courseMentorMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CourseMentor createCourseMentor(UUID courseId, CourseMentor request, boolean validateOnly) {
        validator.validate(request);
        CourseMentorEntity entity = courseMentorMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_MENTOR_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!courseMentorRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCourseMentor(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCourseMentor(entity, true);
            scope.requireWritable(institutionOfCourseMentor(entity));
            assertUniqueCourseMentor(entity);
            if (validateOnly) {
                return courseMentorMapper.toDto(entity);
            }
            CourseMentorEntity stored = courseMentorRepository.insert(entity);
            audit.recordCreate(COURSE_MENTOR_AGGREGATE, stored.getId());
            outbox.publish(COURSE_MENTOR_AGGREGATE + LmsEvents.CREATED, COURSE_MENTOR_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return courseMentorMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CourseMentor updateCourseMentor(UUID courseId, UUID id, String ifMatch, CourseMentorUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_MENTOR_PERMISSION_UPDATE);
            CourseMentorEntity entity = loadVisibleCourseMentor(scope, courseId, id);
            scope.requireWritable(institutionOfCourseMentor(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCourseMentor(entity, changes);
            validator.validate(courseMentorMapper.toDto(entity));
            assertValidCourseMentor(entity, false);
            scope.requireWritable(institutionOfCourseMentor(entity));
            assertUniqueCourseMentor(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return courseMentorMapper.toDto(entity);
            }
            if (courseMentorRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_MENTOR_AGGREGATE, id, fields);
            outbox.publish(COURSE_MENTOR_AGGREGATE + LmsEvents.UPDATED, COURSE_MENTOR_AGGREGATE, id, LmsEvents.payload(id));
            return courseMentorMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourseMentor(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_MENTOR_PERMISSION_DELETE);
            CourseMentorEntity entity = loadVisibleCourseMentor(scope, courseId, id);
            scope.requireWritable(institutionOfCourseMentor(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (courseMentorRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_MENTOR_AGGREGATE, id);
            outbox.publish(COURSE_MENTOR_AGGREGATE + LmsEvents.DELETED, COURSE_MENTOR_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CourseMentorEntity loadVisibleCourseMentor(DataScope scope, UUID courseId, UUID id) {
        CourseMentorEntity entity = courseMentorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_MENTOR_AGGREGATE, id));
        if (!scope.canRead(institutionOfCourseMentor(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_MENTOR_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCourseMentor(CourseMentorEntity entity) {
        return courseMentorRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCourseMentor(CourseMentorEntity entity, CourseMentorUpdate changes) {
        Patches patch = new Patches();
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCourseMentor(CourseMentorEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !courseMentorRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCourseMentor(CourseMentorEntity entity) {
        if (courseMentorRepository.takenCourseIdUserId1(entity)) {
            throw new DuplicateResourceException("A course mentor with the same course id and user id already exists.");
        }
    }

    // ---- CoursePreview: a record of the LmsCourse aggregate ----

    public CoursePreview getCoursePreview(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return coursePreviewMapper.toDto(loadVisibleCoursePreview(DataScope.current(COURSE_PREVIEW_PERMISSION_GET),
                    courseId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CoursePreview createCoursePreview(UUID courseId, CoursePreview request, boolean validateOnly) {
        validator.validate(request);
        CoursePreviewEntity entity = coursePreviewMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_PREVIEW_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!coursePreviewRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCoursePreview(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCoursePreview(entity, true);
            scope.requireWritable(institutionOfCoursePreview(entity));
            assertUniqueCoursePreview(entity);
            if (validateOnly) {
                return coursePreviewMapper.toDto(entity);
            }
            CoursePreviewEntity stored = coursePreviewRepository.insert(entity);
            audit.recordCreate(COURSE_PREVIEW_AGGREGATE, stored.getId());
            outbox.publish(COURSE_PREVIEW_AGGREGATE + LmsEvents.CREATED, COURSE_PREVIEW_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return coursePreviewMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CoursePreview updateCoursePreview(UUID courseId, UUID id, String ifMatch, CoursePreviewUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_PREVIEW_PERMISSION_UPDATE);
            CoursePreviewEntity entity = loadVisibleCoursePreview(scope, courseId, id);
            scope.requireWritable(institutionOfCoursePreview(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCoursePreview(entity, changes);
            validator.validate(coursePreviewMapper.toDto(entity));
            assertValidCoursePreview(entity, false);
            scope.requireWritable(institutionOfCoursePreview(entity));
            assertUniqueCoursePreview(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return coursePreviewMapper.toDto(entity);
            }
            if (coursePreviewRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_PREVIEW_AGGREGATE, id, fields);
            outbox.publish(COURSE_PREVIEW_AGGREGATE + LmsEvents.UPDATED, COURSE_PREVIEW_AGGREGATE, id, LmsEvents.payload(id));
            return coursePreviewMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCoursePreview(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_PREVIEW_PERMISSION_DELETE);
            CoursePreviewEntity entity = loadVisibleCoursePreview(scope, courseId, id);
            scope.requireWritable(institutionOfCoursePreview(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (coursePreviewRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_PREVIEW_AGGREGATE, id);
            outbox.publish(COURSE_PREVIEW_AGGREGATE + LmsEvents.DELETED, COURSE_PREVIEW_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CoursePreviewEntity loadVisibleCoursePreview(DataScope scope, UUID courseId, UUID id) {
        CoursePreviewEntity entity = coursePreviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_PREVIEW_AGGREGATE, id));
        if (!scope.canRead(institutionOfCoursePreview(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_PREVIEW_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCoursePreview(CoursePreviewEntity entity) {
        return coursePreviewRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCoursePreview(CoursePreviewEntity entity, CoursePreviewUpdate changes) {
        Patches patch = new Patches();
        patch.apply("previewKind", changes.getPreviewKind(), v -> entity.setPreviewKind(v == null ? null : v.getValue()));
        patch.apply("fileId", changes.getFileId(), entity::setFileId);
        patch.apply("icon", changes.getIcon(), entity::setIcon);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCoursePreview(CoursePreviewEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getPreviewKind() == null) {
            errors.add(new FieldError("previewKind", "must not be null", "NotNull"));
        }
        if (entity.getFileId() == null) {
            errors.add(new FieldError("fileId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !coursePreviewRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCoursePreview(CoursePreviewEntity entity) {

    }

    // ---- CourseReview: a record of the LmsCourse aggregate ----

    public CourseReview getCourseReview(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return courseReviewMapper.toDto(loadVisibleCourseReview(DataScope.current(COURSE_REVIEW_PERMISSION_GET),
                    courseId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CourseReview createCourseReview(UUID courseId, CourseReview request, boolean validateOnly) {
        validator.validate(request);
        CourseReviewEntity entity = courseReviewMapper.toEntity(request);
        entity.setUserId(OwnRows.caller());
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_REVIEW_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!courseReviewRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCourseReview(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCourseReview(entity, true);
            scope.requireWritable(institutionOfCourseReview(entity));
            assertUniqueCourseReview(entity);
            if (validateOnly) {
                return courseReviewMapper.toDto(entity);
            }
            CourseReviewEntity stored = courseReviewRepository.insert(entity);
            audit.recordCreate(COURSE_REVIEW_AGGREGATE, stored.getId());
            outbox.publish(COURSE_REVIEW_AGGREGATE + LmsEvents.CREATED, COURSE_REVIEW_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return courseReviewMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CourseReview updateCourseReview(UUID courseId, UUID id, String ifMatch, CourseReviewUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_REVIEW_PERMISSION_UPDATE);
            CourseReviewEntity entity = loadVisibleCourseReview(scope, courseId, id);
            OwnRows.require(entity.getUserId(), COURSE_REVIEW_AGGREGATE, id);
            scope.requireWritable(institutionOfCourseReview(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCourseReview(entity, changes);
            validator.validate(courseReviewMapper.toDto(entity));
            assertValidCourseReview(entity, false);
            scope.requireWritable(institutionOfCourseReview(entity));
            assertUniqueCourseReview(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return courseReviewMapper.toDto(entity);
            }
            if (courseReviewRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_REVIEW_AGGREGATE, id, fields);
            outbox.publish(COURSE_REVIEW_AGGREGATE + LmsEvents.UPDATED, COURSE_REVIEW_AGGREGATE, id, LmsEvents.payload(id));
            return courseReviewMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourseReview(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_REVIEW_PERMISSION_DELETE);
            CourseReviewEntity entity = loadVisibleCourseReview(scope, courseId, id);
            OwnRows.require(entity.getUserId(), COURSE_REVIEW_AGGREGATE, id);
            scope.requireWritable(institutionOfCourseReview(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (courseReviewRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_REVIEW_AGGREGATE, id);
            outbox.publish(COURSE_REVIEW_AGGREGATE + LmsEvents.DELETED, COURSE_REVIEW_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CourseReviewEntity loadVisibleCourseReview(DataScope scope, UUID courseId, UUID id) {
        CourseReviewEntity entity = courseReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_REVIEW_AGGREGATE, id));
        if (!scope.canRead(institutionOfCourseReview(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_REVIEW_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCourseReview(CourseReviewEntity entity) {
        return courseReviewRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCourseReview(CourseReviewEntity entity, CourseReviewUpdate changes) {
        Patches patch = new Patches();
        patch.apply("rating", changes.getRating(), v -> entity.setRating(WireValues.decimal(v)));
        patch.apply("review", changes.getReview(), entity::setReview);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCourseReview(CourseReviewEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getRating() == null) {
            errors.add(new FieldError("rating", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("rating", WireValues.decimal(entity.getRating()), 3, 2, errors);
        if (entity.getCourseId() != null && !courseReviewRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCourseReview(CourseReviewEntity entity) {
        if (courseReviewRepository.takenCourseIdUserId1(entity)) {
            throw new DuplicateResourceException("A course review with the same course id and user id already exists.");
        }
    }

    // ---- CourseTeacher: a record of the LmsCourse aggregate ----

    public CourseTeacher getCourseTeacher(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return courseTeacherMapper.toDto(loadVisibleCourseTeacher(DataScope.current(COURSE_TEACHER_PERMISSION_GET),
                    courseId, id));
        });
    }

    public Page<CourseTeacher> listCourseTeachers(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CourseTeacherRepository.SORT_FIELDS, CourseTeacherRepository.DEFAULT_ORDER,
                CourseTeacherRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CourseTeacherRepository.FILTER_FIELDS)
                .and(CourseTeacherRepository.ofParent(courseId))
                        .and(CourseTeacherRepository.scope(DataScope.current(COURSE_TEACHER_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            Page<CourseTeacherEntity> rows = courseTeacherRepository.list(page, where);
            return rows.map(courseTeacherMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CourseTeacher createCourseTeacher(UUID courseId, CourseTeacher request, boolean validateOnly) {
        validator.validate(request);
        CourseTeacherEntity entity = courseTeacherMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_TEACHER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!courseTeacherRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCourseTeacher(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCourseTeacher(entity, true);
            scope.requireWritable(institutionOfCourseTeacher(entity));
            assertUniqueCourseTeacher(entity);
            if (validateOnly) {
                return courseTeacherMapper.toDto(entity);
            }
            CourseTeacherEntity stored = courseTeacherRepository.insert(entity);
            audit.recordCreate(COURSE_TEACHER_AGGREGATE, stored.getId());
            outbox.publish(COURSE_TEACHER_AGGREGATE + LmsEvents.CREATED, COURSE_TEACHER_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return courseTeacherMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CourseTeacher updateCourseTeacher(UUID courseId, UUID id, String ifMatch, CourseTeacherUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_TEACHER_PERMISSION_UPDATE);
            CourseTeacherEntity entity = loadVisibleCourseTeacher(scope, courseId, id);
            scope.requireWritable(institutionOfCourseTeacher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCourseTeacher(entity, changes);
            validator.validate(courseTeacherMapper.toDto(entity));
            assertValidCourseTeacher(entity, false);
            scope.requireWritable(institutionOfCourseTeacher(entity));
            assertUniqueCourseTeacher(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return courseTeacherMapper.toDto(entity);
            }
            if (courseTeacherRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_TEACHER_AGGREGATE, id, fields);
            outbox.publish(COURSE_TEACHER_AGGREGATE + LmsEvents.UPDATED, COURSE_TEACHER_AGGREGATE, id, LmsEvents.payload(id));
            return courseTeacherMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourseTeacher(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_TEACHER_PERMISSION_DELETE);
            CourseTeacherEntity entity = loadVisibleCourseTeacher(scope, courseId, id);
            scope.requireWritable(institutionOfCourseTeacher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (courseTeacherRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_TEACHER_AGGREGATE, id);
            outbox.publish(COURSE_TEACHER_AGGREGATE + LmsEvents.DELETED, COURSE_TEACHER_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CourseTeacherEntity loadVisibleCourseTeacher(DataScope scope, UUID courseId, UUID id) {
        CourseTeacherEntity entity = courseTeacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_TEACHER_AGGREGATE, id));
        if (!scope.canRead(institutionOfCourseTeacher(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_TEACHER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCourseTeacher(CourseTeacherEntity entity) {
        return courseTeacherRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCourseTeacher(CourseTeacherEntity entity, CourseTeacherUpdate changes) {
        Patches patch = new Patches();
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCourseTeacher(CourseTeacherEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !courseTeacherRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCourseTeacher(CourseTeacherEntity entity) {
        if (courseTeacherRepository.takenCourseIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A course teacher with the same course id and user id already exists.");
        }
    }

    // ---- CourseTestimonial: a record of the LmsCourse aggregate ----

    public CourseTestimonial getCourseTestimonial(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return courseTestimonialMapper.toDto(loadVisibleCourseTestimonial(DataScope.current(
                    COURSE_TESTIMONIAL_PERMISSION_GET), courseId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CourseTestimonial createCourseTestimonial(UUID courseId, CourseTestimonial request, boolean validateOnly) {
        validator.validate(request);
        CourseTestimonialEntity entity = courseTestimonialMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_TESTIMONIAL_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!courseTestimonialRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCourseTestimonial(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidCourseTestimonial(entity, true);
            scope.requireWritable(institutionOfCourseTestimonial(entity));
            assertUniqueCourseTestimonial(entity);
            if (validateOnly) {
                return courseTestimonialMapper.toDto(entity);
            }
            CourseTestimonialEntity stored = courseTestimonialRepository.insert(entity);
            audit.recordCreate(COURSE_TESTIMONIAL_AGGREGATE, stored.getId());
            outbox.publish(COURSE_TESTIMONIAL_AGGREGATE + LmsEvents.CREATED, COURSE_TESTIMONIAL_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return courseTestimonialMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CourseTestimonial updateCourseTestimonial(UUID courseId, UUID id, String ifMatch,
            CourseTestimonialUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_TESTIMONIAL_PERMISSION_UPDATE);
            CourseTestimonialEntity entity = loadVisibleCourseTestimonial(scope, courseId, id);
            scope.requireWritable(institutionOfCourseTestimonial(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCourseTestimonial(entity, changes);
            validator.validate(courseTestimonialMapper.toDto(entity));
            assertValidCourseTestimonial(entity, false);
            scope.requireWritable(institutionOfCourseTestimonial(entity));
            assertUniqueCourseTestimonial(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return courseTestimonialMapper.toDto(entity);
            }
            if (courseTestimonialRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_TESTIMONIAL_AGGREGATE, id, fields);
            outbox.publish(COURSE_TESTIMONIAL_AGGREGATE + LmsEvents.UPDATED, COURSE_TESTIMONIAL_AGGREGATE, id,
                    LmsEvents.payload(id));
            return courseTestimonialMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourseTestimonial(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_TESTIMONIAL_PERMISSION_DELETE);
            CourseTestimonialEntity entity = loadVisibleCourseTestimonial(scope, courseId, id);
            scope.requireWritable(institutionOfCourseTestimonial(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (courseTestimonialRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_TESTIMONIAL_AGGREGATE, id);
            outbox.publish(COURSE_TESTIMONIAL_AGGREGATE + LmsEvents.DELETED, COURSE_TESTIMONIAL_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CourseTestimonialEntity loadVisibleCourseTestimonial(DataScope scope, UUID courseId, UUID id) {
        CourseTestimonialEntity entity = courseTestimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_TESTIMONIAL_AGGREGATE, id));
        if (!scope.canRead(institutionOfCourseTestimonial(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_TESTIMONIAL_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfCourseTestimonial(CourseTestimonialEntity entity) {
        return courseTestimonialRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCourseTestimonial(CourseTestimonialEntity entity, CourseTestimonialUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("quote", changes.getQuote(), entity::setQuote);
        patch.apply("photoFileId", changes.getPhotoFileId(), entity::setPhotoFileId);
        patch.apply("currentPosition", changes.getCurrentPosition(), entity::setCurrentPosition);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCourseTestimonial(CourseTestimonialEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !courseTestimonialRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCourseTestimonial(CourseTestimonialEntity entity) {

    }

    // ---- EnrollmentRequest: a record of the LmsCourse aggregate ----

    public EnrollmentRequest getEnrollmentRequest(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return enrollmentRequestMapper.toDto(loadVisibleEnrollmentRequest(DataScope.current(
                    ENROLLMENT_REQUEST_PERMISSION_GET), courseId, id));
        });
    }

    public Page<EnrollmentRequest> listEnrollmentRequests(UUID courseId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EnrollmentRequestRepository.SORT_FIELDS,
                        EnrollmentRequestRepository.DEFAULT_ORDER,
                EnrollmentRequestRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EnrollmentRequestRepository.FILTER_FIELDS)
                .and(EnrollmentRequestRepository.ofParent(courseId))
                        .and(EnrollmentRequestRepository.scope(DataScope.current(ENROLLMENT_REQUEST_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            Page<EnrollmentRequestEntity> rows = enrollmentRequestRepository.list(page, where);
            return rows.map(enrollmentRequestMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EnrollmentRequest createEnrollmentRequest(UUID courseId, EnrollmentRequest request, boolean validateOnly) {
        validator.validate(request);
        EnrollmentRequestEntity entity = enrollmentRequestMapper.toEntity(request);
        entity.setUserId(OwnRows.caller());
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ENROLLMENT_REQUEST_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!enrollmentRequestRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfEnrollmentRequest(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidEnrollmentRequest(entity, true);
            scope.requireWritable(institutionOfEnrollmentRequest(entity));
            assertUniqueEnrollmentRequest(entity);
            if (validateOnly) {
                return enrollmentRequestMapper.toDto(entity);
            }
            EnrollmentRequestEntity stored = enrollmentRequestRepository.insert(entity);
            audit.recordCreate(ENROLLMENT_REQUEST_AGGREGATE, stored.getId());
            outbox.publish(ENROLLMENT_REQUEST_AGGREGATE + LmsEvents.CREATED, ENROLLMENT_REQUEST_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return enrollmentRequestMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public EnrollmentRequest updateEnrollmentRequest(UUID courseId, UUID id, String ifMatch,
            EnrollmentRequestUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ENROLLMENT_REQUEST_PERMISSION_UPDATE);
            EnrollmentRequestEntity entity = loadVisibleEnrollmentRequest(scope, courseId, id);
            OwnRows.require(entity.getUserId(), ENROLLMENT_REQUEST_AGGREGATE, id);
            scope.requireWritable(institutionOfEnrollmentRequest(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyEnrollmentRequest(entity, changes);
            validator.validate(enrollmentRequestMapper.toDto(entity));
            assertValidEnrollmentRequest(entity, false);
            scope.requireWritable(institutionOfEnrollmentRequest(entity));
            assertUniqueEnrollmentRequest(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return enrollmentRequestMapper.toDto(entity);
            }
            if (enrollmentRequestRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ENROLLMENT_REQUEST_AGGREGATE, id, fields);
            outbox.publish(ENROLLMENT_REQUEST_AGGREGATE + LmsEvents.UPDATED, ENROLLMENT_REQUEST_AGGREGATE, id,
                    LmsEvents.payload(id));
            return enrollmentRequestMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEnrollmentRequest(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ENROLLMENT_REQUEST_PERMISSION_DELETE);
            EnrollmentRequestEntity entity = loadVisibleEnrollmentRequest(scope, courseId, id);
            OwnRows.require(entity.getUserId(), ENROLLMENT_REQUEST_AGGREGATE, id);
            scope.requireWritable(institutionOfEnrollmentRequest(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (enrollmentRequestRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ENROLLMENT_REQUEST_AGGREGATE, id);
            outbox.publish(ENROLLMENT_REQUEST_AGGREGATE + LmsEvents.DELETED, ENROLLMENT_REQUEST_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /**
     * lms.RejectEnrollmentRequest: Reject. Allowed while status is pending, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public EnrollmentRequest rejectEnrollmentRequest(UUID courseId, UUID id, RejectEnrollmentRequestRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_ENROLLMENT_REQUEST);
        return doInWriteTransaction(() -> {
            EnrollmentRequestEntity entity = enrollmentRequestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(ENROLLMENT_REQUEST_AGGREGATE, id));
            if (!scope.canRead(institutionOfEnrollmentRequest(entity)) || !courseId.equals(entity.getCourseId())) {
                throw new ResourceNotFoundException(ENROLLMENT_REQUEST_AGGREGATE, id);
            }
            scope.requireWritable(institutionOfEnrollmentRequest(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("pending").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The enrollment request cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (enrollmentRequestRepository.rejectEnrollmentRequest(id, entity.getRowVersion(), now, Actor.current()
                    .orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(ENROLLMENT_REQUEST_AGGREGATE, id, "reject", List.of("status", "decidedBy", "decidedAt"),
                    request == null ? null : request.getReason());
            outbox.publish(ENROLLMENT_REQUEST_AGGREGATE + ".reject", ENROLLMENT_REQUEST_AGGREGATE, id, LmsEvents.payload(id));
            return enrollmentRequestMapper.toDto(enrollmentRequestRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(ENROLLMENT_REQUEST_AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EnrollmentRequestEntity loadVisibleEnrollmentRequest(DataScope scope, UUID courseId, UUID id) {
        EnrollmentRequestEntity entity = enrollmentRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENROLLMENT_REQUEST_AGGREGATE, id));
        if (!scope.canRead(institutionOfEnrollmentRequest(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(ENROLLMENT_REQUEST_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfEnrollmentRequest(EnrollmentRequestEntity entity) {
        return enrollmentRequestRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyEnrollmentRequest(EnrollmentRequestEntity entity, EnrollmentRequestUpdate changes) {
        Patches patch = new Patches();
        patch.apply("cohortId", changes.getCohortId(), entity::setCohortId);
        patch.apply("requestedAt", changes.getRequestedAt(), entity::setRequestedAt);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEnrollmentRequest(EnrollmentRequestEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getRequestedAt() == null) {
            errors.add(new FieldError("requestedAt", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !enrollmentRequestRepository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !enrollmentRequestRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEnrollmentRequest(EnrollmentRequestEntity entity) {

    }

    // ---- RelatedCourse: a record of the LmsCourse aggregate ----

    public RelatedCourse getRelatedCourse(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return relatedCourseMapper.toDto(loadVisibleRelatedCourse(DataScope.current(RELATED_COURSE_PERMISSION_GET),
                    courseId, id));
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RelatedCourse createRelatedCourse(UUID courseId, RelatedCourse request, boolean validateOnly) {
        validator.validate(request);
        RelatedCourseEntity entity = relatedCourseMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RELATED_COURSE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!relatedCourseRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfRelatedCourse(entity))) {
                throw new ResourceNotFoundException("lms.course", entity.getCourseId());
            }
            assertValidRelatedCourse(entity, true);
            scope.requireWritable(institutionOfRelatedCourse(entity));
            assertUniqueRelatedCourse(entity);
            if (validateOnly) {
                return relatedCourseMapper.toDto(entity);
            }
            RelatedCourseEntity stored = relatedCourseRepository.insert(entity);
            audit.recordCreate(RELATED_COURSE_AGGREGATE, stored.getId());
            outbox.publish(RELATED_COURSE_AGGREGATE + LmsEvents.CREATED, RELATED_COURSE_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return relatedCourseMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public RelatedCourse updateRelatedCourse(UUID courseId, UUID id, String ifMatch, RelatedCourseUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELATED_COURSE_PERMISSION_UPDATE);
            RelatedCourseEntity entity = loadVisibleRelatedCourse(scope, courseId, id);
            scope.requireWritable(institutionOfRelatedCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyRelatedCourse(entity, changes);
            validator.validate(relatedCourseMapper.toDto(entity));
            assertValidRelatedCourse(entity, false);
            scope.requireWritable(institutionOfRelatedCourse(entity));
            assertUniqueRelatedCourse(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return relatedCourseMapper.toDto(entity);
            }
            if (relatedCourseRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RELATED_COURSE_AGGREGATE, id, fields);
            outbox.publish(RELATED_COURSE_AGGREGATE + LmsEvents.UPDATED, RELATED_COURSE_AGGREGATE, id, LmsEvents.payload(id));
            return relatedCourseMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRelatedCourse(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELATED_COURSE_PERMISSION_DELETE);
            RelatedCourseEntity entity = loadVisibleRelatedCourse(scope, courseId, id);
            scope.requireWritable(institutionOfRelatedCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (relatedCourseRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RELATED_COURSE_AGGREGATE, id);
            outbox.publish(RELATED_COURSE_AGGREGATE + LmsEvents.DELETED, RELATED_COURSE_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RelatedCourseEntity loadVisibleRelatedCourse(DataScope scope, UUID courseId, UUID id) {
        RelatedCourseEntity entity = relatedCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RELATED_COURSE_AGGREGATE, id));
        if (!scope.canRead(institutionOfRelatedCourse(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(RELATED_COURSE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfRelatedCourse(RelatedCourseEntity entity) {
        return relatedCourseRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyRelatedCourse(RelatedCourseEntity entity, RelatedCourseUpdate changes) {
        Patches patch = new Patches();
        patch.apply("relatedCourseId", changes.getRelatedCourseId(), entity::setRelatedCourseId);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("authorProfileId", changes.getAuthorProfileId(), entity::setAuthorProfileId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRelatedCourse(RelatedCourseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getRelatedCourseId() == null) {
            errors.add(new FieldError("relatedCourseId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getAuthorProfileId() != null && !relatedCourseRepository.existsAuthorProfileId(entity.getAuthorProfileId())) {
            errors.add(new FieldError("authorProfileId", "course author profile does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !relatedCourseRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getRelatedCourseId() != null && !relatedCourseRepository.existsRelatedCourseId(entity.getRelatedCourseId())) {
            errors.add(new FieldError("relatedCourseId", "course does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRelatedCourse(RelatedCourseEntity entity) {
        if (relatedCourseRepository.takenCourseIdRelatedCourseId1(entity)) {
            throw new DuplicateResourceException(
                    "A related course with the same course id and related course id already exists.");
        }
    }
}
