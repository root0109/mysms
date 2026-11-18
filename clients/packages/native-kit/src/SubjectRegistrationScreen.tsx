import {
  useCan,
  useI18n,
  useRemoveRegistrationLine,
  useSession,
  useSubjectNames,
  useSubjectRegistrationLines,
  useSubjectRegistrationStep,
  useSubjectRegistrations,
  type RegistrationStep,
  type SubjectRegistration,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, Text, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import { sisFailure } from './sisFailure';

/**
 * SIS-10 on the student's phone: the student's own subject registrations (the student records iam.GetMe names) and,
 * for a draft, filling it from the course's plan, taking off an elective and submitting it; a rejected registration
 * is shown with its state, and the teacher's decision is made on the web. Every step needs its own permission.
 */
export function SubjectRegistrationScreen({
  tablet = false,
  onBack,
}: {
  tablet?: boolean;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const studentIds = useSession((s) => s.me?.studentIds) ?? [];
  const mayRead = useCan('sis.subject_registration.read');
  const mayLines = useCan('sis.subject_registration_line.read');
  const mayLoad = useCan('sis.subject_registration.load_course_subjects');
  const maySubmit = useCan('sis.subject_registration.submit_subject_registration');
  const mayRemove = useCan('sis.subject_registration_line.delete');
  const maySubjects = useCan('academics.subject.read');
  const registrations = useSubjectRegistrations(studentIds, mayRead);
  const [chosenId, setChosenId] = useState('');
  const items = registrations.data?.items ?? [];
  const chosen: SubjectRegistration | undefined = items.find((item) => item.id === chosenId) ?? items[0];
  const lines = useSubjectRegistrationLines(mayLines && chosen?.id ? chosen.id : null);
  const subjectNames = useSubjectNames(maySubjects && Boolean(chosen));
  const step = useSubjectRegistrationStep();
  const remove = useRemoveRegistrationLine();
  const [done, setDone] = useState<MessageKey | null>(null);
  const draft = chosen?.status === 'draft';
  const run = (taken: RegistrationStep, message: MessageKey) => {
    if (!chosen?.id) return;
    setDone(null);
    step.mutate(
      { registrationId: chosen.id, step: taken, etag: chosen.etag ?? null },
      { onSuccess: () => setDone(message) },
    );
  };
  const section = { marginTop: theme.space[7] };
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {t('screen.SIS10.title')}
      </Text>
      {!mayRead ? <Banner>{t('sis.registration.noAccess')}</Banner> : null}
      {mayRead && studentIds.length === 0 ? <Banner>{t('sis.registration.notStudent')}</Banner> : null}
      {registrations.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void registrations.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {mayRead && studentIds.length > 0 && registrations.isPending ? (
        <Text muted>{t('common.state.loading')}</Text>
      ) : null}
      {registrations.data && items.length === 0 ? <Text muted>{t('sis.registration.none')}</Text> : null}
      {items.length > 1 ? (
        <ChoiceGroup
          label={t('sis.registration.choose')}
          choices={items.map((item) => ({
            value: item.id ?? '',
            label: `${item.referenceNo ?? t('sis.student.registrations.unnumbered')} · ${t(`sis.registration.status.${item.status ?? 'draft'}`)}`,
          }))}
          value={chosen?.id ?? ''}
          onChange={setChosenId}
        />
      ) : null}
      {chosen ? (
        <View style={section}>
          <Text variant="h2" accessibilityRole="header">
            {chosen.referenceNo ?? t('sis.student.registrations.unnumbered')}
          </Text>
          <Text muted>{t(`sis.registration.status.${chosen.status ?? 'draft'}`)}</Text>
          {(step.error ?? remove.error) ? (
            <Banner intent="danger">{sisFailure(t, step.error ?? remove.error)}</Banner>
          ) : null}
          {done && !step.error ? <Banner intent="success">{t(done)}</Banner> : null}
          {mayLines && lines.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
          {lines.data && lines.data.items.length === 0 ? (
            <Text muted>{t('sis.registration.noLines')}</Text>
          ) : null}
          {(lines.data?.items ?? []).map((line) => (
            <View
              key={line.id}
              style={{
                marginTop: theme.space[4],
                flexDirection: 'row',
                flexWrap: 'wrap',
                gap: theme.space[3],
              }}
            >
              <Text style={{ flexGrow: 1 }}>
                {`${subjectNames.get(line.subjectId) ?? t('sis.registration.subject')} · ${t(`sis.registration.selection.${line.selectionType}`)}`}
              </Text>
              {draft && mayRemove && line.selectionType === 'elective' ? (
                <Button
                  variant="ghost"
                  loading={remove.isPending && remove.variables?.line.id === line.id}
                  disabled={remove.isPending}
                  onPress={() => remove.mutate({ registrationId: chosen.id ?? '', line })}
                >
                  {t('sis.registration.remove')}
                </Button>
              ) : null}
            </View>
          ))}
          {draft ? (
            <View style={section}>
              {mayLoad ? (
                <Button
                  variant="secondary"
                  loading={step.isPending && step.variables?.step === 'loadCourseSubjects'}
                  disabled={step.isPending}
                  onPress={() => run('loadCourseSubjects', 'sis.registration.loaded')}
                >
                  {t('sis.registration.load')}
                </Button>
              ) : null}
              {maySubmit ? (
                <Button
                  loading={step.isPending && step.variables?.step === 'submit'}
                  disabled={step.isPending || (lines.data?.items.length ?? 0) === 0}
                  onPress={() => run('submit', 'sis.registration.submitted')}
                >
                  {t('sis.registration.submit')}
                </Button>
              ) : null}
            </View>
          ) : null}
        </View>
      ) : null}
      <View style={section}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}
