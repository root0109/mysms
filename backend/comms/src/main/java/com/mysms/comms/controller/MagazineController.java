package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.MagazineService;
import com.mysms.comms.api.MagazineApi;
import com.mysms.comms.api.model.Magazine;
import com.mysms.comms.api.model.MagazineUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Magazine operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class MagazineController implements MagazineApi {

    private final MagazineService magazineService;

    public MagazineController(MagazineService magazineService) {
        this.magazineService = magazineService;
    }

    @Override
    public ResponseEntity<Magazine> createMagazine(Magazine magazine, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Magazine created = magazineService.createMagazine(magazine, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/magazines/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteMagazine(UUID magazineId, String ifMatch) {
        magazineService.deleteMagazine(magazineId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Magazine> getMagazine(UUID magazineId, String fields) {
        Magazine found = magazineService.getMagazine(magazineId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Magazine>> listMagazines(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Magazine> list =
                PagingUtil.pagingInfo(magazineService.listMagazines(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Magazine> updateMagazine(UUID magazineId, String ifMatch,
            MagazineUpdate magazineUpdate, String updateMask, Boolean validateOnly) {
        Magazine updated = magazineService.updateMagazine(magazineId, ifMatch, magazineUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
