import {
  lmsFailureText,
  isApiError,
  useLmsAssignmentSubmissions,
  useCan,
  useContentItem,
  useGradeEntry,
  useGradeLearningSubmission,
  useGradeQuizAnswers,
  useI18n,
  useQuizAnswers,
  useQuizSubmissions,
  useRubricBundle,
  type ContentItem,
  type LmsAssignmentSubmission,
  type QuizSubmission,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, StatusChip, TextArea, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-32 Item grading: the queue of one graded item's submissions, the selected work in the middle, and the grade
 * on the right — points with feedback through GradeEntry, a teacher's marks on open quiz answers, the pass/fail
 * word on an assignment, and the item's rubric when it has one.
 */
export function ItemGradingScreen() {
  const { t } = useI18n();
  const { contentItemId: param = '' } = useParams();
  const mayGrade = useCan('lms.gradebook_entry.grade_entry');
  const contentItemId = mayGrade && UUID.test(param) ? param : null;
  const item = useContentItem(contentItemId);
  const title = t('screen.LMS32.title');
  if (!mayGrade)
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
  if (item.isPending) return <Skeleton label={t('common.state.loading')} lines={8} />;
  if (item.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void item.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return <GradingBoard contentItemId={contentItemId} item={item.data} />;
}

function GradingBoard({ contentItemId, item }: { contentItemId: string; item: ContentItem }) {
  const { t } = useI18n();
  const isQuiz = item.itemKind === 'quiz';
  const mayReadQuiz = useCan('lms.quiz_submission.read');
  const mayReadAssignment = useCan('lms.assignment_submission.read');
  const quizQueue = useQuizSubmissions(isQuiz && mayReadQuiz ? `contentItemId = "${contentItemId}"` : null);
  const workQueue = useLmsAssignmentSubmissions(
    !isQuiz && mayReadAssignment ? `contentItemId = "${contentItemId}"` : null,
  );
  const rubric = useRubricBundle(item.rubricId ?? null);
  const [selected, setSelected] = useState<string | null>(null);
  const queue: Array<{ id: string; who: string; status: string }> = isQuiz
    ? (quizQueue.data?.items ?? []).map((attempt: QuizSubmission) => ({
        id: attempt.id ?? '',
        who: attempt.userId ?? '',
        status: attempt.status ?? '',
      }))
    : (workQueue.data?.items ?? []).map((submission: LmsAssignmentSubmission) => ({
        id: submission.id ?? '',
        who: submission.userId ?? submission.groupId ?? '',
        status: submission.status ?? '',
      }));
  const current = selected ?? queue[0]?.id ?? null;
  const loading = isQuiz ? quizQueue.isPending : workQueue.isPending;
  return (
    <Card title={item.title ?? t('screen.LMS32.title')}>
      {loading ? (
        <Skeleton label={t('common.state.loading')} lines={4} />
      ) : queue.length === 0 ? (
        <p className="hint">{t('lms.grading.emptyQueue')}</p>
      ) : (
        <div className="sms-grading-layout">
          <nav aria-label={t('lms.grading.queue')}>
            <ol>
              {queue.map((entry) => (
                <li key={entry.id}>
                  <Button
                    variant={entry.id === current ? 'primary' : 'secondary'}
                    onClick={() => setSelected(entry.id)}
                  >
                    {entry.who} — {entry.status}
                  </Button>
                </li>
              ))}
            </ol>
          </nav>
          {current !== undefined &&
            current !== null &&
            (isQuiz ? (
              <QuizMarking quizSubmissionId={current} />
            ) : (
              <WorkGrading
                contentItemId={contentItemId}
                itemPoints={item.points ?? null}
                submission={(workQueue.data?.items ?? []).find((s) => s.id === current)}
              />
            ))}
        </div>
      )}
      {rubric.data && (
        <section aria-label={t('lms.grading.rubric')}>
          <h2>{t('lms.grading.rubric')}</h2>
          <ul>
            {rubric.data.criteria.map((criterion) => (
              <li key={criterion.id}>
                {criterion.name}{' '}
                {criterion.weightPercent !== undefined &&
                  criterion.weightPercent !== null &&
                  `(${criterion.weightPercent}%)`}
              </li>
            ))}
          </ul>
        </section>
      )}
    </Card>
  );
}

/** A teacher's marks for the answers of one submitted quiz attempt. */
function QuizMarking({ quizSubmissionId }: { quizSubmissionId: string }) {
  const { t } = useI18n();
  const answers = useQuizAnswers(quizSubmissionId);
  const grade = useGradeQuizAnswers();
  const mayMark = useCan('lms.quiz_submission.grade_quiz_answers');
  const [marks, setMarks] = useState<Record<string, string>>({});
  if (answers.isPending) return <Skeleton label={t('common.state.loading')} lines={4} />;
  if (answers.isError) return <Banner intent="danger">{t('common.error.generic')}</Banner>;
  const open = answers.data.items;
  return (
    <section aria-label={t('lms.grading.answers')}>
      {grade.isError && <Banner intent="danger">{lmsFailureText(t, grade.error)}</Banner>}
      <ol>
        {open.map((answer) => (
          <li key={answer.id}>
            <p>{answer.questionText}</p>
            <p>{answer.answer ?? t('lms.grading.noAnswer')}</p>
            {answer.isCorrect !== undefined && answer.isCorrect !== null && (
              <StatusChip
                intent={answer.isCorrect ? 'success' : 'danger'}
                label={`${answer.marksAwarded ?? ''} / ${answer.marksPossible ?? ''}`}
              />
            )}
            {mayMark && answer.questionId && (
              <TextInput
                label={t('lms.grading.marksOutOf', { possible: answer.marksPossible ?? '' })}
                inputMode="decimal"
                value={marks[answer.questionId] ?? ''}
                onChange={(event) =>
                  setMarks((all) => ({ ...all, [answer.questionId as string]: event.target.value }))
                }
              />
            )}
          </li>
        ))}
      </ol>
      {mayMark && (
        <Button
          onClick={() =>
            grade.mutate({
              quizSubmissionId,
              answers: Object.entries(marks)
                .filter(([, value]) => value.trim() !== '')
                .map(([questionId, value]) => ({ questionId, marks: value.trim() })),
            })
          }
          disabled={grade.isPending || Object.values(marks).every((value) => value.trim() === '')}
        >
          {t('lms.grading.saveMarks')}
        </Button>
      )}
    </section>
  );
}

/** Points and the pass/fail word on one assignment submission. */
function WorkGrading({
  contentItemId,
  itemPoints,
  submission,
}: {
  contentItemId: string;
  itemPoints: string | null;
  submission: LmsAssignmentSubmission | undefined;
}) {
  const { t } = useI18n();
  const gradeEntry = useGradeEntry();
  const evaluate = useGradeLearningSubmission();
  const mayEvaluate = useCan('lms.assignment_submission.grade_learning_submission');
  const [points, setPoints] = useState('');
  const [feedback, setFeedback] = useState('');
  if (!submission) return null;
  const failure = gradeEntry.error ?? evaluate.error;
  return (
    <section aria-label={t('lms.grading.work')}>
      <p>{submission.answer ?? submission.title ?? ''}</p>
      {submission.isFinal === false && <Banner intent="info">{t('lms.grading.stillADraft')}</Banner>}
      {failure !== undefined && failure !== null && (
        <Banner intent="danger">{lmsFailureText(t, failure)}</Banner>
      )}
      {mayEvaluate && submission.id && (
        <div role="group" aria-label={t('lms.grading.evaluation')}>
          <Button
            variant="secondary"
            onClick={() =>
              evaluate.mutate({
                assignmentSubmissionId: submission.id as string,
                request: { status: 'pass' },
              })
            }
            disabled={evaluate.isPending}
          >
            {t('lms.grading.pass')}
          </Button>
          <Button
            variant="secondary"
            onClick={() =>
              evaluate.mutate({
                assignmentSubmissionId: submission.id as string,
                request: { status: 'fail' },
              })
            }
            disabled={evaluate.isPending}
          >
            {t('lms.grading.fail')}
          </Button>
        </div>
      )}
      {submission.userId !== undefined && submission.userId !== null && (
        <>
          <TextInput
            label={
              itemPoints !== undefined && itemPoints !== null
                ? t('lms.grading.pointsOutOf', { possible: itemPoints })
                : t('lms.grading.points')
            }
            inputMode="decimal"
            value={points}
            onChange={(event) => setPoints(event.target.value)}
          />
          <TextArea
            label={t('lms.grading.feedback')}
            value={feedback}
            onChange={(event) => setFeedback(event.target.value)}
          />
          <Button
            onClick={() =>
              gradeEntry.mutate({
                contentItemId,
                userId: submission.userId as string,
                grade: {
                  points: points.trim(),
                  rubricCellIds: [],
                  ...(feedback.trim() ? { feedback: feedback.trim() } : {}),
                },
              })
            }
            disabled={gradeEntry.isPending || points.trim() === ''}
          >
            {t('lms.grading.saveGrade')}
          </Button>
        </>
      )}
    </section>
  );
}
