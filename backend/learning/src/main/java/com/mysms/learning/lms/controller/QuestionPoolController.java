package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.QuestionPoolApi;
import com.mysms.learning.lms.api.model.QuestionPool;
import com.mysms.learning.lms.api.model.QuestionPoolItem;
import com.mysms.learning.lms.api.model.QuestionPoolItemUpdate;
import com.mysms.learning.lms.api.model.QuestionPoolUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.QuestionPoolService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the QuestionPool operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class QuestionPoolController implements QuestionPoolApi {

    private final QuestionPoolService questionPoolService;

    public QuestionPoolController(QuestionPoolService questionPoolService) {
        this.questionPoolService = questionPoolService;
    }

    @Override
    public ResponseEntity<QuestionPool> createQuestionPool(QuestionPool questionPool, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionPool created = questionPoolService.createQuestionPool(questionPool, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/question-pools/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<QuestionPoolItem> createQuestionPoolItem(UUID questionPoolId,
            QuestionPoolItem questionPoolItem, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionPoolItem created =
                questionPoolService.createQuestionPoolItem(questionPoolId, questionPoolItem, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/question-pools/" + questionPoolId + "/items/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteQuestionPool(UUID questionPoolId, String ifMatch) {
        questionPoolService.deleteQuestionPool(questionPoolId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteQuestionPoolItem(UUID questionPoolId, UUID questionPoolItemId,
            String ifMatch) {
        questionPoolService.deleteQuestionPoolItem(questionPoolId, questionPoolItemId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<QuestionPool> getQuestionPool(UUID questionPoolId, String fields) {
        QuestionPool found = questionPoolService.getQuestionPool(questionPoolId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<QuestionPoolItem> getQuestionPoolItem(UUID questionPoolId,
            UUID questionPoolItemId, String fields) {
        QuestionPoolItem found = questionPoolService.getQuestionPoolItem(questionPoolId, questionPoolItemId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionPoolItem>> listQuestionPoolItems(UUID questionPoolId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionPoolItem> list =
                PagingUtil.pagingInfo(questionPoolService.listQuestionPoolItems(questionPoolId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionPool>> listQuestionPools(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<QuestionPool> list =
                PagingUtil.pagingInfo(questionPoolService.listQuestionPools(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<QuestionPool> updateQuestionPool(UUID questionPoolId, String ifMatch,
            QuestionPoolUpdate questionPoolUpdate, Boolean validateOnly) {
        QuestionPool updated =
                questionPoolService.updateQuestionPool(questionPoolId, ifMatch, questionPoolUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<QuestionPoolItem> updateQuestionPoolItem(UUID questionPoolId,
            UUID questionPoolItemId, String ifMatch, QuestionPoolItemUpdate questionPoolItemUpdate, Boolean validateOnly) {
        QuestionPoolItem updated =
                questionPoolService.updateQuestionPoolItem(questionPoolId, questionPoolItemId, ifMatch,
                        questionPoolItemUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
