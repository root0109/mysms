// Owned API contract of careers.
package com.mysms.students.careers.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.students.careers.api.model.CloseJobOpeningRequest;
import com.mysms.students.careers.api.model.EmailApplicantRequest;
import com.mysms.students.careers.api.model.JobApplication;
import com.mysms.students.careers.api.model.JobApplicationUpdate;
import com.mysms.students.careers.api.model.JobOpening;
import com.mysms.students.careers.api.model.JobOpeningUpdate;
import org.springframework.lang.Nullable;
import com.mysms.students.careers.api.model.ReopenJobOpeningRequest;
import com.mysms.students.careers.api.model.ReportJobOpeningRequest;
import java.util.UUID;
import com.mysms.students.careers.api.model.UpdateApplicationStatusRequest;
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
public interface JobOpeningApi {

    String PATH_CLOSE_JOB_OPENING = "/v1/careers/job-openings/{jobOpeningId}:close";
    /**
     * POST /v1/careers/job-openings/{jobOpeningId}:close : Close
     * Close.
     *
     * @param jobOpeningId  (required)
     * @param closeJobOpeningRequest  (required)
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
        value = JobOpeningApi.PATH_CLOSE_JOB_OPENING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<JobOpening> closeJobOpening(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @Valid @RequestBody CloseJobOpeningRequest closeJobOpeningRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_JOB_APPLICATION = "/v1/careers/job-openings/{jobOpeningId}/applications";
    /**
     * POST /v1/careers/job-openings/{jobOpeningId}/applications : Create JobApplication
     * Create a JobApplication. Supports Idempotency-Key and validateOnly.
     *
     * @param jobOpeningId JobOpening id. (required)
     * @param jobApplication  (required)
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
        value = JobOpeningApi.PATH_CREATE_JOB_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<JobApplication> createJobApplication(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @Valid @RequestBody JobApplication jobApplication,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_JOB_OPENING = "/v1/careers/job-openings";
    /**
     * POST /v1/careers/job-openings : Create JobOpening
     * Create a JobOpening. Supports Idempotency-Key and validateOnly.
     *
     * @param jobOpening  (required)
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
        value = JobOpeningApi.PATH_CREATE_JOB_OPENING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<JobOpening> createJobOpening(
         @Valid @RequestBody JobOpening jobOpening,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_JOB_APPLICATION = "/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}";
    /**
     * DELETE /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId} : Delete JobApplication
     * Soft delete (sets deleted_at).
     *
     * @param jobOpeningId JobOpening id. (required)
     * @param jobApplicationId JobApplication id. (required)
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
        value = JobOpeningApi.PATH_DELETE_JOB_APPLICATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteJobApplication(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @PathVariable("jobApplicationId") UUID jobApplicationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_JOB_OPENING = "/v1/careers/job-openings/{jobOpeningId}";
    /**
     * DELETE /v1/careers/job-openings/{jobOpeningId} : Delete JobOpening
     * Soft delete (sets deleted_at).
     *
     * @param jobOpeningId JobOpening id. (required)
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
        value = JobOpeningApi.PATH_DELETE_JOB_OPENING,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteJobOpening(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_EMAIL_APPLICANT = "/v1/careers/job-applications/{applicationId}:email";
    /**
     * POST /v1/careers/job-applications/{applicationId}:email : E-mail the applicant
     * E-mail the applicant.
     *
     * @param applicationId  (required)
     * @param emailApplicantRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = JobOpeningApi.PATH_EMAIL_APPLICANT,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> emailApplicant(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody EmailApplicantRequest emailApplicantRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_JOB_APPLICATION = "/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}";
    /**
     * GET /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId} : Get JobApplication
     * Get one JobApplication by id. The ETag header carries the row version.
     *
     * @param jobOpeningId JobOpening id. (required)
     * @param jobApplicationId JobApplication id. (required)
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
        value = JobOpeningApi.PATH_GET_JOB_APPLICATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<JobApplication> getJobApplication(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @PathVariable("jobApplicationId") UUID jobApplicationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_JOB_OPENING = "/v1/careers/job-openings/{jobOpeningId}";
    /**
     * GET /v1/careers/job-openings/{jobOpeningId} : Get JobOpening
     * Get one JobOpening by id. The ETag header carries the row version. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param jobOpeningId JobOpening id. (required)
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
        value = JobOpeningApi.PATH_GET_JOB_OPENING,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<JobOpening> getJobOpening(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_JOB_APPLICATIONS = "/v1/careers/job-openings/{jobOpeningId}/applications";
    /**
     * GET /v1/careers/job-openings/{jobOpeningId}/applications : List JobApplications
     * List JobApplications visible to the caller, page by page, with filtering and sorting.
     *
     * @param jobOpeningId JobOpening id. (required)
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
        value = JobOpeningApi.PATH_LIST_JOB_APPLICATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<JobApplication>> listJobApplications(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_JOB_OPENINGS = "/v1/careers/job-openings";
    /**
     * GET /v1/careers/job-openings : List JobOpenings
     * List JobOpenings visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
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
        value = JobOpeningApi.PATH_LIST_JOB_OPENINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<JobOpening>> listJobOpenings(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REOPEN_JOB_OPENING = "/v1/careers/job-openings/{jobOpeningId}:reopen";
    /**
     * POST /v1/careers/job-openings/{jobOpeningId}:reopen : Reopen
     * Reopen.
     *
     * @param jobOpeningId  (required)
     * @param reopenJobOpeningRequest  (required)
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
        value = JobOpeningApi.PATH_REOPEN_JOB_OPENING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<JobOpening> reopenJobOpening(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @Valid @RequestBody ReopenJobOpeningRequest reopenJobOpeningRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REPORT_JOB_OPENING = "/v1/careers/job-openings/{jobOpeningId}:report";
    /**
     * POST /v1/careers/job-openings/{jobOpeningId}:report : Report a listing to moderators
     * Report a listing to moderators.
     *
     * @param jobOpeningId  (required)
     * @param reportJobOpeningRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = JobOpeningApi.PATH_REPORT_JOB_OPENING,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> reportJobOpening(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @Valid @RequestBody ReportJobOpeningRequest reportJobOpeningRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_APPLICATION_STATUS = "/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}:setStatus";
    /**
     * POST /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}:setStatus : Shortlist / reject / hire
     * Shortlist / reject / hire.
     *
     * @param jobOpeningId  (required)
     * @param jobApplicationId  (required)
     * @param updateApplicationStatusRequest  (required)
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
        value = JobOpeningApi.PATH_UPDATE_APPLICATION_STATUS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<JobApplication> updateApplicationStatus(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @PathVariable("jobApplicationId") UUID jobApplicationId,
         @Valid @RequestBody UpdateApplicationStatusRequest updateApplicationStatusRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_JOB_APPLICATION = "/v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId}";
    /**
     * PATCH /v1/careers/job-openings/{jobOpeningId}/applications/{jobApplicationId} : Update JobApplication
     * Partially update a JobApplication (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param jobOpeningId JobOpening id. (required)
     * @param jobApplicationId JobApplication id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param jobApplicationUpdate  (required)
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
        value = JobOpeningApi.PATH_UPDATE_JOB_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<JobApplication> updateJobApplication(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
         @PathVariable("jobApplicationId") UUID jobApplicationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody JobApplicationUpdate jobApplicationUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_JOB_OPENING = "/v1/careers/job-openings/{jobOpeningId}";
    /**
     * PATCH /v1/careers/job-openings/{jobOpeningId} : Update JobOpening
     * Partially update a JobOpening (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param jobOpeningId JobOpening id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param jobOpeningUpdate  (required)
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
        value = JobOpeningApi.PATH_UPDATE_JOB_OPENING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<JobOpening> updateJobOpening(
         @PathVariable("jobOpeningId") UUID jobOpeningId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody JobOpeningUpdate jobOpeningUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
