import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, json, onePage } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  useAccessLogs,
  useActivateExternalTool,
  useAssignmentGroup,
  useAssignmentGroupMembers,
  useAssignmentSubmissionSteps,
  useBatchGradeEntries,
  useCheckQuizAnswer,
  useContentItem,
  useCoursePackageTools,
  useDriveItems,
  useDriveItemShares,
  useExerciseSubmissions,
  useExerciseTestResults,
  useExternalToolLinks,
  useExternalToolResources,
  useExternalTools,
  useGradebook,
  useGradebookTools,
  useGradeChangeLogs,
  useGradeEntry,
  useGradeLearningSubmission,
  useGradeQuizAnswers,
  useGroupWorkspaceSteps,
  useImportExportJobs,
  useImportQuestions,
  useLaunchExternalTool,
  useLmsAssignment,
  useLmsAssignmentSubmissions,
  useMyStorageUsage,
  usePlagiarismReportLink,
  usePlagiarismReports,
  useProgrammingExercise,
  useQuestionSearch,
  useQuiz,
  useQuizAnswers,
  useQuizAttemptPause,
  useQuizQuestions,
  useQuizSubmission,
  useQuizSubmissions,
  useReassignmentLogs,
  useRubricBundle,
  useSaveQuizResponses,
  useSetQuizQuestions,
  useShareDriveItem,
  useStartQuizAttempt,
  useSubmitExercise,
  useSubmitForPlagiarismCheck,
  useSubmitQuiz,
  useUpdateLmsAssignment,
  useUpdateQuiz,
} from './lms';

const id = (suffix: string) => `0196b000-0000-7000-9000-${suffix.padStart(12, '0')}`;
const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: id('11'), username: 'asha', locale: 'en-IN' };
const item = id('i1');
const quiz = id('q1');
const attempt = id('a1');
const assignment = id('as');
const submission = id('su');
const report = id('pr');
const cohort = id('c1');
const rubric = id('rb');
const exercise = id('ex');
const exerciseSubmission = id('es');
const tool = id('to');
const resource = id('re');
const link = id('li');
const job = id('jb');
const course = id('co');
const drive = id('dr');
const group = id('gr');
const member = id('gm');
const operation = (result: Record<string, unknown>) => json({ operationId: id('op'), done: true, result });

/** Every endpoint the coursework hooks call, answered with small fixed rows. */
function routes() {
  const L = '/v1/lms';
  return {
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    [`GET ${L}/content-items/${item}`]: () =>
      json({ id: item, itemKind: 'quiz', quizId: quiz, title: 'Check' }),
    [`POST ${L}/content-items/${item}:startQuizAttempt`]: () =>
      json({ quizSubmissionId: attempt, attemptToken: 'tok', attemptsUsed: 1, questions: [] }),
    [`POST ${L}/quiz-submissions/${attempt}:saveResponses`]: () => json({ savedCount: 1 }),
    [`POST ${L}/quiz-submissions/${attempt}:pause`]: () => json({ id: attempt, status: 'paused' }),
    [`POST ${L}/quiz-submissions/${attempt}:resume`]: () => json({ id: attempt, status: 'in_progress' }),
    [`POST ${L}/quiz-submissions/${attempt}:submit`]: () =>
      json({ id: attempt, status: 'graded', score: '6.00' }),
    [`POST ${L}/quizzes/${quiz}:checkAnswer`]: () => json({ isCorrect: true, correctOptionIds: [] }),
    [`GET ${L}/quiz-submissions/${attempt}`]: () => json({ id: attempt, status: 'graded' }),
    [`GET ${L}/quiz-submissions/${attempt}/answers`]: (req: Request) => onePage(req, [{ id: id('qa') }]),
    [`GET ${L}/quiz-submissions`]: (req: Request) => onePage(req, [{ id: attempt }]),
    [`POST ${L}/quiz-submissions/${attempt}:gradeAnswers`]: () => json({ id: attempt, status: 'graded' }),
    [`GET ${L}/assignments/${assignment}`]: () =>
      json({ id: assignment, title: 'Essay' }, { headers: { ETag: '"4"' } }),
    [`PATCH ${L}/assignments/${assignment}`]: () => json({ id: assignment, title: 'Essay 2' }),
    [`GET ${L}/assignment-submissions`]: (req: Request) => onePage(req, [{ id: submission }]),
    [`POST ${L}/assignment-submissions`]: () => json({ id: submission, answer: 'Draft' }, { status: 201 }),
    [`PATCH ${L}/assignment-submissions/${submission}`]: () => json({ id: submission, answer: 'Final' }),
    [`POST ${L}/assignment-submissions/${submission}:finalize`]: () =>
      json({ id: submission, isFinal: true }),
    [`DELETE ${L}/assignment-submissions/${submission}`]: () => new Response(null, { status: 204 }),
    [`POST ${L}/assignment-submissions/${submission}:grade`]: () => json({ id: submission, status: 'pass' }),
    [`POST ${L}/plagiarism-reports:submit`]: () => json({ id: report, status: 'pending' }),
    [`GET ${L}/plagiarism-reports`]: (req: Request) => onePage(req, [{ id: report }]),
    [`GET ${L}/plagiarism-reports/${report}:reportLink`]: () => json({ url: 'https://provider/report' }),
    [`GET ${L}/cohorts/${cohort}:gradebook`]: () => json({ cohortId: cohort, columns: [], rows: [] }),
    [`POST ${L}/gradebook-entries:grade`]: () => json({ id: id('ge'), points: '8.00' }),
    [`POST ${L}/gradebook-entries:batchGrade`]: () => json({ items: [{ contentItemId: item }] }),
    [`POST ${L}/content-items/${item}:publishGrades`]: () => json({ published: 2 }),
    [`POST ${L}/cohorts/${cohort}:exportGrades`]: () => operation({ fileId: id('fl') }),
    [`POST ${L}/cohorts/${cohort}:publishFinalGrades`]: () => json({ items: [] }),
    [`GET ${L}/grade-change-logs`]: (req: Request) => onePage(req, [{ id: id('gl') }]),
    [`GET ${L}/reassignment-logs`]: (req: Request) => onePage(req, [{ id: id('rl') }]),
    [`GET ${L}/rubrics/${rubric}`]: () => json({ id: rubric, title: 'Essays' }, { headers: { ETag: '"2"' } }),
    [`GET ${L}/rubrics/${rubric}/criteria`]: (req: Request) => onePage(req, [{ id: id('cr') }]),
    [`GET ${L}/rubrics/${rubric}/levels`]: (req: Request) =>
      onePage(req, [{ id: id('lv') }, { id: id('lw') }]),
    [`GET ${L}/rubrics/${rubric}/cells`]: (req: Request) => onePage(req, []),
    [`GET ${L}/questions:search`]: (req: Request) => onePage(req, [{ id: id('qn'), questionText: 'Prime?' }]),
    [`POST ${L}/questions:import`]: () => operation({ imported: 3 }),
    [`GET ${L}/quizzes/${quiz}`]: () => json({ id: quiz, title: 'Check' }, { headers: { ETag: '"7"' } }),
    [`PATCH ${L}/quizzes/${quiz}`]: () => json({ id: quiz, title: 'Check 2' }),
    [`GET ${L}/quizzes/${quiz}/questions`]: (req: Request) => onePage(req, [{ id: id('qq') }]),
    [`POST ${L}/quizzes/${quiz}:setQuestions`]: () => json({ items: [{ id: id('qq') }] }),
    [`GET ${L}/programming-exercises/${exercise}`]: () => json({ id: exercise, title: 'FizzBuzz' }),
    [`GET ${L}/programming-exercises/${exercise}/test-cases`]: (req: Request) =>
      onePage(req, [{ id: id('tc') }]),
    [`POST ${L}/programming-exercises/${exercise}:submit`]: () => json({ id: exerciseSubmission }),
    [`GET ${L}/exercise-submissions`]: (req: Request) => onePage(req, [{ id: exerciseSubmission }]),
    [`GET ${L}/exercise-submissions/${exerciseSubmission}/test-results`]: (req: Request) =>
      onePage(req, [{ id: id('tr'), status: 'passed' }]),
    [`GET ${L}/external-tools`]: (req: Request) => onePage(req, [{ id: tool, title: 'Lab' }]),
    [`POST ${L}/external-tools/${tool}:activate`]: () => json({ id: tool, isActive: true }),
    [`GET ${L}/external-tool-resources`]: (req: Request) => onePage(req, [{ id: resource }]),
    [`GET ${L}/external-tool-resources/${resource}/links`]: (req: Request) => onePage(req, [{ id: link }]),
    [`POST ${L}/external-tool-resources/${resource}/links/${link}:launch`]: () =>
      json({ launchUrl: 'https://tool/launch', parameters: { oauth_signature: 's' } }),
    [`GET ${L}/import-export-jobs`]: (req: Request) => onePage(req, [{ id: job }]),
    [`POST ${L}/courses/${course}:export`]: () => operation({ jobId: job }),
    [`POST ${L}/courses/${course}:importPackage`]: () => operation({ jobId: job }),
    [`POST ${L}/import-export-jobs/${job}:restore`]: () => operation({ jobId: job }),
    [`GET ${L}/import-export-jobs/${job}:package`]: () => json({ url: 'https://files/pkg.zip' }),
    [`GET ${L}/drive-items`]: (req: Request) => onePage(req, [{ id: drive }]),
    [`GET ${L}/drive-items/${drive}/shares`]: (req: Request) => onePage(req, [{ id: id('sh') }]),
    [`POST ${L}/drive-items/${drive}:share`]: () => json({ items: [{ id: id('sh') }] }),
    'GET /v1/platform/me/storage': () => json({ quotaBytes: '100', usedBytes: '40' }),
    [`GET ${L}/content-items/${item}/groups/${group}`]: () => json({ id: group, name: 'Team A' }),
    [`GET ${L}/assignment-group-members`]: (req: Request) => onePage(req, [{ id: member }]),
    [`POST ${L}/content-items/${item}/groups/${group}:requestToJoin`]: () => json({ id: member }),
    [`POST ${L}/assignment-group-members/${member}:accept`]: () => json({ id: member, status: 'accepted' }),
    [`POST ${L}/assignment-group-members/${member}:reject`]: () => json({ id: member, status: 'rejected' }),
    [`POST ${L}/content-items/${item}/groups/${group}:setMembers`]: () => json({ items: [{ id: member }] }),
    [`POST ${L}/content-items/${item}/groups/${group}:submitPeerReview`]: () => json({ items: [] }),
    [`GET ${L}/access-logs`]: (req: Request) => onePage(req, [{ id: id('al') }]),
  };
}

async function setup() {
  const api = fakeApi(routes());
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

/** The request body the client sent on the last call to a path ending this way. */
function sent(api: Awaited<ReturnType<typeof setup>>['api'], ending: string): unknown {
  const call = [...api.calls].reverse().find((c) => c.path.endsWith(ending));
  return JSON.parse(call?.body ?? 'null');
}

describe('the coursework hooks (quizzes, assignments, grading, tools, packages, drive, groups)', () => {
  it('walks a quiz attempt: start, autosave, pause and resume, submit, check, and the teacher marks', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        item: useContentItem(item),
        start: useStartQuizAttempt(),
        save: useSaveQuizResponses(),
        pause: useQuizAttemptPause(),
        submit: useSubmitQuiz(),
        check: useCheckQuizAnswer(),
        attempt: useQuizSubmission(attempt),
        answers: useQuizAnswers(attempt),
        attempts: useQuizSubmissions(`quizId = "${quiz}"`),
        grade: useGradeQuizAnswers(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.item.data?.quizId).toBe(quiz));
    await waitFor(() => expect(result.current.attempt.data?.status).toBe('graded'));
    await waitFor(() => expect(result.current.answers.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.attempts.data?.items).toHaveLength(1));

    act(() => result.current.start.mutate({ contentItemId: item }));
    await waitFor(() => expect(result.current.start.data?.attemptToken).toBe('tok'));
    const answer = {
      questionId: id('qn'),
      selectedOptionIds: [id('op1')],
      blankAnswers: [],
      matchAnswers: [],
      hotspotPoints: [],
    };
    act(() =>
      result.current.save.mutate({
        quizSubmissionId: attempt,
        request: { attemptToken: 'tok', clientSequence: '1', answers: [answer] },
      }),
    );
    await waitFor(() => expect(result.current.save.data?.savedCount).toBe(1));
    act(() => result.current.pause.mutate({ quizSubmissionId: attempt, resume: false }));
    await waitFor(() => expect(result.current.pause.data?.status).toBe('paused'));
    act(() => result.current.pause.mutate({ quizSubmissionId: attempt, resume: true }));
    await waitFor(() => expect(result.current.pause.data?.status).toBe('in_progress'));
    act(() =>
      result.current.submit.mutate({
        quizSubmissionId: attempt,
        request: { attemptToken: 'tok', answers: [answer] },
      }),
    );
    await waitFor(() => expect(result.current.submit.data?.score).toBe('6.00'));
    expect(sent(api, ':submit')).toEqual({ attemptToken: 'tok', answers: [answer] });
    act(() => result.current.check.mutate({ quizId: quiz, questionId: answer.questionId, answer }));
    await waitFor(() => expect(result.current.check.data?.isCorrect).toBe(true));
    act(() =>
      result.current.grade.mutate({
        quizSubmissionId: attempt,
        answers: [{ questionId: id('qn'), marks: '4.00' }],
      }),
    );
    await waitFor(() => expect(result.current.grade.isSuccess).toBe(true));
  });

  it('walks an assignment hand-in, its grading and plagiarism check, and the builders under their etags', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        assignment: useLmsAssignment(assignment),
        submissions: useLmsAssignmentSubmissions(`assignmentId = "${assignment}"`),
        steps: useAssignmentSubmissionSteps(),
        grade: useGradeLearningSubmission(),
        check: useSubmitForPlagiarismCheck(),
        reports: usePlagiarismReports(null),
        reportLink: usePlagiarismReportLink(),
        quiz: useQuiz(quiz),
        questions: useQuizQuestions(quiz),
        setQuestions: useSetQuizQuestions(),
        updateQuiz: useUpdateQuiz(),
        updateAssignment: useUpdateLmsAssignment(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.assignment.data?.etag).toBe('"4"'));
    await waitFor(() => expect(result.current.submissions.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.reports.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.quiz.data?.etag).toBe('"7"'));
    await waitFor(() => expect(result.current.questions.data?.items).toHaveLength(1));

    const steps = result.current.steps;
    act(() => steps.create.mutate({ assignmentId: assignment, userId: USER.id, answer: 'Draft' }));
    await waitFor(() => expect(result.current.steps.create.data?.answer).toBe('Draft'));
    act(() =>
      result.current.steps.update.mutate({
        assignmentSubmissionId: submission,
        etag: '"1"',
        changes: { answer: 'Final' },
      }),
    );
    await waitFor(() => expect(result.current.steps.update.data?.answer).toBe('Final'));
    act(() => result.current.steps.finalize.mutate({ assignmentSubmissionId: submission }));
    await waitFor(() => expect(result.current.steps.finalize.data?.isFinal).toBe(true));
    act(() => result.current.steps.remove.mutate({ assignmentSubmissionId: submission, etag: '"2"' }));
    await waitFor(() => expect(result.current.steps.remove.isSuccess).toBe(true));
    expect(api.calls.find((c) => c.method === 'DELETE')?.headers.get('If-Match')).toBe('"2"');
    act(() =>
      result.current.grade.mutate({
        assignmentSubmissionId: submission,
        request: { status: 'pass' },
      }),
    );
    await waitFor(() => expect(result.current.grade.data?.status).toBe('pass'));
    act(() => result.current.check.mutate({ entityAttachmentId: id('at') }));
    await waitFor(() => expect(result.current.check.data?.status).toBe('pending'));
    act(() => result.current.reportLink.mutate({ plagiarismReportId: report, view: 'similarity' }));
    await waitFor(() => expect(result.current.reportLink.data?.url).toBe('https://provider/report'));

    act(() => result.current.setQuestions.mutate({ quizId: quiz, request: { addQuestionIds: [id('qn')] } }));
    await waitFor(() => expect(result.current.setQuestions.isSuccess).toBe(true));
    act(() => result.current.updateQuiz.mutate({ quizId: quiz, etag: '"7"', changes: { title: 'Check 2' } }));
    await waitFor(() => expect(result.current.updateQuiz.data?.title).toBe('Check 2'));
    act(() =>
      result.current.updateAssignment.mutate({
        assignmentId: assignment,
        etag: '"4"',
        changes: { title: 'Essay 2' },
      }),
    );
    await waitFor(() => expect(result.current.updateAssignment.data?.title).toBe('Essay 2'));
  });

  it('reads and writes the gradebook, its history, the rubric and the question bank', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        gradebook: useGradebook(cohort),
        entry: useGradeEntry(),
        batch: useBatchGradeEntries(),
        tools: useGradebookTools(),
        changes: useGradeChangeLogs(`contentItemId = "${item}"`),
        reassignments: useReassignmentLogs(`contentItemId = "${item}"`),
        rubric: useRubricBundle(rubric),
        search: useQuestionSearch('prime', ['choices'], quiz),
        importQuestions: useImportQuestions(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.gradebook.data?.cohortId).toBe(cohort));
    await waitFor(() => expect(result.current.changes.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.reassignments.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.rubric.data?.levels).toHaveLength(2));
    expect(result.current.rubric.data?.rubric.etag).toBe('"2"');
    await waitFor(() => expect(result.current.search.data?.data[0]?.questionText).toBe('Prime?'));
    const search = api.calls.find((c) => c.path.endsWith('questions:search'));
    expect(search?.search).toContain('excludeQuizId=');

    act(() =>
      result.current.entry.mutate({ contentItemId: item, userId: id('st'), grade: { rubricCellIds: [] } }),
    );
    await waitFor(() => expect(result.current.entry.data?.points).toBe('8.00'));
    act(() =>
      result.current.batch.mutate([{ contentItemId: item, userId: id('st'), grade: { rubricCellIds: [] } }]),
    );
    await waitFor(() => expect(result.current.batch.data?.items).toHaveLength(1));
    act(() => result.current.tools.publishItem.mutate({ contentItemId: item }));
    await waitFor(() => expect(result.current.tools.publishItem.isSuccess).toBe(true));
    act(() => result.current.tools.exportGrades.mutate({ cohortId: cohort }));
    await waitFor(() => expect(result.current.tools.exportGrades.isSuccess).toBe(true));
    act(() => result.current.tools.publishFinal.mutate({ cohortId: cohort }));
    await waitFor(() => expect(result.current.tools.publishFinal.isSuccess).toBe(true));
    act(() => result.current.importQuestions.mutate({ fileId: id('fl'), format: 'gift' }));
    await waitFor(() => expect(result.current.importQuestions.isSuccess).toBe(true));
  });

  it('runs exercises, external tools, course packages, the drive, group work and the access log', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        exercise: useProgrammingExercise(exercise),
        submitExercise: useSubmitExercise(),
        exerciseSubmissions: useExerciseSubmissions(exercise),
        results: useExerciseTestResults(exerciseSubmission),
        tools: useExternalTools(),
        activate: useActivateExternalTool(),
        resources: useExternalToolResources(),
        links: useExternalToolLinks(resource),
        launch: useLaunchExternalTool(),
        jobs: useImportExportJobs(),
        packages: useCoursePackageTools(),
        drive: useDriveItems(),
        shares: useDriveItemShares(drive),
        share: useShareDriveItem(),
        storage: useMyStorageUsage(),
        group: useAssignmentGroup(item, group),
        members: useAssignmentGroupMembers(group),
        workspace: useGroupWorkspaceSteps(),
        accessLogs: useAccessLogs('accessedAt >= "2026-11-01T00:00:00Z"'),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.exercise.data?.testCases).toHaveLength(1));
    await waitFor(() => expect(result.current.exerciseSubmissions.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.results.data?.items[0]?.status).toBe('passed'));
    await waitFor(() => expect(result.current.tools.data?.items[0]?.title).toBe('Lab'));
    await waitFor(() => expect(result.current.resources.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.links.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.jobs.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.drive.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.shares.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.storage.data?.usedBytes).toBe('40'));
    await waitFor(() => expect(result.current.group.data?.name).toBe('Team A'));
    await waitFor(() => expect(result.current.members.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.accessLogs.data?.data).toHaveLength(1));

    act(() =>
      result.current.submitExercise.mutate({
        programmingExerciseId: exercise,
        request: { code: 'print(1)', testOutputs: [] },
      }),
    );
    await waitFor(() => expect(result.current.submitExercise.isSuccess).toBe(true));
    act(() => result.current.activate.mutate({ externalToolId: tool, activationKey: 'KEY-1' }));
    await waitFor(() => expect(result.current.activate.isSuccess).toBe(true));
    expect(sent(api, ':activate')).toEqual({ activationKey: 'KEY-1' });
    act(() =>
      result.current.launch.mutate({
        externalToolResourceId: resource,
        externalToolLinkId: link,
        contentItemId: item,
      }),
    );
    await waitFor(() => expect(result.current.launch.data?.launchUrl).toBe('https://tool/launch'));
    expect(sent(api, ':launch')).toEqual({ contentItemId: item });

    act(() => result.current.packages.exportCourse.mutate({ courseId: course }));
    await waitFor(() => expect(result.current.packages.exportCourse.isSuccess).toBe(true));
    act(() =>
      result.current.packages.importPackage.mutate({
        courseId: course,
        request: { fileId: id('fl') },
      }),
    );
    await waitFor(() => expect(result.current.packages.importPackage.isSuccess).toBe(true));
    act(() => result.current.packages.restore.mutate({ importExportJobId: job }));
    await waitFor(() => expect(result.current.packages.restore.isSuccess).toBe(true));
    act(() => result.current.packages.packageLink.mutate({ importExportJobId: job }));
    await waitFor(() => expect(result.current.packages.packageLink.data?.url).toBe('https://files/pkg.zip'));

    act(() => result.current.share.mutate({ driveItemId: drive, userIds: [id('u2')] }));
    await waitFor(() => expect(result.current.share.isSuccess).toBe(true));
    expect(sent(api, ':share')).toEqual({ userIds: [id('u2')] });

    const workspace = result.current.workspace;
    act(() => workspace.requestToJoin.mutate({ contentItemId: item, assignmentGroupId: group }));
    await waitFor(() => expect(result.current.workspace.requestToJoin.isSuccess).toBe(true));
    act(() =>
      result.current.workspace.decide.mutate({
        assignmentGroupMemberId: member,
        assignmentGroupId: group,
        accept: true,
      }),
    );
    await waitFor(() => expect(result.current.workspace.decide.data?.status).toBe('accepted'));
    act(() =>
      result.current.workspace.decide.mutate({
        assignmentGroupMemberId: member,
        assignmentGroupId: group,
        accept: false,
      }),
    );
    await waitFor(() => expect(result.current.workspace.decide.data?.status).toBe('rejected'));
    act(() =>
      result.current.workspace.setMembers.mutate({
        contentItemId: item,
        assignmentGroupId: group,
        userIds: [id('u2')],
      }),
    );
    await waitFor(() => expect(result.current.workspace.setMembers.isSuccess).toBe(true));
    expect(sent(api, ':setMembers')).toEqual({ addUserIds: [id('u2')] });
    act(() =>
      result.current.workspace.submitPeerReview.mutate({
        contentItemId: item,
        assignmentGroupId: group,
        request: { reviews: [] },
      }),
    );
    await waitFor(() => expect(result.current.workspace.submitPeerReview.isSuccess).toBe(true));
  });
});
