import {
  INBOX_MAX_DAYS,
  INBOX_WINDOW_DAYS,
  resolveLink,
  searchOf,
  toRoutePath,
  useCan,
  useI18n,
  useInbox,
  useInboxSteps,
  type InAppNotification,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, Switch } from '@mysms/ui-web';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { problemText } from '../recordText';

/** Where a notification's link opens in the web app, or nothing for a link no web screen answers. */
function webTarget(actionUrl: string | undefined): string | null {
  if (!actionUrl || resolveLink(actionUrl, 'W', '').kind !== 'screen') return null;
  const search = searchOf(actionUrl);
  return `/app${toRoutePath(actionUrl) ?? ''}${search ? `?${search}` : ''}`;
}

/**
 * ACC-09 Notification inbox: the signed-in account's own notifications (the server lists only the caller's), newest
 * first over the last 90 days and earlier on request (at most a year: the inbox is partitioned by month). Opening one
 * marks it read and follows its link to the screen it is about; all can be marked read at once, and read ones
 * archived. Unread ones are told apart by weight and by their label, not by colour alone.
 */
export function InboxScreen() {
  const { t, format } = useI18n();
  const navigate = useNavigate();
  const mayRead = useCan('comms.in_app_notification.read');
  const mayMark = useCan('comms.in_app_notification.mark_notification_read');
  const mayMarkAll = useCan('comms.in_app_notification.mark_all_notifications_read');
  const mayArchive = useCan('comms.in_app_notification.archive_notifications');
  const [days, setDays] = useState(INBOX_WINDOW_DAYS);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const inbox = useInbox(days, unreadOnly);
  const steps = useInboxSteps();
  const rows: InAppNotification[] = inbox.data?.pages.flatMap((page) => page.data) ?? [];
  const failure = steps.markRead.error ?? steps.markAllRead.error ?? steps.archive.error;
  const open = (row: InAppNotification) => {
    const target = webTarget(row.actionUrl ?? undefined);
    if (row.id && !row.readAt && mayMark) steps.markRead.mutate(row.id);
    if (target) void navigate(target);
  };
  const readIds = rows.flatMap((row) => (row.id && row.readAt ? [row.id] : []));
  if (!mayRead)
    return (
      <div className="page">
        <h1 className="page__title">{t('screen.ACC09.title')}</h1>
        <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
      </div>
    );
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ACC09.title')}</h1>
        <p className="page__subtitle">{t('comms.inbox.intro')}</p>
        <div className="page__actions">
          {mayMarkAll ? (
            <Button
              variant="secondary"
              loading={steps.markAllRead.isPending}
              onClick={() => steps.markAllRead.mutate()}
            >
              {t('comms.inbox.markAll')}
            </Button>
          ) : null}
          {mayArchive && readIds.length > 0 ? (
            <Button
              variant="secondary"
              loading={steps.archive.isPending}
              onClick={() => steps.archive.mutate(readIds)}
            >
              {t('comms.inbox.archiveRead', { count: readIds.length })}
            </Button>
          ) : null}
        </div>
      </header>
      <Switch label={t('comms.inbox.unreadOnly')} checked={unreadOnly} onChange={setUnreadOnly} />
      {failure ? <Banner intent="danger">{problemText(t, failure)}</Banner> : null}
      {inbox.isPending ? <Skeleton label={t('common.state.loading')} lines={5} /> : null}
      {inbox.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void inbox.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {problemText(t, inbox.error)}
        </Banner>
      ) : null}
      {inbox.isSuccess && rows.length === 0 ? (
        <Card title={t('comms.inbox.empty')}>
          <p className="hint">{t('comms.inbox.emptyHint', { days: format.number(days) })}</p>
        </Card>
      ) : null}
      {rows.length > 0 ? (
        <ul className="leave-list" aria-label={t('screen.ACC09.title')}>
          {rows.map((row) => (
            <li key={row.id} className="leave-list__item">
              <div className={`leave-list__what${row.readAt ? '' : ' feed__unread'}`}>
                <span>{row.title ?? t('comms.inbox.untitled')}</span>
                <span className="hint">
                  {[
                    row.readAt ? t('comms.inbox.read') : t('comms.inbox.unread'),
                    row.createTime ? format.dateTime(row.createTime) : '',
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
                {row.body ? <span className="hint">{row.body}</span> : null}
              </div>
              {webTarget(row.actionUrl ?? undefined) ? (
                <Button variant="secondary" onClick={() => open(row)}>
                  {t('comms.inbox.open')}
                </Button>
              ) : !row.readAt && mayMark && row.id ? (
                <Button variant="secondary" onClick={() => steps.markRead.mutate(row.id as string)}>
                  {t('comms.inbox.markRead')}
                </Button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
      {inbox.hasNextPage ? (
        <Button
          variant="secondary"
          loading={inbox.isFetchingNextPage}
          onClick={() => void inbox.fetchNextPage()}
        >
          {t('common.list.more')}
        </Button>
      ) : inbox.isSuccess && days < INBOX_MAX_DAYS ? (
        <Button
          variant="secondary"
          onClick={() => setDays(Math.min(days + INBOX_WINDOW_DAYS, INBOX_MAX_DAYS))}
        >
          {t('comms.inbox.earlier')}
        </Button>
      ) : null}
    </div>
  );
}
