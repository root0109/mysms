package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ForumSection;
import com.mysms.learning.lms.business.ForumSectionService;
import com.mysms.lms.v1.CreateForumSectionRequest;
import com.mysms.lms.v1.CreateForumSectionResponse;
import com.mysms.lms.v1.DeleteForumSectionRequest;
import com.mysms.lms.v1.DeleteForumSectionResponse;
import com.mysms.lms.v1.ForumSectionServiceGrpc;
import com.mysms.lms.v1.ForumSectionUpdate;
import com.mysms.lms.v1.GetForumSectionRequest;
import com.mysms.lms.v1.GetForumSectionResponse;
import com.mysms.lms.v1.ListForumSectionsRequest;
import com.mysms.lms.v1.ListForumSectionsResponse;
import com.mysms.lms.v1.UpdateForumSectionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ForumSectionService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ForumSectionResolver extends ForumSectionServiceGrpc.ForumSectionServiceImplBase {

    private final ForumSectionService service;
    private final ProtoBridge bridge;

    public ForumSectionResolver(ForumSectionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listForumSections(ListForumSectionsRequest request, StreamObserver<ListForumSectionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ForumSection> rows = service.listForumSections(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumSectionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getForumSection(GetForumSectionRequest request, StreamObserver<GetForumSectionResponse> out) {
        ForumSection found = service.getForumSection(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetForumSectionResponse.newBuilder(), "forum_section", found).build());
    }

    @Override
    public void createForumSection(CreateForumSectionRequest request, StreamObserver<CreateForumSectionResponse> out) {
        ForumSection created = service.createForumSection(
                bridge.body(request.getForumSection(), ForumSection.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateForumSectionResponse.newBuilder(), "forum_section", created).build());
    }

    @Override
    public void updateForumSection(ForumSectionUpdate request, StreamObserver<UpdateForumSectionResponse> out) {
        ForumSection updated = service.updateForumSection(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ForumSectionProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateForumSectionResponse.newBuilder(), "forum_section", updated).build());
    }

    @Override
    public void deleteForumSection(DeleteForumSectionRequest request, StreamObserver<DeleteForumSectionResponse> out) {
        service.deleteForumSection(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteForumSectionResponse.getDefaultInstance());
    }
}
