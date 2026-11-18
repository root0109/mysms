package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.HomeworkService;
import com.mysms.assessment.business.HomeworkWorkflowService;
import com.mysms.assessment.api.HomeworkApi;
import com.mysms.assessment.api.model.ApproveHomeworkRequest;
import com.mysms.assessment.api.model.Homework;
import com.mysms.assessment.api.model.HomeworkApproval;
import com.mysms.assessment.api.model.HomeworkUpdate;
import com.mysms.assessment.api.model.PublishHomeworkRequest;
import com.mysms.assessment.api.model.RejectHomeworkRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Homework operations; authentication and the permission check already ran in the auth-client filter.
 */
@RestController
public class HomeworkController implements HomeworkApi {

    private final HomeworkService homeworkService;
    private final HomeworkWorkflowService homeworkWorkflowService;

    public HomeworkController(HomeworkService homeworkService, HomeworkWorkflowService homeworkWorkflowService) {
        this.homeworkService = homeworkService;
        this.homeworkWorkflowService = homeworkWorkflowService;
    }

    @Override
    public ResponseEntity<Homework> approveHomework(UUID homeworkId,
            ApproveHomeworkRequest approveHomeworkRequest, UUID idempotencyKey) {
        Homework approved = homeworkWorkflowService.approveHomework(homeworkId, approveHomeworkRequest);
        return ResponseEntity.ok().eTag(approved.getEtag()).body(approved);
    }

    @Override
    public ResponseEntity<Homework> createHomework(Homework homework, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Homework created = homeworkService.createHomework(homework, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/homeworks/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteHomework(UUID homeworkId, String ifMatch) {
        homeworkService.deleteHomework(homeworkId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Homework> getHomework(UUID homeworkId, String fields) {
        Homework found = homeworkService.getHomework(homeworkId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<HomeworkApproval> getHomeworkApproval(UUID homeworkId, UUID homeworkApprovalId,
            String fields) {
        HomeworkApproval found = homeworkService.getHomeworkApproval(homeworkId, homeworkApprovalId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<HomeworkApproval>> listHomeworkApprovals(UUID homeworkId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<HomeworkApproval> list =
                PagingUtil.pagingInfo(homeworkService.listHomeworkApprovals(homeworkId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Homework>> listHomeworks(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Homework> list =
                PagingUtil.pagingInfo(homeworkService.listHomeworks(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Homework> publishHomework(UUID homeworkId,
            PublishHomeworkRequest publishHomeworkRequest, UUID idempotencyKey) {
        Homework published = homeworkWorkflowService.publishHomework(homeworkId, publishHomeworkRequest);
        return ResponseEntity.ok().eTag(published.getEtag()).body(published);
    }

    @Override
    public ResponseEntity<Homework> rejectHomework(UUID homeworkId,
            RejectHomeworkRequest rejectHomeworkRequest, UUID idempotencyKey) {
        Homework rejected = homeworkWorkflowService.rejectHomework(homeworkId, rejectHomeworkRequest);
        return ResponseEntity.ok().eTag(rejected.getEtag()).body(rejected);
    }

    @Override
    public ResponseEntity<Homework> updateHomework(UUID homeworkId, String ifMatch,
            HomeworkUpdate homeworkUpdate, String updateMask, Boolean validateOnly) {
        Homework updated = homeworkService.updateHomework(homeworkId, ifMatch, homeworkUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
