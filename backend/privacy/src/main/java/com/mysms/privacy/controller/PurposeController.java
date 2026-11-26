package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.PurposeApi;
import com.mysms.privacy.api.model.Purpose;
import com.mysms.privacy.api.model.PurposeUpdate;
import com.mysms.privacy.business.PurposeService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Purpose operations; authentication and the permission check already ran in the auth-client filter.
 */
@RestController
public class PurposeController implements PurposeApi {

    private final PurposeService purposeService;

    public PurposeController(PurposeService purposeService) {
        this.purposeService = purposeService;
    }

    @Override
    public ResponseEntity<Purpose> getPurpose(UUID purposeId, String fields) {
        Purpose found = purposeService.getPurpose(purposeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Purpose>> listPurposes(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Purpose> list = PagingUtil.pagingInfo(purposeService.listPurposes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Purpose> updatePurpose(UUID purposeId, String ifMatch, PurposeUpdate purposeUpdate,
            Boolean validateOnly) {
        Purpose updated = purposeService.updatePurpose(purposeId, ifMatch, purposeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
