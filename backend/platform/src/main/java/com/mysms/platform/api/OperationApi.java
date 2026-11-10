// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.platform.api.model.Operation;
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
public interface OperationApi {

    String PATH_CANCEL_OPERATION = "/v1/platform/operations/{operationId}:cancel";
    /**
     * POST /v1/platform/operations/{operationId}:cancel : Request cancellation of a running operation
     * Request cancellation of a running operation started in this module.
     *
     * @param operationId  (required)
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
        value = OperationApi.PATH_CANCEL_OPERATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Operation> cancelOperation(
         @PathVariable("operationId") UUID operationId,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_OPERATION = "/v1/platform/operations/{operationId}";
    /**
     * GET /v1/platform/operations/{operationId} : Status of a long-running operation started in this module
     * Live status of a long-running operation (report, import, export, bulk action) started in the platform module. Operations started in another module are polled at that module (&#x60;/v1/{module}/operations/{operationId}&#x60;; the &#x60;module&#x60; field says which).
     *
     * @param operationId  (required)
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
        value = OperationApi.PATH_GET_OPERATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Operation> getOperation(
         @PathVariable("operationId") UUID operationId
    );


    String PATH_LIST_OPERATIONS = "/v1/platform/operations";
    /**
     * GET /v1/platform/operations : Job centre — the caller&#39;s operations across all modules
     * The caller&#39;s long-running operations across all modules (job centre). Platform keeps this index from the operation events every module publishes, so a row may lag the owning module by a few seconds; for live status or cancellation call the owning module&#39;s operation endpoints (&#x60;module&#x60; field).
     *
     * @param kind Filter by kind. (optional)
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
        value = OperationApi.PATH_LIST_OPERATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Operation>> listOperations(
         @Valid @RequestParam(value = "kind", required = false) @Nullable String kind,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );

}
