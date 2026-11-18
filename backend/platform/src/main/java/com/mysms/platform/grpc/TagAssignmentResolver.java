package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.TagAssignment;
import com.mysms.platform.business.TagAssignmentService;
import com.mysms.platform.v1.CreateTagAssignmentRequest;
import com.mysms.platform.v1.CreateTagAssignmentResponse;
import com.mysms.platform.v1.DeleteTagAssignmentRequest;
import com.mysms.platform.v1.DeleteTagAssignmentResponse;
import com.mysms.platform.v1.ListTagAssignmentsRequest;
import com.mysms.platform.v1.ListTagAssignmentsResponse;
import com.mysms.platform.v1.TagAssignmentServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.TagAssignmentService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class TagAssignmentResolver extends TagAssignmentServiceGrpc.TagAssignmentServiceImplBase {

    private final TagAssignmentService service;
    private final ProtoBridge bridge;

    public TagAssignmentResolver(TagAssignmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTagAssignments(ListTagAssignmentsRequest request, StreamObserver<ListTagAssignmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TagAssignment> rows = service.listTagAssignments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTagAssignmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void createTagAssignment(CreateTagAssignmentRequest request,
            StreamObserver<CreateTagAssignmentResponse> out) {
        TagAssignment created = service.createTagAssignment(
                bridge.body(request.getTagAssignment(), TagAssignment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTagAssignmentResponse.newBuilder(), "tag_assignment",
                created).build());
    }

    @Override
    public void deleteTagAssignment(DeleteTagAssignmentRequest request,
            StreamObserver<DeleteTagAssignmentResponse> out) {
        service.deleteTagAssignment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTagAssignmentResponse.getDefaultInstance());
    }
}
