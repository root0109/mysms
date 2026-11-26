package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Annotation;
import com.mysms.learning.lms.api.model.AnnotationShare;
import com.mysms.learning.lms.api.model.AnnotationShareUpdate;
import com.mysms.learning.lms.api.model.AnnotationUpdate;
import com.mysms.learning.lms.business.AnnotationService;
import com.mysms.lms.v1.AnnotationServiceGrpc;
import com.mysms.lms.v1.CreateAnnotationRequest;
import com.mysms.lms.v1.CreateAnnotationResponse;
import com.mysms.lms.v1.CreateAnnotationShareRequest;
import com.mysms.lms.v1.CreateAnnotationShareResponse;
import com.mysms.lms.v1.DeleteAnnotationRequest;
import com.mysms.lms.v1.DeleteAnnotationResponse;
import com.mysms.lms.v1.DeleteAnnotationShareRequest;
import com.mysms.lms.v1.DeleteAnnotationShareResponse;
import com.mysms.lms.v1.GetAnnotationRequest;
import com.mysms.lms.v1.GetAnnotationResponse;
import com.mysms.lms.v1.GetAnnotationShareRequest;
import com.mysms.lms.v1.GetAnnotationShareResponse;
import com.mysms.lms.v1.ListAnnotationSharesRequest;
import com.mysms.lms.v1.ListAnnotationSharesResponse;
import com.mysms.lms.v1.ListAnnotationsRequest;
import com.mysms.lms.v1.ListAnnotationsResponse;
import com.mysms.lms.v1.UpdateAnnotationRequest;
import com.mysms.lms.v1.UpdateAnnotationResponse;
import com.mysms.lms.v1.UpdateAnnotationShareRequest;
import com.mysms.lms.v1.UpdateAnnotationShareResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.AnnotationService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AnnotationResolver extends AnnotationServiceGrpc.AnnotationServiceImplBase {

    private final AnnotationService service;
    private final ProtoBridge bridge;

    public AnnotationResolver(AnnotationService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAnnotations(ListAnnotationsRequest request, StreamObserver<ListAnnotationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Annotation> rows = service.listAnnotations(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAnnotationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAnnotation(GetAnnotationRequest request, StreamObserver<GetAnnotationResponse> out) {
        Annotation found = service.getAnnotation(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAnnotationResponse.newBuilder(), "annotation", found).build());
    }

    @Override
    public void createAnnotation(CreateAnnotationRequest request, StreamObserver<CreateAnnotationResponse> out) {
        Annotation created = service.createAnnotation(bridge.body(request.getAnnotation(), Annotation.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAnnotationResponse.newBuilder(), "annotation", created).build());
    }

    @Override
    public void updateAnnotation(UpdateAnnotationRequest request, StreamObserver<UpdateAnnotationResponse> out) {
        Annotation updated = service.updateAnnotation(bridge.id(request.getAnnotation().getId(), "id"),
                bridge.requiredIfMatch(request.getAnnotation().getEtag()),
                bridge.changes(request.getAnnotation(), request.getUpdateMask(), AnnotationUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAnnotationResponse.newBuilder(), "annotation", updated).build());
    }

    @Override
    public void deleteAnnotation(DeleteAnnotationRequest request, StreamObserver<DeleteAnnotationResponse> out) {
        service.deleteAnnotation(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAnnotationResponse.getDefaultInstance());
    }

    @Override
    public void listAnnotationShares(ListAnnotationSharesRequest request, StreamObserver<ListAnnotationSharesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AnnotationShare> rows = service.listAnnotationShares(bridge.id(request.getAnnotationId(), "annotationId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAnnotationSharesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAnnotationShare(GetAnnotationShareRequest request, StreamObserver<GetAnnotationShareResponse> out) {
        AnnotationShare found = service.getAnnotationShare(bridge.id(request.getAnnotationId(), "annotationId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAnnotationShareResponse.newBuilder(), "annotation_share", found).build());
    }

    @Override
    public void createAnnotationShare(CreateAnnotationShareRequest request, StreamObserver<CreateAnnotationShareResponse> out) {
        AnnotationShare created = service.createAnnotationShare(bridge.id(request.getAnnotationId(), "annotationId"),
                bridge.body(request.getAnnotationShare(), AnnotationShare.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAnnotationShareResponse.newBuilder(), "annotation_share", created).build());
    }

    @Override
    public void updateAnnotationShare(UpdateAnnotationShareRequest request, StreamObserver<UpdateAnnotationShareResponse> out) {
        AnnotationShare updated = service.updateAnnotationShare(bridge.id(request.getAnnotationId(), "annotationId"),
                bridge.id(request.getAnnotationShare().getId(), "id"),
                bridge.requiredIfMatch(request.getAnnotationShare().getEtag()),
                bridge.changes(request.getAnnotationShare(), request.getUpdateMask(), AnnotationShareUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAnnotationShareResponse.newBuilder(), "annotation_share", updated).build());
    }

    @Override
    public void deleteAnnotationShare(DeleteAnnotationShareRequest request, StreamObserver<DeleteAnnotationShareResponse> out) {
        service.deleteAnnotationShare(bridge.id(request.getAnnotationId(), "annotationId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAnnotationShareResponse.getDefaultInstance());
    }
}
