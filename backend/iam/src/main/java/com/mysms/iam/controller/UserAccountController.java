package com.mysms.iam.controller;

import com.mysms.contracts.iam.api.UserAccountApi;
import com.mysms.contracts.iam.model.AcceptTermsRequest;
import com.mysms.contracts.iam.model.ActivateUserRequest;
import com.mysms.contracts.iam.model.ApiTokenCreated;
import com.mysms.contracts.iam.model.AuthTokenList;
import com.mysms.contracts.iam.model.Availability;
import com.mysms.contracts.iam.model.ChangePasswordRequest;
import com.mysms.contracts.iam.model.ConfirmVerificationRequest;
import com.mysms.contracts.iam.model.CreateApiTokenRequest;
import com.mysms.contracts.iam.model.DeactivateUserRequest;
import com.mysms.contracts.iam.model.EffectiveSetting;
import com.mysms.contracts.iam.model.EffectiveSettingList;
import com.mysms.contracts.iam.model.ForcePasswordResetRequest;
import com.mysms.contracts.iam.model.ImpersonationToken;
import com.mysms.contracts.iam.model.ImportUsersRequest;
import com.mysms.contracts.iam.model.LoginRequest;
import com.mysms.contracts.iam.model.LoginResult;
import com.mysms.contracts.iam.model.LogoutRequest;
import com.mysms.contracts.iam.model.Me;
import com.mysms.contracts.iam.model.Operation;
import com.mysms.contracts.iam.model.RefreshTokenRequest;
import com.mysms.contracts.iam.model.RequestPasswordResetRequest;
import com.mysms.contracts.iam.model.RequestVerificationRequest;
import com.mysms.contracts.iam.model.ResetPasswordRequest;
import com.mysms.contracts.iam.model.SetMySettingRequest;
import com.mysms.contracts.iam.model.SetUserCredentialsRequest;
import com.mysms.contracts.iam.model.SetUserRolesRequest;
import com.mysms.contracts.iam.model.SignUpRequest;
import com.mysms.contracts.iam.model.StartImpersonationRequest;
import com.mysms.contracts.iam.model.SwitchRoleRequest;
import com.mysms.contracts.iam.model.SwitchRoleResult;
import com.mysms.contracts.iam.model.TokenPair;
import com.mysms.contracts.iam.model.UnlockUserRequest;
import com.mysms.contracts.iam.model.UserAccount;
import com.mysms.contracts.iam.model.UserAccountList;
import com.mysms.contracts.iam.model.UserAccountUpdate;
import com.mysms.contracts.iam.model.UserDepartmentAccess;
import com.mysms.contracts.iam.model.UserDepartmentAccessList;
import com.mysms.contracts.iam.model.UserDepartmentAccessUpdate;
import com.mysms.contracts.iam.model.UserDevice;
import com.mysms.contracts.iam.model.UserDeviceList;
import com.mysms.contracts.iam.model.UserDeviceUpdate;
import com.mysms.contracts.iam.model.UserPermission;
import com.mysms.contracts.iam.model.UserPermissionList;
import com.mysms.contracts.iam.model.UserPermissionUpdate;
import com.mysms.contracts.iam.model.UserPreference;
import com.mysms.contracts.iam.model.UserPreferenceList;
import com.mysms.contracts.iam.model.UserPreferenceUpdate;
import com.mysms.contracts.iam.model.UserRole;
import com.mysms.contracts.iam.model.UserRoleList;
import com.mysms.contracts.iam.model.UserRoleUpdate;
import com.mysms.contracts.iam.model.UserSession;
import com.mysms.contracts.iam.model.UserSessionList;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.ReadMask;
import com.mysms.iam.business.AccountSecurityService;
import com.mysms.iam.business.MeService;
import com.mysms.iam.business.MySettingsService;
import com.mysms.iam.business.SignInService;
import com.mysms.iam.business.UserAccountService;
import com.mysms.iam.business.UserDepartmentAccessService;
import com.mysms.iam.business.UserDeviceService;
import com.mysms.iam.business.UserPermissionService;
import com.mysms.iam.business.UserPreferenceService;
import com.mysms.iam.business.UserRoleService;
import com.mysms.iam.business.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the UserAccount operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/iam.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED. Standard operations follow the generated pattern (tools/backend/crud, merged by
 * merge_controllers.py); login, refreshToken, logout, getMe, listMySettings, setMySetting, resetMySetting,
 * changePassword, setUserCredentials, acceptTerms are hand-written.
 */
@RestController
public class UserAccountController implements UserAccountApi {

    private final SignInService signIn;
    private final AccountSecurityService accountSecurity;
    private final MeService me;
    private final MySettingsService mySettings;
    private final DtoValidator validator;
    private final HttpServletRequest request;
    private final UserAccountService userAccountService;
    private final UserDepartmentAccessService userDepartmentAccessService;
    private final UserDeviceService userDeviceService;
    private final UserPermissionService userPermissionService;
    private final UserPreferenceService userPreferenceService;
    private final UserRoleService userRoleService;
    private final UserSessionService userSessionService;

    public UserAccountController(SignInService signIn, AccountSecurityService accountSecurity, MeService me,
            MySettingsService mySettings, DtoValidator validator, HttpServletRequest request,
                    UserAccountService userAccountService,
            UserDepartmentAccessService userDepartmentAccessService, UserDeviceService userDeviceService,
            UserPermissionService userPermissionService, UserPreferenceService userPreferenceService,
            UserRoleService userRoleService, UserSessionService userSessionService) {
        this.signIn = signIn;
        this.accountSecurity = accountSecurity;
        this.me = me;
        this.mySettings = mySettings;
        this.validator = validator;
        this.request = request;
        this.userAccountService = userAccountService;
        this.userDepartmentAccessService = userDepartmentAccessService;
        this.userDeviceService = userDeviceService;
        this.userPermissionService = userPermissionService;
        this.userPreferenceService = userPreferenceService;
        this.userRoleService = userRoleService;
        this.userSessionService = userSessionService;
    }

    @Override
    public ResponseEntity<UserAccount> acceptTerms(AcceptTermsRequest acceptTermsRequest,
            UUID idempotencyKey) {
        UserAccount account = accountSecurity.acceptTerms(acceptTermsRequest);
        return ResponseEntity.ok().eTag(account.getEtag()).body(account);
    }

    @Override
    public ResponseEntity<UserAccount> activateUser(UUID userAccountId,
            ActivateUserRequest activateUserRequest, UUID idempotencyKey) {
        UserAccount changed = userAccountService.activateUser(userAccountId, activateUserRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> changePassword(ChangePasswordRequest changePasswordRequest,
            UUID idempotencyKey) {
        accountSecurity.changePassword(changePasswordRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Availability> checkUsernameAvailable(String username) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<UserAccount> confirmVerification(
            ConfirmVerificationRequest confirmVerificationRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ApiTokenCreated> createApiToken(UUID userAccountId,
            CreateApiTokenRequest createApiTokenRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<UserAccount> createUserAccount(UserAccount userAccount, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        UserAccount created = userAccountService.createUserAccount(userAccount, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/users/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<UserDepartmentAccess> createUserDepartmentAccess(UUID userAccountId,
            UserDepartmentAccess userDepartmentAccess, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        UserDepartmentAccess created =
                userDepartmentAccessService.createUserDepartmentAccess(userAccountId, userDepartmentAccess, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/users/" + userAccountId + "/departments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<UserDevice> createUserDevice(UUID userAccountId, UserDevice userDevice,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        UserDevice created = userDeviceService.createUserDevice(userAccountId, userDevice, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/users/" + userAccountId + "/devices/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<UserPermission> createUserPermission(UUID userAccountId,
            UserPermission userPermission, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        UserPermission created = userPermissionService.createUserPermission(userAccountId, userPermission, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/users/" + userAccountId + "/permissions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<UserPreference> createUserPreference(UUID userAccountId,
            UserPreference userPreference, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        UserPreference created = userPreferenceService.createUserPreference(userAccountId, userPreference, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/users/" + userAccountId + "/preferences/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<UserRole> createUserRole(UUID userAccountId, UserRole userRole,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        UserRole created = userRoleService.createUserRole(userAccountId, userRole, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/iam/users/" + userAccountId + "/roles/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<UserAccount> deactivateUser(UUID userAccountId,
            DeactivateUserRequest deactivateUserRequest, UUID idempotencyKey) {
        UserAccount changed = userAccountService.deactivateUser(userAccountId, deactivateUserRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> deleteAuthToken(UUID userAccountId, UUID authTokenId, String ifMatch) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Void> deleteUserAccount(UUID userAccountId, String ifMatch) {
        userAccountService.deleteUserAccount(userAccountId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserDepartmentAccess(UUID userAccountId,
            UUID userDepartmentAccessId, String ifMatch) {
        userDepartmentAccessService.deleteUserDepartmentAccess(userAccountId, userDepartmentAccessId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserDevice(UUID userAccountId, UUID userDeviceId, String ifMatch) {
        userDeviceService.deleteUserDevice(userAccountId, userDeviceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserPermission(UUID userAccountId, UUID userPermissionId,
            String ifMatch) {
        userPermissionService.deleteUserPermission(userAccountId, userPermissionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserPreference(UUID userAccountId, UUID userPreferenceId,
            String ifMatch) {
        userPreferenceService.deleteUserPreference(userAccountId, userPreferenceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserRole(UUID userAccountId, UUID userRoleId, String ifMatch) {
        userRoleService.deleteUserRole(userAccountId, userRoleId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserSession(UUID userAccountId, UUID userSessionId,
            String ifMatch) {
        userSessionService.deleteUserSession(userAccountId, userSessionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserAccount> forcePasswordReset(UUID userAccountId,
            ForcePasswordResetRequest forcePasswordResetRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Me> getMe() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(me.getMe());
    }

    @Override
    public ResponseEntity<UserAccount> getUserAccount(UUID userAccountId, String fields) {
        UserAccount found = userAccountService.getUserAccount(userAccountId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserDepartmentAccess> getUserDepartmentAccess(UUID userAccountId,
            UUID userDepartmentAccessId, String fields) {
        UserDepartmentAccess found =
                userDepartmentAccessService.getUserDepartmentAccess(userAccountId, userDepartmentAccessId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserDevice> getUserDevice(UUID userAccountId, UUID userDeviceId,
            String fields) {
        UserDevice found = userDeviceService.getUserDevice(userAccountId, userDeviceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserPermission> getUserPermission(UUID userAccountId, UUID userPermissionId,
            String fields) {
        UserPermission found = userPermissionService.getUserPermission(userAccountId, userPermissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserPreference> getUserPreference(UUID userAccountId, UUID userPreferenceId,
            String fields) {
        UserPreference found = userPreferenceService.getUserPreference(userAccountId, userPreferenceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserRole> getUserRole(UUID userAccountId, UUID userRoleId, String fields) {
        UserRole found = userRoleService.getUserRole(userAccountId, userRoleId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserSession> getUserSession(UUID userAccountId, UUID userSessionId,
            String fields) {
        UserSession found = userSessionService.getUserSession(userAccountId, userSessionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Operation> importUsers(ImportUsersRequest importUsersRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AuthTokenList> listAuthTokens(UUID userAccountId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<EffectiveSettingList> listMySettings() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mySettings.list());
    }

    @Override
    public ResponseEntity<UserAccountList> listUserAccounts(Integer pageSize, String pageToken,
            String filter, String orderBy, String fields) {
        UserAccountList list = userAccountService.listUserAccounts(pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<UserDepartmentAccessList> listUserDepartmentAccesses(UUID userAccountId,
            Integer pageSize, String pageToken, String filter, String orderBy, String fields) {
        UserDepartmentAccessList list =
                userDepartmentAccessService.listUserDepartmentAccesss(userAccountId, pageSize, pageToken, filter,
                        orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<UserDeviceList> listUserDevices(UUID userAccountId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        UserDeviceList list = userDeviceService.listUserDevices(userAccountId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<UserPermissionList> listUserPermissions(UUID userAccountId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        UserPermissionList list =
                userPermissionService.listUserPermissions(userAccountId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<UserPreferenceList> listUserPreferences(UUID userAccountId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        UserPreferenceList list =
                userPreferenceService.listUserPreferences(userAccountId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<UserRoleList> listUserRoles(UUID userAccountId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        UserRoleList list = userRoleService.listUserRoles(userAccountId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<UserSessionList> listUserSessions(UUID userAccountId, Integer pageSize,
            String pageToken, String filter, String orderBy, String fields) {
        UserSessionList list = userSessionService.listUserSessions(userAccountId, pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LoginResult> login(LoginRequest loginRequest, UUID idempotencyKey) {
        LoginResult result = signIn.login(loginRequest, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);
    }

    @Override
    public ResponseEntity<Void> logout(LogoutRequest logoutRequest, UUID idempotencyKey) {
        signIn.logout(Boolean.TRUE.equals(logoutRequest.getAllDevices()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TokenPair> refreshToken(RefreshTokenRequest refreshTokenRequest, UUID idempotencyKey) {
        validator.validate(refreshTokenRequest);
        TokenPair tokens = signIn.refresh(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens);
    }

    @Override
    public ResponseEntity<Void> requestPasswordReset(
            RequestPasswordResetRequest requestPasswordResetRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Void> requestVerification(RequestVerificationRequest requestVerificationRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Void> resetMySetting(String settingKey) {
        mySettings.reset(settingKey);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> resetPassword(ResetPasswordRequest resetPasswordRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<EffectiveSetting> setMySetting(String settingKey, SetMySettingRequest setMySettingRequest) {
        return ResponseEntity.ok(mySettings.set(settingKey, setMySettingRequest));
    }

    @Override
    public ResponseEntity<UserAccount> setUserCredentials(UUID userAccountId,
            SetUserCredentialsRequest setUserCredentialsRequest, UUID idempotencyKey) {
        UserAccount account = accountSecurity.setUserCredentials(userAccountId, setUserCredentialsRequest);
        return ResponseEntity.ok().eTag(account.getEtag()).body(account);
    }

    @Override
    public ResponseEntity<UserRoleList> setUserRoles(UUID userAccountId,
            SetUserRolesRequest setUserRolesRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<UserAccount> signUp(SignUpRequest signUpRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ImpersonationToken> startImpersonation(UUID userAccountId,
            StartImpersonationRequest startImpersonationRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Void> stopImpersonation(Object body, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SwitchRoleResult> switchRole(SwitchRoleRequest switchRoleRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<UserAccount> unlockUser(UUID userAccountId, UnlockUserRequest unlockUserRequest,
            UUID idempotencyKey) {
        UserAccount changed = userAccountService.unlockUser(userAccountId, unlockUserRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<UserAccount> updateUserAccount(UUID userAccountId, String ifMatch,
            UserAccountUpdate userAccountUpdate, String updateMask, Boolean validateOnly) {
        UserAccount updated =
                userAccountService.updateUserAccount(userAccountId, ifMatch, userAccountUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserDepartmentAccess> updateUserDepartmentAccess(UUID userAccountId,
            UUID userDepartmentAccessId, String ifMatch, UserDepartmentAccessUpdate userDepartmentAccessUpdate,
            String updateMask, Boolean validateOnly) {
        UserDepartmentAccess updated =
                userDepartmentAccessService.updateUserDepartmentAccess(userAccountId, userDepartmentAccessId, ifMatch,
                        userDepartmentAccessUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserDevice> updateUserDevice(UUID userAccountId, UUID userDeviceId,
            String ifMatch, UserDeviceUpdate userDeviceUpdate, String updateMask, Boolean validateOnly) {
        UserDevice updated =
                userDeviceService.updateUserDevice(userAccountId, userDeviceId, ifMatch, userDeviceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserPermission> updateUserPermission(UUID userAccountId, UUID userPermissionId,
            String ifMatch, UserPermissionUpdate userPermissionUpdate, String updateMask, Boolean validateOnly) {
        UserPermission updated =
                userPermissionService.updateUserPermission(userAccountId, userPermissionId, ifMatch,
                        userPermissionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserPreference> updateUserPreference(UUID userAccountId, UUID userPreferenceId,
            String ifMatch, UserPreferenceUpdate userPreferenceUpdate, String updateMask, Boolean validateOnly) {
        UserPreference updated =
                userPreferenceService.updateUserPreference(userAccountId, userPreferenceId, ifMatch,
                        userPreferenceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserRole> updateUserRole(UUID userAccountId, UUID userRoleId, String ifMatch,
            UserRoleUpdate userRoleUpdate, String updateMask, Boolean validateOnly) {
        UserRole updated =
                userRoleService.updateUserRole(userAccountId, userRoleId, ifMatch, userRoleUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
