import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { MessageKey } from '@mysms/i18n';
import type { privacy } from '@mysms/sdk';
import { MyConsentApi } from '@mysms/sdk/privacy/apis/MyConsentApi';
import { useRef } from 'react';
import { newIdempotencyKey } from '../api/ids';
import { isApiError } from '../api/problem';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { retriesSameAttempt } from './admission';

export type MyConsent = privacy.MyConsent;
export type MyNotice = privacy.MyNotice;
/** A width a consent reaches (class < institution < inter_school < public), or tenant for a whole-school purpose. */
export type ConsentScope = privacy.MyConsentAllowedScopesEnum;
/** How the signed-in person stands to a row's subject: deciding for themself, for a child, or reading as a minor. */
export type ConsentCapacity = privacy.MyConsentCapacityEnum;
export type ConsentDecision = privacy.RecordMyConsentRequestDecisionEnum;

const MY_CONSENTS = queryKey('privacy', 'my-consents');

/**
 * ACC-13: the consent centre (privacy.ListMyConsents) — one row per person the signed-in user sees (themself, each
 * child they are the legal guardian of) and purpose the school has switched on. Never another guardian's decision.
 */
export function useMyConsents(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: MY_CONSENTS,
    queryFn: () => call(api.get(MyConsentApi).listMyConsents()),
    staleTime: STALE_TIME.list,
  });
}

/**
 * ACC-13: the notice a decision is made under (privacy.GetMyNotice), in the language asked for; the server answers
 * English when the school has no such translation, and the answer names the language it is in.
 */
export function useMyNotice(noticeCode: string | null, language: string) {
  const { api } = useRuntime();
  return useQuery({
    enabled: noticeCode !== null,
    queryKey: queryKey('privacy', 'my-notice', { noticeCode, language }),
    queryFn: () => call(api.get(MyConsentApi).getMyNotice({ noticeCode: noticeCode as string, language })),
    staleTime: STALE_TIME.detail,
  });
}

/** A decision as the consent centre sends it: the version and language of the notice actually shown. */
export interface ConsentAnswer {
  subjectPersonId: string;
  purposeCode: string;
  scope: ConsentScope;
  decision: ConsentDecision;
  notice: Pick<MyNotice, 'noticeVersionId' | 'language'>;
}

/**
 * The Idempotency-Key of one logical attempt: a request repeated after a failure that may be retried (see
 * {@link retriesSameAttempt}) goes out with the key it had; a different request — another answer, width or person —
 * or any request after a success or an answer the server keeps gets a new key, so the server never sees one key
 * with two bodies.
 */
function useAttemptKey() {
  const last = useRef<{ request: string; key: string } | null>(null);
  return {
    keyFor: (request: unknown): string => {
      const body = JSON.stringify(request);
      const key = last.current?.request === body ? last.current.key : newIdempotencyKey();
      last.current = { request: body, key };
      return key;
    },
    settled: (error: unknown) => {
      if (error === null || !retriesSameAttempt(error)) last.current = null;
    },
  };
}

/** privacy.RecordMyConsent: the user's decision for themself or a child; a newer one replaces their live one. */
export function useRecordMyConsent() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const key = useAttemptKey();
  return useMutation({
    mutationFn: ({ subjectPersonId, purposeCode, scope, decision, notice }: ConsentAnswer) => {
      const recordMyConsentRequest = {
        subjectPersonId,
        purposeCode,
        scope,
        decision,
        noticeVersionId: notice.noticeVersionId,
        noticeLanguage: notice.language,
      };
      return call(
        api.get(MyConsentApi).recordMyConsent({
          idempotencyKey: key.keyFor(recordMyConsentRequest),
          recordMyConsentRequest,
        }),
      );
    },
    onSettled: (_data, error) => {
      key.settled(error);
      void qc.invalidateQueries({ queryKey: MY_CONSENTS });
    },
  });
}

/** privacy.WithdrawMyConsent: ends one of the user's own live decisions, guarded by its etag (412 when stale). */
export function useWithdrawMyConsent() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const key = useAttemptKey();
  return useMutation({
    mutationFn: ({ consentRecordId, etag }: { consentRecordId: string; etag: string | undefined }) => {
      const withdrawMyConsentRequest = etag ? { etag } : {};
      return call(
        api.get(MyConsentApi).withdrawMyConsent({
          consentRecordId,
          idempotencyKey: key.keyFor({ consentRecordId, ...withdrawMyConsentRequest }),
          withdrawMyConsentRequest,
        }),
      );
    },
    onSettled: (_data, error) => {
      key.settled(error);
      void qc.invalidateQueries({ queryKey: MY_CONSENTS });
    },
  });
}

/** The rows of one person in the consent centre, in the order the server lists them (the user first). */
export interface ConsentSubject {
  subjectPersonId: string;
  capacity: ConsentCapacity;
  rows: readonly MyConsent[];
}

/** Groups the centre's rows by the person they are about, keeping the server's order of persons and purposes. */
export function consentSubjects(rows: readonly MyConsent[]): ConsentSubject[] {
  const bySubject = new Map<string, ConsentSubject & { rows: MyConsent[] }>();
  for (const row of rows) {
    const subject = bySubject.get(row.subjectPersonId);
    if (subject) subject.rows.push(row);
    else
      bySubject.set(row.subjectPersonId, {
        subjectPersonId: row.subjectPersonId,
        capacity: row.capacity,
        rows: [row],
      });
  }
  return [...bySubject.values()];
}

/**
 * What the user may do on a row: decide (a notice is published and the school allows some width), withdraw (a live
 * decision of theirs), or only read.
 */
export function consentActions(row: MyConsent): { decide: boolean; withdraw: boolean } {
  const deciding = row.capacity !== 'minor_self';
  return {
    decide:
      deciding &&
      row.noticeCode !== undefined &&
      row.noticeVersionId !== undefined &&
      row.allowedScopes.length > 0,
    withdraw: deciding && row.id !== undefined && row.myStatus !== 'expired',
  };
}

/**
 * The scope a refusal is sent at: the narrowest the purpose takes, since a refusal covers every wider width (design
 * §4.2), so "Don't allow" refuses the purpose everywhere.
 */
export function refusalScope(row: MyConsent): ConsentScope | undefined {
  return row.allowedScopes[0];
}

/**
 * The sentence for a failed decision or withdrawal: 412 means the row changed elsewhere (reload), 422 that the
 * school changed the question (notice, width or switch) since it was shown, 403 that this account may not decide
 * here (for example while acting as another user), anything else the generic one.
 */
export function consentFailureKey(error: unknown): MessageKey {
  if (!isApiError(error)) return 'common.error.generic';
  if (error.status === 412) return 'common.conflict.body';
  if (error.status === 422 || error.status === 404) return 'privacy.consentCentre.error.changed';
  if (error.status === 403) return 'privacy.consentCentre.error.notAllowed';
  return 'common.error.generic';
}
