import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, json, pageOf, problem } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  useNotificationPreferenceSets,
  useNotificationPreferences,
  useUpdateNotificationPreference,
} from './notifications';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: 'u1', username: 'asha', locale: 'en-IN' };
const SET_ID = 'set-1';
const SETS = '/v1/comms/notification-preference-sets';

function setup() {
  let emailEnabled = true;
  let version = 1;
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    // pages of one, so the hooks must follow the next links to the end
    [`GET ${SETS}`]: (req) =>
      pageOf(
        req,
        [
          { id: SET_ID, userId: 'u1', roleId: 'r1' },
          { id: 'set-2', userId: 'u1', roleId: 'r2' },
        ],
        { maxSize: 1 },
      ),
    [`GET ${SETS}/{notificationPreferenceSetId}/preferences`]: (req, p) =>
      p.notificationPreferenceSetId !== SET_ID
        ? problem(404, 'NOT_FOUND', 'Not found')
        : pageOf(
            req,
            [
              {
                id: 'pref-1',
                preferenceSetId: SET_ID,
                propertyName: 'attendance.email',
                propertyValue: emailEnabled ? 'on' : 'off',
                etag: `"${version}"`,
              },
              { id: 'pref-2', preferenceSetId: SET_ID, propertyName: 'fees.email', propertyValue: 'off' },
            ],
            { maxSize: 1 },
          ),
    [`PATCH ${SETS}/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}`]: async (
      req,
      p,
    ) => {
      if (req.headers.get('If-Match') !== `"${version}"`) return problem(412, 'PRECONDITION_FAILED', 'Stale');
      emailEnabled = ((await req.json()) as { propertyValue: string }).propertyValue === 'on';
      version += 1;
      return json(
        {
          id: p.notificationPreferenceId,
          preferenceSetId: SET_ID,
          propertyName: 'attendance.email',
          propertyValue: emailEnabled ? 'on' : 'off',
        },
        { headers: { ETag: `"${version}"` } },
      );
    },
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, runtime, wrapper };
}

describe('notification preferences (ACC-10)', () => {
  it('reads every page of the own sets and preferences, and waits while nothing is chosen', async () => {
    const { api, runtime, wrapper } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });
    const { result, rerender } = renderHook(
      ({ userId, setId }: { userId: string | null; setId: string | null }) => ({
        sets: useNotificationPreferenceSets(userId),
        preferences: useNotificationPreferences(setId),
      }),
      { wrapper, initialProps: { userId: null as string | null, setId: null as string | null } },
    );
    // nothing is asked for before the account and the set are known
    expect(result.current.sets.fetchStatus).toBe('idle');
    expect(result.current.preferences.fetchStatus).toBe('idle');
    expect(api.calls.filter((c) => c.path.startsWith(SETS))).toHaveLength(0);

    rerender({ userId: 'u1', setId: SET_ID });
    await waitFor(() => expect(result.current.sets.data?.items.map((s) => s.id)).toEqual([SET_ID, 'set-2']));
    await waitFor(() =>
      expect(result.current.preferences.data?.items.map((p) => p.id)).toEqual(['pref-1', 'pref-2']),
    );
    const firstList = api.calls.find((c) => c.method === 'GET' && c.path === SETS);
    expect(new URLSearchParams(firstList?.search).get('filter')).toBe('userId = "u1"');
    expect(new URLSearchParams(firstList?.search).get('size')).toBe('500');
  });

  it('updates one preference under its ETag with an update mask, then refreshes the set', async () => {
    const { api, runtime, wrapper } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });
    const { result } = renderHook(
      () => ({ preferences: useNotificationPreferences(SET_ID), update: useUpdateNotificationPreference() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.preferences.data?.items[0]?.propertyValue).toBe('on'));

    const saved = await act(() =>
      result.current.update.mutateAsync({
        preferenceSetId: SET_ID,
        preferenceId: 'pref-1',
        etag: '"1"',
        changes: { propertyValue: 'off' },
      }),
    );
    expect(saved.etag).toBe('"2"');
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(new URLSearchParams(patch?.search).get('updateMask')).toBe('propertyValue');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    await waitFor(() => expect(result.current.preferences.data?.items[0]?.propertyValue).toBe('off'));

    // a stale ETag is refused by the server and surfaces as a problem, nothing changes
    await act(async () => {
      await expect(
        result.current.update.mutateAsync({
          preferenceSetId: SET_ID,
          preferenceId: 'pref-1',
          etag: '"1"',
          changes: { propertyValue: 'on' },
        }),
      ).rejects.toMatchObject({ status: 412 });
    });
  });
});
