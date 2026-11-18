import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeIam, iamFixture } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  signUpSchoolsNotice,
  useAcceptTerms,
  useConfirmVerification,
  useImpersonate,
  useRequestPasswordReset,
  useResetPassword,
  useSignUp,
  useSignUpInstitutions,
  useStopImpersonation,
  useSwitchRole,
} from './account';
import {
  currentSessionId,
  useCreateApiToken,
  useEndOtherSessions,
  useMyApiTokens,
  useMySessions,
  useRevokeApiToken,
} from './security';
import {
  userFilter,
  userStatusSteps,
  useSetUserRoles,
  useUserAccounts,
  useUserStatusStep,
  useUsernameAvailability,
} from './users';

async function setup(signedIn = true) {
  const api = fakeApi(fakeIam());
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  if (signedIn) await runtime.session.signIn({ tenantCode: 'demo', identifier: 'ravi', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, runtime, wrapper };
}

describe('account flows without a session', () => {
  it('asks for a reset code and sets a new password with it', async () => {
    const { api, wrapper } = await setup(false);
    const { result } = renderHook(() => ({ ask: useRequestPasswordReset(), reset: useResetPassword() }), {
      wrapper,
    });
    await act(() => result.current.ask.mutateAsync({ tenantCode: 'demo', identifier: 'meera' }));
    await act(() =>
      result.current.reset.mutateAsync({ token: iamFixture.resetToken, newPassword: 'a-new-password-1' }),
    );
    const wrong = await result.current.reset
      .mutateAsync({ token: 'used', newPassword: 'x' })
      .catch((e: unknown) => e);
    expect(isApiError(wrong) && wrong.errors[0]?.code).toBe('INVALID_CODE');
    expect(api.calls.map((c) => c.path)).toEqual([
      '/v1/iam/auth:requestPasswordReset',
      '/v1/iam/auth:resetPassword',
      '/v1/iam/auth:resetPassword',
    ]);
    expect(api.calls[0]?.headers.get('Authorization')).toBeNull();
  });

  it('signs up and confirms a mailed code', async () => {
    const { wrapper } = await setup(false);
    const { result } = renderHook(() => ({ signUp: useSignUp(), confirm: useConfirmVerification() }), {
      wrapper,
    });
    const receipt = await act(() =>
      result.current.signUp.mutateAsync({
        tenantCode: 'demo',
        institutionId: iamFixture.northSchoolId,
        email: 'Asha@School.example',
        fullName: 'Asha',
        acceptTerms: true,
      }),
    );
    expect(receipt).toMatchObject({ username: 'asha@school.example', userType: 'student' });
    const confirmed = await act(() => result.current.confirm.mutateAsync(iamFixture.verificationCode));
    expect(confirmed.emailVerifiedAt).toBeTruthy();
  });
});

describe('the schools a student may sign up at', () => {
  it('asks nothing while the code is blank, then lists the schools of the code by id and name', async () => {
    const { api, wrapper } = await setup(false);
    const { result, rerender } = renderHook(({ code }) => useSignUpInstitutions(code), {
      wrapper,
      initialProps: { code: ' ' },
    });
    expect(result.current.fetchStatus).toBe('idle');
    rerender({ code: ' demo ' });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([
      { id: iamFixture.northSchoolId, name: 'North School' },
      { id: iamFixture.southSchoolId, name: 'South School' },
    ]);
    expect(api.calls.map((c) => c.path)).toEqual(['/v1/iam/tenant-codes/demo/sign-up-institutions']);
    expect(api.calls[0]?.headers.get('Authorization')).toBeNull();
  });

  it('says why the picker offers no school', async () => {
    const { wrapper } = await setup(false);
    const { result } = renderHook(() => useSignUpInstitutions('elsewhere'), { wrapper });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(signUpSchoolsNotice('elsewhere', result.current)).toBe('acc.signUp.closed');
    const none = { isError: false, error: null };
    expect(signUpSchoolsNotice('', { ...none, data: undefined })).toBe('acc.signUp.schoolNeedsCode');
    expect(signUpSchoolsNotice('demo', { ...none, data: undefined })).toBe('acc.signUp.schoolsLoading');
    expect(signUpSchoolsNotice('demo', { ...none, data: [] })).toBe('acc.signUp.noSchools');
    expect(signUpSchoolsNotice('demo', { isError: true, error: new Error('offline'), data: undefined })).toBe(
      'acc.signUp.schoolsUnavailable',
    );
    expect(
      signUpSchoolsNotice('demo', {
        ...none,
        data: [{ id: iamFixture.northSchoolId, name: 'North School' }],
      }),
    ).toBeNull();
  });

  it('names the refused school field when the school does not take the student', async () => {
    const { wrapper } = await setup(false);
    const { result } = renderHook(() => useSignUp(), { wrapper });
    const refused = await result.current
      .mutateAsync({
        tenantCode: 'demo',
        institutionId: iamFixture.teacherId,
        email: 'asha@school.example',
        fullName: 'Asha',
        acceptTerms: true,
      })
      .catch((e: unknown) => e);
    expect(isApiError(refused) && refused.fieldErrors()['institutionId']).toBeTruthy();
  });
});

describe('the signed-in account', () => {
  it('switches role: the answer replaces the tokens and Me', async () => {
    const { runtime, wrapper } = await setup();
    const { result } = renderHook(() => useSwitchRole(), { wrapper });
    await act(() => result.current.mutateAsync({ roleId: iamFixture.reviewerRoleId }));
    expect(runtime.session.store.getState().me?.activeRole?.roleCode).toBe('reviewer');
    expect(runtime.session.store.getState().accessToken).not.toBeNull();
  });

  it('acts as another user and comes back', async () => {
    const { api, runtime, wrapper } = await setup();
    const { result } = renderHook(() => ({ start: useImpersonate(), stop: useStopImpersonation() }), {
      wrapper,
    });
    await act(() => result.current.start.mutateAsync({ userId: iamFixture.teacherId, reason: 'ticket 7' }));
    expect(runtime.session.store.getState().impersonation?.userId).toBe(iamFixture.teacherId);
    expect(runtime.session.store.getState().accessToken).toBe('imp-token');
    await act(() => result.current.stop.mutateAsync());
    expect(runtime.session.store.getState().impersonation).toBeNull();
    const stop = api.calls.find((c) => c.path === '/v1/iam/auth:stopImpersonation');
    expect(stop?.headers.get('Authorization')).toBe('Bearer imp-token');
  });

  it('accepts the terms and re-reads Me', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(() => useAcceptTerms(), { wrapper });
    await act(() => result.current.mutateAsync('2026-11'));
    expect(api.calls.filter((c) => c.path === '/v1/iam/me')).toHaveLength(2);
  });

  it('lists, creates and revokes API tokens; the secret comes only with the creation', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ list: useMyApiTokens(true), create: useCreateApiToken(), revoke: useRevokeApiToken() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.list.data?.items).toHaveLength(1));
    const created = await act(() => result.current.create.mutateAsync({ name: 'CI' }));
    expect(created.token).toBe('mysms_pat_secret-shown-once');
    await waitFor(() => expect(result.current.list.data?.items).toHaveLength(2));
    await act(() => result.current.revoke.mutateAsync({ id: iamFixture.apiTokenId, etag: '"1"' }));
    const revoke = api.calls.find((c) => c.method === 'DELETE');
    expect(revoke?.headers.get('If-Match')).toBe('"1"');
    await waitFor(() => expect(result.current.list.data?.items).toHaveLength(1));
  });

  it('ends every session but this one', async () => {
    const { runtime, wrapper } = await setup();
    expect(currentSessionId(runtime.session.store.getState().accessToken)).toBe(iamFixture.thisSessionId);
    expect(currentSessionId('not-a-jwt')).toBeNull();
    const { result } = renderHook(() => ({ sessions: useMySessions(true), end: useEndOtherSessions() }), {
      wrapper,
    });
    await waitFor(() => expect(result.current.sessions.data?.items).toHaveLength(2));
    const others = (result.current.sessions.data?.items ?? []).filter(
      (s) => s.id !== iamFixture.thisSessionId,
    );
    await act(() => result.current.end.mutateAsync(others));
    await waitFor(() => expect(result.current.sessions.data?.items).toHaveLength(1));
  });
});

describe('account administration', () => {
  it('narrows the list and offers the status steps the state machine allows', () => {
    expect(userFilter({})).toBeUndefined();
    expect(userFilter({ search: ' me"e ', status: 'active', userType: 'teacher' })).toBe(
      '(username : "mee" OR email : "mee" OR mobileNo : "mee") AND status = "active" AND userType = "teacher"',
    );
    expect(userStatusSteps('inactive')).toEqual(['activate']);
    expect(userStatusSteps('locked')).toEqual(['deactivate']);
    expect(userStatusSteps('exited')).toEqual([]);
  });

  it('lists users, takes a status step under the ETag, checks a name and replaces grants', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        list: useUserAccounts({ status: 'inactive' }),
        step: useUserStatusStep(),
        name: useUsernameAvailability('MEERA'),
        roles: useSetUserRoles(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.list.data?.pages[0]?.data).toHaveLength(1));
    await act(() =>
      result.current.step.mutateAsync({ id: iamFixture.studentId, etag: '"1"', step: 'activate' }),
    );
    expect(api.calls.some((c) => c.path.endsWith(':activate') && c.body.includes('"etag":"\\"1\\""'))).toBe(
      true,
    );
    await waitFor(() => expect(result.current.name.data).toEqual({ available: false, suggestion: 'MEERA2' }));
    const items = await act(() =>
      result.current.roles.mutateAsync({
        id: iamFixture.studentId,
        grants: [{ roleCode: 'reviewer', scopeType: 'tenant' }],
      }),
    );
    expect(items.items).toHaveLength(1);
  });
});
