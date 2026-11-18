import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { lms } from '@mysms/sdk';
import { ConferencingAccountApi } from '@mysms/sdk/lms/apis/ConferencingAccountApi';
import { LiveClassApi } from '@mysms/sdk/lms/apis/LiveClassApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type LiveClass = lms.LiveClass;
export type LiveClassParticipant = lms.LiveClassParticipant;
export type ConferencingAccount = lms.ConferencingAccount;
export type ScheduleLiveClassRequest = lms.ScheduleLiveClassRequest;

const LIVE_CLASSES = (cohortId: string | null) => queryKey('lms', 'live-classes', cohortId);
const PARTICIPANTS = (liveClassId: string | null) => queryKey('lms', 'live-class-participants', liveClassId);
const ACCOUNTS = queryKey('lms', 'conferencing-accounts', null);

/** lms.ListLiveClasses, newest class first; for one cohort when given. */
export function useLiveClasses(cohortId?: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: LIVE_CLASSES(cohortId ?? null),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(LiveClassApi).listLiveClasses({
            size: 200,
            sort: ['classDate desc'],
            ...(cohortId ? { filter: `cohortId = "${cohortId}"` } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** lms.ListLiveClassParticipants of one class (who joined, who attended). */
export function useLiveClassParticipants(liveClassId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: PARTICIPANTS(liveClassId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(LiveClassApi).listLiveClassParticipants({
            liveClassId: liveClassId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: liveClassId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListConferencingAccounts: the tenant's meeting-provider accounts (for the schedule form). */
export function useConferencingAccounts(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: ACCOUNTS,
    queryFn: () =>
      allPages((page) =>
        call(api.get(ConferencingAccountApi).listConferencingAccounts({ size: 100, ...pageQuery(page) })),
      ),
    enabled,
    staleTime: STALE_TIME.reference,
  });
}

/** lms.ScheduleLiveClass: creates the meeting at the provider and the class in the cohort. */
export function useScheduleLiveClass() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ request }: { request: ScheduleLiveClassRequest }) =>
      call(api.get(LiveClassApi).scheduleLiveClass({ scheduleLiveClassRequest: request })),
    onSettled: (_r, _e, variables) =>
      qc.invalidateQueries({ queryKey: LIVE_CLASSES(variables.request.cohortId ?? null).slice(0, 2) }),
  });
}

/** lms.JoinLiveClass: the member's join link (their first ask is recorded). */
export function useJoinLiveClass() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ liveClassId }: { liveClassId: string }) =>
      call(api.get(LiveClassApi).joinLiveClass({ liveClassId, body: {} })),
  });
}

/** lms.GetLiveClassStartUrl: a fresh host start link. */
export function useLiveClassStartUrl() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ liveClassId }: { liveClassId: string }) =>
      call(api.get(LiveClassApi).getLiveClassStartUrl({ liveClassId })),
  });
}

/** lms.EndLiveClass / SyncLiveClassAttendance / RetryLiveClassInvites on one class. */
export function useLiveClassStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({
      liveClassId,
      step,
    }: {
      liveClassId: string;
      step: 'end' | 'syncAttendance' | 'retryInvites';
    }): Promise<unknown> => {
      const classes = api.get(LiveClassApi);
      if (step === 'end') return call(classes.endLiveClass({ liveClassId, body: {} }));
      if (step === 'syncAttendance') return call(classes.syncLiveClassAttendance({ liveClassId, body: {} }));
      return call(classes.retryLiveClassInvites({ liveClassId, body: {} }));
    },
    onSettled: (_r, _e, variables) =>
      Promise.all([
        qc.invalidateQueries({ queryKey: LIVE_CLASSES(null).slice(0, 2) }),
        qc.invalidateQueries({ queryKey: PARTICIPANTS(variables.liveClassId) }),
      ]),
  });
}
