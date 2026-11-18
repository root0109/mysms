package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.DiscussionPrompt;
import com.mysms.lms.api.model.DiscussionPromptUpdate;
import com.mysms.lms.business.DiscussionPromptService;
import com.mysms.lms.v1.CreateDiscussionPromptRequest;
import com.mysms.lms.v1.CreateDiscussionPromptResponse;
import com.mysms.lms.v1.DeleteDiscussionPromptRequest;
import com.mysms.lms.v1.DeleteDiscussionPromptResponse;
import com.mysms.lms.v1.DiscussionPromptServiceGrpc;
import com.mysms.lms.v1.GetDiscussionPromptRequest;
import com.mysms.lms.v1.GetDiscussionPromptResponse;
import com.mysms.lms.v1.ListDiscussionPromptsRequest;
import com.mysms.lms.v1.ListDiscussionPromptsResponse;
import com.mysms.lms.v1.UpdateDiscussionPromptRequest;
import com.mysms.lms.v1.UpdateDiscussionPromptResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.DiscussionPromptService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DiscussionPromptResolver extends DiscussionPromptServiceGrpc.DiscussionPromptServiceImplBase {

    private final DiscussionPromptService service;
    private final ProtoBridge bridge;

    public DiscussionPromptResolver(DiscussionPromptService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listDiscussionPrompts(ListDiscussionPromptsRequest request, StreamObserver<ListDiscussionPromptsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<DiscussionPrompt> rows = service.listDiscussionPrompts(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDiscussionPromptsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDiscussionPrompt(GetDiscussionPromptRequest request, StreamObserver<GetDiscussionPromptResponse> out) {
        DiscussionPrompt found = service.getDiscussionPrompt(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetDiscussionPromptResponse.newBuilder(), "discussion_prompt", found).build());
    }

    @Override
    public void createDiscussionPrompt(CreateDiscussionPromptRequest request,
            StreamObserver<CreateDiscussionPromptResponse> out) {
        DiscussionPrompt created = service.createDiscussionPrompt(
                bridge.body(request.getDiscussionPrompt(), DiscussionPrompt.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateDiscussionPromptResponse.newBuilder(), "discussion_prompt", created).build());
    }

    @Override
    public void updateDiscussionPrompt(UpdateDiscussionPromptRequest request,
            StreamObserver<UpdateDiscussionPromptResponse> out) {
        DiscussionPrompt updated = service.updateDiscussionPrompt(bridge.id(request.getDiscussionPrompt().getId(), "id"),
                bridge.requiredIfMatch(request.getDiscussionPrompt().getEtag()),
                bridge.changes(request.getDiscussionPrompt(), request.getUpdateMask(), DiscussionPromptUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateDiscussionPromptResponse.newBuilder(), "discussion_prompt", updated).build());
    }

    @Override
    public void deleteDiscussionPrompt(DeleteDiscussionPromptRequest request,
            StreamObserver<DeleteDiscussionPromptResponse> out) {
        service.deleteDiscussionPrompt(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDiscussionPromptResponse.getDefaultInstance());
    }
}
