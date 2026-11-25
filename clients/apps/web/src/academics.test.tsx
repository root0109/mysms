import {
  ACADEMICS_ADMIN,
  SELF_SERVICE,
  academicsFixture,
  fakeAcademics,
  schoolAdmin,
  timetableFixture,
} from '@mysms/fixtures';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const admin = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...ACADEMICS_ADMIN] };

describe('ACA-06 Class section workspace', () => {
  it('names the class and shows its students, subjects with teachers, week and homework', async () => {
    const { router } = await signedIn(
      `/app/academics/class-sections/${academicsFixture.classSectionId}`,
      admin,
      fakeAcademics(),
    );
    expect(await screen.findByRole('heading', { name: 'Grade 5 A' })).toBeInTheDocument();
    expect(screen.getByText('Room for 40 students')).toBeInTheDocument();
    // students by roll number, each opening their page
    const rows = await screen.findAllByRole('link', { name: /Asha Rao|Bilal Khan|Chitra Nair/ });
    expect(rows.map((a) => a.textContent)).toEqual(['Chitra Nair', 'Asha Rao', 'Bilal Khan']);
    await userEvent.click(screen.getByRole('radio', { name: 'Subjects & teachers' }));
    expect(await screen.findByText('Kavita Iyer (6 a week)')).toBeInTheDocument();
    // an inactive assignment does not count; a subject without a teacher says so
    expect(screen.queryByText(/Anil Das/)).not.toBeInTheDocument();
    expect(screen.getByText('No teacher assigned')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('radio', { name: 'Timetable' }));
    await waitFor(() => expect(router.state.location.search).toContain('tab=timetable'));
    await userEvent.click(screen.getByRole('radio', { name: 'Homework' }));
    expect(await screen.findByText('Science')).toBeInTheDocument();
  });

  it('says a class the server does not show is not available', async () => {
    await signedIn(`/app/academics/class-sections/${academicsFixture.courseId}`, admin, fakeAcademics());
    expect(await screen.findByText(/This item is not available/)).toBeInTheDocument();
  });
});

describe('ACA-02 Courses & subjects', () => {
  it('opens a course in place with its subjects named', async () => {
    await signedIn(`/app/academics/courses?course=${academicsFixture.courseId}`, admin, fakeAcademics());
    expect(await screen.findByRole('heading', { name: 'B.Sc. Physics' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('radio', { name: 'Subjects' }));
    expect(await screen.findByRole('heading', { name: 'Subjects of B.Sc. Physics' })).toBeInTheDocument();
    expect(await screen.findByText('Mathematics')).toBeInTheDocument();
    expect(screen.queryByText(timetableFixture.subjects[0].id)).not.toBeInTheDocument();
  });
});

describe('ACA-04 Grade levels & sections', () => {
  it('lists both catalogues on one page', async () => {
    await signedIn('/app/academics/grade-levels', admin, fakeAcademics());
    expect(await screen.findByRole('heading', { name: 'Grade levels' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Sections' })).toBeInTheDocument();
    expect(await screen.findByText('Grade 5')).toBeInTheDocument();
  });
});

describe('TT-04 Room/staff view', () => {
  it('shows the week of a staff member, with the room of each session', async () => {
    const { api } = await signedIn(
      `/app/timetable/sessions?view=room&by=staff&week=${timetableFixture.week}`,
      admin,
      fakeAcademics(),
    );
    await userEvent.selectOptions(
      await screen.findByRole('combobox', { name: 'Staff member' }),
      await screen.findByRole('option', { name: 'Kavita Iyer' }),
    );
    expect(await screen.findAllByText('Room 12')).not.toHaveLength(0);
    const read = api.calls.find((c) => c.path === '/v1/timetable/sessions:timetable');
    expect(new URLSearchParams(read?.search).get('staffId')).toBe(academicsFixture.teacherId);
    // the exited staff member is not offered
    expect(screen.queryByRole('option', { name: 'X-1' })).not.toBeInTheDocument();
  });
});

describe('ATT-06 Staff attendance', () => {
  it('marks a staff member’s day and keeps a device record read-only', async () => {
    const { api } = await signedIn(
      `/app/attendance/staff?day=${academicsFixture.day}`,
      admin,
      fakeAcademics(),
    );
    expect(await screen.findByRole('heading', { name: '1 of 2 marked' })).toBeInTheDocument();
    const read = api.calls.find((c) => c.path === '/v1/attendance/staff-attendance-days');
    expect(new URLSearchParams(read?.search).get('filter')).toBe(
      `attendanceDate >= "${academicsFixture.day}" AND attendanceDate <= "${academicsFixture.day}"`,
    );
    const grid = screen.getByRole('list', { name: `Staff attendance of ${academicsFixture.day}` });
    expect(within(grid).getByRole('combobox', { name: 'Status of Anil Das' })).toBeDisabled();
    expect(within(grid).getByText('From device')).toBeInTheDocument();
    await userEvent.selectOptions(
      within(grid).getByRole('combobox', { name: 'Status of Kavita Iyer' }),
      'half_day',
    );
    await userEvent.click(within(grid).getByRole('button', { name: 'Save Kavita Iyer' }));
    expect(await screen.findByRole('heading', { name: '2 of 2 marked' })).toBeInTheDocument();
    const created = api.calls.find(
      (c) => c.method === 'POST' && c.path === '/v1/attendance/staff-attendance-days',
    );
    expect(JSON.parse(created?.body ?? '{}')).toMatchObject({
      staffId: academicsFixture.teacherId,
      attendanceDate: academicsFixture.day,
      status: 'half_day',
      source: 'manual',
    });
  });
});
