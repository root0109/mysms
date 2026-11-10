import {
  ATTENDANCE_STATUSES,
  isApiError,
  useAbsenceReasons,
  useAttendanceMarks,
  useAttendanceRegister,
  useAttendanceSheet,
  useAttendanceSheetStep,
  useCan,
  useClassRoster,
  useI18n,
  useRecordAttendance,
  useStudentNames,
  type AttendanceMark,
  type AttendanceRecord,
  type AttendanceSheet,
  type AttendanceStatus,
} from '@mysms/core';
import { enumLabel, type Translate } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  SegmentedControl,
  SelectField,
  Skeleton,
  StatusChip,
  TextInput,
} from '@mysms/ui-web';
import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router';
import { UnsavedChangesGuard } from '../../routing/UnsavedChangesGuard';
import { UUID } from '../ids';
import { SHEET_STATUS, STATUS_INTENT, takesReason } from './status';

/** The contract's limits (attendance RecordAttendanceRequest / AttendanceMark). */
const MAX_MARKS = 500;
const MAX_REMARK = 255;

/** What the teacher has chosen for one student, saved or not. */
interface Mark {
  status: AttendanceStatus;
  absenceReasonId: string;
  remark: string;
}

/**
 * An unsaved choice, with the saved mark it was made against. Once the saved mark is another one — the choice was
 * saved, or the mark was changed elsewhere — the edit is spent and the saved mark shows again.
 */
interface Edit {
  mark: Mark;
  base: Mark | undefined;
}

interface Row {
  studentId: string;
  rollNumber: string | undefined;
  saved: AttendanceRecord | undefined;
}

const savedMark = (record: AttendanceRecord): Mark => ({
  status: record.status,
  absenceReasonId: record.absenceReasonId ?? '',
  remark: record.remark ?? '',
});

/** Two marks are the same when the server would store the same thing for both (it trims the remark). */
const sameMark = (a: Mark | undefined, b: Mark | undefined): boolean =>
  a === undefined || b === undefined
    ? a === b
    : a.status === b.status && a.absenceReasonId === b.absenceReasonId && a.remark.trim() === b.remark.trim();

const savedOf = (row: Row): Mark | undefined => (row.saved ? savedMark(row.saved) : undefined);

/** A mark as the API takes it: the saved attendance type is kept only while the status it stands for is. */
function toApiMark(row: Row, mark: Mark): AttendanceMark {
  const remark = mark.remark.trim();
  return {
    studentId: row.studentId,
    status: mark.status,
    ...(row.saved?.attendanceTypeId && row.saved.status === mark.status
      ? { attendanceTypeId: row.saved.attendanceTypeId }
      : {}),
    ...(mark.absenceReasonId && takesReason(mark.status) ? { absenceReasonId: mark.absenceReasonId } : {}),
    ...(remark ? { remark } : {}),
  };
}

/** A rejected save: the server's field errors (`marks[3].remark`) placed on the students that were sent. */
function rejectedMarks(error: unknown, sent: readonly string[]): ReadonlyMap<string, string> {
  const out = new Map<string, string>();
  if (!isApiError(error) || error.status !== 422) return out;
  for (const fieldError of error.errors) {
    const index = /^marks\[(\d+)\]/.exec(fieldError.field)?.[1];
    const studentId = index === undefined ? undefined : sent[Number(index)];
    if (studentId && !out.has(studentId)) out.set(studentId, fieldError.message);
  }
  return out;
}

function failureText(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422) return t('att.take.invalid');
    if (error.status === 403) return t('att.take.forbidden');
  }
  return t('common.error.generic');
}

/**
 * ATT-03 Take attendance (flow F3): the students expected on one sheet, each marked with one tap, a reason and a
 * remark where they apply. Marks are saved together and all-or-nothing; completing the sheet locks it and makes it
 * count in the summaries. The server decides who may see the sheet and its students; the screen only offers the
 * steps the role holds.
 */
export function AttendanceSheetScreen() {
  const { t } = useI18n();
  const { attendanceSheetId = '' } = useParams();
  const mayRead = useCan('attendance.attendance_sheet.read');
  const sheetId = mayRead && UUID.test(attendanceSheetId) ? attendanceSheetId : null;
  const sheet = useAttendanceSheet(sheetId);
  const title = t('screen.ATT03.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('att.take.noAccess')}</p>
      </Card>
    );
  if (sheetId === null || (isApiError(sheet.error) && sheet.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
        <Link to="/app/attendance/attendance-sheets">{t('att.take.back')}</Link>
      </Card>
    );
  if (sheet.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (sheet.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void sheet.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return (
    <SheetGrid
      sheetId={sheetId}
      sheet={sheet.data.data}
      etag={sheet.data.etag}
      latestEtag={async () => (await sheet.refetch()).data?.etag ?? null}
    />
  );
}

function SheetGrid({
  sheetId,
  sheet,
  etag,
  latestEtag,
}: {
  sheetId: string;
  sheet: AttendanceSheet;
  etag: string | null;
  /** Re-reads the sheet for its current etag (a save in several requests needs the one each request left). */
  latestEtag: () => Promise<string | null>;
}) {
  const { t, format } = useI18n();
  const mayRecord = useCan('attendance.attendance_sheet.record_attendance');
  const mayComplete = useCan('attendance.attendance_sheet.complete_attendance_sheet');
  const mayStart = useCan('attendance.attendance_sheet.start_attendance_sheet');
  const mayReopen = useCan('attendance.attendance_sheet.reset_attendance_sheet');
  const mayReadMarks = useCan('attendance.attendance_record.read');
  const mayReadRegister = useCan('attendance.attendance_register.read');
  const mayReadRoster = useCan('sis.student_enrollment.list_class_roster');
  const mayReadReasons = useCan('attendance.absence_reason.read');

  const register = useAttendanceRegister(mayReadRegister ? sheet.registerId : null);
  const marks = useAttendanceMarks(mayReadMarks ? sheetId : null, sheet.attendanceDate);
  const classSectionId = register.data?.classSectionId ?? null;
  const roster = useClassRoster(mayReadRoster ? classSectionId : null);
  const reasons = useAbsenceReasons(mayReadReasons);

  const [draft, setDraft] = useState<ReadonlyMap<string, Edit>>(new Map());
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<{ error: unknown; sent: readonly string[] } | null>(null);
  const [saving, setSaving] = useState(false);
  const record = useRecordAttendance();
  const step = useAttendanceSheetStep();

  /**
   * Who is on the sheet. A sheet opened for a timetable session already holds a mark for every student the session
   * expects (an elective group is not the whole class), so its marks are its students. A sheet created by hand
   * expects the class as it was placed on the sheet's date, plus anyone already marked. The roster also gives
   * the roll numbers, which order the grid.
   */
  const forSession = Boolean(sheet.timetableSessionId);
  const day = sheet.attendanceDate;
  const rows = useMemo<Row[]>(() => {
    const saved = new Map((marks.data?.items ?? []).map((item) => [item.studentId, item]));
    const placed = (roster.data?.items ?? []).filter(
      (entry) =>
        (!entry.effectiveFrom || entry.effectiveFrom <= day) &&
        (!entry.effectiveTo || entry.effectiveTo >= day),
    );
    const rollOf = new Map(placed.map((entry) => [entry.studentId, entry.rollNumber]));
    const students = new Set([
      ...(forSession ? [] : placed.map((entry) => entry.studentId)),
      ...saved.keys(),
    ]);
    const collator = new Intl.Collator(undefined, { numeric: true });
    return [...students]
      .map((studentId) => ({ studentId, rollNumber: rollOf.get(studentId), saved: saved.get(studentId) }))
      .sort((a, b) =>
        a.rollNumber && b.rollNumber
          ? collator.compare(a.rollNumber, b.rollNumber)
          : Number(!a.rollNumber) - Number(!b.rollNumber),
      );
  }, [marks.data, roster.data, forSession, day]);
  const names = useStudentNames(rows.map((row) => row.studentId));

  const status = sheet.status ?? 'draft';
  // marking needs the marks already there: a role that cannot read them would overwrite them unseen
  const editable = status === 'in_progress' && mayRecord && mayReadMarks;
  /** The edit of a row while it still applies: made against the mark that is saved now. */
  const editOf = (row: Row, edits: ReadonlyMap<string, Edit> = draft): Edit | undefined => {
    const edit = edits.get(row.studentId);
    return edit && sameMark(edit.base, savedOf(row)) ? edit : undefined;
  };
  const markOf = (row: Row): Mark | undefined => editOf(row)?.mark ?? savedOf(row);
  const dirty = rows.filter((row) => {
    const edit = editOf(row);
    return edit !== undefined && !sameMark(edit.mark, savedOf(row));
  });
  /** The edits that still apply; spent ones are dropped whenever the teacher edits again. */
  const liveEdits = (edits: ReadonlyMap<string, Edit>): Map<string, Edit> =>
    new Map(
      rows.flatMap((row) => {
        const edit = editOf(row, edits);
        return edit ? [[row.studentId, edit] as const] : [];
      }),
    );
  const unmarked = rows.filter((row) => markOf(row) === undefined).length;
  const counts = { present: 0, absent: 0, late: 0, excused: 0 };
  for (const row of rows) {
    const mark = markOf(row);
    if (mark) counts[mark.status] += 1;
  }
  const rejected = failure ? rejectedMarks(failure.error, failure.sent) : new Map<string, string>();
  const busy = saving || step.isPending;

  const change = (row: Row, next: Mark) => {
    setNotice(null);
    setDraft((current) => liveEdits(current).set(row.studentId, { mark: next, base: savedOf(row) }));
  };
  const choose = (row: Row, chosen: AttendanceStatus) => {
    const current = markOf(row);
    change(row, {
      status: chosen,
      absenceReasonId: takesReason(chosen) ? (current?.absenceReasonId ?? '') : '',
      remark: current?.remark ?? '',
    });
  };
  const markRestPresent = () => {
    setNotice(null);
    setDraft((current) => {
      const updated = liveEdits(current);
      for (const row of rows)
        if (!row.saved && !updated.has(row.studentId))
          updated.set(row.studentId, {
            mark: { status: 'present', absenceReasonId: '', remark: '' },
            base: undefined,
          });
      return updated;
    });
  };

  /**
   * Saves the changed marks in requests of at most 500, each under the sheet's etag so marks changed on another
   * device are never overwritten unseen (412); `complete` rides on the last request, so it is one step.
   */
  const save = async (complete: boolean) => {
    setNotice(null);
    setFailure(null);
    setSaving(true);
    const changed = dirty.map((row) => toApiMark(row, markOf(row) as Mark));
    let sent: string[] = [];
    try {
      for (let start = 0; start < changed.length; start += MAX_MARKS) {
        const chunk = changed.slice(start, start + MAX_MARKS);
        sent = chunk.map((mark) => mark.studentId);
        const last = start + MAX_MARKS >= changed.length;
        // once saved, a mark is no longer the one its edit was made against: the edit is spent
        await record.mutateAsync({
          sheetId,
          marks: chunk,
          complete: complete && last,
          etag: start === 0 ? etag : await latestEtag(),
        });
      }
      setNotice(t(complete ? 'att.take.completed' : 'att.take.saved'));
    } catch (error) {
      setFailure({ error, sent });
    } finally {
      setSaving(false);
    }
  };
  const run = (kind: 'start' | 'complete' | 'reset', done: string) => {
    setNotice(null);
    setFailure(null);
    step.mutate(
      { sheetId, step: kind, etag },
      { onSuccess: () => setNotice(done), onError: (error) => setFailure({ error, sent: [] }) },
    );
  };

  const options = ATTENDANCE_STATUSES.map((value) => ({
    value,
    label: enumLabel(t, 'attendance', value),
    intent: STATUS_INTENT[value],
  }));
  const reasonOptions = [
    { value: '', label: t('att.take.noReason') },
    ...(reasons.data?.items ?? []).flatMap((reason) =>
      reason.id ? [{ value: reason.id, label: reason.title }] : [],
    ),
  ];
  const sheetStatus = SHEET_STATUS[status];
  const loadingRows =
    (mayReadMarks && marks.isPending) || (mayReadRoster && classSectionId !== null && roster.isPending);
  const rowsFailed = marks.isError || roster.isError;
  const canComplete = editable && mayComplete && rows.length > 0 && unmarked === 0;

  return (
    <div className="page">
      <UnsavedChangesGuard when={dirty.length > 0} />
      <header className="page__header">
        <h1 className="page__title">{t('screen.ATT03.title')}</h1>
        <p className="page__subtitle">
          {t('att.take.subtitle', {
            register: register.data?.name ?? sheet.reference ?? t('screen.ATT02.title'),
            date: format.day(sheet.attendanceDate),
          })}{' '}
          <StatusChip
            intent={sheetStatus?.intent}
            label={sheetStatus ? t(sheetStatus.label) : t('enum.common.unknown')}
          />
        </p>
        <Link to="/app/attendance/attendance-sheets">{t('att.take.back')}</Link>
      </header>

      {status === 'draft' ? (
        <Banner
          intent="info"
          action={
            mayStart ? (
              <Button loading={step.isPending} onClick={() => run('start', t('att.take.started'))}>
                {t('att.take.start')}
              </Button>
            ) : undefined
          }
        >
          {t('att.take.draft')}
        </Banner>
      ) : null}
      {status === 'done' || status === 'cancelled' ? (
        <Banner
          intent={status === 'done' ? 'success' : 'warning'}
          action={
            mayReopen ? (
              <Button
                variant="secondary"
                loading={step.isPending}
                onClick={() => run('reset', t('att.take.reopened'))}
              >
                {t('att.take.reopen')}
              </Button>
            ) : undefined
          }
        >
          {t(status === 'done' ? 'att.take.done' : 'att.take.cancelled')}
        </Banner>
      ) : null}
      {status === 'in_progress' && !editable ? <Banner intent="info">{t('att.take.readOnly')}</Banner> : null}
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {failure ? (
        <Banner
          intent="danger"
          action={
            isApiError(failure.error) && failure.error.isConflict ? (
              <Button variant="secondary" onClick={() => setFailure(null)}>
                {t('common.action.close')}
              </Button>
            ) : undefined
          }
        >
          {failureText(t, failure.error)}
        </Banner>
      ) : null}

      <Card
        title={t('att.take.students', { count: rows.length })}
        actions={
          editable && unmarked > 0 ? (
            <Button variant="secondary" disabled={busy} onClick={markRestPresent}>
              {t('att.take.allPresent')}
            </Button>
          ) : null
        }
      >
        {loadingRows ? (
          <Skeleton label={t('common.state.loading')} lines={6} />
        ) : rowsFailed ? (
          <Banner
            intent="danger"
            action={
              <Button
                variant="secondary"
                onClick={() => {
                  void marks.refetch();
                  void roster.refetch();
                }}
              >
                {t('common.action.retry')}
              </Button>
            }
          >
            {t('common.error.generic')}
          </Banner>
        ) : rows.length === 0 ? (
          <p>{t('att.take.empty')}</p>
        ) : (
          <>
            <p className="hint" role="status">
              {t('att.take.counts', counts)}
              {unmarked > 0 ? ` · ${t('att.take.unmarked', { count: unmarked })}` : ''}
            </p>
            <ol className="attendance-grid">
              {rows.map((row, index) => {
                const mark = markOf(row);
                const name = names.get(row.studentId) ?? t('att.take.student', { number: index + 1 });
                const error = rejected.get(row.studentId);
                return (
                  <li key={row.studentId} className="attendance-grid__row">
                    <div className="attendance-grid__who">
                      <span className="attendance-grid__name">{name}</span>
                      {row.rollNumber ? (
                        <span className="attendance-grid__roll">
                          {t('att.take.roll', { roll: row.rollNumber })}
                        </span>
                      ) : null}
                    </div>
                    {editable ? (
                      <SegmentedControl
                        label={name}
                        options={options}
                        value={mark?.status}
                        disabled={busy}
                        onChange={(chosen) => choose(row, chosen)}
                      />
                    ) : (
                      <StatusChip
                        intent={mark ? STATUS_INTENT[mark.status] : 'neutral'}
                        label={mark ? enumLabel(t, 'attendance', mark.status) : t('att.take.notMarked')}
                      />
                    )}
                    {editable && mark ? (
                      <div className="attendance-grid__detail">
                        {takesReason(mark.status) && reasonOptions.length > 1 ? (
                          <SelectField
                            label={t('att.take.reason')}
                            options={reasonOptions}
                            value={mark.absenceReasonId}
                            disabled={busy}
                            onChange={(event) =>
                              change(row, { ...mark, absenceReasonId: event.target.value })
                            }
                          />
                        ) : null}
                        <TextInput
                          label={t('att.take.remark')}
                          value={mark.remark}
                          maxLength={MAX_REMARK}
                          disabled={busy}
                          onChange={(event) => change(row, { ...mark, remark: event.target.value })}
                        />
                      </div>
                    ) : null}
                    {!editable && mark && (mark.remark || mark.absenceReasonId) ? (
                      <p className="attendance-grid__note">
                        {[
                          reasonOptions.find(
                            (option) => option.value && option.value === mark.absenceReasonId,
                          )?.label,
                          mark.remark,
                        ]
                          .filter(Boolean)
                          .join(' · ')}
                      </p>
                    ) : null}
                    {error ? (
                      <p className="sms-field__error" role="alert">
                        {error}
                      </p>
                    ) : null}
                  </li>
                );
              })}
            </ol>
          </>
        )}
        {editable ? (
          <div className="page__actions">
            {dirty.length > 0 ? (
              <span className="attendance-grid__pending" role="status">
                {t('att.take.unsaved', { count: dirty.length })}
              </span>
            ) : null}
            <Button
              variant="secondary"
              loading={saving}
              disabled={dirty.length === 0 || step.isPending}
              onClick={() => void save(false)}
            >
              {t('att.take.save')}
            </Button>
            {mayComplete ? (
              <Button
                loading={busy}
                disabled={!canComplete}
                onClick={() =>
                  dirty.length > 0 ? void save(true) : run('complete', t('att.take.completed'))
                }
              >
                {t(dirty.length > 0 ? 'att.take.complete' : 'att.take.completeOnly')}
              </Button>
            ) : null}
          </div>
        ) : null}
      </Card>
    </div>
  );
}
