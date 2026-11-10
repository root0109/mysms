import type { attendance } from '@mysms/sdk';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;

/** One class of three students with a register and a sheet, as the attendance screens meet them. */
export const attendanceFixture = {
  institutionId: id('c1'),
  classSectionId: id('c5'),
  registerId: id('a1'),
  sheetId: id('a2'),
  sessionId: id('a3'),
  date: '2026-03-09',
  students: [
    { id: id('501'), personId: id('601'), name: 'Asha Rao', rollNumber: '2' },
    { id: id('502'), personId: id('602'), name: 'Bilal Khan', rollNumber: '10' },
    { id: id('503'), personId: id('603'), name: 'Chitra Nair', rollNumber: '1' },
  ],
  reasons: [
    { id: id('701'), title: 'Sick', isActive: true },
    { id: id('702'), title: 'Family event', isActive: true },
    { id: id('703'), title: 'Retired reason', isActive: false },
  ],
} as const;

/** The permissions of a teacher who takes attendance and reads the summaries. */
export const ATTENDANCE_TEACHER = [
  'attendance.attendance_sheet.read',
  'attendance.attendance_sheet.record_attendance',
  'attendance.attendance_sheet.complete_attendance_sheet',
  'attendance.attendance_sheet.start_attendance_sheet',
  'attendance.attendance_sheet.reset_attendance_sheet',
  'attendance.attendance_sheet.take_attendance_for_session',
  'attendance.attendance_record.read',
  'attendance.attendance_record.summarize_attendance',
  'attendance.attendance_register.read',
  'attendance.absence_reason.read',
  'sis.student_enrollment.list_class_roster',
  'sis.student.read',
  'people.person.read',
  'org.institution.read',
] as const;

export interface FakeAttendanceOptions {
  status?: attendance.AttendanceSheet['status'];
  /** Marks already on the sheet. */
  marks?: attendance.AttendanceMark[];
  /** The sheet was opened for a timetable session: its marks are its expected students. */
  forSession?: boolean;
  /** When a student was placed in the class, by student id (default: always). */
  placed?: Record<string, { effectiveFrom?: string; effectiveTo?: string }>;
  /** Rows of attendance.SummarizeAttendance, whatever the grouping asked for. */
  summaries?: attendance.AttendanceSummary[];
}

/**
 * The attendance operations the screens use, over one in-memory sheet, with the server's rules for that slice:
 * marks only while the sheet is in progress (409), all-or-nothing validation (422 with `marks[i].field`), an
 * optimistic etag on recording and on the workflow steps (412), completion only with marks, and reopening that
 * keeps them.
 */
export function fakeAttendance(options: FakeAttendanceOptions = {}): Record<string, Handler> {
  const fixture = attendanceFixture;
  let status = options.status ?? 'in_progress';
  let version = 1;
  let sequence = 0;
  const records = new Map<string, attendance.AttendanceRecord>();
  const store = (mark: attendance.AttendanceMark) => {
    const existing = records.get(mark.studentId);
    records.set(mark.studentId, {
      ...mark,
      id: existing?.id ?? id(String(900 + ++sequence)),
      sheetId: fixture.sheetId,
      attendanceDate: fixture.date,
    });
  };
  (options.marks ?? []).forEach(store);
  const etag = () => `"${version}"`;
  const sheet = (): Response =>
    json(
      {
        id: fixture.sheetId,
        registerId: fixture.registerId,
        attendanceDate: fixture.date,
        reference: 'SH-0001',
        ...(options.forSession ? { timetableSessionId: fixture.sessionId } : {}),
        status,
        etag: etag(),
      },
      { headers: { ETag: etag() } },
    );
  const list = (req: Request, items: unknown[]): Response => onePage(req, items);
  const idsIn = (req: Request): string[] =>
    [...(new URL(req.url).searchParams.get('filter') ?? '').matchAll(/id = "([^"]+)"/g)].map(
      (m) => m[1] ?? '',
    );
  /** A workflow step: allowed from the given states, under the etag the caller read (when it sends one). */
  const step =
    (from: readonly string[], to: NonNullable<attendance.AttendanceSheet['status']>): Handler =>
    async (req) => {
      const body = (await req.json()) as { etag?: string };
      if (body.etag !== undefined && body.etag !== etag()) return problem(412, 'PRECONDITION_FAILED');
      if (!from.includes(status ?? '')) return problem(409, 'INVALID_STATE_TRANSITION');
      if (to === 'done' && records.size === 0)
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The attendance sheet has no marks to complete.',
        });
      status = to;
      version += 1;
      return sheet();
    };
  const base = '/v1/attendance/attendance-sheets/{attendanceSheetId}';
  return {
    [`GET ${base}`]: (_req, p) =>
      p.attendanceSheetId === fixture.sheetId ? sheet() : problem(404, 'NOT_FOUND'),
    'POST /v1/attendance/attendance-sheets:forSession': async (req) => {
      const body = (await req.json()) as attendance.TakeAttendanceForSessionRequest;
      return body.timetableSessionId === fixture.sessionId
        ? sheet()
        : problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
            detail: 'No active attendance register covers this session.',
          });
    },
    [`GET ${base}/records`]: (req) => list(req, [...records.values()]),
    [`POST ${base}:start`]: step(['draft'], 'in_progress'),
    [`POST ${base}:complete`]: step(['in_progress'], 'done'),
    [`POST ${base}:resetToDraft`]: step(['cancelled', 'done'], 'draft'),
    [`POST ${base}:recordAttendance`]: async (req) => {
      const body = (await req.json()) as attendance.RecordAttendanceRequest;
      if (body.etag !== undefined && body.etag !== etag()) return problem(412, 'PRECONDITION_FAILED');
      if (status !== 'in_progress')
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'Attendance can be recorded only while the sheet is in progress.',
        });
      const errors = body.marks.flatMap((mark, index) =>
        mark.absenceReasonId && mark.status !== 'absent' && mark.status !== 'excused'
          ? [
              {
                field: `marks[${index}].absenceReasonId`,
                message: 'a reason applies only to an absent or excused student',
                code: 'INVALID',
              },
            ]
          : (mark.remark ?? '').includes('!')
            ? [{ field: `marks[${index}].remark`, message: 'remark is not acceptable', code: 'INVALID' }]
            : [],
      );
      if (errors.length > 0) return problem(422, 'VALIDATION_FAILED', 'Invalid', { errors });
      body.marks.forEach(store);
      version += 1;
      if (body.complete) {
        status = 'done';
        version += 1;
      }
      // a write answers every item it wrote at once, not a page
      return json({ items: body.marks.map((mark) => records.get(mark.studentId)) });
    },
    'GET /v1/attendance/attendance-registers/{attendanceRegisterId}': () =>
      json({
        id: fixture.registerId,
        institutionId: fixture.institutionId,
        code: 'G5A',
        name: 'Grade 5 A',
        registerScope: 'class_section',
        classSectionId: fixture.classSectionId,
        isActive: true,
      }),
    'GET /v1/attendance/absence-reasons': (req) => list(req, [...fixture.reasons]),
    'GET /v1/attendance/summaries': (req) => list(req, options.summaries ?? []),
    'GET /v1/sis/class-sections/{classSectionId}:roster': (req, p) =>
      list(
        req,
        p.classSectionId === fixture.classSectionId
          ? fixture.students.map((student) => ({
              id: id(`8${student.rollNumber}`),
              studentId: student.id,
              academicYearId: id('e1'),
              classSectionId: fixture.classSectionId,
              rollNumber: student.rollNumber,
              enrollmentStatus: 'enrolled',
              ...options.placed?.[student.id],
            }))
          : [],
      ),
    'GET /v1/sis/students': (req) => {
      const wanted = idsIn(req);
      return list(
        req,
        fixture.students
          .filter((student) => wanted.includes(student.id))
          .map((student) => ({
            id: student.id,
            personId: student.personId,
            institutionId: fixture.institutionId,
            admissionNumber: `ADM-${student.rollNumber}`,
          })),
      );
    },
    'GET /v1/sis/students/{studentId}': (_req, p) => {
      const student = fixture.students.find((candidate) => candidate.id === p.studentId);
      return student
        ? json({
            id: student.id,
            personId: student.personId,
            institutionId: fixture.institutionId,
            admissionNumber: `ADM-${student.rollNumber}`,
          })
        : problem(404, 'NOT_FOUND');
    },
    'GET /v1/people/persons': (req) => {
      const wanted = idsIn(req);
      return list(
        req,
        fixture.students
          .filter((student) => wanted.includes(student.personId))
          .map((student) => ({ id: student.personId, displayName: student.name })),
      );
    },
    'GET /v1/org/institutions': (req) =>
      list(req, [
        { id: fixture.institutionId, code: 'MAIN', name: 'Main School', institutionType: 'school' },
      ]),
  };
}
