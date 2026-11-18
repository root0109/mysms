import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, json, onePage, problem } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  useApplyTimetableTemplate,
  useBatchEnroll,
  useCohortGroups,
  useCohortOutline,
  useCohortPerformance,
  useCohortProgress,
  useCohortSchedule,
  useCohorts,
  useCohortStep,
  useCohortSyllabusLink,
  useCohortTimetable,
  useCopyCourseOutline,
  useCreateCohortGroup,
  useDeleteCohortGroup,
  useEvaluateReleaseRules,
  useExportGrades,
  useImportCohortMembers,
  usePublishFinalGrades,
  useReleaseRules,
  useReleaseRuleStep,
  useSetCohortMembers,
  useSetStudentAccess,
  useStudentAlerts,
} from './lmsCohorts';
import { useGradebook } from './lms';
import {
  useConferencingAccounts,
  useLiveClassParticipants,
  useLiveClassStartUrl,
  useLiveClassStep,
  useScheduleLiveClass,
} from './lmsLiveClasses';
import {
  useCohortForumTopics,
  useCreateForumPost,
  useFlagForumPost,
  useForumPosts,
  useForums,
  useForumTopic,
  useForumTopics,
  useMarkPostSubstantive,
  useMarkTopicRead,
  useModerateForumPost,
  useVoteForumPost,
} from './lmsForums';
import {
  useAwardBadge,
  useBadgeAwards,
  useCancelCertificateRequest,
  useCertificateEvaluations,
  useCertificateRequests,
  useCertificates,
  useCreateCertificateRequest,
  useDownloadCertificate,
  useIssueCertificateFromEvaluation,
  useMemberProfile,
  useStartEvaluation,
  useUpdateCertificateEvaluation,
  useVerifyCertificate,
} from './lmsCertification';

const id = (suffix: string) => `01970000-0000-7000-9000-${suffix.padStart(12, '0')}`;
const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const USER = { id: id('11'), username: 'meera', locale: 'en-IN' };
const cohortId = id('c1');
const topicId = id('e1');
const postId = id('p1');
const ruleId = id('r1');
const bookingId = id('b1');
const evaluationId = id('ev');

const cohort = {
  id: cohortId,
  title: 'Algebra 2026',
  lifecycleStatus: 'draft',
  isPublished: false,
  etag: '"1"',
};
const topic = { id: topicId, title: 'Introductions', isHidden: false, etag: '"1"' };
const post = { id: postId, forumTopicId: topicId, authorUserId: USER.id, body: 'Hello.', etag: '"1"' };
const operation = (result: unknown) => json({ id: id('op'), status: 'completed', result });

/** Every endpoint the remaining lms hooks call, answered with small fixed rows. */
function routes() {
  return {
    'POST /v1/iam/auth:login': () => json({ tokens, user: USER }),
    'GET /v1/iam/me': () => json({ user: USER, permissions: [] }),
    'GET /v1/lms/cohorts': (req: Request) => onePage(req, [cohort]),
    [`POST /v1/lms/cohorts/${cohortId}:setMembers`]: () =>
      json({ items: [{ userId: id('21'), displayName: 'Asha Rao', roleCode: 'student', isActive: true }] }),
    [`POST /v1/lms/cohorts/${cohortId}:batchEnroll`]: async (req: Request) =>
      req.headers.get('Idempotency-Key')
        ? json({
            items: [
              { userId: id('21'), alreadyEnrolled: false, paymentRequired: false },
              { userId: id('22'), alreadyEnrolled: true, paymentRequired: false },
            ],
          })
        : problem(400, 'VALIDATION_FAILED', 'Idempotency-Key is required.'),
    [`POST /v1/lms/cohorts/${cohortId}:importMembers`]: () => operation({ rows: 2, placed: 2, skipped: 0 }),
    [`POST /v1/lms/cohorts/${cohortId}:setStudentAccess`]: () =>
      json({ ...cohort, studentAccessFrom: '2026-01-01' }, { headers: { ETag: '"2"' } }),
    [`POST /v1/lms/cohorts/${cohortId}:publish`]: () => json({ ...cohort, isPublished: true }),
    [`POST /v1/lms/cohorts/${cohortId}:activate`]: () => json({ ...cohort, lifecycleStatus: 'active' }),
    [`POST /v1/lms/cohorts/${cohortId}:archive`]: () => json({ ...cohort, lifecycleStatus: 'archived' }),
    [`GET /v1/lms/cohorts/${cohortId}:gradebook`]: () =>
      json({ cohortId, columns: [], students: [], rows: [] }),
    [`POST /v1/lms/cohorts/${cohortId}:publishFinalGrades`]: () =>
      json({ items: [{ id: id('en'), cohortId, userId: id('21'), finalGrade: 'A' }] }),
    [`POST /v1/lms/cohorts/${cohortId}:exportGrades`]: () =>
      operation({ url: 'https://files.example/g.csv', rows: 3 }),
    [`GET /v1/lms/cohorts/${cohortId}:syllabus`]: () =>
      json({ url: 'https://files.example/s.pdf', expiresAt: '2026-11-16T10:00:00Z' }),
    [`GET /v1/lms/cohorts/${cohortId}:performance`]: (req: Request) =>
      onePage(req, [{ contentItemId: id('i1'), averagePercent: '72.50', submitted: 3, graded: 2 }]),
    [`GET /v1/lms/cohorts/${cohortId}:progress`]: (req: Request) =>
      onePage(req, [{ userId: id('21'), assessmentsCompleted: 1, assessmentsTotal: 2 }]),
    [`GET /v1/lms/cohorts/${cohortId}:timetable`]: (req: Request) =>
      onePage(req, [{ id: id('t1'), dayNumber: 1, startTime: '10:00', endTime: '11:00' }]),
    [`POST /v1/lms/cohorts/${cohortId}:applyTimetableTemplate`]: () =>
      json({ items: [{ id: id('t2'), dayNumber: 2, startTime: '09:00', endTime: '10:00' }] }),
    [`GET /v1/lms/cohorts/${cohortId}/schedule-slots`]: (req: Request) =>
      onePage(req, [{ id: id('s1'), dayOfWeek: 1, startTime: '10:00' }]),
    [`GET /v1/lms/cohorts/${cohortId}/sessions`]: (req: Request) =>
      onePage(req, [{ id: id('s2'), startsAt: '2026-11-20T10:00:00Z' }]),
    [`GET /v1/lms/cohorts/${cohortId}/holidays`]: (req: Request) =>
      onePage(req, [{ id: id('s3'), startDate: '2026-12-25' }]),
    [`POST /v1/lms/cohorts/${cohortId}:evaluateReleaseRules`]: () =>
      json({ items: [{ id: id('rg'), userId: id('21'), isGranted: true }] }),
    'GET /v1/lms/student-alerts': (req: Request) =>
      onePage(req, [{ id: id('a1'), cohortId, userId: id('21'), alertType: 'missing_work' }]),
    'GET /v1/lms/release-rules': (req: Request) =>
      onePage(req, [{ id: ruleId, cohortId, sourceContentItemId: id('i1'), isDisabled: false, etag: '"1"' }]),
    [`POST /v1/lms/release-rules/${ruleId}:enable`]: () => json({ id: ruleId, isEnabled: true, etag: '"2"' }),
    [`POST /v1/lms/release-rules/${ruleId}:disable`]: () =>
      json({ id: ruleId, isEnabled: false, etag: '"2"' }),
    'GET /v1/lms/content-items': (req: Request) =>
      onePage(req, [{ id: id('i1'), cohortId, title: 'Chapter 1', kind: 'page', sortOrder: 1 }]),
    [`POST /v1/lms/cohorts/${cohortId}:copyCourseOutline`]: () => json({ items: [] }),
    [`GET /v1/lms/cohorts/${cohortId}/groups`]: (req: Request) =>
      onePage(req, [{ id: id('g1'), cohortId, name: 'Group A', etag: '"1"' }]),
    [`POST /v1/lms/cohorts/${cohortId}/groups`]: () =>
      json({ id: id('g2'), cohortId, name: 'Group B', etag: '"1"' }, { status: 201 }),
    [`DELETE /v1/lms/cohorts/${cohortId}/groups/${id('g1')}`]: () => new Response(null, { status: 204 }),
    [`GET /v1/lms/live-classes/${id('d1')}/participants`]: (req: Request) =>
      onePage(req, [{ id: id('lp'), userId: id('21'), attended: true }]),
    'GET /v1/lms/conferencing-accounts': (req: Request) =>
      onePage(req, [{ id: id('ca'), provider: 'zoom', displayName: 'School Zoom', isEnabled: true }]),
    'POST /v1/lms/live-classes:schedule': () =>
      json(
        { id: id('d1'), cohortId, title: 'Live 1', inviteStatus: 'pending', etag: '"1"' },
        { status: 201 },
      ),
    [`GET /v1/lms/live-classes/${id('d1')}:startUrl`]: () => json({ url: 'https://meet.example/start' }),
    [`POST /v1/lms/live-classes/${id('d1')}:end`]: () => json({ id: id('d1'), status: 'ended', etag: '"2"' }),
    [`POST /v1/lms/live-classes/${id('d1')}:syncAttendance`]: () => json({ participantsMatched: 2 }),
    [`GET /v1/lms/cohorts/${cohortId}/forums`]: (req: Request) =>
      onePage(req, [{ id: id('f1'), cohortId, title: 'General', etag: '"1"' }]),
    'GET /v1/lms/forum-sections': (req: Request) =>
      onePage(req, [{ id: id('f2'), forumId: id('f1'), title: 'Week 1', etag: '"1"' }]),
    'GET /v1/lms/forum-topics': (req: Request) => onePage(req, [topic]),
    [`GET /v1/lms/forum-topics/${topicId}`]: () => json(topic, { headers: { ETag: '"1"' } }),
    [`GET /v1/lms/forum-topics/${topicId}/posts`]: (req: Request) => onePage(req, [post]),
    [`POST /v1/lms/forum-topics/${topicId}/posts`]: () =>
      json({ ...post, id: id('p2'), body: 'A reply.' }, { status: 201 }),
    [`POST /v1/lms/forum-topics/${topicId}:markRead`]: () => new Response(null, { status: 204 }),
    [`POST /v1/lms/forum-topics/${topicId}/posts/${postId}:flag`]: () => json({ ...post, isFlagged: true }),
    'POST /v1/lms/forum-post-votes': () =>
      json({ id: id('v1'), forumPostId: postId, userId: USER.id, isLike: true }, { status: 201 }),
    [`POST /v1/lms/forum-topics/${topicId}/posts/${postId}:hide`]: () => json({ ...post, isHidden: true }),
    [`POST /v1/lms/forum-topics/${topicId}/posts/${postId}:unhide`]: () => json({ ...post, isHidden: false }),
    [`POST /v1/lms/forum-topics/${topicId}/posts/${postId}:confirmAbuse`]: () =>
      json({ ...post, isHidden: true, abuseConfirmed: true }),
    [`POST /v1/lms/forum-topics/${topicId}/posts/${postId}:markSubstantive`]: () =>
      json({ ...post, isSubstantive: true }),
    'GET /v1/lms/certificates': (req: Request) =>
      onePage(req, [{ id: id('ct'), userId: USER.id, certificateNumber: 'CRT-1', etag: '"1"' }]),
    [`GET /v1/lms/certificates/${id('ct')}:download`]: () => json({ url: 'https://files.example/c.pdf' }),
    [`GET /v1/lms/certificates/${id('ct')}:verify`]: () => json({ valid: true, title: 'Algebra 2026' }),
    'GET /v1/lms/badge-awards': (req: Request) =>
      onePage(req, [{ id: id('ba'), badgeId: id('bd'), userId: USER.id, issuedOn: '2026-11-16' }]),
    [`POST /v1/lms/badges/${id('bd')}:award`]: () =>
      json({ id: id('ba'), badgeId: id('bd'), userId: USER.id, issuedOn: '2026-11-16' }, { status: 201 }),
    'GET /v1/lms/certificate-requests': (req: Request) =>
      onePage(req, [{ id: bookingId, userId: USER.id, status: 'pending', etag: '"1"' }]),
    'POST /v1/lms/certificate-requests': () =>
      json({ id: id('b2'), userId: USER.id, status: 'pending', etag: '"1"' }, { status: 201 }),
    [`POST /v1/lms/certificate-requests/${bookingId}:cancel`]: () =>
      json({ id: bookingId, status: 'cancelled', etag: '"2"' }),
    [`POST /v1/lms/certificate-requests/${bookingId}:startEvaluation`]: () =>
      json({ id: evaluationId, certificateRequestId: bookingId, status: 'in_progress', etag: '"1"' }),
    'GET /v1/lms/certificate-evaluations': (req: Request) =>
      onePage(req, [{ id: evaluationId, status: 'in_progress', etag: '"1"' }]),
    [`PATCH /v1/lms/certificate-evaluations/${evaluationId}`]: () =>
      json({ id: evaluationId, status: 'pass', etag: '"2"' }),
    [`POST /v1/lms/certificate-evaluations/${evaluationId}:issueCertificate`]: () =>
      json({ id: id('ct'), userId: USER.id, certificateNumber: 'CRT-2', etag: '"1"' }),
    [`GET /v1/lms/members/${USER.id}:profile`]: () =>
      json({ member: { userId: USER.id, displayName: 'Meera Iyer' }, certificates: [], badges: [] }),
  };
}

async function setup() {
  const api = fakeApi(routes());
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'meera', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

describe('the remaining cohort hooks', () => {
  it('lists cohorts, the timetable, the schedule parts, groups, the outline, rules and alerts', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        cohorts: useCohorts('lifecycleStatus = "active"'),
        timetable: useCohortTimetable(cohortId),
        schedule: useCohortSchedule(cohortId),
        groups: useCohortGroups(cohortId),
        outline: useCohortOutline(cohortId),
        rules: useReleaseRules(cohortId),
        alerts: useStudentAlerts(cohortId),
        performance: useCohortPerformance(cohortId),
        progress: useCohortProgress(cohortId, id('21')),
        gradebook: useGradebook(cohortId),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.cohorts.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.timetable.data?.items[0]?.dayNumber).toBe(1));
    await waitFor(() => expect(result.current.schedule.data?.holidays).toHaveLength(1));
    await waitFor(() => expect(result.current.groups.data?.items[0]?.name).toBe('Group A'));
    await waitFor(() => expect(result.current.outline.data?.items[0]?.title).toBe('Chapter 1'));
    await waitFor(() => expect(result.current.rules.data?.items[0]?.isDisabled).toBe(false));
    await waitFor(() => expect(result.current.alerts.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.performance.data?.items[0]?.graded).toBe(2));
    await waitFor(() => expect(result.current.progress.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.gradebook.data?.cohortId).toBe(cohortId));
  });

  it('walks the membership writes: set members, batch enrol, import, access window', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        set: useSetCohortMembers(),
        batch: useBatchEnroll(),
        imp: useImportCohortMembers(),
        access: useSetStudentAccess(),
      }),
      { wrapper },
    );
    act(() =>
      result.current.set.mutate({ cohortId, members: [{ userId: id('21') }], removeUserIds: [id('22')] }),
    );
    await waitFor(() => expect(result.current.set.data?.items).toHaveLength(1));
    act(() => result.current.batch.mutate({ cohortId, userIds: [id('21'), id('22')], validateOnly: true }));
    await waitFor(() => expect(result.current.batch.data?.items).toHaveLength(2));
    act(() => result.current.imp.mutate({ cohortId, fileId: id('fl'), memberKind: 'student' }));
    await waitFor(() => expect(result.current.imp.isSuccess).toBe(true));
    act(() => result.current.access.mutate({ cohortId, studentAccessFrom: '2026-01-01' }));
    await waitFor(() => expect(result.current.access.isSuccess).toBe(true));
    const sent = api.calls.find((c) => c.path.endsWith(':batchEnroll'));
    expect(sent?.headers.get('Idempotency-Key')).toBeTruthy();
  });

  it('walks the remaining lifecycle steps and the grade, outline and timetable actions', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        step: useCohortStep(),
        finals: usePublishFinalGrades(),
        exportGrades: useExportGrades(),
        syllabus: useCohortSyllabusLink(),
        template: useApplyTimetableTemplate(),
        copy: useCopyCourseOutline(),
        evaluate: useEvaluateReleaseRules(),
        ruleStep: useReleaseRuleStep(),
        createGroup: useCreateCohortGroup(),
        deleteGroup: useDeleteCohortGroup(),
      }),
      { wrapper },
    );
    for (const step of ['publish', 'activate', 'archive'] as const) {
      act(() => result.current.step.mutate({ cohortId, step, etag: '"1"' }));
      await waitFor(() => expect(result.current.step.isSuccess).toBe(true));
      act(() => result.current.step.reset());
    }
    act(() => result.current.finals.mutate({ cohortId, userIds: [id('21')] }));
    await waitFor(() => expect(result.current.finals.data?.items).toHaveLength(1));
    act(() => result.current.exportGrades.mutate({ cohortId }));
    await waitFor(() => expect(result.current.exportGrades.isSuccess).toBe(true));
    act(() => result.current.syllabus.mutate({ cohortId }));
    await waitFor(() => expect(result.current.syllabus.data?.url).toContain('s.pdf'));
    act(() => result.current.template.mutate({ cohortId, timetableTemplateId: id('tt') }));
    await waitFor(() => expect(result.current.template.isSuccess).toBe(true));
    act(() => result.current.copy.mutate({ cohortId, courseId: id('co'), shiftDatesFrom: '2026-01-10' }));
    await waitFor(() => expect(result.current.copy.isSuccess).toBe(true));
    act(() => result.current.evaluate.mutate({ cohortId, userId: id('21') }));
    await waitFor(() => expect(result.current.evaluate.isSuccess).toBe(true));
    act(() =>
      result.current.ruleStep.mutate({ cohortId, releaseRuleId: ruleId, step: 'enable', etag: '"1"' }),
    );
    await waitFor(() => expect(result.current.ruleStep.isSuccess).toBe(true));
    act(() => result.current.ruleStep.reset());
    act(() =>
      result.current.ruleStep.mutate({ cohortId, releaseRuleId: ruleId, step: 'disable', etag: '"1"' }),
    );
    await waitFor(() => expect(result.current.ruleStep.isSuccess).toBe(true));
    act(() => result.current.createGroup.mutate({ cohortId, name: 'Group B', description: 'Lab bench two' }));
    await waitFor(() => expect(result.current.createGroup.isSuccess).toBe(true));
    act(() => result.current.deleteGroup.mutate({ cohortId, cohortGroupId: id('g1'), etag: '"1"' }));
    await waitFor(() => expect(result.current.deleteGroup.isSuccess).toBe(true));
  });
});

describe('the remaining live-class hooks', () => {
  it('lists participants and accounts, schedules a class, and walks start, end and attendance', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        participants: useLiveClassParticipants(id('d1')),
        accounts: useConferencingAccounts(),
        schedule: useScheduleLiveClass(),
        start: useLiveClassStartUrl(),
        step: useLiveClassStep(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.participants.data?.items[0]?.attended).toBe(true));
    await waitFor(() => expect(result.current.accounts.data?.items[0]?.provider).toBe('zoom'));
    act(() =>
      result.current.schedule.mutate({
        request: {
          cohortId,
          conferencingAccountId: id('ca'),
          title: 'Live 1',
          classDate: '2026-11-20',
          startTime: '10:00',
          durationMinutes: 45,
          timezone: 'Asia/Kolkata',
        },
      }),
    );
    await waitFor(() => expect(result.current.schedule.isSuccess).toBe(true));
    act(() => result.current.start.mutate({ liveClassId: id('d1') }));
    await waitFor(() => expect(result.current.start.data?.url).toContain('start'));
    for (const step of ['end', 'syncAttendance'] as const) {
      act(() => result.current.step.mutate({ liveClassId: id('d1'), step }));
      await waitFor(() => expect(result.current.step.isSuccess).toBe(true));
      act(() => result.current.step.reset());
    }
  });
});

describe('the remaining forum hooks', () => {
  it('reads the forums, the topic chain and the thread, and walks every member and moderator write', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        forums: useForums(cohortId),
        chain: useCohortForumTopics(cohortId),
        topics: useForumTopics(`forumSectionId = "${id('f2')}"`),
        topic: useForumTopic(topicId),
        posts: useForumPosts(topicId),
        reply: useCreateForumPost(),
        markRead: useMarkTopicRead(),
        flag: useFlagForumPost(),
        vote: useVoteForumPost(),
        moderate: useModerateForumPost(),
        substantive: useMarkPostSubstantive(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.forums.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.chain.data?.topics).toHaveLength(1));
    await waitFor(() => expect(result.current.topics.data?.items[0]?.title).toBe('Introductions'));
    await waitFor(() => expect(result.current.topic.data?.etag).toBe('"1"'));
    await waitFor(() => expect(result.current.posts.data?.items).toHaveLength(1));
    act(() => result.current.reply.mutate({ topicId, body: 'A reply.', parentPostId: postId }));
    await waitFor(() => expect(result.current.reply.isSuccess).toBe(true));
    act(() => result.current.markRead.mutate({ topicId, postIds: [postId] }));
    await waitFor(() => expect(result.current.markRead.isSuccess).toBe(true));
    act(() => result.current.flag.mutate({ topicId, postId, flagged: true }));
    await waitFor(() => expect(result.current.flag.isSuccess).toBe(true));
    act(() => result.current.vote.mutate({ topicId, postId }));
    await waitFor(() => expect(result.current.vote.isSuccess).toBe(true));
    for (const step of ['hide', 'unhide', 'confirmAbuse'] as const) {
      act(() => result.current.moderate.mutate({ topicId, postId, step, etag: '"1"', reason: 'Spam.' }));
      await waitFor(() => expect(result.current.moderate.isSuccess).toBe(true));
      act(() => result.current.moderate.reset());
    }
    act(() => result.current.substantive.mutate({ topicId, postId, substantive: true }));
    await waitFor(() => expect(result.current.substantive.isSuccess).toBe(true));
  });
});

describe('the remaining certification hooks', () => {
  it('reads certificates, awards, bookings and evaluations, and walks the whole evaluation path', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        certificates: useCertificates(USER.id),
        download: useDownloadCertificate(),
        verify: useVerifyCertificate(id('ct')),
        awards: useBadgeAwards(USER.id),
        award: useAwardBadge(),
        bookings: useCertificateRequests(`userId = "${USER.id}"`),
        book: useCreateCertificateRequest(),
        cancel: useCancelCertificateRequest(),
        start: useStartEvaluation(),
        evaluations: useCertificateEvaluations(`evaluatorUserId = "${USER.id}"`),
        update: useUpdateCertificateEvaluation(),
        issue: useIssueCertificateFromEvaluation(),
        profile: useMemberProfile(USER.id),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.certificates.data?.items[0]?.certificateNumber).toBe('CRT-1'));
    await waitFor(() => expect(result.current.verify.data?.valid).toBe(true));
    await waitFor(() => expect(result.current.awards.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.bookings.data?.items[0]?.status).toBe('pending'));
    await waitFor(() => expect(result.current.evaluations.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.profile.data?.member?.displayName).toBe('Meera Iyer'));
    act(() => result.current.download.mutate({ certificateId: id('ct') }));
    await waitFor(() => expect(result.current.download.data?.url).toContain('c.pdf'));
    act(() => result.current.award.mutate({ badgeId: id('bd'), userId: USER.id }));
    await waitFor(() => expect(result.current.award.isSuccess).toBe(true));
    act(() =>
      result.current.book.mutate({
        booking: { userId: USER.id, requestDate: '2026-12-01' } as Parameters<
          typeof result.current.book.mutate
        >[0]['booking'],
      }),
    );
    await waitFor(() => expect(result.current.book.isSuccess).toBe(true));
    act(() => result.current.cancel.mutate({ bookingId, etag: '"1"', reason: 'Clash.' }));
    await waitFor(() => expect(result.current.cancel.data?.status).toBe('cancelled'));
    act(() => result.current.start.mutate({ bookingId }));
    await waitFor(() => expect(result.current.start.isSuccess).toBe(true));
    act(() =>
      result.current.update.mutate({ evaluationId, etag: '"1"', changes: { status: 'pass' } as never }),
    );
    await waitFor(() => expect(result.current.update.data?.status).toBe('pass'));
    act(() =>
      result.current.issue.mutate({ evaluationId, issueDate: '2026-12-02', templateCode: 'default' }),
    );
    await waitFor(() => expect(result.current.issue.data?.certificateNumber).toBe('CRT-2'));
  });
});
