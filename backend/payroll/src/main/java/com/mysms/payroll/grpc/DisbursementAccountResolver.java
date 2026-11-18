package com.mysms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.payroll.api.model.DisbursementAccount;
import com.mysms.payroll.api.model.DisbursementAccountUpdate;
import com.mysms.payroll.business.DisbursementAccountService;
import com.mysms.payroll.v1.CreateDisbursementAccountRequest;
import com.mysms.payroll.v1.CreateDisbursementAccountResponse;
import com.mysms.payroll.v1.DeleteDisbursementAccountRequest;
import com.mysms.payroll.v1.DeleteDisbursementAccountResponse;
import com.mysms.payroll.v1.DisbursementAccountServiceGrpc;
import com.mysms.payroll.v1.GetDisbursementAccountRequest;
import com.mysms.payroll.v1.GetDisbursementAccountResponse;
import com.mysms.payroll.v1.ListDisbursementAccountsRequest;
import com.mysms.payroll.v1.ListDisbursementAccountsResponse;
import com.mysms.payroll.v1.UpdateDisbursementAccountRequest;
import com.mysms.payroll.v1.UpdateDisbursementAccountResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.DisbursementAccountService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DisbursementAccountResolver extends DisbursementAccountServiceGrpc.DisbursementAccountServiceImplBase {

    private final DisbursementAccountService disbursementAccountService;
    private final ProtoBridge bridge;

    public DisbursementAccountResolver(DisbursementAccountService disbursementAccountService, ProtoBridge bridge) {
        this.disbursementAccountService = disbursementAccountService;
        this.bridge = bridge;
    }

    @Override
    public void listDisbursementAccounts(ListDisbursementAccountsRequest request,
            StreamObserver<ListDisbursementAccountsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<DisbursementAccount> rows = disbursementAccountService.listDisbursementAccounts(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDisbursementAccountsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDisbursementAccount(GetDisbursementAccountRequest request,
            StreamObserver<GetDisbursementAccountResponse> out) {
        DisbursementAccount found = disbursementAccountService.getDisbursementAccount(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetDisbursementAccountResponse.newBuilder(),
                "disbursement_account", found).build());
    }

    @Override
    public void createDisbursementAccount(CreateDisbursementAccountRequest request,
            StreamObserver<CreateDisbursementAccountResponse> out) {
        DisbursementAccount created = disbursementAccountService.createDisbursementAccount(
                bridge.body(request.getDisbursementAccount(), DisbursementAccount.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateDisbursementAccountResponse.newBuilder(),
                "disbursement_account", created).build());
    }

    @Override
    public void updateDisbursementAccount(UpdateDisbursementAccountRequest request,
            StreamObserver<UpdateDisbursementAccountResponse> out) {
        DisbursementAccount updated = disbursementAccountService.updateDisbursementAccount(
                bridge.id(request.getDisbursementAccount().getId(), "id"),
                bridge.requiredIfMatch(request.getDisbursementAccount().getEtag()),
                bridge.changes(request.getDisbursementAccount(), request.getUpdateMask(),
                        DisbursementAccountUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateDisbursementAccountResponse.newBuilder(),
                "disbursement_account", updated).build());
    }

    @Override
    public void deleteDisbursementAccount(DeleteDisbursementAccountRequest request,
            StreamObserver<DeleteDisbursementAccountResponse> out) {
        disbursementAccountService.deleteDisbursementAccount(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDisbursementAccountResponse.getDefaultInstance());
    }
}
