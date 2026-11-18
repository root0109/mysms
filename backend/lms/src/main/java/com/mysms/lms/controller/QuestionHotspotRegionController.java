package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.QuestionHotspotRegionApi;
import com.mysms.lms.api.model.QuestionHotspotRegion;
import com.mysms.lms.api.model.QuestionHotspotRegionUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.QuestionHotspotRegionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the QuestionHotspotRegion operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class QuestionHotspotRegionController implements QuestionHotspotRegionApi {

    private final QuestionHotspotRegionService questionHotspotRegionService;

    public QuestionHotspotRegionController(QuestionHotspotRegionService questionHotspotRegionService) {
        this.questionHotspotRegionService = questionHotspotRegionService;
    }

    @Override
    public ResponseEntity<QuestionHotspotRegion> createQuestionHotspotRegion(
            QuestionHotspotRegion questionHotspotRegion, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        QuestionHotspotRegion created =
                questionHotspotRegionService.createQuestionHotspotRegion(questionHotspotRegion, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/question-hotspot-regions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteQuestionHotspotRegion(UUID questionHotspotRegionId,
            String ifMatch) {
        questionHotspotRegionService.deleteQuestionHotspotRegion(questionHotspotRegionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<QuestionHotspotRegion> getQuestionHotspotRegion(UUID questionHotspotRegionId,
            String fields) {
        QuestionHotspotRegion found = questionHotspotRegionService.getQuestionHotspotRegion(questionHotspotRegionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<QuestionHotspotRegion>> listQuestionHotspotRegions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<QuestionHotspotRegion> list =
                PagingUtil.pagingInfo(questionHotspotRegionService.listQuestionHotspotRegions(page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<QuestionHotspotRegion> updateQuestionHotspotRegion(UUID questionHotspotRegionId,
            String ifMatch, QuestionHotspotRegionUpdate questionHotspotRegionUpdate, String updateMask,
            Boolean validateOnly) {
        QuestionHotspotRegion updated =
                questionHotspotRegionService.updateQuestionHotspotRegion(questionHotspotRegionId, ifMatch,
                        questionHotspotRegionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
