// Expo inlines EXPO_PUBLIC_* variables at build time. The app variant comes from app.config.ts (expo-constants).
declare const process: {
  env: {
    readonly EXPO_PUBLIC_API_BASE_URL?: string;
    readonly EXPO_PUBLIC_WEB_ORIGIN?: string;
    readonly EXPO_PUBLIC_TENANT_CODE?: string;
  };
};
