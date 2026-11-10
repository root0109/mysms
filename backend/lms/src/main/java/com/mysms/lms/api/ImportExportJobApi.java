// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.CancelImportExportJobRequest;
import com.mysms.lms.api.model.DownloadLink;
import com.mysms.lms.api.model.ImportExportJob;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.RestoreCohortArchiveRequest;
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
public interface ImportExportJobApi {

    String PATH_CANCEL_IMPORT_EXPORT_JOB = "/v1/lms/import-export-jobs/{importExportJobId}:cancel";
    /**
     * POST /v1/lms/import-export-jobs/{importExportJobId}:cancel : Cancel a pending job
     * Cancel a pending job.
     *
     * @param importExportJobId  (required)
     * @param cancelImportExportJobRequest  (required)
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
        value = ImportExportJobApi.PATH_CANCEL_IMPORT_EXPORT_JOB,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ImportExportJob> cancelImportExportJob(
         @PathVariable("importExportJobId") UUID importExportJobId,
         @Valid @RequestBody CancelImportExportJobRequest cancelImportExportJobRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_IMPORT_EXPORT_JOB = "/v1/lms/import-export-jobs";
    /**
     * POST /v1/lms/import-export-jobs : Create ImportExportJob
     * Create a ImportExportJob. Supports Idempotency-Key and validateOnly.
     *
     * @param importExportJob  (required)
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
        value = ImportExportJobApi.PATH_CREATE_IMPORT_EXPORT_JOB,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ImportExportJob> createImportExportJob(
         @Valid @RequestBody ImportExportJob importExportJob,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_GET_IMPORT_EXPORT_JOB = "/v1/lms/import-export-jobs/{importExportJobId}";
    /**
     * GET /v1/lms/import-export-jobs/{importExportJobId} : Get ImportExportJob
     * Get one ImportExportJob by id. The ETag header carries the row version.
     *
     * @param importExportJobId ImportExportJob id. (required)
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
        value = ImportExportJobApi.PATH_GET_IMPORT_EXPORT_JOB,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ImportExportJob> getImportExportJob(
         @PathVariable("importExportJobId") UUID importExportJobId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_JOB_PACKAGE = "/v1/lms/import-export-jobs/{importExportJobId}:package";
    /**
     * GET /v1/lms/import-export-jobs/{importExportJobId}:package : Download the export package or the import error log
     * Download the export package or the import error log.
     *
     * @param importExportJobId  (required)
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
        method = RequestMethod.GET,
        value = ImportExportJobApi.PATH_GET_JOB_PACKAGE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DownloadLink> getJobPackage(
         @PathVariable("importExportJobId") UUID importExportJobId
    );


    String PATH_LIST_IMPORT_EXPORT_JOBS = "/v1/lms/import-export-jobs";
    /**
     * GET /v1/lms/import-export-jobs : List ImportExportJobs
     * List ImportExportJobs visible to the caller, page by page, with filtering and sorting.
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
        value = ImportExportJobApi.PATH_LIST_IMPORT_EXPORT_JOBS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ImportExportJob>> listImportExportJobs(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RESTORE_COHORT_ARCHIVE = "/v1/lms/import-export-jobs/{importExportJobId}:restore";
    /**
     * POST /v1/lms/import-export-jobs/{importExportJobId}:restore : Restore an archived cohort from its export
     * Restore an archived cohort from its export.
     *
     * @param importExportJobId  (required)
     * @param restoreCohortArchiveRequest  (required)
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
        value = ImportExportJobApi.PATH_RESTORE_COHORT_ARCHIVE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ImportExportJob> restoreCohortArchive(
         @PathVariable("importExportJobId") UUID importExportJobId,
         @Valid @RequestBody RestoreCohortArchiveRequest restoreCohortArchiveRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
