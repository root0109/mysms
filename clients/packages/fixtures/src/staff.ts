import type { hr } from '@mysms/sdk';
import { attendanceFixture } from './attendance';
import { json, problem, type Handler } from './fakeApi';
import { onePage, pageOf } from './paging';

const id = (suffix: string) => `01960000-0000-7000-8000-${suffix.padStart(12, '0')}`;
const INSTITUTION = attendanceFixture.institutionId;

/**
 * A school's staff: a teacher who signs in (account set), a colleague without an account, and one who left; two
 * designations, two departments, two subjects, two leave types with this year's balances, and a salary row.
 */
export const staffFixture = {
  institutionId: INSTITUTION,
  academicYearId: id('e1'),
  teacherId: id('e11'),
  colleagueId: id('e12'),
  leaverId: id('e13'),
  teacherPersonId: id('e21'),
  colleaguePersonId: id('e22'),
  leaverPersonId: id('e23'),
  /** A person recorded in People who is not staff yet. */
  newcomerPersonId: id('e24'),
  teacherDesignationId: id('e31'),
  clerkDesignationId: id('e32'),
  scienceDepartmentId: id('e41'),
  officeDepartmentId: id('e42'),
  physicsSubjectId: id('e51'),
  chemistrySubjectId: id('e52'),
  casualId: id('e61'),
  sickId: id('e62'),
  teacherCasualBalanceId: id('e71'),
  accountId: id('e81'),
} as const;

/** What an HR officer holds for the staff directory, the staff detail and the leave balances. */
export const HR_OFFICER = [
  'hr.staff.read',
  'hr.staff.create',
  'hr.staff.update',
  'hr.staff.delete',
  'hr.staff.change_staff_status',
  'hr.staff.create_staff_portal_account',
  'hr.staff.get_leave_balance',
  'hr.staff_department.read',
  'hr.staff_department.create',
  'hr.staff_department.update',
  'hr.staff_department.delete',
  'hr.staff_subject.read',
  'hr.staff_subject.create',
  'hr.staff_subject.update',
  'hr.staff_subject.delete',
  'hr.designation.read',
  'hr.leave_type.read',
  'hr.leave_balance.read',
  'hr.leave_balance.create',
  'hr.leave_balance.update',
  'hr.leave_application.read',
  'org.department.read',
  'org.academic_year.read',
  'academics.subject.read',
  'people.person.read',
  'payroll.staff_salary.read',
] as const;

type Stored<T> = { -readonly [K in keyof T]: T[K] } & { id: string; version: number };
type StaffRow = Stored<hr.Staff>;
type ChildRow = Stored<{ staffId: string; targetId: string }>;
type BalanceRow = Stored<hr.LeaveBalance>;

const out = <T extends { version: number }>({ version, ...row }: T) => ({ ...row, etag: `"${version}"` });
const filterOf = (req: Request) => new URL(req.url).searchParams.get('filter') ?? '';
/** The values a filter compares one field with (`field = "v"`), in order. */
const valuesOf = (filter: string, field: string) =>
  [...filter.matchAll(new RegExp(`\\b${field} = "([^"]+)"`, 'g'))].map((m) => m[1] as string);
const ifMatches = (req: Request, version: number) => req.headers.get('If-Match') === `"${version}"`;

/**
 * hr's staff records with the server's rules for the slice the screens use: list filters (status, designation, main
 * department, employee number contains, persons, ids), If-Match on updates and deletes (428 / 412), the status moves
 * of hr.ChangeStaffStatus (409 otherwise, body etag 412), a login account only for one who has none, has not left
 * and has an employee number (409 / 422), one row per department or subject (409), and leave balances that keep
 * their staff member, type and year and never fall below the days used (422).
 */
export function fakeStaff(options: { accountUnavailable?: boolean } = {}): Record<string, Handler> {
  const fx = staffFixture;
  let sequence = 0;
  const staff = new Map<string, StaffRow>(
    (
      [
        {
          id: fx.teacherId,
          personId: fx.teacherPersonId,
          userId: fx.accountId,
          employeeCode: 'T-101',
          designationId: fx.teacherDesignationId,
          primaryDepartmentId: fx.scienceDepartmentId,
          employmentType: 'full_time',
          joiningDate: '2019-06-01',
          status: 'active',
        },
        {
          id: fx.colleagueId,
          personId: fx.colleaguePersonId,
          employeeCode: 'O-201',
          designationId: fx.clerkDesignationId,
          primaryDepartmentId: fx.officeDepartmentId,
          employmentType: 'part_time',
          status: 'active',
        },
        { id: fx.leaverId, personId: fx.leaverPersonId, employeeCode: 'T-099', status: 'exited' },
      ] as const
    ).map((row) => [row.id, { ...row, institutionId: INSTITUTION, version: 1 } as StaffRow]),
  );
  const children: Record<'departments' | 'subjects', Map<string, ChildRow>> = {
    departments: new Map([
      [id('e91'), { id: id('e91'), staffId: fx.teacherId, targetId: fx.scienceDepartmentId, version: 1 }],
    ]),
    subjects: new Map([
      [id('e92'), { id: id('e92'), staffId: fx.teacherId, targetId: fx.physicsSubjectId, version: 1 }],
    ]),
  };
  const balances = new Map<string, BalanceRow>([
    [
      fx.teacherCasualBalanceId,
      {
        id: fx.teacherCasualBalanceId,
        staffId: fx.teacherId,
        leaveTypeId: fx.casualId,
        academicYearId: fx.academicYearId,
        entitledDays: '12',
        usedDays: '3',
        version: 1,
      },
    ],
  ]);
  const childField = { departments: 'departmentId', subjects: 'subjectId' } as const;
  const childOut = (kind: 'departments' | 'subjects', { targetId, ...row }: ChildRow) =>
    out({ ...row, [childField[kind]]: targetId });

  const childRoutes = (kind: 'departments' | 'subjects', param: string): Record<string, Handler> => {
    const rows = children[kind];
    const field = childField[kind];
    const taken = (staffId: string, targetId: string, except?: string) =>
      [...rows.values()].some((r) => r.staffId === staffId && r.targetId === targetId && r.id !== except);
    return {
      [`GET /v1/hr/staff/{staffId}/${kind}`]: (req, params) =>
        onePage(
          req,
          [...rows.values()].filter((r) => r.staffId === params.staffId).map((r) => childOut(kind, r)),
        ),
      [`POST /v1/hr/staff/{staffId}/${kind}`]: async (req, params) => {
        const staffId = params.staffId ?? '';
        if (!staff.has(staffId)) return problem(404, 'NOT_FOUND');
        const targetId = ((await req.json()) as Record<string, string>)[field];
        if (!targetId) return problem(422, 'VALIDATION_FAILED');
        if (taken(staffId, targetId)) return problem(409, 'DUPLICATE_RESOURCE', 'Conflict');
        const row: ChildRow = { id: id(`ea${++sequence}`), staffId, targetId, version: 1 };
        rows.set(row.id, row);
        return json(childOut(kind, row), { status: 201 });
      },
      [`PATCH /v1/hr/staff/{staffId}/${kind}/{${param}}`]: async (req, params) => {
        const row = rows.get(params[param] ?? '');
        if (!row || row.staffId !== params.staffId) return problem(404, 'NOT_FOUND');
        if (!ifMatches(req, row.version)) return problem(412, 'PRECONDITION_FAILED');
        const targetId = ((await req.json()) as Record<string, string>)[field];
        if (!targetId) return problem(422, 'VALIDATION_FAILED');
        if (taken(row.staffId, targetId, row.id)) return problem(409, 'DUPLICATE_RESOURCE', 'Conflict');
        row.targetId = targetId;
        row.version += 1;
        return json(childOut(kind, row));
      },
      [`DELETE /v1/hr/staff/{staffId}/${kind}/{${param}}`]: (req, params) => {
        const row = rows.get(params[param] ?? '');
        if (!row || row.staffId !== params.staffId) return problem(404, 'NOT_FOUND');
        if (!ifMatches(req, row.version)) return problem(412, 'PRECONDITION_FAILED');
        rows.delete(row.id);
        return new Response(null, { status: 204 });
      },
    };
  };

  const moves: Record<string, readonly string[]> = {
    active: ['inactive', 'exited'],
    inactive: ['active', 'exited'],
    exited: ['active'],
  };

  return {
    // before the plain read below, whose {staffId} would also match "<id>:leaveBalance"
    'GET /v1/hr/staff/{staffId}:leaveBalance': (req, params) => {
      if (!new URL(req.url).searchParams.get('academicYearId')) return problem(422, 'VALIDATION_FAILED');
      const lines = [...balances.values()]
        .filter((b) => b.staffId === params.staffId)
        .map((b) => ({
          leaveTypeId: b.leaveTypeId,
          entitledDays: b.entitledDays,
          takenDays: b.usedDays ?? '0',
          pendingDays: '0',
          availableDays: String(Number(b.entitledDays) - Number(b.usedDays ?? 0)),
        }));
      return onePage(req, lines);
    },
    'GET /v1/hr/staff': (req) => {
      const filter = filterOf(req);
      const ids = valuesOf(filter, 'id');
      const persons = valuesOf(filter, 'personId');
      const code = /employeeCode : "([^"]*)"/.exec(filter)?.[1];
      const [status] = valuesOf(filter, 'status');
      const [designation] = valuesOf(filter, 'designationId');
      const [department] = valuesOf(filter, 'primaryDepartmentId');
      const [user] = valuesOf(filter, 'userId');
      const items = [...staff.values()].filter(
        (r) =>
          (ids.length === 0 || ids.includes(r.id)) &&
          (code === undefined ||
            (r.employeeCode ?? '').toLowerCase().includes(code.toLowerCase()) ||
            persons.includes(r.personId)) &&
          (code !== undefined || persons.length === 0 || persons.includes(r.personId)) &&
          (!status || r.status === status) &&
          (!designation || r.designationId === designation) &&
          (!department || r.primaryDepartmentId === department) &&
          (!user || r.userId === user),
      );
      return pageOf(req, items.map(out));
    },
    'GET /v1/hr/staff/{staffId}': (_req, params) => {
      const row = staff.get(params.staffId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      return json(out(row), { headers: { ETag: `"${row.version}"` } });
    },
    'POST /v1/hr/staff': async (req) => {
      const body = (await req.json()) as hr.Staff;
      if (!body.personId || !body.institutionId)
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [{ field: 'personId', message: 'must not be null', code: 'NotNull' }],
        });
      if ([...staff.values()].some((r) => r.personId === body.personId))
        return problem(409, 'DUPLICATE_RESOURCE', 'Conflict', {
          detail: 'A staff with the same institution id and person id already exists.',
        });
      const row: StaffRow = { ...body, id: id(`eb${++sequence}`), status: 'active', version: 1 };
      staff.set(row.id, row);
      return json(out(row), { status: 201 });
    },
    'PATCH /v1/hr/staff/{staffId}': async (req, params) => {
      const row = staff.get(params.staffId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (!req.headers.get('If-Match')) return problem(428, 'PRECONDITION_REQUIRED');
      if (!ifMatches(req, row.version)) return problem(412, 'PRECONDITION_FAILED');
      const body = (await req.json()) as Record<string, unknown>;
      const mask = new URL(req.url).searchParams.get('updateMask')?.split(',') ?? Object.keys(body);
      for (const field of mask) (row as Record<string, unknown>)[field] = body[field];
      row.version += 1;
      return json(out(row));
    },
    'DELETE /v1/hr/staff/{staffId}': (req, params) => {
      const row = staff.get(params.staffId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (!ifMatches(req, row.version)) return problem(412, 'PRECONDITION_FAILED');
      staff.delete(row.id);
      return new Response(null, { status: 204 });
    },
    'POST /v1/hr/staff/{staffId}:changeStatus': async (req, params) => {
      const row = staff.get(params.staffId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      const body = (await req.json()) as { status: string; etag?: string };
      if (body.etag !== undefined && body.etag !== `"${row.version}"`)
        return problem(412, 'PRECONDITION_FAILED');
      if (!(moves[row.status ?? ''] ?? []).includes(body.status))
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: `A staff member who is ${row.status ?? ''} cannot become ${body.status}.`,
        });
      row.status = body.status as hr.Staff['status'];
      row.version += 1;
      return json(out(row));
    },
    'POST /v1/hr/staff/{staffId}:createPortalAccount': (_req, params) => {
      const row = staff.get(params.staffId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (row.userId)
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'The staff member already has a login account.',
        });
      if (row.status === 'exited')
        return problem(409, 'INVALID_STATE_TRANSITION', 'Conflict', {
          detail: 'A staff member who has left is not given a login account.',
        });
      if (!row.employeeCode) return problem(422, 'VALIDATION_FAILED');
      if (options.accountUnavailable) return problem(503, 'UNAVAILABLE');
      row.userId = id(`ec${++sequence}`);
      row.version += 1;
      return json(out(row));
    },
    ...childRoutes('departments', 'staffDepartmentId'),
    ...childRoutes('subjects', 'staffSubjectId'),
    'GET /v1/hr/designations': (req) =>
      onePage(req, [
        {
          id: fx.clerkDesignationId,
          code: 'CLK',
          name: 'Office clerk',
          staffCategory: 'non_teaching',
          sortOrder: 2,
        },
        {
          id: fx.teacherDesignationId,
          code: 'TGT',
          name: 'Teacher',
          staffCategory: 'teaching',
          sortOrder: 1,
        },
      ]),
    'GET /v1/org/departments': (req) =>
      onePage(req, [
        { id: fx.scienceDepartmentId, institutionId: INSTITUTION, code: 'SCI', name: 'Science' },
        { id: fx.officeDepartmentId, institutionId: INSTITUTION, code: 'OFF', name: 'Administration office' },
      ]),
    'GET /v1/academics/subjects': (req) =>
      onePage(req, [
        { id: fx.physicsSubjectId, name: 'Physics' },
        { id: fx.chemistrySubjectId, name: 'Chemistry' },
      ]),
    'GET /v1/people/persons': (req) => {
      const filter = filterOf(req);
      const persons = [
        { id: fx.teacherPersonId, displayName: 'Anil Kumar' },
        { id: fx.colleaguePersonId, displayName: 'Meera Iyer' },
        { id: fx.leaverPersonId, displayName: 'Kavya Rao' },
        { id: fx.newcomerPersonId, displayName: 'Kavitha Menon' },
      ];
      const named = /displayName : "([^"]*)"/.exec(filter)?.[1]?.toLowerCase();
      return onePage(
        req,
        persons.filter((p) =>
          named !== undefined ? p.displayName.toLowerCase().includes(named) : filter.includes(p.id),
        ),
      );
    },
    'GET /v1/payroll/staff-salaries': (req) =>
      onePage(
        req,
        valuesOf(filterOf(req), 'staffId').includes(fx.teacherId)
          ? [
              {
                id: id('ed1'),
                staffId: fx.teacherId,
                salaryTemplateId: id('ed2'),
                grossSalary: '52000.00',
                effectiveDate: '2026-04-01',
                remarks: 'Annual revision',
                etag: '"1"',
              },
            ]
          : [],
      ),
    'GET /v1/org/academic-years': (req) =>
      onePage(req, [
        {
          id: fx.academicYearId,
          institutionId: INSTITUTION,
          name: 'This year',
          startDate: '2000-01-01',
          endDate: '2099-12-31',
        },
      ]),
    'GET /v1/hr/leave-types': (req) =>
      onePage(req, [
        {
          id: fx.casualId,
          institutionId: INSTITUTION,
          name: 'Casual leave',
          maxDaysPerYear: '12',
          sortOrder: 1,
        },
        { id: fx.sickId, institutionId: INSTITUTION, name: 'Sick leave', maxDaysPerYear: '6', sortOrder: 2 },
      ]),
    'GET /v1/hr/leave-balances': (req) => {
      const filter = filterOf(req);
      const [year] = valuesOf(filter, 'academicYearId');
      const [staffId] = valuesOf(filter, 'staffId');
      return onePage(
        req,
        [...balances.values()]
          .filter((b) => (!year || b.academicYearId === year) && (!staffId || b.staffId === staffId))
          .map(out),
      );
    },
    'POST /v1/hr/leave-balances': async (req) => {
      const body = (await req.json()) as hr.LeaveBalance;
      if (!body.staffId || !body.leaveTypeId || !body.academicYearId || !body.entitledDays)
        return problem(422, 'VALIDATION_FAILED');
      if (
        [...balances.values()].some(
          (b) =>
            b.staffId === body.staffId &&
            b.leaveTypeId === body.leaveTypeId &&
            b.academicYearId === body.academicYearId,
        )
      )
        return problem(409, 'DUPLICATE_RESOURCE', 'Conflict', {
          detail: 'A leave balance for this staff member, leave type and year already exists.',
        });
      const row: BalanceRow = { ...body, id: id(`ee${++sequence}`), usedDays: '0', version: 1 };
      balances.set(row.id, row);
      return json(out(row), { status: 201 });
    },
    'PATCH /v1/hr/leave-balances/{leaveBalanceId}': async (req, params) => {
      const row = balances.get(params.leaveBalanceId ?? '');
      if (!row) return problem(404, 'NOT_FOUND');
      if (!ifMatches(req, row.version)) return problem(412, 'PRECONDITION_FAILED');
      const body = (await req.json()) as hr.LeaveBalanceUpdate;
      if (Number(body.entitledDays) < Number(row.usedDays ?? 0))
        return problem(422, 'VALIDATION_FAILED', 'Invalid', {
          errors: [{ field: 'entitledDays', message: 'is below the days already used', code: 'BELOW_USED' }],
        });
      row.entitledDays = body.entitledDays ?? row.entitledDays;
      row.carriedForwardDays = body.carriedForwardDays;
      row.version += 1;
      return json(out(row));
    },
  };
}
