// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.learning.lms.api.model.EvaluatorSlot;
import com.mysms.learning.lms.api.model.ProgressBucket;
import com.mysms.core.paging.PagingInfo;
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
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import com.mysms.learning.lms.api.model.MoveChapterRequest;
import org.springframework.lang.Nullable;
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
import java.util.UUID;
import com.mysms.learning.lms.api.model.UnpublishCourseRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
public interface LmsCourseApi {

    String PATH_APPROVE_COURSE = "/v1/lms/courses/{courseId}:approve";
    /**
     * POST /v1/lms/courses/{courseId}:approve : Approve
     * Approve.
     *
     * @param courseId  (required)
     * @param approveCourseRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_APPROVE_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> approveCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody ApproveCourseRequest approveCourseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_APPROVE_ENROLLMENT_REQUEST = "/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:approve";
    /**
     * POST /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:approve : Approve and enrol in a cohort
     * Approve and enrol in a cohort.
     *
     * @param courseId  (required)
     * @param enrollmentRequestId  (required)
     * @param approveEnrollmentRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_APPROVE_ENROLLMENT_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EnrollmentRequest> approveEnrollmentRequest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("enrollmentRequestId") UUID enrollmentRequestId,
         @Valid @RequestBody ApproveEnrollmentRequestRequest approveEnrollmentRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_BATCH_ENROLL_IN_COURSE = "/v1/lms/courses/{courseId}:batchEnroll";
    /**
     * POST /v1/lms/courses/{courseId}:batchEnroll : Enrol many students (or preview)
     * Enrol many students (or preview).
     *
     * @param courseId  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param batchEnrollInCourseRequest  (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_BATCH_ENROLL_IN_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<BatchEnrollPreviewList> batchEnrollInCourse(
         @PathVariable("courseId") UUID courseId,
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody BatchEnrollInCourseRequest batchEnrollInCourseRequest
    );


    String PATH_CHECK_COURSE_DEPENDENCIES = "/v1/lms/courses/{courseId}:dependencies";
    /**
     * GET /v1/lms/courses/{courseId}:dependencies : What prevents deleting the course (cohorts, newer versions, catalogue listing)
     * What prevents deleting the course (cohorts, newer versions, catalogue listing).
     *
     * @param courseId  (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_CHECK_COURSE_DEPENDENCIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Dependencies> checkCourseDependencies(
         @PathVariable("courseId") UUID courseId
    );


    String PATH_COMPARE_COURSES = "/v1/lms/catalog:compare";
    /**
     * GET /v1/lms/catalog:compare : Side-by-side catalogue details of several courses
     * Side-by-side catalogue details of several courses. Public.
     *
     * @param courseIds Courses to compare: at least 2 and at most 4 (INVALID_ARGUMENT otherwise). (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_COMPARE_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LmsCourse>> compareCourses(
        @NotNull  @RequestParam(value = "courseIds", required = true) List<UUID> courseIds,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_CREATE_CATALOG_BANNER = "/v1/lms/courses/{courseId}/banners";
    /**
     * POST /v1/lms/courses/{courseId}/banners : Create CatalogBanner
     * Create a CatalogBanner. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param catalogBanner  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_CATALOG_BANNER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CatalogBanner> createCatalogBanner(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CatalogBanner catalogBanner,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CHAPTER = "/v1/lms/courses/{courseId}/chapters";
    /**
     * POST /v1/lms/courses/{courseId}/chapters : Create Chapter
     * Create a Chapter. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param chapter  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_CHAPTER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Chapter> createChapter(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody Chapter chapter,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_AUTHOR_PROFILE = "/v1/lms/courses/{courseId}/author-profiles";
    /**
     * POST /v1/lms/courses/{courseId}/author-profiles : Create CourseAuthorProfile
     * Create a CourseAuthorProfile. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseAuthorProfile  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_AUTHOR_PROFILE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseAuthorProfile> createCourseAuthorProfile(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CourseAuthorProfile courseAuthorProfile,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_INTEREST = "/v1/lms/courses/{courseId}/interests";
    /**
     * POST /v1/lms/courses/{courseId}/interests : Create CourseInterest
     * Create a CourseInterest. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseInterest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_INTEREST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseInterest> createCourseInterest(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CourseInterest courseInterest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_MENTOR = "/v1/lms/courses/{courseId}/mentors";
    /**
     * POST /v1/lms/courses/{courseId}/mentors : Create CourseMentor
     * Create a CourseMentor. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseMentor  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_MENTOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseMentor> createCourseMentor(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CourseMentor courseMentor,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_PREVIEW = "/v1/lms/courses/{courseId}/previews";
    /**
     * POST /v1/lms/courses/{courseId}/previews : Create CoursePreview
     * Create a CoursePreview. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param coursePreview  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_PREVIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CoursePreview> createCoursePreview(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CoursePreview coursePreview,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_REVIEW = "/v1/lms/courses/{courseId}/reviews";
    /**
     * POST /v1/lms/courses/{courseId}/reviews : Create CourseReview
     * Create a CourseReview. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseReview  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_REVIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseReview> createCourseReview(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CourseReview courseReview,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_TEACHER = "/v1/lms/courses/{courseId}/teachers";
    /**
     * POST /v1/lms/courses/{courseId}/teachers : Create CourseTeacher
     * Create a CourseTeacher. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTeacher  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_TEACHER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseTeacher> createCourseTeacher(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CourseTeacher courseTeacher,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_TESTIMONIAL = "/v1/lms/courses/{courseId}/testimonials";
    /**
     * POST /v1/lms/courses/{courseId}/testimonials : Create CourseTestimonial
     * Create a CourseTestimonial. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTestimonial  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_TESTIMONIAL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseTestimonial> createCourseTestimonial(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CourseTestimonial courseTestimonial,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_VERSION = "/v1/lms/courses/{courseId}:createVersion";
    /**
     * POST /v1/lms/courses/{courseId}:createVersion : Create the next version of a course (copies chapters, items, objectives, grading categories and release rules; previous_version_id and root_course_id are set)
     * Create the next version of a course (copies chapters, items, objectives, grading categories and release rules; previous_version_id and root_course_id are set).
     *
     * @param courseId  (required)
     * @param createCourseVersionRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_COURSE_VERSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> createCourseVersion(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CreateCourseVersionRequest createCourseVersionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ENROLLMENT_REQUEST = "/v1/lms/courses/{courseId}/enrollment-requests";
    /**
     * POST /v1/lms/courses/{courseId}/enrollment-requests : Create EnrollmentRequest
     * Create a EnrollmentRequest. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param enrollmentRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_ENROLLMENT_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EnrollmentRequest> createEnrollmentRequest(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody EnrollmentRequest enrollmentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_LMS_COURSE = "/v1/lms/courses";
    /**
     * POST /v1/lms/courses : Create LmsCourse
     * Create a LmsCourse. Supports Idempotency-Key and validateOnly.
     *
     * @param lmsCourse  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_LMS_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> createLmsCourse(
         @Valid @RequestBody LmsCourse lmsCourse,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RELATED_COURSE = "/v1/lms/courses/{courseId}/related-courses";
    /**
     * POST /v1/lms/courses/{courseId}/related-courses : Create RelatedCourse
     * Create a RelatedCourse. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId LmsCourse id. (required)
     * @param relatedCourse  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_CREATE_RELATED_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RelatedCourse> createRelatedCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody RelatedCourse relatedCourse,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DEACTIVATE_COURSE = "/v1/lms/courses/{courseId}:deactivate";
    /**
     * POST /v1/lms/courses/{courseId}:deactivate : Deactivate a course version (no new cohorts)
     * Deactivate a course version (no new cohorts).
     *
     * @param courseId  (required)
     * @param deactivateCourseRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_DEACTIVATE_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> deactivateCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody DeactivateCourseRequest deactivateCourseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_CATALOG_BANNER = "/v1/lms/courses/{courseId}/banners/{catalogBannerId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/banners/{catalogBannerId} : Delete CatalogBanner
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param catalogBannerId CatalogBanner id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_CATALOG_BANNER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCatalogBanner(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("catalogBannerId") UUID catalogBannerId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CHAPTER = "/v1/lms/courses/{courseId}/chapters/{chapterId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/chapters/{chapterId} : Delete Chapter
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param chapterId Chapter id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_CHAPTER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteChapter(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("chapterId") UUID chapterId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_AUTHOR_PROFILE = "/v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId} : Delete CourseAuthorProfile
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param courseAuthorProfileId CourseAuthorProfile id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_COURSE_AUTHOR_PROFILE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourseAuthorProfile(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseAuthorProfileId") UUID courseAuthorProfileId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_INTEREST = "/v1/lms/courses/{courseId}/interests/{courseInterestId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/interests/{courseInterestId} : Delete CourseInterest
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param courseInterestId CourseInterest id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_COURSE_INTEREST,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourseInterest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseInterestId") UUID courseInterestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_MENTOR = "/v1/lms/courses/{courseId}/mentors/{courseMentorId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/mentors/{courseMentorId} : Delete CourseMentor
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param courseMentorId CourseMentor id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_COURSE_MENTOR,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourseMentor(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseMentorId") UUID courseMentorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_PREVIEW = "/v1/lms/courses/{courseId}/previews/{coursePreviewId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/previews/{coursePreviewId} : Delete CoursePreview
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param coursePreviewId CoursePreview id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_COURSE_PREVIEW,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCoursePreview(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("coursePreviewId") UUID coursePreviewId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_REVIEW = "/v1/lms/courses/{courseId}/reviews/{courseReviewId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/reviews/{courseReviewId} : Delete CourseReview
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param courseReviewId CourseReview id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_COURSE_REVIEW,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourseReview(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseReviewId") UUID courseReviewId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_TEACHER = "/v1/lms/courses/{courseId}/teachers/{courseTeacherId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/teachers/{courseTeacherId} : Delete CourseTeacher
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTeacherId CourseTeacher id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_COURSE_TEACHER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourseTeacher(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseTeacherId") UUID courseTeacherId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_TESTIMONIAL = "/v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/testimonials/{courseTestimonialId} : Delete CourseTestimonial
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTestimonialId CourseTestimonial id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_COURSE_TESTIMONIAL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourseTestimonial(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseTestimonialId") UUID courseTestimonialId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ENROLLMENT_REQUEST = "/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId} : Delete EnrollmentRequest
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param enrollmentRequestId EnrollmentRequest id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_ENROLLMENT_REQUEST,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEnrollmentRequest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("enrollmentRequestId") UUID enrollmentRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_LMS_COURSE = "/v1/lms/courses/{courseId}";
    /**
     * DELETE /v1/lms/courses/{courseId} : Delete LmsCourse
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_LMS_COURSE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLmsCourse(
         @PathVariable("courseId") UUID courseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RELATED_COURSE = "/v1/lms/courses/{courseId}/related-courses/{relatedCourseId}";
    /**
     * DELETE /v1/lms/courses/{courseId}/related-courses/{relatedCourseId} : Delete RelatedCourse
     * Soft delete (sets deleted_at).
     *
     * @param courseId LmsCourse id. (required)
     * @param relatedCourseId RelatedCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = LmsCourseApi.PATH_DELETE_RELATED_COURSE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRelatedCourse(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("relatedCourseId") UUID relatedCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_ENROLL_IN_COURSE = "/v1/lms/courses/{courseId}:enroll";
    /**
     * POST /v1/lms/courses/{courseId}:enroll : Self-enrol the caller
     * Self-enrol the caller.
     *
     * @param courseId  (required)
     * @param enrollInCourseRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_ENROLL_IN_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Enrollment> enrollInCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody EnrollInCourseRequest enrollInCourseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_EXPORT_COURSE = "/v1/lms/courses/{courseId}:export";
    /**
     * POST /v1/lms/courses/{courseId}:export : Export the course as a package (zip)
     * Export the course as a package (zip).
     *
     * @param courseId  (required)
     * @param body  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Accepted; poll the operation. (status code 202)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_EXPORT_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> exportCourse(
         @PathVariable("courseId") UUID courseId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_IMPORT_COURSE_PACKAGE = "/v1/lms/courses/{courseId}:importPackage";
    /**
     * POST /v1/lms/courses/{courseId}:importPackage : Import a course package (zip) into the course
     * Import a course package (zip) into the course: chapters, lessons, quizzes with their questions, assignments.
     *
     * @param courseId  (required)
     * @param importCoursePackageRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Accepted; poll the operation. (status code 202)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_IMPORT_COURSE_PACKAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> importCoursePackage(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody ImportCoursePackageRequest importCoursePackageRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_CATALOG_BANNER = "/v1/lms/courses/{courseId}/banners/{catalogBannerId}";
    /**
     * GET /v1/lms/courses/{courseId}/banners/{catalogBannerId} : Get CatalogBanner
     * Get one CatalogBanner by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param catalogBannerId CatalogBanner id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_CATALOG_BANNER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CatalogBanner> getCatalogBanner(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("catalogBannerId") UUID catalogBannerId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CATALOG_HOME = "/v1/lms/catalog";
    /**
     * GET /v1/lms/catalog : Catalogue home: banners, published courses, categories and cohorts open for enrolment
     * Catalogue home: banners, published courses, categories and cohorts open for enrolment. Public.
     *
     * @param categoryId Filter. (optional)
     * @param query Search text. (optional)
     * @param page One-based page of courses (default 1). (optional, default to 1)
     * @param size Courses per page (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_CATALOG_HOME,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CatalogHome> getCatalogHome(
         @Valid @RequestParam(value = "categoryId", required = false) @Nullable UUID categoryId,
         @Valid @RequestParam(value = "query", required = false) @Nullable String query,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_CHAPTER = "/v1/lms/courses/{courseId}/chapters/{chapterId}";
    /**
     * GET /v1/lms/courses/{courseId}/chapters/{chapterId} : Get Chapter
     * Get one Chapter by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param chapterId Chapter id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_CHAPTER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Chapter> getChapter(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("chapterId") UUID chapterId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_AUTHOR_PROFILE = "/v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}";
    /**
     * GET /v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId} : Get CourseAuthorProfile
     * Get one CourseAuthorProfile by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseAuthorProfileId CourseAuthorProfile id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_AUTHOR_PROFILE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseAuthorProfile> getCourseAuthorProfile(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseAuthorProfileId") UUID courseAuthorProfileId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_INTEREST = "/v1/lms/courses/{courseId}/interests/{courseInterestId}";
    /**
     * GET /v1/lms/courses/{courseId}/interests/{courseInterestId} : Get CourseInterest
     * Get one CourseInterest by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseInterestId CourseInterest id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_INTEREST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseInterest> getCourseInterest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseInterestId") UUID courseInterestId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_MENTOR = "/v1/lms/courses/{courseId}/mentors/{courseMentorId}";
    /**
     * GET /v1/lms/courses/{courseId}/mentors/{courseMentorId} : Get CourseMentor
     * Get one CourseMentor by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseMentorId CourseMentor id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_MENTOR,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseMentor> getCourseMentor(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseMentorId") UUID courseMentorId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_OUTLINE = "/v1/lms/courses/{courseId}:outline";
    /**
     * GET /v1/lms/courses/{courseId}:outline : Chapters and lessons of a published course, with the caller&#39;s progress when signed in
     * Chapters and lessons of a published course, with the caller&#39;s progress when signed in. Public.
     *
     * @param courseId  (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_OUTLINE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseOutline> getCourseOutline(
         @PathVariable("courseId") UUID courseId
    );


    String PATH_GET_COURSE_PREVIEW = "/v1/lms/courses/{courseId}/previews/{coursePreviewId}";
    /**
     * GET /v1/lms/courses/{courseId}/previews/{coursePreviewId} : Get CoursePreview
     * Get one CoursePreview by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param coursePreviewId CoursePreview id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_PREVIEW,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CoursePreview> getCoursePreview(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("coursePreviewId") UUID coursePreviewId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_PROGRESS_DISTRIBUTION = "/v1/lms/courses/{courseId}:progressDistribution";
    /**
     * GET /v1/lms/courses/{courseId}:progressDistribution : Students per progress range
     * Students per progress range.
     *
     * @param courseId  (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_PROGRESS_DISTRIBUTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ProgressBucket>> getCourseProgressDistribution(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_COURSE_REVIEW = "/v1/lms/courses/{courseId}/reviews/{courseReviewId}";
    /**
     * GET /v1/lms/courses/{courseId}/reviews/{courseReviewId} : Get CourseReview
     * Get one CourseReview by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseReviewId CourseReview id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_REVIEW,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseReview> getCourseReview(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseReviewId") UUID courseReviewId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_SYLLABUS = "/v1/lms/courses/{courseId}:syllabus";
    /**
     * GET /v1/lms/courses/{courseId}:syllabus : Syllabus document generated from the outline, objectives and grading categories
     * Syllabus document generated from the outline, objectives and grading categories.
     *
     * @param courseId  (required)
     * @param format Download format. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_SYLLABUS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DownloadLink> getCourseSyllabus(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestParam(value = "format", required = false) @Nullable String format
    );


    String PATH_GET_COURSE_TEACHER = "/v1/lms/courses/{courseId}/teachers/{courseTeacherId}";
    /**
     * GET /v1/lms/courses/{courseId}/teachers/{courseTeacherId} : Get CourseTeacher
     * Get one CourseTeacher by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTeacherId CourseTeacher id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_TEACHER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseTeacher> getCourseTeacher(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseTeacherId") UUID courseTeacherId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_TESTIMONIAL = "/v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}";
    /**
     * GET /v1/lms/courses/{courseId}/testimonials/{courseTestimonialId} : Get CourseTestimonial
     * Get one CourseTestimonial by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTestimonialId CourseTestimonial id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_COURSE_TESTIMONIAL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseTestimonial> getCourseTestimonial(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseTestimonialId") UUID courseTestimonialId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ENROLLMENT_REQUEST = "/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}";
    /**
     * GET /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId} : Get EnrollmentRequest
     * Get one EnrollmentRequest by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param enrollmentRequestId EnrollmentRequest id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_ENROLLMENT_REQUEST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EnrollmentRequest> getEnrollmentRequest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("enrollmentRequestId") UUID enrollmentRequestId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EVALUATOR_SLOTS = "/v1/lms/courses/{courseId}:evaluatorSlots";
    /**
     * GET /v1/lms/courses/{courseId}:evaluatorSlots : Free evaluation slots
     * Free evaluation slots.
     *
     * @param courseId  (required)
     * @param fromDate From. (required)
     * @param toDate To. (required)
     * @param cohortId Cohort. (optional)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_EVALUATOR_SLOTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EvaluatorSlot>> getEvaluatorSlots(
         @PathVariable("courseId") UUID courseId,
        @NotNull  @Valid @RequestParam(value = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @NotNull  @Valid @RequestParam(value = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
         @Valid @RequestParam(value = "cohortId", required = false) @Nullable UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_LMS_COURSE = "/v1/lms/courses/{courseId}";
    /**
     * GET /v1/lms/courses/{courseId} : Get LmsCourse
     * Get one LmsCourse by id. The ETag header carries the row version. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param courseId LmsCourse id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_LMS_COURSE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LmsCourse> getLmsCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RELATED_COURSE = "/v1/lms/courses/{courseId}/related-courses/{relatedCourseId}";
    /**
     * GET /v1/lms/courses/{courseId}/related-courses/{relatedCourseId} : Get RelatedCourse
     * Get one RelatedCourse by id. The ETag header carries the row version.
     *
     * @param courseId LmsCourse id. (required)
     * @param relatedCourseId RelatedCourse id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_RELATED_COURSE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RelatedCourse> getRelatedCourse(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("relatedCourseId") UUID relatedCourseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_SCORM_LAUNCH = "/v1/lms/chapters/{chapterId}:scormLaunch";
    /**
     * GET /v1/lms/chapters/{chapterId}:scormLaunch : Launch URL and saved runtime state of a SCORM chapter
     * Launch URL and saved runtime state of a SCORM chapter.
     *
     * @param chapterId  (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_SCORM_LAUNCH,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ScormLaunch> getScormLaunch(
         @PathVariable("chapterId") UUID chapterId
    );


    String PATH_GET_TEACHER_DASHBOARD = "/v1/lms/me/teacher-dashboard";
    /**
     * GET /v1/lms/me/teacher-dashboard : Teacher home: my cohorts, grading queue, unread posts, upcoming items and alerts
     * Teacher home: my cohorts, grading queue, unread posts, upcoming items and alerts.
     *
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_GET_TEACHER_DASHBOARD,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<TeacherDashboard> getTeacherDashboard(
        
    );


    String PATH_HIDE_CATALOG_LISTING = "/v1/lms/courses/{courseId}:hideCatalogListing";
    /**
     * POST /v1/lms/courses/{courseId}:hideCatalogListing : Remove the course from the catalogue
     * Remove the course from the catalogue.
     *
     * @param courseId  (required)
     * @param hideCatalogListingRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_HIDE_CATALOG_LISTING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> hideCatalogListing(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody HideCatalogListingRequest hideCatalogListingRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_ISSUE_MY_CERTIFICATE = "/v1/lms/courses/{courseId}:issueMyCertificate";
    /**
     * POST /v1/lms/courses/{courseId}:issueMyCertificate : Issue a certificate to the caller when the course is complete and certification is enabled
     * Issue a certificate to the caller when the course is complete and certification is enabled.
     *
     * @param courseId  (required)
     * @param body  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_ISSUE_MY_CERTIFICATE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Certificate> issueMyCertificate(
         @PathVariable("courseId") UUID courseId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_CATALOG_BANNERS = "/v1/lms/courses/{courseId}/banners";
    /**
     * GET /v1/lms/courses/{courseId}/banners : List CatalogBanners
     * List CatalogBanners visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_CATALOG_BANNERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CatalogBanner>> listCatalogBanners(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CHAPTERS = "/v1/lms/courses/{courseId}/chapters";
    /**
     * GET /v1/lms/courses/{courseId}/chapters : List Chapters
     * List Chapters visible to the caller, page by page, with filtering and sorting.
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_CHAPTERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Chapter>> listChapters(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_AUTHOR_PROFILES = "/v1/lms/courses/{courseId}/author-profiles";
    /**
     * GET /v1/lms/courses/{courseId}/author-profiles : List CourseAuthorProfiles
     * List CourseAuthorProfiles visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_COURSE_AUTHOR_PROFILES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CourseAuthorProfile>> listCourseAuthorProfiles(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_INTERESTS = "/v1/lms/courses/{courseId}/interests";
    /**
     * GET /v1/lms/courses/{courseId}/interests : List CourseInterests
     * List CourseInterests visible to the caller, page by page, with filtering and sorting.
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_COURSE_INTERESTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CourseInterest>> listCourseInterests(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_MENTORS = "/v1/lms/courses/{courseId}/mentors";
    /**
     * GET /v1/lms/courses/{courseId}/mentors : List CourseMentors
     * List CourseMentors visible to the caller, page by page, with filtering and sorting.
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_COURSE_MENTORS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CourseMentor>> listCourseMentors(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_PREVIEWS = "/v1/lms/courses/{courseId}/previews";
    /**
     * GET /v1/lms/courses/{courseId}/previews : List CoursePreviews
     * List CoursePreviews visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_COURSE_PREVIEWS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CoursePreview>> listCoursePreviews(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_REVIEWS = "/v1/lms/courses/{courseId}/reviews";
    /**
     * GET /v1/lms/courses/{courseId}/reviews : List CourseReviews
     * List CourseReviews visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_COURSE_REVIEWS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CourseReview>> listCourseReviews(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_TEACHERS = "/v1/lms/courses/{courseId}/teachers";
    /**
     * GET /v1/lms/courses/{courseId}/teachers : List CourseTeachers
     * List CourseTeachers visible to the caller, page by page, with filtering and sorting.
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_COURSE_TEACHERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CourseTeacher>> listCourseTeachers(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_TESTIMONIALS = "/v1/lms/courses/{courseId}/testimonials";
    /**
     * GET /v1/lms/courses/{courseId}/testimonials : List CourseTestimonials
     * List CourseTestimonials visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_COURSE_TESTIMONIALS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CourseTestimonial>> listCourseTestimonials(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ENROLLMENT_REQUESTS = "/v1/lms/courses/{courseId}/enrollment-requests";
    /**
     * GET /v1/lms/courses/{courseId}/enrollment-requests : List EnrollmentRequests
     * List EnrollmentRequests visible to the caller, page by page, with filtering and sorting.
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_ENROLLMENT_REQUESTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EnrollmentRequest>> listEnrollmentRequests(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LMS_COURSES = "/v1/lms/courses";
    /**
     * GET /v1/lms/courses : List LmsCourses
     * List LmsCourses visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_LMS_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LmsCourse>> listLmsCourses(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RELATED_COURSES = "/v1/lms/courses/{courseId}/related-courses";
    /**
     * GET /v1/lms/courses/{courseId}/related-courses : List RelatedCourses
     * List RelatedCourses visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param courseId LmsCourse id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LmsCourseApi.PATH_LIST_RELATED_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RelatedCourse>> listRelatedCourses(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MOVE_CHAPTER = "/v1/lms/chapters/{chapterId}:move";
    /**
     * POST /v1/lms/chapters/{chapterId}:move : Move / reorder a chapter (folder) inside its course or cohort
     * Move / reorder a chapter (folder) inside its course or cohort.
     *
     * @param chapterId  (required)
     * @param moveChapterRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_MOVE_CHAPTER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Chapter> moveChapter(
         @PathVariable("chapterId") UUID chapterId,
         @Valid @RequestBody MoveChapterRequest moveChapterRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_PUBLISH_CATALOG_LISTING = "/v1/lms/courses/{courseId}:publishCatalogListing";
    /**
     * POST /v1/lms/courses/{courseId}:publishCatalogListing : List the course in the catalogue
     * List the course in the catalogue.
     *
     * @param courseId  (required)
     * @param publishCatalogListingRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_PUBLISH_CATALOG_LISTING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> publishCatalogListing(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody PublishCatalogListingRequest publishCatalogListingRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_PUBLISH_COURSE = "/v1/lms/courses/{courseId}:publish";
    /**
     * POST /v1/lms/courses/{courseId}:publish : Publish
     * Publish.
     *
     * @param courseId  (required)
     * @param publishCourseRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_PUBLISH_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> publishCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody PublishCourseRequest publishCourseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REACTIVATE_COURSE = "/v1/lms/courses/{courseId}:reactivate";
    /**
     * POST /v1/lms/courses/{courseId}:reactivate : Reactivate a deactivated version
     * Reactivate a deactivated version.
     *
     * @param courseId  (required)
     * @param reactivateCourseRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_REACTIVATE_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> reactivateCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody ReactivateCourseRequest reactivateCourseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REJECT_ENROLLMENT_REQUEST = "/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:reject";
    /**
     * POST /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}:reject : Reject
     * Reject.
     *
     * @param courseId  (required)
     * @param enrollmentRequestId  (required)
     * @param rejectEnrollmentRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_REJECT_ENROLLMENT_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EnrollmentRequest> rejectEnrollmentRequest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("enrollmentRequestId") UUID enrollmentRequestId,
         @Valid @RequestBody RejectEnrollmentRequestRequest rejectEnrollmentRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REORDER_CHAPTERS = "/v1/lms/courses/{courseId}:reorderChapters";
    /**
     * POST /v1/lms/courses/{courseId}:reorderChapters : Reorder chapters
     * Reorder chapters.
     *
     * @param courseId  (required)
     * @param reorderChaptersRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_REORDER_CHAPTERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ChapterList> reorderChapters(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody ReorderChaptersRequest reorderChaptersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SAVE_SCORM_PROGRESS = "/v1/lms/chapters/{chapterId}:saveScormProgress";
    /**
     * POST /v1/lms/chapters/{chapterId}:saveScormProgress : Persist SCORM runtime data and completion
     * Persist SCORM runtime data and completion.
     *
     * @param chapterId  (required)
     * @param saveScormProgressRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_SAVE_SCORM_PROGRESS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonProgress> saveScormProgress(
         @PathVariable("chapterId") UUID chapterId,
         @Valid @RequestBody SaveScormProgressRequest saveScormProgressRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_COURSE_FOR_REVIEW = "/v1/lms/courses/{courseId}:submitForReview";
    /**
     * POST /v1/lms/courses/{courseId}:submitForReview : Send to review
     * Send to review.
     *
     * @param courseId  (required)
     * @param submitCourseForReviewRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_SUBMIT_COURSE_FOR_REVIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> submitCourseForReview(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody SubmitCourseForReviewRequest submitCourseForReviewRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UNPUBLISH_COURSE = "/v1/lms/courses/{courseId}:unpublish";
    /**
     * POST /v1/lms/courses/{courseId}:unpublish : Unpublish
     * Unpublish.
     *
     * @param courseId  (required)
     * @param unpublishCourseRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = LmsCourseApi.PATH_UNPUBLISH_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> unpublishCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody UnpublishCourseRequest unpublishCourseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_CATALOG_BANNER = "/v1/lms/courses/{courseId}/banners/{catalogBannerId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/banners/{catalogBannerId} : Update CatalogBanner
     * Partially update a CatalogBanner (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param catalogBannerId CatalogBanner id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param catalogBannerUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_CATALOG_BANNER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CatalogBanner> updateCatalogBanner(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("catalogBannerId") UUID catalogBannerId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CatalogBannerUpdate catalogBannerUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CHAPTER = "/v1/lms/courses/{courseId}/chapters/{chapterId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/chapters/{chapterId} : Update Chapter
     * Partially update a Chapter (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param chapterId Chapter id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param chapterUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_CHAPTER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Chapter> updateChapter(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("chapterId") UUID chapterId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ChapterUpdate chapterUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_AUTHOR_PROFILE = "/v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/author-profiles/{courseAuthorProfileId} : Update CourseAuthorProfile
     * Partially update a CourseAuthorProfile (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseAuthorProfileId CourseAuthorProfile id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseAuthorProfileUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_COURSE_AUTHOR_PROFILE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseAuthorProfile> updateCourseAuthorProfile(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseAuthorProfileId") UUID courseAuthorProfileId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseAuthorProfileUpdate courseAuthorProfileUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_INTEREST = "/v1/lms/courses/{courseId}/interests/{courseInterestId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/interests/{courseInterestId} : Update CourseInterest
     * Partially update a CourseInterest (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseInterestId CourseInterest id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseInterestUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_COURSE_INTEREST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseInterest> updateCourseInterest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseInterestId") UUID courseInterestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseInterestUpdate courseInterestUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_MENTOR = "/v1/lms/courses/{courseId}/mentors/{courseMentorId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/mentors/{courseMentorId} : Update CourseMentor
     * Partially update a CourseMentor (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseMentorId CourseMentor id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseMentorUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_COURSE_MENTOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseMentor> updateCourseMentor(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseMentorId") UUID courseMentorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseMentorUpdate courseMentorUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_PREVIEW = "/v1/lms/courses/{courseId}/previews/{coursePreviewId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/previews/{coursePreviewId} : Update CoursePreview
     * Partially update a CoursePreview (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param coursePreviewId CoursePreview id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param coursePreviewUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_COURSE_PREVIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CoursePreview> updateCoursePreview(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("coursePreviewId") UUID coursePreviewId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CoursePreviewUpdate coursePreviewUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_REVIEW = "/v1/lms/courses/{courseId}/reviews/{courseReviewId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/reviews/{courseReviewId} : Update CourseReview
     * Partially update a CourseReview (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseReviewId CourseReview id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseReviewUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_COURSE_REVIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseReview> updateCourseReview(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseReviewId") UUID courseReviewId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseReviewUpdate courseReviewUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_TEACHER = "/v1/lms/courses/{courseId}/teachers/{courseTeacherId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/teachers/{courseTeacherId} : Update CourseTeacher
     * Partially update a CourseTeacher (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTeacherId CourseTeacher id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseTeacherUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_COURSE_TEACHER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseTeacher> updateCourseTeacher(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseTeacherId") UUID courseTeacherId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseTeacherUpdate courseTeacherUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_TESTIMONIAL = "/v1/lms/courses/{courseId}/testimonials/{courseTestimonialId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/testimonials/{courseTestimonialId} : Update CourseTestimonial
     * Partially update a CourseTestimonial (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param courseTestimonialId CourseTestimonial id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseTestimonialUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_COURSE_TESTIMONIAL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseTestimonial> updateCourseTestimonial(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseTestimonialId") UUID courseTestimonialId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseTestimonialUpdate courseTestimonialUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ENROLLMENT_REQUEST = "/v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/enrollment-requests/{enrollmentRequestId} : Update EnrollmentRequest
     * Partially update a EnrollmentRequest (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param enrollmentRequestId EnrollmentRequest id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param enrollmentRequestUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_ENROLLMENT_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EnrollmentRequest> updateEnrollmentRequest(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("enrollmentRequestId") UUID enrollmentRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EnrollmentRequestUpdate enrollmentRequestUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_LMS_COURSE = "/v1/lms/courses/{courseId}";
    /**
     * PATCH /v1/lms/courses/{courseId} : Update LmsCourse
     * Partially update a LmsCourse (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param lmsCourseUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_LMS_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsCourse> updateLmsCourse(
         @PathVariable("courseId") UUID courseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LmsCourseUpdate lmsCourseUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RELATED_COURSE = "/v1/lms/courses/{courseId}/related-courses/{relatedCourseId}";
    /**
     * PATCH /v1/lms/courses/{courseId}/related-courses/{relatedCourseId} : Update RelatedCourse
     * Partially update a RelatedCourse (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param courseId LmsCourse id. (required)
     * @param relatedCourseId RelatedCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param relatedCourseUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = LmsCourseApi.PATH_UPDATE_RELATED_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RelatedCourse> updateRelatedCourse(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("relatedCourseId") UUID relatedCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RelatedCourseUpdate relatedCourseUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
