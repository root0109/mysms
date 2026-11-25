// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.AccessSummary;
import com.mysms.privacy.api.model.MyRightsRequest;
import com.mysms.privacy.api.model.SubmitMyRequestRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public interface MyRightsRequestApi {

    String PATH_GET_MY_ACCESS_SUMMARY = "/v1/privacy/my/requests/{rightsRequestId}/access-summary";
    /**
     * GET /v1/privacy/my/requests/{rightsRequestId}/access-summary : Get my access summary
     * The access summary the school sent in answer to one of the caller's own access requests, as it was when the request was fulfilled. Not found until then, and for any request the caller cannot see.
     *
     * @param rightsRequestId RightsRequest id. (required)
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
        value = MyRightsRequestApi.PATH_GET_MY_ACCESS_SUMMARY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AccessSummary> getMyAccessSummary(
         @PathVariable("rightsRequestId") UUID rightsRequestId
    );


    String PATH_GET_MY_REQUEST = "/v1/privacy/my/requests/{rightsRequestId}";
    /**
     * GET /v1/privacy/my/requests/{rightsRequestId} : Get my RightsRequest
     * One of the caller's own rights requests; another person's is not found.
     *
     * @param rightsRequestId RightsRequest id. (required)
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
        value = MyRightsRequestApi.PATH_GET_MY_REQUEST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MyRightsRequest> getMyRequest(
         @PathVariable("rightsRequestId") UUID rightsRequestId
    );


    String PATH_LIST_MY_REQUESTS = "/v1/privacy/my/requests";
    /**
     * GET /v1/privacy/my/requests : List my RightsRequests
     * The rights requests the caller submitted, newest first, page by page.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = MyRightsRequestApi.PATH_LIST_MY_REQUESTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MyRightsRequest>> listMyRequests(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_SUBMIT_MY_REQUEST = "/v1/privacy/my/requests";
    /**
     * POST /v1/privacy/my/requests : Submit my RightsRequest
     * Submit a rights request (access, correction or grievance) for oneself (an adult) or a minor one is the active legal guardian of; the signed-in session verifies the requester, and the response deadline runs from submission. Refused while impersonating. Supports Idempotency-Key.
     *
     * @param submitMyRequestRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = MyRightsRequestApi.PATH_SUBMIT_MY_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MyRightsRequest> submitMyRequest(
         @Valid @RequestBody SubmitMyRequestRequest submitMyRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );
}
