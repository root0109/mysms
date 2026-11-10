import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { fakeApi, json, onePage, pageOf, problem, type Handler } from '@mysms/fixtures';
import { MySmsProvider } from '../react/context';
import { createRuntime } from '../runtime';
import { useI18n } from './i18n';
import { useMyPerson, useUpdateMyPerson } from './profile';
import {
  useCreateSettingValue,
  useDeleteSettingValue,
  useInfiniteSettingValues,
  useMySettings,
  usePreferences,
  useReloadMySettings,
  useResetMySetting,
  useSetMySetting,
  useSettingDefinitions,
  useSettingValues,
  useUpdateSettingValue,
} from './settings';

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresInSeconds: 900, tokenType: 'Bearer' };
const PERSON_ID = '01960000-0000-7000-8000-0000000000p1';
const ME = {
  user: { id: 'u1', username: 'asha', personId: PERSON_ID, locale: 'en-IN' },
  permissions: ['iam.setting.read_mine', 'iam.setting.update_mine'],
};
const effective = (settingKey: string, value: string, sourceScope = 'default', locked = false) => ({
  settingKey,
  value,
  sourceScope,
  locked,
  userEditable: true,
});

function setup(extra: Record<string, Handler> = {}) {
  let theme = effective('ui.theme', 'system');
  let locale = effective('ui.locale', 'en');
  const api = fakeApi({
    'POST /v1/iam/auth:login': () => json({ tokens, user: ME.user }),
    'GET /v1/iam/me': () => json(ME),
    'GET /v1/iam/me/settings': () =>
      json({ items: [theme, locale, effective('ui.density', 'compact', 'tenant', true)] }),
    'PUT /v1/iam/me/settings/{settingKey}': async (req, p) => {
      const { value } = (await req.json()) as { value: string };
      if (p.settingKey === 'ui.density') return problem(409, 'SETTING_LOCKED', 'Locked');
      if (p.settingKey === 'ui.theme') theme = effective('ui.theme', value, 'user');
      if (p.settingKey === 'ui.locale') locale = effective('ui.locale', value, 'user');
      return json(p.settingKey === 'ui.theme' ? theme : locale);
    },
    'DELETE /v1/iam/me/settings/{settingKey}': () => {
      theme = effective('ui.theme', 'system');
      return new Response(null, { status: 204 });
    },
    'GET /v1/iam/setting-definitions': (req) =>
      onePage(req, [
        { id: 'd1', settingKey: 'ui.theme', valueType: 'choice', allowedValues: ['system', 'dark'] },
      ]),
    'GET /v1/iam/setting-values': (req) =>
      onePage(req, [{ id: 'v1', scopeType: 'tenant', settingKey: 'ui.theme', value: 'dark' }]),
    'POST /v1/iam/setting-values': () =>
      json(
        { id: 'v2', scopeType: 'tenant', settingKey: 'ui.locale', value: 'hi' },
        { status: 201, headers: { ETag: '"1"' } },
      ),
    'PATCH /v1/iam/setting-values/{settingValueId}': () =>
      json(
        { id: 'v1', scopeType: 'tenant', settingKey: 'ui.theme', value: 'light' },
        { headers: { ETag: '"2"' } },
      ),
    'DELETE /v1/iam/setting-values/{settingValueId}': () => new Response(null, { status: 204 }),
    'GET /v1/people/me': () => json({ id: PERSON_ID, displayName: 'Asha Rao' }, { headers: { ETag: '"3"' } }),
    'PATCH /v1/people/me': async (req) =>
      json(
        { id: PERSON_ID, displayName: 'Asha Rao', ...((await req.json()) as object) },
        { headers: { ETag: '"4"' } },
      ),
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

const signIn = (runtime: ReturnType<typeof setup>['runtime']) =>
  runtime.session.signIn({ tenantCode: 'demo', identifier: 'asha', password: 'pw' });

describe('my settings (ACC-14)', () => {
  it('loads effective settings with the session, sets and resets one, and applies them at once', async () => {
    const { api, runtime, wrapper } = setup();
    await signIn(runtime);
    const { result } = renderHook(
      () => ({
        settings: useMySettings(),
        prefs: usePreferences(),
        set: useSetMySetting(),
        reset: useResetMySetting(),
        definitions: useSettingDefinitions(),
      }),
      { wrapper },
    );
    expect(result.current.settings).toHaveLength(3);
    expect(result.current.prefs).toMatchObject({ theme: 'system', density: 'compact' });
    await act(() => result.current.set.mutateAsync({ settingKey: 'ui.theme', value: 'dark' }));
    await waitFor(() => expect(result.current.prefs.theme).toBe('dark'));
    const put = api.calls.find((c) => c.method === 'PUT');
    expect(put?.path).toBe('/v1/iam/me/settings/ui.theme');
    expect(JSON.parse(put?.body ?? '{}')).toEqual({ value: 'dark' });
    expect(put?.headers.get('Authorization')).toBe('Bearer a1');
    await act(async () => {
      await expect(
        result.current.set.mutateAsync({ settingKey: 'ui.density', value: 'comfortable' }),
      ).rejects.toMatchObject({
        status: 409,
        code: 'SETTING_LOCKED',
      });
    });
    await act(() => result.current.reset.mutateAsync('ui.theme'));
    await waitFor(() => expect(result.current.prefs.theme).toBe('system'));
    await waitFor(() => expect(result.current.definitions.data?.items[0]?.settingKey).toBe('ui.theme'));
  });

  it('switches the interface language from the language setting', async () => {
    const { runtime, wrapper } = setup();
    await signIn(runtime);
    const { result } = renderHook(() => ({ i18n: useI18n(), set: useSetMySetting() }), { wrapper });
    expect(result.current.i18n.locale).toBe('en');
    expect(result.current.i18n.formatLocale).toBe('en-IN');
    await act(() => result.current.set.mutateAsync({ settingKey: 'ui.locale', value: 'hi' }));
    await waitFor(() => expect(result.current.i18n.locale).toBe('hi'));
    expect(runtime.locale()).toBe('hi');
  });
});

describe('setting values (ADM-16)', () => {
  it('lists, creates, updates under If-Match and deletes', async () => {
    const { api, runtime, wrapper } = setup();
    await signIn(runtime);
    const { result } = renderHook(
      () => ({
        list: useSettingValues({ filter: 'scopeType = "tenant"' }),
        create: useCreateSettingValue(),
        update: useUpdateSettingValue(),
        remove: useDeleteSettingValue(),
      }),
      { wrapper },
    );
    await waitFor(() => expect(result.current.list.data?.data).toHaveLength(1));
    const created = await act(() =>
      result.current.create.mutateAsync({ scopeType: 'tenant', settingKey: 'ui.locale', value: 'hi' }),
    );
    expect(created.etag).toBe('"1"');
    const updated = await act(() =>
      result.current.update.mutateAsync({ settingValueId: 'v1', etag: '"1"', changes: { value: 'light' } }),
    );
    expect(updated).toMatchObject({ etag: '"2"', data: { value: 'light' } });
    const patch = api.calls.find((c) => c.method === 'PATCH');
    expect(patch?.headers.get('If-Match')).toBe('"1"');
    expect(patch?.path).toBe('/v1/iam/setting-values/v1');
    await act(() => result.current.remove.mutateAsync({ settingValueId: 'v1', etag: '"2"' }));
    expect(api.calls.find((c) => c.method === 'DELETE')?.headers.get('If-Match')).toBe('"2"');
  });
});

describe('my profile (ACC-07)', () => {
  it('reads the own person with its ETag and updates contact fields under If-Match', async () => {
    const { api, runtime, wrapper } = setup();
    await signIn(runtime);
    const { result } = renderHook(() => ({ person: useMyPerson(), update: useUpdateMyPerson() }), {
      wrapper,
    });
    await waitFor(() =>
      expect(result.current.person.data).toEqual({
        data: { id: PERSON_ID, displayName: 'Asha Rao' },
        etag: '"3"',
      }),
    );
    await act(() =>
      result.current.update.mutateAsync({ etag: '"3"', changes: { primaryEmail: 'asha@example.org' } }),
    );
    await waitFor(() => expect(result.current.person.data?.etag).toBe('"4"'));
    expect(result.current.person.data?.data.primaryEmail).toBe('asha@example.org');
    const patch = api.calls.find((c) => c.method === 'PATCH' && c.path === '/v1/people/me');
    expect(patch?.headers.get('If-Match')).toBe('"3"');
    expect(new URL(`https://x${patch?.path ?? ''}`).pathname).toBe('/v1/people/me');
  });

  it('does not ask for a person when the account has none', async () => {
    const { api, runtime, wrapper } = setup({
      'GET /v1/iam/me': () => json({ user: { id: 'u2', username: 'ops' } }),
    });
    await signIn(runtime);
    const { result } = renderHook(() => useMyPerson(), { wrapper });
    expect(result.current.fetchStatus).toBe('idle');
    expect(api.calls.some((c) => c.path === '/v1/people/me')).toBe(false);
  });
});

describe('paging and recovery', () => {
  it('pages setting values and re-reads settings that failed with the session', async () => {
    let failSettings = true;
    const { runtime, wrapper } = setup({
      'GET /v1/iam/setting-values': (req) =>
        pageOf(
          req,
          [
            { id: 'v1', scopeType: 'tenant', settingKey: 'ui.theme', value: 'dark' },
            { id: 'v2', scopeType: 'tenant', settingKey: 'ui.locale', value: 'hi' },
          ],
          { maxSize: 1 },
        ),
      'GET /v1/iam/me/settings': () =>
        failSettings
          ? problem(503, 'UNAVAILABLE', 'Down')
          : json({ items: [effective('ui.theme', 'dark', 'tenant')] }),
    });
    await signIn(runtime);
    const { result } = renderHook(
      () => ({ pages: useInfiniteSettingValues(), reload: useReloadMySettings(), settings: useMySettings() }),
      { wrapper },
    );
    expect(result.current.settings).toBeNull();
    await waitFor(() => expect(result.current.pages.hasNextPage).toBe(true));
    await act(() => result.current.pages.fetchNextPage());
    await waitFor(() => expect(result.current.pages.data?.pages.flatMap((p) => p.data)).toHaveLength(2));
    expect(result.current.pages.hasNextPage).toBe(false);
    failSettings = false;
    await act(() => result.current.reload.mutateAsync());
    expect(result.current.settings).toHaveLength(1);
  });

  it('signs out with a fresh token so the server ends the session', async () => {
    const { api, runtime } = setup({
      'POST /v1/iam/auth:refresh': () => json({ ...tokens, accessToken: 'a2' }),
      'POST /v1/iam/auth:logout': () => new Response(null, { status: 204 }),
    });
    await signIn(runtime);
    runtime.session.store.setState({ accessExpiresAt: Date.now() - 1000 });
    await runtime.session.signOut();
    const logout = api.calls.find((c) => c.path === '/v1/iam/auth:logout');
    expect(logout?.headers.get('Authorization')).toBe('Bearer a2');
  });
});
