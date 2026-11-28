package com.mysms.academics.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.business.AssignmentService;
import com.mysms.academics.assessment.api.AssignmentApi;
import com.mysms.academics.assessment.api.model.ApproveAssignmentRequest;
import com.mysms.academics.assessment.api.model.Assignment;
import com.mysms.academics.assessment.api.model.AssignmentAllocation;
import com.mysms.academics.assessment.api.model.AssignmentAllocationUpdate;
import com.mysms.academics.assessment.api.model.AssignmentApproval;
import com.mysms.academics.assessment.api.model.AssignmentSubject;
import com.mysms.academics.assessment.api.model.AssignmentSubjectUpdate;
import com.mysms.academics.assessment.api.model.AssignmentUpdate;
import com.mysms.academics.assessment.api.model.CancelAssignmentRequest;
import com.mysms.academics.assessment.api.model.FinishAssignmentRequest;
import com.mysms.academics.assessment.api.model.PublishAssignmentRequest;
import com.mysms.academics.assessment.api.model.RejectAssignmentRequest;
import com.mysms.academics.assessment.api.model.RequestAssignmentApprovalRequest;
import com.mysms.academics.assessment.api.model.ResetAssignmentRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Assignment operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class AssignmentController implements AssignmentApi {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @Override
    public ResponseEntity<Assignment> approveAssignment(UUID assignmentId,
            ApproveAssignmentRequest approveAssignmentRequest, UUID idempotencyKey) {
        Assignment changed = assignmentService.approveAssignment(assignmentId, approveAssignmentRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Assignment> cancelAssignment(UUID assignmentId,
            CancelAssignmentRequest cancelAssignmentRequest, UUID idempotencyKey) {
        Assignment changed = assignmentService.cancelAssignment(assignmentId, cancelAssignmentRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Assignment> createAssignment(Assignment assignment, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Assignment created = assignmentService.createAssignment(assignment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/assignments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<AssignmentAllocation> createAssignmentAllocation(UUID assignmentId,
            AssignmentAllocation assignmentAllocation, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AssignmentAllocation created =
                assignmentService.createAssignmentAllocation(assignmentId, assignmentAllocation, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/assessment/assignments/" + assignmentId + "/allocations/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<AssignmentSubject> createAssignmentSubject(UUID assignmentId,
            AssignmentSubject assignmentSubject, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AssignmentSubject created =
                assignmentService.createAssignmentSubject(assignmentId, assignmentSubject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/assessment/assignments/" + assignmentId + "/subjects/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAssignment(UUID assignmentId, String ifMatch) {
        assignmentService.deleteAssignment(assignmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAssignmentAllocation(UUID assignmentId, UUID assignmentAllocationId,
            String ifMatch) {
        assignmentService.deleteAssignmentAllocation(assignmentId, assignmentAllocationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAssignmentSubject(UUID assignmentId, UUID assignmentSubjectId,
            String ifMatch) {
        assignmentService.deleteAssignmentSubject(assignmentId, assignmentSubjectId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Assignment> finishAssignment(UUID assignmentId,
            FinishAssignmentRequest finishAssignmentRequest, UUID idempotencyKey) {
        Assignment changed = assignmentService.finishAssignment(assignmentId, finishAssignmentRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Assignment> getAssignment(UUID assignmentId, String fields) {
        Assignment found = assignmentService.getAssignment(assignmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AssignmentAllocation> getAssignmentAllocation(UUID assignmentId,
            UUID assignmentAllocationId, String fields) {
        AssignmentAllocation found =
                assignmentService.getAssignmentAllocation(assignmentId, assignmentAllocationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AssignmentApproval> getAssignmentApproval(UUID assignmentId,
            UUID assignmentApprovalId, String fields) {
        AssignmentApproval found = assignmentService.getAssignmentApproval(assignmentId, assignmentApprovalId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AssignmentSubject> getAssignmentSubject(UUID assignmentId,
            UUID assignmentSubjectId, String fields) {
        AssignmentSubject found = assignmentService.getAssignmentSubject(assignmentId, assignmentSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AssignmentAllocation>> listAssignmentAllocations(UUID assignmentId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AssignmentAllocation> list =
                PagingUtil.pagingInfo(assignmentService.listAssignmentAllocations(assignmentId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<AssignmentApproval>> listAssignmentApprovals(UUID assignmentId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AssignmentApproval> list =
                PagingUtil.pagingInfo(assignmentService.listAssignmentApprovals(assignmentId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<AssignmentSubject>> listAssignmentSubjects(UUID assignmentId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AssignmentSubject> list =
                PagingUtil.pagingInfo(assignmentService.listAssignmentSubjects(assignmentId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Assignment>> listAssignments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Assignment> list =
                PagingUtil.pagingInfo(assignmentService.listAssignments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Assignment> publishAssignment(UUID assignmentId,
            PublishAssignmentRequest publishAssignmentRequest, UUID idempotencyKey) {
        Assignment changed = assignmentService.publishAssignment(assignmentId, publishAssignmentRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Assignment> rejectAssignment(UUID assignmentId,
            RejectAssignmentRequest rejectAssignmentRequest, UUID idempotencyKey) {
        Assignment changed = assignmentService.rejectAssignment(assignmentId, rejectAssignmentRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Assignment> requestAssignmentApproval(UUID assignmentId,
            RequestAssignmentApprovalRequest requestAssignmentApprovalRequest, UUID idempotencyKey) {
        Assignment unchanged =
                assignmentService.requestAssignmentApproval(assignmentId, requestAssignmentApprovalRequest);
        return ResponseEntity.ok().eTag(unchanged.getEtag()).body(unchanged);
    }

    @Override
    public ResponseEntity<Assignment> resetAssignment(UUID assignmentId,
            ResetAssignmentRequest resetAssignmentRequest, UUID idempotencyKey) {
        Assignment changed = assignmentService.resetAssignment(assignmentId, resetAssignmentRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Assignment> updateAssignment(UUID assignmentId, String ifMatch,
            AssignmentUpdate assignmentUpdate, Boolean validateOnly) {
        Assignment updated = assignmentService.updateAssignment(assignmentId, ifMatch, assignmentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<AssignmentAllocation> updateAssignmentAllocation(UUID assignmentId,
            UUID assignmentAllocationId, String ifMatch, AssignmentAllocationUpdate assignmentAllocationUpdate,
            Boolean validateOnly) {
        AssignmentAllocation updated =
                assignmentService.updateAssignmentAllocation(assignmentId, assignmentAllocationId, ifMatch,
                        assignmentAllocationUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<AssignmentSubject> updateAssignmentSubject(UUID assignmentId,
            UUID assignmentSubjectId, String ifMatch, AssignmentSubjectUpdate assignmentSubjectUpdate,
            Boolean validateOnly) {
        AssignmentSubject updated =
                assignmentService.updateAssignmentSubject(assignmentId, assignmentSubjectId, ifMatch,
                        assignmentSubjectUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
