// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.RetentionPolicy;
import com.mysms.privacy.api.model.RetentionPolicyUpdate;
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
public interface RetentionPolicyApi {

    String PATH_GET_RETENTION_POLICY = "/v1/privacy/retention-policies/{retentionPolicyId}";
    /**
     * GET /v1/privacy/retention-policies/{retentionPolicyId} : Get RetentionPolicy
     * Get one retention policy with the tenant's setting. The ETag header carries the setting's row version (0 while never set).
     *
     * @param retentionPolicyId Retention policy id (the catalogue's). (required)
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
        value = RetentionPolicyApi.PATH_GET_RETENTION_POLICY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RetentionPolicy> getRetentionPolicy(
         @PathVariable("retentionPolicyId") UUID retentionPolicyId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RETENTION_POLICIES = "/v1/privacy/retention-policies";
    /**
     * GET /v1/privacy/retention-policies : List RetentionPolicies
     * The retention policies of the catalogue with the tenant's own period and action where it set them, page by page by code; filter by categoryCode, module, trigger and the other fields.
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
        value = RetentionPolicyApi.PATH_LIST_RETENTION_POLICIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RetentionPolicy>> listRetentionPolicies(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_RETENTION_POLICY = "/v1/privacy/retention-policies/{retentionPolicyId}";
    /**
     * PATCH /v1/privacy/retention-policies/{retentionPolicyId} : Update RetentionPolicy
     * Set the tenant's own period and action of a retention policy (updateMask); a period below the legal floor is refused (422 RETENTION_BELOW_LEGAL_FLOOR). Requires If-Match with the current ETag (0 while never set) and a tenant-wide grant. Refused while impersonating.
     *
     * @param retentionPolicyId Retention policy id (the catalogue's). (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param retentionPolicyUpdate  (required)
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
        value = RetentionPolicyApi.PATH_UPDATE_RETENTION_POLICY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RetentionPolicy> updateRetentionPolicy(
         @PathVariable("retentionPolicyId") UUID retentionPolicyId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RetentionPolicyUpdate retentionPolicyUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask
    );
}
