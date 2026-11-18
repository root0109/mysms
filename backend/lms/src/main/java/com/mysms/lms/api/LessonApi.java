// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.Lesson;
import com.mysms.lms.api.model.LessonActivity;
import com.mysms.lms.api.model.LessonActivityUpdate;
import com.mysms.lms.api.model.LessonContent;
import com.mysms.lms.api.model.LessonNote;
import com.mysms.lms.api.model.LessonNoteUpdate;
import com.mysms.lms.api.model.LessonProgress;
import com.mysms.lms.api.model.LessonUpdate;
import com.mysms.lms.api.model.MoveLessonRequest;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.TrackVideoWatchRequest;
import java.util.UUID;
import com.mysms.lms.api.model.VideoWatch;
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
public interface LessonApi {

    String PATH_CREATE_LESSON = "/v1/lms/lessons";
    /**
     * POST /v1/lms/lessons : Create Lesson
     * Create a Lesson. Supports Idempotency-Key and validateOnly.
     *
     * @param lesson  (required)
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
        value = LessonApi.PATH_CREATE_LESSON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Lesson> createLesson(
         @Valid @RequestBody Lesson lesson,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_LESSON_ACTIVITY = "/v1/lms/lessons/{lessonId}/activities";
    /**
     * POST /v1/lms/lessons/{lessonId}/activities : Create LessonActivity
     * Create a LessonActivity. Supports Idempotency-Key and validateOnly.
     *
     * @param lessonId Lesson id. (required)
     * @param lessonActivity  (required)
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
        value = LessonApi.PATH_CREATE_LESSON_ACTIVITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonActivity> createLessonActivity(
         @PathVariable("lessonId") UUID lessonId,
         @Valid @RequestBody LessonActivity lessonActivity,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_LESSON_NOTE = "/v1/lms/lessons/{lessonId}/notes";
    /**
     * POST /v1/lms/lessons/{lessonId}/notes : Create LessonNote
     * Create a LessonNote. Supports Idempotency-Key and validateOnly.
     *
     * @param lessonId Lesson id. (required)
     * @param lessonNote  (required)
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
        value = LessonApi.PATH_CREATE_LESSON_NOTE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonNote> createLessonNote(
         @PathVariable("lessonId") UUID lessonId,
         @Valid @RequestBody LessonNote lessonNote,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_LESSON = "/v1/lms/lessons/{lessonId}";
    /**
     * DELETE /v1/lms/lessons/{lessonId} : Delete Lesson
     * Soft delete (sets deleted_at).
     *
     * @param lessonId Lesson id. (required)
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
        value = LessonApi.PATH_DELETE_LESSON,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLesson(
         @PathVariable("lessonId") UUID lessonId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_LESSON_ACTIVITY = "/v1/lms/lessons/{lessonId}/activities/{lessonActivityId}";
    /**
     * DELETE /v1/lms/lessons/{lessonId}/activities/{lessonActivityId} : Delete LessonActivity
     * Soft delete (sets deleted_at).
     *
     * @param lessonId Lesson id. (required)
     * @param lessonActivityId LessonActivity id. (required)
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
        value = LessonApi.PATH_DELETE_LESSON_ACTIVITY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLessonActivity(
         @PathVariable("lessonId") UUID lessonId,
         @PathVariable("lessonActivityId") UUID lessonActivityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_LESSON_NOTE = "/v1/lms/lessons/{lessonId}/notes/{lessonNoteId}";
    /**
     * DELETE /v1/lms/lessons/{lessonId}/notes/{lessonNoteId} : Delete LessonNote
     * Soft delete (sets deleted_at).
     *
     * @param lessonId Lesson id. (required)
     * @param lessonNoteId LessonNote id. (required)
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
        value = LessonApi.PATH_DELETE_LESSON_NOTE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLessonNote(
         @PathVariable("lessonId") UUID lessonId,
         @PathVariable("lessonNoteId") UUID lessonNoteId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_LESSON = "/v1/lms/lessons/{lessonId}";
    /**
     * GET /v1/lms/lessons/{lessonId} : Get Lesson
     * Get one Lesson by id. The ETag header carries the row version.
     *
     * @param lessonId Lesson id. (required)
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
        value = LessonApi.PATH_GET_LESSON,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Lesson> getLesson(
         @PathVariable("lessonId") UUID lessonId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LESSON_ACTIVITY = "/v1/lms/lessons/{lessonId}/activities/{lessonActivityId}";
    /**
     * GET /v1/lms/lessons/{lessonId}/activities/{lessonActivityId} : Get LessonActivity
     * Get one LessonActivity by id. The ETag header carries the row version.
     *
     * @param lessonId Lesson id. (required)
     * @param lessonActivityId LessonActivity id. (required)
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
        value = LessonApi.PATH_GET_LESSON_ACTIVITY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LessonActivity> getLessonActivity(
         @PathVariable("lessonId") UUID lessonId,
         @PathVariable("lessonActivityId") UUID lessonActivityId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LESSON_CONTENT = "/v1/lms/lessons/{lessonId}:content";
    /**
     * GET /v1/lms/lessons/{lessonId}:content : Lesson body with navigation
     * Lesson body with navigation. Guests and non-enrolled users receive only lessons marked include_in_preview (canAccess&#x3D;false otherwise). Public.
     *
     * @param lessonId  (required)
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
        value = LessonApi.PATH_GET_LESSON_CONTENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LessonContent> getLessonContent(
         @PathVariable("lessonId") UUID lessonId
    );


    String PATH_GET_LESSON_NOTE = "/v1/lms/lessons/{lessonId}/notes/{lessonNoteId}";
    /**
     * GET /v1/lms/lessons/{lessonId}/notes/{lessonNoteId} : Get LessonNote
     * Get one LessonNote by id. The ETag header carries the row version.
     *
     * @param lessonId Lesson id. (required)
     * @param lessonNoteId LessonNote id. (required)
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
        value = LessonApi.PATH_GET_LESSON_NOTE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LessonNote> getLessonNote(
         @PathVariable("lessonId") UUID lessonId,
         @PathVariable("lessonNoteId") UUID lessonNoteId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_VIDEO_WATCH = "/v1/lms/lessons/{lessonId}/video-watches/{videoWatchId}";
    /**
     * GET /v1/lms/lessons/{lessonId}/video-watches/{videoWatchId} : Get VideoWatch
     * Get one VideoWatch by id. The ETag header carries the row version.
     *
     * @param lessonId Lesson id. (required)
     * @param videoWatchId VideoWatch id. (required)
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
        value = LessonApi.PATH_GET_VIDEO_WATCH,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<VideoWatch> getVideoWatch(
         @PathVariable("lessonId") UUID lessonId,
         @PathVariable("videoWatchId") UUID videoWatchId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LESSON_ACTIVITIES = "/v1/lms/lessons/{lessonId}/activities";
    /**
     * GET /v1/lms/lessons/{lessonId}/activities : List LessonActivities
     * List LessonActivities visible to the caller, page by page, with filtering and sorting.
     *
     * @param lessonId Lesson id. (required)
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
        value = LessonApi.PATH_LIST_LESSON_ACTIVITIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LessonActivity>> listLessonActivities(
         @PathVariable("lessonId") UUID lessonId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LESSON_NOTES = "/v1/lms/lessons/{lessonId}/notes";
    /**
     * GET /v1/lms/lessons/{lessonId}/notes : List LessonNotes
     * List LessonNotes visible to the caller, page by page, with filtering and sorting.
     *
     * @param lessonId Lesson id. (required)
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
        value = LessonApi.PATH_LIST_LESSON_NOTES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LessonNote>> listLessonNotes(
         @PathVariable("lessonId") UUID lessonId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LESSONS = "/v1/lms/lessons";
    /**
     * GET /v1/lms/lessons : List Lessons
     * List Lessons visible to the caller, page by page, with filtering and sorting.
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
        value = LessonApi.PATH_LIST_LESSONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Lesson>> listLessons(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_VIDEO_WATCHES = "/v1/lms/lessons/{lessonId}/video-watches";
    /**
     * GET /v1/lms/lessons/{lessonId}/video-watches : List VideoWatches
     * List VideoWatches visible to the caller, page by page, with filtering and sorting.
     *
     * @param lessonId Lesson id. (required)
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
        value = LessonApi.PATH_LIST_VIDEO_WATCHES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<VideoWatch>> listVideoWatches(
         @PathVariable("lessonId") UUID lessonId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_LESSON_COMPLETE = "/v1/lms/lessons/{lessonId}:markComplete";
    /**
     * POST /v1/lms/lessons/{lessonId}:markComplete : Record completion for the caller (enforces video / quiz rules from settings)
     * Record completion for the caller (enforces video / quiz rules from settings).
     *
     * @param lessonId  (required)
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
        value = LessonApi.PATH_MARK_LESSON_COMPLETE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonProgress> markLessonComplete(
         @PathVariable("lessonId") UUID lessonId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_MOVE_LESSON = "/v1/lms/lessons/{lessonId}:move";
    /**
     * POST /v1/lms/lessons/{lessonId}:move : Move / reorder a lesson
     * Move / reorder a lesson.
     *
     * @param lessonId  (required)
     * @param moveLessonRequest  (required)
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
        value = LessonApi.PATH_MOVE_LESSON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Lesson> moveLesson(
         @PathVariable("lessonId") UUID lessonId,
         @Valid @RequestBody MoveLessonRequest moveLessonRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_TRACK_VIDEO_WATCH = "/v1/lms/lessons/{lessonId}:trackVideoWatch";
    /**
     * POST /v1/lms/lessons/{lessonId}:trackVideoWatch : Record video watch time
     * Record video watch time.
     *
     * @param lessonId  (required)
     * @param trackVideoWatchRequest  (required)
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
        value = LessonApi.PATH_TRACK_VIDEO_WATCH,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<VideoWatch> trackVideoWatch(
         @PathVariable("lessonId") UUID lessonId,
         @Valid @RequestBody TrackVideoWatchRequest trackVideoWatchRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_LESSON = "/v1/lms/lessons/{lessonId}";
    /**
     * PATCH /v1/lms/lessons/{lessonId} : Update Lesson
     * Partially update a Lesson (updateMask). Requires If-Match with the current ETag.
     *
     * @param lessonId Lesson id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param lessonUpdate  (required)
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
        value = LessonApi.PATH_UPDATE_LESSON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Lesson> updateLesson(
         @PathVariable("lessonId") UUID lessonId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LessonUpdate lessonUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_LESSON_ACTIVITY = "/v1/lms/lessons/{lessonId}/activities/{lessonActivityId}";
    /**
     * PATCH /v1/lms/lessons/{lessonId}/activities/{lessonActivityId} : Update LessonActivity
     * Partially update a LessonActivity (updateMask). Requires If-Match with the current ETag.
     *
     * @param lessonId Lesson id. (required)
     * @param lessonActivityId LessonActivity id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param lessonActivityUpdate  (required)
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
        value = LessonApi.PATH_UPDATE_LESSON_ACTIVITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonActivity> updateLessonActivity(
         @PathVariable("lessonId") UUID lessonId,
         @PathVariable("lessonActivityId") UUID lessonActivityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LessonActivityUpdate lessonActivityUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_LESSON_NOTE = "/v1/lms/lessons/{lessonId}/notes/{lessonNoteId}";
    /**
     * PATCH /v1/lms/lessons/{lessonId}/notes/{lessonNoteId} : Update LessonNote
     * Partially update a LessonNote (updateMask). Requires If-Match with the current ETag.
     *
     * @param lessonId Lesson id. (required)
     * @param lessonNoteId LessonNote id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param lessonNoteUpdate  (required)
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
        value = LessonApi.PATH_UPDATE_LESSON_NOTE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LessonNote> updateLessonNote(
         @PathVariable("lessonId") UUID lessonId,
         @PathVariable("lessonNoteId") UUID lessonNoteId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LessonNoteUpdate lessonNoteUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
