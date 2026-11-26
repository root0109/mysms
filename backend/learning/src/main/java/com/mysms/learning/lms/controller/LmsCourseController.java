package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.business.CoursePackageService;
import com.mysms.learning.lms.model.CatalogPage;
import com.mysms.learning.lms.business.CatalogService;
import com.mysms.learning.lms.business.EnrollmentService;
import com.mysms.learning.lms.api.model.EvaluatorSlot;
import com.mysms.learning.lms.api.model.ProgressBucket;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.LmsCourseApi;
import com.mysms.learning.lms.api.model.ApproveCourseRequest;
import com.mysms.learning.lms.api.model.ApproveEnrollmentRequestRequest;
import com.mysms.learning.lms.api.model.BatchEnrollInCourseRequest;
import com.mysms.learning.lms.api.model.BatchEnrollPreviewList;
import com.mysms.learning.lms.api.model.CatalogBanner;
import com.mysms.learning.lms.api.model.CatalogBannerUpdate;
import com.mysms.learning.lms.api.model.CatalogHome;
import com.mysms.learning.lms.api.model.Certificate;
import com.mysms.learning.lms.api.model.Chapter;
import com.mysms.learning.lms.api.model.ChapterList;
import com.mysms.learning.lms.api.model.ChapterUpdate;
import com.mysms.learning.lms.api.model.CourseAuthorProfile;
import com.mysms.learning.lms.api.model.CourseAuthorProfileUpdate;
import com.mysms.learning.lms.api.model.CourseInterest;
import com.mysms.learning.lms.api.model.CourseInterestUpdate;
import com.mysms.learning.lms.api.model.CourseMentor;
import com.mysms.learning.lms.api.model.CourseMentorUpdate;
import com.mysms.learning.lms.api.model.CourseOutline;
import com.mysms.learning.lms.api.model.CoursePreview;
import com.mysms.learning.lms.api.model.CoursePreviewUpdate;
import com.mysms.learning.lms.api.model.CourseReview;
import com.mysms.learning.lms.api.model.CourseReviewUpdate;
import com.mysms.learning.lms.api.model.CourseTeacher;
import com.mysms.learning.lms.api.model.CourseTeacherUpdate;
import com.mysms.learning.lms.api.model.CourseTestimonial;
import com.mysms.learning.lms.api.model.CourseTestimonialUpdate;
import com.mysms.learning.lms.api.model.CreateCourseVersionRequest;
import com.mysms.learning.lms.api.model.DeactivateCourseRequest;
import com.mysms.learning.lms.api.model.Dependencies;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.api.model.EnrollInCourseRequest;
import com.mysms.learning.lms.api.model.Enrollment;
import com.mysms.learning.lms.api.model.EnrollmentRequest;
import com.mysms.learning.lms.api.model.EnrollmentRequestUpdate;
import com.mysms.learning.lms.api.model.HideCatalogListingRequest;
import com.mysms.learning.lms.api.model.LessonProgress;
import com.mysms.learning.lms.api.model.LmsCourse;
import com.mysms.learning.lms.api.model.LmsCourseUpdate;
import com.mysms.learning.lms.api.model.MoveChapterRequest;
import com.mysms.learning.lms.api.model.ImportCoursePackageRequest;
import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.api.model.PublishCatalogListingRequest;
import com.mysms.learning.lms.api.model.PublishCourseRequest;
import com.mysms.learning.lms.api.model.ReactivateCourseRequest;
import com.mysms.learning.lms.api.model.RejectEnrollmentRequestRequest;
import com.mysms.learning.lms.api.model.RelatedCourse;
import com.mysms.learning.lms.api.model.RelatedCourseUpdate;
import com.mysms.learning.lms.api.model.ReorderChaptersRequest;
import com.mysms.learning.lms.api.model.SaveScormProgressRequest;
import com.mysms.learning.lms.api.model.ScormLaunch;
import com.mysms.learning.lms.api.model.SubmitCourseForReviewRequest;
import com.mysms.learning.lms.api.model.TeacherDashboard;
import com.mysms.learning.lms.api.model.UnpublishCourseRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.LmsCourseService;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LmsCourse operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class LmsCourseController implements LmsCourseApi {

    private final LmsCourseService lmsCourseService;
    private final CoursePackageService coursePackageService;
    private final EnrollmentService enrollmentService;
    private final CatalogService catalogService;

    public LmsCourseController(LmsCourseService lmsCourseService, CoursePackageService coursePackageService,
            EnrollmentService enrollmentService, CatalogService catalogService) {
        this.coursePackageService = coursePackageService;
        this.lmsCourseService = lmsCourseService;
        this.enrollmentService = enrollmentService;
        this.catalogService = catalogService;
    }

    @Override
    public ResponseEntity<LmsCourse> approveCourse(UUID courseId,
            ApproveCourseRequest approveCourseRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.approveCourse(courseId, approveCourseRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<EnrollmentRequest> approveEnrollmentRequest(UUID courseId,
            UUID enrollmentRequestId, ApproveEnrollmentRequestRequest approveEnrollmentRequestRequest,
            UUID idempotencyKey) {
        EnrollmentRequest approved = lmsCourseService.approveEnrollmentRequest(courseId, enrollmentRequestId,
                approveEnrollmentRequestRequest);
        return ResponseEntity.ok().eTag(approved.getEtag()).body(approved);
    }

    @Override
    public ResponseEntity<BatchEnrollPreviewList> batchEnrollInCourse(UUID courseId, UUID idempotencyKey,
            BatchEnrollInCourseRequest batchEnrollInCourseRequest) {
        return ResponseEntity.ok(lmsCourseService.batchEnrollInCourse(courseId, batchEnrollInCourseRequest));
    }

    @Override
    public ResponseEntity<Dependencies> checkCourseDependencies(UUID courseId) {
        return ResponseEntity.ok(lmsCourseService.checkCourseDependencies(courseId));
    }

    @Override
    public ResponseEntity<PagingInfo<LmsCourse>> compareCourses(List<UUID> courseIds, Integer page, Integer size,
            String cursor) {
        PagingInfo<LmsCourse> list = PagingUtil.pagingInfo(catalogService.compareCourses(courseIds, page, size, cursor));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CatalogBanner> createCatalogBanner(UUID courseId, CatalogBanner catalogBanner,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CatalogBanner created = lmsCourseService.createCatalogBanner(courseId, catalogBanner, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/banners/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Chapter> createChapter(UUID courseId, Chapter chapter, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Chapter created = lmsCourseService.createChapter(courseId, chapter, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/chapters/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CourseAuthorProfile> createCourseAuthorProfile(UUID courseId,
            CourseAuthorProfile courseAuthorProfile, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CourseAuthorProfile created =
                lmsCourseService.createCourseAuthorProfile(courseId, courseAuthorProfile, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/courses/" + courseId + "/author-profiles/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CourseInterest> createCourseInterest(UUID courseId,
            CourseInterest courseInterest, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CourseInterest created = lmsCourseService.createCourseInterest(courseId, courseInterest, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/interests/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CourseMentor> createCourseMentor(UUID courseId, CourseMentor courseMentor,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CourseMentor created = lmsCourseService.createCourseMentor(courseId, courseMentor, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/mentors/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CoursePreview> createCoursePreview(UUID courseId, CoursePreview coursePreview,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CoursePreview created = lmsCourseService.createCoursePreview(courseId, coursePreview, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/previews/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CourseReview> createCourseReview(UUID courseId, CourseReview courseReview,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CourseReview created = lmsCourseService.createCourseReview(courseId, courseReview, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/reviews/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CourseTeacher> createCourseTeacher(UUID courseId, CourseTeacher courseTeacher,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CourseTeacher created = lmsCourseService.createCourseTeacher(courseId, courseTeacher, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/teachers/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CourseTestimonial> createCourseTestimonial(UUID courseId,
            CourseTestimonial courseTestimonial, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CourseTestimonial created =
                lmsCourseService.createCourseTestimonial(courseId, courseTestimonial, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + courseId + "/testimonials/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<LmsCourse> createCourseVersion(UUID courseId,
            CreateCourseVersionRequest createCourseVersionRequest, UUID idempotencyKey) {
        LmsCourse created = lmsCourseService.createCourseVersion(courseId, createCourseVersionRequest);
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<EnrollmentRequest> createEnrollmentRequest(UUID courseId,
            EnrollmentRequest enrollmentRequest, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EnrollmentRequest created =
                lmsCourseService.createEnrollmentRequest(courseId, enrollmentRequest, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/courses/" + courseId + "/enrollment-requests/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<LmsCourse> createLmsCourse(LmsCourse lmsCourse, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LmsCourse created = lmsCourseService.createLmsCourse(lmsCourse, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/courses/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<RelatedCourse> createRelatedCourse(UUID courseId, RelatedCourse relatedCourse,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RelatedCourse created = lmsCourseService.createRelatedCourse(courseId, relatedCourse, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/courses/" + courseId + "/related-courses/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<LmsCourse> deactivateCourse(UUID courseId,
            DeactivateCourseRequest deactivateCourseRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.deactivateCourse(courseId, deactivateCourseRequest.getEtag(),
                deactivateCourseRequest.getReason());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> deleteCatalogBanner(UUID courseId, UUID catalogBannerId, String ifMatch) {
        lmsCourseService.deleteCatalogBanner(courseId, catalogBannerId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteChapter(UUID courseId, UUID chapterId, String ifMatch) {
        lmsCourseService.deleteChapter(courseId, chapterId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCourseAuthorProfile(UUID courseId, UUID courseAuthorProfileId,
            String ifMatch) {
        lmsCourseService.deleteCourseAuthorProfile(courseId, courseAuthorProfileId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCourseInterest(UUID courseId, UUID courseInterestId,
            String ifMatch) {
        lmsCourseService.deleteCourseInterest(courseId, courseInterestId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCourseMentor(UUID courseId, UUID courseMentorId, String ifMatch) {
        lmsCourseService.deleteCourseMentor(courseId, courseMentorId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCoursePreview(UUID courseId, UUID coursePreviewId, String ifMatch) {
        lmsCourseService.deleteCoursePreview(courseId, coursePreviewId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCourseReview(UUID courseId, UUID courseReviewId, String ifMatch) {
        lmsCourseService.deleteCourseReview(courseId, courseReviewId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCourseTeacher(UUID courseId, UUID courseTeacherId, String ifMatch) {
        lmsCourseService.deleteCourseTeacher(courseId, courseTeacherId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCourseTestimonial(UUID courseId, UUID courseTestimonialId,
            String ifMatch) {
        lmsCourseService.deleteCourseTestimonial(courseId, courseTestimonialId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEnrollmentRequest(UUID courseId, UUID enrollmentRequestId,
            String ifMatch) {
        lmsCourseService.deleteEnrollmentRequest(courseId, enrollmentRequestId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteLmsCourse(UUID courseId, String ifMatch) {
        lmsCourseService.deleteLmsCourse(courseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRelatedCourse(UUID courseId, UUID relatedCourseId, String ifMatch) {
        lmsCourseService.deleteRelatedCourse(courseId, relatedCourseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Enrollment> enrollInCourse(UUID courseId,
            EnrollInCourseRequest enrollInCourseRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(enrollmentService.enrollInCourse(courseId));
    }

    @Override
    public ResponseEntity<Operation> exportCourse(UUID courseId, Object body, UUID idempotencyKey) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(coursePackageService.exportCourse(courseId));
    }

    @Override
    public ResponseEntity<Operation> importCoursePackage(UUID courseId,
            ImportCoursePackageRequest importCoursePackageRequest, UUID idempotencyKey) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(coursePackageService.importCoursePackage(courseId, importCoursePackageRequest));
    }

    @Override
    public ResponseEntity<CatalogBanner> getCatalogBanner(UUID courseId, UUID catalogBannerId,
            String fields) {
        CatalogBanner found = lmsCourseService.getCatalogBanner(courseId, catalogBannerId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CatalogHome> getCatalogHome(UUID categoryId, String query, Integer page,
            Integer size, String cursor) {
        CatalogPage found = catalogService.getCatalogHome(categoryId, query, page, size, cursor);
        PagingInfo<LmsCourse> courses = PagingUtil.pagingInfo(found.courses());
        return ResponseEntity.ok(found.home().courses(courses.data()).paging(courses.paging()));
    }

    @Override
    public ResponseEntity<Chapter> getChapter(UUID courseId, UUID chapterId, String fields) {
        Chapter found = lmsCourseService.getChapter(courseId, chapterId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CourseAuthorProfile> getCourseAuthorProfile(UUID courseId,
            UUID courseAuthorProfileId, String fields) {
        CourseAuthorProfile found = lmsCourseService.getCourseAuthorProfile(courseId, courseAuthorProfileId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CourseInterest> getCourseInterest(UUID courseId, UUID courseInterestId,
            String fields) {
        CourseInterest found = lmsCourseService.getCourseInterest(courseId, courseInterestId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CourseMentor> getCourseMentor(UUID courseId, UUID courseMentorId,
            String fields) {
        CourseMentor found = lmsCourseService.getCourseMentor(courseId, courseMentorId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CourseOutline> getCourseOutline(UUID courseId) {
        return ResponseEntity.ok(lmsCourseService.getCourseOutline(courseId));
    }

    @Override
    public ResponseEntity<CoursePreview> getCoursePreview(UUID courseId, UUID coursePreviewId,
            String fields) {
        CoursePreview found = lmsCourseService.getCoursePreview(courseId, coursePreviewId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ProgressBucket>> getCourseProgressDistribution(UUID courseId, Integer page,
            Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(lmsCourseService.getCourseProgressDistribution(courseId)));
    }

    @Override
    public ResponseEntity<CourseReview> getCourseReview(UUID courseId, UUID courseReviewId,
            String fields) {
        CourseReview found = lmsCourseService.getCourseReview(courseId, courseReviewId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<DownloadLink> getCourseSyllabus(UUID courseId, String format) {
        return ResponseEntity.ok(lmsCourseService.getCourseSyllabus(courseId, format));
    }

    @Override
    public ResponseEntity<CourseTeacher> getCourseTeacher(UUID courseId, UUID courseTeacherId,
            String fields) {
        CourseTeacher found = lmsCourseService.getCourseTeacher(courseId, courseTeacherId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CourseTestimonial> getCourseTestimonial(UUID courseId, UUID courseTestimonialId,
            String fields) {
        CourseTestimonial found = lmsCourseService.getCourseTestimonial(courseId, courseTestimonialId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EnrollmentRequest> getEnrollmentRequest(UUID courseId, UUID enrollmentRequestId,
            String fields) {
        EnrollmentRequest found = lmsCourseService.getEnrollmentRequest(courseId, enrollmentRequestId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<EvaluatorSlot>> getEvaluatorSlots(UUID courseId, LocalDate fromDate,
            LocalDate toDate, UUID cohortId, Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(lmsCourseService.getEvaluatorSlots(courseId, fromDate, toDate,
                page, size)));
    }

    @Override
    public ResponseEntity<LmsCourse> getLmsCourse(UUID courseId, String fields) {
        LmsCourse found = lmsCourseService.getLmsCourse(courseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RelatedCourse> getRelatedCourse(UUID courseId, UUID relatedCourseId,
            String fields) {
        RelatedCourse found = lmsCourseService.getRelatedCourse(courseId, relatedCourseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ScormLaunch> getScormLaunch(UUID chapterId) {
        return ResponseEntity.ok(lmsCourseService.getScormLaunch(chapterId));
    }

    @Override
    public ResponseEntity<TeacherDashboard> getTeacherDashboard() {
        return ResponseEntity.ok(lmsCourseService.getTeacherDashboard());
    }

    @Override
    public ResponseEntity<LmsCourse> hideCatalogListing(UUID courseId,
            HideCatalogListingRequest hideCatalogListingRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.hideCatalogListing(courseId, hideCatalogListingRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Certificate> issueMyCertificate(UUID courseId, Object body,
            UUID idempotencyKey) {
        Certificate issued = lmsCourseService.issueMyCertificate(courseId);
        return ResponseEntity.ok().eTag(issued.getEtag()).body(issued);
    }

    @Override
    public ResponseEntity<PagingInfo<CatalogBanner>> listCatalogBanners(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CatalogBanner> list =
                PagingUtil.pagingInfo(lmsCourseService.listCatalogBanners(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Chapter>> listChapters(UUID courseId, Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Chapter> list =
                PagingUtil.pagingInfo(lmsCourseService.listChapters(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CourseAuthorProfile>> listCourseAuthorProfiles(UUID courseId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CourseAuthorProfile> list =
                PagingUtil.pagingInfo(lmsCourseService.listCourseAuthorProfiles(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CourseInterest>> listCourseInterests(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CourseInterest> list =
                PagingUtil.pagingInfo(lmsCourseService.listCourseInterests(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CourseMentor>> listCourseMentors(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CourseMentor> list =
                PagingUtil.pagingInfo(lmsCourseService.listCourseMentors(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CoursePreview>> listCoursePreviews(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CoursePreview> list =
                PagingUtil.pagingInfo(lmsCourseService.listCoursePreviews(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CourseReview>> listCourseReviews(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CourseReview> list =
                PagingUtil.pagingInfo(lmsCourseService.listCourseReviews(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CourseTeacher>> listCourseTeachers(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CourseTeacher> list =
                PagingUtil.pagingInfo(lmsCourseService.listCourseTeachers(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CourseTestimonial>> listCourseTestimonials(UUID courseId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CourseTestimonial> list =
                PagingUtil.pagingInfo(lmsCourseService.listCourseTestimonials(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<EnrollmentRequest>> listEnrollmentRequests(UUID courseId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EnrollmentRequest> list =
                PagingUtil.pagingInfo(lmsCourseService.listEnrollmentRequests(courseId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<LmsCourse>> listLmsCourses(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LmsCourse> list =
                PagingUtil.pagingInfo(lmsCourseService.listLmsCourses(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<RelatedCourse>> listRelatedCourses(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RelatedCourse> list =
                PagingUtil.pagingInfo(lmsCourseService.listRelatedCourses(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Chapter> moveChapter(UUID chapterId, MoveChapterRequest moveChapterRequest,
            UUID idempotencyKey) {
        Chapter moved = lmsCourseService.moveChapter(chapterId, moveChapterRequest);
        return ResponseEntity.ok().eTag(moved.getEtag()).body(moved);
    }

    @Override
    public ResponseEntity<LmsCourse> publishCatalogListing(UUID courseId,
            PublishCatalogListingRequest publishCatalogListingRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.publishCatalogListing(courseId, publishCatalogListingRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LmsCourse> publishCourse(UUID courseId,
            PublishCourseRequest publishCourseRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.publishCourse(courseId, publishCourseRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LmsCourse> reactivateCourse(UUID courseId,
            ReactivateCourseRequest reactivateCourseRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.reactivateCourse(courseId, reactivateCourseRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<EnrollmentRequest> rejectEnrollmentRequest(UUID courseId,
            UUID enrollmentRequestId, RejectEnrollmentRequestRequest rejectEnrollmentRequestRequest,
            UUID idempotencyKey) {
        EnrollmentRequest changed =
                lmsCourseService.rejectEnrollmentRequest(courseId, enrollmentRequestId,
                        rejectEnrollmentRequestRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ChapterList> reorderChapters(UUID courseId,
            ReorderChaptersRequest reorderChaptersRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(lmsCourseService.reorderChapters(courseId, reorderChaptersRequest));
    }

    @Override
    public ResponseEntity<LessonProgress> saveScormProgress(UUID chapterId,
            SaveScormProgressRequest saveScormProgressRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(lmsCourseService.saveScormProgress(chapterId, saveScormProgressRequest));
    }

    @Override
    public ResponseEntity<LmsCourse> submitCourseForReview(UUID courseId,
            SubmitCourseForReviewRequest submitCourseForReviewRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.submitCourseForReview(courseId, submitCourseForReviewRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LmsCourse> unpublishCourse(UUID courseId,
            UnpublishCourseRequest unpublishCourseRequest, UUID idempotencyKey) {
        LmsCourse changed = lmsCourseService.unpublishCourse(courseId, unpublishCourseRequest.getEtag());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<CatalogBanner> updateCatalogBanner(UUID courseId, UUID catalogBannerId,
            String ifMatch, CatalogBannerUpdate catalogBannerUpdate, Boolean validateOnly) {
        CatalogBanner updated =
                lmsCourseService.updateCatalogBanner(courseId, catalogBannerId, ifMatch, catalogBannerUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Chapter> updateChapter(UUID courseId, UUID chapterId, String ifMatch,
            ChapterUpdate chapterUpdate, Boolean validateOnly) {
        Chapter updated = lmsCourseService.updateChapter(courseId, chapterId, ifMatch, chapterUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CourseAuthorProfile> updateCourseAuthorProfile(UUID courseId,
            UUID courseAuthorProfileId, String ifMatch, CourseAuthorProfileUpdate courseAuthorProfileUpdate,
            Boolean validateOnly) {
        CourseAuthorProfile updated =
                lmsCourseService.updateCourseAuthorProfile(courseId, courseAuthorProfileId, ifMatch,
                        courseAuthorProfileUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CourseInterest> updateCourseInterest(UUID courseId, UUID courseInterestId,
            String ifMatch, CourseInterestUpdate courseInterestUpdate, Boolean validateOnly) {
        CourseInterest updated =
                lmsCourseService.updateCourseInterest(courseId, courseInterestId, ifMatch, courseInterestUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CourseMentor> updateCourseMentor(UUID courseId, UUID courseMentorId,
            String ifMatch, CourseMentorUpdate courseMentorUpdate, Boolean validateOnly) {
        CourseMentor updated =
                lmsCourseService.updateCourseMentor(courseId, courseMentorId, ifMatch, courseMentorUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CoursePreview> updateCoursePreview(UUID courseId, UUID coursePreviewId,
            String ifMatch, CoursePreviewUpdate coursePreviewUpdate, Boolean validateOnly) {
        CoursePreview updated =
                lmsCourseService.updateCoursePreview(courseId, coursePreviewId, ifMatch, coursePreviewUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CourseReview> updateCourseReview(UUID courseId, UUID courseReviewId,
            String ifMatch, CourseReviewUpdate courseReviewUpdate, Boolean validateOnly) {
        CourseReview updated =
                lmsCourseService.updateCourseReview(courseId, courseReviewId, ifMatch, courseReviewUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CourseTeacher> updateCourseTeacher(UUID courseId, UUID courseTeacherId,
            String ifMatch, CourseTeacherUpdate courseTeacherUpdate, Boolean validateOnly) {
        CourseTeacher updated =
                lmsCourseService.updateCourseTeacher(courseId, courseTeacherId, ifMatch, courseTeacherUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CourseTestimonial> updateCourseTestimonial(UUID courseId,
            UUID courseTestimonialId, String ifMatch, CourseTestimonialUpdate courseTestimonialUpdate,
            Boolean validateOnly) {
        CourseTestimonial updated =
                lmsCourseService.updateCourseTestimonial(courseId, courseTestimonialId, ifMatch,
                        courseTestimonialUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EnrollmentRequest> updateEnrollmentRequest(UUID courseId,
            UUID enrollmentRequestId, String ifMatch, EnrollmentRequestUpdate enrollmentRequestUpdate,
            Boolean validateOnly) {
        EnrollmentRequest updated =
                lmsCourseService.updateEnrollmentRequest(courseId, enrollmentRequestId, ifMatch,
                        enrollmentRequestUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<LmsCourse> updateLmsCourse(UUID courseId, String ifMatch,
            LmsCourseUpdate lmsCourseUpdate, Boolean validateOnly) {
        LmsCourse updated = lmsCourseService.updateLmsCourse(courseId, ifMatch, lmsCourseUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RelatedCourse> updateRelatedCourse(UUID courseId, UUID relatedCourseId,
            String ifMatch, RelatedCourseUpdate relatedCourseUpdate, Boolean validateOnly) {
        RelatedCourse updated =
                lmsCourseService.updateRelatedCourse(courseId, relatedCourseId, ifMatch, relatedCourseUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
