import {
  lmsFailureText,
  isApiError,
  useCan,
  useExerciseSubmissions,
  useExerciseTestResults,
  useI18n,
  useProgrammingExercise,
  useSubmitExercise,
  type ExerciseTestCase,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Skeleton, StatusChip, TextArea } from '@mysms/ui-web';
import { useState } from 'react';
import { useParams } from 'react-router';
import { UUID } from '../ids';

/**
 * LMS-12 Programming exercise: the problem with its visible test cases, a code editor, and the run's outputs sent
 * for judgement — the server compares each output with its expectation and keeps the submission history. Hidden
 * test cases are judged without showing their expectation.
 */
export function ProgrammingExerciseScreen() {
  const { t } = useI18n();
  const { programmingExerciseId: param = '' } = useParams();
  const mayRead = useCan('lms.programming_exercise.read');
  const programmingExerciseId = mayRead && UUID.test(param) ? param : null;
  const exercise = useProgrammingExercise(programmingExerciseId);
  const title = t('screen.LMS12.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (programmingExerciseId === null || (isApiError(exercise.error) && exercise.error.isNotAvailable))
    return (
      <Card title={title}>
        <p>{t('common.state.notAvailable')}</p>
      </Card>
    );
  if (exercise.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (exercise.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void exercise.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  return (
    <ExercisePanel
      programmingExerciseId={programmingExerciseId}
      title={exercise.data.exercise.title ?? title}
      statement={exercise.data.exercise.problemStatement ?? ''}
      language={exercise.data.exercise.language ?? ''}
      testCases={exercise.data.testCases}
    />
  );
}

function ExercisePanel({
  programmingExerciseId,
  title,
  statement,
  language,
  testCases,
}: {
  programmingExerciseId: string;
  title: string;
  statement: string;
  language: string;
  testCases: ExerciseTestCase[];
}) {
  const { t } = useI18n();
  const maySubmit = useCan('lms.programming_exercise.submit_exercise');
  const submit = useSubmitExercise();
  const submissions = useExerciseSubmissions(
    useCan('lms.exercise_submission.read') ? programmingExerciseId : null,
  );
  const [code, setCode] = useState('');
  const [outputs, setOutputs] = useState<Record<string, string>>({});
  const visible = testCases.filter((testCase) => !testCase.isHidden);
  const latest = submissions.data?.items[0];
  const results = useExerciseTestResults(
    useCan('lms.exercise_test_result.read') && latest?.id ? latest.id : null,
  );
  return (
    <Card title={title}>
      <p>{statement}</p>
      <p className="hint">{t('lms.exercise.language', { language })}</p>
      {visible.length > 0 && (
        <DataTable
          caption={t('lms.exercise.cases')}
          columns={[
            {
              key: 'input',
              header: t('lms.exercise.input'),
              render: (row: ExerciseTestCase) => row.input ?? '',
            },
            {
              key: 'expected',
              header: t('lms.exercise.expected'),
              render: (row: ExerciseTestCase) => row.expectedOutput ?? '',
            },
          ]}
          rows={visible}
          rowKey={(row) => row.id ?? ''}
          loadingLabel={t('common.state.loading')}
          emptyText={t('common.state.empty')}
        />
      )}
      {maySubmit && (
        <>
          <TextArea
            label={t('lms.exercise.code')}
            value={code}
            onChange={(event) => setCode(event.target.value)}
          />
          {testCases.map((testCase, position) => (
            <TextArea
              key={testCase.id}
              label={t('lms.exercise.outputOf', { position: position + 1 })}
              value={outputs[testCase.id ?? ''] ?? ''}
              onChange={(event) => setOutputs((all) => ({ ...all, [testCase.id ?? '']: event.target.value }))}
            />
          ))}
          {submit.isError && <Banner intent="danger">{lmsFailureText(t, submit.error)}</Banner>}
          <Button
            onClick={() =>
              submit.mutate({
                programmingExerciseId,
                request: {
                  code,
                  testOutputs: testCases.map((testCase) => ({
                    testCaseId: testCase.id ?? '',
                    actualOutput: outputs[testCase.id ?? ''] ?? '',
                  })),
                },
              })
            }
            disabled={submit.isPending || code.trim() === ''}
          >
            {t('lms.exercise.submit')}
          </Button>
        </>
      )}
      {latest && (
        <section aria-label={t('lms.exercise.lastRun')}>
          <h2>{t('lms.exercise.lastRun')}</h2>
          <StatusChip
            intent={latest.status === 'passed' ? 'success' : 'danger'}
            label={latest.status === 'passed' ? t('lms.exercise.passed') : t('lms.exercise.failed')}
          />
          {results.data && (
            <ol>
              {results.data.items.map((result) => (
                <li key={result.id}>
                  <StatusChip
                    intent={result.status === 'passed' ? 'success' : 'danger'}
                    label={result.status === 'passed' ? t('lms.exercise.passed') : t('lms.exercise.failed')}
                  />{' '}
                  {result.actualOutput !== undefined && result.actualOutput !== null && (
                    <code>{result.actualOutput}</code>
                  )}
                </li>
              ))}
            </ol>
          )}
        </section>
      )}
    </Card>
  );
}
