import {
  useCan,
  useI18n,
  useOperation,
  useReportDefinitions,
  useRunReport,
  type PlatformOperation,
  type ReportDefinition,
} from '@mysms/core';
import { Banner, Button, Card, StatusChip, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { FileDownloadButton } from '../../files/FileTransfer';
import { ImportCard } from './ImportCard';
import { problemText } from './problems';
import { RetryBanner } from './shared';

/** The parameters a report's JSON Schema names (the platform's reports take dates only). */
const parameterNames = (report: ReportDefinition): string[] =>
  Object.keys(((report.parameters ?? {}) as { properties?: Record<string, unknown> }).properties ?? {});

/** One report: its date parameters, a run, the operation until it finishes, then its CSV file. */
function ReportRun({ report }: { report: ReportDefinition }) {
  const { t } = useI18n();
  const run = useRunReport();
  const [values, setValues] = useState<Record<string, string>>({});
  const [started, setStarted] = useState<PlatformOperation | null>(null);
  const operation = useOperation(started);
  const current = operation.data ?? started;
  const names = parameterNames(report);
  const complete = names.every((name) => Boolean(values[name]));
  return (
    <Card title={report.title}>
      {names.map((name) => (
        <TextInput
          key={name}
          type="date"
          label={name === 'from' ? t('plt.audit.from') : name === 'to' ? t('plt.audit.to') : name}
          value={values[name] ?? ''}
          onChange={(event) => setValues({ ...values, [name]: event.target.value })}
        />
      ))}
      <Button
        loading={run.isPending}
        disabled={!complete}
        onClick={() =>
          run.mutate({ reportCode: report.reportCode, parameters: values }, { onSuccess: setStarted })
        }
      >
        {t('plt.reports.run')}
      </Button>
      {run.isError ? <Banner intent="danger">{problemText(t, run.error)}</Banner> : null}
      {current ? (
        <p>
          <StatusChip
            intent={
              current.status === 'succeeded' ? 'success' : current.status === 'failed' ? 'danger' : 'info'
            }
            label={t(`plt.reports.status.${current.status}`)}
          />{' '}
          {current.status === 'succeeded' && current.resultFileId ? (
            <FileDownloadButton fileId={current.resultFileId} label={t('plt.reports.download')} />
          ) : null}
        </p>
      ) : null}
    </Card>
  );
}

/**
 * ADM-12 Imports & exports. Imports (platform.GetImportTemplate, platform.StartImport) bring CSV rows into the module
 * that owns them, followed as an operation with every refused row listed ({@link ImportCard}); iam.ImportUsers and
 * lms's import-export jobs are not offered here yet. Exports are the platform's reports
 * (platform.ListReportDefinitions, platform.RunReport): each runs as an operation whose result is a CSV file,
 * downloaded through a URL signed at the click.
 */
export function ImportsExportsScreen() {
  const { t } = useI18n();
  const mayList = useCan('platform.tenant.list_report_definitions');
  const mayRun = useCan('platform.tenant.run_report');
  const reports = useReportDefinitions(mayList);
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ADM12.title')}</h1>
        <p className="page__subtitle">{t('plt.reports.intro')}</p>
      </header>
      <ImportCard />
      {!mayList || !mayRun ? (
        <Banner intent="info">{t('plt.reports.noAccess')}</Banner>
      ) : reports.isError ? (
        <RetryBanner onRetry={() => void reports.refetch()} />
      ) : reports.isPending ? (
        <p className="hint">{t('common.state.loading')}</p>
      ) : reports.data.items.length === 0 ? (
        <p className="hint">{t('plt.reports.none')}</p>
      ) : (
        <div className="page__grid">
          {reports.data.items.map((report) => (
            <ReportRun key={report.reportCode} report={report} />
          ))}
        </div>
      )}
    </div>
  );
}
