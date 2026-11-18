import {
  lmsFailureText,
  RESOURCES,
  useCan,
  useI18n,
  usePlagiarismReportLink,
  usePlagiarismReports,
  useInfiniteRecords,
  type PlagiarismReport,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Skeleton, StatusChip } from '@mysms/ui-web';

const ACCOUNT_DEF = RESOURCES.find((resource) => resource.key === 'lms.PlagiarismAccount');

/**
 * LMS-52 Plagiarism accounts & reports: the tenant's provider accounts (managed on their record forms), every
 * report with its similarity score, and the provider's viewer opened through a short-lived link. Secrets stay
 * masked; the provider decides what a viewer sees.
 */
export function PlagiarismScreen() {
  const { t } = useI18n();
  const mayAccounts = useCan('lms.plagiarism_account.read');
  const mayReports = useCan('lms.plagiarism_report.read');
  const mayLink = useCan('lms.plagiarism_report.get_plagiarism_report_link');
  const accounts = useInfiniteRecords(mayAccounts ? ACCOUNT_DEF : undefined, {});
  const reports = usePlagiarismReports(mayReports ? '' : null);
  const viewer = usePlagiarismReportLink();
  const title = t('screen.LMS52.title');
  if (!mayAccounts && !mayReports)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  return (
    <Card title={title}>
      {mayAccounts && (
        <section aria-label={t('lms.plagiarism.accounts')}>
          <h2>{t('lms.plagiarism.accounts')}</h2>
          {accounts.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={2} />
          ) : accounts.isError ? (
            <Banner intent="danger">{t('common.error.generic')}</Banner>
          ) : (
            <ul>
              {(accounts.data?.pages.flatMap((page) => page.data) ?? []).map((row) => (
                <li key={row.id}>
                  {String(row['accountRef'] ?? row.id)}{' '}
                  {row['isDefault'] === true && (
                    <StatusChip intent="info" label={t('lms.plagiarism.default')} />
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
      {mayReports && (
        <section aria-label={t('lms.plagiarism.reports')}>
          <h2>{t('lms.plagiarism.reports')}</h2>
          {viewer.isError && <Banner intent="danger">{lmsFailureText(t, viewer.error)}</Banner>}
          <DataTable
            caption={t('lms.plagiarism.reports')}
            columns={[
              {
                key: 'kind',
                header: t('lms.plagiarism.item'),
                render: (row: PlagiarismReport) => row.itemKind ?? '',
              },
              {
                key: 'status',
                header: t('lms.jobs.status'),
                render: (row: PlagiarismReport) => (
                  <StatusChip
                    intent={
                      row.status === 'complete' ? 'success' : row.status === 'error' ? 'danger' : 'neutral'
                    }
                    label={row.status ?? ''}
                  />
                ),
              },
              {
                key: 'similarity',
                header: t('lms.plagiarism.similarity'),
                render: (row: PlagiarismReport) =>
                  row.similarityScore !== undefined && row.similarityScore !== null
                    ? `${row.similarityScore}%`
                    : '',
              },
              {
                key: 'actions',
                header: t('lms.bank.actions'),
                render: (row: PlagiarismReport) =>
                  mayLink && row.status === 'complete' && row.id ? (
                    <Button
                      variant="secondary"
                      onClick={() =>
                        viewer.mutate(
                          { plagiarismReportId: row.id as string },
                          {
                            onSuccess: (link) => {
                              if (link.url) window.open(link.url, '_blank', 'noopener');
                            },
                          },
                        )
                      }
                      disabled={viewer.isPending}
                    >
                      {t('lms.plagiarism.open')}
                    </Button>
                  ) : null,
              },
            ]}
            rows={reports.data?.items}
            rowKey={(row) => row.id ?? ''}
            loading={reports.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('common.state.empty')}
          />
        </section>
      )}
    </Card>
  );
}
