// Owned API contract of admission (JAVA-CODING-RULES §7); first materialized from api/openapi/admission.yaml on 2026-11-10.
package com.mysms.admission.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.admission.api.model.AdmissionCycle;
import com.mysms.admission.api.model.AdmissionCycleFee;
import com.mysms.admission.api.model.AdmissionCycleFeeUpdate;
import com.mysms.admission.api.model.AdmissionCycleUpdate;
import com.mysms.admission.api.model.CancelAdmissionCycleRequest;
import com.mysms.admission.api.model.CloseAdmissionCycleRequest;
import com.mysms.admission.api.model.ConfirmAdmissionCycleRequest;
import org.springframework.lang.Nullable;
import com.mysms.admission.api.model.OpenApplicationsRequest;
import com.mysms.admission.api.model.ResetAdmissionCycleRequest;
import com.mysms.admission.api.model.StartAdmissionRequest;
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
public interface AdmissionCycleApi {

    String PATH_CANCEL_ADMISSION_CYCLE = "/v1/admission/admission-cycles/{admissionCycleId}:cancel";
    /**
     * POST /v1/admission/admission-cycles/{admissionCycleId}:cancel : Cancel
     * Cancel.
     *
     * @param admissionCycleId  (required)
     * @param cancelAdmissionCycleRequest  (required)
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
        value = AdmissionCycleApi.PATH_CANCEL_ADMISSION_CYCLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> cancelAdmissionCycle(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestBody CancelAdmissionCycleRequest cancelAdmissionCycleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CLOSE_ADMISSION_CYCLE = "/v1/admission/admission-cycles/{admissionCycleId}:close";
    /**
     * POST /v1/admission/admission-cycles/{admissionCycleId}:close : Close
     * Close.
     *
     * @param admissionCycleId  (required)
     * @param closeAdmissionCycleRequest  (required)
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
        value = AdmissionCycleApi.PATH_CLOSE_ADMISSION_CYCLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> closeAdmissionCycle(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestBody CloseAdmissionCycleRequest closeAdmissionCycleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CONFIRM_ADMISSION_CYCLE = "/v1/admission/admission-cycles/{admissionCycleId}:confirm";
    /**
     * POST /v1/admission/admission-cycles/{admissionCycleId}:confirm : Confirm the cycle
     * Confirm the cycle.
     *
     * @param admissionCycleId  (required)
     * @param confirmAdmissionCycleRequest  (required)
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
        value = AdmissionCycleApi.PATH_CONFIRM_ADMISSION_CYCLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> confirmAdmissionCycle(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestBody ConfirmAdmissionCycleRequest confirmAdmissionCycleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ADMISSION_CYCLE = "/v1/admission/admission-cycles";
    /**
     * POST /v1/admission/admission-cycles : Create AdmissionCycle
     * Create a AdmissionCycle. Supports Idempotency-Key and validateOnly.
     *
     * @param admissionCycle  (required)
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
        value = AdmissionCycleApi.PATH_CREATE_ADMISSION_CYCLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> createAdmissionCycle(
         @Valid @RequestBody AdmissionCycle admissionCycle,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ADMISSION_CYCLE_FEE = "/v1/admission/admission-cycles/{admissionCycleId}/fees";
    /**
     * POST /v1/admission/admission-cycles/{admissionCycleId}/fees : Create AdmissionCycleFee
     * Create a AdmissionCycleFee. Supports Idempotency-Key and validateOnly.
     *
     * @param admissionCycleId AdmissionCycle id. (required)
     * @param admissionCycleFee  (required)
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
        value = AdmissionCycleApi.PATH_CREATE_ADMISSION_CYCLE_FEE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycleFee> createAdmissionCycleFee(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestBody AdmissionCycleFee admissionCycleFee,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ADMISSION_CYCLE = "/v1/admission/admission-cycles/{admissionCycleId}";
    /**
     * DELETE /v1/admission/admission-cycles/{admissionCycleId} : Delete AdmissionCycle
     * Soft delete (sets deleted_at).
     *
     * @param admissionCycleId AdmissionCycle id. (required)
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
        value = AdmissionCycleApi.PATH_DELETE_ADMISSION_CYCLE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAdmissionCycle(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ADMISSION_CYCLE_FEE = "/v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}";
    /**
     * DELETE /v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId} : Delete AdmissionCycleFee
     * Soft delete (sets deleted_at).
     *
     * @param admissionCycleId AdmissionCycle id. (required)
     * @param admissionCycleFeeId AdmissionCycleFee id. (required)
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
        value = AdmissionCycleApi.PATH_DELETE_ADMISSION_CYCLE_FEE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAdmissionCycleFee(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @PathVariable("admissionCycleFeeId") UUID admissionCycleFeeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_ADMISSION_CYCLE = "/v1/admission/admission-cycles/{admissionCycleId}";
    /**
     * GET /v1/admission/admission-cycles/{admissionCycleId} : Get AdmissionCycle
     * Get one AdmissionCycle by id. The ETag header carries the row version.
     *
     * @param admissionCycleId AdmissionCycle id. (required)
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
        value = AdmissionCycleApi.PATH_GET_ADMISSION_CYCLE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AdmissionCycle> getAdmissionCycle(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ADMISSION_CYCLE_FEE = "/v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}";
    /**
     * GET /v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId} : Get AdmissionCycleFee
     * Get one AdmissionCycleFee by id. The ETag header carries the row version.
     *
     * @param admissionCycleId AdmissionCycle id. (required)
     * @param admissionCycleFeeId AdmissionCycleFee id. (required)
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
        value = AdmissionCycleApi.PATH_GET_ADMISSION_CYCLE_FEE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AdmissionCycleFee> getAdmissionCycleFee(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @PathVariable("admissionCycleFeeId") UUID admissionCycleFeeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ADMISSION_CYCLE_FEES = "/v1/admission/admission-cycles/{admissionCycleId}/fees";
    /**
     * GET /v1/admission/admission-cycles/{admissionCycleId}/fees : List AdmissionCycleFees
     * List AdmissionCycleFees visible to the caller, page by page, with filtering and sorting.
     *
     * @param admissionCycleId AdmissionCycle id. (required)
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
        value = AdmissionCycleApi.PATH_LIST_ADMISSION_CYCLE_FEES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AdmissionCycleFee>> listAdmissionCycleFees(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ADMISSION_CYCLES = "/v1/admission/admission-cycles";
    /**
     * GET /v1/admission/admission-cycles : List AdmissionCycles
     * List AdmissionCycles visible to the caller, page by page, with filtering and sorting.
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
        value = AdmissionCycleApi.PATH_LIST_ADMISSION_CYCLES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AdmissionCycle>> listAdmissionCycles(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_OPEN_APPLICATIONS = "/v1/admission/admission-cycles/{admissionCycleId}:openApplications";
    /**
     * POST /v1/admission/admission-cycles/{admissionCycleId}:openApplications : Start accepting applications
     * Start accepting applications.
     *
     * @param admissionCycleId  (required)
     * @param openApplicationsRequest  (required)
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
        value = AdmissionCycleApi.PATH_OPEN_APPLICATIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> openApplications(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestBody OpenApplicationsRequest openApplicationsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_ADMISSION_CYCLE = "/v1/admission/admission-cycles/{admissionCycleId}:resetToDraft";
    /**
     * POST /v1/admission/admission-cycles/{admissionCycleId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param admissionCycleId  (required)
     * @param resetAdmissionCycleRequest  (required)
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
        value = AdmissionCycleApi.PATH_RESET_ADMISSION_CYCLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> resetAdmissionCycle(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestBody ResetAdmissionCycleRequest resetAdmissionCycleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_START_ADMISSION = "/v1/admission/admission-cycles/{admissionCycleId}:startAdmission";
    /**
     * POST /v1/admission/admission-cycles/{admissionCycleId}:startAdmission : Move to the admission stage
     * Move to the admission stage.
     *
     * @param admissionCycleId  (required)
     * @param startAdmissionRequest  (required)
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
        value = AdmissionCycleApi.PATH_START_ADMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> startAdmission(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @Valid @RequestBody StartAdmissionRequest startAdmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_ADMISSION_CYCLE = "/v1/admission/admission-cycles/{admissionCycleId}";
    /**
     * PATCH /v1/admission/admission-cycles/{admissionCycleId} : Update AdmissionCycle
     * Partially update a AdmissionCycle (updateMask). Requires If-Match with the current ETag.
     *
     * @param admissionCycleId AdmissionCycle id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param admissionCycleUpdate  (required)
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
        value = AdmissionCycleApi.PATH_UPDATE_ADMISSION_CYCLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycle> updateAdmissionCycle(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AdmissionCycleUpdate admissionCycleUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ADMISSION_CYCLE_FEE = "/v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId}";
    /**
     * PATCH /v1/admission/admission-cycles/{admissionCycleId}/fees/{admissionCycleFeeId} : Update AdmissionCycleFee
     * Partially update a AdmissionCycleFee (updateMask). Requires If-Match with the current ETag.
     *
     * @param admissionCycleId AdmissionCycle id. (required)
     * @param admissionCycleFeeId AdmissionCycleFee id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param admissionCycleFeeUpdate  (required)
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
        value = AdmissionCycleApi.PATH_UPDATE_ADMISSION_CYCLE_FEE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AdmissionCycleFee> updateAdmissionCycleFee(
         @PathVariable("admissionCycleId") UUID admissionCycleId,
         @PathVariable("admissionCycleFeeId") UUID admissionCycleFeeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AdmissionCycleFeeUpdate admissionCycleFeeUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
