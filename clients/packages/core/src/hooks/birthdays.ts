import { useMutation, useQuery } from '@tanstack/react-query';
import type { people } from '@mysms/sdk';
import { PersonApi } from '@mysms/sdk/people/apis/PersonApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type BirthdayEntry = people.BirthdayEntry;
export type BirthdayKind = people.BirthdayEntryKindEnum;
export type BirthdayRole = people.BirthdayEntryRoleEnum;
export type GreetingChannel = people.SendGreetingsRequestChannelEnum;

export interface BirthdayQuery {
  institutionId: string;
  /** Calendar days (YYYY-MM-DD), both included. */
  fromDate: string;
  toDate: string;
  kinds?: readonly BirthdayKind[];
  roles?: readonly BirthdayRole[];
}

/**
 * people.ListBirthdays: the birthdays and work anniversaries that fall in a range at one institution of the caller's
 * (the server answers only for institutions in the caller's scope), every page, in date order.
 */
export function useBirthdays(params: BirthdayQuery | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: queryKey('people', 'birthdays', params),
    queryFn: () => {
      const p = params as BirthdayQuery;
      return allPages((page) =>
        call(
          api.get(PersonApi).listBirthdays({
            institutionId: p.institutionId,
            fromDate: p.fromDate,
            toDate: p.toDate,
            ...(p.kinds?.length ? { kinds: [...p.kinds] } : {}),
            ...(p.roles?.length ? { roles: [...p.roles] } : {}),
            size: 500,
            ...pageQuery(page),
          }),
        ),
      );
    },
    enabled: params !== null,
    staleTime: STALE_TIME.list,
  });
}

/** people.SendGreetings: one message from a template to each chosen person; says how many were queued and skipped. */
export function useSendGreetings() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: (body: { personIds: readonly string[]; templateId: string; channel: GreetingChannel }) =>
      call(
        api.get(PersonApi).sendGreetings({
          sendGreetingsRequest: {
            personIds: [...body.personIds],
            templateId: body.templateId,
            channel: body.channel,
          },
        }),
      ),
  });
}
