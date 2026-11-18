package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.QuestionBlankAnswerApi;
import com.mysms.lms.api.model.QuestionBlankAnswer;
import com.mysms.lms.api.model.QuestionBlankAnswerUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.QuestionBlankAnswerService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the QuestionBlankAnswer operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class QuestionBlankAnswerController implements QuestionBlankAnswerApi {

    private final QuestionBlankAnswerService questionBlankAnswerService;

    public QuestionBlankAnswerController(QuestionBlankAnswerService questionBlankAnswerService) {
        this.questionBlankAnswerService = questionBlankAnswerService;
    }

    @Override
    public ResponseEntity<QuestionBlankAnswer> createQuestionBlankAnswer(
            QuestionBlankAnswer questionBlankAnswer, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionBlankAnswer created = questionBlankAnswerService.createQuestionBlankAnswer(questionBlankAnswer, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/question-blank-answers/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteQuestionBlankAnswer(UUID questionBlankAnswerId, String ifMatch) {
        questionBlankAnswerService.deleteQuestionBlankAnswer(questionBlankAnswerId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<QuestionBlankAnswer> getQuestionBlankAnswer(UUID questionBlankAnswerId,
            String fields) {
        QuestionBlankAnswer found = questionBlankAnswerService.getQuestionBlankAnswer(questionBlankAnswerId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionBlankAnswer>> listQuestionBlankAnswers(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionBlankAnswer> list =
                PagingUtil.pagingInfo(questionBlankAnswerService.listQuestionBlankAnswers(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<QuestionBlankAnswer> updateQuestionBlankAnswer(UUID questionBlankAnswerId,
            String ifMatch, QuestionBlankAnswerUpdate questionBlankAnswerUpdate, String updateMask,
            Boolean validateOnly) {
        QuestionBlankAnswer updated =
                questionBlankAnswerService.updateQuestionBlankAnswer(questionBlankAnswerId, ifMatch,
                        questionBlankAnswerUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
