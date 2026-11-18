import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/problem';
import {
  REFRESH_SKEW_MS,
  createSession,
  memoryVault,
  type AuthTransport,
  type EffectiveSetting,
  type Me,
} from './session';

const ME: Me = { user: { id: 'u1', username: 'asha' }, permissions: ['master.country.read'] } as Me;
const pair = (n: number, ttl = 900) => ({
  accessToken: `a${n}`,
  refreshToken: `r${n}`,
  expiresInSeconds: ttl,
  tokenType: 'Bearer',
});

function transport(overrides: Partial<AuthTransport> = {}): AuthTransport {
  let n = 0;
  return {
    login: vi.fn(async () => ({ tokens: pair(++n), user: ME.user })),
    completeMfa: vi.fn(async () => ({ tokens: pair(++n), user: ME.user })),
    refresh: vi.fn(async () => pair(++n)),
    logout: vi.fn(async () => undefined),
    me: vi.fn(async () => ME),
    mySettings: vi.fn(async () => SETTINGS),
    stopImpersonation: vi.fn(async () => undefined),
    ...overrides,
  };
}

const login = { tenantCode: 'demo', identifier: 'asha', password: 'pw' };
const SETTINGS: EffectiveSetting[] = [
  { settingKey: 'ui.theme', value: 'dark', sourceScope: 'tenant', locked: false, userEditable: true },
];

describe('session', () => {
  it('signs in, keeps the access token in memory and the refresh token in the vault', async () => {
    const vault = memoryVault();
    const auth = transport();
    const s = createSession({ auth, vault, now: () => 0 });
    await expect(s.signIn(login)).resolves.toBe('signedIn');
    expect(s.store.getState()).toMatchObject({
      status: 'signedIn',
      me: ME,
      accessToken: 'a1',
      accessExpiresAt: 900_000,
    });
    await expect(vault.load()).resolves.toBe('r1');
    expect(auth.me).toHaveBeenCalledWith('a1');
  });

  it('handles the MFA challenge', async () => {
    const auth = transport({ login: vi.fn(async () => ({ mfaRequired: true, mfaToken: 'm1' })) });
    const s = createSession({ auth, vault: memoryVault() });
    await expect(s.signIn(login)).resolves.toBe('mfaRequired');
    expect(s.store.getState()).toMatchObject({ status: 'mfaPending', mfaToken: 'm1', accessToken: null });
    await s.completeMfa('123456');
    expect(auth.completeMfa).toHaveBeenCalledWith({ mfaToken: 'm1', code: '123456' });
    expect(s.store.getState()).toMatchObject({ status: 'signedIn', mfaToken: null });
  });

  it('rejects malformed sign-in results', async () => {
    const noToken = createSession({
      auth: transport({ login: vi.fn(async () => ({ mfaRequired: true })) }),
      vault: memoryVault(),
    });
    await expect(noToken.signIn(login)).rejects.toThrow('no mfaToken');
    const empty = createSession({
      auth: transport({ login: vi.fn(async () => ({})) }),
      vault: memoryVault(),
    });
    await expect(empty.signIn(login)).rejects.toThrow('neither tokens');
    await expect(empty.completeMfa('1')).rejects.toThrow('No MFA challenge');
  });

  it('refreshes single-flight', async () => {
    let resolve!: (v: ReturnType<typeof pair>) => void;
    const auth = transport({
      refresh: vi.fn(() => new Promise<ReturnType<typeof pair>>((r) => (resolve = r))),
    });
    const vault = memoryVault();
    await vault.save('r0');
    const s = createSession({ auth, vault });
    const a = s.refresh();
    const b = s.refresh();
    await vi.waitFor(() => expect(auth.refresh).toHaveBeenCalledTimes(1));
    resolve(pair(7));
    await expect(Promise.all([a, b])).resolves.toEqual([true, true]);
    expect(auth.refresh).toHaveBeenCalledTimes(1);
    expect(s.store.getState().accessToken).toBe('a7');
  });

  it('refreshes proactively inside the skew window', async () => {
    let clock = 0;
    const auth = transport();
    const s = createSession({ auth, vault: memoryVault(), now: () => clock });
    await s.signIn(login);
    clock = 900_000 - REFRESH_SKEW_MS - 1;
    await expect(s.accessToken()).resolves.toBe('a1');
    clock = 900_000 - REFRESH_SKEW_MS + 1;
    await expect(s.accessToken()).resolves.toBe('a2');
    expect(auth.refresh).toHaveBeenCalledTimes(1);
  });

  it('expires the session when the refresh token is rejected', async () => {
    const auth = transport({
      refresh: vi.fn(async () => Promise.reject(new ApiError({ status: 401, code: 'BAD', title: 'Bad' }))),
    });
    const vault = memoryVault();
    const s = createSession({ auth, vault });
    await s.signIn(login);
    await expect(s.refresh()).resolves.toBe(false);
    expect(s.store.getState()).toMatchObject({ status: 'expired', me: null, accessToken: null });
    await expect(vault.load()).resolves.toBeNull();
  });

  it('keeps the session on a network failure during refresh', async () => {
    const auth = transport({ refresh: vi.fn(async () => Promise.reject(new TypeError('offline'))) });
    const s = createSession({ auth, vault: memoryVault() });
    await s.signIn(login);
    await expect(s.refresh()).resolves.toBe(false);
    expect(s.store.getState().status).toBe('signedIn');
  });

  it('marks a signed-in session without a refresh token as expired, and a signed-out one stays signed out', async () => {
    const vault = memoryVault();
    const s = createSession({ auth: transport(), vault });
    await expect(s.refresh()).resolves.toBe(false);
    expect(s.store.getState().status).toBe('signedOut');
    await s.signIn(login);
    await vault.clear();
    await expect(s.refresh()).resolves.toBe(false);
    expect(s.store.getState().status).toBe('expired');
  });

  it('stays signed out and clears the vault when a stored refresh token is rejected', async () => {
    const vault = memoryVault();
    await vault.save('revoked');
    const s = createSession({
      auth: transport({
        refresh: vi.fn(async () => Promise.reject(new ApiError({ status: 400, code: 'X', title: 'X' }))),
      }),
      vault,
    });
    await expect(s.restore()).resolves.toBe(false);
    expect(s.store.getState().status).toBe('signedOut');
    await expect(vault.load()).resolves.toBeNull();
  });

  it('restores from a stored refresh token', async () => {
    const vault = memoryVault();
    await vault.save('r0');
    const s = createSession({ auth: transport(), vault });
    await expect(s.restore()).resolves.toBe(true);
    expect(s.store.getState()).toMatchObject({ status: 'signedIn', me: ME });
    const none = createSession({ auth: transport(), vault: memoryVault() });
    await expect(none.restore()).resolves.toBe(false);
  });

  it('reloads me and returns no token when signed out', async () => {
    const auth = transport();
    const s = createSession({ auth, vault: memoryVault() });
    await expect(s.accessToken()).resolves.toBeNull();
    await s.reloadMe();
    expect(auth.me).not.toHaveBeenCalled();
    await s.signIn(login);
    await s.reloadMe();
    expect(auth.me).toHaveBeenCalledTimes(2);
  });

  it('signs out locally even when the server call fails', async () => {
    const vault = memoryVault();
    const auth = transport({ logout: vi.fn(async () => Promise.reject(new TypeError('offline'))) });
    const s = createSession({ auth, vault });
    await s.signIn(login);
    await s.signOut();
    expect(auth.logout).toHaveBeenCalledWith('a1');
    expect(s.store.getState()).toMatchObject({ status: 'signedOut', me: null, accessToken: null });
    await expect(vault.load()).resolves.toBeNull();
    await s.signOut();
    expect(auth.logout).toHaveBeenCalledTimes(1);
  });

  it('loads the effective settings with the session and tolerates a server without them', async () => {
    const auth = transport();
    const s = createSession({ auth, vault: memoryVault() });
    await s.signIn(login);
    expect(s.store.getState().settings).toEqual(SETTINGS);
    expect(auth.mySettings).toHaveBeenCalledWith('a1');

    const failing = transport({
      mySettings: vi.fn(async () => {
        throw new ApiError({ status: 501, code: 'NOT_IMPLEMENTED', title: 'Not Implemented' });
      }),
    });
    const t = createSession({ auth: failing, vault: memoryVault() });
    await expect(t.signIn(login)).resolves.toBe('signedIn');
    expect(t.store.getState().settings).toBeNull();
  });

  it('reloads settings on demand and restores them with the session', async () => {
    const vault = memoryVault();
    await vault.save('r0');
    const next: EffectiveSetting[] = [
      { settingKey: 'ui.theme', value: 'light', sourceScope: 'user', locked: false, userEditable: true },
    ];
    const auth = transport();
    const s = createSession({ auth, vault, now: () => 0 });
    await expect(s.restore()).resolves.toBe(true);
    expect(s.store.getState().settings).toEqual(SETTINGS);
    vi.mocked(auth.mySettings).mockResolvedValueOnce(next);
    await s.reloadSettings();
    expect(s.store.getState().settings).toEqual(next);
    vi.mocked(auth.mySettings).mockRejectedValueOnce(new Error('offline'));
    await s.reloadSettings();
    expect(s.store.getState().settings).toEqual(next);
    await s.signOut();
    expect(s.store.getState().settings).toBeNull();
    await s.reloadSettings();
    expect(auth.mySettings).toHaveBeenCalledTimes(3);
  });

  it('adopts the tokens and Me of a role switch', async () => {
    const vault = memoryVault();
    const s = createSession({ auth: transport(), vault, now: () => 0 });
    await s.signIn(login);
    const switched = { ...ME, activeRole: { roleCode: 'teacher', roleId: 'r-t', scopeType: 'tenant' } } as Me;
    await s.adopt(pair(7), switched);
    expect(s.store.getState()).toMatchObject({ accessToken: 'a7', me: switched, status: 'signedIn' });
    await expect(vault.load()).resolves.toBe('r7');
  });

  it('acts as another user until the impersonation ends, then returns to the own session', async () => {
    const vault = memoryVault();
    const target = { user: { id: 'u2', username: 'pupil' }, permissions: [] } as unknown as Me;
    const auth = transport({ me: vi.fn(async (token: string) => (token === 'imp' ? target : ME)) });
    const s = createSession({ auth, vault, now: () => 0 });
    await s.signIn(login);
    await s.impersonate({ accessToken: 'imp', expiresInSeconds: 900, impersonatedUserId: 'u2' });
    expect(s.store.getState()).toMatchObject({
      accessToken: 'imp',
      me: target,
      impersonation: { userId: 'u2', expiresAt: 900_000 },
    });
    await expect(vault.load()).resolves.toBe('r1');
    await s.endImpersonation();
    expect(auth.stopImpersonation).toHaveBeenCalledWith('imp');
    expect(s.store.getState()).toMatchObject({ accessToken: 'a1', me: ME, impersonation: null });
    await s.endImpersonation();
    expect(auth.stopImpersonation).toHaveBeenCalledTimes(1);
  });

  it('never refreshes an impersonation token: the own session comes back and is refreshed', async () => {
    const vault = memoryVault();
    const target = { user: { id: 'u2' }, permissions: [] } as unknown as Me;
    const auth = transport({ me: vi.fn(async (token: string) => (token === 'imp' ? target : ME)) });
    const s = createSession({ auth, vault, now: () => 0 });
    await s.signIn(login);
    await s.impersonate({ accessToken: 'imp', expiresInSeconds: 900, impersonatedUserId: 'u2' });
    await expect(s.refresh()).resolves.toBe(true);
    expect(s.store.getState()).toMatchObject({ impersonation: null, me: ME, accessToken: 'a2' });
    expect(auth.refresh).toHaveBeenCalledWith('r1');
  });

  it('signs the administrator out of the impersonation and of their own session', async () => {
    const auth = transport();
    const s = createSession({ auth, vault: memoryVault(), now: () => 0 });
    await s.signIn(login);
    await s.impersonate({ accessToken: 'imp', expiresInSeconds: 900, impersonatedUserId: 'u2' });
    await s.signOut();
    expect(auth.stopImpersonation).toHaveBeenCalledWith('imp');
    expect(auth.logout).toHaveBeenCalledWith('a1');
    expect(s.store.getState()).toMatchObject({ status: 'signedOut', impersonation: null, me: null });
  });
});
