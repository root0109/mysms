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
import {
  PasswordResetScreen,
  PersonaScreen,
  SignUpScreen,
  TermsScreen,
  VerifyScreen,
} from './AccountScreens';
import { LeaveBalanceScreen } from './LeaveBalanceScreen';
import { LeaveScreen } from './LeaveScreen';
import { NotificationPreferencesScreen } from './NotificationPreferencesScreen';
import { PayslipsScreen } from './PayslipsScreen';
import { FeesScreen } from './FeesScreen';
import { ProfileScreen } from './ProfileScreen';
import { SettingsScreen } from './SettingsScreen';
import { SignInScreen } from './SignInScreen';
import { ChildrenScreen, DisciplineScreen, StudentSummaryScreen } from './StudentScreens';
import { SubjectRegistrationScreen } from './SubjectRegistrationScreen';
import { TimetableScreen } from './TimetableScreen';
import { AssessmentScreen } from './AssessmentScreen';
import { LmsScreen } from './LmsScreen';
import { isAssessmentScreenId, type AssessmentRoute } from './assessmentRoutes';
import { isLmsScreenId, type LmsRoute } from './lmsRoutes';
import { useDeepLinks } from './useDeepLinks';

export interface NativeAppProps {
  runtime: Runtime;
  client: ClientKind;
  webOrigin: string;
  defaultTenantCode?: string;
  tablet?: boolean;
}

export type Page =
  | 'home'
  | 'profile'
  | 'settings'
  | 'notifications'
  | 'timetable'
  | 'leave'
  | 'children'
  | 'student'
  | 'discipline'
  | 'registration'
  | 'leaveBalance'
  | 'payslips'
  | 'fees'
  | 'assessment'
  | 'lms'
  | 'verify'
  | 'persona';
/**
 * The pages before sign-in: sign in, a password reset (ACC-02), on the tablet sign-up (ACC-03), and the course
 * catalogue read as a guest of the app's school (LMS-01, LMS-02 and preview lessons).
 */
type SignedOutPage = 'signIn' | 'reset' | 'signUp' | 'catalog';
/** Deep-linked screens this kit renders itself (ui/02: ACC-07 profile, ACC-14 settings, TT-03 my timetable). */
const PAGE_OF_SCREEN: Readonly<Record<string, Page>> = {
  'ACC-05': 'verify',
  'ACC-07': 'profile',
  'ACC-10': 'notifications',
  'ACC-14': 'settings',
  'TT-03': 'timetable',
  'HR-03': 'leaveBalance',
  'HR-04': 'leave',
  'HR-08': 'payslips',
  'FIN-07': 'fees',
  'FIN-08': 'fees',
  'SIS-02': 'student',
  'SIS-06': 'discipline',
  'SIS-10': 'registration',
  'SIS-11': 'children',
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
  'LMS-01': 'lms',
  'LMS-02': 'lms',
  'LMS-04': 'lms',
  'LMS-05': 'lms',
  'LMS-06': 'lms',
  'LMS-07': 'lms',
  'LMS-13': 'lms',
  'LMS-14': 'lms',
  'LMS-15': 'lms',
  'LMS-20': 'lms',
  'LMS-21': 'lms',
  'LMS-22': 'lms',
  'LMS-23': 'lms',
  'LMS-25': 'lms',
  'LMS-26': 'lms',
  'LMS-27': 'lms',
  'LMS-08': 'lms',
  'LMS-09': 'lms',
  'LMS-10': 'lms',
  'LMS-11': 'lms',
  'LMS-31': 'lms',
  'LMS-32': 'lms',
  'LMS-33': 'lms',
  'LMS-54': 'lms',
};

function Root({ client, webOrigin, defaultTenantCode, tablet }: Omit<NativeAppProps, 'runtime'>) {
  const status = useSession((s) => s.status);
  const linked = useDeepLinks(client, webOrigin);
  const [page, setPage] = useState<Page>('home');
  // the student a summary is about (SIS-02 from a link, a child card or an incident), and where to go back to
  const [student, setStudent] = useState<{ id: string; from: Page } | null>(null);
  const [assessmentRoute, setAssessmentRoute] = useState<AssessmentRoute | null>(null);
  const [assessmentHistory, setAssessmentHistory] = useState<AssessmentRoute[]>([]);
  const [lmsRoute, setLmsRoute] = useState<LmsRoute | null>(null);
  const [lmsHistory, setLmsHistory] = useState<LmsRoute[]>([]);
  const [signedOutPage, setSignedOutPage] = useState<SignedOutPage>('signIn');
  // ACC-04: the terms come before anything else, for a user who has not accepted them
  const termsPending = useSession((s) => Boolean(s.me?.user) && !s.me?.user?.termsAcceptedAt);
  useEffect(() => {
    const target = linked ? PAGE_OF_SCREEN[linked.screen.id] : undefined;
    if (!linked || !target) return;
    if (target === 'student') {
      if (!linked.params.studentId) return;
      setStudent({ id: linked.params.studentId, from: 'home' });
    }
    if (target === 'assessment') {
      if (!isAssessmentScreenId(linked.screen.id)) return;
      setAssessmentHistory([]);
      setAssessmentRoute({ screenId: linked.screen.id, params: linked.params });
    }
    if (target === 'lms') {
      if (!isLmsScreenId(linked.screen.id)) return;
      setLmsHistory([]);
      setLmsRoute({ screenId: linked.screen.id, params: linked.params });
    }
    setPage(target);
  }, [linked]);
  const lmsScreen = (onHome: () => void, onSignIn?: () => void) =>
    lmsRoute ? (
      <LmsScreen
        route={lmsRoute}
        tablet={tablet ?? false}
        onBack={() => {
          const previous = lmsHistory[lmsHistory.length - 1];
          if (!previous) return onHome();
          setLmsHistory((history) => history.slice(0, -1));
          setLmsRoute(previous);
        }}
        onNavigate={(route) => {
          setLmsHistory((history) => [...history, lmsRoute]);
          setLmsRoute(route);
        }}
        {...(onSignIn ? { onSignIn } : {})}
      />
    ) : null;
  if (status !== 'signedIn') {
    const toSignIn = () => setSignedOutPage('signIn');
    if (signedOutPage === 'catalog') return lmsScreen(toSignIn, toSignIn);
    if (signedOutPage === 'reset')
      return (
        <PasswordResetScreen
          defaultTenantCode={defaultTenantCode ?? ''}
          tablet={tablet ?? false}
          onBack={toSignIn}
        />
      );
    if (signedOutPage === 'signUp' && tablet)
      return (
        <SignUpScreen
          defaultTenantCode={defaultTenantCode ?? ''}
          tablet
          onBack={toSignIn}
          onEnterCode={() => setSignedOutPage('reset')}
        />
      );
    return (
      <SignInScreen
        defaultTenantCode={defaultTenantCode ?? ''}
        tablet={tablet ?? false}
        onForgot={() => setSignedOutPage('reset')}
        {...(tablet ? { onSignUp: () => setSignedOutPage('signUp') } : {})}
        {...(defaultTenantCode
          ? {
              onBrowseCourses: () => {
                setLmsHistory([]);
                setLmsRoute({ screenId: 'LMS-01', params: {} });
                setSignedOutPage('catalog');
              },
            }
          : {})}
      />
    );
  }
  if (termsPending) return <TermsScreen tablet={tablet ?? false} />;
  const home = () => setPage('home');
  const openStudent = (from: Page) => (id: string) => {
    setStudent({ id, from });
    setPage('student');
  };
  const backFromAssessment = () => {
    const previous = assessmentHistory[assessmentHistory.length - 1];
    if (!previous) return home();
    setAssessmentHistory((history) => history.slice(0, -1));
    setAssessmentRoute(previous);
  };
  if (page === 'profile') return <ProfileScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'settings') return <SettingsScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'leave') return <LeaveScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'leaveBalance')
    return <LeaveBalanceScreen tablet={tablet ?? false} onBack={home} onApply={() => setPage('leave')} />;
  if (page === 'payslips') return <PayslipsScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'fees') return <FeesScreen webOrigin={webOrigin} tablet={tablet ?? false} onBack={home} />;
  if (page === 'timetable') return <TimetableScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'children')
    return <ChildrenScreen tablet={tablet ?? false} onOpen={openStudent('children')} onBack={home} />;
  if (page === 'discipline')
    return <DisciplineScreen tablet={tablet ?? false} onOpen={openStudent('discipline')} onBack={home} />;
  if (page === 'registration') return <SubjectRegistrationScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'student' && student)
    return (
      <StudentSummaryScreen
        studentId={student.id}
        tablet={tablet ?? false}
        onBack={() => setPage(student.from)}
      />
    );
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
  if (page === 'lms' && lmsRoute) return lmsScreen(home);
  if (page === 'notifications')
    return <NotificationPreferencesScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'verify') return <VerifyScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'persona') return <PersonaScreen tablet={tablet ?? false} onBack={home} />;
  return (
    <HomeScreen
      linked={linked}
      tablet={tablet ?? false}
      onOpen={setPage}
      onOpenAssessment={(screenId, params = {}) => {
        setAssessmentRoute({ screenId, params });
        setAssessmentHistory([]);
        setPage('assessment');
      }}
      onOpenLms={(route) => {
        setLmsRoute(route);
        setLmsHistory([]);
        setPage('lms');
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
