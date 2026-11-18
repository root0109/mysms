import { isApiError, useCan, useGradebook, useI18n, useSession, type Gradebook } from '@mysms/core';
import { Banner, Button, Card, DataTable, Skeleton } from '@mysms/ui-web';
import { useParams } from 'react-router';
import { UUID } from '../ids';

type Row = { item: string; points: string; status: string };

/**
 * LMS-33 My grades: the student's own row of the cohort's gradebook — the server scopes GetGradebook to the
 * caller, so a student receives only their own marks and the published ones at that.
 */
export function MyGradesScreen() {
  const { t } = useI18n();
  const { cohortId: param = '' } = useParams();
  const mayRead = useCan('lms.cohort.get_gradebook');
  const cohortId = mayRead && UUID.test(param) ? param : null;
  const gradebook = useGradebook(cohortId);
  const title = t('screen.LMS33.title');
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
  if (gradebook.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
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
  return <MyRow gradebook={gradebook.data} />;
}

function MyRow({ gradebook }: { gradebook: Gradebook }) {
  const { t } = useI18n();
  const userId = useSession((s) => s.me?.user?.id);
  const row = (gradebook.rows ?? []).find((r) => r.userId === userId) ?? (gradebook.rows ?? [])[0];
  const columns = gradebook.columns ?? [];
  const rows: Row[] = columns.map((column) => {
    const cell = row?.cells?.find((c) => c.contentItemId === column.contentItemId);
    return {
      item: column.title ?? '',
      points:
        cell?.points !== undefined && cell?.points !== null
          ? `${cell.points}${column.points !== undefined && column.points !== null ? ` / ${column.points}` : ''}`
          : t('lms.myGrades.none'),
      status: cell?.status ?? '',
    };
  });
  return (
    <Card title={t('screen.LMS33.title')}>
      {row?.totalPercent !== undefined && row?.totalPercent !== null && (
        <p>
          <strong>{t('lms.myGrades.total', { percent: row.totalPercent })}</strong> {row.letterGrade ?? ''}{' '}
          {row.finalGrade ?? ''}
        </p>
      )}
      <DataTable
        caption={t('screen.LMS33.title')}
        columns={[
          { key: 'item', header: t('lms.myGrades.item'), render: (r: Row) => r.item },
          { key: 'points', header: t('lms.myGrades.points'), render: (r: Row) => r.points },
          { key: 'status', header: t('lms.myGrades.status'), render: (r: Row) => r.status },
        ]}
        rows={rows}
        rowKey={(r) => r.item}
        loadingLabel={t('common.state.loading')}
        emptyText={t('common.state.empty')}
      />
    </Card>
  );
}
