import {
  STAFF_STATUSES,
  useCan,
  useDepartments,
  useDesignations,
  useI18n,
  usePersonNames,
  usePersonsNamed,
  useStaffDirectory,
  type Staff,
  type StaffStatus,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  StatusChip,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import type { MessageKey } from '@mysms/i18n';
import { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router';
import { StaffFormDialog } from './StaffFormDialog';
import { STAFF_STATUS, staffLink, staffProblem } from './staffText';

/** The filters that live in the address, so a link or a reload opens the same list. */
type Filter = 'status' | 'designation' | 'department';

/**
 * HR-01 Staff directory: everyone the caller may see, found by employee number or name and narrowed by status,
 * designation and main department; a row opens the staff record (HR-02), where it is edited, its status changed and
 * its login account created (ui/02 §1.6: actions live on the detail page). A role that may create staff adds a
 * staff member here.
 */
export function StaffDirectoryScreen() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const mayRead = useCan('hr.staff.read');
  const mayCreate = useCan('hr.staff.create');
  const mayPersons = useCan('people.person.read');
  const mayDesignations = useCan('hr.designation.read');
  const mayDepartments = useCan('org.department.read');
  const [query, setQuery] = useSearchParams();
  const [search, setSearch] = useState('');
  const [adding, setAdding] = useState(false);
  // a record deleted on its own page comes back here with what happened
  const notice = (useLocation().state as { notice?: MessageKey } | null)?.notice;

  const status = STAFF_STATUSES.find((value) => value === query.get('status'));
  const designationId = query.get('designation') ?? '';
  const departmentId = query.get('department') ?? '';
  const setFilter = (name: Filter, value: string) =>
    setQuery(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (value) next.set(name, value);
        else next.delete(name);
        return next;
      },
      { replace: true },
    );

  const designations = useDesignations(mayRead && mayDesignations);
  const departments = useDepartments(mayRead && mayDepartments);
  const text = search.trim();
  const persons = usePersonsNamed(text, mayRead && mayPersons);
  // a name search waits for the persons it matches; a search by number alone does not
  const personsSettled = !mayPersons || text.length < 2 || !persons.isPending;
  const list = useStaffDirectory(
    {
      ...(text ? { search: text } : {}),
      ...(status ? { status } : {}),
      ...(designationId ? { designationId } : {}),
      ...(departmentId ? { departmentId } : {}),
    },
    persons.data ?? [],
    mayRead && personsSettled,
  );
  const rows = list.data?.pages.flatMap((page) => page.data) ?? [];
  const names = usePersonNames(
    rows.map((row) => row.personId),
    mayPersons,
  );
  const designationName = new Map((designations.data ?? []).map((d) => [d.id, d.name]));
  const departmentName = new Map((departments.data ?? []).map((d) => [d.id, d.name]));
  const title = t('screen.HR01.title');

  if (!mayRead)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('hr.staff.noAccess')}</Banner>
      </div>
    );

  const nameOf = (row: Staff) =>
    names.get(row.personId) ??
    (row.employeeCode ? t('hr.staff.unnamed', { code: row.employeeCode }) : t('hr.staff.noName'));
  const columns: Column<Staff>[] = [
    {
      key: 'name',
      header: t('hr.staff.column.name'),
      render: (row) => (
        <Link to={staffLink(row.id ?? '')} aria-label={t('hr.staff.openFor', { name: nameOf(row) })}>
          {nameOf(row)}
        </Link>
      ),
    },
    { key: 'code', header: t('hr.staff.column.code'), render: (row) => row.employeeCode ?? '—' },
    {
      key: 'designation',
      header: t('hr.staff.column.designation'),
      render: (row) => (row.designationId && designationName.get(row.designationId)) || '—',
    },
    {
      key: 'department',
      header: t('hr.staff.column.department'),
      render: (row) => (row.primaryDepartmentId && departmentName.get(row.primaryDepartmentId)) || '—',
    },
    {
      key: 'status',
      header: t('hr.staff.column.status'),
      render: (row) => {
        const state = row.status ? STAFF_STATUS[row.status] : undefined;
        return <StatusChip intent={state?.intent} label={state ? t(state.label) : (row.status ?? '—')} />;
      },
    },
    {
      key: 'account',
      header: t('hr.staff.column.account'),
      render: (row) => t(row.userId ? 'hr.staff.account.yes' : 'hr.staff.account.no'),
    },
  ];
  const filtered = Boolean(text || status || designationId || departmentId);

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('hr.staff.intro')}</p>
      </header>
      {notice ? <Banner intent="success">{t(notice)}</Banner> : null}
      <Card
        title={t('common.list.count', { count: rows.length })}
        actions={mayCreate ? <Button onClick={() => setAdding(true)}>{t('hr.staff.add')}</Button> : null}
      >
        <div className="page__grid">
          <TextInput
            label={t(mayPersons ? 'hr.staff.search' : 'hr.staff.searchNumberOnly')}
            type="search"
            maxLength={100}
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
          <SelectField
            label={t('hr.staff.filter.status')}
            value={status ?? ''}
            onChange={(event) => setFilter('status', event.target.value)}
            options={[
              { value: '', label: t('hr.staff.filter.all') },
              ...STAFF_STATUSES.map((value: StaffStatus) => ({ value, label: t(STAFF_STATUS[value].label) })),
            ]}
          />
          {designations.data ? (
            <SelectField
              label={t('hr.staff.filter.designation')}
              value={designationId}
              onChange={(event) => setFilter('designation', event.target.value)}
              options={[
                { value: '', label: t('hr.staff.filter.all') },
                ...designations.data.map((d) => ({ value: d.id ?? '', label: d.name })),
              ]}
            />
          ) : null}
          {departments.data ? (
            <SelectField
              label={t('hr.staff.filter.department')}
              value={departmentId}
              onChange={(event) => setFilter('department', event.target.value)}
              options={[
                { value: '', label: t('hr.staff.filter.all') },
                ...departments.data.map((d) => ({ value: d.id ?? '', label: d.name })),
              ]}
            />
          ) : null}
        </div>
        {list.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void list.refetch()}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {staffProblem(t, list.error)}
          </Banner>
        ) : (
          <DataTable
            caption={title}
            columns={columns}
            rows={rows}
            rowKey={(row) => row.id ?? row.personId}
            loading={list.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t(!filtered && mayCreate ? 'hr.staff.emptyFirst' : 'hr.staff.empty')}
          />
        )}
        {list.hasNextPage ? (
          <Button variant="ghost" loading={list.isFetchingNextPage} onClick={() => void list.fetchNextPage()}>
            {t('common.list.more')}
          </Button>
        ) : null}
      </Card>
      {adding ? (
        <StaffFormDialog
          onClose={() => setAdding(false)}
          onSaved={(saved) => {
            setAdding(false);
            void navigate(staffLink(saved.id ?? ''), { state: { notice: 'hr.staff.created' } });
          }}
        />
      ) : null}
    </div>
  );
}
