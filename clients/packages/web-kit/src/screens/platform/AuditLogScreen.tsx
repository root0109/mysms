import { nextPage, useAuditLogs, useCan, useI18n, type AuditLog, type PageParam } from '@mysms/core';
import { Banner, Button, Card, DataTable, TextInput, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { problemText } from './problems';

const MAX_DAYS = 366;
const DAY_MS = 86_400_000;

const today = (): string => new Date().toISOString().slice(0, 10);
const daysBefore = (day: string, days: number): string =>
  new Date(Date.parse(`${day}T00:00:00Z`) - days * DAY_MS).toISOString().slice(0, 10);
const span = (from: string, to: string): number => (Date.parse(to) - Date.parse(from)) / DAY_MS;

/**
 * ADM-13 Audit & access logs: the tenant's audit trail (platform.ListAuditLogs) for a date range of at most 366 days
 * — the trail is stored by month, so the range is mandatory — optionally narrowed to one record type or record,
 * newest first, page by page. A record shows who did what to which record, the changed field names and the reason,
 * never values. Read by tenant administrators only (a school-level grant is refused by the server). LMS access logs
 * (lms.ListAccessLogs) are not served yet.
 */
export function AuditLogScreen() {
  const { t, format } = useI18n();
  const mayRead = useCan('platform.audit_log.read');
  const [from, setFrom] = useState(() => daysBefore(today(), 7));
  const [to, setTo] = useState(today);
  const [subjectType, setSubjectType] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [pages, setPages] = useState<PageParam[]>([{ page: 1 }]);
  const valid = from <= to && span(from, to) <= MAX_DAYS;
  const page = pages[pages.length - 1] as PageParam;
  const logs = useAuditLogs(mayRead && valid ? { from, to, subjectType, subjectId, page } : null);
  const restart = () => setPages([{ page: 1 }]);
  const next = nextPage(logs.data?.paging);

  const columns: Column<AuditLog>[] = [
    {
      key: 'when',
      header: t('plt.audit.when'),
      render: (log) => (log.occurredAt ? format.dateTime(log.occurredAt) : ''),
    },
    { key: 'event', header: t('plt.audit.event'), render: (log) => log.event ?? '' },
    {
      key: 'subject',
      header: t('plt.audit.subject'),
      render: (log) => `${log.subjectType ?? ''} ${log.subjectId ?? ''}`,
    },
    { key: 'causer', header: t('plt.audit.causer'), render: (log) => log.causerId ?? t('plt.audit.system') },
    {
      key: 'fields',
      header: t('plt.audit.fields'),
      render: (log) => {
        const properties = (log.properties ?? {}) as { changedFields?: string[]; reason?: string };
        const fields = (properties.changedFields ?? []).join(', ');
        return properties.reason ? `${fields} — ${properties.reason}` : fields;
      },
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ADM13.title')}</h1>
        <p className="page__subtitle">{t('plt.audit.intro')}</p>
      </header>
      {!mayRead ? (
        <Banner intent="info">{t('plt.audit.noAccess')}</Banner>
      ) : (
        <>
          <Card title={t('plt.audit.filters')}>
            <div className="form-row">
              <TextInput
                type="date"
                label={t('plt.audit.from')}
                value={from}
                onChange={(event) => {
                  setFrom(event.target.value);
                  restart();
                }}
              />
              <TextInput
                type="date"
                label={t('plt.audit.to')}
                value={to}
                error={valid ? undefined : t('plt.audit.rangeInvalid', { days: MAX_DAYS })}
                onChange={(event) => {
                  setTo(event.target.value);
                  restart();
                }}
              />
              <TextInput
                label={t('plt.audit.subjectType')}
                hint={t('plt.audit.subjectTypeHint')}
                value={subjectType}
                onChange={(event) => {
                  setSubjectType(event.target.value);
                  restart();
                }}
              />
              <TextInput
                label={t('plt.audit.subjectId')}
                value={subjectId}
                onChange={(event) => {
                  setSubjectId(event.target.value);
                  restart();
                }}
              />
            </div>
          </Card>
          {logs.isError ? (
            <Banner intent="danger">{problemText(t, logs.error)}</Banner>
          ) : (
            <Card title={t('plt.audit.list')}>
              <DataTable
                caption={t('plt.audit.list')}
                columns={columns}
                rows={logs.data?.data}
                rowKey={(log) => log.id ?? `${log.occurredAt}-${log.subjectId}`}
                loading={valid && logs.isPending}
                loadingLabel={t('common.state.loading')}
                emptyText={t('plt.audit.none')}
                density="compact"
              />
              <div className="form-actions">
                <Button
                  variant="secondary"
                  disabled={pages.length < 2}
                  onClick={() => setPages((list) => list.slice(0, -1))}
                >
                  {t('plt.audit.previous')}
                </Button>
                <Button
                  variant="secondary"
                  disabled={!next}
                  onClick={() => next && setPages((list) => [...list, next])}
                >
                  {t('plt.audit.next')}
                </Button>
              </div>
            </Card>
          )}
          <p className="hint">{t('plt.audit.accessLogsPending')}</p>
        </>
      )}
    </div>
  );
}
