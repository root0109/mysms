import {
  isApiError,
  useCan,
  useCreateNotice,
  useCreateNoticeTranslation,
  useCreateNoticeVersion,
  useDeleteNotice,
  useDeleteNoticeTranslation,
  useI18n,
  useNoticeTranslations,
  useNoticeVersions,
  useNotices,
  usePublishNoticeVersion,
  usePurposes,
  useUpdateNotice,
  useUpdateNoticeTranslation,
  useUpdateNoticeVersion,
  type GrievanceContact,
  type Notice,
  type NoticeTranslation,
  type NoticeVersion,
  type NoticeVersionDraft,
} from '@mysms/core';
import { CATALOGS, enumLabel, type MessageKey } from '@mysms/i18n';
import {
  Banner,
  Button,
  Card,
  Dialog,
  SelectField,
  Skeleton,
  StatusChip,
  Switch,
  TextArea,
  TextInput,
} from '@mysms/ui-web';
import { useState } from 'react';

/** The text of a failed change: the server's own sentence for a refused step (409), else conflict or generic. */
function failure(t: (key: MessageKey) => string, error: unknown): string {
  if (isApiError(error) && error.status === 409 && error.detail) return error.detail;
  if (isApiError(error) && error.status === 412) return t('common.conflict.body');
  return t('common.error.generic');
}

/** The server's sentence for one field of a refused request (422), already in the user's language. */
const fieldError = (error: unknown, field: string) =>
  isApiError(error) ? error.errors.find((e) => e.field === field)?.message : undefined;

/**
 * ADM-18 Privacy notices (ui/02): the school's notices, each a series of versions. A version is written as a draft
 * — the purposes it asks about, whether it asks everyone again, the grievance contact and its wording per language —
 * and publishing it makes it the version families read (the one before it retires). A published version and its
 * wording never change: they are what families were shown.
 */
export function NoticesScreen() {
  const { t } = useI18n();
  const notices = useNotices();
  const mayCreate = useCan('privacy.notice.create');
  const [openId, setOpenId] = useState<string | null>(null);
  if (notices.isPending) return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (notices.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void notices.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const items = notices.data.items;
  const open = items.find((n) => n.id === openId);
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.notices.title')}</h1>
        <p className="page__subtitle">{t('privacy.notices.intro')}</p>
      </header>
      {mayCreate ? <NewNotice /> : null}
      {items.length === 0 ? (
        <Card>
          <p>{t('privacy.notices.empty')}</p>
        </Card>
      ) : (
        <div className="page__grid">
          {items.map((notice) => (
            <NoticeCard
              key={`${notice.id}|${notice.etag}`}
              notice={notice}
              onOpen={() => setOpenId(notice.id ?? null)}
            />
          ))}
        </div>
      )}
      {open ? <Versions notice={open} /> : null}
    </div>
  );
}

function NewNotice() {
  const { t } = useI18n();
  const create = useCreateNotice();
  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  return (
    <Card title={t('privacy.notices.add')}>
      <div className="consent-card">
        <TextInput
          label={t('privacy.notices.code')}
          hint={t('privacy.notices.codeHint')}
          value={code}
          error={fieldError(create.error, 'code')}
          onChange={(event) => setCode(event.currentTarget.value)}
        />
        <TextInput
          label={t('privacy.notices.noticeTitle')}
          value={title}
          error={fieldError(create.error, 'title')}
          onChange={(event) => setTitle(event.currentTarget.value)}
        />
        {create.error && !isApiError(create.error) ? (
          <Banner intent="danger">{t('common.error.generic')}</Banner>
        ) : null}
        <div className="consent-card__actions">
          <Button
            variant="primary"
            loading={create.isPending}
            disabled={!code.trim() || !title.trim()}
            onClick={() =>
              create.mutate(
                { code: code.trim(), title: title.trim() },
                {
                  onSuccess: () => {
                    setCode('');
                    setTitle('');
                  },
                },
              )
            }
          >
            {t('privacy.notices.add')}
          </Button>
        </div>
      </div>
    </Card>
  );
}

/** One notice: its title (renamed under the row's ETag), deletion, and the way into its versions. */
function NoticeCard({ notice, onOpen }: { notice: Notice; onOpen: () => void }) {
  const { t } = useI18n();
  const mayUpdate = useCan('privacy.notice.update');
  const mayDelete = useCan('privacy.notice.delete');
  const mayVersions = useCan('privacy.notice_version.read');
  const rename = useUpdateNotice();
  const remove = useDeleteNotice();
  const [title, setTitle] = useState(notice.title);
  const [confirming, setConfirming] = useState(false);
  const etag = notice.etag ?? '';
  return (
    <Card title={notice.title}>
      <div className="consent-card">
        <p className="hint">{notice.code}</p>
        {mayUpdate ? (
          <TextInput
            label={t('privacy.notices.noticeTitle')}
            value={title}
            error={fieldError(rename.error, 'title')}
            onChange={(event) => setTitle(event.currentTarget.value)}
          />
        ) : null}
        {rename.error && !fieldError(rename.error, 'title') ? (
          <Banner intent="danger">{failure(t, rename.error)}</Banner>
        ) : null}
        {remove.error ? <Banner intent="danger">{failure(t, remove.error)}</Banner> : null}
        <div className="consent-card__actions">
          {mayVersions ? (
            <Button variant="secondary" onClick={onOpen}>
              {t('privacy.notices.open')}
            </Button>
          ) : null}
          {mayUpdate ? (
            <Button
              variant="secondary"
              loading={rename.isPending}
              disabled={!title.trim() || title === notice.title}
              onClick={() => rename.mutate({ noticeId: notice.id ?? '', etag, title: title.trim() })}
            >
              {t('privacy.notices.rename')}
            </Button>
          ) : null}
          {mayDelete ? (
            <Button variant="ghost" onClick={() => setConfirming(true)}>
              {t('privacy.notices.delete')}
            </Button>
          ) : null}
        </div>
      </div>
      <Dialog
        open={confirming}
        title={t('privacy.notices.delete')}
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={remove.isPending}
              onClick={() =>
                remove.mutate({ noticeId: notice.id ?? '', etag }, { onSettled: () => setConfirming(false) })
              }
            >
              {t('common.action.delete')}
            </Button>
          </>
        }
      >
        <p>{t('privacy.notices.deleteBody')}</p>
      </Dialog>
    </Card>
  );
}

/** The versions of the open notice, newest first, and a new draft when none is being written. */
function Versions({ notice }: { notice: Notice }) {
  const { t } = useI18n();
  const noticeId = notice.id ?? '';
  const versions = useNoticeVersions(noticeId);
  const mayCreate = useCan('privacy.notice_version.create');
  const purposes = usePurposes(useCan('privacy.tenant_purpose.read'));
  const items = versions.data?.items ?? [];
  const latest = items[0];
  const hasDraft = items.some((v) => v.status === 'draft');
  const purposeCodes = [
    ...new Set([
      ...(purposes.data?.items ?? []).filter((p) => p.status === 'active').map((p) => p.code ?? ''),
      ...items.flatMap((v) => v.purposeCodes),
    ]),
  ]
    .filter(Boolean)
    .sort();
  return (
    <section className="page" aria-labelledby="notice-versions">
      <h2 id="notice-versions" className="page__title">
        {t('privacy.notices.versions', { title: notice.title })}
      </h2>
      {versions.isPending ? <Skeleton label={t('common.state.loading')} lines={4} /> : null}
      {versions.isError ? (
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void versions.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      ) : null}
      {versions.data && items.length === 0 ? <p className="hint">{t('privacy.notices.noVersions')}</p> : null}
      {mayCreate && versions.data && !hasDraft ? (
        <NewVersion noticeId={noticeId} latest={latest} purposeCodes={purposeCodes} />
      ) : null}
      {items.map((version) => (
        <VersionCard
          key={`${version.id}|${version.etag}`}
          noticeId={noticeId}
          version={version}
          purposeCodes={purposeCodes}
        />
      ))}
    </section>
  );
}

/** One version: a draft is a form with its wording and can be published; a published one is shown as it was. */
function VersionCard({
  noticeId,
  version,
  purposeCodes,
}: {
  noticeId: string;
  version: NoticeVersion;
  purposeCodes: readonly string[];
}) {
  const { t, format } = useI18n();
  const draft = version.status === 'draft';
  const mayUpdate = useCan('privacy.notice_version.update') && draft;
  const mayPublish = useCan('privacy.notice_version.publish') && draft;
  const update = useUpdateNoticeVersion();
  const publish = usePublishNoticeVersion();
  const [form, setForm] = useState<NoticeVersionDraft>({
    purposeCodes: version.purposeCodes,
    grievanceContact: version.grievanceContact,
    requiresReconsent: version.requiresReconsent ?? false,
  });
  const [confirming, setConfirming] = useState(false);
  const etag = version.etag ?? '';
  return (
    <Card
      title={t('privacy.notices.version', { version: version.version ?? 0 })}
      actions={
        <StatusChip
          intent={
            version.status === 'active' ? 'success' : version.status === 'draft' ? 'warning' : 'neutral'
          }
          label={enumLabel(t, 'noticeVersionStatus', version.status)}
        />
      }
    >
      <div className="consent-card">
        {version.publishedAt ? (
          <p className="hint">{t('privacy.notices.published', { date: format.date(version.publishedAt) })}</p>
        ) : null}
        {mayUpdate ? (
          <VersionFields form={form} setForm={setForm} purposeCodes={purposeCodes} error={update.error} />
        ) : (
          <>
            <p>{version.purposeCodes.map((code) => enumLabel(t, 'privacyPurpose', code)).join(', ')}</p>
            {version.requiresReconsent ? <p>{t('privacy.notices.requiresReconsent')}</p> : null}
            <p>
              {[
                version.grievanceContact.name,
                version.grievanceContact.role,
                version.grievanceContact.email,
                version.grievanceContact.phone,
              ]
                .filter(Boolean)
                .join(' · ')}
            </p>
            {!draft ? <p className="hint">{t('privacy.notices.frozen')}</p> : null}
          </>
        )}
        {update.error ? <Banner intent="danger">{failure(t, update.error)}</Banner> : null}
        {publish.error ? <Banner intent="danger">{failure(t, publish.error)}</Banner> : null}
        <div className="consent-card__actions">
          {mayUpdate ? (
            <Button
              variant="secondary"
              loading={update.isPending}
              onClick={() =>
                update.mutate({
                  noticeId,
                  noticeVersionId: version.id ?? '',
                  etag,
                  changes: cleaned(form),
                })
              }
            >
              {t('privacy.notices.saveDraft')}
            </Button>
          ) : null}
          {mayPublish ? (
            <Button variant="primary" onClick={() => setConfirming(true)}>
              {t('privacy.notices.publish')}
            </Button>
          ) : null}
        </div>
        <Translations noticeId={noticeId} version={version} />
      </div>
      <Dialog
        open={confirming}
        title={t('privacy.notices.publish')}
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              {t('common.action.cancel')}
            </Button>
            <Button
              variant="primary"
              loading={publish.isPending}
              onClick={() =>
                publish.mutate(
                  { noticeId, noticeVersionId: version.id ?? '', etag },
                  { onSettled: () => setConfirming(false) },
                )
              }
            >
              {t('privacy.notices.publish')}
            </Button>
          </>
        }
      >
        <p>{t('privacy.notices.publishBody', { version: version.version ?? 0 })}</p>
      </Dialog>
    </Card>
  );
}

/** The languages a notice is written in: those the apps have a catalogue for. */
const LANGUAGES = Object.keys(CATALOGS);

/** A version's wording per language; while the version is a draft each one is edited, added or removed. */
function Translations({ noticeId, version }: { noticeId: string; version: NoticeVersion }) {
  const { t } = useI18n();
  const translations = useNoticeTranslations(noticeId, version.id ?? null);
  const draft = version.status === 'draft';
  const mayCreate = useCan('privacy.notice_translation.create') && draft;
  const items = translations.data?.items ?? [];
  const missing = LANGUAGES.filter((code) => !items.some((tr) => tr.language === code));
  return (
    <section className="consent-card">
      <h3>{t('privacy.notices.translations')}</h3>
      {translations.isPending ? <Skeleton label={t('common.state.loading')} lines={2} /> : null}
      {translations.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {translations.data && items.length === 0 ? (
        <p className="hint">{t('privacy.notices.noTranslations')}</p>
      ) : null}
      {items.map((translation) => (
        <Wording
          key={`${translation.id}|${translation.etag}`}
          noticeId={noticeId}
          version={version}
          translation={translation}
        />
      ))}
      {mayCreate && missing.length > 0 ? (
        <Wording noticeId={noticeId} version={version} languages={missing} />
      ) : null}
    </section>
  );
}

/** One language's wording: an editor while the version is a draft (a new one when `translation` is absent). */
function Wording({
  noticeId,
  version,
  translation,
  languages = [],
}: {
  noticeId: string;
  version: NoticeVersion;
  translation?: NoticeTranslation;
  languages?: readonly string[];
}) {
  const { t } = useI18n();
  const draft = version.status === 'draft';
  const mayUpdate = useCan('privacy.notice_translation.update') && draft;
  const mayDelete = useCan('privacy.notice_translation.delete') && draft;
  const create = useCreateNoticeTranslation();
  const update = useUpdateNoticeTranslation();
  const remove = useDeleteNoticeTranslation();
  const [language, setLanguage] = useState(translation?.language ?? languages[0] ?? 'en');
  const [body, setBody] = useState(translation?.bodyMarkdown ?? '');
  const [summary, setSummary] = useState(translation?.summaryForChildren ?? '');
  const target = { noticeId, noticeVersionId: version.id ?? '' };
  const error = create.error ?? update.error ?? remove.error;
  const languageLabel = (code: string) => t(`setting.value.${code}` as MessageKey);
  if (translation && !mayUpdate)
    return (
      <article>
        <h4>{languageLabel(translation.language)}</h4>
        {translation.summaryForChildren ? (
          <p className="consent-notice">{translation.summaryForChildren}</p>
        ) : null}
        <p className="consent-notice">{translation.bodyMarkdown}</p>
      </article>
    );
  const wording = {
    bodyMarkdown: body,
    ...(summary.trim() ? { summaryForChildren: summary } : {}),
  };
  return (
    <article className="consent-card">
      {translation ? (
        <h4>{languageLabel(translation.language)}</h4>
      ) : (
        <SelectField
          label={t('privacy.notices.language')}
          options={languages.map((code) => ({ value: code, label: languageLabel(code) }))}
          value={language}
          onChange={(event) => setLanguage(event.currentTarget.value)}
        />
      )}
      <TextArea
        label={t('privacy.notices.body')}
        value={body}
        rows={8}
        error={fieldError(error, 'bodyMarkdown')}
        onChange={(event) => setBody(event.currentTarget.value)}
      />
      <TextArea
        label={t('privacy.notices.summaryForChildren')}
        value={summary}
        rows={3}
        error={fieldError(error, 'summaryForChildren')}
        onChange={(event) => setSummary(event.currentTarget.value)}
      />
      {error && !fieldError(error, 'bodyMarkdown') && !fieldError(error, 'summaryForChildren') ? (
        <Banner intent="danger">{failure(t, error)}</Banner>
      ) : null}
      <div className="consent-card__actions">
        <Button
          variant="secondary"
          loading={create.isPending || update.isPending}
          disabled={!body.trim()}
          onClick={() =>
            translation
              ? update.mutate({
                  ...target,
                  noticeTranslationId: translation.id ?? '',
                  etag: translation.etag ?? '',
                  // an emptied summary is cleared: it is in the mask, absent from the body
                  changes: { bodyMarkdown: body, summaryForChildren: summary.trim() ? summary : undefined },
                })
              : create.mutate(
                  { ...target, wording: { language, ...wording } },
                  {
                    onSuccess: () => {
                      setBody('');
                      setSummary('');
                    },
                  },
                )
          }
        >
          {translation ? t('privacy.notices.saveTranslation') : t('privacy.notices.addTranslation')}
        </Button>
        {translation && mayDelete ? (
          <Button
            variant="ghost"
            loading={remove.isPending}
            onClick={() =>
              remove.mutate({
                ...target,
                noticeTranslationId: translation.id ?? '',
                etag: translation.etag ?? '',
              })
            }
          >
            {t('privacy.notices.deleteTranslation')}
          </Button>
        ) : null}
      </div>
    </article>
  );
}

/** The draft as it is sent: trimmed contact, optional parts left out when blank. */
function cleaned(form: NoticeVersionDraft): NoticeVersionDraft {
  const { name, email, role, phone } = form.grievanceContact;
  return {
    purposeCodes: form.purposeCodes,
    requiresReconsent: form.requiresReconsent ?? false,
    grievanceContact: {
      name: name.trim(),
      email: email.trim(),
      ...(role?.trim() ? { role: role.trim() } : {}),
      ...(phone?.trim() ? { phone: phone.trim() } : {}),
    },
  };
}

/** What a version asks, as form fields: the purposes, asking everyone again, the grievance contact. */
function VersionFields({
  form,
  setForm,
  purposeCodes,
  error,
}: {
  form: NoticeVersionDraft;
  setForm: (form: NoticeVersionDraft) => void;
  purposeCodes: readonly string[];
  error: unknown;
}) {
  const { t } = useI18n();
  const contact = (field: keyof GrievanceContact, value: string) =>
    setForm({ ...form, grievanceContact: { ...form.grievanceContact, [field]: value } });
  const togglePurpose = (code: string, on: boolean) =>
    setForm({
      ...form,
      purposeCodes: on ? [...form.purposeCodes, code] : form.purposeCodes.filter((c) => c !== code),
    });
  return (
    <>
      <fieldset className="consent-card">
        <legend>{t('privacy.notices.purposes')}</legend>
        {purposeCodes.map((code) => (
          <Switch
            key={code}
            label={enumLabel(t, 'privacyPurpose', code)}
            checked={form.purposeCodes.includes(code)}
            onChange={(on) => togglePurpose(code, on)}
          />
        ))}
        {fieldError(error, 'purposeCodes') ? <p role="alert">{fieldError(error, 'purposeCodes')}</p> : null}
      </fieldset>
      <Switch
        label={t('privacy.notices.requiresReconsent')}
        hint={t('privacy.notices.requiresReconsentHint')}
        checked={form.requiresReconsent ?? false}
        onChange={(on) => setForm({ ...form, requiresReconsent: on })}
      />
      <TextInput
        label={t('privacy.notices.grievanceName')}
        value={form.grievanceContact.name}
        error={fieldError(error, 'grievanceContact.name')}
        onChange={(event) => contact('name', event.currentTarget.value)}
      />
      <TextInput
        label={t('privacy.notices.grievanceRole')}
        value={form.grievanceContact.role ?? ''}
        onChange={(event) => contact('role', event.currentTarget.value)}
      />
      <TextInput
        label={t('privacy.notices.grievanceEmail')}
        type="email"
        value={form.grievanceContact.email}
        error={fieldError(error, 'grievanceContact.email')}
        onChange={(event) => contact('email', event.currentTarget.value)}
      />
      <TextInput
        label={t('privacy.notices.grievancePhone')}
        value={form.grievanceContact.phone ?? ''}
        onChange={(event) => contact('phone', event.currentTarget.value)}
      />
    </>
  );
}

/** The next version of the notice, written before it is created: it starts from what the latest one asked. */
function NewVersion({
  noticeId,
  latest,
  purposeCodes,
}: {
  noticeId: string;
  latest: NoticeVersion | undefined;
  purposeCodes: readonly string[];
}) {
  const { t } = useI18n();
  const create = useCreateNoticeVersion();
  const [form, setForm] = useState<NoticeVersionDraft>({
    purposeCodes: latest?.purposeCodes ?? [],
    grievanceContact: latest?.grievanceContact ?? { name: '', email: '' },
    requiresReconsent: false,
  });
  return (
    <Card title={t('privacy.notices.newVersion')}>
      <div className="consent-card">
        <VersionFields form={form} setForm={setForm} purposeCodes={purposeCodes} error={create.error} />
        {create.error ? <Banner intent="danger">{failure(t, create.error)}</Banner> : null}
        <div className="consent-card__actions">
          <Button
            variant="primary"
            loading={create.isPending}
            disabled={form.purposeCodes.length === 0}
            onClick={() => create.mutate({ noticeId, draft: cleaned(form) })}
          >
            {t('privacy.notices.newVersion')}
          </Button>
        </div>
      </div>
    </Card>
  );
}
