import {
  MySmsProvider,
  resolveLink,
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
import { ConsentCentreScreen } from './ConsentCentreScreen';
import { MyRequestsScreen } from './MyRequestsScreen';
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
import { SheetsScreen, StudentMonthScreen, TakeAttendanceScreen } from './AttendanceScreens';
import {
  CalendarScreen,
  ClassFeedScreen,
  ConversationsScreen,
  EventsScreen,
  FeedbackScreen,
  InboxScreen,
  NoticesScreen,
  TasksScreen,
} from './CommsScreens';
import { ClassWorkspaceScreen, MyLoansScreen, MyRouteScreen, ReservationsScreen } from './ServicesScreens';

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
  | 'consents'
  | 'privacyRequests'
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
  | 'persona'
  | 'sheets'
  | 'takeAttendance'
  | 'myAttendance'
  | 'inbox'
  | 'notices'
  | 'calendar'
  | 'conversations'
  | 'events'
  | 'feedback'
  | 'feed'
  | 'tasks'
  | 'myLoans'
  | 'reservations'
  | 'myRoute'
  | 'classWorkspace';
/**
 * The pages before sign-in: sign in, a password reset (ACC-02), on the tablet sign-up (ACC-03), and the course
 * catalogue read as a guest of the app's school (LMS-01, LMS-02 and preview lessons).
 */
type SignedOutPage = 'signIn' | 'reset' | 'signUp' | 'catalog';
/**
 * Deep-linked screens this kit renders itself (ui/02: ACC-07 profile, ACC-13 consent centre, ACC-14 settings, ACC-15
 * privacy requests, TT-03 my timetable).
 */
const PAGE_OF_SCREEN: Readonly<Record<string, Page>> = {
  'ACC-05': 'verify',
  'ACC-07': 'profile',
  'ACC-10': 'notifications',
  'ACC-13': 'consents',
  'ACC-14': 'settings',
  'ACC-15': 'privacyRequests',
  'TT-03': 'timetable',
  'ATT-02': 'sheets',
  'ATT-03': 'takeAttendance',
  'ATT-04': 'myAttendance',
  'ACA-06': 'classWorkspace',
  'ACC-09': 'inbox',
  'COM-01': 'notices',
  'COM-02': 'notices',
  'COM-03': 'calendar',
  'COM-04': 'events',
  'COM-05': 'conversations',
  'COM-07': 'feedback',
  'COM-08': 'feed',
  'COM-09': 'tasks',
  'LIB-05': 'reservations',
  'LIB-07': 'myLoans',
  'TRN-03': 'myRoute',
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
  // the record a page is about (a sheet, a student), from a deep link or the page that opened it
  const [pageParams, setPageParams] = useState<Readonly<Record<string, string>>>({});
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
    setPageParams(linked.params);
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
  const open = (target: Page, params: Readonly<Record<string, string>> = {}) => {
    setPageParams(params);
    setPage(target);
  };
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
  if (page === 'consents') return <ConsentCentreScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'privacyRequests') return <MyRequestsScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'verify') return <VerifyScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'persona') return <PersonaScreen tablet={tablet ?? false} onBack={home} />;
  if (page === 'sheets')
    return (
      <SheetsScreen
        tablet={tablet ?? false}
        onOpen={(attendanceSheetId) => open('takeAttendance', { attendanceSheetId })}
        onBack={home}
      />
    );
  if (page === 'takeAttendance' && pageParams.attendanceSheetId)
    return (
      <TakeAttendanceScreen
        sheetId={pageParams.attendanceSheetId}
        tablet={tablet ?? false}
        onBack={() => open('sheets')}
      />
    );
  const plain = { tablet: tablet ?? false, onBack: home };
  if (page === 'inbox')
    return (
      <InboxScreen
        client={client}
        {...plain}
        onOpenLink={(link) => {
          const resolved = resolveLink(link, client, webOrigin);
          if (resolved.kind !== 'screen') return;
          const target = PAGE_OF_SCREEN[resolved.match.screen.id];
          if (target === 'lms' && isLmsScreenId(resolved.match.screen.id)) {
            setLmsHistory([]);
            setLmsRoute({ screenId: resolved.match.screen.id, params: resolved.match.params });
          }
          if (target === 'assessment' && isAssessmentScreenId(resolved.match.screen.id)) {
            setAssessmentHistory([]);
            setAssessmentRoute({ screenId: resolved.match.screen.id, params: resolved.match.params });
          }
          if (target === 'student' && resolved.match.params.studentId)
            setStudent({ id: resolved.match.params.studentId, from: 'inbox' });
          if (target) open(target, resolved.match.params);
        }}
      />
    );
  if (page === 'notices') return <NoticesScreen {...plain} />;
  if (page === 'calendar') return <CalendarScreen {...plain} />;
  if (page === 'conversations') return <ConversationsScreen {...plain} />;
  if (page === 'events') return <EventsScreen {...plain} />;
  if (page === 'feedback') return <FeedbackScreen {...plain} />;
  if (page === 'feed') return <ClassFeedScreen {...plain} />;
  if (page === 'tasks') return <TasksScreen {...plain} />;
  if (page === 'myLoans') return <MyLoansScreen {...plain} />;
  if (page === 'reservations') return <ReservationsScreen {...plain} />;
  if (page === 'myRoute') return <MyRouteScreen {...plain} />;
  if (page === 'classWorkspace') return <ClassWorkspaceScreen {...plain} />;
  if (page === 'myAttendance')
    return (
      <StudentMonthScreen
        {...(pageParams.studentId ? { studentId: pageParams.studentId } : {})}
        tablet={tablet ?? false}
        onBack={home}
      />
    );
  return (
    <HomeScreen
      linked={linked}
      tablet={tablet ?? false}
      onOpen={open}
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
