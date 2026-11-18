import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { iam } from '@mysms/sdk';
import { AuthApi } from '@mysms/sdk/iam/apis/AuthApi';
import { UserAccountApi } from '@mysms/sdk/iam/apis/UserAccountApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { useSession } from './session';

export type MfaCredential = iam.MfaCredential;
export type MfaEnrollment = iam.MfaEnrollment;
export type UserSession = iam.UserSession;

const MFA = queryKey('iam', 'me', { part: 'mfa' });

/** ACC-08: the caller's second factors (iam.ListMfaCredentials); secrets are never returned. */
export function useMfaCredentials() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: MFA,
    queryFn: () => call(api.get(AuthApi).listMfaCredentials()),
    staleTime: STALE_TIME.detail,
  });
}

/** iam.EnrollMfa for an authenticator app: returns the secret, its otpauth URL and the recovery codes once. */
export function useEnrollMfa() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (label: string) =>
      call(
        api
          .get(AuthApi)
          .enrollMfa({ enrollMfaRequest: { credentialType: 'totp', label: label || undefined } }),
      ),
  });
}

/**
 * Verifies a new enrollment with its first code (iam.CompleteMfa with the enrollment's mfaToken). The factor is
 * usable from then on; the current session stays as it is (a verification never signs anyone in).
 */
export function useVerifyMfaEnrollment() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ mfaToken, code }: { mfaToken: string; code: string }) =>
      call(api.get(AuthApi).completeMfa({ completeMfaRequest: { mfaToken, code } })),
    onSuccess: () => void qc.invalidateQueries({ queryKey: MFA }),
  });
}

/** iam.DisableMfa: needs a current code (or a recovery code) to prove possession. */
export function useDisableMfa() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ mfaCredentialId, code }: { mfaCredentialId: string; code: string }) =>
      call(api.get(AuthApi).disableMfa({ disableMfaRequest: { mfaCredentialId, code } })),
    onSuccess: () => void qc.invalidateQueries({ queryKey: MFA }),
  });
}

/** iam.RegenerateRecoveryCodes: the previous codes stop working; the new ones are shown once. */
export function useRegenerateRecoveryCodes() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (code: string) =>
      call(api.get(AuthApi).regenerateRecoveryCodes({ regenerateRecoveryCodesRequest: { code } })),
  });
}

/** iam.ChangePassword: the current password is required; the server signs out the caller's other sessions. */
export function useChangePassword() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { currentPassword: string; newPassword: string }) =>
      call(api.get(UserAccountApi).changePassword({ changePasswordRequest: body })),
    onSuccess: () => void qc.invalidateQueries({ queryKey: queryKey('iam', 'me', { part: 'sessions' }) }),
  });
}

/** The caller's sign-in sessions (iam.ListUserSessions on their own account). */
export function useMySessions(enabled: boolean) {
  const { api } = useRuntime();
  const userId = useSession((s) => s.me?.user?.id);
  return useQuery({
    queryKey: queryKey('iam', 'me', { part: 'sessions', userId }),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(UserAccountApi)
            .listUserSessions({ userAccountId: userId as string, size: 100, ...pageQuery(page) }),
        ),
      ),
    enabled: enabled && Boolean(userId),
    staleTime: STALE_TIME.list,
  });
}

/** Ends one of the caller's sessions (iam.DeleteUserSession; its refresh tokens are revoked by the server). */
export function useEndSession() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const userId = useSession((s) => s.me?.user?.id);
  return useMutation({
    mutationFn: ({ id, etag }: { id: string; etag: string }) =>
      call(
        api
          .get(UserAccountApi)
          .deleteUserSession({ userAccountId: userId as string, userSessionId: id, ifMatch: etag }),
      ),
    onSuccess: () => void qc.invalidateQueries({ queryKey: queryKey('iam', 'me', { part: 'sessions' }) }),
  });
}

export type AuthToken = iam.AuthToken;
export type ApiTokenCreated = iam.ApiTokenCreated;

const API_TOKENS = (userId: string | undefined) => queryKey('iam', 'me', { part: 'apiTokens', userId });

/**
 * The sign-in session the current access token belongs to (its `sid` claim), so a list of sessions can tell "this
 * device" from the others. Read from the token only to label rows; the server decides everything.
 */
export function currentSessionId(accessToken: string | null): string | null {
  const payload = accessToken?.split('.')[1];
  if (!payload) return null;
  try {
    const sid = (JSON.parse(decodeBase64Url(payload)) as { sid?: unknown }).sid;
    return typeof sid === 'string' ? sid : null;
  } catch {
    return null;
  }
}

const BASE64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';

/** Base64url to text, without `atob` (not in every React Native runtime); the claims read here are ASCII. */
function decodeBase64Url(value: string): string {
  const bytes: number[] = [];
  let bits = 0;
  let buffer = 0;
  for (const char of value) {
    const index = BASE64.indexOf(char);
    if (index < 0) throw new Error('not base64url');
    buffer = (buffer << 6) | index;
    bits += 6;
    if (bits >= 8) {
      bits -= 8;
      bytes.push((buffer >> bits) & 0xff);
    }
  }
  return String.fromCharCode(...bytes);
}

/** Ends every other session of the caller (iam.DeleteUserSession each, with its ETag); this one stays. */
export function useEndOtherSessions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const userId = useSession((s) => s.me?.user?.id);
  return useMutation({
    mutationFn: async (others: readonly UserSession[]) => {
      for (const other of others) {
        if (!other.id || !other.etag) continue;
        await call(
          api.get(UserAccountApi).deleteUserSession({
            userAccountId: userId as string,
            userSessionId: other.id,
            ifMatch: other.etag,
          }),
        );
      }
    },
    onSettled: () => void qc.invalidateQueries({ queryKey: queryKey('iam', 'me', { part: 'sessions' }) }),
  });
}

/** ACC-08: the caller's personal API tokens (iam.ListAuthTokens); their secrets are never returned. */
export function useMyApiTokens(enabled: boolean) {
  const { api } = useRuntime();
  const userId = useSession((s) => s.me?.user?.id);
  return useQuery({
    queryKey: API_TOKENS(userId),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(UserAccountApi)
            .listAuthTokens({ userAccountId: userId as string, size: 100, ...pageQuery(page) }),
        ),
      ),
    enabled: enabled && Boolean(userId),
    staleTime: STALE_TIME.list,
  });
}

/** iam.CreateApiToken for the caller: the answer holds the secret, which is shown once and never again. */
export function useCreateApiToken() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const userId = useSession((s) => s.me?.user?.id);
  return useMutation({
    mutationFn: (body: { name: string; expiresAt?: string; abilities?: string[] }) =>
      call(
        api
          .get(UserAccountApi)
          .createApiToken({ userAccountId: userId as string, createApiTokenRequest: body }),
      ),
    onSuccess: () => void qc.invalidateQueries({ queryKey: API_TOKENS(userId) }),
  });
}

/** iam.DeleteAuthToken: the token stops working at once. */
export function useRevokeApiToken() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const userId = useSession((s) => s.me?.user?.id);
  return useMutation({
    mutationFn: ({ id, etag }: { id: string; etag: string }) =>
      call(
        api
          .get(UserAccountApi)
          .deleteAuthToken({ userAccountId: userId as string, authTokenId: id, ifMatch: etag }),
      ),
    onSuccess: () => void qc.invalidateQueries({ queryKey: API_TOKENS(userId) }),
  });
}
