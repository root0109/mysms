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
import { Banner, Button, Screen, SwitchRow, Text, TextInput } from '@mysms/ui-native';
import { useEffect, useState } from 'react';

/** ACC-10: the current account's notification preferences, grouped by role set. */
export function NotificationPreferencesScreen({
  tablet = false,
  onBack,
}: {
  tablet?: boolean;
  onBack: () => void;
}) {
  const { t } = useI18n();
  const userId = useSession((state) => state.me?.user?.id ?? null);
  const sets = useNotificationPreferenceSets(userId);
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('acc.notifications.title')}</Text>
      <Text muted>{t('acc.notifications.intro')}</Text>
      <Button variant="secondary" onPress={onBack}>
        {t('common.action.back')}
      </Button>
      {sets.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {sets.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void sets.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {!sets.isPending && !sets.isError && (sets.data?.items.length ?? 0) === 0 ? (
        <Text>{t('acc.notifications.empty')}</Text>
      ) : null}
      {(sets.data?.items ?? []).map((preferenceSet, index) => (
        <PreferenceSetRows
          key={preferenceSet.id}
          preferenceSet={preferenceSet}
          roleNumber={index + 1}
          tablet={tablet}
        />
      ))}
    </Screen>
  );
}

function PreferenceSetRows({
  preferenceSet,
  roleNumber,
  tablet,
}: {
  preferenceSet: NotificationPreferenceSet;
  roleNumber: number;
  tablet: boolean;
}) {
  const { t } = useI18n();
  const preferences = useNotificationPreferences(preferenceSet.id ?? null);
  const title = preferenceSet.roleId
    ? t('acc.notifications.roleSpecific', { number: roleNumber })
    : t('acc.notifications.allRoles');
  return (
    <>
      <Text variant="h2">{title}</Text>
      {preferences.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {preferences.isError ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" onPress={() => void preferences.refetch()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {!preferences.isPending && !preferences.isError && (preferences.data?.items.length ?? 0) === 0 ? (
        <Text>{t('acc.notifications.noPreferences')}</Text>
      ) : null}
      {(preferences.data?.items ?? []).map((preference) => (
        <PreferenceRow
          key={preference.id}
          preferenceSetId={preferenceSet.id ?? ''}
          preference={preference}
          tablet={tablet}
          onReload={() => void preferences.refetch()}
        />
      ))}
    </>
  );
}

function PreferenceRow({
  preferenceSetId,
  preference,
  tablet,
  onReload,
}: {
  preferenceSetId: string;
  preference: NotificationPreference;
  tablet: boolean;
  onReload: () => void;
}) {
  const { t } = useI18n();
  const update = useUpdateNotificationPreference();
  const etag = preference.etag ?? '';
  const label = preference.propertyName
    ? t('acc.notifications.preferenceType', { name: preference.propertyName })
    : t('acc.notifications.preference');
  const isBoolean = ['true', 'false'].includes(preference.propertyValue?.toLowerCase() ?? '');
  const isConflict = isApiError(update.error) && update.error.status === 412;
  const [value, setValue] = useState(preference.propertyValue ?? '');
  useEffect(() => setValue(preference.propertyValue ?? ''), [preference.propertyValue]);
  const reloadLatest = () => {
    update.reset();
    onReload();
  };
  const retryUpdate = () => {
    if (update.variables) update.mutate(update.variables);
  };
  if (isBoolean)
    return (
      <>
        <SwitchRow
          label={label}
          value={preference.propertyValue?.toLowerCase() === 'true'}
          disabled={update.isPending || !etag}
          onChange={(checked) =>
            update.mutate({
              preferenceSetId,
              preferenceId: preference.id ?? '',
              etag,
              changes: { propertyValue: String(checked) },
            })
          }
        />
        {update.isError ? (
          <Text>{isConflict ? t('common.conflict.body') : t('common.error.generic')}</Text>
        ) : null}
        {update.isError ? (
          <Button variant="secondary" onPress={isConflict ? reloadLatest : retryUpdate}>
            {isConflict ? t('common.conflict.reload') : t('common.action.retry')}
          </Button>
        ) : null}
      </>
    );
  return (
    <>
      <TextInput
        label={label}
        value={value}
        editable={!update.isPending && Boolean(etag)}
        onChangeText={setValue}
        style={tablet ? { maxWidth: 480 } : undefined}
      />
      <Button
        variant="secondary"
        loading={update.isPending}
        disabled={!etag || value === (preference.propertyValue ?? '')}
        onPress={() =>
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
      {update.isError ? (
        <Text>{isConflict ? t('common.conflict.body') : t('common.error.generic')}</Text>
      ) : null}
      {update.isError ? (
        <Button variant="secondary" onPress={isConflict ? reloadLatest : retryUpdate}>
          {isConflict ? t('common.conflict.reload') : t('common.action.retry')}
        </Button>
      ) : null}
    </>
  );
}
