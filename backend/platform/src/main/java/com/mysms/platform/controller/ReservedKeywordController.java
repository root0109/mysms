package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.ReservedKeywordApi;
import com.mysms.platform.api.model.ReservedKeyword;
import com.mysms.platform.api.model.ReservedKeywordUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.ReservedKeywordService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ReservedKeyword operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/platform.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ReservedKeywordController implements ReservedKeywordApi {

    private final ReservedKeywordService reservedKeywordService;

    public ReservedKeywordController(ReservedKeywordService reservedKeywordService) {
        this.reservedKeywordService = reservedKeywordService;
    }

    @Override
    public ResponseEntity<ReservedKeyword> createReservedKeyword(ReservedKeyword reservedKeyword,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ReservedKeyword created = reservedKeywordService.createReservedKeyword(reservedKeyword, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/reserved-keywords/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteReservedKeyword(UUID reservedKeywordId, String ifMatch) {
        reservedKeywordService.deleteReservedKeyword(reservedKeywordId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ReservedKeyword> getReservedKeyword(UUID reservedKeywordId, String fields) {
        ReservedKeyword found = reservedKeywordService.getReservedKeyword(reservedKeywordId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ReservedKeyword>> listReservedKeywords(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ReservedKeyword> list =
                PagingUtil.pagingInfo(reservedKeywordService.listReservedKeywords(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ReservedKeyword> updateReservedKeyword(UUID reservedKeywordId, String ifMatch,
            ReservedKeywordUpdate reservedKeywordUpdate, String updateMask, Boolean validateOnly) {
        ReservedKeyword updated =
                reservedKeywordService.updateReservedKeyword(reservedKeywordId, ifMatch, reservedKeywordUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
