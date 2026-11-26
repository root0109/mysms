package com.mysms.campus.facility.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.facility.api.model.Room;
import com.mysms.campus.facility.api.model.RoomAsset;
import com.mysms.campus.facility.api.model.RoomAssetUpdate;
import com.mysms.campus.facility.api.model.RoomFacility;
import com.mysms.campus.facility.api.model.RoomFacilityUpdate;
import com.mysms.campus.facility.api.model.RoomUpdate;
import com.mysms.campus.facility.business.RoomService;
import com.mysms.facility.v1.CreateRoomAssetRequest;
import com.mysms.facility.v1.CreateRoomAssetResponse;
import com.mysms.facility.v1.CreateRoomFacilityRequest;
import com.mysms.facility.v1.CreateRoomFacilityResponse;
import com.mysms.facility.v1.CreateRoomRequest;
import com.mysms.facility.v1.CreateRoomResponse;
import com.mysms.facility.v1.DeleteRoomAssetRequest;
import com.mysms.facility.v1.DeleteRoomAssetResponse;
import com.mysms.facility.v1.DeleteRoomFacilityRequest;
import com.mysms.facility.v1.DeleteRoomFacilityResponse;
import com.mysms.facility.v1.DeleteRoomRequest;
import com.mysms.facility.v1.DeleteRoomResponse;
import com.mysms.facility.v1.GetRoomAssetRequest;
import com.mysms.facility.v1.GetRoomAssetResponse;
import com.mysms.facility.v1.GetRoomFacilityRequest;
import com.mysms.facility.v1.GetRoomFacilityResponse;
import com.mysms.facility.v1.GetRoomRequest;
import com.mysms.facility.v1.GetRoomResponse;
import com.mysms.facility.v1.ListRoomAssetsRequest;
import com.mysms.facility.v1.ListRoomAssetsResponse;
import com.mysms.facility.v1.ListRoomFacilitiesRequest;
import com.mysms.facility.v1.ListRoomFacilitiesResponse;
import com.mysms.facility.v1.ListRoomsRequest;
import com.mysms.facility.v1.ListRoomsResponse;
import com.mysms.facility.v1.RoomServiceGrpc;
import com.mysms.facility.v1.UpdateRoomAssetRequest;
import com.mysms.facility.v1.UpdateRoomAssetResponse;
import com.mysms.facility.v1.UpdateRoomFacilityRequest;
import com.mysms.facility.v1.UpdateRoomFacilityResponse;
import com.mysms.facility.v1.UpdateRoomRequest;
import com.mysms.facility.v1.UpdateRoomResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of facility.RoomService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class RoomResolver extends RoomServiceGrpc.RoomServiceImplBase {

    private final RoomService service;
    private final ProtoBridge bridge;

    public RoomResolver(RoomService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listRooms(ListRoomsRequest request, StreamObserver<ListRoomsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Room> rows = service.listRooms(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListRoomsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRoom(GetRoomRequest request, StreamObserver<GetRoomResponse> out) {
        Room found = service.getRoom(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRoomResponse.newBuilder(), "room", found).build());
    }

    @Override
    public void createRoom(CreateRoomRequest request, StreamObserver<CreateRoomResponse> out) {
        Room created = service.createRoom(bridge.body(request.getRoom(), Room.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRoomResponse.newBuilder(), "room", created).build());
    }

    @Override
    public void updateRoom(UpdateRoomRequest request, StreamObserver<UpdateRoomResponse> out) {
        Room updated = service.updateRoom(bridge.id(request.getRoom().getId(), "id"),
                bridge.requiredIfMatch(request.getRoom().getEtag()),
                bridge.changes(request.getRoom(), request.getUpdateMask(), RoomUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRoomResponse.newBuilder(), "room", updated).build());
    }

    @Override
    public void deleteRoom(DeleteRoomRequest request, StreamObserver<DeleteRoomResponse> out) {
        service.deleteRoom(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRoomResponse.getDefaultInstance());
    }

    @Override
    public void listRoomFacilities(ListRoomFacilitiesRequest request, StreamObserver<ListRoomFacilitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RoomFacility> rows = service.listRoomFacilitys(bridge.id(request.getRoomId(), "roomId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRoomFacilitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRoomFacility(GetRoomFacilityRequest request, StreamObserver<GetRoomFacilityResponse> out) {
        RoomFacility found = service.getRoomFacility(bridge.id(request.getRoomId(), "roomId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRoomFacilityResponse.newBuilder(), "room_facility", found).build());
    }

    @Override
    public void createRoomFacility(CreateRoomFacilityRequest request, StreamObserver<CreateRoomFacilityResponse> out) {
        RoomFacility created = service.createRoomFacility(bridge.id(request.getRoomId(), "roomId"),
                bridge.body(request.getRoomFacility(), RoomFacility.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRoomFacilityResponse.newBuilder(), "room_facility", created).build());
    }

    @Override
    public void updateRoomFacility(UpdateRoomFacilityRequest request, StreamObserver<UpdateRoomFacilityResponse> out) {
        RoomFacility updated = service.updateRoomFacility(bridge.id(request.getRoomId(), "roomId"),
                bridge.id(request.getRoomFacility().getId(), "id"), bridge.requiredIfMatch(request.getRoomFacility().getEtag()),
                bridge.changes(request.getRoomFacility(), request.getUpdateMask(), RoomFacilityUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRoomFacilityResponse.newBuilder(), "room_facility", updated).build());
    }

    @Override
    public void deleteRoomFacility(DeleteRoomFacilityRequest request, StreamObserver<DeleteRoomFacilityResponse> out) {
        service.deleteRoomFacility(bridge.id(request.getRoomId(), "roomId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRoomFacilityResponse.getDefaultInstance());
    }

    @Override
    public void listRoomAssets(ListRoomAssetsRequest request, StreamObserver<ListRoomAssetsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RoomAsset> rows = service.listRoomAssets(bridge.id(request.getRoomId(), "roomId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRoomAssetsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRoomAsset(GetRoomAssetRequest request, StreamObserver<GetRoomAssetResponse> out) {
        RoomAsset found = service.getRoomAsset(bridge.id(request.getRoomId(), "roomId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRoomAssetResponse.newBuilder(), "room_asset", found).build());
    }

    @Override
    public void createRoomAsset(CreateRoomAssetRequest request, StreamObserver<CreateRoomAssetResponse> out) {
        RoomAsset created = service.createRoomAsset(bridge.id(request.getRoomId(), "roomId"),
                bridge.body(request.getRoomAsset(), RoomAsset.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRoomAssetResponse.newBuilder(), "room_asset", created).build());
    }

    @Override
    public void updateRoomAsset(UpdateRoomAssetRequest request, StreamObserver<UpdateRoomAssetResponse> out) {
        RoomAsset updated = service.updateRoomAsset(bridge.id(request.getRoomId(), "roomId"),
                bridge.id(request.getRoomAsset().getId(), "id"), bridge.requiredIfMatch(request.getRoomAsset().getEtag()),
                bridge.changes(request.getRoomAsset(), request.getUpdateMask(), RoomAssetUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRoomAssetResponse.newBuilder(), "room_asset", updated).build());
    }

    @Override
    public void deleteRoomAsset(DeleteRoomAssetRequest request, StreamObserver<DeleteRoomAssetResponse> out) {
        service.deleteRoomAsset(bridge.id(request.getRoomId(), "roomId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRoomAssetResponse.getDefaultInstance());
    }
}
