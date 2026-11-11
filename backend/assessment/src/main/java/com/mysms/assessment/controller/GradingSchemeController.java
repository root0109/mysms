package com.mysms.assessment.controller;

import com.mysms.assessment.api.GradingSchemeApi;
import com.mysms.assessment.api.model.GradingScheme;
import com.mysms.assessment.api.model.GradingSchemeUpdate;
import com.mysms.assessment.business.GradingSchemeService;
import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge for assessment.grading_scheme; validation, scope, transactions and audit stay in its service. */
@RestController
public class GradingSchemeController implements GradingSchemeApi {

    private final GradingSchemeService service;

    public GradingSchemeController(GradingSchemeService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<GradingScheme> createGradingScheme(GradingScheme gradingScheme, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        GradingScheme created = service.createGradingScheme(gradingScheme, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/grading-schemes/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteGradingScheme(UUID gradingSchemeId, String ifMatch) {
        service.deleteGradingScheme(gradingSchemeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GradingScheme> getGradingScheme(UUID gradingSchemeId, String fields) {
        GradingScheme found = service.getGradingScheme(gradingSchemeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<GradingScheme>> listGradingSchemes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<GradingScheme> list = PagingUtil.pagingInfo(
                service.listGradingSchemes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<GradingScheme> updateGradingScheme(UUID gradingSchemeId, String ifMatch,
            GradingSchemeUpdate gradingSchemeUpdate, String updateMask, Boolean validateOnly) {
        GradingScheme updated = service.updateGradingScheme(gradingSchemeId, ifMatch, gradingSchemeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
