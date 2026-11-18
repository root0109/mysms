import {
  isPayable,
  useCan,
  useFamilyStudents,
  useFeeStatement,
  useI18n,
  useQuoteCheckout,
  useStartCheckout,
  type FamilyStudent,
  type FeeStatement,
  type Invoice,
} from '@mysms/core';
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
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router';
import { openCheckout } from './razorpay';
import { failureText, useFeeMoney } from './fees';
import { FeeStatus, RetryBanner } from './shared';

/** The totals of a statement and its open invoices, as the server worked them out. */
function StatementSummary({ statement }: { statement: FeeStatement }) {
  const { t } = useI18n();
  const money = useFeeMoney(statement.currencyId);
  return (
    <dl className="facts">
      <dt>{t('fin.statement.totalDue')}</dt>
      <dd>{money(statement.totalDue)}</dd>
      <dt>{t('fin.statement.totalPaid')}</dt>
      <dd>{money(statement.totalPaid)}</dd>
      <dt>{t('fin.statement.balance')}</dt>
      <dd>{money(statement.balance)}</dd>
    </dl>
  );
}

/** The student a family screen is about: the route's, else the first the family follows; with a switcher. */
function useChosenStudent(routeStudentId: string | undefined) {
  const family = useFamilyStudents();
  const [chosen, setChosen] = useState('');
  const studentId = routeStudentId || chosen || family.students[0]?.id || '';
  return { family, studentId, choose: setChosen };
}

function studentLabel(t: ReturnType<typeof useI18n>['t'], student: FamilyStudent) {
  return student.self ? t('fin.family.me') : (student.name ?? student.id);
}

/**
 * FIN-08 Fee statement (W; P and S on the phone): a student's dues, invoices and payments with what is owed, paid and
 * outstanding — the student's own or a child's (sis confirms the relation), or any student of the fee office's school.
 * Downloadable as the browser's print of the page.
 */
export function FeeStatementScreen() {
  const { t, format } = useI18n();
  const { studentId: routeStudent } = useParams();
  const mayRead = useCan('finance.invoice.get_fee_statement');
  const { family, studentId } = useChosenStudent(routeStudent);
  const statement = useFeeStatement(mayRead && studentId ? studentId : null);
  const money = useFeeMoney(statement.data?.currencyId);
  const title = t('screen.FIN08.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('fin.common.noAccess')}</p>
      </Card>
    );
  const invoiceColumns: Column<Invoice>[] = [
    { key: 'number', header: t('fin.invoice.number'), render: (row) => row.invoiceNumber },
    { key: 'date', header: t('fin.invoice.date'), render: (row) => format.day(row.invoiceDate) },
    { key: 'total', header: t('fin.invoice.total'), align: 'end', render: (row) => money(row.total) },
    { key: 'paid', header: t('fin.invoice.paid'), align: 'end', render: (row) => money(row.amountPaid) },
    { key: 'status', header: t('fin.common.status'), render: (row) => <FeeStatus status={row.status} /> },
  ];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <div className="page__actions">
          {family.students.map((student) => (
            <Link key={student.id} to={`/app/finance/statement/${student.id}`}>
              {studentLabel(t, student)}
            </Link>
          ))}
          <Button variant="secondary" onClick={() => window.print()}>
            {t('fin.statement.print')}
          </Button>
        </div>
      </header>
      {!studentId ? (
        <Banner intent="info">{t('fin.family.none')}</Banner>
      ) : statement.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={6} />
      ) : statement.isError ? (
        <>
          <Banner intent="danger">{failureText(t, statement.error)}</Banner>
          <RetryBanner onRetry={() => void statement.refetch()} />
        </>
      ) : (
        <>
          <Card title={t('fin.statement.summary')}>
            <StatementSummary statement={statement.data} />
          </Card>
          <Card title={t('fin.statement.dues')}>
            <ul className="plain-list">
              {(statement.data.dues ?? []).map((due) => (
                <li key={due.id}>
                  {`${due.dueDate ? format.day(due.dueDate) : ''} · ${money(due.netAmount)} · `}
                  <FeeStatus status={due.status} />
                </li>
              ))}
            </ul>
          </Card>
          <Card title={t('fin.statement.invoices')}>
            <DataTable
              caption={t('fin.statement.invoices')}
              columns={invoiceColumns}
              rows={statement.data.invoices}
              rowKey={(row) => row.id ?? row.invoiceNumber}
              loadingLabel={t('common.state.loading')}
              emptyText={t('fin.invoice.none')}
              density="compact"
            />
          </Card>
          <Card title={t('fin.statement.payments')}>
            <ul className="plain-list">
              {(statement.data.payments ?? []).map((payment) => (
                <li key={payment.id}>
                  {`${payment.receivedAt ? format.dateTime(payment.receivedAt) : ''} · ${money(payment.amount)} · ${payment.receiptNumber ?? ''} `}
                  <FeeStatus status={payment.status} />
                </li>
              ))}
            </ul>
          </Card>
        </>
      )}
    </div>
  );
}

/**
 * FIN-07 Pay fees (W; P and S open it from the phone): a student's open invoices, each paid online through the
 * gateway — the quote (what is outstanding now), then finance.StartCheckout (one Idempotency-Key per attempt, so a
 * double tap replays the first checkout) and the gateway's own window. The window's answer is not proof: the payment
 * counts once the gateway's signed notification arrives, so the screen follows the statement until it does.
 */
export function PayFeesScreen() {
  const { t } = useI18n();
  const mayPay = useCan('finance.payment.start_checkout');
  const mayRead = useCan('finance.invoice.get_fee_statement');
  const { family, studentId, choose } = useChosenStudent(undefined);
  const [waiting, setWaiting] = useState(false);
  const statement = useFeeStatement(mayRead && studentId ? studentId : null, undefined, waiting);
  const [paying, setPaying] = useState<Invoice | null>(null);
  const title = t('screen.FIN07.title');
  if (!mayPay || !mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('fin.common.noAccess')}</p>
      </Card>
    );
  const open = (statement.data?.invoices ?? []).filter(isPayable);
  const pendingOnline = (statement.data?.payments ?? []).some(
    (payment) => payment.status === 'initiated' && payment.paymentMethod === 'gateway',
  );
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('fin.pay.intro')}</p>
      </header>
      {family.isError ? <RetryBanner onRetry={family.refetch} /> : null}
      {family.students.length > 1 ? (
        <SelectField
          label={t('fin.family.student')}
          options={family.students.map((student) => ({ value: student.id, label: studentLabel(t, student) }))}
          value={studentId}
          onChange={(event) => choose(event.target.value)}
        />
      ) : null}
      {!studentId ? (
        family.isPending ? (
          <Skeleton label={t('common.state.loading')} lines={4} />
        ) : (
          <Banner intent="info">{t('fin.family.none')}</Banner>
        )
      ) : statement.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={4} />
      ) : statement.isError ? (
        <>
          <Banner intent="danger">{failureText(t, statement.error)}</Banner>
          <RetryBanner onRetry={() => void statement.refetch()} />
        </>
      ) : (
        <>
          <Card title={t('fin.statement.summary')}>
            <StatementSummary statement={statement.data} />
            <Link to={`/app/finance/statement/${studentId}`}>{t('screen.FIN08.title')}</Link>
          </Card>
          {waiting && pendingOnline ? <Banner intent="info">{t('fin.pay.waiting')}</Banner> : null}
          <Card title={t('fin.pay.open')}>
            {open.length === 0 ? <p className="hint">{t('fin.pay.nothing')}</p> : null}
            {open.map((invoice) => (
              <div key={invoice.id} className="record-actions__buttons">
                <span>{t('fin.invoice.titleOf', { number: invoice.invoiceNumber })}</span>
                <FeeStatus status={invoice.status} />
                <Button onClick={() => setPaying(invoice)}>{t('fin.pay.payThis')}</Button>
              </div>
            ))}
          </Card>
          {paying ? (
            <Checkout
              invoice={paying}
              onClose={() => setPaying(null)}
              onOpened={() => {
                setPaying(null);
                setWaiting(true);
              }}
            />
          ) : null}
        </>
      )}
    </div>
  );
}

/** The quote of one invoice, the payer's name, and the gateway window. */
function Checkout({
  invoice,
  onClose,
  onOpened,
}: {
  invoice: Invoice;
  onClose: () => void;
  onOpened: () => void;
}) {
  const { t } = useI18n();
  const quote = useQuoteCheckout(invoice.id ?? null);
  const start = useStartCheckout();
  const attemptKey = useMemo(() => crypto.randomUUID(), []);
  const [payer, setPayer] = useState(invoice.billToName);
  const [windowError, setWindowError] = useState(false);
  const money = useFeeMoney(invoice.currencyId);
  return (
    <Card title={t('fin.pay.checkout', { number: invoice.invoiceNumber })}>
      {quote.isPending ? <Skeleton label={t('common.state.loading')} lines={2} /> : null}
      {quote.isError ? <Banner intent="danger">{failureText(t, quote.error)}</Banner> : null}
      {start.error ? <Banner intent="danger">{failureText(t, start.error)}</Banner> : null}
      {windowError ? <Banner intent="danger">{t('fin.pay.windowFailed')}</Banner> : null}
      {quote.data ? (
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault();
            if (!payer.trim()) return;
            setWindowError(false);
            start.mutate(
              { invoiceId: invoice.id ?? '', billingName: payer, attemptKey },
              {
                onSuccess: (session) => {
                  if (!session.gatewayOrderId || !session.gatewayPublicKey) return;
                  openCheckout({
                    key: session.gatewayPublicKey,
                    orderId: session.gatewayOrderId,
                    title: t('screen.FIN07.title'),
                    description: invoice.invoiceNumber,
                    payerName: payer,
                  }).then(onOpened, () => setWindowError(true));
                },
              },
            );
          }}
        >
          <dl className="facts">
            <dt>{t('fin.pay.amount')}</dt>
            <dd>{money(quote.data.totalAmount)}</dd>
          </dl>
          <TextInput
            label={t('fin.pay.payer')}
            maxLength={200}
            value={payer}
            onChange={(event) => setPayer(event.target.value)}
          />
          <p className="hint">{t('fin.pay.consent')}</p>
          <div className="page__actions">
            <Button variant="secondary" type="button" onClick={onClose}>
              {t('fin.common.close')}
            </Button>
            <Button type="submit" loading={start.isPending} disabled={!payer.trim()}>
              {t('fin.pay.submit', { amount: money(quote.data.totalAmount) })}
            </Button>
          </div>
        </form>
      ) : null}
    </Card>
  );
}
