package com.mysms.campus.frontoffice.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.frontoffice.api.model.DirectoryContact;
import com.mysms.campus.frontoffice.api.model.DirectoryContactUpdate;
import com.mysms.campus.frontoffice.business.DirectoryContactService;
import com.mysms.frontoffice.v1.CreateDirectoryContactRequest;
import com.mysms.frontoffice.v1.CreateDirectoryContactResponse;
import com.mysms.frontoffice.v1.DeleteDirectoryContactRequest;
import com.mysms.frontoffice.v1.DeleteDirectoryContactResponse;
import com.mysms.frontoffice.v1.DirectoryContactServiceGrpc;
import com.mysms.frontoffice.v1.GetDirectoryContactRequest;
import com.mysms.frontoffice.v1.GetDirectoryContactResponse;
import com.mysms.frontoffice.v1.ListDirectoryContactsRequest;
import com.mysms.frontoffice.v1.ListDirectoryContactsResponse;
import com.mysms.frontoffice.v1.UpdateDirectoryContactRequest;
import com.mysms.frontoffice.v1.UpdateDirectoryContactResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of frontoffice.DirectoryContactService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DirectoryContactResolver extends DirectoryContactServiceGrpc.DirectoryContactServiceImplBase {

    private final DirectoryContactService directoryContactService;
    private final ProtoBridge bridge;

    public DirectoryContactResolver(DirectoryContactService directoryContactService, ProtoBridge bridge) {
        this.directoryContactService = directoryContactService;
        this.bridge = bridge;
    }

    @Override
    public void listDirectoryContacts(ListDirectoryContactsRequest request,
            StreamObserver<ListDirectoryContactsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<DirectoryContact> rows = directoryContactService.listDirectoryContacts(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDirectoryContactsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDirectoryContact(GetDirectoryContactRequest request,
            StreamObserver<GetDirectoryContactResponse> out) {
        DirectoryContact found = directoryContactService.getDirectoryContact(bridge.id(request.getId(), "id"));
        ProtoBridge.send(
                out, bridge.reply(GetDirectoryContactResponse.newBuilder(), "directory_contact", found).build());
    }

    @Override
    public void createDirectoryContact(CreateDirectoryContactRequest request,
            StreamObserver<CreateDirectoryContactResponse> out) {
        DirectoryContact created = directoryContactService.createDirectoryContact(
                bridge.body(request.getDirectoryContact(), DirectoryContact.class), request.getValidateOnly());
        ProtoBridge.send(
                out, bridge.reply(CreateDirectoryContactResponse.newBuilder(), "directory_contact", created).build());
    }

    @Override
    public void updateDirectoryContact(UpdateDirectoryContactRequest request,
            StreamObserver<UpdateDirectoryContactResponse> out) {
        DirectoryContact updated = directoryContactService.updateDirectoryContact(
                bridge.id(request.getDirectoryContact().getId(), "id"),
                bridge.requiredIfMatch(request.getDirectoryContact().getEtag()),
                bridge.changes(request.getDirectoryContact(), request.getUpdateMask(), DirectoryContactUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(
                out, bridge.reply(UpdateDirectoryContactResponse.newBuilder(), "directory_contact", updated).build());
    }

    @Override
    public void deleteDirectoryContact(DeleteDirectoryContactRequest request,
            StreamObserver<DeleteDirectoryContactResponse> out) {
        directoryContactService.deleteDirectoryContact(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDirectoryContactResponse.getDefaultInstance());
    }
}
