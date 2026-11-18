package com.mysms.lms.controller;

import com.mysms.lms.business.QuestionImportService;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.QuestionApi;
import com.mysms.lms.api.model.ImportQuestionsRequest;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.api.model.Question;
import com.mysms.lms.api.model.QuestionAcceptedAnswer;
import com.mysms.lms.api.model.QuestionAcceptedAnswerUpdate;
import com.mysms.lms.api.model.QuestionBlank;
import com.mysms.lms.api.model.QuestionBlankUpdate;
import com.mysms.lms.api.model.QuestionHotspot;
import com.mysms.lms.api.model.QuestionHotspotUpdate;
import com.mysms.lms.api.model.QuestionMatchPair;
import com.mysms.lms.api.model.QuestionMatchPairUpdate;
import com.mysms.lms.api.model.QuestionOption;
import com.mysms.lms.api.model.QuestionOptionUpdate;
import com.mysms.lms.api.model.QuestionPattern;
import com.mysms.lms.api.model.QuestionPatternUpdate;
import com.mysms.lms.api.model.QuestionUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.QuestionService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Question operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class QuestionController implements QuestionApi {

    private final QuestionService questionService;
    private final QuestionImportService questionImportService;

    public QuestionController(QuestionService questionService, QuestionImportService questionImportService) {
        this.questionImportService = questionImportService;
        this.questionService = questionService;
    }

    @Override
    public ResponseEntity<Question> createQuestion(Question question, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Question created = questionService.createQuestion(question, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/questions/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<QuestionAcceptedAnswer> createQuestionAcceptedAnswer(UUID questionId,
            QuestionAcceptedAnswer questionAcceptedAnswer, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionAcceptedAnswer created =
                questionService.createQuestionAcceptedAnswer(questionId, questionAcceptedAnswer, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/questions/" + questionId + "/accepted-answers/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<QuestionBlank> createQuestionBlank(UUID questionId, QuestionBlank questionBlank,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionBlank created = questionService.createQuestionBlank(questionId, questionBlank, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/questions/" + questionId + "/blanks/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<QuestionHotspot> createQuestionHotspot(UUID questionId,
            QuestionHotspot questionHotspot, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionHotspot created = questionService.createQuestionHotspot(questionId, questionHotspot, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/questions/" + questionId + "/hotspots/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<QuestionMatchPair> createQuestionMatchPair(UUID questionId,
            QuestionMatchPair questionMatchPair, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionMatchPair created =
                questionService.createQuestionMatchPair(questionId, questionMatchPair, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/questions/" + questionId + "/match-pairs/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<QuestionOption> createQuestionOption(UUID questionId,
            QuestionOption questionOption, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionOption created = questionService.createQuestionOption(questionId, questionOption, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/questions/" + questionId + "/options/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<QuestionPattern> createQuestionPattern(UUID questionId,
            QuestionPattern questionPattern, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionPattern created = questionService.createQuestionPattern(questionId, questionPattern, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/questions/" + questionId + "/patterns/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteQuestion(UUID questionId, String ifMatch) {
        questionService.deleteQuestion(questionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuestionAcceptedAnswer(UUID questionId,
            UUID questionAcceptedAnswerId, String ifMatch) {
        questionService.deleteQuestionAcceptedAnswer(questionId, questionAcceptedAnswerId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuestionBlank(UUID questionId, UUID questionBlankId,
            String ifMatch) {
        questionService.deleteQuestionBlank(questionId, questionBlankId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuestionHotspot(UUID questionId, UUID questionHotspotId,
            String ifMatch) {
        questionService.deleteQuestionHotspot(questionId, questionHotspotId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuestionMatchPair(UUID questionId, UUID questionMatchPairId,
            String ifMatch) {
        questionService.deleteQuestionMatchPair(questionId, questionMatchPairId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuestionOption(UUID questionId, UUID questionOptionId,
            String ifMatch) {
        questionService.deleteQuestionOption(questionId, questionOptionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuestionPattern(UUID questionId, UUID questionPatternId,
            String ifMatch) {
        questionService.deleteQuestionPattern(questionId, questionPatternId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Question> getQuestion(UUID questionId, String fields) {
        Question found = questionService.getQuestion(questionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuestionAcceptedAnswer> getQuestionAcceptedAnswer(UUID questionId,
            UUID questionAcceptedAnswerId, String fields) {
        QuestionAcceptedAnswer found =
                questionService.getQuestionAcceptedAnswer(questionId, questionAcceptedAnswerId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuestionBlank> getQuestionBlank(UUID questionId, UUID questionBlankId,
            String fields) {
        QuestionBlank found = questionService.getQuestionBlank(questionId, questionBlankId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuestionHotspot> getQuestionHotspot(UUID questionId, UUID questionHotspotId,
            String fields) {
        QuestionHotspot found = questionService.getQuestionHotspot(questionId, questionHotspotId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuestionMatchPair> getQuestionMatchPair(UUID questionId,
            UUID questionMatchPairId, String fields) {
        QuestionMatchPair found = questionService.getQuestionMatchPair(questionId, questionMatchPairId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuestionOption> getQuestionOption(UUID questionId, UUID questionOptionId,
            String fields) {
        QuestionOption found = questionService.getQuestionOption(questionId, questionOptionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuestionPattern> getQuestionPattern(UUID questionId, UUID questionPatternId,
            String fields) {
        QuestionPattern found = questionService.getQuestionPattern(questionId, questionPatternId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Operation> importQuestions(ImportQuestionsRequest importQuestionsRequest,
            UUID idempotencyKey) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(questionImportService.importQuestions(
                importQuestionsRequest));
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionAcceptedAnswer>> listQuestionAcceptedAnswers(UUID questionId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionAcceptedAnswer> list =
                PagingUtil.pagingInfo(questionService.listQuestionAcceptedAnswers(questionId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionBlank>> listQuestionBlanks(UUID questionId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionBlank> list =
                PagingUtil.pagingInfo(questionService.listQuestionBlanks(questionId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionHotspot>> listQuestionHotspots(UUID questionId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionHotspot> list =
                PagingUtil.pagingInfo(questionService.listQuestionHotspots(questionId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionMatchPair>> listQuestionMatchPairs(UUID questionId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionMatchPair> list =
                PagingUtil.pagingInfo(questionService.listQuestionMatchPairs(questionId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionOption>> listQuestionOptions(UUID questionId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionOption> list =
                PagingUtil.pagingInfo(questionService.listQuestionOptions(questionId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionPattern>> listQuestionPatterns(UUID questionId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionPattern> list =
                PagingUtil.pagingInfo(questionService.listQuestionPatterns(questionId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Question>> listQuestions(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Question> list =
                PagingUtil.pagingInfo(questionService.listQuestions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Question>> searchQuestionBank(String query, List<String> questionTypes,
            UUID excludeQuizId, Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(questionService.searchQuestionBank(query, questionTypes,
                excludeQuizId, page, size, cursor)));
    }

    @Override
    public ResponseEntity<Question> updateQuestion(UUID questionId, String ifMatch,
            QuestionUpdate questionUpdate, String updateMask, Boolean validateOnly) {
        Question updated = questionService.updateQuestion(questionId, ifMatch, questionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuestionAcceptedAnswer> updateQuestionAcceptedAnswer(UUID questionId,
            UUID questionAcceptedAnswerId, String ifMatch, QuestionAcceptedAnswerUpdate questionAcceptedAnswerUpdate,
            String updateMask, Boolean validateOnly) {
        QuestionAcceptedAnswer updated =
                questionService.updateQuestionAcceptedAnswer(questionId, questionAcceptedAnswerId,
                        ifMatch, questionAcceptedAnswerUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuestionBlank> updateQuestionBlank(UUID questionId, UUID questionBlankId,
            String ifMatch, QuestionBlankUpdate questionBlankUpdate, String updateMask, Boolean validateOnly) {
        QuestionBlank updated =
                questionService.updateQuestionBlank(questionId, questionBlankId, ifMatch, questionBlankUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuestionHotspot> updateQuestionHotspot(UUID questionId, UUID questionHotspotId,
            String ifMatch, QuestionHotspotUpdate questionHotspotUpdate, String updateMask, Boolean validateOnly) {
        QuestionHotspot updated =
                questionService.updateQuestionHotspot(questionId, questionHotspotId, ifMatch,
                        questionHotspotUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuestionMatchPair> updateQuestionMatchPair(UUID questionId,
            UUID questionMatchPairId, String ifMatch, QuestionMatchPairUpdate questionMatchPairUpdate,
            String updateMask, Boolean validateOnly) {
        QuestionMatchPair updated =
                questionService.updateQuestionMatchPair(questionId, questionMatchPairId, ifMatch,
                        questionMatchPairUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuestionOption> updateQuestionOption(UUID questionId, UUID questionOptionId,
            String ifMatch, QuestionOptionUpdate questionOptionUpdate, String updateMask, Boolean validateOnly) {
        QuestionOption updated =
                questionService.updateQuestionOption(questionId, questionOptionId, ifMatch, questionOptionUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuestionPattern> updateQuestionPattern(UUID questionId, UUID questionPatternId,
            String ifMatch, QuestionPatternUpdate questionPatternUpdate, String updateMask, Boolean validateOnly) {
        QuestionPattern updated =
                questionService.updateQuestionPattern(questionId, questionPatternId, ifMatch,
                        questionPatternUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
