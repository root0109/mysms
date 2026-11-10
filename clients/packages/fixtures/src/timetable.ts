import type { timetable } from '@mysms/sdk';
import { attendanceFixture } from './attendance';
import { schoolAdmin } from './data';
import { problem, type Handler } from './fakeApi';
import { onePage, pageOf } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;
const INSTITUTION = attendanceFixture.institutionId;

/**
 * A teacher's week of 2–8 March 2026 in a school on Indian time: two sessions on Monday (one held), a cancelled one
 * on Wednesday, a draft the timetable does not show, and one late on Sunday UTC that is already Monday of the next
 * week for the school.
 */
export const timetableFixture = {
  week: '2026-03-02',
  institutionId: INSTITUTION,
  staffId: id('e1'),
  /** Another teacher of the class; also the second staff record of an account employed by two schools. */
  otherStaffId: id('e2'),
  classSectionId: attendanceFixture.classSectionId,
  subjects: [
    { id: id('e51'), name: 'Mathematics' },
    { id: id('e52'), name: 'Science' },
  ],
  rooms: [{ id: id('e61'), name: 'Room 12' }],
} as const;

/** What a teacher's role needs to read their own timetable with names. */
export const TIMETABLE_TEACHER = [
  'timetable.session.get_timetable',
  'hr.staff.read',
  'academics.subject.read',
  'facility.room.read',
] as const;

const session = (
  suffix: string,
  startAt: string,
  endAt: string,
  status: timetable.Session['status'],
  more: Partial<timetable.Session> = {},
): timetable.Session => ({
  id: id(suffix),
  institutionId: INSTITUTION,
  startAt,
  endAt,
  status,
  staffId: timetableFixture.staffId,
  classSectionId: timetableFixture.classSectionId,
  subjectId: timetableFixture.subjects[0].id,
  roomId: timetableFixture.rooms[0].id,
  ...more,
});

const SESSIONS: readonly timetable.Session[] = [
  session('e71', '2026-03-02T03:00:00Z', '2026-03-02T03:45:00Z', 'done'),
  session('e72', '2026-03-02T04:00:00Z', '2026-03-02T04:45:00Z', 'confirmed', {
    subjectId: timetableFixture.subjects[1].id,
  }),
  session('e73', '2026-03-04T03:00:00Z', '2026-03-04T03:45:00Z', 'cancelled'),
  session('e74', '2026-03-05T03:00:00Z', '2026-03-05T03:45:00Z', 'draft'),
  session('e75', '2026-03-08T18:45:00Z', '2026-03-08T19:30:00Z', 'confirmed', { title: 'Remedial class' }),
  session('e76', '2026-03-03T03:00:00Z', '2026-03-03T03:45:00Z', 'confirmed', {
    staffId: timetableFixture.otherStaffId,
    title: 'Another teacher',
  }),
];

/**
 * timetable.GetTimetable over the fixture week with the server's rules for the slice the screens use — whole UTC
 * days, one subject at least (422), no drafts, start order — and the reads a timetable screen makes beside it: the
 * signed-in account's staff record, subject names and room names.
 */
export function fakeTimetable(
  options: {
    /** Sessions per page (default: all in one), to exercise paging. */
    pageSize?: number;
    /** Refuse the first timetable read (403: a failure the client does not retry by itself), to exercise Retry. */
    failOnce?: boolean;
  } = {},
): Record<string, Handler> {
  let failures = options.failOnce ? 1 : 0;
  const staffRow = (staffId: string) => ({
    id: staffId,
    personId: schoolAdmin.user?.personId,
    userId: schoolAdmin.user?.id,
    institutionId: INSTITUTION,
  });
  return {
    'GET /v1/timetable/sessions:timetable': (req) => {
      const query = new URL(req.url).searchParams;
      const staffId = query.get('staffId');
      const classSectionId = query.get('classSectionId');
      if (!staffId && !classSectionId && !query.get('roomId')) return problem(422, 'VALIDATION_FAILED');
      if (failures > 0) {
        failures -= 1;
        return problem(403, 'PERMISSION_DENIED');
      }
      const from = `${query.get('fromDate')}T00:00:00Z`;
      const to = `${query.get('toDate')}T23:59:59Z`;
      const items = SESSIONS.filter(
        (row) =>
          row.status !== 'draft' &&
          row.startAt >= from &&
          row.startAt <= to &&
          (!staffId || row.staffId === staffId) &&
          (!classSectionId || row.classSectionId === classSectionId),
      )
        .sort((a, b) => a.startAt.localeCompare(b.startAt))
        .map((row) => ({ session: row }));
      return options.pageSize ? pageOf(req, items, { maxSize: options.pageSize }) : onePage(req, items);
    },
    // the account's staff records are the ones iam.GetMe names (Me.staffIds): asked for by id
    'GET /v1/hr/staff': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const items = [timetableFixture.staffId, timetableFixture.otherStaffId]
        .filter((staffId) => filter.includes(`id = "${staffId}"`))
        .map(staffRow);
      return onePage(req, items);
    },
    'GET /v1/academics/subjects': (req) => onePage(req, timetableFixture.subjects),
    'GET /v1/facility/rooms': (req) => onePage(req, timetableFixture.rooms),
  };
}
