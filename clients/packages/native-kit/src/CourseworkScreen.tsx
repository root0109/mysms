import {
  useCan,
  useContentItem,
  useGradebook,
  useGradeEntry,
  useI18n,
  useLmsAssignment,
  useLmsAssignmentSubmissions,
  useAssignmentGroupMembers,
  useAssignmentSubmissionSteps,
  useDriveItems,
  useGroupWorkspaceSteps,
  useQuizAnswers,
  useQuizSubmission,
  useQuizSubmissions,
  useSaveQuizResponses,
  useSession,
  useShareDriveItem,
  useStartQuizAttempt,
  useSubmitQuiz,
  type QuizAnswerInput,
  type QuizAttempt,
} from '@mysms/core';
import { Banner, Button, Screen, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import type { CourseworkScreenId, LmsRoute } from './lmsRoutes';
import { QueryFeedback } from './records';

export interface CourseworkScreenProps {
  route: LmsRoute<CourseworkScreenId>;
  tablet: boolean;
  onBack: () => void;
}

/**
 * One LMS workspace for the phone and the tablet, selected by the shared screen id and deep-link parameters: the
 * quiz attempt and its result, the assignment hand-in, the student's grades, and — on the tablet, the LMS's main
 * device — the group workspace, the gradebook, item grading and the drive.
 */
export function CourseworkScreen({ route, tablet, onBack }: CourseworkScreenProps) {
  const { t } = useI18n();
  return (
    <Screen tablet={tablet}>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      <Body route={route} tablet={tablet} />
    </Screen>
  );
}

function Body({ route, tablet }: { route: LmsRoute<CourseworkScreenId>; tablet: boolean }) {
  const screenId: CourseworkScreenId = route.screenId;
  if (screenId === 'LMS-08') return <QuizAttemptPanel contentItemId={route.params.contentItemId ?? ''} />;
  if (screenId === 'LMS-09')
    return <QuizResultPanel quizSubmissionId={route.params.quizSubmissionId ?? ''} />;
  if (screenId === 'LMS-10') return <AssignmentPanel contentItemId={route.params.contentItemId ?? ''} />;
  if (screenId === 'LMS-33') return <MyGradesPanel cohortId={route.params.cohortId ?? ''} />;
  if (screenId === 'LMS-11') return <GroupPanel assignmentGroupId={route.params.assignmentGroupId ?? ''} />;
  if (screenId === 'LMS-31') return <GradebookPanel cohortId={route.params.cohortId ?? ''} />;
  if (screenId === 'LMS-32') return <GradingPanel contentItemId={route.params.contentItemId ?? ''} />;
  return <DrivePanel tablet={tablet} />;
}

// --------------------------------------------------------------------------------------- LMS-08 quiz attempt

function QuizAttemptPanel({ contentItemId }: { contentItemId: string }) {
  const { t } = useI18n();
  const mayStart = useCan('lms.quiz.start_quiz_attempt');
  const item = useContentItem(mayStart && contentItemId !== '' ? contentItemId : null);
  const start = useStartQuizAttempt();
  if (!mayStart) return <Text>{t('lms.common.forbidden')}</Text>;
  if (item.isPending || item.isError)
    return <QueryFeedback query={item} hasCachedRows={false} onRetry={() => void item.refetch()} />;
  if (start.data) return <QuizQuestions attempt={start.data} />;
  return (
    <View>
      <Text accessibilityRole="header">{item.data.title ?? t('screen.LMS08.title')}</Text>
      <Text>{t('lms.quiz.start.hint')}</Text>
      {start.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      <Button onPress={() => start.mutate({ contentItemId })} disabled={start.isPending}>
        {t('lms.quiz.start.action')}
      </Button>
    </View>
  );
}

function QuizQuestions({ attempt }: { attempt: QuizAttempt }) {
  const { t } = useI18n();
  const theme = useTheme();
  const save = useSaveQuizResponses();
  const submit = useSubmitQuiz();
  const [answers, setAnswers] = useState<Record<string, QuizAnswerInput>>({});
  const [sequence, setSequence] = useState(0);
  const questions = attempt.questions ?? [];
  const submissionId = attempt.quizSubmissionId ?? '';
  const token = attempt.attemptToken ?? '';
  const put = (questionId: string, partial: Partial<QuizAnswerInput>) => {
    setAnswers((all) => ({
      ...all,
      [questionId]: {
        questionId,
        selectedOptionIds: [],
        blankAnswers: [],
        matchAnswers: [],
        hotspotPoints: [],
        answeredAt: new Date().toISOString(),
        ...partial,
      },
    }));
  };
  return (
    <View>
      <Text accessibilityRole="header">{attempt.quiz?.title ?? t('screen.LMS08.title')}</Text>
      {questions.map((question, index) => (
        <View key={question.questionId} style={{ marginBottom: theme.space[5] }}>
          <Text>
            {t('lms.quiz.questionOf', { position: index + 1, total: questions.length })}{' '}
            {question.questionText}
          </Text>
          {(question.options ?? []).map((option) => {
            const chosen = answers[question.questionId ?? '']?.selectedOptionIds.includes(option.optionId);
            return (
              <Button
                key={option.optionId}
                variant={chosen ? 'primary' : 'secondary'}
                onPress={() => put(question.questionId ?? '', { selectedOptionIds: [option.optionId] })}
              >
                {option.optionText}
              </Button>
            );
          })}
          {(question.options ?? []).length === 0 ? (
            <TextInput
              label={t('lms.quiz.textAnswer')}
              value={
                answers[question.questionId ?? '']?.answerText ??
                answers[question.questionId ?? '']?.numericValue ??
                ''
              }
              onChangeText={(text) =>
                put(
                  question.questionId ?? '',
                  question.questionType === 'numeric' ? { numericValue: text } : { answerText: text },
                )
              }
            />
          ) : null}
        </View>
      ))}
      {save.isError || submit.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {submit.data ? (
        <Banner intent="success">
          {t('lms.result.score', { score: submit.data.score ?? '', outOf: submit.data.scoreOutOf ?? '' })}
        </Banner>
      ) : (
        <View style={{ flexDirection: 'row', gap: theme.space[4] }}>
          <Button
            variant="secondary"
            onPress={() => {
              setSequence((n) => n + 1);
              save.mutate({
                quizSubmissionId: submissionId,
                request: {
                  attemptToken: token,
                  clientSequence: String(sequence + 1),
                  answers: Object.values(answers),
                },
              });
            }}
            disabled={save.isPending}
          >
            {t('common.action.save')}
          </Button>
          <Button
            onPress={() =>
              submit.mutate({
                quizSubmissionId: submissionId,
                request: { attemptToken: token, answers: Object.values(answers), submissionReason: 'manual' },
              })
            }
            disabled={submit.isPending}
          >
            {t('lms.quiz.submit')}
          </Button>
        </View>
      )}
    </View>
  );
}

// --------------------------------------------------------------------------------------- LMS-09 quiz result

function QuizResultPanel({ quizSubmissionId }: { quizSubmissionId: string }) {
  const { t } = useI18n();
  const mayRead = useCan('lms.quiz_submission.read');
  const mayAnswers = useCan('lms.quiz_answer.read');
  const result = useQuizSubmission(mayRead && quizSubmissionId !== '' ? quizSubmissionId : null);
  const answers = useQuizAnswers(mayAnswers && quizSubmissionId !== '' ? quizSubmissionId : null);
  if (!mayRead) return <Text>{t('lms.common.forbidden')}</Text>;
  if (result.isPending || result.isError)
    return <QueryFeedback query={result} hasCachedRows={false} onRetry={() => void result.refetch()} />;
  return (
    <View>
      <Text accessibilityRole="header">{t('screen.LMS09.title')}</Text>
      <Text>
        {t('lms.result.score', { score: result.data.score ?? '', outOf: result.data.scoreOutOf ?? '' })}{' '}
        {result.data.percentage !== undefined && result.data.percentage !== null
          ? t('lms.result.percentage', { percentage: result.data.percentage })
          : ''}
      </Text>
      {result.data.status === 'submitted' ? (
        <Banner intent="info">{t('lms.result.awaitingMarks')}</Banner>
      ) : null}
      {(answers.data?.items ?? []).map((answer) => (
        <View key={answer.id}>
          <Text>{answer.questionText}</Text>
          <Text>
            {answer.marksAwarded !== undefined && answer.marksAwarded !== null
              ? t('lms.result.marks', { awarded: answer.marksAwarded, possible: answer.marksPossible ?? '' })
              : t('lms.result.status.submitted')}
          </Text>
        </View>
      ))}
    </View>
  );
}

// --------------------------------------------------------------------------------------- LMS-10 assignment

function AssignmentPanel({ contentItemId }: { contentItemId: string }) {
  const { t } = useI18n();
  const mayItem = useCan('lms.content_item.read');
  const mayAssignment = useCan('lms.assignment.read');
  const userId = useSession((s) => s.me?.user?.id) ?? '';
  const item = useContentItem(mayItem && contentItemId !== '' ? contentItemId : null);
  const assignmentId = item.data?.assignmentId ?? null;
  const assignment = useLmsAssignment(mayAssignment ? assignmentId : null);
  const mine = useLmsAssignmentSubmissions(
    assignmentId !== undefined && assignmentId !== null
      ? `assignmentId = "${assignmentId}" AND userId = "${userId}"`
      : null,
  );
  const steps = useAssignmentSubmissionSteps();
  const [answer, setAnswer] = useState('');
  if (!mayItem || !mayAssignment) return <Text>{t('lms.common.forbidden')}</Text>;
  if (item.isPending || item.isError)
    return <QueryFeedback query={item} hasCachedRows={false} onRetry={() => void item.refetch()} />;
  const current = mine.data?.items[0];
  return (
    <View>
      <Text accessibilityRole="header">{assignment.data?.data.title ?? t('screen.LMS10.title')}</Text>
      <Text>{assignment.data?.data.question ?? ''}</Text>
      {steps.create.isError || steps.finalize.isError ? (
        <Banner intent="danger">{t('common.error.generic')}</Banner>
      ) : null}
      {current === undefined || current === null ? (
        <View>
          <TextInput label={t('lms.assignment.answer')} value={answer} onChangeText={setAnswer} multiline />
          <Button
            onPress={() =>
              assignmentId !== undefined &&
              assignmentId !== null &&
              steps.create.mutate({ assignmentId, userId, answer, isFinal: false })
            }
            disabled={steps.create.isPending || answer.trim() === ''}
          >
            {t('lms.assignment.saveDraft')}
          </Button>
        </View>
      ) : current.isFinal ? (
        <View>
          <Banner intent="success">{t('lms.assignment.final')}</Banner>
          <Text>{current.answer ?? ''}</Text>
        </View>
      ) : (
        <View>
          <Text>{current.answer ?? ''}</Text>
          <Button
            onPress={() =>
              current.id !== undefined &&
              current.id !== null &&
              steps.finalize.mutate({ assignmentSubmissionId: current.id })
            }
            disabled={steps.finalize.isPending}
          >
            {t('lms.assignment.handIn')}
          </Button>
        </View>
      )}
    </View>
  );
}

// --------------------------------------------------------------------------------------- LMS-33 my grades

function MyGradesPanel({ cohortId }: { cohortId: string }) {
  const { t } = useI18n();
  const mayRead = useCan('lms.cohort.get_gradebook');
  const userId = useSession((s) => s.me?.user?.id);
  const gradebook = useGradebook(mayRead && cohortId !== '' ? cohortId : null);
  if (!mayRead) return <Text>{t('lms.common.forbidden')}</Text>;
  if (gradebook.isPending || gradebook.isError)
    return <QueryFeedback query={gradebook} hasCachedRows={false} onRetry={() => void gradebook.refetch()} />;
  const row =
    (gradebook.data.rows ?? []).find((candidate) => candidate.userId === userId) ??
    (gradebook.data.rows ?? [])[0];
  return (
    <View>
      <Text accessibilityRole="header">{t('screen.LMS33.title')}</Text>
      {row?.totalPercent !== undefined && row?.totalPercent !== null ? (
        <Text>{t('lms.myGrades.total', { percent: row.totalPercent })}</Text>
      ) : null}
      {(gradebook.data.columns ?? []).map((column) => {
        const cell = row?.cells?.find((candidate) => candidate.contentItemId === column.contentItemId);
        return (
          <Text key={column.contentItemId}>
            {column.title}: {cell?.points ?? t('lms.myGrades.none')}
          </Text>
        );
      })}
    </View>
  );
}

// --------------------------------------------------------------------------------------- LMS-11 group (B)

function GroupPanel({ assignmentGroupId }: { assignmentGroupId: string }) {
  const { t } = useI18n();
  const mayRead = useCan('lms.assignment_group_member.read');
  const mayDecide = useCan('lms.assignment_group_member.accept_group_request');
  const members = useAssignmentGroupMembers(mayRead && assignmentGroupId !== '' ? assignmentGroupId : null);
  const steps = useGroupWorkspaceSteps();
  if (!mayRead) return <Text>{t('lms.common.forbidden')}</Text>;
  if (members.isPending || members.isError)
    return <QueryFeedback query={members} hasCachedRows={false} onRetry={() => void members.refetch()} />;
  return (
    <View>
      <Text accessibilityRole="header">{t('screen.LMS11.title')}</Text>
      {members.data.items.map((member) => (
        <View key={member.id}>
          <Text>
            {member.userId} — {member.status ?? ''}
          </Text>
          {mayDecide && member.status === 'requested' && member.id !== undefined && member.id !== null ? (
            <View style={{ flexDirection: 'row' }}>
              <Button
                variant="secondary"
                onPress={() =>
                  steps.decide.mutate({
                    assignmentGroupMemberId: member.id as string,
                    assignmentGroupId,
                    accept: true,
                  })
                }
              >
                {t('lms.group.accept')}
              </Button>
              <Button
                variant="secondary"
                onPress={() =>
                  steps.decide.mutate({
                    assignmentGroupMemberId: member.id as string,
                    assignmentGroupId,
                    accept: false,
                  })
                }
              >
                {t('lms.group.reject')}
              </Button>
            </View>
          ) : null}
        </View>
      ))}
    </View>
  );
}

// --------------------------------------------------------------------------------------- LMS-31 gradebook (B)

function GradebookPanel({ cohortId }: { cohortId: string }) {
  const { t } = useI18n();
  const mayRead = useCan('lms.cohort.get_gradebook');
  const gradebook = useGradebook(mayRead && cohortId !== '' ? cohortId : null);
  if (!mayRead) return <Text>{t('lms.common.forbidden')}</Text>;
  if (gradebook.isPending || gradebook.isError)
    return <QueryFeedback query={gradebook} hasCachedRows={false} onRetry={() => void gradebook.refetch()} />;
  return (
    <View>
      <Text accessibilityRole="header">{t('screen.LMS31.title')}</Text>
      {(gradebook.data.rows ?? []).map((row) => (
        <Text key={row.userId}>
          {row.studentName ?? row.userId}:{' '}
          {row.totalPercent !== undefined && row.totalPercent !== null ? `${row.totalPercent}%` : '—'}{' '}
          {row.letterGrade ?? ''}
        </Text>
      ))}
    </View>
  );
}

// --------------------------------------------------------------------------------------- LMS-32 grading (B)

function GradingPanel({ contentItemId }: { contentItemId: string }) {
  const { t } = useI18n();
  const mayGrade = useCan('lms.gradebook_entry.grade_entry');
  const mayQueue = useCan('lms.quiz_submission.read');
  const queue = useQuizSubmissions(
    mayQueue && contentItemId !== '' ? `contentItemId = "${contentItemId}"` : null,
  );
  const grade = useGradeEntry();
  const [userId, setUserId] = useState('');
  const [points, setPoints] = useState('');
  if (!mayGrade) return <Text>{t('lms.common.forbidden')}</Text>;
  return (
    <View>
      <Text accessibilityRole="header">{t('screen.LMS32.title')}</Text>
      {(queue.data?.items ?? []).map((attempt) => (
        <Button key={attempt.id} variant="secondary" onPress={() => setUserId(attempt.userId ?? '')}>
          {`${attempt.userId ?? ''} — ${attempt.status ?? ''}`}
        </Button>
      ))}
      {grade.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      <TextInput label={t('lms.gradebook.student')} value={userId} onChangeText={setUserId} />
      <TextInput label={t('lms.grading.points')} value={points} onChangeText={setPoints} />
      <Button
        onPress={() =>
          grade.mutate({ contentItemId, userId, grade: { points: points.trim(), rubricCellIds: [] } })
        }
        disabled={grade.isPending || userId === '' || points.trim() === ''}
      >
        {t('lms.grading.saveGrade')}
      </Button>
    </View>
  );
}

// --------------------------------------------------------------------------------------- LMS-54 drive (B)

function DrivePanel({ tablet }: { tablet: boolean }) {
  const { t } = useI18n();
  const mayRead = useCan('lms.drive_item.read');
  const mayShare = useCan('lms.drive_item.share_drive_item');
  const me = useSession((s) => s.me?.user?.id);
  const items = useDriveItems();
  const share = useShareDriveItem();
  const [target, setTarget] = useState('');
  if (!mayRead) return <Text>{t('lms.common.forbidden')}</Text>;
  if (items.isPending || items.isError)
    return <QueryFeedback query={items} hasCachedRows={false} onRetry={() => void items.refetch()} />;
  return (
    <View>
      <Text accessibilityRole="header">{t('screen.LMS54.title')}</Text>
      {items.data.items.map((item) => (
        <View key={item.id} style={{ marginBottom: tablet ? 12 : 8 }}>
          <Text>
            {item.title} {item.isShared ? `(${t('lms.drive.sharedYes')})` : ''}
          </Text>
          {mayShare && item.ownerUserId === me && item.id !== undefined && item.id !== null ? (
            <Button
              variant="secondary"
              onPress={() =>
                target !== '' && share.mutate({ driveItemId: item.id as string, userIds: [target] })
              }
              disabled={share.isPending || target === ''}
            >
              {t('lms.drive.share')}
            </Button>
          ) : null}
        </View>
      ))}
      {mayShare ? <TextInput label={t('lms.drive.addUser')} value={target} onChangeText={setTarget} /> : null}
      {share.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
    </View>
  );
}
