import {
  lmsFailureText,
  useApplyTimetableTemplate,
  useCan,
  useCohortSchedule,
  useCohortTimetable,
  useI18n,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Dialog, TextInput, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { LmsRetryBanner } from './lmsShared';

/**
 * LMS-25 Schedule & sessions: the cohort's timetable entries, weekly slots, sessions and holidays side by side, and
 * the teacher's step that stamps a timetable template onto the cohort. The slots, sessions and holidays are managed
 * row by row on the cohort's record screens; this screen is the week's reading view.
 */
export function CohortScheduleScreen() {
  const { t, format } = useI18n();
  const { cohortId = '' } = useParams();
  const timetable = useCohortTimetable(cohortId || null);
  const schedule = useCohortSchedule(cohortId || null);
  const apply = useApplyTimetableTemplate();
  const mayApply = useCan('lms.cohort.apply_timetable_template');
  const [applying, setApplying] = useState(false);
  const [templateId, setTemplateId] = useState('');
  const [problem, setProblem] = useState<string | null>(null);

  type Entry = NonNullable<typeof timetable.data>['items'][number];
  const timetableColumns: Column<Entry>[] = [
    {
      key: 'day',
      header: t('lms.schedule.day'),
      render: (e) =>
        e.entryDate ? format.day(e.entryDate) : t('lms.schedule.dayNumber', { day: e.dayNumber ?? 0 }),
    },
    { key: 'start', header: t('lms.schedule.start'), render: (e) => String(e.startTime ?? '') },
    { key: 'end', header: t('lms.schedule.end'), render: (e) => String(e.endTime ?? '') },
    { key: 'minutes', header: t('lms.schedule.minutes'), render: (e) => String(e.durationMinutes ?? '') },
  ];
  type Session = NonNullable<typeof schedule.data>['sessions'][number];
  const sessionColumns: Column<Session>[] = [
    {
      key: 'date',
      header: t('lms.schedule.date'),
      render: (row) => (row.startsAt ? format.day(row.startsAt) : ''),
    },
    { key: 'what', header: t('lms.schedule.what'), render: (row) => row.description ?? row.venue ?? '' },
  ];
  type Holiday = NonNullable<typeof schedule.data>['holidays'][number];
  const holidayColumns: Column<Holiday>[] = [
    {
      key: 'date',
      header: t('lms.schedule.date'),
      render: (h) => (h.startDate ? format.day(h.startDate) : ''),
    },
    { key: 'what', header: t('lms.schedule.what'), render: (h) => h.title ?? '' },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS25.title')}</h1>
        <p className="page__subtitle">
          <Link to={`/app/lms/cohorts/${cohortId}`}>{t('lms.cohort.back')}</Link>
        </p>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <div className="page__grid">
        <Card
          title={t('lms.schedule.timetable')}
          actions={
            mayApply ? (
              <Button onClick={() => setApplying(true)}>{t('lms.schedule.applyTemplate')}</Button>
            ) : null
          }
        >
          {timetable.isError ? (
            <LmsRetryBanner onRetry={() => void timetable.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.schedule.timetable')}
              columns={timetableColumns}
              rows={timetable.data?.items}
              rowKey={(e) => String(e.id)}
              loading={timetable.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.schedule.noTimetable')}
            />
          )}
        </Card>
        <Card title={t('lms.schedule.sessions')}>
          {schedule.isError ? (
            <LmsRetryBanner onRetry={() => void schedule.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.schedule.sessions')}
              columns={sessionColumns}
              rows={schedule.data?.sessions}
              rowKey={(s) => String(s.id)}
              loading={schedule.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.schedule.noSessions')}
            />
          )}
        </Card>
        <Card title={t('lms.schedule.holidays')}>
          {schedule.isError ? null : (
            <DataTable
              caption={t('lms.schedule.holidays')}
              columns={holidayColumns}
              rows={schedule.data?.holidays}
              rowKey={(h) => String(h.id)}
              loading={schedule.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.schedule.noHolidays')}
            />
          )}
        </Card>
      </div>
      <Dialog
        open={applying}
        title={t('lms.schedule.applyTemplate')}
        onClose={() => setApplying(false)}
        footer={
          <>
            <Button onClick={() => setApplying(false)}>{t('common.action.cancel')}</Button>
            <Button
              onClick={() => {
                setProblem(null);
                apply.mutate(
                  { cohortId, timetableTemplateId: templateId.trim() },
                  {
                    onSuccess: () => setApplying(false),
                    onError: (error) => setProblem(lmsFailureText(t, error)),
                  },
                );
              }}
              disabled={apply.isPending || !templateId.trim()}
            >
              {t('common.action.save')}
            </Button>
          </>
        }
      >
        <p className="hint">{t('lms.schedule.applyHint')}</p>
        <TextInput
          label={t('lms.schedule.templateId')}
          value={templateId}
          onChange={(e) => setTemplateId(e.target.value)}
        />
      </Dialog>
    </div>
  );
}
