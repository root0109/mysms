package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.QuizAnswer;
import com.mysms.learning.lms.api.model.QuizSubmission;
import com.mysms.learning.lms.api.model.QuizViolation;
import com.mysms.learning.lms.business.QuizAttemptService;
import com.mysms.learning.lms.business.QuizSubmissionService;
import com.mysms.lms.v1.GetQuizAnswerRequest;
import com.mysms.lms.v1.GetQuizAnswerResponse;
import com.mysms.lms.v1.GetQuizSubmissionRequest;
import com.mysms.lms.v1.GetQuizSubmissionResponse;
import com.mysms.lms.v1.GetQuizViolationRequest;
import com.mysms.lms.v1.GetQuizViolationResponse;
import com.mysms.lms.v1.GradeQuizAnswersRequest;
import com.mysms.lms.v1.GradeQuizAnswersResponse;
import com.mysms.lms.v1.ListQuizAnswersRequest;
import com.mysms.lms.v1.ListQuizAnswersResponse;
import com.mysms.lms.v1.ListQuizSubmissionsRequest;
import com.mysms.lms.v1.ListQuizSubmissionsResponse;
import com.mysms.lms.v1.ListQuizViolationsRequest;
import com.mysms.lms.v1.ListQuizViolationsResponse;
import com.mysms.lms.v1.PauseQuizAttemptRequest;
import com.mysms.lms.v1.PauseQuizAttemptResponse;
import com.mysms.lms.v1.QuizSubmissionServiceGrpc;
import com.mysms.lms.v1.ResumeQuizAttemptRequest;
import com.mysms.lms.v1.ResumeQuizAttemptResponse;
import com.mysms.lms.v1.SaveQuizResponsesRequest;
import com.mysms.lms.v1.SaveQuizResponsesResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.QuizSubmissionService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class QuizSubmissionResolver extends QuizSubmissionServiceGrpc.QuizSubmissionServiceImplBase {

    private final QuizSubmissionService service;
    private final QuizAttemptService attempts;
    private final ProtoBridge bridge;

    public QuizSubmissionResolver(QuizSubmissionService service, QuizAttemptService attempts, ProtoBridge bridge) {
        this.service = service;
        this.attempts = attempts;
        this.bridge = bridge;
    }

    @Override
    public void listQuizSubmissions(ListQuizSubmissionsRequest request, StreamObserver<ListQuizSubmissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuizSubmission> rows = service.listQuizSubmissions(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuizSubmissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuizSubmission(GetQuizSubmissionRequest request, StreamObserver<GetQuizSubmissionResponse> out) {
        QuizSubmission found = service.getQuizSubmission(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuizSubmissionResponse.newBuilder(), "quiz_submission", found).build());
    }

    @Override
    public void listQuizAnswers(ListQuizAnswersRequest request, StreamObserver<ListQuizAnswersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuizAnswer> rows = service.listQuizAnswers(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuizAnswersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuizAnswer(GetQuizAnswerRequest request, StreamObserver<GetQuizAnswerResponse> out) {
        QuizAnswer found = service.getQuizAnswer(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuizAnswerResponse.newBuilder(), "quiz_answer", found).build());
    }

    @Override
    public void listQuizViolations(ListQuizViolationsRequest request, StreamObserver<ListQuizViolationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuizViolation> rows = service.listQuizViolations(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuizViolationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuizViolation(GetQuizViolationRequest request, StreamObserver<GetQuizViolationResponse> out) {
        QuizViolation found = service.getQuizViolation(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuizViolationResponse.newBuilder(), "quiz_violation", found).build());
    }

    @Override
    public void saveQuizResponses(SaveQuizResponsesRequest request, StreamObserver<SaveQuizResponsesResponse> out) {
        ProtoBridge.send(out, bridge.reply(SaveQuizResponsesResponse.newBuilder(), "result",
                attempts.saveQuizResponses(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                        bridge.body(request, com.mysms.learning.lms.api.model.SaveQuizResponsesRequest.class,
                                "quiz_submission_id",
                                "request_id"))).build());
    }

    @Override
    public void pauseQuizAttempt(PauseQuizAttemptRequest request, StreamObserver<PauseQuizAttemptResponse> out) {
        QuizSubmission changed = attempts.pauseQuizAttempt(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                bridge.body(request, com.mysms.learning.lms.api.model.PauseQuizAttemptRequest.class, "quiz_submission_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(PauseQuizAttemptResponse.newBuilder(), "quiz_submission", changed).build());
    }

    @Override
    public void resumeQuizAttempt(ResumeQuizAttemptRequest request, StreamObserver<ResumeQuizAttemptResponse> out) {
        QuizSubmission changed = attempts.resumeQuizAttempt(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ResumeQuizAttemptRequest.class, "quiz_submission_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ResumeQuizAttemptResponse.newBuilder(), "quiz_submission", changed).build());
    }

    @Override
    public void gradeQuizAnswers(GradeQuizAnswersRequest request, StreamObserver<GradeQuizAnswersResponse> out) {
        QuizSubmission changed = attempts.gradeQuizAnswers(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                bridge.body(request, com.mysms.learning.lms.api.model.GradeQuizAnswersRequest.class, "quiz_submission_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(GradeQuizAnswersResponse.newBuilder(), "quiz_submission", changed).build());
    }
}
