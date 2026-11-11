package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.ExamService;
import com.mysms.assessment.api.ExamApi;
import com.mysms.assessment.api.model.AllocateExamRoomsRequest;
import com.mysms.assessment.api.model.CancelExamRequest;
import com.mysms.assessment.api.model.CompleteExamRequest;
import com.mysms.assessment.api.model.Exam;
import com.mysms.assessment.api.model.ExamInvigilator;
import com.mysms.assessment.api.model.ExamInvigilatorUpdate;
import com.mysms.assessment.api.model.ExamResultList;
import com.mysms.assessment.api.model.ExamUpdate;
import com.mysms.assessment.api.model.MarkExamHeldRequest;
import com.mysms.assessment.api.model.MarkExamResultsUpdatedRequest;
import com.mysms.assessment.api.model.RecordExamMarksRequest;
import com.mysms.assessment.api.model.ResetExamRequest;
import com.mysms.assessment.api.model.ScheduleExamRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Exam operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/assessment.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * The controller stays thin: delegate business rules to the service and shape contract responses here.
 */
@RestController
public class ExamController implements ExamApi {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @Override
    public ResponseEntity<ExamResultList> allocateExamRooms(UUID examId,
            AllocateExamRoomsRequest allocateExamRoomsRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(examService.allocateExamRooms(examId, allocateExamRoomsRequest));
    }

    @Override
    public ResponseEntity<Exam> cancelExam(UUID examId, CancelExamRequest cancelExamRequest,
            UUID idempotencyKey) {
        Exam changed = examService.cancelExam(examId, cancelExamRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Exam> completeExam(UUID examId, CompleteExamRequest completeExamRequest,
            UUID idempotencyKey) {
        Exam changed = examService.completeExam(examId, completeExamRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Exam> createExam(Exam exam, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Exam created = examService.createExam(exam, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/exams/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<ExamInvigilator> createExamInvigilator(UUID examId,
            ExamInvigilator examInvigilator, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExamInvigilator created = examService.createExamInvigilator(examId, examInvigilator, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/assessment/exams/" + examId + "/invigilators/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteExam(UUID examId, String ifMatch) {
        examService.deleteExam(examId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteExamInvigilator(UUID examId, UUID examInvigilatorId,
            String ifMatch) {
        examService.deleteExamInvigilator(examId, examInvigilatorId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Exam> getExam(UUID examId, String fields) {
        Exam found = examService.getExam(examId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExamInvigilator> getExamInvigilator(UUID examId, UUID examInvigilatorId,
            String fields) {
        ExamInvigilator found = examService.getExamInvigilator(examId, examInvigilatorId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ExamInvigilator>> listExamInvigilators(UUID examId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExamInvigilator> list =
                PagingUtil.pagingInfo(examService.listExamInvigilators(examId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Exam>> listExams(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Exam> list = PagingUtil.pagingInfo(examService.listExams(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Exam> markExamHeld(UUID examId, MarkExamHeldRequest markExamHeldRequest,
            UUID idempotencyKey) {
        Exam changed = examService.markExamHeld(examId, markExamHeldRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Exam> markExamResultsUpdated(UUID examId,
            MarkExamResultsUpdatedRequest markExamResultsUpdatedRequest, UUID idempotencyKey) {
        Exam changed = examService.markExamResultsUpdated(examId, markExamResultsUpdatedRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ExamResultList> populateExamResults(UUID examId, Object body,
            UUID idempotencyKey) {
        return ResponseEntity.ok(examService.populateExamResults(examId));
    }

    @Override
    public ResponseEntity<ExamResultList> recordExamMarks(UUID examId,
            RecordExamMarksRequest recordExamMarksRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(examService.recordExamMarks(examId, recordExamMarksRequest));
    }

    @Override
    public ResponseEntity<Exam> resetExam(UUID examId, ResetExamRequest resetExamRequest,
            UUID idempotencyKey) {
        Exam changed = examService.resetExam(examId, resetExamRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Exam> scheduleExam(UUID examId, ScheduleExamRequest scheduleExamRequest,
            UUID idempotencyKey) {
        Exam changed = examService.scheduleExam(examId, scheduleExamRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Exam> updateExam(UUID examId, String ifMatch, ExamUpdate examUpdate,
            String updateMask, Boolean validateOnly) {
        Exam updated = examService.updateExam(examId, ifMatch, examUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExamInvigilator> updateExamInvigilator(UUID examId, UUID examInvigilatorId,
            String ifMatch, ExamInvigilatorUpdate examInvigilatorUpdate, String updateMask, Boolean validateOnly) {
        ExamInvigilator updated =
                examService.updateExamInvigilator(examId, examInvigilatorId, ifMatch, examInvigilatorUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
