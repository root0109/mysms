import {
  isApiError,
  useCan,
  useCheckHomeworkSubmission,
  useClassRoster,
  useHomework,
  useHomeworkApprovals,
  useHomeworkStep,
  useHomeworkSubmissions,
  useI18n,
  useStudentNames,
  type Homework,
  type HomeworkStep,
  type HomeworkSubmission,
} from '@mysms/core';
import type { MessageKey, Translate } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  Skeleton,
  StatusChip,
  TextArea,
  TextInput,
  type StatusChipProps,
} from '@mysms/ui-web';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { UUID } from '../ids';
import { HomeworkSubmissionPanel } from '../assessment/SubmissionScreens';

/** The workflow's limit on comments and reasons (api/state-machines.md "Homework"). */
const MAX_TEXT = 4000;
const BOARD = '/app/assessment/homeworks';

const HOMEWORK_STATUS: Readonly<Record<string, { label: MessageKey; intent: StatusChipProps['intent'] }>> = {
  draft: { label: 'hw.status.draft', intent: 'neutral' },
  pending: { label: 'hw.status.pending', intent: 'warning' },
  published: { label: 'hw.status.published', intent: 'success' },
};

function failureText(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return error.errors.map((e) => e.message).join(' ') || t('common.validation.pattern');
    if (error.status === 403) return t('hw.check.forbidden');
  }
  return t('common.error.generic');
}

/**
 * ASM-03 Homework check (flow F5): one homework with where it stands — draft, waiting for review, published — the
 * step the role may take next, and the class as a checklist: who handed in, the teacher's comments, the student's
 * reply. The server decides what the caller may see and do; the screen only offers the steps the role holds.
 */
export function HomeworkCheckScreen() {
  const { t } = useI18n();
  const { homeworkId: param = '' } = useParams();
  const mayRead = useCan('assessment.homework.read');
  const mayStudentSubmit = useCan('assessment.homework_submission.create');
  const mayCheck = useCan('assessment.homework_submission.check_homework_submission');
  const homeworkId = mayRead && UUID.test(param) ? param : null;
  const homework = useHomework(homeworkId);
  const title = t('screen.ASM03.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('hw.check.noAccess')}</p>
      </Card>
    );
  if (homeworkId === null || (isApiError(homework.error) && homework.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
        <Link to={BOARD}>{t('hw.check.back')}</Link>
      </Card>
    );
  if (homework.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (homework.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void homework.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  if (mayStudentSubmit && !mayCheck)
    return <HomeworkSubmissionPanel homeworkId={homeworkId} homework={homework.data.data} />;
  return <HomeworkDetail homeworkId={homeworkId} homework={homework.data.data} etag={homework.data.etag} />;
}

function HomeworkDetail({
  homeworkId,
  homework,
  etag,
}: {
  homeworkId: string;
  homework: Homework;
  etag: string | null;
}) {
  const { t, format } = useI18n();
  const status = homework.status ?? 'draft';
  const shown = HOMEWORK_STATUS[status];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.ASM03.title')}</h1>
        <p className="page__subtitle">
          {homework.dueAt ? `${t('hw.check.due', { date: format.dateTime(homework.dueAt) })} ` : null}
          <StatusChip intent={shown?.intent} label={shown ? t(shown.label) : t('enum.common.unknown')} />
        </p>
        <Link to={BOARD}>{t('hw.check.back')}</Link>
      </header>
      {homework.description ? (
        <Card title={t('hw.check.instructions')}>
          <p className="homework-text">{homework.description}</p>
        </Card>
      ) : null}
      <Lifecycle homeworkId={homeworkId} status={status} etag={etag} />
      {status === 'published' ? (
        <Submissions homeworkId={homeworkId} classSectionId={homework.classSectionId} />
      ) : null}
    </div>
  );
}

/** Where the homework stands before it reaches the class, and the step this role may take. */
function Lifecycle({
  homeworkId,
  status,
  etag,
}: {
  homeworkId: string;
  status: string;
  etag: string | null;
}) {
  const { t } = useI18n();
  const mayPublish = useCan('assessment.homework.publish_homework');
  const mayApprove = useCan('assessment.homework.approve_homework');
  const mayReject = useCan('assessment.homework.reject_homework');
  const mayReadReviews = useCan('assessment.homework_approval.read');
  const reviews = useHomeworkApprovals(mayReadReviews && status !== 'published' ? homeworkId : null);
  const step = useHomeworkStep();
  const [reason, setReason] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const last = reviews.data?.items.at(-1);
  const run = (kind: HomeworkStep) => {
    setNotice(null);
    step.mutate(
      { homeworkId, step: kind, etag, ...(kind === 'publish' ? {} : { reason }) },
      {
        onSuccess: (result) => {
          setReason('');
          setNotice(
            t(
              kind === 'reject'
                ? 'hw.check.sentBack'
                : result.status === 'pending'
                  ? 'hw.check.sentForReview'
                  : 'hw.check.published',
            ),
          );
        },
      },
    );
  };
  const feedback = (
    <>
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {step.error ? <Banner intent="danger">{failureText(t, step.error)}</Banner> : null}
    </>
  );
  if (status === 'published') return feedback;
  if (status === 'pending')
    return (
      <>
        {feedback}
        <Card title={t('hw.status.pending')}>
          <p>{t('hw.check.pending')}</p>
          {mayApprove || mayReject ? (
            <>
              <TextInput
                label={t('hw.check.reason')}
                value={reason}
                maxLength={MAX_TEXT}
                disabled={step.isPending}
                onChange={(event) => setReason(event.target.value)}
              />
              <div className="page__actions">
                {mayReject ? (
                  <Button variant="secondary" loading={step.isPending} onClick={() => run('reject')}>
                    {t('hw.check.reject')}
                  </Button>
                ) : null}
                {mayApprove ? (
                  <Button loading={step.isPending} onClick={() => run('approve')}>
                    {t('hw.check.approve')}
                  </Button>
                ) : null}
              </div>
            </>
          ) : null}
        </Card>
      </>
    );
  return (
    <>
      {feedback}
      <Card title={t('hw.status.draft')}>
        {last?.decision === 'rejected' ? (
          <Banner intent="warning" title={t('hw.check.wasSentBack')}>
            {last.comments ?? t('hw.check.noReason')}
          </Banner>
        ) : null}
        <p>{t('hw.check.draft')}</p>
        {mayPublish ? (
          <div className="page__actions">
            <Button loading={step.isPending} onClick={() => run('publish')}>
              {t(mayApprove ? 'hw.check.publish' : 'hw.check.sendForReview')}
            </Button>
          </div>
        ) : null}
      </Card>
    </>
  );
}

/** The class as a checklist: the roster as placed now, plus anyone who handed in, each with their submission. */
function Submissions({ homeworkId, classSectionId }: { homeworkId: string; classSectionId: string }) {
  const { t } = useI18n();
  const mayReadSubmissions = useCan('assessment.homework_submission.read');
  const mayReadRoster = useCan('sis.student_enrollment.list_class_roster');
  const submissions = useHomeworkSubmissions(mayReadSubmissions ? homeworkId : null);
  const roster = useClassRoster(mayReadRoster ? classSectionId : null);
  const handedIn = new Map((submissions.data?.items ?? []).map((item) => [item.studentId, item]));
  const rollOf = new Map((roster.data?.items ?? []).map((entry) => [entry.studentId, entry.rollNumber]));
  const collator = new Intl.Collator(undefined, { numeric: true });
  const students = [...new Set([...rollOf.keys(), ...handedIn.keys()])].sort((a, b) => {
    const [rollA, rollB] = [rollOf.get(a), rollOf.get(b)];
    return rollA && rollB ? collator.compare(rollA, rollB) : Number(!rollA) - Number(!rollB);
  });
  const names = useStudentNames(students);
  const title = t('hw.check.handedIn', { count: handedIn.size, total: students.length });
  if (!mayReadSubmissions)
    return (
      <Card title={t('hw.check.submissions')}>
        <p className="hint">{t('hw.check.noSubmissionAccess')}</p>
      </Card>
    );
  if (submissions.isPending || (mayReadRoster && roster.isPending))
    return (
      <Card title={t('hw.check.submissions')}>
        <Skeleton label={t('common.state.loading')} lines={5} />
      </Card>
    );
  if (submissions.isError)
    return (
      <Card title={t('hw.check.submissions')}>
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void submissions.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      </Card>
    );
  return (
    <Card title={title}>
      {students.length === 0 ? (
        <p>{t('hw.check.nobody')}</p>
      ) : (
        <ol className="attendance-grid">
          {students.map((studentId, index) => {
            const roll = rollOf.get(studentId);
            return (
              <SubmissionRow
                key={studentId}
                homeworkId={homeworkId}
                name={names.get(studentId) ?? t('att.take.student', { number: index + 1 })}
                roll={roll}
                submission={handedIn.get(studentId)}
              />
            );
          })}
        </ol>
      )}
    </Card>
  );
}

function SubmissionRow({
  homeworkId,
  name,
  roll,
  submission,
}: {
  homeworkId: string;
  name: string;
  roll: string | undefined;
  submission: HomeworkSubmission | undefined;
}) {
  const { t, format } = useI18n();
  const mayCheck = useCan('assessment.homework_submission.check_homework_submission');
  const check = useCheckHomeworkSubmission();
  const saved = submission?.teacherComments ?? '';
  // what the teacher is typing; null while the saved comments are shown as they are
  const [typed, setTyped] = useState<string | null>(null);
  const comments = typed ?? saved;
  // the check operation keeps the saved comments when none are sent, so emptying the box would change nothing
  const cleared = saved.trim() !== '' && comments.trim() === '';
  const checked = submission?.status === 'checked';
  return (
    <li className="attendance-grid__row">
      <div className="attendance-grid__who">
        <span className="attendance-grid__name">{name}</span>
        {roll ? <span className="attendance-grid__roll">{t('att.take.roll', { roll })}</span> : null}
      </div>
      {submission ? (
        <StatusChip
          intent={checked ? 'success' : 'info'}
          label={t(checked ? 'hw.check.checked' : 'hw.check.unchecked')}
        />
      ) : (
        <StatusChip intent="neutral" label={t('hw.check.notHandedIn')} />
      )}
      {submission ? (
        <div className="attendance-grid__detail">
          {submission.submittedOn ? (
            <p className="attendance-grid__note">
              {t('hw.check.submittedOn', { date: format.day(submission.submittedOn) })}
            </p>
          ) : null}
          {submission.studentReply ? (
            <p className="homework-text">
              <strong>{t('hw.check.reply')}</strong> {submission.studentReply}
            </p>
          ) : null}
          {mayCheck ? (
            <>
              <TextArea
                label={t('hw.check.commentsFor', { name })}
                rows={2}
                value={comments}
                maxLength={MAX_TEXT}
                disabled={check.isPending}
                hint={cleared ? t('hw.check.commentsStay') : undefined}
                error={check.error ? failureText(t, check.error) : undefined}
                onChange={(event) => setTyped(event.target.value)}
              />
              <div className="page__actions">
                <Button
                  variant={checked ? 'secondary' : 'primary'}
                  loading={check.isPending}
                  disabled={checked && (cleared || comments.trim() === saved.trim())}
                  onClick={() =>
                    check.mutate(
                      {
                        homeworkId,
                        submissionId: submission.id ?? '',
                        etag: submission.etag ?? null,
                        teacherComments: comments,
                      },
                      { onSuccess: () => setTyped(null) },
                    )
                  }
                >
                  {t(checked ? 'hw.check.updateComments' : 'hw.check.markChecked')}
                </Button>
              </div>
            </>
          ) : saved ? (
            <p className="homework-text">
              <strong>{t('hw.check.comments')}</strong> {saved}
            </p>
          ) : null}
        </div>
      ) : null}
    </li>
  );
}
