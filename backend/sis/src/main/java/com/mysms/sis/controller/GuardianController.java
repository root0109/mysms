package com.mysms.sis.controller;

import com.mysms.sis.api.model.ChildCard;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.GuardianApi;
import com.mysms.sis.api.model.CreatePortalAccountRequest;
import com.mysms.sis.api.model.Guardian;
import com.mysms.sis.api.model.GuardianUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.GuardianService;
import com.mysms.sis.business.PortalAccountService;
import com.mysms.sis.business.StudentSummaryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Guardian operations; authentication and the permission check already happened in the auth-client
 * filter. The portal account and the caller's own children are served by their own services.
 */
@RestController
public class GuardianController implements GuardianApi {

    private final GuardianService guardianService;
    private final PortalAccountService portalAccountService;
    private final StudentSummaryService summaryService;

    public GuardianController(GuardianService guardianService, PortalAccountService portalAccountService,
            StudentSummaryService summaryService) {
        this.guardianService = guardianService;
        this.portalAccountService = portalAccountService;
        this.summaryService = summaryService;
    }

    @Override
    public ResponseEntity<Guardian> createGuardian(Guardian guardian, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Guardian created = guardianService.createGuardian(guardian, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/guardians/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Guardian> createPortalAccount(UUID guardianId,
            CreatePortalAccountRequest createPortalAccountRequest, UUID idempotencyKey) {
        Guardian linked = portalAccountService.createGuardianPortalAccount(guardianId,
                createPortalAccountRequest);
        return ResponseEntity.ok().eTag(linked.getEtag()).body(linked);
    }

    @Override
    public ResponseEntity<Void> deleteGuardian(UUID guardianId, String ifMatch) {
        guardianService.deleteGuardian(guardianId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Guardian> getGuardian(UUID guardianId, String fields) {
        Guardian found = guardianService.getGuardian(guardianId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Guardian>> listGuardians(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Guardian> list =
                PagingUtil.pagingInfo(guardianService.listGuardians(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ChildCard>> listMyChildren(Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(summaryService.listMyChildren(page, size, cursor)));
    }

    @Override
    public ResponseEntity<Guardian> updateGuardian(UUID guardianId, String ifMatch,
            GuardianUpdate guardianUpdate, String updateMask, Boolean validateOnly) {
        Guardian updated = guardianService.updateGuardian(guardianId, ifMatch, guardianUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
