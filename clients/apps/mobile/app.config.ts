import type { ConfigContext, ExpoConfig } from 'expo/config';

/** Three store apps from one project (ui/05 §1): APP_VARIANT=student|parent|teacher picks id, name and client. */
const VARIANTS = {
  student: { name: 'MySMS Student', id: 'com.mysms.student', client: 'S' },
  parent: { name: 'MySMS Parent', id: 'com.mysms.parent', client: 'P' },
  teacher: { name: 'MySMS Teacher', id: 'com.mysms.teacher', client: 'T' },
} as const;

type Variant = keyof typeof VARIANTS;
const key = (process.env.APP_VARIANT ?? 'student') as Variant;
const variant = VARIANTS[key];
if (!variant) throw new Error(`unknown APP_VARIANT ${process.env.APP_VARIANT}`);

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: variant.name,
  slug: `sms-${key}`,
  scheme: 'mysms',
  version: '0.1.0',
  orientation: 'portrait',
  userInterfaceStyle: 'automatic',
  // the web client is the separate React app; Expo builds the native apps only
  platforms: ['ios', 'android'],
  ios: { bundleIdentifier: variant.id, supportsTablet: false },
  android: { package: variant.id },
  plugins: ['expo-secure-store'],
  extra: { client: variant.client },
});
