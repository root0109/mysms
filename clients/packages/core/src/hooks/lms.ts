import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { lms, platform } from '@mysms/sdk';
import { AccessLogApi } from '@mysms/sdk/lms/apis/AccessLogApi';
import { AssignmentGroupMemberApi } from '@mysms/sdk/lms/apis/AssignmentGroupMemberApi';
import { CohortApi } from '@mysms/sdk/lms/apis/CohortApi';
import { ContentItemApi } from '@mysms/sdk/lms/apis/ContentItemApi';
import { DriveItemApi } from '@mysms/sdk/lms/apis/DriveItemApi';
import { ExerciseSubmissionApi } from '@mysms/sdk/lms/apis/ExerciseSubmissionApi';
import { ExternalToolApi } from '@mysms/sdk/lms/apis/ExternalToolApi';
import { ExternalToolResourceApi } from '@mysms/sdk/lms/apis/ExternalToolResourceApi';
import { GradebookEntryApi } from '@mysms/sdk/lms/apis/GradebookEntryApi';
import { GradeChangeLogApi } from '@mysms/sdk/lms/apis/GradeChangeLogApi';
import { ImportExportJobApi } from '@mysms/sdk/lms/apis/ImportExportJobApi';
import { LmsAssignmentApi } from '@mysms/sdk/lms/apis/LmsAssignmentApi';
import { LmsAssignmentSubmissionApi } from '@mysms/sdk/lms/apis/LmsAssignmentSubmissionApi';
import { LmsCourseApi } from '@mysms/sdk/lms/apis/LmsCourseApi';
import { PlagiarismReportApi } from '@mysms/sdk/lms/apis/PlagiarismReportApi';
import { ProgrammingExerciseApi } from '@mysms/sdk/lms/apis/ProgrammingExerciseApi';
import { QuestionApi } from '@mysms/sdk/lms/apis/QuestionApi';
import { QuizApi } from '@mysms/sdk/lms/apis/QuizApi';
import { QuizSubmissionApi } from '@mysms/sdk/lms/apis/QuizSubmissionApi';
import { ReassignmentLogApi } from '@mysms/sdk/lms/apis/ReassignmentLogApi';
import { RubricApi } from '@mysms/sdk/lms/apis/RubricApi';
import { StorageQuotaApi } from '@mysms/sdk/platform/apis/StorageQuotaApi';
import { allPages, pageQuery, type PageParam, FIRST_PAGE } from '../api/pages';
import { call, callWithEtag } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { useExportGrades, usePublishFinalGrades } from './lmsCohorts';

export type ContentItem = lms.ContentItem;
export type QuizAttempt = lms.QuizAttempt;
export type QuizAnswerInput = lms.QuizAnswerInput;
export type QuizAutosaveResult = lms.QuizAutosaveResult;
export type QuizSubmission = lms.QuizSubmission;
export type QuizAnswer = lms.QuizAnswer;
export type AnswerCheck = lms.AnswerCheck;
export type Quiz = lms.Quiz;
export type QuizQuestion = lms.QuizQuestion;
export type LmsQuestion = lms.Question;
export type LmsAssignment = lms.LmsAssignment;
export type LmsAssignmentSubmission = lms.LmsAssignmentSubmission;
export type PlagiarismReport = lms.PlagiarismReport;
export type PlagiarismAccount = lms.PlagiarismAccount;
export type Gradebook = lms.Gradebook;
export type GradebookEntry = lms.GradebookEntry;
export type GradeInput = lms.GradeInput;
export type ProgrammingExercise = lms.ProgrammingExercise;
export type ExerciseTestCase = lms.ExerciseTestCase;
export type ExerciseSubmission = lms.ExerciseSubmission;
export type ExerciseTestResult = lms.ExerciseTestResult;
export type ExternalTool = lms.ExternalTool;
export type ExternalToolResource = lms.ExternalToolResource;
export type ExternalToolLink = lms.ExternalToolLink;
export type LtiLaunch = lms.LtiLaunch;
export type ImportExportJob = lms.ImportExportJob;
export type DriveItem = lms.DriveItem;
export type DriveItemShare = lms.DriveItemShare;
export type AccessLog = lms.AccessLog;
export type Rubric = lms.Rubric;
export type RubricCriterion = lms.RubricCriterion;
export type RubricLevel = lms.RubricLevel;
export type RubricCell = lms.RubricCell;
export type GradeChangeLog = lms.GradeChangeLog;
export type ReassignmentLog = lms.ReassignmentLog;
export type StorageQuota = platform.StorageQuota;
export type AssignmentGroup = lms.AssignmentGroup;
export type AssignmentGroupMember = lms.AssignmentGroupMember;

const CONTENT_ITEM = (id: string | null) => queryKey('lms', 'content-item', id);
const QUIZ = (id: string | null) => queryKey('lms', 'quiz', id);
const QUIZ_QUESTIONS = (quizId: string | null) => queryKey('lms', 'quiz-questions', quizId);
const QUIZ_SUBMISSION = (id: string | null) => queryKey('lms', 'quiz-submission', id);
const QUIZ_ANSWERS = (id: string | null) => queryKey('lms', 'quiz-answers', id);
const QUIZ_SUBMISSIONS = (filter: string) => queryKey('lms', 'quiz-submissions', filter);
const ASSIGNMENT = (id: string | null) => queryKey('lms', 'assignment', id);
const ASSIGNMENT_SUBMISSIONS = (filter: string) => queryKey('lms', 'assignment-submissions', filter);
const PLAGIARISM_REPORTS = (filter: string) => queryKey('lms', 'plagiarism-reports', filter);
const GRADEBOOK = (cohortId: string | null, page: number) =>
  queryKey('lms', 'gradebook', `${cohortId}#${page}`);
const EXERCISE = (id: string | null) => queryKey('lms', 'exercise', id);
const EXERCISE_SUBMISSIONS = (exerciseId: string | null) =>
  queryKey('lms', 'exercise-submissions', exerciseId);
const EXERCISE_RESULTS = (submissionId: string | null) => queryKey('lms', 'exercise-results', submissionId);
const QUESTION_SEARCH = (search: string) => queryKey('lms', 'question-search', search);
const EXTERNAL_TOOLS = queryKey('lms', 'external-tools');
const TOOL_RESOURCES = queryKey('lms', 'external-tool-resources');
const TOOL_LINKS = (resourceId: string | null) => queryKey('lms', 'external-tool-links', resourceId);
const JOBS = queryKey('lms', 'import-export-jobs');
const DRIVE = queryKey('lms', 'drive-items');
const DRIVE_SHARES = (id: string | null) => queryKey('lms', 'drive-item-shares', id);
const STORAGE = queryKey('platform', 'my-storage');
const RUBRIC = (id: string | null) => queryKey('lms', 'rubric', id);
const GRADE_LOGS = (filter: string) => queryKey('lms', 'grade-change-logs', filter);
const REASSIGNMENT_LOGS = (filter: string) => queryKey('lms', 'reassignment-logs', filter);
const ACCESS_LOGS = (filter: string, page: number) => queryKey('lms', 'access-logs', `${filter}#${page}`);

/** Marks the standard list and form of a record type stale: a step changed rows those screens may be showing. */
function staleRecords(qc: QueryClient, recordType: string): void {
  void qc.invalidateQueries({ queryKey: queryKey('records', recordType) });
  void qc.invalidateQueries({ queryKey: queryKey('records', `${recordType}#item`) });
}

// ------------------------------------------------------------------------------------------- the quiz attempt

/** lms.GetContentItem: the placement a player screen was opened for. */
export function useContentItem(contentItemId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: CONTENT_ITEM(contentItemId),
    queryFn: () => call(api.get(ContentItemApi).getContentItem({ contentItemId: contentItemId as string })),
    enabled: contentItemId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.StartQuizAttempt: the delivery of a new or resumed attempt, with its token, questions and deadline. */
export function useStartQuizAttempt() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ contentItemId }: { contentItemId: string }) =>
      call(api.get(QuizApi).startQuizAttempt({ contentItemId, body: {} })),
  });
}

/** lms.SaveQuizResponses: the autosave; stale saves are ignored by the server (clientSequence). */
export function useSaveQuizResponses() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({
      quizSubmissionId,
      request,
    }: {
      quizSubmissionId: string;
      request: lms.SaveQuizResponsesRequest;
    }) =>
      call(
        api.get(QuizSubmissionApi).saveQuizResponses({ quizSubmissionId, saveQuizResponsesRequest: request }),
      ),
  });
}

/** lms.PauseQuizAttempt / lms.ResumeQuizAttempt, for quizzes that allow breaks. */
export function useQuizAttemptPause() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ quizSubmissionId, resume }: { quizSubmissionId: string; resume: boolean }) => {
      const attempts = api.get(QuizSubmissionApi);
      return resume
        ? call(attempts.resumeQuizAttempt({ quizSubmissionId, resumeQuizAttemptRequest: {} }))
        : call(attempts.pauseQuizAttempt({ quizSubmissionId, pauseQuizAttemptRequest: {} }));
    },
  });
}

/** lms.SubmitQuiz: the final answers; the attempt is marked, late submissions on what was saved in time. */
export function useSubmitQuiz() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      quizSubmissionId,
      request,
    }: {
      quizSubmissionId: string;
      request: lms.SubmitQuizRequest;
    }) => call(api.get(QuizApi).submitQuiz({ quizSubmissionId, submitQuizRequest: request })),
    onSettled: (_r, _e, variables) => {
      staleRecords(qc, 'lms.QuizSubmission');
      void qc.invalidateQueries({ queryKey: QUIZ_SUBMISSION(variables.quizSubmissionId) });
      void qc.invalidateQueries({ queryKey: QUIZ_ANSWERS(variables.quizSubmissionId) });
    },
  });
}

/** lms.CheckQuizAnswer: instant feedback, given only on practice quizzes (409 otherwise). */
export function useCheckQuizAnswer() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({
      quizId,
      questionId,
      answer,
    }: {
      quizId: string;
      questionId: string;
      answer: QuizAnswerInput;
    }) => call(api.get(QuizApi).checkQuizAnswer({ quizId, checkQuizAnswerRequest: { questionId, answer } })),
  });
}

/** lms.GetQuizSubmission: one attempt with its outcome. */
export function useQuizSubmission(quizSubmissionId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: QUIZ_SUBMISSION(quizSubmissionId),
    queryFn: () =>
      call(api.get(QuizSubmissionApi).getQuizSubmission({ quizSubmissionId: quizSubmissionId as string })),
    enabled: quizSubmissionId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListQuizAnswers of one attempt, in question order. */
export function useQuizAnswers(quizSubmissionId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: QUIZ_ANSWERS(quizSubmissionId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(QuizSubmissionApi).listQuizAnswers({
            quizSubmissionId: quizSubmissionId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: quizSubmissionId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListQuizSubmissions under a contract filter (a student's own attempts, or an item's, per the caller). */
export function useQuizSubmissions(filter: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: QUIZ_SUBMISSIONS(filter ?? ''),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(QuizSubmissionApi).listQuizSubmissions({
            size: 500,
            filter: filter as string,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: filter !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.GradeQuizAnswers: a teacher's marks for answers that wait for a person. */
export function useGradeQuizAnswers() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ quizSubmissionId, answers }: { quizSubmissionId: string; answers: lms.AnswerGrade[] }) =>
      call(
        api
          .get(QuizSubmissionApi)
          .gradeQuizAnswers({ quizSubmissionId, gradeQuizAnswersRequest: { answers } }),
      ),
    onSettled: (_r, _e, variables) => {
      staleRecords(qc, 'lms.QuizSubmission');
      void qc.invalidateQueries({ queryKey: QUIZ_SUBMISSION(variables.quizSubmissionId) });
      void qc.invalidateQueries({ queryKey: QUIZ_ANSWERS(variables.quizSubmissionId) });
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'gradebook') });
    },
  });
}

// ------------------------------------------------------------------------------------------- assignments

/** lms.GetLmsAssignment with its ETag (the builder edits under it). */
export function useLmsAssignment(assignmentId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: ASSIGNMENT(assignmentId),
    queryFn: () =>
      callWithEtag(api.get(LmsAssignmentApi).getLmsAssignmentRaw({ assignmentId: assignmentId as string })),
    enabled: assignmentId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListLmsAssignmentSubmissions under a contract filter (own drafts, or an assignment's, per the caller). */
export function useLmsAssignmentSubmissions(filter: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: ASSIGNMENT_SUBMISSIONS(filter ?? ''),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(LmsAssignmentSubmissionApi).listLmsAssignmentSubmissions({
            size: 500,
            filter: filter as string,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: filter !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** The submission steps a student takes: create a draft, update it, finalize it (no change after that). */
export function useAssignmentSubmissionSteps() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => {
    staleRecords(qc, 'lms.LmsAssignmentSubmission');
    void qc.invalidateQueries({ queryKey: queryKey('lms', 'assignment-submissions') });
  };
  const submissions = () => api.get(LmsAssignmentSubmissionApi);
  const create = useMutation({
    mutationFn: (request: lms.LmsAssignmentSubmission) =>
      call(submissions().createLmsAssignmentSubmission({ lmsAssignmentSubmission: request })),
    onSettled: refresh,
  });
  const update = useMutation({
    mutationFn: ({
      assignmentSubmissionId,
      etag,
      changes,
    }: {
      assignmentSubmissionId: string;
      etag: string;
      changes: lms.LmsAssignmentSubmissionUpdate;
    }) =>
      call(
        submissions().updateLmsAssignmentSubmission({
          assignmentSubmissionId,
          ifMatch: etag,
          lmsAssignmentSubmissionUpdate: changes,
        }),
      ),
    onSettled: refresh,
  });
  const finalize = useMutation({
    mutationFn: ({ assignmentSubmissionId }: { assignmentSubmissionId: string }) =>
      call(submissions().finalizeSubmission({ assignmentSubmissionId, body: {} })),
    onSettled: refresh,
  });
  const remove = useMutation({
    mutationFn: ({ assignmentSubmissionId, etag }: { assignmentSubmissionId: string; etag: string }) =>
      call(submissions().deleteLmsAssignmentSubmission({ assignmentSubmissionId, ifMatch: etag })),
    onSettled: refresh,
  });
  return { create, update, finalize, remove };
}

/** lms.GradeLearningSubmission: a teacher's pass / fail / not-applicable word on a final submission. */
export function useGradeLearningSubmission() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      assignmentSubmissionId,
      request,
    }: {
      assignmentSubmissionId: string;
      request: lms.GradeLearningSubmissionRequest;
    }) =>
      call(
        api.get(LmsAssignmentSubmissionApi).gradeLearningSubmission({
          assignmentSubmissionId,
          gradeLearningSubmissionRequest: request,
        }),
      ),
    onSettled: () => {
      staleRecords(qc, 'lms.LmsAssignmentSubmission');
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'assignment-submissions') });
    },
  });
}

// ------------------------------------------------------------------------------------------- plagiarism

/** lms.SubmitForPlagiarismCheck: sends an attachment to the tenant's plagiarism provider (503 when none). */
export function useSubmitForPlagiarismCheck() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: lms.SubmitForPlagiarismCheckRequest) =>
      call(
        api.get(PlagiarismReportApi).submitForPlagiarismCheck({ submitForPlagiarismCheckRequest: request }),
      ),
    onSettled: () => {
      staleRecords(qc, 'lms.PlagiarismReport');
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'plagiarism-reports') });
    },
  });
}

/** lms.ListPlagiarismReports under a contract filter. */
export function usePlagiarismReports(filter: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: PLAGIARISM_REPORTS(filter ?? ''),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(PlagiarismReportApi).listPlagiarismReports({
            size: 500,
            ...(filter ? { filter } : {}),
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/** lms.GetPlagiarismReportLink: a short-lived link into the provider's report viewer. */
export function usePlagiarismReportLink() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({ plagiarismReportId, view }: { plagiarismReportId: string; view?: string }) =>
      call(
        api
          .get(PlagiarismReportApi)
          .getPlagiarismReportLink({ plagiarismReportId, ...(view ? { view } : {}) }),
      ),
  });
}

// ------------------------------------------------------------------------------------------- gradebook

/** lms.GetGradebook: one page of the cohort's grid, with its ETag for conditional re-reads. */
export function useGradebook(cohortId: string | null, page: PageParam = FIRST_PAGE) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: GRADEBOOK(cohortId, page.page),
    queryFn: () =>
      call(api.get(CohortApi).getGradebook({ cohortId: cohortId as string, size: 100, ...pageQuery(page) })),
    enabled: cohortId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.GradeEntry: one student's (or group's) grade for one item, under the entry's etag when regrading. */
export function useGradeEntry() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: lms.GradeEntryRequest) =>
      call(api.get(GradebookEntryApi).gradeEntry({ gradeEntryRequest: request })),
    onSettled: () => {
      staleRecords(qc, 'lms.GradebookEntry');
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'gradebook') });
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'grade-change-logs') });
    },
  });
}

/** lms.BatchGradeEntries: a column of grades in one call, each entry answering its own outcome. */
export function useBatchGradeEntries() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entries: lms.GradeEntryInput[]) =>
      call(api.get(GradebookEntryApi).batchGradeEntries({ batchGradeEntriesRequest: { entries } })),
    onSettled: () => {
      staleRecords(qc, 'lms.GradebookEntry');
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'gradebook') });
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'grade-change-logs') });
    },
  });
}

/** lms.ExportGrades / lms.PublishItemGrades / lms.PublishFinalGrades, as the gradebook's toolbar offers them. */
export function useGradebookTools() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => void qc.invalidateQueries({ queryKey: queryKey('lms', 'gradebook') });
  const publishItem = useMutation({
    mutationFn: ({ contentItemId }: { contentItemId: string }) =>
      call(api.get(ContentItemApi).publishItemGrades({ contentItemId, publishItemGradesRequest: {} })),
    onSettled: refresh,
  });
  const exportGrades = useExportGrades();
  const publishFinal = usePublishFinalGrades();
  return { exportGrades, publishItem, publishFinal };
}

/** lms.ListGradeChangeLogs under a contract filter (read-only history). */
export function useGradeChangeLogs(filter: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: GRADE_LOGS(filter ?? ''),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(GradeChangeLogApi)
            .listGradeChangeLogs({ size: 500, ...(filter ? { filter } : {}), ...pageQuery(page) }),
        ),
      ),
    enabled: filter !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListReassignmentLogs under a contract filter (read-only history). */
export function useReassignmentLogs(filter: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: REASSIGNMENT_LOGS(filter ?? ''),
    queryFn: () =>
      allPages((page) =>
        call(
          api
            .get(ReassignmentLogApi)
            .listReassignmentLogs({ size: 500, ...(filter ? { filter } : {}), ...pageQuery(page) }),
        ),
      ),
    enabled: filter !== null,
    staleTime: STALE_TIME.detail,
  });
}

// ------------------------------------------------------------------------------------------- rubrics

/** A rubric with its criteria, levels and cells, as the grading sidebar and the builder need it whole. */
export function useRubricBundle(rubricId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: RUBRIC(rubricId),
    queryFn: async () => {
      const rubrics = api.get(RubricApi);
      const id = rubricId as string;
      const [rubric, criteria, levels, cells] = await Promise.all([
        callWithEtag(rubrics.getRubricRaw({ rubricId: id })),
        allPages((page) => call(rubrics.listRubricCriteria({ rubricId: id, size: 500, ...pageQuery(page) }))),
        allPages((page) => call(rubrics.listRubricLevels({ rubricId: id, size: 500, ...pageQuery(page) }))),
        allPages((page) => call(rubrics.listRubricCells({ rubricId: id, size: 500, ...pageQuery(page) }))),
      ]);
      return { rubric, criteria: criteria.items, levels: levels.items, cells: cells.items };
    },
    enabled: rubricId !== null,
    staleTime: STALE_TIME.detail,
  });
}

// ------------------------------------------------------------------------------------------- question bank

/** lms.SearchQuestionBank: text, type filter and quiz exclusion, one page at a time. */
export function useQuestionSearch(
  query: string,
  questionTypes: string[],
  excludeQuizId: string | null,
  page: PageParam = FIRST_PAGE,
) {
  const { api } = useRuntime();
  const search = `${query}|${questionTypes.join(',')}|${excludeQuizId ?? ''}|${page.page}`;
  return useQuery({
    queryKey: QUESTION_SEARCH(search),
    queryFn: () =>
      call(
        api.get(QuestionApi).searchQuestionBank({
          ...(query ? { query } : {}),
          ...(questionTypes.length > 0 ? { questionTypes } : {}),
          ...(excludeQuizId ? { excludeQuizId } : {}),
          size: 50,
          ...pageQuery(page),
        }),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ImportQuestions: a GIFT or CSV file already uploaded to the file store becomes bank questions (202). */
export function useImportQuestions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: lms.ImportQuestionsRequest) =>
      call(api.get(QuestionApi).importQuestions({ importQuestionsRequest: request })),
    onSettled: () => {
      staleRecords(qc, 'lms.Question');
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'question-search') });
    },
  });
}

// ------------------------------------------------------------------------------------------- quiz builder

/** lms.GetQuiz with its ETag (the builder edits under it). */
export function useQuiz(quizId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: QUIZ(quizId),
    queryFn: () => callWithEtag(api.get(QuizApi).getQuizRaw({ quizId: quizId as string })),
    enabled: quizId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListQuizQuestions: the quiz's question set in order. */
export function useQuizQuestions(quizId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: QUIZ_QUESTIONS(quizId),
    queryFn: () =>
      allPages((page) =>
        call(api.get(QuizApi).listQuizQuestions({ quizId: quizId as string, size: 500, ...pageQuery(page) })),
      ),
    enabled: quizId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.SetQuizQuestions: adds and removes bank questions (or a pool) in one step; 409 once published. */
export function useSetQuizQuestions() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ quizId, request }: { quizId: string; request: lms.SetQuizQuestionsRequest }) =>
      call(api.get(QuizApi).setQuizQuestions({ quizId, setQuizQuestionsRequest: request })),
    onSettled: (_r, _e, variables) => {
      staleRecords(qc, 'lms.QuizQuestion');
      void qc.invalidateQueries({ queryKey: QUIZ_QUESTIONS(variables.quizId) });
      void qc.invalidateQueries({ queryKey: queryKey('lms', 'question-search') });
    },
  });
}

/** lms.UpdateQuiz under the builder's etag. */
export function useUpdateQuiz() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ quizId, etag, changes }: { quizId: string; etag: string; changes: lms.QuizUpdate }) =>
      call(api.get(QuizApi).updateQuiz({ quizId, ifMatch: etag, quizUpdate: changes })),
    onSettled: (_r, _e, variables) => {
      staleRecords(qc, 'lms.Quiz');
      void qc.invalidateQueries({ queryKey: QUIZ(variables.quizId) });
    },
  });
}

/** lms.UpdateLmsAssignment under the builder's etag. */
export function useUpdateLmsAssignment() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      assignmentId,
      etag,
      changes,
    }: {
      assignmentId: string;
      etag: string;
      changes: lms.LmsAssignmentUpdate;
    }) =>
      call(
        api
          .get(LmsAssignmentApi)
          .updateLmsAssignment({ assignmentId, ifMatch: etag, lmsAssignmentUpdate: changes }),
      ),
    onSettled: (_r, _e, variables) => {
      staleRecords(qc, 'lms.LmsAssignment');
      void qc.invalidateQueries({ queryKey: ASSIGNMENT(variables.assignmentId) });
    },
  });
}

// ------------------------------------------------------------------------------------------- exercises

/** lms.GetProgrammingExercise with the test cases a student sees. */
export function useProgrammingExercise(programmingExerciseId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: EXERCISE(programmingExerciseId),
    queryFn: async () => {
      const exercises = api.get(ProgrammingExerciseApi);
      const id = programmingExerciseId as string;
      const [exercise, testCases] = await Promise.all([
        call(exercises.getProgrammingExercise({ programmingExerciseId: id })),
        allPages((page) =>
          call(exercises.listExerciseTestCases({ programmingExerciseId: id, size: 500, ...pageQuery(page) })),
        ),
      ]);
      return { exercise, testCases: testCases.items };
    },
    enabled: programmingExerciseId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.SubmitExercise: the student's code with their runner's outputs; the server judges each test case. */
export function useSubmitExercise() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      programmingExerciseId,
      request,
    }: {
      programmingExerciseId: string;
      request: lms.SubmitExerciseRequest;
    }) =>
      call(
        api
          .get(ProgrammingExerciseApi)
          .submitExercise({ programmingExerciseId, submitExerciseRequest: request }),
      ),
    onSettled: (_r, _e, variables) => {
      staleRecords(qc, 'lms.ExerciseSubmission');
      void qc.invalidateQueries({ queryKey: EXERCISE_SUBMISSIONS(variables.programmingExerciseId) });
    },
  });
}

/** lms.ListExerciseSubmissions of one exercise (the caller's own, as the server scopes them). */
export function useExerciseSubmissions(programmingExerciseId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: EXERCISE_SUBMISSIONS(programmingExerciseId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ExerciseSubmissionApi).listExerciseSubmissions({
            size: 500,
            filter: `exerciseId = "${programmingExerciseId as string}"`,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: programmingExerciseId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListExerciseTestResults of one submission (hidden cases stay hidden). */
export function useExerciseTestResults(exerciseSubmissionId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: EXERCISE_RESULTS(exerciseSubmissionId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ExerciseSubmissionApi).listExerciseTestResults({
            exerciseSubmissionId: exerciseSubmissionId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: exerciseSubmissionId !== null,
    staleTime: STALE_TIME.detail,
  });
}

// ------------------------------------------------------------------------------------------- external tools

/** lms.ListExternalTools: the tenant's tool registry. */
export function useExternalTools() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: EXTERNAL_TOOLS,
    queryFn: () =>
      allPages((page) => call(api.get(ExternalToolApi).listExternalTools({ size: 500, ...pageQuery(page) }))),
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ActivateExternalTool: a licence key enables a paid tool for the tenant. */
export function useActivateExternalTool() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ externalToolId, activationKey }: { externalToolId: string; activationKey: string }) =>
      call(
        api.get(ExternalToolApi).activateExternalTool({
          externalToolId,
          activateExternalToolRequest: { activationKey },
        }),
      ),
    onSettled: () => {
      staleRecords(qc, 'lms.ExternalTool');
      void qc.invalidateQueries({ queryKey: EXTERNAL_TOOLS });
    },
  });
}

/** lms.ListExternalToolResources: where tools are placed. */
export function useExternalToolResources() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: TOOL_RESOURCES,
    queryFn: () =>
      allPages((page) =>
        call(api.get(ExternalToolResourceApi).listExternalToolResources({ size: 500, ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListExternalToolLinks of one resource. */
export function useExternalToolLinks(externalToolResourceId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: TOOL_LINKS(externalToolResourceId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ExternalToolResourceApi).listExternalToolLinks({
            externalToolResourceId: externalToolResourceId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: externalToolResourceId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.LaunchExternalTool: the signed LTI form the client posts to the tool. */
export function useLaunchExternalTool() {
  const { api } = useRuntime();
  return useMutation({
    mutationFn: ({
      externalToolResourceId,
      externalToolLinkId,
      contentItemId,
    }: {
      externalToolResourceId: string;
      externalToolLinkId: string;
      contentItemId?: string;
    }) =>
      call(
        api.get(ExternalToolResourceApi).launchExternalTool({
          externalToolResourceId,
          externalToolLinkId,
          launchExternalToolRequest: contentItemId ? { contentItemId } : {},
        }),
      ),
  });
}

// ------------------------------------------------------------------------------------------- import / export

/** lms.ListImportExportJobs, newest first. */
export function useImportExportJobs() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: JOBS,
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ImportExportJobApi).listImportExportJobs({
            size: 500,
            sort: ['createTime,desc'],
            ...pageQuery(page),
          }),
        ),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ExportCourse / lms.ImportCoursePackage / lms.RestoreCohortArchive / lms.GetJobPackage. */
export function useCoursePackageTools() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = () => {
    staleRecords(qc, 'lms.ImportExportJob');
    void qc.invalidateQueries({ queryKey: JOBS });
  };
  const exportCourse = useMutation({
    mutationFn: ({ courseId }: { courseId: string }) =>
      call(api.get(LmsCourseApi).exportCourse({ courseId, body: {} })),
    onSettled: refresh,
  });
  const importPackage = useMutation({
    mutationFn: ({ courseId, request }: { courseId: string; request: lms.ImportCoursePackageRequest }) =>
      call(api.get(LmsCourseApi).importCoursePackage({ courseId, importCoursePackageRequest: request })),
    onSettled: refresh,
  });
  const restore = useMutation({
    mutationFn: ({ importExportJobId }: { importExportJobId: string }) =>
      call(
        api
          .get(ImportExportJobApi)
          .restoreCohortArchive({ importExportJobId, restoreCohortArchiveRequest: {} }),
      ),
    onSettled: refresh,
  });
  const packageLink = useMutation({
    mutationFn: ({ importExportJobId }: { importExportJobId: string }) =>
      call(api.get(ImportExportJobApi).getJobPackage({ importExportJobId })),
  });
  return { exportCourse, importPackage, restore, packageLink };
}

// ------------------------------------------------------------------------------------------- drive

/** lms.ListDriveItems: the caller's drive (the server scopes to owner and shares). */
export function useDriveItems() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: DRIVE,
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(DriveItemApi).listDriveItems({ size: 500, sort: ['createTime,desc'], ...pageQuery(page) }),
        ),
      ),
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListDriveItemShares of one item. */
export function useDriveItemShares(driveItemId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: DRIVE_SHARES(driveItemId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(DriveItemApi).listDriveItemShares({
            driveItemId: driveItemId as string,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: driveItemId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ShareDriveItem: replaces who sees the caller's own item. */
export function useShareDriveItem() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ driveItemId, userIds }: { driveItemId: string; userIds: string[] }) =>
      call(api.get(DriveItemApi).shareDriveItem({ driveItemId, shareDriveItemRequest: { userIds } })),
    onSettled: (_r, _e, variables) => {
      staleRecords(qc, 'lms.DriveItem');
      void qc.invalidateQueries({ queryKey: DRIVE });
      void qc.invalidateQueries({ queryKey: DRIVE_SHARES(variables.driveItemId) });
    },
  });
}

/** platform.GetMyStorageUsage: the quota meter of the drive screen. */
export function useMyStorageUsage() {
  const { api } = useRuntime();
  return useQuery({
    queryKey: STORAGE,
    queryFn: () => call(api.get(StorageQuotaApi).getMyStorageUsage()),
    staleTime: STALE_TIME.detail,
  });
}

// ------------------------------------------------------------------------------------------- group work

const GROUP = (contentItemId: string | null, groupId: string | null) =>
  queryKey('lms', 'assignment-group', `${contentItemId}#${groupId}`);
const GROUP_MEMBERS = (groupId: string | null) => queryKey('lms', 'assignment-group-members', groupId);

/** lms.GetAssignmentGroup (the item's id comes with the link into the workspace). */
export function useAssignmentGroup(contentItemId: string | null, assignmentGroupId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: GROUP(contentItemId, assignmentGroupId),
    queryFn: () =>
      call(
        api.get(ContentItemApi).getAssignmentGroup({
          contentItemId: contentItemId as string,
          assignmentGroupId: assignmentGroupId as string,
        }),
      ),
    enabled: contentItemId !== null && assignmentGroupId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** lms.ListAssignmentGroupMembers of one group. */
export function useAssignmentGroupMembers(assignmentGroupId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: GROUP_MEMBERS(assignmentGroupId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(AssignmentGroupMemberApi).listAssignmentGroupMembers({
            size: 500,
            filter: `assignmentGroupId = "${assignmentGroupId as string}"`,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: assignmentGroupId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** The group workspace steps: ask to join, accept or reject a request, set the members, submit the peer review. */
export function useGroupWorkspaceSteps() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  const refresh = (groupId: string) => {
    staleRecords(qc, 'lms.AssignmentGroupMember');
    void qc.invalidateQueries({ queryKey: GROUP_MEMBERS(groupId) });
  };
  const requestToJoin = useMutation({
    mutationFn: ({
      contentItemId,
      assignmentGroupId,
    }: {
      contentItemId: string;
      assignmentGroupId: string;
    }) => call(api.get(ContentItemApi).requestToJoinGroup({ contentItemId, assignmentGroupId, body: {} })),
    onSettled: (_r, _e, v) => refresh(v.assignmentGroupId),
  });
  const decide = useMutation({
    mutationFn: ({
      assignmentGroupMemberId,
      accept,
    }: {
      assignmentGroupMemberId: string;
      assignmentGroupId: string;
      accept: boolean;
    }) => {
      const members = api.get(AssignmentGroupMemberApi);
      return accept
        ? call(members.acceptGroupRequest({ assignmentGroupMemberId, acceptGroupRequestRequest: {} }))
        : call(members.rejectGroupRequest({ assignmentGroupMemberId, rejectGroupRequestRequest: {} }));
    },
    onSettled: (_r, _e, v) => refresh(v.assignmentGroupId),
  });
  const setMembers = useMutation({
    mutationFn: ({
      contentItemId,
      assignmentGroupId,
      userIds,
    }: {
      contentItemId: string;
      assignmentGroupId: string;
      userIds: string[];
    }) =>
      call(
        api.get(ContentItemApi).setGroupMembers({
          contentItemId,
          assignmentGroupId,
          setGroupMembersRequest: { addUserIds: userIds },
        }),
      ),
    onSettled: (_r, _e, v) => refresh(v.assignmentGroupId),
  });
  const submitPeerReview = useMutation({
    mutationFn: ({
      contentItemId,
      assignmentGroupId,
      request,
    }: {
      contentItemId: string;
      assignmentGroupId: string;
      request: lms.SubmitPeerReviewRequest;
    }) =>
      call(
        api.get(ContentItemApi).submitPeerReview({
          contentItemId,
          assignmentGroupId,
          submitPeerReviewRequest: request,
        }),
      ),
    onSettled: (_r, _e, v) => refresh(v.assignmentGroupId),
  });
  return { requestToJoin, decide, setMembers, submitPeerReview };
}

// ------------------------------------------------------------------------------------------- access logs

/** lms.ListAccessLogs within a dated range (tenant administrators only; the range is required). */
export function useAccessLogs(filter: string | null, page: PageParam = FIRST_PAGE) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: ACCESS_LOGS(filter ?? '', page.page),
    queryFn: () =>
      call(
        api.get(AccessLogApi).listAccessLogs({
          size: 100,
          filter: filter as string,
          sort: ['accessedAt,desc'],
          ...pageQuery(page),
        }),
      ),
    enabled: filter !== null,
    staleTime: STALE_TIME.detail,
  });
}
