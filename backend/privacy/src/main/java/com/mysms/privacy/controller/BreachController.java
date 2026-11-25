package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.BreachApi;
import com.mysms.privacy.api.model.Breach;
import com.mysms.privacy.api.model.BreachStepRequest;
import com.mysms.privacy.api.model.BreachUpdate;
import com.mysms.privacy.api.model.MarkBreachNotABreachRequest;
import com.mysms.privacy.api.model.RecordBreachNotificationRequest;
import com.mysms.privacy.business.BreachService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the breach register (the data protection officer's); authentication and the permission check already
 * ran in the auth-client filter.
 */
@RestController
public class BreachController implements BreachApi {

    private static final String BREACHES = "/v1/privacy/breaches/";

    private final BreachService breachService;

    public BreachController(BreachService breachService) {
        this.breachService = breachService;
    }

    @Override
    public ResponseEntity<Breach> closeBreach(UUID breachId, BreachStepRequest breachStepRequest,
            UUID idempotencyKey) {
        return stepped(breachService.closeBreach(breachId, breachStepRequest));
    }

    @Override
    public ResponseEntity<Breach> containBreach(UUID breachId, BreachStepRequest breachStepRequest,
            UUID idempotencyKey) {
        return stepped(breachService.containBreach(breachId, breachStepRequest));
    }

    @Override
    public ResponseEntity<Breach> getBreach(UUID breachId, String fields) {
        Breach found = breachService.getBreach(breachId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Breach>> listBreaches(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Breach> list = PagingUtil.pagingInfo(breachService.listBreaches(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Breach> markBreachNotABreach(UUID breachId,
            MarkBreachNotABreachRequest markBreachNotABreachRequest, UUID idempotencyKey) {
        return stepped(breachService.markBreachNotABreach(breachId, markBreachNotABreachRequest));
    }

    @Override
    public ResponseEntity<Breach> recordBreachNotification(UUID breachId,
            RecordBreachNotificationRequest recordBreachNotificationRequest, UUID idempotencyKey) {
        return stepped(breachService.recordBreachNotification(breachId, recordBreachNotificationRequest));
    }

    @Override
    public ResponseEntity<Breach> reportBreach(Breach breach, UUID idempotencyKey) {
        Breach reported = breachService.reportBreach(breach);
        return ResponseEntity.created(URI.create(BREACHES + reported.getId())).eTag(reported.getEtag()).body(reported);
    }

    @Override
    public ResponseEntity<Breach> triageBreach(UUID breachId, BreachStepRequest breachStepRequest,
            UUID idempotencyKey) {
        return stepped(breachService.triageBreach(breachId, breachStepRequest));
    }

    @Override
    public ResponseEntity<Breach> updateBreach(UUID breachId, String ifMatch, BreachUpdate breachUpdate,
            String updateMask) {
        return stepped(breachService.updateBreach(breachId, ifMatch, breachUpdate, updateMask));
    }

    private static ResponseEntity<Breach> stepped(Breach changed) {
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }
}
