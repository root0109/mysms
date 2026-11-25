// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.privacy.api.model.MyNominee;
import com.mysms.privacy.api.model.RevokeMyNomineeRequest;
import com.mysms.privacy.api.model.SetMyNomineeRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Validated
public interface MyNomineeApi {

    String PATH_GET_MY_NOMINEE = "/v1/privacy/my/nominee";
    /**
     * GET /v1/privacy/my/nominee : Get my nominee
     * The caller's active nominee (DPDP Act s.14), name and contact masked. Not found when the caller names none.
     *
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = MyNomineeApi.PATH_GET_MY_NOMINEE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MyNominee> getMyNominee();


    String PATH_REVOKE_MY_NOMINEE = "/v1/privacy/my/nominee:revoke";
    /**
     * POST /v1/privacy/my/nominee:revoke : Revoke my nominee
     * Revoke the caller's active nominee; from then on they cannot act for the caller. Refused while impersonating.
     * Supports Idempotency-Key.
     *
     * @param revokeMyNomineeRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return No content (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = MyNomineeApi.PATH_REVOKE_MY_NOMINEE,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> revokeMyNominee(
         @Valid @RequestBody RevokeMyNomineeRequest revokeMyNomineeRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_MY_NOMINEE = "/v1/privacy/my/nominee:set";
    /**
     * POST /v1/privacy/my/nominee:set : Set my nominee
     * Name the person who may exercise the caller's rights on their death or incapacity (DPDP Act s.14), replacing the
     * active nominee. Only an adult names a nominee. Name and contact are stored encrypted and answered masked. Refused
     * while impersonating. Supports Idempotency-Key.
     *
     * @param setMyNomineeRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = MyNomineeApi.PATH_SET_MY_NOMINEE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<MyNominee> setMyNominee(
         @Valid @RequestBody SetMyNomineeRequest setMyNomineeRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );
}
