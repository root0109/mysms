package com.mysms.people.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.people.api.HonorificApi;
import com.mysms.people.api.model.Honorific;
import com.mysms.people.api.model.HonorificUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.people.business.HonorificService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Honorific operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/people.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class HonorificController implements HonorificApi {

    private final HonorificService honorificService;

    public HonorificController(HonorificService honorificService) {
        this.honorificService = honorificService;
    }

    @Override
    public ResponseEntity<Honorific> createHonorific(Honorific honorific, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Honorific created = honorificService.createHonorific(honorific, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/honorifics/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteHonorific(UUID honorificId, String ifMatch) {
        honorificService.deleteHonorific(honorificId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Honorific> getHonorific(UUID honorificId, String fields) {
        Honorific found = honorificService.getHonorific(honorificId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Honorific>> listHonorifics(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Honorific> list =
                PagingUtil.pagingInfo(honorificService.listHonorifics(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Honorific> updateHonorific(UUID honorificId, String ifMatch,
            HonorificUpdate honorificUpdate, String updateMask, Boolean validateOnly) {
        Honorific updated = honorificService.updateHonorific(honorificId, ifMatch, honorificUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
