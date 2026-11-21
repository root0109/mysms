// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.ConsentRecord;
import com.mysms.privacy.api.model.RecordConsentOnBehalfRequest;
import com.mysms.privacy.api.model.VerifyConsentRecordRequest;
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
public interface ConsentRecordApi {

    String PATH_GET_CONSENT_RECORD = "/v1/privacy/consent-records/{consentRecordId}";
    /**
     * GET /v1/privacy/consent-records/{consentRecordId} : Get ConsentRecord
     * Get one consent record of the tenant by id. The ETag header carries the row version.
     *
     * @param consentRecordId ConsentRecord id. (required)
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
        value = ConsentRecordApi.PATH_GET_CONSENT_RECORD,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ConsentRecord> getConsentRecord(
         @PathVariable("consentRecordId") UUID consentRecordId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CONSENT_RECORDS = "/v1/privacy/consent-records";
    /**
     * GET /v1/privacy/consent-records : List ConsentRecords
     * List the tenant's consent records, page by page; filter by subjectPersonId, giverPersonId, purposeCode, verificationStatus and the other fields.
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
        value = ConsentRecordApi.PATH_LIST_CONSENT_RECORDS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ConsentRecord>> listConsentRecords(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RECORD_CONSENT_ON_BEHALF = "/v1/privacy/consent-records";
    /**
     * POST /v1/privacy/consent-records : Record Consent on behalf
     * Record a signed paper form for its giver (a tenant-wide grant is required). The record is unverified, and does not count, until another staff member verifies it; a newer decision of the giver supersedes the live one. Supports Idempotency-Key.
     *
     * @param recordConsentOnBehalfRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Created (status code 201)
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
        value = ConsentRecordApi.PATH_RECORD_CONSENT_ON_BEHALF,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConsentRecord> recordConsentOnBehalf(
         @Valid @RequestBody RecordConsentOnBehalfRequest recordConsentOnBehalfRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_VERIFY_CONSENT_RECORD = "/v1/privacy/consent-records/{consentRecordId}:verify";
    /**
     * POST /v1/privacy/consent-records/{consentRecordId}:verify : Verify ConsentRecord
     * Verify an unverified paper consent (a tenant-wide grant is required); the verifier must not be the staff member who recorded it. From then on it counts. Supports Idempotency-Key.
     *
     * @param consentRecordId ConsentRecord id. (required)
     * @param verifyConsentRecordRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = ConsentRecordApi.PATH_VERIFY_CONSENT_RECORD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConsentRecord> verifyConsentRecord(
         @PathVariable("consentRecordId") UUID consentRecordId,
         @Valid @RequestBody VerifyConsentRecordRequest verifyConsentRecordRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );
}
