package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.Operation;
import com.mysms.finance.api.model.StudentFeeDue;
import com.mysms.finance.api.model.StudentFeeDueUpdate;
import com.mysms.finance.business.StudentFeeDueService;
import com.mysms.finance.v1.GenerateFeeDuesRequest;
import com.mysms.finance.v1.GenerateFeeDuesResponse;
import com.mysms.finance.v1.GetStudentFeeDueRequest;
import com.mysms.finance.v1.GetStudentFeeDueResponse;
import com.mysms.finance.v1.ListStudentFeeDuesRequest;
import com.mysms.finance.v1.ListStudentFeeDuesResponse;
import com.mysms.finance.v1.StudentFeeDueServiceGrpc;
import com.mysms.finance.v1.UpdateStudentFeeDueRequest;
import com.mysms.finance.v1.UpdateStudentFeeDueResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.StudentFeeDueService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency (the fee run's required {@code request_id}) and error mapping
 * happen at the gRPC edge.
 */
@GrpcService
public class StudentFeeDueResolver extends StudentFeeDueServiceGrpc.StudentFeeDueServiceImplBase {

    private final StudentFeeDueService service;
    private final ProtoBridge bridge;

    public StudentFeeDueResolver(StudentFeeDueService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStudentFeeDues(ListStudentFeeDuesRequest request, StreamObserver<ListStudentFeeDuesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentFeeDue> rows = service.listStudentFeeDues(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentFeeDuesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentFeeDue(GetStudentFeeDueRequest request, StreamObserver<GetStudentFeeDueResponse> out) {
        StudentFeeDue found = service.getStudentFeeDue(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentFeeDueResponse.newBuilder(), "student_fee_due", found).build());
    }

    @Override
    public void updateStudentFeeDue(UpdateStudentFeeDueRequest request,
            StreamObserver<UpdateStudentFeeDueResponse> out) {
        StudentFeeDue updated = service.updateStudentFeeDue(
                bridge.id(request.getStudentFeeDue().getId(), "id"),
                bridge.requiredIfMatch(request.getStudentFeeDue().getEtag()),
                bridge.changes(request.getStudentFeeDue(), request.getUpdateMask(), StudentFeeDueUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentFeeDueResponse.newBuilder(), "student_fee_due",
                updated).build());
    }

    /** finance.GenerateFeeDues: the fee run's operation, as REST's 202 answers it. */
    @Override
    public void generateFeeDues(GenerateFeeDuesRequest request, StreamObserver<GenerateFeeDuesResponse> out) {
        Operation run = service.generateFeeDues(
                bridge.body(request, com.mysms.finance.api.model.GenerateFeeDuesRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(GenerateFeeDuesResponse.newBuilder(), "result", run).build());
    }
}
