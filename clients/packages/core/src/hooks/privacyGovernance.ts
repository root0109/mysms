import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { MessageKey } from '@mysms/i18n';
import type { privacy } from '@mysms/sdk';
import { BreachApi } from '@mysms/sdk/privacy/apis/BreachApi';
import { LegalHoldApi } from '@mysms/sdk/privacy/apis/LegalHoldApi';
import { RetentionPolicyApi } from '@mysms/sdk/privacy/apis/RetentionPolicyApi';
import { RetentionRunApi } from '@mysms/sdk/privacy/apis/RetentionRunApi';
import { newIdempotencyKey } from '../api/ids';
import { allPages, pageQuery } from '../api/pages';
import { isApiError } from '../api/problem';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type Breach = privacy.Breach;
export type BreachUpdate = privacy.BreachUpdate;
export type BreachNotification = privacy.BreachNotification;
export type BreachStatus = privacy.BreachStatusEnum;
export type NotificationAudience = privacy.RecordBreachNotificationRequestAudienceEnum;
export type NotificationKind = privacy.RecordBreachNotificationRequestKindEnum;
export type LegalHold = privacy.LegalHold;
export type LegalHoldReason = privacy.LegalHoldReasonCodeEnum;
export type LegalHoldScope = privacy.LegalHoldScopeEnum;
export type RetentionPolicy = privacy.RetentionPolicy;
export type RetentionAction = privacy.RetentionPolicyUpdateActionEnum;
export type RetentionRun = privacy.RetentionRun;

const BREACHES = queryKey('privacy', 'breaches');
const LEGAL_HOLDS = queryKey('privacy', 'legal-holds');
const RETENTION_POLICIES = queryKey('privacy', 'retention-policies');
const RETENTION_RUNS = queryKey('privacy', 'retention-runs');

/** The fields a partial update names: those present in the change, in the request's update mask. */
const maskOf = (changes: object) => Object.keys(changes).join(',');

/**
 * Puts the server's answer into every cached list that shows the row before the lists are read again, so the next
 * step on the row is sent under its new etag even while the list is still being refetched.
 */
function answered<T extends { id?: string }>(qc: QueryClient, key: readonly unknown[], row: T) {
  qc.setQueriesData<{ items: T[] }>({ queryKey: key }, (old) =>
    old ? { ...old, items: old.items.map((item) => (item.id === row.id ? row : item)) } : old,
  );
}

// ---- ADM-20 breach register

/** Which breaches the register shows: the open ones, those past the Board's deadline, or all of them. */
export type BreachView = 'open' | 'overdue' | 'all';

/** The server filter of a register view: open (and overdue) = neither closed nor found not to be a breach. */
export const breachFilter = (view: BreachView): string | undefined =>
  view === 'all' ? undefined : 'status != "closed" AND status != "not_a_breach"';

/** ADM-20: the tenant's breaches (privacy.ListBreaches), the Board's nearest deadline first. */
export function useBreaches(view: BreachView, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('privacy', 'breaches', view),
    queryFn: async () => {
      const filter = breachFilter(view);
      const all = await allPages((page) =>
        call(
          api.get(BreachApi).listBreaches({
            size: 500,
            sort: ['boardReportDueAt'],
            ...(filter ? { filter } : {}),
            ...pageQuery(page),
          }),
        ),
      );
      // overdue is derived by the server at read time, not a stored field the list can filter on
      return view === 'overdue' ? { ...all, items: all.items.filter((b) => b.overdue) } : all;
    },
    staleTime: STALE_TIME.list,
  });
}

/** Whether the breach is still open: neither closed nor found not to be a breach. */
export const breachOpen = (breach: Pick<Breach, 'closedAt'>): boolean => !breach.closedAt;

/** Where the Board's 72-hour clock stands: report sent, not a breach, overdue, within a day, or running. */
export function boardClock(
  breach: Pick<Breach, 'boardReportSentAt' | 'status' | 'overdue' | 'hoursRemaining'>,
): 'reported' | 'notABreach' | 'overdue' | 'dueSoon' | 'running' {
  if (breach.boardReportSentAt) return 'reported';
  if (breach.status === 'not_a_breach') return 'notABreach';
  if (breach.overdue) return 'overdue';
  return (breach.hoursRemaining ?? Number.POSITIVE_INFINITY) <= 24 ? 'dueSoon' : 'running';
}

/** A step on a breach, with what it sends; every step is guarded by the breach's etag. */
export type BreachStep =
  | { step: 'triage' }
  | { step: 'contain' }
  | { step: 'recordNotification'; audience: NotificationAudience; kind: NotificationKind; sentAt: Date }
  | { step: 'close' }
  | { step: 'markNotABreach'; reason: string };

/**
 * The steps a breach's status allows, in the order the register offers them (design §11): a reported breach is
 * triaged; a triaged one contained or found not to be a breach; notices are recorded from containment on; a breach
 * is closed once the principals are told and the Board's detailed report recorded.
 */
export function breachSteps(
  breach: Pick<Breach, 'status' | 'boardReportSentAt' | 'closedAt'>,
): BreachStep['step'][] {
  if (breach.closedAt) return [];
  switch (breach.status) {
    case 'reported':
      return ['triage'];
    case 'triaged':
      return ['contain', 'markNotABreach'];
    case 'principals_notified':
      return breach.boardReportSentAt ? ['close', 'recordNotification'] : ['recordNotification'];
    case 'contained':
    case 'board_initial_sent':
    case 'board_report_sent':
      return ['recordNotification'];
    default:
      return [];
  }
}

function refreshBreaches(qc: QueryClient) {
  void qc.invalidateQueries({ queryKey: BREACHES });
}

/** ADM-20: privacy.ReportBreach — one Idempotency-Key per report, so a retried send never files it twice. */
export function useReportBreach() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ breach, idempotencyKey }: { breach: Breach; idempotencyKey: string }) =>
      call(api.get(BreachApi).reportBreach({ idempotencyKey, breach })),
    onSettled: () => refreshBreaches(qc),
  });
}

/**
 * The fields of an open breach the officer changed: only those differing from the stored breach (an empty text
 * clears nothing: it is left out), so the update mask names exactly them.
 */
export function breachChanges(breach: Breach, draft: BreachUpdate): BreachUpdate {
  const changes: Record<string, unknown> = {};
  for (const [field, value] of Object.entries(draft) as [keyof BreachUpdate, unknown][]) {
    if (value === undefined || value === '') continue;
    if (JSON.stringify(value) !== JSON.stringify(breach[field])) changes[field] = value;
  }
  return changes as BreachUpdate;
}

/** ADM-20: privacy.UpdateBreach — what is known of an open breach, under If-Match (412 when stale). */
export function useUpdateBreach() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ breachId, etag, changes }: { breachId: string; etag: string; changes: BreachUpdate }) =>
      call(
        api
          .get(BreachApi)
          .updateBreach({ breachId, ifMatch: etag, breachUpdate: changes, updateMask: maskOf(changes) }),
      ),
    onSuccess: (breach) => answered(qc, BREACHES, breach),
    onSettled: () => refreshBreaches(qc),
  });
}

/** ADM-20: one step on a breach (triage, contain, record a notice, close, mark not a breach), under its etag. */
export function useBreachStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ breach, action }: { breach: Breach; action: BreachStep }) => {
      const steps = api.get(BreachApi);
      const breachId = breach.id as string;
      const etag = breach.etag as string;
      const idempotencyKey = newIdempotencyKey();
      switch (action.step) {
        case 'triage':
          return call(steps.triageBreach({ breachId, idempotencyKey, breachStepRequest: { etag } }));
        case 'contain':
          return call(steps.containBreach({ breachId, idempotencyKey, breachStepRequest: { etag } }));
        case 'close':
          return call(steps.closeBreach({ breachId, idempotencyKey, breachStepRequest: { etag } }));
        case 'markNotABreach':
          return call(
            steps.markBreachNotABreach({
              breachId,
              idempotencyKey,
              markBreachNotABreachRequest: { reason: action.reason.trim(), etag },
            }),
          );
        case 'recordNotification':
          return call(
            steps.recordBreachNotification({
              breachId,
              idempotencyKey,
              recordBreachNotificationRequest: {
                audience: action.audience,
                kind: action.kind,
                sentAt: action.sentAt.toISOString(),
                etag,
              },
            }),
          );
      }
    },
    onSuccess: (breach) => answered(qc, BREACHES, breach),
    onSettled: () => refreshBreaches(qc),
  });
}

// ---- ADM-21 legal holds

/** Which holds the register shows: the active ones (nearest review first) or all of them. */
export type LegalHoldView = 'active' | 'all';

/** The server filter of a hold view. */
export const legalHoldFilter = (view: LegalHoldView): string | undefined =>
  view === 'active' ? 'active = true' : undefined;

/** ADM-21: the tenant's legal holds (privacy.ListLegalHolds), the nearest review date first. */
export function useLegalHolds(view: LegalHoldView, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('privacy', 'legal-holds', view),
    queryFn: () =>
      allPages((page) => {
        const filter = legalHoldFilter(view);
        return call(
          api.get(LegalHoldApi).listLegalHolds({
            size: 500,
            sort: ['reviewBy'],
            ...(filter ? { filter } : {}),
            ...pageQuery(page),
          }),
        );
      }),
    staleTime: STALE_TIME.list,
  });
}

/** What the officer places a hold on: its reason, scope, review date and the targets the scope names. */
export interface LegalHoldDraft {
  reasonCode: LegalHoldReason;
  scope: LegalHoldScope;
  reviewBy: string;
  subjectPersonId?: string;
  categoryCode?: string;
  module?: string;
  entityType?: string;
  /** Record ids, separated by commas, spaces or new lines. */
  entityIds?: string;
}

/**
 * The body PlaceLegalHold takes for a draft: a subject hold names the person, a category hold the data category, a
 * records hold the module, the record type and the ids — never a target of another scope (the server refuses it).
 */
export function legalHoldBody(draft: LegalHoldDraft): LegalHold {
  const body: LegalHold = { reasonCode: draft.reasonCode, scope: draft.scope, reviewBy: draft.reviewBy };
  const text = (value: string | undefined) => value?.trim() || undefined;
  if (draft.scope === 'subject') {
    const subject = text(draft.subjectPersonId);
    if (subject) body.subjectPersonId = subject;
  }
  if (draft.scope === 'category') {
    const category = text(draft.categoryCode);
    if (category) body.categoryCode = category;
  }
  if (draft.scope === 'records') {
    const module = text(draft.module);
    const entityType = text(draft.entityType);
    const ids = (draft.entityIds ?? '').split(/[\s,]+/).filter(Boolean);
    if (module) body.module = module;
    if (entityType) body.entityType = entityType;
    if (ids.length > 0) body.entityIds = ids;
  }
  return body;
}

function refreshHolds(qc: QueryClient) {
  void qc.invalidateQueries({ queryKey: LEGAL_HOLDS });
}

/** ADM-21: privacy.PlaceLegalHold — one Idempotency-Key per hold, so a retried send never places it twice. */
export function usePlaceLegalHold() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ draft, idempotencyKey }: { draft: LegalHoldDraft; idempotencyKey: string }) =>
      call(api.get(LegalHoldApi).placeLegalHold({ idempotencyKey, legalHold: legalHoldBody(draft) })),
    onSettled: () => refreshHolds(qc),
  });
}

/** ADM-21: privacy.UpdateLegalHold — an active hold's review date, under If-Match. */
export function useUpdateLegalHold() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ legalHoldId, etag, reviewBy }: { legalHoldId: string; etag: string; reviewBy: string }) =>
      call(
        api.get(LegalHoldApi).updateLegalHold({
          legalHoldId,
          ifMatch: etag,
          legalHoldUpdate: { reviewBy },
          updateMask: 'reviewBy',
        }),
      ),
    onSuccess: (hold) => answered(qc, LEGAL_HOLDS, hold),
    onSettled: () => refreshHolds(qc),
  });
}

/** ADM-21: privacy.ReleaseLegalHold — under the hold's etag; a released hold changes no more. */
export function useReleaseLegalHold() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (hold: LegalHold) =>
      call(
        api.get(LegalHoldApi).releaseLegalHold({
          legalHoldId: hold.id as string,
          idempotencyKey: newIdempotencyKey(),
          releaseLegalHoldRequest: { etag: hold.etag as string },
        }),
      ),
    onSuccess: (hold) => answered(qc, LEGAL_HOLDS, hold),
    onSettled: () => refreshHolds(qc),
  });
}

// ---- ADM-22 retention

/** ADM-22: the retention policies with the school's setting of each (privacy.ListRetentionPolicies), by code. */
export function useRetentionPolicies(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: RETENTION_POLICIES,
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(RetentionPolicyApi)
            .listRetentionPolicies({ size: 500, sort: ['code'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.list,
  });
}

/** ADM-22: the retention runs, newest first (privacy.ListRetentionRuns): one page of the latest 100. */
export function useRetentionRuns(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: RETENTION_RUNS,
    queryFn: () => call(api.get(RetentionRunApi).listRetentionRuns({ size: 100, sort: ['startedAt,desc'] })),
    staleTime: STALE_TIME.list,
  });
}

/** Whether a period is shorter than the policy's legal floor (the server refuses it too). */
export const belowFloor = (policy: Pick<RetentionPolicy, 'minimumPeriodDays'>, periodDays: number): boolean =>
  !Number.isInteger(periodDays) || periodDays < (policy.minimumPeriodDays ?? 0);

/** The school's change of a policy: only the period and action that differ from the stored setting. */
export function retentionChanges(
  policy: Pick<RetentionPolicy, 'periodDays' | 'action'>,
  periodDays: number,
  action: RetentionAction,
): privacy.RetentionPolicyUpdate {
  return {
    ...(periodDays !== policy.periodDays ? { periodDays } : {}),
    ...(action !== policy.action ? { action } : {}),
  };
}

/** ADM-22: privacy.UpdateRetentionPolicy — the school's period and action, under the row's ETag ("0" if never set). */
export function useUpdateRetentionPolicy() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      retentionPolicyId,
      etag,
      changes,
    }: {
      retentionPolicyId: string;
      etag: string;
      changes: privacy.RetentionPolicyUpdate;
    }) =>
      call(
        api.get(RetentionPolicyApi).updateRetentionPolicy({
          retentionPolicyId,
          ifMatch: etag,
          retentionPolicyUpdate: changes,
          updateMask: maskOf(changes),
        }),
      ),
    onSuccess: (policy) => answered(qc, RETENTION_POLICIES, policy),
    onSettled: () => void qc.invalidateQueries({ queryKey: RETENTION_POLICIES }),
  });
}

/**
 * The sentence for a failed governance step: 412 the row changed (reload), 409 a state that no longer allows it, 404
 * not found, 403 not allowed here (for example while acting as another user), 422 a field to fix, else the generic one.
 */
export function governanceFailureKey(error: unknown): MessageKey {
  if (!isApiError(error)) return 'common.error.generic';
  if (error.status === 412) return 'common.conflict.body';
  if (error.status === 409) return 'privacy.governance.error.state';
  if (error.status === 404) return 'privacy.governance.error.notFound';
  if (error.status === 403) return 'privacy.governance.error.notAllowed';
  if (error.status === 422) return 'privacy.governance.error.invalid';
  return 'common.error.generic';
}
