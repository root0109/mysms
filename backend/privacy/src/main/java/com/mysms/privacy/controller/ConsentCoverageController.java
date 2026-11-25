package com.mysms.privacy.controller;

import com.mysms.privacy.api.ConsentCoverageApi;
import com.mysms.privacy.api.model.ConsentCoverage;
import com.mysms.privacy.business.ConsentCoverageService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of consent coverage; authentication and the permission check already ran in the auth-client filter. */
@RestController
public class ConsentCoverageController implements ConsentCoverageApi {

    private final ConsentCoverageService consentCoverageService;

    public ConsentCoverageController(ConsentCoverageService consentCoverageService) {
        this.consentCoverageService = consentCoverageService;
    }

    @Override
    public ResponseEntity<ConsentCoverage> getConsentCoverage(String purposeCode, String scope, UUID institutionId) {
        return ResponseEntity.ok(consentCoverageService.getConsentCoverage(purposeCode, scope, institutionId));
    }
}
