import {
  feeFilter,
  localDay,
  toCents,
  useAcademicYears,
  useCan,
  useFeeDues,
  useFeeHeads,
  useFeePlans,
  useGenerateFeeDues,
  useI18n,
  useInvoiceFromDues,
  useStudentNames,
  type FeeDue,
  type FinanceOperation,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  TextArea,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { failureText, idList, isId, useFeeMoney } from './fees';
import { FeeStatus, RetryBanner } from './shared';

/**
 * FIN-02 Generate dues: charges a fee plan for an academic year to named students and/or a whole class section
 * (whose roster sis gives), from a start date (finance.GenerateFeeDues). The run is safe to repeat: a student already
 * charged for an installment and head keeps that due. The answer is the run's operation with its counts.
 */
export function GenerateDuesScreen() {
  const { t, timeZone } = useI18n();
  const mayGenerate = useCan('finance.student_fee_due.generate_fee_dues');
  const mayYears = useCan('org.academic_year.read');
  const plans = useFeePlans(mayGenerate);
  const years = useAcademicYears(mayGenerate && mayYears);
  const generate = useGenerateFeeDues();
  const [planChoice, setPlan] = useState('');
  const [yearChoice, setYear] = useState('');
  const [startDate, setStartDate] = useState(localDay(new Date(), timeZone));
  const [students, setStudents] = useState('');
  const [classSectionId, setClassSectionId] = useState('');
  const [total, setTotal] = useState('');
  const [outcome, setOutcome] = useState<FinanceOperation | null>(null);
  const title = t('screen.FIN02.title');
  if (!mayGenerate)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('fin.common.noAccess')}</Banner>
      </div>
    );
  const planId = planChoice || plans.data?.items[0]?.id || '';
  const plan = plans.data?.items.find((item) => item.id === planId);
  const yearOptions = (years.data?.items ?? []).filter(
    (year) => !plan || year.institutionId === plan.institutionId,
  );
  const academicYearId = yearChoice || yearOptions[0]?.id || '';
  const studentIds = idList(students);
  const section = classSectionId.trim();
  const totalOk = !total.trim() || (toCents(total) ?? 0) > 0;
  const ready = Boolean(
    planId &&
    isId(academicYearId) &&
    startDate &&
    studentIds !== null &&
    (studentIds.length > 0 || isId(section)) &&
    (!section || isId(section)) &&
    totalOk,
  );
  const result = (outcome?.result ?? {}) as {
    students?: number;
    dues?: number;
    kept?: number;
    skippedCount?: number;
  };
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('fin.generate.intro')}</p>
      </header>
      {generate.error ? <Banner intent="danger">{failureText(t, generate.error)}</Banner> : null}
      {outcome ? (
        <Banner intent="success">
          {t('fin.generate.done', {
            students: result.students ?? 0,
            dues: result.dues ?? 0,
            kept: result.kept ?? 0,
            skipped: result.skippedCount ?? 0,
          })}
        </Banner>
      ) : null}
      {plans.isError ? <RetryBanner onRetry={() => void plans.refetch()} /> : null}
      <Card title={t('fin.generate.form')}>
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault();
            if (!ready || studentIds === null) return;
            setOutcome(null);
            generate.mutate(
              {
                feePlanId: planId,
                academicYearId,
                startDate,
                studentIds,
                ...(section ? { classSectionId: section } : {}),
                ...(total.trim() ? { totalAmount: total } : {}),
              },
              { onSuccess: setOutcome },
            );
          }}
        >
          <SelectField
            label={t('fin.setup.plan')}
            options={(plans.data?.items ?? []).map((item) => ({ value: item.id ?? '', label: item.name }))}
            value={planId}
            onChange={(event) => setPlan(event.target.value)}
          />
          {mayYears ? (
            <SelectField
              label={t('fin.common.year')}
              options={yearOptions.map((year) => ({ value: year.id ?? '', label: year.name }))}
              value={academicYearId}
              onChange={(event) => setYear(event.target.value)}
            />
          ) : (
            <TextInput
              label={t('fin.common.yearId')}
              value={yearChoice}
              error={yearChoice && !isId(yearChoice) ? t('fin.common.badId') : undefined}
              onChange={(event) => setYear(event.target.value.trim())}
            />
          )}
          <TextInput
            label={t('fin.generate.start')}
            type="date"
            value={startDate}
            onChange={(event) => setStartDate(event.target.value)}
          />
          <TextArea
            label={t('fin.generate.students')}
            hint={t('fin.generate.studentsHint')}
            value={students}
            error={studentIds === null ? t('fin.common.badId') : undefined}
            onChange={(event) => setStudents(event.target.value)}
          />
          <TextInput
            label={t('fin.generate.classSection')}
            hint={t('fin.generate.classSectionHint')}
            value={classSectionId}
            error={section && !isId(section) ? t('fin.common.badId') : undefined}
            onChange={(event) => setClassSectionId(event.target.value)}
          />
          <TextInput
            label={t('fin.generate.total')}
            hint={t('fin.generate.totalHint')}
            inputMode="decimal"
            value={total}
            error={totalOk ? undefined : t('fin.common.badAmount')}
            onChange={(event) => setTotal(event.target.value)}
          />
          <div className="page__actions">
            <Button type="submit" loading={generate.isPending} disabled={!ready}>
              {t('fin.generate.submit')}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

/**
 * FIN-03 Dues ledger: the dues the fee office reads, filtered by year, fee head, status and student; the draft dues
 * of one student are raised as an invoice (finance.CreateInvoiceFromDues), which then opens (FIN-05).
 */
export function DuesLedgerScreen() {
  const { t, format } = useI18n();
  const navigate = useNavigate();
  const mayRead = useCan('finance.student_fee_due.read');
  const mayInvoice = useCan('finance.invoice.create_invoice_from_dues');
  const mayYears = useCan('org.academic_year.read');
  const mayHeads = useCan('finance.fee_head.read');
  const [yearId, setYearId] = useState('');
  const [headId, setHeadId] = useState('');
  const [status, setStatus] = useState('');
  const [studentId, setStudentId] = useState('');
  const [chosen, setChosen] = useState<readonly string[]>([]);
  const [billTo, setBillTo] = useState('');
  const student = isId(studentId.trim()) ? studentId.trim() : '';
  const dues = useFeeDues(
    feeFilter({ academicYearId: yearId, feeHeadId: headId, status, studentId: student }),
    mayRead,
  );
  const heads = useFeeHeads(mayRead && mayHeads);
  const years = useAcademicYears(mayRead && mayYears);
  const rows = dues.data?.items ?? [];
  const names = useStudentNames(rows.map((row) => row.studentId));
  const money = useFeeMoney(rows[0]?.currencyId);
  const invoice = useInvoiceFromDues();
  const title = t('screen.FIN03.title');
  if (!mayRead)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('fin.common.noAccess')}</Banner>
      </div>
    );
  const headName = new Map((heads.data?.items ?? []).map((head) => [head.id, head.name]));
  const picked = rows.filter((row) => chosen.includes(row.id ?? ''));
  const pickedStudent = picked[0]?.studentId;
  const oneStudent = picked.every((row) => row.studentId === pickedStudent);
  const toggle = (row: FeeDue) =>
    setChosen((current) =>
      current.includes(row.id ?? '') ? current.filter((id) => id !== row.id) : [...current, row.id ?? ''],
    );
  const columns: Column<FeeDue>[] = [
    ...(mayInvoice
      ? [
          {
            key: 'pick',
            header: t('fin.ledger.pick'),
            render: (row: FeeDue) =>
              row.status === 'draft' ? (
                <input
                  type="checkbox"
                  aria-label={t('fin.ledger.pickOne', {
                    due: headName.get(row.feeHeadId ?? '') ?? row.id ?? '',
                  })}
                  checked={chosen.includes(row.id ?? '')}
                  onChange={() => toggle(row)}
                />
              ) : null,
          },
        ]
      : []),
    {
      key: 'student',
      header: t('fin.common.student'),
      render: (row) => names.get(row.studentId) ?? row.studentId,
    },
    {
      key: 'head',
      header: t('fin.ledger.head'),
      render: (row) => headName.get(row.feeHeadId ?? '') ?? row.feeHeadId ?? '',
    },
    {
      key: 'due',
      header: t('fin.ledger.dueDate'),
      render: (row) => (row.dueDate ? format.day(row.dueDate) : ''),
    },
    { key: 'amount', header: t('fin.ledger.amount'), align: 'end', render: (row) => money(row.amount) },
    {
      key: 'discount',
      header: t('fin.ledger.discount'),
      align: 'end',
      render: (row) => money(row.discountAmount),
    },
    { key: 'net', header: t('fin.ledger.net'), align: 'end', render: (row) => money(row.netAmount) },
    { key: 'status', header: t('fin.common.status'), render: (row) => <FeeStatus status={row.status} /> },
  ];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('fin.ledger.intro')}</p>
      </header>
      {invoice.error ? <Banner intent="danger">{failureText(t, invoice.error)}</Banner> : null}
      <Card title={t('fin.common.filters')}>
        <div className="page__grid">
          {mayYears ? (
            <SelectField
              label={t('fin.common.year')}
              options={[
                { value: '', label: t('fin.common.all') },
                ...(years.data?.items ?? []).map((year) => ({ value: year.id ?? '', label: year.name })),
              ]}
              value={yearId}
              onChange={(event) => setYearId(event.target.value)}
            />
          ) : null}
          <SelectField
            label={t('fin.ledger.head')}
            options={[
              { value: '', label: t('fin.common.all') },
              ...(heads.data?.items ?? []).map((head) => ({ value: head.id ?? '', label: head.name })),
            ]}
            value={headId}
            onChange={(event) => setHeadId(event.target.value)}
          />
          <SelectField
            label={t('fin.common.status')}
            options={[
              { value: '', label: t('fin.common.all') },
              { value: 'draft', label: t('fin.status.draft') },
              { value: 'invoiced', label: t('fin.status.invoiced') },
              { value: 'cancelled', label: t('fin.status.cancelled') },
            ]}
            value={status}
            onChange={(event) => setStatus(event.target.value)}
          />
          <TextInput
            label={t('fin.ledger.studentId')}
            value={studentId}
            error={studentId.trim() && !student ? t('fin.common.badId') : undefined}
            onChange={(event) => setStudentId(event.target.value)}
          />
        </div>
      </Card>
      <Card title={t('fin.ledger.list')}>
        {dues.isError ? (
          <RetryBanner onRetry={() => void dues.refetch()} />
        ) : (
          <DataTable
            caption={t('fin.ledger.list')}
            columns={columns}
            rows={rows}
            rowKey={(row) => row.id ?? row.studentId}
            loading={dues.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('fin.ledger.none')}
          />
        )}
      </Card>
      {mayInvoice && picked.length > 0 ? (
        <Card title={t('fin.ledger.raise')}>
          {!oneStudent ? <Banner intent="warning">{t('fin.ledger.oneStudent')}</Banner> : null}
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              if (!oneStudent || !pickedStudent) return;
              invoice.mutate(
                {
                  studentId: pickedStudent,
                  dueIds: picked.map((row) => row.id ?? ''),
                  billToName: billTo || names.get(pickedStudent),
                },
                {
                  onSuccess: (created) => {
                    setChosen([]);
                    if (created.id) navigate(`/app/finance/invoices/${created.id}`);
                  },
                },
              );
            }}
          >
            <TextInput
              label={t('fin.ledger.billTo')}
              hint={t('fin.ledger.billToHint')}
              maxLength={200}
              value={billTo}
              onChange={(event) => setBillTo(event.target.value)}
            />
            <div className="page__actions">
              <Button type="submit" loading={invoice.isPending} disabled={!oneStudent}>
                {t('fin.ledger.raiseSubmit', { count: picked.length })}
              </Button>
            </div>
          </form>
        </Card>
      ) : null}
    </div>
  );
}
