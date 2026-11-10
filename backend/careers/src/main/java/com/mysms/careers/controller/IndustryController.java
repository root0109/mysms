package com.mysms.careers.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.careers.business.IndustryService;
import com.mysms.careers.api.IndustryApi;
import com.mysms.careers.api.model.Industry;
import com.mysms.careers.api.model.IndustryUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Industry operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/careers.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class IndustryController implements IndustryApi {

    private final IndustryService industryService;

    public IndustryController(IndustryService industryService) {
        this.industryService = industryService;
    }

    @Override
    public ResponseEntity<Industry> createIndustry(Industry industry, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Industry created = industryService.createIndustry(industry, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/careers/industries/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteIndustry(UUID industryId, String ifMatch) {
        industryService.deleteIndustry(industryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Industry> getIndustry(UUID industryId, String fields) {
        Industry found = industryService.getIndustry(industryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Industry>> listIndustries(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Industry> list =
                PagingUtil.pagingInfo(industryService.listIndustrys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Industry> updateIndustry(UUID industryId, String ifMatch,
            IndustryUpdate industryUpdate, String updateMask, Boolean validateOnly) {
        Industry updated = industryService.updateIndustry(industryId, ifMatch, industryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
