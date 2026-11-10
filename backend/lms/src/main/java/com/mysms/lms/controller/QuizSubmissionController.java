package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.QuizSubmissionApi;
import com.mysms.lms.api.model.GradeQuizAnswersRequest;
import com.mysms.lms.api.model.PauseQuizAttemptRequest;
import com.mysms.lms.api.model.QuizAnswer;
import com.mysms.lms.api.model.QuizAutosaveResult;
import com.mysms.lms.api.model.QuizSubmission;
import com.mysms.lms.api.model.QuizViolation;
import com.mysms.lms.api.model.ResumeQuizAttemptRequest;
import com.mysms.lms.api.model.SaveQuizResponsesRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.QuizSubmissionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the QuizSubmission operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class QuizSubmissionController implements QuizSubmissionApi {

    private final QuizSubmissionService quizSubmissionService;

    public QuizSubmissionController(QuizSubmissionService quizSubmissionService) {
        this.quizSubmissionService = quizSubmissionService;
    }

    @Override
    public ResponseEntity<QuizAnswer> getQuizAnswer(UUID quizSubmissionId, UUID quizAnswerId,
            String fields) {
        QuizAnswer found = quizSubmissionService.getQuizAnswer(quizSubmissionId, quizAnswerId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuizSubmission> getQuizSubmission(UUID quizSubmissionId, String fields) {
        QuizSubmission found = quizSubmissionService.getQuizSubmission(quizSubmissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuizViolation> getQuizViolation(UUID quizSubmissionId, UUID quizViolationId,
            String fields) {
        QuizViolation found = quizSubmissionService.getQuizViolation(quizSubmissionId, quizViolationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuizSubmission> gradeQuizAnswers(UUID quizSubmissionId,
            GradeQuizAnswersRequest gradeQuizAnswersRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<QuizAnswer>> listQuizAnswers(UUID quizSubmissionId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuizAnswer> list =
                PagingUtil.pagingInfo(quizSubmissionService.listQuizAnswers(quizSubmissionId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuizSubmission>> listQuizSubmissions(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<QuizSubmission> list =
                PagingUtil.pagingInfo(quizSubmissionService.listQuizSubmissions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuizViolation>> listQuizViolations(UUID quizSubmissionId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuizViolation> list =
                PagingUtil.pagingInfo(quizSubmissionService.listQuizViolations(quizSubmissionId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<QuizSubmission> pauseQuizAttempt(UUID quizSubmissionId,
            PauseQuizAttemptRequest pauseQuizAttemptRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<QuizSubmission> resumeQuizAttempt(UUID quizSubmissionId,
            ResumeQuizAttemptRequest resumeQuizAttemptRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<QuizAutosaveResult> saveQuizResponses(UUID quizSubmissionId,
            SaveQuizResponsesRequest saveQuizResponsesRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }
}
