package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.ExerciseSubmissionService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ExerciseTestCase;
import com.mysms.learning.lms.api.model.ExerciseTestCaseUpdate;
import com.mysms.learning.lms.api.model.ProgrammingExercise;
import com.mysms.learning.lms.api.model.ProgrammingExerciseUpdate;
import com.mysms.learning.lms.business.ProgrammingExerciseService;
import com.mysms.lms.v1.CreateExerciseTestCaseRequest;
import com.mysms.lms.v1.CreateExerciseTestCaseResponse;
import com.mysms.lms.v1.CreateProgrammingExerciseRequest;
import com.mysms.lms.v1.CreateProgrammingExerciseResponse;
import com.mysms.lms.v1.DeleteExerciseTestCaseRequest;
import com.mysms.lms.v1.DeleteExerciseTestCaseResponse;
import com.mysms.lms.v1.DeleteProgrammingExerciseRequest;
import com.mysms.lms.v1.DeleteProgrammingExerciseResponse;
import com.mysms.lms.v1.GetExerciseTestCaseRequest;
import com.mysms.lms.v1.GetExerciseTestCaseResponse;
import com.mysms.lms.v1.GetProgrammingExerciseRequest;
import com.mysms.lms.v1.GetProgrammingExerciseResponse;
import com.mysms.lms.v1.ListExerciseTestCasesRequest;
import com.mysms.lms.v1.ListExerciseTestCasesResponse;
import com.mysms.lms.v1.ListProgrammingExercisesRequest;
import com.mysms.lms.v1.ListProgrammingExercisesResponse;
import com.mysms.lms.v1.ProgrammingExerciseServiceGrpc;
import com.mysms.lms.v1.SubmitExerciseRequest;
import com.mysms.lms.v1.SubmitExerciseResponse;
import com.mysms.lms.v1.UpdateExerciseTestCaseRequest;
import com.mysms.lms.v1.UpdateExerciseTestCaseResponse;
import com.mysms.lms.v1.UpdateProgrammingExerciseRequest;
import com.mysms.lms.v1.UpdateProgrammingExerciseResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ProgrammingExerciseService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class ProgrammingExerciseResolver extends ProgrammingExerciseServiceGrpc.ProgrammingExerciseServiceImplBase {

    private final ProgrammingExerciseService service;
    private final ProtoBridge bridge;
    private final ExerciseSubmissionService submissions;

    public ProgrammingExerciseResolver(ProgrammingExerciseService service, ProtoBridge bridge,
            ExerciseSubmissionService submissions) {
        this.submissions = submissions;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listProgrammingExercises(ListProgrammingExercisesRequest request,
            StreamObserver<ListProgrammingExercisesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ProgrammingExercise> rows = service.listProgrammingExercises(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListProgrammingExercisesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getProgrammingExercise(GetProgrammingExerciseRequest request,
            StreamObserver<GetProgrammingExerciseResponse> out) {
        ProgrammingExercise found = service.getProgrammingExercise(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetProgrammingExerciseResponse.newBuilder(), "programming_exercise", found).build());
    }

    @Override
    public void createProgrammingExercise(CreateProgrammingExerciseRequest request,
            StreamObserver<CreateProgrammingExerciseResponse> out) {
        ProgrammingExercise created = service.createProgrammingExercise(
                bridge.body(request.getProgrammingExercise(), ProgrammingExercise.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateProgrammingExerciseResponse.newBuilder(), "programming_exercise", created).build());
    }

    @Override
    public void updateProgrammingExercise(UpdateProgrammingExerciseRequest request,
            StreamObserver<UpdateProgrammingExerciseResponse> out) {
        ProgrammingExercise updated = service.updateProgrammingExercise(bridge.id(request.getProgrammingExercise().getId(), "id"),
                bridge.requiredIfMatch(request.getProgrammingExercise().getEtag()),
                bridge.changes(request.getProgrammingExercise(), request.getUpdateMask(), ProgrammingExerciseUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateProgrammingExerciseResponse.newBuilder(), "programming_exercise", updated).build());
    }

    @Override
    public void deleteProgrammingExercise(DeleteProgrammingExerciseRequest request,
            StreamObserver<DeleteProgrammingExerciseResponse> out) {
        service.deleteProgrammingExercise(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteProgrammingExerciseResponse.getDefaultInstance());
    }

    @Override
    public void listExerciseTestCases(ListExerciseTestCasesRequest request, StreamObserver<ListExerciseTestCasesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExerciseTestCase> rows = service.listExerciseTestCases(
                bridge.id(request.getProgrammingExerciseId(), "programmingExerciseId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExerciseTestCasesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExerciseTestCase(GetExerciseTestCaseRequest request, StreamObserver<GetExerciseTestCaseResponse> out) {
        ExerciseTestCase found = service.getExerciseTestCase(
                bridge.id(request.getProgrammingExerciseId(), "programmingExerciseId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExerciseTestCaseResponse.newBuilder(), "exercise_test_case", found).build());
    }

    @Override
    public void createExerciseTestCase(CreateExerciseTestCaseRequest request,
            StreamObserver<CreateExerciseTestCaseResponse> out) {
        ExerciseTestCase created = service.createExerciseTestCase(
                bridge.id(request.getProgrammingExerciseId(), "programmingExerciseId"),
                bridge.body(request.getExerciseTestCase(), ExerciseTestCase.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateExerciseTestCaseResponse.newBuilder(), "exercise_test_case", created).build());
    }

    @Override
    public void updateExerciseTestCase(UpdateExerciseTestCaseRequest request,
            StreamObserver<UpdateExerciseTestCaseResponse> out) {
        ExerciseTestCase updated = service.updateExerciseTestCase(
                bridge.id(request.getProgrammingExerciseId(), "programmingExerciseId"),
                bridge.id(request.getExerciseTestCase().getId(), "id"),
                bridge.requiredIfMatch(request.getExerciseTestCase().getEtag()),
                bridge.changes(request.getExerciseTestCase(), request.getUpdateMask(), ExerciseTestCaseUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateExerciseTestCaseResponse.newBuilder(), "exercise_test_case", updated).build());
    }

    @Override
    public void deleteExerciseTestCase(DeleteExerciseTestCaseRequest request,
            StreamObserver<DeleteExerciseTestCaseResponse> out) {
        service.deleteExerciseTestCase(bridge.id(request.getProgrammingExerciseId(), "programmingExerciseId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExerciseTestCaseResponse.getDefaultInstance());
    }

    @Override
    public void submitExercise(SubmitExerciseRequest request, StreamObserver<SubmitExerciseResponse> out) {
        ProtoBridge.send(out, bridge.reply(SubmitExerciseResponse.newBuilder(), "result",
                submissions.submitExercise(bridge.id(request.getProgrammingExerciseId(), "programmingExerciseId"),
                        bridge.body(request, com.mysms.learning.lms.api.model.SubmitExerciseRequest.class,
                                "programming_exercise_id", "request_id"))).build());
    }
}
