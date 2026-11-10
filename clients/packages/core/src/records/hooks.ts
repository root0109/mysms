import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FIRST_PAGE, nextPage, pageQuery } from '../api/pages';
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
