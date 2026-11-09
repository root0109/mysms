import { Configuration, type BaseAPI, type FetchAPI } from '@mysms/sdk/runtime';
import { pathOf } from '../routes/url';
import { newIdempotencyKey } from './ids';

/** What the API client needs from the session: a fresh access token, and a single-flight refresh. */
export interface TokenSource {
  accessToken(): Promise<string | null>;
  refresh(): Promise<boolean>;
}

/** The underlying network call. Defaults to the global `fetch`; tests and React Native may pass their own. */
export type FetchFn = (request: Request) => Promise<Response>;

/** Endpoints that must never trigger refresh-and-replay (they *are* the auth flow). */
const AUTH_PATHS = [
  '/v1/iam/auth:login',
  '/v1/iam/auth:refresh',
  '/v1/iam/mfa:complete',
  '/v1/iam/auth:logout',
];
export const isAuthPath = (url: string): boolean => {
  const path = pathOf(url);
  return AUTH_PATHS.some((p) => path.endsWith(p));
};

export const defaultFetch: FetchFn = (request) => globalThis.fetch(request);

export interface TransportConfig {
  /** `app/version/platform`, e.g. `web/0.1.0/browser`. */
  clientId: string;
  locale?: () => string;
  /** Omitted for the unauthenticated transport the session itself uses. */
  tokens?: TokenSource;
  fetch?: FetchFn;
}

/**
 * The transport under every generated client (ui/05 §3, §6):
 * - Accept-Language, X-Client and, on every POST without one, an Idempotency-Key;
 * - the bearer token, and on a 401 from an ordinary call one single-flight refresh followed by one replay of
 *   the original request with the new token (same Idempotency-Key, so a replayed POST is safe).
 */
export function createTransport(config: TransportConfig): FetchAPI {
  const fetchImpl = config.fetch ?? defaultFetch;
  const locale = config.locale ?? (() => 'en');
  const { tokens } = config;
  return async (input, init) => {
    const request = new Request(input, init);
    request.headers.set('Accept-Language', locale());
    request.headers.set('X-Client', config.clientId);
    if (request.method === 'POST' && !request.headers.has('Idempotency-Key')) {
      request.headers.set('Idempotency-Key', newIdempotencyKey());
    }
    if (!tokens) return fetchImpl(request);
    if (!request.headers.has('Authorization')) {
      const token = await tokens.accessToken();
      if (token) request.headers.set('Authorization', `Bearer ${token}`);
    }
    if (isAuthPath(request.url)) return fetchImpl(request);
    const original = request.clone();
    const response = await fetchImpl(request);
    if (response.status !== 401) return response;
    if (!(await tokens.refresh())) return response;
    const token = await tokens.accessToken();
    if (!token) return response;
    original.headers.set('Authorization', `Bearer ${token}`);
    return fetchImpl(original);
  };
}

/** A generated API class, e.g. `master.CountryApi`. */
export type ApiClass<T extends BaseAPI> = new (configuration: Configuration) => T;

/** Every hook reaches the API through this: one configuration, one instance per generated API class. */
export interface ApiClient {
  readonly configuration: Configuration;
  get<T extends BaseAPI>(api: ApiClass<T>): T;
}

export interface ApiClientConfig extends TransportConfig {
  /** API origin, e.g. `https://school.example.com` (operation paths already start with `/v1`). */
  baseUrl: string;
}

export function createApiClient(config: ApiClientConfig): ApiClient {
  const configuration = new Configuration({
    basePath: config.baseUrl.replace(/\/+$/, ''),
    fetchApi: createTransport(config),
  });
  const instances = new Map<ApiClass<BaseAPI>, BaseAPI>();
  return {
    configuration,
    get<T extends BaseAPI>(api: ApiClass<T>): T {
      let instance = instances.get(api);
      if (!instance) {
        instance = new api(configuration);
        instances.set(api, instance);
      }
      return instance as T;
    },
  };
}
