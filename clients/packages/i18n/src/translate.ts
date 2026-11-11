import { IntlMessageFormat } from 'intl-messageformat';
import { en, type Catalog, type MessageKey } from './catalogs/en';
import { hi } from './catalogs/hi';

export type Values = Record<string, string | number | Date | boolean>;
export type Translate = (key: MessageKey, values?: Values) => string;

export const CATALOGS: Record<string, Partial<Catalog>> = { en, hi };

/** user locale → tenant default → en (ui/04 §7); only locales with a catalogue are chosen. */
export function resolveLocale(
  userLocale?: string | null,
  tenantDefault?: string | null,
  available = Object.keys(CATALOGS),
): string {
  for (const candidate of [userLocale, tenantDefault]) {
    if (!candidate) continue;
    if (available.includes(candidate)) return candidate;
    const language = candidate.split('-')[0];
    if (language && available.includes(language)) return language;
  }
  return 'en';
}

/** A translator for one locale. Missing keys fall back to en; formats are cached per key. */
export function createTranslator(
  locale: string,
  catalogs: Record<string, Partial<Catalog>> = CATALOGS,
): Translate {
  const catalog = catalogs[locale] ?? {};
  const cache = new Map<MessageKey, IntlMessageFormat>();
  return (key, values) => {
    let fmt = cache.get(key);
    if (!fmt) {
      const translated = catalog[key];
      fmt = new IntlMessageFormat(translated ?? en[key], translated ? locale : 'en');
      cache.set(key, fmt);
    }
    return String(fmt.format(values));
  };
}

const enumKeySegment = (segment: string) =>
  segment.replace(/_([a-z0-9])/g, (_match, letter: string) => letter.toUpperCase());

/** Enum labels come from the catalogue keyed by the API value; an unknown value is never shown raw (ui/04 §7). */
export function enumLabel(t: Translate, group: string, value: string | null | undefined): string {
  const key = `enum.${enumKeySegment(group)}.${enumKeySegment(value ?? '')}`;
  return key in en ? t(key as MessageKey) : t('enum.common.unknown');
}
