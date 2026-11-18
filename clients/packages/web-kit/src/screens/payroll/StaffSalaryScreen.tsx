import {
  grossOf,
  isAmount,
  localDay,
  salaryInForce,
  useCan,
  useCreateStaffSalary,
  useDeleteStaffSalary,
  useI18n,
  usePayComponents,
  useSalaryTemplates,
  useSaveStaffSalaryComponent,
  useAllStaff,
  useStaffNames,
  useStaffSalaries,
  useStaffSalaryComponents,
  useTemplateComponents,
  type StaffSalary,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  StatusChip,
  TextArea,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { componentTypeLabel, failureText, useMoney, revisionOf } from './format';
import { RetryBanner } from './shared';

/** Where a salary stands on a day: the one in force, one that takes effect later, or one that was replaced. */
type Standing = 'current' | 'upcoming' | 'past';
const STANDING: Readonly<Record<Standing, { label: MessageKey; intent: 'success' | 'info' | 'neutral' }>> = {
  current: { label: 'pay.salary.current', intent: 'success' },
  upcoming: { label: 'pay.salary.upcoming', intent: 'info' },
  past: { label: 'pay.salary.past', intent: 'neutral' },
};

/**
 * HR-06 Staff salary: each staff member's effective-dated salaries — the one in force today marked, later ones
 * marked upcoming — with the resolved amount of every component of the salary's template. A new salary is entered
 * from a date with a template and an amount per component; its gross is the exact sum of the earning amounts. The
 * server refuses (409) to delete a salary a payslip was generated from.
 */
export function StaffSalaryScreen() {
  const { t, format, timeZone } = useI18n();
  const mayRead = useCan('payroll.staff_salary.read');
  const mayCreate = useCan('payroll.staff_salary.create');
  const mayDelete = useCan('payroll.staff_salary.delete');
  const mayReadAmounts = useCan('payroll.staff_salary_component.read');
  const mayAddAmount = useCan('payroll.staff_salary_component.create');
  const mayChangeAmount = useCan('payroll.staff_salary_component.update');
  const mayTemplates = useCan('payroll.salary_template.read');
  const mayTemplateComponents = useCan('payroll.salary_template_component.read');
  const mayPayComponents = useCan('payroll.pay_component.read');
  const mayStaff = useCan('hr.staff.read');
  const mayPersons = useCan('people.person.read');

  const today = localDay(new Date(), timeZone);
  const salaries = useStaffSalaries({ enabled: mayRead });
  const templates = useSalaryTemplates(mayRead && mayTemplates);
  const payComponents = usePayComponents(mayRead && mayPayComponents);
  const directory = useAllStaff(mayRead && mayCreate && mayStaff);
  const salaryRows = salaries.data?.items ?? [];
  const staffIds = [
    ...new Set([
      ...salaryRows.map((s) => s.staffId),
      ...(directory.data?.items ?? []).flatMap((s) => (s.id ? [s.id] : [])),
    ]),
  ];
  const names = useStaffNames(staffIds, mayStaff && mayPersons);
  const [staffId, setStaffId] = useState<string | null>(null);
  const [salaryId, setSalaryId] = useState<string | null>(null);
  const ownSalaries = salaryRows
    .filter((salary) => salary.staffId === staffId)
    .sort((a, b) => b.effectiveDate.localeCompare(a.effectiveDate));
  const inForce = staffId ? salaryInForce(salaryRows, staffId, today) : undefined;
  const opened = ownSalaries.find((salary) => salary.id === salaryId) ?? inForce ?? ownSalaries[0];
  const amounts = useStaffSalaryComponents(mayReadAmounts && opened ? (opened.id ?? null) : null);
  const openedTemplate = (templates.data?.items ?? []).find((item) => item.id === opened?.salaryTemplateId);
  const openedSlots = useTemplateComponents(mayTemplateComponents && opened ? opened.salaryTemplateId : null);
  const saveAmount = useSaveStaffSalaryComponent();
  const deleteSalary = useDeleteStaffSalary();
  const create = useCreateStaffSalary();

  const [edits, setEdits] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const [newStaff, setNewStaff] = useState('');
  const [newTemplate, setNewTemplate] = useState('');
  const [newDate, setNewDate] = useState(today);
  const [newAmounts, setNewAmounts] = useState<Record<string, string>>({});
  const [newRemarks, setNewRemarks] = useState('');
  const activeTemplates = (templates.data?.items ?? []).filter((item) => item.isActive !== false);
  const draftTemplate = newTemplate || activeTemplates[0]?.id || '';
  const draftSlots = useTemplateComponents(
    mayCreate && mayTemplateComponents && draftTemplate ? draftTemplate : null,
  );
  const draftStaff = newStaff || directory.data?.items[0]?.id || '';
  const money = useMoney(openedTemplate?.institutionId ?? activeTemplates[0]?.institutionId);

  const title = t('screen.HR06.title');
  if (!mayRead)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('pay.salary.noAccess')}</Banner>
      </div>
    );

  const nameOf = (id: string) => names.get(id) ?? t('pay.common.someone');
  const templateName = (id: string) =>
    (templates.data?.items ?? []).find((item) => item.id === id)?.name ?? '';
  const componentOf = new Map((payComponents.data?.items ?? []).map((item) => [item.id, item]));
  const standing = (salary: StaffSalary): Standing =>
    salary.effectiveDate > today
      ? 'upcoming'
      : salary.id === salaryInForce(salaryRows, salary.staffId, today)?.id
        ? 'current'
        : 'past';
  const error = saveAmount.error ?? deleteSalary.error ?? create.error ?? null;
  const clearOutcome = () => {
    setNotice(null);
    saveAmount.reset();
    deleteSalary.reset();
    create.reset();
  };

  // one row per staff member: what is in force today and what comes next
  const people = [...new Set(salaryRows.map((salary) => salary.staffId))];
  const peopleColumns: Column<string>[] = [
    { key: 'staff', header: t('pay.common.staff'), render: (id) => nameOf(id) },
    {
      key: 'current',
      header: t('pay.salary.inForce'),
      render: (id) => {
        const current = salaryInForce(salaryRows, id, today);
        return current
          ? t('pay.salary.grossFrom', {
              gross: money(current.grossSalary),
              date: format.day(current.effectiveDate),
            })
          : t('pay.salary.noneInForce');
      },
    },
    {
      key: 'next',
      header: t('pay.salary.next'),
      render: (id) => {
        const next = salaryRows
          .filter((salary) => salary.staffId === id && salary.effectiveDate > today)
          .sort((a, b) => a.effectiveDate.localeCompare(b.effectiveDate))[0];
        return next
          ? t('pay.salary.grossFrom', {
              gross: money(next.grossSalary),
              date: format.day(next.effectiveDate),
            })
          : '';
      },
    },
  ];
  const historyColumns: Column<StaffSalary>[] = [
    { key: 'from', header: t('pay.salary.from'), render: (row) => format.day(row.effectiveDate) },
    {
      key: 'template',
      header: t('pay.salary.template'),
      render: (row) => templateName(row.salaryTemplateId),
    },
    { key: 'gross', header: t('pay.salary.gross'), align: 'end', render: (row) => money(row.grossSalary) },
    {
      key: 'standing',
      header: t('pay.salary.standing'),
      render: (row) => {
        const state = STANDING[standing(row)];
        return <StatusChip intent={state.intent} label={t(state.label)} />;
      },
    },
  ];

  const draftGross = grossOf(newAmounts, draftSlots.data?.items ?? [], payComponents.data?.items ?? []);
  const draftAmountsOk = (draftSlots.data?.items ?? []).every((slot) => {
    const value = newAmounts[slot.id ?? ''] ?? '';
    return value.trim() === '' || isAmount(value);
  });
  const filledAmounts = Object.fromEntries(
    Object.entries(newAmounts).filter(([, value]) => value.trim() !== ''),
  );

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('pay.salary.intro')}</p>
      </header>
      {error ? <Banner intent="danger">{failureText(t, error)}</Banner> : null}
      {notice && !error ? <Banner intent="success">{notice}</Banner> : null}
      <div className="page__grid">
        <Card title={t('pay.salary.list')}>
          {salaries.isError ? (
            <RetryBanner onRetry={() => void salaries.refetch()} />
          ) : (
            <DataTable
              caption={t('pay.salary.list')}
              columns={peopleColumns}
              rows={salaries.isPending ? undefined : people}
              rowKey={(id) => id}
              loading={salaries.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t(mayCreate ? 'pay.salary.noneCreate' : 'pay.salary.none')}
              onRowClick={(id) => {
                clearOutcome();
                setEdits({});
                setStaffId(id);
                setSalaryId(null);
              }}
            />
          )}
        </Card>

        {staffId ? (
          <Card title={t('pay.salary.of', { name: nameOf(staffId) })}>
            <DataTable
              caption={t('pay.salary.history')}
              columns={historyColumns}
              rows={ownSalaries}
              rowKey={(row) => row.id ?? row.effectiveDate}
              loadingLabel={t('common.state.loading')}
              emptyText={t('pay.salary.none')}
              onRowClick={(row) => {
                clearOutcome();
                setEdits({});
                setSalaryId(row.id ?? null);
              }}
            />
            {opened ? (
              <>
                <h2 className="sms-card__title">
                  {t('pay.salary.componentsFrom', { date: format.day(opened.effectiveDate) })}
                </h2>
                {opened.remarks ? <p className="hint">{opened.remarks}</p> : null}
                {!mayReadAmounts || !mayTemplateComponents ? (
                  <p className="hint">{t('pay.salary.noAmountAccess')}</p>
                ) : amounts.isError || openedSlots.isError ? (
                  <RetryBanner
                    onRetry={() => {
                      void amounts.refetch();
                      void openedSlots.refetch();
                    }}
                  />
                ) : (
                  <ul className="leave-list">
                    {(openedSlots.data?.items ?? []).map((slot) => {
                      const stored = (amounts.data?.items ?? []).find(
                        (item) => item.salaryTemplateComponentId === slot.id,
                      );
                      const component = componentOf.get(slot.payComponentId);
                      const name = component?.name ?? slot.payComponentId;
                      const edit = edits[slot.id ?? ''];
                      const may = stored ? mayChangeAmount : mayAddAmount;
                      return (
                        <li key={slot.id} className="leave-list__item">
                          <span className="leave-list__what">
                            <strong>{name}</strong> {componentTypeLabel(t, component?.componentType)} ·{' '}
                            {stored ? money(stored.amount) : t('pay.salary.missingAmount')}
                          </span>
                          {may ? (
                            <form
                              className="leave-list__item"
                              onSubmit={(event) => {
                                event.preventDefault();
                                const revision = revisionOf(stored);
                                if (edit === undefined || !isAmount(edit) || !revision) return;
                                clearOutcome();
                                saveAmount.mutate(
                                  {
                                    ...revision,
                                    staffSalaryId: opened.id ?? '',
                                    salaryTemplateComponentId: slot.id ?? '',
                                    amount: edit,
                                  },
                                  {
                                    onSuccess: () => {
                                      setEdits(({ [slot.id ?? '']: _saved, ...rest }) => rest);
                                      setNotice(t('pay.salary.done.amountSaved', { name }));
                                    },
                                  },
                                );
                              }}
                            >
                              <TextInput
                                label={t('pay.salary.amountOf', { name })}
                                inputMode="decimal"
                                value={edit ?? stored?.amount ?? ''}
                                error={
                                  edit !== undefined && !isAmount(edit)
                                    ? t('pay.salary.badAmount')
                                    : undefined
                                }
                                onChange={(event) =>
                                  setEdits({ ...edits, [slot.id ?? '']: event.target.value })
                                }
                              />
                              <Button
                                type="submit"
                                variant="secondary"
                                disabled={
                                  edit === undefined ||
                                  !isAmount(edit) ||
                                  saveAmount.isPending ||
                                  !revisionOf(stored)
                                }
                              >
                                {t('pay.common.save')}
                              </Button>
                            </form>
                          ) : null}
                        </li>
                      );
                    })}
                  </ul>
                )}
                {mayDelete ? (
                  <div className="page__actions">
                    <Button
                      variant="danger"
                      loading={deleteSalary.isPending}
                      disabled={!opened.etag}
                      onClick={() => {
                        clearOutcome();
                        deleteSalary.mutate(
                          { id: opened.id ?? '', etag: opened.etag ?? '' },
                          {
                            onSuccess: () => {
                              setSalaryId(null);
                              setNotice(t('pay.salary.done.deleted'));
                            },
                          },
                        );
                      }}
                    >
                      {t('pay.salary.delete')}
                    </Button>
                  </div>
                ) : null}
              </>
            ) : null}
          </Card>
        ) : null}

        {mayCreate ? (
          <Card title={t('pay.salary.new')}>
            {!mayStaff || !mayTemplates || !mayTemplateComponents || !mayPayComponents ? (
              <p className="hint">{t('pay.salary.noCreateLookups')}</p>
            ) : activeTemplates.length === 0 && !templates.isPending ? (
              <p className="hint">{t('pay.salary.noTemplates')}</p>
            ) : (
              <form
                className="form"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!draftStaff || !draftTemplate || !newDate || !draftAmountsOk || draftGross === null)
                    return;
                  clearOutcome();
                  create.mutate(
                    {
                      staffId: draftStaff,
                      salaryTemplateId: draftTemplate,
                      effectiveDate: newDate,
                      grossSalary: draftGross,
                      remarks: newRemarks,
                      amounts: filledAmounts,
                    },
                    {
                      onSuccess: (salary) => {
                        setNewAmounts({});
                        setNewRemarks('');
                        setStaffId(salary.staffId);
                        setSalaryId(salary.id ?? null);
                        setNotice(t('pay.salary.done.created'));
                      },
                    },
                  );
                }}
              >
                <SelectField
                  label={t('pay.common.staff')}
                  options={(directory.data?.items ?? []).map((member) => ({
                    value: member.id ?? '',
                    label: names.get(member.id ?? '') ?? member.id ?? '',
                  }))}
                  value={draftStaff}
                  onChange={(event) => setNewStaff(event.target.value)}
                />
                <SelectField
                  label={t('pay.salary.template')}
                  options={activeTemplates.map((item) => ({ value: item.id ?? '', label: item.name }))}
                  value={draftTemplate}
                  onChange={(event) => {
                    setNewTemplate(event.target.value);
                    setNewAmounts({});
                  }}
                />
                <TextInput
                  label={t('pay.salary.from')}
                  type="date"
                  value={newDate}
                  onChange={(event) => setNewDate(event.target.value)}
                />
                {(draftSlots.data?.items ?? []).map((slot) => {
                  const component = componentOf.get(slot.payComponentId);
                  const name = component?.name ?? slot.payComponentId;
                  const value = newAmounts[slot.id ?? ''] ?? '';
                  return (
                    <TextInput
                      key={slot.id}
                      label={t('pay.salary.amountOf', { name })}
                      hint={componentTypeLabel(t, component?.componentType)}
                      inputMode="decimal"
                      value={value}
                      error={value.trim() !== '' && !isAmount(value) ? t('pay.salary.badAmount') : undefined}
                      onChange={(event) =>
                        setNewAmounts({ ...newAmounts, [slot.id ?? '']: event.target.value })
                      }
                    />
                  );
                })}
                <p className="hint">
                  {draftGross === null
                    ? t('pay.salary.badAmount')
                    : t('pay.salary.grossIs', { gross: money(draftGross) })}
                </p>
                <TextArea
                  label={t('pay.salary.remarks')}
                  rows={2}
                  value={newRemarks}
                  onChange={(event) => setNewRemarks(event.target.value)}
                />
                <div className="page__actions">
                  <Button
                    type="submit"
                    loading={create.isPending}
                    disabled={
                      !draftStaff || !draftTemplate || !newDate || !draftAmountsOk || draftGross === null
                    }
                  >
                    {t('pay.salary.create')}
                  </Button>
                </div>
              </form>
            )}
          </Card>
        ) : null}
      </div>
    </div>
  );
}
