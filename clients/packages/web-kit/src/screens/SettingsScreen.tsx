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
import { Banner, Button, Card, SelectField, Skeleton, StatusChip, Switch, TextInput } from '@mysms/ui-web';
import { useState } from 'react';

const CATEGORY_ORDER = ['appearance', 'language', 'navigation', 'notifications', 'security'];

/** IANA zones the browser knows, when it can list them. */
function timeZones(current: string): string[] {
  const intl = Intl as unknown as { supportedValuesOf?: (key: string) => string[] };
  const zones = intl.supportedValuesOf?.('timeZone') ?? [];
  return zones.includes(current) ? zones : [current, ...zones];
}

/** Choices for an integer setting with a small range (text size 100–150 % in steps of 5). */
function integerSteps(definition: SettingDefinition, current: string): string[] | null {
  const { minValue: min, maxValue: max } = definition;
  if (min === undefined || max === undefined || max - min > 100) return null;
  const steps: string[] = [];
  for (let v = min; v <= max; v += 5) steps.push(String(v));
  if (!steps.includes(String(max))) steps.push(String(max));
  return steps.includes(current) ? steps : [...steps, current];
}

/**
 * ACC-14 My settings: every catalogue setting with its effective value, where that value comes from and whether an
 * administrator locked it (ui/04 §9). Changes save at once and apply to the whole app immediately.
 */
export function SettingsScreen() {
  const { t } = useI18n();
  const settings = useMySettings();
  const definitions = useSettingDefinitions();
  const [notice, setNotice] = useState<string | null>(null);
  const reload = useReloadMySettings();
  if (!settings || definitions.isPending) {
    // settings that failed to load with the session are null: offer a retry instead of waiting forever
    const failed = definitions.isError || (!settings && !reload.isPending);
    return failed ? (
      <Banner
        intent="danger"
        action={
          <Button
            variant="secondary"
            loading={reload.isPending}
            onClick={() => {
              void definitions.refetch();
              if (!settings) reload.mutate();
            }}
          >
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    ) : (
      <Skeleton label={t('common.state.loading')} lines={8} />
    );
  }
  const byKey = new Map((definitions.data?.items ?? []).map((d) => [d.settingKey, d]));
  const categories = new Map<string, EffectiveSetting[]>();
  for (const setting of settings) {
    const category = byKey.get(setting.settingKey)?.category ?? 'other';
    categories.set(category, [...(categories.get(category) ?? []), setting]);
  }
  const ordered = [...categories.keys()].sort(
    (a, b) => (CATEGORY_ORDER.indexOf(a) + 1 || 99) - (CATEGORY_ORDER.indexOf(b) + 1 || 99),
  );
  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('acc.settings.title')}</h1>
        <p className="page__subtitle">{t('acc.settings.intro')}</p>
      </header>
      <p className="sr-only" role="status" aria-live="polite">
        {notice ?? ''}
      </p>
      <div className="page__grid">
        {ordered.map((category) => (
          <Card key={category} title={settingCategoryLabel(t, category)}>
            <ul className="settings-list">
              {(categories.get(category) ?? [])
                .slice()
                .sort(
                  (a, b) =>
                    (byKey.get(a.settingKey)?.sortOrder ?? 0) - (byKey.get(b.settingKey)?.sortOrder ?? 0),
                )
                .map((setting) => (
                  <li key={setting.settingKey} className="settings-list__row">
                    <SettingRow
                      setting={setting}
                      definition={byKey.get(setting.settingKey)}
                      onSaved={setNotice}
                    />
                  </li>
                ))}
            </ul>
          </Card>
        ))}
        <Card title={t('acc.settings.previewTitle')} className="settings-preview">
          <p className="settings-preview__text">{t('acc.settings.preview')}</p>
        </Card>
      </div>
    </div>
  );
}

function SettingRow({
  setting,
  definition,
  onSaved,
}: {
  setting: EffectiveSetting;
  definition: SettingDefinition | undefined;
  onSaved: (message: string) => void;
}) {
  const { t } = useI18n();
  const set = useSetMySetting();
  const reset = useResetMySetting();
  const label = settingLabel(t, setting.settingKey, definition?.description);
  const editable = setting.userEditable && !setting.locked;
  const busy = set.isPending || reset.isPending;
  const save = (value: string) => {
    if (value === setting.value) return;
    set.mutate(
      { settingKey: setting.settingKey, value },
      { onSuccess: () => onSaved(t('acc.settings.saved', { setting: label })) },
    );
  };
  const error = set.error ?? reset.error;
  const errorText = error
    ? isApiError(error) && error.code === 'SETTING_LOCKED'
      ? t('acc.settings.lockedError')
      : isApiError(error) && error.status === 422
        ? (error.errors[0]?.message ?? t('common.validation.pattern'))
        : t('common.error.generic')
    : undefined;
  const hint = setting.locked
    ? t('acc.settings.locked')
    : !setting.userEditable
      ? t('acc.settings.adminOnly')
      : definition?.description;
  return (
    <>
      <div className="settings-list__control">
        <SettingControl
          key={setting.value}
          setting={setting}
          definition={definition}
          label={label}
          hint={hint}
          disabled={!editable || busy}
          error={errorText}
          onSave={save}
        />
      </div>
      <div className="settings-list__meta">
        <StatusChip
          intent={setting.locked ? 'warning' : setting.sourceScope === 'user' ? 'info' : 'neutral'}
          label={setting.locked ? t('acc.settings.locked') : settingSourceLabel(t, setting.sourceScope)}
        />
        {setting.sourceScope === 'user' && editable ? (
          <Button
            variant="ghost"
            loading={reset.isPending}
            onClick={() =>
              reset.mutate(setting.settingKey, {
                onSuccess: () => onSaved(t('acc.settings.saved', { setting: label })),
              })
            }
          >
            {t('acc.settings.reset')}
          </Button>
        ) : null}
      </div>
    </>
  );
}

function SettingControl({
  setting,
  definition,
  label,
  hint,
  disabled,
  error,
  onSave,
}: {
  setting: EffectiveSetting;
  definition: SettingDefinition | undefined;
  label: string;
  hint: string | undefined;
  disabled: boolean;
  error: string | undefined;
  onSave: (value: string) => void;
}) {
  const { t } = useI18n();
  const [draft, setDraft] = useState(setting.value);
  const type = definition?.valueType;
  if (type === 'boolean') {
    return (
      <Switch
        label={label}
        hint={hint}
        error={error}
        checked={setting.value === 'true'}
        disabled={disabled}
        onChange={(on) => onSave(on ? 'true' : 'false')}
      />
    );
  }
  const choices =
    type === 'choice'
      ? (definition?.allowedValues ?? [])
      : type === 'integer' && definition
        ? integerSteps(definition, setting.value)
        : setting.settingKey === 'ui.timezone'
          ? timeZones(setting.value)
          : null;
  if (choices) {
    return (
      <SelectField
        label={label}
        hint={hint}
        error={error}
        value={setting.value}
        disabled={disabled}
        onChange={(e) => onSave(e.target.value)}
        options={choices.map((value) => ({
          value,
          label: setting.settingKey === 'ui.textScale' ? `${value} %` : settingValueLabel(t, value),
        }))}
      />
    );
  }
  return (
    <form
      className="settings-list__text"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        onSave(draft.trim());
      }}
    >
      <TextInput
        label={label}
        hint={hint}
        error={error}
        value={draft}
        disabled={disabled}
        maxLength={500}
        onChange={(e) => setDraft(e.target.value)}
      />
      <Button type="submit" variant="secondary" disabled={disabled || draft.trim() === setting.value}>
        {t('common.action.save')}
      </Button>
    </form>
  );
}
