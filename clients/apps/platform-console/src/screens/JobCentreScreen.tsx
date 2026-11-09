import {
  can,
  useCancelOperation,
  useI18n,
  useJobCentre,
  useSession,
  type Operation,
  type PermissionCode,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import type { Intent } from '@mysms/tokens';
import { Banner, Button, Card, DataTable, ProgressBar, StatusChip, type Column } from '@mysms/ui-web';
import { useState } from 'react';

const STATUS_INTENT: Record<string, Intent> = {
  queued: 'neutral',
  running: 'info',
  succeeded: 'success',
  failed: 'danger',
  cancelled: 'neutral',
};

/** PLT-06 Operations & jobs: the cross-module job centre; cancellation goes to the module that owns the job. */
export function JobCentreScreen() {
  const { t, format } = useI18n();
  const jobs = useJobCentre({ pageSize: 50 });
  const cancel = useCancelOperation();
  const permissions = useSession((s) => s.me?.permissions);
  const [notice, setNotice] = useState<string | null>(null);
  const canCancel = (op: Operation) =>
    can(permissions, `${op.module}.operation.cancel_operation` as PermissionCode);
  const columns: Column<Operation>[] = [
    { key: 'kind', header: t('platform.jobs.kind'), render: (op) => op.kind },
    { key: 'module', header: t('platform.jobs.module'), render: (op) => op.module },
    {
      key: 'status',
      header: t('platform.jobs.status'),
      render: (op) => (
        <StatusChip intent={STATUS_INTENT[op.status]} label={enumLabel(t, 'operation', op.status)} />
      ),
    },
    {
      key: 'progress',
      header: t('platform.jobs.progress'),
      // a bar only while work is happening or when it finished successfully; failed/cancelled jobs show none
      render: (op) =>
        op.status === 'running' || op.status === 'queued' || op.status === 'succeeded' ? (
          <ProgressBar
            label={`${t('platform.jobs.progress')} ${op.kind}`}
            value={op.status === 'succeeded' ? 100 : op.progressPercent}
          />
        ) : null,
    },
    { key: 'started', header: t('platform.jobs.started'), render: (op) => format.dateTime(op.createTime) },
    {
      key: 'actions',
      header: t('platform.jobs.cancel'),
      render: (op) =>
        (op.status === 'running' || op.status === 'queued') && canCancel(op) ? (
          <Button
            variant="secondary"
            loading={cancel.isPending && cancel.variables?.operationId === op.operationId}
            onClick={() =>
              cancel.mutate(op, {
                onSuccess: () => setNotice(t('platform.jobs.cancelRequested', { kind: op.kind })),
              })
            }
          >
            {t('platform.jobs.cancel')}
          </Button>
        ) : null,
    },
  ];
  return (
    <Card title={t('platform.jobs.title')}>
      <Banner intent="info">{t('platform.jobs.lagNote')}</Banner>
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {jobs.isError || cancel.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      <DataTable
        caption={t('common.list.count', { count: jobs.data?.items.length ?? 0 })}
        columns={columns}
        rows={jobs.data?.items}
        rowKey={(op) => op.operationId}
        loading={jobs.isPending}
        loadingLabel={t('common.state.loading')}
        emptyText={t('common.state.empty')}
      />
    </Card>
  );
}
