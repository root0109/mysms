package com.mysms.attendance.grpc;

import com.mysms.attendance.api.model.AbsenceReason;
import com.mysms.attendance.api.model.AbsenceReasonUpdate;
import com.mysms.attendance.business.AbsenceReasonService;
import com.mysms.attendance.v1.AbsenceReasonServiceGrpc;
import com.mysms.attendance.v1.CreateAbsenceReasonRequest;
import com.mysms.attendance.v1.CreateAbsenceReasonResponse;
import com.mysms.attendance.v1.DeleteAbsenceReasonRequest;
import com.mysms.attendance.v1.DeleteAbsenceReasonResponse;
import com.mysms.attendance.v1.GetAbsenceReasonRequest;
import com.mysms.attendance.v1.GetAbsenceReasonResponse;
import com.mysms.attendance.v1.ListAbsenceReasonsRequest;
import com.mysms.attendance.v1.ListAbsenceReasonsResponse;
import com.mysms.attendance.v1.UpdateAbsenceReasonRequest;
import com.mysms.attendance.v1.UpdateAbsenceReasonResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of attendance.AbsenceReasonService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AbsenceReasonResolver extends AbsenceReasonServiceGrpc.AbsenceReasonServiceImplBase {

    private final AbsenceReasonService service;
    private final ProtoBridge bridge;

    public AbsenceReasonResolver(AbsenceReasonService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAbsenceReasons(ListAbsenceReasonsRequest request, StreamObserver<ListAbsenceReasonsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AbsenceReason> rows = service.listAbsenceReasons(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAbsenceReasonsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAbsenceReason(GetAbsenceReasonRequest request, StreamObserver<GetAbsenceReasonResponse> out) {
        AbsenceReason found = service.getAbsenceReason(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAbsenceReasonResponse.newBuilder(), "absence_reason", found).build());
    }

    @Override
    public void createAbsenceReason(CreateAbsenceReasonRequest request, StreamObserver<CreateAbsenceReasonResponse> out) {
        AbsenceReason created = service.createAbsenceReason(bridge.body(request.getAbsenceReason(), AbsenceReason.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAbsenceReasonResponse.newBuilder(), "absence_reason", created).build());
    }

    @Override
    public void updateAbsenceReason(UpdateAbsenceReasonRequest request, StreamObserver<UpdateAbsenceReasonResponse> out) {
        AbsenceReason updated = service.updateAbsenceReason(bridge.id(request.getAbsenceReason().getId(), "id"),
                bridge.requiredIfMatch(request.getAbsenceReason().getEtag()),
                bridge.changes(request.getAbsenceReason(), request.getUpdateMask(), AbsenceReasonUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAbsenceReasonResponse.newBuilder(), "absence_reason", updated).build());
    }

    @Override
    public void deleteAbsenceReason(DeleteAbsenceReasonRequest request, StreamObserver<DeleteAbsenceReasonResponse> out) {
        service.deleteAbsenceReason(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAbsenceReasonResponse.getDefaultInstance());
    }
}
