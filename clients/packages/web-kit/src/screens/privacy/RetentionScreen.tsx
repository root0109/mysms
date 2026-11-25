import {
  belowFloor,
  governanceFailureKey,
  isApiError,
  retentionChanges,
  useCan,
  useI18n,
  useRetentionPolicies,
  useRetentionRuns,
  useUpdateRetentionPolicy,
  type RetentionAction,
  type RetentionPolicy,
  type RetentionRun,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  Skeleton,
  StatusChip,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useEffect, useState } from 'react';

const ACTIONS: readonly RetentionAction[] = ['anonymise', 'archive', 'delete'];

/**
 * ADM-22 Retention (ui/02; design PRIVACY-AND-CONSENT-CENTRE §10): how long the school keeps each kind of personal
 * data and what happens after — the platform's policies, each with the school's own setting: a period in days never
 * shorter than the policy's legal floor, and whether the data is anonymised, archived or deleted. A setting is saved
 * under the row's ETag; the server checks the floor again. Below, the latest retention runs with their cutoff,
 * status and how many records were affected or kept under a hold.
 */
export function RetentionScreen() {
  const { t } = useI18n();
  const policies = useRetentionPolicies();
  const mayReadRuns = useCan('privacy.retention_run.read');
  const runs = useRetentionRuns(mayReadRuns);
  if (policies.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (policies.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void policies.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const items = policies.data.items;
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.retention.title')}</h1>
        <p className="page__subtitle">{t('privacy.retention.intro')}</p>
      </header>
      {items.length === 0 ? (
        <Card>
          <p>{t('privacy.retention.empty')}</p>
        </Card>
      ) : (
        <div className="page__grid">
          {items.map((policy) => (
            <PolicyCard key={policy.id} policy={policy} />
          ))}
        </div>
      )}
      {mayReadRuns ? <Runs runs={runs} /> : null}
    </div>
  );
}

/** One policy: what it keeps and why, and the school's period and action as a small form under the row's ETag. */
function PolicyCard({ policy }: { policy: RetentionPolicy }) {
  const { t, format } = useI18n();
  const mayUpdate = useCan('privacy.retention_policy.update');
  const update = useUpdateRetentionPolicy();
  const [period, setPeriod] = useState(String(policy.periodDays ?? ''));
  const [action, setAction] = useState<RetentionAction>(policy.action ?? 'archive');
  const [saved, setSaved] = useState(false);
  const [tooShort, setTooShort] = useState(false);
  // the form follows the stored setting whenever it changes (after a save, or a reload after a conflict)
  useEffect(() => {
    setPeriod(String(policy.periodDays ?? ''));
    setAction(policy.action ?? 'archive');
  }, [policy.periodDays, policy.action, policy.etag]);
  const days = Number(period);
  const changes = retentionChanges(policy, days, action);
  const changed = Object.keys(changes).length > 0;
  const error = update.error;
  const conflict = isApiError(error) && error.status === 412;
  const periodError = isApiError(error)
    ? error.errors.find((e) => e.field === 'periodDays')?.message
    : undefined;
  const { id: retentionPolicyId, etag } = policy;
  const save = () => {
    if (!retentionPolicyId || !etag) return;
    if (belowFloor(policy, days)) return setTooShort(true);
    setTooShort(false);
    setSaved(false);
    update.mutate({ retentionPolicyId, etag, changes }, { onSuccess: () => setSaved(true) });
  };
  return (
    <Card
      title={policy.code ?? ''}
      actions={
        <StatusChip
          intent={policy.overridden ? 'info' : 'neutral'}
          label={t(policy.overridden ? 'privacy.retention.overridden' : 'privacy.retention.default')}
        />
      }
    >
      <div className="consent-card">
        <p className="hint">
          {[
            policy.categoryCode,
            policy.module,
            enumLabel(t, 'retentionTrigger', policy.trigger),
            t('privacy.retention.current', {
              days: format.number(policy.periodDays ?? 0),
              action: enumLabel(t, 'retentionAction', policy.action),
            }),
          ]
            .filter(Boolean)
            .join(' · ')}
        </p>
        {policy.legalReference ? <p>{policy.legalReference}</p> : null}
        {mayUpdate ? (
          <>
            <TextInput
              type="number"
              min={policy.minimumPeriodDays ?? 0}
              label={t('privacy.retention.period')}
              hint={t('privacy.retention.floor', { days: format.number(policy.minimumPeriodDays ?? 0) })}
              value={period}
              disabled={update.isPending}
              error={
                tooShort
                  ? t('privacy.retention.belowFloor', { days: format.number(policy.minimumPeriodDays ?? 0) })
                  : periodError
              }
              onChange={(event) => {
                setTooShort(false);
                setPeriod(event.currentTarget.value);
              }}
            />
            <SelectField
              label={t('privacy.retention.action')}
              value={action}
              options={ACTIONS.map((value) => ({ value, label: enumLabel(t, 'retentionAction', value) }))}
              disabled={update.isPending}
              onChange={(event) => setAction(event.currentTarget.value as RetentionAction)}
            />
            {saved && !changed ? <Banner intent="success">{t('privacy.retention.saved')}</Banner> : null}
            {error && !periodError ? <Banner intent="danger">{t(governanceFailureKey(error))}</Banner> : null}
            <div className="consent-card__actions">
              {conflict ? (
                <Button variant="ghost" onClick={() => update.reset()}>
                  {t('common.conflict.reload')}
                </Button>
              ) : null}
              <Button
                variant="primary"
                loading={update.isPending}
                disabled={!changed || !retentionPolicyId || !etag}
                onClick={save}
              >
                {t('common.action.save')}
              </Button>
            </div>
          </>
        ) : null}
      </div>
    </Card>
  );
}

/** The latest retention runs, newest first. */
function Runs({ runs }: { runs: ReturnType<typeof useRetentionRuns> }) {
  const { t, format } = useI18n();
  const columns: Column<RetentionRun>[] = [
    { key: 'policy', header: t('privacy.retention.runs.policy'), render: (r) => r.policyCode ?? '' },
    {
      key: 'started',
      header: t('privacy.retention.runs.started'),
      render: (r) => (r.startedAt ? format.dateTime(r.startedAt) : ''),
    },
    {
      key: 'cutoff',
      header: t('privacy.retention.runs.cutoff'),
      render: (r) => (r.cutoff ? format.date(r.cutoff) : ''),
    },
    {
      key: 'status',
      header: t('privacy.retention.runs.status'),
      render: (r) => enumLabel(t, 'retentionRunStatus', r.status),
    },
    {
      key: 'counts',
      header: t('privacy.retention.runs.counts'),
      render: (r) =>
        t('privacy.retention.runs.countsValue', {
          affected: format.number(r.affected ?? 0),
          held: format.number(r.held ?? 0),
        }),
      align: 'end',
    },
  ];
  return (
    <section>
      <h2>{t('privacy.retention.runs.title')}</h2>
      {runs.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void runs.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : (
        <DataTable
          caption={t('privacy.retention.runs.title')}
          columns={columns}
          rows={runs.data?.data}
          rowKey={(r) => r.id ?? ''}
          loading={runs.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('privacy.retention.runs.empty')}
        />
      )}
    </section>
  );
}
