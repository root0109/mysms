import {
  COUNTER_METHODS,
  REFUND_METHODS,
  centsText,
  feeFilter,
  sumAmounts,
  toCents,
  useCan,
  useFeeDues,
  useFeePayments,
  useFeeRefunds,
  useI18n,
  useInvoices,
  useRecordFeePayment,
  useRefundFeePayment,
  type CounterMethod,
  type FeePayment,
  type FeeRefund,
  type RefundMethod,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, SelectField, TextInput, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { METHOD_LABEL, failureText, isId, useFeeMoney } from './fees';
import { FeeStatus, RetryBanner } from './shared';

/** One target a counter payment may settle: a due not on an invoice, or an open invoice of the student. */
interface Target {
  key: string;
  kind: 'due' | 'invoice';
  id: string;
  label: string;
  outstanding: string;
  currencyId: string;
}

/**
 * FIN-06 Payments & receipts: every payment the fee office reads (counter and online, with their allocations and
 * receipt numbers), refunds — through the gateway for an online payment, or handed back at the counter — and the
 * counter payment itself: a student's open dues and invoices, an amount for each, and the server numbers the receipt
 * (finance.RecordPayment; the amounts must add up exactly, each at most what is outstanding).
 */
export function PaymentsScreen() {
  const { t, format } = useI18n();
  const mayRead = useCan('finance.payment.read');
  const mayRecord = useCan('finance.payment.record');
  const mayRefund = useCan('finance.payment.refund_payment');
  const mayRefunds = useCan('finance.refund.read');
  const [status, setStatus] = useState('');
  const payments = useFeePayments(feeFilter({ status }), mayRead);
  const refunds = useFeeRefunds(undefined, mayRefunds);
  const rows = payments.data?.items ?? [];
  const money = useFeeMoney(rows[0]?.currencyId);
  const refund = useRefundFeePayment();
  const [refunding, setRefunding] = useState<FeePayment | null>(null);
  const [refundAmount, setRefundAmount] = useState('');
  const [refundMethod, setRefundMethod] = useState<RefundMethod>('cash');
  const [refundReason, setRefundReason] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const title = t('screen.FIN06.title');
  if (!mayRead && !mayRecord)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('fin.common.noAccess')}</Banner>
      </div>
    );
  const paymentColumns: Column<FeePayment>[] = [
    {
      key: 'when',
      header: t('fin.payment.date'),
      render: (row) => (row.receivedAt ? format.dateTime(row.receivedAt) : ''),
    },
    {
      key: 'receipt',
      header: t('fin.payment.receipt'),
      render: (row) => row.receiptNumber ?? row.gatewayPaymentId ?? '',
    },
    { key: 'payer', header: t('fin.payment.payer'), render: (row) => row.payerName ?? row.billingName },
    {
      key: 'method',
      header: t('fin.payment.method'),
      render: (row) =>
        METHOD_LABEL[row.paymentMethod ?? ''] ? t(METHOD_LABEL[row.paymentMethod ?? '']!) : '',
    },
    { key: 'amount', header: t('fin.ledger.amount'), align: 'end', render: (row) => money(row.amount) },
    {
      key: 'slices',
      header: t('fin.payment.allocations'),
      render: (row) => (row.allocations ?? []).map((slice) => money(slice.amount)).join(' + '),
    },
    { key: 'status', header: t('fin.common.status'), render: (row) => <FeeStatus status={row.status} /> },
    {
      key: 'actions',
      header: t('fin.common.actions'),
      render: (row) =>
        mayRefund && row.status === 'received' && row.etag ? (
          <Button
            variant="ghost"
            aria-label={t('fin.payment.refundOne', { receipt: row.receiptNumber ?? row.id ?? '' })}
            onClick={() => {
              refund.reset();
              setNotice(null);
              setRefunding(row);
              setRefundAmount('');
              setRefundMethod(row.paymentMethod === 'gateway' ? 'gateway' : 'cash');
            }}
          >
            {t('fin.payment.refund')}
          </Button>
        ) : null,
    },
  ];
  const refundColumns: Column<FeeRefund>[] = [
    {
      key: 'when',
      header: t('fin.payment.date'),
      render: (row) => (row.refundedAt ? format.dateTime(row.refundedAt) : ''),
    },
    { key: 'amount', header: t('fin.ledger.amount'), align: 'end', render: (row) => money(row.amount) },
    {
      key: 'method',
      header: t('fin.payment.method'),
      render: (row) => (METHOD_LABEL[row.refundMethod ?? ''] ? t(METHOD_LABEL[row.refundMethod ?? '']!) : ''),
    },
    { key: 'reason', header: t('fin.payment.reason'), render: (row) => row.reason ?? '' },
    { key: 'status', header: t('fin.common.status'), render: (row) => <FeeStatus status={row.status} /> },
  ];
  const refundOk = !refundAmount.trim() || (toCents(refundAmount) ?? 0) > 0;
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('fin.payment.intro')}</p>
      </header>
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {mayRead ? (
        <Card title={t('fin.payment.list')}>
          <SelectField
            label={t('fin.common.status')}
            options={[
              { value: '', label: t('fin.common.all') },
              { value: 'initiated', label: t('fin.status.initiated') },
              { value: 'received', label: t('fin.status.received') },
              { value: 'failed', label: t('fin.status.failed') },
              { value: 'refunded', label: t('fin.status.refunded') },
            ]}
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          />
          {payments.isError ? (
            <RetryBanner onRetry={() => void payments.refetch()} />
          ) : (
            <DataTable
              caption={t('fin.payment.list')}
              columns={paymentColumns}
              rows={rows}
              rowKey={(row) => row.id ?? row.billingName ?? ''}
              loading={payments.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('fin.payment.none')}
            />
          )}
        </Card>
      ) : null}
      {refunding ? (
        <Card title={t('fin.payment.refundOne', { receipt: refunding.receiptNumber ?? refunding.id ?? '' })}>
          {refund.error ? <Banner intent="danger">{failureText(t, refund.error)}</Banner> : null}
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              if (!refundOk) return;
              refund.mutate(
                {
                  paymentId: refunding.id ?? '',
                  etag: refunding.etag ?? '',
                  amount: refundAmount,
                  refundMethod,
                  reason: refundReason,
                },
                {
                  onSuccess: (done) => {
                    setRefunding(null);
                    setNotice(
                      t(done.status === 'processed' ? 'fin.payment.refunded' : 'fin.payment.refundPending'),
                    );
                  },
                },
              );
            }}
          >
            <TextInput
              label={t('fin.payment.refundAmount')}
              hint={t('fin.payment.refundAmountHint')}
              inputMode="decimal"
              value={refundAmount}
              error={refundOk ? undefined : t('fin.common.badAmount')}
              onChange={(event) => setRefundAmount(event.target.value)}
            />
            <SelectField
              label={t('fin.payment.method')}
              options={REFUND_METHODS.filter(
                (m) => m !== 'gateway' || refunding.paymentMethod === 'gateway',
              ).map((value) => ({ value, label: t(METHOD_LABEL[value]!) }))}
              value={refundMethod}
              onChange={(event) => setRefundMethod(event.target.value as RefundMethod)}
            />
            <TextInput
              label={t('fin.payment.reason')}
              maxLength={300}
              value={refundReason}
              onChange={(event) => setRefundReason(event.target.value)}
            />
            <div className="page__actions">
              <Button variant="secondary" type="button" onClick={() => setRefunding(null)}>
                {t('fin.common.close')}
              </Button>
              <Button type="submit" loading={refund.isPending} disabled={!refundOk}>
                {t('fin.payment.refund')}
              </Button>
            </div>
          </form>
        </Card>
      ) : null}
      {mayRecord ? (
        <CounterPayment onDone={(receipt) => setNotice(t('fin.payment.recorded', { receipt }))} />
      ) : null}
      {mayRefunds ? (
        <Card title={t('fin.payment.refunds')}>
          {refunds.isError ? (
            <RetryBanner onRetry={() => void refunds.refetch()} />
          ) : (
            <DataTable
              caption={t('fin.payment.refunds')}
              columns={refundColumns}
              rows={refunds.data?.items}
              rowKey={(row) => row.id ?? row.paymentId}
              loading={refunds.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('fin.payment.noRefunds')}
              density="compact"
            />
          )}
        </Card>
      ) : null}
    </div>
  );
}

/** The counter payment: a student's open dues and invoices, an amount for each, exactly the payment. */
function CounterPayment({ onDone }: { onDone: (receipt: string) => void }) {
  const { t } = useI18n();
  const [studentId, setStudentId] = useState('');
  const student = isId(studentId.trim()) ? studentId.trim() : '';
  const dues = useFeeDues(feeFilter({ studentId: student, status: 'draft' }), Boolean(student));
  const invoices = useInvoices(feeFilter({ studentId: student }), Boolean(student));
  const record = useRecordFeePayment();
  const [amounts, setAmounts] = useState<Readonly<Record<string, string>>>({});
  const [payer, setPayer] = useState('');
  const [method, setMethod] = useState<CounterMethod>('cash');
  const [reference, setReference] = useState('');
  const [instrumentDate, setInstrumentDate] = useState('');
  const [bank, setBank] = useState('');
  const targets: Target[] = [
    ...(dues.data?.items ?? [])
      .filter((due) => due.netAmount && toCents(due.netAmount) !== 0)
      .map((due) => ({
        key: `due:${due.id ?? ''}`,
        kind: 'due' as const,
        id: due.id ?? '',
        label: t('fin.payment.dueOf', { date: due.dueDate ?? '' }),
        outstanding: due.netAmount ?? '0',
        currencyId: due.currencyId,
      })),
    ...(invoices.data?.items ?? [])
      .filter((invoice) => invoice.status === 'posted' || invoice.status === 'partially_paid')
      .map((invoice) => ({
        key: `invoice:${invoice.id ?? ''}`,
        kind: 'invoice' as const,
        id: invoice.id ?? '',
        label: t('fin.invoice.titleOf', { number: invoice.invoiceNumber }),
        outstanding: centsText(
          (toCents(invoice.total ?? '0') ?? 0) - (toCents(invoice.amountPaid ?? '0') ?? 0),
        ),
        currencyId: invoice.currencyId,
      })),
  ];
  const money = useFeeMoney(targets[0]?.currencyId);
  const slices = targets.filter((target) => (toCents(amounts[target.key] ?? '') ?? 0) > 0);
  const badSlice = targets.some(
    (target) => (amounts[target.key] ?? '').trim() && toCents(amounts[target.key] ?? '') === null,
  );
  const total = sumAmounts(slices.map((target) => amounts[target.key] ?? '0'));
  const ready = Boolean(student && payer.trim() && slices.length > 0 && !badSlice && total);
  return (
    <Card title={t('fin.payment.record')}>
      <p className="hint">{t('fin.payment.recordHint')}</p>
      {record.error ? <Banner intent="danger">{failureText(t, record.error)}</Banner> : null}
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault();
          if (!ready || !total) return;
          record.mutate(
            {
              payerName: payer,
              billingName: payer,
              amount: total,
              currencyId: slices[0]?.currencyId ?? '',
              paymentMethod: method,
              instrumentReference: reference,
              ...(instrumentDate ? { instrumentDate } : {}),
              bankName: bank,
              allocations: slices.map((target) =>
                target.kind === 'due'
                  ? { studentFeeDueId: target.id, amount: amounts[target.key] ?? '' }
                  : { invoiceId: target.id, amount: amounts[target.key] ?? '' },
              ),
            },
            {
              onSuccess: (payment) => {
                setAmounts({});
                setReference('');
                onDone(payment.receiptNumber ?? '');
              },
            },
          );
        }}
      >
        <TextInput
          label={t('fin.ledger.studentId')}
          value={studentId}
          error={studentId.trim() && !student ? t('fin.common.badId') : undefined}
          onChange={(event) => {
            setStudentId(event.target.value);
            setAmounts({});
          }}
        />
        {student && !dues.isPending && !invoices.isPending && targets.length === 0 ? (
          <p className="hint">{t('fin.payment.nothingOpen')}</p>
        ) : null}
        {targets.map((target) => (
          <TextInput
            key={target.key}
            label={`${target.label} · ${t('fin.payment.outstanding', { amount: money(target.outstanding) })}`}
            inputMode="decimal"
            value={amounts[target.key] ?? ''}
            error={
              (amounts[target.key] ?? '').trim() && toCents(amounts[target.key] ?? '') === null
                ? t('fin.common.badAmount')
                : undefined
            }
            onChange={(event) => setAmounts({ ...amounts, [target.key]: event.target.value })}
          />
        ))}
        <TextInput
          label={t('fin.payment.payer')}
          maxLength={200}
          value={payer}
          onChange={(e) => setPayer(e.target.value)}
        />
        <SelectField
          label={t('fin.payment.method')}
          options={COUNTER_METHODS.map((value) => ({ value, label: t(METHOD_LABEL[value]!) }))}
          value={method}
          onChange={(event) => setMethod(event.target.value as CounterMethod)}
        />
        {method !== 'cash' ? (
          <>
            <TextInput
              label={t('fin.payment.reference')}
              maxLength={80}
              value={reference}
              onChange={(event) => setReference(event.target.value)}
            />
            <TextInput
              label={t('fin.payment.instrumentDate')}
              type="date"
              value={instrumentDate}
              onChange={(event) => setInstrumentDate(event.target.value)}
            />
            <TextInput
              label={t('fin.payment.bank')}
              maxLength={120}
              value={bank}
              onChange={(e) => setBank(e.target.value)}
            />
          </>
        ) : null}
        <p>{t('fin.payment.total', { amount: total ? money(total) : '' })}</p>
        <div className="page__actions">
          <Button type="submit" loading={record.isPending} disabled={!ready}>
            {t('fin.payment.recordSubmit')}
          </Button>
        </div>
      </form>
    </Card>
  );
}
