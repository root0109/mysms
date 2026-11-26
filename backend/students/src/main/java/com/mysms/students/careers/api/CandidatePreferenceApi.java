// Owned API contract of careers.
package com.mysms.students.careers.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.students.careers.api.model.CandidatePreference;
import com.mysms.students.careers.api.model.CandidatePreferenceUpdate;
import com.mysms.students.careers.api.model.CandidatePreferredFunction;
import com.mysms.students.careers.api.model.CandidatePreferredFunctionUpdate;
import com.mysms.students.careers.api.model.CandidatePreferredIndustry;
import com.mysms.students.careers.api.model.CandidatePreferredIndustryUpdate;
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
public interface CandidatePreferenceApi {

    String PATH_CREATE_CANDIDATE_PREFERENCE = "/v1/careers/candidate-preferences";
    /**
     * POST /v1/careers/candidate-preferences : Create CandidatePreference
     * Create a CandidatePreference. Supports Idempotency-Key and validateOnly.
     *
     * @param candidatePreference  (required)
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
        value = CandidatePreferenceApi.PATH_CREATE_CANDIDATE_PREFERENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CandidatePreference> createCandidatePreference(
         @Valid @RequestBody CandidatePreference candidatePreference,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CANDIDATE_PREFERRED_FUNCTION = "/v1/careers/candidate-preferences/{candidatePreferenceId}/functions";
    /**
     * POST /v1/careers/candidate-preferences/{candidatePreferenceId}/functions : Create CandidatePreferredFunction
     * Create a CandidatePreferredFunction. Supports Idempotency-Key and validateOnly.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredFunction  (required)
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
        value = CandidatePreferenceApi.PATH_CREATE_CANDIDATE_PREFERRED_FUNCTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CandidatePreferredFunction> createCandidatePreferredFunction(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @Valid @RequestBody CandidatePreferredFunction candidatePreferredFunction,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CANDIDATE_PREFERRED_INDUSTRY = "/v1/careers/candidate-preferences/{candidatePreferenceId}/industries";
    /**
     * POST /v1/careers/candidate-preferences/{candidatePreferenceId}/industries : Create CandidatePreferredIndustry
     * Create a CandidatePreferredIndustry. Supports Idempotency-Key and validateOnly.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredIndustry  (required)
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
        value = CandidatePreferenceApi.PATH_CREATE_CANDIDATE_PREFERRED_INDUSTRY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CandidatePreferredIndustry> createCandidatePreferredIndustry(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @Valid @RequestBody CandidatePreferredIndustry candidatePreferredIndustry,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_CANDIDATE_PREFERENCE = "/v1/careers/candidate-preferences/{candidatePreferenceId}";
    /**
     * DELETE /v1/careers/candidate-preferences/{candidatePreferenceId} : Delete CandidatePreference
     * Soft delete (sets deleted_at).
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
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
        value = CandidatePreferenceApi.PATH_DELETE_CANDIDATE_PREFERENCE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCandidatePreference(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CANDIDATE_PREFERRED_FUNCTION = "/v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}";
    /**
     * DELETE /v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId} : Delete CandidatePreferredFunction
     * Soft delete (sets deleted_at).
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredFunctionId CandidatePreferredFunction id. (required)
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
        value = CandidatePreferenceApi.PATH_DELETE_CANDIDATE_PREFERRED_FUNCTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCandidatePreferredFunction(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @PathVariable("candidatePreferredFunctionId") UUID candidatePreferredFunctionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CANDIDATE_PREFERRED_INDUSTRY = "/v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}";
    /**
     * DELETE /v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId} : Delete CandidatePreferredIndustry
     * Soft delete (sets deleted_at).
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredIndustryId CandidatePreferredIndustry id. (required)
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
        value = CandidatePreferenceApi.PATH_DELETE_CANDIDATE_PREFERRED_INDUSTRY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCandidatePreferredIndustry(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @PathVariable("candidatePreferredIndustryId") UUID candidatePreferredIndustryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_CANDIDATE_PREFERENCE = "/v1/careers/candidate-preferences/{candidatePreferenceId}";
    /**
     * GET /v1/careers/candidate-preferences/{candidatePreferenceId} : Get CandidatePreference
     * Get one CandidatePreference by id. The ETag header carries the row version.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
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
        value = CandidatePreferenceApi.PATH_GET_CANDIDATE_PREFERENCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CandidatePreference> getCandidatePreference(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CANDIDATE_PREFERRED_FUNCTION = "/v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}";
    /**
     * GET /v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId} : Get CandidatePreferredFunction
     * Get one CandidatePreferredFunction by id. The ETag header carries the row version.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredFunctionId CandidatePreferredFunction id. (required)
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
        value = CandidatePreferenceApi.PATH_GET_CANDIDATE_PREFERRED_FUNCTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CandidatePreferredFunction> getCandidatePreferredFunction(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @PathVariable("candidatePreferredFunctionId") UUID candidatePreferredFunctionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CANDIDATE_PREFERRED_INDUSTRY = "/v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}";
    /**
     * GET /v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId} : Get CandidatePreferredIndustry
     * Get one CandidatePreferredIndustry by id. The ETag header carries the row version.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredIndustryId CandidatePreferredIndustry id. (required)
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
        value = CandidatePreferenceApi.PATH_GET_CANDIDATE_PREFERRED_INDUSTRY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CandidatePreferredIndustry> getCandidatePreferredIndustry(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @PathVariable("candidatePreferredIndustryId") UUID candidatePreferredIndustryId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CANDIDATE_PREFERENCES = "/v1/careers/candidate-preferences";
    /**
     * GET /v1/careers/candidate-preferences : List CandidatePreferences
     * List CandidatePreferences visible to the caller, page by page, with filtering and sorting.
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
        value = CandidatePreferenceApi.PATH_LIST_CANDIDATE_PREFERENCES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CandidatePreference>> listCandidatePreferences(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CANDIDATE_PREFERRED_FUNCTIONS = "/v1/careers/candidate-preferences/{candidatePreferenceId}/functions";
    /**
     * GET /v1/careers/candidate-preferences/{candidatePreferenceId}/functions : List CandidatePreferredFunctions
     * List CandidatePreferredFunctions visible to the caller, page by page, with filtering and sorting.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
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
        value = CandidatePreferenceApi.PATH_LIST_CANDIDATE_PREFERRED_FUNCTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CandidatePreferredFunction>> listCandidatePreferredFunctions(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CANDIDATE_PREFERRED_INDUSTRIES = "/v1/careers/candidate-preferences/{candidatePreferenceId}/industries";
    /**
     * GET /v1/careers/candidate-preferences/{candidatePreferenceId}/industries : List CandidatePreferredIndustries
     * List CandidatePreferredIndustries visible to the caller, page by page, with filtering and sorting.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
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
        value = CandidatePreferenceApi.PATH_LIST_CANDIDATE_PREFERRED_INDUSTRIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CandidatePreferredIndustry>> listCandidatePreferredIndustries(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_CANDIDATE_PREFERENCE = "/v1/careers/candidate-preferences/{candidatePreferenceId}";
    /**
     * PATCH /v1/careers/candidate-preferences/{candidatePreferenceId} : Update CandidatePreference
     * Partially update a CandidatePreference (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param candidatePreferenceUpdate  (required)
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
        value = CandidatePreferenceApi.PATH_UPDATE_CANDIDATE_PREFERENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CandidatePreference> updateCandidatePreference(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CandidatePreferenceUpdate candidatePreferenceUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CANDIDATE_PREFERRED_FUNCTION = "/v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId}";
    /**
     * PATCH /v1/careers/candidate-preferences/{candidatePreferenceId}/functions/{candidatePreferredFunctionId} : Update CandidatePreferredFunction
     * Partially update a CandidatePreferredFunction (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredFunctionId CandidatePreferredFunction id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param candidatePreferredFunctionUpdate  (required)
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
        value = CandidatePreferenceApi.PATH_UPDATE_CANDIDATE_PREFERRED_FUNCTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CandidatePreferredFunction> updateCandidatePreferredFunction(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @PathVariable("candidatePreferredFunctionId") UUID candidatePreferredFunctionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CandidatePreferredFunctionUpdate candidatePreferredFunctionUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CANDIDATE_PREFERRED_INDUSTRY = "/v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId}";
    /**
     * PATCH /v1/careers/candidate-preferences/{candidatePreferenceId}/industries/{candidatePreferredIndustryId} : Update CandidatePreferredIndustry
     * Partially update a CandidatePreferredIndustry (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param candidatePreferenceId CandidatePreference id. (required)
     * @param candidatePreferredIndustryId CandidatePreferredIndustry id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param candidatePreferredIndustryUpdate  (required)
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
        value = CandidatePreferenceApi.PATH_UPDATE_CANDIDATE_PREFERRED_INDUSTRY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CandidatePreferredIndustry> updateCandidatePreferredIndustry(
         @PathVariable("candidatePreferenceId") UUID candidatePreferenceId,
         @PathVariable("candidatePreferredIndustryId") UUID candidatePreferredIndustryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CandidatePreferredIndustryUpdate candidatePreferredIndustryUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
