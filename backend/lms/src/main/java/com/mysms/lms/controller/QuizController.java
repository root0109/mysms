package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.QuizApi;
import com.mysms.lms.api.model.AnswerCheck;
import com.mysms.lms.api.model.CheckQuizAnswerRequest;
import com.mysms.lms.api.model.Quiz;
import com.mysms.lms.api.model.QuizAttempt;
import com.mysms.lms.api.model.QuizQuestion;
import com.mysms.lms.api.model.QuizQuestionHistory;
import com.mysms.lms.api.model.QuizQuestionList;
import com.mysms.lms.api.model.QuizQuestionUpdate;
import com.mysms.lms.api.model.QuizSubmission;
import com.mysms.lms.api.model.QuizUpdate;
import com.mysms.lms.api.model.SetQuizQuestionsRequest;
import com.mysms.lms.api.model.SubmitQuizRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.QuizService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Quiz operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class QuizController implements QuizApi {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    public ResponseEntity<AnswerCheck> checkQuizAnswer(UUID quizId,
            CheckQuizAnswerRequest checkQuizAnswerRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Quiz> createQuiz(Quiz quiz, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Quiz created = quizService.createQuiz(quiz, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/quizzes/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<QuizQuestion> createQuizQuestion(UUID quizId, QuizQuestion quizQuestion,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuizQuestion created = quizService.createQuizQuestion(quizId, quizQuestion, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/quizzes/" + quizId + "/questions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteQuiz(UUID quizId, String ifMatch) {
        quizService.deleteQuiz(quizId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuizQuestion(UUID quizId, UUID quizQuestionId, String ifMatch) {
        quizService.deleteQuizQuestion(quizId, quizQuestionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Quiz> getQuiz(UUID quizId, String fields) {
        Quiz found = quizService.getQuiz(quizId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuizQuestion> getQuizQuestion(UUID quizId, UUID quizQuestionId, String fields) {
        QuizQuestion found = quizService.getQuizQuestion(quizId, quizQuestionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuizQuestionHistory> getQuizQuestionHistory(UUID quizId,
            UUID quizQuestionHistoryId, String fields) {
        QuizQuestionHistory found = quizService.getQuizQuestionHistory(quizId, quizQuestionHistoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<QuizQuestionHistory>> listQuizQuestionHistory(UUID quizId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuizQuestionHistory> list =
                PagingUtil.pagingInfo(quizService.listQuizQuestionHistorys(quizId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuizQuestion>> listQuizQuestions(UUID quizId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuizQuestion> list =
                PagingUtil.pagingInfo(quizService.listQuizQuestions(quizId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Quiz>> listQuizzes(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Quiz> list = PagingUtil.pagingInfo(quizService.listQuizs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<QuizQuestionList> setQuizQuestions(UUID quizId,
            SetQuizQuestionsRequest setQuizQuestionsRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<QuizAttempt> startQuizAttempt(UUID contentItemId, Object body,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<QuizSubmission> submitQuiz(UUID quizSubmissionId,
            SubmitQuizRequest submitQuizRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Quiz> updateQuiz(UUID quizId, String ifMatch, QuizUpdate quizUpdate,
            String updateMask, Boolean validateOnly) {
        Quiz updated = quizService.updateQuiz(quizId, ifMatch, quizUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuizQuestion> updateQuizQuestion(UUID quizId, UUID quizQuestionId,
            String ifMatch, QuizQuestionUpdate quizQuestionUpdate, String updateMask, Boolean validateOnly) {
        QuizQuestion updated =
                quizService.updateQuizQuestion(quizId, quizQuestionId, ifMatch, quizQuestionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
