import {
  useInfiniteQuery,
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query';
import type { academics, hr, org } from '@mysms/sdk';
import { TeacherSubjectApi } from '@mysms/sdk/academics/apis/TeacherSubjectApi';
import { DesignationApi } from '@mysms/sdk/hr/apis/DesignationApi';
import { LeaveBalanceApi } from '@mysms/sdk/hr/apis/LeaveBalanceApi';
import { StaffApi } from '@mysms/sdk/hr/apis/StaffApi';
import { DepartmentApi } from '@mysms/sdk/org/apis/DepartmentApi';
import { PersonApi } from '@mysms/sdk/people/apis/PersonApi';
import { FIRST_PAGE, allPages, nextPage, pageQuery } from '../api/pages';
import { patchOf } from '../api/patch';
import { call, callWithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { NAME_CHUNK, anyOf } from './attendance';

export type Staff = hr.Staff;
export type StaffStatus = NonNullable<Staff['status']>;
export type StaffEmploymentType = NonNullable<Staff['employmentType']>;
export type StaffDepartment = hr.StaffDepartment;
/** A subject the teacher can teach: academics' teacher profile (academics.teacher_subject, keyed by the staff id). */
export type StaffSubject = academics.TeacherSubject;
export type Designation = hr.Designation;
export type Department = org.Department;
export type LeaveBalance = hr.LeaveBalance;

export const STAFF_STATUSES: readonly StaffStatus[] = ['active', 'inactive', 'exited'];
export const STAFF_EMPLOYMENT_TYPES: readonly StaffEmploymentType[] = [
  'full_time',
  'part_time',
  'contract',
  'visiting',
];

/**
 * hr.ChangeStaffStatus, from each status to the statuses it may become (api/state-machines.md "hr.Staff"): suspended
 * and taken back, leaves from either, and one who left comes back as active on the same record.
 */
const STATUS_MOVES: Readonly<Record<StaffStatus, readonly StaffStatus[]>> = {
  active: ['inactive', 'exited'],
  inactive: ['active', 'exited'],
  exited: ['active'],
};

/** The statuses a staff member may be moved to; none while the status is unknown. */
export function staffStatusMoves(status: Staff['status']): readonly StaffStatus[] {
  return status ? (STATUS_MOVES[status] ?? []) : [];
}

/**
 * Why a staff member cannot be given a login account (hr.CreateStaffPortalAccount), or null when they can: one who
 * has an account or has left is refused (409), and the employee number becomes the login name (422 without one).
 */
export function portalAccountBlocker(
  staff: Pick<Staff, 'userId' | 'status' | 'employeeCode'>,
): 'hasAccount' | 'exited' | 'noEmployeeCode' | null {
  if (staff.userId) return 'hasAccount';
  if (staff.status === 'exited') return 'exited';
  if (!staff.employeeCode?.trim()) return 'noEmployeeCode';
  return null;
}

/** What the staff directory is narrowed to; every part is optional. */
export interface StaffQuery {
  /** Part of an employee number, or of a name (names need `people.person.read`). */
  search?: string;
  status?: StaffStatus;
  designationId?: string;
  /** The staff member's main department. */
  departmentId?: string;
}

const quoted = (value: string) => `"${value.replace(/["\\]/g, '')}"`;

/**
 * The hr.ListStaff filter of a directory query. A search matches the employee number, or the persons whose names
 * matched it (`personIds`, read from people first: hr does not keep names).
 */
export function staffFilter(query: StaffQuery, personIds: readonly string[] = []): string | undefined {
  const parts: string[] = [];
  const search = query.search?.trim();
  if (search) {
    const terms = [
      `employeeCode : ${quoted(search)}`,
      ...personIds.map((personId) => `personId = ${quoted(personId)}`),
    ];
    parts.push(`(${terms.join(' OR ')})`);
  }
  if (query.status) parts.push(`status = ${quoted(query.status)}`);
  if (query.designationId) parts.push(`designationId = ${quoted(query.designationId)}`);
  if (query.departmentId) parts.push(`primaryDepartmentId = ${quoted(query.departmentId)}`);
  return parts.length ? parts.join(' AND ') : undefined;
}

const DIRECTORY = queryKey('hr', 'staff-directory');
const STAFF = (staffId: string | null) => queryKey('hr', 'staff', staffId);

/** After a staff member changed: the directory, their detail, the standard screens and "my staff record" are stale. */
async function refreshStaff(qc: QueryClient, staffId?: string): Promise<void> {
  void qc.invalidateQueries({ queryKey: queryKey('records', 'hr.Staff') });
  void qc.invalidateQueries({ queryKey: queryKey('records', 'hr.Staff#item') });
  void qc.invalidateQueries({ queryKey: queryKey('hr', 'my-staff') });
  await Promise.all([
    qc.invalidateQueries({ queryKey: DIRECTORY }),
    staffId ? qc.invalidateQueries({ queryKey: STAFF(staffId) }) : Promise.resolve(),
  ]);
}

/**
 * The persons whose names contain the search (people.ListPersons, at most forty): a directory search also finds
 * staff by name. Without the permission to read persons, or for a short search, none.
 */
export function usePersonsNamed(search: string, enabled = true) {
  const { api } = useRuntime();
  const text = search.trim();
  return useQuery({
    enabled: enabled && text.length >= 2,
    queryKey: queryKey('people', 'persons-named', text),
    queryFn: async () => {
      const persons = await call(
        api.get(PersonApi).listPersons({ size: NAME_CHUNK, filter: `displayName : ${quoted(text)}` }),
      );
      return persons.data.flatMap((person) => (person.id ? [person.id] : []));
    },
    staleTime: STALE_TIME.list,
    retry: false,
  });
}

/** hr.ListStaff narrowed by a directory query, page by page (the server orders and pages it). */
export function useStaffDirectory(query: StaffQuery, personIds: readonly string[] = [], enabled = true) {
  const { api } = useRuntime();
  const filter = staffFilter(query, personIds);
  return useInfiniteQuery({
    enabled,
    queryKey: queryKey('hr', 'staff-directory', { filter }),
    initialPageParam: FIRST_PAGE,
    queryFn: ({ pageParam }) =>
      call(api.get(StaffApi).listStaff({ size: 50, ...(filter ? { filter } : {}), ...pageQuery(pageParam) })),
    getNextPageParam: (last) => nextPage(last.paging),
    staleTime: STALE_TIME.list,
  });
}

/** hr.GetStaff with its ETag for a later If-Match. */
export function useStaff(staffId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: staffId !== null,
    queryKey: STAFF(staffId),
    queryFn: () => callWithEtag(api.get(StaffApi).getStaffRaw({ staffId: staffId as string })),
    staleTime: STALE_TIME.detail,
  });
}

/** The display names of persons, by person id (people.ListPersons, forty at a time, inside the caller's scope). */
export function usePersonNames(personIds: readonly string[], enabled = true): ReadonlyMap<string, string> {
  const { api } = useRuntime();
  const ids = enabled ? [...new Set(personIds)] : [];
  const chunks: string[][] = [];
  for (let start = 0; start < ids.length; start += NAME_CHUNK)
    chunks.push(ids.slice(start, start + NAME_CHUNK));
  return useQueries({
    queries: chunks.map((chunk) => ({
      queryKey: queryKey('people', 'person-names', chunk),
      queryFn: async (): Promise<[string, string][]> => {
        const persons = await call(
          api.get(PersonApi).listPersons({ size: NAME_CHUNK, filter: anyOf(chunk) }),
        );
        return persons.data.flatMap((person) =>
          person.id ? [[person.id, person.displayName] as [string, string]] : [],
        );
      },
      staleTime: STALE_TIME.reference,
      retry: false,
    })),
    combine: (results) => new Map(results.flatMap((result) => result.data ?? [])),
  });
}

/** hr.ListDesignations: the designations of the tenant, in the order the school lists them. */
export function useDesignations(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('hr', 'designations'),
    queryFn: async () => {
      const read = await allPages((page) =>
        call(api.get(DesignationApi).listDesignations({ size: 500, ...pageQuery(page) })),
      );
      return read.items.sort(
        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name),
      );
    },
    staleTime: STALE_TIME.reference,
  });
}

/** org.ListDepartments: the departments the caller may see, by name. */
export function useDepartments(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('org', 'departments', { all: true }),
    queryFn: async () => {
      const read = await allPages((page) =>
        call(api.get(DepartmentApi).listDepartments({ size: 500, ...pageQuery(page) })),
      );
      return read.items.sort((a, b) => a.name.localeCompare(b.name));
    },
    staleTime: STALE_TIME.reference,
  });
}

/** The fields of a staff member HR types in; the login account and the status have their own operations. */
export type StaffFields = Pick<
  Staff,
  | 'personId'
  | 'institutionId'
  | 'employeeCode'
  | 'designationId'
  | 'subDesignation'
  | 'employmentType'
  | 'joiningDate'
  | 'primaryDepartmentId'
  | 'qualificationDetail'
  | 'specialization'
  | 'maxPeriodsPerWeek'
>;

/** hr.CreateStaff: a staff member of an institution, for a person people already keeps. */
export function useCreateStaff() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (staff: StaffFields) => call(api.get(StaffApi).createStaff({ staff })),
    onSuccess: () => refreshStaff(qc),
  });
}

/**
 * hr.UpdateStaff under If-Match: exactly the fields given are written (an empty value clears an optional field), so
 * what another user changed in other fields stays. A stale ETag is 412.
 */
export function useUpdateStaff() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      staffId,
      etag,
      changes,
    }: {
      staffId: string;
      etag: string;
      changes: Partial<StaffFields>;
    }) =>
      call(
        api.get(StaffApi).updateStaff({
          staffId,
          ifMatch: etag,
          staffUpdate: patchOf(changes),
        }),
      ),
    // also after a refusal (409 / 412): the screen then shows the record as it is now, with its new etag (ui/05 §6)
    onSettled: (_staff, _error, { staffId }) => refreshStaff(qc, staffId),
  });
}

/** hr.DeleteStaff under If-Match; refused (409) while leave or other records still name the staff member. */
export function useDeleteStaff() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ staffId, etag }: { staffId: string; etag: string }) =>
      call(api.get(StaffApi).deleteStaff({ staffId, ifMatch: etag })),
    onSettled: (_void, error, { staffId }) => {
      // a refused delete (409 / 412) leaves the record: read it again, as it is now
      if (error) return refreshStaff(qc, staffId);
      qc.removeQueries({ queryKey: STAFF(staffId) });
      return refreshStaff(qc);
    },
  });
}

/** hr.ChangeStaffStatus under the etag the screen last read (412 when stale, 409 for a move the status forbids). */
export function useChangeStaffStatus() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ staffId, status, etag }: { staffId: string; status: StaffStatus; etag: string | null }) =>
      call(
        api.get(StaffApi).changeStaffStatus({
          staffId,
          changeStaffStatusRequest: { status, ...(etag ? { etag } : {}) },
        }),
      ),
    // also after a refusal (409 / 412): the screen then shows the record as it is now, with its new etag (ui/05 §6)
    onSettled: (_staff, _error, { staffId }) => refreshStaff(qc, staffId),
  });
}

/**
 * hr.CreateStaffPortalAccount: the staff member's login account is made in iam (or their existing one is taken) and
 * stored on the record. No invitation is asked for: iam sends none yet, and an administrator sets the first password.
 */
export function useCreateStaffPortalAccount() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ staffId }: { staffId: string }) =>
      call(api.get(StaffApi).createStaffPortalAccount({ staffId, createStaffPortalAccountRequest: {} })),
    // also after a refusal (409 / 412): the screen then shows the record as it is now, with its new etag (ui/05 §6)
    onSettled: (_staff, _error, { staffId }) => refreshStaff(qc, staffId),
  });
}

/** The two kinds of record a staff member keeps beside the profile. */
export type StaffAssignmentKind = 'departments' | 'subjects';

const ASSIGNMENTS = (kind: StaffAssignmentKind, staffId: string | null) =>
  kind === 'departments'
    ? queryKey('hr', 'staff-departments', staffId)
    : queryKey('academics', 'teacher-subjects', staffId);

/**
 * hr.ListStaffDepartments / academics.ListTeacherSubjects: the departments or subjects of one staff member, all of them.
 */
export function useStaffAssignments<K extends StaffAssignmentKind>(
  kind: K,
  staffId: string | null,
  enabled = true,
) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && staffId !== null,
    queryKey: ASSIGNMENTS(kind, staffId),
    queryFn: async (): Promise<(K extends 'departments' ? StaffDepartment : StaffSubject)[]> => {
      const staff = api.get(StaffApi);
      const id = staffId as string;
      const read =
        kind === 'departments'
          ? await allPages((page) =>
              call(staff.listStaffDepartments({ staffId: id, size: 500, ...pageQuery(page) })),
            )
          : await allPages((page) =>
              call(
                api
                  .get(TeacherSubjectApi)
                  .listTeacherSubjects({ filter: `staffId = "${id}"`, size: 500, ...pageQuery(page) }),
              ),
            );
      return read.items as (K extends 'departments' ? StaffDepartment : StaffSubject)[];
    },
    staleTime: STALE_TIME.list,
  });
}

/**
 * Adds, changes or removes one department (hr) or subject (academics' teacher profile) of a staff member: `add`
 * names the department or subject; `change` re-points a row under its etag; `remove` deletes it under its etag.
 * Naming one the staff member already has is 409.
 */
export function useStaffAssignmentStep(kind: StaffAssignmentKind) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (
      step:
        | { staffId: string; action: 'add'; targetId: string }
        | { staffId: string; action: 'change'; rowId: string; etag: string; targetId: string }
        | { staffId: string; action: 'remove'; rowId: string; etag: string },
    ): Promise<void> => {
      const staff = api.get(StaffApi);
      const { staffId } = step;
      if (kind === 'departments') {
        if (step.action === 'add')
          await call(
            staff.createStaffDepartment({ staffId, staffDepartment: { departmentId: step.targetId } }),
          );
        else if (step.action === 'change')
          await call(
            staff.updateStaffDepartment({
              staffId,
              staffDepartmentId: step.rowId,
              ifMatch: step.etag,
              staffDepartmentUpdate: { departmentId: step.targetId },
            }),
          );
        else
          await call(
            staff.deleteStaffDepartment({ staffId, staffDepartmentId: step.rowId, ifMatch: step.etag }),
          );
        return;
      }
      const subjects = api.get(TeacherSubjectApi);
      if (step.action === 'add')
        await call(subjects.createTeacherSubject({ teacherSubject: { staffId, subjectId: step.targetId } }));
      else if (step.action === 'change')
        await call(
          subjects.updateTeacherSubject({
            teacherSubjectId: step.rowId,
            ifMatch: step.etag,
            teacherSubjectUpdate: { subjectId: step.targetId },
          }),
        );
      else await call(subjects.deleteTeacherSubject({ teacherSubjectId: step.rowId, ifMatch: step.etag }));
    },
    onSettled: (_void, _error, { staffId }) => qc.invalidateQueries({ queryKey: ASSIGNMENTS(kind, staffId) }),
  });
}

/** A number of leave days as the contract stores it: up to 9999.9, in tenths. */
export const LEAVE_DAYS = /^\d{1,4}(\.\d)?$/;

/** What the leave balances list is narrowed to: one academic year, optionally one staff member or leave type. */
export interface LeaveBalanceQuery {
  academicYearId: string;
  staffId?: string;
  leaveTypeId?: string;
}

const BALANCES = queryKey('hr', 'leave-balances');

/** hr.ListLeaveBalances for one academic year: every staff member's entitlement per leave type, all of them. */
export function useLeaveBalances(query: LeaveBalanceQuery | null, enabled = true) {
  const { api } = useRuntime();
  const filter = query
    ? [
        `academicYearId = ${quoted(query.academicYearId)}`,
        ...(query.staffId ? [`staffId = ${quoted(query.staffId)}`] : []),
        ...(query.leaveTypeId ? [`leaveTypeId = ${quoted(query.leaveTypeId)}`] : []),
      ].join(' AND ')
    : '';
  return useQuery({
    enabled: enabled && query !== null,
    queryKey: queryKey('hr', 'leave-balances', { filter }),
    queryFn: () =>
      allPages((page) =>
        call(api.get(LeaveBalanceApi).listLeaveBalances({ size: 500, filter, ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** After an entitlement changed: the balances and every leave position are stale. */
async function refreshBalances(qc: QueryClient): Promise<void> {
  void qc.invalidateQueries({ queryKey: queryKey('records', 'hr.LeaveBalance') });
  await Promise.all([
    qc.invalidateQueries({ queryKey: BALANCES }),
    qc.invalidateQueries({ queryKey: queryKey('hr', 'leave-position') }),
  ]);
}

/**
 * hr.CreateLeaveBalance: a staff member's entitlement to a leave type for a year (one per staff member, type and
 * year: 409 for a second). Days are decimals, as the contract sends them.
 */
export function useCreateLeaveBalance() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (balance: {
      staffId: string;
      leaveTypeId: string;
      academicYearId: string;
      entitledDays: string;
      carriedForwardDays?: string;
    }) => call(api.get(LeaveBalanceApi).createLeaveBalance({ leaveBalance: balance })),
    onSettled: () => refreshBalances(qc),
  });
}

/**
 * hr.UpdateLeaveBalance under If-Match: the entitlement and the days carried forward; never below the days already
 * used (422), and the staff member, type and year stay (409).
 */
export function useUpdateLeaveBalance() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      leaveBalanceId,
      etag,
      entitledDays,
      carriedForwardDays,
    }: {
      leaveBalanceId: string;
      etag: string;
      entitledDays: string;
      carriedForwardDays: string;
    }) =>
      call(
        api.get(LeaveBalanceApi).updateLeaveBalance({
          leaveBalanceId,
          ifMatch: etag,
          // the column is never empty: no days carried forward is zero
          leaveBalanceUpdate: { entitledDays, carriedForwardDays: carriedForwardDays.trim() || '0' },
        }),
      ),
    onSettled: () => refreshBalances(qc),
  });
}
