package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.LmsAssignmentSubmissionApi;
import com.mysms.lms.api.model.GradeLearningSubmissionRequest;
import com.mysms.lms.api.model.LmsAssignmentSubmission;
import com.mysms.lms.api.model.LmsAssignmentSubmissionUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.LmsAssignmentSubmissionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LmsAssignmentSubmission operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class LmsAssignmentSubmissionController implements LmsAssignmentSubmissionApi {

    private final LmsAssignmentSubmissionService lmsAssignmentSubmissionService;

    public LmsAssignmentSubmissionController(LmsAssignmentSubmissionService lmsAssignmentSubmissionService) {
        this.lmsAssignmentSubmissionService = lmsAssignmentSubmissionService;
    }

    @Override
    public ResponseEntity<LmsAssignmentSubmission> createLmsAssignmentSubmission(
            LmsAssignmentSubmission lmsAssignmentSubmission, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LmsAssignmentSubmission created =
                lmsAssignmentSubmissionService.createLmsAssignmentSubmission(lmsAssignmentSubmission, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/assignment-submissions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLmsAssignmentSubmission(UUID assignmentSubmissionId,
            String ifMatch) {
        lmsAssignmentSubmissionService.deleteLmsAssignmentSubmission(assignmentSubmissionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LmsAssignmentSubmission> finalizeSubmission(UUID assignmentSubmissionId,
            Object body, UUID idempotencyKey) {
        LmsAssignmentSubmission changed = lmsAssignmentSubmissionService.finalizeSubmission(assignmentSubmissionId);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LmsAssignmentSubmission> getLmsAssignmentSubmission(UUID assignmentSubmissionId,
            String fields) {
        LmsAssignmentSubmission found =
                lmsAssignmentSubmissionService.getLmsAssignmentSubmission(assignmentSubmissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LmsAssignmentSubmission> gradeLearningSubmission(UUID assignmentSubmissionId,
            GradeLearningSubmissionRequest gradeLearningSubmissionRequest, UUID idempotencyKey) {
        LmsAssignmentSubmission changed = lmsAssignmentSubmissionService.gradeLearningSubmission(
                assignmentSubmissionId, gradeLearningSubmissionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<PagingInfo<LmsAssignmentSubmission>> listLmsAssignmentSubmissions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LmsAssignmentSubmission> list =
                PagingUtil.pagingInfo(lmsAssignmentSubmissionService.listLmsAssignmentSubmissions(page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LmsAssignmentSubmission> updateLmsAssignmentSubmission(
            UUID assignmentSubmissionId, String ifMatch, LmsAssignmentSubmissionUpdate lmsAssignmentSubmissionUpdate,
            String updateMask, Boolean validateOnly) {
        LmsAssignmentSubmission updated =
                lmsAssignmentSubmissionService.updateLmsAssignmentSubmission(assignmentSubmissionId, ifMatch,
                        lmsAssignmentSubmissionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
