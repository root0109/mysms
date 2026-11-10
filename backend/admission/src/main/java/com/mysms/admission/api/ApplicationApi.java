// Owned API contract of admission (JAVA-CODING-RULES §7); first materialized from api/openapi/admission.yaml on 2026-11-10.
package com.mysms.admission.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.admission.api.model.Application;
import com.mysms.admission.api.model.ApplicationEmergencyContact;
import com.mysms.admission.api.model.ApplicationEmergencyContactUpdate;
import com.mysms.admission.api.model.ApplicationGuardian;
import com.mysms.admission.api.model.ApplicationGuardianUpdate;
import com.mysms.admission.api.model.ApplicationUpdate;
import com.mysms.admission.api.model.ApproveApplicationRequest;
import com.mysms.admission.api.model.CancelApplicationRequest;
import com.mysms.admission.api.model.ConfirmAdmissionRequest;
import com.mysms.admission.api.model.ConfirmApplicationRequest;
import com.mysms.admission.api.model.EnrollApplicantRequest;
import com.mysms.admission.api.model.EnrollApplicantResult;
import com.mysms.admission.api.model.MarkApplicationPendingRequest;
import org.springframework.lang.Nullable;
import com.mysms.admission.api.model.RejectApplicationRequest;
import com.mysms.admission.api.model.ResetApplicationRequest;
import com.mysms.admission.api.model.SubmitApplicationRequest;
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
public interface ApplicationApi {

    String PATH_APPROVE_APPLICATION = "/v1/admission/applications/{applicationId}:approve";
    /**
     * POST /v1/admission/applications/{applicationId}:approve : Approve after review / interview (status approved)
     * Approve after review / interview (status approved).
     *
     * @param applicationId  (required)
     * @param approveApplicationRequest  (required)
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
        value = ApplicationApi.PATH_APPROVE_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> approveApplication(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody ApproveApplicationRequest approveApplicationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CANCEL_APPLICATION = "/v1/admission/applications/{applicationId}:cancel";
    /**
     * POST /v1/admission/applications/{applicationId}:cancel : Cancel
     * Cancel.
     *
     * @param applicationId  (required)
     * @param cancelApplicationRequest  (required)
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
        value = ApplicationApi.PATH_CANCEL_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> cancelApplication(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody CancelApplicationRequest cancelApplicationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CONFIRM_ADMISSION = "/v1/admission/applications/{applicationId}:confirmAdmission";
    /**
     * POST /v1/admission/applications/{applicationId}:confirmAdmission : Seat offered and accepted (status admitted)
     * Seat offered and accepted (status admitted).
     *
     * @param applicationId  (required)
     * @param confirmAdmissionRequest  (required)
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
        value = ApplicationApi.PATH_CONFIRM_ADMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> confirmAdmission(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody ConfirmAdmissionRequest confirmAdmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CONFIRM_APPLICATION = "/v1/admission/applications/{applicationId}:confirm";
    /**
     * POST /v1/admission/applications/{applicationId}:confirm : Office confirms the application is complete
     * Office confirms the application is complete.
     *
     * @param applicationId  (required)
     * @param confirmApplicationRequest  (required)
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
        value = ApplicationApi.PATH_CONFIRM_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> confirmApplication(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody ConfirmApplicationRequest confirmApplicationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_APPLICATION = "/v1/admission/applications";
    /**
     * POST /v1/admission/applications : Create Application
     * Create a Application. Supports Idempotency-Key and validateOnly.
     *
     * @param application  (required)
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
        value = ApplicationApi.PATH_CREATE_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> createApplication(
         @Valid @RequestBody Application application,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_APPLICATION_EMERGENCY_CONTACT = "/v1/admission/applications/{applicationId}/emergency-contacts";
    /**
     * POST /v1/admission/applications/{applicationId}/emergency-contacts : Create ApplicationEmergencyContact
     * Create a ApplicationEmergencyContact. Supports Idempotency-Key and validateOnly.
     *
     * @param applicationId Application id. (required)
     * @param applicationEmergencyContact  (required)
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
        value = ApplicationApi.PATH_CREATE_APPLICATION_EMERGENCY_CONTACT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ApplicationEmergencyContact> createApplicationEmergencyContact(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody ApplicationEmergencyContact applicationEmergencyContact,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_APPLICATION_GUARDIAN = "/v1/admission/applications/{applicationId}/guardians";
    /**
     * POST /v1/admission/applications/{applicationId}/guardians : Create ApplicationGuardian
     * Create a ApplicationGuardian. Supports Idempotency-Key and validateOnly.
     *
     * @param applicationId Application id. (required)
     * @param applicationGuardian  (required)
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
        value = ApplicationApi.PATH_CREATE_APPLICATION_GUARDIAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ApplicationGuardian> createApplicationGuardian(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody ApplicationGuardian applicationGuardian,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_APPLICATION = "/v1/admission/applications/{applicationId}";
    /**
     * DELETE /v1/admission/applications/{applicationId} : Delete Application
     * Soft delete (sets deleted_at).
     *
     * @param applicationId Application id. (required)
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
        value = ApplicationApi.PATH_DELETE_APPLICATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteApplication(
         @PathVariable("applicationId") UUID applicationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_APPLICATION_EMERGENCY_CONTACT = "/v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}";
    /**
     * DELETE /v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId} : Delete ApplicationEmergencyContact
     * Soft delete (sets deleted_at).
     *
     * @param applicationId Application id. (required)
     * @param applicationEmergencyContactId ApplicationEmergencyContact id. (required)
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
        value = ApplicationApi.PATH_DELETE_APPLICATION_EMERGENCY_CONTACT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteApplicationEmergencyContact(
         @PathVariable("applicationId") UUID applicationId,
         @PathVariable("applicationEmergencyContactId") UUID applicationEmergencyContactId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_APPLICATION_GUARDIAN = "/v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}";
    /**
     * DELETE /v1/admission/applications/{applicationId}/guardians/{applicationGuardianId} : Delete ApplicationGuardian
     * Soft delete (sets deleted_at).
     *
     * @param applicationId Application id. (required)
     * @param applicationGuardianId ApplicationGuardian id. (required)
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
        value = ApplicationApi.PATH_DELETE_APPLICATION_GUARDIAN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteApplicationGuardian(
         @PathVariable("applicationId") UUID applicationId,
         @PathVariable("applicationGuardianId") UUID applicationGuardianId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_ENROLL_APPLICANT = "/v1/admission/applications/{applicationId}:enroll";
    /**
     * POST /v1/admission/applications/{applicationId}:enroll : Create person, student, guardians, enrolment and fee dues from an admitted application; sets status done
     * Create person, student, guardians, enrolment and fee dues from an admitted application; sets status done.
     *
     * @param applicationId  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param enrollApplicantRequest  (required)
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
        value = ApplicationApi.PATH_ENROLL_APPLICANT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EnrollApplicantResult> enrollApplicant(
         @PathVariable("applicationId") UUID applicationId,
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody EnrollApplicantRequest enrollApplicantRequest
    );


    String PATH_GET_APPLICATION = "/v1/admission/applications/{applicationId}";
    /**
     * GET /v1/admission/applications/{applicationId} : Get Application
     * Get one Application by id. The ETag header carries the row version.
     *
     * @param applicationId Application id. (required)
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
        value = ApplicationApi.PATH_GET_APPLICATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Application> getApplication(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_APPLICATION_EMERGENCY_CONTACT = "/v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}";
    /**
     * GET /v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId} : Get ApplicationEmergencyContact
     * Get one ApplicationEmergencyContact by id. The ETag header carries the row version.
     *
     * @param applicationId Application id. (required)
     * @param applicationEmergencyContactId ApplicationEmergencyContact id. (required)
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
        value = ApplicationApi.PATH_GET_APPLICATION_EMERGENCY_CONTACT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ApplicationEmergencyContact> getApplicationEmergencyContact(
         @PathVariable("applicationId") UUID applicationId,
         @PathVariable("applicationEmergencyContactId") UUID applicationEmergencyContactId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_APPLICATION_GUARDIAN = "/v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}";
    /**
     * GET /v1/admission/applications/{applicationId}/guardians/{applicationGuardianId} : Get ApplicationGuardian
     * Get one ApplicationGuardian by id. The ETag header carries the row version.
     *
     * @param applicationId Application id. (required)
     * @param applicationGuardianId ApplicationGuardian id. (required)
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
        value = ApplicationApi.PATH_GET_APPLICATION_GUARDIAN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ApplicationGuardian> getApplicationGuardian(
         @PathVariable("applicationId") UUID applicationId,
         @PathVariable("applicationGuardianId") UUID applicationGuardianId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_APPLICATION_EMERGENCY_CONTACTS = "/v1/admission/applications/{applicationId}/emergency-contacts";
    /**
     * GET /v1/admission/applications/{applicationId}/emergency-contacts : List ApplicationEmergencyContacts
     * List ApplicationEmergencyContacts visible to the caller, page by page, with filtering and sorting.
     *
     * @param applicationId Application id. (required)
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
        value = ApplicationApi.PATH_LIST_APPLICATION_EMERGENCY_CONTACTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ApplicationEmergencyContact>> listApplicationEmergencyContacts(
         @PathVariable("applicationId") UUID applicationId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_APPLICATION_GUARDIANS = "/v1/admission/applications/{applicationId}/guardians";
    /**
     * GET /v1/admission/applications/{applicationId}/guardians : List ApplicationGuardians
     * List ApplicationGuardians visible to the caller, page by page, with filtering and sorting.
     *
     * @param applicationId Application id. (required)
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
        value = ApplicationApi.PATH_LIST_APPLICATION_GUARDIANS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ApplicationGuardian>> listApplicationGuardians(
         @PathVariable("applicationId") UUID applicationId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_APPLICATIONS = "/v1/admission/applications";
    /**
     * GET /v1/admission/applications : List Applications
     * List Applications visible to the caller, page by page, with filtering and sorting.
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
        value = ApplicationApi.PATH_LIST_APPLICATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Application>> listApplications(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_APPLICATION_PENDING = "/v1/admission/applications/{applicationId}:markPending";
    /**
     * POST /v1/admission/applications/{applicationId}:markPending : Put on hold
     * Put on hold.
     *
     * @param applicationId  (required)
     * @param markApplicationPendingRequest  (required)
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
        value = ApplicationApi.PATH_MARK_APPLICATION_PENDING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> markApplicationPending(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody MarkApplicationPendingRequest markApplicationPendingRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REJECT_APPLICATION = "/v1/admission/applications/{applicationId}:reject";
    /**
     * POST /v1/admission/applications/{applicationId}:reject : Reject
     * Reject.
     *
     * @param applicationId  (required)
     * @param rejectApplicationRequest  (required)
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
        value = ApplicationApi.PATH_REJECT_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> rejectApplication(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody RejectApplicationRequest rejectApplicationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_APPLICATION = "/v1/admission/applications/{applicationId}:resetToDraft";
    /**
     * POST /v1/admission/applications/{applicationId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param applicationId  (required)
     * @param resetApplicationRequest  (required)
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
        value = ApplicationApi.PATH_RESET_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> resetApplication(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody ResetApplicationRequest resetApplicationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_APPLICATION = "/v1/admission/applications/{applicationId}:submit";
    /**
     * POST /v1/admission/applications/{applicationId}:submit : Submit (applicant or office)
     * Submit (applicant or office).
     *
     * @param applicationId  (required)
     * @param submitApplicationRequest  (required)
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
        value = ApplicationApi.PATH_SUBMIT_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> submitApplication(
         @PathVariable("applicationId") UUID applicationId,
         @Valid @RequestBody SubmitApplicationRequest submitApplicationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_APPLICATION = "/v1/admission/applications/{applicationId}";
    /**
     * PATCH /v1/admission/applications/{applicationId} : Update Application
     * Partially update a Application (updateMask). Requires If-Match with the current ETag.
     *
     * @param applicationId Application id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param applicationUpdate  (required)
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
        value = ApplicationApi.PATH_UPDATE_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Application> updateApplication(
         @PathVariable("applicationId") UUID applicationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ApplicationUpdate applicationUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_APPLICATION_EMERGENCY_CONTACT = "/v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId}";
    /**
     * PATCH /v1/admission/applications/{applicationId}/emergency-contacts/{applicationEmergencyContactId} : Update ApplicationEmergencyContact
     * Partially update a ApplicationEmergencyContact (updateMask). Requires If-Match with the current ETag.
     *
     * @param applicationId Application id. (required)
     * @param applicationEmergencyContactId ApplicationEmergencyContact id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param applicationEmergencyContactUpdate  (required)
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
        value = ApplicationApi.PATH_UPDATE_APPLICATION_EMERGENCY_CONTACT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ApplicationEmergencyContact> updateApplicationEmergencyContact(
         @PathVariable("applicationId") UUID applicationId,
         @PathVariable("applicationEmergencyContactId") UUID applicationEmergencyContactId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ApplicationEmergencyContactUpdate applicationEmergencyContactUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_APPLICATION_GUARDIAN = "/v1/admission/applications/{applicationId}/guardians/{applicationGuardianId}";
    /**
     * PATCH /v1/admission/applications/{applicationId}/guardians/{applicationGuardianId} : Update ApplicationGuardian
     * Partially update a ApplicationGuardian (updateMask). Requires If-Match with the current ETag.
     *
     * @param applicationId Application id. (required)
     * @param applicationGuardianId ApplicationGuardian id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param applicationGuardianUpdate  (required)
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
        value = ApplicationApi.PATH_UPDATE_APPLICATION_GUARDIAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ApplicationGuardian> updateApplicationGuardian(
         @PathVariable("applicationId") UUID applicationId,
         @PathVariable("applicationGuardianId") UUID applicationGuardianId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ApplicationGuardianUpdate applicationGuardianUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
