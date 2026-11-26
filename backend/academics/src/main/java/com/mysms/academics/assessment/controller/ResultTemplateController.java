package com.mysms.academics.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.business.ResultTemplateService;
import com.mysms.academics.assessment.business.ResultsGenerationService;
import com.mysms.academics.assessment.api.ResultTemplateApi;
import com.mysms.academics.assessment.api.model.Operation;
import com.mysms.academics.assessment.api.model.ResultTemplate;
import com.mysms.academics.assessment.api.model.ResultTemplateBand;
import com.mysms.academics.assessment.api.model.ResultTemplateBandUpdate;
import com.mysms.academics.assessment.api.model.ResultTemplateUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ResultTemplate operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class ResultTemplateController implements ResultTemplateApi {

    private final ResultTemplateService resultTemplateService;
    private final ResultsGenerationService resultsGenerationService;

    public ResultTemplateController(ResultTemplateService resultTemplateService,
            ResultsGenerationService resultsGenerationService) {
        this.resultTemplateService = resultTemplateService;
        this.resultsGenerationService = resultsGenerationService;
    }

    @Override
    public ResponseEntity<ResultTemplate> createResultTemplate(ResultTemplate resultTemplate,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ResultTemplate created = resultTemplateService.createResultTemplate(resultTemplate, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/result-templates/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ResultTemplateBand> createResultTemplateBand(UUID resultTemplateId,
            ResultTemplateBand resultTemplateBand, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ResultTemplateBand created =
                resultTemplateService.createResultTemplateBand(resultTemplateId, resultTemplateBand, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/assessment/result-templates/" + resultTemplateId + "/result-template-bands/"
                        + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteResultTemplate(UUID resultTemplateId, String ifMatch) {
        resultTemplateService.deleteResultTemplate(resultTemplateId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteResultTemplateBand(UUID resultTemplateId, UUID resultTemplateBandId,
            String ifMatch) {
        resultTemplateService.deleteResultTemplateBand(resultTemplateId, resultTemplateBandId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Operation> generateResults(UUID resultTemplateId, Object body,
            UUID idempotencyKey) {
        return ResponseEntity.accepted().body(resultsGenerationService.generateResults(resultTemplateId));
    }

    @Override
    public ResponseEntity<ResultTemplate> getResultTemplate(UUID resultTemplateId, String fields) {
        ResultTemplate found = resultTemplateService.getResultTemplate(resultTemplateId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ResultTemplateBand> getResultTemplateBand(UUID resultTemplateId,
            UUID resultTemplateBandId, String fields) {
        ResultTemplateBand found =
                resultTemplateService.getResultTemplateBand(resultTemplateId, resultTemplateBandId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ResultTemplateBand>> listResultTemplateBands(UUID resultTemplateId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ResultTemplateBand> list =
                PagingUtil.pagingInfo(resultTemplateService.listResultTemplateBands(resultTemplateId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ResultTemplate>> listResultTemplates(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ResultTemplate> list =
                PagingUtil.pagingInfo(resultTemplateService.listResultTemplates(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ResultTemplate> updateResultTemplate(UUID resultTemplateId, String ifMatch,
            ResultTemplateUpdate resultTemplateUpdate, Boolean validateOnly) {
        ResultTemplate updated =
                resultTemplateService.updateResultTemplate(resultTemplateId, ifMatch, resultTemplateUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ResultTemplateBand> updateResultTemplateBand(UUID resultTemplateId,
            UUID resultTemplateBandId, String ifMatch, ResultTemplateBandUpdate resultTemplateBandUpdate,
            Boolean validateOnly) {
        ResultTemplateBand updated =
                resultTemplateService.updateResultTemplateBand(resultTemplateId, resultTemplateBandId, ifMatch,
                        resultTemplateBandUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
