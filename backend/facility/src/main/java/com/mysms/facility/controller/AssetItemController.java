package com.mysms.facility.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.facility.api.AssetItemApi;
import com.mysms.facility.api.model.AssetItem;
import com.mysms.facility.api.model.AssetItemUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.facility.business.AssetItemService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AssetItem operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/facility.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class AssetItemController implements AssetItemApi {

    private final AssetItemService assetItemService;

    public AssetItemController(AssetItemService assetItemService) {
        this.assetItemService = assetItemService;
    }

    @Override
    public ResponseEntity<AssetItem> createAssetItem(AssetItem assetItem, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AssetItem created = assetItemService.createAssetItem(assetItem, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/facility/asset-items/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAssetItem(UUID assetItemId, String ifMatch) {
        assetItemService.deleteAssetItem(assetItemId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AssetItem> getAssetItem(UUID assetItemId, String fields) {
        AssetItem found = assetItemService.getAssetItem(assetItemId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AssetItem>> listAssetItems(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AssetItem> list =
                PagingUtil.pagingInfo(assetItemService.listAssetItems(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AssetItem> updateAssetItem(UUID assetItemId, String ifMatch,
            AssetItemUpdate assetItemUpdate, String updateMask, Boolean validateOnly) {
        AssetItem updated = assetItemService.updateAssetItem(assetItemId, ifMatch, assetItemUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
