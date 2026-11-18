package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.EvaluatorApi;
import com.mysms.lms.api.model.ConnectEvaluatorCalendarRequest;
import com.mysms.lms.api.model.Evaluator;
import com.mysms.lms.api.model.EvaluatorAvailability;
import com.mysms.lms.api.model.EvaluatorAvailabilityUpdate;
import com.mysms.lms.api.model.EvaluatorUpdate;
import com.mysms.lms.api.model.ExternalLink;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.EvaluatorService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Evaluator operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class EvaluatorController implements EvaluatorApi {

    private final EvaluatorService evaluatorService;

    public EvaluatorController(EvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
    }

    @Override
    public ResponseEntity<ExternalLink> connectEvaluatorCalendar(UUID evaluatorId,
            ConnectEvaluatorCalendarRequest connectEvaluatorCalendarRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(evaluatorService.connectEvaluatorCalendar(evaluatorId,
                connectEvaluatorCalendarRequest));
    }

    @Override
    public ResponseEntity<Evaluator> createEvaluator(Evaluator evaluator, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Evaluator created = evaluatorService.createEvaluator(evaluator, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/evaluators/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<EvaluatorAvailability> createEvaluatorAvailability(UUID evaluatorId,
            EvaluatorAvailability evaluatorAvailability, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EvaluatorAvailability created =
                evaluatorService.createEvaluatorAvailability(evaluatorId, evaluatorAvailability, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/evaluators/" + evaluatorId + "/availability/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteEvaluator(UUID evaluatorId, String ifMatch) {
        evaluatorService.deleteEvaluator(evaluatorId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEvaluatorAvailability(UUID evaluatorId,
            UUID evaluatorAvailabilityId, String ifMatch) {
        evaluatorService.deleteEvaluatorAvailability(evaluatorId, evaluatorAvailabilityId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Evaluator> disconnectEvaluatorCalendar(UUID evaluatorId, Object body,
            UUID idempotencyKey) {
        Evaluator unlinked = evaluatorService.disconnectEvaluatorCalendar(evaluatorId);
        return ResponseEntity.ok().eTag(unlinked.getEtag()).body(unlinked);
    }

    @Override
    public ResponseEntity<Evaluator> getEvaluator(UUID evaluatorId, String fields) {
        Evaluator found = evaluatorService.getEvaluator(evaluatorId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EvaluatorAvailability> getEvaluatorAvailability(UUID evaluatorId,
            UUID evaluatorAvailabilityId, String fields) {
        EvaluatorAvailability found =
                evaluatorService.getEvaluatorAvailability(evaluatorId, evaluatorAvailabilityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<EvaluatorAvailability>> listEvaluatorAvailabilities(UUID evaluatorId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EvaluatorAvailability> list =
                PagingUtil.pagingInfo(evaluatorService.listEvaluatorAvailabilitys(evaluatorId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Evaluator>> listEvaluators(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Evaluator> list =
                PagingUtil.pagingInfo(evaluatorService.listEvaluators(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Evaluator> updateEvaluator(UUID evaluatorId, String ifMatch,
            EvaluatorUpdate evaluatorUpdate, String updateMask, Boolean validateOnly) {
        Evaluator updated = evaluatorService.updateEvaluator(evaluatorId, ifMatch, evaluatorUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EvaluatorAvailability> updateEvaluatorAvailability(UUID evaluatorId,
            UUID evaluatorAvailabilityId, String ifMatch, EvaluatorAvailabilityUpdate evaluatorAvailabilityUpdate,
            String updateMask, Boolean validateOnly) {
        EvaluatorAvailability updated =
                evaluatorService.updateEvaluatorAvailability(evaluatorId, evaluatorAvailabilityId, ifMatch,
                        evaluatorAvailabilityUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
