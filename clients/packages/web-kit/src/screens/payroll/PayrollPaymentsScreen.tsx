import {
  PAYMENT_METHODS,
  localDay,
  toCents,
  useCan,
  useDeleteDisbursementAccount,
  useDisbursementAccounts,
  useI18n,
  useInstitutions,
  usePaymentTypes,
  usePayrollPayments,
  useRecordPayrollPayment,
  useSaveDisbursementAccount,
  useSession,
  useAllStaff,
  useStaffNames,
  type DisbursementAccount,
  type PaymentMethod,
  type PayrollPayment,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, SelectField, TextInput, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { METHOD, failureText, useMoney, revisionOf } from './format';
import { RetryBanner } from './shared';

const EMPTY_ACCOUNT = { name: '', accountKey: '', accountNumber: '', bankRoutingCode: '', branchName: '' };

/**
 * HR-09 Payroll payments: what was paid to staff (salary payments recorded when a payslip is marked paid, and
 * payments of other kinds recorded here) and the institution bank accounts salaries are paid from. An account number
 * is write-only: it is typed when an account is added or replaced and is only ever shown masked, as the API answers
 * it. Payments are documents: the contract offers no change or delete for them.
 */
export function PayrollPaymentsScreen() {
  const { t, format, timeZone } = useI18n();
  const userId = useSession((s) => s.me?.user?.id ?? '');
  const mayRead = useCan('payroll.payroll_payment.read');
  const mayRecord = useCan('payroll.payroll_payment.create');
  const mayAccounts = useCan('payroll.disbursement_account.read');
  const mayAddAccount = useCan('payroll.disbursement_account.create');
  const mayChangeAccount = useCan('payroll.disbursement_account.update');
  const mayRemoveAccount = useCan('payroll.disbursement_account.delete');
  const mayTypes = useCan('payroll.payment_type.read');
  const mayInstitutions = useCan('org.institution.read');
  const mayStaff = useCan('hr.staff.read');
  const mayPersons = useCan('people.person.read');

  const today = localDay(new Date(), timeZone);
  const payments = usePayrollPayments(mayRead);
  const accounts = useDisbursementAccounts(mayAccounts);
  const types = usePaymentTypes(mayRecord && mayTypes);
  const institutions = useInstitutions(mayInstitutions && (mayRead || mayAccounts));
  const directory = useAllStaff(mayRecord && mayStaff);
  const staffIds = [
    ...(payments.data?.items ?? []).map((payment) => payment.staffId),
    ...(directory.data?.items ?? []).flatMap((member) => (member.id ? [member.id] : [])),
  ];
  const names = useStaffNames(staffIds, mayStaff && mayPersons);
  const firstInstitution = institutions.data?.items[0]?.id ?? '';
  const [institutionChoice, setInstitution] = useState('');
  const institutionId = institutionChoice || firstInstitution;
  const money = useMoney(institutionId);
  const record = useRecordPayrollPayment();
  const saveAccount = useSaveDisbursementAccount();
  const removeAccount = useDeleteDisbursementAccount();

  const [notice, setNotice] = useState<string | null>(null);
  const [payee, setPayee] = useState('');
  const [number, setNumber] = useState('');
  const [paidOn, setPaidOn] = useState(today);
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState<PaymentMethod>('bank_transfer');
  const [typeId, setTypeId] = useState('');
  const [fromAccount, setFromAccount] = useState('');
  const [reference, setReference] = useState('');
  const [editing, setEditing] = useState<DisbursementAccount | null>(null);
  const [account, setAccount] = useState(EMPTY_ACCOUNT);

  const title = t('screen.HR09.title');
  if (!mayRead && !mayAccounts)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('pay.payments.noAccess')}</Banner>
      </div>
    );

  const nameOf = (id: string) => names.get(id) ?? t('pay.common.someone');
  const institutionName = new Map((institutions.data?.items ?? []).map((item) => [item.id, item.name]));
  const accountName = new Map(
    (accounts.data?.items ?? []).map((item) => [
      item.id,
      `${item.name} ${item.accountNumberMasked ?? ''}`.trim(),
    ]),
  );
  const typeName = new Map((types.data?.items ?? []).map((item) => [item.id, item.name]));
  const error = record.error ?? saveAccount.error ?? removeAccount.error ?? null;
  const clearOutcome = () => {
    setNotice(null);
    record.reset();
    saveAccount.reset();
    removeAccount.reset();
  };
  const payeeId = payee || directory.data?.items[0]?.id || '';
  const institutionAccounts = (accounts.data?.items ?? []).filter(
    (item) => item.institutionId === institutionId,
  );
  const paymentReady = Boolean(
    payeeId && institutionId && number.trim() && paidOn && (toCents(amount) ?? 0) > 0,
  );
  const accountReady = Boolean(
    account.name.trim() &&
    account.accountKey.trim() &&
    account.bankRoutingCode.trim() &&
    (editing || account.accountNumber.trim()) &&
    institutionId,
  );

  const paymentColumns: Column<PayrollPayment>[] = [
    { key: 'date', header: t('pay.payments.date'), render: (row) => format.day(row.transactionDate) },
    { key: 'number', header: t('pay.payments.number'), render: (row) => row.transactionNumber },
    { key: 'staff', header: t('pay.common.staff'), render: (row) => nameOf(row.staffId) },
    {
      key: 'kind',
      header: t('pay.payments.kind'),
      render: (row) =>
        row.payslipId
          ? t('pay.payments.forPayslip')
          : (typeName.get(row.paymentTypeId ?? '') ?? t('pay.payments.other')),
    },
    { key: 'method', header: t('pay.run.pay.method'), render: (row) => t(METHOD[row.paymentMethod]) },
    {
      key: 'account',
      header: t('pay.run.pay.account'),
      render: (row) => (row.disbursementAccountId ? (accountName.get(row.disbursementAccountId) ?? '') : ''),
    },
    { key: 'amount', header: t('pay.payslip.amount'), align: 'end', render: (row) => money(row.amount) },
  ];
  const accountColumns: Column<DisbursementAccount>[] = [
    { key: 'name', header: t('pay.accounts.name'), render: (row) => `${row.name} (${row.accountKey})` },
    {
      key: 'institution',
      header: t('pay.common.institution'),
      render: (row) => institutionName.get(row.institutionId) ?? row.institutionId,
    },
    { key: 'number', header: t('pay.accounts.number'), render: (row) => row.accountNumberMasked ?? '' },
    { key: 'routing', header: t('pay.accounts.routing'), render: (row) => row.bankRoutingCode },
    {
      key: 'actions',
      header: t('pay.common.actions'),
      render: (row) => (
        <div className="record-actions__buttons">
          {mayChangeAccount ? (
            <Button
              variant="ghost"
              aria-label={t('pay.accounts.editOne', { name: row.name })}
              onClick={() => {
                clearOutcome();
                setEditing(row);
                setInstitution(row.institutionId);
                setAccount({
                  name: row.name,
                  accountKey: row.accountKey,
                  accountNumber: '',
                  bankRoutingCode: row.bankRoutingCode,
                  branchName: row.branchName ?? '',
                });
              }}
            >
              {t('pay.common.edit')}
            </Button>
          ) : null}
          {mayRemoveAccount ? (
            <Button
              variant="ghost"
              aria-label={t('pay.accounts.removeOne', { name: row.name })}
              disabled={removeAccount.isPending || !row.etag}
              onClick={() => {
                clearOutcome();
                removeAccount.mutate(
                  { id: row.id ?? '', etag: row.etag ?? '' },
                  { onSuccess: () => setNotice(t('pay.accounts.done.removed')) },
                );
              }}
            >
              {t('pay.common.remove')}
            </Button>
          ) : null}
        </div>
      ),
    },
  ];

  const institutionField = mayInstitutions ? (
    <SelectField
      label={t('pay.common.institution')}
      options={(institutions.data?.items ?? []).map((item) => ({ value: item.id ?? '', label: item.name }))}
      value={institutionId}
      onChange={(event) => setInstitution(event.target.value)}
    />
  ) : null;

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('pay.payments.intro')}</p>
      </header>
      {error ? <Banner intent="danger">{failureText(t, error)}</Banner> : null}
      {notice && !error ? <Banner intent="success">{notice}</Banner> : null}
      {mayRead ? (
        <Card title={t('pay.payments.list')}>
          {payments.isError ? (
            <RetryBanner onRetry={() => void payments.refetch()} />
          ) : (
            <DataTable
              caption={t('pay.payments.list')}
              columns={paymentColumns}
              rows={payments.data?.items}
              rowKey={(row) => row.id ?? row.transactionNumber}
              loading={payments.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('pay.payments.none')}
            />
          )}
        </Card>
      ) : null}
      <div className="page__grid">
        {mayRecord ? (
          <Card title={t('pay.payments.record')}>
            {!mayInstitutions || !mayStaff ? (
              <p className="hint">{t('pay.payments.noRecordLookups')}</p>
            ) : (
              <form
                className="form"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!paymentReady) return;
                  clearOutcome();
                  record.mutate(
                    {
                      institutionId,
                      staffId: payeeId,
                      transactionNumber: number,
                      transactionDate: paidOn,
                      amount,
                      paymentMethod: method,
                      ...(typeId || types.data?.items[0]?.id
                        ? { paymentTypeId: typeId || types.data?.items[0]?.id }
                        : {}),
                      ...(fromAccount ? { disbursementAccountId: fromAccount } : {}),
                      referenceNumber: reference,
                    },
                    {
                      onSuccess: () => {
                        setNumber('');
                        setAmount('');
                        setReference('');
                        setNotice(t('pay.payments.done.recorded'));
                      },
                    },
                  );
                }}
              >
                {institutionField}
                <SelectField
                  label={t('pay.common.staff')}
                  options={(directory.data?.items ?? []).map((member) => ({
                    value: member.id ?? '',
                    label: names.get(member.id ?? '') ?? member.id ?? '',
                  }))}
                  value={payeeId}
                  onChange={(event) => setPayee(event.target.value)}
                />
                {mayTypes && (types.data?.items.length ?? 0) > 0 ? (
                  <SelectField
                    label={t('pay.payments.kind')}
                    options={(types.data?.items ?? []).map((item) => ({
                      value: item.id ?? '',
                      label: item.name,
                    }))}
                    value={typeId || types.data?.items[0]?.id || ''}
                    onChange={(event) => setTypeId(event.target.value)}
                  />
                ) : null}
                <TextInput
                  label={t('pay.payments.number')}
                  maxLength={40}
                  value={number}
                  onChange={(event) => setNumber(event.target.value)}
                />
                <TextInput
                  label={t('pay.payments.date')}
                  type="date"
                  value={paidOn}
                  onChange={(event) => setPaidOn(event.target.value)}
                />
                <TextInput
                  label={t('pay.payslip.amount')}
                  inputMode="decimal"
                  value={amount}
                  error={
                    amount.trim() && !((toCents(amount) ?? 0) > 0) ? t('pay.salary.badAmount') : undefined
                  }
                  onChange={(event) => setAmount(event.target.value)}
                />
                <SelectField
                  label={t('pay.run.pay.method')}
                  options={PAYMENT_METHODS.map((value) => ({ value, label: t(METHOD[value]) }))}
                  value={method}
                  onChange={(event) => setMethod(event.target.value as PaymentMethod)}
                />
                {mayAccounts ? (
                  <SelectField
                    label={t('pay.payments.fromAccount')}
                    options={[
                      { value: '', label: t('pay.payments.noAccount') },
                      ...institutionAccounts.map((item) => ({
                        value: item.id ?? '',
                        label: accountName.get(item.id) ?? '',
                      })),
                    ]}
                    value={fromAccount}
                    onChange={(event) => setFromAccount(event.target.value)}
                  />
                ) : null}
                <TextInput
                  label={t('pay.run.pay.reference')}
                  maxLength={80}
                  value={reference}
                  onChange={(event) => setReference(event.target.value)}
                />
                <div className="page__actions">
                  <Button type="submit" loading={record.isPending} disabled={!paymentReady}>
                    {t('pay.payments.submit')}
                  </Button>
                </div>
              </form>
            )}
          </Card>
        ) : null}

        {mayAccounts ? (
          <Card title={t('pay.accounts.title')}>
            {accounts.isError ? (
              <RetryBanner onRetry={() => void accounts.refetch()} />
            ) : (
              <DataTable
                caption={t('pay.accounts.title')}
                columns={accountColumns}
                rows={accounts.data?.items}
                rowKey={(row) => row.id ?? row.accountKey}
                loading={accounts.isPending}
                loadingLabel={t('common.state.loading')}
                emptyText={t('pay.accounts.none')}
                density="compact"
              />
            )}
            {(editing ? mayChangeAccount : mayAddAccount) && mayInstitutions ? (
              <form
                className="form"
                onSubmit={(event) => {
                  event.preventDefault();
                  const revision = revisionOf(editing);
                  if (!accountReady || !revision) return;
                  clearOutcome();
                  saveAccount.mutate(
                    {
                      ...revision,
                      institutionId,
                      managedByUserId: editing?.managedByUserId ?? userId,
                      ...account,
                    },
                    {
                      onSuccess: () => {
                        setNotice(t(editing ? 'pay.accounts.done.saved' : 'pay.accounts.done.added'));
                        setEditing(null);
                        setAccount(EMPTY_ACCOUNT);
                      },
                    },
                  );
                }}
              >
                <h3 className="sms-card__title">
                  {editing ? t('pay.accounts.editOne', { name: editing.name }) : t('pay.accounts.add')}
                </h3>
                {institutionField}
                <TextInput
                  label={t('pay.accounts.name')}
                  maxLength={120}
                  value={account.name}
                  onChange={(event) => setAccount({ ...account, name: event.target.value })}
                />
                <TextInput
                  label={t('pay.accounts.key')}
                  maxLength={40}
                  value={account.accountKey}
                  onChange={(event) => setAccount({ ...account, accountKey: event.target.value })}
                />
                <TextInput
                  label={t('pay.accounts.number')}
                  hint={t(editing ? 'pay.accounts.numberKeep' : 'pay.accounts.numberHint')}
                  autoComplete="off"
                  value={account.accountNumber}
                  onChange={(event) => setAccount({ ...account, accountNumber: event.target.value })}
                />
                <TextInput
                  label={t('pay.accounts.routing')}
                  maxLength={20}
                  value={account.bankRoutingCode}
                  onChange={(event) => setAccount({ ...account, bankRoutingCode: event.target.value })}
                />
                <TextInput
                  label={t('pay.accounts.branch')}
                  maxLength={150}
                  value={account.branchName}
                  onChange={(event) => setAccount({ ...account, branchName: event.target.value })}
                />
                <div className="page__actions">
                  {editing ? (
                    <Button
                      variant="secondary"
                      type="button"
                      onClick={() => {
                        setEditing(null);
                        setAccount(EMPTY_ACCOUNT);
                      }}
                    >
                      {t('pay.common.cancelEdit')}
                    </Button>
                  ) : null}
                  <Button type="submit" loading={saveAccount.isPending} disabled={!accountReady}>
                    {t(editing ? 'pay.common.save' : 'pay.accounts.addSubmit')}
                  </Button>
                </div>
              </form>
            ) : null}
          </Card>
        ) : null}
      </div>
    </div>
  );
}
