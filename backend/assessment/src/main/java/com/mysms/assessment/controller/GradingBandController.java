package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.GradingBandService;
import com.mysms.assessment.api.GradingBandApi;
import com.mysms.assessment.api.model.GradingBand;
import com.mysms.assessment.api.model.GradingBandUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the GradingBand operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/assessment.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class GradingBandController implements GradingBandApi {

    private final GradingBandService gradingBandService;

    public GradingBandController(GradingBandService gradingBandService) {
        this.gradingBandService = gradingBandService;
    }

    @Override
    public ResponseEntity<GradingBand> createGradingBand(GradingBand gradingBand, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        GradingBand created = gradingBandService.createGradingBand(gradingBand, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/grading-bands/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteGradingBand(UUID gradingBandId, String ifMatch) {
        gradingBandService.deleteGradingBand(gradingBandId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GradingBand> getGradingBand(UUID gradingBandId, String fields) {
        GradingBand found = gradingBandService.getGradingBand(gradingBandId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<GradingBand>> listGradingBands(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<GradingBand> list =
                PagingUtil.pagingInfo(gradingBandService.listGradingBands(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<GradingBand> updateGradingBand(UUID gradingBandId, String ifMatch,
            GradingBandUpdate gradingBandUpdate, String updateMask, Boolean validateOnly) {
        GradingBand updated =
                gradingBandService.updateGradingBand(gradingBandId, ifMatch, gradingBandUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
