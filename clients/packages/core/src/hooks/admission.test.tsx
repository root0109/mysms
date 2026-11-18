import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { admissionFixture, fakeAdmission, fakeApi, json, schoolAdmin } from '@mysms/fixtures';
import { ApiError, isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  cycleStage,
  newApplicationNumber,
  retriesSameAttempt,
  stepsFor,
  useApplication,
  useApplicationGuardianChange,
  useApplications,
  useApplicationStep,
  useEnrollApplicant,
  useMyDraft,
  useStartApplication,
} from './admission';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const f = admissionFixture;
const APPLICATIONS = '/v1/admission/applications';

async function setup(options: Parameters<typeof fakeAdmission>[0] = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json(schoolAdmin),
    ...fakeAdmission(options),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'ravi', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

describe('the admission state tables', () => {
  it('offers the steps the server allows from each state', () => {
    expect(stepsFor('draft')).toEqual(['submit', 'cancel']);
    expect(stepsFor('submitted')).toEqual(['confirm', 'markPending', 'reject', 'cancel']);
    expect(stepsFor('confirmed')).toEqual(['markPending', 'approve', 'reject', 'cancel']);
    expect(stepsFor('approved')).toEqual(['confirmAdmission', 'reject', 'cancel']);
    expect(stepsFor('admitted')).toEqual(['cancel']);
    expect(stepsFor('rejected')).toEqual(['reset']);
    expect(stepsFor('done')).toEqual([]);
  });

  it('places a cycle on its timeline and takes a cancelled one off it', () => {
    expect(cycleStage('draft')).toBe(0);
    expect(cycleStage('open')).toBe(2);
    expect(cycleStage('closed')).toBe(4);
    expect(cycleStage('cancelled')).toBe(-1);
  });

  it('numbers a new application by day with an unambiguous random tail', () => {
    const number = newApplicationNumber(new Date('2026-03-09T10:00:00Z'), () => 0);
    expect(number).toBe('W-20260309-222222');
    expect(newApplicationNumber()).toMatch(/^W-\d{8}-[2-9A-HJ-NP-Z]{6}$/);
  });
});

describe('admission hooks', () => {
  it('lists the applications of one cycle and resumes the caller’s draft', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ all: useApplications(f.openCycleId), draft: useMyDraft(f.openCycleId) }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.all.isSuccess && result.current.draft.isSuccess).toBe(true));
    expect(result.current.all.data?.items).toHaveLength(5);
    expect(result.current.draft.data?.id).toBe(f.draftId);
    const lists = api.calls.filter((c) => c.method === 'GET' && c.path === APPLICATIONS);
    expect(lists.map((c) => new URLSearchParams(c.search).get('filter'))).toEqual(
      expect.arrayContaining([
        `admissionCycleId = "${f.openCycleId}"`,
        `admissionCycleId = "${f.openCycleId}" AND status = "draft"`,
      ]),
    );
  });

  it('takes a step under the etag it read, keeps a reason, and reports a step the state does not allow', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ app: useApplication(f.submittedId), step: useApplicationStep() }),
      {
        wrapper,
      },
    );
    await waitFor(() => expect(result.current.app.isSuccess).toBe(true));
    expect(result.current.app.data?.etag).toBe('"1"');
    await act(() =>
      result.current.step.mutateAsync({
        applicationId: f.submittedId,
        step: 'markPending',
        etag: result.current.app.data?.etag ?? null,
        reason: '  Birth certificate  ',
      }),
    );
    const post = api.calls.find((c) => c.path === `${APPLICATIONS}/${f.submittedId}:markPending`);
    expect(JSON.parse(post?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Birth certificate' });
    await waitFor(() => expect(result.current.app.data?.data.status).toBe('pending'));

    await act(async () => {
      await result.current.step
        .mutateAsync({ applicationId: f.submittedId, step: 'approve', etag: null })
        .catch(() => undefined);
    });
    await waitFor(() => expect(result.current.step.isError).toBe(true));
    expect(isApiError(result.current.step.error) && result.current.step.error.status).toBe(409);
  });

  it('starts a draft in the cycle with a number and adds a parent to it', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ start: useStartApplication(), guardian: useApplicationGuardianChange() }),
      { wrapper },
    );
    const started = await act(() =>
      result.current.start.mutateAsync({
        cycle: { id: f.openCycleId, institutionId: f.institutionId },
        form: { fullName: 'Nila Bose', dateOfBirth: '2020-02-02', gender: 'female' },
      }),
    );
    expect(started.etag).toBe('"1"');
    const post = api.calls.find((c) => c.method === 'POST' && c.path === APPLICATIONS);
    expect(JSON.parse(post?.body ?? '{}')).toMatchObject({
      institutionId: f.institutionId,
      admissionCycleId: f.openCycleId,
      fullName: 'Nila Bose',
      applicationNumber: expect.stringMatching(/^W-\d{8}-/) as unknown,
    });
    await act(() =>
      result.current.guardian.mutateAsync({
        applicationId: started.data.id ?? '',
        add: { relationship: 'father', name: 'Arun Bose', mobile: '+919800000009' },
      }),
    );
    expect(api.calls.some((c) => c.method === 'POST' && c.path.endsWith('/guardians'))).toBe(true);
  });

  it('enrols with one Idempotency-Key per attempt, the same key again after a failure', async () => {
    const { api, wrapper } = await setup({ enrolmentUnavailable: true });
    const { result } = renderHook(() => useEnrollApplicant(), { wrapper });
    const attempt = () =>
      result.current
        .mutateAsync({
          applicationId: f.admittedId,
          classSectionId: f.classSectionId,
          rollNumber: ' 12 ',
          createPortalAccounts: true,
        })
        .catch((error: unknown) => error);
    const first = await act(attempt);
    expect(isApiError(first) && first.status).toBe(503);
    await act(attempt);
    const enrolments = api.calls.filter((c) => c.path === `${APPLICATIONS}/${f.admittedId}:enroll`);
    expect(enrolments).toHaveLength(2);
    const keys = enrolments.map((c) => c.headers.get('Idempotency-Key'));
    expect(keys[0]).toBeTruthy();
    expect(keys[1]).toBe(keys[0]);
    expect(JSON.parse(enrolments[0]?.body ?? '{}')).toEqual({
      classSectionId: f.classSectionId,
      rollNumber: '12',
      createPortalAccounts: true,
    });
  });

  it('draws a new Idempotency-Key once the server kept a refusal, so the corrected request is not a replay', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(() => useEnrollApplicant(), { wrapper });
    const refused = await act(() =>
      result.current
        .mutateAsync({ applicationId: f.admittedId, createPortalAccounts: false })
        .catch((error: unknown) => error),
    );
    expect(isApiError(refused) && refused.status).toBe(422);
    await act(() =>
      result.current.mutateAsync({
        applicationId: f.admittedId,
        classSectionId: f.classSectionId,
        createPortalAccounts: false,
      }),
    );
    const keys = api.calls
      .filter((c) => c.path === `${APPLICATIONS}/${f.admittedId}:enroll`)
      .map((c) => c.headers.get('Idempotency-Key'));
    expect(keys).toHaveLength(2);
    expect(keys[1]).not.toBe(keys[0]);
  });

  it('retries the same attempt only after a failure the server does not keep', () => {
    const answer = (status: number) =>
      ApiError.fromResponse(new Response(null, { status }), { status, code: 'X' });
    expect(retriesSameAttempt(new TypeError('network'))).toBe(true);
    for (const status of [500, 503, 408, 425, 429]) expect(retriesSameAttempt(answer(status))).toBe(true);
    for (const status of [400, 403, 409, 412, 422]) expect(retriesSameAttempt(answer(status))).toBe(false);
  });

  it('answers the student and the portal-account operation of an enrolment', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(() => useEnrollApplicant(), { wrapper });
    const done = await act(() =>
      result.current.mutateAsync({
        applicationId: f.admittedId,
        classSectionId: f.classSectionId,
        createPortalAccounts: true,
      }),
    );
    expect(done.studentId).toBe(f.studentId);
    expect(done.application.status).toBe('done');
    expect(done.operation?.status).toBe('succeeded');
  });
});
