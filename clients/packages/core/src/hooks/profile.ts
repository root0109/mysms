import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { people } from '@mysms/sdk';
import { PersonApi } from '@mysms/sdk/people/apis/PersonApi';
import { patchOf } from '../api/patch';
import { callWithEtag, type WithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { useSession } from './session';

export type Person = people.Person;
export type MyPersonUpdate = people.MyPersonUpdate;

/** people.GetMyPerson with its ETag — only when the account is linked to a person record. */
export function useMyPerson() {
  const { api } = useRuntime();
  const linked = useSession((s) => Boolean(s.me?.user?.personId));
  return useQuery({
    queryKey: queryKey('people', 'me'),
    queryFn: () => callWithEtag(api.get(PersonApi).getMyPersonRaw({})),
    enabled: linked,
    staleTime: STALE_TIME.detail,
  });
}

/** people.UpdateMyPerson — contact fields under If-Match; only the fields sent change, null clears one. */
export function useUpdateMyPerson() {
  const { api, session } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ etag, changes }: { etag: string; changes: MyPersonUpdate }): Promise<WithEtag<Person>> =>
      callWithEtag(
        api.get(PersonApi).updateMyPersonRaw({
          ifMatch: etag,
          myPersonUpdate: patchOf(changes),
        }),
      ),
    onSuccess: (result) => {
      qc.setQueryData(queryKey('people', 'me'), result);
      void session.reloadMe();
    },
  });
}
