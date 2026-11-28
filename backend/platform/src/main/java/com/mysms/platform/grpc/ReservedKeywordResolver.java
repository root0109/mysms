package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.ReservedKeyword;
import com.mysms.platform.business.ReservedKeywordService;
import com.mysms.platform.v1.CreateReservedKeywordRequest;
import com.mysms.platform.v1.CreateReservedKeywordResponse;
import com.mysms.platform.v1.DeleteReservedKeywordRequest;
import com.mysms.platform.v1.DeleteReservedKeywordResponse;
import com.mysms.platform.v1.GetReservedKeywordRequest;
import com.mysms.platform.v1.GetReservedKeywordResponse;
import com.mysms.platform.v1.ListReservedKeywordsRequest;
import com.mysms.platform.v1.ListReservedKeywordsResponse;
import com.mysms.platform.v1.ReservedKeywordServiceGrpc;
import com.mysms.platform.v1.ReservedKeywordUpdate;
import com.mysms.platform.v1.UpdateReservedKeywordResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.ReservedKeywordService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ReservedKeywordResolver extends ReservedKeywordServiceGrpc.ReservedKeywordServiceImplBase {

    private final ReservedKeywordService service;
    private final ProtoBridge bridge;

    public ReservedKeywordResolver(ReservedKeywordService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listReservedKeywords(ListReservedKeywordsRequest request,
            StreamObserver<ListReservedKeywordsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ReservedKeyword> rows = service.listReservedKeywords(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListReservedKeywordsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getReservedKeyword(GetReservedKeywordRequest request, StreamObserver<GetReservedKeywordResponse> out) {
        ReservedKeyword found = service.getReservedKeyword(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetReservedKeywordResponse.newBuilder(), "reserved_keyword", found).build());
    }

    @Override
    public void createReservedKeyword(CreateReservedKeywordRequest request,
            StreamObserver<CreateReservedKeywordResponse> out) {
        ReservedKeyword created = service.createReservedKeyword(
                bridge.body(request.getReservedKeyword(), ReservedKeyword.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateReservedKeywordResponse.newBuilder(), "reserved_keyword",
                created).build());
    }

    @Override
    public void updateReservedKeyword(ReservedKeywordUpdate request,
            StreamObserver<UpdateReservedKeywordResponse> out) {
        ReservedKeyword updated = service.updateReservedKeyword(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ReservedKeywordProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateReservedKeywordResponse.newBuilder(), "reserved_keyword",
                updated).build());
    }

    @Override
    public void deleteReservedKeyword(DeleteReservedKeywordRequest request,
            StreamObserver<DeleteReservedKeywordResponse> out) {
        service.deleteReservedKeyword(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteReservedKeywordResponse.getDefaultInstance());
    }
}
