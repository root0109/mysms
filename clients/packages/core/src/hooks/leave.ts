import { useMutation, useQueries, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { hr, org } from '@mysms/sdk';
import { LeaveApplicationApi } from '@mysms/sdk/hr/apis/LeaveApplicationApi';
import { LeaveTypeApi } from '@mysms/sdk/hr/apis/LeaveTypeApi';
import { StaffApi } from '@mysms/sdk/hr/apis/StaffApi';
import { AcademicYearApi } from '@mysms/sdk/org/apis/AcademicYearApi';
import { PersonApi } from '@mysms/sdk/people/apis/PersonApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { NAME_CHUNK, anyOf } from './attendance';

export type LeaveApplication = hr.LeaveApplication;
export type LeaveType = hr.LeaveType;
export type AcademicYear = org.AcademicYear;
export type LeaveDayPart = NonNullable<LeaveApplication['dayPart']>;
export type LeaveStep = 'approve' | 'reject' | 'cancel';

/** One line of a staff member's leave position (hr.GetStaffLeaveBalance), as numbers. */
export interface LeavePosition {
  leaveTypeId: string;
  entitled: number;
  taken: number;
  pending: number;
  available: number;
}

/** A leave the staff member asks for: whole calendar days, or one part of each day. */
export interface LeaveRequest {
  institutionId: string;
  academicYearId: string;
  staffId: string;
  leaveTypeId?: string;
  /** First and last day, `YYYY-MM-DD`. */
  from: string;
  to: string;
  dayPart: LeaveDayPart;
  remarks?: string;
}

export const LEAVE_DAY_PARTS: readonly LeaveDayPart[] = ['full_day', 'forenoon', 'afternoon'];
const ISO_DAY = /^\d{4}-\d{2}-\d{2}$/;
const DAY_MS = 86_400_000;
/** A calendar day as the instant the server counts leave days from: its UTC midnight. */
const dayStart = (day: string) => `${day}T00:00:00Z`;

/**
 * The days a leave from `from` to `to` takes, as the server counts them (api/state-machines.md "LeaveApplication"):
 * both days included, halved for a half-day part. Null while the days are not a valid range.
 */
export function leaveDays(from: string, to: string, dayPart: LeaveDayPart): number | null {
  if (!ISO_DAY.test(from) || !ISO_DAY.test(to)) return null;
  const span = (Date.parse(dayStart(to)) - Date.parse(dayStart(from))) / DAY_MS;
  if (Number.isNaN(span) || span < 0) return null;
  return (span + 1) * (dayPart === 'full_day' ? 1 : 0.5);
}

/**
 * How an application of the reader's can be called off (hr.CancelLeave): a pending one is withdrawn, an approved one
 * is cancelled while it has not started (its days go back to the balance); null for anything else, including an
 * approved leave that has begun, which the server refuses.
 */
export function cancellation(
  application: Pick<LeaveApplication, 'status' | 'fromAt'>,
  now: Date = new Date(),
): 'withdraw' | 'cancel' | null {
  if (application.status === 'pending') return 'withdraw';
  if (application.status === 'approved' && Date.parse(application.fromAt) > now.getTime()) return 'cancel';
  return null;
}

/** The academic year of an institution that a day lies in; undefined when none of its years covers the day. */
export function academicYearOn(
  years: readonly AcademicYear[],
  institutionId: string,
  day: string,
): AcademicYear | undefined {
  return years.find(
    (year) => year.institutionId === institutionId && year.startDate <= day && day <= year.endDate,
  );
}

const POSITION = (staffId: string | null, academicYearId: string | null) =>
  queryKey('hr', 'leave-position', { staffId, academicYearId });
const MINE = (staffId: string | null) => queryKey('hr', 'leave-applications', { staffId });
const QUEUE = queryKey('hr', 'leave-applications', { status: 'pending' });

/** After an application or a decision: positions, both lists and the standard screens of applications are stale. */
async function refreshLeave(qc: QueryClient): Promise<void> {
  void qc.invalidateQueries({ queryKey: queryKey('records', 'hr.LeaveApplication') });
  void qc.invalidateQueries({ queryKey: queryKey('records', 'hr.LeaveApplication#item') });
  void qc.invalidateQueries({ queryKey: queryKey('records', 'hr.LeaveBalance') });
  await Promise.all([
    qc.invalidateQueries({ queryKey: queryKey('hr', 'leave-position') }),
    qc.invalidateQueries({ queryKey: queryKey('hr', 'leave-applications') }),
  ]);
}

/** org.ListAcademicYears: the years the caller may see (a handful per institution, read once and kept). */
export function useAcademicYears(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('org', 'academic-years', { all: true }),
    queryFn: () =>
      allPages((page) => call(api.get(AcademicYearApi).listAcademicYears({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.reference,
  });
}

/** hr.ListLeaveTypes of an institution: the active types staff can apply for, in the order the school lists them. */
export function useLeaveTypes(institutionId: string | null, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled: enabled && institutionId !== null,
    queryKey: queryKey('hr', 'leave-types', institutionId),
    queryFn: async () => {
      const read = await allPages((page) =>
        call(
          api.get(LeaveTypeApi).listLeaveTypes({
            size: 500,
            filter: `institutionId = "${institutionId as string}" AND isActive = true`,
            ...pageQuery(page),
          }),
        ),
      );
      return read.items.sort(
        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name),
      );
    },
    staleTime: STALE_TIME.reference,
  });
}

/** hr.GetStaffLeaveBalance: entitled, taken, pending and available days per leave type for one academic year. */
export function useLeavePosition(staffId: string | null, academicYearId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: staffId !== null && academicYearId !== null,
    queryKey: POSITION(staffId, academicYearId),
    queryFn: async (): Promise<LeavePosition[]> => {
      const read = await allPages((page) =>
        call(
          api.get(StaffApi).getStaffLeaveBalance({
            staffId: staffId as string,
            academicYearId: academicYearId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      );
      return read.items.map((line) => ({
        leaveTypeId: line.leaveTypeId,
        entitled: Number(line.entitledDays),
        taken: Number(line.takenDays),
        pending: Number(line.pendingDays),
        available: Number(line.availableDays),
      }));
    },
    staleTime: STALE_TIME.list,
  });
}

/** hr.ListLeaveApplications of one staff member, newest first — all of them, page by page. */
export function useLeaveApplications(staffId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: staffId !== null,
    queryKey: MINE(staffId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(LeaveApplicationApi).listLeaveApplications({
            size: 100,
            filter: `staffId = "${staffId as string}"`,
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** hr.ListLeaveApplications that wait for a decision, oldest first: the approver's queue. */
export function usePendingLeave(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: QUEUE,
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(LeaveApplicationApi).listLeaveApplications({
            size: 100,
            filter: 'status = "pending"',
            sort: ['createTime'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/**
 * The display names of staff members, by staff id: a name lives with the person record in another module, so they
 * are read forty staff at a time with two list reads (hr.ListStaff, people.ListPersons), inside the caller's own
 * scope. A staff member whose name the caller may not read has none; screens fall back to a neutral label.
 */
export function useStaffNames(staffIds: readonly string[], enabled = true): ReadonlyMap<string, string> {
  const { api } = useRuntime();
  const ids = enabled ? [...new Set(staffIds)] : [];
  const chunks: string[][] = [];
  for (let start = 0; start < ids.length; start += NAME_CHUNK)
    chunks.push(ids.slice(start, start + NAME_CHUNK));
  return useQueries({
    queries: chunks.map((chunk) => ({
      queryKey: queryKey('hr', 'staff-names', chunk),
      queryFn: async (): Promise<[string, string][]> => {
        const staff = await call(api.get(StaffApi).listStaff({ size: NAME_CHUNK, filter: anyOf(chunk) }));
        const personOf = new Map<string, string>();
        for (const member of staff.data) if (member.id) personOf.set(member.id, member.personId);
        const personIds = [...new Set(personOf.values())];
        if (personIds.length === 0) return [];
        const persons = await call(
          api.get(PersonApi).listPersons({ size: NAME_CHUNK, filter: anyOf(personIds) }),
        );
        const nameOf = new Map<string, string>();
        for (const person of persons.data) if (person.id) nameOf.set(person.id, person.displayName);
        return [...personOf].flatMap(([staffId, personId]) => {
          const name = nameOf.get(personId);
          return name ? [[staffId, name] as [string, string]] : [];
        });
      },
      staleTime: STALE_TIME.reference,
      retry: false,
    })),
    combine: (results) => new Map(results.flatMap((result) => result.data ?? [])),
  });
}

/** hr.CreateLeaveApplication: the leave is asked for and waits for a decision. */
export function useApplyForLeave() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ from, to, remarks, leaveTypeId, ...rest }: LeaveRequest) =>
      call(
        api.get(LeaveApplicationApi).createLeaveApplication({
          leaveApplication: {
            ...rest,
            ...(leaveTypeId ? { leaveTypeId } : {}),
            fromAt: dayStart(from),
            toAt: dayStart(to),
            ...(remarks?.trim() ? { remarks: remarks.trim() } : {}),
          },
        }),
      ),
    onSettled: () => refreshLeave(qc),
  });
}

/**
 * hr.ApproveLeave / RejectLeave / CancelLeave under the etag the screen last read. Approving counts the days
 * against the staff member's balance and is refused (409) when too few are left; the reason is kept with the
 * decision.
 */
export function useLeaveStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      applicationId,
      step,
      etag,
      reason,
    }: {
      applicationId: string;
      step: LeaveStep;
      etag: string | null;
      reason?: string;
    }) => {
      const applications = api.get(LeaveApplicationApi);
      const body = { ...(etag ? { etag } : {}), ...(reason?.trim() ? { reason: reason.trim() } : {}) };
      const leaveApplicationId = applicationId;
      if (step === 'approve')
        return call(applications.approveLeave({ leaveApplicationId, approveLeaveRequest: body }));
      if (step === 'reject')
        return call(applications.rejectLeave({ leaveApplicationId, rejectLeaveRequest: body }));
      return call(applications.cancelLeave({ leaveApplicationId, cancelLeaveRequest: body }));
    },
    onSettled: () => refreshLeave(qc),
  });
}
