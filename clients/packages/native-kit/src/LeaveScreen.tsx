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
  type LeaveStep,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';

const PART: Record<LeaveDayPart, MessageKey> = {
  full_day: 'hr.leave.part.fullDay',
  forenoon: 'hr.leave.part.forenoon',
  afternoon: 'hr.leave.part.afternoon',
};
const STATUS: Readonly<Record<string, MessageKey>> = {
  pending: 'hr.leave.status.pending',
  approved: 'hr.leave.status.approved',
  rejected: 'hr.leave.status.rejected',
  cancelled: 'hr.leave.status.cancelled',
  completed: 'hr.leave.status.completed',
};

/** The confirmation of a step: cancelling an approved leave also gives its days back. */
const doneMessage = (application: LeaveApplication, taken: LeaveStep): MessageKey =>
  taken === 'cancel' && cancellation(application) === 'cancel'
    ? 'hr.leave.done.cancelApproved'
    : `hr.leave.done.${taken}`;

/**
 * HR-04 on phones and tablets: a staff member sees the leave they have left, applies and withdraws; an approver
 * decides on what waits. The same rules as on the web: days are counted with both ends and halved for a half-day
 * part, and nobody decides on their own leave.
 */
export function LeaveScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format, timeZone } = useI18n();
  const theme = useTheme();
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
  const year = me ? academicYearOn(years.data?.items ?? [], me.institutionId, today) : undefined;

  const position = useLeavePosition(mayPosition ? (me?.id ?? null) : null, year?.id ?? null);
  const mine = useLeaveApplications(mayRead ? (me?.id ?? null) : null);
  const queue = usePendingLeave(mayDecide);
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
  const [decided, setDecided] = useState<MessageKey | null>(null);

  // typed dates are checked before they are counted or sent: 30 February is not a day
  const days = isTimetableDay(from) && isTimetableDay(to) ? leaveDays(from, to, dayPart) : null;
  // an application belongs to the academic year its first day lies in
  const applyYear =
    me && isTimetableDay(from) ? academicYearOn(years.data?.items ?? [], me.institutionId, from) : undefined;
  const sameYear = applyYear !== undefined && applyYear.startDate <= to && to <= applyYear.endDate;
  const halfOfSeveral = dayPart !== 'full_day' && from !== to;
  const typesSettled = !mayTypes || !types.isPending;
  const chosenType = leaveTypeId || types.data?.[0]?.id || '';
  const left = position.data?.find((line) => line.leaveTypeId === chosenType)?.available;
  const failure = (error: unknown) =>
    isApiError(error) && error.status === 409 && error.detail
      ? error.detail
      : isApiError(error) && error.status === 403
        ? t('hr.leave.forbidden')
        : isApiError(error) && error.status === 422
          ? error.errors.map((e) => e.message).join(' ') || t('common.validation.pattern')
          : t('common.error.generic');
  const summary = (application: LeaveApplication) => {
    const first = application.fromAt.slice(0, 10);
    const last = application.toAt.slice(0, 10);
    const length = leaveDays(first, last, application.dayPart ?? 'full_day') ?? 0;
    const when = first === last ? format.day(first) : `${format.day(first)} – ${format.day(last)}`;
    const type = (application.leaveTypeId && typeName.get(application.leaveTypeId)) || t('hr.leave.untyped');
    return `${type} · ${when} · ${t('hr.leave.days', { days: format.number(length) })}`;
  };
  const act = (application: LeaveApplication, taken: LeaveStep) => {
    setDecided(null);
    step.mutate(
      { applicationId: application.id ?? '', step: taken, etag: application.etag ?? null },
      { onSuccess: () => setDecided(doneMessage(application, taken)) },
    );
  };
  const section = { marginTop: theme.space[7] };
  const item = { marginTop: theme.space[5] };
  const row = { flexDirection: 'row' as const, flexWrap: 'wrap' as const, gap: theme.space[3] };

  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.HR04.title')}
      </Text>
      {!mayApply && !mayDecide ? <Banner>{t('hr.leave.noAccess')}</Banner> : null}
      {step.error ? <Banner intent="danger">{failure(step.error)}</Banner> : null}
      {decided && !step.error ? <Banner intent="success">{t(decided)}</Banner> : null}

      {mayReadStaff && myStaff.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {myStaff.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void myStaff.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {mayApply && !me && !myStaff.isPending && !myStaff.isError ? (
        <Banner>{t(mayReadStaff ? 'hr.leave.notStaff' : 'hr.leave.noStaffLookup')}</Banner>
      ) : null}

      {me && mayPosition ? (
        <View style={section}>
          <Text variant="h2" accessibilityRole="header">
            {t('hr.leave.position.title')}
          </Text>
          {mayYears && !year && !years.isPending ? <Text muted>{t('hr.leave.noYear')}</Text> : null}
          {position.isPending && year ? <Text muted>{t('common.state.loading')}</Text> : null}
          {(position.data ?? []).map((line) => (
            <View key={line.leaveTypeId} style={item}>
              <Text style={{ fontWeight: '600' }}>
                {typeName.get(line.leaveTypeId) ?? t('hr.leave.untyped')}
              </Text>
              <Text muted>
                {`${t('hr.leave.position.available')}: ${format.number(line.available)} · ${t('hr.leave.position.taken')}: ${format.number(line.taken)} · ${t('hr.leave.position.pending')}: ${format.number(line.pending)} · ${t('hr.leave.position.entitled')}: ${format.number(line.entitled)}`}
              </Text>
            </View>
          ))}
        </View>
      ) : null}

      {me && mayApply && !mayYears ? <Banner>{t('hr.leave.noYearLookup')}</Banner> : null}
      {me && mayApply && years.isSuccess ? (
        <View style={section}>
          <Text variant="h2" accessibilityRole="header">
            {t('hr.leave.apply.title')}
          </Text>
          {(types.data?.length ?? 0) > 0 ? (
            <ChoiceGroup
              label={t('hr.leave.type')}
              choices={(types.data ?? []).map((type) => ({ value: type.id ?? '', label: type.name }))}
              value={chosenType}
              onChange={setLeaveTypeId}
            />
          ) : null}
          <TextInput
            label={t('hr.leave.apply.from')}
            hint={t('hr.leave.apply.dateHint')}
            value={from}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="numbers-and-punctuation"
            error={
              !isTimetableDay(from)
                ? t('hr.leave.apply.badDate')
                : !applyYear
                  ? t('hr.leave.apply.noYearForDay')
                  : undefined
            }
            onChangeText={(value) => {
              setSent(false);
              setFrom(value);
              // a leave does not end before it starts: the last day follows the first
              if (isTimetableDay(value) && value > to) setTo(value);
            }}
          />
          <TextInput
            label={t('hr.leave.apply.to')}
            hint={t('hr.leave.apply.dateHint')}
            value={to}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="numbers-and-punctuation"
            error={
              !isTimetableDay(to)
                ? t('hr.leave.apply.badDate')
                : days === null
                  ? t('hr.leave.apply.badRange')
                  : applyYear && !sameYear
                    ? t('hr.leave.apply.otherYear')
                    : undefined
            }
            onChangeText={(value) => {
              setSent(false);
              setTo(value);
            }}
          />
          <ChoiceGroup
            label={t('hr.leave.apply.part')}
            choices={LEAVE_DAY_PARTS.map((part) => ({ value: part, label: t(PART[part]) }))}
            value={dayPart}
            onChange={(value) => setDayPart(value as LeaveDayPart)}
          />
          <TextInput
            label={t('hr.leave.apply.remarks')}
            value={remarks}
            maxLength={4000}
            multiline
            onChangeText={setRemarks}
          />
          {halfOfSeveral ? <Banner intent="warning">{t('hr.leave.apply.halfDayOneDay')}</Banner> : null}
          {days !== null ? (
            <Text muted>
              {left !== undefined
                ? t('hr.leave.apply.daysOfLeft', { days: format.number(days), left: format.number(left) })
                : t('hr.leave.days', { days: format.number(days) })}
            </Text>
          ) : null}
          {days !== null && left !== undefined && days > left ? (
            <Banner intent="warning">{t('hr.leave.apply.overdrawn')}</Banner>
          ) : null}
          {apply.error ? <Banner intent="danger">{failure(apply.error)}</Banner> : null}
          {sent && !apply.error ? <Banner intent="success">{t('hr.leave.apply.sent')}</Banner> : null}
          <Button
            loading={apply.isPending}
            disabled={days === null || !applyYear || !sameYear || halfOfSeveral || !typesSettled}
            onPress={() => {
              if (!applyYear) return;
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
            {t('hr.leave.apply.submit')}
          </Button>
        </View>
      ) : null}

      {me && mayRead ? (
        <View style={section}>
          <Text variant="h2" accessibilityRole="header">
            {t('hr.leave.mine.title')}
          </Text>
          {mine.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
          {mine.data && mine.data.items.length === 0 ? <Text muted>{t('hr.leave.mine.none')}</Text> : null}
          {(mine.data?.items ?? []).map((application) => (
            <View key={application.id} style={item}>
              <Text>{summary(application)}</Text>
              <Text variant="caption">
                {t(STATUS[application.status ?? ''] ?? 'hr.leave.status.pending')}
                {application.decisionComments ? ` · ${application.decisionComments}` : ''}
              </Text>
              {mayCancel && cancellation(application) ? (
                <View style={row}>
                  <Button
                    variant="secondary"
                    loading={step.isPending && step.variables?.applicationId === application.id}
                    onPress={() => act(application, 'cancel')}
                  >
                    {t(
                      cancellation(application) === 'cancel'
                        ? 'hr.leave.mine.cancel'
                        : 'hr.leave.mine.withdraw',
                    )}
                  </Button>
                </View>
              ) : null}
            </View>
          ))}
        </View>
      ) : null}

      {mayDecide ? (
        <View style={section}>
          <Text variant="h2" accessibilityRole="header">
            {t('hr.leave.queue.title')}
          </Text>
          {queue.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
          {queue.isError ? (
            <>
              <Banner intent="danger">{t('common.error.generic')}</Banner>
              <Button variant="secondary" onPress={() => void queue.refetch()}>
                {t('common.action.retry')}
              </Button>
            </>
          ) : null}
          {queue.data && waiting.length === 0 ? <Text muted>{t('hr.leave.queue.none')}</Text> : null}
          {waiting.map((application) => {
            const busy = step.isPending && step.variables?.applicationId === application.id;
            return (
              <View key={application.id} style={item}>
                <Text style={{ fontWeight: '600' }}>
                  {staffNames.get(application.staffId) ?? t('hr.leave.queue.someone')}
                </Text>
                <Text>{summary(application)}</Text>
                {application.remarks ? <Text muted>{application.remarks}</Text> : null}
                <View style={row}>
                  {mayApprove ? (
                    <Button loading={busy} onPress={() => act(application, 'approve')}>
                      {t('hr.leave.queue.approve')}
                    </Button>
                  ) : null}
                  {mayReject ? (
                    <Button variant="secondary" loading={busy} onPress={() => act(application, 'reject')}>
                      {t('hr.leave.queue.reject')}
                    </Button>
                  ) : null}
                </View>
              </View>
            );
          })}
        </View>
      ) : null}

      <View style={section}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}
