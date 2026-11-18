import {
  lmsFailureText,
  isApiError,
  useCan,
  useCheckQuizAnswer,
  useContentItem,
  useI18n,
  useQuizAttemptPause,
  useSaveQuizResponses,
  useStartQuizAttempt,
  useSubmitQuiz,
  type AnswerCheck,
  type QuizAnswerInput,
  type QuizAttempt,
} from '@mysms/core';
import { Banner, Button, Card, ProgressBar, Skeleton, StatusChip } from '@mysms/ui-web';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { UUID } from '../ids';

/** How often unsent answers go to the server while the student works. */
const AUTOSAVE_MS = 30_000;

type Question = NonNullable<QuizAttempt['questions']>[number];

/**
 * LMS-08 Quiz player: starts (or picks up) the student's attempt of a quiz placement, keeps answers saved while the
 * timer runs, offers pause where the quiz allows breaks and instant feedback where it is a practice quiz, and hands
 * the submitted attempt over to its result screen. Late saves and submissions are judged by the server.
 */
export function QuizPlayerScreen() {
  const { t } = useI18n();
  const { contentItemId: param = '' } = useParams();
  const mayStart = useCan('lms.quiz.start_quiz_attempt');
  const contentItemId = UUID.test(param) ? param : null;
  const item = useContentItem(mayStart ? contentItemId : null);
  const start = useStartQuizAttempt();
  const title = t('screen.LMS08.title');
  if (!mayStart)
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
  if (item.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
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
  if (start.data) return <AttemptPanel contentItemId={contentItemId} attempt={start.data} />;
  return (
    <Card title={item.data.title ?? title}>
      <p>{t('lms.quiz.start.hint')}</p>
      {start.isError && <Banner intent="danger">{lmsFailureText(t, start.error)}</Banner>}
      <Button onClick={() => start.mutate({ contentItemId })} disabled={start.isPending}>
        {t('lms.quiz.start.action')}
      </Button>
    </Card>
  );
}

function AttemptPanel({ contentItemId, attempt }: { contentItemId: string; attempt: QuizAttempt }) {
  const { t } = useI18n();
  const navigate = useNavigate();
  const save = useSaveQuizResponses();
  const pause = useQuizAttemptPause();
  const submit = useSubmitQuiz();
  const check = useCheckQuizAnswer();
  const mayCheck = useCan('lms.quiz.check_quiz_answer');
  const questions = attempt.questions ?? [];
  const [index, setIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, QuizAnswerInput>>({});
  const [paused, setPaused] = useState(false);
  const [feedback, setFeedback] = useState<Record<string, AnswerCheck>>({});
  const [remaining, setRemaining] = useState<number | null>(
    secondsLeft(attempt.deadlineAt, attempt.serverTime),
  );
  const sequence = useRef(0);
  const dirty = useRef(new Set<string>());
  const submissionId = attempt.quizSubmissionId ?? '';
  const token = attempt.attemptToken ?? '';

  const flush = () => {
    if (dirty.current.size === 0 || paused) return;
    const changed = [...dirty.current]
      .map((id) => answers[id])
      .filter((a): a is QuizAnswerInput => Boolean(a));
    dirty.current.clear();
    sequence.current += 1;
    save.mutate({
      quizSubmissionId: submissionId,
      request: { attemptToken: token, clientSequence: String(sequence.current), answers: changed },
    });
  };
  const flushRef = useRef(flush);
  flushRef.current = flush;

  useEffect(() => {
    const saver = setInterval(() => flushRef.current(), AUTOSAVE_MS);
    const clock = setInterval(
      () => setRemaining((left) => (left === null ? null : Math.max(0, left - 1))),
      1_000,
    );
    return () => {
      clearInterval(saver);
      clearInterval(clock);
    };
  }, []);

  const timeUp = remaining !== null && remaining <= 0;
  const question = questions[index];
  const answered = Object.keys(answers).length;

  const finish = (reason: 'manual' | 'time_up') => {
    submit.mutate(
      {
        quizSubmissionId: submissionId,
        request: { attemptToken: token, answers: Object.values(answers), submissionReason: reason },
      },
      { onSuccess: (done) => void navigate(`/app/lms/quiz-submissions/${done.id ?? submissionId}`) },
    );
  };

  return (
    <Card title={attempt.quiz?.title ?? t('screen.LMS08.title')}>
      {remaining !== null && (
        <p aria-live="polite">
          <StatusChip
            intent={remaining < 60 ? 'warning' : 'neutral'}
            label={t('lms.quiz.timer', { minutes: Math.floor(remaining / 60), seconds: remaining % 60 })}
          />
        </p>
      )}
      <ProgressBar value={(answered / Math.max(1, questions.length)) * 100} label={t('lms.quiz.progress')} />
      {timeUp && <Banner intent="warning">{t('lms.quiz.timeUp')}</Banner>}
      {save.isError && <Banner intent="warning">{lmsFailureText(t, save.error)}</Banner>}
      {submit.isError && <Banner intent="danger">{lmsFailureText(t, submit.error)}</Banner>}
      {paused ? (
        <>
          <p>{t('lms.quiz.pausedBody')}</p>
          <Button
            onClick={() =>
              pause.mutate(
                { quizSubmissionId: submissionId, resume: true },
                { onSuccess: () => setPaused(false) },
              )
            }
            disabled={pause.isPending}
          >
            {t('lms.quiz.resume')}
          </Button>
        </>
      ) : (
        question && (
          <QuestionPanel
            question={question}
            position={index + 1}
            total={questions.length}
            value={answers[question.questionId ?? '']}
            feedback={feedback[question.questionId ?? '']}
            onChange={(input) => {
              const id = question.questionId ?? '';
              setAnswers((all) => ({ ...all, [id]: input }));
              dirty.current.add(id);
            }}
            onCheck={
              mayCheck && attempt.quiz?.showFeedback && attempt.quiz.showAnswers
                ? () => {
                    const id = question.questionId ?? '';
                    const answer = answers[id];
                    if (!answer || !attempt.quiz?.id) return;
                    check.mutate(
                      { quizId: attempt.quiz.id, questionId: id, answer },
                      { onSuccess: (result) => setFeedback((all) => ({ ...all, [id]: result })) },
                    );
                  }
                : undefined
            }
          />
        )
      )}
      <div role="group" aria-label={t('lms.quiz.navigation')}>
        <Button
          variant="secondary"
          onClick={() => {
            flush();
            setIndex((i) => Math.max(0, i - 1));
          }}
          disabled={index === 0 || paused}
        >
          {t('lms.quiz.previous')}
        </Button>
        <Button
          variant="secondary"
          onClick={() => {
            flush();
            setIndex((i) => Math.min(questions.length - 1, i + 1));
          }}
          disabled={index >= questions.length - 1 || paused}
        >
          {t('lms.quiz.next')}
        </Button>
        {attempt.quiz?.allowPause && !paused && !timeUp && (
          <Button
            variant="secondary"
            onClick={() => {
              flush();
              pause.mutate(
                { quizSubmissionId: submissionId, resume: false },
                { onSuccess: () => setPaused(true) },
              );
            }}
            disabled={pause.isPending}
          >
            {t('lms.quiz.pause')}
          </Button>
        )}
        <Button onClick={() => finish(timeUp ? 'time_up' : 'manual')} disabled={submit.isPending || paused}>
          {t('lms.quiz.submit')}
        </Button>
      </div>
      <p className="hint">{t('lms.quiz.contentItem', { contentItemId })}</p>
    </Card>
  );
}

function QuestionPanel({
  question,
  position,
  total,
  value,
  feedback,
  onChange,
  onCheck,
}: {
  question: Question;
  position: number;
  total: number;
  value: QuizAnswerInput | undefined;
  feedback: AnswerCheck | undefined;
  onChange: (input: QuizAnswerInput) => void;
  onCheck?: () => void;
}) {
  const { t } = useI18n();
  const questionId = question.questionId ?? '';
  const selected = new Set(value?.selectedOptionIds ?? []);
  const options = question.options ?? [];
  const choose = (optionId: string, multiple: boolean) => {
    const next = multiple ? new Set(selected) : new Set<string>();
    if (multiple && next.has(optionId)) next.delete(optionId);
    else next.add(optionId);
    onChange(answerInput(questionId, { selectedOptionIds: [...next] }));
  };
  return (
    <fieldset>
      <legend>
        {t('lms.quiz.questionOf', { position, total })} {question.questionText}
      </legend>
      {options.length > 0 &&
        options.map((option) => (
          <label key={option.optionId}>
            <input
              type={question.allowsMultiple ? 'checkbox' : 'radio'}
              name={questionId}
              checked={selected.has(option.optionId)}
              onChange={() => choose(option.optionId, Boolean(question.allowsMultiple))}
            />
            {option.optionText}
          </label>
        ))}
      {options.length === 0 && question.questionType === 'numeric' && (
        <input
          type="text"
          inputMode="decimal"
          aria-label={t('lms.quiz.numericAnswer')}
          value={value?.numericValue ?? ''}
          onChange={(event) => onChange(answerInput(questionId, { numericValue: event.target.value }))}
        />
      )}
      {options.length === 0 && question.questionType !== 'numeric' && (
        <textarea
          aria-label={t('lms.quiz.textAnswer')}
          value={value?.answerText ?? ''}
          onChange={(event) => onChange(answerInput(questionId, { answerText: event.target.value }))}
        />
      )}
      {onCheck && (
        <Button variant="secondary" onClick={onCheck} disabled={!value}>
          {t('lms.quiz.check')}
        </Button>
      )}
      {feedback && (
        <Banner intent={feedback.isCorrect ? 'success' : 'warning'}>
          {feedback.isCorrect ? t('lms.quiz.correct') : t('lms.quiz.incorrect')}
          {feedback.explanation ? ` ${feedback.explanation}` : ''}
        </Banner>
      )}
    </fieldset>
  );
}

function answerInput(questionId: string, partial: Partial<QuizAnswerInput>): QuizAnswerInput {
  return {
    questionId,
    selectedOptionIds: [],
    blankAnswers: [],
    matchAnswers: [],
    hotspotPoints: [],
    answeredAt: new Date().toISOString(),
    ...partial,
  };
}

function secondsLeft(
  deadlineAt: string | null | undefined,
  serverTime: string | null | undefined,
): number | null {
  if (!deadlineAt) return null;
  const base = serverTime ? new Date(serverTime).getTime() : Date.now();
  return Math.max(0, Math.round((new Date(deadlineAt).getTime() - base) / 1_000));
}
