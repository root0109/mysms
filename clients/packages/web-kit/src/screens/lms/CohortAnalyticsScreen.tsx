import {
  lmsFailureText,
  useCohortPerformance,
  useExportGrades,
  useI18n,
  useCan,
  useStudentAlerts,
  type CohortPerformance,
  type StudentAlert,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { LmsRetryBanner } from './lmsShared';
import { openDownload } from './lmsText';

/**
 * LMS-30 Student alerts & analytics: per-item performance (average, median, who is missing), the cohort's student
 * alerts, and the grade export the numbers can be taken away in (a CSV stored for the caller).
 */
export function CohortAnalyticsScreen() {
  const { t, format } = useI18n();
  const { cohortId = '' } = useParams();
  const performance = useCohortPerformance(cohortId || null);
  const alerts = useStudentAlerts(cohortId || null);
  const exportGrades = useExportGrades();
  const mayExport = useCan('lms.cohort.export_grades');
  const [problem, setProblem] = useState<string | null>(null);

  const performanceColumns: Column<CohortPerformance>[] = [
    { key: 'item', header: t('lms.analytics.item'), render: (p) => String(p.contentItemId) },
    { key: 'average', header: t('lms.analytics.average'), render: (p) => p.averagePercent ?? '—' },
    { key: 'median', header: t('lms.analytics.median'), render: (p) => p.medianPercent ?? '—' },
    { key: 'submitted', header: t('lms.analytics.submitted'), render: (p) => String(p.submitted ?? 0) },
    { key: 'graded', header: t('lms.analytics.graded'), render: (p) => String(p.graded ?? 0) },
    { key: 'missing', header: t('lms.analytics.missing'), render: (p) => String(p.missing ?? 0) },
  ];
  const alertColumns: Column<StudentAlert>[] = [
    { key: 'student', header: t('lms.cohort.student'), render: (a) => String(a.userId) },
    { key: 'metric', header: t('lms.analytics.metric'), render: (a) => a.metricCode ?? '' },
    { key: 'subject', header: t('lms.analytics.subject'), render: (a) => a.subject ?? '' },
    { key: 'sent', header: t('lms.analytics.sent'), render: (a) => (a.sentAt ? format.day(a.sentAt) : '') },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS30.title')}</h1>
        <p className="page__subtitle">
          <Link to={`/app/lms/cohorts/${cohortId}`}>{t('lms.cohort.back')}</Link>
        </p>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <div className="page__grid">
        <Card
          title={t('lms.analytics.performance')}
          actions={
            mayExport ? (
              <Button
                onClick={() => {
                  setProblem(null);
                  exportGrades.mutate(
                    { cohortId },
                    {
                      onSuccess: (operation) => {
                        const result = operation.result as { url?: string } | undefined;
                        openDownload(result);
                      },
                      onError: (error) => setProblem(lmsFailureText(t, error)),
                    },
                  );
                }}
                disabled={exportGrades.isPending}
              >
                {t('lms.analytics.export')}
              </Button>
            ) : null
          }
        >
          {performance.isError ? (
            <LmsRetryBanner onRetry={() => void performance.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.analytics.performance')}
              columns={performanceColumns}
              rows={performance.data?.items}
              rowKey={(p) => String(p.contentItemId)}
              loading={performance.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.analytics.noGrades')}
            />
          )}
        </Card>
        <Card title={t('lms.analytics.alerts')}>
          {alerts.isError ? (
            <LmsRetryBanner onRetry={() => void alerts.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.analytics.alerts')}
              columns={alertColumns}
              rows={alerts.data?.items}
              rowKey={(a) => String(a.id)}
              loading={alerts.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.analytics.noAlerts')}
            />
          )}
        </Card>
      </div>
    </div>
  );
}
