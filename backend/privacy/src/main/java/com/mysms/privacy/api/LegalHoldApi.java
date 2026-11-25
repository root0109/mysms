// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.LegalHold;
import com.mysms.privacy.api.model.LegalHoldUpdate;
import com.mysms.privacy.api.model.ListActiveHoldsResponse;
import com.mysms.privacy.api.model.ReleaseLegalHoldRequest;
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
public interface LegalHoldApi {

    String PATH_GET_LEGAL_HOLD = "/v1/privacy/legal-holds/{legalHoldId}";
    /**
     * GET /v1/privacy/legal-holds/{legalHoldId} : Get LegalHold
     * Get one legal hold of the tenant by id. The ETag header carries the row version.
     *
     * @param legalHoldId Legal hold id. (required)
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
        value = LegalHoldApi.PATH_GET_LEGAL_HOLD,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LegalHold> getLegalHold(
         @PathVariable("legalHoldId") UUID legalHoldId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ACTIVE_HOLDS = "/v1/privacy/legal-holds:active";
    /**
     * GET /v1/privacy/legal-holds:active : List active holds
     * The active legal holds that may cover a subject's data, for a module erasing or sweeping it (internal: called with a service token): the subject's subject-wide holds and every category-wide or record-set hold of the subject or of everyone, record sets narrowed to the module when one is named.
     *
     * @param subjectPersonId The data principal whose data the module is about to remove. (required)
     * @param module Only record-set holds of this module (a module code such as sis); absent = every module's. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = LegalHoldApi.PATH_LIST_ACTIVE_HOLDS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ListActiveHoldsResponse> listActiveHolds(
        @NotNull @Valid @RequestParam(value = "subjectPersonId", required = true) UUID subjectPersonId,
        @Size(max = 30) @Valid @RequestParam(value = "module", required = false) @Nullable String module
    );


    String PATH_LIST_LEGAL_HOLDS = "/v1/privacy/legal-holds";
    /**
     * GET /v1/privacy/legal-holds : List LegalHolds
     * The tenant's legal holds, page by page, the nearest review date first; filter by active, reviewBy (review due: active = true AND reviewBy <= today), reasonCode, scope, subjectPersonId and the other fields.
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
        value = LegalHoldApi.PATH_LIST_LEGAL_HOLDS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LegalHold>> listLegalHolds(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PLACE_LEGAL_HOLD = "/v1/privacy/legal-holds";
    /**
     * POST /v1/privacy/legal-holds : Place LegalHold
     * Place a legal hold on a subject's data, a data category or a set of records, with its reason and review date; no erasure or retention removes what it holds until it is released (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param legalHold  (required)
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
        value = LegalHoldApi.PATH_PLACE_LEGAL_HOLD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LegalHold> placeLegalHold(
         @Valid @RequestBody LegalHold legalHold,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RELEASE_LEGAL_HOLD = "/v1/privacy/legal-holds/{legalHoldId}:release";
    /**
     * POST /v1/privacy/legal-holds/{legalHoldId}:release : Release LegalHold
     * Release an active legal hold: from then on erasure and retention treat what it held as any other data (a tenant-wide grant is required). Refused while impersonating. Supports Idempotency-Key.
     *
     * @param legalHoldId Legal hold id. (required)
     * @param releaseLegalHoldRequest  (required)
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
        value = LegalHoldApi.PATH_RELEASE_LEGAL_HOLD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LegalHold> releaseLegalHold(
         @PathVariable("legalHoldId") UUID legalHoldId,
         @Valid @RequestBody ReleaseLegalHoldRequest releaseLegalHoldRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_LEGAL_HOLD = "/v1/privacy/legal-holds/{legalHoldId}";
    /**
     * PATCH /v1/privacy/legal-holds/{legalHoldId} : Update LegalHold
     * Move an active hold's review date (updateMask); what it holds is fixed once placed. Requires If-Match with the current ETag and a tenant-wide grant. Refused while impersonating.
     *
     * @param legalHoldId Legal hold id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param legalHoldUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
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
        value = LegalHoldApi.PATH_UPDATE_LEGAL_HOLD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LegalHold> updateLegalHold(
         @PathVariable("legalHoldId") UUID legalHoldId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LegalHoldUpdate legalHoldUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask
    );
}
