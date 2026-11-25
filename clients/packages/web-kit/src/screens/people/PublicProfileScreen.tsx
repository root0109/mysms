import {
  isApiError,
  resourceDef,
  useCan,
  useCreateRecord,
  useI18n,
  useRecord,
  useSession,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton } from '@mysms/ui-web';
import { Fragment, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { UUID } from '../ids';
import { RecordDialog } from '../RecordsScreen';
import { display, problemText } from '../recordText';

const PROFILES = resourceDef('people.PublicProfile');

/** Where the account's own consents are decided (ACC-13), for a profile that needs the public-profile consent. */
const consentCentre = (personId: string) => `/app/people/persons/${personId}/consents`;

/**
 * On the account's profile (ACC-07): makes the person's public profile (people.CreatePublicProfile) and opens it in
 * its editor (ACC-12). The profile rests on the person's public-profile consent: without it the server answers
 * CONSENT_REQUIRED and the card points to the consent centre instead.
 */
export function CreatePublicProfile() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const personId = useSession((s) => s.me?.user?.personId ?? '');
  const mayCreate = useCan('people.public_profile.create');
  const create = useCreateRecord(PROFILES as NonNullable<typeof PROFILES>);
  if (!mayCreate || !personId) return null;
  const needsConsent = isApiError(create.error) && create.error.code === 'CONSENT_REQUIRED';
  return (
    <Card title={t('people.publicProfile.title')}>
      <p className="hint">{t('people.publicProfile.createHint')}</p>
      {needsConsent ? (
        <Banner
          intent="warning"
          action={<Link to={consentCentre(personId)}>{t('people.publicProfile.toConsents')}</Link>}
        >
          {t('people.publicProfile.needsConsent')}
        </Banner>
      ) : create.error ? (
        <Banner intent="danger">{problemText(t, create.error)}</Banner>
      ) : null}
      <Button
        variant="secondary"
        loading={create.isPending}
        onClick={() =>
          create.mutate(
            { parentId: personId, values: {} },
            { onSuccess: (row) => void navigate(`/app/people/public-profiles/${row.id}`) },
          )
        }
      >
        {t('people.publicProfile.create')}
      </Button>
    </Card>
  );
}

/**
 * ACC-12 Public profile editor: the signed-in person's own public profile (headline, profession, links, whether
 * private details stay hidden) — read and changed under its ETag, or removed. Only the person's own profile is
 * reached (the person comes from the session, never from the address); a profile that is not theirs, or one whose
 * consent was withdrawn for others, reads as not available.
 */
export function PublicProfileScreen() {
  const { t, format } = useI18n();
  const navigate = useNavigate();
  const { publicProfileId = '' } = useParams();
  const personId = useSession((s) => s.me?.user?.personId ?? '');
  const mayRead = useCan('people.public_profile.read');
  const mayUpdate = useCan('people.public_profile.update');
  const mayDelete = useCan('people.public_profile.delete');
  const valid = UUID.test(publicProfileId) && personId !== '';
  const profile = useRecord(PROFILES, mayRead && valid ? publicProfileId : null, personId);
  const [editing, setEditing] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);
  const title = <h1 className="page__title">{t('screen.ACC12.title')}</h1>;
  if (!PROFILES || !mayRead)
    return (
      <div className="page">
        {title}
        <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
      </div>
    );
  if (!valid)
    return (
      <div className="page">
        {title}
        <Banner intent="warning">{t('common.state.notAvailable')}</Banner>
      </div>
    );
  if (profile.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (profile.isError)
    return (
      <div className="page">
        {title}
        <Banner intent={isApiError(profile.error) && profile.error.status === 404 ? 'warning' : 'danger'}>
          {isApiError(profile.error) && profile.error.status === 404
            ? t('common.state.notAvailable')
            : problemText(t, profile.error)}
        </Banner>
      </div>
    );
  const row = profile.data.data;
  const etag = profile.data.etag ?? row.etag ?? null;
  return (
    <div className="page">
      <header className="page__header">
        {title}
        <p className="page__subtitle">{t('people.publicProfile.intro')}</p>
        <div className="page__actions">
          {mayUpdate || mayDelete ? (
            <Button variant="secondary" onClick={() => setEditing(true)}>
              {t('common.action.edit')}
            </Button>
          ) : null}
          <Link to={consentCentre(personId)}>{t('people.publicProfile.toConsents')}</Link>
        </div>
      </header>
      {saved ? <Banner intent="success">{saved}</Banner> : null}
      <Card title={t('people.publicProfile.title')}>
        <dl className="facts">
          {PROFILES.fields
            .filter((f) => f.kind !== 'id')
            .map((f) => (
              <Fragment key={f.name}>
                <dt>{f.label}</dt>
                <dd>{display(t, f, row[f.name], format)}</dd>
              </Fragment>
            ))}
        </dl>
      </Card>
      {editing ? (
        <RecordDialog
          def={PROFILES}
          parentId={personId}
          row={{ ...row, ...(etag ? { etag } : {}) }}
          canUpdate={mayUpdate}
          canDelete={mayDelete}
          onClose={() => setEditing(false)}
          onDone={(message, deleted) => {
            setEditing(false);
            if (deleted) void navigate('/app/people/me', { replace: true });
            else setSaved(message);
          }}
        />
      ) : null}
    </div>
  );
}
