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
import { LeaveScreen } from './LeaveScreen';
import { NotificationPreferencesScreen } from './NotificationPreferencesScreen';
import { ProfileScreen } from './ProfileScreen';
import { SettingsScreen } from './SettingsScreen';
import { SignInScreen } from './SignInScreen';
import { TimetableScreen } from './TimetableScreen';
import { AssessmentScreen } from './AssessmentScreen';
import { isAssessmentScreenId, type AssessmentRoute } from './assessmentRoutes';
import { useDeepLinks } from './useDeepLinks';

export interface NativeAppProps {
  runtime: Runtime;
  client: ClientKind;
  webOrigin: string;
  defaultTenantCode?: string;
  tablet?: boolean;
}

export type Page = 'home' | 'profile' | 'settings' | 'notifications' | 'timetable' | 'leave' | 'assessment';
/** Deep-linked screens this kit renders itself (ui/02: ACC-07 profile, ACC-14 settings, TT-03 my timetable). */
const PAGE_OF_SCREEN: Readonly<Record<string, Page>> = {
  'ACC-07': 'profile',
  'ACC-10': 'notifications',
  'ACC-14': 'settings',
  'TT-03': 'timetable',
  'HR-04': 'leave',
  'ASM-01': 'assessment',
  'ASM-02': 'assessment',
  'ASM-03': 'assessment',
  'ASM-04': 'assessment',
  'ASM-05': 'assessment',
  'ASM-06': 'assessment',
  'ASM-07': 'assessment',
  'ASM-08': 'assessment',
  'ASM-09': 'assessment',
  'ASM-10': 'assessment',
  'ASM-11': 'assessment',
  'ASM-12': 'assessment',
  'ASM-13': 'assessment',
};

function Root({ client, webOrigin, defaultTenantCode, tablet }: Omit<NativeAppProps, 'runtime'>) {
  const status = useSession((s) => s.status);
  const linked = useDeepLinks(client, webOrigin);
  const [page, setPage] = useState<Page>('home');
  const [assessmentRoute, setAssessmentRoute] = useState<AssessmentRoute | null>(null);
  const [assessmentHistory, setAssessmentHistory] = useState<AssessmentRoute[]>([]);
  useEffect(() => {
    if (!linked) return;
    const target = PAGE_OF_SCREEN[linked.screen.id];
    if (!target) return;
    setPage(target);
    if (target === 'assessment' && isAssessmentScreenId(linked.screen.id)) {
      setAssessmentHistory([]);
      setAssessmentRoute({ screenId: linked.screen.id, params: linked.params });
    }
  }, [linked]);
  if (status !== 'signedIn')
    return <SignInScreen defaultTenantCode={defaultTenantCode ?? ''} tablet={tablet ?? false} />;
  const home = () => setPage('home');
  const backFromAssessment = () => {
    const previous = assessmentHistory[assessmentHistory.length - 1];
    if (!previous) return home();
    setAssessmentHistory((history) => history.slice(0, -1));
    setAssessmentRoute(previous);
  };
  if (page === 'profile') return <ProfileScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'settings') return <SettingsScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'leave') return <LeaveScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'timetable') return <TimetableScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'assessment' && assessmentRoute)
    return (
      <AssessmentScreen
        route={assessmentRoute}
        client={client}
        tablet={tablet ?? false}
        onBack={backFromAssessment}
        onNavigate={(route) => {
          if (!assessmentRoute) return;
          setAssessmentHistory((history) => [...history, assessmentRoute]);
          setAssessmentRoute(route);
        }}
      />
    );
  if (page === 'notifications')
    return <NotificationPreferencesScreen tablet={tablet ?? false} onBack={home} />;
  return (
    <HomeScreen
      linked={linked}
      tablet={tablet ?? false}
      onOpen={setPage}
      onOpenAssessment={(screenId) => {
        setAssessmentRoute({ screenId, params: {} });
        setAssessmentHistory([]);
        setPage('assessment');
      }}
    />
  );
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
