package com.mysms.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.iam.api.model.SettingDefinition;
import com.mysms.iam.business.SettingDefinitionService;
import com.mysms.iam.v1.GetSettingDefinitionRequest;
import com.mysms.iam.v1.GetSettingDefinitionResponse;
import com.mysms.iam.v1.ListSettingDefinitionsRequest;
import com.mysms.iam.v1.ListSettingDefinitionsResponse;
import com.mysms.iam.v1.SettingDefinitionServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.SettingDefinitionService: the read-only settings catalogue, delegated to the same business
 * service as REST, which also decides which definitions the caller may see.
 */
@GrpcService
public class SettingDefinitionResolver extends SettingDefinitionServiceGrpc.SettingDefinitionServiceImplBase {

    private final SettingDefinitionService service;
    private final ProtoBridge bridge;

    public SettingDefinitionResolver(SettingDefinitionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSettingDefinitions(ListSettingDefinitionsRequest request,
            StreamObserver<ListSettingDefinitionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SettingDefinition> rows = service.list(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSettingDefinitionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSettingDefinition(GetSettingDefinitionRequest request,
            StreamObserver<GetSettingDefinitionResponse> out) {
        SettingDefinition found = service.get(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSettingDefinitionResponse.newBuilder(), "setting_definition", found)
                .build());
    }
}
