import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { comms } from '@mysms/sdk';
import { CalendarExportApi } from '@mysms/sdk/comms/apis/CalendarExportApi';
import { ConversationApi } from '@mysms/sdk/comms/apis/ConversationApi';
import { EventApi } from '@mysms/sdk/comms/apis/EventApi';
import { NoticeApi } from '@mysms/sdk/comms/apis/NoticeApi';
import { NotificationChannelApi } from '@mysms/sdk/comms/apis/NotificationChannelApi';
import { PostApi } from '@mysms/sdk/comms/apis/PostApi';
import { TaskApi } from '@mysms/sdk/comms/apis/TaskApi';
import { allPages, FIRST_PAGE, nextPage, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { send } from '../records/api';
import type { RecordPage } from '../records/types';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { shiftDay, weekOf } from './timetable';

/**
 * One notification of the account's inbox, as the contract's comms `InAppNotification` carries it. The inbox
 * operations have no documented controller on this branch yet (comms serves them on ops/comms), so the generated SDK
 * has no client for them: the inbox is read through its contract paths.
 */
export interface InAppNotification {
  id: string;
  title?: string;
  body?: string;
  actionUrl?: string;
  readAt?: string;
  createTime?: string;
  entityType?: string;
  entityId?: string;
  cohortId?: string;
}
export type CalendarItem = comms.CalendarItem;
export type CalendarItemType = comms.CalendarItemItemTypeEnum;
export type CalendarProvider = comms.ExportCalendarRequestProviderEnum;

/** How far back the inbox reads in one go: its table is partitioned by month, so every read names a range. */
export const INBOX_WINDOW_DAYS = 90;
/** The longest range the partitioned inbox answers (api/README "Partitioned lists"). */
export const INBOX_MAX_DAYS = 366;

const DAY_MS = 86_400_000;

const INBOX = '/v1/comms/notifications';

/**
 * comms.ListInAppNotifications: the signed-in account's own notifications (the server lists only the caller's,
 * archived ones left out), newest first, from `days` ago until now, page by page. `unread` keeps those not read yet.
 */
export function useInbox(days: number, unread = false, now: () => number = Date.now) {
  const { api } = useRuntime();
  return useInfiniteQuery({
    queryKey: queryKey('comms', 'inbox', { days, unread }),
    queryFn: async ({ pageParam }) => {
      const to = now();
      const from = new Date(to - Math.min(days, INBOX_MAX_DAYS) * DAY_MS).toISOString();
      const range = `createTime >= "${from}" AND createTime <= "${new Date(to).toISOString()}"`;
      const { page, cursor } = pageQuery(pageParam);
      const query = [
        ['filter', unread ? `${range} AND readAt = null` : range],
        ['sort', 'createTime,desc'],
        ['size', '50'],
        ['page', String(page)],
        ...(cursor ? [['cursor', cursor]] : []),
      ]
        .map(([name, value]) => `${name}=${encodeURIComponent(value as string)}`)
        .join('&');
      const response = await send(api, 'GET', `${INBOX}?${query}`);
      return (await response.json()) as Omit<RecordPage, 'data'> & { data: InAppNotification[] };
    },
    initialPageParam: FIRST_PAGE,
    getNextPageParam: (last) => nextPage(last.paging),
    staleTime: STALE_TIME.list,
  });
}

/** The inbox steps: one notification read, every one read, or a choice archived; each refreshes the inbox. */
export function useInboxSteps() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => qc.invalidateQueries({ queryKey: queryKey('comms', 'inbox') });
  return {
    markRead: useMutation({
      mutationFn: (inAppNotificationId: string) =>
        send(api, 'POST', `${INBOX}/${encodeURIComponent(inAppNotificationId)}:markRead`, {}),
      onSettled: refresh,
    }),
    markAllRead: useMutation({
      mutationFn: () => send(api, 'POST', `${INBOX}:markAllRead`, {}),
      onSettled: refresh,
    }),
    archive: useMutation({
      mutationFn: (notificationIds: readonly string[]) =>
        send(api, 'POST', `${INBOX}:archive`, { notificationIds: [...notificationIds] }),
      onSettled: refresh,
    }),
  };
}

export interface CalendarRange {
  /** Instants (ISO-8601), the range the calendar shows. */
  from: string;
  to: string;
  cohortId?: string;
}

/**
 * comms.GetMyCalendar: what the signed-in account's calendar holds in a range — school, class and cohort events it
 * is invited to, its own personal events and holidays — merged and ordered by the server, every page of it.
 */
export function useMyCalendar(range: CalendarRange | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('comms', 'my-calendar', range),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(EventApi).getMyCalendar({
            ...(range as CalendarRange),
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: range !== null,
    staleTime: STALE_TIME.list,
  });
}

/** comms.ExportCalendar: the account's calendar for a range sent to their Google or Microsoft calendar (an operation). */
export function useExportCalendar() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (v: { provider: CalendarProvider; from: string; to: string }) =>
      call(
        api.get(CalendarExportApi).exportCalendar({
          exportCalendarRequest: { provider: v.provider, from: v.from, to: v.to },
        }),
      ),
  });
}

/**
 * comms.MarkConversationRead: the caller's read mark in a conversation moves to now (only a participant's own mark;
 * the conversation list then shows it read).
 */
export function useMarkConversationRead() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (conversationId: string) =>
      call(api.get(ConversationApi).markConversationRead({ conversationId, body: {} })),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey('records', 'comms.Conversation') }),
  });
}

/**
 * comms.CloseNoticeReplies: no more replies to a notice (its discussion stays readable), under the etag the screen
 * last read so a notice changed elsewhere answers 412.
 */
export function useCloseNoticeReplies() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { noticeId: string; etag: string | null }) =>
      call(
        api.get(NoticeApi).closeNoticeReplies({
          noticeId: v.noticeId,
          closeNoticeRepliesRequest: v.etag ? { etag: v.etag } : {},
        }),
      ),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey('records', 'comms.Notice#item') }),
  });
}

/**
 * comms.HideMessageForMe: a message disappears from the caller's own view of the conversation only (the others still
 * see it); the conversation's messages are read again.
 */
export function useHideMessageForMe() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (messageId: string) =>
      call(api.get(ConversationApi).hideMessageForMe({ messageId, body: {} })),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey('records', 'comms.ConversationMessage') }),
  });
}

/** How much of the calendar one screen shows (ui/02 COM-03: month, week, day). */
export type CalendarView = 'month' | 'week' | 'day';

/** The calendar days (both included) a view shows around a day: its month, its Monday-to-Sunday week, or the day. */
export function calendarDays(view: CalendarView, day: string): { from: string; to: string } {
  if (view === 'day') return { from: day, to: day };
  if (view === 'week') return weekOf(day);
  const from = `${day.slice(0, 7)}-01`;
  return { from, to: shiftDay(`${shiftDay(from, 31).slice(0, 7)}-01`, -1) };
}

/** The day the view lands on one step back (-1) or on (1): a month, a week or a day further. */
export function shiftCalendar(view: CalendarView, day: string, step: -1 | 1): string {
  if (view === 'day') return shiftDay(day, step);
  if (view === 'week') return shiftDay(day, 7 * step);
  const { from, to } = calendarDays('month', day);
  return step === 1 ? shiftDay(to, 1) : `${shiftDay(from, -1).slice(0, 7)}-01`;
}

/**
 * The instants to ask the calendar for, for some days: a day more on each side, so an item early or late in the
 * viewer's own time zone is in the answer; the screen places each item on its local day.
 */
export function calendarRange(days: { from: string; to: string }, cohortId?: string): CalendarRange {
  return {
    from: `${shiftDay(days.from, -1)}T00:00:00Z`,
    to: `${shiftDay(days.to, 2)}T00:00:00Z`,
    ...(cohortId ? { cohortId } : {}),
  };
}

/** comms.SendTestMessage: a fixed test text through one channel, to the caller's own verified address. */
export function useSendTestMessage() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (v: { notificationChannelId: string; to: string }) =>
      call(
        api.get(NotificationChannelApi).sendTestMessage({
          notificationChannelId: v.notificationChannelId,
          sendTestMessageRequest: { to: v.to },
        }),
      ),
  });
}

/**
 * comms.GetFeed: the posts the caller may read — on the class pages they belong to, and the school-wide ones of their
 * institutions — newest first, page by page; narrowed to one class page or the caller's saved posts on request.
 */
export function useFeed(params: { classPageId?: string; savedOnly?: boolean } = {}) {
  const { api } = useRuntime();
  return useInfiniteQuery({
    queryKey: queryKey('comms', 'feed', params),
    queryFn: ({ pageParam }) =>
      call(api.get(PostApi).getFeed({ ...params, size: 20, ...pageQuery(pageParam) })),
    initialPageParam: FIRST_PAGE,
    getNextPageParam: (last) => nextPage(last.paging),
    staleTime: STALE_TIME.list,
  });
}

/** comms.ClaimTask: the caller takes an open task offered to a class or group they belong to. */
export function useClaimTask() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) => call(api.get(TaskApi).claimTask({ taskId, body: {} })),
    onSettled: () => qc.invalidateQueries({ queryKey: queryKey('records', 'comms.Task') }),
  });
}
