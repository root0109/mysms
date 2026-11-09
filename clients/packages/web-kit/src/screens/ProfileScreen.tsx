import { zodResolver } from '@hookform/resolvers/zod';
import {
  isApiError,
  useI18n,
  useMyPerson,
  useSession,
  useUpdateMyPerson,
  type MyPersonUpdate,
  type Person,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, StatusChip, TextArea, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useForm, type Path, type UseFormReturn } from 'react-hook-form';
import { z } from 'zod';
import { fieldMessage } from './fieldMessage';

const MAX = 'common.validation.maxLength';
/** The contract's lengths (people MyPersonUpdate); about-me is limited by the service. */
const LIMITS = {
  primaryEmail: 320,
  primaryPhone: 20,
  alternatePhone: 20,
  workPhone: 30,
  websiteUrl: 500,
  aboutMe: 2000,
  motherTongue: 60,
} as const;
const PHONE = /^\+?[0-9(][0-9 ()-]{3,28}$/;

const optional = (max: number) => z.string().trim().max(max, MAX);
const schema = z.object({
  primaryEmail: optional(LIMITS.primaryEmail).refine(
    (v) => v === '' || /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v),
    'acc.profile.invalidEmail',
  ),
  primaryPhone: optional(LIMITS.primaryPhone).refine(
    (v) => v === '' || PHONE.test(v),
    'acc.profile.invalidPhone',
  ),
  alternatePhone: optional(LIMITS.alternatePhone).refine(
    (v) => v === '' || PHONE.test(v),
    'acc.profile.invalidPhone',
  ),
  workPhone: optional(LIMITS.workPhone).refine((v) => v === '' || PHONE.test(v), 'acc.profile.invalidPhone'),
  websiteUrl: optional(LIMITS.websiteUrl).refine(
    (v) => v === '' || /^https?:\/\/[^\s/]+/i.test(v),
    'acc.profile.invalidUrl',
  ),
  motherTongue: optional(LIMITS.motherTongue),
  aboutMe: optional(LIMITS.aboutMe),
});
type Values = z.infer<typeof schema>;
type Field = keyof Values;
const CONTACT: readonly Field[] = [
  'primaryEmail',
  'primaryPhone',
  'alternatePhone',
  'workPhone',
  'websiteUrl',
  'motherTongue',
];
const INPUT_TYPE: Partial<Record<Field, string>> = {
  primaryEmail: 'email',
  primaryPhone: 'tel',
  alternatePhone: 'tel',
  workPhone: 'tel',
  websiteUrl: 'url',
};
const AUTOCOMPLETE: Partial<Record<Field, string>> = {
  primaryEmail: 'email',
  primaryPhone: 'tel',
  websiteUrl: 'url',
};

const valuesOf = (p: Person | undefined): Values => ({
  primaryEmail: p?.primaryEmail ?? '',
  primaryPhone: p?.primaryPhone ?? '',
  alternatePhone: p?.alternatePhone ?? '',
  workPhone: p?.workPhone ?? '',
  websiteUrl: p?.websiteUrl ?? '',
  motherTongue: p?.motherTongue ?? '',
  aboutMe: p?.aboutMe ?? '',
});

/** Server 422 field errors land on the matching inputs (ui/05 §4). */
function applyServerErrors(form: UseFormReturn<Values>, error: unknown): boolean {
  if (!isApiError(error) || error.status !== 422 || error.errors.length === 0) return false;
  for (const [field, message] of Object.entries(error.fieldErrors()))
    form.setError(field as Path<Values>, { message });
  return true;
}

/** A calendar date (no time, no zone): formatted as written, never shifted by the viewer's time zone. */
const calendarDate = (locale: string, isoDate: string) =>
  new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeZone: 'UTC' }).format(
    new Date(`${isoDate}T00:00:00Z`),
  );

const initials = (name: string) =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('');

/** ACC-07 My profile: the account, the identity the school keeps, and the contact details the user edits. */
export function ProfileScreen() {
  const { t, formatLocale } = useI18n();
  const me = useSession((s) => s.me);
  const person = useMyPerson();
  const user = me?.user;
  const name = person.data?.data.displayName ?? user?.username ?? '';
  return (
    <div className="page">
      <Card className="profile-header">
        <div className="profile-header__avatar" aria-hidden="true">
          {initials(name) || '?'}
        </div>
        <div>
          <h1 className="page__title">{name}</h1>
          <p className="page__subtitle">{user?.username}</p>
          <ul className="chip-list" aria-label={t('acc.profile.roles')}>
            {(me?.roles ?? []).map((r) => (
              <li key={`${r.roleCode}-${r.scopeType}-${r.scopeId ?? ''}`}>
                <StatusChip intent="info" label={r.roleCode} />
              </li>
            ))}
          </ul>
        </div>
      </Card>
      <div className="page__grid">
        <Card title={t('acc.profile.account')}>
          <dl className="facts">
            <dt>{t('acc.profile.username')}</dt>
            <dd>{user?.username ?? '—'}</dd>
            <dt>{t('acc.profile.email')}</dt>
            <dd>{user?.email ?? '—'}</dd>
          </dl>
        </Card>
        {user?.personId ? (
          <Card title={t('acc.profile.identity')}>
            {person.isPending ? (
              <Skeleton label={t('common.state.loading')} lines={2} />
            ) : (
              <>
                <dl className="facts">
                  <dt>{t('acc.profile.displayName')}</dt>
                  <dd>{person.data?.data.displayName ?? '—'}</dd>
                  <dt>{t('acc.profile.dateOfBirth')}</dt>
                  <dd>
                    {person.data?.data.dateOfBirth
                      ? calendarDate(formatLocale, String(person.data.data.dateOfBirth).slice(0, 10))
                      : '—'}
                  </dd>
                </dl>
                <p className="hint">{t('acc.profile.identityNote')}</p>
              </>
            )}
          </Card>
        ) : null}
      </div>
      {!user?.personId ? (
        <Banner intent="info">{t('acc.profile.noPerson')}</Banner>
      ) : person.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void person.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : person.data ? (
        <ContactForm
          person={person.data.data}
          etag={person.data.etag}
          onReload={() => void person.refetch()}
        />
      ) : (
        <Skeleton label={t('common.state.loading')} lines={6} />
      )}
    </div>
  );
}

function ContactForm({
  person,
  etag,
  onReload,
}: {
  person: Person;
  etag: string | null;
  onReload: () => void;
}) {
  const { t } = useI18n();
  const update = useUpdateMyPerson();
  const [notice, setNotice] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const form = useForm<Values>({ resolver: zodResolver(schema), values: valuesOf(person) });
  const save = form.handleSubmit((values) => {
    if (!etag) return;
    const dirty = Object.keys(form.formState.dirtyFields) as Field[];
    if (dirty.length === 0) return;
    // a cleared field is sent as null with its name in the update mask, so the server clears it
    const changes = Object.fromEntries(
      dirty.map((k) => [k, values[k] === '' ? null : values[k]]),
    ) as MyPersonUpdate;
    setNotice(null);
    update.mutate(
      { etag, changes },
      {
        onSuccess: () => setNotice(t('acc.profile.saved')),
        onError: (error) => {
          if (isApiError(error) && error.isConflict) setConflict(true);
          else if (!applyServerErrors(form, error)) setNotice(null);
        },
      },
    );
  });
  const failed =
    update.error && !(isApiError(update.error) && (update.error.isConflict || update.error.status === 422));
  return (
    <form noValidate onSubmit={save} aria-describedby={notice ? 'profile-notice' : undefined}>
      {notice ? (
        <Banner intent="success">
          <span id="profile-notice" role="status">
            {notice}
          </span>
        </Banner>
      ) : null}
      {conflict ? (
        <Banner
          intent="warning"
          title={t('common.conflict.title')}
          action={
            <Button
              variant="secondary"
              onClick={() => {
                setConflict(false);
                onReload();
              }}
            >
              {t('common.conflict.reload')}
            </Button>
          }
        >
          {t('common.conflict.body')}
        </Banner>
      ) : null}
      {failed ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      <div className="page__grid">
        <Card title={t('acc.profile.contact')}>
          {CONTACT.map((field) => (
            <TextInput
              key={field}
              label={t(`acc.profile.${field}`)}
              type={INPUT_TYPE[field] ?? 'text'}
              autoComplete={AUTOCOMPLETE[field]}
              error={fieldMessage(t, form.formState.errors[field], LIMITS[field])}
              {...form.register(field)}
            />
          ))}
        </Card>
        <Card title={t('acc.profile.about')}>
          <TextArea
            label={t('acc.profile.aboutMe')}
            hint={t('acc.profile.aboutMeHint')}
            rows={8}
            error={fieldMessage(t, form.formState.errors.aboutMe, LIMITS.aboutMe)}
            {...form.register('aboutMe')}
          />
        </Card>
      </div>
      <div className="page__actions">
        <Button
          variant="secondary"
          onClick={() => form.reset(valuesOf(person))}
          disabled={!form.formState.isDirty}
        >
          {t('common.action.cancel')}
        </Button>
        <Button
          type="submit"
          loading={update.isPending}
          disabled={!etag || conflict || !form.formState.isDirty}
        >
          {t('common.action.save')}
        </Button>
      </div>
    </form>
  );
}
