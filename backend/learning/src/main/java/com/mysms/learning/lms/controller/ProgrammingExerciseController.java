package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.business.ExerciseSubmissionService;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ProgrammingExerciseApi;
import com.mysms.learning.lms.api.model.ExerciseSubmission;
import com.mysms.learning.lms.api.model.ExerciseTestCase;
import com.mysms.learning.lms.api.model.ExerciseTestCaseUpdate;
import com.mysms.learning.lms.api.model.ProgrammingExercise;
import com.mysms.learning.lms.api.model.ProgrammingExerciseUpdate;
import com.mysms.learning.lms.api.model.SubmitExerciseRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ProgrammingExerciseService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ProgrammingExercise operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class ProgrammingExerciseController implements ProgrammingExerciseApi {

    private final ProgrammingExerciseService programmingExerciseService;
    private final ExerciseSubmissionService exerciseSubmissionService;

    public ProgrammingExerciseController(ProgrammingExerciseService programmingExerciseService,
            ExerciseSubmissionService exerciseSubmissionService) {
        this.exerciseSubmissionService = exerciseSubmissionService;
        this.programmingExerciseService = programmingExerciseService;
    }

    @Override
    public ResponseEntity<ExerciseTestCase> createExerciseTestCase(UUID programmingExerciseId,
            ExerciseTestCase exerciseTestCase, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExerciseTestCase created =
                programmingExerciseService.createExerciseTestCase(programmingExerciseId, exerciseTestCase, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/programming-exercises/" + programmingExerciseId + "/test-cases/"
                        + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ProgrammingExercise> createProgrammingExercise(
            ProgrammingExercise programmingExercise, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ProgrammingExercise created = programmingExerciseService.createProgrammingExercise(programmingExercise, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/programming-exercises/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteExerciseTestCase(UUID programmingExerciseId,
            UUID exerciseTestCaseId, String ifMatch) {
        programmingExerciseService.deleteExerciseTestCase(programmingExerciseId, exerciseTestCaseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteProgrammingExercise(UUID programmingExerciseId, String ifMatch) {
        programmingExerciseService.deleteProgrammingExercise(programmingExerciseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ExerciseTestCase> getExerciseTestCase(UUID programmingExerciseId,
            UUID exerciseTestCaseId, String fields) {
        ExerciseTestCase found = programmingExerciseService.getExerciseTestCase(programmingExerciseId, exerciseTestCaseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ProgrammingExercise> getProgrammingExercise(UUID programmingExerciseId,
            String fields) {
        ProgrammingExercise found = programmingExerciseService.getProgrammingExercise(programmingExerciseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ExerciseTestCase>> listExerciseTestCases(UUID programmingExerciseId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExerciseTestCase> list =
                PagingUtil.pagingInfo(programmingExerciseService.listExerciseTestCases(programmingExerciseId, page,
                        size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ProgrammingExercise>> listProgrammingExercises(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ProgrammingExercise> list =
                PagingUtil.pagingInfo(programmingExerciseService.listProgrammingExercises(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ExerciseSubmission> submitExercise(UUID programmingExerciseId,
            SubmitExerciseRequest submitExerciseRequest, UUID idempotencyKey) {
        ExerciseSubmission stored = exerciseSubmissionService.submitExercise(programmingExerciseId,
                submitExerciseRequest);
        return ResponseEntity.ok().eTag(stored.getEtag()).body(stored);
    }

    @Override
    public ResponseEntity<ExerciseTestCase> updateExerciseTestCase(UUID programmingExerciseId,
            UUID exerciseTestCaseId, String ifMatch, ExerciseTestCaseUpdate exerciseTestCaseUpdate, Boolean validateOnly) {
        ExerciseTestCase updated =
                programmingExerciseService.updateExerciseTestCase(programmingExerciseId, exerciseTestCaseId, ifMatch,
                        exerciseTestCaseUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ProgrammingExercise> updateProgrammingExercise(UUID programmingExerciseId,
            String ifMatch, ProgrammingExerciseUpdate programmingExerciseUpdate, Boolean validateOnly) {
        ProgrammingExercise updated =
                programmingExerciseService.updateProgrammingExercise(programmingExerciseId, ifMatch,
                        programmingExerciseUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
