// Owned API contract of sis.
package com.mysms.students.sis.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.students.sis.api.model.DisciplineIncident;
import com.mysms.students.sis.api.model.DisciplineIncidentUpdate;
import com.mysms.students.sis.api.model.MarkIncidentActionTakenRequest;
import com.mysms.students.sis.api.model.MarkIncidentSeenRequest;
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
public interface DisciplineIncidentApi {

    String PATH_CREATE_DISCIPLINE_INCIDENT = "/v1/sis/discipline-incidents";
    /**
     * POST /v1/sis/discipline-incidents : Create DisciplineIncident
     * Create a DisciplineIncident. Supports Idempotency-Key and validateOnly.
     *
     * @param disciplineIncident  (required)
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
        value = DisciplineIncidentApi.PATH_CREATE_DISCIPLINE_INCIDENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DisciplineIncident> createDisciplineIncident(
         @Valid @RequestBody DisciplineIncident disciplineIncident,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_DISCIPLINE_INCIDENT = "/v1/sis/discipline-incidents/{disciplineIncidentId}";
    /**
     * DELETE /v1/sis/discipline-incidents/{disciplineIncidentId} : Delete DisciplineIncident
     * Soft delete (sets deleted_at).
     *
     * @param disciplineIncidentId DisciplineIncident id. (required)
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
        value = DisciplineIncidentApi.PATH_DELETE_DISCIPLINE_INCIDENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteDisciplineIncident(
         @PathVariable("disciplineIncidentId") UUID disciplineIncidentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_DISCIPLINE_INCIDENT = "/v1/sis/discipline-incidents/{disciplineIncidentId}";
    /**
     * GET /v1/sis/discipline-incidents/{disciplineIncidentId} : Get DisciplineIncident
     * Get one DisciplineIncident by id. The ETag header carries the row version.
     *
     * @param disciplineIncidentId DisciplineIncident id. (required)
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
        value = DisciplineIncidentApi.PATH_GET_DISCIPLINE_INCIDENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DisciplineIncident> getDisciplineIncident(
         @PathVariable("disciplineIncidentId") UUID disciplineIncidentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_DISCIPLINE_INCIDENTS = "/v1/sis/discipline-incidents";
    /**
     * GET /v1/sis/discipline-incidents : List DisciplineIncidents
     * List DisciplineIncidents visible to the caller, page by page, with filtering and sorting.
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
        value = DisciplineIncidentApi.PATH_LIST_DISCIPLINE_INCIDENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<DisciplineIncident>> listDisciplineIncidents(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_INCIDENT_ACTION_TAKEN = "/v1/sis/discipline-incidents/{disciplineIncidentId}:markActionTaken";
    /**
     * POST /v1/sis/discipline-incidents/{disciplineIncidentId}:markActionTaken : Record that action was taken
     * Record that action was taken.
     *
     * @param disciplineIncidentId  (required)
     * @param markIncidentActionTakenRequest  (required)
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
        value = DisciplineIncidentApi.PATH_MARK_INCIDENT_ACTION_TAKEN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DisciplineIncident> markIncidentActionTaken(
         @PathVariable("disciplineIncidentId") UUID disciplineIncidentId,
         @Valid @RequestBody MarkIncidentActionTakenRequest markIncidentActionTakenRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_MARK_INCIDENT_SEEN = "/v1/sis/discipline-incidents/{disciplineIncidentId}:markSeen";
    /**
     * POST /v1/sis/discipline-incidents/{disciplineIncidentId}:markSeen : Guardian acknowledges the incident
     * Guardian acknowledges the incident.
     *
     * @param disciplineIncidentId  (required)
     * @param markIncidentSeenRequest  (required)
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
        value = DisciplineIncidentApi.PATH_MARK_INCIDENT_SEEN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DisciplineIncident> markIncidentSeen(
         @PathVariable("disciplineIncidentId") UUID disciplineIncidentId,
         @Valid @RequestBody MarkIncidentSeenRequest markIncidentSeenRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_DISCIPLINE_INCIDENT = "/v1/sis/discipline-incidents/{disciplineIncidentId}";
    /**
     * PATCH /v1/sis/discipline-incidents/{disciplineIncidentId} : Update DisciplineIncident
     * Partially update a DisciplineIncident (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param disciplineIncidentId DisciplineIncident id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param disciplineIncidentUpdate  (required)
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
        value = DisciplineIncidentApi.PATH_UPDATE_DISCIPLINE_INCIDENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DisciplineIncident> updateDisciplineIncident(
         @PathVariable("disciplineIncidentId") UUID disciplineIncidentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody DisciplineIncidentUpdate disciplineIncidentUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
