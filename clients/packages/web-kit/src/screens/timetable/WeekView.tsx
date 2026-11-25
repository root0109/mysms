import {
  shiftDay,
  timetableDays,
  useI18n,
  useTimetable,
  weekOf,
  type TimetableSession,
  type TimetableSubject,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, StatusChip } from '@mysms/ui-web';

/**
 * One week of a published timetable (timetable.GetTimetable) for the subjects given — a class, a room or a staff
 * member — day by day in the reader's time, with the week's moves. `describe` says what each session is beside its
 * time (the screen knows which names matter for its subject).
 */
export function WeekView({
  subjects,
  week,
  today,
  onWeek,
  describe,
}: {
  subjects: readonly TimetableSubject[];
  /** Any day of the week shown, `YYYY-MM-DD`. */
  week: string;
  today: string;
  onWeek: (day: string) => void;
  describe: (session: TimetableSession) => { what: string; where: string };
}) {
  const { t, format, timeZone } = useI18n();
  const { from, to } = weekOf(week);
  const timetable = useTimetable(subjects, from, to);
  const days = timetableDays(timetable.data ?? [], from, to, timeZone);
  return (
    <>
      <Card>
        <p className="hint">{t('tt.my.week', { from: format.day(from), to: format.day(to) })}</p>
        <div className="page__actions">
          <Button variant="secondary" onClick={() => onWeek(shiftDay(from, -7))}>
            {t('tt.my.previousWeek')}
          </Button>
          <Button variant="secondary" disabled={from === weekOf(today).from} onClick={() => onWeek(today)}>
            {t('tt.my.thisWeek')}
          </Button>
          <Button variant="secondary" onClick={() => onWeek(shiftDay(from, 7))}>
            {t('tt.my.nextWeek')}
          </Button>
        </div>
      </Card>
      {timetable.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={8} />
      ) : timetable.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void timetable.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : (
        days.map((day) => (
          <Card
            key={day.date}
            title={`${format.weekday(day.date)}, ${format.day(day.date)}`}
            aria-current={day.date === today ? 'date' : undefined}
          >
            {day.sessions.length === 0 ? (
              <p className="hint">{t('tt.my.free')}</p>
            ) : (
              <ul className="timetable-day">
                {day.sessions.map((session) => {
                  const { what, where } = describe(session);
                  return (
                    <li key={session.id} className="timetable-day__session">
                      <span className="timetable-day__time">
                        {t('tt.my.time', {
                          from: format.time(session.startAt),
                          to: format.time(session.endAt),
                        })}
                      </span>
                      <span className="timetable-day__what">
                        <strong>{what}</strong>
                        {where ? <span className="hint"> {where}</span> : null}
                      </span>
                      {session.status === 'cancelled' ? (
                        <StatusChip intent="danger" label={t('tt.my.cancelled')} />
                      ) : session.status === 'done' ? (
                        <StatusChip intent="success" label={t('tt.my.done')} />
                      ) : null}
                    </li>
                  );
                })}
              </ul>
            )}
          </Card>
        ))
      )}
    </>
  );
}
