package com.mysms.finance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.FeeStructureApi;
import com.mysms.finance.api.model.FeeStructure;
import com.mysms.finance.api.model.FeeStructureUpdate;
import com.mysms.finance.api.model.Operation;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.business.FeeStructureService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the FeeStructure operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class FeeStructureController implements FeeStructureApi {

    private final FeeStructureService feeStructureService;

    public FeeStructureController(FeeStructureService feeStructureService) {
        this.feeStructureService = feeStructureService;
    }

    @Override
    public ResponseEntity<FeeStructure> archiveFeeStructure(UUID feeStructureId, String ifMatch) {
        FeeStructure changed = feeStructureService.archiveFeeStructure(feeStructureId, ifMatch);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<FeeStructure> createFeeStructure(FeeStructure feeStructure, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeeStructure created = feeStructureService.createFeeStructure(feeStructure, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/fee-structures/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFeeStructure(UUID feeStructureId, String ifMatch) {
        feeStructureService.deleteFeeStructure(feeStructureId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FeeStructure> getFeeStructure(UUID feeStructureId, String fields) {
        FeeStructure found = feeStructureService.getFeeStructure(feeStructureId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FeeStructure>> listFeeStructures(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<FeeStructure> list =
                PagingUtil.pagingInfo(feeStructureService.listFeeStructures(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Operation> publishFeeStructure(UUID feeStructureId, UUID idempotencyKey) {
        return ResponseEntity.accepted().body(feeStructureService.publishFeeStructure(feeStructureId));
    }

    @Override
    public ResponseEntity<FeeStructure> updateFeeStructure(UUID feeStructureId, String ifMatch,
            FeeStructureUpdate feeStructureUpdate, String updateMask, Boolean validateOnly) {
        FeeStructure updated =
                feeStructureService.updateFeeStructure(feeStructureId, ifMatch, feeStructureUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
