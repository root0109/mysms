package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.QuestionImportService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Question;
import com.mysms.learning.lms.api.model.QuestionAcceptedAnswer;
import com.mysms.learning.lms.api.model.QuestionAcceptedAnswerUpdate;
import com.mysms.learning.lms.api.model.QuestionBlank;
import com.mysms.learning.lms.api.model.QuestionBlankUpdate;
import com.mysms.learning.lms.api.model.QuestionHotspot;
import com.mysms.learning.lms.api.model.QuestionHotspotUpdate;
import com.mysms.learning.lms.api.model.QuestionMatchPair;
import com.mysms.learning.lms.api.model.QuestionMatchPairUpdate;
import com.mysms.learning.lms.api.model.QuestionOption;
import com.mysms.learning.lms.api.model.QuestionOptionUpdate;
import com.mysms.learning.lms.api.model.QuestionPattern;
import com.mysms.learning.lms.api.model.QuestionPatternUpdate;
import com.mysms.learning.lms.api.model.QuestionUpdate;
import com.mysms.learning.lms.business.QuestionService;
import com.mysms.lms.v1.CreateQuestionAcceptedAnswerRequest;
import com.mysms.lms.v1.CreateQuestionAcceptedAnswerResponse;
import com.mysms.lms.v1.CreateQuestionBlankRequest;
import com.mysms.lms.v1.CreateQuestionBlankResponse;
import com.mysms.lms.v1.CreateQuestionHotspotRequest;
import com.mysms.lms.v1.CreateQuestionHotspotResponse;
import com.mysms.lms.v1.CreateQuestionMatchPairRequest;
import com.mysms.lms.v1.CreateQuestionMatchPairResponse;
import com.mysms.lms.v1.CreateQuestionOptionRequest;
import com.mysms.lms.v1.CreateQuestionOptionResponse;
import com.mysms.lms.v1.CreateQuestionPatternRequest;
import com.mysms.lms.v1.CreateQuestionPatternResponse;
import com.mysms.lms.v1.CreateQuestionRequest;
import com.mysms.lms.v1.CreateQuestionResponse;
import com.mysms.lms.v1.DeleteQuestionAcceptedAnswerRequest;
import com.mysms.lms.v1.DeleteQuestionAcceptedAnswerResponse;
import com.mysms.lms.v1.DeleteQuestionBlankRequest;
import com.mysms.lms.v1.DeleteQuestionBlankResponse;
import com.mysms.lms.v1.DeleteQuestionHotspotRequest;
import com.mysms.lms.v1.DeleteQuestionHotspotResponse;
import com.mysms.lms.v1.DeleteQuestionMatchPairRequest;
import com.mysms.lms.v1.DeleteQuestionMatchPairResponse;
import com.mysms.lms.v1.DeleteQuestionOptionRequest;
import com.mysms.lms.v1.DeleteQuestionOptionResponse;
import com.mysms.lms.v1.DeleteQuestionPatternRequest;
import com.mysms.lms.v1.DeleteQuestionPatternResponse;
import com.mysms.lms.v1.DeleteQuestionRequest;
import com.mysms.lms.v1.DeleteQuestionResponse;
import com.mysms.lms.v1.GetQuestionAcceptedAnswerRequest;
import com.mysms.lms.v1.GetQuestionAcceptedAnswerResponse;
import com.mysms.lms.v1.GetQuestionBlankRequest;
import com.mysms.lms.v1.GetQuestionBlankResponse;
import com.mysms.lms.v1.GetQuestionHotspotRequest;
import com.mysms.lms.v1.GetQuestionHotspotResponse;
import com.mysms.lms.v1.GetQuestionMatchPairRequest;
import com.mysms.lms.v1.GetQuestionMatchPairResponse;
import com.mysms.lms.v1.GetQuestionOptionRequest;
import com.mysms.lms.v1.GetQuestionOptionResponse;
import com.mysms.lms.v1.GetQuestionPatternRequest;
import com.mysms.lms.v1.GetQuestionPatternResponse;
import com.mysms.lms.v1.GetQuestionRequest;
import com.mysms.lms.v1.GetQuestionResponse;
import com.mysms.lms.v1.ImportQuestionsRequest;
import com.mysms.lms.v1.ImportQuestionsResponse;
import com.mysms.lms.v1.ListQuestionAcceptedAnswersRequest;
import com.mysms.lms.v1.ListQuestionAcceptedAnswersResponse;
import com.mysms.lms.v1.ListQuestionBlanksRequest;
import com.mysms.lms.v1.ListQuestionBlanksResponse;
import com.mysms.lms.v1.ListQuestionHotspotsRequest;
import com.mysms.lms.v1.ListQuestionHotspotsResponse;
import com.mysms.lms.v1.ListQuestionMatchPairsRequest;
import com.mysms.lms.v1.ListQuestionMatchPairsResponse;
import com.mysms.lms.v1.ListQuestionOptionsRequest;
import com.mysms.lms.v1.ListQuestionOptionsResponse;
import com.mysms.lms.v1.ListQuestionPatternsRequest;
import com.mysms.lms.v1.ListQuestionPatternsResponse;
import com.mysms.lms.v1.ListQuestionsRequest;
import com.mysms.lms.v1.ListQuestionsResponse;
import com.mysms.lms.v1.QuestionServiceGrpc;
import com.mysms.lms.v1.SearchQuestionBankRequest;
import com.mysms.lms.v1.SearchQuestionBankResponse;
import com.mysms.lms.v1.UpdateQuestionAcceptedAnswerRequest;
import com.mysms.lms.v1.UpdateQuestionAcceptedAnswerResponse;
import com.mysms.lms.v1.UpdateQuestionBlankRequest;
import com.mysms.lms.v1.UpdateQuestionBlankResponse;
import com.mysms.lms.v1.UpdateQuestionHotspotRequest;
import com.mysms.lms.v1.UpdateQuestionHotspotResponse;
import com.mysms.lms.v1.UpdateQuestionMatchPairRequest;
import com.mysms.lms.v1.UpdateQuestionMatchPairResponse;
import com.mysms.lms.v1.UpdateQuestionOptionRequest;
import com.mysms.lms.v1.UpdateQuestionOptionResponse;
import com.mysms.lms.v1.UpdateQuestionPatternRequest;
import com.mysms.lms.v1.UpdateQuestionPatternResponse;
import com.mysms.lms.v1.UpdateQuestionRequest;
import com.mysms.lms.v1.UpdateQuestionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.QuestionService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class QuestionResolver extends QuestionServiceGrpc.QuestionServiceImplBase {

    private final QuestionService service;
    private final ProtoBridge bridge;
    private final QuestionImportService imports;

    public QuestionResolver(QuestionService service, ProtoBridge bridge, QuestionImportService imports) {
        this.imports = imports;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listQuestions(ListQuestionsRequest request, StreamObserver<ListQuestionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Question> rows = service.listQuestions(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestion(GetQuestionRequest request, StreamObserver<GetQuestionResponse> out) {
        Question found = service.getQuestion(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuestionResponse.newBuilder(), "question", found).build());
    }

    @Override
    public void createQuestion(CreateQuestionRequest request, StreamObserver<CreateQuestionResponse> out) {
        Question created = service.createQuestion(bridge.body(request.getQuestion(), Question.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuestionResponse.newBuilder(), "question", created).build());
    }

    @Override
    public void updateQuestion(UpdateQuestionRequest request, StreamObserver<UpdateQuestionResponse> out) {
        Question updated = service.updateQuestion(bridge.id(request.getQuestion().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestion().getEtag()),
                bridge.changes(request.getQuestion(), request.getUpdateMask(), QuestionUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuestionResponse.newBuilder(), "question", updated).build());
    }

    @Override
    public void deleteQuestion(DeleteQuestionRequest request, StreamObserver<DeleteQuestionResponse> out) {
        service.deleteQuestion(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionResponse.getDefaultInstance());
    }

    @Override
    public void listQuestionOptions(ListQuestionOptionsRequest request, StreamObserver<ListQuestionOptionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionOption> rows = service.listQuestionOptions(bridge.id(request.getQuestionId(), "questionId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionOptionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionOption(GetQuestionOptionRequest request, StreamObserver<GetQuestionOptionResponse> out) {
        QuestionOption found = service.getQuestionOption(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuestionOptionResponse.newBuilder(), "question_option", found).build());
    }

    @Override
    public void createQuestionOption(CreateQuestionOptionRequest request, StreamObserver<CreateQuestionOptionResponse> out) {
        QuestionOption created = service.createQuestionOption(bridge.id(request.getQuestionId(), "questionId"),
                bridge.body(request.getQuestionOption(), QuestionOption.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuestionOptionResponse.newBuilder(), "question_option", created).build());
    }

    @Override
    public void updateQuestionOption(UpdateQuestionOptionRequest request, StreamObserver<UpdateQuestionOptionResponse> out) {
        QuestionOption updated = service.updateQuestionOption(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getQuestionOption().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionOption().getEtag()),
                bridge.changes(request.getQuestionOption(), request.getUpdateMask(), QuestionOptionUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuestionOptionResponse.newBuilder(), "question_option", updated).build());
    }

    @Override
    public void deleteQuestionOption(DeleteQuestionOptionRequest request, StreamObserver<DeleteQuestionOptionResponse> out) {
        service.deleteQuestionOption(bridge.id(request.getQuestionId(), "questionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionOptionResponse.getDefaultInstance());
    }

    @Override
    public void listQuestionAcceptedAnswers(ListQuestionAcceptedAnswersRequest request,
            StreamObserver<ListQuestionAcceptedAnswersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionAcceptedAnswer> rows = service.listQuestionAcceptedAnswers(bridge.id(request.getQuestionId(), "questionId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionAcceptedAnswersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionAcceptedAnswer(GetQuestionAcceptedAnswerRequest request,
            StreamObserver<GetQuestionAcceptedAnswerResponse> out) {
        QuestionAcceptedAnswer found = service.getQuestionAcceptedAnswer(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetQuestionAcceptedAnswerResponse.newBuilder(), "question_accepted_answer", found).build());
    }

    @Override
    public void createQuestionAcceptedAnswer(CreateQuestionAcceptedAnswerRequest request,
            StreamObserver<CreateQuestionAcceptedAnswerResponse> out) {
        QuestionAcceptedAnswer created = service.createQuestionAcceptedAnswer(bridge.id(request.getQuestionId(), "questionId"),
                bridge.body(request.getQuestionAcceptedAnswer(), QuestionAcceptedAnswer.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateQuestionAcceptedAnswerResponse.newBuilder(), "question_accepted_answer", created).build());
    }

    @Override
    public void updateQuestionAcceptedAnswer(UpdateQuestionAcceptedAnswerRequest request,
            StreamObserver<UpdateQuestionAcceptedAnswerResponse> out) {
        QuestionAcceptedAnswer updated = service.updateQuestionAcceptedAnswer(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getQuestionAcceptedAnswer().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionAcceptedAnswer().getEtag()),
                bridge.changes(request.getQuestionAcceptedAnswer(), request.getUpdateMask(),
                QuestionAcceptedAnswerUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateQuestionAcceptedAnswerResponse.newBuilder(), "question_accepted_answer", updated).build());
    }

    @Override
    public void deleteQuestionAcceptedAnswer(DeleteQuestionAcceptedAnswerRequest request,
            StreamObserver<DeleteQuestionAcceptedAnswerResponse> out) {
        service.deleteQuestionAcceptedAnswer(bridge.id(request.getQuestionId(), "questionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionAcceptedAnswerResponse.getDefaultInstance());
    }

    @Override
    public void listQuestionBlanks(ListQuestionBlanksRequest request, StreamObserver<ListQuestionBlanksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionBlank> rows = service.listQuestionBlanks(bridge.id(request.getQuestionId(), "questionId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionBlanksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionBlank(GetQuestionBlankRequest request, StreamObserver<GetQuestionBlankResponse> out) {
        QuestionBlank found = service.getQuestionBlank(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuestionBlankResponse.newBuilder(), "question_blank", found).build());
    }

    @Override
    public void createQuestionBlank(CreateQuestionBlankRequest request, StreamObserver<CreateQuestionBlankResponse> out) {
        QuestionBlank created = service.createQuestionBlank(bridge.id(request.getQuestionId(), "questionId"),
                bridge.body(request.getQuestionBlank(), QuestionBlank.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuestionBlankResponse.newBuilder(), "question_blank", created).build());
    }

    @Override
    public void updateQuestionBlank(UpdateQuestionBlankRequest request, StreamObserver<UpdateQuestionBlankResponse> out) {
        QuestionBlank updated = service.updateQuestionBlank(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getQuestionBlank().getId(), "id"), bridge.requiredIfMatch(request.getQuestionBlank().getEtag()),
                bridge.changes(request.getQuestionBlank(), request.getUpdateMask(), QuestionBlankUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuestionBlankResponse.newBuilder(), "question_blank", updated).build());
    }

    @Override
    public void deleteQuestionBlank(DeleteQuestionBlankRequest request, StreamObserver<DeleteQuestionBlankResponse> out) {
        service.deleteQuestionBlank(bridge.id(request.getQuestionId(), "questionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionBlankResponse.getDefaultInstance());
    }

    @Override
    public void listQuestionHotspots(ListQuestionHotspotsRequest request, StreamObserver<ListQuestionHotspotsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionHotspot> rows = service.listQuestionHotspots(bridge.id(request.getQuestionId(), "questionId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionHotspotsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionHotspot(GetQuestionHotspotRequest request, StreamObserver<GetQuestionHotspotResponse> out) {
        QuestionHotspot found = service.getQuestionHotspot(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuestionHotspotResponse.newBuilder(), "question_hotspot", found).build());
    }

    @Override
    public void createQuestionHotspot(CreateQuestionHotspotRequest request, StreamObserver<CreateQuestionHotspotResponse> out) {
        QuestionHotspot created = service.createQuestionHotspot(bridge.id(request.getQuestionId(), "questionId"),
                bridge.body(request.getQuestionHotspot(), QuestionHotspot.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuestionHotspotResponse.newBuilder(), "question_hotspot", created).build());
    }

    @Override
    public void updateQuestionHotspot(UpdateQuestionHotspotRequest request, StreamObserver<UpdateQuestionHotspotResponse> out) {
        QuestionHotspot updated = service.updateQuestionHotspot(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getQuestionHotspot().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionHotspot().getEtag()),
                bridge.changes(request.getQuestionHotspot(), request.getUpdateMask(), QuestionHotspotUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuestionHotspotResponse.newBuilder(), "question_hotspot", updated).build());
    }

    @Override
    public void deleteQuestionHotspot(DeleteQuestionHotspotRequest request, StreamObserver<DeleteQuestionHotspotResponse> out) {
        service.deleteQuestionHotspot(bridge.id(request.getQuestionId(), "questionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionHotspotResponse.getDefaultInstance());
    }

    @Override
    public void listQuestionMatchPairs(ListQuestionMatchPairsRequest request,
            StreamObserver<ListQuestionMatchPairsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionMatchPair> rows = service.listQuestionMatchPairs(bridge.id(request.getQuestionId(), "questionId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionMatchPairsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionMatchPair(GetQuestionMatchPairRequest request, StreamObserver<GetQuestionMatchPairResponse> out) {
        QuestionMatchPair found = service.getQuestionMatchPair(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuestionMatchPairResponse.newBuilder(), "question_match_pair", found).build());
    }

    @Override
    public void createQuestionMatchPair(CreateQuestionMatchPairRequest request,
            StreamObserver<CreateQuestionMatchPairResponse> out) {
        QuestionMatchPair created = service.createQuestionMatchPair(bridge.id(request.getQuestionId(), "questionId"),
                bridge.body(request.getQuestionMatchPair(), QuestionMatchPair.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuestionMatchPairResponse.newBuilder(), "question_match_pair", created).build());
    }

    @Override
    public void updateQuestionMatchPair(UpdateQuestionMatchPairRequest request,
            StreamObserver<UpdateQuestionMatchPairResponse> out) {
        QuestionMatchPair updated = service.updateQuestionMatchPair(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getQuestionMatchPair().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionMatchPair().getEtag()),
                bridge.changes(request.getQuestionMatchPair(), request.getUpdateMask(), QuestionMatchPairUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuestionMatchPairResponse.newBuilder(), "question_match_pair", updated).build());
    }

    @Override
    public void deleteQuestionMatchPair(DeleteQuestionMatchPairRequest request,
            StreamObserver<DeleteQuestionMatchPairResponse> out) {
        service.deleteQuestionMatchPair(bridge.id(request.getQuestionId(), "questionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionMatchPairResponse.getDefaultInstance());
    }

    @Override
    public void listQuestionPatterns(ListQuestionPatternsRequest request, StreamObserver<ListQuestionPatternsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<QuestionPattern> rows = service.listQuestionPatterns(bridge.id(request.getQuestionId(), "questionId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListQuestionPatternsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getQuestionPattern(GetQuestionPatternRequest request, StreamObserver<GetQuestionPatternResponse> out) {
        QuestionPattern found = service.getQuestionPattern(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetQuestionPatternResponse.newBuilder(), "question_pattern", found).build());
    }

    @Override
    public void createQuestionPattern(CreateQuestionPatternRequest request, StreamObserver<CreateQuestionPatternResponse> out) {
        QuestionPattern created = service.createQuestionPattern(bridge.id(request.getQuestionId(), "questionId"),
                bridge.body(request.getQuestionPattern(), QuestionPattern.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateQuestionPatternResponse.newBuilder(), "question_pattern", created).build());
    }

    @Override
    public void updateQuestionPattern(UpdateQuestionPatternRequest request, StreamObserver<UpdateQuestionPatternResponse> out) {
        QuestionPattern updated = service.updateQuestionPattern(bridge.id(request.getQuestionId(), "questionId"),
                bridge.id(request.getQuestionPattern().getId(), "id"),
                bridge.requiredIfMatch(request.getQuestionPattern().getEtag()),
                bridge.changes(request.getQuestionPattern(), request.getUpdateMask(), QuestionPatternUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateQuestionPatternResponse.newBuilder(), "question_pattern", updated).build());
    }

    @Override
    public void deleteQuestionPattern(DeleteQuestionPatternRequest request, StreamObserver<DeleteQuestionPatternResponse> out) {
        service.deleteQuestionPattern(bridge.id(request.getQuestionId(), "questionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteQuestionPatternResponse.getDefaultInstance());
    }

    @Override
    public void searchQuestionBank(SearchQuestionBankRequest request, StreamObserver<SearchQuestionBankResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Question> rows = service.searchQuestionBank(request.hasQuery() ? request.getQuery() : null,
                request.getQuestionTypesList(), request.hasExcludeQuizId()
                        ? bridge.id(request.getExcludeQuizId(), "excludeQuizId") : null,
                page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(SearchQuestionBankResponse.newBuilder(), rows).build());
    }

    @Override
    public void importQuestions(ImportQuestionsRequest request, StreamObserver<ImportQuestionsResponse> out) {
        ProtoBridge.send(out, bridge.reply(ImportQuestionsResponse.newBuilder(), "result", imports.importQuestions(
                bridge.body(request, com.mysms.learning.lms.api.model.ImportQuestionsRequest.class, "request_id"))).build());
    }
}
