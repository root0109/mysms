import {
  isApiError,
  useCan,
  useI18n,
  useQuizAnswers,
  useQuizSubmission,
  type QuizAnswer,
  type QuizSubmission,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, Skeleton, StatusChip, type StatusChipProps } from '@mysms/ui-web';
import { useParams } from 'react-router';
import { UUID } from '../ids';

const STATUS: Readonly<Record<string, { label: MessageKey; intent: StatusChipProps['intent'] }>> = {
  in_progress: { label: 'lms.result.status.inProgress', intent: 'info' },
  paused: { label: 'lms.result.status.paused', intent: 'neutral' },
  submitted: { label: 'lms.result.status.submitted', intent: 'warning' },
  graded: { label: 'lms.result.status.graded', intent: 'success' },
};

/**
 * LMS-09 Quiz result & review: one attempt with its outcome — score, percentage, time — and every answer with the
 * marks and feedback the quiz's settings let the student see. The server already projected what may be shown.
 */
export function QuizResultScreen() {
  const { t } = useI18n();
  const { quizSubmissionId: param = '' } = useParams();
  const mayRead = useCan('lms.quiz_submission.read');
  const quizSubmissionId = mayRead && UUID.test(param) ? param : null;
  const submission = useQuizSubmission(quizSubmissionId);
  const answers = useQuizAnswers(useCan('lms.quiz_answer.read') ? quizSubmissionId : null);
  const title = t('screen.LMS09.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (quizSubmissionId === null || (isApiError(submission.error) && submission.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (submission.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (submission.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void submission.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return <ResultPanel attempt={submission.data} answers={answers.data?.items} />;
}

function ResultPanel({ attempt, answers }: { attempt: QuizSubmission; answers: QuizAnswer[] | undefined }) {
  const { t } = useI18n();
  const status = STATUS[attempt.status ?? ''];
  return (
    <Card title={t('screen.LMS09.title')}>
      <p>
        {status && <StatusChip intent={status.intent} label={t(status.label)} />}{' '}
        {attempt.score !== undefined && attempt.score !== null && (
          <strong>{t('lms.result.score', { score: attempt.score, outOf: attempt.scoreOutOf ?? '' })}</strong>
        )}{' '}
        {attempt.percentage !== undefined &&
          attempt.percentage !== null &&
          t('lms.result.percentage', { percentage: attempt.percentage })}
      </p>
      <dl>
        <dt>{t('lms.result.attempt')}</dt>
        <dd>{attempt.attemptNumber ?? 1}</dd>
        <dt>{t('lms.result.timeTaken')}</dt>
        <dd>{t('lms.result.minutes', { minutes: Math.ceil((attempt.timeTakenSeconds ?? 0) / 60) })}</dd>
        <dt>{t('lms.result.correct')}</dt>
        <dd>
          {attempt.correctCount ?? 0} / {(attempt.correctCount ?? 0) + (attempt.incorrectCount ?? 0)}
        </dd>
      </dl>
      {attempt.status === 'submitted' && <Banner intent="info">{t('lms.result.awaitingMarks')}</Banner>}
      {answers === undefined ? (
        <Skeleton label={t('common.state.loading')} lines={4} />
      ) : answers.length === 0 ? (
        <p className="hint">{t('lms.result.noAnswers')}</p>
      ) : (
        <ol>
          {answers.map((answer) => (
            <li key={answer.id}>
              <p>{answer.questionText}</p>
              {answer.isCorrect !== undefined && answer.isCorrect !== null && (
                <StatusChip
                  intent={answer.isCorrect ? 'success' : 'danger'}
                  label={answer.isCorrect ? t('lms.result.answer.correct') : t('lms.result.answer.incorrect')}
                />
              )}{' '}
              {answer.marksAwarded !== undefined && answer.marksAwarded !== null && (
                <span>
                  {t('lms.result.marks', {
                    awarded: answer.marksAwarded,
                    possible: answer.marksPossible ?? '',
                  })}
                </span>
              )}
              {answer.feedback && <p className="hint">{answer.feedback}</p>}
            </li>
          ))}
        </ol>
      )}
    </Card>
  );
}
