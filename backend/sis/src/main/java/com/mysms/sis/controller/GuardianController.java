package com.mysms.sis.controller;

import com.mysms.sis.api.model.ChildCard;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.GuardianApi;
import com.mysms.sis.api.model.CreatePortalAccountRequest;
import com.mysms.sis.api.model.Guardian;
import com.mysms.sis.api.model.GuardianUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.GuardianService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Guardian operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/sis.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class GuardianController implements GuardianApi {

    private final GuardianService guardianService;

    public GuardianController(GuardianService guardianService) {
        this.guardianService = guardianService;
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
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Guardian> updateGuardian(UUID guardianId, String ifMatch,
            GuardianUpdate guardianUpdate, String updateMask, Boolean validateOnly) {
        Guardian updated = guardianService.updateGuardian(guardianId, ifMatch, guardianUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
