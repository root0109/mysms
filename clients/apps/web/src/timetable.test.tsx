import {
  SELF_SERVICE,
  TIMETABLE_TEACHER,
  fakeTimetable,
  onePage,
  schoolAdmin,
  timetableFixture,
} from '@mysms/fixtures';
import { localDay, weekOf } from '@mysms/core';
import { createFormatters } from '@mysms/i18n';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

// the fixture account reads dates and times in Asia/Kolkata
// iam.GetMe names the teacher's staff record
const teacher = {
  ...schoolAdmin,
  staffIds: [timetableFixture.staffId],
  permissions: [...SELF_SERVICE, ...TIMETABLE_TEACHER],
};
const week = '/app/timetable/my?week=2026-03-02';
const day = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;

describe('TT-03 My timetable', () => {
  it('shows the teacher’s week day by day in their own time, without drafts, and moves to another week', async () => {
    const { api, router } = await signedIn(week, teacher, fakeTimetable());
    expect(await screen.findByRole('heading', { name: 'My timetable' })).toBeInTheDocument();
    expect(screen.getByText('2 Mar 2026 – 8 Mar 2026')).toBeInTheDocument();
    await screen.findAllByText('Mathematics');

    const monday = within(day('Monday, 2 Mar 2026')).getAllByRole('listitem');
    expect(monday).toHaveLength(2);
    expect(within(monday[0] as HTMLElement).getByText('8:30 am – 9:15 am')).toBeInTheDocument();
    expect(within(monday[0] as HTMLElement).getByText('Mathematics')).toBeInTheDocument();
    expect(within(monday[0] as HTMLElement).getByText('Room 12')).toBeInTheDocument();
    expect(within(monday[0] as HTMLElement).getByText('Held')).toBeInTheDocument();
    expect(within(monday[1] as HTMLElement).getByText('Science')).toBeInTheDocument();
    expect(within(day('Wednesday, 4 Mar 2026')).getByText('Cancelled')).toBeInTheDocument();
    // the draft of Thursday is not part of a timetable, and Sunday evening UTC is next week's Monday in India
    expect(within(day('Thursday, 5 Mar 2026')).getByText('No sessions.')).toBeInTheDocument();
    expect(within(day('Sunday, 8 Mar 2026')).getByText('No sessions.')).toBeInTheDocument();
    expect(screen.queryByText('Another teacher')).not.toBeInTheDocument();
    expect(screen.queryByText('Remedial class')).not.toBeInTheDocument();

    const read = api.calls.find((c) => c.path === '/v1/timetable/sessions:timetable');
    expect(new URLSearchParams(read?.search).get('fromDate')).toBe('2026-03-01');
    expect(new URLSearchParams(read?.search).get('toDate')).toBe('2026-03-09');

    await userEvent.click(screen.getByRole('button', { name: 'Next week' }));
    await waitFor(() => expect(router.state.location.search).toBe('?week=2026-03-09'));
    expect(await screen.findByText('Remedial class')).toBeInTheDocument();
    expect(within(day('Monday, 9 Mar 2026')).getByText('12:15 am – 1:00 am')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Previous week' }));
    await waitFor(() => expect(router.state.location.search).toBe('?week=2026-03-02'));

    const nav = await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).getByRole('link', { name: 'My timetable' })).toHaveAttribute(
      'href',
      '/app/timetable/my',
    );
  });

  it('tells an account without a staff record so, and a role without the permission that it has no access', async () => {
    const office = await signedIn(week, { ...teacher, staffIds: [] }, fakeTimetable());
    expect(
      await screen.findByText(
        'Your account is not linked to a staff record, so it has no timetable of its own.',
      ),
    ).toBeInTheDocument();
    expect(office.api.calls.some((c) => c.path === '/v1/timetable/sessions:timetable')).toBe(false);
    office.unmount();

    const visitor = await signedIn(week, { ...schoolAdmin, permissions: [...SELF_SERVICE] }, fakeTimetable());
    expect(await screen.findByText('Your role may not open the timetable.')).toBeInTheDocument();
    expect(visitor.api.calls.some((c) => c.path.startsWith('/v1/timetable'))).toBe(false);
    const nav = await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).queryByRole('link', { name: 'My timetable' })).not.toBeInTheDocument();
  });

  it('opens on this week with today marked, also when the address names a day that does not exist', async () => {
    const today = localDay(new Date(), 'Asia/Kolkata');
    const now = weekOf(today);
    const format = createFormatters('en-IN', 'Asia/Kolkata');
    await signedIn('/app/timetable/my?week=2026-02-30', teacher, fakeTimetable());
    expect(await screen.findByText(`${format.day(now.from)} – ${format.day(now.to)}`)).toBeInTheDocument();
    const marked = (await screen.findByText('Today')).closest('section');
    expect(marked).toHaveAttribute('aria-current', 'date');
    expect(within(marked as HTMLElement).getByRole('heading')).toHaveTextContent(format.day(today));
    expect(screen.getByRole('button', { name: 'This week' })).toBeDisabled();
  });

  it('shows the timetable of both schools to a teacher employed by two, and offers a retry when a read fails', async () => {
    const two = await signedIn(
      week,
      { ...teacher, staffIds: [timetableFixture.staffId, timetableFixture.otherStaffId] },
      fakeTimetable(),
    );
    expect(await screen.findByText('Another teacher')).toBeInTheDocument();
    expect(await screen.findAllByText('Mathematics')).toHaveLength(2);
    two.unmount();

    await signedIn(week, teacher, fakeTimetable({ failOnce: true }));
    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findAllByText('Mathematics')).toHaveLength(2);
  });

  it('says so when the role may not look up its own staff record', async () => {
    const noLookup = {
      ...schoolAdmin,
      permissions: [...SELF_SERVICE, 'timetable.session.get_timetable'],
    };
    const { api } = await signedIn(week, noLookup, fakeTimetable());
    expect(await screen.findByText(/Your role may not look up your staff record/)).toBeInTheDocument();
    expect(api.calls.some((c) => c.path === '/v1/hr/staff')).toBe(false);
  });

  it('lets an account that is not staff pick a class and keeps the choice in the address', async () => {
    const list = (items: unknown[]) => (req: Request) => onePage(req, items);
    const { institutionId, classSectionId } = timetableFixture;
    const office = {
      ...teacher,
      staffIds: [],
      permissions: [
        ...teacher.permissions,
        'org.institution.read',
        'academics.class_section.read',
        'academics.grade_level.read',
        'academics.section.read',
      ],
    };
    const { api, router } = await signedIn(week, office, {
      ...fakeTimetable(),
      'GET /v1/org/institutions': list([{ id: institutionId, code: 'MAIN', name: 'Main School' }]),
      'GET /v1/academics/class-sections': list([
        { id: classSectionId, institutionId, gradeLevelId: 'g5', sectionId: 'sa', isActive: true },
      ]),
      'GET /v1/academics/grade-levels': list([{ id: 'g5', name: 'Grade 5' }]),
      'GET /v1/academics/sections': list([{ id: 'sa', name: 'A' }]),
    });
    expect(
      await screen.findByText(
        'Your account is not linked to a staff record. Choose a class to see its timetable.',
      ),
    ).toBeInTheDocument();
    await userEvent.selectOptions(await screen.findByRole('combobox', { name: 'Timetable of' }), 'Grade 5 A');
    await waitFor(() =>
      expect(router.state.location.search).toBe(`?week=2026-03-02&class=${classSectionId}`),
    );
    expect(screen.getByRole('combobox', { name: 'Timetable of' })).toHaveValue(classSectionId);
    // the class has the other teacher's session too, and its own name is not repeated on every line
    expect(await screen.findByText('Another teacher')).toBeInTheDocument();
    expect(screen.queryByText(/Grade 5 A ·/)).not.toBeInTheDocument();
    const read = api.calls.filter((c) => c.path === '/v1/timetable/sessions:timetable').pop();
    expect(new URLSearchParams(read?.search).get('classSectionId')).toBe(classSectionId);
    expect(new URLSearchParams(read?.search).get('staffId')).toBeNull();
  });
});
