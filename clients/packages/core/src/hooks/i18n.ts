import { createFormatters, createTranslator, type Formatters, type Translate } from '@mysms/i18n';
import { useMemo } from 'react';
import { useRuntime } from '../react/useRuntime';
import { explicitSetting, settingValue } from '../settings/preferences';
import { useSession } from './session';

export interface I18n {
  /** Catalogue locale (nearest translation): user → tenant default → en. */
  locale: string;
  /** Formatting locale: the user's full locale (e.g. en-IN) when Intl supports it, else the catalogue locale. */
  formatLocale: string;
  t: Translate;
  format: Formatters;
}

const supported = (tag: string | null | undefined): tag is string => {
  if (!tag) return false;
  try {
    return Intl.DateTimeFormat.supportedLocalesOf(tag).length > 0;
  } catch {
    return false;
  }
};

/**
 * Strings come from the nearest catalogue, but dates, numbers and money use the user's own locale and time
 * zone (ui/04 §7): an en-IN user reads English text with Indian date and digit-grouping conventions.
 */
export function useI18n(): I18n {
  const runtime = useRuntime();
  // an explicit language or time zone setting (ui/04 §9) wins over the account's own
  const userLocale = useSession(
    (s) => explicitSetting(s.settings, 'ui.locale') ?? s.me?.user?.locale ?? null,
  );
  const timeZone = useSession(
    (s) =>
      explicitSetting(s.settings, 'ui.timezone') ??
      s.me?.user?.timezone ??
      settingValue(s.settings, 'ui.timezone') ??
      undefined,
  );
  const locale = runtime.locale();
  const formatLocale = supported(userLocale) ? userLocale : locale;
  return useMemo(
    () => ({
      locale,
      formatLocale,
      t: createTranslator(locale),
      format: createFormatters(formatLocale, timeZone),
    }),
    [locale, formatLocale, timeZone],
  );
}
