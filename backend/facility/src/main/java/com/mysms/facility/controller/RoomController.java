package com.mysms.facility.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.facility.api.RoomApi;
import com.mysms.facility.api.model.Room;
import com.mysms.facility.api.model.RoomAsset;
import com.mysms.facility.api.model.RoomAssetUpdate;
import com.mysms.facility.api.model.RoomFacility;
import com.mysms.facility.api.model.RoomFacilityUpdate;
import com.mysms.facility.api.model.RoomUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.facility.business.RoomService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Room operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/facility.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class RoomController implements RoomApi {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public ResponseEntity<Room> createRoom(Room room, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Room created = roomService.createRoom(room, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/facility/rooms/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<RoomAsset> createRoomAsset(UUID roomId, RoomAsset roomAsset,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RoomAsset created = roomService.createRoomAsset(roomId, roomAsset, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/facility/rooms/" + roomId + "/assets/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<RoomFacility> createRoomFacility(UUID roomId, RoomFacility roomFacility,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RoomFacility created = roomService.createRoomFacility(roomId, roomFacility, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/facility/rooms/" + roomId + "/facilities/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteRoom(UUID roomId, String ifMatch) {
        roomService.deleteRoom(roomId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRoomAsset(UUID roomId, UUID roomAssetId, String ifMatch) {
        roomService.deleteRoomAsset(roomId, roomAssetId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRoomFacility(UUID roomId, UUID roomFacilityId, String ifMatch) {
        roomService.deleteRoomFacility(roomId, roomFacilityId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Room> getRoom(UUID roomId, String fields) {
        Room found = roomService.getRoom(roomId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RoomAsset> getRoomAsset(UUID roomId, UUID roomAssetId, String fields) {
        RoomAsset found = roomService.getRoomAsset(roomId, roomAssetId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RoomFacility> getRoomFacility(UUID roomId, UUID roomFacilityId, String fields) {
        RoomFacility found = roomService.getRoomFacility(roomId, roomFacilityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RoomAsset>> listRoomAssets(UUID roomId, Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<RoomAsset> list =
                PagingUtil.pagingInfo(roomService.listRoomAssets(roomId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<RoomFacility>> listRoomFacilities(UUID roomId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RoomFacility> list =
                PagingUtil.pagingInfo(roomService.listRoomFacilitys(roomId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Room>> listRooms(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Room> list = PagingUtil.pagingInfo(roomService.listRooms(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Room> updateRoom(UUID roomId, String ifMatch, RoomUpdate roomUpdate,
            String updateMask, Boolean validateOnly) {
        Room updated = roomService.updateRoom(roomId, ifMatch, roomUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RoomAsset> updateRoomAsset(UUID roomId, UUID roomAssetId, String ifMatch,
            RoomAssetUpdate roomAssetUpdate, String updateMask, Boolean validateOnly) {
        RoomAsset updated = roomService.updateRoomAsset(roomId, roomAssetId, ifMatch, roomAssetUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RoomFacility> updateRoomFacility(UUID roomId, UUID roomFacilityId,
            String ifMatch, RoomFacilityUpdate roomFacilityUpdate, String updateMask, Boolean validateOnly) {
        RoomFacility updated =
                roomService.updateRoomFacility(roomId, roomFacilityId, ifMatch, roomFacilityUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
