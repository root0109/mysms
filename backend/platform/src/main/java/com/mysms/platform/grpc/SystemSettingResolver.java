package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.SystemSetting;
import com.mysms.platform.api.model.SystemSettingUpdate;
import com.mysms.platform.business.SystemSettingService;
import com.mysms.platform.v1.CreateSystemSettingRequest;
import com.mysms.platform.v1.CreateSystemSettingResponse;
import com.mysms.platform.v1.DeleteSystemSettingRequest;
import com.mysms.platform.v1.DeleteSystemSettingResponse;
import com.mysms.platform.v1.GetSystemSettingRequest;
import com.mysms.platform.v1.GetSystemSettingResponse;
import com.mysms.platform.v1.ListSystemSettingsRequest;
import com.mysms.platform.v1.ListSystemSettingsResponse;
import com.mysms.platform.v1.SystemSettingServiceGrpc;
import com.mysms.platform.v1.UpdateSystemSettingRequest;
import com.mysms.platform.v1.UpdateSystemSettingResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.SystemSettingService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SystemSettingResolver extends SystemSettingServiceGrpc.SystemSettingServiceImplBase {

    private final SystemSettingService service;
    private final ProtoBridge bridge;

    public SystemSettingResolver(SystemSettingService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSystemSettings(ListSystemSettingsRequest request, StreamObserver<ListSystemSettingsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SystemSetting> rows = service.listSystemSettings(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSystemSettingsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSystemSetting(GetSystemSettingRequest request, StreamObserver<GetSystemSettingResponse> out) {
        SystemSetting found = service.getSystemSetting(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSystemSettingResponse.newBuilder(), "system_setting", found).build());
    }

    @Override
    public void createSystemSetting(CreateSystemSettingRequest request,
            StreamObserver<CreateSystemSettingResponse> out) {
        SystemSetting created = service.createSystemSetting(
                bridge.body(request.getSystemSetting(), SystemSetting.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSystemSettingResponse.newBuilder(), "system_setting",
                created).build());
    }

    @Override
    public void updateSystemSetting(UpdateSystemSettingRequest request,
            StreamObserver<UpdateSystemSettingResponse> out) {
        SystemSetting updated = service.updateSystemSetting(
                bridge.id(request.getSystemSetting().getId(), "id"),
                bridge.requiredIfMatch(request.getSystemSetting().getEtag()),
                bridge.changes(request.getSystemSetting(), request.getUpdateMask(), SystemSettingUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSystemSettingResponse.newBuilder(), "system_setting",
                updated).build());
    }

    @Override
    public void deleteSystemSetting(DeleteSystemSettingRequest request,
            StreamObserver<DeleteSystemSettingResponse> out) {
        service.deleteSystemSetting(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSystemSettingResponse.getDefaultInstance());
    }
}
