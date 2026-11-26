package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.StorageQuotaApi;
import com.mysms.platform.api.model.StorageQuota;
import com.mysms.platform.api.model.StorageQuotaUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.StorageQuotaService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StorageQuota operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class StorageQuotaController implements StorageQuotaApi {

    private final StorageQuotaService storageQuotaService;

    public StorageQuotaController(StorageQuotaService storageQuotaService) {
        this.storageQuotaService = storageQuotaService;
    }

    @Override
    public ResponseEntity<StorageQuota> createStorageQuota(StorageQuota storageQuota, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StorageQuota created = storageQuotaService.createStorageQuota(storageQuota, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/storage-quotas/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStorageQuota(UUID storageQuotaId, String ifMatch) {
        storageQuotaService.deleteStorageQuota(storageQuotaId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StorageQuota> getMyStorageUsage() {
        StorageQuota mine = storageQuotaService.getMyStorageUsage();
        return ResponseEntity.ok().eTag(mine.getEtag()).body(mine);
    }

    @Override
    public ResponseEntity<StorageQuota> getStorageQuota(UUID storageQuotaId, String fields) {
        StorageQuota found = storageQuotaService.getStorageQuota(storageQuotaId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StorageQuota>> listStorageQuotas(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StorageQuota> list =
                PagingUtil.pagingInfo(storageQuotaService.listStorageQuotas(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StorageQuota> updateStorageQuota(UUID storageQuotaId, String ifMatch,
            StorageQuotaUpdate storageQuotaUpdate, Boolean validateOnly) {
        StorageQuota updated =
                storageQuotaService.updateStorageQuota(storageQuotaId, ifMatch, storageQuotaUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
