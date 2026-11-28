package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.ExternalToolWorkflowService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ExternalTool;
import com.mysms.learning.lms.api.model.ExternalToolInstitution;
import com.mysms.learning.lms.api.model.ExternalToolOption;
import com.mysms.learning.lms.api.model.ExternalToolRole;
import com.mysms.learning.lms.api.model.ExternalToolSetting;
import com.mysms.learning.lms.business.ExternalToolService;
import com.mysms.lms.v1.ActivateExternalToolRequest;
import com.mysms.lms.v1.ActivateExternalToolResponse;
import com.mysms.lms.v1.CreateExternalToolInstitutionRequest;
import com.mysms.lms.v1.CreateExternalToolInstitutionResponse;
import com.mysms.lms.v1.CreateExternalToolOptionRequest;
import com.mysms.lms.v1.CreateExternalToolOptionResponse;
import com.mysms.lms.v1.CreateExternalToolRequest;
import com.mysms.lms.v1.CreateExternalToolResponse;
import com.mysms.lms.v1.CreateExternalToolRoleRequest;
import com.mysms.lms.v1.CreateExternalToolRoleResponse;
import com.mysms.lms.v1.CreateExternalToolSettingRequest;
import com.mysms.lms.v1.CreateExternalToolSettingResponse;
import com.mysms.lms.v1.DeleteExternalToolInstitutionRequest;
import com.mysms.lms.v1.DeleteExternalToolInstitutionResponse;
import com.mysms.lms.v1.DeleteExternalToolOptionRequest;
import com.mysms.lms.v1.DeleteExternalToolOptionResponse;
import com.mysms.lms.v1.DeleteExternalToolRequest;
import com.mysms.lms.v1.DeleteExternalToolResponse;
import com.mysms.lms.v1.DeleteExternalToolRoleRequest;
import com.mysms.lms.v1.DeleteExternalToolRoleResponse;
import com.mysms.lms.v1.DeleteExternalToolSettingRequest;
import com.mysms.lms.v1.DeleteExternalToolSettingResponse;
import com.mysms.lms.v1.ExternalToolInstitutionUpdate;
import com.mysms.lms.v1.ExternalToolOptionUpdate;
import com.mysms.lms.v1.ExternalToolRoleUpdate;
import com.mysms.lms.v1.ExternalToolServiceGrpc;
import com.mysms.lms.v1.ExternalToolSettingUpdate;
import com.mysms.lms.v1.ExternalToolUpdate;
import com.mysms.lms.v1.GetExternalToolInstitutionRequest;
import com.mysms.lms.v1.GetExternalToolInstitutionResponse;
import com.mysms.lms.v1.GetExternalToolOptionRequest;
import com.mysms.lms.v1.GetExternalToolOptionResponse;
import com.mysms.lms.v1.GetExternalToolRequest;
import com.mysms.lms.v1.GetExternalToolResponse;
import com.mysms.lms.v1.GetExternalToolRoleRequest;
import com.mysms.lms.v1.GetExternalToolRoleResponse;
import com.mysms.lms.v1.GetExternalToolSettingRequest;
import com.mysms.lms.v1.GetExternalToolSettingResponse;
import com.mysms.lms.v1.ListExternalToolInstitutionsRequest;
import com.mysms.lms.v1.ListExternalToolInstitutionsResponse;
import com.mysms.lms.v1.ListExternalToolOptionsRequest;
import com.mysms.lms.v1.ListExternalToolOptionsResponse;
import com.mysms.lms.v1.ListExternalToolRolesRequest;
import com.mysms.lms.v1.ListExternalToolRolesResponse;
import com.mysms.lms.v1.ListExternalToolSettingsRequest;
import com.mysms.lms.v1.ListExternalToolSettingsResponse;
import com.mysms.lms.v1.ListExternalToolsRequest;
import com.mysms.lms.v1.ListExternalToolsResponse;
import com.mysms.lms.v1.UpdateExternalToolInstitutionResponse;
import com.mysms.lms.v1.UpdateExternalToolOptionResponse;
import com.mysms.lms.v1.UpdateExternalToolResponse;
import com.mysms.lms.v1.UpdateExternalToolRoleResponse;
import com.mysms.lms.v1.UpdateExternalToolSettingResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ExternalToolService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ExternalToolResolver extends ExternalToolServiceGrpc.ExternalToolServiceImplBase {

    private final ExternalToolService service;
    private final ProtoBridge bridge;
    private final ExternalToolWorkflowService workflows;

    public ExternalToolResolver(ExternalToolService service, ProtoBridge bridge,
            ExternalToolWorkflowService workflows) {
        this.workflows = workflows;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listExternalTools(ListExternalToolsRequest request, StreamObserver<ListExternalToolsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExternalTool> rows = service.listExternalTools(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExternalToolsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExternalTool(GetExternalToolRequest request, StreamObserver<GetExternalToolResponse> out) {
        ExternalTool found = service.getExternalTool(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExternalToolResponse.newBuilder(), "external_tool", found).build());
    }

    @Override
    public void createExternalTool(CreateExternalToolRequest request, StreamObserver<CreateExternalToolResponse> out) {
        ExternalTool created = service.createExternalTool(
                bridge.body(request.getExternalTool(), ExternalTool.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateExternalToolResponse.newBuilder(), "external_tool", created).build());
    }

    @Override
    public void updateExternalTool(ExternalToolUpdate request, StreamObserver<UpdateExternalToolResponse> out) {
        ExternalTool updated = service.updateExternalTool(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ExternalToolProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateExternalToolResponse.newBuilder(), "external_tool", updated).build());
    }

    @Override
    public void deleteExternalTool(DeleteExternalToolRequest request, StreamObserver<DeleteExternalToolResponse> out) {
        service.deleteExternalTool(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExternalToolResponse.getDefaultInstance());
    }

    @Override
    public void listExternalToolSettings(ListExternalToolSettingsRequest request,
            StreamObserver<ListExternalToolSettingsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExternalToolSetting> rows = service.listExternalToolSettings(
                bridge.id(request.getExternalToolId(), "externalToolId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExternalToolSettingsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExternalToolSetting(GetExternalToolSettingRequest request,
            StreamObserver<GetExternalToolSettingResponse> out) {
        ExternalToolSetting found = service.getExternalToolSetting(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetExternalToolSettingResponse.newBuilder(), "external_tool_setting", found).build());
    }

    @Override
    public void createExternalToolSetting(CreateExternalToolSettingRequest request,
            StreamObserver<CreateExternalToolSettingResponse> out) {
        ExternalToolSetting created = service.createExternalToolSetting(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.body(request.getExternalToolSetting(), ExternalToolSetting.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateExternalToolSettingResponse.newBuilder(), "external_tool_setting", created).build());
    }

    @Override
    public void updateExternalToolSetting(ExternalToolSettingUpdate request,
            StreamObserver<UpdateExternalToolSettingResponse> out) {
        ExternalToolSetting updated = service.updateExternalToolSetting(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ExternalToolSettingProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateExternalToolSettingResponse.newBuilder(), "external_tool_setting", updated).build());
    }

    @Override
    public void deleteExternalToolSetting(DeleteExternalToolSettingRequest request,
            StreamObserver<DeleteExternalToolSettingResponse> out) {
        service.deleteExternalToolSetting(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExternalToolSettingResponse.getDefaultInstance());
    }

    @Override
    public void listExternalToolInstitutions(ListExternalToolInstitutionsRequest request,
            StreamObserver<ListExternalToolInstitutionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExternalToolInstitution> rows = service.listExternalToolInstitutions(
                bridge.id(request.getExternalToolId(), "externalToolId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExternalToolInstitutionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExternalToolInstitution(GetExternalToolInstitutionRequest request,
            StreamObserver<GetExternalToolInstitutionResponse> out) {
        ExternalToolInstitution found = service.getExternalToolInstitution(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetExternalToolInstitutionResponse.newBuilder(), "external_tool_institution", found).build());
    }

    @Override
    public void createExternalToolInstitution(CreateExternalToolInstitutionRequest request,
            StreamObserver<CreateExternalToolInstitutionResponse> out) {
        ExternalToolInstitution created = service.createExternalToolInstitution(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.body(request.getExternalToolInstitution(), ExternalToolInstitution.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateExternalToolInstitutionResponse.newBuilder(), "external_tool_institution", created).build());
    }

    @Override
    public void updateExternalToolInstitution(ExternalToolInstitutionUpdate request,
            StreamObserver<UpdateExternalToolInstitutionResponse> out) {
        ExternalToolInstitution updated = service.updateExternalToolInstitution(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ExternalToolInstitutionProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateExternalToolInstitutionResponse.newBuilder(), "external_tool_institution", updated).build());
    }

    @Override
    public void deleteExternalToolInstitution(DeleteExternalToolInstitutionRequest request,
            StreamObserver<DeleteExternalToolInstitutionResponse> out) {
        service.deleteExternalToolInstitution(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExternalToolInstitutionResponse.getDefaultInstance());
    }

    @Override
    public void listExternalToolRoles(ListExternalToolRolesRequest request,
            StreamObserver<ListExternalToolRolesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExternalToolRole> rows = service.listExternalToolRoles(
                bridge.id(request.getExternalToolId(), "externalToolId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExternalToolRolesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExternalToolRole(GetExternalToolRoleRequest request,
            StreamObserver<GetExternalToolRoleResponse> out) {
        ExternalToolRole found = service.getExternalToolRole(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetExternalToolRoleResponse.newBuilder(), "external_tool_role", found).build());
    }

    @Override
    public void createExternalToolRole(CreateExternalToolRoleRequest request,
            StreamObserver<CreateExternalToolRoleResponse> out) {
        ExternalToolRole created = service.createExternalToolRole(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.body(request.getExternalToolRole(), ExternalToolRole.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateExternalToolRoleResponse.newBuilder(), "external_tool_role", created).build());
    }

    @Override
    public void updateExternalToolRole(ExternalToolRoleUpdate request,
            StreamObserver<UpdateExternalToolRoleResponse> out) {
        ExternalToolRole updated = service.updateExternalToolRole(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ExternalToolRoleProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateExternalToolRoleResponse.newBuilder(), "external_tool_role", updated).build());
    }

    @Override
    public void deleteExternalToolRole(DeleteExternalToolRoleRequest request,
            StreamObserver<DeleteExternalToolRoleResponse> out) {
        service.deleteExternalToolRole(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExternalToolRoleResponse.getDefaultInstance());
    }

    @Override
    public void listExternalToolOptions(ListExternalToolOptionsRequest request,
            StreamObserver<ListExternalToolOptionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExternalToolOption> rows = service.listExternalToolOptions(
                bridge.id(request.getExternalToolId(), "externalToolId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExternalToolOptionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExternalToolOption(GetExternalToolOptionRequest request,
            StreamObserver<GetExternalToolOptionResponse> out) {
        ExternalToolOption found = service.getExternalToolOption(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetExternalToolOptionResponse.newBuilder(), "external_tool_option", found).build());
    }

    @Override
    public void createExternalToolOption(CreateExternalToolOptionRequest request,
            StreamObserver<CreateExternalToolOptionResponse> out) {
        ExternalToolOption created = service.createExternalToolOption(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.body(request.getExternalToolOption(), ExternalToolOption.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateExternalToolOptionResponse.newBuilder(), "external_tool_option", created).build());
    }

    @Override
    public void updateExternalToolOption(ExternalToolOptionUpdate request,
            StreamObserver<UpdateExternalToolOptionResponse> out) {
        ExternalToolOption updated = service.updateExternalToolOption(
                bridge.id(request.getExternalToolId(), "externalToolId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ExternalToolOptionProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateExternalToolOptionResponse.newBuilder(), "external_tool_option", updated).build());
    }

    @Override
    public void deleteExternalToolOption(DeleteExternalToolOptionRequest request,
            StreamObserver<DeleteExternalToolOptionResponse> out) {
        service.deleteExternalToolOption(
                bridge.id(request.getExternalToolId(), "externalToolId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExternalToolOptionResponse.getDefaultInstance());
    }

    @Override
    public void activateExternalTool(ActivateExternalToolRequest request,
            StreamObserver<ActivateExternalToolResponse> out) {
        ProtoBridge.send(out, bridge.reply(ActivateExternalToolResponse.newBuilder(), "external_tool",
                workflows.activateExternalTool(bridge.id(request.getExternalToolId(), "externalToolId"),
                        bridge.body(request, com.mysms.learning.lms.api.model.ActivateExternalToolRequest.class,
                                "external_tool_id", "request_id"))).build());
    }
}
