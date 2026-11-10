import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeTimetable, json, schoolAdmin, timetableFixture } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  isTimetableDay,
  localDay,
  shiftDay,
  timetableDays,
  useMyStaff,
  useRoomNames,
  useSubjectNames,
  useTimetable,
  weekOf,
  type TimetableSession,
} from './timetable';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const VIEW = '/v1/timetable/sessions:timetable';

/** Signs in an account whose staff records iam.GetMe names (by default the fixture teacher's one). */
async function setup(
  options: Parameters<typeof fakeTimetable>[0] = {},
  staffIds: string[] = [timetableFixture.staffId],
) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json({ ...schoolAdmin, staffIds }),
    ...fakeTimetable(options),
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

const at = (id: string, startAt: string): TimetableSession => ({
  id,
  institutionId: 'i1',
  subjectId: 's1',
  staffId: 't1',
  startAt,
  endAt: startAt,
  status: 'confirmed',
});

describe('the days of a timetable', () => {
  it('moves days across month and year ends and finds the Monday-to-Sunday week of a day', () => {
    expect(shiftDay('2026-03-31', 1)).toBe('2026-04-01');
    expect(shiftDay('2026-01-01', -1)).toBe('2025-12-31');
    expect(weekOf('2026-03-02')).toEqual({ from: '2026-03-02', to: '2026-03-08' });
    expect(weekOf('2026-03-08')).toEqual({ from: '2026-03-02', to: '2026-03-08' });
    expect(weekOf('2026-03-05')).toEqual({ from: '2026-03-02', to: '2026-03-08' });
    expect(weekOf('2026-01-01')).toEqual({ from: '2025-12-29', to: '2026-01-04' });
  });

  it('accepts only calendar days that exist, in years a timetable has', () => {
    expect(isTimetableDay('2026-03-02')).toBe(true);
    expect(isTimetableDay('2024-02-29')).toBe(true);
    expect(isTimetableDay('9999-12-31')).toBe(true);
    for (const bad of [
      '2026-02-30',
      '2026-13-01',
      '1899-12-31',
      '0000-01-01',
      '2026-3-2',
      '2026-03-02T00:00',
      '',
    ])
      expect(isTimetableDay(bad)).toBe(false);
  });

  it('reads the calendar day of an instant in the time zone asked for', () => {
    expect(localDay('2026-03-08T18:45:00Z', 'Asia/Kolkata')).toBe('2026-03-09');
    expect(localDay('2026-03-09T02:00:00Z', 'America/Los_Angeles')).toBe('2026-03-08');
    expect(localDay(new Date('2026-03-08T12:00:00Z'), 'UTC')).toBe('2026-03-08');
  });

  it('lays sessions on the reader’s days in start order, keeps empty days and drops what lies outside', () => {
    const days = timetableDays(
      [
        at('late', '2026-03-02T09:00:00Z'),
        at('early', '2026-03-02T03:00:00Z'),
        // Sunday evening in UTC is already Monday in India: it belongs to the week after
        at('next-week', '2026-03-08T18:45:00Z'),
        // and Sunday evening before the week is this week's Monday
        at('first', '2026-03-01T19:00:00Z'),
      ],
      '2026-03-02',
      '2026-03-04',
      'Asia/Kolkata',
    );
    expect(days.map((day) => day.date)).toEqual(['2026-03-02', '2026-03-03', '2026-03-04']);
    expect(days[0]?.sessions.map((session) => session.id)).toEqual(['first', 'early', 'late']);
    expect(days[1]?.sessions).toEqual([]);
    expect(days[2]?.sessions).toEqual([]);
    // the order is by instant, whatever precision the server writes
    const precise = timetableDays(
      [at('b', '2026-03-02T08:00:00.5Z'), at('a', '2026-03-02T08:00:00Z')],
      '2026-03-02',
      '2026-03-02',
      'UTC',
    );
    expect(precise[0]?.sessions.map((session) => session.id)).toEqual(['a', 'b']);
    // the far ends of the world's zones are still inside the day read on each side
    expect(
      timetableDays([at('k', '2026-03-01T10:30:00Z')], '2026-03-02', '2026-03-02', 'Pacific/Kiritimati')[0]
        ?.sessions,
    ).toHaveLength(1);
    expect(
      timetableDays([at('b', '2026-03-03T11:30:00Z')], '2026-03-02', '2026-03-02', 'Etc/GMT+12')[0]?.sessions,
    ).toHaveLength(1);
  });
});

describe('reading a timetable (TT-03)', () => {
  it('reads the staff records iam names for the signed-in account, else those linked to the account', async () => {
    const teacher = await setup({}, [timetableFixture.staffId, timetableFixture.otherStaffId]);
    const mine = renderHook(() => useMyStaff(), { wrapper: teacher.wrapper });
    await waitFor(() => expect(mine.result.current.isSuccess).toBe(true));
    expect(mine.result.current.data?.items.map((staff) => staff.id)).toEqual([
      timetableFixture.staffId,
      timetableFixture.otherStaffId,
    ]);
    const read = teacher.api.calls.find((c) => c.path === '/v1/hr/staff');
    expect(new URLSearchParams(read?.search).get('filter')).toBe(
      `id = "${timetableFixture.staffId}" OR id = "${timetableFixture.otherStaffId}"`,
    );

    const office = await setup({}, []);
    const none = renderHook(() => useMyStaff(), { wrapper: office.wrapper });
    await waitFor(() => expect(none.result.current.isSuccess).toBe(true));
    expect(none.result.current.data?.items).toEqual([]);
    const byAccount = office.api.calls.find((c) => c.path === '/v1/hr/staff');
    expect(new URLSearchParams(byAccount?.search).get('filter')).toBe(`userId = "${schoolAdmin.user?.id}"`);
    const off = renderHook(() => useMyStaff(false), { wrapper: office.wrapper });
    expect(off.result.current.fetchStatus).toBe('idle');
  });

  it('asks for a day more on each side of the reader’s days and answers the sessions', async () => {
    const { api, wrapper } = await setup();
    const { result, rerender } = renderHook(
      ({ staffId }: { staffId: string | null }) =>
        useTimetable(staffId ? [{ staffId }] : null, '2026-03-02', '2026-03-08'),
      { wrapper, initialProps: { staffId: null as string | null } },
    );
    expect(result.current.fetchStatus).toBe('idle');
    rerender({ staffId: timetableFixture.staffId });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    const query = new URLSearchParams(api.calls.find((c) => c.path === VIEW)?.search);
    expect(query.get('fromDate')).toBe('2026-03-01');
    expect(query.get('toDate')).toBe('2026-03-09');
    expect(query.get('staffId')).toBe(timetableFixture.staffId);
    // the draft and the other teacher's session are not this teacher's timetable
    expect(result.current.data?.map((session) => session.status)).toEqual([
      'done',
      'confirmed',
      'cancelled',
      'confirmed',
    ]);
  });

  it('follows every page of a view, and reads the staff records of someone employed twice as one timetable', async () => {
    const paged = await setup({ pageSize: 2 });
    const one = renderHook(
      () => useTimetable([{ staffId: timetableFixture.staffId }], '2026-03-02', '2026-03-08'),
      { wrapper: paged.wrapper },
    );
    await waitFor(() => expect(one.result.current.isSuccess).toBe(true));
    expect(one.result.current.data).toHaveLength(4);
    expect(paged.api.calls.filter((c) => c.path === VIEW)).toHaveLength(2);

    const twice = await setup();
    const both = renderHook(
      () =>
        useTimetable(
          [
            { staffId: timetableFixture.staffId },
            { staffId: timetableFixture.otherStaffId },
            // the same record asked for again is not counted again
            { staffId: timetableFixture.staffId },
          ],
          '2026-03-02',
          '2026-03-08',
        ),
      { wrapper: twice.wrapper },
    );
    await waitFor(() => expect(both.result.current.isSuccess).toBe(true));
    expect(both.result.current.data).toHaveLength(5);
    const none = renderHook(() => useTimetable([], '2026-03-02', '2026-03-08'), { wrapper: twice.wrapper });
    expect(none.result.current.fetchStatus).toBe('idle');
  });

  it('reads subject and room names once, by id, and nothing when the role may not', async () => {
    const { api, wrapper } = await setup();
    const names = renderHook(() => ({ subjects: useSubjectNames(), rooms: useRoomNames() }), { wrapper });
    await waitFor(() => expect(names.result.current.rooms.size).toBe(1));
    await waitFor(() => expect(names.result.current.subjects.size).toBe(2));
    expect(names.result.current.subjects.get(timetableFixture.subjects[1].id)).toBe('Science');
    expect(names.result.current.rooms.get(timetableFixture.rooms[0].id)).toBe('Room 12');

    const denied = await setup();
    const off = renderHook(() => ({ subjects: useSubjectNames(false), rooms: useRoomNames(false) }), {
      wrapper: denied.wrapper,
    });
    expect(off.result.current.subjects.size).toBe(0);
    expect(off.result.current.rooms.size).toBe(0);
    expect(denied.api.calls.some((c) => c.path === '/v1/academics/subjects')).toBe(false);
    expect(denied.api.calls.some((c) => c.path === '/v1/facility/rooms')).toBe(false);
    expect(api.calls.filter((c) => c.path === '/v1/academics/subjects')).toHaveLength(1);
  });
});
