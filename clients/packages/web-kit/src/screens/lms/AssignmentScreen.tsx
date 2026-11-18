import {
  lmsFailureText,
  isApiError,
  useLmsAssignmentSubmissions,
  useAssignmentSubmissionSteps,
  useCan,
  useContentItem,
  useI18n,
  useLmsAssignment,
  useSession,
  useSubmitForPlagiarismCheck,
  type LmsAssignmentSubmission,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, StatusChip, TextArea } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-10 LMS assignment: the task as placed for the student, their draft kept until they mark it final, and —
 * where the school checks originality — sending the finalized work for a plagiarism check. Drafts change and can
 * be deleted; a final submission is evidence and stays.
 */
export function AssignmentScreen() {
  const { t } = useI18n();
  const { contentItemId: param = '' } = useParams();
  const mayReadItem = useCan('lms.content_item.read');
  const mayReadAssignment = useCan('lms.assignment.read');
  const mayRead = mayReadItem && mayReadAssignment;
  const contentItemId = mayRead && UUID.test(param) ? param : null;
  const item = useContentItem(contentItemId);
  const assignmentId = item.data?.assignmentId ?? null;
  const assignment = useLmsAssignment(assignmentId);
  const title = t('screen.LMS10.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (contentItemId === null || (isApiError(item.error) && item.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (item.isPending || (assignmentId !== null && assignment.isPending))
    return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (item.isError || assignment.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button
            variant="secondary"
            onClick={() => void (item.isError ? item.refetch() : assignment.refetch())}
          >
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  if (assignmentId === null)
    return (
      <Card title={title}>
        <p>{t('lms.assignment.notAnAssignment')}</p>
      </Card>
    );
  return (
    <Card title={assignment.data?.data.title ?? title}>
      <p>{assignment.data?.data.question}</p>
      <SubmissionPanel assignmentId={assignmentId} />
    </Card>
  );
}

function SubmissionPanel({ assignmentId }: { assignmentId: string }) {
  const { t } = useI18n();
  const userId = useSession((s) => s.me?.user?.id) ?? '';
  const maySubmit = useCan('lms.assignment_submission.create');
  const mayCheck = useCan('lms.plagiarism_report.submit_for_plagiarism_check');
  const mine = useLmsAssignmentSubmissions(
    useCan('lms.assignment_submission.read')
      ? `assignmentId = "${assignmentId}" AND userId = "${userId}"`
      : null,
  );
  const steps = useAssignmentSubmissionSteps();
  const plagiarism = useSubmitForPlagiarismCheck();
  const [answer, setAnswer] = useState('');
  if (mine.isPending) return <Skeleton label={t('common.state.loading')} lines={3} />;
  if (mine.isError) return <Banner intent="danger">{t('common.error.generic')}</Banner>;
  const current: LmsAssignmentSubmission | undefined = mine.data.items[0];
  const failure = steps.create.error ?? steps.update.error ?? steps.finalize.error ?? plagiarism.error;
  return (
    <section aria-label={t('lms.assignment.mySubmission')}>
      <h2>{t('lms.assignment.mySubmission')}</h2>
      {failure !== undefined && failure !== null && (
        <Banner intent="danger">{lmsFailureText(t, failure)}</Banner>
      )}
      {current === undefined || current === null ? (
        maySubmit ? (
          <>
            <TextArea
              label={t('lms.assignment.answer')}
              value={answer}
              onChange={(event) => setAnswer(event.target.value)}
            />
            <Button
              onClick={() => steps.create.mutate({ assignmentId, userId, answer, isFinal: false })}
              disabled={steps.create.isPending || answer.trim() === ''}
            >
              {t('lms.assignment.saveDraft')}
            </Button>
          </>
        ) : (
          <p className="hint">{t('common.state.empty')}</p>
        )
      ) : current.isFinal ? (
        <>
          <StatusChip intent="success" label={t('lms.assignment.final')} />
          <p>{current.answer}</p>
          {current.status && current.status !== 'not_graded' && (
            <p>
              {t('lms.assignment.evaluation', { status: current.status })}{' '}
              {current.evaluatorComments && <span className="hint">{current.evaluatorComments}</span>}
            </p>
          )}
          {mayCheck && current.attachmentFileId !== undefined && current.attachmentFileId !== null && (
            <Button
              variant="secondary"
              onClick={() => plagiarism.mutate({ entityAttachmentId: current.attachmentFileId as string })}
              disabled={plagiarism.isPending}
            >
              {t('lms.assignment.checkOriginality')}
            </Button>
          )}
        </>
      ) : (
        <>
          <StatusChip intent="warning" label={t('lms.assignment.draft')} />
          <TextArea
            label={t('lms.assignment.answer')}
            defaultValue={current.answer ?? ''}
            onBlur={(event) => {
              if (event.target.value !== current.answer && current.id && current.etag)
                steps.update.mutate({
                  assignmentSubmissionId: current.id,
                  etag: current.etag,
                  changes: { answer: event.target.value },
                });
            }}
          />
          <Button
            onClick={() => current.id && steps.finalize.mutate({ assignmentSubmissionId: current.id })}
            disabled={steps.finalize.isPending}
          >
            {t('lms.assignment.handIn')}
          </Button>
          <Button
            variant="secondary"
            onClick={() =>
              current.id &&
              current.etag &&
              steps.remove.mutate({ assignmentSubmissionId: current.id, etag: current.etag })
            }
            disabled={steps.remove.isPending}
          >
            {t('lms.assignment.discardDraft')}
          </Button>
        </>
      )}
    </section>
  );
}
