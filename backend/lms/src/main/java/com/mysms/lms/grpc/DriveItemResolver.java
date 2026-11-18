package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.DriveItem;
import com.mysms.lms.api.model.DriveItemShare;
import com.mysms.lms.api.model.DriveItemShareUpdate;
import com.mysms.lms.api.model.DriveItemUpdate;
import com.mysms.lms.business.DriveItemService;
import com.mysms.lms.v1.CreateDriveItemRequest;
import com.mysms.lms.v1.CreateDriveItemResponse;
import com.mysms.lms.v1.CreateDriveItemShareRequest;
import com.mysms.lms.v1.CreateDriveItemShareResponse;
import com.mysms.lms.v1.DeleteDriveItemRequest;
import com.mysms.lms.v1.DeleteDriveItemResponse;
import com.mysms.lms.v1.DeleteDriveItemShareRequest;
import com.mysms.lms.v1.DeleteDriveItemShareResponse;
import com.mysms.lms.v1.DriveItemServiceGrpc;
import com.mysms.lms.v1.GetDriveItemRequest;
import com.mysms.lms.v1.GetDriveItemResponse;
import com.mysms.lms.v1.ListDriveItemSharesRequest;
import com.mysms.lms.v1.ListDriveItemSharesResponse;
import com.mysms.lms.v1.ListDriveItemsRequest;
import com.mysms.lms.v1.ListDriveItemsResponse;
import com.mysms.lms.v1.ShareDriveItemRequest;
import com.mysms.lms.v1.ShareDriveItemResponse;
import com.mysms.lms.v1.UpdateDriveItemRequest;
import com.mysms.lms.v1.UpdateDriveItemResponse;
import com.mysms.lms.v1.UpdateDriveItemShareRequest;
import com.mysms.lms.v1.UpdateDriveItemShareResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.DriveItemService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DriveItemResolver extends DriveItemServiceGrpc.DriveItemServiceImplBase {

    private final DriveItemService service;
    private final ProtoBridge bridge;

    public DriveItemResolver(DriveItemService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listDriveItems(ListDriveItemsRequest request, StreamObserver<ListDriveItemsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<DriveItem> rows = service.listDriveItems(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDriveItemsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDriveItem(GetDriveItemRequest request, StreamObserver<GetDriveItemResponse> out) {
        DriveItem found = service.getDriveItem(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetDriveItemResponse.newBuilder(), "drive_item", found).build());
    }

    @Override
    public void createDriveItem(CreateDriveItemRequest request, StreamObserver<CreateDriveItemResponse> out) {
        DriveItem created = service.createDriveItem(
                bridge.body(request.getDriveItem(), DriveItem.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateDriveItemResponse.newBuilder(), "drive_item", created).build());
    }

    @Override
    public void updateDriveItem(UpdateDriveItemRequest request, StreamObserver<UpdateDriveItemResponse> out) {
        DriveItem updated = service.updateDriveItem(
                bridge.id(request.getDriveItem().getId(), "id"),
                bridge.requiredIfMatch(request.getDriveItem().getEtag()),
                bridge.changes(request.getDriveItem(), request.getUpdateMask(), DriveItemUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateDriveItemResponse.newBuilder(), "drive_item", updated).build());
    }

    @Override
    public void deleteDriveItem(DeleteDriveItemRequest request, StreamObserver<DeleteDriveItemResponse> out) {
        service.deleteDriveItem(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDriveItemResponse.getDefaultInstance());
    }

    @Override
    public void listDriveItemShares(ListDriveItemSharesRequest request,
            StreamObserver<ListDriveItemSharesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<DriveItemShare> rows = service.listDriveItemShares(
                bridge.id(request.getDriveItemId(), "driveItemId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDriveItemSharesResponse.newBuilder(), rows).build());
    }

    @Override
    public void createDriveItemShare(CreateDriveItemShareRequest request,
            StreamObserver<CreateDriveItemShareResponse> out) {
        DriveItemShare created = service.createDriveItemShare(
                bridge.id(request.getDriveItemId(), "driveItemId"),
                bridge.body(request.getDriveItemShare(), DriveItemShare.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateDriveItemShareResponse.newBuilder(), "drive_item_share", created).build());
    }

    @Override
    public void updateDriveItemShare(UpdateDriveItemShareRequest request,
            StreamObserver<UpdateDriveItemShareResponse> out) {
        DriveItemShare updated = service.updateDriveItemShare(
                bridge.id(request.getDriveItemId(), "driveItemId"),
                bridge.id(request.getDriveItemShare().getId(), "id"),
                bridge.requiredIfMatch(request.getDriveItemShare().getEtag()),
                bridge.changes(request.getDriveItemShare(), request.getUpdateMask(), DriveItemShareUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateDriveItemShareResponse.newBuilder(), "drive_item_share", updated).build());
    }

    @Override
    public void deleteDriveItemShare(DeleteDriveItemShareRequest request,
            StreamObserver<DeleteDriveItemShareResponse> out) {
        service.deleteDriveItemShare(
                bridge.id(request.getDriveItemId(), "driveItemId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDriveItemShareResponse.getDefaultInstance());
    }

    @Override
    public void shareDriveItem(ShareDriveItemRequest request, StreamObserver<ShareDriveItemResponse> out) {
        ProtoBridge.send(out, bridge.reply(ShareDriveItemResponse.newBuilder(), "items",
                service.shareDriveItem(bridge.id(request.getDriveItemId(), "driveItemId"),
                        bridge.body(request, com.mysms.lms.api.model.ShareDriveItemRequest.class, "drive_item_id",
                                "request_id")).getItems()).build());
    }
}
