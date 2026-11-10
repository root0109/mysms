import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { comms } from '@mysms/sdk';
import { NotificationPreferenceSetApi } from '@mysms/sdk/comms/apis/NotificationPreferenceSetApi';
import { allPages, pageQuery } from '../api/pages';
import { call, callWithEtag, type WithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type NotificationPreference = comms.NotificationPreference;
export type NotificationPreferenceSet = comms.NotificationPreferenceSet;
export type NotificationPreferenceUpdate = comms.NotificationPreferenceUpdate;

/** Notification preference sets belonging to the signed-in account only. */
export function useNotificationPreferenceSets(userId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('comms', 'notification-preference-sets', userId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(NotificationPreferenceSetApi).listNotificationPreferenceSets({
            size: 500,
            filter: `userId = "${userId as string}"`,
            sort: ['createTime'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: userId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** Preferences in one role set, including the server ETag copied onto each row. */
export function useNotificationPreferences(preferenceSetId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('comms', 'notification-preferences', preferenceSetId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(NotificationPreferenceSetApi).listNotificationPreferences({
            notificationPreferenceSetId: preferenceSetId as string,
            size: 500,
            sort: ['createTime'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: preferenceSetId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** comms.UpdateNotificationPreference — partial update guarded by the row ETag. */
export function useUpdateNotificationPreference() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      preferenceSetId,
      preferenceId,
      etag,
      changes,
    }: {
      preferenceSetId: string;
      preferenceId: string;
      etag: string;
      changes: NotificationPreferenceUpdate;
    }): Promise<WithEtag<NotificationPreference>> =>
      callWithEtag(
        api.get(NotificationPreferenceSetApi).updateNotificationPreferenceRaw({
          notificationPreferenceSetId: preferenceSetId,
          notificationPreferenceId: preferenceId,
          ifMatch: etag,
          updateMask: Object.keys(changes).join(','),
          notificationPreferenceUpdate: changes,
        }),
      ),
    onSuccess: (_result, variables) => {
      void qc.invalidateQueries({
        queryKey: queryKey('comms', 'notification-preferences', variables.preferenceSetId),
      });
    },
  });
}
