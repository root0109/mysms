import {
  lmsFailureText,
  useCan,
  useConferencingAccounts,
  useI18n,
  useJoinLiveClass,
  useLiveClasses,
  useLiveClassStartUrl,
  useLiveClassStep,
  useScheduleLiveClass,
  type LiveClass,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  StatusChip,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { LmsRetryBanner } from './lmsShared';
import { openDownload } from './lmsText';

/**
 * LMS-27 Live classes: every class the caller may see, joining one, the host's start link, scheduling a new class at
 * one of the tenant's meeting-provider accounts, ending it, reading attendance back and retrying the calendar
 * invites. Joining is provider-agnostic: the join link is the provider's, whatever it is.
 */
export function LiveClassesScreen() {
  const { t, format } = useI18n();
  const classes = useLiveClasses();
  const maySchedule = useCan('lms.live_class.schedule_live_class');
  const accounts = useConferencingAccounts(maySchedule);
  const schedule = useScheduleLiveClass();
  const join = useJoinLiveClass();
  const startUrl = useLiveClassStartUrl();
  const step = useLiveClassStep();
  const mayJoin = useCan('lms.live_class.join_live_class');
  const mayStart = useCan('lms.live_class.get_live_class_start_url');
  const mayEnd = useCan('lms.live_class.end_live_class');
  const maySync = useCan('lms.live_class.sync_live_class_attendance');
  const mayRetry = useCan('lms.live_class.retry_live_class_invites');
  const [scheduling, setScheduling] = useState(false);
  const [form, setForm] = useState({
    cohortId: '',
    conferencingAccountId: '',
    title: '',
    classDate: '',
    startTime: '10:00',
    durationMinutes: '45',
    timezone: 'Asia/Kolkata',
  });
  const [problem, setProblem] = useState<string | null>(null);

  const set = (field: keyof typeof form) => (value: string) => setForm((f) => ({ ...f, [field]: value }));
  const fail = (error: unknown) => setProblem(lmsFailureText(t, error));

  const columns: Column<LiveClass>[] = [
    { key: 'title', header: t('lms.live.title'), render: (c) => c.title },
    {
      key: 'when',
      header: t('lms.live.when'),
      render: (c) => `${c.classDate ? format.day(c.classDate) : ''} ${c.startTime ?? ''}`,
    },
    {
      key: 'state',
      header: t('lms.live.state'),
      render: (c) =>
        c.endedAt ? (
          <StatusChip intent="neutral" label={t('lms.live.ended')} />
        ) : (
          <StatusChip intent="success" label={t('lms.live.upcoming')} />
        ),
    },
    {
      key: 'invites',
      header: t('lms.live.invites'),
      render: (c) =>
        t('lms.live.inviteState', { state: c.inviteStatus ?? 'pending', attempts: c.inviteAttempts ?? 0 }),
    },
    {
      key: 'actions',
      header: t('lms.roster.actions'),
      render: (c) => {
        const id = String(c.id);
        return (
          <span className="chip-row">
            {mayJoin && !c.endedAt ? (
              <Button
                onClick={() => join.mutate({ liveClassId: id }, { onSuccess: openDownload, onError: fail })}
                disabled={join.isPending}
              >
                {t('lms.live.join')}
              </Button>
            ) : null}
            {mayStart && !c.endedAt ? (
              <Button
                onClick={() =>
                  startUrl.mutate({ liveClassId: id }, { onSuccess: openDownload, onError: fail })
                }
                disabled={startUrl.isPending}
              >
                {t('lms.live.start')}
              </Button>
            ) : null}
            {mayRetry && !c.endedAt ? (
              <Button
                onClick={() => step.mutate({ liveClassId: id, step: 'retryInvites' }, { onError: fail })}
                disabled={step.isPending}
              >
                {t('lms.live.retryInvites')}
              </Button>
            ) : null}
            {mayEnd && !c.endedAt ? (
              <Button
                onClick={() => step.mutate({ liveClassId: id, step: 'end' }, { onError: fail })}
                disabled={step.isPending}
              >
                {t('lms.live.end')}
              </Button>
            ) : null}
            {maySync && c.endedAt ? (
              <Button
                onClick={() => step.mutate({ liveClassId: id, step: 'syncAttendance' }, { onError: fail })}
                disabled={step.isPending}
              >
                {t('lms.live.syncAttendance')}
              </Button>
            ) : null}
          </span>
        );
      },
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS27.title')}</h1>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <Card
        title={t('lms.live.all')}
        actions={
          maySchedule ? <Button onClick={() => setScheduling(true)}>{t('lms.live.schedule')}</Button> : null
        }
      >
        {classes.isError ? (
          <LmsRetryBanner onRetry={() => void classes.refetch()} />
        ) : (
          <DataTable
            caption={t('lms.live.all')}
            columns={columns}
            rows={classes.data?.items}
            rowKey={(c) => String(c.id)}
            loading={classes.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('lms.live.none')}
          />
        )}
      </Card>
      <Dialog
        open={scheduling}
        title={t('lms.live.schedule')}
        onClose={() => setScheduling(false)}
        footer={
          <>
            <Button onClick={() => setScheduling(false)}>{t('common.action.cancel')}</Button>
            <Button
              onClick={() => {
                setProblem(null);
                schedule.mutate(
                  {
                    request: {
                      cohortId: form.cohortId.trim(),
                      conferencingAccountId: form.conferencingAccountId,
                      title: form.title.trim(),
                      classDate: form.classDate,
                      startTime: form.startTime,
                      durationMinutes: Number(form.durationMinutes),
                      timezone: form.timezone,
                    },
                  },
                  { onSuccess: () => setScheduling(false), onError: fail },
                );
              }}
              disabled={
                schedule.isPending ||
                !form.cohortId.trim() ||
                !form.conferencingAccountId ||
                !form.title.trim() ||
                !form.classDate
              }
            >
              {t('lms.live.schedule')}
            </Button>
          </>
        }
      >
        <p className="hint">{t('lms.live.scheduleHint')}</p>
        <TextInput
          label={t('lms.live.cohortId')}
          value={form.cohortId}
          onChange={(e) => set('cohortId')(e.target.value)}
        />
        <SelectField
          label={t('lms.live.account')}
          value={form.conferencingAccountId}
          onChange={(e) => set('conferencingAccountId')(e.target.value)}
          options={[
            { value: '', label: t('lms.live.pickAccount') },
            ...(accounts.data?.items ?? []).map((account) => ({
              value: String(account.id),
              label: `${account.accountName} (${account.provider})`,
            })),
          ]}
        />
        <TextInput
          label={t('lms.live.title')}
          value={form.title}
          onChange={(e) => set('title')(e.target.value)}
        />
        <TextInput
          label={t('lms.live.date')}
          type="date"
          value={form.classDate}
          onChange={(e) => set('classDate')(e.target.value)}
        />
        <TextInput
          label={t('lms.live.time')}
          value={form.startTime}
          onChange={(e) => set('startTime')(e.target.value)}
        />
        <TextInput
          label={t('lms.live.minutes')}
          value={form.durationMinutes}
          onChange={(e) => set('durationMinutes')(e.target.value)}
        />
        <TextInput
          label={t('lms.live.timezone')}
          value={form.timezone}
          onChange={(e) => set('timezone')(e.target.value)}
        />
      </Dialog>
    </div>
  );
}
