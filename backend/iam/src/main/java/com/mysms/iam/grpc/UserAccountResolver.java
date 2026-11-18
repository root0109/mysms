package com.mysms.iam.grpc;

import com.google.protobuf.Empty;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.iam.api.model.ApiTokenCreated;
import com.mysms.iam.api.model.AuthToken;
import com.mysms.iam.api.model.Availability;
import com.mysms.iam.api.model.SignUpInstitutionList;
import com.mysms.iam.api.model.TenantCodeResolution;
import com.mysms.iam.api.model.EffectiveSetting;
import com.mysms.iam.api.model.EffectiveSettingList;
import com.mysms.iam.api.model.ImpersonationToken;
import com.mysms.iam.api.model.LoginResult;
import com.mysms.iam.api.model.Me;
import com.mysms.iam.api.model.MfaCredentialList;
import com.mysms.iam.api.model.MfaEnrollment;
import com.mysms.iam.api.model.Operation;
import com.mysms.iam.api.model.RecoveryCodes;
import com.mysms.iam.api.model.SwitchRoleResult;
import com.mysms.iam.api.model.TokenPair;
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
import com.mysms.iam.business.AccountProvisioningService;
import com.mysms.iam.business.AccountRecoveryService;
import com.mysms.iam.business.AccountSecurityService;
import com.mysms.iam.business.ApiTokenService;
import com.mysms.iam.business.ApiTokens;
import com.mysms.iam.business.ImpersonationService;
import com.mysms.iam.business.MeService;
import com.mysms.iam.business.MfaService;
import com.mysms.iam.business.MySettingsService;
import com.mysms.iam.business.RoleGrantService;
import com.mysms.iam.business.RoleSwitchService;
import com.mysms.iam.business.SignInService;
import com.mysms.iam.business.UserAccountService;
import com.mysms.iam.v1.AcceptTermsRequest;
import com.mysms.iam.v1.AcceptTermsResponse;
import com.mysms.iam.v1.ActivateUserRequest;
import com.mysms.iam.v1.ActivateUserResponse;
import com.mysms.iam.v1.ChangePasswordRequest;
import com.mysms.iam.v1.ChangePasswordResponse;
import com.mysms.iam.v1.CheckUsernameAvailableRequest;
import com.mysms.iam.v1.CheckUsernameAvailableResponse;
import com.mysms.iam.v1.ListSignUpInstitutionsRequest;
import com.mysms.iam.v1.ListSignUpInstitutionsResponse;
import com.mysms.iam.v1.ResolveTenantCodeRequest;
import com.mysms.iam.v1.ResolveTenantCodeResponse;
import com.mysms.iam.v1.CompleteMfaRequest;
import com.mysms.iam.v1.CompleteMfaResponse;
import com.mysms.iam.v1.ConfirmVerificationRequest;
import com.mysms.iam.v1.ConfirmVerificationResponse;
import com.mysms.iam.v1.CreateApiTokenRequest;
import com.mysms.iam.v1.CreateApiTokenResponse;
import com.mysms.iam.v1.CreateUserAccountRequest;
import com.mysms.iam.v1.CreateUserAccountResponse;
import com.mysms.iam.v1.CreateUserDepartmentAccessRequest;
import com.mysms.iam.v1.CreateUserDepartmentAccessResponse;
import com.mysms.iam.v1.CreateUserDeviceRequest;
import com.mysms.iam.v1.CreateUserDeviceResponse;
import com.mysms.iam.v1.CreateUserPermissionRequest;
import com.mysms.iam.v1.CreateUserPermissionResponse;
import com.mysms.iam.v1.CreateUserPreferenceRequest;
import com.mysms.iam.v1.CreateUserPreferenceResponse;
import com.mysms.iam.v1.CreateUserRoleRequest;
import com.mysms.iam.v1.CreateUserRoleResponse;
import com.mysms.iam.v1.DeactivateUserRequest;
import com.mysms.iam.v1.DeactivateUserResponse;
import com.mysms.iam.v1.DeleteAuthTokenRequest;
import com.mysms.iam.v1.DeleteAuthTokenResponse;
import com.mysms.iam.v1.DeleteUserAccountRequest;
import com.mysms.iam.v1.DeleteUserAccountResponse;
import com.mysms.iam.v1.DeleteUserDepartmentAccessRequest;
import com.mysms.iam.v1.DeleteUserDepartmentAccessResponse;
import com.mysms.iam.v1.DeleteUserDeviceRequest;
import com.mysms.iam.v1.DeleteUserDeviceResponse;
import com.mysms.iam.v1.DeleteUserPermissionRequest;
import com.mysms.iam.v1.DeleteUserPermissionResponse;
import com.mysms.iam.v1.DeleteUserPreferenceRequest;
import com.mysms.iam.v1.DeleteUserPreferenceResponse;
import com.mysms.iam.v1.DeleteUserRoleRequest;
import com.mysms.iam.v1.DeleteUserRoleResponse;
import com.mysms.iam.v1.DeleteUserSessionRequest;
import com.mysms.iam.v1.DeleteUserSessionResponse;
import com.mysms.iam.v1.DisableMfaRequest;
import com.mysms.iam.v1.DisableMfaResponse;
import com.mysms.iam.v1.EnrollMfaRequest;
import com.mysms.iam.v1.EnrollMfaResponse;
import com.mysms.iam.v1.ForcePasswordResetRequest;
import com.mysms.iam.v1.ForcePasswordResetResponse;
import com.mysms.iam.v1.GetMeRequest;
import com.mysms.iam.v1.GetMeResponse;
import com.mysms.iam.v1.GetUserAccountRequest;
import com.mysms.iam.v1.GetUserAccountResponse;
import com.mysms.iam.v1.GetUserDepartmentAccessRequest;
import com.mysms.iam.v1.GetUserDepartmentAccessResponse;
import com.mysms.iam.v1.GetUserDeviceRequest;
import com.mysms.iam.v1.GetUserDeviceResponse;
import com.mysms.iam.v1.GetUserPermissionRequest;
import com.mysms.iam.v1.GetUserPermissionResponse;
import com.mysms.iam.v1.GetUserPreferenceRequest;
import com.mysms.iam.v1.GetUserPreferenceResponse;
import com.mysms.iam.v1.GetUserRoleRequest;
import com.mysms.iam.v1.GetUserRoleResponse;
import com.mysms.iam.v1.GetUserSessionRequest;
import com.mysms.iam.v1.GetUserSessionResponse;
import com.mysms.iam.v1.ImportUsersRequest;
import com.mysms.iam.v1.ImportUsersResponse;
import com.mysms.iam.v1.ListAuthTokensRequest;
import com.mysms.iam.v1.ListAuthTokensResponse;
import com.mysms.iam.v1.ListMfaCredentialsRequest;
import com.mysms.iam.v1.ListMfaCredentialsResponse;
import com.mysms.iam.v1.ListMySettingsRequest;
import com.mysms.iam.v1.ListMySettingsResponse;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.ListUserDepartmentAccessesRequest;
import com.mysms.iam.v1.ListUserDepartmentAccessesResponse;
import com.mysms.iam.v1.ListUserDevicesRequest;
import com.mysms.iam.v1.ListUserDevicesResponse;
import com.mysms.iam.v1.ListUserPermissionsRequest;
import com.mysms.iam.v1.ListUserPermissionsResponse;
import com.mysms.iam.v1.ListUserPreferencesRequest;
import com.mysms.iam.v1.ListUserPreferencesResponse;
import com.mysms.iam.v1.ListUserRolesRequest;
import com.mysms.iam.v1.ListUserRolesResponse;
import com.mysms.iam.v1.ListUserSessionsRequest;
import com.mysms.iam.v1.ListUserSessionsResponse;
import com.mysms.iam.v1.LoginRequest;
import com.mysms.iam.v1.LoginResponse;
import com.mysms.iam.v1.LogoutRequest;
import com.mysms.iam.v1.LogoutResponse;
import com.mysms.iam.v1.RefreshTokenRequest;
import com.mysms.iam.v1.RefreshTokenResponse;
import com.mysms.iam.v1.RegenerateRecoveryCodesRequest;
import com.mysms.iam.v1.RegenerateRecoveryCodesResponse;
import com.mysms.iam.v1.RequestPasswordResetRequest;
import com.mysms.iam.v1.RequestPasswordResetResponse;
import com.mysms.iam.v1.RequestVerificationRequest;
import com.mysms.iam.v1.RequestVerificationResponse;
import com.mysms.iam.v1.ResetMySettingRequest;
import com.mysms.iam.v1.ResetPasswordRequest;
import com.mysms.iam.v1.ResetPasswordResponse;
import com.mysms.iam.v1.SetMySettingRequest;
import com.mysms.iam.v1.SetMySettingResponse;
import com.mysms.iam.v1.SetUserCredentialsRequest;
import com.mysms.iam.v1.SetUserCredentialsResponse;
import com.mysms.iam.v1.SetUserRolesRequest;
import com.mysms.iam.v1.SetUserRolesResponse;
import com.mysms.iam.v1.SignUpRequest;
import com.mysms.iam.v1.SignUpResponse;
import com.mysms.iam.v1.StartImpersonationRequest;
import com.mysms.iam.v1.StartImpersonationResponse;
import com.mysms.iam.v1.StopImpersonationRequest;
import com.mysms.iam.v1.StopImpersonationResponse;
import com.mysms.iam.v1.SwitchRoleRequest;
import com.mysms.iam.v1.SwitchRoleResponse;
import com.mysms.iam.v1.UnlockUserRequest;
import com.mysms.iam.v1.UnlockUserResponse;
import com.mysms.iam.v1.UpdateUserAccountRequest;
import com.mysms.iam.v1.UpdateUserAccountResponse;
import com.mysms.iam.v1.UpdateUserDepartmentAccessRequest;
import com.mysms.iam.v1.UpdateUserDepartmentAccessResponse;
import com.mysms.iam.v1.UpdateUserDeviceRequest;
import com.mysms.iam.v1.UpdateUserDeviceResponse;
import com.mysms.iam.v1.UpdateUserPermissionRequest;
import com.mysms.iam.v1.UpdateUserPermissionResponse;
import com.mysms.iam.v1.UpdateUserPreferenceRequest;
import com.mysms.iam.v1.UpdateUserPreferenceResponse;
import com.mysms.iam.v1.UpdateUserRoleRequest;
import com.mysms.iam.v1.UpdateUserRoleResponse;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.UserAccountService: every RPC is delegated to the business service REST's controller calls,
 * with the same arguments derived the same way. Authentication, permission, idempotency and error mapping happen at
 * the gRPC edge.
 *
 * <p>The client's address and User-Agent, which REST reads from the HTTP request, come from {@link ClientOrigin}. A
 * gRPC answer carries exactly the fields REST's body carries, once-shown secrets included, and nothing more.
 */
@GrpcService
public class UserAccountResolver extends UserAccountServiceGrpc.UserAccountServiceImplBase {

    private static final String USER_ACCOUNT_ID = "user_account_id";
    private static final String REQUEST_ID = "request_id";

    private final UserAccountService service;
    private final SignInService signIn;
    private final MfaService mfa;
    private final AccountSecurityService accountSecurity;
    private final AccountRecoveryService recovery;
    private final AccountProvisioningService provisioning;
    private final MeService me;
    private final MySettingsService mySettings;
    private final ApiTokenService apiTokens;
    private final ImpersonationService impersonation;
    private final RoleGrantService roleGrants;
    private final RoleSwitchService roleSwitch;
    private final ProtoBridge bridge;

    public UserAccountResolver(UserAccountService service, SignInService signIn, MfaService mfa,
            AccountSecurityService accountSecurity, AccountRecoveryService recovery,
            AccountProvisioningService provisioning, MeService me, MySettingsService mySettings,
            ApiTokenService apiTokens, ImpersonationService impersonation, RoleGrantService roleGrants,
            RoleSwitchService roleSwitch, ProtoBridge bridge) {
        this.service = service;
        this.signIn = signIn;
        this.mfa = mfa;
        this.accountSecurity = accountSecurity;
        this.recovery = recovery;
        this.provisioning = provisioning;
        this.me = me;
        this.mySettings = mySettings;
        this.apiTokens = apiTokens;
        this.impersonation = impersonation;
        this.roleGrants = roleGrants;
        this.roleSwitch = roleSwitch;
        this.bridge = bridge;
    }

    // ---- the caller's own settings and second factors

    @Override
    public void listMySettings(ListMySettingsRequest request, StreamObserver<ListMySettingsResponse> out) {
        EffectiveSettingList settings = mySettings.list();
        ProtoBridge.send(out, bridge.reply(ListMySettingsResponse.newBuilder(), "items", settings.getItems()).build());
    }

    /** The setting key is REST's path segment; the value is the body. */
    @Override
    public void setMySetting(SetMySettingRequest request, StreamObserver<SetMySettingResponse> out) {
        EffectiveSetting setting = mySettings.set(request.getSettingKey(),
                bridge.body(request, com.mysms.iam.api.model.SetMySettingRequest.class, "setting_key"));
        ProtoBridge.send(out, bridge.reply(SetMySettingResponse.newBuilder(), "setting", setting).build());
    }

    @Override
    public void resetMySetting(ResetMySettingRequest request, StreamObserver<Empty> out) {
        mySettings.reset(request.getSettingKey());
        ProtoBridge.send(out, Empty.getDefaultInstance());
    }

    @Override
    public void listMfaCredentials(ListMfaCredentialsRequest request,
            StreamObserver<ListMfaCredentialsResponse> out) {
        MfaCredentialList credentials = mfa.list();
        ProtoBridge.send(out, bridge.reply(ListMfaCredentialsResponse.newBuilder(), "items", credentials.getItems())
                .build());
    }

    @Override
    public void regenerateRecoveryCodes(RegenerateRecoveryCodesRequest request,
            StreamObserver<RegenerateRecoveryCodesResponse> out) {
        RecoveryCodes codes = mfa.regenerateRecoveryCodes(
                bridge.body(request, com.mysms.iam.api.model.RegenerateRecoveryCodesRequest.class, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(RegenerateRecoveryCodesResponse.newBuilder(), "recovery_codes",
                codes.getRecoveryCodes()).build());
    }

    /** The response message is REST's body itself, field by field. */
    @Override
    public void enrollMfa(EnrollMfaRequest request, StreamObserver<EnrollMfaResponse> out) {
        MfaEnrollment enrollment = mfa.enroll(
                bridge.body(request, com.mysms.iam.api.model.EnrollMfaRequest.class, REQUEST_ID));
        EnrollMfaResponse.Builder response = EnrollMfaResponse.newBuilder();
        bridge.reply(response, "mfa_credential_id", enrollment.getMfaCredentialId());
        bridge.reply(response, "secret", enrollment.getSecret());
        bridge.reply(response, "otpauth_url", enrollment.getOtpauthUrl());
        bridge.reply(response, "recovery_codes", enrollment.getRecoveryCodes());
        bridge.reply(response, "mfa_token", enrollment.getMfaToken());
        ProtoBridge.send(out, response.build());
    }

    @Override
    public void completeMfa(CompleteMfaRequest request, StreamObserver<CompleteMfaResponse> out) {
        LoginResult result = signIn.completeMfa(
                bridge.body(request, com.mysms.iam.api.model.CompleteMfaRequest.class, REQUEST_ID),
                ClientOrigin.address(), ClientOrigin.userAgent());
        ProtoBridge.send(out, bridge.reply(CompleteMfaResponse.newBuilder(), "result", result).build());
    }

    @Override
    public void disableMfa(DisableMfaRequest request, StreamObserver<DisableMfaResponse> out) {
        mfa.disable(bridge.body(request, com.mysms.iam.api.model.DisableMfaRequest.class, REQUEST_ID));
        ProtoBridge.send(out, DisableMfaResponse.getDefaultInstance());
    }

    // ---- the account: standard operations

    @Override
    public void listUserAccounts(ListUserAccountsRequest request, StreamObserver<ListUserAccountsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UserAccount> rows = service.listUserAccounts(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUserAccountsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUserAccount(GetUserAccountRequest request, StreamObserver<GetUserAccountResponse> out) {
        UserAccount found = service.getUserAccount(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUserAccountResponse.newBuilder(), "user_account", found).build());
    }

    @Override
    public void createUserAccount(CreateUserAccountRequest request, StreamObserver<CreateUserAccountResponse> out) {
        UserAccount created = service.createUserAccount(bridge.body(request.getUserAccount(), UserAccount.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateUserAccountResponse.newBuilder(), "user_account", created).build());
    }

    @Override
    public void updateUserAccount(UpdateUserAccountRequest request, StreamObserver<UpdateUserAccountResponse> out) {
        UserAccount updated = service.updateUserAccount(
                bridge.id(request.getUserAccount().getId(), "id"),
                bridge.requiredIfMatch(request.getUserAccount().getEtag()),
                bridge.changes(request.getUserAccount(), request.getUpdateMask(), UserAccountUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateUserAccountResponse.newBuilder(), "user_account", updated).build());
    }

    @Override
    public void deleteUserAccount(DeleteUserAccountRequest request, StreamObserver<DeleteUserAccountResponse> out) {
        service.deleteUserAccount(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUserAccountResponse.getDefaultInstance());
    }

    // ---- role grants

    @Override
    public void listUserRoles(ListUserRolesRequest request, StreamObserver<ListUserRolesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UserRole> rows = service.listUserRoles(userAccountId(request.getUserAccountId()), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUserRolesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUserRole(GetUserRoleRequest request, StreamObserver<GetUserRoleResponse> out) {
        UserRole found = service.getUserRole(userAccountId(request.getUserAccountId()),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUserRoleResponse.newBuilder(), "user_role", found).build());
    }

    @Override
    public void createUserRole(CreateUserRoleRequest request, StreamObserver<CreateUserRoleResponse> out) {
        UserRole created = service.createUserRole(userAccountId(request.getUserAccountId()),
                bridge.body(request.getUserRole(), UserRole.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateUserRoleResponse.newBuilder(), "user_role", created).build());
    }

    @Override
    public void updateUserRole(UpdateUserRoleRequest request, StreamObserver<UpdateUserRoleResponse> out) {
        UserRole updated = service.updateUserRole(
                userAccountId(request.getUserAccountId()),
                bridge.id(request.getUserRole().getId(), "id"),
                bridge.requiredIfMatch(request.getUserRole().getEtag()),
                bridge.changes(request.getUserRole(), request.getUpdateMask(), UserRoleUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateUserRoleResponse.newBuilder(), "user_role", updated).build());
    }

    @Override
    public void deleteUserRole(DeleteUserRoleRequest request, StreamObserver<DeleteUserRoleResponse> out) {
        service.deleteUserRole(userAccountId(request.getUserAccountId()), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUserRoleResponse.getDefaultInstance());
    }

    // ---- direct permissions

    @Override
    public void listUserPermissions(ListUserPermissionsRequest request,
            StreamObserver<ListUserPermissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UserPermission> rows = service.listUserPermissions(userAccountId(request.getUserAccountId()),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUserPermissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUserPermission(GetUserPermissionRequest request, StreamObserver<GetUserPermissionResponse> out) {
        UserPermission found = service.getUserPermission(userAccountId(request.getUserAccountId()),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUserPermissionResponse.newBuilder(), "user_permission", found).build());
    }

    @Override
    public void createUserPermission(CreateUserPermissionRequest request,
            StreamObserver<CreateUserPermissionResponse> out) {
        UserPermission created = service.createUserPermission(userAccountId(request.getUserAccountId()),
                bridge.body(request.getUserPermission(), UserPermission.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateUserPermissionResponse.newBuilder(), "user_permission", created)
                .build());
    }

    @Override
    public void updateUserPermission(UpdateUserPermissionRequest request,
            StreamObserver<UpdateUserPermissionResponse> out) {
        UserPermission updated = service.updateUserPermission(
                userAccountId(request.getUserAccountId()),
                bridge.id(request.getUserPermission().getId(), "id"),
                bridge.requiredIfMatch(request.getUserPermission().getEtag()),
                bridge.changes(request.getUserPermission(), request.getUpdateMask(), UserPermissionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateUserPermissionResponse.newBuilder(), "user_permission", updated)
                .build());
    }

    @Override
    public void deleteUserPermission(DeleteUserPermissionRequest request,
            StreamObserver<DeleteUserPermissionResponse> out) {
        service.deleteUserPermission(userAccountId(request.getUserAccountId()), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUserPermissionResponse.getDefaultInstance());
    }

    // ---- department access

    @Override
    public void listUserDepartmentAccesses(ListUserDepartmentAccessesRequest request,
            StreamObserver<ListUserDepartmentAccessesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UserDepartmentAccess> rows = service.listUserDepartmentAccesss(userAccountId(request.getUserAccountId()),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUserDepartmentAccessesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUserDepartmentAccess(GetUserDepartmentAccessRequest request,
            StreamObserver<GetUserDepartmentAccessResponse> out) {
        UserDepartmentAccess found = service.getUserDepartmentAccess(userAccountId(request.getUserAccountId()),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUserDepartmentAccessResponse.newBuilder(), "user_department_access",
                found).build());
    }

    @Override
    public void createUserDepartmentAccess(CreateUserDepartmentAccessRequest request,
            StreamObserver<CreateUserDepartmentAccessResponse> out) {
        UserDepartmentAccess created = service.createUserDepartmentAccess(userAccountId(request.getUserAccountId()),
                bridge.body(request.getUserDepartmentAccess(), UserDepartmentAccess.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateUserDepartmentAccessResponse.newBuilder(), "user_department_access",
                created).build());
    }

    @Override
    public void updateUserDepartmentAccess(UpdateUserDepartmentAccessRequest request,
            StreamObserver<UpdateUserDepartmentAccessResponse> out) {
        UserDepartmentAccess updated = service.updateUserDepartmentAccess(
                userAccountId(request.getUserAccountId()),
                bridge.id(request.getUserDepartmentAccess().getId(), "id"),
                bridge.requiredIfMatch(request.getUserDepartmentAccess().getEtag()),
                bridge.changes(request.getUserDepartmentAccess(), request.getUpdateMask(),
                        UserDepartmentAccessUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateUserDepartmentAccessResponse.newBuilder(), "user_department_access",
                updated).build());
    }

    @Override
    public void deleteUserDepartmentAccess(DeleteUserDepartmentAccessRequest request,
            StreamObserver<DeleteUserDepartmentAccessResponse> out) {
        service.deleteUserDepartmentAccess(userAccountId(request.getUserAccountId()), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUserDepartmentAccessResponse.getDefaultInstance());
    }

    // ---- API tokens

    @Override
    public void listAuthTokens(ListAuthTokensRequest request, StreamObserver<ListAuthTokensResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AuthToken> rows = apiTokens.listAuthTokens(userAccountId(request.getUserAccountId()), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAuthTokensResponse.newBuilder(), rows).build());
    }

    @Override
    public void deleteAuthToken(DeleteAuthTokenRequest request, StreamObserver<DeleteAuthTokenResponse> out) {
        apiTokens.deleteAuthToken(userAccountId(request.getUserAccountId()), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAuthTokenResponse.getDefaultInstance());
    }

    // ---- devices

    @Override
    public void listUserDevices(ListUserDevicesRequest request, StreamObserver<ListUserDevicesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UserDevice> rows = service.listUserDevices(userAccountId(request.getUserAccountId()), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUserDevicesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUserDevice(GetUserDeviceRequest request, StreamObserver<GetUserDeviceResponse> out) {
        UserDevice found = service.getUserDevice(userAccountId(request.getUserAccountId()),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUserDeviceResponse.newBuilder(), "user_device", found).build());
    }

    @Override
    public void createUserDevice(CreateUserDeviceRequest request, StreamObserver<CreateUserDeviceResponse> out) {
        UserDevice created = service.createUserDevice(userAccountId(request.getUserAccountId()),
                bridge.body(request.getUserDevice(), UserDevice.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateUserDeviceResponse.newBuilder(), "user_device", created).build());
    }

    @Override
    public void updateUserDevice(UpdateUserDeviceRequest request, StreamObserver<UpdateUserDeviceResponse> out) {
        UserDevice updated = service.updateUserDevice(
                userAccountId(request.getUserAccountId()),
                bridge.id(request.getUserDevice().getId(), "id"),
                bridge.requiredIfMatch(request.getUserDevice().getEtag()),
                bridge.changes(request.getUserDevice(), request.getUpdateMask(), UserDeviceUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateUserDeviceResponse.newBuilder(), "user_device", updated).build());
    }

    @Override
    public void deleteUserDevice(DeleteUserDeviceRequest request, StreamObserver<DeleteUserDeviceResponse> out) {
        service.deleteUserDevice(userAccountId(request.getUserAccountId()), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUserDeviceResponse.getDefaultInstance());
    }

    // ---- preferences

    @Override
    public void listUserPreferences(ListUserPreferencesRequest request,
            StreamObserver<ListUserPreferencesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UserPreference> rows = service.listUserPreferences(userAccountId(request.getUserAccountId()),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUserPreferencesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUserPreference(GetUserPreferenceRequest request, StreamObserver<GetUserPreferenceResponse> out) {
        UserPreference found = service.getUserPreference(userAccountId(request.getUserAccountId()),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUserPreferenceResponse.newBuilder(), "user_preference", found).build());
    }

    @Override
    public void createUserPreference(CreateUserPreferenceRequest request,
            StreamObserver<CreateUserPreferenceResponse> out) {
        UserPreference created = service.createUserPreference(userAccountId(request.getUserAccountId()),
                bridge.body(request.getUserPreference(), UserPreference.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateUserPreferenceResponse.newBuilder(), "user_preference", created)
                .build());
    }

    @Override
    public void updateUserPreference(UpdateUserPreferenceRequest request,
            StreamObserver<UpdateUserPreferenceResponse> out) {
        UserPreference updated = service.updateUserPreference(
                userAccountId(request.getUserAccountId()),
                bridge.id(request.getUserPreference().getId(), "id"),
                bridge.requiredIfMatch(request.getUserPreference().getEtag()),
                bridge.changes(request.getUserPreference(), request.getUpdateMask(), UserPreferenceUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateUserPreferenceResponse.newBuilder(), "user_preference", updated)
                .build());
    }

    @Override
    public void deleteUserPreference(DeleteUserPreferenceRequest request,
            StreamObserver<DeleteUserPreferenceResponse> out) {
        service.deleteUserPreference(userAccountId(request.getUserAccountId()), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUserPreferenceResponse.getDefaultInstance());
    }

    // ---- sessions

    @Override
    public void listUserSessions(ListUserSessionsRequest request, StreamObserver<ListUserSessionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UserSession> rows = service.listUserSessions(userAccountId(request.getUserAccountId()), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUserSessionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUserSession(GetUserSessionRequest request, StreamObserver<GetUserSessionResponse> out) {
        UserSession found = service.getUserSession(userAccountId(request.getUserAccountId()),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUserSessionResponse.newBuilder(), "user_session", found).build());
    }

    @Override
    public void deleteUserSession(DeleteUserSessionRequest request, StreamObserver<DeleteUserSessionResponse> out) {
        service.deleteUserSession(userAccountId(request.getUserAccountId()), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUserSessionResponse.getDefaultInstance());
    }

    // ---- signing in and out, recovery, the caller

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> out) {
        LoginResult result = signIn.login(bridge.body(request, com.mysms.iam.api.model.LoginRequest.class, REQUEST_ID),
                ClientOrigin.address(), ClientOrigin.userAgent());
        ProtoBridge.send(out, bridge.reply(LoginResponse.newBuilder(), "result", result).build());
    }

    /** A personal API token is exchanged for an access token, a refresh token rotated — the controller's dispatch. */
    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> out) {
        String presented = bridge.body(request, com.mysms.iam.api.model.RefreshTokenRequest.class, REQUEST_ID)
                .getRefreshToken();
        TokenPair tokens = ApiTokens.recognises(presented) ? apiTokens.exchange(presented) : signIn.refresh(presented);
        ProtoBridge.send(out, bridge.reply(RefreshTokenResponse.newBuilder(), "result", tokens).build());
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> out) {
        com.mysms.iam.api.model.LogoutRequest body =
                bridge.body(request, com.mysms.iam.api.model.LogoutRequest.class, REQUEST_ID);
        signIn.logout(Boolean.TRUE.equals(body.getAllDevices()));
        ProtoBridge.send(out, LogoutResponse.getDefaultInstance());
    }

    @Override
    public void signUp(SignUpRequest request, StreamObserver<SignUpResponse> out) {
        UserAccount account = provisioning.signUp(
                bridge.body(request, com.mysms.iam.api.model.SignUpRequest.class, REQUEST_ID), ClientOrigin.address());
        ProtoBridge.send(out, bridge.reply(SignUpResponse.newBuilder(), "user_account", account).build());
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetRequest request,
            StreamObserver<RequestPasswordResetResponse> out) {
        recovery.requestPasswordReset(
                bridge.body(request, com.mysms.iam.api.model.RequestPasswordResetRequest.class, REQUEST_ID),
                ClientOrigin.address());
        ProtoBridge.send(out, RequestPasswordResetResponse.getDefaultInstance());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request, StreamObserver<ResetPasswordResponse> out) {
        recovery.resetPassword(bridge.body(request, com.mysms.iam.api.model.ResetPasswordRequest.class, REQUEST_ID));
        ProtoBridge.send(out, ResetPasswordResponse.getDefaultInstance());
    }

    @Override
    public void changePassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> out) {
        accountSecurity.changePassword(
                bridge.body(request, com.mysms.iam.api.model.ChangePasswordRequest.class, REQUEST_ID));
        ProtoBridge.send(out, ChangePasswordResponse.getDefaultInstance());
    }

    @Override
    public void requestVerification(RequestVerificationRequest request,
            StreamObserver<RequestVerificationResponse> out) {
        recovery.requestVerification(
                bridge.body(request, com.mysms.iam.api.model.RequestVerificationRequest.class, REQUEST_ID));
        ProtoBridge.send(out, RequestVerificationResponse.getDefaultInstance());
    }

    @Override
    public void confirmVerification(ConfirmVerificationRequest request,
            StreamObserver<ConfirmVerificationResponse> out) {
        UserAccount account = recovery.confirmVerification(
                bridge.body(request, com.mysms.iam.api.model.ConfirmVerificationRequest.class, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(ConfirmVerificationResponse.newBuilder(), "user_account", account).build());
    }

    @Override
    public void getMe(GetMeRequest request, StreamObserver<GetMeResponse> out) {
        Me caller = me.getMe();
        ProtoBridge.send(out, bridge.reply(GetMeResponse.newBuilder(), "result", caller).build());
    }

    // ---- impersonation

    @Override
    public void startImpersonation(StartImpersonationRequest request,
            StreamObserver<StartImpersonationResponse> out) {
        ImpersonationToken token = impersonation.startImpersonation(userAccountId(request.getUserAccountId()),
                bridge.body(request, com.mysms.iam.api.model.StartImpersonationRequest.class, USER_ACCOUNT_ID,
                        REQUEST_ID), ClientOrigin.address(), ClientOrigin.userAgent());
        ProtoBridge.send(out, bridge.reply(StartImpersonationResponse.newBuilder(), "result", token).build());
    }

    /** REST's body is an empty object; the request carries only the idempotency key. */
    @Override
    public void stopImpersonation(StopImpersonationRequest request, StreamObserver<StopImpersonationResponse> out) {
        impersonation.stopImpersonation();
        ProtoBridge.send(out, StopImpersonationResponse.getDefaultInstance());
    }

    // ---- account administration

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void activateUser(ActivateUserRequest request, StreamObserver<ActivateUserResponse> out) {
        UserAccount changed = service.activateUser(userAccountId(request.getUserAccountId()),
                bridge.body(request, com.mysms.iam.api.model.ActivateUserRequest.class, USER_ACCOUNT_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(ActivateUserResponse.newBuilder(), "user_account", changed).build());
    }

    /** The etag (optional) and the reason are the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void deactivateUser(DeactivateUserRequest request, StreamObserver<DeactivateUserResponse> out) {
        UserAccount changed = service.deactivateUser(userAccountId(request.getUserAccountId()),
                bridge.body(request, com.mysms.iam.api.model.DeactivateUserRequest.class, USER_ACCOUNT_ID,
                        REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(DeactivateUserResponse.newBuilder(), "user_account", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void unlockUser(UnlockUserRequest request, StreamObserver<UnlockUserResponse> out) {
        UserAccount changed = service.unlockUser(userAccountId(request.getUserAccountId()),
                bridge.body(request, com.mysms.iam.api.model.UnlockUserRequest.class, USER_ACCOUNT_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(UnlockUserResponse.newBuilder(), "user_account", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void forcePasswordReset(ForcePasswordResetRequest request,
            StreamObserver<ForcePasswordResetResponse> out) {
        UserAccount changed = recovery.forcePasswordReset(userAccountId(request.getUserAccountId()),
                bridge.body(request, com.mysms.iam.api.model.ForcePasswordResetRequest.class, USER_ACCOUNT_ID,
                        REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(ForcePasswordResetResponse.newBuilder(), "user_account", changed).build());
    }

    /** The token's secret is in the answer once, as on REST; only its hash is stored. */
    @Override
    public void createApiToken(CreateApiTokenRequest request, StreamObserver<CreateApiTokenResponse> out) {
        ApiTokenCreated created = apiTokens.createApiToken(userAccountId(request.getUserAccountId()),
                bridge.body(request, com.mysms.iam.api.model.CreateApiTokenRequest.class, USER_ACCOUNT_ID,
                        REQUEST_ID), ClientOrigin.address());
        ProtoBridge.send(out, bridge.reply(CreateApiTokenResponse.newBuilder(), "result", created).build());
    }

    @Override
    public void setUserCredentials(SetUserCredentialsRequest request,
            StreamObserver<SetUserCredentialsResponse> out) {
        UserAccount account = accountSecurity.setUserCredentials(userAccountId(request.getUserAccountId()),
                bridge.body(request, com.mysms.iam.api.model.SetUserCredentialsRequest.class, USER_ACCOUNT_ID,
                        REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(SetUserCredentialsResponse.newBuilder(), "user_account", account).build());
    }

    /**
     * Every grant the user holds afterwards comes back in one response — it is not a page. A request without grants is
     * REST's {@code "grants": []} (take every grant away): the bridge reads an empty required list as empty.
     */
    @Override
    public void setUserRoles(SetUserRolesRequest request, StreamObserver<SetUserRolesResponse> out) {
        com.mysms.iam.api.model.SetUserRolesRequest wanted =
                bridge.body(request, com.mysms.iam.api.model.SetUserRolesRequest.class, USER_ACCOUNT_ID, REQUEST_ID);
        UserRoleList grants = roleGrants.setUserRoles(userAccountId(request.getUserAccountId()), wanted);
        ProtoBridge.send(out, bridge.reply(SetUserRolesResponse.newBuilder(), "items", grants.getItems()).build());
    }

    @Override
    public void switchRole(SwitchRoleRequest request, StreamObserver<SwitchRoleResponse> out) {
        SwitchRoleResult switched = roleSwitch.switchRole(
                bridge.body(request, com.mysms.iam.api.model.SwitchRoleRequest.class, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(SwitchRoleResponse.newBuilder(), "result", switched).build());
    }

    @Override
    public void acceptTerms(AcceptTermsRequest request, StreamObserver<AcceptTermsResponse> out) {
        UserAccount account = accountSecurity.acceptTerms(
                bridge.body(request, com.mysms.iam.api.model.AcceptTermsRequest.class, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(AcceptTermsResponse.newBuilder(), "user_account", account).build());
    }

    /** REST's required query parameter: not sent (proto3's empty string), it is 400 before the service runs. */
    @Override
    public void checkUsernameAvailable(CheckUsernameAvailableRequest request,
            StreamObserver<CheckUsernameAvailableResponse> out) {
        String username = bridge.orNull(request.getUsername());
        if (username == null) {
            throw new InvalidArgumentException("Missing parameter username.");
        }
        Availability availability = provisioning.checkUsernameAvailable(username);
        ProtoBridge.send(out, bridge.reply(CheckUsernameAvailableResponse.newBuilder(), "result", availability)
                .build());
    }

    @Override
    public void resolveTenantCode(ResolveTenantCodeRequest request,
            StreamObserver<ResolveTenantCodeResponse> out) {
        TenantCodeResolution resolved = signIn.resolveTenantCode(request.getTenantCode());
        ProtoBridge.send(out, bridge.reply(ResolveTenantCodeResponse.newBuilder(), "result", resolved).build());
    }

    @Override
    public void listSignUpInstitutions(ListSignUpInstitutionsRequest request,
            StreamObserver<ListSignUpInstitutionsResponse> out) {
        SignUpInstitutionList schools = provisioning.listSignUpInstitutions(request.getTenantCode());
        ProtoBridge.send(out, bridge.reply(ListSignUpInstitutionsResponse.newBuilder(), "items", schools.getItems())
                .build());
    }

    /** REST's 202: the started operation in {@code result}. The rows travel inline, as in REST's body. */
    @Override
    public void importUsers(ImportUsersRequest request, StreamObserver<ImportUsersResponse> out) {
        Operation started = provisioning.importUsers(
                bridge.body(request, com.mysms.iam.api.model.ImportUsersRequest.class, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(ImportUsersResponse.newBuilder(), "result", started).build());
    }

    /** The account a nested RPC addresses: REST's {@code userAccountId} path parameter. */
    private UUID userAccountId(String value) {
        return bridge.id(value, "userAccountId");
    }
}
