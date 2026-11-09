import { en, type MessageKey, type Translate } from '@mysms/i18n';

/** Catalogue keys are camelCase segments; contract values may be snake_case (`high_contrast`, `class_section`). */
const camel = (value: string) => value.replace(/_([a-z0-9])/g, (_m, c: string) => c.toUpperCase());

function known(key: string): key is MessageKey {
  return key in en;
}

/** A setting's name; a setting this client has no text for yet shows the catalogue's own description. */
export function settingLabel(t: Translate, settingKey: string, fallback?: string): string {
  const key = `setting.${settingKey}`;
  return known(key) ? t(key) : (fallback ?? settingKey);
}

/** A choice value's text (`dark` → "Dark"); values without text (date patterns, ids) show as they are. */
export function settingValueLabel(t: Translate, value: string): string {
  const key = `setting.value.${camel(value)}`;
  return value !== '' && known(key) ? t(key) : value;
}

/** Where an effective value comes from: default, tenant, institution, department, class_section, user. */
export function settingSourceLabel(t: Translate, scope: string): string {
  const key = `acc.settings.source.${camel(scope)}`;
  return known(key) ? t(key) : scope;
}

export function settingCategoryLabel(t: Translate, category: string | undefined): string {
  const key = `acc.settings.category.${camel(category ?? 'other')}`;
  return known(key) ? t(key) : t('acc.settings.category.other');
}

/** A scope an administrator sets a value at. */
export function settingScopeLabel(t: Translate, scope: string): string {
  const key = `adm.settings.scope.${camel(scope)}`;
  return known(key) ? t(key) : scope;
}
