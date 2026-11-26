package com.mysms.campus.transport.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.transport.api.model.StudentTransport;
import com.mysms.campus.transport.api.model.StudentTransportUpdate;
import com.mysms.campus.transport.business.StudentTransportService;
import com.mysms.transport.v1.CreateStudentTransportRequest;
import com.mysms.transport.v1.CreateStudentTransportResponse;
import com.mysms.transport.v1.DeleteStudentTransportRequest;
import com.mysms.transport.v1.DeleteStudentTransportResponse;
import com.mysms.transport.v1.GetStudentTransportRequest;
import com.mysms.transport.v1.GetStudentTransportResponse;
import com.mysms.transport.v1.ListStudentTransportsRequest;
import com.mysms.transport.v1.ListStudentTransportsResponse;
import com.mysms.transport.v1.StudentTransportServiceGrpc;
import com.mysms.transport.v1.UpdateStudentTransportRequest;
import com.mysms.transport.v1.UpdateStudentTransportResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of transport.StudentTransportService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentTransportResolver extends StudentTransportServiceGrpc.StudentTransportServiceImplBase {

    private final StudentTransportService service;
    private final ProtoBridge bridge;

    public StudentTransportResolver(StudentTransportService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStudentTransports(ListStudentTransportsRequest request, StreamObserver<ListStudentTransportsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentTransport> rows = service.listStudentTransports(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentTransportsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentTransport(GetStudentTransportRequest request, StreamObserver<GetStudentTransportResponse> out) {
        StudentTransport found = service.getStudentTransport(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentTransportResponse.newBuilder(), "student_transport", found).build());
    }

    @Override
    public void createStudentTransport(CreateStudentTransportRequest request,
            StreamObserver<CreateStudentTransportResponse> out) {
        StudentTransport created = service.createStudentTransport(
                bridge.body(request.getStudentTransport(), StudentTransport.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStudentTransportResponse.newBuilder(), "student_transport", created).build());
    }

    @Override
    public void updateStudentTransport(UpdateStudentTransportRequest request,
            StreamObserver<UpdateStudentTransportResponse> out) {
        StudentTransport updated = service.updateStudentTransport(bridge.id(request.getStudentTransport().getId(), "id"),
                bridge.requiredIfMatch(request.getStudentTransport().getEtag()),
                bridge.changes(request.getStudentTransport(), request.getUpdateMask(), StudentTransportUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentTransportResponse.newBuilder(), "student_transport", updated).build());
    }

    @Override
    public void deleteStudentTransport(DeleteStudentTransportRequest request,
            StreamObserver<DeleteStudentTransportResponse> out) {
        service.deleteStudentTransport(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentTransportResponse.getDefaultInstance());
    }
}
