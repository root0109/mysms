import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { MessageKey } from '@mysms/i18n';
import type { privacy } from '@mysms/sdk';
import { MyNomineeApi } from '@mysms/sdk/privacy/apis/MyNomineeApi';
import { MyRightsRequestApi } from '@mysms/sdk/privacy/apis/MyRightsRequestApi';
import { RightsRequestApi } from '@mysms/sdk/privacy/apis/RightsRequestApi';
import { newIdempotencyKey } from '../api/ids';
import { allPages, pageQuery } from '../api/pages';
import { isApiError } from '../api/problem';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';

export type MyRightsRequest = privacy.MyRightsRequest;
export type RightsRequest = privacy.RightsRequest;
export type RightsRequestTask = privacy.RightsRequestTask;
export type AccessSummary = privacy.AccessSummary;
export type MyNominee = privacy.MyNominee;
export type RightsRequestType = privacy.SubmitMyRequestRequestTypeEnum;
export type GrievanceCategory = privacy.SubmitMyRequestRequestGrievanceCategoryEnum;
export type RightsRequestCorrection = privacy.RightsRequestCorrection;
export type CloseOutcome = privacy.CloseRightsRequestRequestOutcomeCodeEnum;
export type RejectReason = privacy.RejectRightsRequestRequestReasonCodeEnum;
export type IdentityCheck = privacy.VerifyRightsRequestIdentityRequestIdentityCheckEnum;

const MY_REQUESTS = queryKey('privacy', 'my-requests');
const MY_NOMINEE = queryKey('privacy', 'my-nominee');
const RIGHTS_REQUESTS = queryKey('privacy', 'rights-requests');

/** ACC-15: the signed-in person's own rights requests (privacy.ListMyRequests), newest first, all of them. */
export function useMyRequests(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: MY_REQUESTS,
    queryFn: () =>
      allPages((page) => call(api.get(MyRightsRequestApi).listMyRequests({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.list,
  });
}

/**
 * ACC-15: the access summary the school sent in answer to one of the user's access requests
 * (privacy.GetMyAccessSummary); there is none until the request is fulfilled, so it is read only then.
 */
export function useMyAccessSummary(rightsRequestId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: rightsRequestId !== null,
    queryKey: queryKey('privacy', 'my-access-summary', rightsRequestId),
    queryFn: () =>
      call(api.get(MyRightsRequestApi).getMyAccessSummary({ rightsRequestId: rightsRequestId as string })),
    staleTime: STALE_TIME.detail,
  });
}

/** What the requester asks: the right, about whom, in their words, and the details the type requires. */
export interface RightsRequestDraft {
  type: RightsRequestType;
  subjectPersonId: string;
  description: string;
  correction?: RightsRequestCorrection;
  grievanceCategory?: GrievanceCategory;
}

/**
 * The body SubmitMyRequest takes for a draft: a correction's record and fields only for a correction, a grievance's
 * category only for a grievance (the server refuses either on another type).
 */
export function submitBody(draft: RightsRequestDraft): privacy.SubmitMyRequestRequest {
  const body: privacy.SubmitMyRequestRequest = {
    type: draft.type,
    subjectPersonId: draft.subjectPersonId,
    description: draft.description.trim(),
  };
  if (draft.type === 'correction' && draft.correction) body.correction = draft.correction;
  if (draft.type === 'grievance' && draft.grievanceCategory) body.grievanceCategory = draft.grievanceCategory;
  return body;
}

/** privacy.SubmitMyRequest: one Idempotency-Key per submission, so a retried send never files it twice. */
export function useSubmitMyRequest() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ draft, idempotencyKey }: { draft: RightsRequestDraft; idempotencyKey: string }) =>
      call(
        api
          .get(MyRightsRequestApi)
          .submitMyRequest({ idempotencyKey, submitMyRequestRequest: submitBody(draft) }),
      ),
    onSettled: () => void qc.invalidateQueries({ queryKey: MY_REQUESTS }),
  });
}

/** ACC-15: the user's active nominee (privacy.GetMyNominee), masked; null when they name none (404). */
export function useMyNominee(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: MY_NOMINEE,
    queryFn: async (): Promise<MyNominee | null> => {
      try {
        return await call(api.get(MyNomineeApi).getMyNominee());
      } catch (error) {
        if (isApiError(error) && error.status === 404) return null;
        throw error;
      }
    },
    staleTime: STALE_TIME.detail,
  });
}

/** privacy.SetMyNominee: names the user's nominee, replacing the active one under its etag (412 when stale). */
export function useSetMyNominee() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: privacy.SetMyNomineeRequest) =>
      call(
        api
          .get(MyNomineeApi)
          .setMyNominee({ idempotencyKey: newIdempotencyKey(), setMyNomineeRequest: request }),
      ),
    onSettled: () => void qc.invalidateQueries({ queryKey: MY_NOMINEE }),
  });
}

/** privacy.RevokeMyNominee: revokes the user's active nominee under its etag. */
export function useRevokeMyNominee() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (etag: string) =>
      call(
        api
          .get(MyNomineeApi)
          .revokeMyNominee({ idempotencyKey: newIdempotencyKey(), revokeMyNomineeRequest: { etag } }),
      ),
    onSettled: () => void qc.invalidateQueries({ queryKey: MY_NOMINEE }),
  });
}

/** Which requests the privacy staff's queue shows: the open ones (nearest deadline first) or all of them. */
export type QueueView = 'open' | 'all';

/** The server filter of a queue view: open = not yet closed or rejected. */
export const queueFilter = (view: QueueView): string | undefined =>
  view === 'open' ? 'status != "closed" AND status != "rejected"' : undefined;

/** ADM-19: the tenant's rights requests (privacy.ListRightsRequests), nearest deadline first. */
export function useRightsRequests(view: QueueView, enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    enabled,
    queryKey: queryKey('privacy', 'rights-requests', view),
    queryFn: () =>
      allPages((page) => {
        const filter = queueFilter(view);
        return call(
          api.get(RightsRequestApi).listRightsRequests({
            size: 500,
            sort: ['dueAt'],
            ...(filter ? { filter } : {}),
            ...pageQuery(page),
          }),
        );
      }),
    staleTime: STALE_TIME.list,
  });
}

/**
 * ADM-19: an access request's summary (privacy.GetAccessSummary): a preview while open, the one sent once
 * fulfilled.
 */
export function useAccessSummary(rightsRequestId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    enabled: rightsRequestId !== null,
    queryKey: queryKey('privacy', 'access-summary', rightsRequestId),
    queryFn: () =>
      call(api.get(RightsRequestApi).getAccessSummary({ rightsRequestId: rightsRequestId as string })),
    staleTime: STALE_TIME.detail,
  });
}

/** A staff step on a request, with what it sends; every step is guarded by the request's etag. */
export type RightsRequestStep =
  | { step: 'verifyIdentity'; identityCheck: IdentityCheck }
  | { step: 'assign'; assigneeUserId: string }
  | { step: 'extend'; dueAt: Date; reason: string }
  | { step: 'reject'; reasonCode: RejectReason }
  | { step: 'close'; outcomeCode: CloseOutcome; note?: string; response?: string }
  | { step: 'fulfilAccess' }
  | { step: 'startFulfilment' }
  | { step: 'fulfilWithdrawAll' }
  | { step: 'retryTask'; taskId: string };

function refreshQueue(qc: QueryClient) {
  void qc.invalidateQueries({ queryKey: RIGHTS_REQUESTS });
  void qc.invalidateQueries({ queryKey: queryKey('privacy', 'access-summary') });
}

/**
 * ADM-19: one staff step (verify, assign, extend, reject, close, fulfil an access request, start an erasure or
 * export, fulfil a withdraw-all request, retry a module's task).
 */
export function useRightsRequestStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ request, action }: { request: RightsRequest; action: RightsRequestStep }) => {
      const steps = api.get(RightsRequestApi);
      const rightsRequestId = request.id as string;
      const etag = request.etag as string;
      const idempotencyKey = newIdempotencyKey();
      switch (action.step) {
        case 'verifyIdentity':
          return call(
            steps.verifyRightsRequestIdentity({
              rightsRequestId,
              idempotencyKey,
              verifyRightsRequestIdentityRequest: { identityCheck: action.identityCheck, etag },
            }),
          );
        case 'assign':
          return call(
            steps.assignRightsRequest({
              rightsRequestId,
              idempotencyKey,
              assignRightsRequestRequest: { assigneeUserId: action.assigneeUserId, etag },
            }),
          );
        case 'extend':
          return call(
            steps.extendRightsRequest({
              rightsRequestId,
              idempotencyKey,
              extendRightsRequestRequest: { dueAt: action.dueAt.toISOString(), reason: action.reason, etag },
            }),
          );
        case 'reject':
          return call(
            steps.rejectRightsRequest({
              rightsRequestId,
              idempotencyKey,
              rejectRightsRequestRequest: { reasonCode: action.reasonCode, etag },
            }),
          );
        case 'close':
          return call(
            steps.closeRightsRequest({
              rightsRequestId,
              idempotencyKey,
              closeRightsRequestRequest: {
                outcomeCode: action.outcomeCode,
                etag,
                ...(action.note?.trim() ? { note: action.note.trim() } : {}),
                ...(action.response?.trim() ? { response: action.response.trim() } : {}),
              },
            }),
          );
        case 'fulfilAccess':
          return call(
            steps.fulfilAccessRequest({
              rightsRequestId,
              idempotencyKey,
              fulfilAccessRequestRequest: { etag },
            }),
          );
        case 'startFulfilment':
          return call(
            steps.startRightsRequestFulfilment({
              rightsRequestId,
              idempotencyKey,
              fulfilRightsRequestRequest: { etag },
            }),
          );
        case 'fulfilWithdrawAll':
          return call(
            steps.fulfilWithdrawAllRequest({
              rightsRequestId,
              idempotencyKey,
              fulfilRightsRequestRequest: { etag },
            }),
          );
        case 'retryTask':
          return call(
            steps.retryRightsRequestTask({
              rightsRequestId,
              taskId: action.taskId,
              idempotencyKey,
              retryRightsRequestTaskRequest: { etag },
            }),
          );
      }
    },
    onSettled: () => refreshQueue(qc),
  });
}

/**
 * The steps a request's state allows, in the order the queue offers them (design §9.6, §9.2): an access request is
 * answered with its summary, a withdraw-all request fulfilled in one step, an erasure or export started once (then
 * its modules' tasks carry it and it is no longer closed by hand); a request waiting for action can still be extended.
 */
export function availableSteps(
  request: Pick<RightsRequest, 'status' | 'type' | 'tasks'>,
): Exclude<RightsRequestStep['step'], 'retryTask'>[] {
  switch (request.status) {
    case 'submitted':
      return ['verifyIdentity', 'reject', 'extend'];
    case 'verified':
      return ['assign', 'reject', 'extend'];
    case 'in_progress':
      if (request.type === 'access') return ['fulfilAccess', 'close', 'assign', 'extend'];
      if (request.type === 'withdraw_all') return ['fulfilWithdrawAll', 'close', 'assign', 'extend'];
      if (request.type === 'erasure' || request.type === 'export')
        return request.tasks?.length
          ? ['assign', 'extend']
          : ['startFulfilment', 'close', 'assign', 'extend'];
      return ['close', 'assign', 'extend'];
    case 'action_required':
      return ['extend'];
    default:
      return [];
  }
}

/**
 * Whether a module's task may be dispatched again by the staff: the request still being fulfilled (in progress or
 * waiting for action) and the task failed or unanswered.
 */
export const retryable = (
  request: Pick<RightsRequest, 'status'>,
  task: Pick<RightsRequestTask, 'status'>,
): boolean =>
  (request.status === 'in_progress' || request.status === 'action_required') &&
  (task.status === 'failed' || task.status === 'dispatched');

/** The request types fulfilled by their own step (access summary, the modules' tasks, the consents withdrawn). */
const FULFILLED_BY_THEIR_STEP: (RightsRequestType | undefined)[] = [
  'access',
  'erasure',
  'export',
  'withdraw_all',
];

/** The outcomes a request may close with: one fulfilled by its own step only declined, never a nomination partly. */
export function closeOutcomes(type: RightsRequestType | undefined): CloseOutcome[] {
  if (FULFILLED_BY_THEIR_STEP.includes(type)) return ['declined'];
  if (type === 'nomination') return ['fulfilled', 'declined'];
  return ['fulfilled', 'partially_fulfilled', 'declined'];
}

/** Whether closing with the outcome owes the requester an answer: a grievance always, a declined correction. */
export const responseRequired = (type: RightsRequestType | undefined, outcome: CloseOutcome): boolean =>
  type === 'grievance' || (type === 'correction' && outcome === 'declined');

/** How close a request is to its deadline: the queue's colour. Within 7 days is due soon. */
export function deadlineTone(
  request: Pick<RightsRequest, 'dueAt' | 'overdue' | 'closedAt'>,
  now: Date,
): 'closed' | 'overdue' | 'dueSoon' | 'onTrack' {
  if (request.closedAt) return 'closed';
  if (request.overdue) return 'overdue';
  const due = request.dueAt ? new Date(request.dueAt).getTime() : Number.POSITIVE_INFINITY;
  return due - now.getTime() <= 7 * 24 * 60 * 60 * 1000 ? 'dueSoon' : 'onTrack';
}

/**
 * The sentence for a failed request or step: 412 the request changed (reload), 409 a conflict of interest or a state
 * that no longer allows it, 404 not found (or not the user's to see), 403 not allowed here (for example while acting as
 * another user), 422 a field to fix, anything else the generic one.
 */
export function rightsFailureKey(error: unknown): MessageKey {
  if (!isApiError(error)) return 'common.error.generic';
  if (error.status === 412) return 'common.conflict.body';
  if (error.status === 409 && error.code === 'RIGHTS_REQUEST_CONFLICT_OF_INTEREST')
    return 'privacy.rights.error.conflictOfInterest';
  if (error.status === 409) return 'privacy.rights.error.state';
  if (error.status === 404) return 'privacy.rights.error.notFound';
  if (error.status === 403) return 'privacy.rights.error.notAllowed';
  if (error.status === 422) return 'privacy.rights.error.invalid';
  return 'common.error.generic';
}
