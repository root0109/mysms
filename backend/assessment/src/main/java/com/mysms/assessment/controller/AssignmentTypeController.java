package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.AssignmentTypeService;
import com.mysms.assessment.api.AssignmentTypeApi;
import com.mysms.assessment.api.model.AssignmentType;
import com.mysms.assessment.api.model.AssignmentTypeUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AssignmentType operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class AssignmentTypeController implements AssignmentTypeApi {

    private final AssignmentTypeService assignmentTypeService;

    public AssignmentTypeController(AssignmentTypeService assignmentTypeService) {
        this.assignmentTypeService = assignmentTypeService;
    }

    @Override
    public ResponseEntity<AssignmentType> createAssignmentType(AssignmentType assignmentType,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AssignmentType created = assignmentTypeService.createAssignmentType(assignmentType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/assignment-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAssignmentType(UUID assignmentTypeId, String ifMatch) {
        assignmentTypeService.deleteAssignmentType(assignmentTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AssignmentType> getAssignmentType(UUID assignmentTypeId, String fields) {
        AssignmentType found = assignmentTypeService.getAssignmentType(assignmentTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AssignmentType>> listAssignmentTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AssignmentType> list =
                PagingUtil.pagingInfo(assignmentTypeService.listAssignmentTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AssignmentType> updateAssignmentType(UUID assignmentTypeId, String ifMatch,
            AssignmentTypeUpdate assignmentTypeUpdate, String updateMask, Boolean validateOnly) {
        AssignmentType updated =
                assignmentTypeService.updateAssignmentType(assignmentTypeId, ifMatch, assignmentTypeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
