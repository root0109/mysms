import {
  ATTENDANCE_TEACHER,
  attendanceFixture,
  fakeAttendance,
  fakeRecords,
  schoolAdmin,
  SELF_SERVICE,
} from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const { sheetId, students, reasons, institutionId } = attendanceFixture;
const [asha, bilal, chitra] = students;
const teacher = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...ATTENDANCE_TEACHER] };
const without = (...codes: string[]) => ({
  ...teacher,
  permissions: teacher.permissions.filter((code) => !codes.includes(code)),
});
const sheetPath = `/app/attendance/attendance-sheets/${sheetId}`;
const sheetApi = `/v1/attendance/attendance-sheets/${sheetId}`;
const row = (name: string) => screen.getByText(name).closest('li') as HTMLElement;
const mark = (name: string, status: string) =>
  userEvent.click(within(screen.getByRole('radiogroup', { name })).getByRole('radio', { name: status }));

describe('ATT-03 Take attendance', () => {
  it('marks the class roster with one tap each, saves the changes together and completes the sheet', async () => {
    const { api } = await signedIn(
      sheetPath,
      teacher,
      fakeAttendance({ marks: [{ studentId: asha.id, status: 'present' }] }),
    );
    expect(await screen.findByRole('heading', { name: 'Take attendance' })).toBeInTheDocument();
    expect(await screen.findByText(/Grade 5 A · 9 Mar 2026/)).toBeInTheDocument();
    expect(screen.getByText('In progress')).toBeInTheDocument();
    await screen.findByText('Chitra Nair');
    // roll numbers order the class: 1, 2, 10
    expect(screen.getAllByRole('radiogroup').map((group) => group.getAttribute('aria-label'))).toEqual([
      'Chitra Nair',
      'Asha Rao',
      'Bilal Khan',
    ]);
    expect(within(row('Bilal Khan')).getByText('Roll 10')).toBeInTheDocument();
    expect(screen.getByText(/2 students are not marked yet/)).toBeInTheDocument();
    expect(
      within(screen.getByRole('radiogroup', { name: 'Asha Rao' })).getByRole('radio', { name: 'Present' }),
    ).toBeChecked();
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Complete' })).toBeDisabled();

    await mark('Bilal Khan', 'Absent');
    const reason = within(row('Bilal Khan')).getByRole('combobox', { name: 'Reason' });
    expect(within(reason).queryByRole('option', { name: 'Retired reason' })).not.toBeInTheDocument();
    await userEvent.selectOptions(reason, reasons[0].id);
    await userEvent.type(within(row('Bilal Khan')).getByLabelText('Remark'), 'Fever');
    await userEvent.click(screen.getByRole('button', { name: 'Mark the rest present' }));
    expect(screen.getByText(/2 present · 1 absent · 0 late · 0 excused/)).toBeInTheDocument();
    expect(screen.getByText('2 unsaved changes')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Attendance saved.')).toBeInTheDocument();
    const saved = api.calls.find((c) => c.path === `${sheetApi}:recordAttendance`);
    expect(saved?.headers.get('Idempotency-Key')).toBeTruthy();
    expect(JSON.parse(saved?.body ?? '{}')).toEqual({
      marks: [
        { studentId: chitra.id, status: 'present' },
        { studentId: bilal.id, status: 'absent', absenceReasonId: reasons[0].id, remark: 'Fever' },
      ],
      etag: '"1"',
    });
    expect(screen.queryByText(/unsaved/)).not.toBeInTheDocument();

    // nothing left to save: completing is the workflow step, under the etag the screen last read
    await userEvent.click(screen.getByRole('button', { name: 'Complete' }));
    expect(await screen.findByText('The sheet is completed.')).toBeInTheDocument();
    const completed = api.calls.find((c) => c.path === `${sheetApi}:complete`);
    expect(JSON.parse(completed?.body ?? '{}')).toEqual({ etag: '"2"' });
    expect(await screen.findByText('Completed')).toBeInTheDocument();
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    expect(within(row('Bilal Khan')).getByText('Absent')).toBeInTheDocument();
    expect(within(row('Bilal Khan')).getByText('Sick · Fever')).toBeInTheDocument();
  });

  it('saves and completes in one call when marks are still unsaved', async () => {
    const { api } = await signedIn(
      sheetPath,
      teacher,
      fakeAttendance({ marks: students.map((student) => ({ studentId: student.id, status: 'present' })) }),
    );
    await screen.findByText('Asha Rao');
    await mark('Asha Rao', 'Late');
    await userEvent.click(screen.getByRole('button', { name: 'Save and complete' }));
    expect(await screen.findByText('The sheet is completed.')).toBeInTheDocument();
    const call = api.calls.find((c) => c.path === `${sheetApi}:recordAttendance`);
    expect(JSON.parse(call?.body ?? '{}')).toEqual({
      marks: [{ studentId: asha.id, status: 'late' }],
      complete: true,
      etag: '"1"',
    });
    expect(api.calls.some((c) => c.path === `${sheetApi}:complete`)).toBe(false);
  });

  it('shows a refused mark on its student, saves nothing and keeps what was entered', async () => {
    await signedIn(
      sheetPath,
      teacher,
      fakeAttendance({ marks: students.map((student) => ({ studentId: student.id, status: 'present' })) }),
    );
    await screen.findByText('Asha Rao');
    await mark('Chitra Nair', 'Late');
    await mark('Asha Rao', 'Excused');
    await userEvent.type(within(row('Asha Rao')).getByLabelText('Remark'), 'no!');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText(/Some marks were not accepted/)).toBeInTheDocument();
    expect(within(row('Asha Rao')).getByText('remark is not acceptable')).toBeInTheDocument();
    expect(within(row('Chitra Nair')).queryByText('remark is not acceptable')).not.toBeInTheDocument();
    expect(within(row('Asha Rao')).getByLabelText('Remark')).toHaveValue('no!');
    expect(screen.getByText('2 unsaved changes')).toBeInTheDocument();
  });

  it('starts a sheet that has not been started and reopens a completed one, each under its etag', async () => {
    const { api } = await signedIn(sheetPath, teacher, fakeAttendance({ status: 'draft' }));
    expect(await screen.findByText(/has not been started/)).toBeInTheDocument();
    await screen.findByText('Asha Rao');
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    expect(screen.getAllByText('Not marked')).toHaveLength(3);
    await userEvent.click(screen.getByRole('button', { name: 'Start' }));
    expect(await screen.findByText('The sheet is started.')).toBeInTheDocument();
    expect(JSON.parse(api.calls.find((c) => c.path === `${sheetApi}:start`)?.body ?? '{}')).toEqual({
      etag: '"1"',
    });
    expect(await screen.findAllByRole('radiogroup')).toHaveLength(3);

    await userEvent.click(screen.getByRole('button', { name: 'Mark the rest present' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save and complete' }));
    expect(await screen.findByText(/Its marks count in the summaries/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Reopen' }));
    expect(await screen.findByText(/The sheet is reopened/)).toBeInTheDocument();
    expect(api.calls.some((c) => c.path === `${sheetApi}:resetToDraft`)).toBe(true);
    // reopening keeps the marks
    expect(screen.getAllByText('Present')).toHaveLength(3);
  });

  it("lists only the marks of a sheet opened for a session, and a hand-made sheet's class as placed that day", async () => {
    // an elective session expects one student of the class: the other two are not on its sheet
    const elective = await signedIn(
      sheetPath,
      teacher,
      fakeAttendance({ forSession: true, marks: [{ studentId: bilal.id, status: 'present' }] }),
    );
    expect(await screen.findByRole('radiogroup', { name: 'Bilal Khan' })).toBeInTheDocument();
    expect(screen.getAllByRole('radiogroup')).toHaveLength(1);
    expect(within(row('Bilal Khan')).getByText('Roll 10')).toBeInTheDocument();
    expect(screen.queryByText(/not marked yet/)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Complete' })).toBeEnabled();
    elective.unmount();

    // a sheet made by hand for 9 March: a student placed from 10 March, or until 8 March, is not expected
    await signedIn(
      sheetPath,
      teacher,
      fakeAttendance({
        placed: { [asha.id]: { effectiveFrom: '2026-03-10' }, [chitra.id]: { effectiveTo: '2026-03-08' } },
      }),
    );
    expect(await screen.findByRole('radiogroup', { name: 'Bilal Khan' })).toBeInTheDocument();
    expect(screen.getAllByRole('radiogroup')).toHaveLength(1);
  });

  it('does not save over marks changed on another device: it says so and keeps what was entered', async () => {
    const backend = fakeAttendance({
      marks: students.map((student) => ({ studentId: student.id, status: 'present' })),
    });
    const { api } = await signedIn(sheetPath, teacher, backend);
    await screen.findByText('Asha Rao');
    // another device saves a mark: the sheet moves to a new version this screen has not read
    await api.fetch(
      new Request(`https://school.example.com${sheetApi}:recordAttendance`, {
        method: 'POST',
        body: JSON.stringify({ marks: [{ studentId: bilal.id, status: 'absent' }] }),
      }),
    );
    await mark('Asha Rao', 'Late');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText(/This record was updated after you opened it/)).toBeInTheDocument();
    // the other device's mark is shown now, and this teacher's own change is still there to save
    await waitFor(() =>
      expect(
        within(screen.getByRole('radiogroup', { name: 'Bilal Khan' })).getByRole('radio', { name: 'Absent' }),
      ).toBeChecked(),
    );
    expect(
      within(screen.getByRole('radiogroup', { name: 'Asha Rao' })).getByRole('radio', { name: 'Late' }),
    ).toBeChecked();
    expect(screen.getByText('1 unsaved change')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Attendance saved.')).toBeInTheDocument();
  });

  it('is read-only for a role that may not record, and offers no step the role does not hold', async () => {
    await signedIn(
      sheetPath,
      without(
        'attendance.attendance_sheet.record_attendance',
        'attendance.attendance_sheet.complete_attendance_sheet',
      ),
      fakeAttendance({ marks: [{ studentId: asha.id, status: 'late' }] }),
    );
    expect(await screen.findByText(/may view this sheet but not mark attendance/)).toBeInTheDocument();
    expect(await screen.findByText('Late')).toBeInTheDocument();
    expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /omplete/ })).not.toBeInTheDocument();
  });

  it('asks before leaving with unsaved marks', async () => {
    const { router } = await signedIn(sheetPath, teacher, fakeAttendance());
    await screen.findByText('Asha Rao');
    await mark('Asha Rao', 'Absent');
    await userEvent.click(screen.getByRole('link', { name: 'All sheets' }));
    const dialog = await screen.findByRole('dialog', { name: 'Leave without saving?' });
    expect(router.state.location.pathname).toBe(sheetPath);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Stay' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(
      within(screen.getByRole('radiogroup', { name: 'Asha Rao' })).getByRole('radio', { name: 'Absent' }),
    ).toBeChecked();
    await userEvent.click(screen.getByRole('link', { name: 'All sheets' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Leave' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/app/attendance/attendance-sheets'));
  });

  it('tells a role without access so and reads nothing', async () => {
    const { api } = await signedIn(sheetPath, without('attendance.attendance_sheet.read'), fakeAttendance());
    expect(await screen.findByText('Your role may not open attendance sheets.')).toBeInTheDocument();
    expect(api.calls.some((c) => c.path.startsWith('/v1/attendance/'))).toBe(false);
  });

  it('treats an unknown sheet as not available', async () => {
    await signedIn(
      '/app/attendance/attendance-sheets/01960000-0000-7000-8000-00000000dead',
      teacher,
      fakeAttendance(),
    );
    expect(await screen.findByRole('link', { name: 'All sheets' })).toBeInTheDocument();
    expect(screen.queryByText('Asha Rao')).not.toBeInTheDocument();
  });
});

describe('ATT-02 Sheets', () => {
  it('links every sheet to its own screen without opening the editor', async () => {
    const sheets = fakeRecords({
      collection: '/v1/attendance/attendance-sheets',
      item: '/v1/attendance/attendance-sheets/{attendanceSheetId}',
      required: ['registerId', 'attendanceDate'],
      rows: [{ id: sheetId, reference: 'SH-0001', attendanceDate: '2026-03-09', status: 'in_progress' }],
    });
    const { router } = await signedIn('/app/attendance/attendance-sheets', teacher, {
      ...sheets,
      ...fakeAttendance(),
    });
    await userEvent.click(await screen.findByRole('link', { name: /^Open / }));
    await waitFor(() => expect(router.state.location.pathname).toBe(sheetPath));
    expect(await screen.findByRole('heading', { name: 'Take attendance' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

describe('TT-02 Sessions', () => {
  const sessions = (sessionId: string) =>
    fakeRecords({
      collection: '/v1/timetable/sessions',
      item: '/v1/timetable/sessions/{sessionId}',
      required: ['institutionId', 'startAt', 'endAt'],
      rows: [
        {
          id: sessionId,
          startAt: '2026-03-09T04:00:00Z',
          endAt: '2026-03-09T04:45:00Z',
          status: 'confirmed',
        },
      ],
    });
  const timetabler = { ...teacher, permissions: [...teacher.permissions, 'timetable.session.read'] };

  it('opens the attendance sheet of a session and goes to it', async () => {
    const { api, router } = await signedIn('/app/timetable/sessions', timetabler, {
      ...sessions(attendanceFixture.sessionId),
      ...fakeAttendance({ forSession: true, marks: [{ studentId: asha.id, status: 'present' }] }),
    });
    await userEvent.click(await screen.findByRole('button', { name: 'Take attendance' }));
    await waitFor(() => expect(router.state.location.pathname).toBe(sheetPath));
    expect(await screen.findByRole('radiogroup', { name: 'Asha Rao' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    const call = api.calls.find((c) => c.path === '/v1/attendance/attendance-sheets:forSession');
    expect(JSON.parse(call?.body ?? '{}')).toEqual({ timetableSessionId: attendanceFixture.sessionId });
    expect(call?.headers.get('Idempotency-Key')).toBeTruthy();
  });

  it('says why a session has no sheet, and offers nothing to a role that may not take attendance', async () => {
    const other = '01960000-0000-7000-8000-0000000000ff';
    const first = await signedIn('/app/timetable/sessions', timetabler, {
      ...sessions(other),
      ...fakeAttendance(),
    });
    await userEvent.click(await screen.findByRole('button', { name: 'Take attendance' }));
    expect(await screen.findByText('No active attendance register covers this session.')).toBeInTheDocument();
    expect(first.router.state.location.pathname).toBe('/app/timetable/sessions');
    first.unmount();

    await signedIn(
      '/app/timetable/sessions',
      {
        ...timetabler,
        permissions: timetabler.permissions.filter((code) => !code.includes('take_attendance')),
      },
      { ...sessions(other), ...fakeAttendance() },
    );
    await screen.findByRole('table');
    await screen.findByText(/1 item/);
    expect(screen.queryByRole('button', { name: 'Take attendance' })).not.toBeInTheDocument();
  });
});

describe('ATT-04 Student attendance summary', () => {
  const summaries = [
    { date: '2026-03-02', present: 0, absent: 1, late: 0, excused: 0, attendancePercent: '0.00' },
    { date: '2026-03-03', present: 1, absent: 0, late: 0, excused: 0, attendancePercent: '100.00' },
    { date: '2026-03-04', present: 0, absent: 0, late: 1, excused: 0, attendancePercent: '100.00' },
  ];

  it('shows the month as a calendar of marks with the percentage', async () => {
    const { api } = await signedIn(
      `/app/attendance/summary/${asha.id}`,
      teacher,
      fakeAttendance({ summaries }),
    );
    expect(await screen.findByRole('heading', { name: 'Asha Rao' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-03' } });
    expect(await screen.findByText('66.67% attendance · 3 days recorded')).toBeInTheDocument();
    expect(screen.getByText('1 present · 1 absent · 1 late · 0 excused')).toBeInTheDocument();
    const calendar = screen.getByRole('group', { name: 'Attendance for March 2026' });
    expect(within(calendar).getAllByRole('listitem')).toHaveLength(31);
    expect(within(calendar).getByText('2 Mar 2026: Absent')).toBeInTheDocument();
    expect(within(calendar).getByText('4 Mar 2026: Late')).toBeInTheDocument();
    expect(within(calendar).getByText('5 Mar 2026: no attendance recorded')).toBeInTheDocument();
    // 1 March 2026 is a Sunday: the last column of a Monday-first week
    expect(within(calendar).getAllByRole('listitem')[0]).toHaveStyle({ gridColumnStart: '7' });
    const read = api.calls.filter((c) => c.path === '/v1/attendance/summaries').pop();
    const query = new URLSearchParams(read?.search);
    expect(Object.fromEntries(query)).toMatchObject({
      institutionId,
      studentId: asha.id,
      groupBy: 'date',
      fromDate: '2026-03-01',
      toDate: '2026-03-31',
    });
  });

  it('says so when the month has no completed attendance, and when the student is not available', async () => {
    const { router } = await signedIn(`/app/attendance/summary/${asha.id}`, teacher, fakeAttendance());
    expect(await screen.findByText('No completed attendance this month.')).toBeInTheDocument();
    await router.navigate('/app/attendance/summary/01960000-0000-7000-8000-00000000dead');
    expect(await screen.findByText('This student is not available to your role.')).toBeInTheDocument();
  });

  it('is not offered to a role without the summary permission', async () => {
    await signedIn(
      `/app/attendance/summary/${asha.id}`,
      without('attendance.attendance_record.summarize_attendance'),
      fakeAttendance({ summaries }),
    );
    expect(await screen.findByText('Your role may not view attendance summaries.')).toBeInTheDocument();
  });
});

describe('ATT-05 Class summary & defaulters', () => {
  const summaries = [
    { studentId: asha.id, present: 8, absent: 2, late: 0, excused: 0, attendancePercent: '80.00' },
    { studentId: bilal.id, present: 5, absent: 4, late: 1, excused: 0, attendancePercent: '60.00' },
    { studentId: chitra.id, present: 10, absent: 0, late: 0, excused: 0, attendancePercent: '100.00' },
  ];

  it('lists students lowest first, marks those below the minimum and opens a student', async () => {
    const { api, router } = await signedIn('/app/attendance/summary', teacher, fakeAttendance({ summaries }));
    expect(await screen.findByRole('heading', { name: 'Class summary & defaulters' })).toBeInTheDocument();
    expect(await screen.findByRole('combobox', { name: 'Institution' })).toHaveValue(institutionId);
    const table = await screen.findByRole('table');
    await within(table).findByText('Bilal Khan');
    const names = within(table)
      .getAllByRole('row')
      .slice(1)
      .map((line) => within(line).getAllByRole('cell')[0]?.textContent);
    expect(names).toEqual(['Bilal Khan', 'Asha Rao', 'Chitra Nair']);
    expect(screen.getByText('1 student is below 75%.')).toBeInTheDocument();
    expect(within(table).getAllByText('Below minimum')).toHaveLength(1);
    expect(within(table).getByText('60%')).toBeInTheDocument();
    const query = new URLSearchParams(api.calls.find((c) => c.path === '/v1/attendance/summaries')?.search);
    expect(query.get('groupBy')).toBe('student');
    expect(query.get('institutionId')).toBe(institutionId);
    expect(query.has('classSectionId')).toBe(false);

    const minimum = screen.getByLabelText('Minimum attendance (%)');
    await userEvent.clear(minimum);
    expect(screen.getByText('Enter a number from 0 to 100.')).toBeInTheDocument();
    await userEvent.type(minimum, '90');
    expect(screen.getByText('2 students are below 90%.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('switch', { name: 'Show only students below the minimum' }));
    expect(within(table).getAllByRole('row')).toHaveLength(3);
    expect(within(table).queryByText('Chitra Nair')).not.toBeInTheDocument();

    await userEvent.click(within(table).getByText('Bilal Khan'));
    await waitFor(() => expect(router.state.location.pathname).toBe(`/app/attendance/summary/${bilal.id}`));
  });

  it('refuses a range that ends before it starts without asking the server', async () => {
    const { api } = await signedIn('/app/attendance/summary', teacher, fakeAttendance({ summaries }));
    await screen.findByRole('table');
    const before = api.calls.filter((c) => c.path === '/v1/attendance/summaries').length;
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-03-10' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-03-01' } });
    expect(await screen.findByText(/Choose a range of at most 366 days/)).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(api.calls.filter((c) => c.path === '/v1/attendance/summaries').length).toBeLessThanOrEqual(
      before + 1,
    );
  });

  it('appears in the navigation only for roles that may summarise attendance', async () => {
    await signedIn('/app/attendance/summary', without('attendance.attendance_record.summarize_attendance'));
    expect(await screen.findByText('Your role may not view attendance summaries.')).toBeInTheDocument();
    const nav = screen.getByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).queryByRole('link', { name: 'Class summary & defaulters' })).not.toBeInTheDocument();
  });
});
