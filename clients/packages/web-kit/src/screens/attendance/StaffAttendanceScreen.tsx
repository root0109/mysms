import {
  isTimetableDay,
  localDay,
  rangeFilter,
  resourceDef,
  useAllRecords,
  useCan,
  useCreateRecord,
  useI18n,
  useStaffNames,
  useUpdateRecord,
  type RecordRow,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, SelectField, Skeleton, StatusChip, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { problemText } from '../recordText';

const DAYS = resourceDef('attendance.StaffAttendance');
const STATUSES = ['present', 'absent', 'half_day', 'late', 'on_leave', 'holiday'] as const;

/**
 * One staff member's line of the day: their status and remark, saved on their own — a new day record, or an update
 * of the existing one under its etag. A record from the biometric device or an import is the device's: shown, not
 * edited.
 */
function StaffDay({
  staffId,
  name,
  day,
  saved,
  mayCreate,
  mayUpdate,
}: {
  staffId: string;
  name: string;
  day: string;
  saved: RecordRow | undefined;
  mayCreate: boolean;
  mayUpdate: boolean;
}) {
  const { t, format } = useI18n();
  const days = DAYS as NonNullable<typeof DAYS>;
  const create = useCreateRecord(days);
  const update = useUpdateRecord(days);
  const [status, setStatus] = useState(String(saved?.status ?? ''));
  const [remark, setRemark] = useState(String(saved?.remark ?? ''));
  const fromDevice = saved !== undefined && saved.source !== undefined && saved.source !== 'manual';
  const editable = !fromDevice && (saved ? mayUpdate : mayCreate);
  const changed = status !== String(saved?.status ?? '') || remark !== String(saved?.remark ?? '');
  const error = create.error ?? update.error;
  const save = () => {
    const values = { status, remark };
    if (saved?.etag) update.mutate({ id: saved.id, etag: saved.etag, values });
    else create.mutate({ values: { ...values, staffId, attendanceDate: day, source: 'manual' } });
  };
  return (
    <li className="attendance-grid__row">
      <div className="attendance-grid__who">
        <span className="attendance-grid__name">{name}</span>
        {typeof saved?.checkInAt === 'string' ? (
          <span className="attendance-grid__note">
            {t('att.staff.inOut', {
              in: format.time(saved.checkInAt),
              out: typeof saved.checkOutAt === 'string' ? format.time(saved.checkOutAt) : '—',
            })}
          </span>
        ) : null}
        {fromDevice ? <StatusChip intent="info" label={t('att.staff.fromDevice')} /> : null}
      </div>
      <SelectField
        label={t('att.staff.statusOf', { name })}
        value={status}
        disabled={!editable}
        options={[
          { value: '', label: t('att.staff.notMarked') },
          ...STATUSES.map((value) => ({ value, label: enumLabel(t, 'attendance.staffStatus', value) })),
        ]}
        onChange={(event) => setStatus(event.target.value)}
      />
      <TextInput
        label={t('att.staff.remarkOf', { name })}
        value={remark}
        maxLength={500}
        disabled={!editable}
        onChange={(event) => setRemark(event.target.value)}
      />
      {editable ? (
        <Button
          variant="secondary"
          disabled={!changed || status === ''}
          loading={create.isPending || update.isPending}
          aria-label={t('att.staff.saveOf', { name })}
          onClick={save}
        >
          {t('common.action.save')}
        </Button>
      ) : null}
      {error ? <span className="attendance-grid__pending">{problemText(t, error)}</span> : null}
    </li>
  );
}

/**
 * ATT-06 Staff attendance: one day as a grid of the active staff — each one's status (present, absent, half day,
 * late, on leave, holiday), check-in and check-out and a remark — marked and saved line by line. The day is in the
 * address (`?day=`); the day's records are read by their partition range, and records from a biometric device stay
 * read-only.
 */
export function StaffAttendanceScreen() {
  const { t, timeZone } = useI18n();
  const [search, setSearch] = useSearchParams();
  const mayRead = useCan('attendance.staff_attendance.read');
  const mayStaff = useCan('hr.staff.read');
  const mayCreate = useCan('attendance.staff_attendance.create');
  const mayUpdate = useCan('attendance.staff_attendance.update');
  const asked = search.get('day') ?? '';
  const day = isTimetableDay(asked) ? asked : localDay(new Date(), timeZone);
  const staff = useAllRecords('hr.Staff', { filter: 'status = "active"' }, mayRead && mayStaff);
  const records = useAllRecords(
    'attendance.StaffAttendance',
    { filter: DAYS ? rangeFilter(DAYS, day, day) : undefined },
    mayRead,
  );
  const names = useStaffNames(
    (staff.data?.items ?? []).map((row) => row.id),
    mayStaff,
  );
  const savedOf = new Map((records.data?.items ?? []).map((row) => [String(row.staffId), row]));
  const lines = (staff.data?.items ?? [])
    .map((row) => ({
      id: row.id,
      name: names.get(row.id) ?? String(row.employeeCode ?? t('aca.class.unnamedTeacher')),
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
  const marked = lines.filter((line) => savedOf.has(line.id)).length;
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ATT06.title')}</h1>
        <p className="page__subtitle">{t('att.staff.intro')}</p>
      </header>
      {!mayRead || !mayStaff ? (
        <Banner intent="info">{t('att.staff.noAccess')}</Banner>
      ) : (
        <Card title={t('att.staff.marked', { marked, total: lines.length })}>
          <TextInput
            label={t('att.staff.day')}
            type="date"
            value={day}
            onChange={(event) =>
              isTimetableDay(event.target.value) && setSearch({ day: event.target.value }, { replace: true })
            }
          />
          {staff.isPending || records.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={6} />
          ) : staff.isError || records.isError ? (
            <Banner
              intent="danger"
              action={
                <Button
                  variant="secondary"
                  onClick={() => {
                    void staff.refetch();
                    void records.refetch();
                  }}
                >
                  {t('common.action.retry')}
                </Button>
              }
            >
              {problemText(t, staff.error ?? records.error)}
            </Banner>
          ) : lines.length === 0 ? (
            <p className="hint">{t('att.staff.noStaff')}</p>
          ) : (
            <ul className="attendance-grid" aria-label={t('att.staff.grid', { day })}>
              {lines.map((line) => {
                const saved = savedOf.get(line.id);
                return (
                  <StaffDay
                    key={`${line.id}:${day}:${String(saved?.etag ?? '')}`}
                    staffId={line.id}
                    name={line.name}
                    day={day}
                    saved={saved}
                    mayCreate={mayCreate}
                    mayUpdate={mayUpdate}
                  />
                );
              })}
            </ul>
          )}
        </Card>
      )}
    </div>
  );
}
