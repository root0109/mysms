import {
  isApiError,
  useCan,
  useDisciplineIncidents,
  useI18n,
  useIncidentStep,
  useMyChildren,
  useStudentNames,
  useStudentSummary,
  type DisciplineIncident,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Screen, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { Pressable, View } from 'react-native';
import { sisFailure } from './sisFailure';

/**
 * Incidents with their two steps (SIS-06): a teacher records the action taken (with what was done), and the
 * acknowledgement of a guardian is recorded for an incident shared with guardians. `onOpen` opens the student.
 */
function IncidentItems({
  incidents,
  names,
  onOpen,
}: {
  incidents: readonly DisciplineIncident[];
  names?: ReadonlyMap<string, string>;
  onOpen?: (studentId: string) => void;
}) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const mayAct = useCan('sis.discipline_incident.mark_incident_action_taken');
  const maySeen = useCan('sis.discipline_incident.mark_incident_seen');
  const step = useIncidentStep();
  const [responses, setResponses] = useState<Record<string, string>>({});
  const [done, setDone] = useState<MessageKey | null>(null);
  const row = { flexDirection: 'row' as const, flexWrap: 'wrap' as const, gap: theme.space[3] };
  if (incidents.length === 0) return <Text muted>{t('sis.discipline.none')}</Text>;
  return (
    <>
      {step.error ? <Banner intent="danger">{sisFailure(t, step.error)}</Banner> : null}
      {done && !step.error ? <Banner intent="success">{t(done)}</Banner> : null}
      {incidents.map((incident) => {
        const id = incident.id ?? '';
        const busy = step.isPending && step.variables?.incidentId === id;
        const who = names?.get(incident.studentId);
        return (
          <View key={id} style={{ marginTop: theme.space[5] }}>
            {who ? <Text style={{ fontWeight: '600' }}>{who}</Text> : null}
            <Text>{incident.details}</Text>
            <Text variant="caption">
              {`${format.dateTime(incident.incidentAt)} · ${t(incident.actionTaken ? 'sis.discipline.acted' : 'sis.discipline.open')}${
                incident.notifyGuardians
                  ? ` · ${t(incident.seenByGuardian ? 'sis.discipline.seen' : 'sis.discipline.shared')}`
                  : ''
              }`}
            </Text>
            {incident.response ? <Text muted>{incident.response}</Text> : null}
            {mayAct && !incident.actionTaken ? (
              <TextInput
                label={t('sis.discipline.response')}
                value={responses[id] ?? ''}
                multiline
                onChangeText={(value) => setResponses({ ...responses, [id]: value })}
              />
            ) : null}
            <View style={row}>
              {mayAct && !incident.actionTaken ? (
                <Button
                  loading={busy && step.variables?.step === 'markActionTaken'}
                  disabled={step.isPending}
                  onPress={() => {
                    setDone(null);
                    step.mutate(
                      {
                        incidentId: id,
                        step: 'markActionTaken',
                        etag: incident.etag ?? null,
                        ...(responses[id] ? { response: responses[id] } : {}),
                      },
                      { onSuccess: () => setDone('sis.discipline.actedDone') },
                    );
                  }}
                >
                  {t('sis.discipline.markActionTaken')}
                </Button>
              ) : null}
              {maySeen && incident.notifyGuardians && !incident.seenByGuardian ? (
                <Button
                  variant="secondary"
                  loading={busy && step.variables?.step === 'markSeen'}
                  disabled={step.isPending}
                  onPress={() => {
                    setDone(null);
                    step.mutate(
                      { incidentId: id, step: 'markSeen', etag: incident.etag ?? null },
                      { onSuccess: () => setDone('sis.discipline.seenDone') },
                    );
                  }}
                >
                  {t('sis.discipline.markSeen')}
                </Button>
              ) : null}
              {onOpen ? (
                <Button variant="ghost" onPress={() => onOpen(incident.studentId)}>
                  {t('sis.discipline.openStudent')}
                </Button>
              ) : null}
            </View>
          </View>
        );
      })}
    </>
  );
}

/**
 * SIS-02 on the teacher's phone and tablet, and a child's summary on the parent's phone (SIS-11): who the student is,
 * their placement, guardians and open incidents (sis.GetStudentSummary). A teacher who may read incidents sees them
 * with their steps; a parent sees how many the school shared that are still open.
 */
export function StudentSummaryScreen({
  studentId,
  tablet = false,
  onBack,
}: {
  studentId: string;
  tablet?: boolean;
  onBack: () => void;
}) {
  const { t, format } = useI18n();
  const theme = useTheme();
  const maySummary = useCan('sis.student.get_student_summary');
  const mayIncidents = useCan('sis.discipline_incident.read');
  const summary = useStudentSummary(maySummary ? studentId : null);
  const incidents = useDisciplineIncidents(studentId, maySummary && mayIncidents);
  const data = summary.data;
  const section = { marginTop: theme.space[7] };
  return (
    <Screen tablet={tablet}>
      {!maySummary ? <Banner>{t('sis.student.noAccess')}</Banner> : null}
      {summary.isPending && maySummary ? <Text muted>{t('common.state.loading')}</Text> : null}
      {summary.isError ? (
        <>
          <Banner intent="danger">
            {isApiError(summary.error) && summary.error.status === 404
              ? t('sis.student.notFound')
              : t('common.error.generic')}
          </Banner>
          <Button variant="secondary" onPress={() => void summary.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {data ? (
        <>
          <Text variant="h1" accessibilityRole="header">
            {data.person.displayName}
          </Text>
          <Text muted>
            {`${
              data.student.admissionNumber
                ? t('sis.student.admissionNumber', { number: data.student.admissionNumber })
                : t('sis.student.noAdmissionNumber')
            } · ${t(`sis.student.status.${data.student.status ?? 'active'}`)}`}
          </Text>
          {data.person.dateOfBirth ? (
            <Text>{`${t('sis.student.fact.birth')}: ${format.day(data.person.dateOfBirth)}`}</Text>
          ) : null}
          {data.currentEnrollment ? (
            <Text>
              {`${t('sis.student.tab.enrolment')}: ${t(`sis.student.enrolment.state.${data.currentEnrollment.enrollmentStatus ?? 'enrolled'}`)}${
                data.currentEnrollment.rollNumber
                  ? ` · ${t('sis.student.enrolment.roll')} ${data.currentEnrollment.rollNumber}`
                  : ''
              }`}
            </Text>
          ) : null}
          <Text>{`${t('sis.student.fact.incidents')}: ${format.number(data.openIncidents ?? 0)}`}</Text>
          <Text>{`${t('sis.student.tab.guardians')}: ${format.number(data.guardians?.length ?? 0)}`}</Text>
          {mayIncidents ? (
            <View style={section}>
              <Text variant="h2" accessibilityRole="header">
                {t('sis.student.tab.discipline')}
              </Text>
              {incidents.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
              {incidents.data ? <IncidentItems incidents={incidents.data.items} /> : null}
            </View>
          ) : null}
        </>
      ) : null}
      <View style={section}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}

/** SIS-11 My children on the parent's phone: one card per child (sis.ListMyChildren); a card opens the summary. */
export function ChildrenScreen({
  tablet = false,
  onOpen,
  onBack,
}: {
  tablet?: boolean;
  onOpen: (studentId: string) => void;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const may = useCan('sis.guardian.list_my_children');
  const children = useMyChildren(may);
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.SIS11.title')}
      </Text>
      {!may ? <Banner>{t('sis.children.noAccess')}</Banner> : null}
      {may && children.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {children.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void children.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {children.data && children.data.items.length === 0 ? <Text muted>{t('sis.children.none')}</Text> : null}
      {(children.data?.items ?? []).map((card) => (
        <Pressable
          key={card.student.id}
          accessibilityRole="button"
          accessibilityHint={t('sis.children.openHint')}
          onPress={() => onOpen(card.student.id ?? '')}
          style={{
            marginTop: theme.space[5],
            padding: theme.space[5],
            borderRadius: theme.radius.md,
            borderWidth: 1,
            borderColor: theme.colors.border,
            backgroundColor: theme.colors.surface,
          }}
        >
          <Text variant="h3">{card.person.displayName}</Text>
          <Text muted>
            {[card.classSectionLabel, card.institutionName, card.relationship].filter(Boolean).join(' · ') ||
              t(`sis.student.status.${card.student.status ?? 'active'}`)}
          </Text>
        </Pressable>
      ))}
      <View style={{ marginTop: theme.space[7] }}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}

/** SIS-06 on the teacher's phone and tablet: the incidents no action was taken on yet, with their steps. */
export function DisciplineScreen({
  tablet = false,
  onOpen,
  onBack,
}: {
  tablet?: boolean;
  onOpen: (studentId: string) => void;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const may = useCan('sis.discipline_incident.read');
  const mayStudents = useCan('sis.student.read');
  const mayPersons = useCan('people.person.read');
  const maySummary = useCan('sis.student.get_student_summary');
  const incidents = useDisciplineIncidents(null, may);
  const names = useStudentNames(
    mayStudents && mayPersons ? (incidents.data?.items ?? []).map((incident) => incident.studentId) : [],
  );
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.SIS06.title')}
      </Text>
      {!may ? <Banner>{t('sis.discipline.noAccess')}</Banner> : null}
      {may ? <Text muted>{t('sis.discipline.openIntro')}</Text> : null}
      {may && incidents.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {incidents.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void incidents.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {incidents.data ? (
        <IncidentItems incidents={incidents.data.items} names={names} {...(maySummary ? { onOpen } : {})} />
      ) : null}
      <View style={{ marginTop: theme.space[7] }}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}
