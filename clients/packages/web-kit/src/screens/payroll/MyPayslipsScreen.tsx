import { useCan, useI18n, usePayslips, useSession, type Payslip } from '@mysms/core';
import { Banner, Card, DataTable, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { useMoney } from './format';
import { PayslipDetail, PayslipStatus, RetryBanner } from './shared';

const NO_STAFF: readonly string[] = [];

/**
 * HR-08 My payslips: the signed-in staff member's own payslips (iam.GetMe names their staff records) with their
 * lines and totals, newest period first. The payslip PDF is not offered: no served operation renders it yet, so the
 * payslip is shown on screen. Cancelled payslips are listed too, so a corrected month is explained.
 */
export function MyPayslipsScreen() {
  const { t, format } = useI18n();
  const mayRead = useCan('payroll.payslip.read');
  const staffIds = useSession((s) => s.me?.staffIds ?? NO_STAFF);
  const payslips = usePayslips(mayRead ? { staffIds } : null);
  const [openId, setOpenId] = useState<string | null>(null);
  const rows = payslips.data?.items ?? [];
  const opened = rows.find((payslip) => payslip.id === openId) ?? rows[0];
  const money = useMoney(opened?.institutionId);

  const title = t('screen.HR08.title');
  const columns: Column<Payslip>[] = [
    {
      key: 'period',
      header: t('pay.payslip.period'),
      render: (p) =>
        t('pay.payslip.periodRange', { from: format.day(p.periodStart), to: format.day(p.periodEnd) }),
    },
    { key: 'number', header: t('pay.payslip.number'), render: (p) => p.payslipNumber },
    { key: 'status', header: t('pay.payslip.status'), render: (p) => <PayslipStatus payslip={p} /> },
    { key: 'net', header: t('pay.payslip.net'), align: 'end', render: (p) => money(p.netAmount) },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('pay.mine.intro')}</p>
      </header>
      {!mayRead ? (
        <Banner intent="info">{t('pay.mine.noAccess')}</Banner>
      ) : staffIds.length === 0 ? (
        <Banner intent="info">{t('pay.mine.notStaff')}</Banner>
      ) : payslips.isError ? (
        <RetryBanner onRetry={() => void payslips.refetch()} />
      ) : (
        <div className="page__grid">
          <Card title={t('pay.mine.list')}>
            <DataTable
              caption={t('pay.mine.list')}
              columns={columns}
              rows={payslips.data?.items}
              rowKey={(payslip) => payslip.id ?? payslip.payslipNumber}
              loading={payslips.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('pay.mine.none')}
              onRowClick={(payslip) => setOpenId(payslip.id ?? null)}
            />
          </Card>
          {opened ? (
            <Card title={t('pay.mine.detail', { number: opened.payslipNumber })}>
              <PayslipStatus payslip={opened} />
              <PayslipDetail payslip={opened} money={money} />
              <p className="hint">{t('pay.mine.noPdf')}</p>
            </Card>
          ) : null}
        </div>
      )}
    </div>
  );
}
