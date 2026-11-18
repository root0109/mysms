import {
  isApiError,
  portalAccountBlocker,
  staffStatusMoves,
  useCan,
  useChangeStaffStatus,
  useCreateStaffPortalAccount,
  useDeleteStaff,
  useDepartments,
  useDesignations,
  useI18n,
  usePersonNames,
  useStaff,
  type Staff,
  type StaffStatus,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, SegmentedControl, Skeleton, StatusChip } from '@mysms/ui-web';
import { Fragment, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { UUID } from '../ids';
import { StaffAssignmentsTab } from './StaffAssignmentsTab';
import { StaffFormDialog } from './StaffFormDialog';
import { StaffLeaveTab } from './StaffLeaveTab';
import { StaffSalaryTab } from './StaffSalaryTab';
import { EMPLOYMENT_TYPE, STAFF_STATUS, STATUS_MOVE, screenLink, staffProblem } from './staffText';

type Tab = 'profile' | 'assignments' | 'leave' | 'salary';
const TAB_LABEL: Readonly<Record<Tab, MessageKey>> = {
  profile: 'hr.staff.tab.profile',
  assignments: 'hr.staff.tab.assignments',
  leave: 'hr.staff.tab.leave',
  salary: 'hr.staff.tab.salary',
};
const BLOCKER: Readonly<Record<NonNullable<ReturnType<typeof portalAccountBlocker>>, MessageKey>> = {
  hasAccount: 'hr.staff.account.has',
  exited: 'hr.staff.account.exited',
  noEmployeeCode: 'hr.staff.account.noCode',
};

/** What the record says about itself: the fields of the profile, empty ones shown as a dash. */
function Profile({ staff }: { staff: Staff }) {
  const { t, format } = useI18n();
  const designations = useDesignations(useCan('hr.designation.read'));
  const departments = useDepartments(useCan('org.department.read'));
  const designation = designations.data?.find((d) => d.id === staff.designationId)?.name;
  const department = departments.data?.find((d) => d.id === staff.primaryDepartmentId)?.name;
  const facts: [MessageKey, string | undefined][] = [
    ['hr.staff.form.code', staff.employeeCode],
    ['hr.staff.form.designation', designation ?? staff.designationId],
    ['hr.staff.form.subDesignation', staff.subDesignation],
    [
      'hr.staff.form.employmentType',
      staff.employmentType ? t(EMPLOYMENT_TYPE[staff.employmentType]) : undefined,
    ],
    ['hr.staff.form.joiningDate', staff.joiningDate ? format.day(staff.joiningDate) : undefined],
    ['hr.staff.form.department', department ?? staff.primaryDepartmentId],
    ['hr.staff.form.qualification', staff.qualificationDetail],
    ['hr.staff.form.specialization', staff.specialization],
    [
      'hr.staff.form.maxPeriods',
      staff.maxPeriodsPerWeek === undefined ? undefined : format.number(staff.maxPeriodsPerWeek),
    ],
    ['hr.staff.column.account', t(staff.userId ? 'hr.staff.account.yes' : 'hr.staff.account.no')],
  ];
  return (
    <Card title={t('hr.staff.profile.title')}>
      <dl className="facts">
        {facts.map(([label, value]) => (
          <Fragment key={label}>
            <dt>{t(label)}</dt>
            <dd>{value || '—'}</dd>
          </Fragment>
        ))}
      </dl>
    </Card>
  );
}

/**
 * HR-02 Staff detail: one staff member's record. The header names them and carries the record's steps, each with its
 * permission: edit, the status moves of hr.ChangeStaffStatus (leaving asks for a confirmation), creating the login
 * account (hr.CreateStaffPortalAccount, or why it cannot be created) and delete. Tabs: profile, departments &
 * subjects, leave and salary, each shown only to a role that may read it. There is no documents tab: the contract
 * has no operation for staff documents.
 */
export function StaffDetailScreen() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const location = useLocation();
  const { staffId: param = '' } = useParams();
  const staffId = UUID.test(param) ? param : null;
  const mayRead = useCan('hr.staff.read');
  const mayUpdate = useCan('hr.staff.update');
  const mayDelete = useCan('hr.staff.delete');
  const mayMove = useCan('hr.staff.change_staff_status');
  const mayAccount = useCan('hr.staff.create_staff_portal_account');
  const mayPersons = useCan('people.person.read');
  const mayDepartments = useCan('hr.staff_department.read');
  const maySubjects = useCan('hr.staff_subject.read');
  const mayPosition = useCan('hr.staff.get_leave_balance');
  const mayApplications = useCan('hr.leave_application.read');
  const mayAssignments = mayDepartments || maySubjects;
  const mayLeave = mayPosition || mayApplications;
  const maySalary = useCan('payroll.staff_salary.read');
  const read = useStaff(mayRead ? staffId : null);
  const staff = read.data?.data;
  const etag = read.data?.etag ?? staff?.etag ?? null;
  const names = usePersonNames(staff ? [staff.personId] : [], mayPersons);
  const move = useChangeStaffStatus();
  const account = useCreateStaffPortalAccount();
  const remove = useDeleteStaff();
  const [tab, setTab] = useState<Tab>('profile');
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState<'exit' | 'delete' | null>(null);
  const arrived = (location.state as { notice?: MessageKey } | null)?.notice;
  const [notice, setNotice] = useState<string | null>(arrived ? t(arrived) : null);
  const failure = move.error ?? account.error ?? remove.error;
  const busy = move.isPending || account.isPending || remove.isPending;

  const back = (
    <Link to={screenLink('HR-01')} className="hint">
      {t('hr.staff.detail.back')}
    </Link>
  );
  if (
    !mayRead ||
    !staffId ||
    (read.isError && isApiError(read.error) && [403, 404].includes(read.error.status))
  )
    return (
      <div className="page">
        {back}
        <h1 className="page__title">{t('screen.HR02.title')}</h1>
        <Banner intent="info">{t(mayRead ? 'hr.staff.detail.notFound' : 'hr.staff.noAccess')}</Banner>
      </div>
    );
  if (read.isError)
    return (
      <div className="page">
        {back}
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void read.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {staffProblem(t, read.error)}
        </Banner>
      </div>
    );
  if (!staff) return <Skeleton label={t('common.state.loading')} lines={8} />;

  const name =
    names.get(staff.personId) ??
    (staff.employeeCode ? t('hr.staff.unnamed', { code: staff.employeeCode }) : t('hr.staff.noName'));
  const state = staff.status ? STAFF_STATUS[staff.status] : undefined;
  const blocker = portalAccountBlocker(staff);
  const tabs = (['profile', 'assignments', 'leave', 'salary'] as const).filter(
    (candidate) =>
      candidate === 'profile' ||
      (candidate === 'assignments' && mayAssignments) ||
      (candidate === 'leave' && mayLeave) ||
      (candidate === 'salary' && maySalary),
  );
  const shown = tabs.includes(tab) ? tab : 'profile';
  const moveTo = (status: StaffStatus) => {
    setNotice(null);
    setConfirming(null);
    move.mutate(
      { staffId: staff.id ?? '', status, etag },
      { onSuccess: () => setNotice(t('hr.staff.move.done', { status: t(STAFF_STATUS[status].label) })) },
    );
  };

  return (
    <div className="page">
      {back}
      <header className="page__header">
        <h1 className="page__title">{name}</h1>
        <p className="page__subtitle">
          {[staff.employeeCode, staff.employmentType ? t(EMPLOYMENT_TYPE[staff.employmentType]) : undefined]
            .filter(Boolean)
            .join(' · ')}{' '}
          <StatusChip intent={state?.intent} label={state ? t(state.label) : (staff.status ?? '—')} />
        </p>
        <div className="record-actions__buttons">
          {mayUpdate ? (
            <Button variant="secondary" disabled={busy} onClick={() => setEditing(true)}>
              {t('common.action.edit')}
            </Button>
          ) : null}
          {mayMove
            ? staffStatusMoves(staff.status).map((status) =>
                status === 'exited' && confirming === 'exit' ? (
                  <Button
                    key={status}
                    variant="danger"
                    loading={move.isPending}
                    onClick={() => moveTo(status)}
                  >
                    {t('hr.staff.move.confirmExit', { name })}
                  </Button>
                ) : (
                  <Button
                    key={status}
                    variant="secondary"
                    disabled={busy}
                    loading={move.isPending && move.variables?.status === status}
                    onClick={() => (status === 'exited' ? setConfirming('exit') : moveTo(status))}
                  >
                    {t(STATUS_MOVE[status])}
                  </Button>
                ),
              )
            : null}
          {mayAccount && blocker === null ? (
            <Button
              disabled={busy}
              loading={account.isPending}
              onClick={() => {
                setNotice(null);
                account.mutate(
                  { staffId: staff.id ?? '' },
                  {
                    onSuccess: (given) =>
                      setNotice(t('hr.staff.account.created', { code: given.employeeCode ?? '' })),
                  },
                );
              }}
            >
              {t('hr.staff.account.create')}
            </Button>
          ) : null}
          {mayDelete && etag ? (
            confirming === 'delete' ? (
              <Button
                variant="danger"
                loading={remove.isPending}
                onClick={() =>
                  remove.mutate(
                    { staffId: staff.id ?? '', etag },
                    {
                      onSuccess: () =>
                        void navigate(screenLink('HR-01'), { state: { notice: 'hr.staff.delete.done' } }),
                    },
                  )
                }
              >
                {t('hr.staff.delete.confirm')}
              </Button>
            ) : (
              <Button variant="secondary" disabled={busy} onClick={() => setConfirming('delete')}>
                {t('common.action.delete')}
              </Button>
            )
          ) : null}
        </div>
        {mayAccount && blocker !== null ? <p className="hint">{t(BLOCKER[blocker])}</p> : null}
      </header>
      {failure ? <Banner intent="danger">{staffProblem(t, failure)}</Banner> : null}
      {notice && !failure ? <Banner intent="success">{notice}</Banner> : null}

      {tabs.length > 1 ? (
        <SegmentedControl
          label={t('hr.staff.detail.tabs')}
          value={shown}
          onChange={setTab}
          options={tabs.map((value) => ({ value, label: t(TAB_LABEL[value]) }))}
        />
      ) : null}
      {shown === 'profile' ? <Profile staff={staff} /> : null}
      {shown === 'assignments' ? (
        <StaffAssignmentsTab staffId={staff.id ?? ''} institutionId={staff.institutionId} />
      ) : null}
      {shown === 'leave' ? (
        <StaffLeaveTab staffId={staff.id ?? ''} institutionId={staff.institutionId} />
      ) : null}
      {shown === 'salary' ? <StaffSalaryTab staffId={staff.id ?? ''} /> : null}

      {editing ? (
        <StaffFormDialog
          staff={staff}
          etag={etag}
          onClose={() => setEditing(false)}
          onSaved={() => {
            setEditing(false);
            setNotice(t('hr.staff.saved'));
          }}
        />
      ) : null}
    </div>
  );
}
