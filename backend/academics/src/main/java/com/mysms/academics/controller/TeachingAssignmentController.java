package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.TeachingAssignmentService;
import com.mysms.academics.api.TeachingAssignmentApi;
import com.mysms.academics.api.model.TeachingAssignment;
import com.mysms.academics.api.model.TeachingAssignmentUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the TeachingAssignment operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class TeachingAssignmentController implements TeachingAssignmentApi {

    private final TeachingAssignmentService teachingAssignmentService;

    public TeachingAssignmentController(TeachingAssignmentService teachingAssignmentService) {
        this.teachingAssignmentService = teachingAssignmentService;
    }

    @Override
    public ResponseEntity<TeachingAssignment> createTeachingAssignment(
            TeachingAssignment teachingAssignment, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        TeachingAssignment created = teachingAssignmentService.createTeachingAssignment(teachingAssignment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/teaching-assignments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTeachingAssignment(UUID teachingAssignmentId, String ifMatch) {
        teachingAssignmentService.deleteTeachingAssignment(teachingAssignmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TeachingAssignment> getTeachingAssignment(UUID teachingAssignmentId,
            String fields) {
        TeachingAssignment found = teachingAssignmentService.getTeachingAssignment(teachingAssignmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TeachingAssignment>> listTeachingAssignments(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<TeachingAssignment> list =
                PagingUtil.pagingInfo(teachingAssignmentService.listTeachingAssignments(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<TeachingAssignment> updateTeachingAssignment(UUID teachingAssignmentId,
            String ifMatch, TeachingAssignmentUpdate teachingAssignmentUpdate, String updateMask,
            Boolean validateOnly) {
        TeachingAssignment updated =
                teachingAssignmentService.updateTeachingAssignment(teachingAssignmentId, ifMatch,
                        teachingAssignmentUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
