import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { master } from '@mysms/sdk';
import { CountryApi } from '@mysms/sdk/master/apis/CountryApi';
import { call, callWithEtag, type WithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Country = master.Country;
export type CountryUpdate = master.CountryUpdate;
/** The writable fields of a new country (output-only fields are set by the server). */
export type CountryCreate = master.CountryApiCreateCountryRequest['country'];

export interface ListParams {
  pageSize?: number;
  pageToken?: string;
  filter?: string;
  orderBy?: string;
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

/** master.ListCountries as an infinite list: pages follow nextPageToken (ui/05 §4 list convention). */
export function useInfiniteCountries(params: Omit<ListParams, 'pageToken'> = {}) {
  const { api } = useRuntime();
  return useInfiniteQuery({
    queryKey: queryKey('master', 'countries', { ...params, infinite: true }),
    initialPageParam: undefined as string | undefined,
    queryFn: ({ pageParam }) =>
      call(api.get(CountryApi).listCountries(pageParam ? { ...params, pageToken: pageParam } : params)),
    getNextPageParam: (last) => last.nextPageToken || undefined,
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
