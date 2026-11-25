import {
  allOf,
  calendarDays,
  calendarRange,
  INBOX_MAX_DAYS,
  INBOX_WINDOW_DAYS,
  localDay,
  resolveLink,
  resourceDef,
  shiftCalendar,
  useAllRecords,
  useCan,
  useClaimTask,
  useCreateRecord,
  useFeed,
  useHideMessageForMe,
  useI18n,
  useInbox,
  useInboxSteps,
  useInfiniteRecords,
  useMarkConversationRead,
  useMyCalendar,
  useSession,
  type CalendarItem,
  type RecordRow,
  type ClientKind,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useEffect, useState, type ReactNode } from 'react';
import { View } from 'react-native';
import { RecordEditor, WorkflowActions } from './records';
import { textAreaHeight } from './recordText';

const NOTICES = resourceDef('comms.Notice');
const MESSAGES = resourceDef('comms.ConversationMessage');
const EVENTS = resourceDef('comms.Event');
const ATTENDEES = resourceDef('comms.EventAttendee');
const FEEDBACK = resourceDef('comms.FeedbackThread');
const FEEDBACK_MESSAGES = resourceDef('comms.FeedbackMessage');
const COMMENTS = resourceDef('comms.PostComment');
const TASKS = resourceDef('comms.Task');

/** The page frame every screen here shares: heading, its body, and the way back. */
function Page({
  title,
  tablet,
  onBack,
  children,
}: {
  title: string;
  tablet: boolean;
  onBack: () => void;
  children: ReactNode;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {title}
      </Text>
      {children}
      <View style={{ marginTop: theme.space[7] }}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}

/** A read's loading, failure (with retry) and empty states, said the same way on every screen here. */
function ReadState({
  query,
  empty,
  emptyText,
}: {
  query: { isPending: boolean; isError: boolean; refetch: () => unknown };
  empty: boolean;
  emptyText: string;
}) {
  const { t } = useI18n();
  if (query.isPending) return <Text muted>{t('common.state.loading')}</Text>;
  if (query.isError)
    return (
      <>
        <Banner intent="danger">{t('common.error.generic')}</Banner>
        <Button variant="secondary" onPress={() => void query.refetch()}>
          {t('common.action.retry')}
        </Button>
      </>
    );
  return empty ? <Text muted>{emptyText}</Text> : null;
}

/** One item of a list: a heading line, its detail lines, and what can be done with it. */
function Item({ title, lines, children }: { title: string; lines: readonly string[]; children?: ReactNode }) {
  const theme = useTheme();
  return (
    <View style={{ marginTop: theme.space[5], gap: theme.space[2] }}>
      <Text style={{ fontWeight: '600' }}>{title}</Text>
      {lines.filter(Boolean).map((line, index) => (
        <Text key={index} muted={index > 0}>
          {line}
        </Text>
      ))}
      {children}
    </View>
  );
}

/**
 * ACC-09 on phones and tablets: the account's own notifications, newest first; opening one marks it read and, when
 * its link is a screen of this app, opens it; all can be marked read at once.
 */
export function InboxScreen({
  client,
  tablet = false,
  onOpenLink,
  onBack,
}: {
  client: ClientKind;
  tablet?: boolean;
  onOpenLink: (link: string) => void;
  onBack: () => void;
}) {
  const { t, format } = useI18n();
  const mayRead = useCan('comms.in_app_notification.read');
  const mayMark = useCan('comms.in_app_notification.mark_notification_read');
  const mayMarkAll = useCan('comms.in_app_notification.mark_all_notifications_read');
  const [days, setDays] = useState(INBOX_WINDOW_DAYS);
  const inbox = useInbox(days);
  const steps = useInboxSteps();
  const rows = inbox.data?.pages.flatMap((page) => page.data) ?? [];
  return (
    <Page title={t('screen.ACC09.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayMarkAll && rows.some((row) => !row.readAt) ? (
        <Button
          variant="secondary"
          loading={steps.markAllRead.isPending}
          onPress={() => steps.markAllRead.mutate()}
        >
          {t('comms.inbox.markAll')}
        </Button>
      ) : null}
      {mayRead ? (
        <ReadState query={inbox} empty={rows.length === 0} emptyText={t('comms.inbox.empty')} />
      ) : null}
      {rows.map((row) => {
        const link = row.actionUrl ?? '';
        const opens = link !== '' && resolveLink(link, client, '').kind === 'screen';
        return (
          <Item
            key={row.id}
            title={row.title ?? t('comms.inbox.untitled')}
            lines={[
              [
                row.readAt ? t('comms.inbox.read') : t('comms.inbox.unread'),
                row.createTime ? format.dateTime(row.createTime) : '',
              ]
                .filter(Boolean)
                .join(' · '),
              row.body ?? '',
            ]}
          >
            {opens || (!row.readAt && mayMark) ? (
              <Button
                variant="secondary"
                onPress={() => {
                  if (row.id && !row.readAt && mayMark) steps.markRead.mutate(row.id);
                  if (opens) onOpenLink(link);
                }}
              >
                {opens ? t('comms.inbox.open') : t('comms.inbox.markRead')}
              </Button>
            ) : null}
          </Item>
        );
      })}
      {inbox.hasNextPage ? (
        <Button
          variant="secondary"
          loading={inbox.isFetchingNextPage}
          onPress={() => void inbox.fetchNextPage()}
        >
          {t('common.list.more')}
        </Button>
      ) : inbox.isSuccess && days < INBOX_MAX_DAYS ? (
        <Button
          variant="secondary"
          onPress={() => setDays(Math.min(days + INBOX_WINDOW_DAYS, INBOX_MAX_DAYS))}
        >
          {t('comms.inbox.earlier')}
        </Button>
      ) : null}
    </Page>
  );
}

/**
 * The notices for the account, newest first (COM-01), or one cohort's announcements (LMS-28). A teacher who writes
 * notices (T: COM-02) writes one here and publishes, unpublishes or deactivates it.
 */
export function NoticeList({ cohortId, tablet = false }: { cohortId?: string; tablet?: boolean }) {
  const { t, format } = useI18n();
  const mayRead = useCan('comms.notice.read');
  const mayCreate = useCan('comms.notice.create');
  const mayCompose = useCan('comms.notice.update');
  const [drafts, setDrafts] = useState(false);
  const [writing, setWriting] = useState(false);
  const notices = useInfiniteRecords(mayRead ? NOTICES : undefined, {
    filter: allOf(
      cohortId ? `cohortId = "${cohortId}"` : undefined,
      drafts ? 'isPublished = false' : 'isPublished = true',
    ),
    sort: ['createTime,desc'],
    size: 20,
  });
  const rows = notices.data?.pages.flatMap((page) => page.data) ?? [];
  const def = NOTICES as NonNullable<typeof NOTICES>;
  return (
    <>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayCompose ? (
        <ChoiceGroup
          label={t('comms.notices.showDrafts')}
          choices={[
            { value: 'published', label: t('comms.notices.published') },
            { value: 'drafts', label: t('comms.notices.draft') },
          ]}
          value={drafts ? 'drafts' : 'published'}
          onChange={(value) => setDrafts(value === 'drafts')}
        />
      ) : null}
      {mayCreate && !writing ? (
        <Button onPress={() => setWriting(true)}>{t('comms.notices.new')}</Button>
      ) : null}
      {writing ? (
        <RecordEditor
          def={def}
          tablet={tablet}
          initialValues={cohortId ? { audience: 'cohort', cohortId } : { audience: 'school' }}
          onDone={() => {
            setWriting(false);
            setDrafts(true);
          }}
          onCancel={() => setWriting(false)}
        />
      ) : null}
      {mayRead ? (
        <ReadState query={notices} empty={rows.length === 0} emptyText={t('comms.notices.none')} />
      ) : null}
      {rows.map((row) => (
        <Item
          key={row.id}
          title={String(row.title ?? '')}
          lines={[
            typeof row.createTime === 'string' ? format.dateTime(row.createTime) : '',
            String(row.body ?? ''),
          ]}
        >
          <WorkflowActions
            def={def}
            row={row}
            etag={row.etag ?? null}
            onReload={() => void notices.refetch()}
          />
        </Item>
      ))}
      {notices.hasNextPage ? (
        <Button variant="secondary" onPress={() => void notices.fetchNextPage()}>
          {t('common.list.more')}
        </Button>
      ) : null}
    </>
  );
}

/** COM-01 (and COM-02 on the teacher's phone) as a page of its own. */
export function NoticesScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  return (
    <Page title={t('screen.COM01.title')} tablet={tablet} onBack={onBack}>
      <NoticeList tablet={tablet} />
    </Page>
  );
}

/** COM-03 on phones and tablets: the account's calendar a week at a time, each item on its local day. */
export function CalendarScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format, timeZone } = useI18n();
  const theme = useTheme();
  const mayRead = useCan('comms.event.get_my_calendar');
  const [day, setDay] = useState(() => localDay(new Date(), timeZone));
  const days = calendarDays('week', day);
  const calendar = useMyCalendar(mayRead ? calendarRange(days) : null);
  const items = (calendar.data?.items ?? [])
    .map((item): [string, CalendarItem] => [localDay(item.startAt, timeZone), item])
    .filter(([on]) => on >= days.from && on <= days.to)
    .sort(([a, x], [b, y]) => a.localeCompare(b) || x.startAt.localeCompare(y.startAt));
  return (
    <Page title={t('screen.COM03.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      <Text>{t('comms.calendar.week', { from: format.day(days.from), to: format.day(days.to) })}</Text>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[3] }}>
        <Button variant="secondary" onPress={() => setDay(shiftCalendar('week', day, -1))}>
          {t('comms.calendar.previous')}
        </Button>
        <Button variant="secondary" onPress={() => setDay(shiftCalendar('week', day, 1))}>
          {t('comms.calendar.next')}
        </Button>
      </View>
      {mayRead ? (
        <ReadState query={calendar} empty={items.length === 0} emptyText={t('comms.calendar.empty')} />
      ) : null}
      {items.map(([on, item]) => (
        <Item
          key={`${item.itemType}-${item.id}`}
          title={item.title}
          lines={[
            [
              `${format.weekday(on)}, ${format.day(on)}`,
              item.allDay ? t('comms.calendar.allDay') : format.time(item.startAt),
              enumLabel(t, 'calendarItem', item.itemType),
              item.isCancelled ? t('comms.calendar.cancelled') : '',
            ]
              .filter(Boolean)
              .join(' · '),
          ]}
        />
      ))}
    </Page>
  );
}

/** One conversation on the phone: messages oldest first, sending, hiding one for oneself; opening marks it read. */
function ConversationThread({ conversationId }: { conversationId: string }) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const maySend = useCan('comms.conversation_message.create');
  const mayHide = useCan('comms.conversation_message.hide_message_for_me');
  const mayMark = useCan('comms.conversation.mark_conversation_read');
  const messages = useInfiniteRecords(MESSAGES, {
    parentId: conversationId,
    sort: ['createTime,desc'],
    size: 30,
  });
  const send = useCreateRecord(MESSAGES as NonNullable<typeof MESSAGES>);
  const hide = useHideMessageForMe();
  const { mutate: markRead } = useMarkConversationRead();
  const [text, setText] = useState('');
  useEffect(() => {
    if (mayMark) markRead(conversationId);
  }, [conversationId, mayMark, markRead]);
  const rows = [...(messages.data?.pages.flatMap((page) => page.data) ?? [])].reverse();
  return (
    <>
      {messages.hasNextPage ? (
        <Button variant="secondary" onPress={() => void messages.fetchNextPage()}>
          {t('comms.chat.earlier')}
        </Button>
      ) : null}
      <ReadState query={messages} empty={rows.length === 0} emptyText={t('comms.chat.noMessages')} />
      {rows.map((row) => (
        <Item
          key={row.id}
          title={row.senderUserId === me ? t('comms.chat.you') : t('comms.chat.them')}
          lines={[String(row.body ?? ''), row.createTime ? format.dateTime(String(row.createTime)) : '']}
        >
          {mayHide ? (
            <Button variant="secondary" onPress={() => hide.mutate(row.id)}>
              {t('comms.chat.hide')}
            </Button>
          ) : null}
        </Item>
      ))}
      {send.isError || hide.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {maySend ? (
        <View style={{ marginTop: theme.space[5], gap: theme.space[3] }}>
          <TextInput
            label={t('comms.chat.write')}
            value={text}
            multiline
            style={{ minHeight: textAreaHeight(theme, 3) }}
            onChangeText={setText}
          />
          <Button
            disabled={!text.trim()}
            loading={send.isPending}
            onPress={() =>
              send.mutate(
                { parentId: conversationId, values: { body: text.trim() } },
                { onSuccess: () => setText('') },
              )
            }
          >
            {t('comms.chat.send')}
          </Button>
          <Button variant="secondary" onPress={() => void messages.refetch()}>
            {t('comms.chat.refresh')}
          </Button>
        </View>
      ) : null}
    </>
  );
}

/** COM-05 on phones and tablets: the account's conversations, most recent first; one opens its messages. */
export function ConversationsScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format } = useI18n();
  const mayRead = useCan('comms.conversation.read');
  const [open, setOpen] = useState<RecordRow | null>(null);
  const conversations = useAllRecords(
    'comms.Conversation',
    { filter: 'isActive = true', sort: ['updateTime,desc'] },
    mayRead,
  );
  const rows = conversations.data?.items ?? [];
  if (open)
    return (
      <Page
        title={typeof open.title === 'string' && open.title ? open.title : t('comms.chat.untitled')}
        tablet={tablet}
        onBack={() => setOpen(null)}
      >
        <ConversationThread conversationId={open.id} />
      </Page>
    );
  return (
    <Page title={t('screen.COM05.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayRead ? (
        <ReadState query={conversations} empty={rows.length === 0} emptyText={t('comms.chat.none')} />
      ) : null}
      {rows.map((row) => (
        <Item
          key={row.id}
          title={typeof row.title === 'string' && row.title ? row.title : t('comms.chat.untitled')}
          lines={[
            typeof row.lastMessageAt === 'string'
              ? t('comms.chat.lastMessage', { when: format.dateTime(row.lastMessageAt) })
              : t('comms.chat.noMessages'),
          ]}
        >
          <Button variant="secondary" onPress={() => setOpen(row)}>
            {t('comms.chat.open')}
          </Button>
        </Item>
      ))}
    </Page>
  );
}

/**
 * COM-04 on phones: the school's coming events; the account says it will come (an attendee row of its own, RSVP
 * via attendees, ui/02) or takes that back.
 */
export function EventsScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format, timeZone } = useI18n();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const mayRead = useCan('comms.event.read');
  const mayRsvp = useCan('comms.event_attendee.create');
  const today = localDay(new Date(), timeZone);
  const events = useInfiniteRecords(mayRead ? EVENTS : undefined, {
    filter: `status = "active" AND startAt >= "${today}T00:00:00Z"`,
    sort: ['createTime,desc'],
    size: 20,
  });
  const rsvp = useCreateRecord(ATTENDEES as NonNullable<typeof ATTENDEES>);
  const [going, setGoing] = useState<ReadonlySet<string>>(new Set());
  const rows = events.data?.pages.flatMap((page) => page.data) ?? [];
  return (
    <Page title={t('screen.COM04.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayRead ? (
        <ReadState query={events} empty={rows.length === 0} emptyText={t('comms.events.none')} />
      ) : null}
      {rsvp.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {rows.map((row) => (
        <Item
          key={row.id}
          title={String(row.title ?? '')}
          lines={[
            [
              typeof row.startAt === 'string' ? format.dateTime(row.startAt) : '',
              typeof row.location === 'string' ? row.location : '',
            ]
              .filter(Boolean)
              .join(' · '),
            typeof row.description === 'string' ? row.description : '',
          ]}
        >
          {mayRsvp && me ? (
            going.has(row.id) ? (
              <Text>{t('comms.events.going')}</Text>
            ) : (
              <Button
                variant="secondary"
                loading={rsvp.isPending && rsvp.variables?.parentId === row.id}
                onPress={() =>
                  rsvp.mutate(
                    { parentId: row.id, values: { userId: me } },
                    { onSuccess: () => setGoing((old) => new Set(old).add(row.id)) },
                  )
                }
              >
                {t('comms.events.rsvp')}
              </Button>
            )
          ) : null}
        </Item>
      ))}
      {events.hasNextPage ? (
        <Button variant="secondary" onPress={() => void events.fetchNextPage()}>
          {t('common.list.more')}
        </Button>
      ) : null}
    </Page>
  );
}

/** One feedback thread: its messages, writing one, and closing or reopening the thread as its state allows. */
function FeedbackThreadView({ thread, onChanged }: { thread: RecordRow; onChanged: () => void }) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const mayWrite = useCan('comms.feedback_message.create');
  const messages = useAllRecords('comms.FeedbackMessage', { parentId: thread.id, sort: ['createTime'] });
  const write = useCreateRecord(FEEDBACK_MESSAGES as NonNullable<typeof FEEDBACK_MESSAGES>);
  const [text, setText] = useState('');
  const [category, setCategory] = useState('suggestion');
  const rows = messages.data?.items ?? [];
  return (
    <>
      <WorkflowActions
        def={FEEDBACK as NonNullable<typeof FEEDBACK>}
        row={thread}
        etag={thread.etag ?? null}
        onReload={onChanged}
      />
      <ReadState query={messages} empty={rows.length === 0} emptyText={t('comms.chat.noMessages')} />
      {rows.map((row) => (
        <Item
          key={row.id}
          title={row.senderUserId === me ? t('comms.chat.you') : t('comms.chat.them')}
          lines={[
            String(row.message ?? ''),
            [
              enumLabel(t, 'feedbackCategory', String(row.category ?? '')),
              row.createTime ? format.dateTime(String(row.createTime)) : '',
            ]
              .filter(Boolean)
              .join(' · '),
          ]}
        />
      ))}
      {mayWrite && thread.status !== 'closed' ? (
        <View style={{ marginTop: theme.space[5], gap: theme.space[3] }}>
          <ChoiceGroup
            label={t('comms.feedback.category')}
            choices={['suggestion', 'complaint', 'student_profile', 'app_feedback_or_bug', 'other'].map(
              (value) => ({
                value,
                label: enumLabel(t, 'feedbackCategory', value),
              }),
            )}
            value={category}
            onChange={setCategory}
          />
          <TextInput
            label={t('comms.chat.write')}
            value={text}
            multiline
            style={{ minHeight: textAreaHeight(theme, 3) }}
            onChangeText={setText}
          />
          {write.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
          <Button
            disabled={!text.trim()}
            loading={write.isPending}
            onPress={() =>
              write.mutate(
                { parentId: thread.id, values: { category, message: text.trim() } },
                { onSuccess: () => setText('') },
              )
            }
          >
            {t('comms.chat.send')}
          </Button>
        </View>
      ) : null}
    </>
  );
}

/** COM-07 on the parent's phone: the guardian's feedback threads with the school, each with its messages. */
export function FeedbackScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format } = useI18n();
  const mayRead = useCan('comms.feedback_thread.read');
  const threads = useAllRecords('comms.FeedbackThread', { sort: ['updateTime,desc'] }, mayRead);
  const [openId, setOpenId] = useState('');
  const rows = threads.data?.items ?? [];
  const open = rows.find((row) => row.id === openId);
  if (open)
    return (
      <Page title={t('screen.COM07.title')} tablet={tablet} onBack={() => setOpenId('')}>
        <FeedbackThreadView thread={open} onChanged={() => void threads.refetch()} />
      </Page>
    );
  return (
    <Page title={t('screen.COM07.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayRead ? (
        <ReadState query={threads} empty={rows.length === 0} emptyText={t('comms.feedback.none')} />
      ) : null}
      {rows.map((row) => (
        <Item
          key={row.id}
          title={enumLabel(t, 'feedbackStatus', String(row.status ?? 'open'))}
          lines={[typeof row.updateTime === 'string' ? format.dateTime(row.updateTime) : '']}
        >
          <Button variant="secondary" onPress={() => setOpenId(row.id)}>
            {t('comms.chat.open')}
          </Button>
        </Item>
      ))}
    </Page>
  );
}

/** The comments of a post and writing one. */
function PostComments({ postId }: { postId: string }) {
  const { t } = useI18n();
  const theme = useTheme();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const mayRead = useCan('comms.post_comment.read');
  const mayWrite = useCan('comms.post_comment.create');
  const comments = useAllRecords('comms.PostComment', { parentId: postId, sort: ['createTime'] }, mayRead);
  const write = useCreateRecord(COMMENTS as NonNullable<typeof COMMENTS>);
  const [text, setText] = useState('');
  return (
    <View style={{ marginLeft: theme.space[5], gap: theme.space[2] }}>
      {(comments.data?.items ?? []).map((row) => (
        <Text key={row.id} muted>
          {`${row.authorUserId === me ? t('comms.chat.you') : t('comms.chat.them')}: ${String(row.body ?? '')}`}
        </Text>
      ))}
      {mayWrite ? (
        <>
          <TextInput label={t('comms.feed.comment')} value={text} onChangeText={setText} />
          <Button
            variant="secondary"
            disabled={!text.trim()}
            loading={write.isPending}
            onPress={() =>
              write.mutate(
                { parentId: postId, values: { body: text.trim() } },
                { onSuccess: () => setText('') },
              )
            }
          >
            {t('comms.feed.send')}
          </Button>
        </>
      ) : null}
    </View>
  );
}

/** COM-08 on phones: the account's feed of class-page and school posts, newest first, with their comments. */
export function ClassFeedScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format } = useI18n();
  const mayRead = useCan('comms.post.get_feed');
  const feed = useFeed();
  const [openId, setOpenId] = useState('');
  const rows = feed.data?.pages.flatMap((page) => page.data) ?? [];
  return (
    <Page title={t('screen.COM08.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayRead ? <ReadState query={feed} empty={rows.length === 0} emptyText={t('comms.feed.none')} /> : null}
      {rows.map((post) => (
        <Item
          key={post.id}
          title={post.postedAt ? format.dateTime(post.postedAt) : t('comms.feed.post')}
          lines={[post.body]}
        >
          {openId === post.id && post.id ? (
            <PostComments postId={post.id} />
          ) : (
            <Button variant="secondary" onPress={() => setOpenId(post.id ?? '')}>
              {t('comms.feed.comments')}
            </Button>
          )}
        </Item>
      ))}
      {feed.hasNextPage ? (
        <Button variant="secondary" onPress={() => void feed.fetchNextPage()}>
          {t('common.list.more')}
        </Button>
      ) : null}
    </Page>
  );
}

/**
 * COM-09 on the teacher's phone: the teacher's open tasks, soonest due first — completed, reopened or snoozed as each
 * task's state allows — and the open tasks offered to their classes or groups, which they can claim.
 */
export function TasksScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format } = useI18n();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const mayRead = useCan('comms.task.read');
  const mayClaim = useCan('comms.task.claim_task');
  const [done, setDone] = useState(false);
  const tasks = useAllRecords(
    'comms.Task',
    { filter: `isCompleted = ${done ? 'true' : 'false'}`, sort: ['createTime'] },
    mayRead,
  );
  const claim = useClaimTask();
  // soonest due first (the contract does not sort tasks by due date)
  const rows = [...(tasks.data?.items ?? [])].sort((a, b) =>
    String(a.dueAt ?? '').localeCompare(String(b.dueAt ?? '')),
  );
  return (
    <Page title={t('screen.COM09.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      <ChoiceGroup
        label={t('comms.tasks.show')}
        choices={[
          { value: 'open', label: t('comms.tasks.open') },
          { value: 'done', label: t('comms.tasks.done') },
        ]}
        value={done ? 'done' : 'open'}
        onChange={(value) => setDone(value === 'done')}
      />
      {mayRead ? (
        <ReadState query={tasks} empty={rows.length === 0} emptyText={t('comms.tasks.none')} />
      ) : null}
      {claim.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {rows.map((row) => (
        <Item
          key={row.id}
          title={String(row.title ?? '')}
          lines={[
            [
              typeof row.dueAt === 'string' ? t('comms.tasks.due', { when: format.dateTime(row.dueAt) }) : '',
              row.priority ? enumLabel(t, 'taskPriority', String(row.priority)) : '',
            ]
              .filter(Boolean)
              .join(' · '),
            String(row.checklist ?? ''),
          ]}
        >
          {row.ownerUserId === me ? (
            <WorkflowActions
              def={TASKS as NonNullable<typeof TASKS>}
              row={row}
              etag={row.etag ?? null}
              onReload={() => void tasks.refetch()}
            />
          ) : mayClaim && row.assignmentMode === 'open' && !done ? (
            <Button variant="secondary" loading={claim.isPending} onPress={() => claim.mutate(row.id)}>
              {t('comms.tasks.claim')}
            </Button>
          ) : null}
        </Item>
      ))}
    </Page>
  );
}
