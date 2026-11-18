package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ExerciseSubmissionApi;
import com.mysms.lms.api.model.ExerciseSubmission;
import com.mysms.lms.api.model.ExerciseTestResult;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ExerciseSubmissionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExerciseSubmission operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class ExerciseSubmissionController implements ExerciseSubmissionApi {

    private final ExerciseSubmissionService exerciseSubmissionService;

    public ExerciseSubmissionController(ExerciseSubmissionService exerciseSubmissionService) {
        this.exerciseSubmissionService = exerciseSubmissionService;
    }

    @Override
    public ResponseEntity<ExerciseSubmission> getExerciseSubmission(UUID exerciseSubmissionId,
            String fields) {
        ExerciseSubmission found = exerciseSubmissionService.getExerciseSubmission(exerciseSubmissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExerciseTestResult> getExerciseTestResult(UUID exerciseSubmissionId,
            UUID exerciseTestResultId, String fields) {
        ExerciseTestResult found =
                exerciseSubmissionService.getExerciseTestResult(exerciseSubmissionId, exerciseTestResultId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ExerciseSubmission>> listExerciseSubmissions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExerciseSubmission> list =
                PagingUtil.pagingInfo(exerciseSubmissionService.listExerciseSubmissions(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ExerciseTestResult>> listExerciseTestResults(UUID exerciseSubmissionId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExerciseTestResult> list =
                PagingUtil.pagingInfo(exerciseSubmissionService.listExerciseTestResults(exerciseSubmissionId, page,
                        size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
