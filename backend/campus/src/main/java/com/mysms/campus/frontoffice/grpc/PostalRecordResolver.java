package com.mysms.campus.frontoffice.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.frontoffice.api.model.PostalRecord;
import com.mysms.campus.frontoffice.api.model.PostalRecordUpdate;
import com.mysms.campus.frontoffice.business.PostalRecordService;
import com.mysms.frontoffice.v1.CreatePostalRecordRequest;
import com.mysms.frontoffice.v1.CreatePostalRecordResponse;
import com.mysms.frontoffice.v1.DeletePostalRecordRequest;
import com.mysms.frontoffice.v1.DeletePostalRecordResponse;
import com.mysms.frontoffice.v1.GetPostalRecordRequest;
import com.mysms.frontoffice.v1.GetPostalRecordResponse;
import com.mysms.frontoffice.v1.ListPostalRecordsRequest;
import com.mysms.frontoffice.v1.ListPostalRecordsResponse;
import com.mysms.frontoffice.v1.PostalRecordServiceGrpc;
import com.mysms.frontoffice.v1.UpdatePostalRecordRequest;
import com.mysms.frontoffice.v1.UpdatePostalRecordResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of frontoffice.PostalRecordService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PostalRecordResolver extends PostalRecordServiceGrpc.PostalRecordServiceImplBase {

    private final PostalRecordService postalRecordService;
    private final ProtoBridge bridge;

    public PostalRecordResolver(PostalRecordService postalRecordService, ProtoBridge bridge) {
        this.postalRecordService = postalRecordService;
        this.bridge = bridge;
    }

    @Override
    public void listPostalRecords(ListPostalRecordsRequest request, StreamObserver<ListPostalRecordsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PostalRecord> rows = postalRecordService.listPostalRecords(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPostalRecordsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPostalRecord(GetPostalRecordRequest request, StreamObserver<GetPostalRecordResponse> out) {
        PostalRecord found = postalRecordService.getPostalRecord(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPostalRecordResponse.newBuilder(), "postal_record", found).build());
    }

    @Override
    public void createPostalRecord(CreatePostalRecordRequest request,
            StreamObserver<CreatePostalRecordResponse> out) {
        PostalRecord created = postalRecordService.createPostalRecord(
                bridge.body(request.getPostalRecord(), PostalRecord.class), request.getValidateOnly());
        ProtoBridge.send(
                out, bridge.reply(CreatePostalRecordResponse.newBuilder(), "postal_record", created).build());
    }

    @Override
    public void updatePostalRecord(UpdatePostalRecordRequest request,
            StreamObserver<UpdatePostalRecordResponse> out) {
        PostalRecord updated = postalRecordService.updatePostalRecord(
                bridge.id(request.getPostalRecord().getId(), "id"),
                bridge.requiredIfMatch(request.getPostalRecord().getEtag()),
                bridge.changes(request.getPostalRecord(), request.getUpdateMask(), PostalRecordUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(
                out, bridge.reply(UpdatePostalRecordResponse.newBuilder(), "postal_record", updated).build());
    }

    @Override
    public void deletePostalRecord(DeletePostalRecordRequest request,
            StreamObserver<DeletePostalRecordResponse> out) {
        postalRecordService.deletePostalRecord(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePostalRecordResponse.getDefaultInstance());
    }
}
