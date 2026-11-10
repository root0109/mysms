import { useMutation, useQueries, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { attendance, sis } from '@mysms/sdk';
import { ClassSectionApi } from '@mysms/sdk/academics/apis/ClassSectionApi';
import { GradeLevelApi } from '@mysms/sdk/academics/apis/GradeLevelApi';
import { SectionApi } from '@mysms/sdk/academics/apis/SectionApi';
import { AbsenceReasonApi } from '@mysms/sdk/attendance/apis/AbsenceReasonApi';
import { AttendanceRegisterApi } from '@mysms/sdk/attendance/apis/AttendanceRegisterApi';
import { AttendanceSheetApi } from '@mysms/sdk/attendance/apis/AttendanceSheetApi';
import { InstitutionApi } from '@mysms/sdk/org/apis/InstitutionApi';
import { PersonApi } from '@mysms/sdk/people/apis/PersonApi';
import { StudentApi } from '@mysms/sdk/sis/apis/StudentApi';
import { StudentEnrollmentApi } from '@mysms/sdk/sis/apis/StudentEnrollmentApi';
import { allPages, pageQuery, type PageParam } from '../api/pages';
import { call, callWithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type AttendanceSheet = attendance.AttendanceSheet;
export type AttendanceRecord = attendance.AttendanceRecord;
export type AttendanceRegister = attendance.AttendanceRegister;
export type AttendanceMark = attendance.AttendanceMark;
export type AttendanceSummary = attendance.AttendanceSummary;
export type AbsenceReason = attendance.AbsenceReason;
export type AttendanceStatus = AttendanceMark['status'];
export type AttendanceGrouping = 'student' | 'class_section' | 'date';
export type RosterEntry = sis.StudentEnrollment;

/** The four normalised marks, in the order a teacher cycles through them (ui/03 F3). */
export const ATTENDANCE_STATUSES = ['present', 'absent', 'late', 'excused'] as const;

/** The day after an ISO date (`2026-03-31` → `2026-04-01`), for the half-open range of a partitioned list. */
export function nextDay(date: string): string {
  const next = new Date(`${date}T00:00:00Z`);
  next.setUTCDate(next.getUTCDate() + 1);
  return next.toISOString().slice(0, 10);
}

const SHEET = (sheetId: string | null) => queryKey('attendance', 'sheet', sheetId);
const MARKS = (sheetId: string | null) => queryKey('attendance', 'marks', sheetId);

/** Re-reads a sheet and its marks after a change; summaries count completed sheets, so they go stale with it. */
async function refreshSheet(qc: QueryClient, sheetId: string): Promise<void> {
  void qc.invalidateQueries({ queryKey: queryKey('attendance', 'summary') });
  await Promise.all([
    qc.invalidateQueries({ queryKey: SHEET(sheetId) }),
    qc.invalidateQueries({ queryKey: MARKS(sheetId) }),
  ]);
}

/** attendance.GetAttendanceSheet with its ETag, which completes the sheet under optimistic locking. */
export function useAttendanceSheet(sheetId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: SHEET(sheetId),
    queryFn: () =>
      callWithEtag(
        api.get(AttendanceSheetApi).getAttendanceSheetRaw({ attendanceSheetId: sheetId as string }),
      ),
    enabled: sheetId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** attendance.GetAttendanceRegister: what the sheet's register covers (its class section gives the roster). */
export function useAttendanceRegister(registerId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('attendance', 'register', registerId),
    queryFn: () =>
      call(
        api.get(AttendanceRegisterApi).getAttendanceRegister({ attendanceRegisterId: registerId as string }),
      ),
    enabled: registerId !== null,
    staleTime: STALE_TIME.reference,
  });
}

/**
 * Every mark of a sheet (attendance.ListAttendanceRecords). The list is partitioned by date, so it is read with the
 * sheet's own day as its range.
 */
export function useAttendanceMarks(sheetId: string | null, attendanceDate: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: MARKS(sheetId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(AttendanceSheetApi).listAttendanceRecords({
            attendanceSheetId: sheetId as string,
            size: 500,
            filter: `attendanceDate >= "${attendanceDate as string}" AND attendanceDate < "${nextDay(attendanceDate as string)}"`,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: sheetId !== null && attendanceDate !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** attendance.ListAbsenceReasons: the active reasons, a pick-list cached like reference data. */
export function useAbsenceReasons(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('attendance', 'absence-reasons'),
    queryFn: async () => {
      const all = await allPages((page) =>
        call(api.get(AbsenceReasonApi).listAbsenceReasons({ size: 500, ...pageQuery(page) })),
      );
      return { items: all.items.filter((reason) => reason.isActive !== false) };
    },
    enabled,
    staleTime: STALE_TIME.reference,
  });
}

/** sis.ListClassRoster: the students placed in a class section now. */
export function useClassRoster(classSectionId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('sis', 'class-roster', classSectionId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StudentEnrollmentApi).listClassRoster({
            classSectionId: classSectionId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: classSectionId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** Ids per lookup request: the list filter takes 50 terms and 2,000 characters; 40 ids stay inside both. */
export const NAME_CHUNK = 40;

/** A filter that matches any of the ids. */
export const anyOf = (ids: readonly string[]) => ids.map((id) => `id = "${id}"`).join(' OR ');

/**
 * The display names of students, by student id. A student's name lives with their person record in another module,
 * so names are read forty students at a time with two list reads (sis.ListStudents, people.ListPersons), both
 * answered inside the caller's own scope. Ids are grouped in the order given, so a list that grows at its end
 * reuses what it already read. A student whose name the caller may not read simply has none; screens fall back to
 * the roll number or a position.
 */
export function useStudentNames(studentIds: readonly string[]): ReadonlyMap<string, string> {
  const { api } = useRuntime();
  const ids = [...new Set(studentIds)];
  const chunks: string[][] = [];
  for (let start = 0; start < ids.length; start += NAME_CHUNK)
    chunks.push(ids.slice(start, start + NAME_CHUNK));
  return useQueries({
    queries: chunks.map((chunk) => ({
      queryKey: queryKey('sis', 'student-names', chunk),
      queryFn: async (): Promise<[string, string][]> => {
        const students = await call(
          api.get(StudentApi).listStudents({ size: NAME_CHUNK, filter: anyOf(chunk) }),
        );
        const personOf = new Map<string, string>();
        for (const student of students.data) if (student.id) personOf.set(student.id, student.personId);
        const personIds = [...new Set(personOf.values())];
        if (personIds.length === 0) return [];
        const persons = await call(
          api.get(PersonApi).listPersons({ size: NAME_CHUNK, filter: anyOf(personIds) }),
        );
        const nameOf = new Map<string, string>();
        for (const person of persons.data) if (person.id) nameOf.set(person.id, person.displayName);
        return [...personOf].flatMap(([studentId, personId]) => {
          const name = nameOf.get(personId);
          return name ? [[studentId, name] as [string, string]] : [];
        });
      },
      staleTime: STALE_TIME.reference,
      retry: false,
    })),
    combine: (results) => new Map(results.flatMap((result) => result.data ?? [])),
  });
}

/**
 * attendance.RecordAttendance: one mark per student, optionally completing the sheet in the same call. The etag is
 * the sheet's as the screen last read it: marks saved from another device since then answer 412 instead of being
 * overwritten unseen.
 */
export function useRecordAttendance() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      sheetId,
      marks,
      complete,
      etag,
    }: {
      sheetId: string;
      marks: AttendanceMark[];
      complete?: boolean;
      etag?: string | null;
    }) =>
      call(
        api.get(AttendanceSheetApi).recordAttendance({
          attendanceSheetId: sheetId,
          recordAttendanceRequest: {
            marks,
            ...(complete ? { complete: true } : {}),
            ...(etag ? { etag } : {}),
          },
        }),
      ),
    // awaited: when the mutation settles the cache already holds what the server now has
    onSettled: (_result, _error, variables) => refreshSheet(qc, variables.sheetId),
  });
}

/**
 * attendance.TakeAttendanceForSession: the sheet of a timetable session — the one already opened, or a new sheet in
 * progress with every expected student marked present.
 */
export function useTakeAttendanceForSession() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (timetableSessionId: string) =>
      call(
        api
          .get(AttendanceSheetApi)
          .takeAttendanceForSession({ takeAttendanceForSessionRequest: { timetableSessionId } }),
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKey('records', 'attendance.AttendanceSheet') }),
  });
}

type SheetStep = 'start' | 'complete' | 'reset';

/**
 * The sheet's workflow steps: start (draft → in progress), complete (in progress → done) and reset (cancelled or
 * done → draft, to reopen a sheet). Each sends the etag the screen last read, so a sheet changed elsewhere answers
 * 412 instead of being moved on unseen.
 */
export function useAttendanceSheetStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ sheetId, step, etag }: { sheetId: string; step: SheetStep; etag: string | null }) => {
      const sheets = api.get(AttendanceSheetApi);
      const body = etag ? { etag } : {};
      if (step === 'start')
        return call(
          sheets.startAttendanceSheet({ attendanceSheetId: sheetId, startAttendanceSheetRequest: body }),
        );
      if (step === 'complete')
        return call(
          sheets.completeAttendanceSheet({
            attendanceSheetId: sheetId,
            completeAttendanceSheetRequest: body,
          }),
        );
      return call(
        sheets.resetAttendanceSheet({ attendanceSheetId: sheetId, resetAttendanceSheetRequest: body }),
      );
    },
    onSettled: (_result, _error, variables) => refreshSheet(qc, variables.sheetId),
  });
}

export interface AttendanceSummaryParams {
  institutionId: string;
  fromDate: string;
  toDate: string;
  groupBy: AttendanceGrouping;
  studentId?: string;
  classSectionId?: string;
}

/** attendance.SummarizeAttendance: every group of the range (a class's students, a month's days). */
export function useAttendanceSummary(params: AttendanceSummaryParams | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('attendance', 'summary', params),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(AttendanceSheetApi).summarizeAttendance({
            ...(params as AttendanceSummaryParams),
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: params !== null,
    staleTime: STALE_TIME.list,
  });
}

/** sis.GetStudent: the student a summary is about (their institution scopes the summary). */
export function useStudent(studentId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('sis', 'student', studentId),
    queryFn: () => call(api.get(StudentApi).getStudent({ studentId: studentId as string })),
    enabled: studentId !== null,
    staleTime: STALE_TIME.reference,
  });
}

/** org.ListInstitutions: the institutions the caller may see, for screens that work on one of them. */
export function useInstitutions(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('org', 'institutions', { all: true }),
    queryFn: () =>
      allPages((page) => call(api.get(InstitutionApi).listInstitutions({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.reference,
  });
}

export interface ClassSectionOption {
  id: string;
  /** Grade and section as the school names them, e.g. `Grade 5 A` (with the stream when the class has one). */
  label: string;
}

/**
 * The active classes of an institution with readable names. A class is a grade and a section, each named in its own
 * catalogue, so the options take three list reads (academics.ListClassSections, ListGradeLevels, ListSections).
 */
export function useClassSectionOptions(institutionId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('academics', 'class-section-options', institutionId),
    queryFn: async (): Promise<ClassSectionOption[]> => {
      const filter = `institutionId = "${institutionId as string}"`;
      const read = (page: PageParam) => ({ size: 500, filter, ...pageQuery(page) });
      const [classes, grades, sections] = await Promise.all([
        allPages((page) =>
          call(
            api
              .get(ClassSectionApi)
              .listClassSections({ ...read(page), filter: `${filter} AND isActive = true` }),
          ),
        ),
        allPages((page) => call(api.get(GradeLevelApi).listGradeLevels(read(page)))),
        allPages((page) => call(api.get(SectionApi).listSections(read(page)))),
      ]);
      const gradeOf = new Map(grades.items.map((grade) => [grade.id, grade]));
      const sectionOf = new Map(sections.items.map((section) => [section.id, section]));
      return classes.items
        .flatMap((item) => {
          const grade = gradeOf.get(item.gradeLevelId);
          const section = sectionOf.get(item.sectionId);
          if (!item.id || !grade || !section) return [];
          const label = [grade.name, section.name, item.stream].filter(Boolean).join(' ');
          return [{ id: item.id, label, order: grade.sortOrder ?? 0 }];
        })
        .sort((a, b) => a.order - b.order || a.label.localeCompare(b.label))
        .map(({ id, label }) => ({ id, label }));
    },
    enabled: enabled && institutionId !== null,
    staleTime: STALE_TIME.reference,
  });
}
