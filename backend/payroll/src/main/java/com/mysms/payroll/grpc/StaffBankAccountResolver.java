package com.mysms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.payroll.api.model.StaffBankAccount;
import com.mysms.payroll.api.model.StaffBankAccountUpdate;
import com.mysms.payroll.business.StaffBankAccountService;
import com.mysms.payroll.v1.CreateStaffBankAccountRequest;
import com.mysms.payroll.v1.CreateStaffBankAccountResponse;
import com.mysms.payroll.v1.DeleteStaffBankAccountRequest;
import com.mysms.payroll.v1.DeleteStaffBankAccountResponse;
import com.mysms.payroll.v1.GetStaffBankAccountRequest;
import com.mysms.payroll.v1.GetStaffBankAccountResponse;
import com.mysms.payroll.v1.ListStaffBankAccountsRequest;
import com.mysms.payroll.v1.ListStaffBankAccountsResponse;
import com.mysms.payroll.v1.StaffBankAccountServiceGrpc;
import com.mysms.payroll.v1.UpdateStaffBankAccountRequest;
import com.mysms.payroll.v1.UpdateStaffBankAccountResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.StaffBankAccountService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StaffBankAccountResolver extends StaffBankAccountServiceGrpc.StaffBankAccountServiceImplBase {

    private final StaffBankAccountService staffBankAccountService;
    private final ProtoBridge bridge;

    public StaffBankAccountResolver(StaffBankAccountService staffBankAccountService, ProtoBridge bridge) {
        this.staffBankAccountService = staffBankAccountService;
        this.bridge = bridge;
    }

    @Override
    public void listStaffBankAccounts(ListStaffBankAccountsRequest request,
            StreamObserver<ListStaffBankAccountsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StaffBankAccount> rows = staffBankAccountService.listStaffBankAccounts(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStaffBankAccountsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStaffBankAccount(GetStaffBankAccountRequest request,
            StreamObserver<GetStaffBankAccountResponse> out) {
        StaffBankAccount found = staffBankAccountService.getStaffBankAccount(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStaffBankAccountResponse.newBuilder(),
                "staff_bank_account", found).build());
    }

    @Override
    public void createStaffBankAccount(CreateStaffBankAccountRequest request,
            StreamObserver<CreateStaffBankAccountResponse> out) {
        StaffBankAccount created = staffBankAccountService.createStaffBankAccount(
                bridge.body(request.getStaffBankAccount(), StaffBankAccount.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStaffBankAccountResponse.newBuilder(),
                "staff_bank_account", created).build());
    }

    @Override
    public void updateStaffBankAccount(UpdateStaffBankAccountRequest request,
            StreamObserver<UpdateStaffBankAccountResponse> out) {
        StaffBankAccount updated = staffBankAccountService.updateStaffBankAccount(
                bridge.id(request.getStaffBankAccount().getId(), "id"),
                bridge.requiredIfMatch(request.getStaffBankAccount().getEtag()),
                bridge.changes(request.getStaffBankAccount(), request.getUpdateMask(), StaffBankAccountUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStaffBankAccountResponse.newBuilder(),
                "staff_bank_account", updated).build());
    }

    @Override
    public void deleteStaffBankAccount(DeleteStaffBankAccountRequest request,
            StreamObserver<DeleteStaffBankAccountResponse> out) {
        staffBankAccountService.deleteStaffBankAccount(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStaffBankAccountResponse.getDefaultInstance());
    }
}
