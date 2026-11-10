// Owned API contract of people (JAVA-CODING-RULES §7); first materialized from api/openapi/people.yaml on 2026-11-10.
package com.mysms.people.api;

import com.mysms.people.api.model.BirthdayEntry;
import com.mysms.core.paging.PagingInfo;
import com.mysms.people.api.model.Certification;
import com.mysms.people.api.model.CertificationUpdate;
import com.mysms.people.api.model.ConsentRecord;
import org.springframework.format.annotation.DateTimeFormat;
import com.mysms.people.api.model.EducationHistory;
import com.mysms.people.api.model.EducationHistoryUpdate;
import com.mysms.people.api.model.EmergencyContact;
import com.mysms.people.api.model.EmergencyContactUpdate;
import java.time.LocalDate;
import com.mysms.people.api.model.MyPersonUpdate;
import org.springframework.lang.Nullable;
import com.mysms.people.api.model.Person;
import com.mysms.people.api.model.PersonAddress;
import com.mysms.people.api.model.PersonAddressUpdate;
import com.mysms.people.api.model.PersonDocument;
import com.mysms.people.api.model.PersonDocumentUpdate;
import com.mysms.people.api.model.PersonSkill;
import com.mysms.people.api.model.PersonSkillUpdate;
import com.mysms.people.api.model.PersonSocialLink;
import com.mysms.people.api.model.PersonSocialLinkUpdate;
import com.mysms.people.api.model.PersonUpdate;
import com.mysms.people.api.model.PublicProfile;
import com.mysms.people.api.model.PublicProfileUpdate;
import com.mysms.people.api.model.SendGreetingsRequest;
import com.mysms.people.api.model.SendResult;
import java.util.UUID;
import com.mysms.people.api.model.WithdrawConsentRequest;
import com.mysms.people.api.model.WorkExperience;
import com.mysms.people.api.model.WorkExperienceUpdate;
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
public interface PersonApi {

    String PATH_CREATE_CERTIFICATION = "/v1/people/persons/{personId}/certifications";
    /**
     * POST /v1/people/persons/{personId}/certifications : Create Certification
     * Create a Certification. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param certification  (required)
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
        value = PersonApi.PATH_CREATE_CERTIFICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Certification> createCertification(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody Certification certification,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CONSENT_RECORD = "/v1/people/persons/{personId}/consents";
    /**
     * POST /v1/people/persons/{personId}/consents : Create ConsentRecord
     * Create a ConsentRecord. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param consentRecord  (required)
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
        value = PersonApi.PATH_CREATE_CONSENT_RECORD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConsentRecord> createConsentRecord(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody ConsentRecord consentRecord,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EDUCATION_HISTORY = "/v1/people/persons/{personId}/education";
    /**
     * POST /v1/people/persons/{personId}/education : Create EducationHistory
     * Create a EducationHistory. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param educationHistory  (required)
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
        value = PersonApi.PATH_CREATE_EDUCATION_HISTORY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EducationHistory> createEducationHistory(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody EducationHistory educationHistory,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EMERGENCY_CONTACT = "/v1/people/persons/{personId}/emergency-contacts";
    /**
     * POST /v1/people/persons/{personId}/emergency-contacts : Create EmergencyContact
     * Create a EmergencyContact. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param emergencyContact  (required)
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
        value = PersonApi.PATH_CREATE_EMERGENCY_CONTACT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EmergencyContact> createEmergencyContact(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody EmergencyContact emergencyContact,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PERSON = "/v1/people/persons";
    /**
     * POST /v1/people/persons : Create Person
     * Create a Person. Supports Idempotency-Key and validateOnly.
     *
     * @param person  (required)
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
        value = PersonApi.PATH_CREATE_PERSON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Person> createPerson(
         @Valid @RequestBody Person person,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PERSON_ADDRESS = "/v1/people/persons/{personId}/addresses";
    /**
     * POST /v1/people/persons/{personId}/addresses : Create PersonAddress
     * Create a PersonAddress. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param personAddress  (required)
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
        value = PersonApi.PATH_CREATE_PERSON_ADDRESS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonAddress> createPersonAddress(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody PersonAddress personAddress,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PERSON_DOCUMENT = "/v1/people/persons/{personId}/documents";
    /**
     * POST /v1/people/persons/{personId}/documents : Create PersonDocument
     * Create a PersonDocument. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param personDocument  (required)
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
        value = PersonApi.PATH_CREATE_PERSON_DOCUMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonDocument> createPersonDocument(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody PersonDocument personDocument,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PERSON_SKILL = "/v1/people/persons/{personId}/skills";
    /**
     * POST /v1/people/persons/{personId}/skills : Create PersonSkill
     * Create a PersonSkill. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param personSkill  (required)
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
        value = PersonApi.PATH_CREATE_PERSON_SKILL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonSkill> createPersonSkill(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody PersonSkill personSkill,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PERSON_SOCIAL_LINK = "/v1/people/persons/{personId}/social-links";
    /**
     * POST /v1/people/persons/{personId}/social-links : Create PersonSocialLink
     * Create a PersonSocialLink. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param personSocialLink  (required)
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
        value = PersonApi.PATH_CREATE_PERSON_SOCIAL_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonSocialLink> createPersonSocialLink(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody PersonSocialLink personSocialLink,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PUBLIC_PROFILE = "/v1/people/persons/{personId}/public-profile";
    /**
     * POST /v1/people/persons/{personId}/public-profile : Create PublicProfile
     * Create a PublicProfile. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param publicProfile  (required)
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
        value = PersonApi.PATH_CREATE_PUBLIC_PROFILE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PublicProfile> createPublicProfile(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody PublicProfile publicProfile,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_WORK_EXPERIENCE = "/v1/people/persons/{personId}/work-experience";
    /**
     * POST /v1/people/persons/{personId}/work-experience : Create WorkExperience
     * Create a WorkExperience. Supports Idempotency-Key and validateOnly.
     *
     * @param personId Person id. (required)
     * @param workExperience  (required)
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
        value = PersonApi.PATH_CREATE_WORK_EXPERIENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<WorkExperience> createWorkExperience(
         @PathVariable("personId") UUID personId,
         @Valid @RequestBody WorkExperience workExperience,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_CERTIFICATION = "/v1/people/persons/{personId}/certifications/{certificationId}";
    /**
     * DELETE /v1/people/persons/{personId}/certifications/{certificationId} : Delete Certification
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param certificationId Certification id. (required)
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
        value = PersonApi.PATH_DELETE_CERTIFICATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCertification(
         @PathVariable("personId") UUID personId,
         @PathVariable("certificationId") UUID certificationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EDUCATION_HISTORY = "/v1/people/persons/{personId}/education/{educationHistoryId}";
    /**
     * DELETE /v1/people/persons/{personId}/education/{educationHistoryId} : Delete EducationHistory
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param educationHistoryId EducationHistory id. (required)
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
        value = PersonApi.PATH_DELETE_EDUCATION_HISTORY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEducationHistory(
         @PathVariable("personId") UUID personId,
         @PathVariable("educationHistoryId") UUID educationHistoryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EMERGENCY_CONTACT = "/v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}";
    /**
     * DELETE /v1/people/persons/{personId}/emergency-contacts/{emergencyContactId} : Delete EmergencyContact
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param emergencyContactId EmergencyContact id. (required)
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
        value = PersonApi.PATH_DELETE_EMERGENCY_CONTACT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEmergencyContact(
         @PathVariable("personId") UUID personId,
         @PathVariable("emergencyContactId") UUID emergencyContactId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PERSON = "/v1/people/persons/{personId}";
    /**
     * DELETE /v1/people/persons/{personId} : Delete Person
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_DELETE_PERSON,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePerson(
         @PathVariable("personId") UUID personId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PERSON_ADDRESS = "/v1/people/persons/{personId}/addresses/{personAddressId}";
    /**
     * DELETE /v1/people/persons/{personId}/addresses/{personAddressId} : Delete PersonAddress
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param personAddressId PersonAddress id. (required)
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
        value = PersonApi.PATH_DELETE_PERSON_ADDRESS,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePersonAddress(
         @PathVariable("personId") UUID personId,
         @PathVariable("personAddressId") UUID personAddressId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PERSON_DOCUMENT = "/v1/people/persons/{personId}/documents/{personDocumentId}";
    /**
     * DELETE /v1/people/persons/{personId}/documents/{personDocumentId} : Delete PersonDocument
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param personDocumentId PersonDocument id. (required)
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
        value = PersonApi.PATH_DELETE_PERSON_DOCUMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePersonDocument(
         @PathVariable("personId") UUID personId,
         @PathVariable("personDocumentId") UUID personDocumentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PERSON_SKILL = "/v1/people/persons/{personId}/skills/{personSkillId}";
    /**
     * DELETE /v1/people/persons/{personId}/skills/{personSkillId} : Delete PersonSkill
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param personSkillId PersonSkill id. (required)
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
        value = PersonApi.PATH_DELETE_PERSON_SKILL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePersonSkill(
         @PathVariable("personId") UUID personId,
         @PathVariable("personSkillId") UUID personSkillId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PERSON_SOCIAL_LINK = "/v1/people/persons/{personId}/social-links/{personSocialLinkId}";
    /**
     * DELETE /v1/people/persons/{personId}/social-links/{personSocialLinkId} : Delete PersonSocialLink
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param personSocialLinkId PersonSocialLink id. (required)
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
        value = PersonApi.PATH_DELETE_PERSON_SOCIAL_LINK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePersonSocialLink(
         @PathVariable("personId") UUID personId,
         @PathVariable("personSocialLinkId") UUID personSocialLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PUBLIC_PROFILE = "/v1/people/persons/{personId}/public-profile/{publicProfileId}";
    /**
     * DELETE /v1/people/persons/{personId}/public-profile/{publicProfileId} : Delete PublicProfile
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param publicProfileId PublicProfile id. (required)
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
        value = PersonApi.PATH_DELETE_PUBLIC_PROFILE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePublicProfile(
         @PathVariable("personId") UUID personId,
         @PathVariable("publicProfileId") UUID publicProfileId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_WORK_EXPERIENCE = "/v1/people/persons/{personId}/work-experience/{workExperienceId}";
    /**
     * DELETE /v1/people/persons/{personId}/work-experience/{workExperienceId} : Delete WorkExperience
     * Soft delete (sets deleted_at).
     *
     * @param personId Person id. (required)
     * @param workExperienceId WorkExperience id. (required)
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
        value = PersonApi.PATH_DELETE_WORK_EXPERIENCE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteWorkExperience(
         @PathVariable("personId") UUID personId,
         @PathVariable("workExperienceId") UUID workExperienceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_CERTIFICATION = "/v1/people/persons/{personId}/certifications/{certificationId}";
    /**
     * GET /v1/people/persons/{personId}/certifications/{certificationId} : Get Certification
     * Get one Certification by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param certificationId Certification id. (required)
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
        value = PersonApi.PATH_GET_CERTIFICATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Certification> getCertification(
         @PathVariable("personId") UUID personId,
         @PathVariable("certificationId") UUID certificationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CONSENT_RECORD = "/v1/people/persons/{personId}/consents/{consentRecordId}";
    /**
     * GET /v1/people/persons/{personId}/consents/{consentRecordId} : Get ConsentRecord
     * Get one ConsentRecord by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param consentRecordId ConsentRecord id. (required)
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
        value = PersonApi.PATH_GET_CONSENT_RECORD,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ConsentRecord> getConsentRecord(
         @PathVariable("personId") UUID personId,
         @PathVariable("consentRecordId") UUID consentRecordId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CONSENT_STATUS = "/v1/people/persons/{personId}:consentStatus";
    /**
     * GET /v1/people/persons/{personId}:consentStatus : Consents currently in force for a person, one per purpose (latest record with withdrawnAt empty)
     * Consents currently in force for a person, one per purpose (latest record with withdrawnAt empty).
     *
     * @param personId  (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
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
        value = PersonApi.PATH_GET_CONSENT_STATUS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ConsentRecord>> getConsentStatus(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_EDUCATION_HISTORY = "/v1/people/persons/{personId}/education/{educationHistoryId}";
    /**
     * GET /v1/people/persons/{personId}/education/{educationHistoryId} : Get EducationHistory
     * Get one EducationHistory by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param educationHistoryId EducationHistory id. (required)
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
        value = PersonApi.PATH_GET_EDUCATION_HISTORY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EducationHistory> getEducationHistory(
         @PathVariable("personId") UUID personId,
         @PathVariable("educationHistoryId") UUID educationHistoryId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EMERGENCY_CONTACT = "/v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}";
    /**
     * GET /v1/people/persons/{personId}/emergency-contacts/{emergencyContactId} : Get EmergencyContact
     * Get one EmergencyContact by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param emergencyContactId EmergencyContact id. (required)
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
        value = PersonApi.PATH_GET_EMERGENCY_CONTACT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EmergencyContact> getEmergencyContact(
         @PathVariable("personId") UUID personId,
         @PathVariable("emergencyContactId") UUID emergencyContactId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MY_PERSON = "/v1/people/me";
    /**
     * GET /v1/people/me : The caller&#39;s own person record
     * The Person linked to the caller&#39;s user account (token claim pid), without the administrators&#39; internal remarks. 404 when the account has no person record.
     *
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
        value = PersonApi.PATH_GET_MY_PERSON,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Person> getMyPerson(
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PERSON = "/v1/people/persons/{personId}";
    /**
     * GET /v1/people/persons/{personId} : Get Person
     * Get one Person by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_GET_PERSON,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Person> getPerson(
         @PathVariable("personId") UUID personId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PERSON_ADDRESS = "/v1/people/persons/{personId}/addresses/{personAddressId}";
    /**
     * GET /v1/people/persons/{personId}/addresses/{personAddressId} : Get PersonAddress
     * Get one PersonAddress by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param personAddressId PersonAddress id. (required)
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
        value = PersonApi.PATH_GET_PERSON_ADDRESS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PersonAddress> getPersonAddress(
         @PathVariable("personId") UUID personId,
         @PathVariable("personAddressId") UUID personAddressId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PERSON_DOCUMENT = "/v1/people/persons/{personId}/documents/{personDocumentId}";
    /**
     * GET /v1/people/persons/{personId}/documents/{personDocumentId} : Get PersonDocument
     * Get one PersonDocument by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param personDocumentId PersonDocument id. (required)
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
        value = PersonApi.PATH_GET_PERSON_DOCUMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PersonDocument> getPersonDocument(
         @PathVariable("personId") UUID personId,
         @PathVariable("personDocumentId") UUID personDocumentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PERSON_SKILL = "/v1/people/persons/{personId}/skills/{personSkillId}";
    /**
     * GET /v1/people/persons/{personId}/skills/{personSkillId} : Get PersonSkill
     * Get one PersonSkill by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param personSkillId PersonSkill id. (required)
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
        value = PersonApi.PATH_GET_PERSON_SKILL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PersonSkill> getPersonSkill(
         @PathVariable("personId") UUID personId,
         @PathVariable("personSkillId") UUID personSkillId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PERSON_SOCIAL_LINK = "/v1/people/persons/{personId}/social-links/{personSocialLinkId}";
    /**
     * GET /v1/people/persons/{personId}/social-links/{personSocialLinkId} : Get PersonSocialLink
     * Get one PersonSocialLink by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param personSocialLinkId PersonSocialLink id. (required)
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
        value = PersonApi.PATH_GET_PERSON_SOCIAL_LINK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PersonSocialLink> getPersonSocialLink(
         @PathVariable("personId") UUID personId,
         @PathVariable("personSocialLinkId") UUID personSocialLinkId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PUBLIC_PROFILE = "/v1/people/persons/{personId}/public-profile/{publicProfileId}";
    /**
     * GET /v1/people/persons/{personId}/public-profile/{publicProfileId} : Get PublicProfile
     * Get one PublicProfile by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param publicProfileId PublicProfile id. (required)
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
        value = PersonApi.PATH_GET_PUBLIC_PROFILE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PublicProfile> getPublicProfile(
         @PathVariable("personId") UUID personId,
         @PathVariable("publicProfileId") UUID publicProfileId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_WORK_EXPERIENCE = "/v1/people/persons/{personId}/work-experience/{workExperienceId}";
    /**
     * GET /v1/people/persons/{personId}/work-experience/{workExperienceId} : Get WorkExperience
     * Get one WorkExperience by id. The ETag header carries the row version.
     *
     * @param personId Person id. (required)
     * @param workExperienceId WorkExperience id. (required)
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
        value = PersonApi.PATH_GET_WORK_EXPERIENCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<WorkExperience> getWorkExperience(
         @PathVariable("personId") UUID personId,
         @PathVariable("workExperienceId") UUID workExperienceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_BIRTHDAYS = "/v1/people/persons:birthdays";
    /**
     * GET /v1/people/persons:birthdays : Birthdays and work anniversaries in a date window
     * Birthdays and work anniversaries in a date window.
     *
     * @param institutionId Institution. (required)
     * @param fromDate From. (required)
     * @param toDate To. (required)
     * @param kinds birthday / work_anniversary. (optional)
     * @param roles student / staff / guardian. (optional)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
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
        value = PersonApi.PATH_LIST_BIRTHDAYS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<BirthdayEntry>> listBirthdays(
        @NotNull  @Valid @RequestParam(value = "institutionId", required = true) UUID institutionId,
        @NotNull  @Valid @RequestParam(value = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @NotNull  @Valid @RequestParam(value = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
         @RequestParam(value = "kinds", required = false) @Nullable List<String> kinds,
         @RequestParam(value = "roles", required = false) @Nullable List<String> roles,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_LIST_CERTIFICATIONS = "/v1/people/persons/{personId}/certifications";
    /**
     * GET /v1/people/persons/{personId}/certifications : List Certifications
     * List Certifications visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_CERTIFICATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Certification>> listCertifications(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CONSENT_RECORDS = "/v1/people/persons/{personId}/consents";
    /**
     * GET /v1/people/persons/{personId}/consents : List ConsentRecords
     * List ConsentRecords visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_CONSENT_RECORDS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ConsentRecord>> listConsentRecords(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EDUCATION_HISTORY = "/v1/people/persons/{personId}/education";
    /**
     * GET /v1/people/persons/{personId}/education : List EducationHistory
     * List EducationHistory visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_EDUCATION_HISTORY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EducationHistory>> listEducationHistory(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EMERGENCY_CONTACTS = "/v1/people/persons/{personId}/emergency-contacts";
    /**
     * GET /v1/people/persons/{personId}/emergency-contacts : List EmergencyContacts
     * List EmergencyContacts visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_EMERGENCY_CONTACTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EmergencyContact>> listEmergencyContacts(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PERSON_ADDRESSES = "/v1/people/persons/{personId}/addresses";
    /**
     * GET /v1/people/persons/{personId}/addresses : List PersonAddresses
     * List PersonAddresses visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_PERSON_ADDRESSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PersonAddress>> listPersonAddresses(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PERSON_DOCUMENTS = "/v1/people/persons/{personId}/documents";
    /**
     * GET /v1/people/persons/{personId}/documents : List PersonDocuments
     * List PersonDocuments visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_PERSON_DOCUMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PersonDocument>> listPersonDocuments(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PERSON_SKILLS = "/v1/people/persons/{personId}/skills";
    /**
     * GET /v1/people/persons/{personId}/skills : List PersonSkills
     * List PersonSkills visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_PERSON_SKILLS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PersonSkill>> listPersonSkills(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PERSON_SOCIAL_LINKS = "/v1/people/persons/{personId}/social-links";
    /**
     * GET /v1/people/persons/{personId}/social-links : List PersonSocialLinks
     * List PersonSocialLinks visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_PERSON_SOCIAL_LINKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PersonSocialLink>> listPersonSocialLinks(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PERSONS = "/v1/people/persons";
    /**
     * GET /v1/people/persons : List Persons
     * List Persons visible to the caller, page by page, with filtering and sorting.
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
        value = PersonApi.PATH_LIST_PERSONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Person>> listPersons(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_WORK_EXPERIENCES = "/v1/people/persons/{personId}/work-experience";
    /**
     * GET /v1/people/persons/{personId}/work-experience : List WorkExperiences
     * List WorkExperiences visible to the caller, page by page, with filtering and sorting.
     *
     * @param personId Person id. (required)
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
        value = PersonApi.PATH_LIST_WORK_EXPERIENCES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<WorkExperience>> listWorkExperiences(
         @PathVariable("personId") UUID personId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SEND_GREETINGS = "/v1/people/persons:sendGreetings";
    /**
     * POST /v1/people/persons:sendGreetings : Send birthday / anniversary greetings
     * Send birthday / anniversary greetings.
     *
     * @param sendGreetingsRequest  (required)
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
        value = PersonApi.PATH_SEND_GREETINGS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SendResult> sendGreetings(
         @Valid @RequestBody SendGreetingsRequest sendGreetingsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_CERTIFICATION = "/v1/people/persons/{personId}/certifications/{certificationId}";
    /**
     * PATCH /v1/people/persons/{personId}/certifications/{certificationId} : Update Certification
     * Partially update a Certification (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param certificationId Certification id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param certificationUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_CERTIFICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Certification> updateCertification(
         @PathVariable("personId") UUID personId,
         @PathVariable("certificationId") UUID certificationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CertificationUpdate certificationUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EDUCATION_HISTORY = "/v1/people/persons/{personId}/education/{educationHistoryId}";
    /**
     * PATCH /v1/people/persons/{personId}/education/{educationHistoryId} : Update EducationHistory
     * Partially update a EducationHistory (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param educationHistoryId EducationHistory id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param educationHistoryUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_EDUCATION_HISTORY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EducationHistory> updateEducationHistory(
         @PathVariable("personId") UUID personId,
         @PathVariable("educationHistoryId") UUID educationHistoryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EducationHistoryUpdate educationHistoryUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EMERGENCY_CONTACT = "/v1/people/persons/{personId}/emergency-contacts/{emergencyContactId}";
    /**
     * PATCH /v1/people/persons/{personId}/emergency-contacts/{emergencyContactId} : Update EmergencyContact
     * Partially update a EmergencyContact (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param emergencyContactId EmergencyContact id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param emergencyContactUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_EMERGENCY_CONTACT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EmergencyContact> updateEmergencyContact(
         @PathVariable("personId") UUID personId,
         @PathVariable("emergencyContactId") UUID emergencyContactId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EmergencyContactUpdate emergencyContactUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_MY_PERSON = "/v1/people/me";
    /**
     * PATCH /v1/people/me : Update the caller&#39;s own person record
     * Partially updates the contact and self-description fields of the caller&#39;s own Person (updateMask). Requires If-Match with the current ETag.
     *
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param myPersonUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_MY_PERSON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Person> updateMyPerson(
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MyPersonUpdate myPersonUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PERSON = "/v1/people/persons/{personId}";
    /**
     * PATCH /v1/people/persons/{personId} : Update Person
     * Partially update a Person (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param personUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_PERSON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Person> updatePerson(
         @PathVariable("personId") UUID personId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PersonUpdate personUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PERSON_ADDRESS = "/v1/people/persons/{personId}/addresses/{personAddressId}";
    /**
     * PATCH /v1/people/persons/{personId}/addresses/{personAddressId} : Update PersonAddress
     * Partially update a PersonAddress (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param personAddressId PersonAddress id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param personAddressUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_PERSON_ADDRESS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonAddress> updatePersonAddress(
         @PathVariable("personId") UUID personId,
         @PathVariable("personAddressId") UUID personAddressId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PersonAddressUpdate personAddressUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PERSON_DOCUMENT = "/v1/people/persons/{personId}/documents/{personDocumentId}";
    /**
     * PATCH /v1/people/persons/{personId}/documents/{personDocumentId} : Update PersonDocument
     * Partially update a PersonDocument (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param personDocumentId PersonDocument id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param personDocumentUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_PERSON_DOCUMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonDocument> updatePersonDocument(
         @PathVariable("personId") UUID personId,
         @PathVariable("personDocumentId") UUID personDocumentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PersonDocumentUpdate personDocumentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PERSON_SKILL = "/v1/people/persons/{personId}/skills/{personSkillId}";
    /**
     * PATCH /v1/people/persons/{personId}/skills/{personSkillId} : Update PersonSkill
     * Partially update a PersonSkill (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param personSkillId PersonSkill id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param personSkillUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_PERSON_SKILL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonSkill> updatePersonSkill(
         @PathVariable("personId") UUID personId,
         @PathVariable("personSkillId") UUID personSkillId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PersonSkillUpdate personSkillUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PERSON_SOCIAL_LINK = "/v1/people/persons/{personId}/social-links/{personSocialLinkId}";
    /**
     * PATCH /v1/people/persons/{personId}/social-links/{personSocialLinkId} : Update PersonSocialLink
     * Partially update a PersonSocialLink (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param personSocialLinkId PersonSocialLink id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param personSocialLinkUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_PERSON_SOCIAL_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PersonSocialLink> updatePersonSocialLink(
         @PathVariable("personId") UUID personId,
         @PathVariable("personSocialLinkId") UUID personSocialLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PersonSocialLinkUpdate personSocialLinkUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PUBLIC_PROFILE = "/v1/people/persons/{personId}/public-profile/{publicProfileId}";
    /**
     * PATCH /v1/people/persons/{personId}/public-profile/{publicProfileId} : Update PublicProfile
     * Partially update a PublicProfile (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param publicProfileId PublicProfile id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param publicProfileUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_PUBLIC_PROFILE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PublicProfile> updatePublicProfile(
         @PathVariable("personId") UUID personId,
         @PathVariable("publicProfileId") UUID publicProfileId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PublicProfileUpdate publicProfileUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_WORK_EXPERIENCE = "/v1/people/persons/{personId}/work-experience/{workExperienceId}";
    /**
     * PATCH /v1/people/persons/{personId}/work-experience/{workExperienceId} : Update WorkExperience
     * Partially update a WorkExperience (updateMask). Requires If-Match with the current ETag.
     *
     * @param personId Person id. (required)
     * @param workExperienceId WorkExperience id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param workExperienceUpdate  (required)
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
        value = PersonApi.PATH_UPDATE_WORK_EXPERIENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<WorkExperience> updateWorkExperience(
         @PathVariable("personId") UUID personId,
         @PathVariable("workExperienceId") UUID workExperienceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody WorkExperienceUpdate workExperienceUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_WITHDRAW_CONSENT = "/v1/people/persons/{personId}/consents/{consentRecordId}:withdraw";
    /**
     * POST /v1/people/persons/{personId}/consents/{consentRecordId}:withdraw : Withdraw a consent (sets withdrawnAt)
     * Withdraw a consent (sets withdrawnAt). Publishing that depends on it stops immediately, and already published media for that purpose is unpublished by a background job.
     *
     * @param personId  (required)
     * @param consentRecordId  (required)
     * @param withdrawConsentRequest  (required)
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
        value = PersonApi.PATH_WITHDRAW_CONSENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ConsentRecord> withdrawConsent(
         @PathVariable("personId") UUID personId,
         @PathVariable("consentRecordId") UUID consentRecordId,
         @Valid @RequestBody WithdrawConsentRequest withdrawConsentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
