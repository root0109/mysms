import type { hr } from '@mysms/sdk';
import { attendanceFixture } from './attendance';
import { schoolAdmin } from './data';
import { json, problem, type Handler } from './fakeApi';
import { onePage } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;
const INSTITUTION = attendanceFixture.institutionId;

/**
 * A teacher (the signed-in account) with ten days of casual and five of sick leave, one application of their own
 * that waits for a decision, and a colleague's that waits in the approver's queue.
 */
export const leaveFixture = {
  institutionId: INSTITUTION,
  academicYearId: id('f1'),
  staffId: id('f11'),
  colleagueStaffId: id('f12'),
  casualId: id('f21'),
  sickId: id('f22'),
  ownApplicationId: id('f31'),
  colleagueApplicationId: id('f32'),
  approvedApplicationId: id('f33'),
} as const;

/** What a teacher's role needs to see their leave position and apply. */
export const LEAVE_STAFF = [
  'hr.staff.read',
  'hr.staff.get_leave_balance',
  'hr.leave_type.read',
  'hr.leave_application.read',
  'hr.leave_application.create',
  'hr.leave_application.cancel_leave',
  'org.academic_year.read',
] as const;

/** What an approver adds. */
export const LEAVE_APPROVER = [
  'hr.leave_application.approve_leave',
  'hr.leave_application.reject_leave',
] as const;

/** A stored application: the server sets what the contract marks read-only, so the fake may write it too. */
type Row = { -readonly [K in keyof hr.LeaveApplication]: hr.LeaveApplication[K] } & {
  id: string;
  version: number;
};

/**
 * The leave ledger over an in-memory set of applications, with the server's rules for the slice the screens use:
 * days counted with both ends (halved for a half-day part), a position per leave type (entitled, taken, pending,
 * available), decisions only while pending (409), no approval of one's own application (403) or beyond what is left
 * (409), cancelling a pending application or an approved one that has not started (its days go back), and the body
 * etag on every step (412).
 */
export function fakeLeave(
  options: {
    /** Days the colleague has left for casual leave (default: plenty), to exercise a refused approval. */
    colleagueLeft?: number;
    /** The staff member also has an approved casual leave of two days ahead (counted as taken), to cancel. */
    approvedAhead?: boolean;
  } = {},
): Record<string, Handler> {
  const fixture = leaveFixture;
  const allowance: Record<string, number> = { [fixture.casualId]: 10, [fixture.sickId]: 5 };
  const taken: Record<string, number> = {
    [fixture.casualId]: options.approvedAhead ? 4 : 2,
    [fixture.sickId]: 0,
  };
  let sequence = 0;
  const application = (
    appId: string,
    staffId: string,
    from: string,
    to: string,
    more: Partial<hr.LeaveApplication> = {},
  ): Row => ({
    id: appId,
    institutionId: INSTITUTION,
    academicYearId: fixture.academicYearId,
    staffId,
    leaveTypeId: fixture.casualId,
    fromAt: `${from}T00:00:00Z`,
    toAt: `${to}T00:00:00Z`,
    dayPart: 'full_day',
    status: 'pending',
    version: 1,
    ...more,
  });
  const rows = new Map<string, Row>(
    [
      application(fixture.ownApplicationId, fixture.staffId, '2026-03-10', '2026-03-11'),
      application(fixture.colleagueApplicationId, fixture.colleagueStaffId, '2026-03-16', '2026-03-18', {
        remarks: 'Family function',
      }),
      ...(options.approvedAhead
        ? [
            application(fixture.approvedApplicationId, fixture.staffId, '2098-12-01', '2098-12-02', {
              status: 'approved',
            }),
          ]
        : []),
    ].map((row) => [row.id, row]),
  );
  const daysOf = (row: hr.LeaveApplication) =>
    ((Date.parse(row.toAt) - Date.parse(row.fromAt)) / 86_400_000 + 1) *
    (row.dayPart === 'full_day' ? 1 : 0.5);
  const out = ({ version, ...row }: Row) => ({ ...row, etag: `"${version}"` });
  const pending = (staffId: string, leaveTypeId: string) =>
    [...rows.values()]
      .filter((row) => row.staffId === staffId && row.leaveTypeId === leaveTypeId && row.status === 'pending')
      .reduce((sum, row) => sum + daysOf(row), 0);
  const step =
    (to: hr.LeaveApplication['status'], approve = false): Handler =>
    async (req, params) => {
      const row = rows.get(params.leaveApplicationId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { etag?: string; reason?: string };
      if (body.etag !== undefined && body.etag !== `"${row.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      const cancelsApproved =
        to === 'cancelled' && row.status === 'approved' && Date.parse(row.fromAt) > Date.now();
      if (row.status !== 'pending' && !cancelsApproved) return problem(409, 'INVALID_STATE_TRANSITION');
      if (cancelsApproved && row.leaveTypeId)
        taken[row.leaveTypeId] = (taken[row.leaveTypeId] ?? 0) - daysOf(row);
      if (approve) {
        if (row.staffId === fixture.staffId) return problem(403, 'PERMISSION_DENIED');
        const left = options.colleagueLeft ?? allowance[row.leaveTypeId ?? ''] ?? 0;
        if (row.leaveTypeId && daysOf(row) > left)
          return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
            detail: `The application asks for ${daysOf(row)} days and ${left} are left for this leave type.`,
          });
      }
      row.status = to;
      row.version += 1;
      if (to !== 'cancelled') {
        row.decidedByUserId = schoolAdmin.user?.id;
        row.decidedOn = '2026-03-09';
        if (body.reason) row.decisionComments = body.reason;
      }
      return json(out(row));
    };
  return {
    'GET /v1/org/academic-years': (req) =>
      onePage(req, [
        {
          id: fixture.academicYearId,
          institutionId: INSTITUTION,
          name: 'This year',
          // wide enough to hold the day a test runs on
          startDate: '2000-01-01',
          endDate: '2099-12-31',
        },
      ]),
    'GET /v1/hr/staff': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const me = {
        id: fixture.staffId,
        personId: schoolAdmin.user?.personId,
        userId: schoolAdmin.user?.id,
        institutionId: INSTITUTION,
      };
      const colleague = { id: fixture.colleagueStaffId, personId: id('f52'), institutionId: INSTITUTION };
      // the account's own records (named by iam.GetMe) and colleagues' names are both asked for by staff id
      const items = [me, colleague].filter((member) => filter.includes(`id = "${member.id}"`));
      return onePage(req, items);
    },
    'GET /v1/people/persons': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const items = [{ id: id('f52'), displayName: 'Meera Iyer' }].filter((person) =>
        filter.includes(person.id),
      );
      return onePage(req, items);
    },
    'GET /v1/hr/leave-types': (req) =>
      onePage(req, [
        { id: fixture.sickId, institutionId: INSTITUTION, name: 'Sick leave', code: 'SL', sortOrder: 2 },
        {
          id: fixture.casualId,
          institutionId: INSTITUTION,
          name: 'Casual leave',
          code: 'CL',
          sortOrder: 1,
        },
      ]),
    'GET /v1/hr/staff/{staffId}:leaveBalance': (req, params) => {
      if (!new URL(req.url).searchParams.get('academicYearId')) return problem(422, 'VALIDATION_FAILED');
      if (params.staffId !== fixture.staffId) return problem(403, 'PERMISSION_DENIED');
      return onePage(
        req,
        [fixture.casualId, fixture.sickId].map((leaveTypeId) => {
          const asked = pending(fixture.staffId, leaveTypeId);
          const entitled = allowance[leaveTypeId] ?? 0;
          const used = taken[leaveTypeId] ?? 0;
          return {
            leaveTypeId,
            entitledDays: String(entitled),
            takenDays: String(used),
            pendingDays: String(asked),
            availableDays: String(Math.max(0, entitled - used - asked)),
          };
        }),
      );
    },
    'GET /v1/hr/leave-applications': (req) => {
      const filter = new URL(req.url).searchParams.get('filter') ?? '';
      const staffId = /staffId = "([^"]+)"/.exec(filter)?.[1];
      const status = /status = "([^"]+)"/.exec(filter)?.[1];
      const items = [...rows.values()]
        .filter((row) => (!staffId || row.staffId === staffId) && (!status || row.status === status))
        .map(out);
      return onePage(req, items);
    },
    'POST /v1/hr/leave-applications': async (req) => {
      const body = (await req.json()) as hr.LeaveApplication;
      if (!body.staffId || !body.academicYearId || !body.fromAt || !body.toAt)
        return problem(422, 'VALIDATION_FAILED');
      const row: Row = { ...body, id: id(`f4${++sequence}`), status: 'pending', version: 1 };
      rows.set(row.id, row);
      return json(out(row), { status: 201 });
    },
    'POST /v1/hr/leave-applications/{leaveApplicationId}:approve': step('approved', true),
    'POST /v1/hr/leave-applications/{leaveApplicationId}:reject': step('rejected'),
    'POST /v1/hr/leave-applications/{leaveApplicationId}:cancel': step('cancelled'),
  };
}
