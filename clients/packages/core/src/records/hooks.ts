import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { allPages, FIRST_PAGE, nextPage, pageQuery } from '../api/pages';
import { RESOURCES } from '../generated/resources';
import { STALE_TIME } from '../query/queryClient';
import { queryKey } from '../query/keys';
import { useRuntime } from '../react/useRuntime';
import {
  createRecord,
  deleteRecord,
  getRecord,
  listRecords,
  runAction,
  updateRecord,
  type RecordListParams,
} from './api';
import type { ActionDef, ResourceDef } from './types';

const KEY = 'records';

/** The rows of one resource as an infinite list (pages follow each response's next link, ui/05 §4). */
export function useInfiniteRecords(
  def: ResourceDef | undefined,
  params: Omit<RecordListParams, 'page' | 'cursor'> = {},
) {
  const { api } = useRuntime();
  return useInfiniteQuery({
    queryKey: queryKey(KEY, def?.key ?? '-', { ...params, infinite: true }),
    initialPageParam: FIRST_PAGE,
    queryFn: ({ pageParam }) => listRecords(api, def as ResourceDef, { ...params, ...pageQuery(pageParam) }),
    getNextPageParam: (last) => nextPage(last.paging),
    // a partitioned resource is listed only with its range (the server answers 400 without one)
    enabled:
      def !== undefined &&
      (!def.parent || Boolean(params.parentId)) &&
      (!def.partition || (params.filter ?? '').includes(`${def.partition.field} >=`)),
    staleTime: STALE_TIME.list,
  });
}

/** One row with its ETag for a later If-Match. */
export function useRecord(def: ResourceDef | undefined, id: string | null, parentId?: string) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey(KEY, `${def?.key ?? '-'}#item`, { id, parentId }),
    queryFn: () => getRecord(api, def as ResourceDef, id as string, parentId),
    enabled: def !== undefined && id !== null && def.ops.Get !== undefined,
    staleTime: STALE_TIME.detail,
  });
}

function useInvalidate(def: ResourceDef) {
  const qc = useQueryClient();
  return () => qc.invalidateQueries({ queryKey: queryKey(KEY, def.key) });
}

export function useCreateRecord(def: ResourceDef) {
  const { api } = useRuntime();
  const invalidate = useInvalidate(def);
  return useMutation({
    mutationFn: ({ values, parentId }: { values: Record<string, unknown>; parentId?: string }) =>
      createRecord(api, def, values, parentId),
    onSuccess: () => invalidate(),
  });
}

/** A partial update under If-Match; a stale ETag surfaces as ApiError 412. */
export function useUpdateRecord(def: ResourceDef) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const invalidate = useInvalidate(def);
  return useMutation({
    mutationFn: (v: { id: string; etag: string; values: Record<string, unknown>; parentId?: string }) =>
      updateRecord(api, def, v.id, v.etag, v.values, v.parentId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKey(KEY, `${def.key}#item`) });
      return invalidate();
    },
  });
}

export function useDeleteRecord(def: ResourceDef) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const invalidate = useInvalidate(def);
  return useMutation({
    mutationFn: (v: { id: string; etag: string; parentId?: string }) =>
      deleteRecord(api, def, v.id, v.etag, v.parentId),
    onSuccess: () => {
      // the row is gone: drop its cached detail instead of refetching a 404
      qc.removeQueries({ queryKey: queryKey(KEY, `${def.key}#item`) });
      return invalidate();
    },
  });
}

/** A state transition on one row; refreshes the row and the lists. */
export function useRecordAction(def: ResourceDef) {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const invalidate = useInvalidate(def);
  return useMutation({
    mutationFn: (v: {
      action: ActionDef;
      id: string;
      etag: string | null;
      reason?: string;
      params?: Record<string, unknown>;
      parentId?: string;
    }) => runAction(api, v.action, v.id, v.etag, { reason: v.reason, params: v.params }, v.parentId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKey(KEY, `${def.key}#item`) });
      return invalidate();
    },
  });
}

/** The record type of api/standard-resources.json named `<module>.<Resource>`; undefined for an unknown name. */
export function resourceDef(key: string): ResourceDef | undefined {
  return RESOURCES.find((r) => r.key === key);
}

/**
 * Every row of one record type that meets `params` (all pages), for a page that shows a small set whole: a role's
 * permissions, a route's stops, a title's copies. Off while `enabled` is false or a nested type has no parent.
 */
export function useAllRecords(
  key: string,
  params: Omit<RecordListParams, 'page' | 'cursor' | 'size'> = {},
  enabled = true,
) {
  const { api } = useRuntime();
  const def = resourceDef(key);
  return useQuery({
    queryKey: queryKey(KEY, key, { ...params, all: true }),
    queryFn: () =>
      allPages((page) => listRecords(api, def as ResourceDef, { ...params, size: 500, ...pageQuery(page) })),
    enabled: enabled && def !== undefined && (!def.parent || Boolean(params.parentId)),
    staleTime: STALE_TIME.list,
  });
}

/**
 * How many rows of a record type meet a filter, counted up to `cap` (a dashboard says "50+" past it): one page of
 * `cap` rows, no more. Off while `enabled` is false.
 */
export function useRecordTotal(key: string, filter: string | undefined, enabled = true, cap = 50) {
  const { api } = useRuntime();
  const def = resourceDef(key);
  return useQuery({
    queryKey: queryKey(KEY, key, { filter, total: cap }),
    queryFn: async () => {
      const page = await listRecords(api, def as ResourceDef, { size: cap, ...(filter ? { filter } : {}) });
      return { count: page.data.length, more: page.paging.next !== undefined };
    },
    enabled: enabled && def !== undefined && !def.parent,
    staleTime: STALE_TIME.list,
  });
}
