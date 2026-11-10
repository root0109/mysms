import {
  isTimetableDay,
  localDay,
  shiftDay,
  timetableDays,
  useCan,
  useClassSectionOptions,
  useI18n,
  useInstitutions,
  useMyStaff,
  useRoomNames,
  useSubjectNames,
  useTimetable,
  weekOf,
  type TimetableSession,
  type TimetableSubject,
} from '@mysms/core';
import { Banner, Button, Card, SelectField, Skeleton, StatusChip } from '@mysms/ui-web';
import { useSearchParams } from 'react-router';

const UUID = /^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i;

/**
 * TT-03 My timetable: one week of the published timetable, day by day — a teacher's own sessions, or those of a
 * class the reader picks. The week and the class live in the address (`?week=2026-03-02&class=…`), so a view can
 * be linked and reloaded. Days and times are the reader's own (their time zone setting, else the device's);
 * drafts are not part of a timetable, and a cancelled session stays visible as cancelled.
 */
export function MyTimetableScreen() {
  const { t, format, timeZone } = useI18n();
  const [search, setSearch] = useSearchParams();
  const mayView = useCan('timetable.session.get_timetable');
  const mayReadStaff = useCan('hr.staff.read');
  const mayInstitutions = useCan('org.institution.read');
  const mayClasses = useCan('academics.class_section.read');
  const mayGrades = useCan('academics.grade_level.read');
  const maySections = useCan('academics.section.read');
  // a class is named by its grade and section, so picking one takes all four reads
  const mayPickClass = mayInstitutions && mayClasses && mayGrades && maySections;
  const maySubjects = useCan('academics.subject.read');
  const mayRooms = useCan('facility.room.read');

  const today = localDay(new Date(), timeZone);
  const asked = search.get('week') ?? '';
  const week = weekOf(isTimetableDay(asked) ? asked : today);
  const askedClass = search.get('class') ?? '';

  const myStaff = useMyStaff(mayView && mayReadStaff);
  const staff = myStaff.data?.items ?? [];
  const institutions = useInstitutions(mayView && mayPickClass);
  // the classes offered are those of the reader's own school: the one they teach in, else the first they may see
  const institutionId =
    staff[0]?.institutionId ?? institutions.data?.items.find((institution) => institution.id)?.id ?? null;
  const classes = useClassSectionOptions(institutionId, mayView && mayPickClass);
  const classLabel = new Map((classes.data ?? []).map((option) => [option.id, option.label]));
  // a class from the address is taken while the classes are read, and dropped if it is not one of them
  const classSectionId =
    mayPickClass && UUID.test(askedClass) && (classes.data === undefined || classLabel.has(askedClass))
      ? askedClass
      : '';
  const subjectNames = useSubjectNames(mayView && maySubjects);
  const roomNames = useRoomNames(mayView && mayRooms);

  // someone employed by two schools of the tenant has a staff record in each: their timetable is both
  const mine: TimetableSubject[] = staff.flatMap((record) => (record.id ? [{ staffId: record.id }] : []));
  const subjects: TimetableSubject[] = classSectionId ? [{ classSectionId }] : mine;
  const timetable = useTimetable(mayView ? subjects : null, week.from, week.to);
  const title = t('screen.TT03.title');

  const go = (changes: { week?: string; class?: string }) =>
    setSearch((current) => {
      const next = new URLSearchParams(current);
      for (const [key, value] of Object.entries(changes)) {
        if (value) next.set(key, value);
        else next.delete(key);
      }
      return next;
    });

  const heading = (
    <header className="page__header">
      <h1 className="page__title">{title}</h1>
      <p className="page__subtitle">
        {t('tt.my.week', { from: format.day(week.from), to: format.day(week.to) })}
      </p>
    </header>
  );
  if (!mayView)
    return (
      <div className="page">
        {heading}
        <Banner intent="info">{t('tt.my.noAccess')}</Banner>
      </div>
    );
  if (mayReadStaff && myStaff.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (myStaff.isError)
    return (
      <div className="page">
        {heading}
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
      </div>
    );

  const days = timetableDays(timetable.data ?? [], week.from, week.to, timeZone);
  const name = (session: TimetableSession) =>
    session.title || (session.subjectId && subjectNames.get(session.subjectId)) || t('tt.my.session');
  const place = (session: TimetableSession) =>
    [
      // on a teacher's own timetable the class is what changes from session to session
      classSectionId ? null : session.classSectionId && classLabel.get(session.classSectionId),
      session.roomId && roomNames.get(session.roomId),
    ]
      .filter(Boolean)
      .join(' · ');

  return (
    <div className="page">
      {heading}
      <Card>
        <div className="form">
          {mayPickClass && (classes.data?.length ?? 0) > 0 ? (
            <SelectField
              label={t('tt.my.whose')}
              options={[
                { value: '', label: t(mine.length > 0 ? 'tt.my.mine' : 'tt.my.chooseClass') },
                ...(classes.data ?? []).map((option) => ({ value: option.id, label: option.label })),
              ]}
              value={classSectionId}
              onChange={(event) => go({ class: event.target.value })}
            />
          ) : null}
          <div className="page__actions">
            <Button variant="secondary" onClick={() => go({ week: shiftDay(week.from, -7) })}>
              {t('tt.my.previousWeek')}
            </Button>
            <Button
              variant="secondary"
              disabled={week.from === weekOf(today).from}
              onClick={() => go({ week: '' })}
            >
              {t('tt.my.thisWeek')}
            </Button>
            <Button variant="secondary" onClick={() => go({ week: shiftDay(week.from, 7) })}>
              {t('tt.my.nextWeek')}
            </Button>
          </div>
        </div>
      </Card>
      {subjects.length === 0 ? (
        <Banner intent="info">
          {t(
            !mayReadStaff && !mayPickClass
              ? 'tt.my.noStaffLookup'
              : mayPickClass
                ? 'tt.my.notStaffPick'
                : 'tt.my.notStaff',
          )}
        </Banner>
      ) : timetable.isPending ? (
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
            actions={day.date === today ? <StatusChip intent="info" label={t('tt.my.today')} /> : undefined}
            aria-current={day.date === today ? 'date' : undefined}
          >
            {day.sessions.length === 0 ? (
              <p className="hint">{t('tt.my.free')}</p>
            ) : (
              <ul className="timetable-day">
                {day.sessions.map((session) => (
                  <li key={session.id} className="timetable-day__session">
                    <span className="timetable-day__time">
                      {t('tt.my.time', {
                        from: format.time(session.startAt),
                        to: format.time(session.endAt),
                      })}
                    </span>
                    <span className="timetable-day__what">
                      <strong>{name(session)}</strong>
                      {place(session) ? <span className="hint"> {place(session)}</span> : null}
                    </span>
                    {session.status === 'cancelled' ? (
                      <StatusChip intent="danger" label={t('tt.my.cancelled')} />
                    ) : session.status === 'done' ? (
                      <StatusChip intent="success" label={t('tt.my.done')} />
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </Card>
        ))
      )}
    </div>
  );
}
