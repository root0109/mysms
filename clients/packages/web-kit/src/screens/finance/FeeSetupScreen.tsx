import {
  useCan,
  useFeePlans,
  useFeeStructures,
  useI18n,
  usePublishFeeStructure,
  type FeeStructure,
  type FinanceOperation,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, SegmentedControl, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { ResourceScreen } from '../ResourceScreen';
import type { ResourceScreenDef } from '../resourceScreens';
import { failureText } from './fees';
import { FeeStatus, RetryBanner } from './shared';

type Part = 'heads' | 'plans' | 'installments' | 'components' | 'structures' | 'concessions';

const PART_LABEL: Readonly<Record<Part, MessageKey>> = {
  heads: 'fin.setup.part.heads',
  plans: 'fin.setup.part.plans',
  installments: 'fin.setup.part.installments',
  components: 'fin.setup.part.components',
  structures: 'fin.setup.part.structures',
  concessions: 'fin.setup.part.concessions',
};

/** The record types fee setup manages, each on the shared list manager (create, edit, delete, archive). */
const PARTS: Readonly<Record<Part, ResourceScreenDef>> = {
  heads: { id: 'FIN-01', resource: 'finance.FeeHead', group: 'setup' },
  plans: { id: 'FIN-01', resource: 'finance.FeePlan', group: 'setup' },
  installments: { id: 'FIN-01', resource: 'finance.FeePlanInstallment', group: 'setup' },
  components: { id: 'FIN-01', resource: 'finance.FeePlanInstallmentComponent', group: 'setup' },
  structures: { id: 'FIN-01', resource: 'finance.FeeStructure', group: 'setup' },
  concessions: { id: 'FIN-01', resource: 'finance.FeeConcession', group: 'setup' },
};

/**
 * FIN-01 Fee setup: fee heads, fee plans with their installments and the heads each installment shares out, fee
 * structures (what a class or program costs in a year) and concessions — each on the shared list manager — and
 * publishing a draft structure, which charges its plan to every student it covers (finance.PublishFeeStructure,
 * answered with the fee run's counts).
 */
export function FeeSetupScreen() {
  const { t } = useI18n();
  const [part, setPart] = useState<Part>('heads');
  const mayPublish = useCan('finance.fee_structure.publish');
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.FIN01.title')}</h1>
        <p className="page__subtitle">{t('fin.setup.intro')}</p>
      </header>
      <SegmentedControl<Part>
        label={t('fin.setup.parts')}
        options={(Object.keys(PARTS) as Part[]).map((value) => ({ value, label: t(PART_LABEL[value]) }))}
        value={part}
        onChange={setPart}
      />
      <ResourceScreen key={part} screen={PARTS[part]} />
      {mayPublish ? <PublishStructures /> : null}
    </div>
  );
}

/** The draft structures, each with Publish: the dues are generated first, then the structure is published. */
function PublishStructures() {
  const { t } = useI18n();
  const structures = useFeeStructures();
  const plans = useFeePlans(useCan('finance.fee_plan.read'));
  const publish = usePublishFeeStructure();
  const [outcome, setOutcome] = useState<FinanceOperation | null>(null);
  const planName = new Map((plans.data?.items ?? []).map((plan) => [plan.id, plan.name]));
  const drafts = (structures.data?.items ?? []).filter((structure) => structure.status === 'draft');
  const result = (outcome?.result ?? {}) as { students?: number; dues?: number; kept?: number };
  const columns: Column<FeeStructure>[] = [
    { key: 'name', header: t('fin.setup.structure'), render: (row) => row.name },
    {
      key: 'plan',
      header: t('fin.setup.plan'),
      render: (row) => planName.get(row.feePlanId) ?? row.feePlanId,
    },
    { key: 'status', header: t('fin.common.status'), render: (row) => <FeeStatus status={row.status} /> },
    {
      key: 'publish',
      header: t('fin.common.actions'),
      render: (row) => (
        <Button
          variant="secondary"
          aria-label={t('fin.setup.publishOne', { name: row.name })}
          loading={publish.isPending && publish.variables === row.id}
          disabled={publish.isPending || !row.id}
          onClick={() => {
            setOutcome(null);
            publish.mutate(row.id ?? '', { onSuccess: setOutcome });
          }}
        >
          {t('fin.setup.publish')}
        </Button>
      ),
    },
  ];
  return (
    <Card title={t('fin.setup.publishTitle')}>
      <p className="hint">{t('fin.setup.publishHint')}</p>
      {publish.error ? <Banner intent="danger">{failureText(t, publish.error)}</Banner> : null}
      {outcome ? (
        <Banner intent="success">
          {t('fin.setup.published', {
            students: result.students ?? 0,
            dues: result.dues ?? 0,
            kept: result.kept ?? 0,
          })}
        </Banner>
      ) : null}
      {structures.isError ? (
        <RetryBanner onRetry={() => void structures.refetch()} />
      ) : (
        <DataTable
          caption={t('fin.setup.publishTitle')}
          columns={columns}
          rows={drafts}
          rowKey={(row) => row.id ?? row.name}
          loading={structures.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('fin.setup.noDrafts')}
          density="compact"
        />
      )}
    </Card>
  );
}
