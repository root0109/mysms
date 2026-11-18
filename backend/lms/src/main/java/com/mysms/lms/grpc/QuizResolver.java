package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.Quiz;
import com.mysms.lms.api.model.QuizQuestion;
import com.mysms.lms.api.model.QuizQuestionHistory;
import com.mysms.lms.api.model.QuizQuestionUpdate;
import com.mysms.lms.api.model.QuizUpdate;
import com.mysms.lms.business.QuizAttemptService;
import com.mysms.lms.business.QuizService;
import com.mysms.lms.business.QuizWorkflowService;
import com.mysms.lms.v1.CheckQuizAnswerRequest;
import com.mysms.lms.v1.CheckQuizAnswerResponse;
import com.mysms.lms.v1.CreateQuizQuestionRequest;
import com.mysms.lms.v1.CreateQuizQuestionResponse;
import com.mysms.lms.v1.CreateQuizRequest;
import com.mysms.lms.v1.CreateQuizResponse;
import com.mysms.lms.v1.DeleteQuizQuestionRequest;
import com.mysms.lms.v1.DeleteQuizQuestionResponse;
import com.mysms.lms.v1.DeleteQuizRequest;
import com.mysms.lms.v1.DeleteQuizResponse;
import com.mysms.lms.v1.GetQuizQuestionHistoryRequest;
import com.mysms.lms.v1.GetQuizQuestionHistoryResponse;
import com.mysms.lms.v1.GetQuizQuestionRequest;
import com.mysms.lms.v1.GetQuizQuestionResponse;
import com.mysms.lms.v1.GetQuizRequest;
import com.mysms.lms.v1.GetQuizResponse;
import com.mysms.lms.v1.ListQuizQuestionHistoryRequest;
import com.mysms.lms.v1.ListQuizQuestionHistoryResponse;
import com.mysms.lms.v1.ListQuizQuestionsRequest;
import com.mysms.lms.v1.ListQuizQuestionsResponse;
import com.mysms.lms.v1.ListQuizzesRequest;
import com.mysms.lms.v1.ListQuizzesResponse;
import com.mysms.lms.v1.QuizServiceGrpc;
import com.mysms.lms.v1.SetQuizQuestionsRequest;
import com.mysms.lms.v1.SetQuizQuestionsResponse;
import com.mysms.lms.v1.StartQuizAttemptRequest;
import com.mysms.lms.v1.StartQuizAttemptResponse;
import com.mysms.lms.v1.SubmitQuizRequest;
import com.mysms.lms.v1.SubmitQuizResponse;
import com.mysms.lms.v1.UpdateQuizQuestionRequest;
import com.mysms.lms.v1.UpdateQuizQuestionResponse;
import com.mysms.lms.v1.UpdateQuizRequest;
import com.mysms.lms.v1.UpdateQuizResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.QuizService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class QuizResolver extends QuizServiceGrpc.QuizServiceImplBase {

    private final QuizService service;
    private final QuizWorkflowService workflows;
    private final QuizAttemptService attempts;
    private final ProtoBridge bridge;

    public QuizResolver(QuizService service, QuizWorkflowService workflows, QuizAttemptService attempts,
            ProtoBridge bridge) {
        this.service = service;
        this.workflows = workflows;
        this.attempts = attempts;
        this.bridge = bridge;
    }

    @Override
    public void listQuizzes(ListQuizzesRequest request, StreamObserver<ListQuizzesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Quiz> rows = service.listQuizs(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListQuizzesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuiz(GetQuizRequest request, StreamObserver<GetQuizResponse> out) {
        Quiz found = service.getQuiz(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuizResponse.newBuilder(), "quiz", found).build());
    }

    @Override
    public void createQuiz(CreateQuizRequest request, StreamObserver<CreateQuizResponse> out) {
        Quiz created = service.createQuiz(bridge.body(request.getQuiz(), Quiz.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuizResponse.newBuilder(), "quiz", created).build());
    }

    @Override
    public void updateQuiz(UpdateQuizRequest request, StreamObserver<UpdateQuizResponse> out) {
        Quiz updated = service.updateQuiz(bridge.id(request.getQuiz().getId(), "id"),
                bridge.requiredIfMatch(request.getQuiz().getEtag()),
                bridge.changes(request.getQuiz(), request.getUpdateMask(), QuizUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuizResponse.newBuilder(), "quiz", updated).build());
    }

    @Override
    public void deleteQuiz(DeleteQuizRequest request, StreamObserver<DeleteQuizResponse> out) {
        service.deleteQuiz(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuizResponse.getDefaultInstance());
    }

    @Override
    public void listQuizQuestions(ListQuizQuestionsRequest request, StreamObserver<ListQuizQuestionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuizQuestion> rows = service.listQuizQuestions(bridge.id(request.getQuizId(), "quizId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuizQuestionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuizQuestion(GetQuizQuestionRequest request, StreamObserver<GetQuizQuestionResponse> out) {
        QuizQuestion found = service.getQuizQuestion(bridge.id(request.getQuizId(), "quizId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuizQuestionResponse.newBuilder(), "quiz_question", found).build());
    }

    @Override
    public void createQuizQuestion(CreateQuizQuestionRequest request, StreamObserver<CreateQuizQuestionResponse> out) {
        QuizQuestion created = service.createQuizQuestion(bridge.id(request.getQuizId(), "quizId"),
                bridge.body(request.getQuizQuestion(), QuizQuestion.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuizQuestionResponse.newBuilder(), "quiz_question", created).build());
    }

    @Override
    public void updateQuizQuestion(UpdateQuizQuestionRequest request, StreamObserver<UpdateQuizQuestionResponse> out) {
        QuizQuestion updated = service.updateQuizQuestion(bridge.id(request.getQuizId(), "quizId"),
                bridge.id(request.getQuizQuestion().getId(), "id"), bridge.requiredIfMatch(request.getQuizQuestion().getEtag()),
                bridge.changes(request.getQuizQuestion(), request.getUpdateMask(), QuizQuestionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuizQuestionResponse.newBuilder(), "quiz_question", updated).build());
    }

    @Override
    public void deleteQuizQuestion(DeleteQuizQuestionRequest request, StreamObserver<DeleteQuizQuestionResponse> out) {
        service.deleteQuizQuestion(bridge.id(request.getQuizId(), "quizId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuizQuestionResponse.getDefaultInstance());
    }

    @Override
    public void listQuizQuestionHistory(ListQuizQuestionHistoryRequest request,
            StreamObserver<ListQuizQuestionHistoryResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuizQuestionHistory> rows = service.listQuizQuestionHistorys(bridge.id(request.getQuizId(), "quizId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuizQuestionHistoryResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuizQuestionHistory(GetQuizQuestionHistoryRequest request,
            StreamObserver<GetQuizQuestionHistoryResponse> out) {
        QuizQuestionHistory found = service.getQuizQuestionHistory(bridge.id(request.getQuizId(), "quizId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuizQuestionHistoryResponse.newBuilder(), "quiz_question_history", found).build());
    }

    @Override
    public void startQuizAttempt(StartQuizAttemptRequest request, StreamObserver<StartQuizAttemptResponse> out) {
        ProtoBridge.send(out, bridge.reply(StartQuizAttemptResponse.newBuilder(), "result",
                service.startQuizAttempt(bridge.id(request.getContentItemId(), "contentItemId"))).build());
    }

    @Override
    public void checkQuizAnswer(CheckQuizAnswerRequest request, StreamObserver<CheckQuizAnswerResponse> out) {
        ProtoBridge.send(out, bridge.reply(CheckQuizAnswerResponse.newBuilder(), "result",
                workflows.checkQuizAnswer(bridge.id(request.getQuizId(), "quizId"),
                        bridge.body(request, com.mysms.lms.api.model.CheckQuizAnswerRequest.class, "quiz_id",
                                "request_id"))).build());
    }

    @Override
    public void submitQuiz(SubmitQuizRequest request, StreamObserver<SubmitQuizResponse> out) {
        ProtoBridge.send(out, bridge.reply(SubmitQuizResponse.newBuilder(), "result",
                attempts.submitQuiz(bridge.id(request.getQuizSubmissionId(), "quizSubmissionId"),
                        bridge.body(request, com.mysms.lms.api.model.SubmitQuizRequest.class, "quiz_submission_id",
                                "request_id"))).build());
    }

    @Override
    public void setQuizQuestions(SetQuizQuestionsRequest request, StreamObserver<SetQuizQuestionsResponse> out) {
        ProtoBridge.send(out, bridge.reply(SetQuizQuestionsResponse.newBuilder(), "items",
                workflows.setQuizQuestions(bridge.id(request.getQuizId(), "quizId"),
                        bridge.body(request, com.mysms.lms.api.model.SetQuizQuestionsRequest.class, "quiz_id",
                                "request_id")).getItems()).build());
    }
}
