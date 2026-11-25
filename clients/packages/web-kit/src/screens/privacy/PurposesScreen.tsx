import {
  isApiError,
  useCan,
  useI18n,
  useNotices,
  usePurposes,
  useUpdatePurpose,
  type Purpose,
  type PurposeUpdate,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, SelectField, Skeleton, StatusChip, Switch } from '@mysms/ui-web';
import { useEffect, useState } from 'react';

/** The ceilings a width purpose can have, narrowest first (privacy.tenant_purpose.max_scope). */
const CEILINGS: readonly NonNullable<PurposeUpdate['maxScope']>[] = [
  'class',
  'institution',
  'inter_school',
  'public',
];

/**
 * ADM-17 Privacy purposes (ui/02): the platform's purpose catalogue with the school's setting of each — the switch,
 * the notice families read before deciding and, for a width purpose, the widest sharing the school allows. A
 * purpose MySMS decides, or one no longer active, is shown but cannot be switched on; the server checks every rule
 * again and its answer is shown on the field.
 */
export function PurposesScreen() {
  const { t } = useI18n();
  const purposes = usePurposes();
  const notices = useNotices(useCan('privacy.notice.read'));
  if (purposes.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (purposes.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void purposes.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const items = purposes.data.items;
  const noticeOptions = (notices.data?.items ?? []).map((n) => ({
    value: n.id ?? '',
    label: `${n.title} (${n.code})`,
  }));
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('privacy.purposes.title')}</h1>
        <p className="page__subtitle">{t('privacy.purposes.intro')}</p>
      </header>
      {items.length === 0 ? (
        <Card>
          <p>{t('privacy.purposes.empty')}</p>
        </Card>
      ) : (
        <div className="page__grid">
          {items.map((purpose) => (
            <PurposeCard key={purpose.id} purpose={purpose} noticeOptions={noticeOptions} />
          ))}
        </div>
      )}
    </div>
  );
}

/** One purpose: what it is, and the school's setting of it as a small form saved under the row's ETag. */
function PurposeCard({
  purpose,
  noticeOptions,
}: {
  purpose: Purpose;
  noticeOptions: readonly { value: string; label: string }[];
}) {
  const { t } = useI18n();
  const mayUpdate = useCan('privacy.tenant_purpose.update');
  const update = useUpdatePurpose();
  const width = purpose.scopeKind === 'width';
  const platform = purpose.fiduciary === 'platform';
  const [enabled, setEnabled] = useState(purpose.enabled ?? false);
  const [noticeId, setNoticeId] = useState(purpose.noticeId ?? '');
  const [maxScope, setMaxScope] = useState<string>(purpose.maxScope ?? '');
  const [saved, setSaved] = useState(false);
  // the form follows the stored setting whenever it changes (after a save, or a reload after a conflict)
  useEffect(() => {
    setEnabled(purpose.enabled ?? false);
    setNoticeId(purpose.noticeId ?? '');
    setMaxScope(purpose.maxScope ?? '');
  }, [purpose.enabled, purpose.noticeId, purpose.maxScope, purpose.etag]);
  const changed =
    enabled !== (purpose.enabled ?? false) ||
    noticeId !== (purpose.noticeId ?? '') ||
    maxScope !== (purpose.maxScope ?? '');
  const error = update.error;
  const conflict = isApiError(error) && error.status === 412;
  const fieldError = (field: string) =>
    isApiError(error) ? error.errors.find((e) => e.field === field)?.message : undefined;
  const { id: purposeId, etag } = purpose;
  const save = () => {
    // the server sends both on every row ("0" while the school has never set the purpose)
    if (!purposeId || !etag) return;
    const changes: PurposeUpdate = {
      enabled,
      noticeId: noticeId || undefined,
      ...(width ? { maxScope: CEILINGS.find((c) => c === maxScope) } : {}),
    };
    setSaved(false);
    update.mutate({ purposeId, etag, changes }, { onSuccess: () => setSaved(true) });
  };
  return (
    <Card
      title={enumLabel(t, 'privacyPurpose', purpose.code)}
      actions={
        <StatusChip
          intent={purpose.enabled ? 'success' : 'neutral'}
          label={t(purpose.enabled ? 'privacy.purposes.on' : 'privacy.purposes.off')}
        />
      }
    >
      <div className="consent-card">
        <p className="hint">
          {[
            purpose.code,
            t(width ? 'privacy.purposes.widths' : 'privacy.purposes.wholeSchool'),
            purpose.status && purpose.status !== 'active'
              ? enumLabel(t, 'purposeStatus', purpose.status)
              : null,
          ]
            .filter(Boolean)
            .join(' · ')}
        </p>
        {platform ? <p>{t('privacy.purposes.platform')}</p> : null}
        {purpose.forbiddenForMinors ? <p>{t('privacy.purposes.forbiddenForMinors')}</p> : null}
        {mayUpdate ? (
          <>
            <Switch
              label={t('privacy.purposes.enabled')}
              checked={enabled}
              disabled={update.isPending || ((platform || purpose.status !== 'active') && !enabled)}
              error={fieldError('enabled')}
              onChange={setEnabled}
            />
            <SelectField
              label={t('privacy.purposes.notice')}
              options={[
                { value: '', label: t('privacy.purposes.noNotice') },
                ...noticeOptions,
                // a notice the role cannot list is still the stored one
                ...(noticeId && !noticeOptions.some((o) => o.value === noticeId)
                  ? [{ value: noticeId, label: noticeId }]
                  : []),
              ]}
              value={noticeId}
              disabled={update.isPending}
              error={fieldError('noticeId')}
              onChange={(event) => setNoticeId(event.currentTarget.value)}
            />
            {width ? (
              <SelectField
                label={t('privacy.purposes.ceiling')}
                options={[
                  { value: '', label: t('privacy.purposes.noCeiling') },
                  ...CEILINGS.map((c) => ({ value: c, label: enumLabel(t, 'consentScope', c) })),
                ]}
                value={maxScope}
                disabled={update.isPending}
                error={fieldError('maxScope')}
                onChange={(event) => setMaxScope(event.currentTarget.value)}
              />
            ) : null}
            {saved && !changed ? <Banner intent="success">{t('privacy.purposes.saved')}</Banner> : null}
            {error && !fieldError('enabled') && !fieldError('noticeId') && !fieldError('maxScope') ? (
              <Banner intent="danger">{t(conflict ? 'common.conflict.body' : 'common.error.generic')}</Banner>
            ) : null}
            <div className="consent-card__actions">
              {conflict ? (
                <Button variant="ghost" onClick={() => update.reset()}>
                  {t('common.conflict.reload')}
                </Button>
              ) : null}
              <Button
                variant="primary"
                loading={update.isPending}
                disabled={!changed || !purposeId || !etag}
                onClick={save}
              >
                {t('common.action.save')}
              </Button>
            </div>
          </>
        ) : null}
      </div>
    </Card>
  );
}
