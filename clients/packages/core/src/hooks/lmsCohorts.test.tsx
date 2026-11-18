import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeLms, lmsFixture, json } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  useCohort,
  useCohortRoster,
  useCohortStep,
  useEnrollInCohort,
  useSendCohortAnnouncement,
} from './lmsCohorts';
import { useLiveClasses, useLiveClassStep } from './lmsLiveClasses';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: lmsFixture.teacherUserId, username: 'meera', locale: 'en-IN' };
const { cohortId, liveClassId } = lmsFixture;

async function setup() {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    ...fakeLms(),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'meera', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

describe('the cohort workflows (LMS-21, LMS-23, LMS-27)', () => {
  it('reads the cohort with its etag and walks a lifecycle step under it; the wrong state is a 409', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(() => ({ cohort: useCohort(cohortId), step: useCohortStep() }), {
      wrapper,
    });
    await waitFor(() => expect(result.current.cohort.data?.etag).toBe('"1"'));
    expect(result.current.cohort.data?.data.lifecycleStatus).toBe('active');

    act(() => result.current.step.mutate({ cohortId, step: 'publish', etag: '"1"' }));
    await waitFor(() => expect(result.current.step.isError).toBe(true));
    expect(isApiError(result.current.step.error) && result.current.step.error.status).toBe(409);

    act(() => result.current.step.mutate({ cohortId, step: 'deactivate', etag: '"1"' }));
    await waitFor(() => expect(result.current.step.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.cohort.data?.data.lifecycleStatus).toBe('suspended'));
  });

  it('refuses a stale etag on a step with 412 and changes nothing', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(() => ({ cohort: useCohort(cohortId), step: useCohortStep() }), {
      wrapper,
    });
    await waitFor(() => expect(result.current.cohort.data?.etag).toBe('"1"'));
    act(() => result.current.step.mutate({ cohortId, step: 'deactivate', etag: '"9"' }));
    await waitFor(() => expect(result.current.step.isError).toBe(true));
    expect(isApiError(result.current.step.error) && result.current.step.error.status).toBe(412);
  });

  it('names the roster and counts an announcement to every active student', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({ roster: useCohortRoster(cohortId), announce: useSendCohortAnnouncement() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.roster.data?.items).toHaveLength(3));
    expect(result.current.roster.data?.items[0]?.displayName).toBe('Meera Iyer');

    act(() =>
      result.current.announce.mutate({ cohortId, subject: 'Exam week', content: 'The hall opens at nine.' }),
    );
    await waitFor(() => expect(result.current.announce.data?.queued).toBe(2));
  });

  it('enrols the caller while the cohort is open', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(() => useEnrollInCohort(), { wrapper });
    act(() => result.current.mutate({ cohortId }));
    await waitFor(() => expect(result.current.data?.isSelfEnrolled).toBe(true));
  });

  it('joins a live class through the provider link and counts an invite retry', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(() => ({ classes: useLiveClasses(), step: useLiveClassStep() }), {
      wrapper,
    });
    await waitFor(() => expect(result.current.classes.data?.items).toHaveLength(1));
    expect(result.current.classes.data?.items[0]?.inviteAttempts).toBe(0);

    act(() => result.current.step.mutate({ liveClassId, step: 'retryInvites' }));
    await waitFor(() => expect(result.current.step.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.classes.data?.items[0]?.inviteAttempts).toBe(1));
    expect(result.current.classes.data?.items[0]?.inviteStatus).toBe('retrying');
  });
});
