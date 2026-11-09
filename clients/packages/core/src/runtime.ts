import { AuthApi } from '@mysms/sdk/iam/apis/AuthApi';
import { UserAccountApi } from '@mysms/sdk/iam/apis/UserAccountApi';
import type { InitOverrideFunction } from '@mysms/sdk/runtime';
import { resolveLocale } from '@mysms/i18n';
import type { QueryClient } from '@tanstack/react-query';
import { createApiClient, defaultFetch, type ApiClient, type FetchFn } from './api/client';
import { call } from './api/unwrap';
import {
  createSession,
  memoryVault,
  type AuthTransport,
  type Session,
  type TokenVault,
} from './auth/session';
import { createQueryClient } from './query/queryClient';
import { explicitSetting, settingValue } from './settings/preferences';

export interface RuntimeConfig {
  /** API origin, e.g. `https://school.example.com`. */
  baseUrl: string;
  /** `app/version/platform` for the X-Client header. */
  clientId: string;
  vault?: TokenVault;
  /** Tenant default locale; the user's own locale wins (ui/04 §7). */
  defaultLocale?: string;
  fetch?: FetchFn;
}

/** Everything a client app needs, created once at startup. */
export interface Runtime {
  api: ApiClient;
  session: Session;
  queryClient: QueryClient;
  /** The resolved locale: user → tenant default → en. Also sent as Accept-Language. */
  locale(): string;
}

export function createRuntime(config: RuntimeConfig): Runtime {
  const fetchImpl = config.fetch ?? defaultFetch;
  // the locale closure is created before the session exists; it reads the session through this holder
  const holder: { session?: Session } = {};
  // the language setting (ui.locale, ui/04 §9) wins over the account's locale, which wins over the tenant default
  const locale = () => {
    const state = holder.session?.store.getState();
    return resolveLocale(
      explicitSetting(state?.settings, 'ui.locale') ?? state?.me?.user?.locale,
      config.defaultLocale ?? settingValue(state?.settings, 'ui.locale'),
    );
  };
  // the session's own calls: no bearer injection and no refresh-and-replay (they are the auth flow)
  const bare = createApiClient({
    baseUrl: config.baseUrl,
    clientId: config.clientId,
    locale,
    fetch: fetchImpl,
  });
  const accounts = () => bare.get(UserAccountApi);
  const bearer =
    (token: string): InitOverrideFunction =>
    async ({ init }) => ({ headers: { ...init.headers, Authorization: `Bearer ${token}` } });
  const auth: AuthTransport = {
    login: (body) => call(accounts().login({ loginRequest: body })),
    completeMfa: (body) => call(bare.get(AuthApi).completeMfa({ completeMfaRequest: body })),
    refresh: (refreshToken) => call(accounts().refreshToken({ refreshTokenRequest: { refreshToken } })),
    logout: (accessToken) =>
      call(accounts().logout({ logoutRequest: { allDevices: false } }, bearer(accessToken))),
    me: (accessToken) => call(accounts().getMe(bearer(accessToken))),
    mySettings: async (accessToken) => (await call(accounts().listMySettings(bearer(accessToken)))).items,
  };
  const session = createSession({ auth, vault: config.vault ?? memoryVault() });
  holder.session = session;
  const api = createApiClient({
    baseUrl: config.baseUrl,
    clientId: config.clientId,
    tokens: session,
    locale,
    fetch: fetchImpl,
  });
  const queryClient = createQueryClient();
  // Permission-dependent data must not survive a change of user (ui/05 §3 persona switch / sign-out).
  session.store.subscribe((state, previous) => {
    if (state.me?.user?.id !== previous.me?.user?.id) queryClient.clear();
  });
  return { api, session, queryClient, locale };
}
