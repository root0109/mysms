// Owned API contract of iam.
package com.mysms.identity.iam.api;

import com.mysms.identity.iam.api.model.AuthToken;
import com.mysms.core.paging.PagingInfo;
import com.mysms.identity.iam.api.model.AcceptTermsRequest;
import com.mysms.identity.iam.api.model.ActivateUserRequest;
import com.mysms.identity.iam.api.model.ApiTokenCreated;
import com.mysms.identity.iam.api.model.Availability;
import com.mysms.identity.iam.api.model.TenantCodeResolution;
import com.mysms.identity.iam.api.model.ChangePasswordRequest;
import com.mysms.identity.iam.api.model.ConfirmVerificationRequest;
import com.mysms.identity.iam.api.model.CreateApiTokenRequest;
import com.mysms.identity.iam.api.model.DeactivateUserRequest;
import com.mysms.identity.iam.api.model.EffectiveSetting;
import com.mysms.identity.iam.api.model.EffectiveSettingList;
import com.mysms.identity.iam.api.model.ForcePasswordResetRequest;
import com.mysms.identity.iam.api.model.ImpersonationToken;
import com.mysms.identity.iam.api.model.ImportUsersRequest;
import com.mysms.identity.iam.api.model.LoginRequest;
import com.mysms.identity.iam.api.model.LoginResult;
import com.mysms.identity.iam.api.model.LogoutRequest;
import com.mysms.identity.iam.api.model.Me;
import org.springframework.lang.Nullable;
import com.mysms.identity.iam.api.model.Operation;
import com.mysms.identity.iam.api.model.RefreshTokenRequest;
import com.mysms.identity.iam.api.model.RequestPasswordResetRequest;
import com.mysms.identity.iam.api.model.RequestVerificationRequest;
import com.mysms.identity.iam.api.model.ResetPasswordRequest;
import com.mysms.identity.iam.api.model.SetMySettingRequest;
import com.mysms.identity.iam.api.model.SetUserCredentialsRequest;
import com.mysms.identity.iam.api.model.SetUserRolesRequest;
import com.mysms.identity.iam.api.model.SignUpInstitutionList;
import com.mysms.identity.iam.api.model.SignUpRequest;
import com.mysms.identity.iam.api.model.StartImpersonationRequest;
import com.mysms.identity.iam.api.model.SwitchRoleRequest;
import com.mysms.identity.iam.api.model.SwitchRoleResult;
import com.mysms.identity.iam.api.model.TokenPair;
import java.util.UUID;
import com.mysms.identity.iam.api.model.UnlockUserRequest;
import com.mysms.identity.iam.api.model.UserAccount;
import com.mysms.identity.iam.api.model.UserAccountUpdate;
import com.mysms.identity.iam.api.model.UserDepartmentAccess;
import com.mysms.identity.iam.api.model.UserDepartmentAccessUpdate;
import com.mysms.identity.iam.api.model.UserDevice;
import com.mysms.identity.iam.api.model.UserDeviceUpdate;
import com.mysms.identity.iam.api.model.UserPermission;
import com.mysms.identity.iam.api.model.UserPermissionUpdate;
import com.mysms.identity.iam.api.model.UserPreference;
import com.mysms.identity.iam.api.model.UserPreferenceUpdate;
import com.mysms.identity.iam.api.model.UserRole;
import com.mysms.identity.iam.api.model.UserRoleList;
import com.mysms.identity.iam.api.model.UserRoleUpdate;
import com.mysms.identity.iam.api.model.UserSession;
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
public interface UserAccountApi {

    String PATH_ACCEPT_TERMS = "/v1/iam/me:acceptTerms";
    /**
     * POST /v1/iam/me:acceptTerms : Record acceptance of the terms and conditions
     * Record acceptance of the terms and conditions.
     *
     * @param acceptTermsRequest  (required)
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
        value = UserAccountApi.PATH_ACCEPT_TERMS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> acceptTerms(
         @Valid @RequestBody AcceptTermsRequest acceptTermsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_ACTIVATE_USER = "/v1/iam/users/{userAccountId}:activate";
    /**
     * POST /v1/iam/users/{userAccountId}:activate : Set status active
     * Set status active.
     *
     * @param userAccountId  (required)
     * @param activateUserRequest  (required)
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
        value = UserAccountApi.PATH_ACTIVATE_USER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> activateUser(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody ActivateUserRequest activateUserRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CHANGE_PASSWORD = "/v1/iam/auth:changePassword";
    /**
     * POST /v1/iam/auth:changePassword : Change the caller&#39;s password
     * Change the caller&#39;s password.
     *
     * @param changePasswordRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = UserAccountApi.PATH_CHANGE_PASSWORD,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> changePassword(
         @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CHECK_USERNAME_AVAILABLE = "/v1/iam/users:checkUsername";
    /**
     * GET /v1/iam/users:checkUsername : Whether a login name is free in the tenant
     * Whether a login name is free in the tenant.
     *
     * @param username Wanted login name. (required)
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
        method = RequestMethod.GET,
        value = UserAccountApi.PATH_CHECK_USERNAME_AVAILABLE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Availability> checkUsernameAvailable(
        @NotNull  @Valid @RequestParam(value = "username", required = true) String username
    );


    String PATH_RESOLVE_TENANT_CODE = "/v1/iam/tenant-codes/{tenantCode}";
    /**
     * GET /v1/iam/tenant-codes/{tenantCode} : The active tenant a school code names
     * Guests name their school by the code users type at sign-in; the edge of a module serving guests resolves it here.
     *
     * @param tenantCode Code users type at sign-in. (required)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Resource not found (an unknown, suspended or closed school). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = UserAccountApi.PATH_RESOLVE_TENANT_CODE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<TenantCodeResolution> resolveTenantCode(
        @PathVariable("tenantCode") String tenantCode
    );


    String PATH_LIST_SIGN_UP_INSTITUTIONS = "/v1/iam/tenant-codes/{tenantCode}/sign-up-institutions";
    /**
     * GET /v1/iam/tenant-codes/{tenantCode}/sign-up-institutions : the schools a guest may sign up at (id and name of
     * the tenant's active institutions); 403 where sign-up is closed or the code names no active school, like SignUp.
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = UserAccountApi.PATH_LIST_SIGN_UP_INSTITUTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SignUpInstitutionList> listSignUpInstitutions(
        @PathVariable("tenantCode") String tenantCode
    );


    String PATH_CONFIRM_VERIFICATION = "/v1/iam/auth:confirmVerification";
    /**
     * POST /v1/iam/auth:confirmVerification : Confirm the code and mark e-mail / mobile verified
     * Confirm the code and mark e-mail / mobile verified.
     *
     * @param confirmVerificationRequest  (required)
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
        value = UserAccountApi.PATH_CONFIRM_VERIFICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> confirmVerification(
         @Valid @RequestBody ConfirmVerificationRequest confirmVerificationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_API_TOKEN = "/v1/iam/users/{userAccountId}:createApiToken";
    /**
     * POST /v1/iam/users/{userAccountId}:createApiToken : Create a personal API token
     * Create a personal API token. The secret is returned once and only its hash is stored.
     *
     * @param userAccountId  (required)
     * @param createApiTokenRequest  (required)
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
        value = UserAccountApi.PATH_CREATE_API_TOKEN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ApiTokenCreated> createApiToken(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody CreateApiTokenRequest createApiTokenRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_USER_ACCOUNT = "/v1/iam/users";
    /**
     * POST /v1/iam/users : Create UserAccount
     * Create a UserAccount. Supports Idempotency-Key and validateOnly.
     *
     * @param userAccount  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
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
        value = UserAccountApi.PATH_CREATE_USER_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> createUserAccount(
         @Valid @RequestBody UserAccount userAccount,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_USER_DEPARTMENT_ACCESS = "/v1/iam/users/{userAccountId}/departments";
    /**
     * POST /v1/iam/users/{userAccountId}/departments : Create UserDepartmentAccess
     * Create a UserDepartmentAccess. Supports Idempotency-Key and validateOnly.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDepartmentAccess  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
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
        value = UserAccountApi.PATH_CREATE_USER_DEPARTMENT_ACCESS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserDepartmentAccess> createUserDepartmentAccess(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody UserDepartmentAccess userDepartmentAccess,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_USER_DEVICE = "/v1/iam/users/{userAccountId}/devices";
    /**
     * POST /v1/iam/users/{userAccountId}/devices : Create UserDevice
     * Create a UserDevice. Supports Idempotency-Key and validateOnly.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDevice  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
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
        value = UserAccountApi.PATH_CREATE_USER_DEVICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserDevice> createUserDevice(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody UserDevice userDevice,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_USER_PERMISSION = "/v1/iam/users/{userAccountId}/permissions";
    /**
     * POST /v1/iam/users/{userAccountId}/permissions : Create UserPermission
     * Create a UserPermission. Supports Idempotency-Key and validateOnly.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPermission  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
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
        value = UserAccountApi.PATH_CREATE_USER_PERMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserPermission> createUserPermission(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody UserPermission userPermission,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_USER_PREFERENCE = "/v1/iam/users/{userAccountId}/preferences";
    /**
     * POST /v1/iam/users/{userAccountId}/preferences : Create UserPreference
     * Create a UserPreference. Supports Idempotency-Key and validateOnly.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPreference  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
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
        value = UserAccountApi.PATH_CREATE_USER_PREFERENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserPreference> createUserPreference(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody UserPreference userPreference,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_USER_ROLE = "/v1/iam/users/{userAccountId}/roles";
    /**
     * POST /v1/iam/users/{userAccountId}/roles : Create UserRole
     * Create a UserRole. Supports Idempotency-Key and validateOnly.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userRole  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
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
        value = UserAccountApi.PATH_CREATE_USER_ROLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserRole> createUserRole(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody UserRole userRole,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DEACTIVATE_USER = "/v1/iam/users/{userAccountId}:deactivate";
    /**
     * POST /v1/iam/users/{userAccountId}:deactivate : Set status inactive (login blocked)
     * Set status inactive (login blocked).
     *
     * @param userAccountId  (required)
     * @param deactivateUserRequest  (required)
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
        value = UserAccountApi.PATH_DEACTIVATE_USER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> deactivateUser(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody DeactivateUserRequest deactivateUserRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_AUTH_TOKEN = "/v1/iam/users/{userAccountId}/api-tokens/{authTokenId}";
    /**
     * DELETE /v1/iam/users/{userAccountId}/api-tokens/{authTokenId} : Delete AuthToken
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param authTokenId AuthToken id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_AUTH_TOKEN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAuthToken(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("authTokenId") UUID authTokenId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_USER_ACCOUNT = "/v1/iam/users/{userAccountId}";
    /**
     * DELETE /v1/iam/users/{userAccountId} : Delete UserAccount
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_USER_ACCOUNT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteUserAccount(
         @PathVariable("userAccountId") UUID userAccountId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_USER_DEPARTMENT_ACCESS = "/v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}";
    /**
     * DELETE /v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId} : Delete UserDepartmentAccess
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDepartmentAccessId UserDepartmentAccess id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_USER_DEPARTMENT_ACCESS,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteUserDepartmentAccess(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userDepartmentAccessId") UUID userDepartmentAccessId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_USER_DEVICE = "/v1/iam/users/{userAccountId}/devices/{userDeviceId}";
    /**
     * DELETE /v1/iam/users/{userAccountId}/devices/{userDeviceId} : Delete UserDevice
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDeviceId UserDevice id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_USER_DEVICE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteUserDevice(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userDeviceId") UUID userDeviceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_USER_PERMISSION = "/v1/iam/users/{userAccountId}/permissions/{userPermissionId}";
    /**
     * DELETE /v1/iam/users/{userAccountId}/permissions/{userPermissionId} : Delete UserPermission
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPermissionId UserPermission id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_USER_PERMISSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteUserPermission(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userPermissionId") UUID userPermissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_USER_PREFERENCE = "/v1/iam/users/{userAccountId}/preferences/{userPreferenceId}";
    /**
     * DELETE /v1/iam/users/{userAccountId}/preferences/{userPreferenceId} : Delete UserPreference
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPreferenceId UserPreference id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_USER_PREFERENCE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteUserPreference(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userPreferenceId") UUID userPreferenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_USER_ROLE = "/v1/iam/users/{userAccountId}/roles/{userRoleId}";
    /**
     * DELETE /v1/iam/users/{userAccountId}/roles/{userRoleId} : Delete UserRole
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param userRoleId UserRole id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_USER_ROLE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteUserRole(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userRoleId") UUID userRoleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_USER_SESSION = "/v1/iam/users/{userAccountId}/sessions/{userSessionId}";
    /**
     * DELETE /v1/iam/users/{userAccountId}/sessions/{userSessionId} : Delete UserSession
     * Soft delete (sets deleted_at).
     *
     * @param userAccountId UserAccount id. (required)
     * @param userSessionId UserSession id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_DELETE_USER_SESSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteUserSession(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userSessionId") UUID userSessionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_FORCE_PASSWORD_RESET = "/v1/iam/users/{userAccountId}:forcePasswordReset";
    /**
     * POST /v1/iam/users/{userAccountId}:forcePasswordReset : Require a new password at next login and send a reset link
     * Require a new password at next login and send a reset link.
     *
     * @param userAccountId  (required)
     * @param forcePasswordResetRequest  (required)
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
        value = UserAccountApi.PATH_FORCE_PASSWORD_RESET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> forcePasswordReset(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody ForcePasswordResetRequest forcePasswordResetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_ME = "/v1/iam/me";
    /**
     * GET /v1/iam/me : Current user, person, role grants, effective permissions and linked student/staff/guardian records
     * Current user, person, role grants, effective permissions and linked student/staff/guardian records.
     *
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
        method = RequestMethod.GET,
        value = UserAccountApi.PATH_GET_ME,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Me> getMe(
        
    );


    String PATH_GET_USER_ACCOUNT = "/v1/iam/users/{userAccountId}";
    /**
     * GET /v1/iam/users/{userAccountId} : Get UserAccount
     * Get one UserAccount by id. The ETag header carries the row version.
     *
     * @param userAccountId UserAccount id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_GET_USER_ACCOUNT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UserAccount> getUserAccount(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_USER_DEPARTMENT_ACCESS = "/v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}";
    /**
     * GET /v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId} : Get UserDepartmentAccess
     * Get one UserDepartmentAccess by id. The ETag header carries the row version.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDepartmentAccessId UserDepartmentAccess id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_GET_USER_DEPARTMENT_ACCESS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UserDepartmentAccess> getUserDepartmentAccess(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userDepartmentAccessId") UUID userDepartmentAccessId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_USER_DEVICE = "/v1/iam/users/{userAccountId}/devices/{userDeviceId}";
    /**
     * GET /v1/iam/users/{userAccountId}/devices/{userDeviceId} : Get UserDevice
     * Get one UserDevice by id. The ETag header carries the row version.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDeviceId UserDevice id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_GET_USER_DEVICE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UserDevice> getUserDevice(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userDeviceId") UUID userDeviceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_USER_PERMISSION = "/v1/iam/users/{userAccountId}/permissions/{userPermissionId}";
    /**
     * GET /v1/iam/users/{userAccountId}/permissions/{userPermissionId} : Get UserPermission
     * Get one UserPermission by id. The ETag header carries the row version.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPermissionId UserPermission id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_GET_USER_PERMISSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UserPermission> getUserPermission(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userPermissionId") UUID userPermissionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_USER_PREFERENCE = "/v1/iam/users/{userAccountId}/preferences/{userPreferenceId}";
    /**
     * GET /v1/iam/users/{userAccountId}/preferences/{userPreferenceId} : Get UserPreference
     * Get one UserPreference by id. The ETag header carries the row version.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPreferenceId UserPreference id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_GET_USER_PREFERENCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UserPreference> getUserPreference(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userPreferenceId") UUID userPreferenceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_USER_ROLE = "/v1/iam/users/{userAccountId}/roles/{userRoleId}";
    /**
     * GET /v1/iam/users/{userAccountId}/roles/{userRoleId} : Get UserRole
     * Get one UserRole by id. The ETag header carries the row version.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userRoleId UserRole id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_GET_USER_ROLE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UserRole> getUserRole(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userRoleId") UUID userRoleId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_USER_SESSION = "/v1/iam/users/{userAccountId}/sessions/{userSessionId}";
    /**
     * GET /v1/iam/users/{userAccountId}/sessions/{userSessionId} : Get UserSession
     * Get one UserSession by id. The ETag header carries the row version.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userSessionId UserSession id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_GET_USER_SESSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<UserSession> getUserSession(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userSessionId") UUID userSessionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_IMPORT_USERS = "/v1/iam/users:import";
    /**
     * POST /v1/iam/users:import : Bulk-create users with roles from a file
     * Bulk-create users with roles from a file.
     *
     * @param importUsersRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Accepted; poll the operation. (status code 202)
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
        value = UserAccountApi.PATH_IMPORT_USERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> importUsers(
         @Valid @RequestBody ImportUsersRequest importUsersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_AUTH_TOKENS = "/v1/iam/users/{userAccountId}/api-tokens";
    /**
     * GET /v1/iam/users/{userAccountId}/api-tokens : List AuthTokens
     * List AuthTokens visible to the caller, page by page, with filtering and sorting.
     *
     * @param userAccountId UserAccount id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_AUTH_TOKENS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AuthToken>> listAuthTokens(
         @PathVariable("userAccountId") UUID userAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MY_SETTINGS = "/v1/iam/me/settings";
    /**
     * GET /v1/iam/me/settings : The caller&#39;s effective settings
     * Every setting with the caller&#39;s effective value, its source scope and whether it is locked (user &gt; class section &gt; primary department &gt; institution &gt; tenant &gt; default; a lock at a broader scope wins). Settings users may not change are included only for callers who administer settings at one of their allowed scopes, and sourceScopeId only for tenant-wide administrators or the caller&#39;s own values.
     *
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
        method = RequestMethod.GET,
        value = UserAccountApi.PATH_LIST_MY_SETTINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EffectiveSettingList> listMySettings(
        
    );


    String PATH_LIST_USER_ACCOUNTS = "/v1/iam/users";
    /**
     * GET /v1/iam/users : List UserAccounts
     * List UserAccounts visible to the caller, page by page, with filtering and sorting.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_USER_ACCOUNTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<UserAccount>> listUserAccounts(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_USER_DEPARTMENT_ACCESSES = "/v1/iam/users/{userAccountId}/departments";
    /**
     * GET /v1/iam/users/{userAccountId}/departments : List UserDepartmentAccesses
     * List UserDepartmentAccesses visible to the caller, page by page, with filtering and sorting.
     *
     * @param userAccountId UserAccount id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_USER_DEPARTMENT_ACCESSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<UserDepartmentAccess>> listUserDepartmentAccesses(
         @PathVariable("userAccountId") UUID userAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_USER_DEVICES = "/v1/iam/users/{userAccountId}/devices";
    /**
     * GET /v1/iam/users/{userAccountId}/devices : List UserDevices
     * List UserDevices visible to the caller, page by page, with filtering and sorting.
     *
     * @param userAccountId UserAccount id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_USER_DEVICES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<UserDevice>> listUserDevices(
         @PathVariable("userAccountId") UUID userAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_USER_PERMISSIONS = "/v1/iam/users/{userAccountId}/permissions";
    /**
     * GET /v1/iam/users/{userAccountId}/permissions : List UserPermissions
     * List UserPermissions visible to the caller, page by page, with filtering and sorting.
     *
     * @param userAccountId UserAccount id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_USER_PERMISSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<UserPermission>> listUserPermissions(
         @PathVariable("userAccountId") UUID userAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_USER_PREFERENCES = "/v1/iam/users/{userAccountId}/preferences";
    /**
     * GET /v1/iam/users/{userAccountId}/preferences : List UserPreferences
     * List UserPreferences visible to the caller, page by page, with filtering and sorting.
     *
     * @param userAccountId UserAccount id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_USER_PREFERENCES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<UserPreference>> listUserPreferences(
         @PathVariable("userAccountId") UUID userAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_USER_ROLES = "/v1/iam/users/{userAccountId}/roles";
    /**
     * GET /v1/iam/users/{userAccountId}/roles : List UserRoles
     * List UserRoles visible to the caller, page by page, with filtering and sorting.
     *
     * @param userAccountId UserAccount id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_USER_ROLES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<UserRole>> listUserRoles(
         @PathVariable("userAccountId") UUID userAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_USER_SESSIONS = "/v1/iam/users/{userAccountId}/sessions";
    /**
     * GET /v1/iam/users/{userAccountId}/sessions : List UserSessions
     * List UserSessions visible to the caller, page by page, with filtering and sorting.
     *
     * @param userAccountId UserAccount id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
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
        value = UserAccountApi.PATH_LIST_USER_SESSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<UserSession>> listUserSessions(
         @PathVariable("userAccountId") UUID userAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LOGIN = "/v1/iam/auth:login";
    /**
     * POST /v1/iam/auth:login : Authenticate with credentials and receive tokens
     * Authenticate with credentials and receive tokens. Public.
     *
     * @param loginRequest  (required)
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
        value = UserAccountApi.PATH_LOGIN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LoginResult> login(
         @Valid @RequestBody LoginRequest loginRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LOGOUT = "/v1/iam/auth:logout";
    /**
     * POST /v1/iam/auth:logout : Revoke the current (or all) sessions and unregister the device
     * Revoke the current (or all) sessions and unregister the device.
     *
     * @param logoutRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = UserAccountApi.PATH_LOGOUT,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> logout(
         @Valid @RequestBody LogoutRequest logoutRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REFRESH_TOKEN = "/v1/iam/auth:refresh";
    /**
     * POST /v1/iam/auth:refresh : Rotate tokens
     * Rotate tokens. Public.
     *
     * @param refreshTokenRequest  (required)
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
        value = UserAccountApi.PATH_REFRESH_TOKEN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<TokenPair> refreshToken(
         @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REQUEST_PASSWORD_RESET = "/v1/iam/auth:requestPasswordReset";
    /**
     * POST /v1/iam/auth:requestPasswordReset : Send a reset link / OTP
     * Send a reset link / OTP. Always returns 204 to avoid account enumeration. Public.
     *
     * @param requestPasswordResetRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = UserAccountApi.PATH_REQUEST_PASSWORD_RESET,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> requestPasswordReset(
         @Valid @RequestBody RequestPasswordResetRequest requestPasswordResetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REQUEST_VERIFICATION = "/v1/iam/auth:requestVerification";
    /**
     * POST /v1/iam/auth:requestVerification : Send a verification code to the caller&#39;s e-mail or mobile
     * Send a verification code to the caller&#39;s e-mail or mobile.
     *
     * @param requestVerificationRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = UserAccountApi.PATH_REQUEST_VERIFICATION,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> requestVerification(
         @Valid @RequestBody RequestVerificationRequest requestVerificationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_MY_SETTING = "/v1/iam/me/settings/{settingKey}";
    /**
     * DELETE /v1/iam/me/settings/{settingKey} : Reset one of the caller&#39;s own settings
     * Removes the caller&#39;s user-level value, so the broader scopes apply again.
     *
     * @param settingKey The setting. (required)
     * @return Reset; the broader scopes apply. (status code 204)
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
        method = RequestMethod.DELETE,
        value = UserAccountApi.PATH_RESET_MY_SETTING,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> resetMySetting(
        @Size(max = 100)  @PathVariable("settingKey") String settingKey
    );


    String PATH_RESET_PASSWORD = "/v1/iam/auth:resetPassword";
    /**
     * POST /v1/iam/auth:resetPassword : Complete a password reset
     * Complete a password reset. Public.
     *
     * @param resetPasswordRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = UserAccountApi.PATH_RESET_PASSWORD,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> resetPassword(
         @Valid @RequestBody ResetPasswordRequest resetPasswordRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_MY_SETTING = "/v1/iam/me/settings/{settingKey}";
    /**
     * PUT /v1/iam/me/settings/{settingKey} : Set one of the caller&#39;s own settings
     * Sets the caller&#39;s user-level value. Rejected with 409 when a broader scope locks the setting and 422 when the value does not fit the setting or the setting is not user-editable.
     *
     * @param settingKey The setting. (required)
     * @param setMySettingRequest  (required)
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
        method = RequestMethod.PUT,
        value = UserAccountApi.PATH_SET_MY_SETTING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EffectiveSetting> setMySetting(
        @Size(max = 100)  @PathVariable("settingKey") String settingKey,
         @Valid @RequestBody SetMySettingRequest setMySettingRequest
    );


    String PATH_SET_USER_CREDENTIALS = "/v1/iam/users/{userAccountId}:setCredentials";
    /**
     * POST /v1/iam/users/{userAccountId}:setCredentials : Administrator sets a user&#39;s login name and a temporary password
     * Administrator sets a user&#39;s login name and a temporary password.
     *
     * @param userAccountId  (required)
     * @param setUserCredentialsRequest  (required)
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
        value = UserAccountApi.PATH_SET_USER_CREDENTIALS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> setUserCredentials(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody SetUserCredentialsRequest setUserCredentialsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_USER_ROLES = "/v1/iam/users/{userAccountId}:setRoles";
    /**
     * POST /v1/iam/users/{userAccountId}:setRoles : Replace the user&#39;s role grants in one call
     * Replace the user&#39;s role grants in one call. The caller may only grant roles whose permissions are a subset of their own, within their own scopes (no privilege escalation).
     *
     * @param userAccountId  (required)
     * @param setUserRolesRequest  (required)
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
        value = UserAccountApi.PATH_SET_USER_ROLES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserRoleList> setUserRoles(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody SetUserRolesRequest setUserRolesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SIGN_UP = "/v1/iam/auth:signUp";
    /**
     * POST /v1/iam/auth:signUp : Self-registration of a student (when the tenant allows signup)
     * Self-registration of a student (when the tenant allows signup). Creates the account and sends an e-mail verification; tokens are then obtained with Login. Public.
     *
     * @param signUpRequest  (required)
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
        value = UserAccountApi.PATH_SIGN_UP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> signUp(
         @Valid @RequestBody SignUpRequest signUpRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_START_IMPERSONATION = "/v1/iam/users/{userAccountId}:impersonate";
    /**
     * POST /v1/iam/users/{userAccountId}:impersonate : Administrator obtains a non-refreshable access token (maximum 30 minutes) acting as this user
     * Administrator obtains a non-refreshable access token (maximum 30 minutes) acting as this user. The target must have strictly lower privileges and lie within the administrator&#39;s institution scope. Every call is audited with both identities; StopImpersonation ends it.
     *
     * @param userAccountId  (required)
     * @param startImpersonationRequest  (required)
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
        value = UserAccountApi.PATH_START_IMPERSONATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ImpersonationToken> startImpersonation(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody StartImpersonationRequest startImpersonationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_STOP_IMPERSONATION = "/v1/iam/auth:stopImpersonation";
    /**
     * POST /v1/iam/auth:stopImpersonation : Return to the administrator&#39;s own session
     * Return to the administrator&#39;s own session.
     *
     * @param body  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = UserAccountApi.PATH_STOP_IMPERSONATION,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> stopImpersonation(
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SWITCH_ROLE = "/v1/iam/me:switchRole";
    /**
     * POST /v1/iam/me:switchRole : Switch the active role: issues new tokens whose claims carry the active role (permissions are evaluated for that role only) and remembers it as last_login_role_id
     * Switch the active role: issues new tokens whose claims carry the active role (permissions are evaluated for that role only) and remembers it as last_login_role_id.
     *
     * @param switchRoleRequest  (required)
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
        value = UserAccountApi.PATH_SWITCH_ROLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SwitchRoleResult> switchRole(
         @Valid @RequestBody SwitchRoleRequest switchRoleRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UNLOCK_USER = "/v1/iam/users/{userAccountId}:unlock";
    /**
     * POST /v1/iam/users/{userAccountId}:unlock : Clear a lock caused by failed logins
     * Clear a lock caused by failed logins.
     *
     * @param userAccountId  (required)
     * @param unlockUserRequest  (required)
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
        value = UserAccountApi.PATH_UNLOCK_USER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> unlockUser(
         @PathVariable("userAccountId") UUID userAccountId,
         @Valid @RequestBody UnlockUserRequest unlockUserRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_USER_ACCOUNT = "/v1/iam/users/{userAccountId}";
    /**
     * PATCH /v1/iam/users/{userAccountId} : Update UserAccount
     * Partially update a UserAccount (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param userAccountId UserAccount id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param userAccountUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = UserAccountApi.PATH_UPDATE_USER_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserAccount> updateUserAccount(
         @PathVariable("userAccountId") UUID userAccountId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody UserAccountUpdate userAccountUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_USER_DEPARTMENT_ACCESS = "/v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId}";
    /**
     * PATCH /v1/iam/users/{userAccountId}/departments/{userDepartmentAccessId} : Update UserDepartmentAccess
     * Partially update a UserDepartmentAccess (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDepartmentAccessId UserDepartmentAccess id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param userDepartmentAccessUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = UserAccountApi.PATH_UPDATE_USER_DEPARTMENT_ACCESS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserDepartmentAccess> updateUserDepartmentAccess(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userDepartmentAccessId") UUID userDepartmentAccessId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody UserDepartmentAccessUpdate userDepartmentAccessUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_USER_DEVICE = "/v1/iam/users/{userAccountId}/devices/{userDeviceId}";
    /**
     * PATCH /v1/iam/users/{userAccountId}/devices/{userDeviceId} : Update UserDevice
     * Partially update a UserDevice (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userDeviceId UserDevice id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param userDeviceUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = UserAccountApi.PATH_UPDATE_USER_DEVICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserDevice> updateUserDevice(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userDeviceId") UUID userDeviceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody UserDeviceUpdate userDeviceUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_USER_PERMISSION = "/v1/iam/users/{userAccountId}/permissions/{userPermissionId}";
    /**
     * PATCH /v1/iam/users/{userAccountId}/permissions/{userPermissionId} : Update UserPermission
     * Partially update a UserPermission (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPermissionId UserPermission id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param userPermissionUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = UserAccountApi.PATH_UPDATE_USER_PERMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserPermission> updateUserPermission(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userPermissionId") UUID userPermissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody UserPermissionUpdate userPermissionUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_USER_PREFERENCE = "/v1/iam/users/{userAccountId}/preferences/{userPreferenceId}";
    /**
     * PATCH /v1/iam/users/{userAccountId}/preferences/{userPreferenceId} : Update UserPreference
     * Partially update a UserPreference (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userPreferenceId UserPreference id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param userPreferenceUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = UserAccountApi.PATH_UPDATE_USER_PREFERENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserPreference> updateUserPreference(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userPreferenceId") UUID userPreferenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody UserPreferenceUpdate userPreferenceUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_USER_ROLE = "/v1/iam/users/{userAccountId}/roles/{userRoleId}";
    /**
     * PATCH /v1/iam/users/{userAccountId}/roles/{userRoleId} : Update UserRole
     * Partially update a UserRole (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param userAccountId UserAccount id. (required)
     * @param userRoleId UserRole id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param userRoleUpdate  (required)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = UserAccountApi.PATH_UPDATE_USER_ROLE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UserRole> updateUserRole(
         @PathVariable("userAccountId") UUID userAccountId,
         @PathVariable("userRoleId") UUID userRoleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody UserRoleUpdate userRoleUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
