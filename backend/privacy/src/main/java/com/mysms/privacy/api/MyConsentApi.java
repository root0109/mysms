// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.MyConsent;
import com.mysms.privacy.api.model.MyNotice;
import com.mysms.privacy.api.model.RecordMyConsentRequest;
import com.mysms.privacy.api.model.WithdrawMyConsentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public interface MyConsentApi {

    String PATH_GET_MY_NOTICE = "/v1/privacy/my/notices/{noticeCode}";
    /**
     * GET /v1/privacy/my/notices/{noticeCode} : Get my Notice
     * The active version of the tenant's notice with this code, in the language asked for (English when it has no such translation): what the consent centre shows before a decision.
     *
     * @param noticeCode Code of the notice within the tenant. (required)
     * @param language BCP 47 language of the wording wanted, e.g. hi. (required)
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
        value = MyConsentApi.PATH_GET_MY_NOTICE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MyNotice> getMyNotice(
         @Size(max = 60) @PathVariable("noticeCode") String noticeCode,
        @NotNull @Size(max = 10) @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")  @Valid @RequestParam(value = "language", required = true) String language
    );


    String PATH_LIST_MY_CONSENTS = "/v1/privacy/my/consents";
    /**
     * GET /v1/privacy/my/consents : List my Consents
     * The consent centre: one row per person the caller may decide for (themself as an adult, each minor they are the active legal guardian of) and purpose the tenant has switched on, with the caller's own decision and the effective result. Another guardian's decision is never shown.
     *
     * @return OK (status code 200)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = MyConsentApi.PATH_LIST_MY_CONSENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MyConsent>> listMyConsents();


    String PATH_RECORD_MY_CONSENT = "/v1/privacy/my/consents";
    /**
     * POST /v1/privacy/my/consents : Record my Consent
     * Record the caller's decision for themself (an adult) or a minor they are the active legal guardian of; a newer decision of the caller supersedes the live one. Refused while impersonating. Supports Idempotency-Key.
     *
     * @param recordMyConsentRequest  (required)
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
        value = MyConsentApi.PATH_RECORD_MY_CONSENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MyConsent> recordMyConsent(
         @Valid @RequestBody RecordMyConsentRequest recordMyConsentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_WITHDRAW_MY_CONSENT = "/v1/privacy/my/consents/{consentRecordId}:withdraw";
    /**
     * POST /v1/privacy/my/consents/{consentRecordId}:withdraw : Withdraw my Consent
     * Withdraw one of the caller's own live decisions. Refused while impersonating. Supports Idempotency-Key.
     *
     * @param consentRecordId ConsentRecord id. (required)
     * @param withdrawMyConsentRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = MyConsentApi.PATH_WITHDRAW_MY_CONSENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MyConsent> withdrawMyConsent(
         @PathVariable("consentRecordId") UUID consentRecordId,
         @Valid @RequestBody WithdrawMyConsentRequest withdrawMyConsentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );
}
