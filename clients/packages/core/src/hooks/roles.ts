import { useQuery } from '@tanstack/react-query';
import type { iam } from '@mysms/sdk';
import { PermissionApi } from '@mysms/sdk/iam/apis/PermissionApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Permission = iam.Permission;

/**
 * iam.ListPermissions whole, in code order: the catalogue a role's permissions are chosen from (fixed by the
 * platform, so it is read once and kept).
 */
export function usePermissionCatalogue(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('iam', 'permissions'),
    queryFn: async () => {
      const read = await allPages((page) =>
        call(api.get(PermissionApi).listPermissions({ size: 500, sort: ['code'], ...pageQuery(page) })),
      );
      return read.items;
    },
    staleTime: STALE_TIME.reference,
  });
}
