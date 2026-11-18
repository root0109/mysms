package com.mysms.admission.grpc;

import com.mysms.admission.api.model.AdmissionCycle;
import com.mysms.admission.api.model.AdmissionCycleFee;
import com.mysms.admission.api.model.AdmissionCycleFeeUpdate;
import com.mysms.admission.api.model.AdmissionCycleUpdate;
import com.mysms.admission.business.AdmissionCycleService;
import com.mysms.admission.v1.AdmissionCycleServiceGrpc;
import com.mysms.admission.v1.CancelAdmissionCycleRequest;
import com.mysms.admission.v1.CancelAdmissionCycleResponse;
import com.mysms.admission.v1.CloseAdmissionCycleRequest;
import com.mysms.admission.v1.CloseAdmissionCycleResponse;
import com.mysms.admission.v1.ConfirmAdmissionCycleRequest;
import com.mysms.admission.v1.ConfirmAdmissionCycleResponse;
import com.mysms.admission.v1.CreateAdmissionCycleFeeRequest;
import com.mysms.admission.v1.CreateAdmissionCycleFeeResponse;
import com.mysms.admission.v1.CreateAdmissionCycleRequest;
import com.mysms.admission.v1.CreateAdmissionCycleResponse;
import com.mysms.admission.v1.DeleteAdmissionCycleFeeRequest;
import com.mysms.admission.v1.DeleteAdmissionCycleFeeResponse;
import com.mysms.admission.v1.DeleteAdmissionCycleRequest;
import com.mysms.admission.v1.DeleteAdmissionCycleResponse;
import com.mysms.admission.v1.GetAdmissionCycleFeeRequest;
import com.mysms.admission.v1.GetAdmissionCycleFeeResponse;
import com.mysms.admission.v1.GetAdmissionCycleRequest;
import com.mysms.admission.v1.GetAdmissionCycleResponse;
import com.mysms.admission.v1.ListAdmissionCycleFeesRequest;
import com.mysms.admission.v1.ListAdmissionCycleFeesResponse;
import com.mysms.admission.v1.ListAdmissionCyclesRequest;
import com.mysms.admission.v1.ListAdmissionCyclesResponse;
import com.mysms.admission.v1.OpenApplicationsRequest;
import com.mysms.admission.v1.OpenApplicationsResponse;
import com.mysms.admission.v1.ResetAdmissionCycleRequest;
import com.mysms.admission.v1.ResetAdmissionCycleResponse;
import com.mysms.admission.v1.StartAdmissionRequest;
import com.mysms.admission.v1.StartAdmissionResponse;
import com.mysms.admission.v1.UpdateAdmissionCycleFeeRequest;
import com.mysms.admission.v1.UpdateAdmissionCycleFeeResponse;
import com.mysms.admission.v1.UpdateAdmissionCycleRequest;
import com.mysms.admission.v1.UpdateAdmissionCycleResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of admission.AdmissionCycleService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AdmissionCycleResolver extends AdmissionCycleServiceGrpc.AdmissionCycleServiceImplBase {

    private final AdmissionCycleService service;
    private final ProtoBridge bridge;

    public AdmissionCycleResolver(AdmissionCycleService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAdmissionCycles(ListAdmissionCyclesRequest request, StreamObserver<ListAdmissionCyclesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AdmissionCycle> rows = service.listAdmissionCycles(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAdmissionCyclesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAdmissionCycle(GetAdmissionCycleRequest request, StreamObserver<GetAdmissionCycleResponse> out) {
        AdmissionCycle found = service.getAdmissionCycle(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAdmissionCycleResponse.newBuilder(), "admission_cycle", found).build());
    }

    @Override
    public void createAdmissionCycle(CreateAdmissionCycleRequest request, StreamObserver<CreateAdmissionCycleResponse> out) {
        AdmissionCycle created = service.createAdmissionCycle(
                bridge.body(request.getAdmissionCycle(), AdmissionCycle.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAdmissionCycleResponse.newBuilder(), "admission_cycle", created).build());
    }

    @Override
    public void updateAdmissionCycle(UpdateAdmissionCycleRequest request, StreamObserver<UpdateAdmissionCycleResponse> out) {
        AdmissionCycle updated = service.updateAdmissionCycle(
                bridge.id(request.getAdmissionCycle().getId(), "id"),
                bridge.requiredIfMatch(request.getAdmissionCycle().getEtag()),
                bridge.changes(request.getAdmissionCycle(), request.getUpdateMask(), AdmissionCycleUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAdmissionCycleResponse.newBuilder(), "admission_cycle", updated).build());
    }

    @Override
    public void deleteAdmissionCycle(DeleteAdmissionCycleRequest request, StreamObserver<DeleteAdmissionCycleResponse> out) {
        service.deleteAdmissionCycle(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAdmissionCycleResponse.getDefaultInstance());
    }

    @Override
    public void listAdmissionCycleFees(ListAdmissionCycleFeesRequest request,
            StreamObserver<ListAdmissionCycleFeesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AdmissionCycleFee> rows = service.listAdmissionCycleFees(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAdmissionCycleFeesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAdmissionCycleFee(GetAdmissionCycleFeeRequest request, StreamObserver<GetAdmissionCycleFeeResponse> out) {
        AdmissionCycleFee found = service.getAdmissionCycleFee(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAdmissionCycleFeeResponse.newBuilder(), "admission_cycle_fee", found).build());
    }

    @Override
    public void createAdmissionCycleFee(CreateAdmissionCycleFeeRequest request,
            StreamObserver<CreateAdmissionCycleFeeResponse> out) {
        AdmissionCycleFee created = service.createAdmissionCycleFee(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.body(request.getAdmissionCycleFee(), AdmissionCycleFee.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateAdmissionCycleFeeResponse.newBuilder(), "admission_cycle_fee", created).build());
    }

    @Override
    public void updateAdmissionCycleFee(UpdateAdmissionCycleFeeRequest request,
            StreamObserver<UpdateAdmissionCycleFeeResponse> out) {
        AdmissionCycleFee updated = service.updateAdmissionCycleFee(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.id(request.getAdmissionCycleFee().getId(), "id"),
                bridge.requiredIfMatch(request.getAdmissionCycleFee().getEtag()),
                bridge.changes(request.getAdmissionCycleFee(), request.getUpdateMask(), AdmissionCycleFeeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateAdmissionCycleFeeResponse.newBuilder(), "admission_cycle_fee", updated).build());
    }

    @Override
    public void deleteAdmissionCycleFee(DeleteAdmissionCycleFeeRequest request,
            StreamObserver<DeleteAdmissionCycleFeeResponse> out) {
        service.deleteAdmissionCycleFee(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAdmissionCycleFeeResponse.getDefaultInstance());
    }

    @Override
    public void confirmAdmissionCycle(ConfirmAdmissionCycleRequest request, StreamObserver<ConfirmAdmissionCycleResponse> out) {
        AdmissionCycle changed = service.confirmAdmissionCycle(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.body(request, com.mysms.admission.api.model.ConfirmAdmissionCycleRequest.class, "admission_cycle_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ConfirmAdmissionCycleResponse.newBuilder(), "admission_cycle", changed).build());
    }

    @Override
    public void openApplications(OpenApplicationsRequest request, StreamObserver<OpenApplicationsResponse> out) {
        AdmissionCycle changed = service.openApplications(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.body(request, com.mysms.admission.api.model.OpenApplicationsRequest.class, "admission_cycle_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(OpenApplicationsResponse.newBuilder(), "admission_cycle", changed).build());
    }

    @Override
    public void startAdmission(StartAdmissionRequest request, StreamObserver<StartAdmissionResponse> out) {
        AdmissionCycle changed = service.startAdmission(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.body(request, com.mysms.admission.api.model.StartAdmissionRequest.class, "admission_cycle_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(StartAdmissionResponse.newBuilder(), "admission_cycle", changed).build());
    }

    @Override
    public void closeAdmissionCycle(CloseAdmissionCycleRequest request, StreamObserver<CloseAdmissionCycleResponse> out) {
        AdmissionCycle changed = service.closeAdmissionCycle(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.body(request, com.mysms.admission.api.model.CloseAdmissionCycleRequest.class, "admission_cycle_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CloseAdmissionCycleResponse.newBuilder(), "admission_cycle", changed).build());
    }

    @Override
    public void cancelAdmissionCycle(CancelAdmissionCycleRequest request, StreamObserver<CancelAdmissionCycleResponse> out) {
        AdmissionCycle changed = service.cancelAdmissionCycle(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.body(request, com.mysms.admission.api.model.CancelAdmissionCycleRequest.class, "admission_cycle_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelAdmissionCycleResponse.newBuilder(), "admission_cycle", changed).build());
    }

    @Override
    public void resetAdmissionCycle(ResetAdmissionCycleRequest request, StreamObserver<ResetAdmissionCycleResponse> out) {
        AdmissionCycle changed = service.resetAdmissionCycle(
                bridge.id(request.getAdmissionCycleId(), "admissionCycleId"),
                bridge.body(request, com.mysms.admission.api.model.ResetAdmissionCycleRequest.class, "admission_cycle_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetAdmissionCycleResponse.newBuilder(), "admission_cycle", changed).build());
    }
}
