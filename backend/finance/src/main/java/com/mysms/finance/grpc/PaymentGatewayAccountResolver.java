package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.PaymentGatewayAccount;
import com.mysms.finance.api.model.PaymentGatewayAccountUpdate;
import com.mysms.finance.business.PaymentGatewayAccountService;
import com.mysms.finance.v1.CreatePaymentGatewayAccountRequest;
import com.mysms.finance.v1.CreatePaymentGatewayAccountResponse;
import com.mysms.finance.v1.DeletePaymentGatewayAccountRequest;
import com.mysms.finance.v1.DeletePaymentGatewayAccountResponse;
import com.mysms.finance.v1.GetPaymentGatewayAccountRequest;
import com.mysms.finance.v1.GetPaymentGatewayAccountResponse;
import com.mysms.finance.v1.ListPaymentGatewayAccountsRequest;
import com.mysms.finance.v1.ListPaymentGatewayAccountsResponse;
import com.mysms.finance.v1.PaymentGatewayAccountServiceGrpc;
import com.mysms.finance.v1.UpdatePaymentGatewayAccountRequest;
import com.mysms.finance.v1.UpdatePaymentGatewayAccountResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.PaymentGatewayAccountService: delegates every RPC to the business service REST uses, so the
 * rules exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PaymentGatewayAccountResolver
        extends PaymentGatewayAccountServiceGrpc.PaymentGatewayAccountServiceImplBase {

    private final PaymentGatewayAccountService service;
    private final ProtoBridge bridge;

    public PaymentGatewayAccountResolver(PaymentGatewayAccountService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPaymentGatewayAccounts(ListPaymentGatewayAccountsRequest request,
            StreamObserver<ListPaymentGatewayAccountsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PaymentGatewayAccount> rows = service.listPaymentGatewayAccounts(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPaymentGatewayAccountsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPaymentGatewayAccount(GetPaymentGatewayAccountRequest request,
            StreamObserver<GetPaymentGatewayAccountResponse> out) {
        PaymentGatewayAccount found = service.getPaymentGatewayAccount(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPaymentGatewayAccountResponse.newBuilder(), "payment_gateway_account",
                found).build());
    }

    @Override
    public void createPaymentGatewayAccount(CreatePaymentGatewayAccountRequest request,
            StreamObserver<CreatePaymentGatewayAccountResponse> out) {
        PaymentGatewayAccount created = service.createPaymentGatewayAccount(
                bridge.body(request.getPaymentGatewayAccount(), PaymentGatewayAccount.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePaymentGatewayAccountResponse.newBuilder(), "payment_gateway_account",
                created).build());
    }

    @Override
    public void updatePaymentGatewayAccount(UpdatePaymentGatewayAccountRequest request,
            StreamObserver<UpdatePaymentGatewayAccountResponse> out) {
        PaymentGatewayAccount updated = service.updatePaymentGatewayAccount(
                bridge.id(request.getPaymentGatewayAccount().getId(), "id"),
                bridge.requiredIfMatch(request.getPaymentGatewayAccount().getEtag()),
                bridge.changes(request.getPaymentGatewayAccount(), request.getUpdateMask(),
                        PaymentGatewayAccountUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePaymentGatewayAccountResponse.newBuilder(), "payment_gateway_account",
                updated).build());
    }

    @Override
    public void deletePaymentGatewayAccount(DeletePaymentGatewayAccountRequest request,
            StreamObserver<DeletePaymentGatewayAccountResponse> out) {
        service.deletePaymentGatewayAccount(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePaymentGatewayAccountResponse.getDefaultInstance());
    }
}
