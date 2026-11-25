// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.privacy.api.model.ConsentCoverage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
public interface ConsentCoverageApi {

    String PATH_GET_CONSENT_COVERAGE = "/v1/privacy/coverage";
    /**
     * GET /v1/privacy/coverage : Get Consent Coverage
     * How far a purpose is covered by consent: per width, how many data principals are allowed, denied by reason and not answered — over the whole tenant (a tenant-wide grant is required) or one institution's students. Counts only, never person ids.
     *
     * @param purposeCode The catalogue purpose, e.g. photo_video_publication. (required)
     * @param scope Only this width: class, institution, inter_school, public or tenant; absent = every width the purpose takes. (optional)
     * @param institutionId Only the students of this institution; absent = every data principal of the tenant. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = ConsentCoverageApi.PATH_GET_CONSENT_COVERAGE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ConsentCoverage> getConsentCoverage(
        @NotNull @Size(max = 40) @Valid @RequestParam(value = "purposeCode", required = true) String purposeCode,
         @Valid @RequestParam(value = "scope", required = false) @Nullable String scope,
         @Valid @RequestParam(value = "institutionId", required = false) @Nullable UUID institutionId
    );
}
