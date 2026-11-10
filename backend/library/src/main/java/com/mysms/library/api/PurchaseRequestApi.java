// Owned API contract of library (JAVA-CODING-RULES §7); first materialized from api/openapi/library.yaml on 2026-11-10.
package com.mysms.library.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.model.AcceptPurchaseRequestRequest;
import org.springframework.lang.Nullable;
import com.mysms.library.api.model.PurchaseRequest;
import com.mysms.library.api.model.PurchaseRequestCourse;
import com.mysms.library.api.model.PurchaseRequestCourseUpdate;
import com.mysms.library.api.model.PurchaseRequestSubject;
import com.mysms.library.api.model.PurchaseRequestSubjectUpdate;
import com.mysms.library.api.model.PurchaseRequestUpdate;
import com.mysms.library.api.model.RejectPurchaseRequestRequest;
import com.mysms.library.api.model.SubmitPurchaseRequestRequest;
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
public interface PurchaseRequestApi {

    String PATH_ACCEPT_PURCHASE_REQUEST = "/v1/library/purchase-requests/{purchaseRequestId}:accept";
    /**
     * POST /v1/library/purchase-requests/{purchaseRequestId}:accept : Accept
     * Accept.
     *
     * @param purchaseRequestId  (required)
     * @param acceptPurchaseRequestRequest  (required)
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
        value = PurchaseRequestApi.PATH_ACCEPT_PURCHASE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequest> acceptPurchaseRequest(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @Valid @RequestBody AcceptPurchaseRequestRequest acceptPurchaseRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_PURCHASE_REQUEST = "/v1/library/purchase-requests";
    /**
     * POST /v1/library/purchase-requests : Create PurchaseRequest
     * Create a PurchaseRequest. Supports Idempotency-Key and validateOnly.
     *
     * @param purchaseRequest  (required)
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
        value = PurchaseRequestApi.PATH_CREATE_PURCHASE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequest> createPurchaseRequest(
         @Valid @RequestBody PurchaseRequest purchaseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PURCHASE_REQUEST_COURSE = "/v1/library/purchase-requests/{purchaseRequestId}/courses";
    /**
     * POST /v1/library/purchase-requests/{purchaseRequestId}/courses : Create PurchaseRequestCourse
     * Create a PurchaseRequestCourse. Supports Idempotency-Key and validateOnly.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestCourse  (required)
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
        value = PurchaseRequestApi.PATH_CREATE_PURCHASE_REQUEST_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequestCourse> createPurchaseRequestCourse(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @Valid @RequestBody PurchaseRequestCourse purchaseRequestCourse,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PURCHASE_REQUEST_SUBJECT = "/v1/library/purchase-requests/{purchaseRequestId}/subjects";
    /**
     * POST /v1/library/purchase-requests/{purchaseRequestId}/subjects : Create PurchaseRequestSubject
     * Create a PurchaseRequestSubject. Supports Idempotency-Key and validateOnly.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestSubject  (required)
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
        value = PurchaseRequestApi.PATH_CREATE_PURCHASE_REQUEST_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequestSubject> createPurchaseRequestSubject(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @Valid @RequestBody PurchaseRequestSubject purchaseRequestSubject,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_PURCHASE_REQUEST = "/v1/library/purchase-requests/{purchaseRequestId}";
    /**
     * DELETE /v1/library/purchase-requests/{purchaseRequestId} : Delete PurchaseRequest
     * Soft delete (sets deleted_at).
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
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
        value = PurchaseRequestApi.PATH_DELETE_PURCHASE_REQUEST,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePurchaseRequest(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PURCHASE_REQUEST_COURSE = "/v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}";
    /**
     * DELETE /v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId} : Delete PurchaseRequestCourse
     * Soft delete (sets deleted_at).
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestCourseId PurchaseRequestCourse id. (required)
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
        value = PurchaseRequestApi.PATH_DELETE_PURCHASE_REQUEST_COURSE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePurchaseRequestCourse(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @PathVariable("purchaseRequestCourseId") UUID purchaseRequestCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PURCHASE_REQUEST_SUBJECT = "/v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}";
    /**
     * DELETE /v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId} : Delete PurchaseRequestSubject
     * Soft delete (sets deleted_at).
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestSubjectId PurchaseRequestSubject id. (required)
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
        value = PurchaseRequestApi.PATH_DELETE_PURCHASE_REQUEST_SUBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePurchaseRequestSubject(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @PathVariable("purchaseRequestSubjectId") UUID purchaseRequestSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_PURCHASE_REQUEST = "/v1/library/purchase-requests/{purchaseRequestId}";
    /**
     * GET /v1/library/purchase-requests/{purchaseRequestId} : Get PurchaseRequest
     * Get one PurchaseRequest by id. The ETag header carries the row version.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
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
        value = PurchaseRequestApi.PATH_GET_PURCHASE_REQUEST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PurchaseRequest> getPurchaseRequest(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PURCHASE_REQUEST_COURSE = "/v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}";
    /**
     * GET /v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId} : Get PurchaseRequestCourse
     * Get one PurchaseRequestCourse by id. The ETag header carries the row version.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestCourseId PurchaseRequestCourse id. (required)
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
        value = PurchaseRequestApi.PATH_GET_PURCHASE_REQUEST_COURSE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PurchaseRequestCourse> getPurchaseRequestCourse(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @PathVariable("purchaseRequestCourseId") UUID purchaseRequestCourseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PURCHASE_REQUEST_SUBJECT = "/v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}";
    /**
     * GET /v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId} : Get PurchaseRequestSubject
     * Get one PurchaseRequestSubject by id. The ETag header carries the row version.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestSubjectId PurchaseRequestSubject id. (required)
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
        value = PurchaseRequestApi.PATH_GET_PURCHASE_REQUEST_SUBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PurchaseRequestSubject> getPurchaseRequestSubject(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @PathVariable("purchaseRequestSubjectId") UUID purchaseRequestSubjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PURCHASE_REQUEST_COURSES = "/v1/library/purchase-requests/{purchaseRequestId}/courses";
    /**
     * GET /v1/library/purchase-requests/{purchaseRequestId}/courses : List PurchaseRequestCourses
     * List PurchaseRequestCourses visible to the caller, page by page, with filtering and sorting.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
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
        value = PurchaseRequestApi.PATH_LIST_PURCHASE_REQUEST_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PurchaseRequestCourse>> listPurchaseRequestCourses(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PURCHASE_REQUEST_SUBJECTS = "/v1/library/purchase-requests/{purchaseRequestId}/subjects";
    /**
     * GET /v1/library/purchase-requests/{purchaseRequestId}/subjects : List PurchaseRequestSubjects
     * List PurchaseRequestSubjects visible to the caller, page by page, with filtering and sorting.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
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
        value = PurchaseRequestApi.PATH_LIST_PURCHASE_REQUEST_SUBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PurchaseRequestSubject>> listPurchaseRequestSubjects(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PURCHASE_REQUESTS = "/v1/library/purchase-requests";
    /**
     * GET /v1/library/purchase-requests : List PurchaseRequests
     * List PurchaseRequests visible to the caller, page by page, with filtering and sorting.
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
        value = PurchaseRequestApi.PATH_LIST_PURCHASE_REQUESTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PurchaseRequest>> listPurchaseRequests(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REJECT_PURCHASE_REQUEST = "/v1/library/purchase-requests/{purchaseRequestId}:reject";
    /**
     * POST /v1/library/purchase-requests/{purchaseRequestId}:reject : Reject
     * Reject.
     *
     * @param purchaseRequestId  (required)
     * @param rejectPurchaseRequestRequest  (required)
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
        value = PurchaseRequestApi.PATH_REJECT_PURCHASE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequest> rejectPurchaseRequest(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @Valid @RequestBody RejectPurchaseRequestRequest rejectPurchaseRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_PURCHASE_REQUEST = "/v1/library/purchase-requests/{purchaseRequestId}:submit";
    /**
     * POST /v1/library/purchase-requests/{purchaseRequestId}:submit : Send to the librarian
     * Send to the librarian.
     *
     * @param purchaseRequestId  (required)
     * @param submitPurchaseRequestRequest  (required)
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
        value = PurchaseRequestApi.PATH_SUBMIT_PURCHASE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequest> submitPurchaseRequest(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @Valid @RequestBody SubmitPurchaseRequestRequest submitPurchaseRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_PURCHASE_REQUEST = "/v1/library/purchase-requests/{purchaseRequestId}";
    /**
     * PATCH /v1/library/purchase-requests/{purchaseRequestId} : Update PurchaseRequest
     * Partially update a PurchaseRequest (updateMask). Requires If-Match with the current ETag.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param purchaseRequestUpdate  (required)
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
        value = PurchaseRequestApi.PATH_UPDATE_PURCHASE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequest> updatePurchaseRequest(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PurchaseRequestUpdate purchaseRequestUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PURCHASE_REQUEST_COURSE = "/v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId}";
    /**
     * PATCH /v1/library/purchase-requests/{purchaseRequestId}/courses/{purchaseRequestCourseId} : Update PurchaseRequestCourse
     * Partially update a PurchaseRequestCourse (updateMask). Requires If-Match with the current ETag.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestCourseId PurchaseRequestCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param purchaseRequestCourseUpdate  (required)
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
        value = PurchaseRequestApi.PATH_UPDATE_PURCHASE_REQUEST_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequestCourse> updatePurchaseRequestCourse(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @PathVariable("purchaseRequestCourseId") UUID purchaseRequestCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PurchaseRequestCourseUpdate purchaseRequestCourseUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PURCHASE_REQUEST_SUBJECT = "/v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId}";
    /**
     * PATCH /v1/library/purchase-requests/{purchaseRequestId}/subjects/{purchaseRequestSubjectId} : Update PurchaseRequestSubject
     * Partially update a PurchaseRequestSubject (updateMask). Requires If-Match with the current ETag.
     *
     * @param purchaseRequestId PurchaseRequest id. (required)
     * @param purchaseRequestSubjectId PurchaseRequestSubject id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param purchaseRequestSubjectUpdate  (required)
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
        value = PurchaseRequestApi.PATH_UPDATE_PURCHASE_REQUEST_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PurchaseRequestSubject> updatePurchaseRequestSubject(
         @PathVariable("purchaseRequestId") UUID purchaseRequestId,
         @PathVariable("purchaseRequestSubjectId") UUID purchaseRequestSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PurchaseRequestSubjectUpdate purchaseRequestSubjectUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
