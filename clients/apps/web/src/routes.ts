import {
  buildAppRoutes,
  HomeScreen,
  RESOURCE_SCREENS,
  ResourceScreen,
  ScreenLoading,
  SignInScreen,
} from '@mysms/web-kit';
import { createElement, lazy, Suspense, type ComponentType, type ReactNode } from 'react';
import { appConfig } from './config';
import { WebShell } from './shell/WebShell';

/**
 * A built screen that loads on demand: each purpose-built screen is its own chunk, so the initial route carries only
 * the shell, sign-in and the list managers (ui/05 §10, initial JS ≤ 300 KB gzip).
 */
function onDemand<K extends string>(load: () => Promise<Record<K, ComponentType>>, name: K): ReactNode {
  const Screen = lazy(() => load().then((module) => ({ default: module[name] })));
  return createElement(Suspense, { fallback: createElement(ScreenLoading) }, createElement(Screen));
}

/** A built screen that shows the tenant's branding: it is given the school code this deployment serves. */
function platformScreen<K extends string>(
  load: () => Promise<Record<K, ComponentType<{ tenantCode?: string }>>>,
  name: K,
): ReactNode {
  const Screen = lazy(() =>
    load().then((module) => ({ default: module[name] as ComponentType<{ tenantCode?: string }> })),
  );
  return createElement(
    Suspense,
    { fallback: createElement(ScreenLoading) },
    createElement(Screen, { tenantCode: appConfig.tenantCode }),
  );
}

/** The list-manager screens of the inventory, one per record type (web-kit `RESOURCE_SCREENS`). */
const resourceScreens: Record<string, ReactNode> = Object.fromEntries(
  RESOURCE_SCREENS.map((screen) => [screen.id, createElement(ResourceScreen, { screen, key: screen.id })]),
);

/** W: every web screen of the inventory; built ones listed here, the rest show their placeholder. */
export const routes = buildAppRoutes({
  client: 'W',
  Shell: WebShell,
  Home: HomeScreen,
  built: {
    ...resourceScreens,
    'ACC-01': createElement(SignInScreen, { defaultTenantCode: appConfig.tenantCode }),
    'ACC-02': onDemand(
      () =>
        import('@mysms/web-kit/screens/account/PasswordResetScreen').then((m) => ({
          PasswordResetScreen: () =>
            createElement(m.PasswordResetScreen, { defaultTenantCode: appConfig.tenantCode }),
        })),
      'PasswordResetScreen',
    ),
    'ACC-03': onDemand(
      () =>
        import('@mysms/web-kit/screens/account/SignUpScreen').then((m) => ({
          SignUpScreen: () => createElement(m.SignUpScreen, { defaultTenantCode: appConfig.tenantCode }),
        })),
      'SignUpScreen',
    ),
    'ACC-04': onDemand(() => import('@mysms/web-kit/screens/account/TermsScreen'), 'TermsScreen'),
    'ACC-05': onDemand(() => import('@mysms/web-kit/screens/account/VerifyScreen'), 'VerifyScreen'),
    'ACC-07': onDemand(() => import('@mysms/web-kit/screens/ProfileScreen'), 'ProfileScreen'),
    'ACC-08': onDemand(() => import('@mysms/web-kit/screens/SecurityScreen'), 'SecurityScreen'),
    'ACC-10': onDemand(
      () => import('@mysms/web-kit/screens/NotificationPreferencesScreen'),
      'NotificationPreferencesScreen',
    ),
    'ACC-13': onDemand(
      () => import('@mysms/web-kit/screens/privacy/ConsentCentreScreen'),
      'ConsentCentreScreen',
    ),
    'ACC-14': onDemand(() => import('@mysms/web-kit/screens/SettingsScreen'), 'SettingsScreen'),
    'ACC-15': onDemand(() => import('@mysms/web-kit/screens/privacy/MyRequestsScreen'), 'MyRequestsScreen'),
    'ADM-17': onDemand(() => import('@mysms/web-kit/screens/privacy/PurposesScreen'), 'PurposesScreen'),
    'ADM-18': onDemand(() => import('@mysms/web-kit/screens/privacy/NoticesScreen'), 'NoticesScreen'),
    'ADM-19': onDemand(
      () => import('@mysms/web-kit/screens/privacy/RightsRequestsScreen'),
      'RightsRequestsScreen',
    ),
    'ADM-20': onDemand(() => import('@mysms/web-kit/screens/privacy/BreachesScreen'), 'BreachesScreen'),
    'ADM-21': onDemand(() => import('@mysms/web-kit/screens/privacy/LegalHoldsScreen'), 'LegalHoldsScreen'),
    'ADM-22': onDemand(() => import('@mysms/web-kit/screens/privacy/RetentionScreen'), 'RetentionScreen'),
    'LMS-15': onDemand(() => import('@mysms/web-kit/screens/lms/CertificatesScreen'), 'CertificatesScreen'),
    'LMS-16': onDemand(
      () => import('@mysms/web-kit/screens/lms/EvaluationBookingScreen'),
      'EvaluationBookingScreen',
    ),
    'LMS-20': onDemand(
      () => import('@mysms/web-kit/screens/lms/TeachingDashboardScreen'),
      'TeachingDashboardScreen',
    ),
    'LMS-21': onDemand(() => import('@mysms/web-kit/screens/lms/CohortHomeScreen'), 'CohortHomeScreen'),
    'LMS-22': onDemand(() => import('@mysms/web-kit/screens/lms/CohortOutlineScreen'), 'CohortOutlineScreen'),
    'LMS-23': onDemand(() => import('@mysms/web-kit/screens/lms/CohortRosterScreen'), 'CohortRosterScreen'),
    'LMS-24': onDemand(() => import('@mysms/web-kit/screens/lms/CohortGroupsScreen'), 'CohortGroupsScreen'),
    'LMS-25': onDemand(
      () => import('@mysms/web-kit/screens/lms/CohortScheduleScreen'),
      'CohortScheduleScreen',
    ),
    'LMS-26': onDemand(() => import('@mysms/web-kit/screens/lms/ForumsScreen'), 'ForumsScreen'),
    'LMS-27': onDemand(() => import('@mysms/web-kit/screens/lms/LiveClassesScreen'), 'LiveClassesScreen'),
    'LMS-29': onDemand(() => import('@mysms/web-kit/screens/lms/ReleaseRulesScreen'), 'ReleaseRulesScreen'),
    'LMS-30': onDemand(
      () => import('@mysms/web-kit/screens/lms/CohortAnalyticsScreen'),
      'CohortAnalyticsScreen',
    ),
    'ASM-03': onDemand(
      () => import('@mysms/web-kit/screens/homework/HomeworkCheckScreen'),
      'HomeworkCheckScreen',
    ),
    'ATT-03': onDemand(
      () => import('@mysms/web-kit/screens/attendance/AttendanceSheetScreen'),
      'AttendanceSheetScreen',
    ),
    'ATT-04': onDemand(
      () => import('@mysms/web-kit/screens/attendance/StudentAttendanceScreen'),
      'StudentAttendanceScreen',
    ),
    'ATT-05': onDemand(
      () => import('@mysms/web-kit/screens/attendance/AttendanceSummaryScreen'),
      'AttendanceSummaryScreen',
    ),
    'TT-03': onDemand(
      () => import('@mysms/web-kit/screens/timetable/MyTimetableScreen'),
      'MyTimetableScreen',
    ),
    'HR-01': onDemand(
      () => import('@mysms/web-kit/screens/staff/StaffDirectoryScreen'),
      'StaffDirectoryScreen',
    ),
    'HR-02': onDemand(() => import('@mysms/web-kit/screens/staff/StaffDetailScreen'), 'StaffDetailScreen'),
    'HR-03': onDemand(
      () => import('@mysms/web-kit/screens/leave/LeaveBalancesScreen'),
      'LeaveBalancesScreen',
    ),
    'HR-04': onDemand(() => import('@mysms/web-kit/screens/leave/StaffLeaveScreen'), 'StaffLeaveScreen'),
    'HR-05': onDemand(
      () => import('@mysms/web-kit/screens/payroll/SalaryTemplatesScreen'),
      'SalaryTemplatesScreen',
    ),
    'HR-06': onDemand(() => import('@mysms/web-kit/screens/payroll/StaffSalaryScreen'), 'StaffSalaryScreen'),
    'HR-07': onDemand(() => import('@mysms/web-kit/screens/payroll/PayslipRunScreen'), 'PayslipRunScreen'),
    'HR-08': onDemand(() => import('@mysms/web-kit/screens/payroll/MyPayslipsScreen'), 'MyPayslipsScreen'),
    'HR-09': onDemand(
      () => import('@mysms/web-kit/screens/payroll/PayrollPaymentsScreen'),
      'PayrollPaymentsScreen',
    ),
    'SIS-02': onDemand(() => import('@mysms/web-kit/screens/students/StudentScreen'), 'StudentScreen'),
    'SIS-03': onDemand(() => import('@mysms/web-kit/screens/students/PromotionScreen'), 'PromotionScreen'),
    'ADMN-01': onDemand(
      () => import('@mysms/web-kit/screens/admission/AdmissionCyclesScreen'),
      'AdmissionCyclesScreen',
    ),
    'ADMN-02': onDemand(
      () => import('@mysms/web-kit/screens/admission/ApplicationsPipelineScreen'),
      'ApplicationsPipelineScreen',
    ),
    'ADMN-03': onDemand(
      () => import('@mysms/web-kit/screens/admission/ApplicationDetailScreen'),
      'ApplicationDetailScreen',
    ),
    'ADMN-05': onDemand(() => import('@mysms/web-kit/screens/admission/ApplyScreen'), 'ApplyScreen'),
    'ADM-08': onDemand(() => import('@mysms/web-kit/screens/users/UsersScreen'), 'UsersScreen'),
    'ADM-09': onDemand(() => import('@mysms/web-kit/screens/users/UserDetailScreen'), 'UserDetailScreen'),
    'ADM-11': onDemand(() => import('@mysms/web-kit/screens/RecordsScreen'), 'RecordsScreen'),
    'ADM-07': platformScreen(() => import('@mysms/web-kit/screens/platform/ThemesScreen'), 'ThemesScreen'),
    'ADM-12': onDemand(
      () => import('@mysms/web-kit/screens/platform/ImportsExportsScreen'),
      'ImportsExportsScreen',
    ),
    'ADM-13': onDemand(() => import('@mysms/web-kit/screens/platform/AuditLogScreen'), 'AuditLogScreen'),
    'ADM-14': onDemand(
      () => import('@mysms/web-kit/screens/platform/SubscriptionScreen'),
      'SubscriptionScreen',
    ),
    'ADM-15': platformScreen(
      () => import('@mysms/web-kit/screens/platform/TenantSettingsScreen'),
      'TenantSettingsScreen',
    ),
    'ACC-11': onDemand(
      () => import('@mysms/web-kit/screens/platform/GlobalSearchScreen'),
      'GlobalSearchScreen',
    ),
    'ADM-16': onDemand(() => import('@mysms/web-kit/screens/SettingValuesScreen'), 'SettingValuesScreen'),
    'ASM-06': onDemand(
      () => import('@mysms/web-kit/screens/assessment/SubmissionScreens'),
      'AssignmentDetailScreen',
    ),
    'ASM-10': onDemand(
      () => import('@mysms/web-kit/screens/assessment/AssessmentScreens'),
      'ExamMarksScreen',
    ),
    'ASM-11': onDemand(
      () => import('@mysms/web-kit/screens/assessment/AssessmentScreens'),
      'MarksheetsScreen',
    ),
    'ASM-12': onDemand(
      () => import('@mysms/web-kit/screens/assessment/AssessmentScreens'),
      'StudentResultsScreen',
    ),
    'ASM-13': onDemand(
      () => import('@mysms/web-kit/screens/assessment/GradingSchemesScreen'),
      'GradingSchemesScreen',
    ),
    'FIN-01': onDemand(() => import('@mysms/web-kit/screens/finance/FeeSetupScreen'), 'FeeSetupScreen'),
    'FIN-02': onDemand(() => import('@mysms/web-kit/screens/finance/FeeDuesScreens'), 'GenerateDuesScreen'),
    'FIN-03': onDemand(() => import('@mysms/web-kit/screens/finance/FeeDuesScreens'), 'DuesLedgerScreen'),
    'FIN-04': onDemand(() => import('@mysms/web-kit/screens/finance/InvoiceScreens'), 'InvoicesScreen'),
    'FIN-05': onDemand(() => import('@mysms/web-kit/screens/finance/InvoiceScreens'), 'InvoiceDetailScreen'),
    'FIN-06': onDemand(() => import('@mysms/web-kit/screens/finance/PaymentsScreen'), 'PaymentsScreen'),
    'FIN-07': onDemand(() => import('@mysms/web-kit/screens/finance/FamilyFeesScreens'), 'PayFeesScreen'),
    'FIN-08': onDemand(
      () => import('@mysms/web-kit/screens/finance/FamilyFeesScreens'),
      'FeeStatementScreen',
    ),
    'FIN-10': onDemand(() => import('@mysms/web-kit/screens/finance/RemindersScreen'), 'RemindersScreen'),
    'LMS-08': onDemand(() => import('@mysms/web-kit/screens/lms/QuizPlayerScreen'), 'QuizPlayerScreen'),
    'LMS-09': onDemand(() => import('@mysms/web-kit/screens/lms/QuizResultScreen'), 'QuizResultScreen'),
    'LMS-10': onDemand(() => import('@mysms/web-kit/screens/lms/AssignmentScreen'), 'AssignmentScreen'),
    'LMS-11': onDemand(
      () => import('@mysms/web-kit/screens/lms/GroupWorkspaceScreen'),
      'GroupWorkspaceScreen',
    ),
    'LMS-12': onDemand(
      () => import('@mysms/web-kit/screens/lms/ProgrammingExerciseScreen'),
      'ProgrammingExerciseScreen',
    ),
    'LMS-31': onDemand(() => import('@mysms/web-kit/screens/lms/GradebookScreen'), 'GradebookScreen'),
    'LMS-32': onDemand(() => import('@mysms/web-kit/screens/lms/ItemGradingScreen'), 'ItemGradingScreen'),
    'LMS-33': onDemand(() => import('@mysms/web-kit/screens/lms/MyGradesScreen'), 'MyGradesScreen'),
    'LMS-43': onDemand(() => import('@mysms/web-kit/screens/lms/QuestionBankScreen'), 'QuestionBankScreen'),
    'LMS-44': onDemand(() => import('@mysms/web-kit/screens/lms/QuizBuilderScreen'), 'QuizBuilderScreen'),
    'LMS-45': onDemand(
      () => import('@mysms/web-kit/screens/lms/AssignmentBuilderScreen'),
      'AssignmentBuilderScreen',
    ),
    'LMS-46': onDemand(() => import('@mysms/web-kit/screens/lms/RubricBuilderScreen'), 'RubricBuilderScreen'),
    'LMS-49': onDemand(() => import('@mysms/web-kit/screens/lms/ExternalToolsScreen'), 'ExternalToolsScreen'),
    'LMS-51': onDemand(() => import('@mysms/web-kit/screens/lms/ImportExportScreen'), 'ImportExportScreen'),
    'LMS-52': onDemand(() => import('@mysms/web-kit/screens/lms/PlagiarismScreen'), 'PlagiarismScreen'),
    'LMS-54': onDemand(() => import('@mysms/web-kit/screens/lms/DriveScreen'), 'DriveScreen'),
    'LMS-01': onDemand(() => import('@mysms/web-kit/screens/lms/CatalogScreens'), 'CatalogHomeScreen'),
    'LMS-02': onDemand(() => import('@mysms/web-kit/screens/lms/CatalogScreens'), 'CoursePageScreen'),
    'LMS-04': onDemand(() => import('@mysms/web-kit/screens/lms/LearningScreens'), 'StudentDashboardScreen'),
    'LMS-05': onDemand(() => import('@mysms/web-kit/screens/lms/LearningScreens'), 'CourseHomeScreen'),
    'LMS-06': onDemand(() => import('@mysms/web-kit/screens/lms/LearningScreens'), 'LessonPlayerScreen'),
    'LMS-07': onDemand(() => import('@mysms/web-kit/screens/lms/LearningScreens'), 'ScormPlayerScreen'),
    'LMS-17': onDemand(() => import('@mysms/web-kit/screens/lms/LearningScreens'), 'LearningProgramsScreen'),
    'LMS-40': onDemand(() => import('@mysms/web-kit/screens/lms/AuthoringScreens'), 'MyCoursesScreen'),
    'LMS-41': onDemand(() => import('@mysms/web-kit/screens/lms/AuthoringScreens'), 'StructureEditorScreen'),
    'LMS-42': onDemand(() => import('@mysms/web-kit/screens/lms/AuthoringScreens'), 'LessonEditorScreen'),
    'LMS-50': onDemand(
      () => import('@mysms/web-kit/screens/lms/AuthoringScreens'),
      'CatalogPageEditorScreen',
    ),
    'LMS-13': onDemand(() => import('@mysms/web-kit/screens/lms/LibraryScreens'), 'EbookReaderScreen'),
    'LMS-14': onDemand(() => import('@mysms/web-kit/screens/lms/LibraryScreens'), 'NotesBookmarksScreen'),
    'LMS-47': onDemand(() => import('@mysms/web-kit/screens/lms/LibraryScreens'), 'RepositoryScreen'),
    'LMS-48': onDemand(() => import('@mysms/web-kit/screens/lms/LibraryScreens'), 'ResourcesAdminScreen'),
    'LMS-53': onDemand(() => import('@mysms/web-kit/screens/lms/LibraryScreens'), 'LmsSettingsScreen'),
  },
});
