package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.identity.people.api.model.Qualification;
import com.mysms.identity.people.business.QualificationService;
import com.mysms.people.v1.CreateQualificationRequest;
import com.mysms.people.v1.CreateQualificationResponse;
import com.mysms.people.v1.DeleteQualificationRequest;
import com.mysms.people.v1.DeleteQualificationResponse;
import com.mysms.people.v1.GetQualificationRequest;
import com.mysms.people.v1.GetQualificationResponse;
import com.mysms.people.v1.ListQualificationsRequest;
import com.mysms.people.v1.ListQualificationsResponse;
import com.mysms.people.v1.QualificationServiceGrpc;
import com.mysms.people.v1.QualificationUpdate;
import com.mysms.people.v1.UpdateQualificationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of people.QualificationService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class QualificationResolver extends QualificationServiceGrpc.QualificationServiceImplBase {

    private final QualificationService service;
    private final ProtoBridge bridge;

    public QualificationResolver(QualificationService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listQualifications(ListQualificationsRequest request, StreamObserver<ListQualificationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Qualification> rows = service.listQualifications(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQualificationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQualification(GetQualificationRequest request, StreamObserver<GetQualificationResponse> out) {
        Qualification found = service.getQualification(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQualificationResponse.newBuilder(), "qualification", found).build());
    }

    @Override
    public void createQualification(CreateQualificationRequest request, StreamObserver<CreateQualificationResponse> out) {
        Qualification created = service.createQualification(
                bridge.body(request.getQualification(), Qualification.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQualificationResponse.newBuilder(), "qualification", created).build());
    }

    @Override
    public void updateQualification(QualificationUpdate request, StreamObserver<UpdateQualificationResponse> out) {
        Qualification updated = service.updateQualification(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                QualificationProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQualificationResponse.newBuilder(), "qualification", updated).build());
    }

    @Override
    public void deleteQualification(DeleteQualificationRequest request, StreamObserver<DeleteQualificationResponse> out) {
        service.deleteQualification(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQualificationResponse.getDefaultInstance());
    }
}
