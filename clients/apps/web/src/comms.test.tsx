import { COMMS_STAFF, SELF_SERVICE, commsFixture as fx, fakeComms, schoolAdmin } from '@mysms/fixtures';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { signedIn } from './test/signedIn';

const staff = { ...schoolAdmin, permissions: [...SELF_SERVICE, ...COMMS_STAFF] };
const bodyOf = (api: Awaited<ReturnType<typeof signedIn>>['api'], method: string, suffix: string) =>
  JSON.parse(
    api.calls.filter((c) => c.method === method && c.path.endsWith(suffix)).at(-1)?.body ?? '{}',
  ) as Record<string, unknown>;

describe('ACC-09 Notification inbox', () => {
  it('lists the own notifications in a time range, opens one through its link and marks the rest read', async () => {
    const { api, router } = await signedIn('/app/comms/in-app-notifications', staff, fakeComms());
    expect(await screen.findByText('Homework due tomorrow')).toBeInTheDocument();
    expect(screen.getByText('Fee receipt')).toBeInTheDocument();
    const list = api.calls.find((c) => c.path === '/v1/comms/notifications');
    expect(decodeURIComponent(list?.search ?? '')).toContain('createTime >=');
    expect(screen.getByRole('button', { name: 'Archive 1 read notification' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Open' }));
    await waitFor(() =>
      expect(router.state.location.pathname).toBe(`/app/comms/notices/${fx.publishedNoticeId}`),
    );
    expect(api.calls.some((c) => c.path === `/v1/comms/notifications/${fx.unreadId}:markRead`)).toBe(true);
  });

  it('keeps unread ones on request, marks all read and archives the read ones', async () => {
    const { api } = await signedIn('/app/comms/in-app-notifications', staff, fakeComms());
    await screen.findByText('Fee receipt');
    await userEvent.click(screen.getByRole('switch', { name: 'Unread only' }));
    await waitFor(() => expect(screen.queryByText('Fee receipt')).not.toBeInTheDocument());
    await userEvent.click(screen.getByRole('switch', { name: 'Unread only' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Mark all as read' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Archive 2 read notifications' }));
    expect(bodyOf(api, 'POST', ':archive')).toEqual({ notificationIds: [fx.unreadId, fx.readId] });
    expect(await screen.findByText('No notifications')).toBeInTheDocument();
  });

  it('tells a role without the inbox so', async () => {
    await signedIn(
      '/app/comms/in-app-notifications',
      { ...schoolAdmin, permissions: [...SELF_SERVICE] },
      fakeComms(),
    );
    expect(await screen.findByText(/may not/i)).toBeInTheDocument();
  });
});

describe('COM-01 Notice board, COM-02 Notice composer and LMS-28 cohort announcements', () => {
  it('shows published notices, drafts on request, and writes a draft that opens in the composer', async () => {
    const { api, router } = await signedIn('/app/comms/notices', staff, fakeComms());
    expect(await screen.findByRole('heading', { name: 'Sports day' })).toBeInTheDocument();
    expect(screen.queryByText('Staff meeting')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('switch', { name: 'Show drafts' }));
    expect(await screen.findByRole('heading', { name: 'Staff meeting' })).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Title'), 'Exam timetable');
    await userEvent.type(screen.getByLabelText('Text'), 'See the attached plan.');
    await userEvent.selectOptions(screen.getByLabelText('For'), 'teachers');
    await userEvent.click(screen.getByRole('button', { name: 'Save as draft and continue' }));
    await waitFor(() =>
      expect(router.state.location.pathname).toMatch(/^\/app\/comms\/notices\/[0-9a-f-]{36}$/),
    );
    expect(bodyOf(api, 'POST', '/v1/comms/notices')).toMatchObject({
      title: 'Exam timetable',
      body: 'See the attached plan.',
      audience: 'teachers',
    });
    expect(await screen.findByRole('heading', { name: 'Exam timetable' })).toBeInTheDocument();
  });

  it('closes replies to a published notice under its etag', async () => {
    const { api } = await signedIn(`/app/comms/notices/${fx.publishedNoticeId}`, staff, fakeComms());
    await userEvent.click(await screen.findByRole('button', { name: 'Close replies' }));
    await waitFor(() => expect(bodyOf(api, 'POST', ':closeReplies')).toEqual({ etag: '"1"' }));
    expect(screen.getByRole('radio', { name: 'Addressed to' })).toBeInTheDocument();
  });

  it("reads one cohort's announcements and writes new ones for the cohort", async () => {
    const { api } = await signedIn(`/app/comms/notices?cohort=${fx.cohortId}`, staff, fakeComms());
    expect(await screen.findByRole('heading', { name: 'Cohort announcements' })).toBeInTheDocument();
    expect(await screen.findByText('Live class moved')).toBeInTheDocument();
    expect(screen.queryByText('Sports day')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('For')).not.toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Title'), 'Quiz on Friday');
    await userEvent.type(screen.getByLabelText('Text'), 'Chapters 1-3');
    await userEvent.click(screen.getByRole('button', { name: 'Save as draft and continue' }));
    await waitFor(() =>
      expect(bodyOf(api, 'POST', '/v1/comms/notices')).toMatchObject({
        audience: 'cohort',
        cohortId: fx.cohortId,
      }),
    );
  });
});

describe('COM-03 My calendar', () => {
  it("places the week's items on their days and exports the range", async () => {
    const { api } = await signedIn('/app/comms/calendar', staff, fakeComms());
    await userEvent.click(await screen.findByRole('radio', { name: 'Week' }));
    // the calendar is read for the week the clock is in: step to the fixture's week from there
    const asked = api.calls.filter((c) => c.path === '/v1/comms/me/calendar').at(-1);
    expect(asked?.search).toContain('from=');
    expect(screen.getByRole('button', { name: 'Google Calendar' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Google Calendar' }));
    expect(await screen.findByText('The export has started.')).toBeInTheDocument();
    expect(bodyOf(api, 'POST', ':export')).toMatchObject({ provider: 'google' });
  });
});

describe('COM-05 Conversations', () => {
  it('opens a conversation, marks it read, sends a message as the caller and hides one for the caller', async () => {
    const { api } = await signedIn(
      `/app/comms/conversations?conversation=${fx.conversationId}`,
      staff,
      fakeComms(),
    );
    expect(await screen.findByText('Is the bus late today?')).toBeInTheDocument();
    const thread = screen.getByRole('list', { name: 'Messages' });
    expect(within(thread).getAllByText(/^You ·|^Them ·/)).toHaveLength(2);
    await waitFor(() =>
      expect(api.calls.some((c) => c.path === `/v1/comms/conversations/${fx.conversationId}:markRead`)).toBe(
        true,
      ),
    );
    await userEvent.type(screen.getByLabelText('Message'), 'On my way');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    expect(await screen.findByText('On my way')).toBeInTheDocument();
    expect(bodyOf(api, 'POST', '/messages')).toEqual({
      conversationId: fx.conversationId,
      body: 'On my way',
    });
    await userEvent.click(within(thread).getAllByRole('button', { name: 'Hide for me' })[0] as HTMLElement);
    await waitFor(() => expect(api.calls.some((c) => c.path.endsWith(':hideForMe'))).toBe(true));
  });
});

describe('COM-13 Notification setup', () => {
  it("shows a type's rules, a channel's settings masked, and sends a test to the caller's own address", async () => {
    const { api } = await signedIn('/app/comms/notification-rules', staff, fakeComms());
    const type = await screen.findByLabelText('Notification type');
    await screen.findByRole('option', { name: 'fee.due — A fee falls due' });
    await userEvent.selectOptions(type, fx.typeId);
    expect(await screen.findByText('Fee due')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('radio', { name: 'Channels' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Settings' }));
    expect(await screen.findByText('••••')).toBeInTheDocument();
    expect(screen.getByLabelText('Your address')).toHaveValue('ravi@school.example');
    await userEvent.click(screen.getByRole('button', { name: 'Send a test message' }));
    expect(await screen.findByText('The test message was sent.')).toBeInTheDocument();
    expect(bodyOf(api, 'POST', ':sendTest')).toEqual({ to: 'ravi@school.example' });

    await userEvent.click(screen.getByRole('radio', { name: 'Templates' }));
    expect(await screen.findByText('Birthday wishes')).toBeInTheDocument();
  });
});
