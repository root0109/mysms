import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { fakeApi, fakePlatform, json, platformFixture, problem, type Handler } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import {
  auditFilter,
  themeTokens,
  uploadFile,
  UploadTransferError,
  useAuditLogs,
  useBranding,
  useCancelSubscription,
  useClearDemoData,
  useConfirmSubscriptionCheckout,
  useDeleteTheme,
  useDownloadUrl,
  useFileObject,
  useGlobalSearch,
  useReportDefinitions,
  useRunReport,
  useSaasPlans,
  useSaveTheme,
  useStartSubscriptionCheckout,
  useTenantSubscriptions,
  useThemes,
  useUploadFile,
} from './platform';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const ME = { user: { id: 'u1', username: 'ravi', locale: 'en-IN' }, permissions: [] };
const f = platformFixture;
const TENANT_ID = '01960000-0000-7000-8000-000000000b99';

function setup(extra: Record<string, Handler> = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: ME.user }),
    'GET /v1/iam/me': () => json(ME),
    'GET /v1/iam/me/settings': () => json({ items: [] }),
    ...fakePlatform(),
    'DELETE /v1/platform/themes/{themeId}': (req) =>
      req.headers.get('If-Match') === '"1"'
        ? new Response(null, { status: 204 })
        : problem(412, 'ETAG_MISMATCH'),
    'GET /v1/platform/files/{fileObjectId}': (_req, { fileObjectId }) =>
      json({ id: fileObjectId, fileName: 'logo.png', status: 'available' }),
    'POST /v1/platform/tenant-subscriptions:checkout': () =>
      json({ subscription: { id: f.subscriptionId, status: 'pending' }, gatewayOrderId: 'order_1' }),
    'POST /v1/platform/tenant-subscriptions/{tenantSubscriptionId}:confirmCheckout': (_req, p) =>
      json({ id: p.tenantSubscriptionId, status: 'active' }),
    'POST /v1/platform/tenants/{tenantId}:clearDemoData': (_req, p) =>
      json({ id: f.operationId, status: 'succeeded', resourceName: p.tenantId }),
    ...extra,
  });
  const runtime = createRuntime({
    baseUrl: 'https://school.example.com',
    clientId: 'test/0/jsdom',
    fetch: api.fetch,
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MySmsProvider runtime={runtime}>{children}</MySmsProvider>
  );
  return { api, runtime, wrapper };
}

const signedIn = async (extra: Record<string, Handler> = {}) => {
  const env = setup(extra);
  await env.runtime.session.signIn({ tenantCode: 'demo', identifier: 'ravi', password: 'pw' });
  return env;
};

const picked = (bytes: string, name = 'logo.png', type = 'image/png') => {
  const data = new TextEncoder().encode(bytes);
  return { name, type, size: data.byteLength, arrayBuffer: async () => data.buffer as ArrayBuffer };
};

afterEach(() => vi.restoreAllMocks());

describe('files', () => {
  it('uploads through a signed URL without the MySMS token and completes the file', async () => {
    const store = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    const { api, runtime } = await signedIn();
    const file = await uploadFile(runtime.api, picked('logo'), { collectionName: 'logo' });
    expect(file).toMatchObject({ id: f.fileId, status: 'available' });
    const [url, init] = store.mock.calls[0] ?? [];
    expect(String(url)).toContain('/upload/');
    expect(init).toMatchObject({ method: 'PUT', headers: { 'Content-Type': 'image/png' } });
    const create = api.calls.find((c) => c.path === '/v1/platform/files:createUploadUrl');
    expect(JSON.parse(create?.body ?? '{}')).toMatchObject({
      fileName: 'logo.png',
      mimeType: 'image/png',
      sizeBytes: '4',
      collectionName: 'logo',
    });
  });

  it('reports a refused transfer so the user can upload again, and reads a file through FileReader', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 403 }));
    const { runtime } = await signedIn();
    const blob = new Blob(['x']);
    const source = Object.assign(blob, { name: 'a.bin', arrayBuffer: undefined }) as unknown as Parameters<
      typeof uploadFile
    >[1];
    const refused = uploadFile(runtime.api, source);
    await expect(refused).rejects.toBeInstanceOf(UploadTransferError);
    await expect(refused).rejects.toMatchObject({ status: 403 });
  });

  it('offers the upload, download and file hooks', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }));
    const { wrapper } = await signedIn();
    const { result } = renderHook(
      () => ({ upload: useUploadFile(), link: useDownloadUrl(), file: useFileObject(f.fileId) }),
      { wrapper },
    );
    await act(async () => {
      await result.current.upload.mutateAsync({ source: picked('logo') });
    });
    let link: { url?: string } | undefined;
    await act(async () => {
      link = await result.current.link.mutateAsync(f.fileId);
    });
    expect(link?.url).toContain(f.fileId);
    await waitFor(() => expect(result.current.file.data?.fileName).toBe('logo.png'));
  });
});

describe('themes and branding', () => {
  it('lists, creates, updates under the etag and deletes themes', async () => {
    const { api, wrapper } = await signedIn();
    const { result } = renderHook(
      () => ({ themes: useThemes(), save: useSaveTheme(), remove: useDeleteTheme() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.themes.data?.items).toHaveLength(1));
    const classic = result.current.themes.data?.items[0];
    expect(themeTokens(classic)).toEqual({ 'color.primary': '#0F766E' });
    expect(themeTokens(undefined)).toEqual({});
    const draft = { name: ' Night ', description: '', logoFileId: null, isDefault: false, tokens: {} };
    await act(async () => {
      await result.current.save.mutateAsync(draft);
    });
    expect(
      JSON.parse(
        api.calls.find((c) => c.method === 'POST' && c.path === '/v1/platform/themes')?.body ?? '{}',
      ),
    ).toEqual({ name: 'Night', isDefault: false, tokens: {} });
    await act(async () => {
      await result.current.save.mutateAsync({
        ...draft,
        id: f.themeId,
        etag: '"1"',
        description: 'Blue',
        logoFileId: f.fileId,
      });
    });
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    await expect(result.current.save.mutateAsync({ ...draft, id: f.themeId })).rejects.toThrow('etag');
    await act(async () => {
      await result.current.remove.mutateAsync({ id: f.themeId, etag: '"1"' });
    });
    expect(api.calls.some((c) => c.method === 'DELETE')).toBe(true);
  });

  it('reads the public branding only for a school code', async () => {
    const { api, wrapper } = setup();
    const { result, rerender } = renderHook(({ code }) => useBranding(code), {
      wrapper,
      initialProps: { code: '  ' },
    });
    expect(result.current.fetchStatus).toBe('idle');
    rerender({ code: ' demo ' });
    await waitFor(() => expect(result.current.data?.tenantName).toBe('Sunrise Schools'));
    expect(api.calls.find((c) => c.path === '/v1/platform/branding')?.search).toContain('tenantCode=demo');
  });
});

describe('audit trail', () => {
  it('builds the range filter first and reads one page', async () => {
    expect(
      auditFilter({ from: '2026-11-01', to: '2026-11-30', subjectType: ' sis.student ', subjectId: ' s1 ' }),
    ).toBe(
      'occurredAt >= "2026-11-01T00:00:00Z" AND occurredAt < "2026-12-01T00:00:00Z" AND subjectType = "sis.student" AND subjectId = "s1"',
    );
    expect(auditFilter({ from: '2026-12-31', to: '2026-12-31', subjectType: ' ' })).toBe(
      'occurredAt >= "2026-12-31T00:00:00Z" AND occurredAt < "2027-01-01T00:00:00Z"',
    );
    const { wrapper } = await signedIn();
    const { result, rerender } = renderHook(({ query }) => useAuditLogs(query), {
      wrapper,
      initialProps: { query: null as Parameters<typeof useAuditLogs>[0] },
    });
    expect(result.current.fetchStatus).toBe('idle');
    rerender({ query: { from: '2026-11-01', to: '2026-11-30', page: { page: 1 } } });
    await waitFor(() => expect(result.current.data?.data).toHaveLength(1));
  });
});

describe('subscription', () => {
  it('lists plans and subscriptions, checks out, confirms and cancels', async () => {
    const { api, wrapper } = await signedIn();
    const { result } = renderHook(
      () => ({
        plans: useSaasPlans(),
        subscriptions: useTenantSubscriptions(),
        start: useStartSubscriptionCheckout(),
        confirm: useConfirmSubscriptionCheckout(),
        cancel: useCancelSubscription(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.plans.data?.items).toHaveLength(1));
    await waitFor(() => expect(result.current.subscriptions.data?.items).toHaveLength(1));
    await act(async () => {
      await result.current.start.mutateAsync({ planId: f.planId, institutionId: 'i1' });
      await result.current.start.mutateAsync({ planId: f.planId });
    });
    const checkout = api.calls.find((c) => c.path === '/v1/platform/tenant-subscriptions:checkout');
    expect(checkout?.headers.get('Idempotency-Key')).toBeTruthy();
    expect(JSON.parse(checkout?.body ?? '{}')).toEqual({ planId: f.planId, institutionId: 'i1' });
    const proof = { gatewayOrderId: 'order_1', gatewayPaymentId: 'pay_1', gatewaySignature: 'sig' };
    let confirmed: { status?: string } | undefined;
    await act(async () => {
      confirmed = await result.current.confirm.mutateAsync({ subscriptionId: f.subscriptionId, proof });
    });
    expect(confirmed?.status).toBe('active');
    await act(async () => {
      await result.current.cancel.mutateAsync({ id: f.subscriptionId, etag: '"1"', reason: ' Moving ' });
    });
    const cancel = api.calls.find((c) => c.path.endsWith(':cancel'));
    expect(JSON.parse(cancel?.body ?? '{}')).toEqual({ etag: '"1"', reason: 'Moving' });
    await expect(result.current.cancel.mutateAsync({ id: f.subscriptionId })).rejects.toMatchObject({
      status: 409,
    });
  });
});

describe('search, reports and demo data', () => {
  it('searches from two characters, runs a report and clears demo data', async () => {
    const { api, wrapper } = await signedIn();
    const { result, rerender } = renderHook(
      ({ text }) => ({
        search: useGlobalSearch(text, 1),
        reports: useReportDefinitions(),
        run: useRunReport(),
        clear: useClearDemoData(),
      }),
      { wrapper, initialProps: { text: 'a' } },
    );
    expect(result.current.search.fetchStatus).toBe('idle');
    rerender({ text: ' annual ' });
    await waitFor(() => expect(result.current.search.data?.data).toHaveLength(2));
    await waitFor(() => expect(result.current.reports.data?.items.length).toBeGreaterThan(0));
    await act(async () => {
      await result.current.run.mutateAsync({ reportCode: 'platform.files', parameters: {} });
    });
    expect(
      JSON.parse(api.calls.find((c) => c.path === '/v1/platform/reports:run')?.body ?? '{}'),
    ).toMatchObject({
      reportCode: 'platform.files',
      format: 'csv',
    });
    await act(async () => {
      await result.current.clear.mutateAsync(TENANT_ID);
    });
    expect(api.calls.some((c) => c.path === `/v1/platform/tenants/${TENANT_ID}:clearDemoData`)).toBe(true);
  });
});
