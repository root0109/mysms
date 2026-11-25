import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeSis, json, schoolAdmin, sisFixture } from '@mysms/fixtures';
import type { iam } from '@mysms/sdk';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import { useMyStudents } from './myStudents';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const f = sisFixture;

/** Signs in as the given account (its own student records and permissions) over the sis fakes. */
async function setup(me: iam.Me) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: me.user }),
    'GET /v1/iam/me': () => json(me),
    ...fakeSis(),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'x', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

describe('the students an account reads its own views for', () => {
  it('a student follows themself, named from their records, and never asks for children', async () => {
    const { api, wrapper } = await setup({
      ...schoolAdmin,
      studentIds: [f.studentId],
      permissions: ['sis.student.read', 'people.person.read'],
    });
    const mine = renderHook(() => useMyStudents(), { wrapper });
    expect(mine.result.current.isPending).toBe(false);
    await waitFor(() =>
      expect(mine.result.current.students).toEqual([{ id: f.studentId, name: 'Diya Menon' }]),
    );
    expect(api.calls.some((c) => c.path === '/v1/sis/me/children')).toBe(false);
  });

  it('a guardian follows their children, with the names the server gave them', async () => {
    const { wrapper } = await setup({
      ...schoolAdmin,
      studentIds: [],
      permissions: ['sis.guardian.list_my_children'],
    });
    const mine = renderHook(() => useMyStudents(), { wrapper });
    expect(mine.result.current.isPending).toBe(true);
    await waitFor(() => expect(mine.result.current.isPending).toBe(false));
    expect(mine.result.current.students).toEqual([{ id: f.studentId, name: 'Diya Menon' }]);
    expect(mine.result.current.isError).toBe(false);
  });

  it('an account with neither follows no one', async () => {
    const { wrapper } = await setup({ ...schoolAdmin, permissions: [] });
    const mine = renderHook(() => useMyStudents(), { wrapper });
    expect(mine.result.current.students).toEqual([]);
    expect(mine.result.current.isPending).toBe(false);
  });
});
