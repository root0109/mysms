package com.mysms.iam.controller;

import com.mysms.contracts.iam.api.AuthApi;
import com.mysms.contracts.iam.model.CompleteMfaRequest;
import com.mysms.contracts.iam.model.DisableMfaRequest;
import com.mysms.contracts.iam.model.EnrollMfaRequest;
import com.mysms.contracts.iam.model.LoginResult;
import com.mysms.contracts.iam.model.MfaCredentialList;
import com.mysms.contracts.iam.model.MfaEnrollment;
import com.mysms.contracts.iam.model.RecoveryCodes;
import com.mysms.contracts.iam.model.RegenerateRecoveryCodesRequest;
import com.mysms.iam.business.MfaService;
import com.mysms.iam.business.SignInService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the second-factor operations (api/openapi/iam.yaml, tag Auth). Thin: delegate, then shape the
 * response. Answers that carry secrets, codes or tokens are never cached.
 */
@RestController
public class AuthController implements AuthApi {

    private final MfaService mfa;
    private final SignInService signIn;
    private final HttpServletRequest request;

    public AuthController(MfaService mfa, SignInService signIn, HttpServletRequest request) {
        this.mfa = mfa;
        this.signIn = signIn;
        this.request = request;
    }

    @Override
    public ResponseEntity<LoginResult> completeMfa(CompleteMfaRequest completeMfaRequest, UUID idempotencyKey) {
        LoginResult result = signIn.completeMfa(completeMfaRequest, request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    @Override
    public ResponseEntity<Void> disableMfa(DisableMfaRequest disableMfaRequest, UUID idempotencyKey) {
        mfa.disable(disableMfaRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MfaEnrollment> enrollMfa(EnrollMfaRequest enrollMfaRequest, UUID idempotencyKey) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mfa.enroll(enrollMfaRequest));
    }

    @Override
    public ResponseEntity<MfaCredentialList> listMfaCredentials() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mfa.list());
    }

    @Override
    public ResponseEntity<RecoveryCodes> regenerateRecoveryCodes(RegenerateRecoveryCodesRequest request,
                                                                UUID idempotencyKey) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mfa.regenerateRecoveryCodes(request));
    }
}
