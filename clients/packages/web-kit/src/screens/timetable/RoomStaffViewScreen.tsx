import {
  localDay,
  useAllRecords,
  useCan,
  useI18n,
  useRoomNames,
  useStaffNames,
  useSubjectNames,
} from '@mysms/core';
import { Banner, Card, SegmentedControl, SelectField } from '@mysms/ui-web';
import { useSearchParams } from 'react-router';
import { WeekView } from './WeekView';

type Whose = 'room' | 'staff';

/**
 * TT-04 Room/staff view: the week of one room or one staff member (timetable.GetTimetable by `roomId` or
 * `staffId`), for an administrator checking where a room is free or a teacher's load. What is shown lives in the
 * address (`?view=room&by=staff&staff=…&week=…`), so a view can be linked.
 */
export function RoomStaffViewScreen() {
  const { t, timeZone } = useI18n();
  const [search, setSearch] = useSearchParams();
  const mayView = useCan('timetable.session.get_timetable');
  const mayRooms = useCan('facility.room.read');
  const mayStaff = useCan('hr.staff.read');
  const by: Whose = search.get('by') === 'staff' ? 'staff' : 'room';
  const roomNames = useRoomNames(mayView && mayRooms);
  const staff = useAllRecords(
    'hr.Staff',
    { filter: 'status = "active"' },
    mayView && mayStaff && by === 'staff',
  );
  const staffNames = useStaffNames(
    (staff.data?.items ?? []).map((row) => row.id),
    by === 'staff',
  );
  const subjectNames = useSubjectNames(useCan('academics.subject.read'));
  const today = localDay(new Date(), timeZone);
  const roomId = search.get('room') ?? '';
  const staffId = search.get('staff') ?? '';
  const chosen = by === 'room' ? (roomNames.has(roomId) ? roomId : '') : staffId;
  const go = (changes: Record<string, string>) =>
    setSearch(
      (previous) => {
        const next = new URLSearchParams(previous);
        for (const [key, value] of Object.entries(changes)) {
          if (value) next.set(key, value);
          else next.delete(key);
        }
        return next;
      },
      { replace: true },
    );
  const staffOptions = (staff.data?.items ?? [])
    .map((row) => ({ value: row.id, label: staffNames.get(row.id) ?? String(row.employeeCode ?? row.id) }))
    .sort((a, b) => a.label.localeCompare(b.label));
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.TT04.title')}</h1>
        <p className="page__subtitle">{t('tt.view.intro')}</p>
      </header>
      {!mayView ? (
        <Banner intent="info">{t('tt.my.noAccess')}</Banner>
      ) : (
        <>
          <Card>
            <div className="form">
              <SegmentedControl
                label={t('tt.view.by')}
                options={[
                  { value: 'room', label: t('tt.view.room') },
                  { value: 'staff', label: t('tt.view.staff') },
                ]}
                value={by}
                onChange={(value) => go({ by: value === 'staff' ? 'staff' : '' })}
              />
              {by === 'room' ? (
                mayRooms ? (
                  <SelectField
                    label={t('tt.view.room')}
                    value={chosen}
                    options={[
                      { value: '', label: t('tt.view.chooseRoom') },
                      ...[...roomNames]
                        .map(([value, label]) => ({ value, label }))
                        .sort((a, b) => a.label.localeCompare(b.label)),
                    ]}
                    onChange={(event) => go({ room: event.target.value })}
                  />
                ) : (
                  <Banner intent="info">{t('tt.view.noRooms')}</Banner>
                )
              ) : mayStaff ? (
                <SelectField
                  label={t('tt.view.staff')}
                  value={staffOptions.some((option) => option.value === staffId) ? staffId : ''}
                  options={[{ value: '', label: t('tt.view.chooseStaff') }, ...staffOptions]}
                  onChange={(event) => go({ staff: event.target.value })}
                />
              ) : (
                <Banner intent="info">{t('tt.view.noStaff')}</Banner>
              )}
            </div>
          </Card>
          {chosen ? (
            <WeekView
              subjects={[by === 'room' ? { roomId: chosen } : { staffId: chosen }]}
              week={search.get('week') ?? today}
              today={today}
              onWeek={(day) => go({ week: day })}
              describe={(session) => ({
                what:
                  session.title ||
                  (session.subjectId && subjectNames.get(session.subjectId)) ||
                  t('tt.my.session'),
                // a room's week names what is taught; a teacher's week also says where
                where: by === 'staff' ? (session.roomId && roomNames.get(session.roomId)) || '' : '',
              })}
            />
          ) : null}
        </>
      )}
    </div>
  );
}
