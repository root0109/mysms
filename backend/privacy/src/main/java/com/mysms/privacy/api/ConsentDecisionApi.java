// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.BatchCheckConsentRequest;
import com.mysms.privacy.api.model.BatchCheckConsentResponse;
import com.mysms.privacy.api.model.ConsentDecision;
import com.mysms.privacy.api.model.ConsentedSubject;
import com.mysms.privacy.api.model.PurposeStatus;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/** Consent decisions for the other modules (design §6.2). Internal: service callers only. */
@Validated
public interface ConsentDecisionApi {

    String PATH_CHECK_CONSENT = "/v1/privacy/consent-decisions:check";
    /**
     * GET /v1/privacy/consent-decisions:check : Check Consent
     * Whether one subject's data may be processed for an activity at a width (internal: called with a service token).
     *
     * @param activityCode The processing activity, e.g. comms.marketing_broadcast. (required)
     * @param subjectPersonId The person the decision is about. (required)
     * @param scope The width the processing reaches: class, institution, inter_school, public or tenant. (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = ConsentDecisionApi.PATH_CHECK_CONSENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ConsentDecision> checkConsent(
        @NotNull @Size(max = 60) @Valid @RequestParam(value = "activityCode", required = true) String activityCode,
        @NotNull @Valid @RequestParam(value = "subjectPersonId", required = true) UUID subjectPersonId,
        @NotNull @Valid @RequestParam(value = "scope", required = true) String scope
    );


    String PATH_BATCH_CHECK_CONSENT = "/v1/privacy/consent-decisions:batchCheck";
    /**
     * POST /v1/privacy/consent-decisions:batchCheck : Batch Check Consent
     * One decision per subject (up to 1000), in the order asked (internal: called with a service token). Reads only.
     *
     * @param batchCheckConsentRequest  (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = ConsentDecisionApi.PATH_BATCH_CHECK_CONSENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<BatchCheckConsentResponse> batchCheckConsent(
        @Valid @RequestBody BatchCheckConsentRequest batchCheckConsentRequest
    );


    String PATH_GET_PURPOSE_STATUS = "/v1/privacy/consent-decisions:purposeStatus";
    /**
     * GET /v1/privacy/consent-decisions:purposeStatus : Get Purpose Status
     * The activity's legal basis and the calling tenant's setting of its purpose (internal: called with a service token).
     *
     * @param activityCode The processing activity. (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = ConsentDecisionApi.PATH_GET_PURPOSE_STATUS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PurposeStatus> getPurposeStatus(
        @NotNull @Size(max = 60) @Valid @RequestParam(value = "activityCode", required = true) String activityCode
    );


    String PATH_LIST_SUBJECTS_WITH_CONSENT = "/v1/privacy/consent-decisions:subjects";
    /**
     * GET /v1/privacy/consent-decisions:subjects : List Subjects With Consent
     * The subjects whose effective consent allows an activity at a width, optionally of some institutions, in person
     * order (internal: called with a service token).
     *
     * @param activityCode The processing activity; it must rest on consent. (required)
     * @param scope The width the processing reaches. (required)
     * @param institutionIds Only subjects of these institutions; omitted (or empty) = every institution. (optional)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = ConsentDecisionApi.PATH_LIST_SUBJECTS_WITH_CONSENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ConsentedSubject>> listSubjectsWithConsent(
        @NotNull @Size(max = 60) @Valid @RequestParam(value = "activityCode", required = true) String activityCode,
        @NotNull @Valid @RequestParam(value = "scope", required = true) String scope,
         @RequestParam(value = "institutionIds", required = false) @Nullable List<UUID> institutionIds,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );
}
