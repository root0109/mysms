package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.StudentAlert;
import com.mysms.learning.lms.business.StudentAlertService;
import com.mysms.lms.v1.CreateStudentAlertRequest;
import com.mysms.lms.v1.CreateStudentAlertResponse;
import com.mysms.lms.v1.DeleteStudentAlertRequest;
import com.mysms.lms.v1.DeleteStudentAlertResponse;
import com.mysms.lms.v1.GetStudentAlertRequest;
import com.mysms.lms.v1.GetStudentAlertResponse;
import com.mysms.lms.v1.ListStudentAlertsRequest;
import com.mysms.lms.v1.ListStudentAlertsResponse;
import com.mysms.lms.v1.StudentAlertServiceGrpc;
import com.mysms.lms.v1.StudentAlertUpdate;
import com.mysms.lms.v1.UpdateStudentAlertResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.StudentAlertService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentAlertResolver extends StudentAlertServiceGrpc.StudentAlertServiceImplBase {

    private final StudentAlertService service;
    private final ProtoBridge bridge;

    public StudentAlertResolver(StudentAlertService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStudentAlerts(ListStudentAlertsRequest request, StreamObserver<ListStudentAlertsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentAlert> rows = service.listStudentAlerts(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentAlertsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentAlert(GetStudentAlertRequest request, StreamObserver<GetStudentAlertResponse> out) {
        StudentAlert found = service.getStudentAlert(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentAlertResponse.newBuilder(), "student_alert", found).build());
    }

    @Override
    public void createStudentAlert(CreateStudentAlertRequest request, StreamObserver<CreateStudentAlertResponse> out) {
        StudentAlert created = service.createStudentAlert(
                bridge.body(request.getStudentAlert(), StudentAlert.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStudentAlertResponse.newBuilder(), "student_alert", created).build());
    }

    @Override
    public void updateStudentAlert(StudentAlertUpdate request, StreamObserver<UpdateStudentAlertResponse> out) {
        StudentAlert updated = service.updateStudentAlert(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                StudentAlertProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentAlertResponse.newBuilder(), "student_alert", updated).build());
    }

    @Override
    public void deleteStudentAlert(DeleteStudentAlertRequest request, StreamObserver<DeleteStudentAlertResponse> out) {
        service.deleteStudentAlert(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentAlertResponse.getDefaultInstance());
    }
}
