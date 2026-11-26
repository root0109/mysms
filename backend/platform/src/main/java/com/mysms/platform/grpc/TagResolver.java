package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.Tag;
import com.mysms.platform.api.model.TagUpdate;
import com.mysms.platform.business.TagService;
import com.mysms.platform.v1.CreateTagRequest;
import com.mysms.platform.v1.CreateTagResponse;
import com.mysms.platform.v1.DeleteTagRequest;
import com.mysms.platform.v1.DeleteTagResponse;
import com.mysms.platform.v1.GetTagRequest;
import com.mysms.platform.v1.GetTagResponse;
import com.mysms.platform.v1.ListTagsRequest;
import com.mysms.platform.v1.ListTagsResponse;
import com.mysms.platform.v1.TagServiceGrpc;
import com.mysms.platform.v1.UpdateTagRequest;
import com.mysms.platform.v1.UpdateTagResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.TagService: delegates every RPC to the business service REST uses, so the rules exist once.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class TagResolver extends TagServiceGrpc.TagServiceImplBase {

    private final TagService service;
    private final ProtoBridge bridge;

    public TagResolver(TagService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTags(ListTagsRequest request, StreamObserver<ListTagsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Tag> rows = service.listTags(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTagsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTag(GetTagRequest request, StreamObserver<GetTagResponse> out) {
        Tag found = service.getTag(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTagResponse.newBuilder(), "tag", found).build());
    }

    @Override
    public void createTag(CreateTagRequest request, StreamObserver<CreateTagResponse> out) {
        Tag created = service.createTag(
                bridge.body(request.getTag(), Tag.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTagResponse.newBuilder(), "tag", created).build());
    }

    @Override
    public void updateTag(UpdateTagRequest request, StreamObserver<UpdateTagResponse> out) {
        Tag updated = service.updateTag(
                bridge.id(request.getTag().getId(), "id"),
                bridge.requiredIfMatch(request.getTag().getEtag()),
                bridge.changes(request.getTag(), request.getUpdateMask(), TagUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateTagResponse.newBuilder(), "tag", updated).build());
    }

    @Override
    public void deleteTag(DeleteTagRequest request, StreamObserver<DeleteTagResponse> out) {
        service.deleteTag(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTagResponse.getDefaultInstance());
    }
}
