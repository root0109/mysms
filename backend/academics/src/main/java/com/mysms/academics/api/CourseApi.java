// Owned API contract of academics.
package com.mysms.academics.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.api.model.Course;
import com.mysms.academics.api.model.CourseSubject;
import com.mysms.academics.api.model.CourseSubjectUpdate;
import com.mysms.academics.api.model.CourseUpdate;
import org.springframework.lang.Nullable;
import java.util.UUID;
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
public interface CourseApi {

    String PATH_CREATE_COURSE = "/v1/academics/courses";
    /**
     * POST /v1/academics/courses : Create Course
     * Create a Course. Supports Idempotency-Key and validateOnly.
     *
     * @param course  (required)
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
        value = CourseApi.PATH_CREATE_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Course> createCourse(
         @Valid @RequestBody Course course,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COURSE_SUBJECT = "/v1/academics/courses/{courseId}/subjects";
    /**
     * POST /v1/academics/courses/{courseId}/subjects : Create CourseSubject
     * Create a CourseSubject. Supports Idempotency-Key and validateOnly.
     *
     * @param courseId Course id. (required)
     * @param courseSubject  (required)
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
        value = CourseApi.PATH_CREATE_COURSE_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseSubject> createCourseSubject(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestBody CourseSubject courseSubject,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_COURSE = "/v1/academics/courses/{courseId}";
    /**
     * DELETE /v1/academics/courses/{courseId} : Delete Course
     * Soft delete (sets deleted_at).
     *
     * @param courseId Course id. (required)
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
        value = CourseApi.PATH_DELETE_COURSE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourse(
         @PathVariable("courseId") UUID courseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COURSE_SUBJECT = "/v1/academics/courses/{courseId}/subjects/{courseSubjectId}";
    /**
     * DELETE /v1/academics/courses/{courseId}/subjects/{courseSubjectId} : Delete CourseSubject
     * Soft delete (sets deleted_at).
     *
     * @param courseId Course id. (required)
     * @param courseSubjectId CourseSubject id. (required)
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
        value = CourseApi.PATH_DELETE_COURSE_SUBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCourseSubject(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseSubjectId") UUID courseSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_COURSE = "/v1/academics/courses/{courseId}";
    /**
     * GET /v1/academics/courses/{courseId} : Get Course
     * Get one Course by id. The ETag header carries the row version.
     *
     * @param courseId Course id. (required)
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
        value = CourseApi.PATH_GET_COURSE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Course> getCourse(
         @PathVariable("courseId") UUID courseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COURSE_SUBJECT = "/v1/academics/courses/{courseId}/subjects/{courseSubjectId}";
    /**
     * GET /v1/academics/courses/{courseId}/subjects/{courseSubjectId} : Get CourseSubject
     * Get one CourseSubject by id. The ETag header carries the row version.
     *
     * @param courseId Course id. (required)
     * @param courseSubjectId CourseSubject id. (required)
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
        value = CourseApi.PATH_GET_COURSE_SUBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CourseSubject> getCourseSubject(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseSubjectId") UUID courseSubjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSE_SUBJECTS = "/v1/academics/courses/{courseId}/subjects";
    /**
     * GET /v1/academics/courses/{courseId}/subjects : List CourseSubjects
     * List CourseSubjects visible to the caller, page by page, with filtering and sorting.
     *
     * @param courseId Course id. (required)
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
        value = CourseApi.PATH_LIST_COURSE_SUBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CourseSubject>> listCourseSubjects(
         @PathVariable("courseId") UUID courseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COURSES = "/v1/academics/courses";
    /**
     * GET /v1/academics/courses : List Courses
     * List Courses visible to the caller, page by page, with filtering and sorting.
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
        value = CourseApi.PATH_LIST_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Course>> listCourses(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_COURSE = "/v1/academics/courses/{courseId}";
    /**
     * PATCH /v1/academics/courses/{courseId} : Update Course
     * Partially update a Course (updateMask). Requires If-Match with the current ETag.
     *
     * @param courseId Course id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
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
        value = CourseApi.PATH_UPDATE_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Course> updateCourse(
         @PathVariable("courseId") UUID courseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseUpdate courseUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COURSE_SUBJECT = "/v1/academics/courses/{courseId}/subjects/{courseSubjectId}";
    /**
     * PATCH /v1/academics/courses/{courseId}/subjects/{courseSubjectId} : Update CourseSubject
     * Partially update a CourseSubject (updateMask). Requires If-Match with the current ETag.
     *
     * @param courseId Course id. (required)
     * @param courseSubjectId CourseSubject id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param courseSubjectUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
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
        value = CourseApi.PATH_UPDATE_COURSE_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CourseSubject> updateCourseSubject(
         @PathVariable("courseId") UUID courseId,
         @PathVariable("courseSubjectId") UUID courseSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CourseSubjectUpdate courseSubjectUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
