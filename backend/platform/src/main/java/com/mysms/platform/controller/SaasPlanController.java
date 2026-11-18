package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.SaasPlanApi;
import com.mysms.platform.api.model.SaasPlan;
import com.mysms.platform.api.model.SaasPlanUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.SaasPlanService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the SaasPlan operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SaasPlanController implements SaasPlanApi {

    private final SaasPlanService saasPlanService;

    public SaasPlanController(SaasPlanService saasPlanService) {
        this.saasPlanService = saasPlanService;
    }

    @Override
    public ResponseEntity<SaasPlan> createSaasPlan(SaasPlan saasPlan, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SaasPlan created = saasPlanService.createSaasPlan(saasPlan, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/saas-plans/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSaasPlan(UUID saasPlanId, String ifMatch) {
        saasPlanService.deleteSaasPlan(saasPlanId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SaasPlan> getSaasPlan(UUID saasPlanId, String fields) {
        SaasPlan found = saasPlanService.getSaasPlan(saasPlanId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SaasPlan>> listSaasPlans(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<SaasPlan> list =
                PagingUtil.pagingInfo(saasPlanService.listSaasPlans(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SaasPlan> updateSaasPlan(UUID saasPlanId, String ifMatch,
            SaasPlanUpdate saasPlanUpdate, String updateMask, Boolean validateOnly) {
        SaasPlan updated = saasPlanService.updateSaasPlan(saasPlanId, ifMatch, saasPlanUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
