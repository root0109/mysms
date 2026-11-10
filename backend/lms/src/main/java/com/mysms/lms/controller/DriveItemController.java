package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.DriveItemApi;
import com.mysms.lms.api.model.DriveItem;
import com.mysms.lms.api.model.DriveItemShare;
import com.mysms.lms.api.model.DriveItemShareList;
import com.mysms.lms.api.model.DriveItemShareUpdate;
import com.mysms.lms.api.model.DriveItemUpdate;
import com.mysms.lms.api.model.ShareDriveItemRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.DriveItemService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the DriveItem operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class DriveItemController implements DriveItemApi {

    private final DriveItemService driveItemService;

    public DriveItemController(DriveItemService driveItemService) {
        this.driveItemService = driveItemService;
    }

    @Override
    public ResponseEntity<DriveItem> createDriveItem(DriveItem driveItem, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        DriveItem created = driveItemService.createDriveItem(driveItem, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/drive-items/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<DriveItemShare> createDriveItemShare(UUID driveItemId,
            DriveItemShare driveItemShare, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        DriveItemShare created = driveItemService.createDriveItemShare(driveItemId, driveItemShare, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteDriveItem(UUID driveItemId, String ifMatch) {
        driveItemService.deleteDriveItem(driveItemId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteDriveItemShare(UUID driveItemId, UUID driveItemShareId,
            String ifMatch) {
        driveItemService.deleteDriveItemShare(driveItemId, driveItemShareId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DriveItem> getDriveItem(UUID driveItemId, String fields) {
        DriveItem found = driveItemService.getDriveItem(driveItemId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<DriveItemShare>> listDriveItemShares(UUID driveItemId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<DriveItemShare> list =
                PagingUtil.pagingInfo(driveItemService.listDriveItemShares(driveItemId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<DriveItem>> listDriveItems(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<DriveItem> list =
                PagingUtil.pagingInfo(driveItemService.listDriveItems(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<DriveItemShareList> shareDriveItem(UUID driveItemId,
            ShareDriveItemRequest shareDriveItemRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<DriveItem> updateDriveItem(UUID driveItemId, String ifMatch,
            DriveItemUpdate driveItemUpdate, String updateMask, Boolean validateOnly) {
        DriveItem updated = driveItemService.updateDriveItem(driveItemId, ifMatch, driveItemUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<DriveItemShare> updateDriveItemShare(UUID driveItemId, UUID driveItemShareId,
            String ifMatch, DriveItemShareUpdate driveItemShareUpdate, String updateMask, Boolean validateOnly) {
        DriveItemShare updated =
                driveItemService.updateDriveItemShare(driveItemId, driveItemShareId, ifMatch, driveItemShareUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
