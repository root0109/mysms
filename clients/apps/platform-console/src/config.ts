/** Runtime configuration from Vite env. The console talks to the same API with platform-operator tokens. */
export const appConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? window.location.origin,
  clientId: `console/${import.meta.env.VITE_APP_VERSION ?? '0.1.0'}/browser`,
  defaultLocale: import.meta.env.VITE_DEFAULT_LOCALE ?? 'en',
  tenantCode: import.meta.env.VITE_TENANT_CODE ?? '',
};
