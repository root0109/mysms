package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.students.sis.api.model.ChildCard;
import com.mysms.students.sis.api.model.Guardian;
import com.mysms.students.sis.business.GuardianService;
import com.mysms.students.sis.business.PortalAccountService;
import com.mysms.students.sis.business.StudentSummaryService;
import com.mysms.sis.v1.CreateGuardianRequest;
import com.mysms.sis.v1.CreateGuardianResponse;
import com.mysms.sis.v1.CreatePortalAccountRequest;
import com.mysms.sis.v1.CreatePortalAccountResponse;
import com.mysms.sis.v1.DeleteGuardianRequest;
import com.mysms.sis.v1.DeleteGuardianResponse;
import com.mysms.sis.v1.GetGuardianRequest;
import com.mysms.sis.v1.GetGuardianResponse;
import com.mysms.sis.v1.GuardianServiceGrpc;
import com.mysms.sis.v1.GuardianUpdate;
import com.mysms.sis.v1.ListGuardiansRequest;
import com.mysms.sis.v1.ListGuardiansResponse;
import com.mysms.sis.v1.ListMyChildrenRequest;
import com.mysms.sis.v1.ListMyChildrenResponse;
import com.mysms.sis.v1.UpdateGuardianResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.GuardianService: delegates every RPC to the business service REST uses, so the rules exist once.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class GuardianResolver extends GuardianServiceGrpc.GuardianServiceImplBase {

    private final GuardianService guardianService;
    private final PortalAccountService portalAccountService;
    private final StudentSummaryService summaryService;
    private final ProtoBridge bridge;

    public GuardianResolver(
            GuardianService guardianService, PortalAccountService portalAccountService, StudentSummaryService summaryService,
            ProtoBridge bridge) {
        this.guardianService = guardianService;
        this.portalAccountService = portalAccountService;
        this.summaryService = summaryService;
        this.bridge = bridge;
    }

    @Override
    public void listGuardians(ListGuardiansRequest request, StreamObserver<ListGuardiansResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Guardian> rows = guardianService.listGuardians(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListGuardiansResponse.newBuilder(), rows).build());
    }

    @Override
    public void getGuardian(GetGuardianRequest request, StreamObserver<GetGuardianResponse> out) {
        Guardian found = guardianService.getGuardian(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetGuardianResponse.newBuilder(), "guardian", found).build());
    }

    @Override
    public void createGuardian(CreateGuardianRequest request, StreamObserver<CreateGuardianResponse> out) {
        Guardian created = guardianService.createGuardian(
                bridge.body(request.getGuardian(), Guardian.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateGuardianResponse.newBuilder(), "guardian", created).build());
    }

    @Override
    public void updateGuardian(GuardianUpdate request, StreamObserver<UpdateGuardianResponse> out) {
        Guardian updated = guardianService.updateGuardian(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                GuardianProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateGuardianResponse.newBuilder(), "guardian", updated).build());
    }

    @Override
    public void deleteGuardian(DeleteGuardianRequest request, StreamObserver<DeleteGuardianResponse> out) {
        guardianService.deleteGuardian(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteGuardianResponse.getDefaultInstance());
    }

    @Override
    public void createPortalAccount(CreatePortalAccountRequest request, StreamObserver<CreatePortalAccountResponse> out) {
        Guardian changed = portalAccountService.createGuardianPortalAccount(
                bridge.id(request.getGuardianId(), "guardianId"),
                bridge.body(request, com.mysms.students.sis.api.model.CreatePortalAccountRequest.class, "guardian_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CreatePortalAccountResponse.newBuilder(), "guardian", changed).build());
    }

    @Override
    public void listMyChildren(ListMyChildrenRequest request, StreamObserver<ListMyChildrenResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ChildCard> rows = summaryService.listMyChildren(page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListMyChildrenResponse.newBuilder(), rows).build());
    }
}
