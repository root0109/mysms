import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakeSis, json, schoolAdmin, sisFixture } from '@mysms/fixtures';
import { isApiError } from '../api/problem';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  isExitStatus,
  useApplyPromotions,
  useChangeStudentStatus,
  useCreatePortalAccount,
  useDisciplineIncidents,
  useGradeLevels,
  useGuardianCards,
  useIncidentStep,
  useIssueBonafideCertificate,
  useMyChildren,
  usePromotionStep,
  usePromotions,
  useProposePromotions,
  useRemoveRegistrationLine,
  useSections,
  useStudentActivities,
  useStudentEnrollments,
  useStudentHealthRecords,
  useStudentSummary,
  useSubjectRegistrationLines,
  useSubjectRegistrationStep,
  useSubjectRegistrations,
} from './sis';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const f = sisFixture;

async function setup() {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json(schoolAdmin),
    ...fakeSis(),
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  await runtime.session.signIn({ tenantCode: 'demo', identifier: 'ravi', password: 'pw' });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, wrapper };
}

const bodyOf = (api: Awaited<ReturnType<typeof setup>>['api'], suffix: string) =>
  JSON.parse(api.calls.filter((c) => c.path.endsWith(suffix)).at(-1)?.body ?? '{}') as Record<
    string,
    unknown
  >;

describe('the student 360° hooks', () => {
  it('knows which statuses are an exit', () => {
    expect(isExitStatus('withdrawn')).toBe(true);
    expect(isExitStatus('alumni')).toBe(true);
    expect(isExitStatus('inactive')).toBe(false);
  });

  it('reads the summary and changes the status under its etag, with exit details only for an exit', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ summary: useStudentSummary(f.studentId), change: useChangeStudentStatus() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.summary.data?.person.displayName).toBe('Diya Menon'));
    expect(result.current.summary.data?.openIncidents).toBe(1);

    await act(async () => {
      await result.current.change.mutateAsync({
        studentId: f.studentId,
        etag: '"1"',
        status: 'inactive',
        exitDate: '2026-04-01',
        exitReason: 'ignored',
      });
    });
    expect(bodyOf(api, ':changeStatus')).toEqual({ status: 'inactive', etag: '"1"' });
    await act(async () => {
      await result.current.change.mutateAsync({
        studentId: f.studentId,
        etag: '"2"',
        status: 'withdrawn',
        exitDate: '2026-04-01',
        exitReason: '  Moved  ',
      });
    });
    expect(bodyOf(api, ':changeStatus')).toEqual({
      status: 'withdrawn',
      etag: '"2"',
      exitDate: '2026-04-01',
      exitReason: 'Moved',
    });
    await waitFor(() => expect(result.current.summary.data?.student.status).toBe('withdrawn'));
    const stale = await result.current.change
      .mutateAsync({ studentId: f.studentId, etag: '"1"', status: 'active' })
      .catch((error: unknown) => error);
    expect(isApiError(stale) && stale.status).toBe(412);
  });

  it('issues a certificate and opens portal accounts once', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({ issue: useIssueBonafideCertificate(), account: useCreatePortalAccount() }),
      { wrapper },
    );
    let operation: { status?: string } | undefined;
    await act(async () => {
      operation = await result.current.issue.mutateAsync({ studentId: f.studentId, purpose: ' Passport ' });
    });
    expect(operation?.status).toBe('succeeded');
    expect(bodyOf(api, ':issueBonafideCertificate')).toEqual({ purpose: 'Passport' });
    await act(async () => {
      await result.current.issue.mutateAsync({ studentId: f.studentId });
    });
    expect(bodyOf(api, ':issueBonafideCertificate')).toEqual({});

    await act(async () => {
      await result.current.account.mutateAsync({ who: 'student', id: f.studentId });
      await result.current.account.mutateAsync({ who: 'guardian', id: f.guardianId });
    });
    expect(api.calls.some((c) => c.path === `/v1/sis/guardians/${f.guardianId}:createPortalAccount`)).toBe(
      true,
    );
    const again = await result.current.account
      .mutateAsync({ who: 'student', id: f.studentId })
      .catch((error: unknown) => error);
    expect(isApiError(again) && again.status).toBe(409);
  });

  it('re-reads the guardian cards once a guardian has a portal account', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({ cards: useGuardianCards([f.guardianId], true), account: useCreatePortalAccount() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.cards.data?.get(f.guardianId)).toBeDefined());
    expect(result.current.cards.data?.get(f.guardianId)?.guardian.userId).toBeUndefined();
    await act(async () => {
      await result.current.account.mutateAsync({ who: 'guardian', id: f.guardianId });
    });
    await waitFor(() => expect(result.current.cards.data?.get(f.guardianId)?.guardian.userId).toBeDefined());
  });

  it('lists what belongs to a student and the guardians with their names', async () => {
    const { wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        placements: useStudentEnrollments(f.studentId),
        health: useStudentHealthRecords(f.studentId),
        activities: useStudentActivities(f.studentId),
        incidents: useDisciplineIncidents(f.studentId),
        registrations: useSubjectRegistrations([f.studentId]),
        guardians: useGuardianCards([f.guardianId], true),
        children: useMyChildren(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.children.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.guardians.data?.get(f.guardianId)?.name).toBe('Lakshmi Menon'));
    await waitFor(() => expect(result.current.registrations.data?.items[0]?.referenceNo).toBe('SR-2026-001'));
    expect(result.current.placements.data?.items[0]?.rollNumber).toBe('7');
    expect(result.current.health.data?.items[0]?.foodAllergies).toBe('Peanuts');
    expect(result.current.activities.data?.items[0]?.description).toBe('Inter-school debate');
    expect(result.current.incidents.data?.items[0]?.id).toBe(f.incidentId);
    expect(result.current.children.data?.items[0]?.relationship).toBe('Mother');
  });

  it('takes the incident steps: action taken with what was done, then the guardian has seen it', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(() => ({ open: useDisciplineIncidents(null), step: useIncidentStep() }), {
      wrapper,
    });
    await waitFor(() => expect(result.current.open.data?.items).toHaveLength(1));
    expect(new URLSearchParams(api.calls.at(-1)?.search).get('filter')).toBe('actionTaken = false');
    await act(async () => {
      await result.current.step.mutateAsync({
        incidentId: f.incidentId,
        step: 'markActionTaken',
        etag: '"1"',
        response: ' Met the parents ',
      });
    });
    expect(bodyOf(api, ':markActionTaken')).toEqual({ etag: '"1"', response: 'Met the parents' });
    await waitFor(() => expect(result.current.open.data?.items).toHaveLength(0));
    await act(async () => {
      await result.current.step.mutateAsync({ incidentId: f.incidentId, step: 'markSeen', etag: '"2"' });
    });
    expect(bodyOf(api, ':markSeen')).toEqual({ etag: '"2"' });
  });
});

describe('the promotion wizard hooks', () => {
  it('proposes for a class, decides each proposal and applies the approved ones', async () => {
    const { api, wrapper } = await setup();
    const { result, rerender } = renderHook(
      ({ ids }: { ids: string[] }) => ({
        grades: useGradeLevels(f.institutionId),
        sections: useSections(f.institutionId),
        propose: useProposePromotions(),
        list: usePromotions(ids),
        step: usePromotionStep(),
        apply: useApplyPromotions(),
      }),
      { wrapper, initialProps: { ids: [] as string[] } },
    );
    await waitFor(() =>
      expect(result.current.grades.data?.map((g) => g.name)).toEqual(['Grade 5', 'Grade 6']),
    );
    await waitFor(() => expect(result.current.sections.data).toHaveLength(1));
    let ids: string[] = [];
    await act(async () => {
      const list = await result.current.propose.mutateAsync({
        fromClassSectionId: f.classSectionId,
        toAcademicYearId: f.nextYear,
        toGradeLevelId: f.nextGrade,
      });
      ids = list.items.map((item) => item.id ?? '');
    });
    rerender({ ids });
    await waitFor(() => expect(result.current.list.data).toHaveLength(2));
    await act(async () => {
      await result.current.step.mutateAsync({ promotionId: ids[0] ?? '', step: 'approve', etag: '"1"' });
      await result.current.step.mutateAsync({
        promotionId: ids[1] ?? '',
        step: 'reject',
        etag: '"1"',
        reason: ' Exam ',
      });
    });
    expect(bodyOf(api, ':reject')).toEqual({ etag: '"1"', reason: 'Exam' });
    const refused = await result.current.apply.mutateAsync(ids).catch((error: unknown) => error);
    expect(isApiError(refused) && refused.status).toBe(409);
    let done: { status?: string } | undefined;
    await act(async () => {
      done = await result.current.apply.mutateAsync([ids[0] ?? '']);
    });
    expect(done?.status).toBe('succeeded');
    await waitFor(() => expect(result.current.list.data?.[0]?.status).toBe('applied'));
  });
});

describe('the subject registration hooks', () => {
  it('fills a draft from the course, takes an elective off and submits it', async () => {
    const { api, wrapper } = await setup();
    const { result } = renderHook(
      () => ({
        lines: useSubjectRegistrationLines(f.registrationId),
        step: useSubjectRegistrationStep(),
        remove: useRemoveRegistrationLine(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.lines.data?.items).toHaveLength(1));
    await act(async () => {
      await result.current.step.mutateAsync({
        registrationId: f.registrationId,
        step: 'loadCourseSubjects',
        etag: null,
      });
    });
    await waitFor(() => expect(result.current.lines.data?.items).toHaveLength(2));
    const elective = result.current.lines.data?.items.find((line) => line.selectionType === 'elective');
    expect(elective?.subjectId).toBe(f.art);
    await act(async () => {
      if (elective)
        await result.current.remove.mutateAsync({ registrationId: f.registrationId, line: elective });
    });
    expect(api.calls.find((c) => c.method === 'DELETE')?.headers.get('If-Match')).toBe('"1"');
    await waitFor(() => expect(result.current.lines.data?.items).toHaveLength(1));
    for (const [step, etag] of [
      ['submit', '"1"'],
      ['reject', '"2"'],
      ['reset', '"3"'],
      ['submit', '"4"'],
      ['approve', '"5"'],
    ] as const) {
      await act(async () => {
        await result.current.step.mutateAsync({
          registrationId: f.registrationId,
          step,
          etag,
          reason: 'Check',
        });
      });
    }
    expect(bodyOf(api, ':reject')).toEqual({ etag: '"2"', reason: 'Check' });
    expect(bodyOf(api, ':approve')).toEqual({ etag: '"5"' });
  });
});
