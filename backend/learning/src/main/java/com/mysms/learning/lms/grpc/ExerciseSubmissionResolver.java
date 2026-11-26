package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ExerciseSubmission;
import com.mysms.learning.lms.api.model.ExerciseTestResult;
import com.mysms.learning.lms.business.ExerciseSubmissionService;
import com.mysms.lms.v1.ExerciseSubmissionServiceGrpc;
import com.mysms.lms.v1.GetExerciseSubmissionRequest;
import com.mysms.lms.v1.GetExerciseSubmissionResponse;
import com.mysms.lms.v1.GetExerciseTestResultRequest;
import com.mysms.lms.v1.GetExerciseTestResultResponse;
import com.mysms.lms.v1.ListExerciseSubmissionsRequest;
import com.mysms.lms.v1.ListExerciseSubmissionsResponse;
import com.mysms.lms.v1.ListExerciseTestResultsRequest;
import com.mysms.lms.v1.ListExerciseTestResultsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ExerciseSubmissionService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class ExerciseSubmissionResolver extends ExerciseSubmissionServiceGrpc.ExerciseSubmissionServiceImplBase {

    private final ExerciseSubmissionService service;
    private final ProtoBridge bridge;

    public ExerciseSubmissionResolver(ExerciseSubmissionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listExerciseSubmissions(ListExerciseSubmissionsRequest request,
            StreamObserver<ListExerciseSubmissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExerciseSubmission> rows = service.listExerciseSubmissions(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExerciseSubmissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExerciseSubmission(GetExerciseSubmissionRequest request, StreamObserver<GetExerciseSubmissionResponse> out) {
        ExerciseSubmission found = service.getExerciseSubmission(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExerciseSubmissionResponse.newBuilder(), "exercise_submission", found).build());
    }

    @Override
    public void listExerciseTestResults(ListExerciseTestResultsRequest request,
            StreamObserver<ListExerciseTestResultsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExerciseTestResult> rows = service.listExerciseTestResults(
                bridge.id(request.getExerciseSubmissionId(), "exerciseSubmissionId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExerciseTestResultsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExerciseTestResult(GetExerciseTestResultRequest request, StreamObserver<GetExerciseTestResultResponse> out) {
        ExerciseTestResult found = service.getExerciseTestResult(
                bridge.id(request.getExerciseSubmissionId(), "exerciseSubmissionId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExerciseTestResultResponse.newBuilder(), "exercise_test_result", found).build());
    }
}
