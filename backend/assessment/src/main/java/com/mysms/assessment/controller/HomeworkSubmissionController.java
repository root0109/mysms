package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.HomeworkSubmissionService;
import com.mysms.assessment.business.HomeworkWorkflowService;
import com.mysms.assessment.api.HomeworkSubmissionApi;
import com.mysms.assessment.api.model.CheckHomeworkSubmissionRequest;
import com.mysms.assessment.api.model.HomeworkSubmission;
import com.mysms.assessment.api.model.HomeworkSubmissionUpdate;
import com.mysms.assessment.api.model.ReplyToHomeworkFeedbackRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the HomeworkSubmission operations; authentication and the permission check already ran in the
 * auth-client filter.
 */
@RestController
public class HomeworkSubmissionController implements HomeworkSubmissionApi {

    private final HomeworkSubmissionService homeworkSubmissionService;

    private final HomeworkWorkflowService homeworkWorkflowService;

    public HomeworkSubmissionController(HomeworkSubmissionService homeworkSubmissionService,
            HomeworkWorkflowService homeworkWorkflowService) {
        this.homeworkSubmissionService = homeworkSubmissionService;
        this.homeworkWorkflowService = homeworkWorkflowService;
    }

    @Override
    public ResponseEntity<HomeworkSubmission> checkHomeworkSubmission(UUID homeworkSubmissionId,
            CheckHomeworkSubmissionRequest checkHomeworkSubmissionRequest, UUID idempotencyKey) {
        HomeworkSubmission checked =
                homeworkWorkflowService.checkHomeworkSubmission(homeworkSubmissionId, checkHomeworkSubmissionRequest);
        return ResponseEntity.ok().eTag(checked.getEtag()).body(checked);
    }

    @Override
    public ResponseEntity<HomeworkSubmission> createHomeworkSubmission(
            HomeworkSubmission homeworkSubmission, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        HomeworkSubmission created = homeworkSubmissionService.createHomeworkSubmission(homeworkSubmission, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/homework-submissions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteHomeworkSubmission(UUID homeworkSubmissionId, String ifMatch) {
        homeworkSubmissionService.deleteHomeworkSubmission(homeworkSubmissionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<HomeworkSubmission> getHomeworkSubmission(UUID homeworkSubmissionId,
            String fields) {
        HomeworkSubmission found = homeworkSubmissionService.getHomeworkSubmission(homeworkSubmissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<HomeworkSubmission>> listHomeworkSubmissions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<HomeworkSubmission> list =
                PagingUtil.pagingInfo(homeworkSubmissionService.listHomeworkSubmissions(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<HomeworkSubmission> replyToHomeworkFeedback(UUID homeworkSubmissionId,
            ReplyToHomeworkFeedbackRequest replyToHomeworkFeedbackRequest, UUID idempotencyKey) {
        HomeworkSubmission replied =
                homeworkWorkflowService.replyToHomeworkFeedback(homeworkSubmissionId, replyToHomeworkFeedbackRequest);
        return ResponseEntity.ok().eTag(replied.getEtag()).body(replied);
    }

    @Override
    public ResponseEntity<HomeworkSubmission> updateHomeworkSubmission(UUID homeworkSubmissionId,
            String ifMatch, HomeworkSubmissionUpdate homeworkSubmissionUpdate, String updateMask,
            Boolean validateOnly) {
        HomeworkSubmission updated =
                homeworkSubmissionService.updateHomeworkSubmission(homeworkSubmissionId, ifMatch,
                        homeworkSubmissionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
