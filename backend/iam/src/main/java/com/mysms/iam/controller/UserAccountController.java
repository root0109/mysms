package com.mysms.iam.controller;

import com.mysms.iam.api.model.AuthToken;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.iam.api.UserAccountApi;
import com.mysms.iam.api.model.AcceptTermsRequest;
import com.mysms.iam.api.model.ActivateUserRequest;
import com.mysms.iam.api.model.ApiTokenCreated;
import com.mysms.iam.api.model.Availability;
import com.mysms.iam.api.model.TenantCodeResolution;
import com.mysms.iam.api.model.ChangePasswordRequest;
import com.mysms.iam.api.model.ConfirmVerificationRequest;
import com.mysms.iam.api.model.CreateApiTokenRequest;
import com.mysms.iam.api.model.DeactivateUserRequest;
import com.mysms.iam.api.model.EffectiveSetting;
import com.mysms.iam.api.model.EffectiveSettingList;
import com.mysms.iam.api.model.ForcePasswordResetRequest;
import com.mysms.iam.api.model.ImpersonationToken;
import com.mysms.iam.api.model.ImportUsersRequest;
import com.mysms.iam.api.model.LoginRequest;
import com.mysms.iam.api.model.LoginResult;
import com.mysms.iam.api.model.LogoutRequest;
import com.mysms.iam.api.model.Me;
import com.mysms.iam.api.model.Operation;
import com.mysms.iam.api.model.RefreshTokenRequest;
import com.mysms.iam.api.model.RequestPasswordResetRequest;
import com.mysms.iam.api.model.RequestVerificationRequest;
import com.mysms.iam.api.model.ResetPasswordRequest;
import com.mysms.iam.api.model.SetMySettingRequest;
import com.mysms.iam.api.model.SetUserCredentialsRequest;
import com.mysms.iam.api.model.SetUserRolesRequest;
import com.mysms.iam.api.model.SignUpInstitutionList;
import com.mysms.iam.api.model.SignUpRequest;
import com.mysms.iam.api.model.StartImpersonationRequest;
import com.mysms.iam.api.model.SwitchRoleRequest;
import com.mysms.iam.api.model.SwitchRoleResult;
import com.mysms.iam.api.model.TokenPair;
import com.mysms.iam.api.model.UnlockUserRequest;
import com.mysms.iam.api.model.UserAccount;
import com.mysms.iam.api.model.UserAccountUpdate;
import com.mysms.iam.api.model.UserDepartmentAccess;
import com.mysms.iam.api.model.UserDepartmentAccessUpdate;
import com.mysms.iam.api.model.UserDevice;
import com.mysms.iam.api.model.UserDeviceUpdate;
import com.mysms.iam.api.model.UserPermission;
import com.mysms.iam.api.model.UserPermissionUpdate;
import com.mysms.iam.api.model.UserPreference;
import com.mysms.iam.api.model.UserPreferenceUpdate;
import com.mysms.iam.api.model.UserRole;
import com.mysms.iam.api.model.UserRoleList;
import com.mysms.iam.api.model.UserRoleUpdate;
import com.mysms.iam.api.model.UserSession;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.ReadMask;
import com.mysms.iam.business.AccountProvisioningService;
import com.mysms.iam.business.AccountRecoveryService;
import com.mysms.iam.business.AccountSecurityService;
import com.mysms.iam.business.ApiTokenService;
import com.mysms.iam.business.ApiTokens;
import com.mysms.iam.business.ImpersonationService;
import com.mysms.iam.business.RoleGrantService;
import com.mysms.iam.business.RoleSwitchService;
import com.mysms.iam.business.MeService;
import com.mysms.iam.business.MySettingsService;
import com.mysms.iam.business.SignInService;
import com.mysms.iam.business.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the UserAccount operations; authentication and the permission check already ran in the auth-client
 * filter. Sign-in, refresh, the caller's own account, credentials, mailed secrets, sign-up and import, role grants
 * and switch, impersonation and API tokens are served by their own services.
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
    private final AccountRecoveryService recovery;
    private final AccountProvisioningService provisioning;
    private final ApiTokenService apiTokens;
    private final ImpersonationService impersonation;
    private final RoleGrantService roleGrants;
    private final RoleSwitchService roleSwitch;

    public UserAccountController(SignInService signIn,
            AccountSecurityService accountSecurity,
            MeService me,
            MySettingsService mySettings,
            DtoValidator validator,
            HttpServletRequest request,
            UserAccountService userAccountService,
            AccountRecoveryService recovery,
            AccountProvisioningService provisioning,
            ApiTokenService apiTokens,
            ImpersonationService impersonation,
            RoleGrantService roleGrants,
            RoleSwitchService roleSwitch) {
        this.signIn = signIn;
        this.accountSecurity = accountSecurity;
        this.me = me;
        this.mySettings = mySettings;
        this.validator = validator;
        this.request = request;
        this.userAccountService = userAccountService;
        this.recovery = recovery;
        this.provisioning = provisioning;
        this.apiTokens = apiTokens;
        this.impersonation = impersonation;
        this.roleGrants = roleGrants;
        this.roleSwitch = roleSwitch;
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
        return ResponseEntity.ok(provisioning.checkUsernameAvailable(username));
    }

    @Override
    public ResponseEntity<TenantCodeResolution> resolveTenantCode(String tenantCode) {
        return ResponseEntity.ok(signIn.resolveTenantCode(tenantCode));
    }

    @Override
    public ResponseEntity<SignUpInstitutionList> listSignUpInstitutions(String tenantCode) {
        return ResponseEntity.ok(provisioning.listSignUpInstitutions(tenantCode));
    }

    @Override
    public ResponseEntity<UserAccount> confirmVerification(
            ConfirmVerificationRequest confirmVerificationRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(recovery.confirmVerification(confirmVerificationRequest));
    }

    @Override
    public ResponseEntity<ApiTokenCreated> createApiToken(UUID userAccountId,
            CreateApiTokenRequest createApiTokenRequest, UUID idempotencyKey) {
        ApiTokenCreated created = apiTokens.createApiToken(userAccountId, createApiTokenRequest, request.getRemoteAddr());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(created);
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
                userAccountService.createUserDepartmentAccess(userAccountId, userDepartmentAccess, dryRun);
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
        UserDevice created = userAccountService.createUserDevice(userAccountId, userDevice, dryRun);
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
        UserPermission created = userAccountService.createUserPermission(userAccountId, userPermission, dryRun);
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
        UserPreference created = userAccountService.createUserPreference(userAccountId, userPreference, dryRun);
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
        UserRole created = userAccountService.createUserRole(userAccountId, userRole, dryRun);
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
        apiTokens.deleteAuthToken(userAccountId, authTokenId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserAccount(UUID userAccountId, String ifMatch) {
        userAccountService.deleteUserAccount(userAccountId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserDepartmentAccess(UUID userAccountId,
            UUID userDepartmentAccessId, String ifMatch) {
        userAccountService.deleteUserDepartmentAccess(userAccountId, userDepartmentAccessId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserDevice(UUID userAccountId, UUID userDeviceId, String ifMatch) {
        userAccountService.deleteUserDevice(userAccountId, userDeviceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserPermission(UUID userAccountId, UUID userPermissionId,
            String ifMatch) {
        userAccountService.deleteUserPermission(userAccountId, userPermissionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserPreference(UUID userAccountId, UUID userPreferenceId,
            String ifMatch) {
        userAccountService.deleteUserPreference(userAccountId, userPreferenceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserRole(UUID userAccountId, UUID userRoleId, String ifMatch) {
        userAccountService.deleteUserRole(userAccountId, userRoleId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteUserSession(UUID userAccountId, UUID userSessionId,
            String ifMatch) {
        userAccountService.deleteUserSession(userAccountId, userSessionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserAccount> forcePasswordReset(UUID userAccountId,
            ForcePasswordResetRequest forcePasswordResetRequest, UUID idempotencyKey) {
        UserAccount changed = recovery.forcePasswordReset(userAccountId, forcePasswordResetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
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
                userAccountService.getUserDepartmentAccess(userAccountId, userDepartmentAccessId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserDevice> getUserDevice(UUID userAccountId, UUID userDeviceId,
            String fields) {
        UserDevice found = userAccountService.getUserDevice(userAccountId, userDeviceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserPermission> getUserPermission(UUID userAccountId, UUID userPermissionId,
            String fields) {
        UserPermission found = userAccountService.getUserPermission(userAccountId, userPermissionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserPreference> getUserPreference(UUID userAccountId, UUID userPreferenceId,
            String fields) {
        UserPreference found = userAccountService.getUserPreference(userAccountId, userPreferenceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserRole> getUserRole(UUID userAccountId, UUID userRoleId, String fields) {
        UserRole found = userAccountService.getUserRole(userAccountId, userRoleId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<UserSession> getUserSession(UUID userAccountId, UUID userSessionId,
            String fields) {
        UserSession found = userAccountService.getUserSession(userAccountId, userSessionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Operation> importUsers(ImportUsersRequest importUsersRequest,
            UUID idempotencyKey) {
        return ResponseEntity.accepted().body(provisioning.importUsers(importUsersRequest));
    }

    @Override
    public ResponseEntity<PagingInfo<AuthToken>> listAuthTokens(UUID userAccountId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AuthToken> list = PagingUtil.pagingInfo(apiTokens.listAuthTokens(userAccountId, page, size, cursor,
                filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<EffectiveSettingList> listMySettings() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mySettings.list());
    }

    @Override
    public ResponseEntity<PagingInfo<UserAccount>> listUserAccounts(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<UserAccount> list =
                PagingUtil.pagingInfo(userAccountService.listUserAccounts(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<UserDepartmentAccess>> listUserDepartmentAccesses(UUID userAccountId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<UserDepartmentAccess> list =
                PagingUtil.pagingInfo(userAccountService.listUserDepartmentAccesss(userAccountId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<UserDevice>> listUserDevices(UUID userAccountId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<UserDevice> list =
                PagingUtil.pagingInfo(userAccountService.listUserDevices(userAccountId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<UserPermission>> listUserPermissions(UUID userAccountId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<UserPermission> list =
                PagingUtil.pagingInfo(userAccountService.listUserPermissions(userAccountId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<UserPreference>> listUserPreferences(UUID userAccountId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<UserPreference> list =
                PagingUtil.pagingInfo(userAccountService.listUserPreferences(userAccountId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<UserRole>> listUserRoles(UUID userAccountId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<UserRole> list =
                PagingUtil.pagingInfo(userAccountService.listUserRoles(userAccountId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<UserSession>> listUserSessions(UUID userAccountId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<UserSession> list =
                PagingUtil.pagingInfo(userAccountService.listUserSessions(userAccountId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
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
        String presented = refreshTokenRequest.getRefreshToken();
        TokenPair tokens = ApiTokens.recognises(presented) ? apiTokens.exchange(presented) : signIn.refresh(presented);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens);
    }

    @Override
    public ResponseEntity<Void> requestPasswordReset(
            RequestPasswordResetRequest requestPasswordResetRequest, UUID idempotencyKey) {
        recovery.requestPasswordReset(requestPasswordResetRequest, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> requestVerification(RequestVerificationRequest requestVerificationRequest,
            UUID idempotencyKey) {
        recovery.requestVerification(requestVerificationRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> resetMySetting(String settingKey) {
        mySettings.reset(settingKey);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> resetPassword(ResetPasswordRequest resetPasswordRequest,
            UUID idempotencyKey) {
        recovery.resetPassword(resetPasswordRequest);
        return ResponseEntity.noContent().build();
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
        return ResponseEntity.ok(roleGrants.setUserRoles(userAccountId, setUserRolesRequest));
    }

    @Override
    public ResponseEntity<UserAccount> signUp(SignUpRequest signUpRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(provisioning.signUp(signUpRequest, request.getRemoteAddr()));
    }

    @Override
    public ResponseEntity<ImpersonationToken> startImpersonation(UUID userAccountId,
            StartImpersonationRequest startImpersonationRequest, UUID idempotencyKey) {
        ImpersonationToken token = impersonation.startImpersonation(userAccountId, startImpersonationRequest,
                request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(token);
    }

    @Override
    public ResponseEntity<Void> stopImpersonation(Object body, UUID idempotencyKey) {
        impersonation.stopImpersonation();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SwitchRoleResult> switchRole(SwitchRoleRequest switchRoleRequest,
            UUID idempotencyKey) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(roleSwitch.switchRole(switchRoleRequest));
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
                userAccountService.updateUserDepartmentAccess(userAccountId, userDepartmentAccessId, ifMatch,
                        userDepartmentAccessUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserDevice> updateUserDevice(UUID userAccountId, UUID userDeviceId,
            String ifMatch, UserDeviceUpdate userDeviceUpdate, String updateMask, Boolean validateOnly) {
        UserDevice updated =
                userAccountService.updateUserDevice(userAccountId, userDeviceId, ifMatch, userDeviceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserPermission> updateUserPermission(UUID userAccountId, UUID userPermissionId,
            String ifMatch, UserPermissionUpdate userPermissionUpdate, String updateMask, Boolean validateOnly) {
        UserPermission updated =
                userAccountService.updateUserPermission(userAccountId, userPermissionId, ifMatch,
                        userPermissionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserPreference> updateUserPreference(UUID userAccountId, UUID userPreferenceId,
            String ifMatch, UserPreferenceUpdate userPreferenceUpdate, String updateMask, Boolean validateOnly) {
        UserPreference updated =
                userAccountService.updateUserPreference(userAccountId, userPreferenceId, ifMatch,
                        userPreferenceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<UserRole> updateUserRole(UUID userAccountId, UUID userRoleId, String ifMatch,
            UserRoleUpdate userRoleUpdate, String updateMask, Boolean validateOnly) {
        UserRole updated =
                userAccountService.updateUserRole(userAccountId, userRoleId, ifMatch, userRoleUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
