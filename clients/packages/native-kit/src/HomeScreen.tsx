import { useCan, useI18n, useSession, useSignOut, type RouteMatch } from '@mysms/core';
import { Button, Screen, Text } from '@mysms/ui-native';
import type { AssessmentScreenId } from './assessmentRoutes';

/** The app home until the per-persona dashboards are built; shows where a deep link pointed. */
export function HomeScreen({
  linked,
  tablet = false,
  onOpen,
  onOpenAssessment,
}: {
  linked: RouteMatch | null;
  tablet?: boolean;
  onOpen: (page: 'profile' | 'settings' | 'notifications' | 'timetable' | 'leave') => void;
  onOpenAssessment: (screenId: AssessmentScreenId) => void;
}) {
  const { t } = useI18n();
  const name = useSession((s) => s.me?.user?.username ?? '');
  const signOut = useSignOut();
  const mayTimetable = useCan('timetable.session.get_timetable');
  const mayApplyForLeave = useCan('hr.leave_application.create');
  const mayApproveLeave = useCan('hr.leave_application.approve_leave');
  const mayReadHomework = useCan('assessment.homework.read');
  const mayApproveHomework = useCan('assessment.homework.approve_homework');
  const mayReadAssignments = useCan('assessment.assignment.read');
  const mayReadExamSessions = useCan('assessment.exam_session.read');
  const mayReadExams = useCan('assessment.exam.read');
  const mayReadMarksheets = useCan('assessment.marksheet.read');
  const mayReadSchemes = useCan('assessment.grading_scheme.read');
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
      {mayApplyForLeave || mayApproveLeave ? (
        <Button variant="secondary" onPress={() => onOpen('leave')}>
          {t('hr.leave.open')}
        </Button>
      ) : null}
      {mayReadHomework ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-01')}>
          {t('screen.ASM01.title')}
        </Button>
      ) : null}
      {mayApproveHomework ? (
        <Button variant="secondary" onPress={() => onOpenAssessment('ASM-02')}>
          {t('screen.ASM02.title')}
        </Button>
      ) : null}
      {mayReadAssignments ? (
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
