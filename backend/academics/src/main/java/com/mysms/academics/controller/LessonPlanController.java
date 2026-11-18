package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.LessonPlanService;
import com.mysms.academics.api.LessonPlanApi;
import com.mysms.academics.api.model.ApproveLessonPlanRequest;
import com.mysms.academics.api.model.ArchiveLessonPlanRequest;
import com.mysms.academics.api.model.LessonPlan;
import com.mysms.academics.api.model.LessonPlanApproval;
import com.mysms.academics.api.model.LessonPlanApprovalUpdate;
import com.mysms.academics.api.model.LessonPlanUpdate;
import com.mysms.academics.api.model.PublishLessonPlanRequest;
import com.mysms.academics.api.model.RejectLessonPlanRequest;
import com.mysms.academics.api.model.SubmitLessonPlanRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LessonPlan operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class LessonPlanController implements LessonPlanApi {

    private final LessonPlanService lessonPlanService;

    public LessonPlanController(LessonPlanService lessonPlanService) {
        this.lessonPlanService = lessonPlanService;
    }

    @Override
    public ResponseEntity<LessonPlan> approveLessonPlan(UUID lessonPlanId,
            ApproveLessonPlanRequest approveLessonPlanRequest, UUID idempotencyKey) {
        LessonPlan changed = lessonPlanService.approveLessonPlan(lessonPlanId, approveLessonPlanRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LessonPlan> archiveLessonPlan(UUID lessonPlanId,
            ArchiveLessonPlanRequest archiveLessonPlanRequest, UUID idempotencyKey) {
        LessonPlan changed = lessonPlanService.archiveLessonPlan(lessonPlanId, archiveLessonPlanRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LessonPlan> createLessonPlan(LessonPlan lessonPlan, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LessonPlan created = lessonPlanService.createLessonPlan(lessonPlan, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/lesson-plans/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<LessonPlanApproval> createLessonPlanApproval(UUID lessonPlanId,
            LessonPlanApproval lessonPlanApproval, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LessonPlanApproval created =
                lessonPlanService.createLessonPlanApproval(lessonPlanId, lessonPlanApproval, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/academics/lesson-plans/" + lessonPlanId + "/approvals/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLessonPlan(UUID lessonPlanId, String ifMatch) {
        lessonPlanService.deleteLessonPlan(lessonPlanId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteLessonPlanApproval(UUID lessonPlanId, UUID lessonPlanApprovalId,
            String ifMatch) {
        lessonPlanService.deleteLessonPlanApproval(lessonPlanId, lessonPlanApprovalId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LessonPlan> getLessonPlan(UUID lessonPlanId, String fields) {
        LessonPlan found = lessonPlanService.getLessonPlan(lessonPlanId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LessonPlanApproval> getLessonPlanApproval(UUID lessonPlanId,
            UUID lessonPlanApprovalId, String fields) {
        LessonPlanApproval found = lessonPlanService.getLessonPlanApproval(lessonPlanId, lessonPlanApprovalId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LessonPlanApproval>> listLessonPlanApprovals(UUID lessonPlanId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LessonPlanApproval> list =
                PagingUtil.pagingInfo(lessonPlanService.listLessonPlanApprovals(lessonPlanId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<LessonPlan>> listLessonPlans(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LessonPlan> list =
                PagingUtil.pagingInfo(lessonPlanService.listLessonPlans(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LessonPlan> publishLessonPlan(UUID lessonPlanId,
            PublishLessonPlanRequest publishLessonPlanRequest, UUID idempotencyKey) {
        LessonPlan changed = lessonPlanService.publishLessonPlan(lessonPlanId, publishLessonPlanRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LessonPlan> rejectLessonPlan(UUID lessonPlanId,
            RejectLessonPlanRequest rejectLessonPlanRequest, UUID idempotencyKey) {
        LessonPlan changed = lessonPlanService.rejectLessonPlan(lessonPlanId, rejectLessonPlanRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LessonPlan> submitLessonPlan(UUID lessonPlanId,
            SubmitLessonPlanRequest submitLessonPlanRequest, UUID idempotencyKey) {
        LessonPlan changed = lessonPlanService.submitLessonPlan(lessonPlanId, submitLessonPlanRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<LessonPlan> updateLessonPlan(UUID lessonPlanId, String ifMatch,
            LessonPlanUpdate lessonPlanUpdate, String updateMask, Boolean validateOnly) {
        LessonPlan updated = lessonPlanService.updateLessonPlan(lessonPlanId, ifMatch, lessonPlanUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<LessonPlanApproval> updateLessonPlanApproval(UUID lessonPlanId,
            UUID lessonPlanApprovalId, String ifMatch, LessonPlanApprovalUpdate lessonPlanApprovalUpdate,
            String updateMask, Boolean validateOnly) {
        LessonPlanApproval updated =
                lessonPlanService.updateLessonPlanApproval(lessonPlanId, lessonPlanApprovalId, ifMatch,
                        lessonPlanApprovalUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
