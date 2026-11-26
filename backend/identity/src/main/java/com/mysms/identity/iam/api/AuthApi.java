// Owned API contract of iam.
package com.mysms.identity.iam.api;

import com.mysms.identity.iam.api.model.CompleteMfaRequest;
import com.mysms.identity.iam.api.model.DisableMfaRequest;
import com.mysms.identity.iam.api.model.EnrollMfaRequest;
import com.mysms.identity.iam.api.model.LoginResult;
import com.mysms.identity.iam.api.model.MfaCredentialList;
import com.mysms.identity.iam.api.model.MfaEnrollment;
import org.springframework.lang.Nullable;
import com.mysms.identity.iam.api.model.Problem;
import com.mysms.identity.iam.api.model.RecoveryCodes;
import com.mysms.identity.iam.api.model.RegenerateRecoveryCodesRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
public interface AuthApi {

    String PATH_COMPLETE_MFA = "/v1/iam/mfa:complete";
    /**
     * POST /v1/iam/mfa:complete : Complete the MFA step of sign-in
     * Exchanges the mfaToken from Login plus a valid code for the token pair. Also verifies a new enrollment on first use.
     *
     * @param completeMfaRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = AuthApi.PATH_COMPLETE_MFA,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LoginResult> completeMfa(
         @Valid @RequestBody CompleteMfaRequest completeMfaRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DISABLE_MFA = "/v1/iam/mfa:disable";
    /**
     * POST /v1/iam/mfa:disable : Disable a second factor
     * Disables one of the caller&#39;s MFA credentials. Requires a fresh code (or a recovery code) to prove possession.
     *
     * @param disableMfaRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Disabled (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = AuthApi.PATH_DISABLE_MFA,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> disableMfa(
         @Valid @RequestBody DisableMfaRequest disableMfaRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_ENROLL_MFA = "/v1/iam/mfa:enroll";
    /**
     * POST /v1/iam/mfa:enroll : Enroll a second factor
     * Starts MFA enrollment for the caller (TOTP secret or OTP target). The credential becomes usable after it is verified once with a valid code.
     *
     * @param enrollMfaRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = AuthApi.PATH_ENROLL_MFA,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MfaEnrollment> enrollMfa(
         @Valid @RequestBody EnrollMfaRequest enrollMfaRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_MFA_CREDENTIALS = "/v1/iam/mfa";
    /**
     * GET /v1/iam/mfa : List the caller&#39;s MFA credentials
     * Lists the caller&#39;s enrolled second factors (type, label, verification and usage state). Secrets are never returned.
     *
     * @return OK (status code 200)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = AuthApi.PATH_LIST_MFA_CREDENTIALS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MfaCredentialList> listMfaCredentials(
        
    );


    String PATH_REGENERATE_RECOVERY_CODES = "/v1/iam/mfa:regenerateRecoveryCodes";
    /**
     * POST /v1/iam/mfa:regenerateRecoveryCodes : Regenerate recovery codes
     * Invalidates all previous recovery codes and returns a fresh set once. Requires a current code to prove possession.
     *
     * @param regenerateRecoveryCodesRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = AuthApi.PATH_REGENERATE_RECOVERY_CODES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RecoveryCodes> regenerateRecoveryCodes(
         @Valid @RequestBody RegenerateRecoveryCodesRequest regenerateRecoveryCodesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
