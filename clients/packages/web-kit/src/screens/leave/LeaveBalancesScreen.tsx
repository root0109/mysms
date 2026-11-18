import {
  LEAVE_DAYS,
  academicYearOn,
  localDay,
  useAcademicYears,
  useCan,
  useCreateLeaveBalance,
  useI18n,
  useLeaveBalances,
  useLeavePosition,
  useLeaveTypes,
  useMyStaff,
  usePersonNames,
  usePersonsNamed,
  useStaffDirectory,
  useStaffNames,
  useUpdateLeaveBalance,
  type LeaveBalance,
  type LeavePosition,
  type LeaveType,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  Skeleton,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useState } from 'react';
import { Link } from 'react-router';
import { ResourceScreen } from '../ResourceScreen';
import { RESOURCE_SCREENS } from '../resourceScreens';
import { screenLink, staffProblem } from '../staff/staffText';

/** The leave types' own list manager (create, edit, deactivate), as the inventory gives it to HR-03. */
const LEAVE_TYPES = RESOURCE_SCREENS.find((screen) => screen.id === 'HR-03');

/** The signed-in staff member's own position this year, for whoever has a staff record (ui/02: HR-03 "staff"). */
function MyBalance() {
  const { t, format, timeZone } = useI18n();
  const mayReadStaff = useCan('hr.staff.read');
  const mayPosition = useCan('hr.staff.get_leave_balance');
  const mayYears = useCan('org.academic_year.read');
  const mayTypes = useCan('hr.leave_type.read');
  const myStaff = useMyStaff(mayReadStaff && mayPosition);
  const me = myStaff.data?.items[0];
  const years = useAcademicYears(mayYears && Boolean(me));
  const year = me
    ? academicYearOn(years.data?.items ?? [], me.institutionId, localDay(new Date(), timeZone))
    : undefined;
  const position = useLeavePosition(me?.id ?? null, year?.id ?? null);
  const types = useLeaveTypes(me?.institutionId ?? null, mayTypes);
  const typeName = new Map((types.data ?? []).flatMap((type) => (type.id ? [[type.id, type.name]] : [])));
  if (!me || !year) return null;
  const columns: Column<LeavePosition>[] = [
    {
      key: 'type',
      header: t('hr.leave.type'),
      render: (line) => typeName.get(line.leaveTypeId) ?? t('hr.leave.untyped'),
    },
    ...(['entitled', 'taken', 'pending', 'available'] as const).map((key) => ({
      key,
      header: t(`hr.leave.position.${key}`),
      align: 'end' as const,
      render: (line: LeavePosition) => format.number(line[key]),
    })),
  ];
  return (
    <Card
      title={t('hr.balance.mine')}
      actions={<Link to={screenLink('HR-04')}>{t('hr.balance.openLeave')}</Link>}
    >
      <DataTable
        caption={t('hr.balance.mine')}
        columns={columns}
        rows={position.data}
        rowKey={(line) => line.leaveTypeId}
        loading={position.isPending}
        loadingLabel={t('common.state.loading')}
        emptyText={t('hr.leave.position.none')}
      />
    </Card>
  );
}

/** Opens a balance (hr.CreateLeaveBalance) or adjusts one (hr.UpdateLeaveBalance under its etag). */
function BalanceDialog({
  academicYearId,
  institutionId,
  types,
  balance,
  staffName,
  onClose,
  onSaved,
}: {
  academicYearId: string;
  institutionId: string;
  types: readonly LeaveType[];
  /** The balance to adjust; absent to open one. */
  balance?: LeaveBalance;
  staffName?: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const { t } = useI18n();
  const mayPersons = useCan('people.person.read');
  const create = useCreateLeaveBalance();
  const update = useUpdateLeaveBalance();
  const [search, setSearch] = useState('');
  const [staffId, setStaffId] = useState('');
  const [leaveTypeId, setLeaveTypeId] = useState(types[0]?.id ?? '');
  const [entitled, setEntitled] = useState(balance?.entitledDays ?? types[0]?.maxDaysPerYear ?? '');
  const [carried, setCarried] = useState(balance?.carriedForwardDays ?? '');
  const [checked, setChecked] = useState(false);
  const isNew = balance === undefined;
  const persons = usePersonsNamed(search, isNew && mayPersons);
  const settled = !mayPersons || search.trim().length < 2 || !persons.isPending;
  const found = useStaffDirectory(
    { search: search.trim(), status: 'active' },
    persons.data ?? [],
    isNew && search.trim().length > 0 && settled,
  );
  const candidates = (found.data?.pages.flatMap((page) => page.data) ?? []).filter(
    (member) => member.institutionId === institutionId,
  );
  const names = usePersonNames(
    candidates.map((member) => member.personId),
    isNew && mayPersons,
  );
  const entitledOk = LEAVE_DAYS.test(entitled.trim());
  const carriedOk = carried.trim() === '' || LEAVE_DAYS.test(carried.trim());
  const error = create.error ?? update.error;
  const pending = create.isPending || update.isPending;
  const submit = () => {
    setChecked(true);
    if (!entitledOk || !carriedOk || pending) return;
    if (!isNew) {
      update.mutate(
        {
          leaveBalanceId: balance.id ?? '',
          etag: balance.etag ?? '',
          entitledDays: entitled.trim(),
          carriedForwardDays: carried,
        },
        { onSuccess: onSaved },
      );
      return;
    }
    if (!staffId || !leaveTypeId) return;
    create.mutate(
      {
        staffId,
        leaveTypeId,
        academicYearId,
        entitledDays: entitled.trim(),
        ...(carried.trim() ? { carriedForwardDays: carried.trim() } : {}),
      },
      { onSuccess: onSaved },
    );
  };
  return (
    <Dialog
      open
      title={isNew ? t('hr.balance.addTitle') : t('hr.balance.editTitle')}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button type="submit" form="leave-balance-form" loading={pending}>
            {t('common.action.save')}
          </Button>
        </>
      }
    >
      <form
        id="leave-balance-form"
        className="form-grid"
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
      >
        {error ? <Banner intent="danger">{staffProblem(t, error)}</Banner> : null}
        {staffName ? <p className="hint">{staffName}</p> : null}
        {isNew ? (
          <>
            <TextInput
              label={t('hr.balance.staffSearch')}
              type="search"
              maxLength={100}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            {search.trim() && found.isSuccess && candidates.length === 0 ? (
              <p className="hint">{t('hr.balance.staffNone')}</p>
            ) : null}
            <SelectField
              label={t('hr.balance.staff')}
              value={staffId}
              error={checked && !staffId ? t('common.validation.required') : undefined}
              onChange={(event) => setStaffId(event.target.value)}
              options={[
                { value: '', label: t('hr.staff.form.none') },
                ...candidates.map((member) => ({
                  value: member.id ?? '',
                  label:
                    names.get(member.personId) ??
                    t('hr.staff.unnamed', { code: member.employeeCode ?? member.id ?? '' }),
                })),
              ]}
            />
            <SelectField
              label={t('hr.balance.type')}
              value={leaveTypeId}
              onChange={(event) => {
                setLeaveTypeId(event.target.value);
                const type = types.find((candidate) => candidate.id === event.target.value);
                if (type) setEntitled(type.maxDaysPerYear);
              }}
              options={types.map((type) => ({ value: type.id ?? '', label: type.name }))}
            />
          </>
        ) : null}
        <TextInput
          label={t('hr.balance.entitled')}
          inputMode="decimal"
          value={entitled}
          error={checked && !entitledOk ? t('hr.balance.badDays') : undefined}
          onChange={(event) => setEntitled(event.target.value)}
        />
        <TextInput
          label={t('hr.balance.carried')}
          inputMode="decimal"
          value={carried}
          error={checked && !carriedOk ? t('hr.balance.badDays') : undefined}
          onChange={(event) => setCarried(event.target.value)}
        />
      </form>
    </Dialog>
  );
}

/** Every staff member's entitlement per leave type for one academic year, as HR keeps it. */
function Balances() {
  const { t, format, timeZone } = useI18n();
  const mayCreate = useCan('hr.leave_balance.create');
  const mayUpdate = useCan('hr.leave_balance.update');
  const mayYears = useCan('org.academic_year.read');
  const mayTypes = useCan('hr.leave_type.read');
  const mayStaffNames = useCan('hr.staff.read');
  const mayPersons = useCan('people.person.read');
  const years = useAcademicYears(mayYears);
  const all = years.data?.items ?? [];
  const today = localDay(new Date(), timeZone);
  const [chosen, setChosen] = useState('');
  const year =
    all.find((candidate) => candidate.id === chosen) ??
    all.find((candidate) => candidate.startDate <= today && today <= candidate.endDate) ??
    all[0];
  const balances = useLeaveBalances(year?.id ? { academicYearId: year.id } : null);
  const types = useLeaveTypes(year?.institutionId ?? null, mayTypes);
  const typeName = new Map((types.data ?? []).flatMap((type) => (type.id ? [[type.id, type.name]] : [])));
  const rows = balances.data?.items ?? [];
  const staffNames = useStaffNames(
    rows.map((row) => row.staffId),
    mayStaffNames && mayPersons,
  );
  const [editing, setEditing] = useState<LeaveBalance | null>(null);
  const [adding, setAdding] = useState(false);
  const [saved, setSaved] = useState(false);
  const nameOf = (staffId: string) => staffNames.get(staffId) ?? t('hr.leave.queue.someone');
  const days = (value: string | undefined) => format.number(Number(value ?? 0));

  const columns: Column<LeaveBalance>[] = [
    { key: 'staff', header: t('hr.balance.staff'), render: (row) => nameOf(row.staffId) },
    {
      key: 'type',
      header: t('hr.balance.type'),
      render: (row) => typeName.get(row.leaveTypeId) ?? t('hr.leave.untyped'),
    },
    {
      key: 'entitled',
      header: t('hr.balance.entitled'),
      align: 'end',
      render: (row) => days(row.entitledDays),
    },
    {
      key: 'carried',
      header: t('hr.balance.carried'),
      align: 'end',
      render: (row) => days(row.carriedForwardDays),
    },
    { key: 'used', header: t('hr.balance.used'), align: 'end', render: (row) => days(row.usedDays) },
    ...(mayUpdate
      ? [
          {
            key: 'adjust',
            header: t('hr.balance.adjust'),
            render: (row: LeaveBalance) => (
              <Button
                variant="secondary"
                aria-label={t('hr.balance.adjustFor', { name: nameOf(row.staffId) })}
                onClick={() => {
                  setSaved(false);
                  setEditing(row);
                }}
              >
                {t('hr.balance.adjust')}
              </Button>
            ),
          },
        ]
      : []),
  ];

  const done = () => {
    setEditing(null);
    setAdding(false);
    setSaved(true);
  };
  return (
    <Card
      title={t('hr.balance.title')}
      actions={
        mayCreate && year && (types.data?.length ?? 0) > 0 ? (
          <Button
            onClick={() => {
              setSaved(false);
              setAdding(true);
            }}
          >
            {t('hr.balance.add')}
          </Button>
        ) : null
      }
    >
      <p className="hint">{t('hr.balance.intro')}</p>
      {saved ? <Banner intent="success">{t('hr.balance.saved')}</Banner> : null}
      {!mayYears ? (
        <Banner intent="info">{t('hr.leave.noYearLookup')}</Banner>
      ) : years.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={4} />
      ) : !year ? (
        <p className="hint">{t('hr.balance.noYears')}</p>
      ) : (
        <>
          <SelectField
            label={t('hr.balance.year')}
            value={year.id ?? ''}
            onChange={(event) => setChosen(event.target.value)}
            options={all.map((candidate) => ({ value: candidate.id ?? '', label: candidate.name }))}
          />
          {balances.isError ? (
            <Banner
              intent="danger"
              action={
                <Button variant="secondary" onClick={() => void balances.refetch()}>
                  {t('common.action.retry')}
                </Button>
              }
            >
              {staffProblem(t, balances.error)}
            </Banner>
          ) : (
            <DataTable
              caption={t('hr.balance.title')}
              columns={columns}
              rows={rows}
              rowKey={(row) => row.id ?? `${row.staffId}-${row.leaveTypeId}`}
              loading={balances.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('hr.balance.none')}
            />
          )}
        </>
      )}
      {(adding || editing) && year ? (
        <BalanceDialog
          academicYearId={year.id ?? ''}
          institutionId={year.institutionId}
          types={types.data ?? []}
          {...(editing ? { balance: editing, staffName: nameOf(editing.staffId) } : {})}
          onClose={() => {
            setEditing(null);
            setAdding(false);
          }}
          onSaved={done}
        />
      ) : null}
    </Card>
  );
}

/**
 * HR-03 Leave types & balance: the school's leave types (their own list manager), every staff member's balances for
 * a year — opened and adjusted by HR, never below the days already used — and, for a staff member, their own
 * position. Each part is shown only to a role that may read it; applying and deciding are HR-04's.
 */
export function LeaveBalancesScreen() {
  const { t } = useI18n();
  const mayTypes = useCan('hr.leave_type.read');
  const mayBalances = useCan('hr.leave_balance.read');
  const mayPosition = useCan('hr.staff.get_leave_balance');
  const title = t('screen.HR03.title');
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
      </header>
      {!mayTypes && !mayBalances && !mayPosition ? (
        <Banner intent="info">{t('hr.balance.noAccess')}</Banner>
      ) : null}
      {mayPosition ? <MyBalance /> : null}
      {mayBalances ? <Balances /> : null}
      {mayTypes && LEAVE_TYPES ? <ResourceScreen screen={LEAVE_TYPES} /> : null}
    </div>
  );
}
