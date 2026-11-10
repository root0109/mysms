import {
  localDay,
  shiftDay,
  timetableDays,
  useCan,
  useI18n,
  useMyStaff,
  useRoomNames,
  useSubjectNames,
  useTimetable,
  weekOf,
  type TimetableDay,
  type TimetableSession,
} from '@mysms/core';
import { Banner, Button, Screen, Text, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';

/**
 * TT-03 on phones and tablets: the signed-in teacher's published timetable. A phone shows one day at a time,
 * starting with today; a tablet shows the week of that day on one screen, two days to a row. Days and times are the reader's own (their
 * time zone setting, else the device's); a cancelled session stays visible as cancelled.
 */
export function TimetableScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format, timeZone } = useI18n();
  const theme = useTheme();
  const mayView = useCan('timetable.session.get_timetable');
  const mayReadStaff = useCan('hr.staff.read');
  const maySubjects = useCan('academics.subject.read');
  const mayRooms = useCan('facility.room.read');
  const today = localDay(new Date(), timeZone);
  // the day shown is kept as its distance from today, so it stays right when the time zone setting arrives
  const [offset, setOffset] = useState(0);
  const day = shiftDay(today, offset);
  const week = weekOf(day);
  const myStaff = useMyStaff(mayView && mayReadStaff);
  // someone employed by two schools of the tenant has a staff record in each: their timetable is both
  const mine = (myStaff.data?.items ?? []).flatMap((record) => (record.id ? [{ staffId: record.id }] : []));
  const timetable = useTimetable(mayView ? mine : null, week.from, week.to);
  const subjectNames = useSubjectNames(mayView && maySubjects);
  const roomNames = useRoomNames(mayView && mayRooms);
  const step = tablet ? 7 : 1;

  const name = (session: TimetableSession) =>
    session.title || (session.subjectId && subjectNames.get(session.subjectId)) || t('tt.my.session');
  const state = (session: TimetableSession) =>
    session.status === 'cancelled' ? t('tt.my.cancelled') : session.status === 'done' ? t('tt.my.done') : '';

  const oneDay = (shown: TimetableDay) => (
    <View
      key={shown.date}
      style={{
        width: tablet ? '50%' : '100%',
        paddingRight: tablet ? theme.space[5] : 0,
        marginBottom: theme.space[5],
      }}
    >
      <Text variant="h3" accessibilityRole="header">
        {shown.date === today
          ? `${t('tt.my.today')} · ${format.day(shown.date)}`
          : `${format.weekday(shown.date)}, ${format.day(shown.date)}`}
      </Text>
      {shown.sessions.length === 0 ? <Text muted>{t('tt.my.free')}</Text> : null}
      {shown.sessions.map((session) => (
        <View key={session.id} style={{ marginTop: theme.space[3] }}>
          <Text variant="caption" muted>
            {t('tt.my.time', { from: format.time(session.startAt), to: format.time(session.endAt) })}
          </Text>
          <Text style={session.status === 'cancelled' ? { textDecorationLine: 'line-through' } : undefined}>
            {name(session)}
          </Text>
          {session.roomId && roomNames.get(session.roomId) ? (
            <Text variant="caption" muted>
              {roomNames.get(session.roomId)}
            </Text>
          ) : null}
          {/* a state is words, never only a struck-through line (ui/04 §6) */}
          {state(session) ? <Text variant="caption">{state(session)}</Text> : null}
        </View>
      ))}
    </View>
  );

  const days = timetableDays(timetable.data ?? [], week.from, week.to, timeZone);
  const shown = tablet ? days : days.filter((one) => one.date === day);

  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.TT03.title')}
      </Text>
      {!mayView ? (
        <Banner>{t('tt.my.noAccess')}</Banner>
      ) : mayReadStaff && myStaff.isPending ? (
        <Text muted>{t('common.state.loading')}</Text>
      ) : myStaff.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void myStaff.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : mine.length === 0 ? (
        <Banner>{t(mayReadStaff ? 'tt.my.notStaff' : 'tt.my.noStaffLookup')}</Banner>
      ) : (
        <>
          {tablet ? (
            <Text muted>{t('tt.my.week', { from: format.day(week.from), to: format.day(week.to) })}</Text>
          ) : null}
          <View
            style={{
              flexDirection: 'row',
              flexWrap: 'wrap',
              gap: theme.space[3],
              marginVertical: theme.space[5],
            }}
          >
            <Button variant="secondary" onPress={() => setOffset(offset - step)}>
              {t(tablet ? 'tt.my.previousWeek' : 'tt.my.previousDay')}
            </Button>
            <Button variant="secondary" disabled={offset === 0} onPress={() => setOffset(0)}>
              {t('tt.my.today')}
            </Button>
            <Button variant="secondary" onPress={() => setOffset(offset + step)}>
              {t(tablet ? 'tt.my.nextWeek' : 'tt.my.nextDay')}
            </Button>
          </View>
          {timetable.isPending ? (
            <Text muted>{t('common.state.loading')}</Text>
          ) : timetable.isError ? (
            <>
              <Banner intent="danger">{t('common.error.generic')}</Banner>
              <Button variant="secondary" onPress={() => void timetable.refetch()}>
                {t('common.action.retry')}
              </Button>
            </>
          ) : (
            <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>{shown.map(oneDay)}</View>
          )}
        </>
      )}
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
    </Screen>
  );
}
