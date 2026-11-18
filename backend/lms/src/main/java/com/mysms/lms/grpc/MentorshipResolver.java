package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.Mentorship;
import com.mysms.lms.api.model.MentorshipUpdate;
import com.mysms.lms.business.MentorshipService;
import com.mysms.lms.v1.CreateMentorshipRequest;
import com.mysms.lms.v1.CreateMentorshipResponse;
import com.mysms.lms.v1.DeleteMentorshipRequest;
import com.mysms.lms.v1.DeleteMentorshipResponse;
import com.mysms.lms.v1.GetMentorshipRequest;
import com.mysms.lms.v1.GetMentorshipResponse;
import com.mysms.lms.v1.ListMentorshipsRequest;
import com.mysms.lms.v1.ListMentorshipsResponse;
import com.mysms.lms.v1.MentorshipServiceGrpc;
import com.mysms.lms.v1.UpdateMentorshipRequest;
import com.mysms.lms.v1.UpdateMentorshipResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.MentorshipService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MentorshipResolver extends MentorshipServiceGrpc.MentorshipServiceImplBase {

    private final MentorshipService service;
    private final ProtoBridge bridge;

    public MentorshipResolver(MentorshipService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listMentorships(ListMentorshipsRequest request, StreamObserver<ListMentorshipsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Mentorship> rows = service.listMentorships(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMentorshipsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMentorship(GetMentorshipRequest request, StreamObserver<GetMentorshipResponse> out) {
        Mentorship found = service.getMentorship(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMentorshipResponse.newBuilder(), "mentorship", found).build());
    }

    @Override
    public void createMentorship(CreateMentorshipRequest request, StreamObserver<CreateMentorshipResponse> out) {
        Mentorship created = service.createMentorship(
                bridge.body(request.getMentorship(), Mentorship.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMentorshipResponse.newBuilder(), "mentorship", created).build());
    }

    @Override
    public void updateMentorship(UpdateMentorshipRequest request, StreamObserver<UpdateMentorshipResponse> out) {
        Mentorship updated = service.updateMentorship(
                bridge.id(request.getMentorship().getId(), "id"),
                bridge.requiredIfMatch(request.getMentorship().getEtag()),
                bridge.changes(request.getMentorship(), request.getUpdateMask(), MentorshipUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMentorshipResponse.newBuilder(), "mentorship", updated).build());
    }

    @Override
    public void deleteMentorship(DeleteMentorshipRequest request, StreamObserver<DeleteMentorshipResponse> out) {
        service.deleteMentorship(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMentorshipResponse.getDefaultInstance());
    }
}
