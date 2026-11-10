// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.Enrollment;
import com.mysms.lms.api.model.EnrollmentUpdate;
import com.mysms.lms.api.model.LmsStatistics;
import com.mysms.lms.api.model.MemberProfile;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.StudentDashboard;
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
public interface EnrollmentApi {

    String PATH_DELETE_ENROLLMENT = "/v1/lms/enrollments/{enrollmentId}";
    /**
     * DELETE /v1/lms/enrollments/{enrollmentId} : Delete Enrollment
     * Soft delete (sets deleted_at).
     *
     * @param enrollmentId Enrollment id. (required)
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
        value = EnrollmentApi.PATH_DELETE_ENROLLMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEnrollment(
         @PathVariable("enrollmentId") UUID enrollmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_ENROLLMENT = "/v1/lms/enrollments/{enrollmentId}";
    /**
     * GET /v1/lms/enrollments/{enrollmentId} : Get Enrollment
     * Get one Enrollment by id. The ETag header carries the row version.
     *
     * @param enrollmentId Enrollment id. (required)
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
        value = EnrollmentApi.PATH_GET_ENROLLMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Enrollment> getEnrollment(
         @PathVariable("enrollmentId") UUID enrollmentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LMS_STATISTICS = "/v1/lms/statistics";
    /**
     * GET /v1/lms/statistics : Public catalogue statistics (courses, students, certificates, completion)
     * Public catalogue statistics (courses, students, certificates, completion). Public.
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
        value = EnrollmentApi.PATH_GET_LMS_STATISTICS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LmsStatistics> getLmsStatistics(
        
    );


    String PATH_GET_MEMBER_PROFILE = "/v1/lms/members/{userId}:profile";
    /**
     * GET /v1/lms/members/{userId}:profile : Public student profile with published certificates and badges (private details hidden when the student chose hideStudentInfo)
     * Public student profile with published certificates and badges (private details hidden when the student chose hideStudentInfo). Public.
     *
     * @param userId  (required)
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
        value = EnrollmentApi.PATH_GET_MEMBER_PROFILE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MemberProfile> getMemberProfile(
         @PathVariable("userId") UUID userId
    );


    String PATH_GET_STUDENT_DASHBOARD = "/v1/lms/me/dashboard";
    /**
     * GET /v1/lms/me/dashboard : My courses, cohorts, resume point, live classes, streak and heatmap
     * My courses, cohorts, resume point, live classes, streak and heatmap.
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
        value = EnrollmentApi.PATH_GET_STUDENT_DASHBOARD,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StudentDashboard> getStudentDashboard(
        
    );


    String PATH_LIST_ENROLLMENTS = "/v1/lms/enrollments";
    /**
     * GET /v1/lms/enrollments : List Enrollments
     * List Enrollments visible to the caller, page by page, with filtering and sorting.
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
        value = EnrollmentApi.PATH_LIST_ENROLLMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Enrollment>> listEnrollments(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_ENROLLMENT = "/v1/lms/enrollments/{enrollmentId}";
    /**
     * PATCH /v1/lms/enrollments/{enrollmentId} : Update Enrollment
     * Partially update a Enrollment (updateMask). Requires If-Match with the current ETag.
     *
     * @param enrollmentId Enrollment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param enrollmentUpdate  (required)
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
        value = EnrollmentApi.PATH_UPDATE_ENROLLMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Enrollment> updateEnrollment(
         @PathVariable("enrollmentId") UUID enrollmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EnrollmentUpdate enrollmentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
