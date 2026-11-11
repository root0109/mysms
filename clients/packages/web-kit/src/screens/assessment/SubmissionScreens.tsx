import {
  isApiError,
  RESOURCES,
  useAssignmentSubmissionStep,
  useAssignmentSubmissions,
  useCan,
  useCreateHomeworkSubmission,
  useCreateRecord,
  useHomeworkSubmissions,
  useI18n,
  useMyStudent,
  useRecord,
  useReplyToHomeworkFeedback,
  useUpdateRecord,
  type AssignmentSubmission,
  type Homework,
  type RecordRow,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, Skeleton, StatusChip, TextArea, TextInput } from '@mysms/ui-web';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { ManagedResourcePanel } from './ManagedResourcePanel';

function useOnline() {
  const [online, setOnline] = useState(() => typeof navigator === 'undefined' || navigator.onLine);
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const on = () => setOnline(true);
    const off = () => setOnline(false);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    return () => {
      window.removeEventListener('online', on);
      window.removeEventListener('offline', off);
    };
  }, []);
  return online;
}

function OfflineBanner() {
  const online = useOnline();
  const { t } = useI18n();
  return online ? null : <Banner intent="warning">{t('common.state.offline')}</Banner>;
}

function errorText(error: unknown, t: ReturnType<typeof useI18n>['t']): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 403) return t('adm.records.forbidden');
    if (error.status === 404) return t('common.state.notAvailable');
    if (error.status === 409) return error.detail ?? t('common.error.generic');
    if (error.status === 422)
      return error.errors.map((item) => item.message).join(' ') || t('common.validation.pattern');
  }
  return t('common.error.generic');
}

export function HomeworkSubmissionPanel({
  homeworkId,
  homework,
}: {
  homeworkId: string;
  homework: Homework;
}) {
  const { t, format } = useI18n();
  const online = useOnline();
  const mayCreate = useCan('assessment.homework_submission.create');
  const mayRead = useCan('assessment.homework_submission.read');
  const mayReply = useCan('assessment.homework_submission.reply_to_homework_feedback');
  const student = useMyStudent();
  const submissions = useHomeworkSubmissions(mayRead ? homeworkId : null);
  const create = useCreateHomeworkSubmission();
  const reply = useReplyToHomeworkFeedback();
  const [text, setText] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  if (!mayCreate) {
    return (
      <Card title={t('screen.ASM04.title')}>
        <p className="hint">{t('asm.web.noAccess')}</p>
      </Card>
    );
  }
  if (student.isPending || (mayRead && submissions.isPending))
    return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (student.isError || (mayRead && submissions.isError)) {
    const error = student.error ?? submissions.error;
    return (
      <Banner
        intent="danger"
        action={
          <Button
            variant="secondary"
            onClick={() => {
              void student.refetch();
              if (mayRead) void submissions.refetch();
            }}
          >
            {t('common.action.retry')}
          </Button>
        }
      >
        {errorText(error, t)}
      </Banner>
    );
  }
  const studentId = student.data?.id;
  const own = studentId ? submissions.data?.items.find((item) => item.studentId === studentId) : undefined;
  const createSubmission = async () => {
    if (!studentId || !online || create.isPending) return;
    setFailure(null);
    setNotice(null);
    try {
      await create.mutateAsync({ homeworkId, studentId });
      setNotice(t('asm.web.submissionSaved'));
    } catch (error) {
      setFailure(error);
    }
  };
  const sendReply = async () => {
    if (!own?.id || !text.trim() || !online || reply.isPending) return;
    setFailure(null);
    setNotice(null);
    try {
      await reply.mutateAsync({
        homeworkId,
        submissionId: own.id,
        etag: own.etag ?? null,
        studentReply: text,
      });
      setText('');
      setNotice(t('asm.web.submissionSaved'));
    } catch (error) {
      setFailure(error);
    }
  };
  return (
    <div className="page">
      <OfflineBanner />
      <header className="page__header">
        <h1 className="page__title">{t('screen.ASM04.title')}</h1>
        <p className="page__subtitle">
          {homework.dueAt
            ? t('hw.check.due', { date: format.dateTime(homework.dueAt) })
            : t('screen.ASM04.title')}
        </p>
        <Link to="/app/assessment/homeworks">{t('hw.check.back')}</Link>
      </header>
      <Card title={t('hw.check.instructions')}>
        <p>{homework.description ?? t('common.state.notAvailable')}</p>
        <p className="hint">{t('asm.web.homeworkRecordOnly')}</p>
        <p className="hint">{t('asm.native.attachmentsUnavailable')}</p>
      </Card>
      {student.data ? null : <Banner intent="warning">{t('asm.web.noStudent')}</Banner>}
      {own ? (
        <Card title={t('asm.native.submitted')}>
          <StatusChip label={enumLabel(t, 'assessment.status', own.status)} />
          {own.submittedOn ? (
            <p>
              {t('asm.native.created')}: {format.day(own.submittedOn)}
            </p>
          ) : null}
          {own.teacherComments ? (
            <p>
              <strong>{t('asm.native.feedback')}:</strong> {own.teacherComments}
            </p>
          ) : null}
          {own.studentReply ? (
            <p>
              <strong>{t('asm.native.reply')}:</strong> {own.studentReply}
            </p>
          ) : null}
          {mayReply && own.teacherComments ? (
            <>
              <TextArea
                label={t('asm.native.replyLabel')}
                value={text}
                maxLength={4000}
                onChange={(event) => setText(event.target.value)}
              />
              <Button
                loading={reply.isPending}
                disabled={!text.trim() || !online}
                onClick={() => void sendReply()}
              >
                {t('asm.native.reply')}
              </Button>
            </>
          ) : null}
        </Card>
      ) : (
        <Card title={t('asm.native.submit')}>
          {homework.status !== 'published' ? (
            <p>{homework.status === 'pending' ? t('hw.check.pending') : t('hw.check.draft')}</p>
          ) : (
            <Button
              loading={create.isPending}
              disabled={!studentId || !online || !mayRead}
              onClick={() => void createSubmission()}
            >
              {t('asm.web.createSubmission')}
            </Button>
          )}
        </Card>
      )}
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {failure ? <Banner intent="danger">{errorText(failure, t)}</Banner> : null}
    </div>
  );
}

function StudentAssignmentSubmission({ assignmentId }: { assignmentId: string }) {
  const { t } = useI18n();
  const online = useOnline();
  const student = useMyStudent();
  const mayRead = useCan('assessment.assignment_submission.read');
  const mayReset = useCan('assessment.assignment_submission.reset_assignment_submission');
  const submissionDef = RESOURCES.find((item) => item.key === 'assessment.AssignmentSubmission');
  const create = useCreateRecord(submissionDef!);
  const update = useUpdateRecord(submissionDef!);
  const step = useAssignmentSubmissionStep();
  const submissions = useAssignmentSubmissions(mayRead ? assignmentId : null);
  const [text, setText] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  if (student.isPending || (mayRead && submissions.isPending))
    return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (student.isError || (mayRead && submissions.isError)) {
    const error = student.error ?? submissions.error;
    return (
      <Banner
        intent="danger"
        action={
          <Button
            variant="secondary"
            onClick={() => {
              void student.refetch();
              if (mayRead) void submissions.refetch();
            }}
          >
            {t('common.action.retry')}
          </Button>
        }
      >
        {errorText(error, t)}
      </Banner>
    );
  }
  const studentId = student.data?.id;
  const own = studentId ? submissions.data?.items.find((item) => item.studentId === studentId) : undefined;
  const canEdit = !own || own.status === 'draft' || own.status === 'change_requested';
  const busy = create.isPending || update.isPending || step.isPending;
  const submit = async () => {
    if (!studentId || !text.trim() || !online || busy || !mayRead) return;
    setNotice(null);
    setFailure(null);
    try {
      let submission: RecordRow;
      if (own?.id) {
        if (!own.etag) throw new Error('The submission is missing its entity tag.');
        submission = await update.mutateAsync({
          id: own.id,
          etag: own.etag,
          values: { content: text.trim() },
        });
      } else {
        submission = await create.mutateAsync({
          values: { assignmentId, studentId, submittedAt: new Date().toISOString(), content: text.trim() },
        });
      }
      if (!submission.id) throw new Error('The submission id was not returned.');
      await step.mutateAsync({
        submissionId: submission.id,
        assignmentId,
        step: 'submit',
        etag: typeof submission.etag === 'string' ? submission.etag : (own?.etag ?? null),
      });
      setNotice(t('asm.web.submissionSaved'));
    } catch (error) {
      setFailure(error);
    }
  };
  const reset = async () => {
    if (!own?.id || !online || busy) return;
    setFailure(null);
    try {
      await step.mutateAsync({ submissionId: own.id, assignmentId, step: 'reset', etag: own.etag ?? null });
      setNotice(t('asm.web.submissionSaved'));
    } catch (error) {
      setFailure(error);
    }
  };
  return (
    <>
      {student.data ? null : <Banner intent="warning">{t('asm.web.noStudent')}</Banner>}
      <OfflineBanner />
      {own ? (
        <StatusChip label={enumLabel(t, 'assessment.assignment_submission_status', own.status)} />
      ) : null}
      {canEdit ? (
        <>
          <TextArea
            label={t('asm.web.submitText')}
            value={text || (own?.content ?? '')}
            maxLength={8000}
            onChange={(event) => setText(event.target.value)}
          />
          <Button
            loading={busy}
            disabled={!studentId || !text.trim() || !online || !mayRead}
            onClick={() => void submit()}
          >
            {own ? t('asm.web.updateSubmission') : t('asm.web.submitAssignment')}
          </Button>
        </>
      ) : null}
      {own?.status === 'cancelled' && mayReset ? (
        <Button variant="secondary" loading={busy} disabled={!online} onClick={() => void reset()}>
          {t('asm.native.reset')}
        </Button>
      ) : null}
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {failure ? <Banner intent="danger">{errorText(failure, t)}</Banner> : null}
    </>
  );
}

function SubmissionReview({
  assignmentId,
  submission,
}: {
  assignmentId: string;
  submission: AssignmentSubmission;
}) {
  const { t, format } = useI18n();
  const step = useAssignmentSubmissionStep();
  const mayAccept = useCan('assessment.assignment_submission.accept_assignment_submission');
  const mayReject = useCan('assessment.assignment_submission.reject_assignment_submission');
  const mayChanges = useCan('assessment.assignment_submission.request_submission_changes');
  const mayGrade = useCan('assessment.assignment_submission.grade_assignment_submission');
  const [marks, setMarks] = useState(submission.marksObtained ?? '');
  const [feedback, setFeedback] = useState(submission.feedback ?? '');
  const [reason, setReason] = useState('');
  const status = String(submission.status);
  const run = (action: 'accept' | 'reject' | 'requestChanges' | 'grade') =>
    step.mutate({
      submissionId: submission.id ?? '',
      assignmentId,
      step: action,
      etag: submission.etag ?? null,
      ...(action === 'grade' ? { marks, feedback } : {}),
      ...(action === 'reject' || action === 'requestChanges' ? { reason } : {}),
    });
  return (
    <>
      <div>
        <p>{submission.content ?? t('common.state.notAvailable')}</p>
        <p>
          {submission.submittedAt ? format.dateTime(submission.submittedAt) : t('common.state.notAvailable')}
        </p>
      </div>
      {status === 'submitted' && mayAccept ? (
        <Button variant="secondary" loading={step.isPending} onClick={() => run('accept')}>
          {t('asm.native.approve')}
        </Button>
      ) : null}
      {status === 'submitted' && (mayReject || mayChanges) ? (
        <>
          <TextInput
            label={t('asm.native.approvalNote')}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          {mayReject ? (
            <Button
              variant="danger"
              loading={step.isPending}
              disabled={!reason.trim()}
              onClick={() => run('reject')}
            >
              {t('asm.native.reject')}
            </Button>
          ) : null}
          {mayChanges ? (
            <Button
              variant="secondary"
              loading={step.isPending}
              disabled={!reason.trim()}
              onClick={() => run('requestChanges')}
            >
              {t('asm.native.requestChanges')}
            </Button>
          ) : null}
        </>
      ) : null}
      {mayGrade && ['accepted', 'submitted'].includes(status) ? (
        <>
          <TextInput
            label={t('asm.native.marks')}
            inputMode="decimal"
            value={marks}
            onChange={(event) => setMarks(event.target.value)}
          />
          <TextArea
            label={t('asm.native.feedback')}
            value={feedback}
            onChange={(event) => setFeedback(event.target.value)}
          />
          <Button loading={step.isPending} disabled={!marks.trim()} onClick={() => run('grade')}>
            {t('asm.web.gradeSubmission')}
          </Button>
        </>
      ) : null}
      {step.error ? (
        <p className="sms-field__error" role="alert">
          {errorText(step.error, t)}
        </p>
      ) : null}
    </>
  );
}

export function AssignmentDetailScreen() {
  const { t } = useI18n();
  const { assignmentId: param = '' } = useParams();
  const assignmentId = param;
  const mayRead = useCan('assessment.assignment.read');
  const maySubmit = useCan('assessment.assignment_submission.create');
  const mayReadSubmissions = useCan('assessment.assignment_submission.read');
  const mayGrade = useCan('assessment.assignment_submission.grade_assignment_submission');
  const mayAccept = useCan('assessment.assignment_submission.accept_assignment_submission');
  const mayReject = useCan('assessment.assignment_submission.reject_assignment_submission');
  const mayRequestChanges = useCan('assessment.assignment_submission.request_submission_changes');
  const mayReviewSubmissions = mayReadSubmissions || mayGrade || mayAccept || mayReject || mayRequestChanges;
  const def = RESOURCES.find((item) => item.key === 'assessment.Assignment');
  const assignment = useRecord(mayRead ? def : undefined, mayRead ? assignmentId || null : null);
  const submissions = useAssignmentSubmissions(mayReviewSubmissions ? assignmentId : null);
  if (!mayRead && !maySubmit && !mayReviewSubmissions) {
    return (
      <Card title={t('screen.ASM06.title')}>
        <p className="hint">{t('asm.web.noAccess')}</p>
      </Card>
    );
  }
  if (mayRead && assignment.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (mayRead && assignment.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void assignment.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {errorText(assignment.error, t)}
      </Banner>
    );
  const row = assignment.data?.data;
  if (mayRead && !row)
    return (
      <Card title={t('screen.ASM06.title')}>
        <p>{t('asm.web.noAssignment')}</p>
      </Card>
    );
  const studentView = maySubmit && !mayReviewSubmissions;
  if (studentView) {
    return (
      <div className="page">
        <OfflineBanner />
        <header className="page__header">
          <h1 className="page__title">{t('screen.ASM07.title')}</h1>
          <p className="page__subtitle">{String(row?.title ?? row?.name ?? '')}</p>
          <p className="page__subtitle">
            <StatusChip label={enumLabel(t, 'assessment.status', String(row?.status ?? ''))} />
          </p>
        </header>
        <Card title={String(row?.title ?? row?.name ?? t('screen.ASM07.title'))}>
          <p>{String(row?.description ?? t('common.state.notAvailable'))}</p>
          <p>
            {t('asm.native.due')}: {String(row?.dueAt ?? t('common.state.notAvailable'))}
          </p>
          <StudentAssignmentSubmission assignmentId={assignmentId} />
        </Card>
      </div>
    );
  }
  return (
    <div className="page">
      <OfflineBanner />
      <header className="page__header">
        <h1 className="page__title">{t('screen.ASM06.title')}</h1>
        <p className="page__subtitle">{String(row?.title ?? row?.name ?? '')}</p>
        <p className="page__subtitle">
          <StatusChip label={enumLabel(t, 'assessment.status', String(row?.status ?? ''))} />
        </p>
        <Link to="/app/assessment/assignments">{t('screen.ASM05.title')}</Link>
      </header>
      {!mayReviewSubmissions ? (
        <Card title={t('asm.native.submissions')}>
          <p className="hint">{t('asm.web.noAccess')}</p>
        </Card>
      ) : (
        <>
          {submissions.isError ? (
            <Banner
              intent="danger"
              action={
                <Button variant="secondary" onClick={() => void submissions.refetch()}>
                  {t('common.action.retry')}
                </Button>
              }
            >
              {errorText(submissions.error, t)}
            </Banner>
          ) : null}
          {submissions.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={5} />
          ) : (
            <Card title={t('asm.web.assignmentSubmissions')}>
              <DataTable
                caption={t('asm.web.assignmentSubmissions')}
                rows={submissions.data?.items}
                rowKey={(item) => item.id ?? item.studentId}
                loading={false}
                loadingLabel={t('common.state.loading')}
                emptyText={t('asm.web.noAssignments')}
                columns={[
                  { key: 'student', header: t('asm.native.student'), render: (item) => item.studentId },
                  {
                    key: 'status',
                    header: t('asm.native.currentStatus'),
                    render: (item) => enumLabel(t, 'assessment.assignment_submission_status', item.status),
                  },
                  {
                    key: 'review',
                    header: t('asm.native.feedback'),
                    render: (item) => <SubmissionReview assignmentId={assignmentId} submission={item} />,
                  },
                ]}
              />
            </Card>
          )}
          <ManagedResourcePanel
            resource="assessment.AssignmentAllocation"
            title={t('asm.web.assignmentAllocations')}
            parentId={assignmentId}
          />
          <p className="hint">{t('asm.native.attachmentsUnavailable')}</p>
        </>
      )}
    </div>
  );
}
