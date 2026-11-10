// Owned API contract of academics (JAVA-CODING-RULES §7); first materialized from api/openapi/academics.yaml on 2026-11-10.
package com.mysms.academics.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.api.model.ApproveLessonPlanRequest;
import com.mysms.academics.api.model.ArchiveLessonPlanRequest;
import com.mysms.academics.api.model.LessonPlan;
import com.mysms.academics.api.model.LessonPlanApproval;
import com.mysms.academics.api.model.LessonPlanApprovalUpdate;
import com.mysms.academics.api.model.LessonPlanUpdate;
import org.springframework.lang.Nullable;
import com.mysms.academics.api.model.PublishLessonPlanRequest;
import com.mysms.academics.api.model.RejectLessonPlanRequest;
import com.mysms.academics.api.model.SubmitLessonPlanRequest;
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
public interface LessonPlanApi {

    String PATH_APPROVE_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}:approve";
    /**
     * POST /v1/academics/lesson-plans/{lessonPlanId}:approve : Approve (records an approval)
     * Approve (records an approval).
     *
     * @param lessonPlanId  (required)
     * @param approveLessonPlanRequest  (required)
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
        value = LessonPlanApi.PATH_APPROVE_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlan> approveLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @Valid @RequestBody ApproveLessonPlanRequest approveLessonPlanRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_ARCHIVE_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}:archive";
    /**
     * POST /v1/academics/lesson-plans/{lessonPlanId}:archive : Archive
     * Archive.
     *
     * @param lessonPlanId  (required)
     * @param archiveLessonPlanRequest  (required)
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
        value = LessonPlanApi.PATH_ARCHIVE_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlan> archiveLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @Valid @RequestBody ArchiveLessonPlanRequest archiveLessonPlanRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_LESSON_PLAN = "/v1/academics/lesson-plans";
    /**
     * POST /v1/academics/lesson-plans : Create LessonPlan
     * Create a LessonPlan. Supports Idempotency-Key and validateOnly.
     *
     * @param lessonPlan  (required)
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
        value = LessonPlanApi.PATH_CREATE_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlan> createLessonPlan(
         @Valid @RequestBody LessonPlan lessonPlan,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_LESSON_PLAN_APPROVAL = "/v1/academics/lesson-plans/{lessonPlanId}/approvals";
    /**
     * POST /v1/academics/lesson-plans/{lessonPlanId}/approvals : Create LessonPlanApproval
     * Create a LessonPlanApproval. Supports Idempotency-Key and validateOnly.
     *
     * @param lessonPlanId LessonPlan id. (required)
     * @param lessonPlanApproval  (required)
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
        value = LessonPlanApi.PATH_CREATE_LESSON_PLAN_APPROVAL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlanApproval> createLessonPlanApproval(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @Valid @RequestBody LessonPlanApproval lessonPlanApproval,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}";
    /**
     * DELETE /v1/academics/lesson-plans/{lessonPlanId} : Delete LessonPlan
     * Soft delete (sets deleted_at).
     *
     * @param lessonPlanId LessonPlan id. (required)
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
        value = LessonPlanApi.PATH_DELETE_LESSON_PLAN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_LESSON_PLAN_APPROVAL = "/v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}";
    /**
     * DELETE /v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId} : Delete LessonPlanApproval
     * Soft delete (sets deleted_at).
     *
     * @param lessonPlanId LessonPlan id. (required)
     * @param lessonPlanApprovalId LessonPlanApproval id. (required)
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
        value = LessonPlanApi.PATH_DELETE_LESSON_PLAN_APPROVAL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLessonPlanApproval(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @PathVariable("lessonPlanApprovalId") UUID lessonPlanApprovalId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}";
    /**
     * GET /v1/academics/lesson-plans/{lessonPlanId} : Get LessonPlan
     * Get one LessonPlan by id. The ETag header carries the row version.
     *
     * @param lessonPlanId LessonPlan id. (required)
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
        value = LessonPlanApi.PATH_GET_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LessonPlan> getLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LESSON_PLAN_APPROVAL = "/v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}";
    /**
     * GET /v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId} : Get LessonPlanApproval
     * Get one LessonPlanApproval by id. The ETag header carries the row version.
     *
     * @param lessonPlanId LessonPlan id. (required)
     * @param lessonPlanApprovalId LessonPlanApproval id. (required)
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
        value = LessonPlanApi.PATH_GET_LESSON_PLAN_APPROVAL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LessonPlanApproval> getLessonPlanApproval(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @PathVariable("lessonPlanApprovalId") UUID lessonPlanApprovalId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LESSON_PLAN_APPROVALS = "/v1/academics/lesson-plans/{lessonPlanId}/approvals";
    /**
     * GET /v1/academics/lesson-plans/{lessonPlanId}/approvals : List LessonPlanApprovals
     * List LessonPlanApprovals visible to the caller, page by page, with filtering and sorting.
     *
     * @param lessonPlanId LessonPlan id. (required)
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
        value = LessonPlanApi.PATH_LIST_LESSON_PLAN_APPROVALS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LessonPlanApproval>> listLessonPlanApprovals(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LESSON_PLANS = "/v1/academics/lesson-plans";
    /**
     * GET /v1/academics/lesson-plans : List LessonPlans
     * List LessonPlans visible to the caller, page by page, with filtering and sorting.
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
        value = LessonPlanApi.PATH_LIST_LESSON_PLANS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LessonPlan>> listLessonPlans(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}:publish";
    /**
     * POST /v1/academics/lesson-plans/{lessonPlanId}:publish : Publish to students / guardians
     * Publish to students / guardians.
     *
     * @param lessonPlanId  (required)
     * @param publishLessonPlanRequest  (required)
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
        value = LessonPlanApi.PATH_PUBLISH_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlan> publishLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @Valid @RequestBody PublishLessonPlanRequest publishLessonPlanRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REJECT_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}:reject";
    /**
     * POST /v1/academics/lesson-plans/{lessonPlanId}:reject : Reject
     * Reject.
     *
     * @param lessonPlanId  (required)
     * @param rejectLessonPlanRequest  (required)
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
        value = LessonPlanApi.PATH_REJECT_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlan> rejectLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @Valid @RequestBody RejectLessonPlanRequest rejectLessonPlanRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}:submit";
    /**
     * POST /v1/academics/lesson-plans/{lessonPlanId}:submit : Submit for approval
     * Submit for approval.
     *
     * @param lessonPlanId  (required)
     * @param submitLessonPlanRequest  (required)
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
        value = LessonPlanApi.PATH_SUBMIT_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlan> submitLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @Valid @RequestBody SubmitLessonPlanRequest submitLessonPlanRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_LESSON_PLAN = "/v1/academics/lesson-plans/{lessonPlanId}";
    /**
     * PATCH /v1/academics/lesson-plans/{lessonPlanId} : Update LessonPlan
     * Partially update a LessonPlan (updateMask). Requires If-Match with the current ETag.
     *
     * @param lessonPlanId LessonPlan id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param lessonPlanUpdate  (required)
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
        value = LessonPlanApi.PATH_UPDATE_LESSON_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlan> updateLessonPlan(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LessonPlanUpdate lessonPlanUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_LESSON_PLAN_APPROVAL = "/v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId}";
    /**
     * PATCH /v1/academics/lesson-plans/{lessonPlanId}/approvals/{lessonPlanApprovalId} : Update LessonPlanApproval
     * Partially update a LessonPlanApproval (updateMask). Requires If-Match with the current ETag.
     *
     * @param lessonPlanId LessonPlan id. (required)
     * @param lessonPlanApprovalId LessonPlanApproval id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param lessonPlanApprovalUpdate  (required)
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
        value = LessonPlanApi.PATH_UPDATE_LESSON_PLAN_APPROVAL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonPlanApproval> updateLessonPlanApproval(
         @PathVariable("lessonPlanId") UUID lessonPlanId,
         @PathVariable("lessonPlanApprovalId") UUID lessonPlanApprovalId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LessonPlanApprovalUpdate lessonPlanApprovalUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
