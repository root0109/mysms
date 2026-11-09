import {
  isApiError,
  useI18n,
  useMyPerson,
  useSession,
  useUpdateMyPerson,
  type MyPersonUpdate,
} from '@mysms/core';
import { Banner, Button, Screen, Text, TextInput } from '@mysms/ui-native';
import { useEffect, useState } from 'react';

type Field =
  | 'primaryEmail'
  | 'primaryPhone'
  | 'alternatePhone'
  | 'workPhone'
  | 'websiteUrl'
  | 'aboutMe'
  | 'motherTongue';
const FIELDS: readonly Field[] = [
  'primaryEmail',
  'primaryPhone',
  'alternatePhone',
  'workPhone',
  'websiteUrl',
  'motherTongue',
  'aboutMe',
];
const KEYBOARD: Partial<Record<Field, 'email-address' | 'phone-pad' | 'url'>> = {
  primaryEmail: 'email-address',
  primaryPhone: 'phone-pad',
  alternatePhone: 'phone-pad',
  workPhone: 'phone-pad',
  websiteUrl: 'url',
};

/** ACC-07 on phones and tablets: who I am, and the contact details I keep up to date myself. */
export function ProfileScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  const user = useSession((s) => s.me?.user);
  const person = useMyPerson();
  const update = useUpdateMyPerson();
  const [draft, setDraft] = useState<Record<Field, string>>({} as Record<Field, string>);
  const [saved, setSaved] = useState(false);
  const current = person.data?.data;
  useEffect(() => {
    if (current) {
      setDraft(Object.fromEntries(FIELDS.map((f) => [f, current[f] ?? ''])) as Record<Field, string>);
    }
  }, [current]);
  const changed = current ? FIELDS.filter((f) => (current[f] ?? '') !== (draft[f] ?? '')) : [];
  const fieldError = (f: Field) =>
    isApiError(update.error) && update.error.status === 422 ? update.error.fieldErrors()[f] : undefined;
  const save = () => {
    const etag = person.data?.etag;
    if (!etag || changed.length === 0) return;
    setSaved(false);
    const changes = Object.fromEntries(
      changed.map((f) => [f, draft[f].trim() === '' ? null : draft[f].trim()]),
    ) as MyPersonUpdate;
    update.mutate({ etag, changes }, { onSuccess: () => setSaved(true) });
  };
  return (
    <Screen tablet={tablet}>
      <Button variant="ghost" onPress={onBack}>
        {t('shell.nav.home')}
      </Button>
      <Text variant="h1">{current?.displayName ?? user?.username ?? ''}</Text>
      <Text muted>{user?.username ?? ''}</Text>
      {!user?.personId ? (
        <Banner intent="info">{t('acc.profile.noPerson')}</Banner>
      ) : person.isError ? (
        <Banner intent="danger">{t('common.error.generic')}</Banner>
      ) : !current ? (
        <Text muted>{t('common.state.loading')}</Text>
      ) : (
        <>
          <Text muted>{t('acc.profile.identityNote')}</Text>
          {saved ? <Banner intent="success">{t('acc.profile.saved')}</Banner> : null}
          {isApiError(update.error) && update.error.isConflict ? (
            <>
              <Banner intent="warning">{t('common.conflict.body')}</Banner>
              <Button
                variant="secondary"
                onPress={() => {
                  update.reset();
                  void person.refetch();
                }}
              >
                {t('common.conflict.reload')}
              </Button>
            </>
          ) : update.error && !(isApiError(update.error) && update.error.status === 422) ? (
            <Banner intent="danger">{t('common.error.generic')}</Banner>
          ) : null}
          <Text variant="h2">{t('acc.profile.contact')}</Text>
          {FIELDS.map((f) => (
            <TextInput
              key={f}
              label={t(`acc.profile.${f}`)}
              value={draft[f] ?? ''}
              onChangeText={(v) => setDraft((d) => ({ ...d, [f]: v }))}
              keyboardType={KEYBOARD[f] ?? 'default'}
              autoCapitalize={KEYBOARD[f] ? 'none' : 'sentences'}
              multiline={f === 'aboutMe'}
              error={fieldError(f)}
            />
          ))}
          <Button loading={update.isPending} disabled={changed.length === 0} onPress={save}>
            {t('common.action.save')}
          </Button>
        </>
      )}
    </Screen>
  );
}
