import {
  isApiError,
  useI18n,
  useNotificationPreferenceSets,
  useNotificationPreferences,
  useSession,
  useUpdateNotificationPreference,
  type NotificationPreference,
  type NotificationPreferenceSet,
} from '@mysms/core';
import { Banner, Button, Card, Skeleton, Switch, TextInput } from '@mysms/ui-web';
import { useEffect, useState } from 'react';

/** ACC-10: the current account's notification preferences, grouped by the role set they apply to. */
export function NotificationPreferencesScreen() {
  const { t } = useI18n();
  const userId = useSession((state) => state.me?.user?.id ?? null);
  const sets = useNotificationPreferenceSets(userId);

  if (sets.isPending) return <Skeleton label={t('common.state.loading')} lines={5} />;
  if (sets.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void sets.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );

  const items = sets.data?.items ?? [];
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('acc.notifications.title')}</h1>
        <p className="page__subtitle">{t('acc.notifications.intro')}</p>
      </header>
      {items.length === 0 ? (
        <Card>
          <p>{t('acc.notifications.empty')}</p>
        </Card>
      ) : (
        <div className="page__grid">
          {items.map((set, index) => (
            <PreferenceSetCard key={set.id} preferenceSet={set} roleNumber={index + 1} />
          ))}
        </div>
      )}
    </div>
  );
}

function PreferenceSetCard({
  preferenceSet,
  roleNumber,
}: {
  preferenceSet: NotificationPreferenceSet;
  roleNumber: number;
}) {
  const { t } = useI18n();
  const preferences = useNotificationPreferences(preferenceSet.id ?? null);
  const roleLabel = preferenceSet.roleId
    ? t('acc.notifications.roleSpecific', { number: roleNumber })
    : t('acc.notifications.allRoles');
  if (preferences.isPending)
    return (
      <Card title={roleLabel}>
        <Skeleton label={t('common.state.loading')} lines={3} />
      </Card>
    );
  if (preferences.isError)
    return (
      <Card title={roleLabel}>
        <Banner
          intent="danger"
          action={
            <Button variant="secondary" onClick={() => void preferences.refetch()}>
              {t('common.action.retry')}
            </Button>
          }
        >
          {t('common.error.generic')}
        </Banner>
      </Card>
    );
  const items = preferences.data?.items ?? [];
  return (
    <Card title={roleLabel}>
      {items.length === 0 ? (
        <p>{t('acc.notifications.noPreferences')}</p>
      ) : (
        <ul className="settings-list">
          {items.map((preference) => (
            <li key={preference.id} className="settings-list__row">
              <PreferenceRow
                preferenceSetId={preferenceSet.id ?? ''}
                preference={preference}
                onReload={() => void preferences.refetch()}
              />
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

function PreferenceRow({
  preferenceSetId,
  preference,
  onReload,
}: {
  preferenceSetId: string;
  preference: NotificationPreference;
  onReload: () => void;
}) {
  const { t } = useI18n();
  const update = useUpdateNotificationPreference();
  const on = preference.propertyValue?.toLowerCase() === 'true';
  const isBoolean = ['true', 'false'].includes(preference.propertyValue?.toLowerCase() ?? '');
  const etag = preference.etag ?? '';
  const [value, setValue] = useState(preference.propertyValue ?? '');
  useEffect(() => setValue(preference.propertyValue ?? ''), [preference.propertyValue]);
  const error = update.error;
  const isConflict = isApiError(error) && error.status === 412;
  const errorText = error ? (isConflict ? t('common.conflict.body') : t('common.error.generic')) : undefined;
  const label = preference.propertyName
    ? t('acc.notifications.preferenceType', { name: preference.propertyName })
    : t('acc.notifications.preference');
  const reloadLatest = () => {
    update.reset();
    onReload();
  };
  return (
    <div className="settings-list__control">
      {isBoolean ? (
        <>
          <Switch
            label={label}
            checked={on}
            disabled={update.isPending || !etag}
            error={errorText}
            onChange={(checked) =>
              update.mutate({
                preferenceSetId,
                preferenceId: preference.id ?? '',
                etag,
                changes: { propertyValue: String(checked) },
              })
            }
          />
          {isConflict ? (
            <Button variant="ghost" onClick={reloadLatest}>
              {t('common.conflict.reload')}
            </Button>
          ) : null}
        </>
      ) : (
        <div>
          <TextInput
            label={label}
            value={value}
            disabled={update.isPending || !etag}
            error={errorText}
            onChange={(event) => setValue(event.currentTarget.value)}
          />
          {isConflict ? (
            <Button variant="ghost" onClick={reloadLatest}>
              {t('common.conflict.reload')}
            </Button>
          ) : null}
          <Button
            variant="secondary"
            disabled={update.isPending || !etag || value === (preference.propertyValue ?? '')}
            onClick={() =>
              update.mutate({
                preferenceSetId,
                preferenceId: preference.id ?? '',
                etag,
                changes: { propertyValue: value },
              })
            }
          >
            {t('common.action.save')}
          </Button>
        </div>
      )}
    </div>
  );
}
