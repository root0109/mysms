package com.mysms.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.sis.api.model.StudentLeaveDecisionRequest;
import com.mysms.sis.api.model.StudentLeaveRequest;
import com.mysms.sis.api.model.StudentLeaveRequestUpdate;
import com.mysms.sis.business.StudentLeaveRequestService;
import com.mysms.sis.v1.ApproveStudentLeaveRequestRequest;
import com.mysms.sis.v1.ApproveStudentLeaveRequestResponse;
import com.mysms.sis.v1.CreateStudentLeaveRequestRequest;
import com.mysms.sis.v1.CreateStudentLeaveRequestResponse;
import com.mysms.sis.v1.DeleteStudentLeaveRequestRequest;
import com.mysms.sis.v1.DeleteStudentLeaveRequestResponse;
import com.mysms.sis.v1.GetStudentLeaveRequestRequest;
import com.mysms.sis.v1.GetStudentLeaveRequestResponse;
import com.mysms.sis.v1.ListStudentLeaveRequestsRequest;
import com.mysms.sis.v1.ListStudentLeaveRequestsResponse;
import com.mysms.sis.v1.RejectStudentLeaveRequestRequest;
import com.mysms.sis.v1.RejectStudentLeaveRequestResponse;
import com.mysms.sis.v1.StudentLeaveRequestServiceGrpc;
import com.mysms.sis.v1.UpdateStudentLeaveRequestRequest;
import com.mysms.sis.v1.UpdateStudentLeaveRequestResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.StudentLeaveRequestService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentLeaveRequestResolver extends StudentLeaveRequestServiceGrpc.StudentLeaveRequestServiceImplBase {

    private final StudentLeaveRequestService service;
    private final ProtoBridge bridge;

    public StudentLeaveRequestResolver(StudentLeaveRequestService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStudentLeaveRequests(ListStudentLeaveRequestsRequest request,
            StreamObserver<ListStudentLeaveRequestsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentLeaveRequest> rows = service.listStudentLeaveRequests(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentLeaveRequestsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentLeaveRequest(GetStudentLeaveRequestRequest request,
            StreamObserver<GetStudentLeaveRequestResponse> out) {
        StudentLeaveRequest found = service.getStudentLeaveRequest(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetStudentLeaveRequestResponse.newBuilder(), "student_leave_request", found).build());
    }

    @Override
    public void createStudentLeaveRequest(CreateStudentLeaveRequestRequest request,
            StreamObserver<CreateStudentLeaveRequestResponse> out) {
        StudentLeaveRequest created = service.createStudentLeaveRequest(
                bridge.body(request.getStudentLeaveRequest(), StudentLeaveRequest.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateStudentLeaveRequestResponse.newBuilder(), "student_leave_request", created).build());
    }

    @Override
    public void updateStudentLeaveRequest(UpdateStudentLeaveRequestRequest request,
            StreamObserver<UpdateStudentLeaveRequestResponse> out) {
        StudentLeaveRequest updated = service.updateStudentLeaveRequest(
                bridge.id(request.getStudentLeaveRequest().getId(), "id"),
                bridge.requiredIfMatch(request.getStudentLeaveRequest().getEtag()),
                bridge.changes(request.getStudentLeaveRequest(), request.getUpdateMask(), StudentLeaveRequestUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateStudentLeaveRequestResponse.newBuilder(), "student_leave_request", updated).build());
    }

    @Override
    public void deleteStudentLeaveRequest(DeleteStudentLeaveRequestRequest request,
            StreamObserver<DeleteStudentLeaveRequestResponse> out) {
        service.deleteStudentLeaveRequest(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentLeaveRequestResponse.getDefaultInstance());
    }

    @Override
    public void approveStudentLeaveRequest(ApproveStudentLeaveRequestRequest request,
            StreamObserver<ApproveStudentLeaveRequestResponse> out) {
        StudentLeaveRequest changed = service.approveStudentLeaveRequest(
                bridge.id(request.getStudentLeaveRequestId(), "studentLeaveRequestId"),
                bridge.requiredIfMatch(request.getEtag()),
                bridge.body(request, StudentLeaveDecisionRequest.class, "student_leave_request_id", "etag",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(
                ApproveStudentLeaveRequestResponse.newBuilder(), "student_leave_request", changed).build());
    }

    @Override
    public void rejectStudentLeaveRequest(RejectStudentLeaveRequestRequest request,
            StreamObserver<RejectStudentLeaveRequestResponse> out) {
        StudentLeaveRequest changed = service.rejectStudentLeaveRequest(
                bridge.id(request.getStudentLeaveRequestId(), "studentLeaveRequestId"),
                bridge.requiredIfMatch(request.getEtag()),
                bridge.body(request, StudentLeaveDecisionRequest.class, "student_leave_request_id", "etag",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(
                RejectStudentLeaveRequestResponse.newBuilder(), "student_leave_request", changed).build());
    }
}
