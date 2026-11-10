package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.TagAssignmentApi;
import com.mysms.platform.api.model.TagAssignment;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.TagAssignmentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the TagAssignment operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/platform.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class TagAssignmentController implements TagAssignmentApi {

    private final TagAssignmentService tagAssignmentService;

    public TagAssignmentController(TagAssignmentService tagAssignmentService) {
        this.tagAssignmentService = tagAssignmentService;
    }

    @Override
    public ResponseEntity<TagAssignment> createTagAssignment(TagAssignment tagAssignment,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        TagAssignment created = tagAssignmentService.createTagAssignment(tagAssignment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTagAssignment(UUID tagAssignmentId, String ifMatch) {
        tagAssignmentService.deleteTagAssignment(tagAssignmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagingInfo<TagAssignment>> listTagAssignments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<TagAssignment> list =
                PagingUtil.pagingInfo(tagAssignmentService.listTagAssignments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
