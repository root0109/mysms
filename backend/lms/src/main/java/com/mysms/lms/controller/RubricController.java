package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.RubricApi;
import com.mysms.lms.api.model.Rubric;
import com.mysms.lms.api.model.RubricCell;
import com.mysms.lms.api.model.RubricCellUpdate;
import com.mysms.lms.api.model.RubricCriterion;
import com.mysms.lms.api.model.RubricCriterionUpdate;
import com.mysms.lms.api.model.RubricLevel;
import com.mysms.lms.api.model.RubricLevelUpdate;
import com.mysms.lms.api.model.RubricUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.RubricService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Rubric operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class RubricController implements RubricApi {

    private final RubricService rubricService;

    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    @Override
    public ResponseEntity<Rubric> createRubric(Rubric rubric, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Rubric created = rubricService.createRubric(rubric, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/rubrics/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<RubricCell> createRubricCell(UUID rubricId, RubricCell rubricCell,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RubricCell created = rubricService.createRubricCell(rubricId, rubricCell, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/rubrics/" + rubricId + "/cells/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<RubricCriterion> createRubricCriterion(UUID rubricId,
            RubricCriterion rubricCriterion, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RubricCriterion created = rubricService.createRubricCriterion(rubricId, rubricCriterion, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/rubrics/" + rubricId + "/criteria/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<RubricLevel> createRubricLevel(UUID rubricId, RubricLevel rubricLevel,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RubricLevel created = rubricService.createRubricLevel(rubricId, rubricLevel, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/rubrics/" + rubricId + "/levels/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteRubric(UUID rubricId, String ifMatch) {
        rubricService.deleteRubric(rubricId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRubricCell(UUID rubricId, UUID rubricCellId, String ifMatch) {
        rubricService.deleteRubricCell(rubricId, rubricCellId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRubricCriterion(UUID rubricId, UUID rubricCriterionId,
            String ifMatch) {
        rubricService.deleteRubricCriterion(rubricId, rubricCriterionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRubricLevel(UUID rubricId, UUID rubricLevelId, String ifMatch) {
        rubricService.deleteRubricLevel(rubricId, rubricLevelId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Rubric> getRubric(UUID rubricId, String fields) {
        Rubric found = rubricService.getRubric(rubricId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RubricCell> getRubricCell(UUID rubricId, UUID rubricCellId, String fields) {
        RubricCell found = rubricService.getRubricCell(rubricId, rubricCellId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RubricCriterion> getRubricCriterion(UUID rubricId, UUID rubricCriterionId,
            String fields) {
        RubricCriterion found = rubricService.getRubricCriterion(rubricId, rubricCriterionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RubricLevel> getRubricLevel(UUID rubricId, UUID rubricLevelId, String fields) {
        RubricLevel found = rubricService.getRubricLevel(rubricId, rubricLevelId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RubricCell>> listRubricCells(UUID rubricId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RubricCell> list =
                PagingUtil.pagingInfo(rubricService.listRubricCells(rubricId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<RubricCriterion>> listRubricCriteria(UUID rubricId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RubricCriterion> list =
                PagingUtil.pagingInfo(rubricService.listRubricCriterions(rubricId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<RubricLevel>> listRubricLevels(UUID rubricId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RubricLevel> list =
                PagingUtil.pagingInfo(rubricService.listRubricLevels(rubricId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Rubric>> listRubrics(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Rubric> list = PagingUtil.pagingInfo(rubricService.listRubrics(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Rubric> updateRubric(UUID rubricId, String ifMatch, RubricUpdate rubricUpdate,
            String updateMask, Boolean validateOnly) {
        Rubric updated = rubricService.updateRubric(rubricId, ifMatch, rubricUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RubricCell> updateRubricCell(UUID rubricId, UUID rubricCellId, String ifMatch,
            RubricCellUpdate rubricCellUpdate, String updateMask, Boolean validateOnly) {
        RubricCell updated =
                rubricService.updateRubricCell(rubricId, rubricCellId, ifMatch, rubricCellUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RubricCriterion> updateRubricCriterion(UUID rubricId, UUID rubricCriterionId,
            String ifMatch, RubricCriterionUpdate rubricCriterionUpdate, String updateMask, Boolean validateOnly) {
        RubricCriterion updated =
                rubricService.updateRubricCriterion(rubricId, rubricCriterionId, ifMatch,
                        rubricCriterionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RubricLevel> updateRubricLevel(UUID rubricId, UUID rubricLevelId,
            String ifMatch, RubricLevelUpdate rubricLevelUpdate, String updateMask, Boolean validateOnly) {
        RubricLevel updated =
                rubricService.updateRubricLevel(rubricId, rubricLevelId, ifMatch, rubricLevelUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
