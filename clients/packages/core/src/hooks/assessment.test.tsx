import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, json, onePage } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  useAllocateExamRooms,
  useAssignmentApprovals,
  useAssignmentStep,
  useAssignmentSubmissions,
  useAssignmentSubmissionStep,
  useCreateAssignmentApproval,
  useCreateGradingBand,
  useCreateGradingScheme,
  useCreateHomeworkSubmission,
  useDeleteGradingBand,
  useDeleteGradingScheme,
  useExamSessionStep,
  useExamStep,
  useExamResults,
  useGradingBands,
  useGradingSchemes,
  useMarksheetStep,
  useMyStudentIds,
  usePopulateExamResults,
  useRecordExamMarks,
  useReplyToHomeworkFeedback,
  useResultTemplateGenerateResults,
  useStudentResults,
  useUpdateGradingBand,
  useUpdateGradingScheme,
  useMarksheetLines,
} from './assessment';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const user = { id: 'user-1', username: 'teacher', personId: 'person-1' };

function setup() {
  const assignmentSubmissions = [
    { id: 's2', assignmentId: 'a1', studentId: 'student-b', submittedAt: '2026-03-02T00:00:00Z' },
    { id: 's1', assignmentId: 'a1', studentId: 'student-a', submittedAt: '2026-03-01T00:00:00Z' },
  ];
  const examResults = [
    { id: 'e2', examId: 'e1', studentId: 'student-b' },
    { id: 'e1', examId: 'e1', studentId: 'student-a' },
  ];
  const marksheetLines = [
    { id: 'm2', marksheetId: 'm1', studentId: 'student-b' },
    { id: 'm1', marksheetId: 'm1', studentId: 'student-a' },
  ];
  const gradingBands = [
    { id: 'g2', gradingSchemeId: 'g1', minPercent: '50' },
    { id: 'g1', gradingSchemeId: 'g1', minPercent: '0' },
  ];
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user }),
    'GET /v1/iam/me': () => json({ user, permissions: [], studentIds: ['student-a'] }),
    'GET /v1/assessment/assignment-submissions': (request) => onePage(request, assignmentSubmissions),
    'GET /v1/assessment/exam-results': (request) => onePage(request, examResults),
    'GET /v1/assessment/marksheet-lines': (request) => onePage(request, marksheetLines),
    'GET /v1/assessment/grading-bands': (request) => onePage(request, gradingBands),
    'GET /v1/assessment/assignments/{assignmentId}/approvals': (request) => onePage(request, []),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  return { api, runtime };
}

describe('assessment list sort allowlists', () => {
  it('uses repository-supported request sorts and locally orders fully loaded pages', async () => {
    const { api, runtime } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'teacher', password: 'pw' });
    const wrapper = ({ children }: { children: ReactNode }) => (
      <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
    );
    const { result } = renderHook(
      () => ({
        submissions: useAssignmentSubmissions('a1'),
        approvals: useAssignmentApprovals('a1'),
        examResults: useExamResults('e1'),
        marksheetLines: useMarksheetLines('m1'),
        gradingBands: useGradingBands('g1'),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.gradingBands.data?.items).toHaveLength(2));
    expect(result.current.submissions.data?.items.map((item) => item.id)).toEqual(['s1', 's2']);
    expect(result.current.examResults.data?.items.map((item) => item.id)).toEqual(['e1', 'e2']);
    expect(result.current.marksheetLines.data?.items.map((item) => item.id)).toEqual(['m1', 'm2']);
    expect(result.current.gradingBands.data?.items.map((item) => item.id)).toEqual(['g1', 'g2']);

    const paths = [
      '/v1/assessment/assignment-submissions',
      '/v1/assessment/assignments/a1/approvals',
      '/v1/assessment/exam-results',
      '/v1/assessment/marksheet-lines',
      '/v1/assessment/grading-bands',
    ];
    for (const path of paths) {
      const request = api.calls.find((call) => call.path === path);
      expect(request, path).toBeDefined();
      expect(new URLSearchParams(request?.search).getAll('sort'), path).toEqual(['createTime,asc']);
    }
  });
});

describe('assessment hook operations', () => {
  it('runs every assessment query and lifecycle mutation through the typed client', async () => {
    const { runtime } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'teacher', password: 'pw' });

    const calls: { api: string; method: string; args: unknown[] }[] = [];
    let resultPageCalls = 0;
    const listResults: Record<string, { data: unknown[]; paging: { page: number; next?: string } }> = {
      listAssignmentSubmissions: { data: [], paging: { page: 1 } },
      listAssignmentApprovals: { data: [], paging: { page: 1 } },
      listExamResults: { data: [], paging: { page: 1 } },
      listMarksheetLines: { data: [], paging: { page: 1 } },
      listGradingSchemes: { data: [{ id: 'scheme-1' }], paging: { page: 1 } },
      listGradingBands: { data: [{ id: 'band-1', minPercent: '0' }], paging: { page: 1 } },
    };
    runtime.api = {
      ...runtime.api,
      get: ((apiClass: unknown) =>
        new Proxy(
          {},
          {
            get:
              (_target, property) =>
              (...args: unknown[]) => {
                const method = String(property);
                const api = typeof apiClass === 'function' ? apiClass.name : 'UnknownApi';
                calls.push({ api, method, args });
                if (method === 'getStudentResults' && resultPageCalls++ === 0) {
                  return Promise.resolve({
                    data: [{ id: 'line-1' }],
                    paging: { page: 1, next: '?page=2&cursor=next-page' },
                  });
                }
                if (method === 'getStudentResults' && resultPageCalls === 2) {
                  return Promise.resolve({ data: [{ id: 'line-2' }], paging: { page: 2 } });
                }
                return Promise.resolve(listResults[method] ?? { id: 'saved' });
              },
          },
        )) as never,
    };

    const wrapper = ({ children }: { children: ReactNode }) => (
      <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
    );
    const { result } = renderHook(
      () => ({
        myStudents: useMyStudentIds(),
        submissions: useAssignmentSubmissions('a1'),
        approvals: useAssignmentApprovals('a1'),
        examResults: useExamResults('e1'),
        marksheetLines: useMarksheetLines('m1'),
        studentResults: useStudentResults('student-1', {
          academicYearId: 'year-1',
          examSessionId: 'session-1',
        }),
        schemes: useGradingSchemes(),
        bands: useGradingBands('scheme-1'),
        createApproval: useCreateAssignmentApproval(),
        assignmentStep: useAssignmentStep(),
        submissionStep: useAssignmentSubmissionStep(),
        sessionStep: useExamSessionStep(),
        examStep: useExamStep(),
        populateResults: usePopulateExamResults(),
        allocateRooms: useAllocateExamRooms(),
        recordMarks: useRecordExamMarks(),
        marksheetStep: useMarksheetStep(),
        generateResults: useResultTemplateGenerateResults(),
        createHomeworkSubmission: useCreateHomeworkSubmission(),
        replyToFeedback: useReplyToHomeworkFeedback(),
        createScheme: useCreateGradingScheme(),
        updateScheme: useUpdateGradingScheme(),
        deleteScheme: useDeleteGradingScheme(),
        createBand: useCreateGradingBand(),
        updateBand: useUpdateGradingBand(),
        deleteBand: useDeleteGradingBand(),
      }),
      { wrapper },
    );

    await waitFor(() => {
      expect(result.current.studentResults.data?.items).toHaveLength(2);
      expect(result.current.schemes.data?.items).toHaveLength(1);
      expect(result.current.bands.data?.items).toHaveLength(1);
    });

    const schemeDraft = {
      institutionId: 'institution-1',
      name: 'Term grades',
      type: 'scholastic',
      gradingMethod: 'percentage_band',
    } as never;
    const bandDraft = {
      gradingSchemeId: 'scheme-1',
      minPercent: '0',
      maxPercent: '100',
      label: 'A',
      isPass: true,
    } as never;

    await act(async () => {
      await result.current.createApproval.mutateAsync('a1');
      for (const [step, reason] of [
        ['publish', undefined],
        ['approve', ' approved '],
        ['reject', ' rejected '],
        ['finish', undefined],
        ['cancel', ' cancelled '],
        ['reset', undefined],
      ] as const) {
        await result.current.assignmentStep.mutateAsync({
          assignmentId: 'a1',
          step,
          etag: 'v1',
          ...(reason === undefined ? {} : { reason }),
        });
      }
      for (const [step, reason] of [
        ['submit', undefined],
        ['accept', undefined],
        ['reject', ' rejected '],
        ['requestChanges', ' changes requested '],
        ['reset', undefined],
        ['grade', undefined],
      ] as const) {
        await result.current.submissionStep.mutateAsync({
          assignmentId: 'a1',
          submissionId: 'sub-1',
          step,
          etag: 'v1',
          ...(reason === undefined ? {} : { reason }),
          ...(step === 'grade' ? { marks: '8', feedback: ' good ' } : {}),
        });
      }
      for (const [step, reason] of [
        ['schedule', undefined],
        ['markHeld', undefined],
        ['complete', undefined],
        ['cancel', ' cancelled '],
        ['reset', undefined],
      ] as const) {
        await result.current.sessionStep.mutateAsync({
          examSessionId: 'session-1',
          step,
          etag: 'v1',
          ...(reason === undefined ? {} : { reason }),
        });
      }
      for (const [step, reason] of [
        ['schedule', undefined],
        ['markHeld', undefined],
        ['resultsUpdated', undefined],
        ['complete', undefined],
        ['cancel', ' cancelled '],
        ['reset', undefined],
      ] as const) {
        await result.current.examStep.mutateAsync({
          examId: 'e1',
          step,
          etag: 'v1',
          ...(reason === undefined ? {} : { reason }),
        });
      }
      await result.current.populateResults.mutateAsync('e1');
      await result.current.allocateRooms.mutateAsync({ examId: 'e1', roomIds: ['room-1'] });
      await result.current.recordMarks.mutateAsync({
        examId: 'e1',
        entries: [{ studentId: 'student-1', attendanceStatus: 'present', marksObtained: '8', etag: 'v3' }],
      });
      for (const [step, visibleToGuardians, reason] of [
        ['validate', undefined, undefined],
        ['publish', true, undefined],
        ['cancel', undefined, ' withdrawn '],
        ['reset', undefined, undefined],
      ] as const) {
        await result.current.marksheetStep.mutateAsync({
          marksheetId: 'm1',
          step,
          etag: 'v1',
          ...(visibleToGuardians === undefined ? {} : { visibleToGuardians }),
          ...(reason === undefined ? {} : { reason }),
        });
      }
      await result.current.generateResults.mutateAsync('template-1');
      await result.current.createHomeworkSubmission.mutateAsync({
        homeworkId: 'homework-1',
        studentId: 'student-1',
        submittedOn: '2026-03-01T00:00:00Z',
      });
      await result.current.replyToFeedback.mutateAsync({
        homeworkId: 'homework-1',
        submissionId: 'homework-sub-1',
        etag: 'v1',
        studentReply: ' Thanks for the feedback. ',
      });
      await result.current.createScheme.mutateAsync(schemeDraft);
      await result.current.updateScheme.mutateAsync({
        gradingSchemeId: 'scheme-1',
        etag: 'v1',
        gradingSchemeUpdate: { name: 'Updated term grades' } as never,
      });
      await result.current.deleteScheme.mutateAsync({ gradingSchemeId: 'scheme-1', etag: 'v1' });
      await result.current.createBand.mutateAsync(bandDraft);
      await result.current.updateBand.mutateAsync({
        gradingBandId: 'band-1',
        etag: 'v1',
        gradingBandUpdate: { label: 'A+' } as never,
      });
      await result.current.deleteBand.mutateAsync({ gradingBandId: 'band-1', etag: 'v1' });
    });

    expect(result.current.myStudents).toEqual(['student-a']);
    const versions = calls
      .filter((entry) =>
        /Assignment$|AssignmentSubmission$|Exam$|ExamSession$|Marksheet$|Changes$/.test(entry.method),
      )
      .filter((entry) => !entry.method.startsWith('create'))
      .map((entry) => JSON.stringify(entry.args[0]));
    for (const request of versions) expect(request, 'every step carries the etag').toContain('v1');
    const marks = calls.find((entry) => entry.method === 'recordExamMarks');
    expect(JSON.stringify(marks?.args[0])).toContain('"etag":"v3"');
    const publish = calls.find((entry) => entry.method === 'publishMarksheet');
    expect(publish?.args[0]).toMatchObject({ ifMatch: 'v1' });

    const methods = new Set(calls.map((entry) => entry.method));
    for (const method of [
      'createAssignmentApproval',
      'publishAssignment',
      'approveAssignment',
      'rejectAssignment',
      'finishAssignment',
      'cancelAssignment',
      'resetAssignment',
      'submitAssignmentSubmission',
      'acceptAssignmentSubmission',
      'rejectAssignmentSubmission',
      'requestSubmissionChanges',
      'resetAssignmentSubmission',
      'gradeAssignmentSubmission',
      'scheduleExamSession',
      'markExamSessionHeld',
      'completeExamSession',
      'cancelExamSession',
      'resetExamSession',
      'scheduleExam',
      'markExamHeld',
      'markExamResultsUpdated',
      'completeExam',
      'cancelExam',
      'resetExam',
      'populateExamResults',
      'allocateExamRooms',
      'recordExamMarks',
      'validateMarksheet',
      'publishMarksheet',
      'cancelMarksheet',
      'resetMarksheet',
      'generateResults',
      'createHomeworkSubmission',
      'replyToHomeworkFeedback',
      'createGradingScheme',
      'updateGradingScheme',
      'deleteGradingScheme',
      'createGradingBand',
      'updateGradingBand',
      'deleteGradingBand',
    ]) {
      expect(methods.has(method), method).toBe(true);
    }
  });
});

describe('own rows, own students and what goes stale', () => {
  it("lists only the given student's submission and takes own students from the session", async () => {
    const { api, runtime } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'student', password: 'pw' });
    const wrapper = ({ children }: { children: ReactNode }) => (
      <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
    );
    const { result } = renderHook(
      () => ({ own: useAssignmentSubmissions('a1', 'student-a'), mine: useMyStudentIds() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.own.isSuccess).toBe(true));
    const request = api.calls.find((call) => call.path === '/v1/assessment/assignment-submissions');
    expect(new URLSearchParams(request?.search).get('filter')).toBe(
      'assignmentId = "a1" AND studentId = "student-a"',
    );
    expect(result.current.mine).toEqual(['student-a']);
    expect(api.calls.some((call) => call.path.startsWith('/v1/sis/'))).toBe(false);
  });

  it('refreshes results, lines and report cards after generation and marksheet steps', async () => {
    const { api, runtime } = setup();
    await runtime.session.signIn({ tenantCode: 'demo', identifier: 'teacher', password: 'pw' });
    const wrapper = ({ children }: { children: ReactNode }) => (
      <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
    );
    const { result } = renderHook(
      () => ({
        results: useExamResults('e1'),
        lines: useMarksheetLines('m1'),
        generate: useResultTemplateGenerateResults(),
        step: useMarksheetStep(),
      }),
      { wrapper },
    );
    await waitFor(() =>
      expect(result.current.lines.isSuccess && result.current.results.isSuccess).toBe(true),
    );
    const reads = (path: string) => api.calls.filter((call) => call.path === path).length;
    const before = [reads('/v1/assessment/exam-results'), reads('/v1/assessment/marksheet-lines')];
    await act(async () => {
      await result.current.generate.mutateAsync('t1').catch(() => undefined);
    });
    await waitFor(() => {
      expect(reads('/v1/assessment/exam-results')).toBeGreaterThan(before[0] ?? 0);
      expect(reads('/v1/assessment/marksheet-lines')).toBeGreaterThan(before[1] ?? 0);
    });
    const after = reads('/v1/assessment/exam-results');
    await act(async () => {
      await result.current.step
        .mutateAsync({ marksheetId: 'm1', step: 'cancel', etag: 'v1' })
        .catch(() => undefined);
    });
    await waitFor(() => expect(reads('/v1/assessment/exam-results')).toBeGreaterThan(after));
  });
});
