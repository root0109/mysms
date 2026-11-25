import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import {
  COMMS_STAFF,
  SERVICES_MEMBER,
  commsFixture as fx,
  fakeApi,
  fakeComms,
  fakeServices,
  json,
  schoolAdmin,
} from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import { useBirthdays, useSendGreetings } from './birthdays';
import { useEmailApplicant, useMyApplications, useUpdateApplicationStatus } from './careers';
import {
  calendarDays,
  calendarRange,
  shiftCalendar,
  useClaimTask,
  useCloseNoticeReplies,
  useExportCalendar,
  useFeed,
  useHideMessageForMe,
  useInbox,
  useInboxSteps,
  useMarkConversationRead,
  useMyCalendar,
  useSendTestMessage,
} from './comms';
import { useMyLoans, useMyTransport } from './myServices';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };

async function setup(permissions: readonly string[] = [...COMMS_STAFF, ...SERVICES_MEMBER]) {
  const me = { ...schoolAdmin, permissions: [...permissions] };
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: me.user }),
    'GET /v1/iam/me': () => json(me),
    ...fakeComms(),
    ...fakeServices(),
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
const lastBody = (api: Awaited<ReturnType<typeof setup>>['api'], suffix: string) =>
  JSON.parse(api.calls.filter((c) => c.path.endsWith(suffix)).at(-1)?.body ?? '{}') as unknown;

describe('the calendar views', () => {
  it('names the days of a month, a week and a day and steps between them', () => {
    expect(calendarDays('month', '2026-02-14')).toEqual({ from: '2026-02-01', to: '2026-02-28' });
    expect(calendarDays('week', '2026-03-04')).toEqual({ from: '2026-03-02', to: '2026-03-08' });
    expect(calendarDays('day', '2026-03-04')).toEqual({ from: '2026-03-04', to: '2026-03-04' });
    expect(shiftCalendar('month', '2026-01-31', 1)).toBe('2026-02-01');
    expect(shiftCalendar('month', '2026-03-15', -1)).toBe('2026-02-01');
    expect(shiftCalendar('week', '2026-03-04', -1)).toBe('2026-02-25');
    expect(shiftCalendar('day', '2026-03-01', -1)).toBe('2026-02-28');
    expect(calendarRange({ from: '2026-03-02', to: '2026-03-08' }, fx.cohortId)).toEqual({
      from: '2026-03-01T00:00:00Z',
      to: '2026-03-10T00:00:00Z',
      cohortId: fx.cohortId,
    });
  });
});

describe('the inbox', () => {
  it('reads a time range of the own notifications, unread ones on request, and marks and archives them', async () => {
    const { api, wrapper } = await setup();
    const now = () => Date.parse('2026-03-02T12:00:00Z');
    const all = renderHook(() => ({ inbox: useInbox(30, false, now), steps: useInboxSteps() }), { wrapper });
    await waitFor(() => expect(all.result.current.inbox.data?.pages[0]?.data).toHaveLength(2));
    const asked = new URL(
      `https://x${api.calls.find((c) => c.path === '/v1/comms/notifications')?.search ?? ''}`,
    );
    expect(asked.searchParams.get('filter')).toBe(
      'createTime >= "2026-01-31T12:00:00.000Z" AND createTime <= "2026-03-02T12:00:00.000Z"',
    );
    expect(asked.searchParams.get('sort')).toBe('createTime,desc');
    const unread = renderHook(() => useInbox(400, true, now), { wrapper });
    await waitFor(() =>
      expect(unread.result.current.data?.pages[0]?.data.map((n) => n.id)).toEqual([fx.unreadId]),
    );
    // a year at most: the inbox is partitioned
    expect(api.calls.at(-1)?.search).toContain(encodeURIComponent('2025-03-01T12:00:00.000Z'));

    await act(() => all.result.current.steps.markRead.mutateAsync(fx.unreadId));
    await act(() => all.result.current.steps.markAllRead.mutateAsync());
    await act(() => all.result.current.steps.archive.mutateAsync([fx.readId]));
    expect(lastBody(api, ':archive')).toEqual({ notificationIds: [fx.readId] });
    await waitFor(() =>
      expect(all.result.current.inbox.data?.pages[0]?.data.map((n) => n.id)).toEqual([fx.unreadId]),
    );
  });
});

describe('the calendar, conversations, notices, channels, feed and tasks', () => {
  it('reads the calendar of a range and exports it', async () => {
    const { api, wrapper } = await setup();
    const hooks = renderHook(
      () => ({
        calendar: useMyCalendar(calendarRange(calendarDays('week', '2026-03-04'))),
        exportTo: useExportCalendar(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(hooks.result.current.calendar.data?.items).toHaveLength(3));
    expect(api.calls.find((c) => c.path === '/v1/comms/me/calendar')?.search).toContain('from=2026-03-01T00');
    await act(() =>
      hooks.result.current.exportTo.mutateAsync({
        provider: 'microsoft',
        from: '2026-03-02T00:00:00Z',
        to: '2026-03-09T00:00:00Z',
      }),
    );
    expect(lastBody(api, ':export')).toEqual({
      provider: 'microsoft',
      from: '2026-03-02T00:00:00Z',
      to: '2026-03-09T00:00:00Z',
    });
    const off = renderHook(() => useMyCalendar(null), { wrapper });
    expect(off.result.current.fetchStatus).toBe('idle');
  });

  it('marks a conversation read, hides a message, closes replies, sends a test, reads the feed and claims a task', async () => {
    const { api, wrapper } = await setup();
    const hooks = renderHook(
      () => ({
        read: useMarkConversationRead(),
        hide: useHideMessageForMe(),
        close: useCloseNoticeReplies(),
        test: useSendTestMessage(),
        feed: useFeed({ savedOnly: true }),
        claim: useClaimTask(),
      }),
      { wrapper },
    );
    await act(() => hooks.result.current.read.mutateAsync(fx.conversationId));
    await act(() => hooks.result.current.hide.mutateAsync(fx.messageId));
    await act(() => hooks.result.current.close.mutateAsync({ noticeId: fx.publishedNoticeId, etag: '"1"' }));
    await expect(
      hooks.result.current.close.mutateAsync({ noticeId: fx.publishedNoticeId, etag: null }),
    ).rejects.toMatchObject({
      status: 412,
    });
    await act(() =>
      hooks.result.current.test.mutateAsync({
        notificationChannelId: fx.channelId,
        to: 'ravi@school.example',
      }),
    );
    await act(() => hooks.result.current.claim.mutateAsync(fx.taskId));
    await waitFor(() =>
      expect(hooks.result.current.feed.data?.pages[0]?.data[0]?.body).toBe('Science fair photos'),
    );
    expect(api.calls.find((c) => c.path === '/v1/comms/posts:feed')?.search).toContain('savedOnly=true');
    expect(api.calls.map((c) => c.path)).toEqual(
      expect.arrayContaining([
        `/v1/comms/conversations/${fx.conversationId}:markRead`,
        `/v1/comms/messages/${fx.messageId}:hideForMe`,
        `/v1/comms/notification-channels/${fx.channelId}:sendTest`,
        `/v1/comms/tasks/${fx.taskId}:claim`,
      ]),
    );
  });
});

describe('birthdays and greetings', () => {
  it('reads one kind in a range at an institution and greets the people chosen', async () => {
    const { api, wrapper } = await setup();
    const hooks = renderHook(
      () => ({
        list: useBirthdays({
          institutionId: fx.institutionId,
          fromDate: '2026-03-02',
          toDate: '2026-03-08',
          kinds: ['birthday'],
          roles: ['student'],
        }),
        send: useSendGreetings(),
      }),
      { wrapper },
    );
    await waitFor(() =>
      expect(hooks.result.current.list.data?.items.map((b) => b.displayName)).toEqual(['Diya Menon']),
    );
    expect(api.calls.find((c) => c.path === '/v1/people/persons:birthdays')?.search).toContain(
      'roles=student',
    );
    const sent = await act(() =>
      hooks.result.current.send.mutateAsync({
        personIds: [fx.birthdayPersonId],
        templateId: fx.templateId,
        channel: 'sms',
      }),
    );
    expect(sent).toMatchObject({ queued: 1, skipped: 0 });
    expect(lastBody(api, ':sendGreetings')).toEqual({
      personIds: [fx.birthdayPersonId],
      templateId: fx.templateId,
      channel: 'sms',
    });
  });
});

describe('careers', () => {
  it("finds the candidate's applications per opening, moves one and writes to the applicant", async () => {
    const { api, wrapper } = await setup();
    const hooks = renderHook(
      () => ({
        mine: useMyApplications([fx.openingId, fx.closedOpeningId]),
        status: useUpdateApplicationStatus(),
        email: useEmailApplicant(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(hooks.result.current.mine.isPending).toBe(false));
    expect(hooks.result.current.mine.applications.map((a) => a.id)).toEqual([fx.applicationId]);
    expect(
      decodeURIComponent(api.calls.find((c) => c.path.endsWith('/applications'))?.search ?? ''),
    ).toContain(`applicantUserId = "${fx.me}"`);
    await act(() =>
      hooks.result.current.status.mutateAsync({
        jobOpeningId: fx.closedOpeningId,
        jobApplicationId: fx.applicationId,
        status: 'hired',
        etag: '"1"',
      }),
    );
    expect(lastBody(api, ':setStatus')).toEqual({ status: 'hired', etag: '"1"' });
    await act(() =>
      hooks.result.current.email.mutateAsync({
        applicationId: fx.applicationId,
        subject: 'Hi',
        message: 'Welcome',
      }),
    );
    expect(lastBody(api, ':email')).toEqual({ subject: 'Hi', message: 'Welcome' });
  });
});

describe('my loans and my route', () => {
  it("reads the loans on the own card with the titles' names, and a student's route with its stops", async () => {
    const { wrapper } = await setup();
    const hooks = renderHook(() => ({ loans: useMyLoans(), route: useMyTransport(fx.studentId) }), {
      wrapper,
    });
    await waitFor(() => expect(hooks.result.current.loans.titleOf.get(fx.mediaId)).toBe('Wings of Fire'));
    expect(hooks.result.current.loans.loans.map((l) => l.id)).toEqual([fx.loanId]);
    await waitFor(() =>
      expect(hooks.result.current.route.stops.map((s) => s.stopName)).toEqual(['Depot', 'Lake Road']),
    );
    expect(hooks.result.current.route.routes[0]?.name).toBe('Route 4');
    expect(hooks.result.current.route.isError).toBe(false);
  });

  it('asks nothing without the permissions', async () => {
    const { api, wrapper } = await setup([]);
    const hooks = renderHook(() => ({ loans: useMyLoans(), route: useMyTransport(fx.studentId) }), {
      wrapper,
    });
    expect(hooks.result.current.loans.may).toBe(false);
    expect(hooks.result.current.route.may).toBe(false);
    expect(
      api.calls.some((c) => c.path.startsWith('/v1/library') || c.path.startsWith('/v1/transport')),
    ).toBe(false);
  });
});
