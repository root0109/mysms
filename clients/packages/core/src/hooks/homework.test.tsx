import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeHomework, homeworkFixture, json } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  useCheckHomeworkSubmission,
  useHomework,
  useHomeworkApprovals,
  useHomeworkStep,
  useHomeworkSubmissions,
} from './homework';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: 'u1', username: 'asha', locale: 'en-IN' };
const { homeworkId, submissions } = homeworkFixture;
const HOMEWORK = `/v1/assessment/homeworks/${homeworkId}`;

async function setup(options: Parameters<typeof fakeHomework>[0] = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    ...fakeHomework(options),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

describe('the homework lifecycle (ASM-01…03)', () => {
  it('reads nothing until a homework is chosen, then the homework with its etag, reviews and submissions', async () => {
    const { api, wrapper } = await setup();
    const { result, rerender } = renderHook(
      ({ id }: { id: string | null }) => ({
        homework: useHomework(id),
        reviews: useHomeworkApprovals(id),
        submissions: useHomeworkSubmissions(id),
      }),
      { wrapper, initialProps: { id: null as string | null } },
    );
    expect(result.current.homework.fetchStatus).toBe('idle');
    expect(result.current.reviews.fetchStatus).toBe('idle');
    expect(result.current.submissions.fetchStatus).toBe('idle');
    rerender({ id: homeworkId });
    await waitFor(() => expect(result.current.homework.data?.etag).toBe('"1"'));
    expect(result.current.homework.data?.data.status).toBe('draft');
    await waitFor(() => expect(result.current.reviews.data?.items).toEqual([]));
    await waitFor(() => expect(result.current.submissions.data?.items).toHaveLength(2));
    const listed = api.calls.find((c) => c.path === '/v1/assessment/homework-submissions');
    expect(new URLSearchParams(listed?.search).get('filter')).toBe(`homeworkId = "${homeworkId}"`);
  });

  it('sends a draft for review, lets the reviewer send it back with a reason and approve the next round', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        homework: useHomework(homeworkId),
        reviews: useHomeworkApprovals(homeworkId),
        step: useHomeworkStep(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.homework.data?.etag).toBe('"1"'));
    const sent = await act(() =>
      result.current.step.mutateAsync({ homeworkId, step: 'publish', etag: '"1"' }),
    );
    expect(sent.status).toBe('pending');
    await waitFor(() =>
      expect(result.current.reviews.data?.items.map((r) => r.decision)).toEqual(['pending']),
    );

    const stale = await act(() =>
      result.current.step
        .mutateAsync({ homeworkId, step: 'reject', etag: '"1"', reason: 'x' })
        .catch((error: unknown) => error),
    );
    expect(isApiError(stale) && stale.status).toBe(412);
    await act(() =>
      result.current.step.mutateAsync({
        homeworkId,
        step: 'reject',
        etag: '"2"',
        reason: '  Due on a holiday ',
      }),
    );
    await waitFor(() => expect(result.current.homework.data?.data.status).toBe('draft'));
    await waitFor(() => expect(result.current.reviews.data?.items[0]?.comments).toBe('Due on a holiday'));

    await act(() => result.current.step.mutateAsync({ homeworkId, step: 'publish', etag: null }));
    await act(() =>
      result.current.step.mutateAsync({ homeworkId, step: 'approve', etag: null, reason: '  ' }),
    );
    await waitFor(() => expect(result.current.homework.data?.data.status).toBe('published'));
    expect(
      api.calls.filter((c) => c.method === 'POST' && c.path.startsWith(HOMEWORK)).map((c) => c.body),
    ).toEqual([
      '{"etag":"\\"1\\""}',
      '{"etag":"\\"1\\"","reason":"x"}',
      '{"etag":"\\"2\\"","reason":"Due on a holiday"}',
      '{}',
      '{}',
    ]);
  });

  it('marks a submission checked with trimmed comments under its etag and re-reads the class', async () => {
    const { api, wrapper } = await setup({ status: 'published' });
    const first = submissions[0].id;
    const { result } = renderHook(
      () => ({ submissions: useHomeworkSubmissions(homeworkId), check: useCheckHomeworkSubmission() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.submissions.data?.items).toHaveLength(2));
    await act(() =>
      result.current.check.mutateAsync({
        homeworkId,
        submissionId: first,
        etag: '"1"',
        teacherComments: ' Show your working ',
      }),
    );
    await waitFor(() =>
      expect(result.current.submissions.data?.items.find((s) => s.id === first)?.status).toBe('checked'),
    );
    const post = api.calls.find((c) => c.path.endsWith(`${first}:check`));
    expect(JSON.parse(post?.body ?? '{}')).toEqual({ etag: '"1"', teacherComments: 'Show your working' });
    expect(post?.headers.get('Idempotency-Key')).toBeTruthy();
    // no comments, no etag: the bare step
    await act(() => result.current.check.mutateAsync({ homeworkId, submissionId: first, etag: null }));
    expect(api.calls.filter((c) => c.path.endsWith(`${first}:check`))[1]?.body).toBe('{}');
  });
});
