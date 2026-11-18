import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, fakePlatform, json, platformFixture } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import { IMPORT_TYPES, importResult, useImportTemplate, useStartImport } from './imports';
import { useOperation } from './operations';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const ME = { user: { id: 'u1', username: 'ravi', locale: 'en-IN' }, permissions: [] };
const f = platformFixture;

async function signedIn() {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: ME.user }),
    'GET /v1/iam/me': () => json(ME),
    'GET /v1/iam/me/settings': () => json({ items: [] }),
    ...fakePlatform(),
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

describe('importResult', () => {
  it('reads an import operation result and ignores anything else', () => {
    expect(
      importResult({
        result: {
          entityType: 'staff',
          institutionId: f.institutionId,
          dryRun: true,
          rowCount: 4,
          createdCount: 0,
          errorCount: 2,
          errors: [
            { rowNumber: 3, field: 'employeeCode', message: 'at most 30 characters' },
            { rowNumber: 5 },
            7,
          ],
        },
      }),
    ).toEqual({
      entityType: 'staff',
      institutionId: f.institutionId,
      dryRun: true,
      rowCount: 4,
      createdCount: 0,
      errorCount: 2,
      errors: [
        { rowNumber: 3, field: 'employeeCode', message: 'at most 30 characters' },
        { rowNumber: 5, message: '' },
      ],
    });
    expect(importResult({ result: { entityType: 'holidays', rowCount: -1 } })).toMatchObject({
      dryRun: false,
      rowCount: 0,
      errors: [],
    });
    expect(importResult({ result: 'file' })).toBeNull();
    expect(importResult({ result: [] })).toBeNull();
    expect(importResult({})).toBeNull();
    expect(importResult(null)).toBeNull();
  });

  it('offers every CSV type of the contract, never the course package', () => {
    expect(IMPORT_TYPES.map((type) => type.entityType)).toEqual([
      'students',
      'guardians',
      'staff',
      'library_media',
      'holidays',
      'promotions',
    ]);
    expect(IMPORT_TYPES.find((type) => type.entityType === 'guardians')?.scope).toBe('tenant');
  });
});

describe('import hooks', () => {
  it('signs a template link and starts an import under a fresh Idempotency-Key, then follows it', async () => {
    const { api, wrapper } = await signedIn();
    const { result } = renderHook(() => ({ template: useImportTemplate(), start: useStartImport() }), {
      wrapper,
    });
    let link: { url: string } | undefined;
    await act(async () => {
      link = await result.current.template.mutateAsync('students');
    });
    expect(link?.url).toContain('import-template-students.csv');

    let started: Awaited<ReturnType<typeof result.current.start.mutateAsync>> | undefined;
    await act(async () => {
      started = await result.current.start.mutateAsync({
        entityType: 'students',
        fileId: f.fileId,
        institutionId: f.institutionId,
        dryRun: false,
      });
    });
    expect(started).toMatchObject({ status: 'running', kind: 'platform.StartImport' });
    const call = api.calls.find((c) => c.method === 'POST' && c.path === '/v1/platform/imports');
    expect(call?.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    expect(JSON.parse(call?.body ?? '{}')).toEqual({
      entityType: 'students',
      fileId: f.fileId,
      institutionId: f.institutionId,
      dryRun: false,
    });

    const followed = renderHook(() => useOperation(started ?? null), { wrapper });
    await waitFor(() => expect(followed.result.current.data?.status).toBe('succeeded'), { timeout: 4000 });
    expect(importResult(followed.result.current.data)?.createdCount).toBe(2);

    await act(async () => {
      await result.current.start.mutateAsync({ entityType: 'guardians', fileId: f.fileId, dryRun: true });
    });
    const guardians = api.calls.filter((c) => c.path === '/v1/platform/imports').at(-1);
    expect(JSON.parse(guardians?.body ?? '{}')).not.toHaveProperty('institutionId');
    expect(guardians?.headers.get('Idempotency-Key')).not.toBe(call?.headers.get('Idempotency-Key'));
  });

  it('surfaces a refusal', async () => {
    const { wrapper } = await signedIn();
    const { result } = renderHook(() => ({ template: useImportTemplate(), start: useStartImport() }), {
      wrapper,
    });
    await act(async () => {
      await expect(
        result.current.start.mutateAsync({ entityType: 'students', fileId: f.fileId, dryRun: true }),
      ).rejects.toMatchObject({ status: 422 });
    });
  });
});
