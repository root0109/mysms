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
