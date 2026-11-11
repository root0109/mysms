import {
  isApiError,
  RESOURCES,
  useAllocateExamRooms,
  useAssignmentApprovals,
  useAssignmentStep,
  useCan,
  useCreateAssignmentApproval,
  useExamSessionStep,
  useExamStep,
  useHomeworkStep,
  useI18n,
  useInfiniteRecords,
  useMarksheetStep,
  useOperation,
  useResultTemplateGenerateResults,
  type Operation,
  usePopulateExamResults,
  type RecordRow,
} from '@mysms/core';
import { Button, SelectField, StatusChip, Switch, TextInput } from '@mysms/ui-web';
import { enumLabel } from '@mysms/i18n';
import { useState } from 'react';

type T = ReturnType<typeof useI18n>['t'];
function actionError(t: T, error: unknown): string | null {
  if (!error) return null;
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 403) return t('adm.records.forbidden');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return error.errors.map((item) => item.message).join(' ') || t('common.validation.pattern');
  }
  return t('common.error.generic');
}

function RowError({ error, t }: { error: unknown; t: T }) {
  const message = actionError(t, error);
  return message ? (
    <p className="sms-field__error" role="alert">
      {message}
    </p>
  ) : null;
}

export function HomeworkBoardTools({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const mayPublish = useCan('assessment.homework.publish_homework');
  const action = useHomeworkStep();
  if (!mayPublish || row.status !== 'draft') return null;
  return (
    <>
      <Button
        variant="secondary"
        loading={action.isPending}
        onClick={() =>
          action.mutate({
            homeworkId: row.id,
            step: 'publish',
            etag: typeof row.etag === 'string' ? row.etag : null,
          })
        }
      >
        {t('asm.native.publish')}
      </Button>
      <RowError error={action.error} t={t} />
    </>
  );
}

export function HomeworkApprovalTools({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const mayApprove = useCan('assessment.homework.approve_homework');
  const mayReject = useCan('assessment.homework.reject_homework');
  const action = useHomeworkStep();
  const [reason, setReason] = useState('');
  const [error, setError] = useState<unknown>(null);
  const [pending, setPending] = useState(false);
  const run = async (step: 'approve' | 'reject') => {
    if (pending) return;
    setPending(true);
    setError(null);
    try {
      await action.mutateAsync({
        homeworkId: row.id,
        step,
        etag: typeof row.etag === 'string' ? row.etag : null,
        reason,
      });
    } catch (next) {
      setError(next);
    } finally {
      setPending(false);
    }
  };
  if (row.status !== 'pending' || (!mayApprove && !mayReject)) return null;
  return (
    <>
      {mayReject ? (
        <TextInput
          label={t('asm.native.approvalNote')}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
        />
      ) : null}
      {mayApprove ? (
        <Button variant="secondary" loading={pending} onClick={() => void run('approve')}>
          {t('asm.native.approve')}
        </Button>
      ) : null}
      {mayReject ? (
        <Button
          variant="danger"
          loading={pending}
          disabled={!reason.trim()}
          onClick={() => void run('reject')}
        >
          {t('asm.native.reject')}
        </Button>
      ) : null}
      <RowError error={error} t={t} />
    </>
  );
}

export function AssignmentTools({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const action = useAssignmentStep();
  const createApproval = useCreateAssignmentApproval();
  const may = {
    publish: useCan('assessment.assignment.publish_assignment'),
    approve: useCan('assessment.assignment.approve_assignment'),
    reject: useCan('assessment.assignment.reject_assignment'),
    finish: useCan('assessment.assignment.finish_assignment'),
    cancel: useCan('assessment.assignment.cancel_assignment'),
    reset: useCan('assessment.assignment.reset_assignment'),
  };
  const mayReadApprovals = useCan('assessment.assignment_approval.read');
  const mayCreateApproval = useCan('assessment.assignment_approval.create');
  const approvals = useAssignmentApprovals(mayReadApprovals ? row.id : null);
  const [reason, setReason] = useState('');
  const [approvalError, setApprovalError] = useState<unknown>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const status = String(row.status);
  const pendingApproval = approvals.data?.items.find((item) => item.decision === 'pending');
  const available: {
    step: 'publish' | 'finish' | 'cancel' | 'reset';
    label: string;
    states: string[];
  }[] = [
    { step: 'publish', label: t('asm.native.publish'), states: ['draft'] },
    { step: 'finish', label: t('asm.native.finish'), states: ['published', 'ongoing'] },
    { step: 'cancel', label: t('common.action.cancel'), states: ['draft', 'published', 'ongoing'] },
    { step: 'reset', label: t('asm.native.reset'), states: ['cancelled'] },
  ];
  const requestApproval = async () => {
    setApprovalError(null);
    setNotice(null);
    try {
      await createApproval.mutateAsync(row.id);
      setNotice(t('asm.web.approvalRequestSaved'));
    } catch (error) {
      setApprovalError(error);
    }
  };
  const review = async (step: 'approve' | 'reject') => {
    setApprovalError(null);
    setNotice(null);
    try {
      await action.mutateAsync({
        assignmentId: row.id,
        step,
        etag: typeof row.etag === 'string' ? row.etag : null,
        ...(step === 'reject' ? { reason } : {}),
      });
      setReason('');
    } catch (error) {
      setApprovalError(error);
    }
  };
  const canRequestApproval =
    mayCreateApproval &&
    status === 'draft' &&
    (!mayReadApprovals || (approvals.data !== undefined && !pendingApproval));
  const needsReason =
    (may.cancel && ['draft', 'published', 'ongoing'].includes(status)) ||
    (pendingApproval !== undefined && status === 'draft' && may.reject);
  return (
    <>
      {needsReason ? (
        <TextInput
          label={t('adm.records.reason')}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
        />
      ) : null}
      {available.map((button) =>
        may[button.step] && button.states.includes(status) ? (
          <Button
            key={button.step}
            variant={button.step === 'cancel' ? 'danger' : 'secondary'}
            loading={action.isPending}
            disabled={button.step === 'cancel' && !reason.trim()}
            onClick={() =>
              action.mutate({
                assignmentId: row.id,
                step: button.step,
                etag: typeof row.etag === 'string' ? row.etag : null,
                ...(button.step === 'cancel' ? { reason } : {}),
              })
            }
          >
            {button.label}
          </Button>
        ) : null,
      )}
      {canRequestApproval ? (
        <Button variant="secondary" loading={createApproval.isPending} onClick={() => void requestApproval()}>
          {t('asm.web.requestApproval')}
        </Button>
      ) : null}
      {mayReadApprovals ? (
        <div className="assessment-approvals">
          <strong>{t('asm.web.assignmentApprovals')}</strong>
          {approvals.isPending ? <p>{t('common.state.loading')}</p> : null}
          {approvals.isError ? <RowError error={approvals.error} t={t} /> : null}
          {approvals.data?.items.length === 0 ? (
            <p className="hint">{t('asm.web.noApprovalRecords')}</p>
          ) : null}
          {approvals.data?.items.map((approval) => (
            <div key={approval.id} className="assessment-approval">
              <StatusChip
                label={enumLabel(
                  t,
                  'assessment.assignment_approval_decision',
                  String(approval.decision ?? ''),
                )}
              />
              {approval.comments ? <p>{approval.comments}</p> : null}
              {approval.decision === 'pending' && status === 'draft' && may.approve ? (
                <Button variant="secondary" loading={action.isPending} onClick={() => void review('approve')}>
                  {t('asm.native.approve')}
                </Button>
              ) : null}
              {approval.decision === 'pending' && status === 'draft' && may.reject ? (
                <Button
                  variant="danger"
                  loading={action.isPending}
                  disabled={!reason.trim()}
                  onClick={() => void review('reject')}
                >
                  {t('asm.native.reject')}
                </Button>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}
      {notice ? <p role="status">{notice}</p> : null}
      <RowError error={approvalError ?? action.error ?? createApproval.error} t={t} />
    </>
  );
}

export function ExamSessionTools({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const action = useExamSessionStep();
  const may = {
    schedule: useCan('assessment.exam_session.schedule_exam_session'),
    markHeld: useCan('assessment.exam_session.mark_exam_session_held'),
    complete: useCan('assessment.exam_session.complete_exam_session'),
    cancel: useCan('assessment.exam_session.cancel_exam_session'),
    reset: useCan('assessment.exam_session.reset_exam_session'),
  };
  const status = String(row.status);
  const available: {
    step: 'schedule' | 'markHeld' | 'complete' | 'cancel' | 'reset';
    label: string;
    states: string[];
  }[] = [
    { step: 'schedule', label: t('asm.native.schedule'), states: ['draft'] },
    { step: 'markHeld', label: t('asm.native.markHeld'), states: ['scheduled'] },
    { step: 'complete', label: t('asm.native.complete'), states: ['held'] },
    { step: 'cancel', label: t('common.action.cancel'), states: ['draft', 'scheduled', 'held'] },
    { step: 'reset', label: t('asm.native.reset'), states: ['cancelled'] },
  ];
  return (
    <>
      {available.map((button) =>
        may[button.step] && button.states.includes(status) ? (
          <Button
            key={button.step}
            variant={button.step === 'cancel' ? 'danger' : 'secondary'}
            loading={action.isPending}
            onClick={() =>
              action.mutate({
                examSessionId: row.id,
                step: button.step,
                etag: typeof row.etag === 'string' ? row.etag : null,
              })
            }
          >
            {button.label}
          </Button>
        ) : null,
      )}
      <RowError error={action.error} t={t} />
    </>
  );
}

export function ExamTools({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const transitions = useExamStep();
  const populate = usePopulateExamResults();
  const allocate = useAllocateExamRooms();
  const may = {
    schedule: useCan('assessment.exam.schedule_exam'),
    markHeld: useCan('assessment.exam.mark_exam_held'),
    complete: useCan('assessment.exam.complete_exam'),
    cancel: useCan('assessment.exam.cancel_exam'),
    reset: useCan('assessment.exam.reset_exam'),
    populate: useCan('assessment.exam.populate_exam_results'),
    allocate: useCan('assessment.exam.allocate_exam_rooms'),
  };
  const [roomIds, setRoomIds] = useState<string[]>([]);
  const [error, setError] = useState<unknown>(null);
  const roomsDef = RESOURCES.find((resource) => resource.key === 'facility.Room');
  const rooms = useInfiniteRecords(may.allocate ? roomsDef : undefined, { size: 100 });
  const roomRows = rooms.data?.pages.flatMap((page) => page.data) ?? [];
  const status = String(row.status);
  const available: {
    step: 'schedule' | 'markHeld' | 'complete' | 'cancel' | 'reset';
    label: string;
    states: string[];
  }[] = [
    { step: 'schedule', label: t('asm.native.schedule'), states: ['draft'] },
    { step: 'markHeld', label: t('asm.native.markHeld'), states: ['scheduled'] },
    { step: 'complete', label: t('asm.native.complete'), states: ['held', 'result_updated'] },
    {
      step: 'cancel',
      label: t('common.action.cancel'),
      states: ['draft', 'scheduled'],
    },
    { step: 'reset', label: t('asm.native.reset'), states: ['cancelled'] },
  ];
  const busy = transitions.isPending || populate.isPending || allocate.isPending;
  const runPopulate = async () => {
    setError(null);
    try {
      await populate.mutateAsync(row.id);
    } catch (next) {
      setError(next);
    }
  };
  const runAllocate = async () => {
    setError(null);
    try {
      await allocate.mutateAsync({ examId: row.id, roomIds });
    } catch (next) {
      setError(next);
    }
  };
  return (
    <>
      {available.map((button) =>
        may[button.step] && button.states.includes(status) ? (
          <Button
            key={button.step}
            variant={button.step === 'cancel' ? 'danger' : 'secondary'}
            loading={busy}
            onClick={() =>
              transitions.mutate({
                examId: row.id,
                step: button.step,
                etag: typeof row.etag === 'string' ? row.etag : null,
              })
            }
          >
            {button.label}
          </Button>
        ) : null,
      )}
      {may.populate ? (
        <Button variant="secondary" loading={busy} onClick={() => void runPopulate()}>
          {t('asm.native.populateResults')}
        </Button>
      ) : null}
      {may.allocate ? (
        <>
          {rooms.isError ? <RowError error={rooms.error} t={t} /> : null}
          <SelectField
            label={t('asm.native.chooseRoom')}
            multiple
            value={roomIds}
            onChange={(event) =>
              setRoomIds(Array.from(event.target.selectedOptions).map((option) => option.value))
            }
            options={roomRows.map((room) => ({
              value: room.id,
              label: String(room.name ?? room.code ?? room.id),
            }))}
          />
          <Button
            variant="secondary"
            loading={busy}
            disabled={roomIds.length === 0}
            onClick={() => void runAllocate()}
          >
            {t('asm.native.allocateRooms')}
          </Button>
        </>
      ) : null}
      <RowError error={error ?? transitions.error ?? populate.error ?? allocate.error} t={t} />
    </>
  );
}

export function MarksheetTools({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const action = useMarksheetStep();
  const may = {
    validate: useCan('assessment.marksheet.validate_marksheet'),
    publish: useCan('assessment.marksheet.publish'),
    cancel: useCan('assessment.marksheet.cancel_marksheet'),
    reset: useCan('assessment.marksheet.reset_marksheet'),
  };
  const status = String(row.status);
  const [visible, setVisible] = useState(row.visibleToGuardians === true);
  const available: { step: 'validate' | 'publish' | 'cancel' | 'reset'; label: string; states: string[] }[] =
    [
      { step: 'validate', label: t('asm.native.validate'), states: ['draft'] },
      { step: 'publish', label: t('asm.native.publish'), states: ['validated'] },
      { step: 'cancel', label: t('common.action.cancel'), states: ['draft', 'validated'] },
      { step: 'reset', label: t('asm.native.reset'), states: ['cancelled'] },
    ];
  return (
    <>
      {status === 'validated' && may.publish ? (
        <Switch label={t('asm.native.visibleToGuardians')} checked={visible} onChange={setVisible} />
      ) : null}
      {available.map((button) =>
        may[button.step] && button.states.includes(status) ? (
          <Button
            key={button.step}
            variant={button.step === 'cancel' ? 'danger' : 'secondary'}
            loading={action.isPending}
            onClick={() =>
              action.mutate({
                marksheetId: row.id,
                step: button.step,
                etag: typeof row.etag === 'string' ? row.etag : null,
                ...(button.step === 'publish' ? { visibleToGuardians: visible } : {}),
              })
            }
          >
            {button.label}
          </Button>
        ) : null,
      )}
      <RowError error={action.error} t={t} />
    </>
  );
}

export function GenerateResultsTool({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const mayGenerate = useCan('assessment.result_template.generate_results');
  const generate = useResultTemplateGenerateResults();
  const [job, setJob] = useState<Pick<Operation, 'module' | 'operationId'> | null>(null);
  const monitor = useOperation(job);
  if (!mayGenerate) return null;
  const start = async () => {
    try {
      const operation = await generate.mutateAsync(row.id);
      setJob({ module: 'assessment', operationId: operation.operationId });
    } catch {
      // The mutation exposes its typed error below.
    }
  };
  return (
    <>
      <Button variant="secondary" loading={generate.isPending} onClick={() => void start()}>
        {t('asm.web.generateResults')}
      </Button>
      {job ? <p>{t('asm.web.generationQueued')}</p> : null}
      {monitor.data ? (
        <>
          <StatusChip label={enumLabel(t, 'operation', monitor.data.status)} />
          {monitor.data.progressPercent !== undefined ? (
            <p>{t('asm.web.progress', { value: monitor.data.progressPercent })}</p>
          ) : null}
          {monitor.data.errorMessage ? <p className="sms-field__error">{monitor.data.errorMessage}</p> : null}
        </>
      ) : null}
      <RowError error={generate.error ?? monitor.error} t={t} />
    </>
  );
}
