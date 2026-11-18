package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.ConferencingAccount;
import com.mysms.lms.api.model.ConferencingAccountUpdate;
import com.mysms.lms.business.ConferencingAccountService;
import com.mysms.lms.business.ConferencingWebhookService;
import com.mysms.lms.v1.ConferencingAccountServiceGrpc;
import com.mysms.lms.v1.CreateConferencingAccountRequest;
import com.mysms.lms.v1.CreateConferencingAccountResponse;
import com.mysms.lms.v1.DeleteConferencingAccountRequest;
import com.mysms.lms.v1.DeleteConferencingAccountResponse;
import com.mysms.lms.v1.GetConferencingAccountRequest;
import com.mysms.lms.v1.GetConferencingAccountResponse;
import com.mysms.lms.v1.ListConferencingAccountsRequest;
import com.mysms.lms.v1.ListConferencingAccountsResponse;
import com.mysms.lms.v1.ReceiveConferencingWebhookRequest;
import com.mysms.lms.v1.ReceiveConferencingWebhookResponse;
import com.mysms.lms.v1.UpdateConferencingAccountRequest;
import com.mysms.lms.v1.UpdateConferencingAccountResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ConferencingAccountService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class ConferencingAccountResolver extends ConferencingAccountServiceGrpc.ConferencingAccountServiceImplBase {

    private final ConferencingAccountService service;
    private final ConferencingWebhookService webhooks;
    private final ProtoBridge bridge;

    public ConferencingAccountResolver(ConferencingAccountService service, ConferencingWebhookService webhooks,
            ProtoBridge bridge) {
        this.service = service;
        this.webhooks = webhooks;
        this.bridge = bridge;
    }

    @Override
    public void listConferencingAccounts(ListConferencingAccountsRequest request,
            StreamObserver<ListConferencingAccountsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ConferencingAccount> rows = service.listConferencingAccounts(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListConferencingAccountsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getConferencingAccount(GetConferencingAccountRequest request,
            StreamObserver<GetConferencingAccountResponse> out) {
        ConferencingAccount found = service.getConferencingAccount(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetConferencingAccountResponse.newBuilder(), "conferencing_account", found).build());
    }

    @Override
    public void createConferencingAccount(CreateConferencingAccountRequest request,
            StreamObserver<CreateConferencingAccountResponse> out) {
        ConferencingAccount created = service.createConferencingAccount(
                bridge.body(request.getConferencingAccount(), ConferencingAccount.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateConferencingAccountResponse.newBuilder(), "conferencing_account", created).build());
    }

    @Override
    public void updateConferencingAccount(UpdateConferencingAccountRequest request,
            StreamObserver<UpdateConferencingAccountResponse> out) {
        ConferencingAccount updated = service.updateConferencingAccount(bridge.id(request.getConferencingAccount().getId(), "id"),
                bridge.requiredIfMatch(request.getConferencingAccount().getEtag()),
                bridge.changes(request.getConferencingAccount(), request.getUpdateMask(), ConferencingAccountUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateConferencingAccountResponse.newBuilder(), "conferencing_account", updated).build());
    }

    @Override
    public void deleteConferencingAccount(DeleteConferencingAccountRequest request,
            StreamObserver<DeleteConferencingAccountResponse> out) {
        service.deleteConferencingAccount(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteConferencingAccountResponse.getDefaultInstance());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void receiveConferencingWebhook(ReceiveConferencingWebhookRequest request,
            StreamObserver<ReceiveConferencingWebhookResponse> out) {
        webhooks.receiveConferencingWebhook(request.getProvider(),
                request.getHeadersOrDefault("x-signature", null), request.getHeadersOrDefault("x-timestamp", null),
                request.getRawBody().toByteArray());
        ProtoBridge.send(out, ReceiveConferencingWebhookResponse.getDefaultInstance());
    }
}
