import {
  lmsFailureText,
  isApiError,
  useCan,
  useI18n,
  useQuestionSearch,
  useQuiz,
  useQuizQuestions,
  useSetQuizQuestions,
  useUpdateQuiz,
  type LmsQuestion,
  type QuizQuestion,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Skeleton, StatusChip, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-44 Quiz builder: the quiz's settings edited under its etag and its question set — bank questions searched on
 * the right (the quiz's own already excluded) and added or removed in one SetQuizQuestions step. A published quiz
 * no longer changes; the server says so with 409.
 */
export function QuizBuilderScreen() {
  const { t } = useI18n();
  const { quizId: param = '' } = useParams();
  const mayRead = useCan('lms.quiz.read');
  const quizId = mayRead && UUID.test(param) ? param : null;
  const quiz = useQuiz(quizId);
  const title = t('screen.LMS44.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (quizId === null || (isApiError(quiz.error) && quiz.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (quiz.isPending) return <Skeleton label={t('common.state.loading')} lines={8} />;
  if (quiz.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void quiz.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return (
    <BuilderPanel
      quizId={quizId}
      title={quiz.data.data.title ?? title}
      published={quiz.data.data.publishedAt !== undefined && quiz.data.data.publishedAt !== null}
      passingPercentage={String(quiz.data.data.passingPercentage ?? '')}
      etag={quiz.data.etag}
    />
  );
}

function BuilderPanel({
  quizId,
  title,
  published,
  passingPercentage,
  etag,
}: {
  quizId: string;
  title: string;
  published: boolean;
  passingPercentage: string;
  etag: string | null;
}) {
  const { t } = useI18n();
  const mayEdit = useCan('lms.quiz.update');
  const maySet = useCan('lms.quiz.set_quiz_questions');
  const maySearch = useCan('lms.question.search_question_bank');
  const update = useUpdateQuiz();
  const setQuestions = useSetQuizQuestions();
  const placed = useQuizQuestions(quizId);
  const [query, setQuery] = useState('');
  const [passing, setPassing] = useState(passingPercentage);
  const bank = useQuestionSearch(maySearch ? query : '', [], quizId);
  const failure = update.error ?? setQuestions.error;
  return (
    <Card title={title}>
      {published && <StatusChip intent="info" label={t('lms.builder.published')} />}
      {failure !== undefined && failure !== null && (
        <Banner intent="danger">{lmsFailureText(t, failure)}</Banner>
      )}
      {mayEdit && !published && (
        <div role="group" aria-label={t('lms.builder.settings')}>
          <TextInput
            label={t('lms.builder.passing')}
            inputMode="numeric"
            value={passing}
            onChange={(event) => setPassing(event.target.value)}
          />
          <Button
            variant="secondary"
            onClick={() =>
              etag !== undefined &&
              etag !== null &&
              update.mutate({ quizId, etag, changes: { passingPercentage: Number(passing) } })
            }
            disabled={update.isPending || etag === undefined || etag === null || passing.trim() === ''}
          >
            {t('common.action.save')}
          </Button>
        </div>
      )}
      <section aria-label={t('lms.builder.placed')}>
        <h2>{t('lms.builder.placed')}</h2>
        <DataTable
          caption={t('lms.builder.placed')}
          columns={[
            {
              key: 'question',
              header: t('lms.bank.question'),
              render: (row: QuizQuestion) => row.questionId ?? '',
            },
            { key: 'marks', header: t('lms.bank.marks'), render: (row: QuizQuestion) => row.marks ?? '' },
            {
              key: 'actions',
              header: t('lms.bank.actions'),
              render: (row: QuizQuestion) =>
                maySet && !published && row.questionId ? (
                  <Button
                    variant="secondary"
                    onClick={() =>
                      setQuestions.mutate({
                        quizId,
                        request: { removeQuestionIds: [row.questionId as string] },
                      })
                    }
                    disabled={setQuestions.isPending}
                  >
                    {t('lms.builder.remove')}
                  </Button>
                ) : null,
            },
          ]}
          rows={placed.data?.items}
          rowKey={(row) => row.id ?? ''}
          loading={placed.isPending}
          loadingLabel={t('common.state.loading')}
          emptyText={t('lms.builder.noQuestions')}
        />
      </section>
      {maySearch && maySet && !published && (
        <section aria-label={t('lms.builder.bank')}>
          <h2>{t('lms.builder.bank')}</h2>
          <TextInput
            label={t('lms.bank.search')}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <DataTable
            caption={t('lms.builder.bank')}
            columns={[
              {
                key: 'question',
                header: t('lms.bank.question'),
                render: (row: LmsQuestion) => row.questionText ?? '',
              },
              {
                key: 'actions',
                header: t('lms.bank.actions'),
                render: (row: LmsQuestion) =>
                  row.id ? (
                    <Button
                      variant="secondary"
                      onClick={() =>
                        setQuestions.mutate({ quizId, request: { addQuestionIds: [row.id as string] } })
                      }
                      disabled={setQuestions.isPending}
                    >
                      {t('lms.builder.add')}
                    </Button>
                  ) : null,
              },
            ]}
            rows={bank.data?.data}
            rowKey={(row) => row.id ?? ''}
            loading={bank.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('lms.bank.empty')}
          />
        </section>
      )}
    </Card>
  );
}
