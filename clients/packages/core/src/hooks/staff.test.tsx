import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeStaff, json, schoolAdmin, staffFixture } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  portalAccountBlocker,
  staffFilter,
  staffStatusMoves,
  useChangeStaffStatus,
  useCreateLeaveBalance,
  useCreateStaff,
  useCreateStaffPortalAccount,
  useLeaveBalances,
  usePersonNames,
  usePersonsNamed,
  useStaff,
  useStaffAssignmentStep,
  useStaffAssignments,
  useStaffDirectory,
  useUpdateLeaveBalance,
  useUpdateStaff,
} from './staff';
import { useStaffSalaries } from './payroll';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const fx = staffFixture;

async function setup(options: Parameters<typeof fakeStaff>[0] = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json(schoolAdmin),
    ...fakeStaff(options),
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

describe('staff rules', () => {
  it('moves a staff member only along the allowed status changes', () => {
    expect(staffStatusMoves('active')).toEqual(['inactive', 'exited']);
    expect(staffStatusMoves('inactive')).toEqual(['active', 'exited']);
    expect(staffStatusMoves('exited')).toEqual(['active']);
    expect(staffStatusMoves(undefined)).toEqual([]);
  });

  it('says why a staff member cannot be given a login account', () => {
    expect(portalAccountBlocker({ userId: 'u', status: 'active', employeeCode: 'T-1' })).toBe('hasAccount');
    expect(portalAccountBlocker({ status: 'exited', employeeCode: 'T-1' })).toBe('exited');
    expect(portalAccountBlocker({ status: 'active', employeeCode: '  ' })).toBe('noEmployeeCode');
    expect(portalAccountBlocker({ status: 'inactive', employeeCode: 'T-1' })).toBeNull();
  });

  it('narrows the directory by every part of the query, a search by number or by name', () => {
    expect(staffFilter({})).toBeUndefined();
    expect(staffFilter({ status: 'active', designationId: 'd1', departmentId: 'p1' })).toBe(
      'status = "active" AND designationId = "d1" AND primaryDepartmentId = "p1"',
    );
    expect(staffFilter({ search: ' Iy"er ' }, ['x1', 'x2'])).toBe(
      '(employeeCode : "Iyer" OR personId = "x1" OR personId = "x2")',
    );
  });
});

describe('staff directory and detail', () => {
  it('lists staff narrowed by status, designation and department, with their names', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => {
        const list = useStaffDirectory({ status: 'active', designationId: fx.teacherDesignationId });
        const rows = list.data?.pages.flatMap((page) => page.data) ?? [];
        return { list, rows, names: usePersonNames(rows.map((row) => row.personId)) };
      },
      { wrapper },
    );
    await waitFor(() => expect(result.current.names.size).toBe(1));
    expect(result.current.rows.map((row) => row.id)).toEqual([fx.teacherId]);
    expect(result.current.names.get(fx.teacherPersonId)).toBe('Anil Kumar');
    const read = api.calls.find((c) => c.path === '/v1/hr/staff');
    expect(new URLSearchParams(read?.search).get('filter')).toBe(
      `status = "active" AND designationId = "${fx.teacherDesignationId}"`,
    );
  });

  it('finds staff by a part of their name through the persons it matches', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => {
        const persons = usePersonsNamed('meera');
        const list = useStaffDirectory({ search: 'meera' }, persons.data ?? [], persons.isSuccess);
        return list.data?.pages.flatMap((page) => page.data) ?? [];
      },
      { wrapper },
    );
    await waitFor(() => expect(result.current.map((row) => row.id)).toEqual([fx.colleagueId]));
  });

  it('creates a staff member, updates exactly the fields given under If-Match and refuses a stale one', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ create: useCreateStaff(), update: useUpdateStaff(), staff: useStaff(fx.colleagueId) }),
      { wrapper },
    );
    await act(() =>
      result.current.create.mutateAsync({
        personId: 'p-new',
        institutionId: fx.institutionId,
        employeeCode: 'N-1',
      }),
    );
    await waitFor(() => expect(result.current.staff.data?.etag).toBe('"1"'));
    await act(() =>
      result.current.update.mutateAsync({
        staffId: fx.colleagueId,
        etag: '"1"',
        changes: { specialization: 'Accounts' },
      }),
    );
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(new URLSearchParams(patch?.search).get('updateMask')).toBe('specialization');
    await waitFor(() => expect(result.current.staff.data?.data.specialization).toBe('Accounts'));
    const stale = await result.current.update
      .mutateAsync({ staffId: fx.colleagueId, etag: '"1"', changes: { specialization: 'x' } })
      .catch((e: unknown) => e);
    expect(isApiError(stale) && stale.status).toBe(412);
  });

  it('reads the record again after a refused step, so a retry carries the current etag', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        staff: useStaff(fx.colleagueId),
        move: useChangeStaffStatus(),
        update: useUpdateStaff(),
        account: useCreateStaffPortalAccount(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.staff.data?.etag).toBe('"1"'));
    // another user changes the record meanwhile
    await api.fetch(
      new Request(`https://school.example.com/v1/hr/staff/${fx.colleagueId}?updateMask=specialization`, {
        method: 'PATCH',
        headers: { 'If-Match': '"1"', 'Content-Type': 'application/json' },
        body: JSON.stringify({ specialization: 'Accounts' }),
      }),
    );
    const stale = await result.current.move
      .mutateAsync({ staffId: fx.colleagueId, status: 'inactive', etag: '"1"' })
      .catch((e: unknown) => e);
    expect(isApiError(stale) && stale.status).toBe(412);
    await waitFor(() => expect(result.current.staff.data?.etag).toBe('"2"'));
    expect(result.current.staff.data?.data.specialization).toBe('Accounts');
    const retried = await act(() =>
      result.current.move.mutateAsync({ staffId: fx.colleagueId, status: 'inactive', etag: '"2"' }),
    );
    expect(retried.status).toBe('inactive');
    await waitFor(() => expect(result.current.staff.data?.etag).toBe('"3"'));

    const staleEdit = await result.current.update
      .mutateAsync({ staffId: fx.colleagueId, etag: '"2"', changes: { specialization: 'x' } })
      .catch((e: unknown) => e);
    expect(isApiError(staleEdit) && staleEdit.status).toBe(412);
    // a conflict (409) is followed by a fresh read too
    const reads = () =>
      api.calls.filter((c) => c.method === 'GET' && c.path === `/v1/hr/staff/${fx.colleagueId}`);
    const before = reads().length;
    await act(() => result.current.account.mutateAsync({ staffId: fx.colleagueId }));
    const twice = await result.current.account
      .mutateAsync({ staffId: fx.colleagueId })
      .catch((e: unknown) => e);
    expect(isApiError(twice) && twice.status).toBe(409);
    await waitFor(() => expect(reads().length).toBeGreaterThanOrEqual(before + 2));
  });

  it('changes the status with the etag last read and refuses a move the status does not allow', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(() => useChangeStaffStatus(), { wrapper });
    const changed = await act(() =>
      result.current.mutateAsync({ staffId: fx.colleagueId, status: 'inactive', etag: '"1"' }),
    );
    expect(changed.status).toBe('inactive');
    expect(JSON.parse(api.calls.at(-1)?.body ?? '{}')).toEqual({ status: 'inactive', etag: '"1"' });
    const refused = await result.current
      .mutateAsync({ staffId: fx.leaverId, status: 'inactive', etag: null })
      .catch((e: unknown) => e);
    expect(isApiError(refused) && refused.status).toBe(409);
  });

  it('gives a login account once and says why not otherwise', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(() => useCreateStaffPortalAccount(), { wrapper });
    const given = await act(() => result.current.mutateAsync({ staffId: fx.colleagueId }));
    expect(given.userId).toBeTruthy();
    const again = await result.current.mutateAsync({ staffId: fx.colleagueId }).catch((e: unknown) => e);
    expect(isApiError(again) && again.detail).toBe('The staff member already has a login account.');
  });

  it('adds, re-points and removes a department, refusing one the staff member already has', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        rows: useStaffAssignments('departments', fx.teacherId),
        step: useStaffAssignmentStep('departments'),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.rows.data).toHaveLength(1));
    const duplicate = await result.current.step
      .mutateAsync({ staffId: fx.teacherId, action: 'add', targetId: fx.scienceDepartmentId })
      .catch((e: unknown) => e);
    expect(isApiError(duplicate) && duplicate.status).toBe(409);
    await act(() =>
      result.current.step.mutateAsync({
        staffId: fx.teacherId,
        action: 'add',
        targetId: fx.officeDepartmentId,
      }),
    );
    await waitFor(() => expect(result.current.rows.data).toHaveLength(2));
    const first = result.current.rows.data?.[0];
    await act(() =>
      result.current.step.mutateAsync({
        staffId: fx.teacherId,
        action: 'remove',
        rowId: first?.id ?? '',
        etag: first?.etag ?? '',
      }),
    );
    await waitFor(() => expect(result.current.rows.data).toHaveLength(1));
    expect(result.current.rows.data?.[0]?.departmentId).toBe(fx.officeDepartmentId);
  });

  it('adds and re-points a subject under its etag', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        rows: useStaffAssignments('subjects', fx.teacherId),
        step: useStaffAssignmentStep('subjects'),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.rows.data).toHaveLength(1));
    const row = result.current.rows.data?.[0];
    await act(() =>
      result.current.step.mutateAsync({
        staffId: fx.teacherId,
        action: 'change',
        rowId: row?.id ?? '',
        etag: row?.etag ?? '',
        targetId: fx.chemistrySubjectId,
      }),
    );
    await waitFor(() => expect(result.current.rows.data?.[0]?.subjectId).toBe(fx.chemistrySubjectId));
  });

  it('reads the salary rows of one staff member, latest first', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(() => useStaffSalaries({ staffId: fx.teacherId }), { wrapper });
    await waitFor(() => expect(result.current.data?.items).toHaveLength(1));
    const read = api.calls.find((c) => c.path === '/v1/payroll/staff-salaries');
    expect(new URLSearchParams(read?.search).getAll('sort')).toEqual(['effectiveDate,desc']);
    expect(new URLSearchParams(read?.search).get('filter')).toBe(`staffId = "${fx.teacherId}"`);
  });
});

describe('leave balances', () => {
  it('lists a year, opens an entitlement once and never sets it below the days used', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        list: useLeaveBalances({ academicYearId: fx.academicYearId }),
        create: useCreateLeaveBalance(),
        update: useUpdateLeaveBalance(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.list.data?.items).toHaveLength(1));
    const opening = {
      staffId: fx.colleagueId,
      leaveTypeId: fx.casualId,
      academicYearId: fx.academicYearId,
      entitledDays: '10',
    };
    await act(() => result.current.create.mutateAsync(opening));
    await waitFor(() => expect(result.current.list.data?.items).toHaveLength(2));
    const twice = await result.current.create.mutateAsync(opening).catch((e: unknown) => e);
    expect(isApiError(twice) && twice.status).toBe(409);
    const below = await result.current.update
      .mutateAsync({
        leaveBalanceId: fx.teacherCasualBalanceId,
        etag: '"1"',
        entitledDays: '2',
        carriedForwardDays: '',
      })
      .catch((e: unknown) => e);
    expect(isApiError(below) && below.errors[0]?.field).toBe('entitledDays');
    await act(() =>
      result.current.update.mutateAsync({
        leaveBalanceId: fx.teacherCasualBalanceId,
        etag: '"1"',
        entitledDays: '14',
        carriedForwardDays: '2',
      }),
    );
    await waitFor(() =>
      expect(
        result.current.list.data?.items.find((b) => b.id === fx.teacherCasualBalanceId)?.entitledDays,
      ).toBe('14'),
    );
  });
});
