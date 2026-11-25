import {
  availableSteps,
  closeOutcomes,
  deadlineTone,
  isApiError,
  responseRequired,
  retryable,
  rightsFailureKey,
  useAccessSummary,
  useCan,
  useI18n,
  useRightsRequests,
  useRightsRequestStep,
  type CloseOutcome,
  type IdentityCheck,
  type QueueView,
  type RejectReason,
  type RightsRequest,
  type RightsRequestStep,
  type RightsRequestTask,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import {
  Banner,
  Button,
  DataTable,
  Dialog,
  SegmentedControl,
  SelectField,
  StatusChip,
  TextArea,
  TextInput,
  type Column,
  type StatusChipProps,
} from '@mysms/ui-web';
import { useState } from 'react';
import { SummaryView } from './MyRequestsScreen';

const TONE_INTENT: Readonly<Record<ReturnType<typeof deadlineTone>, StatusChipProps['intent']>> = {
  overdue: 'danger',
  dueSoon: 'warning',
  onTrack: 'success',
  closed: 'neutral',
};
const IDENTITY_CHECKS: readonly IdentityCheck[] = ['in_person', 'document'];
const REJECT_REASONS: readonly RejectReason[] = [
  'identity_not_verified',
  'not_entitled',
  'duplicate',
  'other',
];

/** The server's sentence for one field of a refused step (422), already in the user's language. */
const fieldError = (error: unknown, field: string) =>
  isApiError(error) ? error.errors.find((e) => e.field === field)?.message : undefined;

/**
 * ADM-19 Privacy requests (ui/02; design PRIVACY-AND-CONSENT-CENTRE §9.6, §12): the school's privacy officer's
 * queue — the open requests nearest their deadline first, coloured by how close it is — and each request's steps as
 * its state allows: verify the requester, assign, extend the deadline with a reason, reject, close with the answer the
 * requester sees (a grievance always, a declined correction with its reason), or answer an access request with the
 * summary previewed here. An erasure or export is started once and then carried by one task per module, listed with
 * its status, attempts and last error, a failed or unanswered task dispatched again on request; a withdraw-all request
 * is fulfilled in one step. A staff member is never offered a request they made or that is about them: the server
 * refuses it, and the screen says why.
 */
export function RightsRequestsScreen() {
  const { t, format } = useI18n();
  const [view, setView] = useState<QueueView>('open');
  const requests = useRightsRequests(view);
  const [selected, setSelected] = useState<RightsRequest | null>(null);
  const now = new Date();
  // the list's copy when it still shows the request, else the one last answered (a closed request leaves the open view)
  const open = selected ? (requests.data?.items.find((r) => r.id === selected.id) ?? selected) : null;

  const columns: Column<RightsRequest>[] = [
    { key: 'number', header: t('privacy.queue.column.number'), render: (r) => r.number ?? '' },
    {
      key: 'type',
      header: t('privacy.queue.column.type'),
      render: (r) => enumLabel(t, 'rightsRequestType', r.type),
    },
    {
      key: 'requester',
      header: t('privacy.queue.column.requester'),
      render: (r) => enumLabel(t, 'requesterCapacity', r.requesterCapacity),
    },
    {
      key: 'status',
      header: t('privacy.queue.column.status'),
      render: (r) =>
        r.outcomeCode
          ? enumLabel(t, 'rightsOutcome', r.outcomeCode)
          : enumLabel(t, 'rightsRequestStatus', r.status),
    },
    {
      key: 'due',
      header: t('privacy.queue.column.due'),
      render: (r) => {
        const tone = deadlineTone(r, now);
        return (
          <StatusChip
            intent={TONE_INTENT[tone]}
            label={`${t(`privacy.queue.tone.${tone}` as const)}${r.dueAt && tone !== 'closed' ? ` · ${format.date(r.dueAt)}` : ''}`}
          />
        );
      },
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.queue.title')}</h1>
        <p className="page__subtitle">{t('privacy.queue.intro')}</p>
      </header>
      <SegmentedControl
        label={t('privacy.queue.view')}
        options={[
          { value: 'open', label: t('privacy.queue.view.open') },
          { value: 'all', label: t('privacy.queue.view.all') },
        ]}
        value={view}
        onChange={(value) => setView(value as QueueView)}
      />
      {requests.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void requests.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : (
        <DataTable
          caption={t('privacy.queue.caption')}
          columns={columns}
          rows={requests.data?.items}
          rowKey={(r) => r.id ?? ''}
          loading={requests.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('privacy.queue.empty')}
          onRowClick={setSelected}
        />
      )}
      {open ? (
        <RequestSteps request={open} onChanged={setSelected} onClose={() => setSelected(null)} />
      ) : null}
    </div>
  );
}

type StepName = RightsRequestStep['step'];
/** The steps confirmed in a form before they run: those that ask for details, and those that cannot be undone. */
type FormStep = Exclude<StepName, 'fulfilAccess' | 'retryTask'>;
/** The steps that fulfil a request by the modules' tasks or at once, under the fulfil permission. */
const FULFILMENT: readonly StepName[] = ['startFulfilment', 'fulfilWithdrawAll'];

function RequestSteps({
  request,
  onChanged,
  onClose,
}: {
  request: RightsRequest;
  onChanged: (changed: RightsRequest) => void;
  onClose: () => void;
}) {
  const { t } = useI18n();
  const run = useRightsRequestStep();
  const mayFulfil = useCan('privacy.rights_request.fulfil');
  const mayRetry = useCan('privacy.rights_request.retry_task');
  const steps = availableSteps(request).filter((name) => mayFulfil || !FULFILMENT.includes(name));
  const [step, setStep] = useState<StepName | null>(null);
  const [retrying, setRetrying] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const preview = useAccessSummary(
    request.type === 'access' && request.status === 'in_progress' ? (request.id ?? null) : null,
  );
  const perform = (action: RightsRequestStep) =>
    run.mutate(
      { request, action },
      {
        onSuccess: (changed) => {
          setStep(null);
          setDone(true);
          onChanged(changed);
        },
      },
    );
  return (
    <Dialog
      open
      title={t('privacy.myRequests.detail', { number: request.number ?? '' })}
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          {t('common.action.close')}
        </Button>
      }
    >
      <h3>{t('privacy.queue.request')}</h3>
      <p>{enumLabel(t, 'rightsRequestType', request.type)}</p>
      <p>{request.description}</p>
      {request.correction ? (
        <>
          <p>
            {t('privacy.rights.correctionOf', {
              recordType: request.correction.recordType,
              module: request.correction.module,
            })}
          </p>
          <ul>
            {request.correction.fields.map((f) => (
              <li key={f.field}>
                {f.field}: {f.requestedValue}
              </li>
            ))}
          </ul>
        </>
      ) : null}
      {request.grievanceCategory ? (
        <p>{enumLabel(t, 'grievanceCategory', request.grievanceCategory)}</p>
      ) : null}
      {request.response ? (
        <>
          <h3>{t('privacy.rights.response')}</h3>
          <p>{request.response}</p>
        </>
      ) : null}
      {preview.data ? <SummaryView summary={preview.data} title={t('privacy.queue.preview')} /> : null}
      {request.tasks?.length ? (
        <TaskList
          request={request}
          mayRetry={mayRetry}
          retrying={run.isPending ? retrying : null}
          onRetry={(taskId) => {
            setRetrying(taskId);
            perform({ step: 'retryTask', taskId });
          }}
        />
      ) : null}
      {done ? <Banner intent="success">{t('privacy.queue.done')}</Banner> : null}
      {run.isError ? <Banner intent="danger">{t(rightsFailureKey(run.error))}</Banner> : null}
      <div className="consent-card__actions">
        {steps.map((name) => (
          <Button
            key={name}
            variant={step === name ? 'primary' : 'secondary'}
            loading={run.isPending && step === name}
            onClick={() => (name === 'fulfilAccess' ? perform({ step: 'fulfilAccess' }) : setStep(name))}
          >
            {t(`privacy.queue.step.${name}` as const)}
          </Button>
        ))}
      </div>
      {step && step !== 'fulfilAccess' && step !== 'retryTask' ? (
        <StepForm
          key={step}
          step={step}
          request={request}
          error={run.error}
          pending={run.isPending}
          onRun={perform}
        />
      ) : null}
    </Dialog>
  );
}

/** The modules' tasks of an erasure or export: status, attempts and last error, a retry per failed or silent task. */
function TaskList(props: {
  request: RightsRequest;
  mayRetry: boolean;
  retrying: string | null;
  onRetry: (taskId: string) => void;
}) {
  const { t } = useI18n();
  const columns: Column<RightsRequestTask>[] = [
    { key: 'module', header: t('privacy.queue.task.module'), render: (task) => task.module ?? '' },
    {
      key: 'wave',
      header: t('privacy.queue.task.wave'),
      render: (task) => String(task.wave ?? ''),
      align: 'end',
    },
    {
      key: 'status',
      header: t('privacy.queue.task.status'),
      render: (task) => enumLabel(t, 'rightsTaskStatus', task.status),
    },
    {
      key: 'attempts',
      header: t('privacy.queue.task.attempts'),
      render: (task) => String(task.attempts ?? 0),
      align: 'end',
    },
    {
      key: 'error',
      header: t('privacy.queue.task.lastError'),
      render: (task) => task.lastErrorCode ?? '',
    },
    {
      key: 'retry',
      header: t('privacy.queue.task.action'),
      render: (task) =>
        props.mayRetry && task.id && retryable(props.request, task) ? (
          <Button
            variant="secondary"
            loading={props.retrying === task.id}
            onClick={() => props.onRetry(task.id as string)}
          >
            {t('privacy.queue.task.retry', { module: task.module ?? '' })}
          </Button>
        ) : null,
    },
  ];
  return (
    <>
      <h3>{t('privacy.queue.task.title')}</h3>
      <DataTable
        caption={t('privacy.queue.task.title')}
        columns={columns}
        rows={props.request.tasks}
        rowKey={(task) => task.id ?? ''}
        loadingLabel={t('common.state.loading')}
        emptyText={t('privacy.queue.task.empty')}
      />
    </>
  );
}

function StepForm(props: {
  step: FormStep;
  request: RightsRequest;
  error: unknown;
  pending: boolean;
  onRun: (action: RightsRequestStep) => void;
}) {
  const { t } = useI18n();
  const outcomes = closeOutcomes(props.request.type);
  const [identityCheck, setIdentityCheck] = useState<IdentityCheck>('in_person');
  const [assignee, setAssignee] = useState('');
  const [dueAt, setDueAt] = useState('');
  const [reason, setReason] = useState('');
  const [rejectReason, setRejectReason] = useState<RejectReason>('other');
  const [outcome, setOutcome] = useState<CloseOutcome>(outcomes[0] ?? 'declined');
  const [response, setResponse] = useState('');
  const [note, setNote] = useState('');
  const [missing, setMissing] = useState(false);
  const submit = () => {
    switch (props.step) {
      case 'verifyIdentity':
        return props.onRun({ step: 'verifyIdentity', identityCheck });
      case 'assign':
        return props.onRun({ step: 'assign', assigneeUserId: assignee.trim() });
      case 'extend':
        return props.onRun({ step: 'extend', dueAt: new Date(dueAt), reason });
      case 'reject':
        return props.onRun({ step: 'reject', reasonCode: rejectReason });
      case 'close':
        if (responseRequired(props.request.type, outcome) && !response.trim()) return setMissing(true);
        return props.onRun({ step: 'close', outcomeCode: outcome, note, response });
      case 'startFulfilment':
      case 'fulfilWithdrawAll':
        return props.onRun({ step: props.step });
    }
  };
  return (
    <div className="consent-card">
      {props.step === 'verifyIdentity' ? (
        <SelectField
          label={t('privacy.queue.identityCheck')}
          value={identityCheck}
          options={IDENTITY_CHECKS.map((value) => ({ value, label: enumLabel(t, 'identityCheck', value) }))}
          onChange={(event) => setIdentityCheck(event.currentTarget.value as IdentityCheck)}
        />
      ) : null}
      {props.step === 'assign' ? (
        <TextInput
          label={t('privacy.queue.assignee')}
          value={assignee}
          error={fieldError(props.error, 'assigneeUserId')}
          onChange={(event) => setAssignee(event.currentTarget.value)}
        />
      ) : null}
      {props.step === 'extend' ? (
        <>
          <TextInput
            type="date"
            label={t('privacy.queue.dueAt')}
            value={dueAt}
            error={fieldError(props.error, 'dueAt')}
            onChange={(event) => setDueAt(event.currentTarget.value)}
          />
          <TextInput
            label={t('privacy.queue.reason')}
            value={reason}
            onChange={(event) => setReason(event.currentTarget.value)}
          />
        </>
      ) : null}
      {props.step === 'reject' ? (
        <SelectField
          label={t('privacy.queue.rejectReason')}
          value={rejectReason}
          options={REJECT_REASONS.map((value) => ({ value, label: enumLabel(t, 'rightsOutcome', value) }))}
          onChange={(event) => setRejectReason(event.currentTarget.value as RejectReason)}
        />
      ) : null}
      {props.step === 'startFulfilment' || props.step === 'fulfilWithdrawAll' ? (
        <p>{t(`privacy.queue.confirm.${props.step}` as const)}</p>
      ) : null}
      {props.step === 'close' ? (
        <>
          <SelectField
            label={t('privacy.queue.outcome')}
            value={outcome}
            options={outcomes.map((value) => ({ value, label: enumLabel(t, 'rightsOutcome', value) }))}
            onChange={(event) => setOutcome(event.currentTarget.value as CloseOutcome)}
          />
          <TextArea
            label={t('privacy.queue.response')}
            value={response}
            maxLength={4000}
            error={missing ? t('privacy.queue.responseRequired') : fieldError(props.error, 'response')}
            onChange={(event) => setResponse(event.currentTarget.value)}
          />
          <TextArea
            label={t('privacy.queue.note')}
            value={note}
            maxLength={4000}
            onChange={(event) => setNote(event.currentTarget.value)}
          />
        </>
      ) : null}
      <div className="consent-card__actions">
        <Button variant="primary" loading={props.pending} onClick={submit}>
          {t('privacy.queue.confirm')}
        </Button>
      </div>
    </div>
  );
}
