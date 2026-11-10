import { useQuery } from '@tanstack/react-query';
import type { timetable } from '@mysms/sdk';
import { SubjectApi } from '@mysms/sdk/academics/apis/SubjectApi';
import { RoomApi } from '@mysms/sdk/facility/apis/RoomApi';
import { StaffApi } from '@mysms/sdk/hr/apis/StaffApi';
import { SessionApi } from '@mysms/sdk/timetable/apis/SessionApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { useSession } from './session';

export type TimetableSession = timetable.Session;

/** One day of a timetable as the reader's calendar has it, with its sessions in the order they start. */
export interface TimetableDay {
  /** The day, `YYYY-MM-DD`. */
  date: string;
  sessions: TimetableSession[];
}

/** Whose timetable: exactly the subject the reader chose (timetable.GetTimetable takes one or more of them). */
export interface TimetableSubject {
  staffId?: string;
  classSectionId?: string;
  roomId?: string;
}

const DAY_MS = 86_400_000;
const asUtc = (day: string) => new Date(`${day}T00:00:00Z`);

/**
 * Whether a string is a calendar day a timetable can be asked for: `YYYY-MM-DD` that exists (not 30 February) in
 * the years 1900–9999. An address or a deep link is checked with this before it is used.
 */
export function isTimetableDay(day: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day) || day < '1900-01-01') return false;
  const parsed = asUtc(day);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === day;
}

/** An ISO day moved by a number of days (`2026-03-31`, 1 → `2026-04-01`). */
export function shiftDay(day: string, days: number): string {
  return new Date(asUtc(day).getTime() + days * DAY_MS).toISOString().slice(0, 10);
}

/** The Monday-to-Sunday week an ISO day lies in (ISO 8601 weeks, which is how a school week is drawn). */
export function weekOf(day: string): { from: string; to: string } {
  const sinceMonday = (asUtc(day).getUTCDay() + 6) % 7;
  const from = shiftDay(day, -sinceMonday);
  return { from, to: shiftDay(from, 6) };
}

/** The calendar day of an instant in a time zone (the device's when none is given), `YYYY-MM-DD`. */
export function localDay(instant: Date | string, timeZone?: string): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...(timeZone ? { timeZone } : {}),
  }).formatToParts(typeof instant === 'string' ? new Date(instant) : instant);
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')}`;
}

/**
 * The sessions of a timetable laid out on the reader's days `from`…`to`: every day is present (an empty day shows as
 * free), a session sits on the day it starts in the reader's time zone, and a day's sessions are in start order.
 * Sessions that start outside the days are dropped — the server answers whole UTC days, so the read asks for a day
 * more on each side.
 */
export function timetableDays(
  sessions: readonly TimetableSession[],
  from: string,
  to: string,
  timeZone?: string,
): TimetableDay[] {
  const days = new Map<string, TimetableSession[]>();
  for (let day = from; day <= to; day = shiftDay(day, 1)) days.set(day, []);
  for (const session of sessions) days.get(localDay(session.startAt, timeZone))?.push(session);
  return [...days].map(([date, onDay]) => ({
    date,
    sessions: onDay.sort(
      (a, b) => Date.parse(a.startAt) - Date.parse(b.startAt) || (a.id ?? '').localeCompare(b.id ?? ''),
    ),
  }));
}

/** The staff ids of an account iam names none for (a constant, so the session selector stays stable). */
const NO_STAFF: readonly string[] = [];

/**
 * The staff records of the signed-in account (usually one; one per institution for someone employed by two schools
 * of the tenant): iam.GetMe names them (`staffIds`, the records of the account and of its person), and hr.ListStaff
 * reads those rows. When iam names none, the records linked to the account are looked up by `userId` instead: iam
 * learns a record from hr's events, so one that has not changed since iam started keeping them is not named yet.
 */
export function useMyStaff(enabled = true) {
  const { api } = useRuntime();
  const userId = useSession((s) => s.me?.user?.id ?? null);
  const staffIds = useSession((s) => (s.me ? (s.me.staffIds ?? NO_STAFF) : null));
  return useQuery({
    enabled: enabled && userId !== null && staffIds !== null,
    queryKey: queryKey('hr', 'my-staff', { userId, staffIds }),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(StaffApi).listStaff({
            size: 50,
            filter:
              staffIds && staffIds.length > 0
                ? staffIds.map((id) => `id = "${id}"`).join(' OR ')
                : `userId = "${userId as string}"`,
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.reference,
  });
}

/**
 * timetable.GetTimetable for the days `from`…`to` of the reader's calendar. The server works in UTC days, so the
 * read covers a day more on each side; {@link timetableDays} then keeps what falls on the reader's days. Several
 * subjects are read side by side and answered as one timetable — a teacher employed by two schools of the tenant
 * has a staff record in each — with a session that matches more than one of them counted once. Null or no subject
 * reads nothing.
 */
export function useTimetable(subjects: readonly TimetableSubject[] | null, from: string, to: string) {
  const { api } = useRuntime();
  return useQuery({
    enabled: subjects !== null && subjects.length > 0,
    queryKey: queryKey('timetable', 'view', { subjects, from, to }),
    queryFn: async () => {
      const reads = await Promise.all(
        (subjects ?? []).map((subject) =>
          allPages((page) =>
            call(
              api.get(SessionApi).getTimetable({
                fromDate: shiftDay(from, -1),
                toDate: shiftDay(to, 1),
                size: 500,
                ...subject,
                ...pageQuery(page),
              }),
            ),
          ),
        ),
      );
      const sessions = new Map<string, TimetableSession>();
      for (const read of reads)
        for (const view of read.items) sessions.set(view.session.id ?? view.session.startAt, view.session);
      return [...sessions.values()];
    },
    staleTime: STALE_TIME.list,
  });
}

/** Names by id from a list read, for showing a session's subject or room; empty until the read answers. */
function namesOf(items: readonly { id?: string; name: string }[] | undefined): ReadonlyMap<string, string> {
  return new Map((items ?? []).flatMap((item) => (item.id ? [[item.id, item.name] as const] : [])));
}

/** academics.ListSubjects as names by id (a school has tens of subjects, read once and kept). */
export function useSubjectNames(enabled = true): ReadonlyMap<string, string> {
  const { api } = useRuntime();
  const subjects = useQuery({
    enabled,
    queryKey: queryKey('academics', 'subject-names'),
    queryFn: () =>
      allPages((page) => call(api.get(SubjectApi).listSubjects({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.reference,
  });
  return namesOf(subjects.data?.items);
}

/** facility.ListRooms as names by id. */
export function useRoomNames(enabled = true): ReadonlyMap<string, string> {
  const { api } = useRuntime();
  const rooms = useQuery({
    enabled,
    queryKey: queryKey('facility', 'room-names'),
    queryFn: () => allPages((page) => call(api.get(RoomApi).listRooms({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.reference,
  });
  return namesOf(rooms.data?.items);
}
