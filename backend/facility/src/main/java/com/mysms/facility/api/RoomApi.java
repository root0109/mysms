// Owned API contract of facility (JAVA-CODING-RULES §7); first materialized from api/openapi/facility.yaml on 2026-11-10.
package com.mysms.facility.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.facility.api.model.Room;
import com.mysms.facility.api.model.RoomAsset;
import com.mysms.facility.api.model.RoomAssetUpdate;
import com.mysms.facility.api.model.RoomFacility;
import com.mysms.facility.api.model.RoomFacilityUpdate;
import com.mysms.facility.api.model.RoomUpdate;
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
public interface RoomApi {

    String PATH_CREATE_ROOM = "/v1/facility/rooms";
    /**
     * POST /v1/facility/rooms : Create Room
     * Create a Room. Supports Idempotency-Key and validateOnly.
     *
     * @param room  (required)
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
        value = RoomApi.PATH_CREATE_ROOM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Room> createRoom(
         @Valid @RequestBody Room room,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ROOM_ASSET = "/v1/facility/rooms/{roomId}/assets";
    /**
     * POST /v1/facility/rooms/{roomId}/assets : Create RoomAsset
     * Create a RoomAsset. Supports Idempotency-Key and validateOnly.
     *
     * @param roomId Room id. (required)
     * @param roomAsset  (required)
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
        value = RoomApi.PATH_CREATE_ROOM_ASSET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RoomAsset> createRoomAsset(
         @PathVariable("roomId") UUID roomId,
         @Valid @RequestBody RoomAsset roomAsset,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ROOM_FACILITY = "/v1/facility/rooms/{roomId}/facilities";
    /**
     * POST /v1/facility/rooms/{roomId}/facilities : Create RoomFacility
     * Create a RoomFacility. Supports Idempotency-Key and validateOnly.
     *
     * @param roomId Room id. (required)
     * @param roomFacility  (required)
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
        value = RoomApi.PATH_CREATE_ROOM_FACILITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RoomFacility> createRoomFacility(
         @PathVariable("roomId") UUID roomId,
         @Valid @RequestBody RoomFacility roomFacility,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ROOM = "/v1/facility/rooms/{roomId}";
    /**
     * DELETE /v1/facility/rooms/{roomId} : Delete Room
     * Soft delete (sets deleted_at).
     *
     * @param roomId Room id. (required)
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
        value = RoomApi.PATH_DELETE_ROOM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRoom(
         @PathVariable("roomId") UUID roomId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ROOM_ASSET = "/v1/facility/rooms/{roomId}/assets/{roomAssetId}";
    /**
     * DELETE /v1/facility/rooms/{roomId}/assets/{roomAssetId} : Delete RoomAsset
     * Soft delete (sets deleted_at).
     *
     * @param roomId Room id. (required)
     * @param roomAssetId RoomAsset id. (required)
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
        value = RoomApi.PATH_DELETE_ROOM_ASSET,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRoomAsset(
         @PathVariable("roomId") UUID roomId,
         @PathVariable("roomAssetId") UUID roomAssetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ROOM_FACILITY = "/v1/facility/rooms/{roomId}/facilities/{roomFacilityId}";
    /**
     * DELETE /v1/facility/rooms/{roomId}/facilities/{roomFacilityId} : Delete RoomFacility
     * Soft delete (sets deleted_at).
     *
     * @param roomId Room id. (required)
     * @param roomFacilityId RoomFacility id. (required)
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
        value = RoomApi.PATH_DELETE_ROOM_FACILITY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRoomFacility(
         @PathVariable("roomId") UUID roomId,
         @PathVariable("roomFacilityId") UUID roomFacilityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_ROOM = "/v1/facility/rooms/{roomId}";
    /**
     * GET /v1/facility/rooms/{roomId} : Get Room
     * Get one Room by id. The ETag header carries the row version.
     *
     * @param roomId Room id. (required)
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
        value = RoomApi.PATH_GET_ROOM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Room> getRoom(
         @PathVariable("roomId") UUID roomId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ROOM_ASSET = "/v1/facility/rooms/{roomId}/assets/{roomAssetId}";
    /**
     * GET /v1/facility/rooms/{roomId}/assets/{roomAssetId} : Get RoomAsset
     * Get one RoomAsset by id. The ETag header carries the row version.
     *
     * @param roomId Room id. (required)
     * @param roomAssetId RoomAsset id. (required)
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
        value = RoomApi.PATH_GET_ROOM_ASSET,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RoomAsset> getRoomAsset(
         @PathVariable("roomId") UUID roomId,
         @PathVariable("roomAssetId") UUID roomAssetId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ROOM_FACILITY = "/v1/facility/rooms/{roomId}/facilities/{roomFacilityId}";
    /**
     * GET /v1/facility/rooms/{roomId}/facilities/{roomFacilityId} : Get RoomFacility
     * Get one RoomFacility by id. The ETag header carries the row version.
     *
     * @param roomId Room id. (required)
     * @param roomFacilityId RoomFacility id. (required)
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
        value = RoomApi.PATH_GET_ROOM_FACILITY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RoomFacility> getRoomFacility(
         @PathVariable("roomId") UUID roomId,
         @PathVariable("roomFacilityId") UUID roomFacilityId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ROOM_ASSETS = "/v1/facility/rooms/{roomId}/assets";
    /**
     * GET /v1/facility/rooms/{roomId}/assets : List RoomAssets
     * List RoomAssets visible to the caller, page by page, with filtering and sorting.
     *
     * @param roomId Room id. (required)
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
        value = RoomApi.PATH_LIST_ROOM_ASSETS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RoomAsset>> listRoomAssets(
         @PathVariable("roomId") UUID roomId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ROOM_FACILITIES = "/v1/facility/rooms/{roomId}/facilities";
    /**
     * GET /v1/facility/rooms/{roomId}/facilities : List RoomFacilities
     * List RoomFacilities visible to the caller, page by page, with filtering and sorting.
     *
     * @param roomId Room id. (required)
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
        value = RoomApi.PATH_LIST_ROOM_FACILITIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RoomFacility>> listRoomFacilities(
         @PathVariable("roomId") UUID roomId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ROOMS = "/v1/facility/rooms";
    /**
     * GET /v1/facility/rooms : List Rooms
     * List Rooms visible to the caller, page by page, with filtering and sorting.
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
        value = RoomApi.PATH_LIST_ROOMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Room>> listRooms(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_ROOM = "/v1/facility/rooms/{roomId}";
    /**
     * PATCH /v1/facility/rooms/{roomId} : Update Room
     * Partially update a Room (updateMask). Requires If-Match with the current ETag.
     *
     * @param roomId Room id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param roomUpdate  (required)
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
        value = RoomApi.PATH_UPDATE_ROOM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Room> updateRoom(
         @PathVariable("roomId") UUID roomId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RoomUpdate roomUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ROOM_ASSET = "/v1/facility/rooms/{roomId}/assets/{roomAssetId}";
    /**
     * PATCH /v1/facility/rooms/{roomId}/assets/{roomAssetId} : Update RoomAsset
     * Partially update a RoomAsset (updateMask). Requires If-Match with the current ETag.
     *
     * @param roomId Room id. (required)
     * @param roomAssetId RoomAsset id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param roomAssetUpdate  (required)
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
        value = RoomApi.PATH_UPDATE_ROOM_ASSET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RoomAsset> updateRoomAsset(
         @PathVariable("roomId") UUID roomId,
         @PathVariable("roomAssetId") UUID roomAssetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RoomAssetUpdate roomAssetUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ROOM_FACILITY = "/v1/facility/rooms/{roomId}/facilities/{roomFacilityId}";
    /**
     * PATCH /v1/facility/rooms/{roomId}/facilities/{roomFacilityId} : Update RoomFacility
     * Partially update a RoomFacility (updateMask). Requires If-Match with the current ETag.
     *
     * @param roomId Room id. (required)
     * @param roomFacilityId RoomFacility id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param roomFacilityUpdate  (required)
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
        value = RoomApi.PATH_UPDATE_ROOM_FACILITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RoomFacility> updateRoomFacility(
         @PathVariable("roomId") UUID roomId,
         @PathVariable("roomFacilityId") UUID roomFacilityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RoomFacilityUpdate roomFacilityUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
