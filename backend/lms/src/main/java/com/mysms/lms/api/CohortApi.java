// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.lms.api.model.TimetableEntry;
import com.mysms.lms.api.model.RosterEntry;
import com.mysms.lms.api.model.CohortStudentProgress;
import com.mysms.lms.api.model.CohortPerformance;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ActivateCohortRequest;
import com.mysms.lms.api.model.ApplyTimetableTemplateRequest;
import com.mysms.lms.api.model.ArchiveCohortRequest;
import com.mysms.lms.api.model.BatchEnrollInCohortRequest;
import com.mysms.lms.api.model.BatchEnrollPreviewList;
import com.mysms.lms.api.model.ChapterList;
import com.mysms.lms.api.model.Cohort;
import com.mysms.lms.api.model.CohortActivity;
import com.mysms.lms.api.model.CohortActivityUpdate;
import com.mysms.lms.api.model.CohortCourse;
import com.mysms.lms.api.model.CohortCourseUpdate;
import com.mysms.lms.api.model.CohortEnrollment;
import com.mysms.lms.api.model.CohortEnrollmentList;
import com.mysms.lms.api.model.CohortFeedback;
import com.mysms.lms.api.model.CohortFeedbackUpdate;
import com.mysms.lms.api.model.CohortGroup;
import com.mysms.lms.api.model.CohortGroupUpdate;
import com.mysms.lms.api.model.CohortHoliday;
import com.mysms.lms.api.model.CohortHolidayUpdate;
import com.mysms.lms.api.model.CohortScheduleSlot;
import com.mysms.lms.api.model.CohortScheduleSlotUpdate;
import com.mysms.lms.api.model.CohortSession;
import com.mysms.lms.api.model.CohortSessionUpdate;
import com.mysms.lms.api.model.CohortTeacher;
import com.mysms.lms.api.model.CohortTeacherUpdate;
import com.mysms.lms.api.model.CohortUpdate;
import com.mysms.lms.api.model.CopyCourseOutlineToCohortRequest;
import com.mysms.lms.api.model.DeactivateCohortRequest;
import com.mysms.lms.api.model.DownloadLink;
import com.mysms.lms.api.model.EnrollInCohortRequest;
import com.mysms.lms.api.model.EvaluateReleaseRulesRequest;
import com.mysms.lms.api.model.ExportGradesRequest;
import com.mysms.lms.api.model.Forum;
import com.mysms.lms.api.model.ForumUpdate;
import com.mysms.lms.api.model.GenerateCohortCertificatesRequest;
import com.mysms.lms.api.model.Gradebook;
import com.mysms.lms.api.model.ImportCohortMembersRequest;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.api.model.PlagiarismCohortLink;
import com.mysms.lms.api.model.PlagiarismCohortLinkUpdate;
import com.mysms.lms.api.model.PublishCohortRequest;
import com.mysms.lms.api.model.PublishFinalGradesRequest;
import com.mysms.lms.api.model.ReleaseGrantList;
import com.mysms.lms.api.model.RosterEntryList;
import com.mysms.lms.api.model.SendCohortAnnouncementRequest;
import com.mysms.lms.api.model.SendResult;
import com.mysms.lms.api.model.SetCohortMembersRequest;
import com.mysms.lms.api.model.SetStudentAccessRequest;
import com.mysms.lms.api.model.TimetableEntryList;
import java.util.UUID;
import com.mysms.lms.api.model.UnpublishCohortRequest;
import com.mysms.lms.api.model.UnreadCounts;
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
public interface CohortApi {

    String PATH_ACTIVATE_COHORT = "/v1/lms/cohorts/{cohortId}:activate";
    /**
     * POST /v1/lms/cohorts/{cohortId}:activate : Open the cohort to its members
     * Open the cohort to its members.
     *
     * @param cohortId  (required)
     * @param activateCohortRequest  (required)
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
        value = CohortApi.PATH_ACTIVATE_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> activateCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody ActivateCohortRequest activateCohortRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_APPLY_TIMETABLE_TEMPLATE = "/v1/lms/cohorts/{cohortId}:applyTimetableTemplate";
    /**
     * POST /v1/lms/cohorts/{cohortId}:applyTimetableTemplate : Materialise a template into dated entries
     * Materialise a template into dated entries.
     *
     * @param cohortId  (required)
     * @param applyTimetableTemplateRequest  (required)
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
        value = CohortApi.PATH_APPLY_TIMETABLE_TEMPLATE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<TimetableEntryList> applyTimetableTemplate(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody ApplyTimetableTemplateRequest applyTimetableTemplateRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_ARCHIVE_COHORT = "/v1/lms/cohorts/{cohortId}:archive";
    /**
     * POST /v1/lms/cohorts/{cohortId}:archive : Archive a finished cohort (read-only; archive package kept)
     * Archive a finished cohort (read-only; archive package kept).
     *
     * @param cohortId  (required)
     * @param archiveCohortRequest  (required)
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
        value = CohortApi.PATH_ARCHIVE_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> archiveCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody ArchiveCohortRequest archiveCohortRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_BATCH_ENROLL_IN_COHORT = "/v1/lms/cohorts/{cohortId}:batchEnroll";
    /**
     * POST /v1/lms/cohorts/{cohortId}:batchEnroll : Enrol many students
     * Enrol many students.
     *
     * @param cohortId  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param batchEnrollInCohortRequest  (required)
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
        value = CohortApi.PATH_BATCH_ENROLL_IN_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<BatchEnrollPreviewList> batchEnrollInCohort(
         @PathVariable("cohortId") UUID cohortId,
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody BatchEnrollInCohortRequest batchEnrollInCohortRequest
    );


    String PATH_COPY_COURSE_OUTLINE_TO_COHORT = "/v1/lms/cohorts/{cohortId}:copyCourseOutline";
    /**
     * POST /v1/lms/cohorts/{cohortId}:copyCourseOutline : Copy the course template (chapters, items, objectives, grading categories, release rules) into the cohort, converting day offsets into dates
     * Copy the course template (chapters, items, objectives, grading categories, release rules) into the cohort, converting day offsets into dates.
     *
     * @param cohortId  (required)
     * @param copyCourseOutlineToCohortRequest  (required)
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
        value = CohortApi.PATH_COPY_COURSE_OUTLINE_TO_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ChapterList> copyCourseOutlineToCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CopyCourseOutlineToCohortRequest copyCourseOutlineToCohortRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_COHORT = "/v1/lms/cohorts";
    /**
     * POST /v1/lms/cohorts : Create Cohort
     * Create a Cohort. Supports Idempotency-Key and validateOnly.
     *
     * @param cohort  (required)
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
        value = CohortApi.PATH_CREATE_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> createCohort(
         @Valid @RequestBody Cohort cohort,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_ACTIVITY = "/v1/lms/cohorts/{cohortId}/activities";
    /**
     * POST /v1/lms/cohorts/{cohortId}/activities : Create CohortActivity
     * Create a CohortActivity. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortActivity  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_ACTIVITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortActivity> createCohortActivity(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortActivity cohortActivity,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_COURSE = "/v1/lms/cohorts/{cohortId}/courses";
    /**
     * POST /v1/lms/cohorts/{cohortId}/courses : Create CohortCourse
     * Create a CohortCourse. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortCourse  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortCourse> createCohortCourse(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortCourse cohortCourse,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_FEEDBACK = "/v1/lms/cohorts/{cohortId}/feedback";
    /**
     * POST /v1/lms/cohorts/{cohortId}/feedback : Create CohortFeedback
     * Create a CohortFeedback. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortFeedback  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_FEEDBACK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortFeedback> createCohortFeedback(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortFeedback cohortFeedback,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_GROUP = "/v1/lms/cohorts/{cohortId}/groups";
    /**
     * POST /v1/lms/cohorts/{cohortId}/groups : Create CohortGroup
     * Create a CohortGroup. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortGroup  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_GROUP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortGroup> createCohortGroup(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortGroup cohortGroup,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_HOLIDAY = "/v1/lms/cohorts/{cohortId}/holidays";
    /**
     * POST /v1/lms/cohorts/{cohortId}/holidays : Create CohortHoliday
     * Create a CohortHoliday. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortHoliday  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_HOLIDAY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortHoliday> createCohortHoliday(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortHoliday cohortHoliday,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_SCHEDULE_SLOT = "/v1/lms/cohorts/{cohortId}/schedule-slots";
    /**
     * POST /v1/lms/cohorts/{cohortId}/schedule-slots : Create CohortScheduleSlot
     * Create a CohortScheduleSlot. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortScheduleSlot  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_SCHEDULE_SLOT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortScheduleSlot> createCohortScheduleSlot(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortScheduleSlot cohortScheduleSlot,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_SESSION = "/v1/lms/cohorts/{cohortId}/sessions";
    /**
     * POST /v1/lms/cohorts/{cohortId}/sessions : Create CohortSession
     * Create a CohortSession. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortSession  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortSession> createCohortSession(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortSession cohortSession,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COHORT_TEACHER = "/v1/lms/cohorts/{cohortId}/teachers";
    /**
     * POST /v1/lms/cohorts/{cohortId}/teachers : Create CohortTeacher
     * Create a CohortTeacher. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortTeacher  (required)
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
        value = CohortApi.PATH_CREATE_COHORT_TEACHER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortTeacher> createCohortTeacher(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody CohortTeacher cohortTeacher,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_FORUM = "/v1/lms/cohorts/{cohortId}/forums";
    /**
     * POST /v1/lms/cohorts/{cohortId}/forums : Create Forum
     * Create a Forum. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param forum  (required)
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
        value = CohortApi.PATH_CREATE_FORUM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Forum> createForum(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody Forum forum,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PLAGIARISM_COHORT_LINK = "/v1/lms/cohorts/{cohortId}/plagiarism-links";
    /**
     * POST /v1/lms/cohorts/{cohortId}/plagiarism-links : Create PlagiarismCohortLink
     * Create a PlagiarismCohortLink. Supports Idempotency-Key and validateOnly.
     *
     * @param cohortId Cohort id. (required)
     * @param plagiarismCohortLink  (required)
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
        value = CohortApi.PATH_CREATE_PLAGIARISM_COHORT_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PlagiarismCohortLink> createPlagiarismCohortLink(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody PlagiarismCohortLink plagiarismCohortLink,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DEACTIVATE_COHORT = "/v1/lms/cohorts/{cohortId}:deactivate";
    /**
     * POST /v1/lms/cohorts/{cohortId}:deactivate : Suspend the cohort
     * Suspend the cohort.
     *
     * @param cohortId  (required)
     * @param deactivateCohortRequest  (required)
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
        value = CohortApi.PATH_DEACTIVATE_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> deactivateCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody DeactivateCohortRequest deactivateCohortRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_COHORT = "/v1/lms/cohorts/{cohortId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId} : Delete Cohort
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohort(
         @PathVariable("cohortId") UUID cohortId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_ACTIVITY = "/v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/activities/{cohortActivityId} : Delete CohortActivity
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortActivityId CohortActivity id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_ACTIVITY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortActivity(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortActivityId") UUID cohortActivityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_COURSE = "/v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/courses/{cohortCourseId} : Delete CohortCourse
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortCourseId CohortCourse id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_COURSE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortCourse(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortCourseId") UUID cohortCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_FEEDBACK = "/v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId} : Delete CohortFeedback
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortFeedbackId CohortFeedback id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_FEEDBACK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortFeedback(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortFeedbackId") UUID cohortFeedbackId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_GROUP = "/v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/groups/{cohortGroupId} : Delete CohortGroup
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortGroupId CohortGroup id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_GROUP,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortGroup(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortGroupId") UUID cohortGroupId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_HOLIDAY = "/v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId} : Delete CohortHoliday
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortHolidayId CohortHoliday id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_HOLIDAY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortHoliday(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortHolidayId") UUID cohortHolidayId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_SCHEDULE_SLOT = "/v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId} : Delete CohortScheduleSlot
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortScheduleSlotId CohortScheduleSlot id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_SCHEDULE_SLOT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortScheduleSlot(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortScheduleSlotId") UUID cohortScheduleSlotId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_SESSION = "/v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId} : Delete CohortSession
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortSessionId CohortSession id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_SESSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortSession(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortSessionId") UUID cohortSessionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COHORT_TEACHER = "/v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId} : Delete CohortTeacher
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param cohortTeacherId CohortTeacher id. (required)
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
        value = CohortApi.PATH_DELETE_COHORT_TEACHER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCohortTeacher(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortTeacherId") UUID cohortTeacherId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_FORUM = "/v1/lms/cohorts/{cohortId}/forums/{forumId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/forums/{forumId} : Delete Forum
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param forumId Forum id. (required)
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
        value = CohortApi.PATH_DELETE_FORUM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteForum(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("forumId") UUID forumId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PLAGIARISM_COHORT_LINK = "/v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}";
    /**
     * DELETE /v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId} : Delete PlagiarismCohortLink
     * Soft delete (sets deleted_at).
     *
     * @param cohortId Cohort id. (required)
     * @param plagiarismCohortLinkId PlagiarismCohortLink id. (required)
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
        value = CohortApi.PATH_DELETE_PLAGIARISM_COHORT_LINK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePlagiarismCohortLink(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("plagiarismCohortLinkId") UUID plagiarismCohortLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_ENROLL_IN_COHORT = "/v1/lms/cohorts/{cohortId}:enroll";
    /**
     * POST /v1/lms/cohorts/{cohortId}:enroll : Self-enrol
     * Self-enrol.
     *
     * @param cohortId  (required)
     * @param enrollInCohortRequest  (required)
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
        value = CohortApi.PATH_ENROLL_IN_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortEnrollment> enrollInCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody EnrollInCohortRequest enrollInCohortRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_EVALUATE_RELEASE_RULES = "/v1/lms/cohorts/{cohortId}:evaluateReleaseRules";
    /**
     * POST /v1/lms/cohorts/{cohortId}:evaluateReleaseRules : Re-evaluate conditional-release rules (normally automatic after each grade)
     * Re-evaluate conditional-release rules (normally automatic after each grade).
     *
     * @param cohortId  (required)
     * @param evaluateReleaseRulesRequest  (required)
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
        value = CohortApi.PATH_EVALUATE_RELEASE_RULES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseGrantList> evaluateReleaseRules(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody EvaluateReleaseRulesRequest evaluateReleaseRulesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_EXPORT_GRADES = "/v1/lms/cohorts/{cohortId}:exportGrades";
    /**
     * POST /v1/lms/cohorts/{cohortId}:exportGrades : Export grades (whole gradebook, or one assignment / quiz / discussion / participation item)
     * Export grades (whole gradebook, or one assignment / quiz / discussion / participation item).
     *
     * @param cohortId  (required)
     * @param exportGradesRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Accepted; poll the operation. (status code 202)
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
        value = CohortApi.PATH_EXPORT_GRADES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> exportGrades(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody ExportGradesRequest exportGradesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GENERATE_COHORT_CERTIFICATES = "/v1/lms/cohorts/{cohortId}:generateCertificates";
    /**
     * POST /v1/lms/cohorts/{cohortId}:generateCertificates : Issue certificates to completing students
     * Issue certificates to completing students.
     *
     * @param cohortId  (required)
     * @param generateCohortCertificatesRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Accepted; poll the operation. (status code 202)
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
        value = CohortApi.PATH_GENERATE_COHORT_CERTIFICATES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> generateCohortCertificates(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody GenerateCohortCertificatesRequest generateCohortCertificatesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_COHORT = "/v1/lms/cohorts/{cohortId}";
    /**
     * GET /v1/lms/cohorts/{cohortId} : Get Cohort
     * Get one Cohort by id. The ETag header carries the row version. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_GET_COHORT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Cohort> getCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_ACTIVITY = "/v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/activities/{cohortActivityId} : Get CohortActivity
     * Get one CohortActivity by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortActivityId CohortActivity id. (required)
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
        value = CohortApi.PATH_GET_COHORT_ACTIVITY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortActivity> getCohortActivity(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortActivityId") UUID cohortActivityId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_COURSE = "/v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/courses/{cohortCourseId} : Get CohortCourse
     * Get one CohortCourse by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortCourseId CohortCourse id. (required)
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
        value = CohortApi.PATH_GET_COHORT_COURSE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortCourse> getCohortCourse(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortCourseId") UUID cohortCourseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_FEEDBACK = "/v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId} : Get CohortFeedback
     * Get one CohortFeedback by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortFeedbackId CohortFeedback id. (required)
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
        value = CohortApi.PATH_GET_COHORT_FEEDBACK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortFeedback> getCohortFeedback(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortFeedbackId") UUID cohortFeedbackId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_GROUP = "/v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/groups/{cohortGroupId} : Get CohortGroup
     * Get one CohortGroup by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortGroupId CohortGroup id. (required)
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
        value = CohortApi.PATH_GET_COHORT_GROUP,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortGroup> getCohortGroup(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortGroupId") UUID cohortGroupId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_HOLIDAY = "/v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId} : Get CohortHoliday
     * Get one CohortHoliday by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortHolidayId CohortHoliday id. (required)
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
        value = CohortApi.PATH_GET_COHORT_HOLIDAY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortHoliday> getCohortHoliday(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortHolidayId") UUID cohortHolidayId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_PERFORMANCE = "/v1/lms/cohorts/{cohortId}:performance";
    /**
     * GET /v1/lms/cohorts/{cohortId}:performance : Per-item statistics (average, median, missing)
     * Per-item statistics (average, median, missing).
     *
     * @param cohortId  (required)
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
        value = CohortApi.PATH_GET_COHORT_PERFORMANCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortPerformance>> getCohortPerformance(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_COHORT_PROGRESS = "/v1/lms/cohorts/{cohortId}:progress";
    /**
     * GET /v1/lms/cohorts/{cohortId}:progress : Student progress across courses and assessments
     * Student progress across courses and assessments.
     *
     * @param cohortId  (required)
     * @param userId One student. (optional)
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
        value = CohortApi.PATH_GET_COHORT_PROGRESS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortStudentProgress>> getCohortProgress(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestParam(value = "userId", required = false) @Nullable UUID userId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_COHORT_ROSTER = "/v1/lms/cohorts/{cohortId}:roster";
    /**
     * GET /v1/lms/cohorts/{cohortId}:roster : Students and staff of a cohort
     * Students and staff of a cohort.
     *
     * @param cohortId  (required)
     * @param roleCode Filter by role. (optional)
     * @param includeInactive Include removed members. (optional)
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
        value = CohortApi.PATH_GET_COHORT_ROSTER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RosterEntry>> getCohortRoster(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestParam(value = "roleCode", required = false) @Nullable String roleCode,
         @Valid @RequestParam(value = "includeInactive", required = false) @Nullable Boolean includeInactive,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_COHORT_SCHEDULE_SLOT = "/v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId} : Get CohortScheduleSlot
     * Get one CohortScheduleSlot by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortScheduleSlotId CohortScheduleSlot id. (required)
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
        value = CohortApi.PATH_GET_COHORT_SCHEDULE_SLOT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortScheduleSlot> getCohortScheduleSlot(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortScheduleSlotId") UUID cohortScheduleSlotId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_SESSION = "/v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId} : Get CohortSession
     * Get one CohortSession by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortSessionId CohortSession id. (required)
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
        value = CohortApi.PATH_GET_COHORT_SESSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortSession> getCohortSession(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortSessionId") UUID cohortSessionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_SYLLABUS = "/v1/lms/cohorts/{cohortId}:syllabus";
    /**
     * GET /v1/lms/cohorts/{cohortId}:syllabus : Syllabus of the cohort with its dates
     * Syllabus of the cohort with its dates.
     *
     * @param cohortId  (required)
     * @param format Download format. (optional)
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
        value = CohortApi.PATH_GET_COHORT_SYLLABUS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DownloadLink> getCohortSyllabus(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestParam(value = "format", required = false) @Nullable String format
    );


    String PATH_GET_COHORT_TEACHER = "/v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId} : Get CohortTeacher
     * Get one CohortTeacher by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortTeacherId CohortTeacher id. (required)
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
        value = CohortApi.PATH_GET_COHORT_TEACHER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CohortTeacher> getCohortTeacher(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortTeacherId") UUID cohortTeacherId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COHORT_TIMETABLE = "/v1/lms/cohorts/{cohortId}:timetable";
    /**
     * GET /v1/lms/cohorts/{cohortId}:timetable : Timetable with legend
     * Timetable with legend.
     *
     * @param cohortId  (required)
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
        value = CohortApi.PATH_GET_COHORT_TIMETABLE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<TimetableEntry>> getCohortTimetable(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_FORUM = "/v1/lms/cohorts/{cohortId}/forums/{forumId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/forums/{forumId} : Get Forum
     * Get one Forum by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param forumId Forum id. (required)
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
        value = CohortApi.PATH_GET_FORUM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Forum> getForum(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("forumId") UUID forumId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_GRADEBOOK = "/v1/lms/cohorts/{cohortId}:gradebook";
    /**
     * GET /v1/lms/cohorts/{cohortId}:gradebook : Gradebook grid: graded items × students with category and weighted totals
     * Gradebook grid: graded items × students with category and weighted totals.
     *
     * @param cohortId  (required)
     * @param userId One student (students always get only their own row). (optional)
     * @param gradingCategoryId Only one category. (optional)
     * @param page One-based page of students (default 1). (optional, default to 1)
     * @param size Students per page (1-200, default 50). (optional, default to 50)
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
        value = CohortApi.PATH_GET_GRADEBOOK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Gradebook> getGradebook(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestParam(value = "userId", required = false) @Nullable UUID userId,
         @Valid @RequestParam(value = "gradingCategoryId", required = false) @Nullable UUID gradingCategoryId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 200)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_PLAGIARISM_COHORT_LINK = "/v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}";
    /**
     * GET /v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId} : Get PlagiarismCohortLink
     * Get one PlagiarismCohortLink by id. The ETag header carries the row version.
     *
     * @param cohortId Cohort id. (required)
     * @param plagiarismCohortLinkId PlagiarismCohortLink id. (required)
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
        value = CohortApi.PATH_GET_PLAGIARISM_COHORT_LINK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PlagiarismCohortLink> getPlagiarismCohortLink(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("plagiarismCohortLinkId") UUID plagiarismCohortLinkId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_UNREAD_COUNTS = "/v1/lms/me/unread-counts";
    /**
     * GET /v1/lms/me/unread-counts : Unread posts, notifications and mail for badges in the apps
     * Unread posts, notifications and mail for badges in the apps.
     *
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
        value = CohortApi.PATH_GET_UNREAD_COUNTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UnreadCounts> getUnreadCounts(
        
    );


    String PATH_IMPORT_COHORT_MEMBERS = "/v1/lms/cohorts/{cohortId}:importMembers";
    /**
     * POST /v1/lms/cohorts/{cohortId}:importMembers : Bulk-add members from a file (background operation with row errors)
     * Bulk-add members from a file (background operation with row errors).
     *
     * @param cohortId  (required)
     * @param importCohortMembersRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Accepted; poll the operation. (status code 202)
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
        value = CohortApi.PATH_IMPORT_COHORT_MEMBERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> importCohortMembers(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody ImportCohortMembersRequest importCohortMembersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_COHORT_ACTIVITIES = "/v1/lms/cohorts/{cohortId}/activities";
    /**
     * GET /v1/lms/cohorts/{cohortId}/activities : List CohortActivities
     * List CohortActivities visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_ACTIVITIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortActivity>> listCohortActivities(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORT_COURSES = "/v1/lms/cohorts/{cohortId}/courses";
    /**
     * GET /v1/lms/cohorts/{cohortId}/courses : List CohortCourses
     * List CohortCourses visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortCourse>> listCohortCourses(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORT_FEEDBACK = "/v1/lms/cohorts/{cohortId}/feedback";
    /**
     * GET /v1/lms/cohorts/{cohortId}/feedback : List CohortFeedback
     * List CohortFeedback visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_FEEDBACK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortFeedback>> listCohortFeedback(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORT_GROUPS = "/v1/lms/cohorts/{cohortId}/groups";
    /**
     * GET /v1/lms/cohorts/{cohortId}/groups : List CohortGroups
     * List CohortGroups visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_GROUPS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortGroup>> listCohortGroups(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORT_HOLIDAYS = "/v1/lms/cohorts/{cohortId}/holidays";
    /**
     * GET /v1/lms/cohorts/{cohortId}/holidays : List CohortHolidays
     * List CohortHolidays visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_HOLIDAYS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortHoliday>> listCohortHolidays(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORT_SCHEDULE_SLOTS = "/v1/lms/cohorts/{cohortId}/schedule-slots";
    /**
     * GET /v1/lms/cohorts/{cohortId}/schedule-slots : List CohortScheduleSlots
     * List CohortScheduleSlots visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_SCHEDULE_SLOTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortScheduleSlot>> listCohortScheduleSlots(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORT_SESSIONS = "/v1/lms/cohorts/{cohortId}/sessions";
    /**
     * GET /v1/lms/cohorts/{cohortId}/sessions : List CohortSessions
     * List CohortSessions visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_SESSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortSession>> listCohortSessions(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORT_TEACHERS = "/v1/lms/cohorts/{cohortId}/teachers";
    /**
     * GET /v1/lms/cohorts/{cohortId}/teachers : List CohortTeachers
     * List CohortTeachers visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_COHORT_TEACHERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CohortTeacher>> listCohortTeachers(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COHORTS = "/v1/lms/cohorts";
    /**
     * GET /v1/lms/cohorts : List Cohorts
     * List Cohorts visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
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
        value = CohortApi.PATH_LIST_COHORTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Cohort>> listCohorts(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FORUMS = "/v1/lms/cohorts/{cohortId}/forums";
    /**
     * GET /v1/lms/cohorts/{cohortId}/forums : List Forums
     * List Forums visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_FORUMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Forum>> listForums(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PLAGIARISM_COHORT_LINKS = "/v1/lms/cohorts/{cohortId}/plagiarism-links";
    /**
     * GET /v1/lms/cohorts/{cohortId}/plagiarism-links : List PlagiarismCohortLinks
     * List PlagiarismCohortLinks visible to the caller, page by page, with filtering and sorting.
     *
     * @param cohortId Cohort id. (required)
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
        value = CohortApi.PATH_LIST_PLAGIARISM_COHORT_LINKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PlagiarismCohortLink>> listPlagiarismCohortLinks(
         @PathVariable("cohortId") UUID cohortId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_COHORT = "/v1/lms/cohorts/{cohortId}:publish";
    /**
     * POST /v1/lms/cohorts/{cohortId}:publish : Publish
     * Publish.
     *
     * @param cohortId  (required)
     * @param publishCohortRequest  (required)
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
        value = CohortApi.PATH_PUBLISH_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> publishCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody PublishCohortRequest publishCohortRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_PUBLISH_FINAL_GRADES = "/v1/lms/cohorts/{cohortId}:publishFinalGrades";
    /**
     * POST /v1/lms/cohorts/{cohortId}:publishFinalGrades : Compute (from the gradebook and the cohort grading scheme) and publish final grades
     * Compute (from the gradebook and the cohort grading scheme) and publish final grades.
     *
     * @param cohortId  (required)
     * @param publishFinalGradesRequest  (required)
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
        value = CohortApi.PATH_PUBLISH_FINAL_GRADES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortEnrollmentList> publishFinalGrades(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody PublishFinalGradesRequest publishFinalGradesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SEND_COHORT_ANNOUNCEMENT = "/v1/lms/cohorts/{cohortId}:sendAnnouncement";
    /**
     * POST /v1/lms/cohorts/{cohortId}:sendAnnouncement : E-mail all students
     * E-mail all students.
     *
     * @param cohortId  (required)
     * @param sendCohortAnnouncementRequest  (required)
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
        value = CohortApi.PATH_SEND_COHORT_ANNOUNCEMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SendResult> sendCohortAnnouncement(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody SendCohortAnnouncementRequest sendCohortAnnouncementRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_COHORT_MEMBERS = "/v1/lms/cohorts/{cohortId}:setMembers";
    /**
     * POST /v1/lms/cohorts/{cohortId}:setMembers : Add, update or remove students and staff in one call
     * Add, update or remove students and staff in one call.
     *
     * @param cohortId  (required)
     * @param setCohortMembersRequest  (required)
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
        value = CohortApi.PATH_SET_COHORT_MEMBERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RosterEntryList> setCohortMembers(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody SetCohortMembersRequest setCohortMembersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_STUDENT_ACCESS = "/v1/lms/cohorts/{cohortId}:setStudentAccess";
    /**
     * POST /v1/lms/cohorts/{cohortId}:setStudentAccess : Change the students&#39; access window (e.g
     * Change the students&#39; access window (e.g. extend after the end date).
     *
     * @param cohortId  (required)
     * @param setStudentAccessRequest  (required)
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
        value = CohortApi.PATH_SET_STUDENT_ACCESS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> setStudentAccess(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody SetStudentAccessRequest setStudentAccessRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UNPUBLISH_COHORT = "/v1/lms/cohorts/{cohortId}:unpublish";
    /**
     * POST /v1/lms/cohorts/{cohortId}:unpublish : Unpublish
     * Unpublish.
     *
     * @param cohortId  (required)
     * @param unpublishCohortRequest  (required)
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
        value = CohortApi.PATH_UNPUBLISH_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> unpublishCohort(
         @PathVariable("cohortId") UUID cohortId,
         @Valid @RequestBody UnpublishCohortRequest unpublishCohortRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_COHORT = "/v1/lms/cohorts/{cohortId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId} : Update Cohort
     * Partially update a Cohort (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Cohort> updateCohort(
         @PathVariable("cohortId") UUID cohortId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortUpdate cohortUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_ACTIVITY = "/v1/lms/cohorts/{cohortId}/activities/{cohortActivityId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/activities/{cohortActivityId} : Update CohortActivity
     * Partially update a CohortActivity (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortActivityId CohortActivity id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortActivityUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_ACTIVITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortActivity> updateCohortActivity(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortActivityId") UUID cohortActivityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortActivityUpdate cohortActivityUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_COURSE = "/v1/lms/cohorts/{cohortId}/courses/{cohortCourseId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/courses/{cohortCourseId} : Update CohortCourse
     * Partially update a CohortCourse (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortCourseId CohortCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortCourseUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortCourse> updateCohortCourse(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortCourseId") UUID cohortCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortCourseUpdate cohortCourseUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_FEEDBACK = "/v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/feedback/{cohortFeedbackId} : Update CohortFeedback
     * Partially update a CohortFeedback (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortFeedbackId CohortFeedback id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortFeedbackUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_FEEDBACK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortFeedback> updateCohortFeedback(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortFeedbackId") UUID cohortFeedbackId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortFeedbackUpdate cohortFeedbackUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_GROUP = "/v1/lms/cohorts/{cohortId}/groups/{cohortGroupId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/groups/{cohortGroupId} : Update CohortGroup
     * Partially update a CohortGroup (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortGroupId CohortGroup id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortGroupUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_GROUP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortGroup> updateCohortGroup(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortGroupId") UUID cohortGroupId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortGroupUpdate cohortGroupUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_HOLIDAY = "/v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/holidays/{cohortHolidayId} : Update CohortHoliday
     * Partially update a CohortHoliday (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortHolidayId CohortHoliday id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortHolidayUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_HOLIDAY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortHoliday> updateCohortHoliday(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortHolidayId") UUID cohortHolidayId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortHolidayUpdate cohortHolidayUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_SCHEDULE_SLOT = "/v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/schedule-slots/{cohortScheduleSlotId} : Update CohortScheduleSlot
     * Partially update a CohortScheduleSlot (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortScheduleSlotId CohortScheduleSlot id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortScheduleSlotUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_SCHEDULE_SLOT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortScheduleSlot> updateCohortScheduleSlot(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortScheduleSlotId") UUID cohortScheduleSlotId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortScheduleSlotUpdate cohortScheduleSlotUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_SESSION = "/v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/sessions/{cohortSessionId} : Update CohortSession
     * Partially update a CohortSession (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortSessionId CohortSession id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortSessionUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortSession> updateCohortSession(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortSessionId") UUID cohortSessionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortSessionUpdate cohortSessionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COHORT_TEACHER = "/v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/teachers/{cohortTeacherId} : Update CohortTeacher
     * Partially update a CohortTeacher (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param cohortTeacherId CohortTeacher id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param cohortTeacherUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_COHORT_TEACHER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CohortTeacher> updateCohortTeacher(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("cohortTeacherId") UUID cohortTeacherId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CohortTeacherUpdate cohortTeacherUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_FORUM = "/v1/lms/cohorts/{cohortId}/forums/{forumId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/forums/{forumId} : Update Forum
     * Partially update a Forum (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param forumId Forum id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param forumUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_FORUM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Forum> updateForum(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("forumId") UUID forumId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ForumUpdate forumUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PLAGIARISM_COHORT_LINK = "/v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId}";
    /**
     * PATCH /v1/lms/cohorts/{cohortId}/plagiarism-links/{plagiarismCohortLinkId} : Update PlagiarismCohortLink
     * Partially update a PlagiarismCohortLink (updateMask). Requires If-Match with the current ETag.
     *
     * @param cohortId Cohort id. (required)
     * @param plagiarismCohortLinkId PlagiarismCohortLink id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param plagiarismCohortLinkUpdate  (required)
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
        value = CohortApi.PATH_UPDATE_PLAGIARISM_COHORT_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PlagiarismCohortLink> updatePlagiarismCohortLink(
         @PathVariable("cohortId") UUID cohortId,
         @PathVariable("plagiarismCohortLinkId") UUID plagiarismCohortLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PlagiarismCohortLinkUpdate plagiarismCohortLinkUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
