// Owned API contract of library.
package com.mysms.campus.library.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.campus.library.api.model.Media;
import com.mysms.campus.library.api.model.MediaAuthor;
import com.mysms.campus.library.api.model.MediaAuthorUpdate;
import com.mysms.campus.library.api.model.MediaCourse;
import com.mysms.campus.library.api.model.MediaCourseUpdate;
import com.mysms.campus.library.api.model.MediaPublisher;
import com.mysms.campus.library.api.model.MediaPublisherUpdate;
import com.mysms.campus.library.api.model.MediaSubject;
import com.mysms.campus.library.api.model.MediaSubjectUpdate;
import com.mysms.campus.library.api.model.MediaUnit;
import com.mysms.campus.library.api.model.MediaUnitUpdate;
import com.mysms.campus.library.api.model.MediaUpdate;
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
public interface MediaApi {

    String PATH_CREATE_MEDIA = "/v1/library/media";
    /**
     * POST /v1/library/media : Create Media
     * Create a Media. Supports Idempotency-Key and validateOnly.
     *
     * @param media  (required)
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
        value = MediaApi.PATH_CREATE_MEDIA,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Media> createMedia(
         @Valid @RequestBody Media media,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_MEDIA_AUTHOR = "/v1/library/media/{mediaId}/authors";
    /**
     * POST /v1/library/media/{mediaId}/authors : Create MediaAuthor
     * Create a MediaAuthor. Supports Idempotency-Key and validateOnly.
     *
     * @param mediaId Media id. (required)
     * @param mediaAuthor  (required)
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
        value = MediaApi.PATH_CREATE_MEDIA_AUTHOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaAuthor> createMediaAuthor(
         @PathVariable("mediaId") UUID mediaId,
         @Valid @RequestBody MediaAuthor mediaAuthor,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_MEDIA_COURSE = "/v1/library/media/{mediaId}/courses";
    /**
     * POST /v1/library/media/{mediaId}/courses : Create MediaCourse
     * Create a MediaCourse. Supports Idempotency-Key and validateOnly.
     *
     * @param mediaId Media id. (required)
     * @param mediaCourse  (required)
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
        value = MediaApi.PATH_CREATE_MEDIA_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaCourse> createMediaCourse(
         @PathVariable("mediaId") UUID mediaId,
         @Valid @RequestBody MediaCourse mediaCourse,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_MEDIA_PUBLISHER = "/v1/library/media/{mediaId}/publishers";
    /**
     * POST /v1/library/media/{mediaId}/publishers : Create MediaPublisher
     * Create a MediaPublisher. Supports Idempotency-Key and validateOnly.
     *
     * @param mediaId Media id. (required)
     * @param mediaPublisher  (required)
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
        value = MediaApi.PATH_CREATE_MEDIA_PUBLISHER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaPublisher> createMediaPublisher(
         @PathVariable("mediaId") UUID mediaId,
         @Valid @RequestBody MediaPublisher mediaPublisher,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_MEDIA_SUBJECT = "/v1/library/media/{mediaId}/subjects";
    /**
     * POST /v1/library/media/{mediaId}/subjects : Create MediaSubject
     * Create a MediaSubject. Supports Idempotency-Key and validateOnly.
     *
     * @param mediaId Media id. (required)
     * @param mediaSubject  (required)
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
        value = MediaApi.PATH_CREATE_MEDIA_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaSubject> createMediaSubject(
         @PathVariable("mediaId") UUID mediaId,
         @Valid @RequestBody MediaSubject mediaSubject,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_MEDIA_UNIT = "/v1/library/media/{mediaId}/units";
    /**
     * POST /v1/library/media/{mediaId}/units : Create MediaUnit
     * Create a MediaUnit. Supports Idempotency-Key and validateOnly.
     *
     * @param mediaId Media id. (required)
     * @param mediaUnit  (required)
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
        value = MediaApi.PATH_CREATE_MEDIA_UNIT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaUnit> createMediaUnit(
         @PathVariable("mediaId") UUID mediaId,
         @Valid @RequestBody MediaUnit mediaUnit,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_MEDIA = "/v1/library/media/{mediaId}";
    /**
     * DELETE /v1/library/media/{mediaId} : Delete Media
     * Soft delete (sets deleted_at).
     *
     * @param mediaId Media id. (required)
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
        value = MediaApi.PATH_DELETE_MEDIA,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMedia(
         @PathVariable("mediaId") UUID mediaId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_MEDIA_AUTHOR = "/v1/library/media/{mediaId}/authors/{mediaAuthorId}";
    /**
     * DELETE /v1/library/media/{mediaId}/authors/{mediaAuthorId} : Delete MediaAuthor
     * Soft delete (sets deleted_at).
     *
     * @param mediaId Media id. (required)
     * @param mediaAuthorId MediaAuthor id. (required)
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
        value = MediaApi.PATH_DELETE_MEDIA_AUTHOR,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMediaAuthor(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaAuthorId") UUID mediaAuthorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_MEDIA_COURSE = "/v1/library/media/{mediaId}/courses/{mediaCourseId}";
    /**
     * DELETE /v1/library/media/{mediaId}/courses/{mediaCourseId} : Delete MediaCourse
     * Soft delete (sets deleted_at).
     *
     * @param mediaId Media id. (required)
     * @param mediaCourseId MediaCourse id. (required)
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
        value = MediaApi.PATH_DELETE_MEDIA_COURSE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMediaCourse(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaCourseId") UUID mediaCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_MEDIA_PUBLISHER = "/v1/library/media/{mediaId}/publishers/{mediaPublisherId}";
    /**
     * DELETE /v1/library/media/{mediaId}/publishers/{mediaPublisherId} : Delete MediaPublisher
     * Soft delete (sets deleted_at).
     *
     * @param mediaId Media id. (required)
     * @param mediaPublisherId MediaPublisher id. (required)
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
        value = MediaApi.PATH_DELETE_MEDIA_PUBLISHER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMediaPublisher(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaPublisherId") UUID mediaPublisherId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_MEDIA_SUBJECT = "/v1/library/media/{mediaId}/subjects/{mediaSubjectId}";
    /**
     * DELETE /v1/library/media/{mediaId}/subjects/{mediaSubjectId} : Delete MediaSubject
     * Soft delete (sets deleted_at).
     *
     * @param mediaId Media id. (required)
     * @param mediaSubjectId MediaSubject id. (required)
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
        value = MediaApi.PATH_DELETE_MEDIA_SUBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMediaSubject(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaSubjectId") UUID mediaSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_MEDIA_UNIT = "/v1/library/media/{mediaId}/units/{mediaUnitId}";
    /**
     * DELETE /v1/library/media/{mediaId}/units/{mediaUnitId} : Delete MediaUnit
     * Soft delete (sets deleted_at).
     *
     * @param mediaId Media id. (required)
     * @param mediaUnitId MediaUnit id. (required)
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
        value = MediaApi.PATH_DELETE_MEDIA_UNIT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMediaUnit(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaUnitId") UUID mediaUnitId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_MEDIA = "/v1/library/media/{mediaId}";
    /**
     * GET /v1/library/media/{mediaId} : Get Media
     * Get one Media by id. The ETag header carries the row version.
     *
     * @param mediaId Media id. (required)
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
        value = MediaApi.PATH_GET_MEDIA,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Media> getMedia(
         @PathVariable("mediaId") UUID mediaId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MEDIA_AUTHOR = "/v1/library/media/{mediaId}/authors/{mediaAuthorId}";
    /**
     * GET /v1/library/media/{mediaId}/authors/{mediaAuthorId} : Get MediaAuthor
     * Get one MediaAuthor by id. The ETag header carries the row version.
     *
     * @param mediaId Media id. (required)
     * @param mediaAuthorId MediaAuthor id. (required)
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
        value = MediaApi.PATH_GET_MEDIA_AUTHOR,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MediaAuthor> getMediaAuthor(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaAuthorId") UUID mediaAuthorId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MEDIA_COURSE = "/v1/library/media/{mediaId}/courses/{mediaCourseId}";
    /**
     * GET /v1/library/media/{mediaId}/courses/{mediaCourseId} : Get MediaCourse
     * Get one MediaCourse by id. The ETag header carries the row version.
     *
     * @param mediaId Media id. (required)
     * @param mediaCourseId MediaCourse id. (required)
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
        value = MediaApi.PATH_GET_MEDIA_COURSE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MediaCourse> getMediaCourse(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaCourseId") UUID mediaCourseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MEDIA_PUBLISHER = "/v1/library/media/{mediaId}/publishers/{mediaPublisherId}";
    /**
     * GET /v1/library/media/{mediaId}/publishers/{mediaPublisherId} : Get MediaPublisher
     * Get one MediaPublisher by id. The ETag header carries the row version.
     *
     * @param mediaId Media id. (required)
     * @param mediaPublisherId MediaPublisher id. (required)
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
        value = MediaApi.PATH_GET_MEDIA_PUBLISHER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MediaPublisher> getMediaPublisher(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaPublisherId") UUID mediaPublisherId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MEDIA_SUBJECT = "/v1/library/media/{mediaId}/subjects/{mediaSubjectId}";
    /**
     * GET /v1/library/media/{mediaId}/subjects/{mediaSubjectId} : Get MediaSubject
     * Get one MediaSubject by id. The ETag header carries the row version.
     *
     * @param mediaId Media id. (required)
     * @param mediaSubjectId MediaSubject id. (required)
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
        value = MediaApi.PATH_GET_MEDIA_SUBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MediaSubject> getMediaSubject(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaSubjectId") UUID mediaSubjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MEDIA_UNIT = "/v1/library/media/{mediaId}/units/{mediaUnitId}";
    /**
     * GET /v1/library/media/{mediaId}/units/{mediaUnitId} : Get MediaUnit
     * Get one MediaUnit by id. The ETag header carries the row version.
     *
     * @param mediaId Media id. (required)
     * @param mediaUnitId MediaUnit id. (required)
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
        value = MediaApi.PATH_GET_MEDIA_UNIT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MediaUnit> getMediaUnit(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaUnitId") UUID mediaUnitId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MEDIA = "/v1/library/media";
    /**
     * GET /v1/library/media : List Media
     * List Media visible to the caller, page by page, with filtering and sorting.
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
        value = MediaApi.PATH_LIST_MEDIA,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Media>> listMedia(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MEDIA_AUTHORS = "/v1/library/media/{mediaId}/authors";
    /**
     * GET /v1/library/media/{mediaId}/authors : List MediaAuthors
     * List MediaAuthors visible to the caller, page by page, with filtering and sorting.
     *
     * @param mediaId Media id. (required)
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
        value = MediaApi.PATH_LIST_MEDIA_AUTHORS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MediaAuthor>> listMediaAuthors(
         @PathVariable("mediaId") UUID mediaId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MEDIA_COURSES = "/v1/library/media/{mediaId}/courses";
    /**
     * GET /v1/library/media/{mediaId}/courses : List MediaCourses
     * List MediaCourses visible to the caller, page by page, with filtering and sorting.
     *
     * @param mediaId Media id. (required)
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
        value = MediaApi.PATH_LIST_MEDIA_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MediaCourse>> listMediaCourses(
         @PathVariable("mediaId") UUID mediaId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MEDIA_PUBLISHERS = "/v1/library/media/{mediaId}/publishers";
    /**
     * GET /v1/library/media/{mediaId}/publishers : List MediaPublishers
     * List MediaPublishers visible to the caller, page by page, with filtering and sorting.
     *
     * @param mediaId Media id. (required)
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
        value = MediaApi.PATH_LIST_MEDIA_PUBLISHERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MediaPublisher>> listMediaPublishers(
         @PathVariable("mediaId") UUID mediaId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MEDIA_SUBJECTS = "/v1/library/media/{mediaId}/subjects";
    /**
     * GET /v1/library/media/{mediaId}/subjects : List MediaSubjects
     * List MediaSubjects visible to the caller, page by page, with filtering and sorting.
     *
     * @param mediaId Media id. (required)
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
        value = MediaApi.PATH_LIST_MEDIA_SUBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MediaSubject>> listMediaSubjects(
         @PathVariable("mediaId") UUID mediaId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MEDIA_UNITS = "/v1/library/media/{mediaId}/units";
    /**
     * GET /v1/library/media/{mediaId}/units : List MediaUnits
     * List MediaUnits visible to the caller, page by page, with filtering and sorting.
     *
     * @param mediaId Media id. (required)
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
        value = MediaApi.PATH_LIST_MEDIA_UNITS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MediaUnit>> listMediaUnits(
         @PathVariable("mediaId") UUID mediaId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_MEDIA = "/v1/library/media/{mediaId}";
    /**
     * PATCH /v1/library/media/{mediaId} : Update Media
     * Partially update a Media (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param mediaId Media id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param mediaUpdate  (required)
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
        value = MediaApi.PATH_UPDATE_MEDIA,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Media> updateMedia(
         @PathVariable("mediaId") UUID mediaId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MediaUpdate mediaUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_MEDIA_AUTHOR = "/v1/library/media/{mediaId}/authors/{mediaAuthorId}";
    /**
     * PATCH /v1/library/media/{mediaId}/authors/{mediaAuthorId} : Update MediaAuthor
     * Partially update a MediaAuthor (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param mediaId Media id. (required)
     * @param mediaAuthorId MediaAuthor id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param mediaAuthorUpdate  (required)
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
        value = MediaApi.PATH_UPDATE_MEDIA_AUTHOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaAuthor> updateMediaAuthor(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaAuthorId") UUID mediaAuthorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MediaAuthorUpdate mediaAuthorUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_MEDIA_COURSE = "/v1/library/media/{mediaId}/courses/{mediaCourseId}";
    /**
     * PATCH /v1/library/media/{mediaId}/courses/{mediaCourseId} : Update MediaCourse
     * Partially update a MediaCourse (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param mediaId Media id. (required)
     * @param mediaCourseId MediaCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param mediaCourseUpdate  (required)
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
        value = MediaApi.PATH_UPDATE_MEDIA_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaCourse> updateMediaCourse(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaCourseId") UUID mediaCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MediaCourseUpdate mediaCourseUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_MEDIA_PUBLISHER = "/v1/library/media/{mediaId}/publishers/{mediaPublisherId}";
    /**
     * PATCH /v1/library/media/{mediaId}/publishers/{mediaPublisherId} : Update MediaPublisher
     * Partially update a MediaPublisher (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param mediaId Media id. (required)
     * @param mediaPublisherId MediaPublisher id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param mediaPublisherUpdate  (required)
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
        value = MediaApi.PATH_UPDATE_MEDIA_PUBLISHER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaPublisher> updateMediaPublisher(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaPublisherId") UUID mediaPublisherId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MediaPublisherUpdate mediaPublisherUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_MEDIA_SUBJECT = "/v1/library/media/{mediaId}/subjects/{mediaSubjectId}";
    /**
     * PATCH /v1/library/media/{mediaId}/subjects/{mediaSubjectId} : Update MediaSubject
     * Partially update a MediaSubject (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param mediaId Media id. (required)
     * @param mediaSubjectId MediaSubject id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param mediaSubjectUpdate  (required)
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
        value = MediaApi.PATH_UPDATE_MEDIA_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaSubject> updateMediaSubject(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaSubjectId") UUID mediaSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MediaSubjectUpdate mediaSubjectUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_MEDIA_UNIT = "/v1/library/media/{mediaId}/units/{mediaUnitId}";
    /**
     * PATCH /v1/library/media/{mediaId}/units/{mediaUnitId} : Update MediaUnit
     * Partially update a MediaUnit (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param mediaId Media id. (required)
     * @param mediaUnitId MediaUnit id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param mediaUnitUpdate  (required)
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
        value = MediaApi.PATH_UPDATE_MEDIA_UNIT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MediaUnit> updateMediaUnit(
         @PathVariable("mediaId") UUID mediaId,
         @PathVariable("mediaUnitId") UUID mediaUnitId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MediaUnitUpdate mediaUnitUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
