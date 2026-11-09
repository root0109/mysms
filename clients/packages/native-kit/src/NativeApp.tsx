import {
  MySmsProvider,
  useI18n,
  usePreferences,
  useSession,
  type ClientKind,
  type Runtime,
} from '@mysms/core';
import { Screen, Text, ThemeProvider } from '@mysms/ui-native';
import { useEffect, useState, type ReactNode } from 'react';
import { HomeScreen } from './HomeScreen';
import { ProfileScreen } from './ProfileScreen';
import { SettingsScreen } from './SettingsScreen';
import { SignInScreen } from './SignInScreen';
import { useDeepLinks } from './useDeepLinks';

export interface NativeAppProps {
  runtime: Runtime;
  client: ClientKind;
  webOrigin: string;
  defaultTenantCode?: string;
  tablet?: boolean;
}

export type Page = 'home' | 'profile' | 'settings';
/** Deep-linked screens this kit renders itself (ui/02: ACC-07 profile, ACC-14 settings). */
const PAGE_OF_SCREEN: Readonly<Record<string, Page>> = { 'ACC-07': 'profile', 'ACC-14': 'settings' };

function Root({ client, webOrigin, defaultTenantCode, tablet }: Omit<NativeAppProps, 'runtime'>) {
  const status = useSession((s) => s.status);
  const linked = useDeepLinks(client, webOrigin);
  const [page, setPage] = useState<Page>('home');
  useEffect(() => {
    const target = linked ? PAGE_OF_SCREEN[linked.screen.id] : undefined;
    if (target) setPage(target);
  }, [linked]);
  if (status !== 'signedIn')
    return <SignInScreen defaultTenantCode={defaultTenantCode ?? ''} tablet={tablet ?? false} />;
  const home = () => setPage('home');
  if (page === 'profile') return <ProfileScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'settings') return <SettingsScreen tablet={tablet ?? false} onBack={home} />;
  return <HomeScreen linked={linked} tablet={tablet ?? false} onOpen={setPage} />;
}

/** The theme follows the signed-in user's settings: theme and text size (ui/04 §9). */
function PreferencesTheme({ children }: { children: ReactNode }) {
  const preferences = usePreferences();
  const mode =
    preferences.theme === 'system'
      ? undefined
      : preferences.theme === 'high_contrast'
        ? 'highContrast'
        : preferences.theme;
  return (
    <ThemeProvider mode={mode} textScale={preferences.textScale / 100}>
      {children}
    </ThemeProvider>
  );
}

/** Root of an Expo app: runtime, theme, session restore from secure storage, then the app. */
export function NativeApp({ runtime, ...rest }: NativeAppProps) {
  const [ready, setReady] = useState(false);
  useEffect(() => {
    let live = true;
    runtime.session
      .restore()
      .catch(() => false)
      .finally(() => live && setReady(true));
    return () => {
      live = false;
    };
  }, [runtime]);
  return (
    <MySmsProvider runtime={runtime}>
      <PreferencesTheme>
        {ready ? <Root {...rest} /> : <Booting tablet={rest.tablet ?? false} />}
      </PreferencesTheme>
    </MySmsProvider>
  );
}

function Booting({ tablet }: { tablet: boolean }) {
  const { t } = useI18n();
  return (
    <Screen tablet={tablet}>
      <Text muted>{t('common.state.loading')}</Text>
    </Screen>
  );
}
