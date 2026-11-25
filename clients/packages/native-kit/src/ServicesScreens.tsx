import {
  fieldIn,
  resourceDef,
  useAllRecords,
  useCan,
  useClassRoster,
  useClassSectionOptions,
  useClassSubjects,
  useI18n,
  useMyLoans,
  useMyStudents,
  useMyTransport,
  useSession,
  useStudentNames,
  useSubjectNames,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, Screen, Text, useTheme } from '@mysms/ui-native';
import { useState, type ReactNode } from 'react';
import { View } from 'react-native';
import { RecordEditor, WorkflowActions } from './records';
import { StudentChoice } from './StudentChoice';

const RESERVATIONS = resourceDef('library.Reservation');
const NO_STAFF: readonly string[] = [];

function Page({
  title,
  tablet,
  onBack,
  children,
}: {
  title: string;
  tablet: boolean;
  onBack: () => void;
  children: ReactNode;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  return (
    <Screen tablet={tablet}>
      <Text variant="h1" accessibilityRole="header">
        {title}
      </Text>
      {children}
      <View style={{ marginTop: theme.space[7] }}>
        <Button variant="secondary" onPress={onBack}>
          {t('common.action.back')}
        </Button>
      </View>
    </Screen>
  );
}

function Line({ title, detail }: { title: string; detail: string }) {
  const theme = useTheme();
  return (
    <View style={{ marginTop: theme.space[4] }}>
      <Text style={{ fontWeight: '600' }}>{title}</Text>
      {detail ? <Text muted>{detail}</Text> : null}
    </View>
  );
}

/** LIB-07 on phones: the loans on the signed-in student's (S) or teacher's (T) own library card. */
export function MyLoansScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format, timeZone } = useI18n();
  const loans = useMyLoans();
  const today = new Intl.DateTimeFormat('en-CA', { timeZone }).format(new Date());
  return (
    <Page title={t('screen.LIB07.title')} tablet={tablet} onBack={onBack}>
      {!loans.may ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {loans.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {loans.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={loans.refetch}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {loans.may && !loans.isPending && !loans.isError && loans.cards.length === 0 ? (
        <Text muted>{t('lib.mine.noCard')}</Text>
      ) : null}
      {loans.cards.length > 0 && !loans.isPending && loans.loans.length === 0 ? (
        <Text muted>{t('lib.mine.none')}</Text>
      ) : null}
      {loans.loans.map((loan) => {
        const due = typeof loan.dueOn === 'string' ? loan.dueOn : '';
        const overdue =
          due !== '' && due < today && (loan.status === 'issued' || loan.status === 'return_pending');
        return (
          <Line
            key={loan.id}
            title={loans.titleOf.get(String(loan.mediaId)) || t('lib.mine.untitled')}
            detail={[
              enumLabel(t, 'loanStatus', String(loan.status ?? '')),
              due ? t('lib.mine.due', { day: format.day(due) }) : '',
              overdue ? t('lib.mine.overdue') : '',
            ]
              .filter(Boolean)
              .join(' · ')}
          />
        );
      })}
    </Page>
  );
}

/**
 * LIB-05 on the student's phone: the reservations the student asked for (newest first, each title named) and a new
 * one; the library accepts or rejects them.
 */
export function ReservationsScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t, format } = useI18n();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const mayRead = useCan('library.reservation.read');
  const mayCreate = useCan('library.reservation.create');
  const mayTitles = useCan('library.media.read');
  const [asking, setAsking] = useState(false);
  const reservations = useAllRecords(
    'library.Reservation',
    { filter: `requestedByUserId = "${me}"`, sort: ['createTime,desc'] },
    mayRead && me !== '',
  );
  const rows = reservations.data?.items ?? [];
  const mediaIds = [...new Set(rows.map((row) => String(row.mediaId)))];
  const titles = useAllRecords(
    'library.Media',
    { ...(mediaIds.length ? { filter: fieldIn('id', mediaIds) } : {}) },
    mayTitles && mediaIds.length > 0,
  );
  const titleOf = new Map((titles.data?.items ?? []).map((row) => [row.id, String(row.title ?? '')]));
  const def = RESERVATIONS as NonNullable<typeof RESERVATIONS>;
  return (
    <Page title={t('screen.LIB05.title')} tablet={tablet} onBack={onBack}>
      {!mayRead ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayCreate && !asking ? (
        <Button onPress={() => setAsking(true)}>{t('lib.reservations.new')}</Button>
      ) : null}
      {asking ? (
        <RecordEditor
          def={def}
          tablet={tablet}
          onDone={() => setAsking(false)}
          onCancel={() => setAsking(false)}
        />
      ) : null}
      {reservations.isPending && mayRead ? <Text muted>{t('common.state.loading')}</Text> : null}
      {reservations.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {reservations.isSuccess && rows.length === 0 ? <Text muted>{t('lib.reservations.none')}</Text> : null}
      {rows.map((row) => (
        <View key={row.id}>
          <Line
            title={titleOf.get(String(row.mediaId)) || t('lib.mine.untitled')}
            detail={[
              enumLabel(t, 'reservationStatus', String(row.status ?? '')),
              typeof row.dateFrom === 'string' && typeof row.dateTo === 'string'
                ? `${format.day(row.dateFrom)} – ${format.day(row.dateTo)}`
                : '',
            ]
              .filter(Boolean)
              .join(' · ')}
          />
          <WorkflowActions
            def={def}
            row={row}
            etag={row.etag ?? null}
            onReload={() => void reservations.refetch()}
          />
        </View>
      ))}
    </Page>
  );
}

/** TRN-03 on phones: how the student (S) or a chosen child (P) gets to school — mode, route, bus and stops. */
export function MyRouteScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  const mine = useMyStudents();
  const [chosen, setChosen] = useState('');
  const studentId = chosen || mine.students[0]?.id || null;
  const transport = useMyTransport(studentId);
  const own = transport.transports[0];
  const route = transport.routes[0];
  return (
    <Page title={t('screen.TRN03.title')} tablet={tablet} onBack={onBack}>
      <StudentChoice students={mine.students} value={studentId ?? ''} onChange={setChosen} />
      {!transport.may ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {transport.may && !studentId && !mine.isPending ? <Banner>{t('native.whose.none')}</Banner> : null}
      {transport.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {transport.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={transport.refetch}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {transport.may && studentId && !transport.isPending && !own ? (
        <Text muted>{t('trn.mine.none')}</Text>
      ) : null}
      {own ? (
        <Line
          title={enumLabel(t, 'transportMode', String(own.transportMode ?? ''))}
          detail={typeof own.details === 'string' ? own.details : ''}
        />
      ) : null}
      {route ? (
        <Line
          title={String(route.name ?? '')}
          detail={t('trn.mine.bus', {
            vehicle: String(route.vehicleNumber ?? ''),
            start: String(route.startTime ?? ''),
            end: String(route.endTime ?? ''),
          })}
        />
      ) : null}
      {transport.stops.map((stop) => (
        <Line
          key={stop.id}
          title={`${String(stop.sequence ?? '')}. ${String(stop.stopName ?? '')}${stop.id === own?.routeStopId ? ` — ${t('trn.mine.yourStop')}` : ''}`}
          detail={[
            stop.pickupTime ? t('trn.mine.pickup', { time: String(stop.pickupTime) }) : '',
            stop.dropTime ? t('trn.mine.drop', { time: String(stop.dropTime) }) : '',
          ]
            .filter(Boolean)
            .join(' · ')}
        />
      ))}
    </Page>
  );
}

/**
 * ACA-06 on the teacher's phone: the classes the teacher teaches (their teaching assignments), and for the one
 * chosen its students by roll number and its subjects.
 */
export function ClassWorkspaceScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  const staffIds = useSession((s) => s.me?.staffIds ?? NO_STAFF);
  const mayAssignments = useCan('academics.teaching_assignment.read');
  const mayClasses = useCan('academics.class_section.read');
  const assignments = useAllRecords(
    'academics.TeachingAssignment',
    { ...(staffIds.length ? { filter: fieldIn('staffId', staffIds) } : {}) },
    mayAssignments && staffIds.length > 0,
  );
  const classIds = [...new Set((assignments.data?.items ?? []).map((row) => String(row.classSectionId)))];
  const classes = useAllRecords(
    'academics.ClassSection',
    { ...(classIds.length ? { filter: fieldIn('id', classIds) } : {}) },
    mayClasses && classIds.length > 0,
  );
  const institutionId = (classes.data?.items[0]?.institutionId as string | undefined) ?? null;
  const options = useClassSectionOptions(institutionId, institutionId !== null);
  const labelOf = new Map((options.data ?? []).map((option) => [option.id, option.label]));
  const [chosen, setChosen] = useState('');
  const classSectionId = chosen || classIds[0] || '';
  const roster = useClassRoster(classSectionId || null);
  const students = [...(roster.data?.items ?? [])].sort((a, b) =>
    (a.rollNumber ?? '').localeCompare(b.rollNumber ?? '', undefined, { numeric: true }),
  );
  const names = useStudentNames(students.map((row) => row.studentId));
  const subjects = useClassSubjects(classSectionId || null, false);
  const subjectNames = useSubjectNames(useCan('academics.subject.read'));
  return (
    <Page title={t('screen.ACA06.title')} tablet={tablet} onBack={onBack}>
      {!mayAssignments ? <Banner>{t('adm.resource.noAccess')}</Banner> : null}
      {mayAssignments && staffIds.length === 0 ? <Banner>{t('aca.native.noStaff')}</Banner> : null}
      {assignments.isPending && staffIds.length > 0 ? <Text muted>{t('common.state.loading')}</Text> : null}
      {assignments.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {assignments.isSuccess && classIds.length === 0 ? <Text muted>{t('aca.native.noClasses')}</Text> : null}
      {classIds.length > 1 ? (
        <ChoiceGroup
          label={t('aca.native.class')}
          choices={classIds.map((id) => ({
            value: id,
            label: labelOf.get(id) ?? t('aca.class.unnamedClass'),
          }))}
          value={classSectionId}
          onChange={setChosen}
        />
      ) : null}
      {classSectionId ? (
        <>
          <Text variant="h2">{labelOf.get(classSectionId) ?? t('aca.class.unnamedClass')}</Text>
          <Text style={{ fontWeight: '600' }}>{t('aca.native.subjects')}</Text>
          {(subjects.data ?? []).map((line) => (
            <Text key={line.id}>{subjectNames.get(line.subjectId) ?? t('aca.class.unnamedSubject')}</Text>
          ))}
          <Text style={{ fontWeight: '600' }}>{t('aca.native.students')}</Text>
          {roster.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
          {roster.isSuccess && students.length === 0 ? <Text muted>{t('aca.class.noStudents')}</Text> : null}
          {students.map((row) => (
            <Text
              key={row.studentId}
            >{`${row.rollNumber ?? '—'}. ${names.get(row.studentId) ?? t('aca.class.unnamed')}`}</Text>
          ))}
        </>
      ) : null}
    </Page>
  );
}
