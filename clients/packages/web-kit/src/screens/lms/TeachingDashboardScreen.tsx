import { useCohorts, useI18n, useLiveClasses, type Cohort, type LiveClass } from '@mysms/core';
import { Card, DataTable, type Column } from '@mysms/ui-web';
import { Link } from 'react-router';
import { CohortStatus, LmsRetryBanner } from './lmsShared';

/**
 * LMS-20 Teaching dashboard: the teacher's cohorts and the live classes coming up, each a step into its cohort's
 * tabs. Built from the cohort and live-class reads (the one-call lms.GetTeacherDashboard summary, served now, could
 * replace them).
 */
export function TeachingDashboardScreen() {
  const { t, format } = useI18n();
  const cohorts = useCohorts();
  const classes = useLiveClasses();

  const cohortColumns: Column<Cohort>[] = [
    {
      key: 'title',
      header: t('lms.teaching.cohort'),
      render: (c) => <Link to={`/app/lms/cohorts/${c.id}`}>{c.title}</Link>,
    },
    { key: 'state', header: t('lms.teaching.state'), render: (c) => <CohortStatus cohort={c} /> },
    {
      key: 'dates',
      header: t('lms.teaching.dates'),
      render: (c) => t('lms.cohort.dates', { from: format.day(c.startDate), to: format.day(c.endDate) }),
    },
  ];
  const upcoming = (classes.data?.items ?? []).filter((c) => !c.endedAt);
  const classColumns: Column<LiveClass>[] = [
    { key: 'title', header: t('lms.live.title'), render: (c) => c.title },
    {
      key: 'when',
      header: t('lms.live.when'),
      render: (c) => `${c.classDate ? format.day(c.classDate) : ''} ${c.startTime ?? ''}`,
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS20.title')}</h1>
      </header>
      <div className="page__grid">
        <Card title={t('lms.teaching.cohorts')}>
          {cohorts.isError ? (
            <LmsRetryBanner onRetry={() => void cohorts.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.teaching.cohorts')}
              columns={cohortColumns}
              rows={cohorts.data?.items}
              rowKey={(c) => String(c.id)}
              loading={cohorts.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.teaching.noCohorts')}
            />
          )}
        </Card>
        <Card title={t('lms.teaching.upcoming')}>
          {classes.isError ? (
            <LmsRetryBanner onRetry={() => void classes.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.teaching.upcoming')}
              columns={classColumns}
              rows={upcoming}
              rowKey={(c) => String(c.id)}
              loading={classes.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.teaching.noClasses')}
            />
          )}
        </Card>
      </div>
    </div>
  );
}
