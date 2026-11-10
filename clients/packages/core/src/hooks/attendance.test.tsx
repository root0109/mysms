import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { attendanceFixture, fakeApi, fakeAttendance, json, onePage, type Handler } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  nextDay,
  useAbsenceReasons,
  useAttendanceMarks,
  useAttendanceRegister,
  useAttendanceSheet,
  useAttendanceSheetStep,
  useAttendanceSummary,
  useClassRoster,
  useClassSectionOptions,
  useInstitutions,
  useRecordAttendance,
  useStudent,
  useStudentNames,
  useTakeAttendanceForSession,
} from './attendance';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: 'u1', username: 'asha', locale: 'en-IN' };
const { sheetId, registerId, classSectionId, institutionId, students, date } = attendanceFixture;
const [asha, bilal, chitra] = students;
const SHEET = `/v1/attendance/attendance-sheets/${sheetId}`;

async function setup(handlers: Record<string, Handler>) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    ...handlers,
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

describe('nextDay', () => {
  it('crosses month and year ends', () => {
    expect(nextDay('2026-03-09')).toBe('2026-03-10');
    expect(nextDay('2026-03-31')).toBe('2026-04-01');
    expect(nextDay('2026-12-31')).toBe('2027-01-01');
    expect(nextDay('2028-02-28')).toBe('2028-02-29');
  });
});

describe('taking attendance (ATT-03)', () => {
  it('reads the sheet with its etag, its register, marks, roster, reasons and names once their ids are known', async () => {
    const { api, wrapper } = await setup(fakeAttendance({ marks: [{ studentId: asha.id, status: 'late' }] }));
    const { result, rerender } = renderHook(
      ({ on }: { on: boolean }) => ({
        sheet: useAttendanceSheet(on ? sheetId : null),
        register: useAttendanceRegister(on ? registerId : null),
        marks: useAttendanceMarks(on ? sheetId : null, on ? date : null),
        roster: useClassRoster(on ? classSectionId : null),
        reasons: useAbsenceReasons(on),
        names: useStudentNames(on ? [asha.id, bilal.id, asha.id, 'unknown'] : []),
      }),
      { wrapper, initialProps: { on: false } },
    );
    expect(result.current.sheet.fetchStatus).toBe('idle');
    expect(result.current.marks.fetchStatus).toBe('idle');
    expect(result.current.names.size).toBe(0);
    expect(
      api.calls.filter((c) => c.path.startsWith('/v1/attendance') || c.path.startsWith('/v1/sis')),
    ).toHaveLength(0);

    rerender({ on: true });
    await waitFor(() => expect(result.current.sheet.data?.etag).toBe('"1"'));
    await waitFor(() => expect(result.current.sheet.data?.data.status).toBe('in_progress'));
    await waitFor(() => expect(result.current.register.data?.classSectionId).toBe(classSectionId));
    await waitFor(() => expect(result.current.marks.data?.items.map((m) => m.status)).toEqual(['late']));
    await waitFor(() => expect(result.current.roster.data?.items).toHaveLength(3));
    // retired reasons are not offered
    await waitFor(() =>
      expect(result.current.reasons.data?.items.map((r) => r.title)).toEqual(['Sick', 'Family event']),
    );
    await waitFor(() =>
      expect([...result.current.names]).toEqual([
        [asha.id, 'Asha Rao'],
        [bilal.id, 'Bilal Khan'],
      ]),
    );
    // the partitioned list is read for the sheet's own day
    const marks = api.calls.find((c) => c.path === `${SHEET}/records`);
    expect(new URLSearchParams(marks?.search).get('filter')).toBe(
      'attendanceDate >= "2026-03-09" AND attendanceDate < "2026-03-10"',
    );
    // names: one list read per module for the whole (deduplicated) group
    const studentReads = api.calls.filter((c) => c.path === '/v1/sis/students');
    expect(studentReads).toHaveLength(1);
    expect(new URLSearchParams(studentReads[0]?.search).get('filter')).toBe(
      `id = "${asha.id}" OR id = "${bilal.id}" OR id = "unknown"`,
    );
    expect(api.calls.filter((c) => c.path === '/v1/people/persons')).toHaveLength(1);
  });

  it('reads names forty students at a time and none when no student is visible to the caller', async () => {
    const { api, wrapper } = await setup(fakeAttendance());
    const many = Array.from({ length: 45 }, (_, index) => `student-${index}`);
    const { result } = renderHook(() => useStudentNames(many), { wrapper });
    await waitFor(() => expect(api.calls.filter((c) => c.path === '/v1/sis/students')).toHaveLength(2));
    const sizes = api.calls
      .filter((c) => c.path === '/v1/sis/students')
      .map((c) => (new URLSearchParams(c.search).get('filter') ?? '').split(' OR ').length);
    expect(sizes).toEqual([40, 5]);
    expect(result.current.size).toBe(0);
    // no student came back, so no person is asked for
    expect(api.calls.some((c) => c.path === '/v1/people/persons')).toBe(false);
  });

  it('records marks and has the fresh sheet and marks when the mutation settles', async () => {
    const { api, wrapper } = await setup(fakeAttendance());
    const { result } = renderHook(
      () => ({
        sheet: useAttendanceSheet(sheetId),
        marks: useAttendanceMarks(sheetId, date),
        record: useRecordAttendance(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.marks.data?.items).toEqual([]));
    await act(() =>
      result.current.record.mutateAsync({
        sheetId,
        marks: [{ studentId: asha.id, status: 'absent', remark: 'Fever' }],
      }),
    );
    await waitFor(() =>
      expect(result.current.marks.data?.items.map((m) => [m.studentId, m.status])).toEqual([
        [asha.id, 'absent'],
      ]),
    );
    await waitFor(() => expect(result.current.sheet.data?.etag).toBe('"2"'));
    const post = api.calls.find((c) => c.path === `${SHEET}:recordAttendance`);
    expect(JSON.parse(post?.body ?? '{}')).toEqual({
      marks: [{ studentId: asha.id, status: 'absent', remark: 'Fever' }],
    });
    expect(post?.headers.get('Idempotency-Key')).toBeTruthy();

    await act(() =>
      result.current.record.mutateAsync({
        sheetId,
        marks: [{ studentId: chitra.id, status: 'present' }],
        complete: true,
      }),
    );
    await waitFor(() => expect(result.current.sheet.data?.data.status).toBe('done'));
    expect(
      JSON.parse(api.calls.filter((c) => c.path === `${SHEET}:recordAttendance`)[1]?.body ?? '{}'),
    ).toMatchObject({
      complete: true,
    });
  });

  it('moves the sheet through its steps under the etag and surfaces a stale one as 412', async () => {
    const { api, wrapper } = await setup(
      fakeAttendance({ status: 'draft', marks: [{ studentId: asha.id, status: 'present' }] }),
    );
    const { result } = renderHook(
      () => ({ sheet: useAttendanceSheet(sheetId), step: useAttendanceSheetStep() }),
      {
        wrapper,
      },
    );
    await waitFor(() => expect(result.current.sheet.data?.etag).toBe('"1"'));
    await act(() => result.current.step.mutateAsync({ sheetId, step: 'start', etag: '"1"' }));
    await waitFor(() => expect(result.current.sheet.data?.data.status).toBe('in_progress'));

    const stale = await act(() =>
      result.current.step
        .mutateAsync({ sheetId, step: 'complete', etag: '"1"' })
        .catch((error: unknown) => error),
    );
    expect(isApiError(stale) && stale.status).toBe(412);
    await waitFor(() => expect(result.current.sheet.data?.data.status).toBe('in_progress'));

    await act(() => result.current.step.mutateAsync({ sheetId, step: 'complete', etag: '"2"' }));
    await waitFor(() => expect(result.current.sheet.data?.data.status).toBe('done'));
    // without an etag the step is unconditional
    await act(() => result.current.step.mutateAsync({ sheetId, step: 'reset', etag: null }));
    await waitFor(() => expect(result.current.sheet.data?.data.status).toBe('draft'));
    expect(
      api.calls.filter((c) => c.method === 'POST' && c.path.startsWith(SHEET)).map((c) => c.body),
    ).toEqual(['{"etag":"\\"1\\""}', '{"etag":"\\"1\\""}', '{"etag":"\\"2\\""}', '{}']);
  });
});

describe('the sheet etag on recording, and the sheet of a session', () => {
  it('sends the etag it is given and surfaces marks saved elsewhere as 412', async () => {
    const { api, wrapper } = await setup(fakeAttendance());
    const { result } = renderHook(() => useRecordAttendance(), { wrapper });
    await act(() =>
      result.current.mutateAsync({ sheetId, marks: [{ studentId: asha.id, status: 'late' }], etag: '"1"' }),
    );
    expect(JSON.parse(api.calls.find((c) => c.path === `${SHEET}:recordAttendance`)?.body ?? '{}')).toEqual({
      marks: [{ studentId: asha.id, status: 'late' }],
      etag: '"1"',
    });
    const stale = await act(() =>
      result.current
        .mutateAsync({ sheetId, marks: [{ studentId: bilal.id, status: 'absent' }], etag: '"1"' })
        .catch((error: unknown) => error),
    );
    expect(isApiError(stale) && stale.status).toBe(412);
  });

  it('opens the sheet of a timetable session', async () => {
    const { api, wrapper } = await setup(fakeAttendance({ forSession: true }));
    const { result } = renderHook(() => useTakeAttendanceForSession(), { wrapper });
    const sheet = await act(() => result.current.mutateAsync(attendanceFixture.sessionId));
    expect(sheet.id).toBe(sheetId);
    expect(sheet.timetableSessionId).toBe(attendanceFixture.sessionId);
    const post = api.calls.find((c) => c.path === '/v1/attendance/attendance-sheets:forSession');
    expect(JSON.parse(post?.body ?? '{}')).toEqual({ timetableSessionId: attendanceFixture.sessionId });
  });
});

describe('attendance summaries (ATT-04, ATT-05)', () => {
  const summaries = [
    { studentId: asha.id, present: 8, absent: 2, late: 0, excused: 0, attendancePercent: '80.00' },
  ];

  it('asks for a summary only once its range is known, with the grouping and filters given', async () => {
    const { api, wrapper } = await setup(fakeAttendance({ summaries }));
    const params = {
      institutionId,
      fromDate: '2026-03-01',
      toDate: '2026-03-31',
      groupBy: 'student' as const,
      classSectionId,
    };
    const { result, rerender } = renderHook(
      ({ on }: { on: boolean }) => ({
        summary: useAttendanceSummary(on ? params : null),
        student: useStudent(on ? asha.id : null),
        institutions: useInstitutions(on),
      }),
      { wrapper, initialProps: { on: false } },
    );
    expect(result.current.summary.fetchStatus).toBe('idle');
    expect(result.current.student.fetchStatus).toBe('idle');
    expect(result.current.institutions.fetchStatus).toBe('idle');
    rerender({ on: true });
    await waitFor(() => expect(result.current.summary.data?.items).toEqual(summaries));
    await waitFor(() => expect(result.current.student.data?.institutionId).toBe(institutionId));
    await waitFor(() =>
      expect(result.current.institutions.data?.items.map((i) => i.name)).toEqual(['Main School']),
    );
    const read = api.calls.find((c) => c.path === '/v1/attendance/summaries');
    expect(Object.fromEntries(new URLSearchParams(read?.search))).toEqual({
      institutionId,
      fromDate: '2026-03-01',
      toDate: '2026-03-31',
      groupBy: 'student',
      classSectionId,
      size: '500',
      page: '1',
    });
  });

  it('names the active classes of an institution by grade and section, lowest grade first', async () => {
    const page = (items: unknown[]) => (req: Request) => onePage(req, items);
    const { api, wrapper } = await setup({
      'GET /v1/academics/class-sections': page([
        {
          id: 'c2',
          institutionId,
          academicYearId: 'y',
          gradeLevelId: 'g6',
          sectionId: 'sa',
          stream: 'Science',
        },
        { id: 'c1', institutionId, academicYearId: 'y', gradeLevelId: 'g5', sectionId: 'sb' },
        { id: 'c0', institutionId, academicYearId: 'y', gradeLevelId: 'g5', sectionId: 'sa' },
        { id: 'c9', institutionId, academicYearId: 'y', gradeLevelId: 'gone', sectionId: 'sa' },
      ]),
      'GET /v1/academics/grade-levels': page([
        { id: 'g6', institutionId, name: 'Grade 6', slug: 'g6', sortOrder: 6 },
        { id: 'g5', institutionId, name: 'Grade 5', slug: 'g5', sortOrder: 5 },
      ]),
      'GET /v1/academics/sections': page([
        { id: 'sa', institutionId, name: 'A' },
        { id: 'sb', institutionId, name: 'B' },
      ]),
    });
    const { result, rerender } = renderHook(({ id }: { id: string | null }) => useClassSectionOptions(id), {
      wrapper,
      initialProps: { id: null as string | null },
    });
    expect(result.current.fetchStatus).toBe('idle');
    rerender({ id: institutionId });
    await waitFor(() =>
      expect(result.current.data).toEqual([
        { id: 'c0', label: 'Grade 5 A' },
        { id: 'c1', label: 'Grade 5 B' },
        { id: 'c2', label: 'Grade 6 A Science' },
      ]),
    );
    const classes = api.calls.find((c) => c.path === '/v1/academics/class-sections');
    expect(new URLSearchParams(classes?.search).get('filter')).toBe(
      `institutionId = "${institutionId}" AND isActive = true`,
    );
  });
});
