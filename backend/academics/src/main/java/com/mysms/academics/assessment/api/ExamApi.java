// Owned API contract of assessment.
package com.mysms.academics.assessment.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.api.model.AllocateExamRoomsRequest;
import com.mysms.academics.assessment.api.model.CancelExamRequest;
import com.mysms.academics.assessment.api.model.CompleteExamRequest;
import com.mysms.academics.assessment.api.model.Exam;
import com.mysms.academics.assessment.api.model.ExamInvigilator;
import com.mysms.academics.assessment.api.model.ExamInvigilatorUpdate;
import com.mysms.academics.assessment.api.model.ExamResultList;
import com.mysms.academics.assessment.api.model.ExamUpdate;
import com.mysms.academics.assessment.api.model.MarkExamHeldRequest;
import com.mysms.academics.assessment.api.model.MarkExamResultsUpdatedRequest;
import org.springframework.lang.Nullable;
import com.mysms.academics.assessment.api.model.RecordExamMarksRequest;
import com.mysms.academics.assessment.api.model.ResetExamRequest;
import com.mysms.academics.assessment.api.model.ScheduleExamRequest;
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
public interface ExamApi {

    String PATH_ALLOCATE_EXAM_ROOMS = "/v1/assessment/exams/{examId}:allocateRooms";
    /**
     * POST /v1/assessment/exams/{examId}:allocateRooms : Distribute attendees across rooms by capacity
     * Distribute attendees across rooms by capacity.
     *
     * @param examId  (required)
     * @param allocateExamRoomsRequest  (required)
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
        value = ExamApi.PATH_ALLOCATE_EXAM_ROOMS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamResultList> allocateExamRooms(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody AllocateExamRoomsRequest allocateExamRoomsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CANCEL_EXAM = "/v1/assessment/exams/{examId}:cancel";
    /**
     * POST /v1/assessment/exams/{examId}:cancel : Cancel
     * Cancel.
     *
     * @param examId  (required)
     * @param cancelExamRequest  (required)
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
        value = ExamApi.PATH_CANCEL_EXAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> cancelExam(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody CancelExamRequest cancelExamRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COMPLETE_EXAM = "/v1/assessment/exams/{examId}:complete";
    /**
     * POST /v1/assessment/exams/{examId}:complete : Done
     * Done.
     *
     * @param examId  (required)
     * @param completeExamRequest  (required)
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
        value = ExamApi.PATH_COMPLETE_EXAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> completeExam(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody CompleteExamRequest completeExamRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_EXAM = "/v1/assessment/exams";
    /**
     * POST /v1/assessment/exams : Create Exam
     * Create a Exam. Supports Idempotency-Key and validateOnly.
     *
     * @param exam  (required)
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
        value = ExamApi.PATH_CREATE_EXAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> createExam(
         @Valid @RequestBody Exam exam,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EXAM_INVIGILATOR = "/v1/assessment/exams/{examId}/invigilators";
    /**
     * POST /v1/assessment/exams/{examId}/invigilators : Create ExamInvigilator
     * Create a ExamInvigilator. Supports Idempotency-Key and validateOnly.
     *
     * @param examId Exam id. (required)
     * @param examInvigilator  (required)
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
        value = ExamApi.PATH_CREATE_EXAM_INVIGILATOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamInvigilator> createExamInvigilator(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody ExamInvigilator examInvigilator,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_EXAM = "/v1/assessment/exams/{examId}";
    /**
     * DELETE /v1/assessment/exams/{examId} : Delete Exam
     * Soft delete (sets deleted_at).
     *
     * @param examId Exam id. (required)
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
        value = ExamApi.PATH_DELETE_EXAM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExam(
         @PathVariable("examId") UUID examId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EXAM_INVIGILATOR = "/v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}";
    /**
     * DELETE /v1/assessment/exams/{examId}/invigilators/{examInvigilatorId} : Delete ExamInvigilator
     * Soft delete (sets deleted_at).
     *
     * @param examId Exam id. (required)
     * @param examInvigilatorId ExamInvigilator id. (required)
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
        value = ExamApi.PATH_DELETE_EXAM_INVIGILATOR,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExamInvigilator(
         @PathVariable("examId") UUID examId,
         @PathVariable("examInvigilatorId") UUID examInvigilatorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_EXAM = "/v1/assessment/exams/{examId}";
    /**
     * GET /v1/assessment/exams/{examId} : Get Exam
     * Get one Exam by id. The ETag header carries the row version.
     *
     * @param examId Exam id. (required)
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
        value = ExamApi.PATH_GET_EXAM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Exam> getExam(
         @PathVariable("examId") UUID examId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EXAM_INVIGILATOR = "/v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}";
    /**
     * GET /v1/assessment/exams/{examId}/invigilators/{examInvigilatorId} : Get ExamInvigilator
     * Get one ExamInvigilator by id. The ETag header carries the row version.
     *
     * @param examId Exam id. (required)
     * @param examInvigilatorId ExamInvigilator id. (required)
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
        value = ExamApi.PATH_GET_EXAM_INVIGILATOR,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExamInvigilator> getExamInvigilator(
         @PathVariable("examId") UUID examId,
         @PathVariable("examInvigilatorId") UUID examInvigilatorId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXAM_INVIGILATORS = "/v1/assessment/exams/{examId}/invigilators";
    /**
     * GET /v1/assessment/exams/{examId}/invigilators : List ExamInvigilators
     * List ExamInvigilators visible to the caller, page by page, with filtering and sorting.
     *
     * @param examId Exam id. (required)
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
        value = ExamApi.PATH_LIST_EXAM_INVIGILATORS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExamInvigilator>> listExamInvigilators(
         @PathVariable("examId") UUID examId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXAMS = "/v1/assessment/exams";
    /**
     * GET /v1/assessment/exams : List Exams
     * List Exams visible to the caller, page by page, with filtering and sorting.
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
        value = ExamApi.PATH_LIST_EXAMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Exam>> listExams(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_EXAM_HELD = "/v1/assessment/exams/{examId}:markHeld";
    /**
     * POST /v1/assessment/exams/{examId}:markHeld : Mark held; attendees are registered
     * Mark held; attendees are registered.
     *
     * @param examId  (required)
     * @param markExamHeldRequest  (required)
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
        value = ExamApi.PATH_MARK_EXAM_HELD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> markExamHeld(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody MarkExamHeldRequest markExamHeldRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_MARK_EXAM_RESULTS_UPDATED = "/v1/assessment/exams/{examId}:markResultsUpdated";
    /**
     * POST /v1/assessment/exams/{examId}:markResultsUpdated : All marks entered
     * All marks entered.
     *
     * @param examId  (required)
     * @param markExamResultsUpdatedRequest  (required)
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
        value = ExamApi.PATH_MARK_EXAM_RESULTS_UPDATED,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> markExamResultsUpdated(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody MarkExamResultsUpdatedRequest markExamResultsUpdatedRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_POPULATE_EXAM_RESULTS = "/v1/assessment/exams/{examId}:populateResults";
    /**
     * POST /v1/assessment/exams/{examId}:populateResults : Create attendee rows from the students of the class / batch
     * Create attendee rows from the students of the class / batch.
     *
     * @param examId  (required)
     * @param body  (required)
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
        value = ExamApi.PATH_POPULATE_EXAM_RESULTS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamResultList> populateExamResults(
         @PathVariable("examId") UUID examId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RECORD_EXAM_MARKS = "/v1/assessment/exams/{examId}:recordMarks";
    /**
     * POST /v1/assessment/exams/{examId}:recordMarks : Enter or correct marks for many students
     * Enter or correct marks for many students.
     *
     * @param examId  (required)
     * @param recordExamMarksRequest  (required)
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
        value = ExamApi.PATH_RECORD_EXAM_MARKS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamResultList> recordExamMarks(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody RecordExamMarksRequest recordExamMarksRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_EXAM = "/v1/assessment/exams/{examId}:resetToDraft";
    /**
     * POST /v1/assessment/exams/{examId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param examId  (required)
     * @param resetExamRequest  (required)
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
        value = ExamApi.PATH_RESET_EXAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> resetExam(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody ResetExamRequest resetExamRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SCHEDULE_EXAM = "/v1/assessment/exams/{examId}:schedule";
    /**
     * POST /v1/assessment/exams/{examId}:schedule : Schedule
     * Schedule.
     *
     * @param examId  (required)
     * @param scheduleExamRequest  (required)
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
        value = ExamApi.PATH_SCHEDULE_EXAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> scheduleExam(
         @PathVariable("examId") UUID examId,
         @Valid @RequestBody ScheduleExamRequest scheduleExamRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_EXAM = "/v1/assessment/exams/{examId}";
    /**
     * PATCH /v1/assessment/exams/{examId} : Update Exam
     * Partially update a Exam (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param examId Exam id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param examUpdate  (required)
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
        value = ExamApi.PATH_UPDATE_EXAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Exam> updateExam(
         @PathVariable("examId") UUID examId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExamUpdate examUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EXAM_INVIGILATOR = "/v1/assessment/exams/{examId}/invigilators/{examInvigilatorId}";
    /**
     * PATCH /v1/assessment/exams/{examId}/invigilators/{examInvigilatorId} : Update ExamInvigilator
     * Partially update a ExamInvigilator (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param examId Exam id. (required)
     * @param examInvigilatorId ExamInvigilator id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param examInvigilatorUpdate  (required)
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
        value = ExamApi.PATH_UPDATE_EXAM_INVIGILATOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamInvigilator> updateExamInvigilator(
         @PathVariable("examId") UUID examId,
         @PathVariable("examInvigilatorId") UUID examInvigilatorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExamInvigilatorUpdate examInvigilatorUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
