// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.model.ActivateExternalToolRequest;
import com.mysms.learning.lms.api.model.ExternalTool;
import com.mysms.learning.lms.api.model.ExternalToolInstitution;
import com.mysms.learning.lms.api.model.ExternalToolInstitutionUpdate;
import com.mysms.learning.lms.api.model.ExternalToolOption;
import com.mysms.learning.lms.api.model.ExternalToolOptionUpdate;
import com.mysms.learning.lms.api.model.ExternalToolRole;
import com.mysms.learning.lms.api.model.ExternalToolRoleUpdate;
import com.mysms.learning.lms.api.model.ExternalToolSetting;
import com.mysms.learning.lms.api.model.ExternalToolSettingUpdate;
import com.mysms.learning.lms.api.model.ExternalToolUpdate;
import org.springframework.lang.Nullable;
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
public interface ExternalToolApi {

    String PATH_ACTIVATE_EXTERNAL_TOOL = "/v1/lms/external-tools/{externalToolId}:activate";
    /**
     * POST /v1/lms/external-tools/{externalToolId}:activate : Validate a licence key and enable the tool for the tenant
     * Validate a licence key and enable the tool for the tenant.
     *
     * @param externalToolId  (required)
     * @param activateExternalToolRequest  (required)
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
        value = ExternalToolApi.PATH_ACTIVATE_EXTERNAL_TOOL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalTool> activateExternalTool(
         @PathVariable("externalToolId") UUID externalToolId,
         @Valid @RequestBody ActivateExternalToolRequest activateExternalToolRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_EXTERNAL_TOOL = "/v1/lms/external-tools";
    /**
     * POST /v1/lms/external-tools : Create ExternalTool
     * Create a ExternalTool. Supports Idempotency-Key and validateOnly.
     *
     * @param externalTool  (required)
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
        value = ExternalToolApi.PATH_CREATE_EXTERNAL_TOOL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalTool> createExternalTool(
         @Valid @RequestBody ExternalTool externalTool,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EXTERNAL_TOOL_INSTITUTION = "/v1/lms/external-tools/{externalToolId}/institutions";
    /**
     * POST /v1/lms/external-tools/{externalToolId}/institutions : Create ExternalToolInstitution
     * Create a ExternalToolInstitution. Supports Idempotency-Key and validateOnly.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolInstitution  (required)
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
        value = ExternalToolApi.PATH_CREATE_EXTERNAL_TOOL_INSTITUTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolInstitution> createExternalToolInstitution(
         @PathVariable("externalToolId") UUID externalToolId,
         @Valid @RequestBody ExternalToolInstitution externalToolInstitution,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EXTERNAL_TOOL_OPTION = "/v1/lms/external-tools/{externalToolId}/options";
    /**
     * POST /v1/lms/external-tools/{externalToolId}/options : Create ExternalToolOption
     * Create a ExternalToolOption. Supports Idempotency-Key and validateOnly.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolOption  (required)
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
        value = ExternalToolApi.PATH_CREATE_EXTERNAL_TOOL_OPTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolOption> createExternalToolOption(
         @PathVariable("externalToolId") UUID externalToolId,
         @Valid @RequestBody ExternalToolOption externalToolOption,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EXTERNAL_TOOL_ROLE = "/v1/lms/external-tools/{externalToolId}/roles";
    /**
     * POST /v1/lms/external-tools/{externalToolId}/roles : Create ExternalToolRole
     * Create a ExternalToolRole. Supports Idempotency-Key and validateOnly.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolRole  (required)
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
        value = ExternalToolApi.PATH_CREATE_EXTERNAL_TOOL_ROLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolRole> createExternalToolRole(
         @PathVariable("externalToolId") UUID externalToolId,
         @Valid @RequestBody ExternalToolRole externalToolRole,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EXTERNAL_TOOL_SETTING = "/v1/lms/external-tools/{externalToolId}/settings";
    /**
     * POST /v1/lms/external-tools/{externalToolId}/settings : Create ExternalToolSetting
     * Create a ExternalToolSetting. Supports Idempotency-Key and validateOnly.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolSetting  (required)
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
        value = ExternalToolApi.PATH_CREATE_EXTERNAL_TOOL_SETTING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolSetting> createExternalToolSetting(
         @PathVariable("externalToolId") UUID externalToolId,
         @Valid @RequestBody ExternalToolSetting externalToolSetting,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_EXTERNAL_TOOL = "/v1/lms/external-tools/{externalToolId}";
    /**
     * DELETE /v1/lms/external-tools/{externalToolId} : Delete ExternalTool
     * Soft delete (sets deleted_at).
     *
     * @param externalToolId ExternalTool id. (required)
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
        value = ExternalToolApi.PATH_DELETE_EXTERNAL_TOOL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExternalTool(
         @PathVariable("externalToolId") UUID externalToolId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EXTERNAL_TOOL_INSTITUTION = "/v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}";
    /**
     * DELETE /v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId} : Delete ExternalToolInstitution
     * Soft delete (sets deleted_at).
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolInstitutionId ExternalToolInstitution id. (required)
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
        value = ExternalToolApi.PATH_DELETE_EXTERNAL_TOOL_INSTITUTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExternalToolInstitution(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolInstitutionId") UUID externalToolInstitutionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EXTERNAL_TOOL_OPTION = "/v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}";
    /**
     * DELETE /v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId} : Delete ExternalToolOption
     * Soft delete (sets deleted_at).
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolOptionId ExternalToolOption id. (required)
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
        value = ExternalToolApi.PATH_DELETE_EXTERNAL_TOOL_OPTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExternalToolOption(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolOptionId") UUID externalToolOptionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EXTERNAL_TOOL_ROLE = "/v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}";
    /**
     * DELETE /v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId} : Delete ExternalToolRole
     * Soft delete (sets deleted_at).
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolRoleId ExternalToolRole id. (required)
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
        value = ExternalToolApi.PATH_DELETE_EXTERNAL_TOOL_ROLE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExternalToolRole(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolRoleId") UUID externalToolRoleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EXTERNAL_TOOL_SETTING = "/v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}";
    /**
     * DELETE /v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId} : Delete ExternalToolSetting
     * Soft delete (sets deleted_at).
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolSettingId ExternalToolSetting id. (required)
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
        value = ExternalToolApi.PATH_DELETE_EXTERNAL_TOOL_SETTING,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExternalToolSetting(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolSettingId") UUID externalToolSettingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_EXTERNAL_TOOL = "/v1/lms/external-tools/{externalToolId}";
    /**
     * GET /v1/lms/external-tools/{externalToolId} : Get ExternalTool
     * Get one ExternalTool by id. The ETag header carries the row version.
     *
     * @param externalToolId ExternalTool id. (required)
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
        value = ExternalToolApi.PATH_GET_EXTERNAL_TOOL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalTool> getExternalTool(
         @PathVariable("externalToolId") UUID externalToolId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EXTERNAL_TOOL_INSTITUTION = "/v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId} : Get ExternalToolInstitution
     * Get one ExternalToolInstitution by id. The ETag header carries the row version.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolInstitutionId ExternalToolInstitution id. (required)
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
        value = ExternalToolApi.PATH_GET_EXTERNAL_TOOL_INSTITUTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalToolInstitution> getExternalToolInstitution(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolInstitutionId") UUID externalToolInstitutionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EXTERNAL_TOOL_OPTION = "/v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId} : Get ExternalToolOption
     * Get one ExternalToolOption by id. The ETag header carries the row version.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolOptionId ExternalToolOption id. (required)
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
        value = ExternalToolApi.PATH_GET_EXTERNAL_TOOL_OPTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalToolOption> getExternalToolOption(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolOptionId") UUID externalToolOptionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EXTERNAL_TOOL_ROLE = "/v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId} : Get ExternalToolRole
     * Get one ExternalToolRole by id. The ETag header carries the row version.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolRoleId ExternalToolRole id. (required)
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
        value = ExternalToolApi.PATH_GET_EXTERNAL_TOOL_ROLE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalToolRole> getExternalToolRole(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolRoleId") UUID externalToolRoleId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EXTERNAL_TOOL_SETTING = "/v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId} : Get ExternalToolSetting
     * Get one ExternalToolSetting by id. The ETag header carries the row version.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolSettingId ExternalToolSetting id. (required)
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
        value = ExternalToolApi.PATH_GET_EXTERNAL_TOOL_SETTING,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalToolSetting> getExternalToolSetting(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolSettingId") UUID externalToolSettingId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXTERNAL_TOOL_INSTITUTIONS = "/v1/lms/external-tools/{externalToolId}/institutions";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/institutions : List ExternalToolInstitutions
     * List ExternalToolInstitutions visible to the caller, page by page, with filtering and sorting.
     *
     * @param externalToolId ExternalTool id. (required)
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
        value = ExternalToolApi.PATH_LIST_EXTERNAL_TOOL_INSTITUTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExternalToolInstitution>> listExternalToolInstitutions(
         @PathVariable("externalToolId") UUID externalToolId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXTERNAL_TOOL_OPTIONS = "/v1/lms/external-tools/{externalToolId}/options";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/options : List ExternalToolOptions
     * List ExternalToolOptions visible to the caller, page by page, with filtering and sorting.
     *
     * @param externalToolId ExternalTool id. (required)
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
        value = ExternalToolApi.PATH_LIST_EXTERNAL_TOOL_OPTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExternalToolOption>> listExternalToolOptions(
         @PathVariable("externalToolId") UUID externalToolId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXTERNAL_TOOL_ROLES = "/v1/lms/external-tools/{externalToolId}/roles";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/roles : List ExternalToolRoles
     * List ExternalToolRoles visible to the caller, page by page, with filtering and sorting.
     *
     * @param externalToolId ExternalTool id. (required)
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
        value = ExternalToolApi.PATH_LIST_EXTERNAL_TOOL_ROLES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExternalToolRole>> listExternalToolRoles(
         @PathVariable("externalToolId") UUID externalToolId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXTERNAL_TOOL_SETTINGS = "/v1/lms/external-tools/{externalToolId}/settings";
    /**
     * GET /v1/lms/external-tools/{externalToolId}/settings : List ExternalToolSettings
     * List ExternalToolSettings visible to the caller, page by page, with filtering and sorting.
     *
     * @param externalToolId ExternalTool id. (required)
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
        value = ExternalToolApi.PATH_LIST_EXTERNAL_TOOL_SETTINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExternalToolSetting>> listExternalToolSettings(
         @PathVariable("externalToolId") UUID externalToolId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXTERNAL_TOOLS = "/v1/lms/external-tools";
    /**
     * GET /v1/lms/external-tools : List ExternalTools
     * List ExternalTools visible to the caller, page by page, with filtering and sorting.
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
        value = ExternalToolApi.PATH_LIST_EXTERNAL_TOOLS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExternalTool>> listExternalTools(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_EXTERNAL_TOOL = "/v1/lms/external-tools/{externalToolId}";
    /**
     * PATCH /v1/lms/external-tools/{externalToolId} : Update ExternalTool
     * Partially update a ExternalTool (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param externalToolUpdate  (required)
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
        value = ExternalToolApi.PATH_UPDATE_EXTERNAL_TOOL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalTool> updateExternalTool(
         @PathVariable("externalToolId") UUID externalToolId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExternalToolUpdate externalToolUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EXTERNAL_TOOL_INSTITUTION = "/v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId}";
    /**
     * PATCH /v1/lms/external-tools/{externalToolId}/institutions/{externalToolInstitutionId} : Update ExternalToolInstitution
     * Partially update a ExternalToolInstitution (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolInstitutionId ExternalToolInstitution id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param externalToolInstitutionUpdate  (required)
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
        value = ExternalToolApi.PATH_UPDATE_EXTERNAL_TOOL_INSTITUTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolInstitution> updateExternalToolInstitution(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolInstitutionId") UUID externalToolInstitutionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExternalToolInstitutionUpdate externalToolInstitutionUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EXTERNAL_TOOL_OPTION = "/v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId}";
    /**
     * PATCH /v1/lms/external-tools/{externalToolId}/options/{externalToolOptionId} : Update ExternalToolOption
     * Partially update a ExternalToolOption (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolOptionId ExternalToolOption id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param externalToolOptionUpdate  (required)
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
        value = ExternalToolApi.PATH_UPDATE_EXTERNAL_TOOL_OPTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolOption> updateExternalToolOption(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolOptionId") UUID externalToolOptionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExternalToolOptionUpdate externalToolOptionUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EXTERNAL_TOOL_ROLE = "/v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId}";
    /**
     * PATCH /v1/lms/external-tools/{externalToolId}/roles/{externalToolRoleId} : Update ExternalToolRole
     * Partially update a ExternalToolRole (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolRoleId ExternalToolRole id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param externalToolRoleUpdate  (required)
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
        value = ExternalToolApi.PATH_UPDATE_EXTERNAL_TOOL_ROLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolRole> updateExternalToolRole(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolRoleId") UUID externalToolRoleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExternalToolRoleUpdate externalToolRoleUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EXTERNAL_TOOL_SETTING = "/v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId}";
    /**
     * PATCH /v1/lms/external-tools/{externalToolId}/settings/{externalToolSettingId} : Update ExternalToolSetting
     * Partially update a ExternalToolSetting (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param externalToolId ExternalTool id. (required)
     * @param externalToolSettingId ExternalToolSetting id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param externalToolSettingUpdate  (required)
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
        value = ExternalToolApi.PATH_UPDATE_EXTERNAL_TOOL_SETTING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolSetting> updateExternalToolSetting(
         @PathVariable("externalToolId") UUID externalToolId,
         @PathVariable("externalToolSettingId") UUID externalToolSettingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExternalToolSettingUpdate externalToolSettingUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
