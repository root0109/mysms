package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.PlagiarismAccount;
import com.mysms.learning.lms.api.model.PlagiarismAccountUpdate;
import com.mysms.learning.lms.api.model.PlagiarismUserLink;
import com.mysms.learning.lms.api.model.PlagiarismUserLinkUpdate;
import com.mysms.learning.lms.business.PlagiarismAccountService;
import com.mysms.lms.v1.CreatePlagiarismAccountRequest;
import com.mysms.lms.v1.CreatePlagiarismAccountResponse;
import com.mysms.lms.v1.CreatePlagiarismUserLinkRequest;
import com.mysms.lms.v1.CreatePlagiarismUserLinkResponse;
import com.mysms.lms.v1.DeletePlagiarismAccountRequest;
import com.mysms.lms.v1.DeletePlagiarismAccountResponse;
import com.mysms.lms.v1.DeletePlagiarismUserLinkRequest;
import com.mysms.lms.v1.DeletePlagiarismUserLinkResponse;
import com.mysms.lms.v1.GetPlagiarismAccountRequest;
import com.mysms.lms.v1.GetPlagiarismAccountResponse;
import com.mysms.lms.v1.GetPlagiarismUserLinkRequest;
import com.mysms.lms.v1.GetPlagiarismUserLinkResponse;
import com.mysms.lms.v1.ListPlagiarismAccountsRequest;
import com.mysms.lms.v1.ListPlagiarismAccountsResponse;
import com.mysms.lms.v1.ListPlagiarismUserLinksRequest;
import com.mysms.lms.v1.ListPlagiarismUserLinksResponse;
import com.mysms.lms.v1.PlagiarismAccountServiceGrpc;
import com.mysms.lms.v1.UpdatePlagiarismAccountRequest;
import com.mysms.lms.v1.UpdatePlagiarismAccountResponse;
import com.mysms.lms.v1.UpdatePlagiarismUserLinkRequest;
import com.mysms.lms.v1.UpdatePlagiarismUserLinkResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.PlagiarismAccountService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PlagiarismAccountResolver extends PlagiarismAccountServiceGrpc.PlagiarismAccountServiceImplBase {

    private final PlagiarismAccountService service;
    private final ProtoBridge bridge;

    public PlagiarismAccountResolver(PlagiarismAccountService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPlagiarismAccounts(ListPlagiarismAccountsRequest request,
            StreamObserver<ListPlagiarismAccountsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PlagiarismAccount> rows = service.listPlagiarismAccounts(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPlagiarismAccountsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPlagiarismAccount(GetPlagiarismAccountRequest request,
            StreamObserver<GetPlagiarismAccountResponse> out) {
        PlagiarismAccount found = service.getPlagiarismAccount(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetPlagiarismAccountResponse.newBuilder(), "plagiarism_account", found).build());
    }

    @Override
    public void createPlagiarismAccount(CreatePlagiarismAccountRequest request,
            StreamObserver<CreatePlagiarismAccountResponse> out) {
        PlagiarismAccount created = service.createPlagiarismAccount(
                bridge.body(request.getPlagiarismAccount(), PlagiarismAccount.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreatePlagiarismAccountResponse.newBuilder(), "plagiarism_account", created).build());
    }

    @Override
    public void updatePlagiarismAccount(UpdatePlagiarismAccountRequest request,
            StreamObserver<UpdatePlagiarismAccountResponse> out) {
        PlagiarismAccount updated = service.updatePlagiarismAccount(
                bridge.id(request.getPlagiarismAccount().getId(), "id"),
                bridge.requiredIfMatch(request.getPlagiarismAccount().getEtag()),
                bridge.changes(request.getPlagiarismAccount(), request.getUpdateMask(), PlagiarismAccountUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdatePlagiarismAccountResponse.newBuilder(), "plagiarism_account", updated).build());
    }

    @Override
    public void deletePlagiarismAccount(DeletePlagiarismAccountRequest request,
            StreamObserver<DeletePlagiarismAccountResponse> out) {
        service.deletePlagiarismAccount(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePlagiarismAccountResponse.getDefaultInstance());
    }

    @Override
    public void listPlagiarismUserLinks(ListPlagiarismUserLinksRequest request,
            StreamObserver<ListPlagiarismUserLinksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PlagiarismUserLink> rows = service.listPlagiarismUserLinks(
                bridge.id(request.getPlagiarismAccountId(), "plagiarismAccountId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPlagiarismUserLinksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPlagiarismUserLink(GetPlagiarismUserLinkRequest request,
            StreamObserver<GetPlagiarismUserLinkResponse> out) {
        PlagiarismUserLink found = service.getPlagiarismUserLink(
                bridge.id(request.getPlagiarismAccountId(), "plagiarismAccountId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetPlagiarismUserLinkResponse.newBuilder(), "plagiarism_user_link", found).build());
    }

    @Override
    public void createPlagiarismUserLink(CreatePlagiarismUserLinkRequest request,
            StreamObserver<CreatePlagiarismUserLinkResponse> out) {
        PlagiarismUserLink created = service.createPlagiarismUserLink(
                bridge.id(request.getPlagiarismAccountId(), "plagiarismAccountId"),
                bridge.body(request.getPlagiarismUserLink(), PlagiarismUserLink.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreatePlagiarismUserLinkResponse.newBuilder(), "plagiarism_user_link", created).build());
    }

    @Override
    public void updatePlagiarismUserLink(UpdatePlagiarismUserLinkRequest request,
            StreamObserver<UpdatePlagiarismUserLinkResponse> out) {
        PlagiarismUserLink updated = service.updatePlagiarismUserLink(
                bridge.id(request.getPlagiarismAccountId(), "plagiarismAccountId"),
                bridge.id(request.getPlagiarismUserLink().getId(), "id"),
                bridge.requiredIfMatch(request.getPlagiarismUserLink().getEtag()),
                bridge.changes(request.getPlagiarismUserLink(), request.getUpdateMask(),
                        PlagiarismUserLinkUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdatePlagiarismUserLinkResponse.newBuilder(), "plagiarism_user_link", updated).build());
    }

    @Override
    public void deletePlagiarismUserLink(DeletePlagiarismUserLinkRequest request,
            StreamObserver<DeletePlagiarismUserLinkResponse> out) {
        service.deletePlagiarismUserLink(
                bridge.id(request.getPlagiarismAccountId(), "plagiarismAccountId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePlagiarismUserLinkResponse.getDefaultInstance());
    }
}
