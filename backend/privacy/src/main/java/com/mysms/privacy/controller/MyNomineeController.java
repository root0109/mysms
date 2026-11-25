package com.mysms.privacy.controller;

import com.mysms.privacy.api.MyNomineeApi;
import com.mysms.privacy.api.model.MyNominee;
import com.mysms.privacy.api.model.RevokeMyNomineeRequest;
import com.mysms.privacy.api.model.SetMyNomineeRequest;
import com.mysms.privacy.business.NomineeService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the caller's own nominee; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class MyNomineeController implements MyNomineeApi {

    private final NomineeService nomineeService;

    public MyNomineeController(NomineeService nomineeService) {
        this.nomineeService = nomineeService;
    }

    @Override
    public ResponseEntity<MyNominee> getMyNominee() {
        return ResponseEntity.ok(nomineeService.getMyNominee());
    }

    @Override
    public ResponseEntity<Void> revokeMyNominee(RevokeMyNomineeRequest revokeMyNomineeRequest, UUID idempotencyKey) {
        nomineeService.revokeMyNominee(revokeMyNomineeRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MyNominee> setMyNominee(SetMyNomineeRequest setMyNomineeRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(nomineeService.setMyNominee(setMyNomineeRequest));
    }
}
