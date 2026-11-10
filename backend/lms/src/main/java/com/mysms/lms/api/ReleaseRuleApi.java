// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.CopyReleaseRuleRequest;
import com.mysms.lms.api.model.DisableReleaseRuleRequest;
import com.mysms.lms.api.model.EnableReleaseRuleRequest;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.ReleaseRule;
import com.mysms.lms.api.model.ReleaseRuleCondition;
import com.mysms.lms.api.model.ReleaseRuleConditionUpdate;
import com.mysms.lms.api.model.ReleaseRuleList;
import com.mysms.lms.api.model.ReleaseRuleTarget;
import com.mysms.lms.api.model.ReleaseRuleTargetUpdate;
import com.mysms.lms.api.model.ReleaseRuleUpdate;
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
public interface ReleaseRuleApi {

    String PATH_COPY_RELEASE_RULE = "/v1/lms/release-rules/{releaseRuleId}:copyTo";
    /**
     * POST /v1/lms/release-rules/{releaseRuleId}:copyTo : Copy a rule (conditions and targets) to other items
     * Copy a rule (conditions and targets) to other items.
     *
     * @param releaseRuleId  (required)
     * @param copyReleaseRuleRequest  (required)
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
        value = ReleaseRuleApi.PATH_COPY_RELEASE_RULE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRuleList> copyReleaseRule(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @Valid @RequestBody CopyReleaseRuleRequest copyReleaseRuleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_RELEASE_RULE = "/v1/lms/release-rules";
    /**
     * POST /v1/lms/release-rules : Create ReleaseRule
     * Create a ReleaseRule. Supports Idempotency-Key and validateOnly.
     *
     * @param releaseRule  (required)
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
        value = ReleaseRuleApi.PATH_CREATE_RELEASE_RULE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRule> createReleaseRule(
         @Valid @RequestBody ReleaseRule releaseRule,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RELEASE_RULE_CONDITION = "/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions";
    /**
     * POST /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions : Create ReleaseRuleCondition
     * Create a ReleaseRuleCondition. Supports Idempotency-Key and validateOnly.
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleCondition  (required)
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
        value = ReleaseRuleApi.PATH_CREATE_RELEASE_RULE_CONDITION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRuleCondition> createReleaseRuleCondition(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @Valid @RequestBody ReleaseRuleCondition releaseRuleCondition,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RELEASE_RULE_TARGET = "/v1/lms/release-rules/{releaseRuleId}/release-rule-targets";
    /**
     * POST /v1/lms/release-rules/{releaseRuleId}/release-rule-targets : Create ReleaseRuleTarget
     * Create a ReleaseRuleTarget. Supports Idempotency-Key and validateOnly.
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleTarget  (required)
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
        value = ReleaseRuleApi.PATH_CREATE_RELEASE_RULE_TARGET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRuleTarget> createReleaseRuleTarget(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @Valid @RequestBody ReleaseRuleTarget releaseRuleTarget,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_RELEASE_RULE = "/v1/lms/release-rules/{releaseRuleId}";
    /**
     * DELETE /v1/lms/release-rules/{releaseRuleId} : Delete ReleaseRule
     * Soft delete (sets deleted_at).
     *
     * @param releaseRuleId ReleaseRule id. (required)
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
        value = ReleaseRuleApi.PATH_DELETE_RELEASE_RULE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteReleaseRule(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RELEASE_RULE_CONDITION = "/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}";
    /**
     * DELETE /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId} : Delete ReleaseRuleCondition
     * Soft delete (sets deleted_at).
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleConditionId ReleaseRuleCondition id. (required)
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
        value = ReleaseRuleApi.PATH_DELETE_RELEASE_RULE_CONDITION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteReleaseRuleCondition(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @PathVariable("releaseRuleConditionId") UUID releaseRuleConditionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RELEASE_RULE_TARGET = "/v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}";
    /**
     * DELETE /v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId} : Delete ReleaseRuleTarget
     * Soft delete (sets deleted_at).
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleTargetId ReleaseRuleTarget id. (required)
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
        value = ReleaseRuleApi.PATH_DELETE_RELEASE_RULE_TARGET,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteReleaseRuleTarget(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @PathVariable("releaseRuleTargetId") UUID releaseRuleTargetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DISABLE_RELEASE_RULE = "/v1/lms/release-rules/{releaseRuleId}:disable";
    /**
     * POST /v1/lms/release-rules/{releaseRuleId}:disable : Disable
     * Disable.
     *
     * @param releaseRuleId  (required)
     * @param disableReleaseRuleRequest  (required)
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
        value = ReleaseRuleApi.PATH_DISABLE_RELEASE_RULE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRule> disableReleaseRule(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @Valid @RequestBody DisableReleaseRuleRequest disableReleaseRuleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_ENABLE_RELEASE_RULE = "/v1/lms/release-rules/{releaseRuleId}:enable";
    /**
     * POST /v1/lms/release-rules/{releaseRuleId}:enable : Enable
     * Enable.
     *
     * @param releaseRuleId  (required)
     * @param enableReleaseRuleRequest  (required)
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
        value = ReleaseRuleApi.PATH_ENABLE_RELEASE_RULE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRule> enableReleaseRule(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @Valid @RequestBody EnableReleaseRuleRequest enableReleaseRuleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_RELEASE_RULE = "/v1/lms/release-rules/{releaseRuleId}";
    /**
     * GET /v1/lms/release-rules/{releaseRuleId} : Get ReleaseRule
     * Get one ReleaseRule by id. The ETag header carries the row version.
     *
     * @param releaseRuleId ReleaseRule id. (required)
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
        value = ReleaseRuleApi.PATH_GET_RELEASE_RULE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ReleaseRule> getReleaseRule(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RELEASE_RULE_CONDITION = "/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}";
    /**
     * GET /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId} : Get ReleaseRuleCondition
     * Get one ReleaseRuleCondition by id. The ETag header carries the row version.
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleConditionId ReleaseRuleCondition id. (required)
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
        value = ReleaseRuleApi.PATH_GET_RELEASE_RULE_CONDITION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ReleaseRuleCondition> getReleaseRuleCondition(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @PathVariable("releaseRuleConditionId") UUID releaseRuleConditionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RELEASE_RULE_TARGET = "/v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}";
    /**
     * GET /v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId} : Get ReleaseRuleTarget
     * Get one ReleaseRuleTarget by id. The ETag header carries the row version.
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleTargetId ReleaseRuleTarget id. (required)
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
        value = ReleaseRuleApi.PATH_GET_RELEASE_RULE_TARGET,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ReleaseRuleTarget> getReleaseRuleTarget(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @PathVariable("releaseRuleTargetId") UUID releaseRuleTargetId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RELEASE_RULE_CONDITIONS = "/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions";
    /**
     * GET /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions : List ReleaseRuleConditions
     * List ReleaseRuleConditions visible to the caller, page by page, with filtering and sorting.
     *
     * @param releaseRuleId ReleaseRule id. (required)
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
        value = ReleaseRuleApi.PATH_LIST_RELEASE_RULE_CONDITIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ReleaseRuleCondition>> listReleaseRuleConditions(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RELEASE_RULE_TARGETS = "/v1/lms/release-rules/{releaseRuleId}/release-rule-targets";
    /**
     * GET /v1/lms/release-rules/{releaseRuleId}/release-rule-targets : List ReleaseRuleTargets
     * List ReleaseRuleTargets visible to the caller, page by page, with filtering and sorting.
     *
     * @param releaseRuleId ReleaseRule id. (required)
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
        value = ReleaseRuleApi.PATH_LIST_RELEASE_RULE_TARGETS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ReleaseRuleTarget>> listReleaseRuleTargets(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RELEASE_RULES = "/v1/lms/release-rules";
    /**
     * GET /v1/lms/release-rules : List ReleaseRules
     * List ReleaseRules visible to the caller, page by page, with filtering and sorting.
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
        value = ReleaseRuleApi.PATH_LIST_RELEASE_RULES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ReleaseRule>> listReleaseRules(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_RELEASE_RULE = "/v1/lms/release-rules/{releaseRuleId}";
    /**
     * PATCH /v1/lms/release-rules/{releaseRuleId} : Update ReleaseRule
     * Partially update a ReleaseRule (updateMask). Requires If-Match with the current ETag.
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param releaseRuleUpdate  (required)
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
        value = ReleaseRuleApi.PATH_UPDATE_RELEASE_RULE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRule> updateReleaseRule(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ReleaseRuleUpdate releaseRuleUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RELEASE_RULE_CONDITION = "/v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId}";
    /**
     * PATCH /v1/lms/release-rules/{releaseRuleId}/release-rule-conditions/{releaseRuleConditionId} : Update ReleaseRuleCondition
     * Partially update a ReleaseRuleCondition (updateMask). Requires If-Match with the current ETag.
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleConditionId ReleaseRuleCondition id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param releaseRuleConditionUpdate  (required)
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
        value = ReleaseRuleApi.PATH_UPDATE_RELEASE_RULE_CONDITION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRuleCondition> updateReleaseRuleCondition(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @PathVariable("releaseRuleConditionId") UUID releaseRuleConditionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ReleaseRuleConditionUpdate releaseRuleConditionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RELEASE_RULE_TARGET = "/v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId}";
    /**
     * PATCH /v1/lms/release-rules/{releaseRuleId}/release-rule-targets/{releaseRuleTargetId} : Update ReleaseRuleTarget
     * Partially update a ReleaseRuleTarget (updateMask). Requires If-Match with the current ETag.
     *
     * @param releaseRuleId ReleaseRule id. (required)
     * @param releaseRuleTargetId ReleaseRuleTarget id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param releaseRuleTargetUpdate  (required)
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
        value = ReleaseRuleApi.PATH_UPDATE_RELEASE_RULE_TARGET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRuleTarget> updateReleaseRuleTarget(
         @PathVariable("releaseRuleId") UUID releaseRuleId,
         @PathVariable("releaseRuleTargetId") UUID releaseRuleTargetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ReleaseRuleTargetUpdate releaseRuleTargetUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
