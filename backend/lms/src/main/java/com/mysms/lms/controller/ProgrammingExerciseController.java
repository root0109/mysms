package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ProgrammingExerciseApi;
import com.mysms.lms.api.model.ExerciseSubmission;
import com.mysms.lms.api.model.ExerciseTestCase;
import com.mysms.lms.api.model.ExerciseTestCaseUpdate;
import com.mysms.lms.api.model.ProgrammingExercise;
import com.mysms.lms.api.model.ProgrammingExerciseUpdate;
import com.mysms.lms.api.model.SubmitExerciseRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ProgrammingExerciseService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ProgrammingExercise operations: implements the generated contract interface, so paths, parameters
 * and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ProgrammingExerciseController implements ProgrammingExerciseApi {

    private final ProgrammingExerciseService programmingExerciseService;

    public ProgrammingExerciseController(ProgrammingExerciseService programmingExerciseService) {
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
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ExerciseTestCase> updateExerciseTestCase(UUID programmingExerciseId,
            UUID exerciseTestCaseId, String ifMatch, ExerciseTestCaseUpdate exerciseTestCaseUpdate, String updateMask,
            Boolean validateOnly) {
        ExerciseTestCase updated =
                programmingExerciseService.updateExerciseTestCase(programmingExerciseId, exerciseTestCaseId, ifMatch,
                        exerciseTestCaseUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ProgrammingExercise> updateProgrammingExercise(UUID programmingExerciseId,
            String ifMatch, ProgrammingExerciseUpdate programmingExerciseUpdate, String updateMask,
            Boolean validateOnly) {
        ProgrammingExercise updated =
                programmingExerciseService.updateProgrammingExercise(programmingExerciseId, ifMatch,
                        programmingExerciseUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
