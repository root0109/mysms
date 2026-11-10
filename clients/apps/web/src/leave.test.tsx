import {
  LEAVE_APPROVER,
  LEAVE_STAFF,
  SELF_SERVICE,
  fakeLeave,
  leaveFixture,
  schoolAdmin,
} from '@mysms/fixtures';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

// iam.GetMe names the teacher's staff record
const teacher = {
  ...schoolAdmin,
  staffIds: [leaveFixture.staffId],
  permissions: [...SELF_SERVICE, ...LEAVE_STAFF],
};
const head = { ...teacher, permissions: [...teacher.permissions, ...LEAVE_APPROVER, 'people.person.read'] };
const route = '/app/hr/leave-applications';
const card = (name: string) => screen.getByRole('heading', { name }).closest('section') as HTMLElement;

describe('HR-04 Leave applications', () => {
  it('shows a staff member what leave is left and lets them apply and withdraw', async () => {
    const { api } = await signedIn(route, teacher, fakeLeave());
    expect(await screen.findByRole('heading', { name: 'Leave applications' })).toBeInTheDocument();
    const position = await screen.findByRole('table', { name: 'My leave this year' });
    const casual = (await within(position).findByText('Casual leave')).closest('tr') as HTMLElement;
    // entitled 10, taken 2, asked for 2, left 6
    expect(
      within(casual)
        .getAllByRole('cell')
        .map((cell) => cell.textContent),
    ).toEqual(['Casual leave', '10', '2', '2', '6']);
    // a teacher has no queue
    expect(screen.queryByRole('heading', { name: 'Waiting for a decision' })).not.toBeInTheDocument();

    const form = card('Apply for leave');
    fireEvent.change(within(form).getByLabelText('First day'), { target: { value: '2026-04-06' } });
    fireEvent.change(within(form).getByLabelText('Last day'), { target: { value: '2026-04-08' } });
    expect(within(form).getByText('3 days of 6 left.')).toBeInTheDocument();
    fireEvent.change(within(form).getByLabelText('Last day'), { target: { value: '2026-04-14' } });
    expect(
      within(form).getByText(/This is more than is left once what you have already asked for/),
    ).toBeInTheDocument();
    fireEvent.change(within(form).getByLabelText('Last day'), { target: { value: '2026-04-06' } });
    // a half day is one day, and a leave lies inside one academic year
    await userEvent.selectOptions(within(form).getByLabelText('Part of the day'), 'Forenoon');
    fireEvent.change(within(form).getByLabelText('Last day'), { target: { value: '2026-04-07' } });
    expect(within(form).getByText(/A half day is a leave of one day/)).toBeInTheDocument();
    expect(within(form).getByRole('button', { name: 'Apply' })).toBeDisabled();
    fireEvent.change(within(form).getByLabelText('Last day'), { target: { value: '2026-04-06' } });
    fireEvent.change(within(form).getByLabelText('First day'), { target: { value: '2100-01-05' } });
    expect(within(form).getByText('No academic year covers this day.')).toBeInTheDocument();
    expect(within(form).getByRole('button', { name: 'Apply' })).toBeDisabled();
    fireEvent.change(within(form).getByLabelText('First day'), { target: { value: '2026-04-06' } });
    fireEvent.change(within(form).getByLabelText('Last day'), { target: { value: '2026-04-06' } });
    expect(within(form).getByText('0.5 days of 6 left.')).toBeInTheDocument();
    await userEvent.type(within(form).getByLabelText('Remarks (optional)'), 'Dentist');
    await userEvent.click(within(form).getByRole('button', { name: 'Apply' }));
    expect(await screen.findByText('Your application is sent and waits for a decision.')).toBeInTheDocument();
    const post = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/hr/leave-applications');
    expect(JSON.parse(post?.body ?? '{}')).toMatchObject({
      staffId: leaveFixture.staffId,
      academicYearId: leaveFixture.academicYearId,
      leaveTypeId: leaveFixture.casualId,
      fromAt: '2026-04-06T00:00:00Z',
      toAt: '2026-04-06T00:00:00Z',
      dayPart: 'forenoon',
      remarks: 'Dentist',
    });

    const mine = card('My applications');
    await waitFor(() => expect(within(mine).getAllByRole('listitem')).toHaveLength(2));
    const first = within(mine)
      .getByText(/10 Mar 2026 – 11 Mar 2026/)
      .closest('li') as HTMLElement;
    expect(within(first).getByText('Waiting')).toBeInTheDocument();
    await userEvent.click(within(first).getByRole('button', { name: 'Withdraw' }));
    expect(await screen.findByText('The application is withdrawn.')).toBeInTheDocument();
    await waitFor(() => expect(within(first).getByText('Withdrawn')).toBeInTheDocument());
    expect(within(first).queryByRole('button', { name: 'Withdraw' })).not.toBeInTheDocument();
    const cancel = api.calls.find((c) => c.path.endsWith(':cancel'));
    expect(JSON.parse(cancel?.body ?? '{}')).toEqual({ etag: '"1"' });
  });

  it('cancels an approved leave that has not started and gives its days back', async () => {
    const { api } = await signedIn(route, teacher, fakeLeave({ approvedAhead: true }));
    const position = await screen.findByRole('table', { name: 'My leave this year' });
    const casual = () => within(position).getByText('Casual leave').closest('tr') as HTMLElement;
    // entitled 10, taken 2 + the 2 approved ahead, asked for 2, left 4
    await waitFor(() =>
      expect(
        within(casual())
          .getAllByRole('cell')
          .map((cell) => cell.textContent),
      ).toEqual(['Casual leave', '10', '4', '2', '4']),
    );
    const mine = card('My applications');
    const ahead = (await within(mine).findByText(/1 Dec 2098 – 2 Dec 2098/)).closest('li') as HTMLElement;
    expect(within(ahead).getByText('Approved')).toBeInTheDocument();
    await userEvent.click(within(ahead).getByRole('button', { name: 'Cancel leave' }));
    expect(
      await screen.findByText('The leave is cancelled and its days are back in your balance.'),
    ).toBeInTheDocument();
    await waitFor(() => expect(within(ahead).getByText('Withdrawn')).toBeInTheDocument());
    expect(within(ahead).queryByRole('button', { name: 'Cancel leave' })).not.toBeInTheDocument();
    await waitFor(() =>
      expect(
        within(casual())
          .getAllByRole('cell')
          .map((cell) => cell.textContent),
      ).toEqual(['Casual leave', '10', '2', '2', '6']),
    );
    const cancel = api.calls.find((c) => c.path.endsWith(`${leaveFixture.approvedApplicationId}:cancel`));
    expect(JSON.parse(cancel?.body ?? '{}')).toEqual({ etag: '"1"' });
    // a past approved leave offers no cancellation
    const past = within(mine)
      .queryByText(/10 Mar 2026/)
      ?.closest('li');
    expect(
      past ? within(past as HTMLElement).queryByRole('button', { name: 'Cancel leave' }) : null,
    ).toBeNull();
  });

  it('lets an approver decide on a colleague’s leave with a comment, and never on their own', async () => {
    const { api } = await signedIn(route, head, fakeLeave());
    const item = (await screen.findByText('Meera Iyer')).closest('li') as HTMLElement;
    const queue = card('Waiting for a decision');
    expect(within(queue).getAllByRole('listitem')).toHaveLength(1);
    expect(within(item).getByText(/16 Mar 2026 – 18 Mar 2026 · 3 days/)).toBeInTheDocument();
    expect(within(item).getByText('Family function')).toBeInTheDocument();
    await userEvent.type(within(item).getByLabelText('Comment for Meera Iyer (optional)'), 'Enjoy');
    await userEvent.click(within(item).getByRole('button', { name: 'Approve the leave of Meera Iyer' }));
    expect(
      await screen.findByText('The leave is approved and counted against the balance.'),
    ).toBeInTheDocument();
    const approve = api.calls.find((c) => c.path.endsWith(':approve'));
    expect(approve?.path).toContain(leaveFixture.colleagueApplicationId);
    expect(JSON.parse(approve?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Enjoy' });
    expect(await within(queue).findByText('Nothing waits for a decision.')).toBeInTheDocument();
  });

  it('shows why an approval was refused, and tells accounts without leave or access so', async () => {
    const short = await signedIn(route, head, fakeLeave({ colleagueLeft: 2 }));
    const item = (await screen.findByText('Meera Iyer')).closest('li') as HTMLElement;
    await userEvent.click(within(item).getByRole('button', { name: 'Approve the leave of Meera Iyer' }));
    expect(
      await screen.findByText('The application asks for 3 days and 2 are left for this leave type.'),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(within(item).getByRole('button', { name: 'Reject the leave of Meera Iyer' })).toBeEnabled(),
    );
    short.unmount();

    const office = await signedIn(route, { ...teacher, staffIds: [] }, fakeLeave());
    expect(
      await screen.findByText('Your account is not linked to a staff record, so it has no leave of its own.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Apply for leave' })).not.toBeInTheDocument();
    office.unmount();

    await signedIn(route, { ...schoolAdmin, permissions: [...SELF_SERVICE] }, fakeLeave());
    expect(await screen.findByText('Your role may not open leave applications.')).toBeInTheDocument();
    const nav = await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).queryByRole('link', { name: 'Leave applications' })).not.toBeInTheDocument();
  });

  it('tells a staff member whose role cannot read academic years so, and serves an approver who is not staff', async () => {
    const noYears = {
      ...teacher,
      permissions: teacher.permissions.filter((code) => code !== 'org.academic_year.read'),
    };
    const blocked = await signedIn(route, noYears, fakeLeave());
    expect(await screen.findByText(/Your role may not read academic years/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Apply for leave' })).not.toBeInTheDocument();
    expect(screen.queryByRole('table', { name: 'My leave this year' })).not.toBeInTheDocument();
    blocked.unmount();

    // an administrator who approves but has no staff record: the queue still names the leave type
    const admin = {
      ...schoolAdmin,
      permissions: [
        ...SELF_SERVICE,
        'hr.staff.read',
        'hr.leave_type.read',
        'hr.leave_application.read',
        ...LEAVE_APPROVER,
        'people.person.read',
      ],
    };
    await signedIn(route, admin, fakeLeave());
    const item = (await screen.findByText('Meera Iyer')).closest('li') as HTMLElement;
    expect(await within(item).findByText(/Casual leave · 16 Mar 2026 – 18 Mar 2026/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'My applications' })).not.toBeInTheDocument();
    // their own application is not theirs here (no staff record), so both wait in the queue
    expect(within(card('Waiting for a decision')).getAllByRole('listitem')).toHaveLength(2);
  });
});
