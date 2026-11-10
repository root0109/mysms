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
import com.mysms.lms.api.model.CatalogBanner;
import com.mysms.lms.api.model.CatalogBannerUpdate;
import com.mysms.lms.api.model.Chapter;
import com.mysms.lms.api.model.ChapterUpdate;
import com.mysms.lms.api.model.CourseAuthorProfile;
import com.mysms.lms.api.model.CourseAuthorProfileUpdate;
import com.mysms.lms.api.model.CourseInterest;
import com.mysms.lms.api.model.CourseInterestUpdate;
import com.mysms.lms.api.model.CourseMentor;
import com.mysms.lms.api.model.CourseMentorUpdate;
import com.mysms.lms.api.model.CoursePreview;
import com.mysms.lms.api.model.CoursePreviewUpdate;
import com.mysms.lms.api.model.CourseReview;
import com.mysms.lms.api.model.CourseReviewUpdate;
import com.mysms.lms.api.model.CourseTeacher;
import com.mysms.lms.api.model.CourseTeacherUpdate;
import com.mysms.lms.api.model.CourseTestimonial;
import com.mysms.lms.api.model.CourseTestimonialUpdate;
import com.mysms.lms.api.model.EnrollmentRequest;
import com.mysms.lms.api.model.EnrollmentRequestUpdate;
import com.mysms.lms.api.model.LmsCourse;
import com.mysms.lms.api.model.LmsCourseUpdate;
import com.mysms.lms.api.model.RejectEnrollmentRequestRequest;
import com.mysms.lms.api.model.RelatedCourse;
import com.mysms.lms.api.model.RelatedCourseUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.CatalogBannerEntity;
import com.mysms.lms.domain.ChapterEntity;
import com.mysms.lms.domain.CourseAuthorProfileEntity;
import com.mysms.lms.domain.CourseInterestEntity;
import com.mysms.lms.domain.CourseMentorEntity;
import com.mysms.lms.domain.CoursePreviewEntity;
import com.mysms.lms.domain.CourseReviewEntity;
import com.mysms.lms.domain.CourseTeacherEntity;
import com.mysms.lms.domain.CourseTestimonialEntity;
import com.mysms.lms.domain.EnrollmentRequestEntity;
import com.mysms.lms.domain.LmsCourseEntity;
import com.mysms.lms.domain.RelatedCourseEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.CatalogBannerMapper;
import com.mysms.lms.mapper.ChapterMapper;
import com.mysms.lms.mapper.CourseAuthorProfileMapper;
import com.mysms.lms.mapper.CourseInterestMapper;
import com.mysms.lms.mapper.CourseMentorMapper;
import com.mysms.lms.mapper.CoursePreviewMapper;
import com.mysms.lms.mapper.CourseReviewMapper;
import com.mysms.lms.mapper.CourseTeacherMapper;
import com.mysms.lms.mapper.CourseTestimonialMapper;
import com.mysms.lms.mapper.EnrollmentRequestMapper;
import com.mysms.lms.mapper.LmsCourseMapper;
import com.mysms.lms.mapper.RelatedCourseMapper;
import com.mysms.lms.repository.CatalogBannerRepository;
import com.mysms.lms.repository.ChapterRepository;
import com.mysms.lms.repository.CourseAuthorProfileRepository;
import com.mysms.lms.repository.CourseInterestRepository;
import com.mysms.lms.repository.CourseMentorRepository;
import com.mysms.lms.repository.CoursePreviewRepository;
import com.mysms.lms.repository.CourseReviewRepository;
import com.mysms.lms.repository.CourseTeacherRepository;
import com.mysms.lms.repository.CourseTestimonialRepository;
import com.mysms.lms.repository.EnrollmentRequestRepository;
import com.mysms.lms.repository.LmsCourseRepository;
import com.mysms.lms.repository.RelatedCourseRepository;
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
 * Business service of lms.course (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a LmsCourse
 * are served here too — CatalogBanner, Chapter, CourseAuthorProfile, CourseInterest, CourseMentor, CoursePreview,
 * CourseReview, CourseTeacher, CourseTestimonial, EnrollmentRequest, RelatedCourse.
 */
@Service
public class LmsCourseService extends AppTransactionManager {

    public static final String AGGREGATE = LmsCourseEntity.TABLE;
    static final String PERMISSION_CREATE = "lms.course.create";
    static final String PERMISSION_UPDATE = "lms.course.update";
    /** The contract's LmsCourseUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "title", "shortIntroduction", "description", "imageFileId", "videoUrl",
                    "categoryId",
            "publishedOn", "isUpcoming", "isFeatured", "disableSelfLearning", "enforceLessonOrder",
            "certificationEnabled", "paidCertificate", "defaultEvaluatorId", "isPaid", "price", "currencyId",
            "priceUsd", "cardGradient", "timezone", "publishNotificationSent", "enrollmentCount", "lessonCount",
            "averageRating", "code", "stateStandardCode", "credits", "prerequisites", "corequisites", "effectiveFrom",
            "lifecycleStatus", "deactivatedOn", "versionMajor", "versionMinor", "previousVersionId", "rootCourseId",
            "authorComments", "externalRef", "nominalDuration", "deliveryModality", "isShared", "contentUpdatedAt",
            "gradeDisplay", "gradingSchemeId", "isInCatalog", "catalogDescription", "catalogVisibleFrom",
            "catalogVisibleUntil", "outline", "workloadHours", "catalogStatus");

    public static final String CATALOG_BANNER_AGGREGATE = CatalogBannerEntity.TABLE;
    static final String CATALOG_BANNER_PERMISSION_GET = "lms.catalog_banner.read";
    static final String CATALOG_BANNER_PERMISSION_CREATE = "lms.catalog_banner.create";
    static final String CATALOG_BANNER_PERMISSION_UPDATE = "lms.catalog_banner.update";
    static final String CATALOG_BANNER_PERMISSION_DELETE = "lms.catalog_banner.delete";
    /** The contract's CatalogBannerUpdate fields, in apply order. */
    static final List<String> CATALOG_BANNER_WRITABLE = List.of("placement", "imageFileId", "sortOrder");

    public static final String CHAPTER_AGGREGATE = ChapterEntity.TABLE;
    static final String CHAPTER_PERMISSION_LIST = "lms.chapter.read";
    static final String CHAPTER_PERMISSION_GET = "lms.chapter.read";
    static final String CHAPTER_PERMISSION_CREATE = "lms.chapter.create";
    static final String CHAPTER_PERMISSION_UPDATE = "lms.chapter.update";
    static final String CHAPTER_PERMISSION_DELETE = "lms.chapter.delete";
    /** The contract's ChapterUpdate fields, in apply order. */
    static final List<String> CHAPTER_WRITABLE =
            List.of("title", "sortOrder", "isScormPackage", "scormPackageFileId", "scormPackagePath",
                    "scormManifestFile",
            "scormLaunchFile", "cohortId", "parentChapterId", "description", "chapterNumber", "startDay", "startTime",
            "dueDay", "dueTime", "startsAt", "endsAt", "allowDelete", "allowEditDetails", "allowEditDates",
            "copiedFromId", "isOriginal");

    public static final String COURSE_AUTHOR_PROFILE_AGGREGATE = CourseAuthorProfileEntity.TABLE;
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_GET = "lms.course_author_profile.read";
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_CREATE = "lms.course_author_profile.create";
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_UPDATE = "lms.course_author_profile.update";
    static final String COURSE_AUTHOR_PROFILE_PERMISSION_DELETE = "lms.course_author_profile.delete";
    /** The contract's CourseAuthorProfileUpdate fields, in apply order. */
    static final List<String> COURSE_AUTHOR_PROFILE_WRITABLE = List.of("name", "bio", "photoFileId", "qualification",
            "sortOrder");

    public static final String COURSE_INTEREST_AGGREGATE = CourseInterestEntity.TABLE;
    static final String COURSE_INTEREST_PERMISSION_LIST = "lms.course_interest.read";
    static final String COURSE_INTEREST_PERMISSION_GET = "lms.course_interest.read";
    static final String COURSE_INTEREST_PERMISSION_CREATE = "lms.course_interest.create";
    static final String COURSE_INTEREST_PERMISSION_UPDATE = "lms.course_interest.update";
    static final String COURSE_INTEREST_PERMISSION_DELETE = "lms.course_interest.delete";
    /** The contract's CourseInterestUpdate fields, in apply order. */
    static final List<String> COURSE_INTEREST_WRITABLE = List.of("userId", "emailSent");

    public static final String COURSE_MENTOR_AGGREGATE = CourseMentorEntity.TABLE;
    static final String COURSE_MENTOR_PERMISSION_LIST = "lms.course_mentor.read";
    static final String COURSE_MENTOR_PERMISSION_GET = "lms.course_mentor.read";
    static final String COURSE_MENTOR_PERMISSION_CREATE = "lms.course_mentor.create";
    static final String COURSE_MENTOR_PERMISSION_UPDATE = "lms.course_mentor.update";
    static final String COURSE_MENTOR_PERMISSION_DELETE = "lms.course_mentor.delete";
    /** The contract's CourseMentorUpdate fields, in apply order. */
    static final List<String> COURSE_MENTOR_WRITABLE = List.of("userId");

    public static final String COURSE_PREVIEW_AGGREGATE = CoursePreviewEntity.TABLE;
    static final String COURSE_PREVIEW_PERMISSION_GET = "lms.course_preview.read";
    static final String COURSE_PREVIEW_PERMISSION_CREATE = "lms.course_preview.create";
    static final String COURSE_PREVIEW_PERMISSION_UPDATE = "lms.course_preview.update";
    static final String COURSE_PREVIEW_PERMISSION_DELETE = "lms.course_preview.delete";
    /** The contract's CoursePreviewUpdate fields, in apply order. */
    static final List<String> COURSE_PREVIEW_WRITABLE = List.of("previewKind", "fileId", "icon", "title", "description",
            "sortOrder");

    public static final String COURSE_REVIEW_AGGREGATE = CourseReviewEntity.TABLE;
    static final String COURSE_REVIEW_PERMISSION_GET = "lms.course_review.read";
    static final String COURSE_REVIEW_PERMISSION_CREATE = "lms.course_review.create";
    static final String COURSE_REVIEW_PERMISSION_UPDATE = "lms.course_review.update";
    static final String COURSE_REVIEW_PERMISSION_DELETE = "lms.course_review.delete";
    /** The contract's CourseReviewUpdate fields, in apply order. */
    static final List<String> COURSE_REVIEW_WRITABLE = List.of("userId", "rating", "review");

    public static final String COURSE_TEACHER_AGGREGATE = CourseTeacherEntity.TABLE;
    static final String COURSE_TEACHER_PERMISSION_LIST = "lms.course_teacher.read";
    static final String COURSE_TEACHER_PERMISSION_GET = "lms.course_teacher.read";
    static final String COURSE_TEACHER_PERMISSION_CREATE = "lms.course_teacher.create";
    static final String COURSE_TEACHER_PERMISSION_UPDATE = "lms.course_teacher.update";
    static final String COURSE_TEACHER_PERMISSION_DELETE = "lms.course_teacher.delete";
    /** The contract's CourseTeacherUpdate fields, in apply order. */
    static final List<String> COURSE_TEACHER_WRITABLE = List.of("userId", "sortOrder");

    public static final String COURSE_TESTIMONIAL_AGGREGATE = CourseTestimonialEntity.TABLE;
    static final String COURSE_TESTIMONIAL_PERMISSION_GET = "lms.course_testimonial.read";
    static final String COURSE_TESTIMONIAL_PERMISSION_CREATE = "lms.course_testimonial.create";
    static final String COURSE_TESTIMONIAL_PERMISSION_UPDATE = "lms.course_testimonial.update";
    static final String COURSE_TESTIMONIAL_PERMISSION_DELETE = "lms.course_testimonial.delete";
    /** The contract's CourseTestimonialUpdate fields, in apply order. */
    static final List<String> COURSE_TESTIMONIAL_WRITABLE = List.of("title", "quote", "photoFileId", "currentPosition",
            "sortOrder");

    public static final String ENROLLMENT_REQUEST_AGGREGATE = EnrollmentRequestEntity.TABLE;
    static final String ENROLLMENT_REQUEST_PERMISSION_LIST = "lms.enrollment_request.read";
    static final String ENROLLMENT_REQUEST_PERMISSION_GET = "lms.enrollment_request.read";
    static final String ENROLLMENT_REQUEST_PERMISSION_CREATE = "lms.enrollment_request.create";
    static final String ENROLLMENT_REQUEST_PERMISSION_UPDATE = "lms.enrollment_request.update";
    static final String ENROLLMENT_REQUEST_PERMISSION_DELETE = "lms.enrollment_request.delete";
    static final String PERMISSION_REJECT_ENROLLMENT_REQUEST = "lms.enrollment_request.reject_enrollment_request";
    /** The contract's EnrollmentRequestUpdate fields, in apply order. */
    static final List<String> ENROLLMENT_REQUEST_WRITABLE = List.of("cohortId", "userId", "requestedAt");

    public static final String RELATED_COURSE_AGGREGATE = RelatedCourseEntity.TABLE;
    static final String RELATED_COURSE_PERMISSION_GET = "lms.related_course.read";
    static final String RELATED_COURSE_PERMISSION_CREATE = "lms.related_course.create";
    static final String RELATED_COURSE_PERMISSION_UPDATE = "lms.related_course.update";
    static final String RELATED_COURSE_PERMISSION_DELETE = "lms.related_course.delete";
    /** The contract's RelatedCourseUpdate fields, in apply order. */
    static final List<String> RELATED_COURSE_WRITABLE = List.of("relatedCourseId", "sortOrder", "authorProfileId");

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
            RelatedCourseMapper relatedCourseMapper) {
        super(database, transactions);
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LmsCourse updateLmsCourse(UUID id, String ifMatch, LmsCourseUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LmsCourseEntity entity = loadVisible(scope, id);
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

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LmsCourseEntity loadVisible(DataScope scope, UUID id) {
        LmsCourseEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(LmsCourseEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected LmsCourseUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(LmsCourseEntity entity, LmsCourseUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "shortIntroduction" -> entity.setShortIntroduction(changes.getShortIntroduction());
                case "description" -> entity.setDescription(changes.getDescription());
                case "imageFileId" -> entity.setImageFileId(changes.getImageFileId());
                case "videoUrl" -> entity.setVideoUrl(changes.getVideoUrl());
                case "categoryId" -> entity.setCategoryId(changes.getCategoryId());
                case "publishedOn" -> entity.setPublishedOn(changes.getPublishedOn());
                case "isUpcoming" -> entity.setIsUpcoming(changes.getIsUpcoming());
                case "isFeatured" -> entity.setIsFeatured(changes.getIsFeatured());
                case "disableSelfLearning" -> entity.setDisableSelfLearning(changes.getDisableSelfLearning());
                case "enforceLessonOrder" -> entity.setEnforceLessonOrder(changes.getEnforceLessonOrder());
                case "certificationEnabled" -> entity.setCertificationEnabled(changes.getCertificationEnabled());
                case "paidCertificate" -> entity.setPaidCertificate(changes.getPaidCertificate());
                case "defaultEvaluatorId" -> entity.setDefaultEvaluatorId(changes.getDefaultEvaluatorId());
                case "isPaid" -> entity.setIsPaid(changes.getIsPaid());
                case "price" -> entity.setPrice(WireValues.decimal(changes.getPrice()));
                case "currencyId" -> entity.setCurrencyId(changes.getCurrencyId());
                case "priceUsd" -> entity.setPriceUsd(WireValues.decimal(changes.getPriceUsd()));
                case "cardGradient" -> entity.setCardGradient(Optional.ofNullable(changes.getCardGradient())
                        .map(v -> v.getValue()).orElse(null));
                case "timezone" -> entity.setTimezone(changes.getTimezone());
                case "publishNotificationSent" -> entity.setPublishNotificationSent(
                        changes.getPublishNotificationSent());
                case "enrollmentCount" -> entity.setEnrollmentCount(changes.getEnrollmentCount());
                case "lessonCount" -> entity.setLessonCount(changes.getLessonCount());
                case "averageRating" -> entity.setAverageRating(WireValues.decimal(changes.getAverageRating()));
                case "code" -> entity.setCode(changes.getCode());
                case "stateStandardCode" -> entity.setStateStandardCode(changes.getStateStandardCode());
                case "credits" -> entity.setCredits(WireValues.decimal(changes.getCredits()));
                case "prerequisites" -> entity.setPrerequisites(changes.getPrerequisites());
                case "corequisites" -> entity.setCorequisites(changes.getCorequisites());
                case "effectiveFrom" -> entity.setEffectiveFrom(changes.getEffectiveFrom());
                case "lifecycleStatus" -> entity.setLifecycleStatus(Optional.ofNullable(changes.getLifecycleStatus())
                        .map(v -> v.getValue()).orElse(null));
                case "deactivatedOn" -> entity.setDeactivatedOn(changes.getDeactivatedOn());
                case "versionMajor" -> entity.setVersionMajor(changes.getVersionMajor());
                case "versionMinor" -> entity.setVersionMinor(changes.getVersionMinor());
                case "previousVersionId" -> entity.setPreviousVersionId(changes.getPreviousVersionId());
                case "rootCourseId" -> entity.setRootCourseId(changes.getRootCourseId());
                case "authorComments" -> entity.setAuthorComments(changes.getAuthorComments());
                case "externalRef" -> entity.setExternalRef(changes.getExternalRef());
                case "nominalDuration" -> entity.setNominalDuration(changes.getNominalDuration());
                case "deliveryModality" -> entity.setDeliveryModality(
                        Optional.ofNullable(changes.getDeliveryModality()).map(v -> v.getValue()).orElse(null));
                case "isShared" -> entity.setIsShared(changes.getIsShared());
                case "contentUpdatedAt" -> entity.setContentUpdatedAt(changes.getContentUpdatedAt());
                case "gradeDisplay" -> entity.setGradeDisplay(Optional.ofNullable(changes.getGradeDisplay())
                        .map(v -> v.getValue()).orElse(null));
                case "gradingSchemeId" -> entity.setGradingSchemeId(changes.getGradingSchemeId());
                case "isInCatalog" -> entity.setIsInCatalog(changes.getIsInCatalog());
                case "catalogDescription" -> entity.setCatalogDescription(changes.getCatalogDescription());
                case "catalogVisibleFrom" -> entity.setCatalogVisibleFrom(changes.getCatalogVisibleFrom());
                case "catalogVisibleUntil" -> entity.setCatalogVisibleUntil(changes.getCatalogVisibleUntil());
                case "outline" -> entity.setOutline(changes.getOutline());
                case "workloadHours" -> entity.setWorkloadHours(changes.getWorkloadHours());
                case "catalogStatus" -> entity.setCatalogStatus(Optional.ofNullable(changes.getCatalogStatus())
                        .map(v -> v.getValue()).orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CatalogBanner updateCatalogBanner(UUID courseId, UUID id, String ifMatch, CatalogBannerUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CATALOG_BANNER_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CATALOG_BANNER_PERMISSION_UPDATE);
            CatalogBannerEntity entity = loadVisibleCatalogBanner(scope, courseId, id);
            scope.requireWritable(institutionOfCatalogBanner(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCatalogBanner(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCatalogBanner(CatalogBannerEntity entity) {
        return catalogBannerRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CatalogBannerUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCatalogBanner(CatalogBannerEntity entity, CatalogBannerUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "placement" -> entity.setPlacement(Optional.ofNullable(changes.getPlacement())
                        .map(v -> v.getValue()).orElse(null));
                case "imageFileId" -> entity.setImageFileId(changes.getImageFileId());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Chapter updateChapter(UUID courseId, UUID id, String ifMatch, ChapterUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CHAPTER_WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("dueTime", changes.getDueTime(), wire);
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
            applyChapter(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfChapter(ChapterEntity entity) {
        return chapterRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected ChapterUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyChapter(ChapterEntity entity, ChapterUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "isScormPackage" -> entity.setIsScormPackage(changes.getIsScormPackage());
                case "scormPackageFileId" -> entity.setScormPackageFileId(changes.getScormPackageFileId());
                case "scormPackagePath" -> entity.setScormPackagePath(changes.getScormPackagePath());
                case "scormManifestFile" -> entity.setScormManifestFile(changes.getScormManifestFile());
                case "scormLaunchFile" -> entity.setScormLaunchFile(changes.getScormLaunchFile());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "parentChapterId" -> entity.setParentChapterId(changes.getParentChapterId());
                case "description" -> entity.setDescription(changes.getDescription());
                case "chapterNumber" -> entity.setChapterNumber(changes.getChapterNumber());
                case "startDay" -> entity.setStartDay(changes.getStartDay());
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "dueDay" -> entity.setDueDay(changes.getDueDay());
                case "dueTime" -> entity.setDueTime(WireValues.time(changes.getDueTime()));
                case "startsAt" -> entity.setStartsAt(changes.getStartsAt());
                case "endsAt" -> entity.setEndsAt(changes.getEndsAt());
                case "allowDelete" -> entity.setAllowDelete(changes.getAllowDelete());
                case "allowEditDetails" -> entity.setAllowEditDetails(changes.getAllowEditDetails());
                case "allowEditDates" -> entity.setAllowEditDates(changes.getAllowEditDates());
                case "copiedFromId" -> entity.setCopiedFromId(changes.getCopiedFromId());
                case "isOriginal" -> entity.setIsOriginal(changes.getIsOriginal());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CourseAuthorProfile updateCourseAuthorProfile(UUID courseId, UUID id, String ifMatch,
            CourseAuthorProfileUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COURSE_AUTHOR_PROFILE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_AUTHOR_PROFILE_PERMISSION_UPDATE);
            CourseAuthorProfileEntity entity = loadVisibleCourseAuthorProfile(scope, courseId, id);
            scope.requireWritable(institutionOfCourseAuthorProfile(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCourseAuthorProfile(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCourseAuthorProfile(CourseAuthorProfileEntity entity) {
        return courseAuthorProfileRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CourseAuthorProfileUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyCourseAuthorProfile(CourseAuthorProfileEntity entity, CourseAuthorProfileUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "name" -> entity.setName(changes.getName());
                case "bio" -> entity.setBio(changes.getBio());
                case "photoFileId" -> entity.setPhotoFileId(changes.getPhotoFileId());
                case "qualification" -> entity.setQualification(changes.getQualification());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CourseInterest updateCourseInterest(UUID courseId, UUID id, String ifMatch, CourseInterestUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COURSE_INTEREST_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_INTEREST_PERMISSION_UPDATE);
            CourseInterestEntity entity = loadVisibleCourseInterest(scope, courseId, id);
            scope.requireWritable(institutionOfCourseInterest(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCourseInterest(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCourseInterest(CourseInterestEntity entity) {
        return courseInterestRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CourseInterestUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCourseInterest(CourseInterestEntity entity, CourseInterestUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "emailSent" -> entity.setEmailSent(changes.getEmailSent());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CourseMentor updateCourseMentor(UUID courseId, UUID id, String ifMatch, CourseMentorUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COURSE_MENTOR_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_MENTOR_PERMISSION_UPDATE);
            CourseMentorEntity entity = loadVisibleCourseMentor(scope, courseId, id);
            scope.requireWritable(institutionOfCourseMentor(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCourseMentor(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCourseMentor(CourseMentorEntity entity) {
        return courseMentorRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CourseMentorUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCourseMentor(CourseMentorEntity entity, CourseMentorUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CoursePreview updateCoursePreview(UUID courseId, UUID id, String ifMatch, CoursePreviewUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COURSE_PREVIEW_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_PREVIEW_PERMISSION_UPDATE);
            CoursePreviewEntity entity = loadVisibleCoursePreview(scope, courseId, id);
            scope.requireWritable(institutionOfCoursePreview(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCoursePreview(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCoursePreview(CoursePreviewEntity entity) {
        return coursePreviewRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CoursePreviewUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCoursePreview(CoursePreviewEntity entity, CoursePreviewUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "previewKind" -> entity.setPreviewKind(Optional.ofNullable(changes.getPreviewKind())
                        .map(v -> v.getValue()).orElse(null));
                case "fileId" -> entity.setFileId(changes.getFileId());
                case "icon" -> entity.setIcon(changes.getIcon());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CourseReview updateCourseReview(UUID courseId, UUID id, String ifMatch, CourseReviewUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COURSE_REVIEW_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_REVIEW_PERMISSION_UPDATE);
            CourseReviewEntity entity = loadVisibleCourseReview(scope, courseId, id);
            scope.requireWritable(institutionOfCourseReview(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCourseReview(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCourseReview(CourseReviewEntity entity) {
        return courseReviewRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CourseReviewUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCourseReview(CourseReviewEntity entity, CourseReviewUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "rating" -> entity.setRating(WireValues.decimal(changes.getRating()));
                case "review" -> entity.setReview(changes.getReview());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CourseTeacher updateCourseTeacher(UUID courseId, UUID id, String ifMatch, CourseTeacherUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COURSE_TEACHER_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_TEACHER_PERMISSION_UPDATE);
            CourseTeacherEntity entity = loadVisibleCourseTeacher(scope, courseId, id);
            scope.requireWritable(institutionOfCourseTeacher(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCourseTeacher(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCourseTeacher(CourseTeacherEntity entity) {
        return courseTeacherRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CourseTeacherUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyCourseTeacher(CourseTeacherEntity entity, CourseTeacherUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CourseTestimonial updateCourseTestimonial(UUID courseId, UUID id, String ifMatch,
            CourseTestimonialUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, COURSE_TESTIMONIAL_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_TESTIMONIAL_PERMISSION_UPDATE);
            CourseTestimonialEntity entity = loadVisibleCourseTestimonial(scope, courseId, id);
            scope.requireWritable(institutionOfCourseTestimonial(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyCourseTestimonial(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfCourseTestimonial(CourseTestimonialEntity entity) {
        return courseTestimonialRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected CourseTestimonialUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyCourseTestimonial(CourseTestimonialEntity entity, CourseTestimonialUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "quote" -> entity.setQuote(changes.getQuote());
                case "photoFileId" -> entity.setPhotoFileId(changes.getPhotoFileId());
                case "currentPosition" -> entity.setCurrentPosition(changes.getCurrentPosition());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EnrollmentRequest updateEnrollmentRequest(UUID courseId, UUID id, String ifMatch,
            EnrollmentRequestUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ENROLLMENT_REQUEST_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ENROLLMENT_REQUEST_PERMISSION_UPDATE);
            EnrollmentRequestEntity entity = loadVisibleEnrollmentRequest(scope, courseId, id);
            scope.requireWritable(institutionOfEnrollmentRequest(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEnrollmentRequest(entity, changes, fields);
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
     * lms.RejectEnrollmentRequest: Reject. The row must be visible and writable for the caller, and the step is allowed
     * while status is pending; in any other state
     * the answer is 409 and nothing changes.
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfEnrollmentRequest(EnrollmentRequestEntity entity) {
        return enrollmentRequestRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected EnrollmentRequestUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyEnrollmentRequest(EnrollmentRequestEntity entity, EnrollmentRequestUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "userId" -> entity.setUserId(changes.getUserId());
                case "requestedAt" -> entity.setRequestedAt(changes.getRequestedAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public RelatedCourse updateRelatedCourse(UUID courseId, UUID id, String ifMatch, RelatedCourseUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, RELATED_COURSE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELATED_COURSE_PERMISSION_UPDATE);
            RelatedCourseEntity entity = loadVisibleRelatedCourse(scope, courseId, id);
            scope.requireWritable(institutionOfRelatedCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyRelatedCourse(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfRelatedCourse(RelatedCourseEntity entity) {
        return relatedCourseRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected RelatedCourseUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyRelatedCourse(RelatedCourseEntity entity, RelatedCourseUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "relatedCourseId" -> entity.setRelatedCourseId(changes.getRelatedCourseId());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "authorProfileId" -> entity.setAuthorProfileId(changes.getAuthorProfileId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
