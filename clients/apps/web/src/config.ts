/** Runtime configuration from Vite env. The API defaults to the same origin as the app (/v1 is proxied in dev). */
export const appConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? window.location.origin,
  clientId: `web/${import.meta.env.VITE_APP_VERSION ?? '0.1.0'}/browser`,
  defaultLocale: import.meta.env.VITE_DEFAULT_LOCALE ?? 'en',
  /** School code pre-filled on sign-in for single-tenant hosts (tenant is otherwise resolved from the host). */
  tenantCode: import.meta.env.VITE_TENANT_CODE ?? '',
};
