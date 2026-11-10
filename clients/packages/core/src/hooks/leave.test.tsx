import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeLeave, json, leaveFixture, schoolAdmin } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  academicYearOn,
  cancellation,
  leaveDays,
  useAcademicYears,
  useApplyForLeave,
  useLeaveApplications,
  useLeavePosition,
  useLeaveStep,
  useLeaveTypes,
  usePendingLeave,
  useStaffNames,
  type AcademicYear,
} from './leave';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const { institutionId, academicYearId, staffId, colleagueStaffId, casualId } = leaveFixture;
const APPLICATIONS = '/v1/hr/leave-applications';

async function setup(options: Parameters<typeof fakeLeave>[0] = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json(schoolAdmin),
    ...fakeLeave(options),
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

describe('counting leave', () => {
  it('counts both ends of a leave and halves a half-day part, as the server does', () => {
    expect(leaveDays('2026-03-16', '2026-03-16', 'full_day')).toBe(1);
    expect(leaveDays('2026-03-16', '2026-03-18', 'full_day')).toBe(3);
    expect(leaveDays('2026-03-16', '2026-03-16', 'forenoon')).toBe(0.5);
    expect(leaveDays('2026-03-16', '2026-03-17', 'afternoon')).toBe(1);
    // across the end of a month, a year and a clock change
    expect(leaveDays('2026-02-27', '2026-03-02', 'full_day')).toBe(4);
    expect(leaveDays('2025-12-31', '2026-01-01', 'full_day')).toBe(2);
    expect(leaveDays('2026-03-28', '2026-03-30', 'full_day')).toBe(3);
  });

  it('answers nothing for days that are not a range', () => {
    expect(leaveDays('2026-03-18', '2026-03-16', 'full_day')).toBeNull();
    expect(leaveDays('', '2026-03-16', 'full_day')).toBeNull();
    expect(leaveDays('2026-03-16', '16/03/2026', 'full_day')).toBeNull();
    expect(leaveDays('2026-13-40', '2026-13-41', 'full_day')).toBeNull();
  });

  it('finds the academic year of an institution that a day lies in', () => {
    const year = (id: string, institution: string, startDate: string, endDate: string): AcademicYear => ({
      id,
      institutionId: institution,
      name: id,
      startDate,
      endDate,
    });
    const years = [
      year('last', 'i1', '2025-04-01', '2026-03-31'),
      year('this', 'i1', '2026-04-01', '2027-03-31'),
      year('other-school', 'i2', '2026-01-01', '2026-12-31'),
    ];
    expect(academicYearOn(years, 'i1', '2026-03-31')?.id).toBe('last');
    expect(academicYearOn(years, 'i1', '2026-04-01')?.id).toBe('this');
    expect(academicYearOn(years, 'i2', '2026-04-01')?.id).toBe('other-school');
    expect(academicYearOn(years, 'i1', '2028-01-01')).toBeUndefined();
  });

  it('withdraws a pending application and cancels an approved leave only before it starts', () => {
    const now = new Date('2026-03-09T10:00:00Z');
    expect(cancellation({ status: 'pending', fromAt: '2026-03-01T00:00:00Z' }, now)).toBe('withdraw');
    expect(cancellation({ status: 'approved', fromAt: '2026-03-10T00:00:00Z' }, now)).toBe('cancel');
    expect(cancellation({ status: 'approved', fromAt: '2026-03-09T00:00:00Z' }, now)).toBeNull();
    expect(cancellation({ status: 'rejected', fromAt: '2026-03-10T00:00:00Z' }, now)).toBeNull();
    expect(cancellation({ status: 'cancelled', fromAt: '2026-03-10T00:00:00Z' }, now)).toBeNull();
  });
});

describe('staff leave (HR-04)', () => {
  it('reads the position as numbers, the leave types in the school’s order and the year, and nothing unasked', async () => {
    const { api, wrapper } = await setup();
    const { result, rerender } = renderHook(
      ({ staff }: { staff: string | null }) => ({
        position: useLeavePosition(staff, staff ? academicYearId : null),
        types: useLeaveTypes(staff ? institutionId : null),
        years: useAcademicYears(staff !== null),
      }),
      { wrapper, initialProps: { staff: null as string | null } },
    );
    expect(result.current.position.fetchStatus).toBe('idle');
    expect(result.current.types.fetchStatus).toBe('idle');
    expect(result.current.years.fetchStatus).toBe('idle');
    rerender({ staff: staffId });
    await waitFor(() => expect(result.current.position.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.types.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.years.isSuccess).toBe(true));
    expect(result.current.position.data?.[0]).toEqual({
      leaveTypeId: casualId,
      entitled: 10,
      taken: 2,
      pending: 2,
      available: 6,
    });
    expect(result.current.types.data?.map((type) => type.name)).toEqual(['Casual leave', 'Sick leave']);
    expect(result.current.years.data?.items[0]?.id).toBe(academicYearId);
    const read = api.calls.find((c) => c.path.endsWith(':leaveBalance'));
    expect(new URLSearchParams(read?.search).get('academicYearId')).toBe(academicYearId);
  });

  it('applies for whole days from the first to the last, and the position counts what was asked for', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ apply: useApplyForLeave(), position: useLeavePosition(staffId, academicYearId) }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.position.isSuccess).toBe(true));
    await act(async () => {
      await result.current.apply.mutateAsync({
        institutionId,
        academicYearId,
        staffId,
        leaveTypeId: casualId,
        from: '2026-04-06',
        to: '2026-04-07',
        dayPart: 'full_day',
        remarks: '  Wedding  ',
      });
    });
    const post = api.calls.find((c) => c.method === 'POST' && c.path === APPLICATIONS);
    expect(JSON.parse(post?.body ?? '{}')).toEqual({
      institutionId,
      academicYearId,
      staffId,
      leaveTypeId: casualId,
      fromAt: '2026-04-06T00:00:00Z',
      toAt: '2026-04-07T00:00:00Z',
      dayPart: 'full_day',
      remarks: 'Wedding',
    });
    await waitFor(() => expect(result.current.position.data?.[0]?.pending).toBe(4));
  });

  it('lists a staff member’s applications and the queue, and decides under the etag with the reason', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        mine: useLeaveApplications(staffId),
        queue: usePendingLeave(),
        names: useStaffNames([colleagueStaffId, colleagueStaffId]),
        step: useLeaveStep(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.queue.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.mine.isSuccess).toBe(true));
    await waitFor(() => expect(result.current.names.get(colleagueStaffId)).toBe('Meera Iyer'));
    expect(result.current.mine.data?.items).toHaveLength(1);
    expect(result.current.queue.data?.items).toHaveLength(2);
    const lists = api.calls.filter((c) => c.path === APPLICATIONS).map((c) => new URLSearchParams(c.search));
    expect(lists.map((query) => query.get('filter')).sort()).toEqual([
      `staffId = "${staffId}"`,
      'status = "pending"',
    ]);

    const colleague = result.current.queue.data?.items.find((row) => row.staffId === colleagueStaffId);
    await act(async () => {
      await result.current.step.mutateAsync({
        applicationId: colleague?.id ?? '',
        step: 'approve',
        etag: colleague?.etag ?? null,
        reason: ' Enjoy ',
      });
    });
    const approve = api.calls.find((c) => c.path.endsWith(':approve'));
    expect(JSON.parse(approve?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Enjoy' });
    await waitFor(() => expect(result.current.queue.data?.items).toHaveLength(1));

    // one's own application is withdrawn, never approved
    const own = result.current.mine.data?.items[0];
    await act(async () => {
      await expect(
        result.current.step.mutateAsync({ applicationId: own?.id ?? '', step: 'approve', etag: null }),
      ).rejects.toSatisfy((error: unknown) => isApiError(error) && error.status === 403);
    });
    await act(async () => {
      await result.current.step.mutateAsync({ applicationId: own?.id ?? '', step: 'cancel', etag: '"1"' });
    });
    await waitFor(() => expect(result.current.mine.data?.items[0]?.status).toBe('cancelled'));
    await act(async () => {
      await expect(
        result.current.step.mutateAsync({ applicationId: own?.id ?? '', step: 'reject', etag: null }),
      ).rejects.toSatisfy((error: unknown) => isApiError(error) && error.status === 409);
    });
  });

  it('reads no names when it may not, and none for nobody', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ off: useStaffNames([colleagueStaffId], false), none: useStaffNames([]) }),
      { wrapper },
    );
    expect(result.current.off.size).toBe(0);
    expect(result.current.none.size).toBe(0);
    expect(api.calls.some((c) => c.path === '/v1/hr/staff')).toBe(false);
  });
});
