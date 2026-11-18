import {
  PAYMENT_METHODS,
  isDone,
  localDay,
  monthPeriod,
  useCan,
  useDisbursementAccounts,
  useGeneratePayslips,
  useI18n,
  useInstitutions,
  useOperation,
  usePayslipStep,
  usePayslips,
  useStaffNames,
  type PaymentMethod,
  type Payslip,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  ProgressBar,
  SelectField,
  Skeleton,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useEffect, useState } from 'react';
import { METHOD, failureText, useMoney } from './format';
import { PayslipDetail, PayslipStatus, RetryBanner } from './shared';

/** The longest reference a payment keeps (api/state-machines.md "payroll.Payslip"). */
const MAX_REFERENCE = 80;

const REASON: Readonly<Record<string, MessageKey>> = {
  NO_SALARY: 'pay.run.reason.noSalary',
  NO_COMPONENTS: 'pay.run.reason.noComponents',
  NEGATIVE_NET: 'pay.run.reason.negativeNet',
  OVERLAPPING_PERIOD: 'pay.run.reason.overlapping',
};

/** The outcome payroll.GeneratePayslips keeps on its operation (`result`). */
interface RunOutcome {
  generated: number;
  kept: number;
  skippedCount: number;
  skipped: { staffId: string; reason: string }[];
}

const outcomeOf = (result: unknown): RunOutcome | null => {
  if (!result || typeof result !== 'object') return null;
  const value = result as Partial<RunOutcome>;
  return {
    generated: Number(value.generated ?? 0),
    kept: Number(value.kept ?? 0),
    skippedCount: Number(value.skippedCount ?? 0),
    skipped: Array.isArray(value.skipped) ? value.skipped : [],
  };
};

/** The month before the one a day lies in (`YYYY-MM`): the month a school usually pays at the start of the next. */
const previousMonth = (day: string): string => {
  const [year = 0, month = 1] = day.split('-').map(Number);
  return month === 1 ? `${year - 1}-12` : `${year}-${String(month - 1).padStart(2, '0')}`;
};

/**
 * HR-07 Payslip run (ui/03 flow 7.2): pick an institution and a month → payroll.GeneratePayslips (202 + operation,
 * polled at the payroll module) → the run's outcome and its payslips with their lines and totals → mark each paid
 * (recording the payment of its net) or cancel it, as the role allows. Amounts are the server's; the screen never
 * recomputes one.
 */
export function PayslipRunScreen() {
  const { t, timeZone } = useI18n();
  const mayRead = useCan('payroll.payslip.read');
  const mayGenerate = useCan('payroll.payslip.generate_payslips');
  const mayPay = useCan('payroll.payslip.mark_payslip_paid');
  const mayCancel = useCan('payroll.payslip.cancel_payslip');
  const mayAccounts = useCan('payroll.disbursement_account.read');
  const mayInstitutions = useCan('org.institution.read');
  const mayStaff = useCan('hr.staff.read');
  const mayPersons = useCan('people.person.read');
  const mayNames = mayStaff && mayPersons;

  const today = localDay(new Date(), timeZone);
  const institutions = useInstitutions(mayInstitutions && (mayRead || mayGenerate));
  const [chosenInstitution, setInstitution] = useState('');
  const institutionId = chosenInstitution || institutions.data?.items[0]?.id || '';
  const [month, setMonth] = useState(previousMonth(today));
  const period = monthPeriod(month);
  const generate = useGeneratePayslips();
  const operation = useOperation(generate.data ? generate.data : null);
  const running = generate.isPending || (generate.isSuccess && !(operation.data && isDone(operation.data)));
  const payslips = usePayslips(mayRead && institutionId && period ? { institutionId, ...period } : null);
  const accounts = useDisbursementAccounts(mayPay && mayAccounts);
  const names = useStaffNames(
    (payslips.data?.items ?? []).map((p) => p.staffId),
    mayNames,
  );
  const money = useMoney(institutionId);
  const step = usePayslipStep();

  const [openId, setOpenId] = useState<string | null>(null);
  const [accountId, setAccountId] = useState('');
  const [method, setMethod] = useState<PaymentMethod>('bank_transfer');
  const [paidOn, setPaidOn] = useState(today);
  const [reference, setReference] = useState('');
  const [reason, setReason] = useState('');
  const [done, setDone] = useState<MessageKey | null>(null);

  const finished = operation.data && isDone(operation.data) ? operation.data : null;
  const refetchPayslips = payslips.refetch;
  // the run wrote payslips while it ran: read the list again once it has finished
  useEffect(() => {
    if (finished) void refetchPayslips();
  }, [finished, refetchPayslips]);

  const title = t('screen.HR07.title');
  if (!mayRead && !mayGenerate)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('pay.run.noAccess')}</Banner>
      </div>
    );
  if (!mayInstitutions)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('pay.common.noInstitutionLookup')}</Banner>
      </div>
    );
  if (institutions.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;

  const outcome = finished?.status === 'succeeded' ? outcomeOf(finished.result) : null;
  const opened = (payslips.data?.items ?? []).find((payslip) => payslip.id === openId);
  const nameOf = (staffId: string) => names.get(staffId) ?? t('pay.common.someone');
  const usableAccounts = (accounts.data?.items ?? []).filter(
    (account) => account.institutionId === institutionId,
  );
  const chosenAccount = accountId || usableAccounts[0]?.id || '';
  const act = (payslip: Payslip, taken: 'markPaid' | 'cancel') => {
    setDone(null);
    const base = { payslipId: payslip.id ?? '', ...(payslip.etag ? { etag: payslip.etag } : {}) };
    step.mutate(
      taken === 'markPaid'
        ? {
            step: 'markPaid',
            ...base,
            payment: {
              disbursementAccountId: chosenAccount,
              paymentMethod: method,
              transactionDate: paidOn,
              referenceNumber: reference,
            },
          }
        : { step: 'cancel', ...base, reason },
      {
        onSuccess: () => {
          setDone(taken === 'markPaid' ? 'pay.run.done.paid' : 'pay.run.done.cancelled');
          setReference('');
          setReason('');
        },
      },
    );
  };

  const columns: Column<Payslip>[] = [
    { key: 'staff', header: t('pay.common.staff'), render: (payslip) => nameOf(payslip.staffId) },
    { key: 'number', header: t('pay.payslip.number'), render: (payslip) => payslip.payslipNumber },
    {
      key: 'status',
      header: t('pay.payslip.status'),
      render: (payslip) => <PayslipStatus payslip={payslip} />,
    },
    { key: 'gross', header: t('pay.payslip.gross'), align: 'end', render: (p) => money(p.grossAmount) },
    {
      key: 'deductions',
      header: t('pay.payslip.deductions'),
      align: 'end',
      render: (p) => money(p.deductionTotal),
    },
    { key: 'net', header: t('pay.payslip.net'), align: 'end', render: (p) => money(p.netAmount) },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('pay.run.intro')}</p>
      </header>
      {institutions.isError ? <RetryBanner onRetry={() => void institutions.refetch()} /> : null}
      <Card title={t('pay.run.period.title')}>
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault();
            if (!period || !institutionId || running) return;
            setDone(null);
            generate.mutate({ institutionId, ...period });
          }}
        >
          <SelectField
            label={t('pay.common.institution')}
            options={(institutions.data?.items ?? []).map((item) => ({
              value: item.id ?? '',
              label: item.name,
            }))}
            value={institutionId}
            onChange={(event) => {
              setInstitution(event.target.value);
              setOpenId(null);
            }}
          />
          <TextInput
            label={t('pay.run.period.month')}
            type="month"
            value={month}
            error={period ? undefined : t('pay.run.period.badMonth')}
            onChange={(event) => {
              setMonth(event.target.value);
              setOpenId(null);
            }}
          />
          {period ? (
            <p className="hint">
              {t('pay.run.period.explain', { from: period.periodStart, to: period.periodEnd })}
            </p>
          ) : null}
          {mayGenerate ? (
            <div className="page__actions">
              <Button type="submit" loading={running} disabled={!period || !institutionId || running}>
                {t('pay.run.generate')}
              </Button>
            </div>
          ) : null}
        </form>
        {generate.error ? <Banner intent="danger">{failureText(t, generate.error)}</Banner> : null}
        {running ? (
          <ProgressBar label={t('pay.run.progress')} value={operation.data?.progressPercent ?? undefined} />
        ) : null}
        {operation.isError ? <RetryBanner onRetry={() => void operation.refetch()} /> : null}
        {finished && finished.status !== 'succeeded' ? (
          <Banner intent="danger">{finished.errorMessage ?? t('pay.run.failed')}</Banner>
        ) : null}
        {outcome ? (
          <Banner intent={outcome.skippedCount > 0 ? 'warning' : 'success'} title={t('pay.run.finished')}>
            {t('pay.run.outcome', {
              generated: outcome.generated,
              kept: outcome.kept,
              skipped: outcome.skippedCount,
            })}
            {outcome.skipped.length > 0 ? (
              <ul>
                {outcome.skipped.map((skip) => (
                  <li key={skip.staffId}>
                    {nameOf(skip.staffId)}:{' '}
                    {REASON[skip.reason] ? t(REASON[skip.reason] as MessageKey) : skip.reason}
                  </li>
                ))}
              </ul>
            ) : null}
          </Banner>
        ) : null}
      </Card>

      {mayRead ? (
        <Card title={t('pay.run.list.title')}>
          {done && !step.error ? <Banner intent="success">{t(done)}</Banner> : null}
          {payslips.isError ? (
            <RetryBanner onRetry={() => void payslips.refetch()} />
          ) : (
            <DataTable
              caption={t('pay.run.list.title')}
              columns={columns}
              rows={payslips.data?.items}
              rowKey={(payslip) => payslip.id ?? payslip.payslipNumber}
              loading={payslips.isPending && Boolean(period && institutionId)}
              loadingLabel={t('common.state.loading')}
              emptyText={t(mayGenerate ? 'pay.run.list.noneGenerate' : 'pay.run.list.none')}
              onRowClick={(payslip) => {
                step.reset();
                setOpenId(payslip.id ?? null);
              }}
            />
          )}
        </Card>
      ) : null}

      {opened ? (
        <Card title={t('pay.run.detail.title', { name: nameOf(opened.staffId) })}>
          <PayslipStatus payslip={opened} />
          <PayslipDetail payslip={opened} money={money} />
          {step.error ? <Banner intent="danger">{failureText(t, step.error)}</Banner> : null}
          {opened.status === 'unpaid' && mayPay ? (
            <fieldset className="record-actions">
              <legend>{t('pay.run.pay.title')}</legend>
              {!mayAccounts ? (
                <p className="hint">{t('pay.run.pay.noAccountLookup')}</p>
              ) : usableAccounts.length === 0 && !accounts.isPending ? (
                <p className="hint">{t('pay.run.pay.noAccounts')}</p>
              ) : (
                <>
                  <SelectField
                    label={t('pay.run.pay.account')}
                    options={usableAccounts.map((account) => ({
                      value: account.id ?? '',
                      label: `${account.name} ${account.accountNumberMasked ?? ''}`.trim(),
                    }))}
                    value={chosenAccount}
                    onChange={(event) => setAccountId(event.target.value)}
                  />
                  <SelectField
                    label={t('pay.run.pay.method')}
                    options={PAYMENT_METHODS.map((value) => ({ value, label: t(METHOD[value]) }))}
                    value={method}
                    onChange={(event) => setMethod(event.target.value as PaymentMethod)}
                  />
                  <TextInput
                    label={t('pay.run.pay.date')}
                    type="date"
                    value={paidOn}
                    onChange={(event) => setPaidOn(event.target.value)}
                  />
                  <TextInput
                    label={t('pay.run.pay.reference')}
                    maxLength={MAX_REFERENCE}
                    value={reference}
                    onChange={(event) => setReference(event.target.value)}
                  />
                  <div className="record-actions__buttons">
                    <Button
                      loading={step.isPending && step.variables?.step === 'markPaid'}
                      disabled={step.isPending || !chosenAccount || !paidOn}
                      onClick={() => act(opened, 'markPaid')}
                    >
                      {t('pay.run.pay.submit', { amount: money(opened.netAmount) })}
                    </Button>
                  </div>
                </>
              )}
            </fieldset>
          ) : null}
          {opened.status === 'unpaid' && mayCancel ? (
            <fieldset className="record-actions">
              <legend>{t('pay.run.cancel.title')}</legend>
              <TextInput
                label={t('pay.run.cancel.reason')}
                value={reason}
                onChange={(event) => setReason(event.target.value)}
              />
              <div className="record-actions__buttons">
                <Button
                  variant="danger"
                  loading={step.isPending && step.variables?.step === 'cancel'}
                  disabled={step.isPending}
                  onClick={() => act(opened, 'cancel')}
                >
                  {t('pay.run.cancel.submit')}
                </Button>
              </div>
            </fieldset>
          ) : null}
          {opened.status !== 'unpaid' ? <p className="hint">{t('pay.run.detail.final')}</p> : null}
        </Card>
      ) : null}
    </div>
  );
}
