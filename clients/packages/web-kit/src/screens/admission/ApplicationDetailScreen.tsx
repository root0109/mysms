import {
  APPLICATION_STEPS,
  buildPath,
  screenById,
  stepsFor,
  useApplication,
  useApplicationContacts,
  useApplicationGuardians,
  useApplicationStep,
  useCan,
  useI18n,
  useSession,
  can,
  type Application,
  type ApplicationStep,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, Skeleton, TextArea } from '@mysms/ui-web';
import { useState, type ReactNode } from 'react';
import { Link, useParams } from 'react-router';
import { EnrolApplicantWizard } from './EnrolApplicantWizard';
import { ApplicationStatusChip } from './ApplicationStatusChip';
import { STEP_LABEL, failureText } from './status';

/** The longest reason a step keeps (api/state-machines.md "admission.Application"). */
const MAX_REASON = 4000;

/** The facts of the form, by section, in the order the form asks for them (contract field names). */
const SECTIONS: readonly { title: MessageKey; fields: readonly (keyof Application)[] }[] = [
  {
    title: 'admission.detail.applicant',
    fields: ['fullName', 'dateOfBirth', 'gender', 'nationality', 'motherTongue', 'bloodGroup', 'birthPlace'],
  },
  {
    title: 'admission.detail.contact',
    fields: ['email', 'mobile', 'phone', 'addressLine1', 'addressLine2', 'cityName', 'postalCode'],
  },
  {
    title: 'admission.detail.schooling',
    fields: ['previousInstitution', 'previousCourse', 'previousResult', 'previousBoard', 'reasonForLeaving'],
  },
  {
    title: 'admission.detail.health',
    fields: ['hasMedicalHistory', 'medicalDetails', 'transportMode', 'transportDetails'],
  },
  {
    title: 'admission.detail.office',
    fields: [
      'applicationNumber',
      'applicationDate',
      'admissionDate',
      'applicationFee',
      'paymentStatus',
      'remarks',
    ],
  },
];

const FIELD_LABEL: Partial<Record<keyof Application, MessageKey>> = {
  fullName: 'admission.field.fullName',
  dateOfBirth: 'admission.field.dateOfBirth',
  gender: 'admission.field.gender',
  nationality: 'admission.field.nationality',
  motherTongue: 'admission.field.motherTongue',
  bloodGroup: 'admission.field.bloodGroup',
  birthPlace: 'admission.field.birthPlace',
  email: 'admission.field.email',
  mobile: 'admission.field.mobile',
  phone: 'admission.field.phone',
  addressLine1: 'admission.field.addressLine1',
  addressLine2: 'admission.field.addressLine2',
  cityName: 'admission.field.cityName',
  postalCode: 'admission.field.postalCode',
  previousInstitution: 'admission.field.previousInstitution',
  previousCourse: 'admission.field.previousCourse',
  previousResult: 'admission.field.previousResult',
  previousBoard: 'admission.field.previousBoard',
  reasonForLeaving: 'admission.field.reasonForLeaving',
  hasMedicalHistory: 'admission.field.hasMedicalHistory',
  medicalDetails: 'admission.field.medicalDetails',
  transportMode: 'admission.field.transportMode',
  transportDetails: 'admission.field.transportDetails',
  applicationNumber: 'admission.field.applicationNumber',
  applicationDate: 'admission.field.applicationDate',
  admissionDate: 'admission.field.admissionDate',
  applicationFee: 'admission.field.applicationFee',
  paymentStatus: 'admission.field.paymentStatus',
  remarks: 'admission.field.remarks',
};

/**
 * ADMN-03 Application detail: the whole form as the applicant handed it in — the applicant, contact, schooling,
 * health and the office's fields, the parents or guardians and the emergency contacts — and every decision step the
 * application's state allows and the role may take, each under the version last read (a stale copy is 412 and is
 * read again). An admitted application is enrolled from here (ADMN-04); an enrolled one links to its student.
 */
export function ApplicationDetailScreen() {
  const { t, format } = useI18n();
  const { applicationId = '' } = useParams();
  const permissions = useSession((s) => s.me?.permissions);
  const mayRead = useCan('admission.application.read');
  const mayGuardians = useCan('admission.application_guardian.read');
  const mayContacts = useCan('admission.application_emergency_contact.read');
  const mayEnrol = useCan('admission.application.enroll_applicant');
  const application = useApplication(applicationId || null, mayRead);
  const guardians = useApplicationGuardians(applicationId || null, mayRead && mayGuardians);
  const contacts = useApplicationContacts(applicationId || null, mayRead && mayContacts);
  const step = useApplicationStep();
  const [reason, setReason] = useState('');
  const [taken, setTaken] = useState<ApplicationStep | null>(null);
  const [enrolling, setEnrolling] = useState(false);
  const student = screenById('SIS-02')?.routes[0];
  const title = t('screen.ADMN03.title');

  const header = (extra?: ReactNode) => (
    <header className="page__header">
      <h1 className="page__title">{title}</h1>
      {extra}
    </header>
  );
  if (!mayRead)
    return (
      <div className="page">
        {header()}
        <Banner intent="info">{t('admission.detail.noAccess')}</Banner>
      </div>
    );
  if (application.isPending) return <Skeleton label={t('common.state.loading')} lines={8} />;
  if (application.isError)
    return (
      <div className="page">
        {header()}
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void application.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {failureText(t, application.error)}
        </Banner>
      </div>
    );

  const { data: row, etag } = application.data;
  const steps = stepsFor(row.status).filter((name) => can(permissions, APPLICATION_STEPS[name].permission));
  const keepsReason = steps.some((name) => APPLICATION_STEPS[name].reason);
  const show = (field: keyof Application): string => {
    const value = row[field];
    if (value === undefined || value === null || value === '') return '—';
    if (typeof value === 'boolean') return t(value ? 'admission.common.yes' : 'admission.common.no');
    if (field === 'applicationDate' && typeof value === 'string') return format.dateTime(value);
    if ((field === 'dateOfBirth' || field === 'admissionDate') && typeof value === 'string')
      return format.day(value);
    return String(value);
  };
  const act = (name: ApplicationStep) => {
    setTaken(null);
    step.mutate(
      {
        applicationId,
        step: name,
        etag,
        ...(APPLICATION_STEPS[name].reason && reason.trim() ? { reason } : {}),
      },
      {
        onSuccess: () => {
          setTaken(name);
          setReason('');
        },
        onError: () => void application.refetch(),
      },
    );
  };

  return (
    <div className="page">
      {header(
        <p className="page__subtitle">
          {row.fullName} · {row.applicationNumber} <ApplicationStatusChip t={t} application={row} />
        </p>,
      )}
      {step.error ? <Banner intent="danger">{failureText(t, step.error)}</Banner> : null}
      {taken && !step.error ? <Banner intent="success">{t(STEP_LABEL[taken].done)}</Banner> : null}

      <Card title={t('admission.detail.decide')}>
        {steps.length === 0 && !(mayEnrol && row.status === 'admitted') ? (
          <p className="hint">{t('admission.detail.noSteps')}</p>
        ) : null}
        {keepsReason ? (
          <TextArea
            label={t('admission.detail.reason')}
            rows={2}
            maxLength={MAX_REASON}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
        ) : null}
        <div className="page__actions">
          {steps.map((name) => (
            <Button
              key={name}
              variant={name === 'reject' || name === 'cancel' ? 'danger' : 'primary'}
              loading={step.isPending && step.variables?.step === name}
              disabled={step.isPending}
              onClick={() => act(name)}
            >
              {t(STEP_LABEL[name].action)}
            </Button>
          ))}
          {mayEnrol && row.status === 'admitted' ? (
            <Button onClick={() => setEnrolling(true)}>{t('admission.enrol.open')}</Button>
          ) : null}
        </div>
        {row.status === 'done' && row.studentId ? (
          <p>
            {t('admission.detail.enrolled')}{' '}
            {student ? (
              <Link to={`/app${buildPath(student, { studentId: row.studentId })}`}>
                {t('admission.enrol.openStudent')}
              </Link>
            ) : null}
          </p>
        ) : null}
      </Card>

      {SECTIONS.map((section) => (
        <Card key={section.title} title={t(section.title)}>
          <dl className="facts">
            {section.fields.map((field) => (
              <FactRow
                key={field}
                label={t(FIELD_LABEL[field] ?? 'admission.field.status')}
                value={show(field)}
              />
            ))}
          </dl>
        </Card>
      ))}

      {mayGuardians ? (
        <Card title={t('admission.detail.guardians')}>
          {guardians.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={2} />
          ) : guardians.isError ? (
            <Banner intent="danger">{t('common.error.generic')}</Banner>
          ) : (guardians.data?.items.length ?? 0) === 0 ? (
            <p className="hint">{t('admission.detail.noGuardians')}</p>
          ) : (
            <ul className="leave-list">
              {(guardians.data?.items ?? []).map((guardian) => (
                <li key={guardian.id} className="leave-list__item">
                  <span className="leave-list__what">
                    <strong>{guardian.name}</strong> · {t(`admission.relationship.${guardian.relationship}`)}
                    <span className="hint">
                      {[guardian.mobile, guardian.email, guardian.occupation].filter(Boolean).join(' · ')}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      ) : null}

      {mayContacts ? (
        <Card title={t('admission.detail.contacts')}>
          {contacts.isPending ? (
            <Skeleton label={t('common.state.loading')} lines={2} />
          ) : contacts.isError ? (
            <Banner intent="danger">{t('common.error.generic')}</Banner>
          ) : (contacts.data?.length ?? 0) === 0 ? (
            <p className="hint">{t('admission.detail.noContacts')}</p>
          ) : (
            <ol className="leave-list">
              {(contacts.data ?? []).map((contact) => (
                <li key={contact.id} className="leave-list__item">
                  <span className="leave-list__what">
                    <strong>{contact.contactNumber}</strong> · {contact.relationship}
                  </span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      ) : null}

      {enrolling ? <EnrolApplicantWizard application={row} onClose={() => setEnrolling(false)} /> : null}
    </div>
  );
}

function FactRow({ label, value }: { label: string; value: string }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </>
  );
}
