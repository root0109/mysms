// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ExternalToolLink;
import com.mysms.lms.api.model.ExternalToolLinkUpdate;
import com.mysms.lms.api.model.ExternalToolResource;
import com.mysms.lms.api.model.ExternalToolResourceUpdate;
import com.mysms.lms.api.model.LaunchExternalToolRequest;
import com.mysms.lms.api.model.LtiLaunch;
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
public interface ExternalToolResourceApi {

    String PATH_CREATE_EXTERNAL_TOOL_LINK = "/v1/lms/external-tool-resources/{externalToolResourceId}/links";
    /**
     * POST /v1/lms/external-tool-resources/{externalToolResourceId}/links : Create ExternalToolLink
     * Create a ExternalToolLink. Supports Idempotency-Key and validateOnly.
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
     * @param externalToolLink  (required)
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
        value = ExternalToolResourceApi.PATH_CREATE_EXTERNAL_TOOL_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolLink> createExternalToolLink(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
         @Valid @RequestBody ExternalToolLink externalToolLink,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EXTERNAL_TOOL_RESOURCE = "/v1/lms/external-tool-resources";
    /**
     * POST /v1/lms/external-tool-resources : Create ExternalToolResource
     * Create a ExternalToolResource. Supports Idempotency-Key and validateOnly.
     *
     * @param externalToolResource  (required)
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
        value = ExternalToolResourceApi.PATH_CREATE_EXTERNAL_TOOL_RESOURCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolResource> createExternalToolResource(
         @Valid @RequestBody ExternalToolResource externalToolResource,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_EXTERNAL_TOOL_LINK = "/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}";
    /**
     * DELETE /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId} : Delete ExternalToolLink
     * Soft delete (sets deleted_at).
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
     * @param externalToolLinkId ExternalToolLink id. (required)
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
        value = ExternalToolResourceApi.PATH_DELETE_EXTERNAL_TOOL_LINK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExternalToolLink(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
         @PathVariable("externalToolLinkId") UUID externalToolLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EXTERNAL_TOOL_RESOURCE = "/v1/lms/external-tool-resources/{externalToolResourceId}";
    /**
     * DELETE /v1/lms/external-tool-resources/{externalToolResourceId} : Delete ExternalToolResource
     * Soft delete (sets deleted_at).
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
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
        value = ExternalToolResourceApi.PATH_DELETE_EXTERNAL_TOOL_RESOURCE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExternalToolResource(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_EXTERNAL_TOOL_LINK = "/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}";
    /**
     * GET /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId} : Get ExternalToolLink
     * Get one ExternalToolLink by id. The ETag header carries the row version.
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
     * @param externalToolLinkId ExternalToolLink id. (required)
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
        value = ExternalToolResourceApi.PATH_GET_EXTERNAL_TOOL_LINK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalToolLink> getExternalToolLink(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
         @PathVariable("externalToolLinkId") UUID externalToolLinkId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EXTERNAL_TOOL_RESOURCE = "/v1/lms/external-tool-resources/{externalToolResourceId}";
    /**
     * GET /v1/lms/external-tool-resources/{externalToolResourceId} : Get ExternalToolResource
     * Get one ExternalToolResource by id. The ETag header carries the row version.
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
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
        value = ExternalToolResourceApi.PATH_GET_EXTERNAL_TOOL_RESOURCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalToolResource> getExternalToolResource(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LAUNCH_EXTERNAL_TOOL = "/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}:launch";
    /**
     * POST /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}:launch : Signed LTI launch for the caller
     * Signed LTI launch for the caller.
     *
     * @param externalToolResourceId  (required)
     * @param externalToolLinkId  (required)
     * @param launchExternalToolRequest  (required)
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
        value = ExternalToolResourceApi.PATH_LAUNCH_EXTERNAL_TOOL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LtiLaunch> launchExternalTool(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
         @PathVariable("externalToolLinkId") UUID externalToolLinkId,
         @Valid @RequestBody LaunchExternalToolRequest launchExternalToolRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_EXTERNAL_TOOL_LINKS = "/v1/lms/external-tool-resources/{externalToolResourceId}/links";
    /**
     * GET /v1/lms/external-tool-resources/{externalToolResourceId}/links : List ExternalToolLinks
     * List ExternalToolLinks visible to the caller, page by page, with filtering and sorting.
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
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
        value = ExternalToolResourceApi.PATH_LIST_EXTERNAL_TOOL_LINKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExternalToolLink>> listExternalToolLinks(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXTERNAL_TOOL_RESOURCES = "/v1/lms/external-tool-resources";
    /**
     * GET /v1/lms/external-tool-resources : List ExternalToolResources
     * List ExternalToolResources visible to the caller, page by page, with filtering and sorting.
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
        value = ExternalToolResourceApi.PATH_LIST_EXTERNAL_TOOL_RESOURCES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExternalToolResource>> listExternalToolResources(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RECEIVE_TOOL_OUTCOME = "/v1/lms/integrations/lti:outcome";
    /**
     * POST /v1/lms/integrations/lti:outcome : LTI grade pass-back from a tool into the gradebook: LTI 1.1 Basic Outcomes (XML, OAuth 1.0a body signature) or LTI Advantage score (JSON, JWT bearer from the tool)
     * LTI grade pass-back from a tool into the gradebook: LTI 1.1 Basic Outcomes (XML, OAuth 1.0a body signature) or LTI Advantage score (JSON, JWT bearer from the tool). The tool and tenant are resolved from the result sourcedid / line item; scores for unknown or closed items are rejected.
     *
     * @param authorization Provider signature / authentication header, verified against the raw body. (required)
     * @param body Raw provider payload, passed through unchanged for signature verification. (required)
     * @return Done (status code 204)
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
        value = ExternalToolResourceApi.PATH_RECEIVE_TOOL_OUTCOME,
        produces = { "application/problem+json" },
        consumes = { "application/xml", "application/vnd.ims.lis.v1.score+json" }
    )
    ResponseEntity<Void> receiveToolOutcome(
        @NotNull  @RequestHeader(value = "Authorization", required = true) String authorization,
         @Valid @RequestBody String body
    );


    String PATH_UPDATE_EXTERNAL_TOOL_LINK = "/v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId}";
    /**
     * PATCH /v1/lms/external-tool-resources/{externalToolResourceId}/links/{externalToolLinkId} : Update ExternalToolLink
     * Partially update a ExternalToolLink (updateMask). Requires If-Match with the current ETag.
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
     * @param externalToolLinkId ExternalToolLink id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param externalToolLinkUpdate  (required)
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
        value = ExternalToolResourceApi.PATH_UPDATE_EXTERNAL_TOOL_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolLink> updateExternalToolLink(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
         @PathVariable("externalToolLinkId") UUID externalToolLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExternalToolLinkUpdate externalToolLinkUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EXTERNAL_TOOL_RESOURCE = "/v1/lms/external-tool-resources/{externalToolResourceId}";
    /**
     * PATCH /v1/lms/external-tool-resources/{externalToolResourceId} : Update ExternalToolResource
     * Partially update a ExternalToolResource (updateMask). Requires If-Match with the current ETag.
     *
     * @param externalToolResourceId ExternalToolResource id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param externalToolResourceUpdate  (required)
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
        value = ExternalToolResourceApi.PATH_UPDATE_EXTERNAL_TOOL_RESOURCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalToolResource> updateExternalToolResource(
         @PathVariable("externalToolResourceId") UUID externalToolResourceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExternalToolResourceUpdate externalToolResourceUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
