import {
  lmsFailureText,
  useCan,
  useCohort,
  useCohortProgress,
  useCohortStep,
  useCohortSyllabusLink,
  useEnrollInCohort,
  useI18n,
  useSendCohortAnnouncement,
  useSession,
  useSetStudentAccess,
  type CohortStep,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, Dialog, TextArea, TextInput, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { CohortStatus, LmsRetryBanner } from './lmsShared';
import { openDownload } from './lmsText';

const STEP_LABEL: Readonly<Record<CohortStep, MessageKey>> = {
  publish: 'lms.cohort.step.publish',
  unpublish: 'lms.cohort.step.unpublish',
  activate: 'lms.cohort.step.activate',
  deactivate: 'lms.cohort.step.deactivate',
  archive: 'lms.cohort.step.archive',
};

/** The lifecycle steps the cohort's state allows, in the order the header offers them. */
function allowedSteps(lifecycle: string, published: boolean): CohortStep[] {
  const steps: CohortStep[] = [];
  if (!published && (lifecycle === 'draft' || lifecycle === 'active')) steps.push('publish');
  if (published) steps.push('unpublish');
  if (lifecycle === 'draft' || lifecycle === 'suspended') steps.push('activate');
  if (lifecycle === 'active') steps.push('deactivate');
  if (lifecycle === 'active' || lifecycle === 'suspended' || lifecycle === 'completed') steps.push('archive');
  return steps;
}

/**
 * LMS-21 Cohort home: the cohort's header with its lifecycle steps, the member's way into its tabs (roster,
 * schedule, forums, analytics, release rules, outline), the syllabus download, each student's progress, and the
 * teacher's announcement to every student (LMS-28 lives here until comms' notice screen carries cohort audiences).
 */
export function CohortHomeScreen() {
  const { t, format } = useI18n();
  const { cohortId = '' } = useParams();
  const me = useSession((s) => s.me?.user?.id ?? null);
  const cohort = useCohort(cohortId || null);
  const progress = useCohortProgress(cohortId || null);
  const step = useCohortStep();
  const access = useSetStudentAccess();
  const syllabus = useCohortSyllabusLink();
  const announce = useSendCohortAnnouncement();
  const enroll = useEnrollInCohort();
  const mayManage = useCan('lms.cohort.update');
  const mayAnnounce = useCan('lms.cohort.send_cohort_announcement');
  const mayEnroll = useCan('lms.cohort.enroll_in_cohort');
  const [announcing, setAnnouncing] = useState(false);
  const [subject, setSubject] = useState('');
  const [content, setContent] = useState('');
  const [sent, setSent] = useState<number | null>(null);
  const [problem, setProblem] = useState<string | null>(null);

  if (cohort.isPending) return <div className="page">{t('common.state.loading')}</div>;
  if (cohort.isError || !cohort.data)
    return (
      <div className="page">
        <LmsRetryBanner onRetry={() => void cohort.refetch()} />
      </div>
    );
  const row = cohort.data.data;
  const etag = cohort.data.etag;
  const lifecycle = row.lifecycleStatus ?? 'draft';

  const act = (chosen: CohortStep) => {
    setProblem(null);
    step.mutate(
      { cohortId, step: chosen, etag },
      { onError: (error) => setProblem(lmsFailureText(t, error)) },
    );
  };
  const send = () => {
    setProblem(null);
    announce.mutate(
      { cohortId, subject, content },
      {
        onSuccess: (result) => {
          setSent(result.queued ?? 0);
          setAnnouncing(false);
          setSubject('');
          setContent('');
        },
        onError: (error) => setProblem(lmsFailureText(t, error)),
      },
    );
  };

  const progressColumns: Column<NonNullable<typeof progress.data>['items'][number]>[] = [
    { key: 'user', header: t('lms.cohort.student'), render: (p) => p.userId },
    {
      key: 'done',
      header: t('lms.cohort.progressDone'),
      render: (p) => t('lms.cohort.progressOf', { done: p.assessmentsCompleted, total: p.assessmentsTotal }),
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{row.title}</h1>
        <CohortStatus cohort={row} />
        <p className="page__subtitle">
          {t('lms.cohort.dates', { from: format.day(row.startDate), to: format.day(row.endDate) })}
          {typeof row.seatCount === 'number' ? ` · ${t('lms.cohort.seats', { seats: row.seatCount })}` : ''}
        </p>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      {sent !== null ? (
        <Banner intent="success">{t('lms.cohort.announcementSent', { queued: sent })}</Banner>
      ) : null}
      <nav className="chip-row" aria-label={t('lms.cohort.tabs')}>
        <Link to={`/app/lms/cohorts/${cohortId}/roster`}>{t('screen.LMS23.title')}</Link>
        <Link to={`/app/lms/cohorts/${cohortId}/schedule`}>{t('screen.LMS25.title')}</Link>
        <Link to={`/app/lms/cohorts/${cohortId}/forums`}>{t('screen.LMS26.title')}</Link>
        <Link to={`/app/lms/cohorts/${cohortId}/outline`}>{t('screen.LMS22.title')}</Link>
        <Link to={`/app/lms/cohorts/${cohortId}/release-rules`}>{t('screen.LMS29.title')}</Link>
        <Link to={`/app/lms/cohorts/${cohortId}/analytics`}>{t('screen.LMS30.title')}</Link>
      </nav>
      <div className="page__grid">
        <Card
          title={t('lms.cohort.actions')}
          actions={
            <>
              <Button
                onClick={() => syllabus.mutate({ cohortId }, { onSuccess: openDownload })}
                disabled={syllabus.isPending}
              >
                {t('lms.cohort.syllabus')}
              </Button>
              {mayEnroll && row.isPublished && row.allowSelfEnrollment && !me ? null : null}
            </>
          }
        >
          {mayManage ? (
            <div className="chip-row">
              {allowedSteps(lifecycle, Boolean(row.isPublished)).map((chosen) => (
                <Button key={chosen} onClick={() => act(chosen)} disabled={step.isPending}>
                  {t(STEP_LABEL[chosen])}
                </Button>
              ))}
            </div>
          ) : (
            <p className="hint">{t('lms.cohort.memberHint')}</p>
          )}
          {mayEnroll && row.isPublished && row.allowSelfEnrollment ? (
            <Button
              onClick={() =>
                enroll.mutate({ cohortId }, { onError: (error) => setProblem(lmsFailureText(t, error)) })
              }
              disabled={enroll.isPending}
            >
              {t('lms.cohort.enroll')}
            </Button>
          ) : null}
          {mayAnnounce ? (
            <Button onClick={() => setAnnouncing(true)}>{t('lms.cohort.announce')}</Button>
          ) : null}
        </Card>
        <Card title={t('lms.cohort.accessWindow')}>
          <p className="hint">{t('lms.cohort.accessHint')}</p>
          <p>
            {t('lms.cohort.accessFrom')}: {row.studentAccessFrom ? format.day(row.studentAccessFrom) : '—'} ·{' '}
            {t('lms.cohort.accessUntil')}: {row.studentAccessUntil ? format.day(row.studentAccessUntil) : '—'}
          </p>
          {mayManage ? (
            <Button
              onClick={() =>
                access.mutate(
                  { cohortId, studentAccessFrom: row.startDate, studentAccessUntil: undefined },
                  { onError: (error) => setProblem(lmsFailureText(t, error)) },
                )
              }
              disabled={access.isPending}
            >
              {t('lms.cohort.accessFromStart')}
            </Button>
          ) : null}
        </Card>
        <Card title={t('lms.cohort.progress')}>
          {progress.isError ? (
            <LmsRetryBanner onRetry={() => void progress.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.cohort.progress')}
              columns={progressColumns}
              rows={progress.data?.items}
              rowKey={(p) => String(p.userId)}
              loading={progress.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.cohort.noProgress')}
            />
          )}
        </Card>
      </div>
      <Dialog
        open={announcing}
        title={t('lms.cohort.announce')}
        onClose={() => setAnnouncing(false)}
        footer={
          <>
            <Button onClick={() => setAnnouncing(false)}>{t('common.action.cancel')}</Button>
            <Button onClick={send} disabled={announce.isPending || !subject.trim() || !content.trim()}>
              {t('lms.cohort.announceSend')}
            </Button>
          </>
        }
      >
        <p className="hint">{t('lms.cohort.announceHint')}</p>
        <TextInput
          label={t('lms.cohort.announceSubject')}
          value={subject}
          onChange={(e) => setSubject(e.target.value)}
        />
        <TextArea
          label={t('lms.cohort.announceContent')}
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
      </Dialog>
    </div>
  );
}
