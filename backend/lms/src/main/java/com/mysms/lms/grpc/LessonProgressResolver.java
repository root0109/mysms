package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.LessonProgress;
import com.mysms.lms.business.LessonProgressService;
import com.mysms.lms.v1.GetLessonProgressRequest;
import com.mysms.lms.v1.GetLessonProgressResponse;
import com.mysms.lms.v1.LessonProgressServiceGrpc;
import com.mysms.lms.v1.ListLessonProgressRequest;
import com.mysms.lms.v1.ListLessonProgressResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LessonProgressService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class LessonProgressResolver extends LessonProgressServiceGrpc.LessonProgressServiceImplBase {

    private final LessonProgressService service;
    private final ProtoBridge bridge;

    public LessonProgressResolver(LessonProgressService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLessonProgress(ListLessonProgressRequest request, StreamObserver<ListLessonProgressResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LessonProgress> rows = service.listLessonProgresss(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLessonProgressResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLessonProgress(GetLessonProgressRequest request, StreamObserver<GetLessonProgressResponse> out) {
        LessonProgress found = service.getLessonProgress(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLessonProgressResponse.newBuilder(), "lesson_progress", found).build());
    }
}
