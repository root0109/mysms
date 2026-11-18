import { useMutation, useQuery } from '@tanstack/react-query';
import type { iam } from '@mysms/sdk';
import { UserAccountApi } from '@mysms/sdk/iam/apis/UserAccountApi';
import { isApiError } from '../api/problem';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type SignUpRequest = iam.SignUpRequest;
export type SignUpInstitution = iam.SignUpInstitution;

/**
 * The version of the terms the apps show and record with iam.AcceptTerms (kept in the audit). The terms text is the
 * catalogue's `acc.terms.body`; a new text gets a new version.
 */
export const TERMS_VERSION = '2026-11';

/**
 * ACC-02: asks for a reset code by e-mail (iam.RequestPasswordReset). The server answers the same whether or not the
 * account exists, so the screen always says the same thing.
 */
export function useRequestPasswordReset() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (body: { tenantCode: string; identifier: string }) =>
      call(api.get(UserAccountApi).requestPasswordReset({ requestPasswordResetRequest: body })),
  });
}

/** ACC-02: sets a new password with a mailed code (iam.ResetPassword); every session of the account ends. */
export function useResetPassword() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (body: { token: string; newPassword: string }) =>
      call(api.get(UserAccountApi).resetPassword({ resetPasswordRequest: body })),
  });
}

/**
 * ACC-03: self-service sign-up (iam.SignUp) where the school allows it. The answer is a receipt that looks the same for
 * a new and an already registered address; either way an e-mail tells the student what to do next.
 */
export function useSignUp() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (body: SignUpRequest) => call(api.get(UserAccountApi).signUp({ signUpRequest: body })),
  });
}

/**
 * ACC-03: the schools a student may sign up at (iam.ListSignUpInstitutions): id and name of the active schools of the
 * tenant the code names. Nothing is asked while the code is blank; a closed sign-up or an unknown code is 403.
 */
export function useSignUpInstitutions(tenantCode: string) {
  const { api } = useRuntime();
  const code = tenantCode.trim();
  return useQuery({
    enabled: code !== '',
    queryKey: queryKey('iam', 'sign-up-institutions', { tenantCode: code.toLowerCase() }),
    queryFn: () => call(api.get(UserAccountApi).listSignUpInstitutions({ tenantCode: code })),
    select: (list) => list.items,
    staleTime: STALE_TIME.reference,
    retry: false,
  });
}

/** Why the school picker of ACC-03 offers no school, as a catalogue key; null when it offers the schools. */
export type SignUpSchoolsNotice =
  | 'acc.signUp.schoolNeedsCode'
  | 'acc.signUp.schoolsLoading'
  | 'acc.signUp.closed'
  | 'acc.signUp.schoolsUnavailable'
  | 'acc.signUp.noSchools';

/** The picker's notice for the code asked about and the state of {@link useSignUpInstitutions}. */
export function signUpSchoolsNotice(
  tenantCode: string,
  schools: { isError: boolean; error: unknown; data: readonly SignUpInstitution[] | undefined },
): SignUpSchoolsNotice | null {
  if (!tenantCode.trim()) return 'acc.signUp.schoolNeedsCode';
  if (schools.isError)
    return isApiError(schools.error) && schools.error.status === 403
      ? 'acc.signUp.closed'
      : 'acc.signUp.schoolsUnavailable';
  if (schools.data === undefined) return 'acc.signUp.schoolsLoading';
  return schools.data.length === 0 ? 'acc.signUp.noSchools' : null;
}

/** ACC-05: mails a code that proves the caller's e-mail address (iam.RequestVerification). */
export function useRequestVerification() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: () =>
      call(api.get(UserAccountApi).requestVerification({ requestVerificationRequest: { channel: 'email' } })),
  });
}

/** ACC-05: confirms a mailed code (iam.ConfirmVerification, also without a session); a signed-in Me is re-read. */
export function useConfirmVerification() {
  const { api, session } = useRuntime();
  return useMutation({
    mutationFn: (code: string) =>
      call(
        api
          .get(UserAccountApi)
          .confirmVerification({ confirmVerificationRequest: { channel: 'email', code } }),
      ),
    onSuccess: () => session.reloadMe(),
  });
}

/** ACC-04: records that the caller accepted the terms (iam.AcceptTerms); Me is re-read so the gate opens. */
export function useAcceptTerms() {
  const { api, session } = useRuntime();
  return useMutation({
    mutationFn: (termsVersion: string) =>
      call(api.get(UserAccountApi).acceptTerms({ acceptTermsRequest: { termsVersion } })),
    onSuccess: () => session.reloadMe(),
  });
}

/**
 * ACC-06: works in one of the caller's own role grants (iam.SwitchRole). The answer's tokens and Me replace the
 * session's, so navigation and every permission check follow the new role at once.
 */
export function useSwitchRole() {
  const { api, session } = useRuntime();
  return useMutation({
    mutationFn: async (grant: { roleId: string; scopeId?: string }) => {
      const result = await call(api.get(UserAccountApi).switchRole({ switchRoleRequest: grant }));
      await session.adopt(result.tokens, result.me);
      return result;
    },
  });
}

/** ADM-09: acts as another user (iam.StartImpersonation, a reason is required); the session switches to them. */
export function useImpersonate() {
  const { api, session } = useRuntime();
  return useMutation({
    mutationFn: async ({ userId, reason }: { userId: string; reason: string }) => {
      const token = await call(
        api
          .get(UserAccountApi)
          .startImpersonation({ userAccountId: userId, startImpersonationRequest: { reason } }),
      );
      await session.impersonate(token);
      return token;
    },
  });
}

/** Ends acting as another user (iam.StopImpersonation) and returns to the administrator's own session. */
export function useStopImpersonation() {
  const { session } = useRuntime();
  return useMutation({ mutationFn: () => session.endImpersonation() });
}
