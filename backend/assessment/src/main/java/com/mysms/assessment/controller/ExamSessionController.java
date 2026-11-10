package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.ExamSessionService;
import com.mysms.assessment.api.ExamSessionApi;
import com.mysms.assessment.api.model.CancelExamSessionRequest;
import com.mysms.assessment.api.model.CompleteExamSessionRequest;
import com.mysms.assessment.api.model.ExamSession;
import com.mysms.assessment.api.model.ExamSessionUpdate;
import com.mysms.assessment.api.model.MarkExamSessionHeldRequest;
import com.mysms.assessment.api.model.ResetExamSessionRequest;
import com.mysms.assessment.api.model.ScheduleExamSessionRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExamSession operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/assessment.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ExamSessionController implements ExamSessionApi {

    private final ExamSessionService examSessionService;

    public ExamSessionController(ExamSessionService examSessionService) {
        this.examSessionService = examSessionService;
    }

    @Override
    public ResponseEntity<ExamSession> cancelExamSession(UUID examSessionId,
            CancelExamSessionRequest cancelExamSessionRequest, UUID idempotencyKey) {
        ExamSession changed = examSessionService.cancelExamSession(examSessionId, cancelExamSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ExamSession> completeExamSession(UUID examSessionId,
            CompleteExamSessionRequest completeExamSessionRequest, UUID idempotencyKey) {
        ExamSession changed = examSessionService.completeExamSession(examSessionId, completeExamSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ExamSession> createExamSession(ExamSession examSession, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExamSession created = examSessionService.createExamSession(examSession, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/exam-sessions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteExamSession(UUID examSessionId, String ifMatch) {
        examSessionService.deleteExamSession(examSessionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ExamSession> getExamSession(UUID examSessionId, String fields) {
        ExamSession found = examSessionService.getExamSession(examSessionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ExamSession>> listExamSessions(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ExamSession> list =
                PagingUtil.pagingInfo(examSessionService.listExamSessions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ExamSession> markExamSessionHeld(UUID examSessionId,
            MarkExamSessionHeldRequest markExamSessionHeldRequest, UUID idempotencyKey) {
        ExamSession changed = examSessionService.markExamSessionHeld(examSessionId, markExamSessionHeldRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ExamSession> resetExamSession(UUID examSessionId,
            ResetExamSessionRequest resetExamSessionRequest, UUID idempotencyKey) {
        ExamSession changed = examSessionService.resetExamSession(examSessionId, resetExamSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ExamSession> scheduleExamSession(UUID examSessionId,
            ScheduleExamSessionRequest scheduleExamSessionRequest, UUID idempotencyKey) {
        ExamSession changed = examSessionService.scheduleExamSession(examSessionId, scheduleExamSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<ExamSession> updateExamSession(UUID examSessionId, String ifMatch,
            ExamSessionUpdate examSessionUpdate, String updateMask, Boolean validateOnly) {
        ExamSession updated =
                examSessionService.updateExamSession(examSessionId, ifMatch, examSessionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
