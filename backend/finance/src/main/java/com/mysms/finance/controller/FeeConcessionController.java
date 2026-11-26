package com.mysms.finance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.FeeConcessionApi;
import com.mysms.finance.api.model.FeeConcession;
import com.mysms.finance.api.model.FeeConcessionUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.business.FeeConcessionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the FeeConcession operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class FeeConcessionController implements FeeConcessionApi {

    private final FeeConcessionService feeConcessionService;

    public FeeConcessionController(FeeConcessionService feeConcessionService) {
        this.feeConcessionService = feeConcessionService;
    }

    @Override
    public ResponseEntity<FeeConcession> createFeeConcession(FeeConcession feeConcession,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeeConcession created = feeConcessionService.createFeeConcession(feeConcession, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/fee-concessions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFeeConcession(UUID feeConcessionId, String ifMatch) {
        feeConcessionService.deleteFeeConcession(feeConcessionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FeeConcession> getFeeConcession(UUID feeConcessionId, String fields) {
        FeeConcession found = feeConcessionService.getFeeConcession(feeConcessionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FeeConcession>> listFeeConcessions(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<FeeConcession> list =
                PagingUtil.pagingInfo(feeConcessionService.listFeeConcessions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<FeeConcession> updateFeeConcession(UUID feeConcessionId, String ifMatch,
            FeeConcessionUpdate feeConcessionUpdate, Boolean validateOnly) {
        FeeConcession updated =
                feeConcessionService.updateFeeConcession(feeConcessionId, ifMatch, feeConcessionUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
