import {
  lmsFailureText,
  isApiError,
  useCan,
  useGradebook,
  useGradebookTools,
  useGradeChangeLogs,
  useI18n,
  useReassignmentLogs,
  type Gradebook,
} from '@mysms/core';
import { Banner, Button, Card, Dialog, Skeleton } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-31 Gradebook (with the LMS-35 history drawer): the cohort's grid — students by graded items — read one page
 * at a time, with export, per-item publication and final-grade publication where the role holds them, and the
 * read-only grade and reassignment history behind a drawer.
 */
export function GradebookScreen() {
  const { t } = useI18n();
  const { cohortId: param = '' } = useParams();
  const mayRead = useCan('lms.cohort.get_gradebook');
  const cohortId = mayRead && UUID.test(param) ? param : null;
  const gradebook = useGradebook(cohortId);
  const title = t('screen.LMS31.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (cohortId === null || (isApiError(gradebook.error) && gradebook.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (gradebook.isPending) return <Skeleton label={t('common.state.loading')} lines={8} />;
  if (gradebook.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void gradebook.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return <GradebookGrid cohortId={cohortId} gradebook={gradebook.data} />;
}

function GradebookGrid({ cohortId, gradebook }: { cohortId: string; gradebook: Gradebook }) {
  const { t } = useI18n();
  const tools = useGradebookTools();
  const mayExport = useCan('lms.cohort.export_grades');
  const mayPublishItem = useCan('lms.content_item.publish_item_grades');
  const mayPublishFinal = useCan('lms.cohort.publish_final_grades');
  const mayHistory = useCan('lms.grade_change_log.read');
  const [history, setHistory] = useState(false);
  const columns = gradebook.columns ?? [];
  const rows = gradebook.rows ?? [];
  const failure = tools.exportGrades.error ?? tools.publishItem.error ?? tools.publishFinal.error;
  return (
    <Card title={t('screen.LMS31.title')}>
      <div role="toolbar" aria-label={t('lms.gradebook.tools')}>
        {mayExport && (
          <Button
            variant="secondary"
            onClick={() => tools.exportGrades.mutate({ cohortId })}
            disabled={tools.exportGrades.isPending}
          >
            {t('lms.gradebook.export')}
          </Button>
        )}
        {mayPublishFinal && (
          <Button
            variant="secondary"
            onClick={() => tools.publishFinal.mutate({ cohortId })}
            disabled={tools.publishFinal.isPending}
          >
            {t('lms.gradebook.publishFinal')}
          </Button>
        )}
        {mayHistory && (
          <Button variant="secondary" onClick={() => setHistory(true)}>
            {t('lms.gradebook.history')}
          </Button>
        )}
      </div>
      {failure !== undefined && failure !== null && (
        <Banner intent="danger">{lmsFailureText(t, failure)}</Banner>
      )}
      {rows.length === 0 ? (
        <p className="hint">{t('common.state.empty')}</p>
      ) : (
        <div className="sms-grid-scroll" tabIndex={0} role="region" aria-label={t('screen.LMS31.title')}>
          <table>
            <caption>{t('screen.LMS31.title')}</caption>
            <thead>
              <tr>
                <th scope="col">{t('lms.gradebook.student')}</th>
                {columns.map((column) => (
                  <th scope="col" key={column.contentItemId}>
                    {column.title}
                    {column.points !== undefined && column.points !== null && (
                      <span className="hint"> /{column.points}</span>
                    )}
                    {mayPublishItem && (
                      <Button
                        variant="secondary"
                        onClick={() =>
                          column.contentItemId &&
                          tools.publishItem.mutate({ contentItemId: column.contentItemId })
                        }
                        disabled={tools.publishItem.isPending}
                      >
                        {t('lms.gradebook.publishItem')}
                      </Button>
                    )}
                  </th>
                ))}
                <th scope="col">{t('lms.gradebook.total')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.userId}>
                  <th scope="row">{row.studentName ?? row.userId}</th>
                  {columns.map((column) => {
                    const cell = row.cells?.find((c) => c.contentItemId === column.contentItemId);
                    return (
                      <td key={column.contentItemId}>
                        {cell?.points ?? '—'}
                        {cell?.isPublished === false && (
                          <span className="hint"> {t('lms.gradebook.unpublished')}</span>
                        )}
                      </td>
                    );
                  })}
                  <td>
                    {row.totalPercent !== undefined && row.totalPercent !== null && `${row.totalPercent}%`}{' '}
                    {row.letterGrade ?? ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <HistoryDrawer open={history} onClose={() => setHistory(false)} />
    </Card>
  );
}

/** LMS-35 Grade & reassignment history: read-only logs of the cohort's grading. */
function HistoryDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useI18n();
  const mayReassignments = useCan('lms.reassignment_log.read');
  const grades = useGradeChangeLogs(open ? '' : null);
  const reassignments = useReassignmentLogs(open && mayReassignments ? '' : null);
  return (
    <Dialog open={open} title={t('lms.gradebook.history')} onClose={onClose}>
      {grades.isPending && open ? (
        <Skeleton label={t('common.state.loading')} lines={4} />
      ) : grades.isError ? (
        <Banner intent="danger">{t('common.error.generic')}</Banner>
      ) : (
        <ul>
          {(grades.data?.items ?? []).map((log) => (
            <li key={log.id}>
              {t('lms.gradebook.historyRow', { points: log.points ?? '', changedAt: log.changedAt ?? '' })}
              {log.comment !== undefined && log.comment !== null && (
                <span className="hint"> {log.comment}</span>
              )}
            </li>
          ))}
          {(grades.data?.items.length ?? 0) === 0 && <li className="hint">{t('common.state.empty')}</li>}
        </ul>
      )}
      {mayReassignments && reassignments.data && reassignments.data.items.length > 0 && (
        <>
          <h3>{t('lms.gradebook.reassignments')}</h3>
          <ul>
            {reassignments.data.items.map((log) => (
              <li key={log.id}>{log.comment ?? log.id}</li>
            ))}
          </ul>
        </>
      )}
    </Dialog>
  );
}
