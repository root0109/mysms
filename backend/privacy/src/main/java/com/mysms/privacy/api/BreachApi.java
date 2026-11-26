// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.Breach;
import com.mysms.privacy.api.model.BreachStepRequest;
import com.mysms.privacy.api.model.BreachUpdate;
import com.mysms.privacy.api.model.MarkBreachNotABreachRequest;
import com.mysms.privacy.api.model.RecordBreachNotificationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public interface BreachApi {

    String PATH_CLOSE_BREACH = "/v1/privacy/breaches/{breachId}:close";
    /**
     * POST /v1/privacy/breaches/{breachId}:close : Close Breach
     * Close a breach once the Board's detailed report and the principals' notice are recorded (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param breachId Breach id. (required)
     * @param breachStepRequest  (required)
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
        value = BreachApi.PATH_CLOSE_BREACH,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Breach> closeBreach(
         @PathVariable("breachId") UUID breachId,
         @Valid @RequestBody BreachStepRequest breachStepRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CONTAIN_BREACH = "/v1/privacy/breaches/{breachId}:contain";
    /**
     * POST /v1/privacy/breaches/{breachId}:contain : Contain Breach
     * Record that a triaged breach is contained (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param breachId Breach id. (required)
     * @param breachStepRequest  (required)
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
        value = BreachApi.PATH_CONTAIN_BREACH,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Breach> containBreach(
         @PathVariable("breachId") UUID breachId,
         @Valid @RequestBody BreachStepRequest breachStepRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_BREACH = "/v1/privacy/breaches/{breachId}";
    /**
     * GET /v1/privacy/breaches/{breachId} : Get Breach
     * Get one breach of the tenant's register by id, with its notices and the Board's clock. The ETag header carries the row version.
     *
     * @param breachId Breach id. (required)
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
        value = BreachApi.PATH_GET_BREACH,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Breach> getBreach(
         @PathVariable("breachId") UUID breachId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_BREACHES = "/v1/privacy/breaches";
    /**
     * GET /v1/privacy/breaches : List Breaches
     * The tenant's breach register, page by page, the Board's nearest deadline first; filter by status, becameAwareAt, boardReportDueAt and the other fields.
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
        value = BreachApi.PATH_LIST_BREACHES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Breach>> listBreaches(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_BREACH_NOT_A_BREACH = "/v1/privacy/breaches/{breachId}:markNotABreach";
    /**
     * POST /v1/privacy/breaches/{breachId}:markNotABreach : Mark Breach not a breach
     * Record that triage found a triaged breach not to be a personal data breach, with the reason, which ends it (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param breachId Breach id. (required)
     * @param markBreachNotABreachRequest  (required)
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
        value = BreachApi.PATH_MARK_BREACH_NOT_A_BREACH,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Breach> markBreachNotABreach(
         @PathVariable("breachId") UUID breachId,
         @Valid @RequestBody MarkBreachNotABreachRequest markBreachNotABreachRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RECORD_BREACH_NOTIFICATION = "/v1/privacy/breaches/{breachId}:recordNotification";
    /**
     * POST /v1/privacy/breaches/{breachId}:recordNotification : Record a Breach notification
     * Record a notice about the breach sent outside MySMS (to the Board, the principals, a processor or the tenant); the Board's initial intimation, its detailed report and the principals' notice move the status (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param breachId Breach id. (required)
     * @param recordBreachNotificationRequest  (required)
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
        value = BreachApi.PATH_RECORD_BREACH_NOTIFICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Breach> recordBreachNotification(
         @PathVariable("breachId") UUID breachId,
         @Valid @RequestBody RecordBreachNotificationRequest recordBreachNotificationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REPORT_BREACH = "/v1/privacy/breaches";
    /**
     * POST /v1/privacy/breaches : Report Breach
     * Report a breach into the tenant's register: numbered, handled by the reporter, its Board report due 72 hours after the school became aware (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param breach  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = BreachApi.PATH_REPORT_BREACH,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Breach> reportBreach(
         @Valid @RequestBody Breach breach,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_TRIAGE_BREACH = "/v1/privacy/breaches/{breachId}:triage";
    /**
     * POST /v1/privacy/breaches/{breachId}:triage : Triage Breach
     * Record that a reported breach was triaged (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param breachId Breach id. (required)
     * @param breachStepRequest  (required)
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
        value = BreachApi.PATH_TRIAGE_BREACH,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Breach> triageBreach(
         @PathVariable("breachId") UUID breachId,
         @Valid @RequestBody BreachStepRequest breachStepRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_BREACH = "/v1/privacy/breaches/{breachId}";
    /**
     * PATCH /v1/privacy/breaches/{breachId} : Update Breach
     * Partially update what is known of an open breach (absent fields are kept, null clears a field); its times and the Board's clock are fixed once reported. Requires If-Match with the current ETag and a tenant-wide grant. Refused while impersonating.
     *
     * @param breachId Breach id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param breachUpdate  (required)
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
        value = BreachApi.PATH_UPDATE_BREACH,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Breach> updateBreach(
         @PathVariable("breachId") UUID breachId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody BreachUpdate breachUpdate
    );
}
