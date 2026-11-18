import {
  academicYearOn,
  localDay,
  useAcademicYears,
  useCan,
  useI18n,
  useLeavePosition,
  useLeaveTypes,
  useMyStaff,
} from '@mysms/core';
import { Banner, Button, Screen, Text, useTheme } from '@mysms/ui-native';
import { View } from 'react-native';

/**
 * HR-03 on phones and tablets ("T"): the signed-in staff member's leave types and, per type, what they are entitled
 * to this academic year, have taken, have asked for and have left (hr.ListLeaveTypes, hr.GetStaffLeaveBalance). The
 * balances of others and the leave types' setup are HR's, on the web.
 */
export function LeaveBalanceScreen({
  tablet = false,
  onBack,
  onApply,
}: {
  tablet?: boolean;
  onBack: () => void;
  /** Opens the leave applications (HR-04). */
  onApply: () => void;
}) {
  const { t, format, timeZone } = useI18n();
  const theme = useTheme();
  const mayReadStaff = useCan('hr.staff.read');
  const mayPosition = useCan('hr.staff.get_leave_balance');
  const mayTypes = useCan('hr.leave_type.read');
  const mayYears = useCan('org.academic_year.read');
  const mayApply = useCan('hr.leave_application.create');
  const myStaff = useMyStaff(mayReadStaff);
  const me = myStaff.data?.items[0];
  const years = useAcademicYears(mayYears && Boolean(me));
  const year = me
    ? academicYearOn(years.data?.items ?? [], me.institutionId, localDay(new Date(), timeZone))
    : undefined;
  const types = useLeaveTypes(me?.institutionId ?? null, mayTypes);
  const position = useLeavePosition(mayPosition ? (me?.id ?? null) : null, year?.id ?? null);
  const lineOf = new Map((position.data ?? []).map((line) => [line.leaveTypeId, line]));
  const item = { marginTop: theme.space[5] };
  const section = { marginTop: theme.space[7] };

  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('hr.balance.open')}
      </Text>
      {!mayTypes && !mayPosition ? <Banner>{t('hr.balance.noAccess')}</Banner> : null}
      {mayReadStaff && myStaff.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {myStaff.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void myStaff.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {!myStaff.isPending && !myStaff.isError && !me ? (
        <Banner>{t(mayReadStaff ? 'hr.leave.notStaff' : 'hr.leave.noStaffLookup')}</Banner>
      ) : null}
      {me && mayYears && years.isSuccess && !year ? <Text muted>{t('hr.leave.noYear')}</Text> : null}
      {me && !mayYears ? <Banner>{t('hr.leave.noYearLookup')}</Banner> : null}
      {types.isError || position.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button
            variant="secondary"
            onPress={() => {
              void types.refetch();
              void position.refetch();
            }}
          >
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {me && (types.isPending || (year && position.isPending)) && mayTypes ? (
        <Text muted>{t('common.state.loading')}</Text>
      ) : null}
      {types.data && types.data.length === 0 ? <Text muted>{t('hr.balance.noTypes')}</Text> : null}
      {(types.data ?? []).map((type) => {
        const line = type.id ? lineOf.get(type.id) : undefined;
        return (
          <View key={type.id} style={item}>
            <Text style={{ fontWeight: '600' }}>{type.name}</Text>
            <Text variant="caption">
              {t('hr.balance.typeDays', { days: format.number(Number(type.maxDaysPerYear)) })}
            </Text>
            {line ? (
              <Text muted>
                {`${t('hr.leave.position.available')}: ${format.number(line.available)} · ${t('hr.leave.position.taken')}: ${format.number(line.taken)} · ${t('hr.leave.position.pending')}: ${format.number(line.pending)} · ${t('hr.leave.position.entitled')}: ${format.number(line.entitled)}`}
              </Text>
            ) : null}
          </View>
        );
      })}
      <View style={section}>
        {me && mayApply ? <Button onPress={onApply}>{t('hr.balance.openLeave')}</Button> : null}
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}
