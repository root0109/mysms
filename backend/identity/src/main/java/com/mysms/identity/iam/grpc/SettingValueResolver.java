package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.identity.iam.api.model.SettingValue;
import com.mysms.identity.iam.api.model.SettingValueUpdate;
import com.mysms.identity.iam.business.SettingValueService;
import com.mysms.iam.v1.CreateSettingValueRequest;
import com.mysms.iam.v1.CreateSettingValueResponse;
import com.mysms.iam.v1.DeleteSettingValueRequest;
import com.mysms.iam.v1.DeleteSettingValueResponse;
import com.mysms.iam.v1.GetSettingValueRequest;
import com.mysms.iam.v1.GetSettingValueResponse;
import com.mysms.iam.v1.ListSettingValuesRequest;
import com.mysms.iam.v1.ListSettingValuesResponse;
import com.mysms.iam.v1.SettingValueServiceGrpc;
import com.mysms.iam.v1.UpdateSettingValueRequest;
import com.mysms.iam.v1.UpdateSettingValueResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.SettingValueService: delegates every RPC to the same business service as REST, which owns the
 * scope rules of setting values.
 */
@GrpcService
public class SettingValueResolver extends SettingValueServiceGrpc.SettingValueServiceImplBase {

    private final SettingValueService service;
    private final ProtoBridge bridge;

    public SettingValueResolver(SettingValueService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSettingValues(ListSettingValuesRequest request, StreamObserver<ListSettingValuesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SettingValue> rows = service.list(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSettingValuesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSettingValue(GetSettingValueRequest request, StreamObserver<GetSettingValueResponse> out) {
        SettingValue found = service.get(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSettingValueResponse.newBuilder(), "setting_value", found).build());
    }

    @Override
    public void createSettingValue(CreateSettingValueRequest request, StreamObserver<CreateSettingValueResponse> out) {
        SettingValue created = service.create(bridge.body(request.getSettingValue(), SettingValue.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSettingValueResponse.newBuilder(), "setting_value", created).build());
    }

    @Override
    public void updateSettingValue(UpdateSettingValueRequest request, StreamObserver<UpdateSettingValueResponse> out) {
        SettingValue updated = service.update(
                bridge.id(request.getSettingValue().getId(), "id"),
                bridge.requiredIfMatch(request.getSettingValue().getEtag()),
                bridge.changes(request.getSettingValue(), request.getUpdateMask(), SettingValueUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSettingValueResponse.newBuilder(), "setting_value", updated).build());
    }

    @Override
    public void deleteSettingValue(DeleteSettingValueRequest request, StreamObserver<DeleteSettingValueResponse> out) {
        service.delete(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSettingValueResponse.getDefaultInstance());
    }
}
