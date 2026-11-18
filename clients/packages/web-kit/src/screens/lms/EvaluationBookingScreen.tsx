import {
  lmsFailureText,
  useCan,
  useCancelCertificateRequest,
  useCertificateEvaluations,
  useCertificateRequests,
  useI18n,
  useIssueCertificateFromEvaluation,
  useSession,
  useStartEvaluation,
  useUpdateCertificateEvaluation,
  type CertificateEvaluation,
  type CertificateRequest,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, StatusChip, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { LmsRetryBanner } from './lmsShared';

const DEFAULT_TEMPLATE = 'default';

/**
 * LMS-16 Evaluation booking: the student's bookings with their cancel step, and the evaluator's side — opening an
 * upcoming booking's evaluation, recording the verdict, and issuing the certificate a passed evaluation earns. New
 * bookings come from the course page once its evaluator slots are served (lms.GetEvaluatorSlots is still 501).
 */
export function EvaluationBookingScreen() {
  const { t, format } = useI18n();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const bookings = useCertificateRequests();
  const evaluations = useCertificateEvaluations(me ? `evaluatorUserId = "${me}"` : undefined);
  const cancel = useCancelCertificateRequest();
  const start = useStartEvaluation();
  const verdict = useUpdateCertificateEvaluation();
  const issue = useIssueCertificateFromEvaluation();
  const mayCancel = useCan('lms.certificate_request.cancel_certificate_request');
  const mayStart = useCan('lms.certificate_request.start_evaluation');
  const mayVerdict = useCan('lms.certificate_evaluation.update');
  const mayIssue = useCan('lms.certificate_evaluation.issue_certificate_from_evaluation');
  const [problem, setProblem] = useState<string | null>(null);
  const fail = (error: unknown) => setProblem(lmsFailureText(t, error));

  const bookingColumns: Column<CertificateRequest>[] = [
    {
      key: 'date',
      header: t('lms.booking.date'),
      render: (b) => (b.requestDate ? format.day(b.requestDate) : ''),
    },
    { key: 'slot', header: t('lms.booking.slot'), render: (b) => `${b.startTime ?? ''}–${b.endTime ?? ''}` },
    {
      key: 'status',
      header: t('lms.booking.status'),
      render: (b) => <StatusChip label={String(b.status ?? '')} />,
    },
    {
      key: 'actions',
      header: t('lms.roster.actions'),
      render: (b) => (
        <span className="chip-row">
          {mayCancel && b.status === 'upcoming' ? (
            <Button
              onClick={() =>
                cancel.mutate({ bookingId: String(b.id), etag: b.etag ?? null }, { onError: fail })
              }
              disabled={cancel.isPending}
            >
              {t('lms.booking.cancel')}
            </Button>
          ) : null}
          {mayStart && b.status === 'upcoming' ? (
            <Button
              onClick={() => start.mutate({ bookingId: String(b.id) }, { onError: fail })}
              disabled={start.isPending}
            >
              {t('lms.booking.startEvaluation')}
            </Button>
          ) : null}
        </span>
      ),
    },
  ];

  const evaluationColumns: Column<CertificateEvaluation>[] = [
    {
      key: 'date',
      header: t('lms.booking.date'),
      render: (e) => (e.evaluationDate ? format.day(e.evaluationDate) : ''),
    },
    {
      key: 'status',
      header: t('lms.booking.status'),
      render: (e) => <StatusChip label={String(e.status ?? '')} />,
    },
    {
      key: 'actions',
      header: t('lms.roster.actions'),
      render: (e) => (
        <span className="chip-row">
          {mayVerdict && e.status === 'in_progress' ? (
            <>
              <Button
                onClick={() =>
                  verdict.mutate(
                    { evaluationId: String(e.id), etag: e.etag ?? '', changes: { status: 'pass' } },
                    { onError: fail },
                  )
                }
                disabled={verdict.isPending}
              >
                {t('lms.booking.pass')}
              </Button>
              <Button
                onClick={() =>
                  verdict.mutate(
                    { evaluationId: String(e.id), etag: e.etag ?? '', changes: { status: 'fail' } },
                    { onError: fail },
                  )
                }
                disabled={verdict.isPending}
              >
                {t('lms.booking.fail')}
              </Button>
            </>
          ) : null}
          {mayIssue && e.status === 'pass' ? (
            <Button
              onClick={() =>
                issue.mutate(
                  {
                    evaluationId: String(e.id),
                    issueDate: new Date().toISOString().slice(0, 10),
                    templateCode: DEFAULT_TEMPLATE,
                  },
                  { onError: fail },
                )
              }
              disabled={issue.isPending}
            >
              {t('lms.booking.issue')}
            </Button>
          ) : null}
        </span>
      ),
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS16.title')}</h1>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <div className="page__grid">
        <Card title={t('lms.booking.bookings')}>
          {bookings.isError ? (
            <LmsRetryBanner onRetry={() => void bookings.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.booking.bookings')}
              columns={bookingColumns}
              rows={bookings.data?.items}
              rowKey={(b) => String(b.id)}
              loading={bookings.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.booking.none')}
            />
          )}
        </Card>
        <Card title={t('lms.booking.evaluations')}>
          {evaluations.isError ? (
            <LmsRetryBanner onRetry={() => void evaluations.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.booking.evaluations')}
              columns={evaluationColumns}
              rows={evaluations.data?.items}
              rowKey={(e) => String(e.id)}
              loading={evaluations.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.booking.noEvaluations')}
            />
          )}
        </Card>
      </div>
    </div>
  );
}
