package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.BatchService;
import com.mysms.academics.api.BatchApi;
import com.mysms.academics.api.model.Batch;
import com.mysms.academics.api.model.BatchUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Batch operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class BatchController implements BatchApi {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public ResponseEntity<Batch> createBatch(Batch batch, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Batch created = batchService.createBatch(batch, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/batches/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteBatch(UUID batchId, String ifMatch) {
        batchService.deleteBatch(batchId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Batch> getBatch(UUID batchId, String fields) {
        Batch found = batchService.getBatch(batchId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Batch>> listBatches(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Batch> list = PagingUtil.pagingInfo(batchService.listBatchs(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Batch> updateBatch(UUID batchId, String ifMatch, BatchUpdate batchUpdate,
            String updateMask, Boolean validateOnly) {
        Batch updated = batchService.updateBatch(batchId, ifMatch, batchUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
