package com.mysms.campus.facility.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.facility.api.model.AssetItem;
import com.mysms.campus.facility.api.model.AssetItemUpdate;
import com.mysms.campus.facility.business.AssetItemService;
import com.mysms.facility.v1.AssetItemServiceGrpc;
import com.mysms.facility.v1.CreateAssetItemRequest;
import com.mysms.facility.v1.CreateAssetItemResponse;
import com.mysms.facility.v1.DeleteAssetItemRequest;
import com.mysms.facility.v1.DeleteAssetItemResponse;
import com.mysms.facility.v1.GetAssetItemRequest;
import com.mysms.facility.v1.GetAssetItemResponse;
import com.mysms.facility.v1.ListAssetItemsRequest;
import com.mysms.facility.v1.ListAssetItemsResponse;
import com.mysms.facility.v1.UpdateAssetItemRequest;
import com.mysms.facility.v1.UpdateAssetItemResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of facility.AssetItemService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AssetItemResolver extends AssetItemServiceGrpc.AssetItemServiceImplBase {

    private final AssetItemService service;
    private final ProtoBridge bridge;

    public AssetItemResolver(AssetItemService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAssetItems(ListAssetItemsRequest request, StreamObserver<ListAssetItemsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssetItem> rows = service.listAssetItems(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListAssetItemsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssetItem(GetAssetItemRequest request, StreamObserver<GetAssetItemResponse> out) {
        AssetItem found = service.getAssetItem(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssetItemResponse.newBuilder(), "asset_item", found).build());
    }

    @Override
    public void createAssetItem(CreateAssetItemRequest request, StreamObserver<CreateAssetItemResponse> out) {
        AssetItem created = service.createAssetItem(bridge.body(request.getAssetItem(), AssetItem.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssetItemResponse.newBuilder(), "asset_item", created).build());
    }

    @Override
    public void updateAssetItem(UpdateAssetItemRequest request, StreamObserver<UpdateAssetItemResponse> out) {
        AssetItem updated = service.updateAssetItem(bridge.id(request.getAssetItem().getId(), "id"),
                bridge.requiredIfMatch(request.getAssetItem().getEtag()),
                bridge.changes(request.getAssetItem(), request.getUpdateMask(), AssetItemUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssetItemResponse.newBuilder(), "asset_item", updated).build());
    }

    @Override
    public void deleteAssetItem(DeleteAssetItemRequest request, StreamObserver<DeleteAssetItemResponse> out) {
        service.deleteAssetItem(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssetItemResponse.getDefaultInstance());
    }
}
