import { describe, expect, it } from 'vitest';
import type { iam } from '@mysms/sdk';
import {
  countries,
  operations,
  platformAdmin,
  schoolAdmin,
  schoolAdminPerson,
  settingDefinitions,
  TENANT_CODE,
} from './data';
import { fakeApi } from './fakeApi';
import { fakeBackend } from './handlers';

const BASE = 'https://h';
const auth = { Authorization: 'Bearer x' };

function backend() {
  const api = fakeApi(fakeBackend());
  const call = (
    method: string,
    path: string,
    init: { headers?: Record<string, string>; body?: unknown } = {},
  ) =>
    api.fetch(
      new Request(BASE + path, {
        method,
        headers: { 'Content-Type': 'application/json', ...init.headers },
        body: init.body === undefined ? null : JSON.stringify(init.body),
      }),
    );
  return { api, call };
}

describe('fake backend', () => {
  it('signs in only with the seeded credentials', async () => {
    const { call } = backend();
    const ok = await call('POST', '/v1/iam/auth:login', {
      body: { tenantCode: TENANT_CODE, identifier: platformAdmin.user?.username, password: 'password' },
    });
    expect(ok.status).toBe(200);
    const bad = await call('POST', '/v1/iam/auth:login', {
      body: { tenantCode: TENANT_CODE, identifier: 'x', password: 'password' },
    });
    expect(bad.status).toBe(401);
    expect((await call('GET', '/v1/iam/me')).status).toBe(401);
    expect((await call('GET', '/v1/iam/me', { headers: auth })).status).toBe(200);
  });

  it('pages countries the way the server does', async () => {
    const { call } = backend();
    type Page = { data: unknown[]; paging: { page: number; size: number; previous?: string; next?: string } };
    const first = (await (await call('GET', '/v1/master/countries?size=3')).json()) as Page;
    expect(first.data).toHaveLength(3);
    expect(first.paging).toMatchObject({ page: 1, size: 3 });
    expect(first.paging.previous).toBeUndefined();
    expect(first.paging.next).toBe('/v1/master/countries?page=2&size=3&cursor=2:3');
    const second = (await (await call('GET', first.paging.next ?? '')).json()) as Page;
    expect(second.data).toHaveLength(countries.length - 3);
    expect(second.paging.next).toBeUndefined();
    expect(second.paging.previous).toBe('/v1/master/countries?page=1&size=3');
    expect((await call('GET', '/v1/master/countries?page=3&size=3&cursor=2:3')).status).toBe(400);
  });

  it('enforces If-Match on writes and bumps the ETag', async () => {
    const { call } = backend();
    const id = countries[0]?.id as string;
    expect((await call('GET', `/v1/master/countries/${id}`)).headers.get('ETag')).toBe('"1"');
    expect(
      (await call('PATCH', `/v1/master/countries/${id}`, { headers: auth, body: { name: 'X' } })).status,
    ).toBe(428);
    expect(
      (
        await call('PATCH', `/v1/master/countries/${id}`, {
          headers: { ...auth, 'If-Match': '"9"' },
          body: { name: 'X' },
        })
      ).status,
    ).toBe(412);
    const ok = await call('PATCH', `/v1/master/countries/${id}`, {
      headers: { ...auth, 'If-Match': '"1"' },
      body: { name: 'Bharat' },
    });
    expect(ok.status).toBe(200);
    expect(ok.headers.get('ETag')).toBe('"2"');
    expect(
      (await call('DELETE', `/v1/master/countries/${id}`, { headers: { ...auth, 'If-Match': '"1"' } }))
        .status,
    ).toBe(412);
    expect(
      (await call('DELETE', `/v1/master/countries/${id}`, { headers: { ...auth, 'If-Match': '"2"' } }))
        .status,
    ).toBe(204);
    expect((await call('GET', `/v1/master/countries/${id}`)).status).toBe(404);
    expect((await call('PATCH', `/v1/master/countries/${id}`, { headers: auth, body: {} })).status).toBe(404);
    expect((await call('DELETE', `/v1/master/countries/${id}`, { headers: auth })).status).toBe(404);
    expect((await call('PATCH', `/v1/master/countries/${id}`, { body: {} })).status).toBe(401);
    expect((await call('DELETE', `/v1/master/countries/${id}`)).status).toBe(401);
  });

  it('validates creates with field errors', async () => {
    const { call } = backend();
    expect((await call('POST', '/v1/master/countries', { body: { name: 'X' } })).status).toBe(401);
    const bad = await call('POST', '/v1/master/countries', { headers: auth, body: {} });
    expect(bad.status).toBe(422);
    expect(((await bad.json()) as { errors: { field: string }[] }).errors.map((e) => e.field)).toEqual([
      'name',
      'iso2',
    ]);
    const created = await call('POST', '/v1/master/countries', {
      headers: auth,
      body: { name: 'Bhutan', iso2: 'BT' },
    });
    expect(created.status).toBe(201);
    expect(created.headers.get('ETag')).toBe('"1"');
  });

  it('serves operations per module and refuses to cancel finished ones', async () => {
    const { call } = backend();
    const [running, done] = operations;
    expect((await call('GET', '/v1/platform/operations')).status).toBe(401);
    expect((await call('GET', '/v1/platform/operations', { headers: auth })).status).toBe(200);
    expect(
      (await call('GET', `/v1/${running?.module}/operations/${running?.operationId}`, { headers: auth }))
        .status,
    ).toBe(200);
    expect(
      (await call('GET', `/v1/payroll/operations/${running?.operationId}`, { headers: auth })).status,
    ).toBe(404);
    expect((await call('GET', `/v1/finance/operations/${running?.operationId}`)).status).toBe(401);
    const cancelled = await call('POST', `/v1/finance/operations/${running?.operationId}:cancel`, {
      headers: auth,
    });
    expect(((await cancelled.json()) as { status: string }).status).toBe('cancelled');
    expect(
      (await call('POST', `/v1/payroll/operations/${done?.operationId}:cancel`, { headers: auth })).status,
    ).toBe(409);
    expect(
      (await call('POST', `/v1/lms/operations/${done?.operationId}:cancel`, { headers: auth })).status,
    ).toBe(404);
    expect((await call('POST', `/v1/lms/operations/x:cancel`)).status).toBe(401);
    expect((await call('POST', '/v1/iam/auth:refresh', { body: { refreshToken: 'r' } })).status).toBe(200);
    expect((await call('POST', '/v1/iam/auth:logout', { headers: auth, body: {} })).status).toBe(204);
    expect((await call('POST', '/v1/iam/auth:logout', { body: {} })).status).toBe(401);
    expect((await call('GET', '/v1/unknown')).status).toBe(404);
  });
});

describe('fake backend: settings and profile', () => {
  function asAdmin() {
    const api = fakeApi(fakeBackend({ me: schoolAdmin }));
    const call = (method: string, path: string, body?: unknown, headers: Record<string, string> = {}) =>
      api.fetch(
        new Request(BASE + path, {
          method,
          headers: { 'Content-Type': 'application/json', ...auth, ...headers },
          body: body === undefined ? null : JSON.stringify(body),
        }),
      );
    return { call };
  }
  const items = async (res: Response) => ((await res.json()) as { items: iam.EffectiveSetting[] }).items;
  const find = (list: iam.EffectiveSetting[], key: string) => list.find((s) => s.settingKey === key);

  it('resolves defaults, tenant values, own values and tenant locks', async () => {
    const { call } = asAdmin();
    const defaults = await items(await call('GET', '/v1/iam/me/settings'));
    expect(defaults).toHaveLength(settingDefinitions.length);
    expect(find(defaults, 'ui.theme')).toMatchObject({
      value: 'system',
      sourceScope: 'default',
      locked: false,
    });
    expect(find(defaults, 'security.sessionTimeoutMinutes')?.userEditable).toBe(false);

    const tenantTheme = await call('POST', '/v1/iam/setting-values', {
      scopeType: 'tenant',
      settingKey: 'ui.theme',
      value: 'dark',
    });
    expect(tenantTheme.status).toBe(201);
    expect(find(await items(await call('GET', '/v1/iam/me/settings')), 'ui.theme')?.sourceScope).toBe(
      'tenant',
    );
    const mine = await call('PUT', '/v1/iam/me/settings/ui.theme', { value: 'light' });
    expect(await mine.json()).toMatchObject({ value: 'light', sourceScope: 'user' });
    expect((await call('PUT', '/v1/iam/me/settings/ui.theme', { value: 'neon' })).status).toBe(422);
    expect(
      (await call('PUT', '/v1/iam/me/settings/security.sessionTimeoutMinutes', { value: '30' })).status,
    ).toBe(422);
    expect((await call('PUT', '/v1/iam/me/settings/nope', { value: 'x' })).status).toBe(404);

    const id = ((await tenantTheme.json()) as { id: string }).id;
    expect((await call('PATCH', `/v1/iam/setting-values/${id}`, { isLocked: true })).status).toBe(428);
    expect(
      (await call('PATCH', `/v1/iam/setting-values/${id}`, { isLocked: true }, { 'If-Match': '"9"' })).status,
    ).toBe(412);
    const locked = await call(
      'PATCH',
      `/v1/iam/setting-values/${id}`,
      { isLocked: true },
      { 'If-Match': '"1"' },
    );
    expect(locked.headers.get('ETag')).toBe('"2"');
    expect(find(await items(await call('GET', '/v1/iam/me/settings')), 'ui.theme')).toMatchObject({
      value: 'dark',
      locked: true,
    });
    const refused = await call('PUT', '/v1/iam/me/settings/ui.theme', { value: 'light' });
    expect(refused.status).toBe(409);
    expect(((await refused.json()) as { code: string }).code).toBe('SETTING_LOCKED');

    expect(
      (await call('DELETE', `/v1/iam/setting-values/${id}`, undefined, { 'If-Match': '"2"' })).status,
    ).toBe(204);
    expect(find(await items(await call('GET', '/v1/iam/me/settings')), 'ui.theme')?.sourceScope).toBe('user');
    expect((await call('DELETE', '/v1/iam/me/settings/ui.theme')).status).toBe(204);
    expect(find(await items(await call('GET', '/v1/iam/me/settings')), 'ui.theme')?.sourceScope).toBe(
      'default',
    );
  });

  it('hides administrator-only settings from users who do not administer settings', async () => {
    const api = fakeApi(fakeBackend());
    const get = (path: string) => api.fetch(new Request(BASE + path, { headers: auth }));
    const mine = await items(await get('/v1/iam/me/settings'));
    expect(find(mine, 'security.sessionTimeoutMinutes')).toBeUndefined();
    expect(mine.length).toBe(settingDefinitions.length - 1);
    const catalogue = (await (await get('/v1/iam/setting-definitions')).json()) as { data: unknown[] };
    expect(catalogue.data).toHaveLength(settingDefinitions.length - 1);
    const put = await api.fetch(
      new Request(`${BASE}/v1/iam/me/settings/security.sessionTimeoutMinutes`, {
        method: 'PUT',
        headers: { ...auth, 'Content-Type': 'application/json' },
        body: JSON.stringify({ value: '30' }),
      }),
    );
    expect(put.status).toBe(404);
  });

  it('applies the catalogue rules to administrators too', async () => {
    const { call } = asAdmin();
    for (const body of [
      { scopeType: 'tenant', settingKey: 'nope', value: 'x' },
      { scopeType: 'department', settingKey: 'security.sessionTimeoutMinutes', scopeId: 'd1', value: '30' },
      { scopeType: 'department', settingKey: 'ui.theme', value: 'dark' },
      { scopeType: 'tenant', settingKey: 'ui.theme', scopeId: 'x', value: 'dark' },
      { scopeType: 'user', settingKey: 'ui.theme', scopeId: 'u', value: 'dark', isLocked: true },
      { scopeType: 'tenant', settingKey: 'ui.textScale', value: '99' },
      { scopeType: 'tenant', settingKey: 'ui.reducedMotion', value: 'yes' },
    ]) {
      expect((await call('POST', '/v1/iam/setting-values', body)).status, JSON.stringify(body)).toBe(422);
    }
    expect(
      (
        await call('POST', '/v1/iam/setting-values', {
          scopeType: 'tenant',
          settingKey: 'ui.textScale',
          value: '120',
        })
      ).status,
    ).toBe(201);
    expect(
      (
        await call('POST', '/v1/iam/setting-values', {
          scopeType: 'tenant',
          settingKey: 'ui.textScale',
          value: '130',
        })
      ).status,
    ).toBe(409);
    const list = (await (await call('GET', '/v1/iam/setting-values')).json()) as { data: unknown[] };
    expect(list.data).toHaveLength(1);
  });

  it('serves and updates the own person record under If-Match', async () => {
    const { call } = asAdmin();
    const read = await call('GET', '/v1/people/me');
    expect(read.headers.get('ETag')).toBe('"1"');
    expect(await read.json()).toMatchObject({ displayName: schoolAdminPerson.displayName });
    expect((await call('PATCH', '/v1/people/me', { aboutMe: 'x' })).status).toBe(428);
    expect(
      (await call('PATCH', '/v1/people/me', { websiteUrl: 'javascript:alert(1)' }, { 'If-Match': '"1"' }))
        .status,
    ).toBe(422);
    const ok = await call(
      'PATCH',
      '/v1/people/me',
      { primaryEmail: 'ravi@new.example' },
      { 'If-Match': '"1"' },
    );
    expect(ok.headers.get('ETag')).toBe('"2"');
    const me = (await (await call('GET', '/v1/iam/me')).json()) as iam.Me;
    expect(me.person?.primaryEmail).toBe('ravi@new.example');
    const noPerson = fakeApi(fakeBackend());
    const res = await noPerson.fetch(new Request(`${BASE}/v1/people/me`, { headers: auth }));
    expect(res.status).toBe(404);
  });
});
