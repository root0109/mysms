import {
  feeFilter,
  useCan,
  useI18n,
  useInvoice,
  useInvoiceLines,
  useInvoices,
  useInvoiceStep,
  useStudentNames,
  type Invoice,
  type InvoiceLine,
  type InvoiceStep,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  Skeleton,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { failureText, useFeeMoney } from './fees';
import { FeeStatus, RetryBanner } from './shared';

/** The states an invoice list is filtered by, with their labels. */
const INVOICE_STATES: readonly (readonly [string, MessageKey])[] = [
  ['draft', 'fin.status.draft'],
  ['posted', 'fin.status.posted'],
  ['partially_paid', 'fin.status.partiallyPaid'],
  ['paid', 'fin.status.paid'],
  ['cancelled', 'fin.status.cancelled'],
];

/** Post and Cancel of one invoice, as the role may use them (a posted invoice is a document: neither applies). */
function InvoiceActions({ invoice, onDone }: { invoice: Invoice; onDone?: (changed: Invoice) => void }) {
  const { t } = useI18n();
  const mayPost = useCan('finance.invoice.post_invoice');
  const mayCancel = useCan('finance.invoice.cancel_invoice');
  const step = useInvoiceStep();
  const [reason, setReason] = useState('');
  if (invoice.status !== 'draft' || !invoice.id || !invoice.etag || (!mayPost && !mayCancel)) return null;
  const run = (which: InvoiceStep) =>
    step.mutate(
      { step: which, id: invoice.id ?? '', etag: invoice.etag ?? '', reason },
      { onSuccess: (changed) => onDone?.(changed) },
    );
  return (
    <div className="record-actions__buttons">
      {step.error ? <Banner intent="danger">{failureText(t, step.error)}</Banner> : null}
      {mayPost ? (
        <Button loading={step.isPending && step.variables?.step === 'post'} onClick={() => run('post')}>
          {t('fin.invoice.post')}
        </Button>
      ) : null}
      {mayCancel ? (
        <>
          <TextInput
            label={t('fin.invoice.cancelReason')}
            maxLength={300}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          <Button
            variant="secondary"
            loading={step.isPending && step.variables?.step === 'cancel'}
            onClick={() => run('cancel')}
          >
            {t('fin.invoice.cancel')}
          </Button>
        </>
      ) : null}
    </div>
  );
}

/**
 * FIN-04 Invoices: the invoices the fee office reads, by state; a row opens the invoice (FIN-05). A draft is posted
 * (numbered, never changed again) or cancelled (its dues are released); a posted invoice is corrected by a credit note,
 * which this release does not raise.
 */
export function InvoicesScreen() {
  const { t, format } = useI18n();
  const mayRead = useCan('finance.invoice.read');
  const [status, setStatus] = useState('');
  const invoices = useInvoices(feeFilter({ status }), mayRead);
  const rows = invoices.data?.items ?? [];
  const names = useStudentNames(rows.flatMap((row) => (row.studentId ? [row.studentId] : [])));
  const money = useFeeMoney(rows[0]?.currencyId);
  const title = t('screen.FIN04.title');
  if (!mayRead)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('fin.common.noAccess')}</Banner>
      </div>
    );
  const columns: Column<Invoice>[] = [
    {
      key: 'number',
      header: t('fin.invoice.number'),
      render: (row) => <Link to={`/app/finance/invoices/${row.id ?? ''}`}>{row.invoiceNumber}</Link>,
    },
    { key: 'date', header: t('fin.invoice.date'), render: (row) => format.day(row.invoiceDate) },
    {
      key: 'billed',
      header: t('fin.invoice.billTo'),
      render: (row) => `${row.billToName}${row.studentId ? ` · ${names.get(row.studentId) ?? ''}` : ''}`,
    },
    { key: 'total', header: t('fin.invoice.total'), align: 'end', render: (row) => money(row.total) },
    { key: 'paid', header: t('fin.invoice.paid'), align: 'end', render: (row) => money(row.amountPaid) },
    { key: 'status', header: t('fin.common.status'), render: (row) => <FeeStatus status={row.status} /> },
    { key: 'actions', header: t('fin.common.actions'), render: (row) => <InvoiceActions invoice={row} /> },
  ];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('fin.invoice.intro')}</p>
      </header>
      <Card title={t('fin.invoice.list')}>
        <SelectField
          label={t('fin.common.status')}
          options={[
            { value: '', label: t('fin.common.all') },
            ...INVOICE_STATES.map(([value, label]) => ({ value, label: t(label) })),
          ]}
          value={status}
          onChange={(event) => setStatus(event.target.value)}
        />
        {invoices.isError ? (
          <RetryBanner onRetry={() => void invoices.refetch()} />
        ) : (
          <DataTable
            caption={t('fin.invoice.list')}
            columns={columns}
            rows={rows}
            rowKey={(row) => row.id ?? row.invoiceNumber}
            loading={invoices.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('fin.invoice.none')}
          />
        )}
      </Card>
    </div>
  );
}

/**
 * FIN-05 Invoice detail: the invoice as a document — number, bill-to, dates, its lines and the totals the server
 * worked out — with Post / Cancel while it is a draft, and Print (the browser's own print of the page; no PDF is
 * rendered by any served operation).
 */
export function InvoiceDetailScreen() {
  const { t, format } = useI18n();
  const { invoiceId = '' } = useParams();
  const mayRead = useCan('finance.invoice.read');
  const mayLines = useCan('finance.invoice_line.read');
  const invoice = useInvoice(mayRead && invoiceId ? invoiceId : null);
  const lines = useInvoiceLines(mayRead && mayLines && invoiceId ? invoiceId : null);
  const money = useFeeMoney(invoice.data?.currencyId);
  if (!mayRead)
    return (
      <Card title={t('screen.FIN05.title')}>
        <p className="hint">{t('fin.common.noAccess')}</p>
      </Card>
    );
  if (invoice.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (invoice.isError) return <RetryBanner onRetry={() => void invoice.refetch()} />;
  const row = invoice.data;
  const columns: Column<InvoiceLine>[] = [
    { key: 'description', header: t('fin.invoice.line'), render: (line) => line.description },
    { key: 'price', header: t('fin.ledger.amount'), align: 'end', render: (line) => money(line.unitPrice) },
    {
      key: 'discount',
      header: t('fin.ledger.discount'),
      align: 'end',
      render: (line) => money(line.discountAmount),
    },
    { key: 'total', header: t('fin.ledger.net'), align: 'end', render: (line) => money(line.lineTotal) },
  ];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('fin.invoice.titleOf', { number: row.invoiceNumber })}</h1>
        <FeeStatus status={row.status} />
        <Link to="/app/finance/invoices">{t('screen.FIN04.title')}</Link>
      </header>
      <Card title={t('fin.invoice.facts')}>
        <dl className="facts">
          <dt>{t('fin.invoice.billTo')}</dt>
          <dd>{row.billToName}</dd>
          <dt>{t('fin.invoice.date')}</dt>
          <dd>{format.day(row.invoiceDate)}</dd>
          <dt>{t('fin.ledger.dueDate')}</dt>
          <dd>{row.dueDate ? format.day(row.dueDate) : ''}</dd>
          <dt>{t('fin.invoice.subtotal')}</dt>
          <dd>{money(row.subtotal)}</dd>
          <dt>{t('fin.ledger.discount')}</dt>
          <dd>{money(row.discountTotal)}</dd>
          <dt>{t('fin.invoice.tax')}</dt>
          <dd>{money(row.taxTotal)}</dd>
          <dt>{t('fin.invoice.total')}</dt>
          <dd>{money(row.total)}</dd>
          <dt>{t('fin.invoice.paid')}</dt>
          <dd>{money(row.amountPaid)}</dd>
        </dl>
        <div className="page__actions">
          <InvoiceActions invoice={row} onDone={() => void invoice.refetch()} />
          <Button variant="secondary" onClick={() => window.print()}>
            {t('fin.invoice.print')}
          </Button>
        </div>
      </Card>
      <Card title={t('fin.invoice.lines')}>
        {!mayLines ? (
          <p className="hint">{t('fin.invoice.noLineAccess')}</p>
        ) : lines.isError ? (
          <RetryBanner onRetry={() => void lines.refetch()} />
        ) : (
          <DataTable
            caption={t('fin.invoice.lines')}
            columns={columns}
            rows={lines.data?.items}
            rowKey={(line) => line.id ?? line.description}
            loading={lines.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('fin.invoice.noLines')}
            density="compact"
          />
        )}
      </Card>
    </div>
  );
}
