import {
  allOf,
  resourceDef,
  useCan,
  useCreateRecord,
  useI18n,
  useInfiniteRecords,
  type RecordRow,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, SelectField, Skeleton, Switch, TextArea, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { FileDownloadButton } from '../../files/FileTransfer';
import { UUID } from '../ids';
import { problemText } from '../recordText';

const NOTICES = resourceDef('comms.Notice');
/** Who a new notice is for; a cohort's notices (LMS-28) are always for the cohort. */
const AUDIENCES = ['school', 'class', 'teachers', 'roles'] as const;
const AUDIENCE_LABEL: Readonly<Record<(typeof AUDIENCES)[number], MessageKey>> = {
  school: 'comms.notices.audience.school',
  class: 'comms.notices.audience.class',
  teachers: 'comms.notices.audience.teachers',
  roles: 'comms.notices.audience.roles',
};

/** Writes a notice as a draft (comms.CreateNotice) and opens it in the composer for its audiences and publishing. */
function NewNotice({ cohortId }: { cohortId: string }) {
  const { t } = useI18n();
  const navigate = useNavigate();
  const create = useCreateRecord(NOTICES as NonNullable<typeof NOTICES>);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [audience, setAudience] = useState<(typeof AUDIENCES)[number]>('school');
  const ready = title.trim() !== '' && body.trim() !== '';
  return (
    <Card title={t('comms.notices.new')}>
      {create.error ? <Banner intent="danger">{problemText(t, create.error)}</Banner> : null}
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault();
          if (!ready) return;
          create.mutate(
            {
              values: {
                title: title.trim(),
                body: body.trim(),
                ...(cohortId ? { audience: 'cohort', cohortId } : { audience }),
              },
            },
            { onSuccess: (row) => void navigate(`/app/comms/notices/${row.id}`) },
          );
        }}
      >
        <TextInput
          label={t('comms.notices.title')}
          value={title}
          maxLength={200}
          onChange={(e) => setTitle(e.target.value)}
        />
        <TextArea
          label={t('comms.notices.body')}
          value={body}
          rows={5}
          onChange={(e) => setBody(e.target.value)}
        />
        {cohortId ? null : (
          <SelectField
            label={t('comms.notices.audience')}
            value={audience}
            options={AUDIENCES.map((value) => ({ value, label: t(AUDIENCE_LABEL[value]) }))}
            onChange={(e) => setAudience(e.target.value as (typeof AUDIENCES)[number])}
          />
        )}
        <div className="form__actions">
          <Button type="submit" disabled={!ready} loading={create.isPending}>
            {t('comms.notices.saveDraft')}
          </Button>
        </div>
      </form>
    </Card>
  );
}

/** One notice as the board shows it: title, when, its text with the school's line breaks, and its attachment. */
function NoticeCard({ row, mayCompose }: { row: RecordRow; mayCompose: boolean }) {
  const { t, format } = useI18n();
  const when = typeof row.publishAt === 'string' ? row.publishAt : (row.createTime as string | undefined);
  return (
    <li>
      <Card title={String(row.title ?? '')}>
        <p className="hint">
          {[
            when ? format.dateTime(when) : '',
            row.isPublished === true ? '' : t('comms.notices.draft'),
            row.isClosed === true ? t('comms.notices.deactivated') : '',
          ]
            .filter(Boolean)
            .join(' · ')}
        </p>
        <p className="consent-notice">{String(row.body ?? '')}</p>
        <div className="chip-row">
          {typeof row.attachmentFileId === 'string' ? (
            <FileDownloadButton fileId={row.attachmentFileId} label={t('comms.notices.attachment')} />
          ) : null}
          {mayCompose ? <Link to={`/app/comms/notices/${row.id}`}>{t('comms.notices.compose')}</Link> : null}
        </div>
      </Card>
    </li>
  );
}

/**
 * COM-01 Notice board: the notices the signed-in account is meant to see, newest first — the server keeps the list
 * to the caller's audiences and institutions. A role that writes notices also sees drafts on request, writes a new
 * one and opens any in the composer (COM-02). With `?cohort=` the board is one cohort's announcements (LMS-28):
 * only that cohort's notices, and new ones are for the cohort.
 */
export function NoticeBoardScreen() {
  const { t } = useI18n();
  const [search, setSearch] = useSearchParams();
  const asked = search.get('cohort') ?? '';
  const cohortId = UUID.test(asked) ? asked : '';
  const drafts = search.has('drafts');
  const mayRead = useCan('comms.notice.read');
  const mayCreate = useCan('comms.notice.create');
  const mayCompose = useCan('comms.notice.update');
  const notices = useInfiniteRecords(mayRead ? NOTICES : undefined, {
    filter: allOf(
      cohortId ? `cohortId = "${cohortId}"` : undefined,
      drafts && mayCompose ? 'isPublished = false' : 'isPublished = true',
    ),
    sort: ['createTime,desc'],
    size: 20,
  });
  const rows = notices.data?.pages.flatMap((page) => page.data) ?? [];
  const title = cohortId ? t('screen.LMS28.title') : t('screen.COM01.title');
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">
          {cohortId ? t('comms.notices.cohortIntro') : t('comms.notices.intro')}
        </p>
      </header>
      {!mayRead ? <Banner intent="info">{t('adm.resource.noAccess')}</Banner> : null}
      {mayRead && mayCompose ? (
        <Switch
          label={t('comms.notices.showDrafts')}
          checked={drafts}
          onChange={(on) =>
            setSearch(
              (previous) => {
                const next = new URLSearchParams(previous);
                if (on) next.set('drafts', '');
                else next.delete('drafts');
                return next;
              },
              { replace: true },
            )
          }
        />
      ) : null}
      {mayCreate ? <NewNotice cohortId={cohortId} /> : null}
      {notices.isPending && mayRead ? <Skeleton label={t('common.state.loading')} lines={5} /> : null}
      {notices.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void notices.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {problemText(t, notices.error)}
        </Banner>
      ) : null}
      {notices.isSuccess && rows.length === 0 ? <p className="hint">{t('comms.notices.none')}</p> : null}
      {rows.length > 0 ? (
        <ul className="leave-list" aria-label={title}>
          {rows.map((row) => (
            <NoticeCard key={row.id} row={row} mayCompose={mayCompose} />
          ))}
        </ul>
      ) : null}
      {notices.hasNextPage ? (
        <Button
          variant="secondary"
          loading={notices.isFetchingNextPage}
          onClick={() => void notices.fetchNextPage()}
        >
          {t('common.list.more')}
        </Button>
      ) : null}
    </div>
  );
}
