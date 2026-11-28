package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.StorageQuota;
import com.mysms.platform.business.StorageQuotaService;
import com.mysms.platform.v1.CreateStorageQuotaRequest;
import com.mysms.platform.v1.CreateStorageQuotaResponse;
import com.mysms.platform.v1.DeleteStorageQuotaRequest;
import com.mysms.platform.v1.DeleteStorageQuotaResponse;
import com.mysms.platform.v1.GetMyStorageUsageRequest;
import com.mysms.platform.v1.GetMyStorageUsageResponse;
import com.mysms.platform.v1.GetStorageQuotaRequest;
import com.mysms.platform.v1.GetStorageQuotaResponse;
import com.mysms.platform.v1.ListStorageQuotasRequest;
import com.mysms.platform.v1.ListStorageQuotasResponse;
import com.mysms.platform.v1.StorageQuotaServiceGrpc;
import com.mysms.platform.v1.StorageQuotaUpdate;
import com.mysms.platform.v1.UpdateStorageQuotaResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.StorageQuotaService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StorageQuotaResolver extends StorageQuotaServiceGrpc.StorageQuotaServiceImplBase {

    private final StorageQuotaService service;
    private final ProtoBridge bridge;

    public StorageQuotaResolver(StorageQuotaService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStorageQuotas(ListStorageQuotasRequest request, StreamObserver<ListStorageQuotasResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StorageQuota> rows = service.listStorageQuotas(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStorageQuotasResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStorageQuota(GetStorageQuotaRequest request, StreamObserver<GetStorageQuotaResponse> out) {
        StorageQuota found = service.getStorageQuota(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStorageQuotaResponse.newBuilder(), "storage_quota", found).build());
    }

    @Override
    public void createStorageQuota(CreateStorageQuotaRequest request, StreamObserver<CreateStorageQuotaResponse> out) {
        StorageQuota created = service.createStorageQuota(
                bridge.body(request.getStorageQuota(), StorageQuota.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStorageQuotaResponse.newBuilder(), "storage_quota", created).build());
    }

    @Override
    public void updateStorageQuota(StorageQuotaUpdate request, StreamObserver<UpdateStorageQuotaResponse> out) {
        StorageQuota updated = service.updateStorageQuota(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                StorageQuotaProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStorageQuotaResponse.newBuilder(), "storage_quota", updated).build());
    }

    @Override
    public void deleteStorageQuota(DeleteStorageQuotaRequest request, StreamObserver<DeleteStorageQuotaResponse> out) {
        service.deleteStorageQuota(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStorageQuotaResponse.getDefaultInstance());
    }

    @Override
    public void getMyStorageUsage(GetMyStorageUsageRequest request, StreamObserver<GetMyStorageUsageResponse> out) {
        StorageQuota mine = service.getMyStorageUsage();
        ProtoBridge.send(out, bridge.reply(GetMyStorageUsageResponse.newBuilder(), "result", mine).build());
    }
}
