import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import type { assessment } from '@mysms/sdk';
import { AssignmentApi } from '@mysms/sdk/assessment/apis/AssignmentApi';
import { AssignmentSubmissionApi } from '@mysms/sdk/assessment/apis/AssignmentSubmissionApi';
import { ExamApi } from '@mysms/sdk/assessment/apis/ExamApi';
import { ExamResultApi } from '@mysms/sdk/assessment/apis/ExamResultApi';
import { ExamSessionApi } from '@mysms/sdk/assessment/apis/ExamSessionApi';
import { GradingBandApi } from '@mysms/sdk/assessment/apis/GradingBandApi';
import { GradingSchemeApi } from '@mysms/sdk/assessment/apis/GradingSchemeApi';
import { HomeworkSubmissionApi } from '@mysms/sdk/assessment/apis/HomeworkSubmissionApi';
import { MarksheetApi } from '@mysms/sdk/assessment/apis/MarksheetApi';
import { MarksheetLineApi } from '@mysms/sdk/assessment/apis/MarksheetLineApi';
import { ResultTemplateApi } from '@mysms/sdk/assessment/apis/ResultTemplateApi';
import { allPages, pageQuery } from '../api/pages';
import { call } from '../api/unwrap';
import { queryKey } from '../query/keys';
import { STALE_TIME } from '../query/queryClient';
import { useRuntime } from '../react/useRuntime';
import { useSession } from './session';

export type Assignment = assessment.Assignment;
export type AssignmentApproval = assessment.AssignmentApproval;
export type AssignmentSubmission = assessment.AssignmentSubmission;
export type Exam = assessment.Exam;
export type ExamResult = assessment.ExamResult;
export type ExamResultList = assessment.ExamResultList;
export type ExamSession = assessment.ExamSession;
export type GradingBand = assessment.GradingBand;
export type GradingScheme = assessment.GradingScheme;
export type MarkEntry = assessment.MarkEntry;
export type Marksheet = assessment.Marksheet;
export type MarksheetLine = assessment.MarksheetLine;
export type ResultCard = assessment.ResultCard;
export type ResultTemplate = assessment.ResultTemplate;
export type ResultTemplateBand = assessment.ResultTemplateBand;

export type AssignmentStep = 'publish' | 'approve' | 'reject' | 'finish' | 'cancel' | 'reset';
export type AssignmentSubmissionStep = 'submit' | 'accept' | 'reject' | 'requestChanges' | 'reset' | 'grade';
export type ExamSessionStep = 'schedule' | 'markHeld' | 'complete' | 'cancel' | 'reset';
export type ExamStep = 'schedule' | 'markHeld' | 'resultsUpdated' | 'complete' | 'cancel' | 'reset';
export type MarksheetStep = 'validate' | 'publish' | 'cancel' | 'reset';
export type AssessmentStep =
  | AssignmentStep
  | AssignmentSubmissionStep
  | ExamSessionStep
  | ExamStep
  | MarksheetStep
  | 'populateResults'
  | 'allocateRooms'
  | 'recordMarks'
  | 'generateResults';

const ASSIGNMENT_SUBMISSIONS = (id: string | null) => queryKey('assessment', 'assignment-submissions', id);
const ASSIGNMENT_APPROVALS = (id: string | null) => queryKey('assessment', 'assignment-approvals', id);
const EXAM_RESULTS = (id: string | null) => queryKey('assessment', 'exam-results', id);
const MARKSHEET_LINES = (id: string | null) => queryKey('assessment', 'marksheet-lines', id);
const STUDENT_RESULTS = (id: string | null, filters: { academicYearId?: string; examSessionId?: string }) =>
  queryKey('assessment', 'student-results', { id, ...filters });
const GRADING_BANDS = (id: string | null) => queryKey('assessment', 'grading-bands', id);

function staleRecords(qc: QueryClient, resource: string): void {
  void qc.invalidateQueries({ queryKey: queryKey('records', resource) });
  void qc.invalidateQueries({ queryKey: queryKey('records', resource + '#item') });
}

function staleAssessment(qc: QueryClient, resource: string, id: string): void {
  staleRecords(qc, 'assessment.' + resource);
  void qc.invalidateQueries({ queryKey: queryKey('assessment', resource, id) });
}

const NO_STUDENTS: readonly string[] = [];

/**
 * The student records of the signed-in person, as iam.GetMe names them in the session. No sis read is needed (a
 * student holds no sis.student.read); the assessment service confirms ownership itself on every request.
 */
export function useMyStudentIds(): readonly string[] {
  return useSession((state) => state.me?.studentIds ?? NO_STUDENTS);
}

/**
 * assessment.ListAssignmentSubmissions for one assignment; with a student, only that student's submission (a student
 * or guardian asks for their own rows only — the service refuses everyone else's anyway).
 */
export function useAssignmentSubmissions(assignmentId: string | null, studentId: string | null = null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: [...ASSIGNMENT_SUBMISSIONS(assignmentId), studentId] as const,
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(AssignmentSubmissionApi).listAssignmentSubmissions({
            size: 500,
            filter:
              'assignmentId = "' +
              (assignmentId as string) +
              '"' +
              (studentId === null ? '' : ' AND studentId = "' + studentId + '"'),
            sort: ['createTime,asc'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: assignmentId !== null,
    staleTime: STALE_TIME.detail,
    select: (result) => ({
      items: [...result.items].sort((left, right) =>
        (left.submittedAt ?? '').localeCompare(right.submittedAt ?? ''),
      ),
    }),
  });
}

/** Reads approval records under one assignment; pending state belongs to these records, not Assignment.status. */
export function useAssignmentApprovals(assignmentId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: ASSIGNMENT_APPROVALS(assignmentId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(AssignmentApi).listAssignmentApprovals({
            assignmentId: assignmentId as string,
            size: 500,
            sort: ['createTime,asc'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: assignmentId !== null,
    staleTime: STALE_TIME.detail,
  });
}

/** Creates a pending approval record for an assignment draft. */
export function useCreateAssignmentApproval() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (assignmentId: string) =>
      call(
        api.get(AssignmentApi).createAssignmentApproval({
          assignmentId,
          assignmentApproval: { decision: 'pending' },
        }),
      ),
    onSettled: (_result, _error, assignmentId) => {
      staleRecords(qc, 'assessment.AssignmentApproval');
      staleAssessment(qc, 'Assignment', assignmentId);
      return qc.invalidateQueries({ queryKey: ASSIGNMENT_APPROVALS(assignmentId) });
    },
  });
}

/** Executes an assignment lifecycle step under the last-read ETag. */
export function useAssignmentStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      assignmentId,
      step,
      etag,
      reason,
    }: {
      assignmentId: string;
      step: AssignmentStep;
      etag: string;
      reason?: string;
    }) => {
      const assignments = api.get(AssignmentApi);
      const version = { etag };
      const decided = { ...version, ...(reason?.trim() ? { reason: reason.trim() } : {}) };
      switch (step) {
        case 'publish':
          return call(assignments.publishAssignment({ assignmentId, publishAssignmentRequest: version }));
        case 'approve':
          return call(assignments.approveAssignment({ assignmentId, approveAssignmentRequest: decided }));
        case 'reject':
          return call(assignments.rejectAssignment({ assignmentId, rejectAssignmentRequest: decided }));
        case 'finish':
          return call(assignments.finishAssignment({ assignmentId, finishAssignmentRequest: version }));
        case 'cancel':
          return call(assignments.cancelAssignment({ assignmentId, cancelAssignmentRequest: decided }));
        case 'reset':
          return call(assignments.resetAssignment({ assignmentId, resetAssignmentRequest: version }));
      }
    },
    onSettled: (_result, _error, variables) => {
      staleAssessment(qc, 'Assignment', variables.assignmentId);
      void qc.invalidateQueries({ queryKey: ASSIGNMENT_APPROVALS(variables.assignmentId) });
    },
  });
}

/** Executes a submission lifecycle step under its last-read ETag. */
export function useAssignmentSubmissionStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      submissionId,
      step,
      etag,
      reason,
      marks,
      feedback,
    }: {
      submissionId: string;
      step: AssignmentSubmissionStep;
      etag: string;
      reason?: string;
      marks?: string;
      feedback?: string;
      assignmentId: string;
    }) => {
      const submissions = api.get(AssignmentSubmissionApi);
      const version = { etag };
      const decided = { ...version, ...(reason?.trim() ? { reason: reason.trim() } : {}) };
      switch (step) {
        case 'submit':
          return call(
            submissions.submitAssignmentSubmission({
              assignmentSubmissionId: submissionId,
              submitAssignmentSubmissionRequest: version,
            }),
          );
        case 'accept':
          return call(
            submissions.acceptAssignmentSubmission({
              assignmentSubmissionId: submissionId,
              acceptAssignmentSubmissionRequest: version,
            }),
          );
        case 'reject':
          return call(
            submissions.rejectAssignmentSubmission({
              assignmentSubmissionId: submissionId,
              rejectAssignmentSubmissionRequest: decided,
            }),
          );
        case 'requestChanges':
          return call(
            submissions.requestSubmissionChanges({
              assignmentSubmissionId: submissionId,
              requestSubmissionChangesRequest: decided,
            }),
          );
        case 'reset':
          return call(
            submissions.resetAssignmentSubmission({
              assignmentSubmissionId: submissionId,
              resetAssignmentSubmissionRequest: version,
            }),
          );
        case 'grade':
          if (marks === undefined) throw new Error('Grading needs the marks.');
          return call(
            submissions.gradeAssignmentSubmission({
              assignmentSubmissionId: submissionId,
              gradeAssignmentSubmissionRequest: {
                ...version,
                marks,
                ...(feedback?.trim() ? { feedback: feedback.trim() } : {}),
              },
            }),
          );
      }
    },
    onSettled: (_result, _error, variables) => {
      staleRecords(qc, 'assessment.AssignmentSubmission');
      void qc.invalidateQueries({ queryKey: ASSIGNMENT_SUBMISSIONS(variables.assignmentId) });
    },
  });
}

/** assessment.ListExamResults for one paper. */
export function useExamResults(examId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: EXAM_RESULTS(examId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(ExamResultApi).listExamResults({
            size: 500,
            filter: 'examId = "' + (examId as string) + '"',
            sort: ['createTime,asc'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: examId !== null,
    staleTime: STALE_TIME.detail,
    select: (result) => ({
      items: [...result.items].sort((left, right) => left.studentId.localeCompare(right.studentId)),
    }),
  });
}

/** Executes an exam-session lifecycle transition. */
export function useExamSessionStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      examSessionId,
      step,
      etag,
      reason,
    }: {
      examSessionId: string;
      step: ExamSessionStep;
      etag: string;
      reason?: string;
    }) => {
      const sessions = api.get(ExamSessionApi);
      const version = { etag };
      const decided = { ...version, ...(reason?.trim() ? { reason: reason.trim() } : {}) };
      switch (step) {
        case 'schedule':
          return call(sessions.scheduleExamSession({ examSessionId, scheduleExamSessionRequest: version }));
        case 'markHeld':
          return call(sessions.markExamSessionHeld({ examSessionId, markExamSessionHeldRequest: version }));
        case 'complete':
          return call(sessions.completeExamSession({ examSessionId, completeExamSessionRequest: version }));
        case 'cancel':
          return call(sessions.cancelExamSession({ examSessionId, cancelExamSessionRequest: decided }));
        case 'reset':
          return call(sessions.resetExamSession({ examSessionId, resetExamSessionRequest: version }));
      }
    },
    onSettled: (_result, _error, variables) => staleAssessment(qc, 'ExamSession', variables.examSessionId),
  });
}

/** Executes an exam lifecycle transition; bulk result work has separate typed hooks below. */
export function useExamStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      examId,
      step,
      etag,
      reason,
    }: {
      examId: string;
      step: ExamStep;
      etag: string;
      reason?: string;
    }) => {
      const exams = api.get(ExamApi);
      const version = { etag };
      const decided = { ...version, ...(reason?.trim() ? { reason: reason.trim() } : {}) };
      switch (step) {
        case 'schedule':
          return call(exams.scheduleExam({ examId, scheduleExamRequest: version }));
        case 'markHeld':
          return call(exams.markExamHeld({ examId, markExamHeldRequest: version }));
        case 'resultsUpdated':
          return call(exams.markExamResultsUpdated({ examId, markExamResultsUpdatedRequest: version }));
        case 'complete':
          return call(exams.completeExam({ examId, completeExamRequest: version }));
        case 'cancel':
          return call(exams.cancelExam({ examId, cancelExamRequest: decided }));
        case 'reset':
          return call(exams.resetExam({ examId, resetExamRequest: version }));
      }
    },
    onSettled: (_result, _error, variables) => staleAssessment(qc, 'Exam', variables.examId),
  });
}

/** Creates attendee rows from the class or batch for an exam. */
export function usePopulateExamResults() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (examId: string) => call(api.get(ExamApi).populateExamResults({ examId, body: {} })),
    onSettled: (_result, _error, examId) => {
      if (examId) {
        staleRecords(qc, 'assessment.ExamResult');
        void qc.invalidateQueries({ queryKey: EXAM_RESULTS(examId) });
      }
    },
  });
}

/** Allocates exam attendees into the supplied rooms according to server-side capacity. */
export function useAllocateExamRooms() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ examId, roomIds }: { examId: string; roomIds: string[] }) =>
      call(api.get(ExamApi).allocateExamRooms({ examId, allocateExamRoomsRequest: { roomIds } })),
    onSettled: (_result, _error, variables) => {
      staleRecords(qc, 'assessment.ExamResult');
      return qc.invalidateQueries({ queryKey: EXAM_RESULTS(variables.examId) });
    },
  });
}

/** Writes a whole marks grid as one all-or-nothing operation. */
export function useRecordExamMarks() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ examId, entries }: { examId: string; entries: MarkEntry[] }) =>
      call(api.get(ExamApi).recordExamMarks({ examId, recordExamMarksRequest: { entries } })),
    onSettled: (_result, _error, variables) => {
      staleRecords(qc, 'assessment.ExamResult');
      return qc.invalidateQueries({ queryKey: EXAM_RESULTS(variables.examId) });
    },
  });
}

/** Marksheet detail lines, ordered by student id. */
export function useMarksheetLines(marksheetId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: MARKSHEET_LINES(marksheetId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(MarksheetLineApi).listMarksheetLines({
            size: 500,
            filter: 'marksheetId = "' + (marksheetId as string) + '"',
            sort: ['createTime,asc'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: marksheetId !== null,
    staleTime: STALE_TIME.detail,
    select: (result) => ({
      items: [...result.items].sort((left, right) => left.studentId.localeCompare(right.studentId)),
    }),
  });
}

/** Validates or releases a marksheet, or cancels/resets it before publication. */
export function useMarksheetStep() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      marksheetId,
      step,
      etag,
      reason,
      visibleToGuardians,
    }: {
      marksheetId: string;
      step: MarksheetStep;
      etag: string;
      reason?: string;
      visibleToGuardians?: boolean;
    }) => {
      const marksheets = api.get(MarksheetApi);
      const version = { etag };
      switch (step) {
        case 'validate':
          return call(marksheets.validateMarksheet({ marksheetId, validateMarksheetRequest: version }));
        case 'publish':
          return call(
            marksheets.publishMarksheet({
              marksheetId,
              ifMatch: etag,
              publishMarksheetRequest: visibleToGuardians === undefined ? {} : { visibleToGuardians },
            }),
          );
        case 'cancel':
          return call(
            marksheets.cancelMarksheet({
              marksheetId,
              cancelMarksheetRequest: { ...version, ...(reason?.trim() ? { reason: reason.trim() } : {}) },
            }),
          );
        case 'reset':
          return call(marksheets.resetMarksheet({ marksheetId, resetMarksheetRequest: version }));
      }
    },
    onSettled: (_result, _error, variables) => {
      staleAssessment(qc, 'Marksheet', variables.marksheetId);
      // what students read follows validation and publication; a cancelled marksheet releases its results
      void qc.invalidateQueries({ queryKey: queryKey('assessment', 'student-results') });
      if (variables.step === 'cancel') {
        staleRecords(qc, 'assessment.ExamResult');
        staleRecords(qc, 'assessment.ResultTemplate');
        void qc.invalidateQueries({ queryKey: queryKey('assessment', 'exam-results') });
        void qc.invalidateQueries({ queryKey: queryKey('assessment', 'marksheet-lines') });
      }
    },
  });
}

/** Reads result cards for a student; the service returns only published result data the caller may read. */
export function useStudentResults(
  studentId: string | null,
  filters: { academicYearId?: string; examSessionId?: string } = {},
) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: STUDENT_RESULTS(studentId, filters),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(MarksheetLineApi).getStudentResults({
            studentId: studentId as string,
            ...filters,
            size: 500,
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: studentId !== null,
    staleTime: STALE_TIME.list,
  });
}

/** Creates a long-running result-generation job; poll its operation id through core's operation hooks. */
export function useResultTemplateGenerateResults() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (resultTemplateId: string) =>
      call(api.get(ResultTemplateApi).generateResults({ resultTemplateId, body: {} })),
    onSettled: () => {
      staleRecords(qc, 'assessment.ResultTemplate');
      staleRecords(qc, 'assessment.Marksheet');
      staleRecords(qc, 'assessment.ExamResult');
      // generation links every result of the session to a new marksheet line
      void qc.invalidateQueries({ queryKey: queryKey('assessment', 'exam-results') });
      return qc.invalidateQueries({ queryKey: queryKey('assessment', 'marksheet-lines') });
    },
  });
}

/** A student starts a homework submission; file attachments use the platform upload flow after this creates its id. */
export function useCreateHomeworkSubmission() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      homeworkId,
      studentId,
      submittedOn,
    }: {
      homeworkId: string;
      studentId: string;
      submittedOn?: string;
    }) =>
      call(
        api.get(HomeworkSubmissionApi).createHomeworkSubmission({
          homeworkSubmission: { homeworkId, studentId, ...(submittedOn ? { submittedOn } : {}) },
        }),
      ),
    onSettled: (_result, _error, variables) => {
      staleRecords(qc, 'assessment.HomeworkSubmission');
      return qc.invalidateQueries({
        queryKey: queryKey('assessment', 'homework-submissions', variables.homeworkId),
      });
    },
  });
}

/** Stores a student's reply to teacher feedback under the submission's last-read ETag. */
export function useReplyToHomeworkFeedback() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      submissionId,
      etag,
      studentReply,
    }: {
      homeworkId: string;
      submissionId: string;
      etag: string;
      studentReply: string;
    }) =>
      call(
        api.get(HomeworkSubmissionApi).replyToHomeworkFeedback({
          homeworkSubmissionId: submissionId,
          replyToHomeworkFeedbackRequest: {
            etag,
            studentReply: studentReply.trim(),
          },
        }),
      ),
    onSettled: (_result, _error, variables) => {
      staleRecords(qc, 'assessment.HomeworkSubmission');
      return qc.invalidateQueries({
        queryKey: queryKey('assessment', 'homework-submissions', variables.homeworkId),
      });
    },
  });
}

const GRADING_SCHEMES = queryKey('assessment', 'grading-schemes');

/** Lists reusable grading schemes. Typed CRUD is kept here until the standard-resource generator includes it. */
export function useGradingSchemes(enabled = true) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: GRADING_SCHEMES,
    enabled,
    queryFn: () =>
      allPages((page) =>
        call(api.get(GradingSchemeApi).listGradingSchemes({ size: 500, ...pageQuery(page) })),
      ),
    staleTime: STALE_TIME.list,
  });
}

export type GradingSchemeDraft = Omit<
  GradingScheme,
  'id' | 'createTime' | 'createdBy' | 'updateTime' | 'updatedBy' | 'etag'
>;
export type GradingBandDraft = Omit<
  GradingBand,
  'id' | 'createTime' | 'createdBy' | 'updateTime' | 'updatedBy' | 'etag'
>;

function refreshGrading(qc: QueryClient): void {
  void qc.invalidateQueries({ queryKey: GRADING_SCHEMES });
  staleRecords(qc, 'assessment.GradingScheme');
  staleRecords(qc, 'assessment.GradingBand');
  void qc.invalidateQueries({ queryKey: queryKey('assessment', 'grading-bands') });
}

export function useCreateGradingScheme() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (gradingScheme: GradingSchemeDraft) =>
      call(api.get(GradingSchemeApi).createGradingScheme({ gradingScheme })),
    onSettled: () => refreshGrading(qc),
  });
}

export function useUpdateGradingScheme() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      gradingSchemeId,
      etag,
      gradingSchemeUpdate,
    }: {
      gradingSchemeId: string;
      etag: string;
      gradingSchemeUpdate: assessment.GradingSchemeUpdate;
    }) =>
      call(
        api
          .get(GradingSchemeApi)
          .updateGradingScheme({ gradingSchemeId, ifMatch: etag, gradingSchemeUpdate }),
      ),
    onSettled: () => refreshGrading(qc),
  });
}

export function useDeleteGradingScheme() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ gradingSchemeId, etag }: { gradingSchemeId: string; etag: string }) =>
      call(api.get(GradingSchemeApi).deleteGradingScheme({ gradingSchemeId, ifMatch: etag })),
    onSettled: () => refreshGrading(qc),
  });
}

export function useCreateGradingBand() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (gradingBand: GradingBandDraft) =>
      call(api.get(GradingBandApi).createGradingBand({ gradingBand })),
    onSettled: () => refreshGrading(qc),
  });
}

export function useUpdateGradingBand() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      gradingBandId,
      etag,
      gradingBandUpdate,
    }: {
      gradingBandId: string;
      etag: string;
      gradingBandUpdate: assessment.GradingBandUpdate;
    }) =>
      call(api.get(GradingBandApi).updateGradingBand({ gradingBandId, ifMatch: etag, gradingBandUpdate })),
    onSettled: () => refreshGrading(qc),
  });
}

export function useDeleteGradingBand() {
  const { api } = useRuntime();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ gradingBandId, etag }: { gradingBandId: string; etag: string }) =>
      call(api.get(GradingBandApi).deleteGradingBand({ gradingBandId, ifMatch: etag })),
    onSettled: () => refreshGrading(qc),
  });
}
/** Grading bands nested under the selected scheme. */
export function useGradingBands(gradingSchemeId: string | null) {
  const { api } = useRuntime();
  return useQuery({
    queryKey: GRADING_BANDS(gradingSchemeId),
    queryFn: () =>
      allPages((page) =>
        call(
          api.get(GradingBandApi).listGradingBands({
            size: 500,
            filter: 'gradingSchemeId = "' + (gradingSchemeId as string) + '"',
            sort: ['createTime,asc'],
            ...pageQuery(page),
          }),
        ),
      ),
    enabled: gradingSchemeId !== null,
    staleTime: STALE_TIME.detail,
    select: (result) => ({
      items: [...result.items].sort(
        (left, right) => Number(left.minPercent ?? 0) - Number(right.minPercent ?? 0),
      ),
    }),
  });
}
