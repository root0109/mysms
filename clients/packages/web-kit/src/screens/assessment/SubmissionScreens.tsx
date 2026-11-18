import {
  RESOURCES,
  useAssignmentSubmissionStep,
  useAssignmentSubmissions,
  useCan,
  useCreateHomeworkSubmission,
  useCreateRecord,
  useHomeworkSubmissions,
  useI18n,
  useMyStudentIds,
  useRecord,
  useReplyToHomeworkFeedback,
  useUpdateRecord,
  type AssignmentSubmission,
  type Homework,
  type RecordRow,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, Skeleton, StatusChip, TextArea, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { assessmentErrorText as errorText, useOnline } from './assessmentText';
import { OfflineBanner } from './assessmentUi';
import { ManagedResourcePanel } from './ManagedResourcePanel';

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
  const studentId = useMyStudentIds()[0];
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
  if (mayRead && submissions.isPending) return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (mayRead && submissions.isError) {
    return (
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
    );
  }
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
    if (!own?.id || !own.etag || !text.trim() || !online || reply.isPending) return;
    setFailure(null);
    setNotice(null);
    try {
      await reply.mutateAsync({
        homeworkId,
        submissionId: own.id,
        etag: own.etag,
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
      {studentId ? null : <Banner intent="warning">{t('asm.web.noStudent')}</Banner>}
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
                disabled={!text.trim() || !online || !own.etag}
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

/** The states in which a student may change their own submission and hand it in (again). */
const EDITABLE = ['draft', 'change_requested'];
/** The states from which a returned submission can be taken back to a draft. */
const RESETTABLE = ['rejected', 'change_requested'];

function StudentAssignmentSubmission({
  assignmentId,
  studentId,
}: {
  assignmentId: string;
  studentId: string;
}) {
  const { t } = useI18n();
  const online = useOnline();
  const mayCreate = useCan('assessment.assignment_submission.create');
  const mayUpdate = useCan('assessment.assignment_submission.update');
  const maySubmit = useCan('assessment.assignment_submission.submit_assignment_submission');
  const mayReset = useCan('assessment.assignment_submission.reset_assignment_submission');
  const submissionDef = RESOURCES.find((item) => item.key === 'assessment.AssignmentSubmission');
  const create = useCreateRecord(submissionDef!);
  const update = useUpdateRecord(submissionDef!);
  const step = useAssignmentSubmissionStep();
  // only the student's own submission is asked for (the service shows a student nothing else)
  const submissions = useAssignmentSubmissions(assignmentId, studentId);
  const [text, setText] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  if (submissions.isPending) return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (submissions.isError) {
    return (
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
    );
  }
  const own = submissions.data?.items.find((item) => item.studentId === studentId);
  const canEdit = own ? EDITABLE.includes(own.status ?? '') && mayUpdate && Boolean(own.etag) : mayCreate;
  const busy = create.isPending || update.isPending || step.isPending;
  const submit = async () => {
    if (!canEdit || !maySubmit || !text.trim() || !online || busy) return;
    setNotice(null);
    setFailure(null);
    try {
      let submission: RecordRow;
      if (own?.id && own.etag) {
        submission = await update.mutateAsync({
          id: own.id,
          etag: own.etag,
          values: { content: text.trim() },
        });
      } else {
        // the service records when the work was handed in
        submission = await create.mutateAsync({ values: { assignmentId, studentId, content: text.trim() } });
      }
      if (typeof submission.etag !== 'string' || !submission.etag) {
        throw new Error('The submission came back without its entity tag.');
      }
      await step.mutateAsync({
        submissionId: submission.id,
        assignmentId,
        step: 'submit',
        etag: submission.etag,
      });
      setText('');
      setNotice(t('asm.web.submissionSaved'));
    } catch (error) {
      setFailure(error);
    }
  };
  const reset = async () => {
    if (!own?.id || !own.etag || !online || busy) return;
    setFailure(null);
    try {
      await step.mutateAsync({ submissionId: own.id, assignmentId, step: 'reset', etag: own.etag });
      setNotice(t('asm.web.submissionSaved'));
    } catch (error) {
      setFailure(error);
    }
  };
  return (
    <>
      <OfflineBanner />
      {own ? (
        <StatusChip label={enumLabel(t, 'assessment.assignment_submission_status', own.status)} />
      ) : null}
      {own?.feedback ? (
        <p>
          <strong>{t('asm.native.feedback')}:</strong> {own.feedback}
        </p>
      ) : null}
      {canEdit && maySubmit ? (
        <>
          <TextArea
            label={t('asm.web.submitText')}
            value={text || (own?.content ?? '')}
            maxLength={8000}
            onChange={(event) => setText(event.target.value)}
          />
          <Button loading={busy} disabled={!text.trim() || !online} onClick={() => void submit()}>
            {own ? t('asm.web.updateSubmission') : t('asm.web.submitAssignment')}
          </Button>
        </>
      ) : null}
      {own && RESETTABLE.includes(own.status ?? '') && mayReset ? (
        <Button
          variant="secondary"
          loading={busy}
          disabled={!online || !own.etag}
          onClick={() => void reset()}
        >
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
  const ready = Boolean(submission.id && submission.etag);
  const run = (action: 'accept' | 'reject' | 'requestChanges' | 'grade') =>
    submission.id &&
    submission.etag &&
    step.mutate({
      submissionId: submission.id,
      assignmentId,
      step: action,
      etag: submission.etag,
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
        <Button variant="secondary" loading={step.isPending} disabled={!ready} onClick={() => run('accept')}>
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
              disabled={!ready || !reason.trim()}
              onClick={() => run('reject')}
            >
              {t('asm.native.reject')}
            </Button>
          ) : null}
          {mayChanges ? (
            <Button
              variant="secondary"
              loading={step.isPending}
              disabled={!ready || !reason.trim()}
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
          <Button loading={step.isPending} disabled={!ready || !marks.trim()} onClick={() => run('grade')}>
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
  const { t, format } = useI18n();
  const { assignmentId: param = '' } = useParams();
  const assignmentId = param;
  const mayRead = useCan('assessment.assignment.read');
  const maySubmit = useCan('assessment.assignment_submission.submit_assignment_submission');
  const mayReadSubmissions = useCan('assessment.assignment_submission.read');
  const mayGrade = useCan('assessment.assignment_submission.grade_assignment_submission');
  const mayAccept = useCan('assessment.assignment_submission.accept_assignment_submission');
  const mayReject = useCan('assessment.assignment_submission.reject_assignment_submission');
  const mayRequestChanges = useCan('assessment.assignment_submission.request_submission_changes');
  // a teacher reviews; the read grant alone does not make one (a student's role carries it too)
  const reviewer = mayGrade || mayAccept || mayReject || mayRequestChanges;
  // the student view belongs to whoever is a student (a student record of their own) and reviews nothing
  const ownStudentId = useMyStudentIds()[0];
  const studentView = ownStudentId !== undefined && !reviewer;
  const def = RESOURCES.find((item) => item.key === 'assessment.Assignment');
  const assignment = useRecord(mayRead ? def : undefined, mayRead ? assignmentId || null : null);
  const submissions = useAssignmentSubmissions(reviewer && mayReadSubmissions ? assignmentId : null);
  if (!mayRead && !studentView && !reviewer) {
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
            {typeof row?.dueAt === 'string' && row.dueAt
              ? t('asm.native.due', { date: format.dateTime(row.dueAt) })
              : t('common.state.notAvailable')}
          </p>
          {maySubmit || mayReadSubmissions ? (
            <StudentAssignmentSubmission assignmentId={assignmentId} studentId={ownStudentId} />
          ) : (
            <p className="hint">{t('asm.web.noAccess')}</p>
          )}
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
      {!reviewer || !mayReadSubmissions ? (
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
