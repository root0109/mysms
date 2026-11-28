package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.hr.api.model.Designation;
import com.mysms.hrms.hr.business.DesignationService;
import com.mysms.hr.v1.CreateDesignationRequest;
import com.mysms.hr.v1.CreateDesignationResponse;
import com.mysms.hr.v1.DeleteDesignationRequest;
import com.mysms.hr.v1.DeleteDesignationResponse;
import com.mysms.hr.v1.DesignationServiceGrpc;
import com.mysms.hr.v1.DesignationUpdate;
import com.mysms.hr.v1.GetDesignationRequest;
import com.mysms.hr.v1.GetDesignationResponse;
import com.mysms.hr.v1.ListDesignationsRequest;
import com.mysms.hr.v1.ListDesignationsResponse;
import com.mysms.hr.v1.UpdateDesignationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of hr.DesignationService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DesignationResolver extends DesignationServiceGrpc.DesignationServiceImplBase {

    private final DesignationService designationService;
    private final ProtoBridge bridge;

    public DesignationResolver(DesignationService designationService, ProtoBridge bridge) {
        this.designationService = designationService;
        this.bridge = bridge;
    }

    @Override
    public void listDesignations(ListDesignationsRequest request, StreamObserver<ListDesignationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Designation> rows = designationService.listDesignations(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDesignationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDesignation(GetDesignationRequest request, StreamObserver<GetDesignationResponse> out) {
        Designation found = designationService.getDesignation(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetDesignationResponse.newBuilder(), "designation", found).build());
    }

    @Override
    public void createDesignation(CreateDesignationRequest request, StreamObserver<CreateDesignationResponse> out) {
        Designation created = designationService.createDesignation(
                bridge.body(request.getDesignation(), Designation.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateDesignationResponse.newBuilder(), "designation", created).build());
    }

    @Override
    public void updateDesignation(DesignationUpdate request, StreamObserver<UpdateDesignationResponse> out) {
        Designation updated = designationService.updateDesignation(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                DesignationProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateDesignationResponse.newBuilder(), "designation", updated).build());
    }

    @Override
    public void deleteDesignation(DeleteDesignationRequest request, StreamObserver<DeleteDesignationResponse> out) {
        designationService.deleteDesignation(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDesignationResponse.getDefaultInstance());
    }
}
