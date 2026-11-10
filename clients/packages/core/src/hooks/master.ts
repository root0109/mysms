import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { master } from '@mysms/sdk';
import { CountryApi } from '@mysms/sdk/master/apis/CountryApi';
import { FIRST_PAGE, nextPage, pageQuery } from '../api/pages';
import { call, callWithEtag, type WithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Country = master.Country;
export type CountryUpdate = master.CountryUpdate;
/** The writable fields of a new country (output-only fields are set by the server). */
export type CountryCreate = master.CountryApiCreateCountryRequest['country'];

/** The parameters every list operation takes (api/README "Pagination"). */
export interface ListParams {
  /** One-based page number (default 1). */
  page?: number;
  size?: number;
  /** The cursor of the previous page's next link. */
  cursor?: string;
  filter?: string;
  /** Sort criteria `field,asc|desc`, in priority order. */
  sort?: string[];
}

/** master.ListCountries — reference data, cached for 10 minutes (ui/05 §4). */
export function useCountries(params: ListParams = {}) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('master', 'countries', params),
    queryFn: () => call(api.get(CountryApi).listCountries(params)),
    staleTime: STALE_TIME.reference,
  });
}

/** master.ListCountries as an infinite list: pages follow each response's next link (ui/05 §4 list convention). */
export function useInfiniteCountries(params: Omit<ListParams, 'page' | 'cursor'> = {}) {
  const { api } = useRuntime();
  return useInfiniteQuery({
    queryKey: queryKey('master', 'countries', { ...params, infinite: true }),
    initialPageParam: FIRST_PAGE,
    queryFn: ({ pageParam }) =>
      call(api.get(CountryApi).listCountries({ ...params, ...pageQuery(pageParam) })),
    getNextPageParam: (last) => nextPage(last.paging),
    staleTime: STALE_TIME.reference,
  });
}

/** master.GetCountry with its ETag for a later If-Match. */
export function useCountry(countryId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('master', 'country', countryId),
    queryFn: () => callWithEtag(api.get(CountryApi).getCountryRaw({ countryId: countryId as string })),
    enabled: countryId !== null,
    staleTime: STALE_TIME.detail,
  });
}

export function useCreateCountry() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (country: CountryCreate) => callWithEtag(api.get(CountryApi).createCountryRaw({ country })),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKey('master', 'countries') }),
  });
}

/** master.UpdateCountry — partial update guarded by If-Match; a stale ETag surfaces as ApiError 412. */
export function useUpdateCountry() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ countryId, etag, changes }: { countryId: string; etag: string; changes: CountryUpdate }) =>
      callWithEtag(
        api.get(CountryApi).updateCountryRaw({
          countryId,
          ifMatch: etag,
          updateMask: Object.keys(changes).join(','),
          countryUpdate: changes,
        }),
      ),
    onSuccess: (result: WithEtag<Country>, { countryId }) => {
      qc.setQueryData(queryKey('master', 'country', countryId), result);
      void qc.invalidateQueries({ queryKey: queryKey('master', 'countries') });
    },
  });
}

export function useDeleteCountry() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ countryId, etag }: { countryId: string; etag: string }) => {
      await call(api.get(CountryApi).deleteCountry({ countryId, ifMatch: etag }));
    },
    onSuccess: (_void, { countryId }) => {
      qc.removeQueries({ queryKey: queryKey('master', 'country', countryId) });
      void qc.invalidateQueries({ queryKey: queryKey('master', 'countries') });
    },
  });
}
