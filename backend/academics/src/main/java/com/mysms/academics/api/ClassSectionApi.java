// Owned API contract of academics (JAVA-CODING-RULES §7); first materialized from api/openapi/academics.yaml on 2026-11-10.
package com.mysms.academics.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.api.model.ClassSection;
import com.mysms.academics.api.model.ClassSectionUpdate;
import com.mysms.academics.api.model.ClassSubject;
import com.mysms.academics.api.model.ClassSubjectUpdate;
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
public interface ClassSectionApi {

    String PATH_CREATE_CLASS_SECTION = "/v1/academics/class-sections";
    /**
     * POST /v1/academics/class-sections : Create ClassSection
     * Create a ClassSection. Supports Idempotency-Key and validateOnly.
     *
     * @param classSection  (required)
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
        value = ClassSectionApi.PATH_CREATE_CLASS_SECTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassSection> createClassSection(
         @Valid @RequestBody ClassSection classSection,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CLASS_SUBJECT = "/v1/academics/class-sections/{classSectionId}/subjects";
    /**
     * POST /v1/academics/class-sections/{classSectionId}/subjects : Create ClassSubject
     * Create a ClassSubject. Supports Idempotency-Key and validateOnly.
     *
     * @param classSectionId ClassSection id. (required)
     * @param classSubject  (required)
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
        value = ClassSectionApi.PATH_CREATE_CLASS_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassSubject> createClassSubject(
         @PathVariable("classSectionId") UUID classSectionId,
         @Valid @RequestBody ClassSubject classSubject,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_CLASS_SECTION = "/v1/academics/class-sections/{classSectionId}";
    /**
     * DELETE /v1/academics/class-sections/{classSectionId} : Delete ClassSection
     * Soft delete (sets deleted_at).
     *
     * @param classSectionId ClassSection id. (required)
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
        value = ClassSectionApi.PATH_DELETE_CLASS_SECTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteClassSection(
         @PathVariable("classSectionId") UUID classSectionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CLASS_SUBJECT = "/v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}";
    /**
     * DELETE /v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId} : Delete ClassSubject
     * Soft delete (sets deleted_at).
     *
     * @param classSectionId ClassSection id. (required)
     * @param classSubjectId ClassSubject id. (required)
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
        value = ClassSectionApi.PATH_DELETE_CLASS_SUBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteClassSubject(
         @PathVariable("classSectionId") UUID classSectionId,
         @PathVariable("classSubjectId") UUID classSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_CLASS_SECTION = "/v1/academics/class-sections/{classSectionId}";
    /**
     * GET /v1/academics/class-sections/{classSectionId} : Get ClassSection
     * Get one ClassSection by id. The ETag header carries the row version.
     *
     * @param classSectionId ClassSection id. (required)
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
        value = ClassSectionApi.PATH_GET_CLASS_SECTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ClassSection> getClassSection(
         @PathVariable("classSectionId") UUID classSectionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CLASS_SUBJECT = "/v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}";
    /**
     * GET /v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId} : Get ClassSubject
     * Get one ClassSubject by id. The ETag header carries the row version.
     *
     * @param classSectionId ClassSection id. (required)
     * @param classSubjectId ClassSubject id. (required)
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
        value = ClassSectionApi.PATH_GET_CLASS_SUBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ClassSubject> getClassSubject(
         @PathVariable("classSectionId") UUID classSectionId,
         @PathVariable("classSubjectId") UUID classSubjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CLASS_SECTIONS = "/v1/academics/class-sections";
    /**
     * GET /v1/academics/class-sections : List ClassSections
     * List ClassSections visible to the caller, page by page, with filtering and sorting.
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
        value = ClassSectionApi.PATH_LIST_CLASS_SECTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ClassSection>> listClassSections(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CLASS_SUBJECTS = "/v1/academics/class-sections/{classSectionId}/subjects";
    /**
     * GET /v1/academics/class-sections/{classSectionId}/subjects : List ClassSubjects
     * List ClassSubjects visible to the caller, page by page, with filtering and sorting.
     *
     * @param classSectionId ClassSection id. (required)
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
        value = ClassSectionApi.PATH_LIST_CLASS_SUBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ClassSubject>> listClassSubjects(
         @PathVariable("classSectionId") UUID classSectionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_CLASS_SECTION = "/v1/academics/class-sections/{classSectionId}";
    /**
     * PATCH /v1/academics/class-sections/{classSectionId} : Update ClassSection
     * Partially update a ClassSection (updateMask). Requires If-Match with the current ETag.
     *
     * @param classSectionId ClassSection id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param classSectionUpdate  (required)
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
        value = ClassSectionApi.PATH_UPDATE_CLASS_SECTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassSection> updateClassSection(
         @PathVariable("classSectionId") UUID classSectionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ClassSectionUpdate classSectionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CLASS_SUBJECT = "/v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId}";
    /**
     * PATCH /v1/academics/class-sections/{classSectionId}/subjects/{classSubjectId} : Update ClassSubject
     * Partially update a ClassSubject (updateMask). Requires If-Match with the current ETag.
     *
     * @param classSectionId ClassSection id. (required)
     * @param classSubjectId ClassSubject id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param classSubjectUpdate  (required)
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
        value = ClassSectionApi.PATH_UPDATE_CLASS_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassSubject> updateClassSubject(
         @PathVariable("classSectionId") UUID classSectionId,
         @PathVariable("classSubjectId") UUID classSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ClassSubjectUpdate classSubjectUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
