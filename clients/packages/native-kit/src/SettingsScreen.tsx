import {
  isApiError,
  settingCategoryLabel,
  settingLabel,
  settingSourceLabel,
  settingValueLabel,
  useI18n,
  useMySettings,
  useReloadMySettings,
  useResetMySetting,
  useSetMySetting,
  useSettingDefinitions,
  type EffectiveSetting,
  type SettingDefinition,
} from '@mysms/core';
import { Banner, Button, ChoiceGroup, Screen, SwitchRow, Text } from '@mysms/ui-native';
import { View } from 'react-native';

const CATEGORY_ORDER = ['appearance', 'language', 'navigation', 'notifications', 'security'];
const SCALE_STEPS = ['100', '110', '125', '150'];

/** ACC-14 on phones and tablets: the user-editable settings as large controls; locked ones say who decides. */
export function SettingsScreen({ tablet = false, onBack }: { tablet?: boolean; onBack: () => void }) {
  const { t } = useI18n();
  const settings = useMySettings();
  const definitions = useSettingDefinitions();
  const reload = useReloadMySettings();
  const byKey = new Map((definitions.data?.items ?? []).map((d) => [d.settingKey, d]));
  const groups = new Map<string, EffectiveSetting[]>();
  for (const s of settings ?? []) {
    const definition = byKey.get(s.settingKey);
    // free-text settings (time zone, start screen) are edited on the web
    if (!definition || definition.valueType === 'string') continue;
    const category = definition.category ?? 'other';
    groups.set(category, [...(groups.get(category) ?? []), s]);
  }
  const ordered = [...groups.keys()].sort(
    (a, b) => (CATEGORY_ORDER.indexOf(a) + 1 || 99) - (CATEGORY_ORDER.indexOf(b) + 1 || 99),
  );
  return (
    <Screen tablet={tablet}>
      <Button variant="ghost" onPress={onBack}>
        {t('shell.nav.home')}
      </Button>
      <Text variant="h1">{t('acc.settings.title')}</Text>
      <Text muted>{t('acc.settings.intro')}</Text>
      {definitions.isError ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      {definitions.isPending ? <Text muted>{t('common.state.loading')}</Text> : null}
      {!settings && !definitions.isPending ? (
        <>
          <Banner intent="danger">{t('common.error.generic')}</Banner>
          <Button variant="secondary" loading={reload.isPending} onPress={() => reload.mutate()}>
            {t('common.action.retry')}
          </Button>
        </>
      ) : null}
      {ordered.map((category) => (
        <View key={category}>
          <Text variant="h2">{settingCategoryLabel(t, category)}</Text>
          {(groups.get(category) ?? []).map((s) => (
            <NativeSettingRow key={s.settingKey} setting={s} definition={byKey.get(s.settingKey)} />
          ))}
        </View>
      ))}
    </Screen>
  );
}

function NativeSettingRow({
  setting,
  definition,
}: {
  setting: EffectiveSetting;
  definition: SettingDefinition | undefined;
}) {
  const { t } = useI18n();
  const set = useSetMySetting();
  const reset = useResetMySetting();
  const label = settingLabel(t, setting.settingKey, definition?.description);
  const editable = setting.userEditable && !setting.locked && !set.isPending && !reset.isPending;
  const hint = setting.locked
    ? t('acc.settings.locked')
    : !setting.userEditable
      ? t('acc.settings.adminOnly')
      : settingSourceLabel(t, setting.sourceScope);
  const save = (value: string) => {
    if (value !== setting.value) set.mutate({ settingKey: setting.settingKey, value });
  };
  const error = set.error ?? reset.error;
  return (
    <View>
      {definition?.valueType === 'boolean' ? (
        <SwitchRow
          label={label}
          hint={hint}
          value={setting.value === 'true'}
          disabled={!editable}
          onChange={(on) => save(String(on))}
        />
      ) : (
        <ChoiceGroup
          label={label}
          hint={hint}
          value={setting.value}
          disabled={!editable}
          onChange={save}
          choices={(definition?.valueType === 'choice'
            ? (definition.allowedValues ?? [])
            : setting.settingKey === 'ui.textScale'
              ? [...new Set([...SCALE_STEPS, setting.value])]
              : [setting.value]
          ).map((v) => ({
            value: v,
            label: setting.settingKey === 'ui.textScale' ? `${v} %` : settingValueLabel(t, v),
          }))}
        />
      )}
      {error ? (
        <Banner intent="danger">
          {isApiError(error) && error.code === 'SETTING_LOCKED'
            ? t('acc.settings.lockedError')
            : t('common.error.generic')}
        </Banner>
      ) : null}
      {setting.sourceScope === 'user' && editable ? (
        <Button variant="ghost" loading={reset.isPending} onPress={() => reset.mutate(setting.settingKey)}>
          {t('acc.settings.reset')}
        </Button>
      ) : null}
    </View>
  );
}
