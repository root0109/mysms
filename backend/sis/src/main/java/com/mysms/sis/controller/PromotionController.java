package com.mysms.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.PromotionApi;
import com.mysms.sis.api.model.ApplyPromotionsRequest;
import com.mysms.sis.api.model.ApprovePromotionRequest;
import com.mysms.sis.api.model.Operation;
import com.mysms.sis.api.model.Promotion;
import com.mysms.sis.api.model.PromotionList;
import com.mysms.sis.api.model.PromotionUpdate;
import com.mysms.sis.api.model.ProposePromotionsRequest;
import com.mysms.sis.api.model.RejectPromotionRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.PromotionService;
import com.mysms.sis.business.PromotionWorkflowService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Promotion operations; authentication and the permission check already happened in the auth-client
 * filter. Proposing and applying promotions are served by the promotion workflow.
 */
@RestController
public class PromotionController implements PromotionApi {

    private final PromotionService promotionService;
    private final PromotionWorkflowService workflowService;

    public PromotionController(PromotionService promotionService, PromotionWorkflowService workflowService) {
        this.promotionService = promotionService;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<Operation> applyPromotions(ApplyPromotionsRequest applyPromotionsRequest,
            UUID idempotencyKey) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(workflowService.applyPromotions(applyPromotionsRequest));
    }

    @Override
    public ResponseEntity<Promotion> approvePromotion(UUID promotionId,
            ApprovePromotionRequest approvePromotionRequest, UUID idempotencyKey) {
        Promotion changed = promotionService.approvePromotion(promotionId, approvePromotionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Promotion> createPromotion(Promotion promotion, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Promotion created = promotionService.createPromotion(promotion, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/promotions/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deletePromotion(UUID promotionId, String ifMatch) {
        promotionService.deletePromotion(promotionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Promotion> getPromotion(UUID promotionId, String fields) {
        Promotion found = promotionService.getPromotion(promotionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Promotion>> listPromotions(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Promotion> list =
                PagingUtil.pagingInfo(promotionService.listPromotions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PromotionList> proposePromotions(
            ProposePromotionsRequest proposePromotionsRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(workflowService.proposePromotions(proposePromotionsRequest));
    }

    @Override
    public ResponseEntity<Promotion> rejectPromotion(UUID promotionId,
            RejectPromotionRequest rejectPromotionRequest, UUID idempotencyKey) {
        Promotion changed = promotionService.rejectPromotion(promotionId, rejectPromotionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Promotion> updatePromotion(UUID promotionId, String ifMatch,
            PromotionUpdate promotionUpdate, String updateMask, Boolean validateOnly) {
        Promotion updated = promotionService.updatePromotion(promotionId, ifMatch, promotionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
