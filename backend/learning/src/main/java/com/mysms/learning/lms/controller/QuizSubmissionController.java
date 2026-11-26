package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.QuizSubmissionApi;
import com.mysms.learning.lms.api.model.GradeQuizAnswersRequest;
import com.mysms.learning.lms.api.model.PauseQuizAttemptRequest;
import com.mysms.learning.lms.api.model.QuizAnswer;
import com.mysms.learning.lms.api.model.QuizAutosaveResult;
import com.mysms.learning.lms.api.model.QuizSubmission;
import com.mysms.learning.lms.api.model.QuizViolation;
import com.mysms.learning.lms.api.model.ResumeQuizAttemptRequest;
import com.mysms.learning.lms.api.model.SaveQuizResponsesRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.QuizAttemptService;
import com.mysms.learning.lms.business.QuizSubmissionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the QuizSubmission operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class QuizSubmissionController implements QuizSubmissionApi {

    private final QuizSubmissionService quizSubmissionService;
    private final QuizAttemptService quizAttemptService;

    public QuizSubmissionController(QuizSubmissionService quizSubmissionService,
            QuizAttemptService quizAttemptService) {
        this.quizSubmissionService = quizSubmissionService;
        this.quizAttemptService = quizAttemptService;
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
        QuizSubmission changed = quizAttemptService.gradeQuizAnswers(quizSubmissionId, gradeQuizAnswersRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
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
        QuizSubmission changed = quizAttemptService.pauseQuizAttempt(quizSubmissionId, pauseQuizAttemptRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<QuizSubmission> resumeQuizAttempt(UUID quizSubmissionId,
            ResumeQuizAttemptRequest resumeQuizAttemptRequest, UUID idempotencyKey) {
        QuizSubmission changed = quizAttemptService.resumeQuizAttempt(quizSubmissionId, resumeQuizAttemptRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<QuizAutosaveResult> saveQuizResponses(UUID quizSubmissionId,
            SaveQuizResponsesRequest saveQuizResponsesRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(quizAttemptService.saveQuizResponses(quizSubmissionId, saveQuizResponsesRequest));
    }
}
