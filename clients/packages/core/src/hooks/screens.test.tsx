import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import {
  academicsFixture,
  fakeAcademics,
  fakeApi,
  fakeOrg,
  json,
  orgFixture,
  schoolAdmin,
  type Handler,
} from '@mysms/fixtures';
import { useClassSubjects } from './classWorkspace';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import { fieldIn } from '../records/api';
import { resourceDef, useAllRecords, useRecordTotal } from '../records/hooks';
import { useActivateAcademicYear, useGenerateTerms } from './org';
import { usePermissionCatalogue } from './roles';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };

/** Signs the fixture administrator in against the given fakes and wraps hooks in the provider. */
async function setup(routes: Record<string, Handler>) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: schoolAdmin.user }),
    'GET /v1/iam/me': () => json(schoolAdmin),
    ...routes,
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

describe('generic record reads for detail pages and dashboards', () => {
  it('names record types, reads a nested type whole and stays off without its parent', async () => {
    expect(resourceDef('org.Institution')?.ops.List?.path).toBe('/v1/org/institutions');
    expect(resourceDef('org.Nothing')).toBeUndefined();
    const { api, wrapper } = await setup(fakeOrg());
    const terms = renderHook(() => useAllRecords('org.AcademicTerm', { parentId: orgFixture.closedYearId }), {
      wrapper,
    });
    await waitFor(() => expect(terms.result.current.data?.items.map((t) => t.name)).toEqual(['Whole year']));
    const orphan = renderHook(() => useAllRecords('org.AcademicTerm'), { wrapper });
    expect(orphan.result.current.fetchStatus).toBe('idle');
    expect(api.calls.filter((c) => c.path.endsWith('/terms'))).toHaveLength(1);
  });

  it('builds an OR filter of values and none for no values', () => {
    expect(fieldIn('personId', ['a', 'b"c'])).toBe('personId = "a" OR personId = "bc"');
    expect(fieldIn('personId', [])).toBeUndefined();
  });

  it('counts rows up to a cap and says when there are more', async () => {
    const { api, wrapper } = await setup(fakeOrg());
    const some = renderHook(() => useRecordTotal('hr.LeaveApplication', 'status = "pending"'), { wrapper });
    await waitFor(() => expect(some.result.current.data).toEqual({ count: 1, more: false }));
    const capped = renderHook(() => useRecordTotal('admission.Application', undefined, true, 2), { wrapper });
    await waitFor(() => expect(capped.result.current.data).toEqual({ count: 2, more: true }));
    expect(api.calls.find((c) => c.path === '/v1/admission/applications')?.search).toBe('?size=2');
  });
});

describe('academic year steps and the permission catalogue', () => {
  it('activates a year under its etag, generates its terms and reads the catalogue', async () => {
    const { api, wrapper } = await setup(fakeOrg());
    const steps = renderHook(() => ({ activate: useActivateAcademicYear(), generate: useGenerateTerms() }), {
      wrapper,
    });
    await act(() =>
      steps.result.current.activate.mutateAsync({ academicYearId: orgFixture.plannedYearId, etag: '"1"' }),
    );
    await expect(
      steps.result.current.activate.mutateAsync({ academicYearId: orgFixture.plannedYearId, etag: null }),
    ).rejects.toMatchObject({ status: 412 });
    const made = await act(() =>
      steps.result.current.generate.mutateAsync({ academicYearId: orgFixture.plannedYearId }),
    );
    expect(made.items.map((term) => term.name)).toEqual(['Semester 1', 'Semester 2']);
    expect(api.calls.filter((c) => c.method === 'POST').map((c) => c.path)).toContain(
      `/v1/org/academic-years/${orgFixture.plannedYearId}:generateTerms`,
    );
    const catalogue = renderHook(() => usePermissionCatalogue(), { wrapper });
    await waitFor(() => expect(catalogue.result.current.data).toHaveLength(3));
    expect(
      new URLSearchParams(api.calls.find((c) => c.path === '/v1/iam/permissions')?.search).get('sort'),
    ).toBe('code');
  });
});

describe('a class section’s subjects', () => {
  it('reads the subjects with their active teachers, or the subjects alone', async () => {
    const { api, wrapper } = await setup(fakeAcademics());
    const lines = renderHook(() => useClassSubjects(academicsFixture.classSectionId, true), { wrapper });
    await waitFor(() => expect(lines.result.current.data).toHaveLength(2));
    const maths = lines.result.current.data?.find((line) => line.id === academicsFixture.mathsClassSubjectId);
    expect(maths?.teachers).toEqual([{ staffId: academicsFixture.teacherId, periodsPerWeek: 6 }]);
    const bare = renderHook(() => useClassSubjects(academicsFixture.classSectionId, false), { wrapper });
    await waitFor(() =>
      expect(bare.result.current.data?.every((line) => line.teachers.length === 0)).toBe(true),
    );
    expect(api.calls.filter((c) => c.path === '/v1/academics/teaching-assignments')).toHaveLength(1);
  });
});
