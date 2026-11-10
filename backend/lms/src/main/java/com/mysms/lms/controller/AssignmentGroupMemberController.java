package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.AssignmentGroupMemberApi;
import com.mysms.lms.api.model.AcceptGroupRequestRequest;
import com.mysms.lms.api.model.AssignmentGroupMember;
import com.mysms.lms.api.model.AssignmentGroupMemberUpdate;
import com.mysms.lms.api.model.RejectGroupRequestRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.AssignmentGroupMemberService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AssignmentGroupMember operations: implements the generated contract interface, so paths, parameters
 * and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class AssignmentGroupMemberController implements AssignmentGroupMemberApi {

    private final AssignmentGroupMemberService assignmentGroupMemberService;

    public AssignmentGroupMemberController(AssignmentGroupMemberService assignmentGroupMemberService) {
        this.assignmentGroupMemberService = assignmentGroupMemberService;
    }

    @Override
    public ResponseEntity<AssignmentGroupMember> acceptGroupRequest(UUID assignmentGroupMemberId,
            AcceptGroupRequestRequest acceptGroupRequestRequest, UUID idempotencyKey) {
        AssignmentGroupMember changed =
                assignmentGroupMemberService.acceptGroupRequest(assignmentGroupMemberId, acceptGroupRequestRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentGroupMember> createAssignmentGroupMember(
            AssignmentGroupMember assignmentGroupMember, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AssignmentGroupMember created =
                assignmentGroupMemberService.createAssignmentGroupMember(assignmentGroupMember, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/assignment-group-members/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAssignmentGroupMember(UUID assignmentGroupMemberId,
            String ifMatch) {
        assignmentGroupMemberService.deleteAssignmentGroupMember(assignmentGroupMemberId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AssignmentGroupMember> getAssignmentGroupMember(UUID assignmentGroupMemberId,
            String fields) {
        AssignmentGroupMember found = assignmentGroupMemberService.getAssignmentGroupMember(assignmentGroupMemberId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AssignmentGroupMember>> listAssignmentGroupMembers(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AssignmentGroupMember> list =
                PagingUtil.pagingInfo(assignmentGroupMemberService.listAssignmentGroupMembers(page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AssignmentGroupMember> rejectGroupRequest(UUID assignmentGroupMemberId,
            RejectGroupRequestRequest rejectGroupRequestRequest, UUID idempotencyKey) {
        AssignmentGroupMember changed =
                assignmentGroupMemberService.rejectGroupRequest(assignmentGroupMemberId, rejectGroupRequestRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentGroupMember> updateAssignmentGroupMember(UUID assignmentGroupMemberId,
            String ifMatch, AssignmentGroupMemberUpdate assignmentGroupMemberUpdate, String updateMask,
            Boolean validateOnly) {
        AssignmentGroupMember updated =
                assignmentGroupMemberService.updateAssignmentGroupMember(assignmentGroupMemberId, ifMatch,
                        assignmentGroupMemberUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
