package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.LearningObjectiveApi;
import com.mysms.lms.api.model.LearningObjective;
import com.mysms.lms.api.model.LearningObjectiveUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.LearningObjectiveService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LearningObjective operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class LearningObjectiveController implements LearningObjectiveApi {

    private final LearningObjectiveService learningObjectiveService;

    public LearningObjectiveController(LearningObjectiveService learningObjectiveService) {
        this.learningObjectiveService = learningObjectiveService;
    }

    @Override
    public ResponseEntity<LearningObjective> createLearningObjective(LearningObjective learningObjective,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LearningObjective created = learningObjectiveService.createLearningObjective(learningObjective, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/learning-objectives/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLearningObjective(UUID learningObjectiveId, String ifMatch) {
        learningObjectiveService.deleteLearningObjective(learningObjectiveId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LearningObjective> getLearningObjective(UUID learningObjectiveId,
            String fields) {
        LearningObjective found = learningObjectiveService.getLearningObjective(learningObjectiveId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LearningObjective>> listLearningObjectives(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LearningObjective> list =
                PagingUtil.pagingInfo(learningObjectiveService.listLearningObjectives(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LearningObjective> updateLearningObjective(UUID learningObjectiveId,
            String ifMatch, LearningObjectiveUpdate learningObjectiveUpdate, String updateMask, Boolean validateOnly) {
        LearningObjective updated =
                learningObjectiveService.updateLearningObjective(learningObjectiveId, ifMatch, learningObjectiveUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
