package com.mysms.finance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.FeeHeadApi;
import com.mysms.finance.api.model.FeeHead;
import com.mysms.finance.api.model.FeeHeadUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.business.FeeHeadService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the FeeHead operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class FeeHeadController implements FeeHeadApi {

    private final FeeHeadService feeHeadService;

    public FeeHeadController(FeeHeadService feeHeadService) {
        this.feeHeadService = feeHeadService;
    }

    @Override
    public ResponseEntity<FeeHead> createFeeHead(FeeHead feeHead, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeeHead created = feeHeadService.createFeeHead(feeHead, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/fee-heads/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFeeHead(UUID feeHeadId, String ifMatch) {
        feeHeadService.deleteFeeHead(feeHeadId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FeeHead> getFeeHead(UUID feeHeadId, String fields) {
        FeeHead found = feeHeadService.getFeeHead(feeHeadId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FeeHead>> listFeeHeads(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<FeeHead> list = PagingUtil.pagingInfo(feeHeadService.listFeeHeads(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<FeeHead> updateFeeHead(UUID feeHeadId, String ifMatch,
            FeeHeadUpdate feeHeadUpdate, String updateMask, Boolean validateOnly) {
        FeeHead updated = feeHeadService.updateFeeHead(feeHeadId, ifMatch, feeHeadUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
