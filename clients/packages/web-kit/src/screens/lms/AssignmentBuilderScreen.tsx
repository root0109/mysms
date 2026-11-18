import {
  isApiError,
  useCan,
  useI18n,
  useLmsAssignment,
  useUpdateLmsAssignment,
  lmsFailureText,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, StatusChip, TextArea, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-45 Assignment builder: the task's title, question and model answer edited under its etag. A published
 * assignment no longer changes; the server says so with 409.
 */
export function AssignmentBuilderScreen() {
  const { t } = useI18n();
  const { lmsAssignmentId: param = '' } = useParams();
  const mayRead = useCan('lms.assignment.read');
  const assignmentId = mayRead && UUID.test(param) ? param : null;
  const assignment = useLmsAssignment(assignmentId);
  const title = t('screen.LMS45.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (assignmentId === null || (isApiError(assignment.error) && assignment.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (assignment.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (assignment.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void assignment.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return (
    <EditorPanel
      assignmentId={assignmentId}
      etag={assignment.data.etag}
      published={assignment.data.data.publishedAt !== undefined && assignment.data.data.publishedAt !== null}
      initialTitle={assignment.data.data.title ?? ''}
      initialQuestion={assignment.data.data.question ?? ''}
      initialModelAnswer={assignment.data.data.modelAnswer ?? ''}
    />
  );
}

function EditorPanel({
  assignmentId,
  etag,
  published,
  initialTitle,
  initialQuestion,
  initialModelAnswer,
}: {
  assignmentId: string;
  etag: string | null;
  published: boolean;
  initialTitle: string;
  initialQuestion: string;
  initialModelAnswer: string;
}) {
  const { t } = useI18n();
  const mayEdit = useCan('lms.assignment.update');
  const update = useUpdateLmsAssignment();
  const [name, setName] = useState(initialTitle);
  const [question, setQuestion] = useState(initialQuestion);
  const [modelAnswer, setModelAnswer] = useState(initialModelAnswer);
  const editable = mayEdit && !published && etag !== undefined && etag !== null;
  return (
    <Card title={t('screen.LMS45.title')}>
      {published && <StatusChip intent="info" label={t('lms.builder.published')} />}
      {update.isError && <Banner intent="danger">{lmsFailureText(t, update.error)}</Banner>}
      <TextInput
        label={t('lms.assignmentBuilder.titleField')}
        value={name}
        onChange={(event) => setName(event.target.value)}
        disabled={!editable}
      />
      <TextArea
        label={t('lms.assignmentBuilder.question')}
        value={question}
        onChange={(event) => setQuestion(event.target.value)}
        disabled={!editable}
      />
      <TextArea
        label={t('lms.assignmentBuilder.modelAnswer')}
        hint={t('lms.assignmentBuilder.modelAnswerHint')}
        value={modelAnswer}
        onChange={(event) => setModelAnswer(event.target.value)}
        disabled={!editable}
      />
      {editable && (
        <Button
          onClick={() =>
            update.mutate({
              assignmentId,
              etag: etag as string,
              changes: {
                title: name,
                question,
                ...(modelAnswer.trim() ? { modelAnswer } : {}),
              },
            })
          }
          disabled={update.isPending || name.trim() === '' || question.trim() === ''}
        >
          {t('common.action.save')}
        </Button>
      )}
    </Card>
  );
}
