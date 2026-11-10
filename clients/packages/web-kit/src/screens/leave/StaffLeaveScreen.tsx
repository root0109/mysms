import {
  LEAVE_DAY_PARTS,
  academicYearOn,
  cancellation,
  isApiError,
  isTimetableDay,
  leaveDays,
  localDay,
  useAcademicYears,
  useApplyForLeave,
  useCan,
  useI18n,
  useLeaveApplications,
  useLeavePosition,
  useLeaveStep,
  useLeaveTypes,
  useMyStaff,
  usePendingLeave,
  useStaffNames,
  type LeaveApplication,
  type LeaveDayPart,
  type LeavePosition,
  type LeaveStep,
} from '@mysms/core';
import type { MessageKey, Translate } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  Skeleton,
  StatusChip,
  TextArea,
  TextInput,
  type Column,
  type StatusChipProps,
} from '@mysms/ui-web';
import { useState } from 'react';

/** The longest comment a decision keeps (api/state-machines.md "LeaveApplication"). */
const MAX_TEXT = 4000;

const STATUS: Readonly<Record<string, { label: MessageKey; intent: StatusChipProps['intent'] }>> = {
  pending: { label: 'hr.leave.status.pending', intent: 'warning' },
  approved: { label: 'hr.leave.status.approved', intent: 'success' },
  rejected: { label: 'hr.leave.status.rejected', intent: 'danger' },
  cancelled: { label: 'hr.leave.status.cancelled', intent: 'neutral' },
  completed: { label: 'hr.leave.status.completed', intent: 'neutral' },
};
/** The confirmation of a step: cancelling an approved leave also gives its days back. */
const doneMessage = (application: LeaveApplication, taken: LeaveStep): MessageKey =>
  taken === 'cancel' && cancellation(application) === 'cancel'
    ? 'hr.leave.done.cancelApproved'
    : `hr.leave.done.${taken}`;

const PART: Record<LeaveDayPart, MessageKey> = {
  full_day: 'hr.leave.part.fullDay',
  forenoon: 'hr.leave.part.forenoon',
  afternoon: 'hr.leave.part.afternoon',
};

function failureText(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return error.errors.map((e) => e.message).join(' ') || t('common.validation.pattern');
    if (error.status === 403) return t('hr.leave.forbidden');
  }
  return t('common.error.generic');
}

/**
 * HR-04 Leave applications, as staff and approvers use it: a staff member sees what leave they have left, applies
 * and withdraws; an approver decides on what waits. The days of an application are counted with both ends and
 * halved for a half-day part, the way the server counts them against the balance. Every record of every state
 * remains in the records manager (ADM-11) for the office.
 */
export function StaffLeaveScreen() {
  const { t, format, timeZone } = useI18n();
  const mayApply = useCan('hr.leave_application.create');
  const mayRead = useCan('hr.leave_application.read');
  const mayPosition = useCan('hr.staff.get_leave_balance');
  const mayReadStaff = useCan('hr.staff.read');
  const mayTypes = useCan('hr.leave_type.read');
  const mayYears = useCan('org.academic_year.read');
  const mayCancel = useCan('hr.leave_application.cancel_leave');
  const mayApprove = useCan('hr.leave_application.approve_leave');
  const mayReject = useCan('hr.leave_application.reject_leave');
  const mayNames = useCan('people.person.read');
  const mayDecide = mayRead && (mayApprove || mayReject);

  const today = localDay(new Date(), timeZone);
  const myStaff = useMyStaff(mayReadStaff);
  const me = myStaff.data?.items[0];
  const years = useAcademicYears(mayYears && Boolean(me));
  // the position is this year's; an application belongs to the year its first day lies in
  const year = me ? academicYearOn(years.data?.items ?? [], me.institutionId, today) : undefined;
  const position = useLeavePosition(mayPosition ? (me?.id ?? null) : null, year?.id ?? null);
  const mine = useLeaveApplications(mayRead ? (me?.id ?? null) : null);
  const queue = usePendingLeave(mayDecide);
  // nobody decides on their own leave: it is not in their queue
  const waiting = (queue.data?.items ?? []).filter((application) => application.staffId !== me?.id);
  // an approver without a staff record of their own still reads the leave types of what they decide on
  const types = useLeaveTypes(me?.institutionId ?? waiting[0]?.institutionId ?? null, mayTypes);
  const typeName = new Map((types.data ?? []).flatMap((type) => (type.id ? [[type.id, type.name]] : [])));
  const staffNames = useStaffNames(
    waiting.map((application) => application.staffId),
    mayReadStaff && mayNames,
  );
  const apply = useApplyForLeave();
  const step = useLeaveStep();

  const [leaveTypeId, setLeaveTypeId] = useState('');
  const [from, setFrom] = useState(today);
  const [to, setTo] = useState(today);
  const [dayPart, setDayPart] = useState<LeaveDayPart>('full_day');
  const [remarks, setRemarks] = useState('');
  const [sent, setSent] = useState(false);
  const [reasons, setReasons] = useState<Record<string, string>>({});
  const [decided, setDecided] = useState<MessageKey | null>(null);
  const applyYear =
    me && isTimetableDay(from) ? academicYearOn(years.data?.items ?? [], me.institutionId, from) : undefined;
  const sameYear = applyYear !== undefined && applyYear.startDate <= to && to <= applyYear.endDate;
  /** A field changed: what the last attempt answered no longer describes the form. */
  const edited = () => {
    setSent(false);
    apply.reset();
  };

  const title = t('screen.HR04.title');
  const days = isTimetableDay(from) && isTimetableDay(to) ? leaveDays(from, to, dayPart) : null;
  const typesSettled = !mayTypes || !types.isPending;
  const chosenType = leaveTypeId || types.data?.[0]?.id || '';
  const left = position.data?.find((line) => line.leaveTypeId === chosenType)?.available;
  const range = (application: LeaveApplication) => {
    const first = application.fromAt.slice(0, 10);
    const last = application.toAt.slice(0, 10);
    return first === last ? format.day(first) : `${format.day(first)} – ${format.day(last)}`;
  };
  const length = (application: LeaveApplication) =>
    t('hr.leave.days', {
      days: format.number(
        leaveDays(
          application.fromAt.slice(0, 10),
          application.toAt.slice(0, 10),
          application.dayPart ?? 'full_day',
        ) ?? 0,
      ),
    });
  const what = (application: LeaveApplication) =>
    (application.leaveTypeId && typeName.get(application.leaveTypeId)) || t('hr.leave.untyped');
  const chip = (application: LeaveApplication) => {
    const state = STATUS[application.status ?? ''];
    return <StatusChip intent={state?.intent} label={state ? t(state.label) : (application.status ?? '')} />;
  };
  const act = (application: LeaveApplication, taken: LeaveStep) => {
    setDecided(null);
    setSent(false);
    const reason = reasons[application.id ?? ''];
    step.mutate(
      {
        applicationId: application.id ?? '',
        step: taken,
        etag: application.etag ?? null,
        ...(reason ? { reason } : {}),
      },
      { onSuccess: () => setDecided(doneMessage(application, taken)) },
    );
  };

  if (!mayApply && !mayDecide)
    return (
      <div className="page">
        <header className="page__header">
          <h1 className="page__title">{title}</h1>
        </header>
        <Banner intent="info">{t('hr.leave.noAccess')}</Banner>
      </div>
    );
  if (mayReadStaff && myStaff.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;

  const positionColumns: Column<LeavePosition>[] = [
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
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{title}</h1>
        <p className="page__subtitle">{t('hr.leave.intro')}</p>
      </header>
      {step.error ? <Banner intent="danger">{failureText(t, step.error)}</Banner> : null}
      {decided && !step.error ? <Banner intent="success">{t(decided)}</Banner> : null}

      {myStaff.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void myStaff.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : !me ? (
        mayApply ? (
          <Banner intent="info">{t(mayReadStaff ? 'hr.leave.notStaff' : 'hr.leave.noStaffLookup')}</Banner>
        ) : null
      ) : (
        <>
          {!mayYears ? <Banner intent="info">{t('hr.leave.noYearLookup')}</Banner> : null}
          {years.isError ? (
            <Banner
              intent="danger"
              action={
                <Button variant="secondary" onClick={() => void years.refetch()}>
                  {t('common.action.retry')}
                </Button>
              }
            >
              {t('common.error.generic')}
            </Banner>
          ) : null}
          {mayPosition && mayYears && !years.isError ? (
            <Card title={t('hr.leave.position.title')}>
              {!year && !years.isPending ? (
                <p className="hint">{t('hr.leave.noYear')}</p>
              ) : (
                <DataTable
                  caption={t('hr.leave.position.title')}
                  columns={positionColumns}
                  rows={position.data}
                  rowKey={(line) => line.leaveTypeId}
                  loading={position.isPending}
                  loadingLabel={t('common.state.loading')}
                  emptyText={t('hr.leave.position.none')}
                />
              )}
            </Card>
          ) : null}

          {mayApply && years.isSuccess ? (
            <Card title={t('hr.leave.apply.title')}>
              <form
                className="form"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (days === null || !applyYear || !sameYear || !typesSettled || apply.isPending) return;
                  setSent(false);
                  apply.mutate(
                    {
                      institutionId: me.institutionId,
                      academicYearId: applyYear.id ?? '',
                      staffId: me.id ?? '',
                      ...(chosenType ? { leaveTypeId: chosenType } : {}),
                      from,
                      to,
                      dayPart,
                      remarks,
                    },
                    {
                      onSuccess: () => {
                        setSent(true);
                        setRemarks('');
                      },
                    },
                  );
                }}
              >
                {(types.data?.length ?? 0) > 0 ? (
                  <SelectField
                    label={t('hr.leave.type')}
                    options={(types.data ?? []).map((type) => ({ value: type.id ?? '', label: type.name }))}
                    value={chosenType}
                    onChange={(event) => {
                      setLeaveTypeId(event.target.value);
                      edited();
                    }}
                  />
                ) : null}
                <TextInput
                  label={t('hr.leave.apply.from')}
                  type="date"
                  value={from}
                  error={isTimetableDay(from) && !applyYear ? t('hr.leave.apply.noYearForDay') : undefined}
                  onChange={(event) => {
                    edited();
                    setFrom(event.target.value);
                    // a leave does not end before it starts: the last day follows the first
                    if (event.target.value > to) setTo(event.target.value);
                  }}
                />
                <TextInput
                  label={t('hr.leave.apply.to')}
                  type="date"
                  min={from}
                  value={to}
                  error={
                    days === null
                      ? t('hr.leave.apply.badRange')
                      : applyYear && !sameYear
                        ? t('hr.leave.apply.otherYear')
                        : undefined
                  }
                  onChange={(event) => {
                    edited();
                    setTo(event.target.value);
                  }}
                />
                <SelectField
                  label={t('hr.leave.apply.part')}
                  options={LEAVE_DAY_PARTS.map((part) => ({ value: part, label: t(PART[part]) }))}
                  value={dayPart}
                  onChange={(event) => {
                    edited();
                    setDayPart(event.target.value as LeaveDayPart);
                  }}
                />
                <TextArea
                  label={t('hr.leave.apply.remarks')}
                  rows={2}
                  maxLength={MAX_TEXT}
                  value={remarks}
                  onChange={(event) => {
                    edited();
                    setRemarks(event.target.value);
                  }}
                />
                {dayPart !== 'full_day' && from !== to ? (
                  <Banner intent="warning">{t('hr.leave.apply.halfDayOneDay')}</Banner>
                ) : null}
                {days !== null ? (
                  <p className="hint">
                    {left !== undefined
                      ? t('hr.leave.apply.daysOfLeft', {
                          days: format.number(days),
                          left: format.number(left),
                        })
                      : t('hr.leave.days', { days: format.number(days) })}
                  </p>
                ) : null}
                {days !== null && left !== undefined && days > left ? (
                  <Banner intent="warning">{t('hr.leave.apply.overdrawn')}</Banner>
                ) : null}
                {apply.error ? <Banner intent="danger">{failureText(t, apply.error)}</Banner> : null}
                {sent && !apply.error ? <Banner intent="success">{t('hr.leave.apply.sent')}</Banner> : null}
                <div className="page__actions">
                  <Button
                    type="submit"
                    loading={apply.isPending}
                    disabled={
                      days === null ||
                      !applyYear ||
                      !sameYear ||
                      !typesSettled ||
                      (dayPart !== 'full_day' && from !== to)
                    }
                  >
                    {t('hr.leave.apply.submit')}
                  </Button>
                </div>
              </form>
            </Card>
          ) : null}

          {mayRead ? (
            <Card title={t('hr.leave.mine.title')}>
              {mine.isPending ? (
                <Skeleton label={t('common.state.loading')} lines={3} />
              ) : (mine.data?.items.length ?? 0) === 0 ? (
                <p className="hint">{t('hr.leave.mine.none')}</p>
              ) : (
                <ul className="leave-list">
                  {(mine.data?.items ?? []).map((application) => (
                    <li key={application.id} className="leave-list__item">
                      <span className="leave-list__what">
                        <strong>{what(application)}</strong> {range(application)} · {length(application)}
                        {application.decisionComments ? (
                          <span className="hint"> {application.decisionComments}</span>
                        ) : null}
                      </span>
                      {chip(application)}
                      {mayCancel && cancellation(application) ? (
                        <Button
                          variant="secondary"
                          loading={step.isPending && step.variables?.applicationId === application.id}
                          disabled={step.isPending}
                          onClick={() => act(application, 'cancel')}
                        >
                          {t(
                            cancellation(application) === 'cancel'
                              ? 'hr.leave.mine.cancel'
                              : 'hr.leave.mine.withdraw',
                          )}
                        </Button>
                      ) : null}
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          ) : null}
        </>
      )}

      {mayDecide ? (
        <Card title={t('hr.leave.queue.title')}>
          {queue.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={3} />
          ) : queue.isError ? (
            <Banner
              intent="danger"
              action={
                <Button variant="secondary" onClick={() => void queue.refetch()}>
                  {t('common.action.retry')}
                </Button>
              }
            >
              {t('common.error.generic')}
            </Banner>
          ) : waiting.length === 0 ? (
            <p className="hint">{t('hr.leave.queue.none')}</p>
          ) : (
            <ul className="leave-list">
              {waiting.map((application) => {
                const name = staffNames.get(application.staffId) ?? t('hr.leave.queue.someone');
                const mine = step.isPending && step.variables?.applicationId === application.id;
                return (
                  <li key={application.id} className="leave-list__item">
                    <span className="leave-list__what">
                      <strong>{name}</strong> {what(application)} · {range(application)} ·{' '}
                      {length(application)}
                      {application.remarks ? <span className="hint"> {application.remarks}</span> : null}
                    </span>
                    <TextInput
                      label={t('hr.leave.queue.commentFor', { name })}
                      maxLength={MAX_TEXT}
                      value={reasons[application.id ?? ''] ?? ''}
                      onChange={(event) =>
                        setReasons({ ...reasons, [application.id ?? '']: event.target.value })
                      }
                    />
                    <div className="page__actions">
                      {mayApprove ? (
                        <Button
                          aria-label={t('hr.leave.queue.approveFor', { name })}
                          loading={mine && step.variables?.step === 'approve'}
                          disabled={step.isPending}
                          onClick={() => act(application, 'approve')}
                        >
                          {t('hr.leave.queue.approve')}
                        </Button>
                      ) : null}
                      {mayReject ? (
                        <Button
                          variant="secondary"
                          aria-label={t('hr.leave.queue.rejectFor', { name })}
                          loading={mine && step.variables?.step === 'reject'}
                          disabled={step.isPending}
                          onClick={() => act(application, 'reject')}
                        >
                          {t('hr.leave.queue.reject')}
                        </Button>
                      ) : null}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>
      ) : null}
    </div>
  );
}
