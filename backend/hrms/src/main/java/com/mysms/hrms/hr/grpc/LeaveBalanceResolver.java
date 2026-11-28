package com.mysms.hrms.hr.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.hr.api.model.LeaveBalance;
import com.mysms.hrms.hr.business.LeaveBalanceService;
import com.mysms.hr.v1.CreateLeaveBalanceRequest;
import com.mysms.hr.v1.CreateLeaveBalanceResponse;
import com.mysms.hr.v1.GetLeaveBalanceRequest;
import com.mysms.hr.v1.GetLeaveBalanceResponse;
import com.mysms.hr.v1.LeaveBalanceServiceGrpc;
import com.mysms.hr.v1.LeaveBalanceUpdate;
import com.mysms.hr.v1.ListLeaveBalancesRequest;
import com.mysms.hr.v1.ListLeaveBalancesResponse;
import com.mysms.hr.v1.UpdateLeaveBalanceResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of hr.LeaveBalanceService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LeaveBalanceResolver extends LeaveBalanceServiceGrpc.LeaveBalanceServiceImplBase {

    private final LeaveBalanceService leaveBalanceService;
    private final ProtoBridge bridge;

    public LeaveBalanceResolver(LeaveBalanceService leaveBalanceService, ProtoBridge bridge) {
        this.leaveBalanceService = leaveBalanceService;
        this.bridge = bridge;
    }

    @Override
    public void listLeaveBalances(ListLeaveBalancesRequest request, StreamObserver<ListLeaveBalancesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LeaveBalance> rows = leaveBalanceService.listLeaveBalances(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLeaveBalancesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLeaveBalance(GetLeaveBalanceRequest request, StreamObserver<GetLeaveBalanceResponse> out) {
        LeaveBalance found = leaveBalanceService.getLeaveBalance(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLeaveBalanceResponse.newBuilder(), "leave_balance", found).build());
    }

    @Override
    public void createLeaveBalance(CreateLeaveBalanceRequest request,
            StreamObserver<CreateLeaveBalanceResponse> out) {
        LeaveBalance created = leaveBalanceService.createLeaveBalance(
                bridge.body(request.getLeaveBalance(), LeaveBalance.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLeaveBalanceResponse.newBuilder(),
                "leave_balance", created).build());
    }

    @Override
    public void updateLeaveBalance(LeaveBalanceUpdate request,
            StreamObserver<UpdateLeaveBalanceResponse> out) {
        LeaveBalance updated = leaveBalanceService.updateLeaveBalance(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                LeaveBalanceProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLeaveBalanceResponse.newBuilder(),
                "leave_balance", updated).build());
    }
}
