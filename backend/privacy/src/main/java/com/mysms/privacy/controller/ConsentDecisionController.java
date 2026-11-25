package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.privacy.api.ConsentDecisionApi;
import com.mysms.privacy.api.model.BatchCheckConsentRequest;
import com.mysms.privacy.api.model.BatchCheckConsentResponse;
import com.mysms.privacy.api.model.ConsentDecision;
import com.mysms.privacy.api.model.ConsentedSubject;
import com.mysms.privacy.api.model.PurposeStatus;
import com.mysms.privacy.business.ConsentDecisionService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsentDecisionController implements ConsentDecisionApi {

    private final ConsentDecisionService consentDecisionService;

    public ConsentDecisionController(ConsentDecisionService consentDecisionService) {
        this.consentDecisionService = consentDecisionService;
    }

    @Override
    public ResponseEntity<ConsentDecision> checkConsent(String activityCode, UUID subjectPersonId, String scope) {
        return ResponseEntity.ok(consentDecisionService.checkConsent(activityCode, subjectPersonId, scope));
    }

    @Override
    public ResponseEntity<BatchCheckConsentResponse> batchCheckConsent(BatchCheckConsentRequest request) {
        return ResponseEntity.ok(consentDecisionService.batchCheckConsent(request));
    }

    @Override
    public ResponseEntity<PurposeStatus> getPurposeStatus(String activityCode) {
        return ResponseEntity.ok(consentDecisionService.getPurposeStatus(activityCode));
    }

    @Override
    public ResponseEntity<PagingInfo<ConsentedSubject>> listSubjectsWithConsent(String activityCode, String scope,
            List<UUID> institutionIds, Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(consentDecisionService.listSubjectsWithConsent(activityCode,
                scope, institutionIds, page, size, cursor)));
    }
}
