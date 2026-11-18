import { useCan, useI18n, usePayslipLines, type Payslip, type PayslipLine } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, DataTable, StatusChip, type Column, type StatusChipProps } from '@mysms/ui-web';
import { componentTypeLabel } from './format';

/** A failed read with a retry. */
export function RetryBanner({ onRetry }: { onRetry: () => void }) {
  const { t } = useI18n();
  return (
    <Banner
      intent="danger"
      action={
        <Button variant="secondary" onClick={onRetry}>
          {t('common.action.retry')}
        </Button>
      }
    >
      {t('common.error.generic')}
    </Banner>
  );
}

const PAYSLIP_STATUS: Readonly<Record<string, { label: MessageKey; intent: StatusChipProps['intent'] }>> = {
  unpaid: { label: 'pay.payslip.status.unpaid', intent: 'warning' },
  paid: { label: 'pay.payslip.status.paid', intent: 'success' },
  cancelled: { label: 'pay.payslip.status.cancelled', intent: 'neutral' },
};

export function PayslipStatus({ payslip }: { payslip: Pick<Payslip, 'status'> }) {
  const { t } = useI18n();
  const state = PAYSLIP_STATUS[payslip.status ?? ''];
  return <StatusChip intent={state?.intent} label={state ? t(state.label) : (payslip.status ?? '')} />;
}

/**
 * One payslip as a document: its period and number, its lines in the order they were computed, and the totals the
 * server worked out (the screen never recomputes an official amount). Employer-defined lines are listed and count
 * in neither total.
 */
export function PayslipDetail({ payslip, money }: { payslip: Payslip; money: (amount?: string) => string }) {
  const { t, format } = useI18n();
  const mayLines = useCan('payroll.payslip_line.read');
  const lines = usePayslipLines(mayLines ? (payslip.id ?? null) : null);
  const columns: Column<PayslipLine>[] = [
    {
      key: 'name',
      header: t('pay.payslip.component'),
      render: (line) => line.componentName ?? line.componentCode,
    },
    {
      key: 'type',
      header: t('pay.payslip.type'),
      render: (line) => componentTypeLabel(t, line.componentType),
    },
    { key: 'amount', header: t('pay.payslip.amount'), align: 'end', render: (line) => money(line.amount) },
  ];
  return (
    <div>
      <dl className="facts">
        <dt>{t('pay.payslip.number')}</dt>
        <dd>{payslip.payslipNumber}</dd>
        <dt>{t('pay.payslip.period')}</dt>
        <dd>
          {t('pay.payslip.periodRange', {
            from: format.day(payslip.periodStart),
            to: format.day(payslip.periodEnd),
          })}
        </dd>
        <dt>{t('pay.payslip.gross')}</dt>
        <dd>{money(payslip.grossAmount)}</dd>
        <dt>{t('pay.payslip.deductions')}</dt>
        <dd>{money(payslip.deductionTotal)}</dd>
        <dt>{t('pay.payslip.net')}</dt>
        <dd>{money(payslip.netAmount)}</dd>
      </dl>
      {!mayLines ? (
        <p className="hint">{t('pay.payslip.noLineAccess')}</p>
      ) : lines.isError ? (
        <RetryBanner onRetry={() => void lines.refetch()} />
      ) : (
        <DataTable
          caption={t('pay.payslip.linesOf', { number: payslip.payslipNumber })}
          columns={columns}
          rows={lines.data?.items}
          rowKey={(line) => line.id ?? line.staffSalaryComponentId}
          loading={lines.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('pay.payslip.noLines')}
          density="compact"
        />
      )}
    </div>
  );
}
