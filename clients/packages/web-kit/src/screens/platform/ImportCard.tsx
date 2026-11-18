import {
  can,
  IMPORT_TYPES,
  importResult,
  useCan,
  useI18n,
  useImportTemplate,
  useInstitutions,
  useOperation,
  useSession,
  useStartImport,
  type FileObject,
  type ImportEntityType,
  type ImportRowError,
  type PlatformOperation,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, ProgressBar, SelectField, StatusChip } from '@mysms/ui-web';
import { useState } from 'react';
import { FileUploadButton } from '../../files/FileTransfer';
import { problemText } from './problems';

const TYPE_LABELS: Readonly<Record<ImportEntityType, MessageKey>> = {
  students: 'plt.imports.type.students',
  guardians: 'plt.imports.type.guardians',
  staff: 'plt.imports.type.staff',
  library_media: 'plt.imports.type.libraryMedia',
  holidays: 'plt.imports.type.holidays',
  promotions: 'plt.imports.type.promotions',
};

/** The empty institution choice of the types whose records may belong to every institution (holidays). */
const EVERY_INSTITUTION = '';

/** An import's operation: its state, the totals of its result and the rows that were refused. */
function ImportOutcome({ operation }: { operation: PlatformOperation }) {
  const { t } = useI18n();
  const result = importResult(operation);
  const done = operation.status !== 'running' && operation.status !== 'queued';
  return (
    <section aria-live="polite">
      <p>
        <StatusChip
          intent={
            operation.status === 'succeeded' ? 'success' : operation.status === 'failed' ? 'danger' : 'info'
          }
          label={t(`plt.imports.status.${operation.status}`)}
        />
      </p>
      {!done ? <ProgressBar label={t('plt.imports.progress')} value={operation.progressPercent} /> : null}
      {operation.status === 'failed' ? <Banner intent="danger">{t('plt.imports.failed')}</Banner> : null}
      {result ? (
        <>
          {result.dryRun ? <p className="hint">{t('plt.imports.checked')}</p> : null}
          <p>
            {t('plt.imports.summary', {
              rows: result.rowCount,
              created: result.createdCount,
              errors: result.errorCount,
            })}
          </p>
          {done && result.errorCount === 0 ? <p className="hint">{t('plt.imports.noErrors')}</p> : null}
          {result.errors.length > 0 ? (
            <>
              <DataTable<ImportRowError>
                caption={t('plt.imports.errors')}
                columns={[
                  {
                    key: 'row',
                    header: t('plt.imports.column.row'),
                    render: (e) => e.rowNumber,
                    align: 'end',
                  },
                  {
                    key: 'field',
                    header: t('plt.imports.column.field'),
                    render: (e) => e.field ?? t('plt.imports.wholeRow'),
                  },
                  { key: 'problem', header: t('plt.imports.column.problem'), render: (e) => e.message },
                ]}
                rows={result.errors}
                rowKey={(e) => `${e.rowNumber}:${e.field ?? ''}:${e.message}`}
                loadingLabel={t('common.state.loading')}
                emptyText={t('plt.imports.noErrors')}
              />
              {result.errorCount > result.errors.length ? (
                <p className="hint">
                  {t('plt.imports.errorsShown', { shown: result.errors.length, total: result.errorCount })}
                </p>
              ) : null}
            </>
          ) : null}
        </>
      ) : null}
    </section>
  );
}

/**
 * The import part of ADM-12 (platform.GetImportTemplate, platform.StartImport): choose what to import — only the
 * record types the role may create — and where, download that type's CSV template, upload the filled file, check it
 * (a dry run: every row against the template, nothing created) and import it; the operation is followed until the
 * module that owns the records has created them, with every refused row listed by row and column.
 */
export function ImportCard() {
  const { t } = useI18n();
  const mayStart = useCan('platform.tenant.start_import');
  const mayTemplate = useCan('platform.tenant.get_import_template');
  const mayListInstitutions = useCan('org.institution.read');
  const grants = useSession((s) => s.me?.permissions);
  const types = IMPORT_TYPES.filter((type) => can(grants, type.permission));
  const [chosenType, setChosenType] = useState<ImportEntityType | ''>('');
  const type = types.find((candidate) => candidate.entityType === chosenType) ?? types[0];
  const needsInstitution = type !== undefined && type.scope !== 'tenant';
  const institutions = useInstitutions(mayStart && needsInstitution && mayListInstitutions);
  const [chosenInstitution, setChosenInstitution] = useState<string | null>(null);
  const [file, setFile] = useState<FileObject | null>(null);
  const [started, setStarted] = useState<PlatformOperation | null>(null);
  const template = useImportTemplate();
  const start = useStartImport();
  const operation = useOperation(started);
  const current = operation.data ?? started;

  if (!mayStart) {
    return (
      <Card title={t('plt.imports.title')}>
        <p className="hint">{t('plt.imports.noAccess')}</p>
      </Card>
    );
  }
  if (type === undefined) {
    return (
      <Card title={t('plt.imports.title')}>
        <p className="hint">{t('plt.imports.noTypes')}</p>
      </Card>
    );
  }

  const institutionOptions = (institutions.data?.items ?? []).flatMap((institution) =>
    institution.id ? [{ value: institution.id, label: institution.name }] : [],
  );
  const options =
    type.scope === 'either'
      ? [{ value: EVERY_INSTITUTION, label: t('plt.imports.everyInstitution') }, ...institutionOptions]
      : institutionOptions;
  const institutionId =
    type.scope === 'tenant'
      ? undefined
      : (options.find((option) => option.value === chosenInstitution) ?? options[0])?.value;
  const institutionReady =
    type.scope !== 'institution' || (institutionId !== undefined && institutionId !== EVERY_INSTITUTION);
  const running = current?.status === 'running' || current?.status === 'queued';
  const ready = file !== null && institutionReady && !start.isPending && !running;

  const run = (dryRun: boolean) => {
    if (!file?.id) return;
    start.mutate(
      {
        entityType: type.entityType,
        fileId: file.id,
        ...(institutionId ? { institutionId } : {}),
        dryRun,
      },
      { onSuccess: setStarted },
    );
  };

  return (
    <Card title={t('plt.imports.title')}>
      <p className="hint">{t('plt.imports.intro')}</p>
      <SelectField
        label={t('plt.imports.type')}
        value={type.entityType}
        options={types.map((candidate) => ({
          value: candidate.entityType,
          label: t(TYPE_LABELS[candidate.entityType]),
        }))}
        onChange={(event) => {
          setChosenType(event.target.value as ImportEntityType);
          setStarted(null);
        }}
      />
      {type.scope === 'tenant' ? (
        <p className="hint">{t('plt.imports.tenantRecords')}</p>
      ) : institutions.isError ? (
        <Banner intent="danger">{t('common.error.generic')}</Banner>
      ) : institutions.isPending && mayListInstitutions ? (
        <p className="hint">{t('common.state.loading')}</p>
      ) : options.length === 0 ? (
        <p className="hint">{t('plt.imports.noInstitutions')}</p>
      ) : (
        <SelectField
          label={t('plt.imports.institution')}
          value={institutionId ?? ''}
          options={options}
          onChange={(event) => setChosenInstitution(event.target.value)}
        />
      )}
      {mayTemplate ? (
        <p>
          <Button
            variant="secondary"
            loading={template.isPending}
            onClick={() =>
              template.mutate(type.entityType, {
                onSuccess: (link) => window.open(link.url, '_blank', 'noopener'),
              })
            }
          >
            {t('plt.imports.template')}
          </Button>{' '}
          <span className="hint">{t('plt.imports.templateHint')}</span>
        </p>
      ) : null}
      {template.isError ? <Banner intent="danger">{problemText(t, template.error)}</Banner> : null}
      <FileUploadButton
        label={t('plt.imports.upload')}
        accept=".csv,text/csv"
        onUploaded={(uploaded) => {
          setFile(uploaded);
          setStarted(null);
        }}
      />
      {file ? <p>{t('plt.imports.file', { name: file.fileName })}</p> : null}
      <p>
        <Button variant="secondary" disabled={!ready} onClick={() => run(true)}>
          {t('plt.imports.check')}
        </Button>{' '}
        <Button loading={start.isPending} disabled={!ready} onClick={() => run(false)}>
          {t('plt.imports.start')}
        </Button>
      </p>
      {start.isError ? <Banner intent="danger">{problemText(t, start.error)}</Banner> : null}
      {current ? <ImportOutcome operation={current} /> : null}
    </Card>
  );
}
