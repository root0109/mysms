package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.QuestionBlankAnswer;
import com.mysms.learning.lms.business.QuestionBlankAnswerService;
import com.mysms.lms.v1.CreateQuestionBlankAnswerRequest;
import com.mysms.lms.v1.CreateQuestionBlankAnswerResponse;
import com.mysms.lms.v1.DeleteQuestionBlankAnswerRequest;
import com.mysms.lms.v1.DeleteQuestionBlankAnswerResponse;
import com.mysms.lms.v1.GetQuestionBlankAnswerRequest;
import com.mysms.lms.v1.GetQuestionBlankAnswerResponse;
import com.mysms.lms.v1.ListQuestionBlankAnswersRequest;
import com.mysms.lms.v1.ListQuestionBlankAnswersResponse;
import com.mysms.lms.v1.QuestionBlankAnswerServiceGrpc;
import com.mysms.lms.v1.QuestionBlankAnswerUpdate;
import com.mysms.lms.v1.UpdateQuestionBlankAnswerResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.QuestionBlankAnswerService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class QuestionBlankAnswerResolver extends QuestionBlankAnswerServiceGrpc.QuestionBlankAnswerServiceImplBase {

    private final QuestionBlankAnswerService service;
    private final ProtoBridge bridge;

    public QuestionBlankAnswerResolver(QuestionBlankAnswerService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listQuestionBlankAnswers(ListQuestionBlankAnswersRequest request,
            StreamObserver<ListQuestionBlankAnswersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionBlankAnswer> rows = service.listQuestionBlankAnswers(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionBlankAnswersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionBlankAnswer(GetQuestionBlankAnswerRequest request,
            StreamObserver<GetQuestionBlankAnswerResponse> out) {
        QuestionBlankAnswer found = service.getQuestionBlankAnswer(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetQuestionBlankAnswerResponse.newBuilder(), "question_blank_answer", found).build());
    }

    @Override
    public void createQuestionBlankAnswer(CreateQuestionBlankAnswerRequest request,
            StreamObserver<CreateQuestionBlankAnswerResponse> out) {
        QuestionBlankAnswer created = service.createQuestionBlankAnswer(
                bridge.body(request.getQuestionBlankAnswer(), QuestionBlankAnswer.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateQuestionBlankAnswerResponse.newBuilder(), "question_blank_answer", created).build());
    }

    @Override
    public void updateQuestionBlankAnswer(QuestionBlankAnswerUpdate request,
            StreamObserver<UpdateQuestionBlankAnswerResponse> out) {
        QuestionBlankAnswer updated = service.updateQuestionBlankAnswer(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                QuestionBlankAnswerProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateQuestionBlankAnswerResponse.newBuilder(), "question_blank_answer", updated).build());
    }

    @Override
    public void deleteQuestionBlankAnswer(DeleteQuestionBlankAnswerRequest request,
            StreamObserver<DeleteQuestionBlankAnswerResponse> out) {
        service.deleteQuestionBlankAnswer(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionBlankAnswerResponse.getDefaultInstance());
    }
}
