import type { ConfigContext, ExpoConfig } from 'expo/config';

/** B — the tablet learning app: landscape-first (ui/04 §3); the offline store (ui/05 §7) is its next increment. */
export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: 'MySMS Learn',
  slug: 'sms-tablet',
  scheme: 'mysms',
  version: '0.1.0',
  orientation: 'landscape',
  userInterfaceStyle: 'automatic',
  // the web client is the separate React app; Expo builds the native apps only
  platforms: ['ios', 'android'],
  ios: { bundleIdentifier: 'com.mysms.learn', supportsTablet: true, requireFullScreen: false },
  android: { package: 'com.mysms.learn' },
  plugins: ['expo-secure-store'],
  extra: { client: 'B' },
});
