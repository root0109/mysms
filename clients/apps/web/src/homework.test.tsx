import {
  HOMEWORK_REVIEWER,
  HOMEWORK_TEACHER,
  fakeAttendance,
  fakeHomework,
  homeworkFixture,
  schoolAdmin,
  SELF_SERVICE,
} from '@mysms/fixtures';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const { homeworkId, submissions } = homeworkFixture;
const teacher = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...HOMEWORK_TEACHER] };
const head = { ...teacher, permissions: [...teacher.permissions, ...HOMEWORK_REVIEWER] };
const board = '/app/assessment/homeworks';
const detail = `${board}/${homeworkId}`;
const api = `/v1/assessment/homeworks/${homeworkId}`;
// names and the class roster come from the attendance fixture's class
const backend = (options: Parameters<typeof fakeHomework>[0] = {}) => ({
  ...fakeAttendance(),
  ...fakeHomework(options),
});
const row = (name: string) => screen.getByText(name).closest('li') as HTMLElement;

describe('ASM-03 Homework check', () => {
  it('sends a draft for review when the teacher may not approve, and shows why it came back', async () => {
    const teacherSide = await signedIn(detail, teacher, backend());
    expect(await screen.findByRole('heading', { name: 'Homework check' })).toBeInTheDocument();
    expect(screen.getByText('Read chapter 4 and answer the questions.')).toBeInTheDocument();
    expect(screen.getByText(/The class does not see it yet/)).toBeInTheDocument();
    expect(screen.queryByText(/handed in/)).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Send for review' }));
    expect(await screen.findByText('The homework is sent for review.')).toBeInTheDocument();
    expect(await screen.findByText(/waiting for a reviewer/)).toBeInTheDocument();
    expect(JSON.parse(teacherSide.api.calls.find((c) => c.path === `${api}:publish`)?.body ?? '{}')).toEqual({
      etag: '"1"',
    });
    // a teacher has no decision to take
    expect(screen.queryByRole('button', { name: 'Approve and publish' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Send back' })).not.toBeInTheDocument();
  });

  it('lets a reviewer send homework back with a note, which the teacher then reads on the draft', async () => {
    const { api: calls } = await signedIn(detail, head, backend({ status: 'pending' }));
    await userEvent.type(await screen.findByLabelText('Note for the teacher (optional)'), 'Due on a holiday');
    await userEvent.click(screen.getByRole('button', { name: 'Send back' }));
    expect(await screen.findByText('The homework is sent back to its teacher.')).toBeInTheDocument();
    expect(JSON.parse(calls.calls.find((c) => c.path === `${api}:reject`)?.body ?? '{}')).toEqual({
      etag: '"1"',
      reason: 'Due on a holiday',
    });
    expect(await screen.findByText(/The class does not see it yet/)).toBeInTheDocument();
  });

  it('lets a reviewer approve, and a role that may approve publish at once', async () => {
    const review = await signedIn(detail, head, backend({ status: 'pending' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Approve and publish' }));
    expect(await screen.findByText('The homework is published.')).toBeInTheDocument();
    expect(await screen.findByText('Published')).toBeInTheDocument();
    expect(await screen.findByText('2 of 3 handed in')).toBeInTheDocument();
    review.unmount();

    await signedIn(detail, head, backend({ reviewer: true }));
    await userEvent.click(await screen.findByRole('button', { name: 'Publish' }));
    expect(await screen.findByText('The homework is published.')).toBeInTheDocument();
  });

  it('shows the class as a checklist and marks a submission checked with comments under its etag', async () => {
    const { api: calls } = await signedIn(detail, teacher, backend({ status: 'published' }));
    expect(await screen.findByText('2 of 3 handed in')).toBeInTheDocument();
    await screen.findByText('Chitra Nair');
    // roll numbers order the class (1, 2, 10); the student who did not hand in is listed too
    expect([...document.querySelectorAll('.attendance-grid__name')].map((name) => name.textContent)).toEqual([
      'Chitra Nair',
      'Asha Rao',
      'Bilal Khan',
    ]);
    expect(within(row('Chitra Nair')).getByText('Not handed in')).toBeInTheDocument();
    expect(within(row('Asha Rao')).getByText('To check')).toBeInTheDocument();
    expect(within(row('Asha Rao')).getByText('Handed in on 10 Mar 2026')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Comments for Asha Rao'), 'Show your working');
    await userEvent.click(within(row('Asha Rao')).getByRole('button', { name: 'Mark checked' }));
    await waitFor(() => expect(within(row('Asha Rao')).getByText('Checked')).toBeInTheDocument());
    const check = calls.calls.find((c) => c.path.endsWith(`${submissions[0].id}:check`));
    expect(JSON.parse(check?.body ?? '{}')).toEqual({ etag: '"1"', teacherComments: 'Show your working' });
    // nothing new to save until the comment changes
    const update = within(row('Asha Rao')).getByRole('button', { name: 'Update comments' });
    expect(update).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Comments for Asha Rao'), ' please');
    expect(update).toBeEnabled();
    expect(within(row('Bilal Khan')).getByRole('button', { name: 'Mark checked' })).toBeEnabled();
  });

  it('is read-only for a role that may not check, and tells a role without access so', async () => {
    const viewer = {
      ...teacher,
      permissions: teacher.permissions.filter((code) => !code.includes('check_homework_submission')),
    };
    const view = await signedIn(detail, viewer, backend({ status: 'published' }));
    expect(await screen.findByText('2 of 3 handed in')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mark checked' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Comments for Asha Rao')).not.toBeInTheDocument();
    view.unmount();

    await signedIn(detail, { ...schoolAdmin, permissions: [...SELF_SERVICE] }, backend());
    expect(await screen.findByText('Your role may not open homework.')).toBeInTheDocument();
  });
});

describe('ASM-01 Homework board and ASM-02 Homework approval', () => {
  it('opens the approval queue from the address, keeps the filter there and links a homework to its screen', async () => {
    const { api: calls, router } = await signedIn(
      `${board}?status=pending`,
      head,
      backend({ status: 'pending' }),
    );
    expect(await screen.findByRole('heading', { name: 'Homework approval' })).toBeInTheDocument();
    await screen.findByRole('link', { name: /^Open / });
    const listed = calls.calls.filter((c) => c.path === '/v1/assessment/homeworks').pop();
    expect(new URLSearchParams(listed?.search).get('filter')).toBe('status = "pending"');
    const nav = screen.getByRole('navigation', { name: 'Main navigation' });
    expect(within(nav).getByRole('link', { name: 'Homework approval' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    expect(within(nav).getByRole('link', { name: 'Homework board' })).not.toHaveAttribute('aria-current');

    // choosing another state is another address: the board
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Status' }), '');
    await waitFor(() => expect(router.state.location.search).toBe(''));
    expect(await screen.findByRole('heading', { name: 'Homework board' })).toBeInTheDocument();
    expect(within(nav).getByRole('link', { name: 'Homework board' })).toHaveAttribute('aria-current', 'page');

    await userEvent.click(await screen.findByRole('link', { name: /^Open / }));
    await waitFor(() => expect(router.state.location.pathname).toBe(detail));
    expect(await screen.findByRole('heading', { name: 'Homework check' })).toBeInTheDocument();
  });

  it('does not show the approval queue in the navigation of a role that may not approve', async () => {
    await signedIn(board, teacher, backend());
    const nav = await screen.findByRole('navigation', { name: 'Main navigation' });
    expect(await within(nav).findByRole('link', { name: 'Homework board' })).toBeInTheDocument();
    expect(within(nav).queryByRole('link', { name: 'Homework approval' })).not.toBeInTheDocument();
  });
});
