import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { iam } from '@mysms/sdk';
import { SettingDefinitionApi } from '@mysms/sdk/iam/apis/SettingDefinitionApi';
import { SettingValueApi } from '@mysms/sdk/iam/apis/SettingValueApi';
import { UserAccountApi } from '@mysms/sdk/iam/apis/UserAccountApi';
import { useMemo } from 'react';
import { call, callWithEtag, type WithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { preferencesFrom, type UiPreferences } from '../settings/preferences';
import type { ListParams } from './master';
import { useSession } from './session';

export type SettingDefinition = iam.SettingDefinition;
export type SettingValue = iam.SettingValue;
export type SettingValueUpdate = iam.SettingValueUpdate;
/** The writable fields of a new setting value. */
export type SettingValueCreate = iam.SettingValueApiCreateSettingValueRequest['settingValue'];

/** The caller's effective settings, loaded with the session (iam.ListMySettings). */
export function useMySettings() {
  return useSession((s) => s.settings);
}

/** Typed interface preferences from the effective settings; defaults when none are loaded. */
export function usePreferences(): UiPreferences {
  const settings = useSession((s) => s.settings);
  return useMemo(() => preferencesFrom(settings), [settings]);
}

/** iam.ListSettingDefinitions — the product catalogue (labels, types, allowed values), cached like reference data. */
export function useSettingDefinitions() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('iam', 'setting-definitions'),
    queryFn: () => call(api.get(SettingDefinitionApi).listSettingDefinitions({ pageSize: 500 })),
    staleTime: STALE_TIME.reference,
  });
}

/** iam.SetMySetting; the session's settings are re-read so the change applies at once. */
export function useSetMySetting() {
  const { api, session } = useRuntime();
  return useMutation({
    mutationFn: ({ settingKey, value }: { settingKey: string; value: string }) =>
      call(api.get(UserAccountApi).setMySetting({ settingKey, setMySettingRequest: { value } })),
    onSuccess: () => session.reloadSettings(),
  });
}

/** Re-reads the effective settings (iam.ListMySettings), e.g. when they failed to load with the session. */
export function useReloadMySettings() {
  const { session } = useRuntime();
  return useMutation({ mutationFn: () => session.reloadSettings() });
}

/** iam.ResetMySetting: the broader scopes apply again. */
export function useResetMySetting() {
  const { api, session } = useRuntime();
  return useMutation({
    mutationFn: async (settingKey: string) => {
      await call(api.get(UserAccountApi).resetMySetting({ settingKey }));
    },
    onSuccess: () => session.reloadSettings(),
  });
}

/** iam.ListSettingValues — the values the caller may see (their scopes only, enforced by the server). */
export function useSettingValues(params: ListParams = {}) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('iam', 'setting-values', params),
    queryFn: () => call(api.get(SettingValueApi).listSettingValues(params)),
    staleTime: STALE_TIME.list,
  });
}

/** iam.ListSettingValues page by page (nextPageToken), for the administration list. */
export function useInfiniteSettingValues(params: Omit<ListParams, 'pageToken'> = {}) {
  const { api } = useRuntime();
  return useInfiniteQuery({
    queryKey: queryKey('iam', 'setting-values', { ...params, infinite: true }),
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) =>
      call(
        api.get(SettingValueApi).listSettingValues(pageParam ? { ...params, pageToken: pageParam } : params),
      ),
    getNextPageParam: (last) => last.nextPageToken || undefined,
    staleTime: STALE_TIME.list,
  });
}

/** After an administrator changes a value, the caller's own effective settings may change too. */
function useSettingValuesChanged() {
  const { session } = useRuntime();
  const qc = useQueryClient();
  return () => {
    void qc.invalidateQueries({ queryKey: queryKey('iam', 'setting-values') });
    void session.reloadSettings();
  };
}

export function useCreateSettingValue() {
  const { api } = useRuntime();
  const changed = useSettingValuesChanged();
  return useMutation({
    mutationFn: (settingValue: SettingValueCreate) =>
      callWithEtag(api.get(SettingValueApi).createSettingValueRaw({ settingValue })),
    onSuccess: changed,
  });
}

/** iam.UpdateSettingValue — value and lock under If-Match; a stale ETag surfaces as ApiError 412. */
export function useUpdateSettingValue() {
  const { api } = useRuntime();
  const changed = useSettingValuesChanged();
  return useMutation({
    mutationFn: ({
      settingValueId,
      etag,
      changes,
    }: {
      settingValueId: string;
      etag: string;
      changes: SettingValueUpdate;
    }): Promise<WithEtag<SettingValue>> =>
      callWithEtag(
        api.get(SettingValueApi).updateSettingValueRaw({
          settingValueId,
          ifMatch: etag,
          updateMask: Object.keys(changes).join(','),
          settingValueUpdate: changes,
        }),
      ),
    onSuccess: changed,
  });
}

export function useDeleteSettingValue() {
  const { api } = useRuntime();
  const changed = useSettingValuesChanged();
  return useMutation({
    mutationFn: async ({ settingValueId, etag }: { settingValueId: string; etag: string }) => {
      await call(api.get(SettingValueApi).deleteSettingValue({ settingValueId, ifMatch: etag }));
    },
    onSuccess: changed,
  });
}
