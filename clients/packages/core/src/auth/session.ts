import type { iam } from '@mysms/sdk';
import { createStore, type StoreApi } from 'zustand/vanilla';
import { isApiError } from '../api/problem';

export type Me = iam.Me;
export type LoginRequest = iam.LoginRequest;
export type LoginResult = iam.LoginResult;
export type TokenPair = iam.TokenPair;
export type EffectiveSetting = iam.EffectiveSetting;
export type ImpersonationToken = iam.ImpersonationToken;

/** An administrator acting as another user (iam.StartImpersonation): whom, and until when (epoch ms). */
export interface Impersonation {
  userId: string;
  expiresAt: number;
}

export type SessionStatus = 'signedOut' | 'mfaPending' | 'signedIn' | 'expired';

export interface SessionState {
  status: SessionStatus;
  me: Me | null;
  mfaToken: string | null;
  /** Access token lives in memory only (ui/05 §3). */
  accessToken: string | null;
  accessExpiresAt: number | null;
  /**
   * The caller's effective settings (iam.ListMySettings), loaded with the session so theme, density, text size,
   * motion and language apply before the shell renders (ui/04 §9). Null until loaded or when the server has none.
   */
  settings: EffectiveSetting[] | null;
  /**
   * Set while an administrator acts as another user: requests carry the impersonation token and `me` is that
   * user's; the administrator's own session waits and comes back when it ends (ui/02 ADM-09 banner).
   */
  impersonation: Impersonation | null;
}

/** Where the refresh token is kept: secure storage on devices, memory (or an httpOnly cookie flow) on the web. */
export interface TokenVault {
  load(): Promise<string | null>;
  save(refreshToken: string): Promise<void>;
  clear(): Promise<void>;
}

export function memoryVault(): TokenVault {
  let value: string | null = null;
  return {
    load: async () => value,
    save: async (t) => {
      value = t;
    },
    clear: async () => {
      value = null;
    },
  };
}

/** The auth endpoints, called without the refresh-and-replay middleware. */
export interface AuthTransport {
  login(body: LoginRequest): Promise<LoginResult>;
  completeMfa(body: { mfaToken: string; code: string }): Promise<LoginResult>;
  refresh(refreshToken: string): Promise<TokenPair>;
  logout(accessToken: string): Promise<void>;
  me(accessToken: string): Promise<Me>;
  mySettings(accessToken: string): Promise<EffectiveSetting[]>;
  stopImpersonation(accessToken: string): Promise<void>;
}

/** Refresh this long before the access token expires (clock-skew tolerance, ui/05 §3). */
export const REFRESH_SKEW_MS = 60_000;

export interface Session {
  readonly store: StoreApi<SessionState>;
  signIn(body: LoginRequest): Promise<'signedIn' | 'mfaRequired'>;
  completeMfa(code: string): Promise<void>;
  /** On app start: exchange a stored refresh token for a session. */
  restore(): Promise<boolean>;
  /** Single-flight: concurrent callers share one refresh request. */
  refresh(): Promise<boolean>;
  /** A usable access token, refreshed proactively when about to expire; null when signed out. */
  accessToken(): Promise<string | null>;
  reloadMe(): Promise<void>;
  /** Re-reads the effective settings, e.g. after the user changed one. */
  reloadSettings(): Promise<void>;
  /** A role switch (iam.SwitchRole) answered: its tokens replace the session's and its Me becomes the caller's. */
  adopt(tokens: TokenPair, me: Me): Promise<void>;
  /** Act as another user with an impersonation token (no refresh token); the own session waits. */
  impersonate(token: ImpersonationToken): Promise<void>;
  /** Ends acting as another user (iam.StopImpersonation, best effort) and returns to the own session. */
  endImpersonation(): Promise<void>;
  signOut(): Promise<void>;
}

const EMPTY: SessionState = {
  status: 'signedOut',
  me: null,
  mfaToken: null,
  accessToken: null,
  accessExpiresAt: null,
  settings: null,
  impersonation: null,
};

/** What waits while an administrator acts as another user. */
type OwnSession = Pick<SessionState, 'me' | 'settings' | 'accessToken' | 'accessExpiresAt'>;

export function createSession(deps: { auth: AuthTransport; vault: TokenVault; now?: () => number }): Session {
  const { auth, vault } = deps;
  const now = deps.now ?? (() => Date.now());
  const store = createStore<SessionState>(() => ({ ...EMPTY }));
  let inflight: Promise<boolean> | null = null;
  let own: OwnSession | null = null;

  /** Back to the administrator's own session; its access token is refreshed as usual when needed. */
  function restoreOwn(): void {
    if (!own) return;
    store.setState({ ...own, impersonation: null });
    own = null;
  }

  async function applyTokens(t: TokenPair): Promise<void> {
    await vault.save(t.refreshToken);
    store.setState({ accessToken: t.accessToken, accessExpiresAt: now() + t.expiresInSeconds * 1000 });
  }

  /** Settings never block the session: without them the defaults apply. */
  async function loadSettings(accessToken: string): Promise<EffectiveSetting[] | null> {
    try {
      return await auth.mySettings(accessToken);
    } catch {
      return null;
    }
  }

  async function finishSignIn(result: LoginResult): Promise<void> {
    if (!result.tokens) throw new Error('Sign-in returned neither tokens nor an MFA challenge');
    await applyTokens(result.tokens);
    const [me, settings] = await Promise.all([
      auth.me(result.tokens.accessToken),
      loadSettings(result.tokens.accessToken),
    ]);
    store.setState({ status: 'signedIn', me, settings, mfaToken: null });
  }

  async function doRefresh(): Promise<boolean> {
    // an impersonation token is never refreshed: when it runs out (or is refused) the own session comes back
    restoreOwn();
    const refreshToken = await vault.load();
    if (!refreshToken) {
      if (store.getState().status === 'signedIn') store.setState({ ...EMPTY, status: 'expired' });
      return false;
    }
    try {
      await applyTokens(await auth.refresh(refreshToken));
      return true;
    } catch (e) {
      // A rejected refresh token ends the session; a network failure keeps it (the next call can retry).
      if (isApiError(e) && e.status >= 400 && e.status < 500) {
        await vault.clear();
        const wasSignedIn = store.getState().status === 'signedIn';
        store.setState({ ...EMPTY, status: wasSignedIn ? 'expired' : 'signedOut' });
      }
      return false;
    }
  }

  const session: Session = {
    store,
    async signIn(body) {
      const result = await auth.login(body);
      if (result.mfaRequired) {
        if (!result.mfaToken) throw new Error('MFA required but no mfaToken returned');
        store.setState({ ...EMPTY, status: 'mfaPending', mfaToken: result.mfaToken });
        return 'mfaRequired';
      }
      await finishSignIn(result);
      return 'signedIn';
    },
    async completeMfa(code) {
      const { mfaToken } = store.getState();
      if (!mfaToken) throw new Error('No MFA challenge is pending');
      await finishSignIn(await auth.completeMfa({ mfaToken, code }));
    },
    async restore() {
      if (!(await session.refresh())) return false;
      const token = store.getState().accessToken;
      if (!token) return false;
      const [me, settings] = await Promise.all([auth.me(token), loadSettings(token)]);
      store.setState({ status: 'signedIn', me, settings });
      return true;
    },
    refresh() {
      inflight ??= doRefresh().finally(() => {
        inflight = null;
      });
      return inflight;
    },
    async accessToken() {
      const s = store.getState();
      if (!s.accessToken || s.accessExpiresAt === null) return null;
      if (s.accessExpiresAt - now() <= REFRESH_SKEW_MS) await session.refresh();
      return store.getState().accessToken;
    },
    async reloadMe() {
      const token = await session.accessToken();
      if (!token) return;
      store.setState({ me: await auth.me(token) });
    },
    async reloadSettings() {
      const token = await session.accessToken();
      if (!token) return;
      const settings = await loadSettings(token);
      if (settings) store.setState({ settings });
    },
    async adopt(tokens, me) {
      await applyTokens(tokens);
      store.setState({ me });
    },
    async impersonate(token) {
      const s = store.getState();
      const me = await auth.me(token.accessToken);
      own ??= {
        me: s.me,
        settings: s.settings,
        accessToken: s.accessToken,
        accessExpiresAt: s.accessExpiresAt,
      };
      const expiresAt = now() + token.expiresInSeconds * 1000;
      store.setState({
        me,
        accessToken: token.accessToken,
        accessExpiresAt: expiresAt,
        impersonation: { userId: token.impersonatedUserId, expiresAt },
      });
    },
    async endImpersonation() {
      const { impersonation, accessToken } = store.getState();
      if (!impersonation) return;
      try {
        if (accessToken) await auth.stopImpersonation(accessToken);
      } catch {
        // the token ends on its own within minutes; the own session comes back either way
      } finally {
        restoreOwn();
      }
    },
    async signOut() {
      await session.endImpersonation();
      // a fresh token, so an expired one cannot leave the server-side session open
      const token = await session.accessToken().catch(() => store.getState().accessToken);
      try {
        if (token) await auth.logout(token);
      } catch {
        // Sign-out always succeeds locally; the server revokes on its side when reachable.
      } finally {
        await vault.clear();
        store.setState({ ...EMPTY });
      }
    },
  };
  return session;
}
