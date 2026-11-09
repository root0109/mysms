import { describe, expect, it, vi } from 'vitest';
import { iam, master, platform } from '@mysms/sdk';
import { fakeApi, json, problem } from '@mysms/fixtures';
import { createApiClient, createTransport, isAuthPath } from './client';
import { call } from './unwrap';

const BASE = 'https://school.example.com';

function tokens(initial: string | null, refreshTo: string | null) {
  let current = initial;
  return {
    accessToken: vi.fn(async () => current),
    refresh: vi.fn(async () => {
      if (refreshTo === null) return false;
      current = refreshTo;
      return true;
    }),
  };
}

describe('createApiClient', () => {
  it('sends the standard headers, bearer token and an Idempotency-Key on POST', async () => {
    const api = fakeApi({
      'GET /v1/master/countries': () => json({ items: [] }),
      'POST /v1/master/countries': () => json({ id: 'c1', iso2: 'IN', name: 'India' }, { status: 201 }),
    });
    const client = createApiClient({
      baseUrl: `${BASE}/`,
      clientId: 'web/0.1.0/browser',
      tokens: tokens('tok', null),
      locale: () => 'hi',
      fetch: api.fetch,
    });
    const countries = client.get(master.CountryApi);
    await expect(call(countries.listCountries({ pageSize: 20 }))).resolves.toEqual({ items: [] });
    await call(countries.createCountry({ country: { iso2: 'IN', iso3: 'IND', name: 'India' } }));
    const [get, post] = api.calls;
    expect(get?.path).toBe('/v1/master/countries');
    expect(get?.headers.get('Authorization')).toBe('Bearer tok');
    expect(get?.headers.get('Accept-Language')).toBe('hi');
    expect(get?.headers.get('X-Client')).toBe('web/0.1.0/browser');
    expect(get?.headers.has('Idempotency-Key')).toBe(false);
    expect(post?.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-/);
    expect(post?.headers.get('Content-Type')).toBe('application/json');
    expect(JSON.parse(post?.body ?? '')).toEqual({ iso2: 'IN', iso3: 'IND', name: 'India' });
  });

  it('returns one instance per generated API class', () => {
    const client = createApiClient({ baseUrl: BASE, clientId: 'c', fetch: fakeApi({}).fetch });
    expect(client.get(master.CountryApi)).toBe(client.get(master.CountryApi));
    expect(client.get(master.CityApi)).not.toBe(client.get(master.CountryApi));
    expect(client.configuration.basePath).toBe(BASE);
  });

  it('keeps a caller-supplied Idempotency-Key and works without a token', async () => {
    const api = fakeApi({ 'POST /v1/platform/tags': () => json({}, { status: 201 }) });
    const client = createApiClient({
      baseUrl: BASE,
      clientId: 'c',
      tokens: tokens(null, null),
      fetch: api.fetch,
    });
    await call(
      client
        .get(platform.TagApi)
        .createTag({ tag: { name: 'Sports', slug: 'sports' }, idempotencyKey: 'fixed' }),
    );
    expect(api.calls[0]?.headers.get('Idempotency-Key')).toBe('fixed');
    expect(api.calls[0]?.headers.has('Authorization')).toBe(false);
    expect(api.calls[0]?.headers.get('Accept-Language')).toBe('en');
  });

  it('refreshes once on 401 and replays the request with the same Idempotency-Key', async () => {
    let first = true;
    const api = fakeApi({
      'POST /v1/master/countries': (req) => {
        if (first) {
          first = false;
          return problem(401, 'UNAUTHENTICATED');
        }
        expect(req.headers.get('Authorization')).toBe('Bearer fresh');
        return json({ id: 'c1' }, { status: 201 });
      },
    });
    const t = tokens('stale', 'fresh');
    const client = createApiClient({ baseUrl: BASE, clientId: 'c', tokens: t, fetch: api.fetch });
    const country = { iso2: 'XX', iso3: 'XXX', name: 'X' };
    const created = await call(client.get(master.CountryApi).createCountry({ country }));
    expect(created).toEqual({ id: 'c1' });
    expect(t.refresh).toHaveBeenCalledTimes(1);
    expect(api.calls).toHaveLength(2);
    expect(api.calls[0]?.headers.get('Idempotency-Key')).toBe(api.calls[1]?.headers.get('Idempotency-Key'));
    expect(api.calls[1]?.body).toBe(JSON.stringify(country));
  });

  it('returns the 401 when the refresh fails, and never replays auth endpoints', async () => {
    const api = fakeApi({
      'GET /v1/iam/me': () => problem(401, 'UNAUTHENTICATED'),
      'POST /v1/iam/auth:refresh': () => problem(401, 'BAD_REFRESH'),
    });
    const t = tokens('stale', null);
    const client = createApiClient({ baseUrl: BASE, clientId: 'c', tokens: t, fetch: api.fetch });
    const accounts = client.get(iam.UserAccountApi);
    await expect(call(accounts.getMe())).rejects.toMatchObject({ status: 401, code: 'UNAUTHENTICATED' });
    expect(t.refresh).toHaveBeenCalledTimes(1);
    await expect(
      call(accounts.refreshToken({ refreshTokenRequest: { refreshToken: 'x' } })),
    ).rejects.toMatchObject({ code: 'BAD_REFRESH' });
    expect(t.refresh).toHaveBeenCalledTimes(1);
  });

  it('gives up when the refreshed session has no token', async () => {
    const api = fakeApi({ 'GET /v1/iam/me': () => problem(401, 'UNAUTHENTICATED') });
    const t = {
      accessToken: vi.fn().mockResolvedValueOnce('stale').mockResolvedValue(null),
      refresh: vi.fn(async () => true),
    };
    const client = createApiClient({ baseUrl: BASE, clientId: 'c', tokens: t, fetch: api.fetch });
    await expect(call(client.get(iam.UserAccountApi).getMe())).rejects.toMatchObject({ status: 401 });
    expect(api.calls).toHaveLength(1);
  });

  it('the transport alone adds headers without tokens', async () => {
    const api = fakeApi({ 'POST /v1/iam/auth:login': () => json({}) });
    const transport = createTransport({ clientId: 'c', fetch: api.fetch });
    await transport(`${BASE}/v1/iam/auth:login`, { method: 'POST', body: '{}' });
    expect(api.calls[0]?.headers.get('X-Client')).toBe('c');
    expect(api.calls[0]?.headers.has('Idempotency-Key')).toBe(true);
    expect(api.calls[0]?.headers.has('Authorization')).toBe(false);
  });

  it('recognises auth paths behind a base path', () => {
    expect(isAuthPath('https://h/api/v1/iam/auth:login')).toBe(true);
    expect(isAuthPath('https://h/v1/iam/mfa:complete')).toBe(true);
    expect(isAuthPath('https://h/v1/iam/me')).toBe(false);
  });
});
