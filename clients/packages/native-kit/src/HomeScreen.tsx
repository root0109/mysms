import { useCan, useI18n, useSession, useSignOut, type RouteMatch } from '@mysms/core';
import type { Page } from './NativeApp';
import { Button, Screen, Text } from '@mysms/ui-native';
import type { AssessmentScreenId } from './assessmentRoutes';
import type { LmsRoute } from './lmsRoutes';

/** The app home until the per-persona dashboards are built; shows where a deep link pointed. */
export function HomeScreen({
  linked,
  tablet = false,
  onOpen,
  onOpenAssessment,
  onOpenLms,
}: {
  linked: RouteMatch | null;
  tablet?: boolean;
  onOpen: (page: Exclude<Page, 'home' | 'student' | 'assessment' | 'lms'>) => void;
  onOpenAssessment: (screenId: AssessmentScreenId, params?: Record<string, string>) => void;
  onOpenLms: (route: LmsRoute) => void;
}) {
  const { t } = useI18n();
  const name = useSession((s) => s.me?.user?.username ?? '');
  const severalRoles = useSession((s) => (s.me?.roles?.length ?? 0) > 1);
  const unverifiedEmail = useSession((s) => Boolean(s.me?.user?.email) && !s.me?.user?.emailVerifiedAt);
  const signOut = useSignOut();
  const mayTimetable = useCan('timetable.session.get_timetable');
  const mayLms = useCan('lms.cohort.read');
  const mayMyCertificates = useCan('lms.certificate.read');
  const mayApplyForLeave = useCan('hr.leave_application.create');
  const mayApproveLeave = useCan('hr.leave_application.approve_leave');
  const mayLeaveBalance = useCan('hr.staff.get_leave_balance');
  const mayPayslips = useCan('payroll.payslip.read');
  const mayChildren = useCan('sis.guardian.list_my_children');
  const mayIncidents = useCan('sis.discipline_incident.read');
  const mayRegistrations = useCan('sis.subject_registration.read');
  const mayFees = useCan('finance.invoice.get_fee_statement');
  const ownStudentId = useSession((s) => s.me?.studentIds?.[0]);
  const isStudent = ownStudentId !== undefined;
  const mayReadHomework = useCan('assessment.homework.read');
  const mayApproveHomework = useCan('assessment.homework.approve_homework');
  const mayReadAssignments = useCan('assessment.assignment.read');
  const mayReadExamSessions = useCan('assessment.exam_session.read');
  const mayReadExams = useCan('assessment.exam.read');
  const mayReadMarksheets = useCan('assessment.marksheet.read');
  const mayReadSchemes = useCan('assessment.grading_scheme.read');
  const maySubmitHomework = useCan('assessment.homework_submission.create');
  const maySubmitAssignment = useCan('assessment.assignment_submission.submit_assignment_submission');
  const mayReadResults = useCan('assessment.marksheet_line.get_student_results');
  const checksHomework = useCan('assessment.homework_submission.check_homework_submission');
  const gradesAssignments = useCan('assessment.assignment_submission.grade_assignment_submission');
  const mayLearn = useCan('lms.enrollment.get_student_dashboard');
  const mayBrowseCatalog = useCan('lms.course.read') || mayLearn;
  const mayNotes = useCan('lms.annotation.read');
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('shell.home.welcome', { name })}</Text>
      <Text muted>{t('shell.home.body')}</Text>
      {linked ? (
        <Text>
          {t('shell.placeholder.title', { screenId: linked.screen.id, title: linked.screen.title })}
        </Text>
      ) : null}
      {mayTimetable ? <Button onPress={() => onOpen('timetable')}>{t('tt.my.open')}</Button> : null}
      {mayLms ? (
        <Button onPress={() => onOpenLms({ screenId: 'LMS-20', params: {} })}>{t('lms.native.open')}</Button>
      ) : null}
      {mayMyCertificates ? (
        <Button variant="secondary" onPress={() => onOpenLms({ screenId: 'LMS-15', params: {} })}>
          {t('screen.LMS15.title')}
        </Button>
      ) : null}
      {mayApplyForLeave || mayApproveLeave ? (
        <Button variant="secondary" onPress={() => onOpen('leave')}>
          {t('hr.leave.open')}
        </Button>
      ) : null}
      {mayLeaveBalance ? (
        <Button variant="secondary" onPress={() => onOpen('leaveBalance')}>
          {t('hr.balance.open')}
        </Button>
      ) : null}
      {mayPayslips ? (
        <Button variant="secondary" onPress={() => onOpen('payslips')}>
          {t('pay.mine.open')}
        </Button>
      ) : null}
      {mayChildren ? <Button onPress={() => onOpen('children')}>{t('screen.SIS11.title')}</Button> : null}
      {mayIncidents ? (
        <Button variant="secondary" onPress={() => onOpen('discipline')}>
          {t('screen.SIS06.title')}
        </Button>
      ) : null}
      {mayFees && (isStudent || mayChildren) ? (
        <Button variant="secondary" onPress={() => onOpen('fees')}>
          {t('fin.native.open')}
        </Button>
      ) : null}
      {mayRegistrations && isStudent ? (
        <Button variant="secondary" onPress={() => onOpen('registration')}>
          {t('screen.SIS10.title')}
        </Button>
      ) : null}
      {/* learning (LMS-04 my learning, LMS-01 the catalogue, LMS-14 notes and bookmarks) */}
      {mayLearn ? (
        <Button onPress={() => onOpenLms({ screenId: 'LMS-04', params: {} })}>
          {t('screen.LMS04.title')}
        </Button>
      ) : null}
      {mayBrowseCatalog ? (
        <Button variant="secondary" onPress={() => onOpenLms({ screenId: 'LMS-01', params: {} })}>
          {t('screen.LMS01.title')}
        </Button>
      ) : null}
      {mayNotes ? (
        <Button variant="secondary" onPress={() => onOpenLms({ screenId: 'LMS-14', params: {} })}>
          {t('screen.LMS14.title')}
        </Button>
      ) : null}
      {/* a student's own work: their homework (each opens ASM-04), assignments (ASM-07) and results (ASM-12) */}
      {isStudent && mayReadHomework && maySubmitHomework && !checksHomework ? (
        <Button onPress={() => onOpenAssessment('ASM-01')}>{t('screen.ASM04.title')}</Button>
      ) : mayReadHomework ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-01')}>
          {t('screen.ASM01.title')}
        </Button>
      ) : null}
      {isStudent && mayReadAssignments && maySubmitAssignment && !gradesAssignments ? (
        <Button onPress={() => onOpenAssessment('ASM-05')}>{t('screen.ASM07.title')}</Button>
      ) : null}
      {isStudent && mayReadResults && ownStudentId ? (
        <Button onPress={() => onOpenAssessment('ASM-12', { studentId: ownStudentId })}>
          {t('screen.ASM12.title')}
        </Button>
      ) : null}
      {mayApproveHomework ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-02')}>
          {t('screen.ASM02.title')}
        </Button>
      ) : null}
      {mayReadAssignments && !(isStudent && !gradesAssignments) ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-05')}>
          {t('screen.ASM05.title')}
        </Button>
      ) : null}
      {mayReadExamSessions ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-08')}>
          {t('screen.ASM08.title')}
        </Button>
      ) : null}
      {mayReadExams ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-09')}>
          {t('screen.ASM09.title')}
        </Button>
      ) : null}
      {mayReadMarksheets ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-11')}>
          {t('screen.ASM11.title')}
        </Button>
      ) : null}
      {mayReadSchemes ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-13')}>
          {t('screen.ASM13.title')}
        </Button>
      ) : null}
      {severalRoles ? (
        <Button variant="secondary" onPress={() => onOpen('persona')}>
          {t('acc.persona.title')}
        </Button>
      ) : null}
      {unverifiedEmail ? (
        <Button variant="secondary" onPress={() => onOpen('verify')}>
          {t('acc.verify.title')}
        </Button>
      ) : null}
      <Button variant="secondary" onPress={() => onOpen('profile')}>
        {t('shell.nav.profile')}
      </Button>
      <Button variant="secondary" onPress={() => onOpen('settings')}>
        {t('shell.nav.settings')}
      </Button>
      <Button variant="secondary" onPress={() => onOpen('notifications')}>
        {t('acc.notifications.title')}
      </Button>
      <Button variant="secondary" loading={signOut.isPending} onPress={() => signOut.mutate()}>
        {t('shell.user.signOut')}
      </Button>
    </Screen>
  );
}
