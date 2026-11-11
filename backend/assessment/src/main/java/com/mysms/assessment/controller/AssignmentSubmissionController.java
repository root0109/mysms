package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.AssignmentSubmissionService;
import com.mysms.assessment.api.AssignmentSubmissionApi;
import com.mysms.assessment.api.model.AcceptAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.AssignmentSubmission;
import com.mysms.assessment.api.model.AssignmentSubmissionUpdate;
import com.mysms.assessment.api.model.GradeAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.RejectAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.RequestSubmissionChangesRequest;
import com.mysms.assessment.api.model.ResetAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.SubmitAssignmentSubmissionRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AssignmentSubmission operations: implements the generated contract interface, so paths, parameters
 * and payloads
 * cannot drift from api/openapi/assessment.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * The controller stays thin: delegate business rules to the service and shape contract responses here.
 */
@RestController
public class AssignmentSubmissionController implements AssignmentSubmissionApi {

    private final AssignmentSubmissionService assignmentSubmissionService;

    public AssignmentSubmissionController(AssignmentSubmissionService assignmentSubmissionService) {
        this.assignmentSubmissionService = assignmentSubmissionService;
    }

    @Override
    public ResponseEntity<AssignmentSubmission> acceptAssignmentSubmission(UUID assignmentSubmissionId,
            AcceptAssignmentSubmissionRequest acceptAssignmentSubmissionRequest, UUID idempotencyKey) {
        AssignmentSubmission changed =
                assignmentSubmissionService.acceptAssignmentSubmission(assignmentSubmissionId,
                        acceptAssignmentSubmissionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentSubmission> createAssignmentSubmission(
            AssignmentSubmission assignmentSubmission, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AssignmentSubmission created =
                assignmentSubmissionService.createAssignmentSubmission(assignmentSubmission, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/assignment-submissions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAssignmentSubmission(UUID assignmentSubmissionId, String ifMatch) {
        assignmentSubmissionService.deleteAssignmentSubmission(assignmentSubmissionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AssignmentSubmission> getAssignmentSubmission(UUID assignmentSubmissionId,
            String fields) {
        AssignmentSubmission found = assignmentSubmissionService.getAssignmentSubmission(assignmentSubmissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AssignmentSubmission> gradeAssignmentSubmission(UUID assignmentSubmissionId,
            GradeAssignmentSubmissionRequest gradeAssignmentSubmissionRequest, UUID idempotencyKey) {
        AssignmentSubmission changed = assignmentSubmissionService.gradeAssignmentSubmission(
                assignmentSubmissionId, gradeAssignmentSubmissionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<PagingInfo<AssignmentSubmission>> listAssignmentSubmissions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AssignmentSubmission> list =
                PagingUtil.pagingInfo(assignmentSubmissionService.listAssignmentSubmissions(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AssignmentSubmission> rejectAssignmentSubmission(UUID assignmentSubmissionId,
            RejectAssignmentSubmissionRequest rejectAssignmentSubmissionRequest, UUID idempotencyKey) {
        AssignmentSubmission changed =
                assignmentSubmissionService.rejectAssignmentSubmission(assignmentSubmissionId,
                        rejectAssignmentSubmissionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentSubmission> requestSubmissionChanges(UUID assignmentSubmissionId,
            RequestSubmissionChangesRequest requestSubmissionChangesRequest, UUID idempotencyKey) {
        AssignmentSubmission changed = assignmentSubmissionService.requestSubmissionChanges(
                assignmentSubmissionId, requestSubmissionChangesRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentSubmission> resetAssignmentSubmission(UUID assignmentSubmissionId,
            ResetAssignmentSubmissionRequest resetAssignmentSubmissionRequest, UUID idempotencyKey) {
        AssignmentSubmission changed = assignmentSubmissionService.resetAssignmentSubmission(
                assignmentSubmissionId, resetAssignmentSubmissionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentSubmission> submitAssignmentSubmission(UUID assignmentSubmissionId,
            SubmitAssignmentSubmissionRequest submitAssignmentSubmissionRequest, UUID idempotencyKey) {
        AssignmentSubmission changed =
                assignmentSubmissionService.submitAssignmentSubmission(assignmentSubmissionId,
                        submitAssignmentSubmissionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentSubmission> updateAssignmentSubmission(UUID assignmentSubmissionId,
            String ifMatch, AssignmentSubmissionUpdate assignmentSubmissionUpdate, String updateMask,
            Boolean validateOnly) {
        AssignmentSubmission updated =
                assignmentSubmissionService.updateAssignmentSubmission(assignmentSubmissionId, ifMatch,
                        assignmentSubmissionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
