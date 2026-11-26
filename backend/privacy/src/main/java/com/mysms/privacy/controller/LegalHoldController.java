package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.LegalHoldApi;
import com.mysms.privacy.api.model.LegalHold;
import com.mysms.privacy.api.model.LegalHoldUpdate;
import com.mysms.privacy.api.model.ListActiveHoldsResponse;
import com.mysms.privacy.api.model.ReleaseLegalHoldRequest;
import com.mysms.privacy.business.LegalHoldService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the legal holds (the data protection officer's and the safeguarding lead's) and of the modules' check
 * of the holds that apply; authentication and the permission check already ran in the auth-client filter.
 */
@RestController
public class LegalHoldController implements LegalHoldApi {

    private static final String LEGAL_HOLDS = "/v1/privacy/legal-holds/";

    private final LegalHoldService legalHoldService;

    public LegalHoldController(LegalHoldService legalHoldService) {
        this.legalHoldService = legalHoldService;
    }

    @Override
    public ResponseEntity<LegalHold> getLegalHold(UUID legalHoldId, String fields) {
        LegalHold found = legalHoldService.getLegalHold(legalHoldId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ListActiveHoldsResponse> listActiveHolds(UUID subjectPersonId, String module) {
        return ResponseEntity.ok(legalHoldService.listActiveHolds(subjectPersonId, module));
    }

    @Override
    public ResponseEntity<PagingInfo<LegalHold>> listLegalHolds(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LegalHold> list =
                PagingUtil.pagingInfo(legalHoldService.listLegalHolds(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LegalHold> placeLegalHold(LegalHold legalHold, UUID idempotencyKey) {
        LegalHold placed = legalHoldService.placeLegalHold(legalHold);
        return ResponseEntity.created(URI.create(LEGAL_HOLDS + placed.getId())).eTag(placed.getEtag()).body(placed);
    }

    @Override
    public ResponseEntity<LegalHold> releaseLegalHold(UUID legalHoldId, ReleaseLegalHoldRequest releaseLegalHoldRequest,
            UUID idempotencyKey) {
        return changed(legalHoldService.releaseLegalHold(legalHoldId, releaseLegalHoldRequest));
    }

    @Override
    public ResponseEntity<LegalHold> updateLegalHold(UUID legalHoldId, String ifMatch, LegalHoldUpdate legalHoldUpdate) {
        return changed(legalHoldService.updateLegalHold(legalHoldId, ifMatch, legalHoldUpdate));
    }

    private static ResponseEntity<LegalHold> changed(LegalHold hold) {
        return ResponseEntity.ok().eTag(hold.getEtag()).body(hold);
    }
}
