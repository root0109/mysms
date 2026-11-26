import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { iam } from '@mysms/sdk';
import { call } from '../api/unwrap';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MySmsProvider } from '../react/context';
import { useRuntime } from '../react/useRuntime';
import { createRuntime, type Runtime } from '../runtime';
import { fakeApi, json, onePage, pageOf, type Handler } from '@mysms/fixtures';
import { useCompleteMfa, useSignIn, useSignOut } from './auth';
import {
  useCountries,
  useCountry,
  useCreateCountry,
  useDeleteCountry,
  useInfiniteCountries,
  useUpdateCountry,
} from './master';
import { useCancelOperation, useJobCentre, useOperation } from './operations';
import { useI18n } from './i18n';
import { useCan, useSession } from './session';

const ME = {
  user: { id: 'u1', username: 'asha' },
  permissions: ['master.country.read', 'master.country.update'],
};
const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const COUNTRY = { id: 'c1', iso2: 'IN', iso3: 'IND', name: 'India' };

function setup(extra: Record<string, Handler> = {}) {
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: ME.user }),
    'POST /v1/iam/mfa:complete': () => json({ tokens, user: ME.user }),
    'POST /v1/iam/auth:logout': () => new Response(null, { status: 204 }),
    'GET /v1/iam/me': () => json(ME),
    'GET /v1/master/countries': (req) => onePage(req, [COUNTRY]),
    'GET /v1/master/countries/{countryId}': () => json(COUNTRY, { headers: { ETag: '"4"' } }),
    'POST /v1/master/countries': () => json(COUNTRY, { status: 201, headers: { ETag: '"1"' } }),
    'PATCH /v1/master/countries/{countryId}': () =>
      json({ ...COUNTRY, name: 'Bharat' }, { headers: { ETag: '"5"' } }),
    'DELETE /v1/master/countries/{countryId}': () => new Response(null, { status: 204 }),
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

const signIn = (runtime: Runtime) =>
  runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });

afterEach(() => vi.unstubAllGlobals());

describe('runtime and session hooks', () => {
  it('requires the provider', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => renderHook(() => useRuntime())).toThrow('inside <MySmsProvider>');
  });

  it('signs in, exposes the session and permissions, signs out and clears cached data', async () => {
    const { runtime, wrapper } = setup();
    const { result } = renderHook(
      () => ({
        signIn: useSignIn(),
        signOut: useSignOut(),
        status: useSession((s) => s.status),
        canEdit: useCan('master.country.update'),
        canDelete: useCan('master.country.delete'),
      }),
      { wrapper },
    );
    expect(result.current.status).toBe('signedOut');
    await act(() =>
      result.current.signIn.mutateAsync({ tenantCode: 'demo', identifier: 'asha', password: 'pw' }),
    );
    expect(result.current.status).toBe('signedIn');
    expect(result.current.canEdit).toBe(true);
    expect(result.current.canDelete).toBe(false);
    runtime.queryClient.setQueryData(['master', 'countries'], { data: [COUNTRY] });
    await act(() => result.current.signOut.mutateAsync());
    expect(result.current.status).toBe('signedOut');
    expect(runtime.queryClient.getQueryData(['master', 'countries'])).toBeUndefined();
  });

  it('completes an MFA challenge', async () => {
    const { wrapper } = setup({
      'POST /v1/iam/auth:login': () => json({ mfaRequired: true, mfaToken: 'm1' }),
    });
    const { result } = renderHook(
      () => ({ signIn: useSignIn(), mfa: useCompleteMfa(), status: useSession((s) => s.status) }),
      { wrapper },
    );
    await act(async () => {
      await expect(
        result.current.signIn.mutateAsync({ tenantCode: 'demo', identifier: 'asha', password: 'pw' }),
      ).resolves.toBe('mfaRequired');
    });
    expect(result.current.status).toBe('mfaPending');
    await act(() => result.current.mfa.mutateAsync('123456'));
    expect(result.current.status).toBe('signedIn');
  });

  it('uses the global fetch by default', async () => {
    const fetchSpy = vi.fn(async () => json(ME));
    vi.stubGlobal('fetch', fetchSpy);
    const runtime = createRuntime({ baseUrl: 'https://h', clientId: 'c' });
    await expect(call(runtime.api.get(iam.UserAccountApi).getMe())).resolves.toMatchObject(ME);
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });
});

describe('master data hooks (PLT-07)', () => {
  it('lists, reads with ETag, creates, updates with If-Match and deletes', async () => {
    const { api, runtime, wrapper } = setup();
    await signIn(runtime);
    const { result } = renderHook(
      () => ({
        list: useCountries({ size: 25 }),
        one: useCountry('c1'),
        none: useCountry(null),
        create: useCreateCountry(),
        update: useUpdateCountry(),
        remove: useDeleteCountry(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.list.data?.data).toEqual([COUNTRY]));
    await waitFor(() => expect(result.current.one.data).toEqual({ data: COUNTRY, etag: '"4"' }));
    expect(result.current.none.fetchStatus).toBe('idle');

    await act(() => result.current.create.mutateAsync({ iso2: 'IN', name: 'India' }));
    const updated = await act(() =>
      result.current.update.mutateAsync({ countryId: 'c1', etag: '"4"', changes: { name: 'Bharat' } }),
    );
    expect(updated).toEqual({ data: { ...COUNTRY, name: 'Bharat' }, etag: '"5"' });
    await act(() => result.current.remove.mutateAsync({ countryId: 'c1', etag: '"5"' }));

    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"4"');
    expect(patch?.body).toBe(JSON.stringify({ name: 'Bharat' }));
    const del = api.calls.find((c) => c.method === 'DELETE');
    expect(del?.headers.get('If-Match')).toBe('"5"');
    expect(
      api.calls
        .find((c) => c.method === 'GET' && c.path === '/v1/master/countries')
        ?.headers.get('Authorization'),
    ).toBe('Bearer a1');
  });

  it('pages through the next links, sending the page and its cursor', async () => {
    const { api, runtime, wrapper } = setup({
      'GET /v1/master/countries': (req) => pageOf(req, [COUNTRY, { ...COUNTRY, id: 'c2' }]),
    });
    await signIn(runtime);
    const { result } = renderHook(() => useInfiniteCountries({ size: 1 }), { wrapper });
    await waitFor(() => expect(result.current.data?.pages).toHaveLength(1));
    expect(result.current.hasNextPage).toBe(true);
    await act(() => result.current.fetchNextPage());
    await waitFor(() =>
      expect(result.current.data?.pages.flatMap((p) => p.data).map((c) => c.id)).toEqual(['c1', 'c2']),
    );
    expect(result.current.hasNextPage).toBe(false);
    const reads = api.calls.filter((c) => c.path === '/v1/master/countries');
    expect(reads).toHaveLength(2);
    expect(new URLSearchParams(reads[1]?.search).get('page')).toBe('2');
    expect(new URLSearchParams(reads[1]?.search).get('cursor')).toBe('2:1');
  });

  it('sends only the changed fields, null to clear, and no update mask', async () => {
    let query = '';
    let body: unknown;
    const { runtime, wrapper } = setup({
      'PATCH /v1/master/countries/{countryId}': async (req) => {
        query = new URL(req.url).search;
        body = await req.json();
        return json(COUNTRY, { headers: { ETag: '"9"' } });
      },
    });
    await signIn(runtime);
    const { result } = renderHook(() => useUpdateCountry(), { wrapper });
    await act(() =>
      result.current.mutateAsync({
        countryId: 'c1',
        etag: '"8"',
        changes: { name: 'X', shortName: undefined },
      }),
    );
    expect(query).toBe('');
    expect(body).toEqual({ name: 'X', shortName: null });
  });
});

describe('operation hooks', () => {
  it('polls the owning module until the operation finishes', async () => {
    let polls = 0;
    const { api, runtime, wrapper } = setup({
      'GET /v1/finance/operations/{operationId}': (_r, p) =>
        json({
          operationId: p.operationId,
          module: 'finance',
          kind: 'fees.generate',
          status: ++polls < 2 ? 'running' : 'succeeded',
          createTime: 't',
        }),
    });
    await signIn(runtime);
    const { result } = renderHook(
      () => ({ op: useOperation({ module: 'finance', operationId: 'o1' }), idle: useOperation(null) }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.op.data?.status).toBe('succeeded'), { timeout: 4000 });
    expect(result.current.idle.fetchStatus).toBe('idle');
    expect(api.calls.filter((c) => c.path === '/v1/finance/operations/o1')).toHaveLength(2);
  });

  it('lists the job centre and cancels at the owning module', async () => {
    const op = {
      operationId: 'o2',
      module: 'payroll',
      kind: 'payslips.generate',
      status: 'running',
      createTime: 't',
    };
    const { api, runtime, wrapper } = setup({
      'GET /v1/platform/operations': (req) => onePage(req, [op]),
      'POST /v1/payroll/operations/{operationId}:cancel': () => json({ ...op, status: 'cancelled' }),
    });
    await signIn(runtime);
    const { result } = renderHook(
      () => ({ jobs: useJobCentre({ size: 10 }), cancel: useCancelOperation() }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.jobs.data?.data).toHaveLength(1));
    await act(() => result.current.cancel.mutateAsync(op));
    expect(runtime.queryClient.getQueryData(['payroll', 'operation', 'o2'])).toMatchObject({
      status: 'cancelled',
    });
    expect(api.calls.some((c) => c.path === '/v1/payroll/operations/o2:cancel')).toBe(true);
  });
});

describe('PermissionGate and i18n', () => {
  it('gates on permissions and follows the user locale', async () => {
    const { runtime, wrapper } = setup({
      'GET /v1/iam/me': () =>
        json({ ...ME, user: { ...ME.user, locale: 'hi-IN', timezone: 'Asia/Kolkata' } }),
    });
    const { render, screen } = await import('@testing-library/react');
    const { PermissionGate } = await import('../react/PermissionGate');
    const { result } = renderHook(() => useI18n(), { wrapper });
    expect(result.current.locale).toBe('en');
    expect(runtime.locale()).toBe('en');
    await act(() => signIn(runtime));
    expect(result.current.locale).toBe('hi');
    // the Hindi catalogue loads on first use, and the strings follow when it arrives
    await waitFor(() => expect(result.current.t('acc.signIn.title')).toBe('साइन इन करें'));
    expect(result.current.format.money(10, 'INR')).toBe('₹10.00');
    expect(result.current.formatLocale).toBe('hi-IN');
    render(
      <MySmsProvider runtime={runtime}>
        <PermissionGate code="master.country.update">
          <p>can-edit</p>
        </PermissionGate>
        <PermissionGate code="master.country.delete" fallback={<p>no-delete</p>}>
          <p>can-delete</p>
        </PermissionGate>
      </MySmsProvider>,
    );
    expect(screen.getByText('can-edit')).toBeTruthy();
    expect(screen.getByText('no-delete')).toBeTruthy();
    expect(screen.queryByText('can-delete')).toBeNull();
  });

  it('formats with the user locale even when only the base language has a catalogue', async () => {
    const { runtime, wrapper } = setup({
      'GET /v1/iam/me': () =>
        json({ ...ME, user: { ...ME.user, locale: 'en-IN', timezone: 'Asia/Kolkata' } }),
    });
    const { result } = renderHook(() => useI18n(), { wrapper });
    await act(() => signIn(runtime));
    expect(result.current.locale).toBe('en');
    expect(result.current.formatLocale).toBe('en-IN');
    expect(result.current.format.dateTime('2026-11-06T08:00:00Z')).toBe('6 Nov 2026, 1:30 pm');
    expect(result.current.format.number(1234567)).toBe('12,34,567');
  });

  it('falls back to the catalogue locale for an unsupported user locale', async () => {
    const { runtime, wrapper } = setup({
      'GET /v1/iam/me': () => json({ ...ME, user: { ...ME.user, locale: 'not a locale!' } }),
    });
    const { result } = renderHook(() => useI18n(), { wrapper });
    await act(() => signIn(runtime));
    expect(result.current.formatLocale).toBe('en');
  });

  it('uses the tenant default locale before English', () => {
    const runtime = createRuntime({ baseUrl: 'https://h', clientId: 'c', defaultLocale: 'hi' });
    expect(runtime.locale()).toBe('hi');
  });
});
