package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.RubricAssessmentApi;
import com.mysms.lms.api.model.RubricAssessment;
import com.mysms.lms.api.model.RubricAssessmentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.RubricAssessmentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the RubricAssessment operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class RubricAssessmentController implements RubricAssessmentApi {

    private final RubricAssessmentService rubricAssessmentService;

    public RubricAssessmentController(RubricAssessmentService rubricAssessmentService) {
        this.rubricAssessmentService = rubricAssessmentService;
    }

    @Override
    public ResponseEntity<RubricAssessment> createRubricAssessment(RubricAssessment rubricAssessment,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RubricAssessment created = rubricAssessmentService.createRubricAssessment(rubricAssessment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/rubric-assessments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteRubricAssessment(UUID rubricAssessmentId, String ifMatch) {
        rubricAssessmentService.deleteRubricAssessment(rubricAssessmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<RubricAssessment> getRubricAssessment(UUID rubricAssessmentId, String fields) {
        RubricAssessment found = rubricAssessmentService.getRubricAssessment(rubricAssessmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RubricAssessment>> listRubricAssessments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<RubricAssessment> list =
                PagingUtil.pagingInfo(rubricAssessmentService.listRubricAssessments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<RubricAssessment> updateRubricAssessment(UUID rubricAssessmentId,
            String ifMatch, RubricAssessmentUpdate rubricAssessmentUpdate, String updateMask, Boolean validateOnly) {
        RubricAssessment updated =
                rubricAssessmentService.updateRubricAssessment(rubricAssessmentId, ifMatch, rubricAssessmentUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
