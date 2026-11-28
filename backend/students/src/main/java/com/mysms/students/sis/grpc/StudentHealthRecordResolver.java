package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.students.sis.api.model.StudentHealthRecord;
import com.mysms.students.sis.business.StudentHealthRecordService;
import com.mysms.sis.v1.CreateStudentHealthRecordRequest;
import com.mysms.sis.v1.CreateStudentHealthRecordResponse;
import com.mysms.sis.v1.DeleteStudentHealthRecordRequest;
import com.mysms.sis.v1.DeleteStudentHealthRecordResponse;
import com.mysms.sis.v1.GetStudentHealthRecordRequest;
import com.mysms.sis.v1.GetStudentHealthRecordResponse;
import com.mysms.sis.v1.ListStudentHealthRecordsRequest;
import com.mysms.sis.v1.ListStudentHealthRecordsResponse;
import com.mysms.sis.v1.StudentHealthRecordServiceGrpc;
import com.mysms.sis.v1.StudentHealthRecordUpdate;
import com.mysms.sis.v1.UpdateStudentHealthRecordResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.StudentHealthRecordService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentHealthRecordResolver extends StudentHealthRecordServiceGrpc.StudentHealthRecordServiceImplBase {

    private final StudentHealthRecordService service;
    private final ProtoBridge bridge;

    public StudentHealthRecordResolver(StudentHealthRecordService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStudentHealthRecords(ListStudentHealthRecordsRequest request,
            StreamObserver<ListStudentHealthRecordsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentHealthRecord> rows = service.listStudentHealthRecords(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentHealthRecordsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentHealthRecord(GetStudentHealthRecordRequest request,
            StreamObserver<GetStudentHealthRecordResponse> out) {
        StudentHealthRecord found = service.getStudentHealthRecord(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetStudentHealthRecordResponse.newBuilder(), "student_health_record", found).build());
    }

    @Override
    public void createStudentHealthRecord(CreateStudentHealthRecordRequest request,
            StreamObserver<CreateStudentHealthRecordResponse> out) {
        StudentHealthRecord created = service.createStudentHealthRecord(
                bridge.body(request.getStudentHealthRecord(), StudentHealthRecord.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateStudentHealthRecordResponse.newBuilder(), "student_health_record", created).build());
    }

    @Override
    public void updateStudentHealthRecord(StudentHealthRecordUpdate request,
            StreamObserver<UpdateStudentHealthRecordResponse> out) {
        StudentHealthRecord updated = service.updateStudentHealthRecord(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                StudentHealthRecordProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateStudentHealthRecordResponse.newBuilder(), "student_health_record", updated).build());
    }

    @Override
    public void deleteStudentHealthRecord(DeleteStudentHealthRecordRequest request,
            StreamObserver<DeleteStudentHealthRecordResponse> out) {
        service.deleteStudentHealthRecord(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentHealthRecordResponse.getDefaultInstance());
    }
}
