import {
  useAdmissionCycle,
  useApplication,
  useApplicationContactChange,
  useApplicationContacts,
  useApplicationGuardianChange,
  useApplicationGuardians,
  useApplicationStep,
  useCan,
  useI18n,
  useMyDraft,
  useSaveApplication,
  useStartApplication,
  type Application,
  type ApplicationForm,
  type GuardianRelationship,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Banner, Button, Card, SelectField, Skeleton, TextArea, TextInput } from '@mysms/ui-web';
import { useEffect, useState, type ReactNode } from 'react';
import { useParams } from 'react-router';
import { failureText } from './status';

type Stage = 'applicant' | 'guardians' | 'contacts' | 'review';
const STAGES: readonly Stage[] = ['applicant', 'guardians', 'contacts', 'review'];
const STAGE_LABEL: Readonly<Record<Stage, MessageKey>> = {
  applicant: 'admission.apply.stage.applicant',
  guardians: 'admission.apply.stage.guardians',
  contacts: 'admission.apply.stage.contacts',
  review: 'admission.apply.stage.review',
};
const RELATIONSHIPS: readonly GuardianRelationship[] = ['mother', 'father', 'guardian'];
const GENDERS = ['female', 'male', 'other'] as const;

/** The form's text fields with their limits (the contract's maxLength), in the order they are asked. */
const TEXT_FIELDS: readonly { name: keyof ApplicationForm; label: MessageKey; max: number; type?: string }[] =
  [
    { name: 'firstName', label: 'admission.field.firstName', max: 100 },
    { name: 'lastName', label: 'admission.field.lastName', max: 100 },
    { name: 'email', label: 'admission.field.email', max: 320, type: 'email' },
    { name: 'mobile', label: 'admission.field.mobile', max: 20, type: 'tel' },
    { name: 'addressLine1', label: 'admission.field.addressLine1', max: 255 },
    { name: 'addressLine2', label: 'admission.field.addressLine2', max: 255 },
    { name: 'cityName', label: 'admission.field.cityName', max: 120 },
    { name: 'postalCode', label: 'admission.field.postalCode', max: 12 },
    { name: 'previousInstitution', label: 'admission.field.previousInstitution', max: 255 },
    { name: 'previousCourse', label: 'admission.field.previousCourse', max: 150 },
    { name: 'previousResult', label: 'admission.field.previousResult', max: 255 },
  ];
const EMPTY: ApplicationForm = { fullName: '', dateOfBirth: '', gender: 'female' };

/** What the form holds of an application: its own fields, blanks left out. */
function formOf(application: Application): ApplicationForm {
  const form: ApplicationForm = {
    fullName: application.fullName,
    dateOfBirth: application.dateOfBirth,
    gender: application.gender,
  };
  for (const { name } of TEXT_FIELDS) {
    const value = application[name];
    if (typeof value === 'string' && value) Object.assign(form, { [name]: value });
  }
  if (application.medicalDetails) form.medicalDetails = application.medicalDetails;
  return form;
}

/** The form as it is sent: trimmed, blanks left out. */
function cleaned(form: ApplicationForm): ApplicationForm {
  const out: ApplicationForm = {
    fullName: form.fullName.trim(),
    dateOfBirth: form.dateOfBirth,
    gender: form.gender,
  };
  for (const [name, value] of Object.entries(form)) {
    if (name === 'fullName' || name === 'dateOfBirth' || name === 'gender') continue;
    if (typeof value === 'string' && value.trim()) Object.assign(out, { [name]: value.trim() });
  }
  return out;
}

/**
 * ADMN-05 Application form (`/admission/apply/{admissionCycleId}`), step by step: the applicant, their parents or
 * guardians, emergency contacts, then a review that hands the application in. The contract has no public
 * operation for it: the applicant signs in with an applicant account (the permissions to start, change and hand in
 * their own application, without any institution), and the server lets them reach only their own applications and
 * only in a cycle that is open. Save and resume is the server's: the form is a draft from the first step on, and
 * coming back to this address opens the caller's latest draft of the cycle. Uploads (photo, documents) are not
 * part of this form yet.
 */
export function ApplyScreen() {
  const { t } = useI18n();
  const { admissionCycleId = '' } = useParams();
  const mayCreate = useCan('admission.application.create');
  const mayRead = useCan('admission.application.read');
  const maySubmit = useCan('admission.application.submit_application');
  const cycle = useAdmissionCycle(admissionCycleId || null, mayCreate);
  const draft = useMyDraft(admissionCycleId || null, mayCreate && mayRead);
  const [applicationId, setApplicationId] = useState<string | null>(null);
  const current = useApplication(applicationId, mayRead);
  const guardians = useApplicationGuardians(applicationId);
  const contacts = useApplicationContacts(applicationId);
  const start = useStartApplication();
  const save = useSaveApplication();
  const guardianChange = useApplicationGuardianChange();
  const contactChange = useApplicationContactChange();
  const submit = useApplicationStep();
  const [stage, setStage] = useState<Stage>('applicant');
  const [form, setForm] = useState<ApplicationForm>(EMPTY);
  const [guardian, setGuardian] = useState({
    relationship: 'mother' as GuardianRelationship,
    name: '',
    mobile: '',
    email: '',
    occupation: '',
  });
  const [contact, setContact] = useState({ contactNumber: '', relationship: '' });
  const [sent, setSent] = useState<Application | null>(null);
  const title = t('screen.ADMN05.title');

  // resume: the caller's latest draft of the cycle becomes the form
  const resumed = draft.data;
  useEffect(() => {
    if (resumed?.id && applicationId === null) {
      setApplicationId(resumed.id);
      setForm(formOf(resumed));
    }
  }, [resumed, applicationId]);

  const page = (body: ReactNode) => (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{cycle.data ? `${title} · ${cycle.data.name}` : title}</h1>
      </header>
      {body}
    </div>
  );
  if (!mayCreate) return page(<Banner intent="info">{t('admission.apply.noAccess')}</Banner>);
  if (cycle.isPending || draft.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (cycle.isError || cycle.data.status !== 'open')
    return page(<Banner intent="info">{t('admission.apply.notOpen')}</Banner>);
  if (draft.isError)
    return page(
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void draft.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>,
    );
  if (sent)
    return page(
      <Card title={t('admission.apply.sentTitle')}>
        <p>{t('admission.apply.sent', { number: sent.applicationNumber })}</p>
      </Card>,
    );

  const etag = current.data?.etag ?? null;
  const applicantOk = form.fullName.trim() !== '' && /^\d{4}-\d{2}-\d{2}$/.test(form.dateOfBirth);
  const saveApplicant = () => {
    const body = cleaned(form);
    if (applicationId && etag && current.data)
      save.mutate(
        { applicationId, etag, saved: cleaned(formOf(current.data.data)), form: body },
        { onSuccess: () => setStage('guardians') },
      );
    else if (!applicationId)
      start.mutate(
        { cycle: { id: cycle.data.id, institutionId: cycle.data.institutionId }, form: body },
        {
          onSuccess: (created) => {
            setApplicationId(created.data.id ?? null);
            setStage('guardians');
          },
        },
      );
  };
  const guardianItems = guardians.data?.items ?? [];
  const contactItems = contacts.data ?? [];
  const writeError = start.error ?? save.error ?? guardianChange.error ?? contactChange.error ?? submit.error;

  return page(
    <>
      <ol className="admission-steps" aria-label={t('admission.apply.progress')}>
        {STAGES.map((name) => (
          <li
            key={name}
            className={
              name === stage
                ? 'admission-steps__step admission-steps__step--current'
                : 'admission-steps__step'
            }
            aria-current={name === stage ? 'step' : undefined}
          >
            {t(STAGE_LABEL[name])}
          </li>
        ))}
      </ol>
      {resumed && stage === 'applicant' ? (
        <Banner intent="info">{t('admission.apply.resumed')}</Banner>
      ) : null}
      {writeError ? <Banner intent="danger">{failureText(t, writeError)}</Banner> : null}

      {stage === 'applicant' ? (
        <Card title={t(STAGE_LABEL.applicant)}>
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              if (applicantOk) saveApplicant();
            }}
          >
            <TextInput
              label={t('admission.field.fullName')}
              required
              maxLength={255}
              value={form.fullName}
              onChange={(event) => setForm({ ...form, fullName: event.target.value })}
            />
            <TextInput
              label={t('admission.field.dateOfBirth')}
              type="date"
              required
              value={form.dateOfBirth}
              onChange={(event) => setForm({ ...form, dateOfBirth: event.target.value })}
            />
            <SelectField
              label={t('admission.field.gender')}
              value={form.gender}
              options={GENDERS.map((gender) => ({ value: gender, label: t(`admission.gender.${gender}`) }))}
              onChange={(event) =>
                setForm({ ...form, gender: event.target.value as ApplicationForm['gender'] })
              }
            />
            {TEXT_FIELDS.map((field) => (
              <TextInput
                key={field.name}
                label={t(field.label)}
                type={field.type ?? 'text'}
                maxLength={field.max}
                value={(form[field.name] as string | undefined) ?? ''}
                onChange={(event) => setForm({ ...form, [field.name]: event.target.value })}
              />
            ))}
            <TextArea
              label={t('admission.field.medicalDetails')}
              rows={2}
              value={form.medicalDetails ?? ''}
              onChange={(event) => setForm({ ...form, medicalDetails: event.target.value })}
            />
            <div className="page__actions">
              <Button type="submit" loading={start.isPending || save.isPending} disabled={!applicantOk}>
                {t('admission.apply.saveNext')}
              </Button>
            </div>
          </form>
        </Card>
      ) : null}

      {stage === 'guardians' && applicationId ? (
        <Card title={t(STAGE_LABEL.guardians)}>
          <p className="hint">{t('admission.apply.guardiansHint')}</p>
          <ul className="leave-list">
            {guardianItems.map((item) => (
              <li key={item.id} className="leave-list__item">
                <span className="leave-list__what">
                  <strong>{item.name}</strong> · {t(`admission.relationship.${item.relationship}`)}
                </span>
                <Button
                  variant="secondary"
                  disabled={guardianChange.isPending}
                  onClick={() => guardianChange.mutate({ applicationId, remove: item })}
                >
                  {t('admission.apply.remove')}
                </Button>
              </li>
            ))}
          </ul>
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              if (!guardian.name.trim()) return;
              guardianChange.mutate(
                {
                  applicationId,
                  add: {
                    relationship: guardian.relationship,
                    name: guardian.name.trim(),
                    ...(guardian.mobile.trim() ? { mobile: guardian.mobile.trim() } : {}),
                    ...(guardian.email.trim() ? { email: guardian.email.trim() } : {}),
                    ...(guardian.occupation.trim() ? { occupation: guardian.occupation.trim() } : {}),
                  },
                },
                {
                  onSuccess: () =>
                    setGuardian({ ...guardian, name: '', mobile: '', email: '', occupation: '' }),
                },
              );
            }}
          >
            <SelectField
              label={t('admission.apply.relationship')}
              value={guardian.relationship}
              options={RELATIONSHIPS.map((r) => ({ value: r, label: t(`admission.relationship.${r}`) }))}
              onChange={(event) =>
                setGuardian({ ...guardian, relationship: event.target.value as GuardianRelationship })
              }
            />
            <TextInput
              label={t('admission.apply.guardianName')}
              maxLength={150}
              value={guardian.name}
              onChange={(event) => setGuardian({ ...guardian, name: event.target.value })}
            />
            <TextInput
              label={t('admission.field.mobile')}
              type="tel"
              maxLength={20}
              value={guardian.mobile}
              onChange={(event) => setGuardian({ ...guardian, mobile: event.target.value })}
            />
            <TextInput
              label={t('admission.field.email')}
              type="email"
              maxLength={320}
              value={guardian.email}
              onChange={(event) => setGuardian({ ...guardian, email: event.target.value })}
            />
            <TextInput
              label={t('admission.apply.occupation')}
              maxLength={120}
              value={guardian.occupation}
              onChange={(event) => setGuardian({ ...guardian, occupation: event.target.value })}
            />
            <div className="page__actions">
              <Button
                type="submit"
                variant="secondary"
                loading={guardianChange.isPending}
                disabled={!guardian.name.trim()}
              >
                {t('admission.apply.addGuardian')}
              </Button>
            </div>
          </form>
          <div className="page__actions">
            <Button variant="ghost" onClick={() => setStage('applicant')}>
              {t('common.action.back')}
            </Button>
            <Button disabled={guardianItems.length === 0} onClick={() => setStage('contacts')}>
              {t('admission.apply.next')}
            </Button>
          </div>
        </Card>
      ) : null}

      {stage === 'contacts' && applicationId ? (
        <Card title={t(STAGE_LABEL.contacts)}>
          <ol className="leave-list">
            {contactItems.map((item) => (
              <li key={item.id} className="leave-list__item">
                <span className="leave-list__what">
                  <strong>{item.contactNumber}</strong> · {item.relationship}
                </span>
                <Button
                  variant="secondary"
                  disabled={contactChange.isPending}
                  onClick={() => contactChange.mutate({ applicationId, remove: item })}
                >
                  {t('admission.apply.remove')}
                </Button>
              </li>
            ))}
          </ol>
          <form
            className="form"
            onSubmit={(event) => {
              event.preventDefault();
              if (!contact.contactNumber.trim() || !contact.relationship.trim()) return;
              contactChange.mutate(
                {
                  applicationId,
                  add: {
                    priority: Math.max(0, ...contactItems.map((item) => item.priority)) + 1,
                    contactNumber: contact.contactNumber.trim(),
                    relationship: contact.relationship.trim(),
                  },
                },
                { onSuccess: () => setContact({ contactNumber: '', relationship: '' }) },
              );
            }}
          >
            <TextInput
              label={t('admission.apply.contactNumber')}
              type="tel"
              maxLength={20}
              value={contact.contactNumber}
              onChange={(event) => setContact({ ...contact, contactNumber: event.target.value })}
            />
            <TextInput
              label={t('admission.apply.contactRelationship')}
              maxLength={60}
              value={contact.relationship}
              onChange={(event) => setContact({ ...contact, relationship: event.target.value })}
            />
            <div className="page__actions">
              <Button
                type="submit"
                variant="secondary"
                loading={contactChange.isPending}
                disabled={!contact.contactNumber.trim() || !contact.relationship.trim()}
              >
                {t('admission.apply.addContact')}
              </Button>
            </div>
          </form>
          <div className="page__actions">
            <Button variant="ghost" onClick={() => setStage('guardians')}>
              {t('common.action.back')}
            </Button>
            <Button onClick={() => setStage('review')}>{t('admission.apply.next')}</Button>
          </div>
        </Card>
      ) : null}

      {stage === 'review' && applicationId ? (
        <Card title={t(STAGE_LABEL.review)}>
          <dl className="facts">
            <dt>{t('admission.field.fullName')}</dt>
            <dd>{form.fullName}</dd>
            <dt>{t('admission.field.dateOfBirth')}</dt>
            <dd>{form.dateOfBirth}</dd>
            <dt>{t('admission.apply.stage.guardians')}</dt>
            <dd>{guardianItems.map((item) => item.name).join(', ') || '—'}</dd>
            <dt>{t('admission.apply.stage.contacts')}</dt>
            <dd>{contactItems.map((item) => item.contactNumber).join(', ') || '—'}</dd>
          </dl>
          <p className="hint">{t('admission.apply.reviewHint')}</p>
          <div className="page__actions">
            <Button variant="ghost" onClick={() => setStage('contacts')}>
              {t('common.action.back')}
            </Button>
            {maySubmit ? (
              <Button
                loading={submit.isPending}
                onClick={() =>
                  submit.mutate(
                    { applicationId, step: 'submit', etag },
                    { onSuccess: (done) => setSent(done) },
                  )
                }
              >
                {t('admission.apply.submit')}
              </Button>
            ) : null}
          </div>
        </Card>
      ) : null}
    </>,
  );
}
