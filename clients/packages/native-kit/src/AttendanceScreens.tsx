import {
  ATTENDANCE_STATUSES,
  isApiError,
  localDay,
  shiftDay,
  useAllRecords,
  useAttendanceMarks,
  useAttendanceRegister,
  useAttendanceSheet,
  useAttendanceSheetStep,
  useAttendanceSummary,
  useCan,
  useClassRoster,
  useI18n,
  useMyStudents,
  useRecordAttendance,
  useStudent,
  useStudentNames,
  type AttendanceMark,
  type AttendanceStatus,
} from '@mysms/core';
import { enumLabel, type MessageKey } from '@mysms/i18n';
import { Banner, Button, Screen, Text, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import { StudentChoice } from './StudentChoice';

/** A sheet's workflow state as the catalogue names it (the same words as the web's sheet list). */
const SHEET_STATUS: Readonly<Record<string, MessageKey>> = {
  draft: 'att.sheet.status.draft',
  in_progress: 'att.sheet.status.inProgress',
  done: 'att.sheet.status.done',
  cancelled: 'att.sheet.status.cancelled',
};
const sheetStatusKey = (status: string | undefined): MessageKey =>
  SHEET_STATUS[status ?? 'draft'] ?? 'enum.common.unknown';

/** The next mark of the one-tap cycle a teacher goes through (ui/03 F3): present → absent → late → excused. */
const nextStatus = (status: AttendanceStatus): AttendanceStatus =>
  ATTENDANCE_STATUSES[
    (ATTENDANCE_STATUSES.indexOf(status) + 1) % ATTENDANCE_STATUSES.length
  ] as AttendanceStatus;

/**
 * ATT-02 on the teacher's phone: the attendance sheets of a day (the server lists those of the teacher's registers
 * and scope), each opening its roster to take attendance (ATT-03).
 */
export function SheetsScreen({
  tablet = false,
  onOpen,
  onBack,
}: {
  tablet?: boolean;
  onOpen: (sheetId: string) => void;
  onBack: () => void;
}) {
  const { t, format, timeZone } = useI18n();
  const theme = useTheme();
  const mayRead = useCan('attendance.attendance_sheet.read');
  const mayRegisters = useCan('attendance.attendance_register.read');
  const [day, setDay] = useState(() => localDay(new Date(), timeZone));
  const sheets = useAllRecords(
    'attendance.AttendanceSheet',
    { filter: `attendanceDate = "${day}"` },
    mayRead,
  );
  const registers = useAllRecords('attendance.AttendanceRegister', {}, mayRead && mayRegisters);
  const registerName = new Map((registers.data?.items ?? []).map((row) => [row.id, String(row.name)]));
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.ATT02.title')}
      </Text>
      {!mayRead ? <Banner>{t('att.native.noAccess')}</Banner> : null}
      <Text>{format.day(day)}</Text>
      <View style={{ flexDirection: 'row', gap: theme.space[3] }}>
        <Button variant="secondary" onPress={() => setDay(shiftDay(day, -1))}>
          {t('att.native.previousDay')}
        </Button>
        <Button variant="secondary" onPress={() => setDay(shiftDay(day, 1))}>
          {t('att.native.nextDay')}
        </Button>
      </View>
      {sheets.isPending && mayRead ? <Text muted>{t('common.state.loading')}</Text> : null}
      {sheets.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void sheets.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {sheets.data?.items.length === 0 ? <Text muted>{t('att.native.noSheets')}</Text> : null}
      {(sheets.data?.items ?? []).map((sheet) => (
        <View key={sheet.id} style={{ marginTop: theme.space[5] }}>
          <Button variant="secondary" onPress={() => onOpen(sheet.id)}>
            {t('att.native.sheet', {
              register: registerName.get(String(sheet.registerId)) ?? t('att.native.register'),
              status: t(sheetStatusKey(typeof sheet.status === 'string' ? sheet.status : undefined)),
            })}
          </Button>
        </View>
      ))}
      <View style={{ marginTop: theme.space[7] }}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}

/**
 * ATT-03 on the teacher's phone: the roster of the sheet's class with one tap per student to move their mark
 * (present → absent → late → excused), saved together (attendance.RecordAttendance under the sheet's etag) and,
 * when the teacher is done, completed in the same call. A draft sheet is started first; a completed one is shown
 * read-only. Marks someone saved from another device meanwhile answer 412 and are re-read, not overwritten.
 */
export function TakeAttendanceScreen({
  sheetId,
  tablet = false,
  onBack,
}: {
  sheetId: string;
  tablet?: boolean;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const mayRecord = useCan('attendance.attendance_sheet.record_attendance');
  const mayStart = useCan('attendance.attendance_sheet.start_attendance_sheet');
  const sheet = useAttendanceSheet(sheetId);
  const data = sheet.data?.data;
  const etag = sheet.data?.etag ?? data?.etag ?? null;
  const register = useAttendanceRegister(data?.registerId ?? null);
  const roster = useClassRoster(register.data?.classSectionId ?? null);
  const saved = useAttendanceMarks(data ? sheetId : null, data?.attendanceDate ?? null);
  const students = roster.data?.items ?? [];
  const names = useStudentNames(students.map((row) => row.studentId));
  const record = useRecordAttendance();
  const step = useAttendanceSheetStep();
  const [changes, setChanges] = useState<Record<string, AttendanceStatus>>({});
  const savedOf = new Map((saved.data?.items ?? []).map((row) => [row.studentId, row]));
  const statusOf = (studentId: string): AttendanceStatus =>
    changes[studentId] ?? savedOf.get(studentId)?.status ?? 'present';
  const editable = mayRecord && data?.status === 'in_progress';
  const failure = record.error ?? step.error;
  const save = (complete: boolean) => {
    const marks: AttendanceMark[] = students.map((row) => {
      const before = savedOf.get(row.studentId);
      const status = statusOf(row.studentId);
      // a reason or remark belongs to the mark it was given with
      return before && before.status === status
        ? {
            studentId: row.studentId,
            status,
            ...(before.absenceReasonId ? { absenceReasonId: before.absenceReasonId } : {}),
            ...(before.remark ? { remark: before.remark } : {}),
          }
        : { studentId: row.studentId, status };
    });
    record.mutate(
      { sheetId, marks, complete, etag },
      {
        onSuccess: () => setChanges({}),
        onError: (error) => {
          if (isApiError(error) && error.status === 412) setChanges({});
        },
      },
    );
  };
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {register.data?.name ?? t('screen.ATT03.title')}
      </Text>
      {sheet.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {sheet.isError ? (
        <Banner intent="danger">
          {isApiError(sheet.error) && sheet.error.status === 404
            ? t('common.state.notAvailable')
            : t('common.error.generic')}
        </Banner>
      ) : null}
      {data ? <Text muted>{t(sheetStatusKey(data.status))}</Text> : null}
      {failure ? (
        <Banner intent="danger">
          {isApiError(failure) && failure.status === 412
            ? t('common.conflict.body')
            : t('common.error.generic')}
        </Banner>
      ) : null}
      {record.isSuccess && !failure ? <Banner intent="success">{t('att.native.saved')}</Banner> : null}
      {data?.status === 'draft' && mayStart ? (
        <Button loading={step.isPending} onPress={() => step.mutate({ sheetId, step: 'start', etag })}>
          {t('att.native.start')}
        </Button>
      ) : null}
      {roster.isPending && register.data ? <Text muted>{t('common.state.loading')}</Text> : null}
      {students.length === 0 && roster.isSuccess ? <Text muted>{t('aca.class.noStudents')}</Text> : null}
      {[...students]
        .sort((a, b) => (a.rollNumber ?? '').localeCompare(b.rollNumber ?? '', undefined, { numeric: true }))
        .map((row) => {
          const status = statusOf(row.studentId);
          const name = names.get(row.studentId) ?? t('aca.class.unnamed');
          const label = t('att.native.mark', {
            roll: row.rollNumber ?? '—',
            name,
            status: enumLabel(t, 'attendance', status),
          });
          return (
            <View key={row.studentId} style={{ marginTop: theme.space[3] }}>
              {editable ? (
                <Button
                  variant={status === 'present' ? 'secondary' : status === 'absent' ? 'danger' : 'primary'}
                  accessibilityHint={t('att.native.markHint')}
                  onPress={() => setChanges((old) => ({ ...old, [row.studentId]: nextStatus(status) }))}
                >
                  {label}
                </Button>
              ) : (
                <Text>{label}</Text>
              )}
            </View>
          );
        })}
      {editable && students.length > 0 ? (
        <View style={{ marginTop: theme.space[7], gap: theme.space[3] }}>
          <Button variant="secondary" loading={record.isPending} onPress={() => save(false)}>
            {t('att.native.save')}
          </Button>
          <Button loading={record.isPending} onPress={() => save(true)}>
            {t('att.native.complete')}
          </Button>
        </View>
      ) : null}
      <View style={{ marginTop: theme.space[5] }}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}

/**
 * ATT-04 on phones: a student's month of attendance — the days with completed attendance and their marks, and the
 * month's percentage (attendance.SummarizeAttendance by date). Whose: the student themself (S), one of the
 * guardian's children (P), or the student a teacher opened (T).
 */
export function StudentMonthScreen({
  studentId: given,
  tablet = false,
  onBack,
}: {
  studentId?: string;
  tablet?: boolean;
  onBack: () => void;
}) {
  const { t, format, timeZone } = useI18n();
  const theme = useTheme();
  const maySummarize = useCan('attendance.attendance_record.summarize_attendance');
  const mine = useMyStudents();
  const [chosen, setChosen] = useState('');
  const studentId = given ?? (chosen || mine.students[0]?.id) ?? null;
  const student = useStudent(maySummarize ? studentId : null);
  const [month, setMonth] = useState(() => localDay(new Date(), timeZone).slice(0, 7));
  const first = `${month}-01`;
  const last = shiftDay(`${shiftDay(first, 31).slice(0, 7)}-01`, -1);
  const institutionId = student.data?.institutionId ?? null;
  const summary = useAttendanceSummary(
    studentId && institutionId
      ? { institutionId, studentId, groupBy: 'date', fromDate: first, toDate: last }
      : null,
  );
  const days = [...(summary.data?.items ?? [])].sort((a, b) => (a.date ?? '').localeCompare(b.date ?? ''));
  const totals = days.reduce(
    (sum, day) => ({
      attended: sum.attended + day.present + day.late,
      all: sum.all + day.present + day.late + day.absent + day.excused,
    }),
    { attended: 0, all: 0 },
  );
  const dayMark = (day: (typeof days)[number]): AttendanceStatus =>
    day.absent > 0 ? 'absent' : day.late > 0 ? 'late' : day.excused > 0 ? 'excused' : 'present';
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.ATT04.title')}
      </Text>
      {!maySummarize ? <Banner>{t('att.summary.noAccess')}</Banner> : null}
      {given ? null : <StudentChoice students={mine.students} value={studentId ?? ''} onChange={setChosen} />}
      {maySummarize && !studentId && !mine.isPending ? <Banner>{t('native.whose.none')}</Banner> : null}
      <Text>{format.month(first)}</Text>
      <View style={{ flexDirection: 'row', gap: theme.space[3] }}>
        <Button variant="secondary" onPress={() => setMonth(shiftDay(first, -1).slice(0, 7))}>
          {t('att.native.previousMonth')}
        </Button>
        <Button variant="secondary" onPress={() => setMonth(shiftDay(last, 1).slice(0, 7))}>
          {t('att.native.nextMonth')}
        </Button>
      </View>
      {student.isError || summary.isError ? (
        <Banner intent="danger">
          {isApiError(student.error) && student.error.status === 404
            ? t('att.student.unavailable')
            : t('common.error.generic')}
        </Banner>
      ) : null}
      {summary.isPending && institutionId ? <Text muted>{t('common.state.loading')}</Text> : null}
      {summary.isSuccess ? (
        <Text style={{ fontWeight: '600' }}>
          {totals.all === 0
            ? t('att.native.noMarks')
            : t('att.native.percent', {
                percent: format.number(Math.round((totals.attended * 1000) / totals.all) / 10),
              })}
        </Text>
      ) : null}
      {days.map((day) => (
        <Text key={day.date}>
          {`${format.day(day.date ?? first)} · ${enumLabel(t, 'attendance', dayMark(day))}`}
        </Text>
      ))}
      <View style={{ marginTop: theme.space[7] }}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}
