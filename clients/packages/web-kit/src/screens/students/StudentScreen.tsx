import {
  STUDENT_STATUSES,
  isApiError,
  isExitStatus,
  useCan,
  useChangeStudentStatus,
  useClassSectionOptions,
  useCreatePortalAccount,
  useDisciplineIncidents,
  useGuardianCards,
  useI18n,
  useIncidentStep,
  useIssueBonafideCertificate,
  useStudentActivities,
  useStudentEnrollments,
  useStudentHealthRecords,
  useStudentSummary,
  useSubjectRegistrations,
  type DisciplineIncident,
  type StudentEnrollment,
  type StudentStatus,
  type StudentSummary,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  DataTable,
  SelectField,
  Skeleton,
  StatusChip,
  TextInput,
  type StatusChipProps,
} from '@mysms/ui-web';
import { Fragment, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { UUID } from '../ids';
import { sisFailure } from './sisFailure';

const LIST = '/app/sis/students';
const TABS = [
  'profile',
  'guardians',
  'enrolment',
  'health',
  'discipline',
  'activities',
  'registrations',
] as const;
type Tab = (typeof TABS)[number];

const STATUS_INTENT: Readonly<Record<StudentStatus, StatusChipProps['intent']>> = {
  active: 'success',
  inactive: 'warning',
  withdrawn: 'neutral',
  transferred: 'neutral',
  graduated: 'info',
  alumni: 'info',
};

function StudentStatusChip({ status }: { status: StudentStatus | undefined }) {
  const { t } = useI18n();
  const value = status ?? 'active';
  return <StatusChip intent={STATUS_INTENT[value]} label={t(`sis.student.status.${value}`)} />;
}

/**
 * SIS-02 Student 360° (with SIS-09, the bonafide certificate): who the student is and where they stand — status,
 * placement, guardians, health, discipline, activities, subject registrations — and the office's steps on the
 * record: changing the status (an exit ends the placement), issuing a bonafide certificate and opening the portal
 * accounts of the student and their guardians. Every tab and step appears only for a role that may use it; the
 * server decides which students the caller may see (404 otherwise).
 */
export function StudentScreen() {
  const { t } = useI18n();
  const { studentId: param = '' } = useParams();
  const [search, setSearch] = useSearchParams();
  const maySummary = useCan('sis.student.get_student_summary');
  const studentId = maySummary && UUID.test(param) ? param : null;
  const summary = useStudentSummary(studentId);
  const asked = search.get('tab') as Tab | null;
  const tab: Tab = asked && TABS.includes(asked) ? asked : 'profile';
  const title = t('screen.SIS02.title');

  const back = (
    <Link to={LIST} className="hint">
      {t('sis.student.back')}
    </Link>
  );
  if (!maySummary)
    return (
      <div className="page">
        <h1 className="page__title">{title}</h1>
        <Banner intent="info">{t('sis.student.noAccess')}</Banner>
      </div>
    );
  if (!studentId || (summary.isError && isApiError(summary.error) && summary.error.status === 404))
    return (
      <div className="page">
        <h1 className="page__title">{title}</h1>
        <Banner intent="warning">{t('sis.student.notFound')}</Banner>
        {back}
      </div>
    );
  if (summary.isError)
    return (
      <div className="page">
        <h1 className="page__title">{title}</h1>
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void summary.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      </div>
    );
  if (!summary.data) return <Skeleton label={t('common.state.loading')} lines={8} />;

  const data = summary.data;
  const student = data.student;
  return (
    <div className="page">
      <header className="page__header">
        {back}
        <h1 className="page__title">{data.person.displayName}</h1>
        <p className="page__subtitle">
          {student.admissionNumber
            ? t('sis.student.admissionNumber', { number: student.admissionNumber })
            : t('sis.student.noAdmissionNumber')}{' '}
          <StudentStatusChip status={student.status} />
        </p>
      </header>
      <StudentActions summary={data} />
      <div className="tabs" role="tablist" aria-label={t('sis.student.tabs')}>
        {TABS.map((name) => (
          <button
            key={name}
            type="button"
            role="tab"
            id={`student-tab-${name}`}
            aria-selected={tab === name}
            aria-controls="student-tab-panel"
            className={tab === name ? 'tabs__tab tabs__tab--on' : 'tabs__tab'}
            onClick={() => setSearch(name === 'profile' ? {} : { tab: name }, { replace: true })}
          >
            {t(`sis.student.tab.${name}`)}
          </button>
        ))}
      </div>
      <div role="tabpanel" id="student-tab-panel" aria-labelledby={`student-tab-${tab}`}>
        {tab === 'profile' ? <ProfileTab summary={data} /> : null}
        {tab === 'guardians' ? <GuardiansTab summary={data} /> : null}
        {tab === 'enrolment' ? <EnrolmentTab summary={data} /> : null}
        {tab === 'health' ? <HealthTab studentId={studentId} /> : null}
        {tab === 'discipline' ? <DisciplineTab studentId={studentId} /> : null}
        {tab === 'activities' ? <ActivitiesTab studentId={studentId} /> : null}
        {tab === 'registrations' ? <RegistrationsTab studentId={studentId} /> : null}
      </div>
    </div>
  );
}

/** The office's steps on the record, each for the role that holds its permission. */
function StudentActions({ summary }: { summary: StudentSummary }) {
  const { t } = useI18n();
  const student = summary.student;
  const studentId = student.id ?? '';
  const mayStatus = useCan('sis.student.change_student_status');
  const mayBonafide = useCan('sis.student.issue_bonafide_certificate');
  const mayAccount = useCan('sis.student.create_student_portal_account');
  const change = useChangeStudentStatus();
  const issue = useIssueBonafideCertificate();
  const account = useCreatePortalAccount();
  const others = STUDENT_STATUSES.filter((status) => status !== student.status);
  const [status, setStatus] = useState<StudentStatus>(others[0] ?? 'inactive');
  const [exitDate, setExitDate] = useState('');
  const [exitReason, setExitReason] = useState('');
  const [purpose, setPurpose] = useState('');
  const [done, setDone] = useState<MessageKey | null>(null);
  const chosen = others.includes(status) ? status : (others[0] ?? 'inactive');
  const leaving = isExitStatus(chosen);
  if (!mayStatus && !mayBonafide && !(mayAccount && !student.userId)) return null;
  const error = change.error ?? issue.error ?? account.error;
  const reset = () => {
    setDone(null);
    change.reset();
    issue.reset();
    account.reset();
  };
  return (
    <Card title={t('sis.student.actions')}>
      {error ? <Banner intent="danger">{sisFailure(t, error)}</Banner> : null}
      {done && !error ? (
        <Banner intent="success">
          {t(done, {
            number: student.bonafideCertificateNumber ?? '',
            status: t(`sis.student.status.${student.status ?? 'active'}`),
          })}
        </Banner>
      ) : null}
      <div className="page__grid">
        {mayStatus ? (
          <form
            className="form"
            aria-label={t('sis.student.status.change')}
            onSubmit={(event) => {
              event.preventDefault();
              if (leaving && !exitDate) return;
              reset();
              change.mutate(
                {
                  studentId,
                  etag: student.etag ?? null,
                  status: chosen,
                  ...(leaving ? { exitDate, exitReason } : {}),
                },
                { onSuccess: () => setDone('sis.student.status.changed') },
              );
            }}
          >
            <SelectField
              label={t('sis.student.status.new')}
              options={others.map((value) => ({ value, label: t(`sis.student.status.${value}`) }))}
              value={chosen}
              onChange={(event) => setStatus(event.target.value as StudentStatus)}
            />
            {leaving ? (
              <>
                <TextInput
                  label={t('sis.student.exitDate')}
                  type="date"
                  required
                  value={exitDate}
                  onChange={(event) => setExitDate(event.target.value)}
                />
                <TextInput
                  label={t('sis.student.exitReason')}
                  value={exitReason}
                  onChange={(event) => setExitReason(event.target.value)}
                />
                <p className="hint">{t('sis.student.exitEndsPlacement')}</p>
              </>
            ) : null}
            <div className="form__actions">
              <Button
                type="submit"
                variant="secondary"
                loading={change.isPending}
                disabled={leaving && !exitDate}
              >
                {t('sis.student.status.change')}
              </Button>
            </div>
          </form>
        ) : null}
        {mayBonafide ? (
          <form
            className="form"
            aria-label={t('sis.student.bonafide.title')}
            onSubmit={(event) => {
              event.preventDefault();
              reset();
              issue.mutate(
                { studentId, purpose },
                {
                  onSuccess: () => {
                    setPurpose('');
                    setDone('sis.student.bonafide.issued');
                  },
                },
              );
            }}
          >
            <TextInput
              label={t('sis.student.bonafide.purpose')}
              value={purpose}
              onChange={(event) => setPurpose(event.target.value)}
            />
            {student.bonafideCertificateNumber ? (
              <p className="hint">
                {t('sis.student.bonafide.last', { number: student.bonafideCertificateNumber })}
              </p>
            ) : null}
            <div className="form__actions">
              <Button type="submit" variant="secondary" loading={issue.isPending}>
                {t('sis.student.bonafide.issue')}
              </Button>
            </div>
          </form>
        ) : null}
        {mayAccount && !student.userId ? (
          <div className="form">
            <p className="hint">{t('sis.student.account.none')}</p>
            <div className="form__actions">
              <Button
                variant="secondary"
                loading={account.isPending}
                onClick={() => {
                  reset();
                  account.mutate(
                    { who: 'student', id: studentId },
                    { onSuccess: () => setDone('sis.student.account.created') },
                  );
                }}
              >
                {t('sis.student.account.create')}
              </Button>
            </div>
          </div>
        ) : null}
      </div>
    </Card>
  );
}

function ProfileTab({ summary }: { summary: StudentSummary }) {
  const { t, format } = useI18n();
  const student = summary.student;
  const person = summary.person;
  const mayAttendance = useCan('attendance.attendance_record.summarize_attendance');
  const facts: [MessageKey, string | undefined][] = [
    ['sis.student.fact.name', person.displayName],
    ['sis.student.fact.birth', person.dateOfBirth ? format.day(person.dateOfBirth) : undefined],
    ['sis.student.fact.admissionNumber', student.admissionNumber],
    ['sis.student.fact.admitted', student.admissionDate ? format.day(student.admissionDate) : undefined],
    ['sis.student.fact.joined', student.joiningDate ? format.day(student.joiningDate) : undefined],
    ['sis.student.fact.governmentId', student.governmentStudentId],
    ['sis.student.fact.bonafide', student.bonafideCertificateNumber],
    ['sis.student.fact.account', t(student.userId ? 'sis.student.account.yes' : 'sis.student.account.no')],
    ['sis.student.fact.exit', student.exitDate ? format.day(student.exitDate) : undefined],
    ['sis.student.fact.exitReason', student.exitReason],
    ['sis.student.fact.incidents', format.number(summary.openIncidents ?? 0)],
    ['sis.student.fact.courses', format.number(summary.programEnrollments?.length ?? 0)],
  ];
  return (
    <Card title={t('sis.student.tab.profile')}>
      <dl className="facts">
        {facts
          .filter(([, value]) => value !== undefined && value !== '')
          .map(([label, value]) => (
            <Fragment key={label}>
              <dt>{t(label)}</dt>
              <dd>{value}</dd>
            </Fragment>
          ))}
      </dl>
      {mayAttendance && student.id ? (
        <Link to={`/app/attendance/summary/${student.id}`}>{t('sis.student.attendanceLink')}</Link>
      ) : null}
    </Card>
  );
}

function GuardiansTab({ summary }: { summary: StudentSummary }) {
  const { t } = useI18n();
  const mayGuardians = useCan('sis.guardian.read');
  const mayNames = useCan('people.person.read');
  const mayAccount = useCan('sis.guardian.create_portal_account');
  const links = summary.guardians ?? [];
  const cards = useGuardianCards(
    links.map((link) => link.guardianId),
    mayNames,
    mayGuardians,
  );
  const account = useCreatePortalAccount();
  const [opened, setOpened] = useState<string | null>(null);
  return (
    <Card title={t('sis.student.tab.guardians')}>
      {account.error ? <Banner intent="danger">{sisFailure(t, account.error)}</Banner> : null}
      {opened && !account.error ? <Banner intent="success">{t('sis.student.account.created')}</Banner> : null}
      {links.length === 0 ? (
        <p className="hint">{t('sis.student.guardians.none')}</p>
      ) : (
        <ul className="leave-list">
          {links.map((link) => {
            const card = cards.data?.get(link.guardianId);
            const name =
              card?.name ??
              card?.guardian.email ??
              card?.guardian.mobile ??
              t('sis.student.guardians.someone');
            return (
              <li key={link.id} className="leave-list__item">
                <span className="leave-list__what">
                  <strong>{name}</strong>
                  {link.isPrimaryContact ? ` · ${t('sis.student.guardians.primary')}` : ''}
                  {link.status === 'inactive' ? ` · ${t('sis.student.guardians.inactive')}` : ''}
                  {card?.guardian.mobile ? <span className="hint"> {card.guardian.mobile}</span> : null}
                </span>
                {mayAccount && card && !card.guardian.userId ? (
                  <Button
                    variant="secondary"
                    aria-label={t('sis.student.guardians.accountFor', { name })}
                    loading={account.isPending && account.variables?.id === link.guardianId}
                    disabled={account.isPending}
                    onClick={() => {
                      setOpened(null);
                      account.mutate(
                        { who: 'guardian', id: link.guardianId },
                        { onSuccess: () => setOpened(link.guardianId) },
                      );
                    }}
                  >
                    {t('sis.student.account.create')}
                  </Button>
                ) : null}
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}

function EnrolmentTab({ summary }: { summary: StudentSummary }) {
  const { t, format } = useI18n();
  const student = summary.student;
  const mayList = useCan('sis.student_enrollment.read');
  const mayClasses = useCan('academics.class_section.read');
  const placements = useStudentEnrollments(student.id ?? null, mayList);
  const classes = useClassSectionOptions(student.institutionId, mayClasses);
  const label = new Map((classes.data ?? []).map((option) => [option.id, option.label]));
  const rows = mayList
    ? placements.data?.items
    : summary.currentEnrollment
      ? [summary.currentEnrollment]
      : [];
  return (
    <Card title={t('sis.student.tab.enrolment')}>
      <DataTable<StudentEnrollment>
        caption={t('sis.student.tab.enrolment')}
        columns={[
          {
            key: 'class',
            header: t('sis.student.enrolment.class'),
            render: (row) =>
              row.classSectionId
                ? (label.get(row.classSectionId) ?? t('sis.student.enrolment.someClass'))
                : '—',
          },
          { key: 'roll', header: t('sis.student.enrolment.roll'), render: (row) => row.rollNumber ?? '—' },
          {
            key: 'status',
            header: t('sis.student.enrolment.status'),
            render: (row) => t(`sis.student.enrolment.state.${row.enrollmentStatus ?? 'enrolled'}`),
          },
          {
            key: 'from',
            header: t('sis.student.enrolment.from'),
            render: (row) => (row.effectiveFrom ? format.day(row.effectiveFrom) : '—'),
          },
          {
            key: 'to',
            header: t('sis.student.enrolment.to'),
            render: (row) =>
              row.effectiveTo ? format.day(row.effectiveTo) : t('sis.student.enrolment.open'),
          },
        ]}
        rows={rows}
        rowKey={(row) => row.id ?? ''}
        loading={mayList && placements.isPending}
        loadingLabel={t('common.state.loading')}
        emptyText={t('sis.student.enrolment.none')}
      />
    </Card>
  );
}

function HealthTab({ studentId }: { studentId: string }) {
  const { t, format } = useI18n();
  const may = useCan('sis.student_health_record.read');
  const records = useStudentHealthRecords(studentId, may);
  if (!may) return <p className="hint">{t('sis.student.tabNoAccess')}</p>;
  return (
    <Card title={t('sis.student.tab.health')}>
      {records.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={3} />
      ) : (records.data?.items.length ?? 0) === 0 ? (
        <p className="hint">{t('sis.student.health.none')}</p>
      ) : (
        <ul className="leave-list">
          {(records.data?.items ?? []).map((record) => (
            <li key={record.id} className="leave-list__item">
              <span className="leave-list__what">
                {[
                  record.heightCm
                    ? t('sis.student.health.height', { cm: format.number(Number(record.heightCm)) })
                    : '',
                  record.weightKg
                    ? t('sis.student.health.weight', { kg: format.number(Number(record.weightKg)) })
                    : '',
                ]
                  .filter(Boolean)
                  .join(' · ')}
                {[
                  record.medicalConditions,
                  record.foodAllergies,
                  record.medicationAllergies,
                  record.medicationNeeds,
                ]
                  .filter(Boolean)
                  .map((text) => (
                    <span key={text} className="hint">
                      {' '}
                      {text}
                    </span>
                  ))}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

/** The incidents of a student, with the two steps: action taken (and what was done), and a guardian's acknowledgement. */
function IncidentList({ incidents }: { incidents: readonly DisciplineIncident[] }) {
  const { t, format } = useI18n();
  const mayAct = useCan('sis.discipline_incident.mark_incident_action_taken');
  const maySeen = useCan('sis.discipline_incident.mark_incident_seen');
  const step = useIncidentStep();
  const [responses, setResponses] = useState<Record<string, string>>({});
  const [done, setDone] = useState<MessageKey | null>(null);
  if (incidents.length === 0) return <p className="hint">{t('sis.discipline.none')}</p>;
  return (
    <>
      {step.error ? <Banner intent="danger">{sisFailure(t, step.error)}</Banner> : null}
      {done && !step.error ? <Banner intent="success">{t(done)}</Banner> : null}
      <ul className="leave-list">
        {incidents.map((incident) => {
          const id = incident.id ?? '';
          const mine = step.isPending && step.variables?.incidentId === id;
          return (
            <li key={id} className="leave-list__item">
              <span className="leave-list__what">
                <strong>{format.dateTime(incident.incidentAt)}</strong> {incident.details}
                {incident.response ? <span className="hint"> {incident.response}</span> : null}
              </span>
              <StatusChip
                intent={incident.actionTaken ? 'success' : 'warning'}
                label={t(incident.actionTaken ? 'sis.discipline.acted' : 'sis.discipline.open')}
              />
              {incident.notifyGuardians ? (
                <StatusChip
                  intent={incident.seenByGuardian ? 'success' : 'neutral'}
                  label={t(incident.seenByGuardian ? 'sis.discipline.seen' : 'sis.discipline.shared')}
                />
              ) : null}
              {mayAct && !incident.actionTaken ? (
                <>
                  <TextInput
                    label={t('sis.discipline.response')}
                    value={responses[id] ?? ''}
                    onChange={(event) => setResponses({ ...responses, [id]: event.target.value })}
                  />
                  <Button
                    loading={mine && step.variables?.step === 'markActionTaken'}
                    disabled={step.isPending}
                    onClick={() => {
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
                </>
              ) : null}
              {maySeen && incident.notifyGuardians && !incident.seenByGuardian ? (
                <Button
                  variant="secondary"
                  loading={mine && step.variables?.step === 'markSeen'}
                  disabled={step.isPending}
                  onClick={() => {
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
            </li>
          );
        })}
      </ul>
    </>
  );
}

function DisciplineTab({ studentId }: { studentId: string }) {
  const { t } = useI18n();
  const may = useCan('sis.discipline_incident.read');
  const incidents = useDisciplineIncidents(studentId, may);
  if (!may) return <p className="hint">{t('sis.student.tabNoAccess')}</p>;
  return (
    <Card title={t('sis.student.tab.discipline')}>
      {incidents.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={3} />
      ) : incidents.isError ? (
        <Banner intent="danger">{t('common.error.generic')}</Banner>
      ) : (
        <IncidentList incidents={incidents.data?.items ?? []} />
      )}
    </Card>
  );
}

function ActivitiesTab({ studentId }: { studentId: string }) {
  const { t, format } = useI18n();
  const may = useCan('sis.student_activity.read');
  const activities = useStudentActivities(studentId, may);
  if (!may) return <p className="hint">{t('sis.student.tabNoAccess')}</p>;
  return (
    <Card title={t('sis.student.tab.activities')}>
      {activities.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={3} />
      ) : (activities.data?.items.length ?? 0) === 0 ? (
        <p className="hint">{t('sis.student.activities.none')}</p>
      ) : (
        <ul className="leave-list">
          {(activities.data?.items ?? []).map((activity) => (
            <li key={activity.id} className="leave-list__item">
              <span className="leave-list__what">
                {activity.activityDate ? <strong>{format.day(activity.activityDate)}</strong> : null}{' '}
                {activity.description}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

function RegistrationsTab({ studentId }: { studentId: string }) {
  const { t } = useI18n();
  const may = useCan('sis.subject_registration.read');
  const registrations = useSubjectRegistrations([studentId], may);
  if (!may) return <p className="hint">{t('sis.student.tabNoAccess')}</p>;
  return (
    <Card title={t('sis.student.tab.registrations')}>
      {registrations.isPending ? (
        <Skeleton label={t('common.state.loading')} lines={3} />
      ) : (registrations.data?.items.length ?? 0) === 0 ? (
        <p className="hint">{t('sis.student.registrations.none')}</p>
      ) : (
        <ul className="leave-list">
          {(registrations.data?.items ?? []).map((registration) => (
            <li key={registration.id} className="leave-list__item">
              <span className="leave-list__what">
                {registration.referenceNo ?? t('sis.student.registrations.unnumbered')}
              </span>
              <StatusChip label={t(`sis.registration.status.${registration.status ?? 'draft'}`)} />
            </li>
          ))}
        </ul>
      )}
      <Link to="/app/sis/subject-registrations">{t('sis.student.registrations.all')}</Link>
    </Card>
  );
}
