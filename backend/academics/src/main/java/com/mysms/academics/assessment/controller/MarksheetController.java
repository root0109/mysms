package com.mysms.academics.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.business.MarksheetService;
import com.mysms.academics.assessment.api.MarksheetApi;
import com.mysms.academics.assessment.api.model.CancelMarksheetRequest;
import com.mysms.academics.assessment.api.model.Marksheet;
import com.mysms.academics.assessment.api.model.MarksheetUpdate;
import com.mysms.academics.assessment.api.model.PublishMarksheetRequest;
import com.mysms.academics.assessment.api.model.ResetMarksheetRequest;
import com.mysms.academics.assessment.api.model.ValidateMarksheetRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Marksheet operations; authentication and the permission check already ran in the auth-client filter.
 */
@RestController
public class MarksheetController implements MarksheetApi {

    private final MarksheetService marksheetService;

    public MarksheetController(MarksheetService marksheetService) {
        this.marksheetService = marksheetService;
    }

    @Override
    public ResponseEntity<Marksheet> cancelMarksheet(UUID marksheetId,
            CancelMarksheetRequest cancelMarksheetRequest, UUID idempotencyKey) {
        Marksheet changed = marksheetService.cancelMarksheet(marksheetId, cancelMarksheetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Marksheet> createMarksheet(Marksheet marksheet, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Marksheet created = marksheetService.createMarksheet(marksheet, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/marksheets/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteMarksheet(UUID marksheetId, String ifMatch) {
        marksheetService.deleteMarksheet(marksheetId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Marksheet> getMarksheet(UUID marksheetId, String fields) {
        Marksheet found = marksheetService.getMarksheet(marksheetId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Marksheet>> listMarksheets(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Marksheet> list =
                PagingUtil.pagingInfo(marksheetService.listMarksheets(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Marksheet> publishMarksheet(UUID marksheetId, String ifMatch,
            UUID idempotencyKey, PublishMarksheetRequest publishMarksheetRequest) {
        Marksheet changed = marksheetService.publishMarksheet(marksheetId, ifMatch, publishMarksheetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Marksheet> resetMarksheet(UUID marksheetId,
            ResetMarksheetRequest resetMarksheetRequest, UUID idempotencyKey) {
        Marksheet changed = marksheetService.resetMarksheet(marksheetId, resetMarksheetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Marksheet> updateMarksheet(UUID marksheetId, String ifMatch,
            MarksheetUpdate marksheetUpdate, Boolean validateOnly) {
        Marksheet updated = marksheetService.updateMarksheet(marksheetId, ifMatch, marksheetUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Marksheet> validateMarksheet(UUID marksheetId,
            ValidateMarksheetRequest validateMarksheetRequest, UUID idempotencyKey) {
        Marksheet changed = marksheetService.validateMarksheet(marksheetId, validateMarksheetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }
}
