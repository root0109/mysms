package com.mysms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.payroll.api.model.SalaryTemplate;
import com.mysms.payroll.api.model.SalaryTemplateComponent;
import com.mysms.payroll.api.model.SalaryTemplateComponentUpdate;
import com.mysms.payroll.api.model.SalaryTemplateUpdate;
import com.mysms.payroll.business.SalaryTemplateService;
import com.mysms.payroll.v1.CreateSalaryTemplateComponentRequest;
import com.mysms.payroll.v1.CreateSalaryTemplateComponentResponse;
import com.mysms.payroll.v1.CreateSalaryTemplateRequest;
import com.mysms.payroll.v1.CreateSalaryTemplateResponse;
import com.mysms.payroll.v1.DeleteSalaryTemplateComponentRequest;
import com.mysms.payroll.v1.DeleteSalaryTemplateComponentResponse;
import com.mysms.payroll.v1.DeleteSalaryTemplateRequest;
import com.mysms.payroll.v1.DeleteSalaryTemplateResponse;
import com.mysms.payroll.v1.GetSalaryTemplateComponentRequest;
import com.mysms.payroll.v1.GetSalaryTemplateComponentResponse;
import com.mysms.payroll.v1.GetSalaryTemplateRequest;
import com.mysms.payroll.v1.GetSalaryTemplateResponse;
import com.mysms.payroll.v1.ListSalaryTemplateComponentsRequest;
import com.mysms.payroll.v1.ListSalaryTemplateComponentsResponse;
import com.mysms.payroll.v1.ListSalaryTemplatesRequest;
import com.mysms.payroll.v1.ListSalaryTemplatesResponse;
import com.mysms.payroll.v1.SalaryTemplateServiceGrpc;
import com.mysms.payroll.v1.UpdateSalaryTemplateComponentRequest;
import com.mysms.payroll.v1.UpdateSalaryTemplateComponentResponse;
import com.mysms.payroll.v1.UpdateSalaryTemplateRequest;
import com.mysms.payroll.v1.UpdateSalaryTemplateResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.SalaryTemplateService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SalaryTemplateResolver extends SalaryTemplateServiceGrpc.SalaryTemplateServiceImplBase {

    private final SalaryTemplateService salaryTemplateService;
    private final ProtoBridge bridge;

    public SalaryTemplateResolver(SalaryTemplateService salaryTemplateService, ProtoBridge bridge) {
        this.salaryTemplateService = salaryTemplateService;
        this.bridge = bridge;
    }

    @Override
    public void listSalaryTemplates(ListSalaryTemplatesRequest request,
            StreamObserver<ListSalaryTemplatesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SalaryTemplate> rows = salaryTemplateService.listSalaryTemplates(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSalaryTemplatesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSalaryTemplate(GetSalaryTemplateRequest request, StreamObserver<GetSalaryTemplateResponse> out) {
        SalaryTemplate found = salaryTemplateService.getSalaryTemplate(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSalaryTemplateResponse.newBuilder(), "salary_template", found).build());
    }

    @Override
    public void createSalaryTemplate(CreateSalaryTemplateRequest request,
            StreamObserver<CreateSalaryTemplateResponse> out) {
        SalaryTemplate created = salaryTemplateService.createSalaryTemplate(
                bridge.body(request.getSalaryTemplate(), SalaryTemplate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSalaryTemplateResponse.newBuilder(),
                "salary_template", created).build());
    }

    @Override
    public void updateSalaryTemplate(UpdateSalaryTemplateRequest request,
            StreamObserver<UpdateSalaryTemplateResponse> out) {
        SalaryTemplate updated = salaryTemplateService.updateSalaryTemplate(
                bridge.id(request.getSalaryTemplate().getId(), "id"),
                bridge.requiredIfMatch(request.getSalaryTemplate().getEtag()),
                bridge.changes(request.getSalaryTemplate(), request.getUpdateMask(), SalaryTemplateUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSalaryTemplateResponse.newBuilder(),
                "salary_template", updated).build());
    }

    @Override
    public void deleteSalaryTemplate(DeleteSalaryTemplateRequest request,
            StreamObserver<DeleteSalaryTemplateResponse> out) {
        salaryTemplateService.deleteSalaryTemplate(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSalaryTemplateResponse.getDefaultInstance());
    }

    @Override
    public void listSalaryTemplateComponents(ListSalaryTemplateComponentsRequest request,
            StreamObserver<ListSalaryTemplateComponentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SalaryTemplateComponent> rows = salaryTemplateService.listSalaryTemplateComponents(
                bridge.id(request.getSalaryTemplateId(), "salaryTemplateId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSalaryTemplateComponentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSalaryTemplateComponent(GetSalaryTemplateComponentRequest request,
            StreamObserver<GetSalaryTemplateComponentResponse> out) {
        SalaryTemplateComponent found = salaryTemplateService.getSalaryTemplateComponent(
                bridge.id(request.getSalaryTemplateId(), "salaryTemplateId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSalaryTemplateComponentResponse.newBuilder(),
                "salary_template_component", found).build());
    }

    @Override
    public void createSalaryTemplateComponent(CreateSalaryTemplateComponentRequest request,
            StreamObserver<CreateSalaryTemplateComponentResponse> out) {
        SalaryTemplateComponent created = salaryTemplateService.createSalaryTemplateComponent(
                bridge.id(request.getSalaryTemplateId(), "salaryTemplateId"),
                bridge.body(request.getSalaryTemplateComponent(), SalaryTemplateComponent.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSalaryTemplateComponentResponse.newBuilder(),
                "salary_template_component", created).build());
    }

    @Override
    public void updateSalaryTemplateComponent(UpdateSalaryTemplateComponentRequest request,
            StreamObserver<UpdateSalaryTemplateComponentResponse> out) {
        SalaryTemplateComponent updated = salaryTemplateService.updateSalaryTemplateComponent(
                bridge.id(request.getSalaryTemplateId(), "salaryTemplateId"),
                bridge.id(request.getSalaryTemplateComponent().getId(), "id"),
                bridge.requiredIfMatch(request.getSalaryTemplateComponent().getEtag()),
                bridge.changes(request.getSalaryTemplateComponent(), request.getUpdateMask(),
                        SalaryTemplateComponentUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSalaryTemplateComponentResponse.newBuilder(),
                "salary_template_component", updated).build());
    }

    @Override
    public void deleteSalaryTemplateComponent(DeleteSalaryTemplateComponentRequest request,
            StreamObserver<DeleteSalaryTemplateComponentResponse> out) {
        salaryTemplateService.deleteSalaryTemplateComponent(
                bridge.id(request.getSalaryTemplateId(), "salaryTemplateId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSalaryTemplateComponentResponse.getDefaultInstance());
    }
}
