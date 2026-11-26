// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.AccessSummary;
import com.mysms.privacy.api.model.AssignRightsRequestRequest;
import com.mysms.privacy.api.model.CloseRightsRequestRequest;
import com.mysms.privacy.api.model.ExtendRightsRequestRequest;
import com.mysms.privacy.api.model.FulfilAccessRequestRequest;
import com.mysms.privacy.api.model.FulfilRightsRequestRequest;
import com.mysms.privacy.api.model.RejectRightsRequestRequest;
import com.mysms.privacy.api.model.RetryRightsRequestTaskRequest;
import com.mysms.privacy.api.model.RightsRequest;
import com.mysms.privacy.api.model.VerifyRightsRequestIdentityRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
public interface RightsRequestApi {

    String PATH_ASSIGN_RIGHTS_REQUEST = "/v1/privacy/rights-requests/{rightsRequestId}:assign";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:assign : Assign RightsRequest
     * Assign a verified request, or reassign one in progress, to a staff member (a tenant-wide grant is required); it is then in progress. Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param assignRightsRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_ASSIGN_RIGHTS_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> assignRightsRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody AssignRightsRequestRequest assignRightsRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CLOSE_RIGHTS_REQUEST = "/v1/privacy/rights-requests/{rightsRequestId}:close";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:close : Close RightsRequest
     * Close a request in progress with its outcome (a tenant-wide grant is required). Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param closeRightsRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_CLOSE_RIGHTS_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> closeRightsRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody CloseRightsRequestRequest closeRightsRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_EXTEND_RIGHTS_REQUEST = "/v1/privacy/rights-requests/{rightsRequestId}:extend";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:extend : Extend RightsRequest
     * Move an open request's response deadline later, with the reason (a tenant-wide grant is required); the status does not change. Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param extendRightsRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_EXTEND_RIGHTS_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> extendRightsRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody ExtendRightsRequestRequest extendRightsRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_FULFIL_ACCESS_REQUEST = "/v1/privacy/rights-requests/{rightsRequestId}:fulfilAccess";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:fulfilAccess : Fulfil an access RightsRequest
     * Answer an access request in progress with the subject's access summary, generated now and kept on the request, and close it as fulfilled (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param fulfilAccessRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_FULFIL_ACCESS_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> fulfilAccessRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody FulfilAccessRequestRequest fulfilAccessRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_FULFIL_WITHDRAW_ALL_REQUEST = "/v1/privacy/rights-requests/{rightsRequestId}:fulfilWithdrawAll";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:fulfilWithdrawAll : Fulfil a withdraw-all RightsRequest
     * Answer a withdraw-all request in progress: every live consent decision the requester gave about the subject (the principal's own when the requester is their nominee) is withdrawn, with its evidence, and the request is closed as fulfilled (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param fulfilRightsRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_FULFIL_WITHDRAW_ALL_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> fulfilWithdrawAllRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody FulfilRightsRequestRequest fulfilRightsRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_ACCESS_SUMMARY = "/v1/privacy/rights-requests/{rightsRequestId}/access-summary";
    /**
     * GET /v1/privacy/rights-requests/{rightsRequestId}/access-summary : Get the access summary of a RightsRequest
     * The access summary of an access request: the one kept on it once fulfilled, until then a preview of what the processing register holds about the subject now. Another type of request is a conflict.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = RightsRequestApi.PATH_GET_ACCESS_SUMMARY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AccessSummary> getAccessSummary(
         @PathVariable("rightsRequestId") UUID rightsRequestId
    );


    String PATH_GET_RIGHTS_REQUEST = "/v1/privacy/rights-requests/{rightsRequestId}";
    /**
     * GET /v1/privacy/rights-requests/{rightsRequestId} : Get RightsRequest
     * Get one rights request of the tenant by id. The ETag header carries the row version.
     *
     * @param rightsRequestId RightsRequest id. (required)
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
        value = RightsRequestApi.PATH_GET_RIGHTS_REQUEST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RightsRequest> getRightsRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RIGHTS_REQUESTS = "/v1/privacy/rights-requests";
    /**
     * GET /v1/privacy/rights-requests : List RightsRequests
     * List the tenant's rights requests, page by page, nearest deadline first; filter by status, type, dueAt and the other fields.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = RightsRequestApi.PATH_LIST_RIGHTS_REQUESTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RightsRequest>> listRightsRequests(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REJECT_RIGHTS_REQUEST = "/v1/privacy/rights-requests/{rightsRequestId}:reject";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:reject : Reject RightsRequest
     * Reject a submitted or verified request with a reason code (a tenant-wide grant is required); the requester may appeal it as a grievance. Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param rejectRightsRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_REJECT_RIGHTS_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> rejectRightsRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody RejectRightsRequestRequest rejectRightsRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RETRY_RIGHTS_REQUEST_TASK = "/v1/privacy/rights-requests/{rightsRequestId}/tasks/{taskId}:retry";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}/tasks/{taskId}:retry : Retry a RightsRequest task
     * Dispatch a failed or unanswered task of an erasure or export request to its module again, under the same task id; a request waiting for action goes back in progress (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param taskId Task id. (required)
     * @param retryRightsRequestTaskRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_RETRY_RIGHTS_REQUEST_TASK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> retryRightsRequestTask(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @PathVariable("taskId") UUID taskId,
         @Valid @RequestBody RetryRightsRequestTaskRequest retryRightsRequestTaskRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_START_RIGHTS_REQUEST_FULFILMENT = "/v1/privacy/rights-requests/{rightsRequestId}:startFulfilment";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:startFulfilment : Start the fulfilment of a RightsRequest
     * Start fulfilling an erasure or export request in progress: one task per module holding personal data, dispatched to the modules (for an erasure, comms and iam first); the request is fulfilled as the modules answer (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param fulfilRightsRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_START_RIGHTS_REQUEST_FULFILMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> startRightsRequestFulfilment(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody FulfilRightsRequestRequest fulfilRightsRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_VERIFY_RIGHTS_REQUEST_IDENTITY = "/v1/privacy/rights-requests/{rightsRequestId}:verifyIdentity";
    /**
     * POST /v1/privacy/rights-requests/{rightsRequestId}:verifyIdentity : Verify RightsRequest identity
     * Record that the requester of a submitted request was verified in person or by document (a tenant-wide grant is required). Supports Idempotency-Key.
     *
     * @param rightsRequestId RightsRequest id. (required)
     * @param verifyRightsRequestIdentityRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = RightsRequestApi.PATH_VERIFY_RIGHTS_REQUEST_IDENTITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RightsRequest> verifyRightsRequestIdentity(
         @PathVariable("rightsRequestId") UUID rightsRequestId,
         @Valid @RequestBody VerifyRightsRequestIdentityRequest verifyRightsRequestIdentityRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );
}
