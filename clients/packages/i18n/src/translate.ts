import { IntlMessageFormat } from 'intl-messageformat';
import { en, type Catalog, type MessageKey } from './catalogs/en';

export type Values = Record<string, string | number | Date | boolean>;
export type Translate = (key: MessageKey, values?: Values) => string;

/** The locales the apps ship a catalogue for. en is built in; every other one loads on first use. */
export const LOCALES: readonly string[] = ['en', 'hi'];

/** How each catalogue but en is fetched: its own chunk, so the initial route carries English only (ui/05 §10). */
const LOADERS: Readonly<Record<string, () => Promise<Partial<Catalog>>>> = {
  hi: () => import('./catalogs/hi').then((module) => module.hi),
};
const loaded = new Map<string, Partial<Catalog>>([['en', en]]);
const listeners = new Set<() => void>();
let version = 0;

/**
 * Loads a locale's catalogue once (en and unknown locales need nothing); listeners hear when one arrives, so a
 * screen first shown in English re-renders in the reader's language (Observer).
 */
export async function loadCatalog(locale: string): Promise<void> {
  const load = LOADERS[locale];
  if (!load || loaded.has(locale)) return;
  loaded.set(locale, await load());
  version += 1;
  for (const listener of listeners) listener();
}

/** Counts the catalogues loaded so far: a snapshot for React's `useSyncExternalStore`. */
export const catalogVersion = (): number => version;

/** Calls `listener` each time a catalogue arrives; returns the way to stop. */
export function onCatalogLoaded(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** user locale → tenant default → en (ui/04 §7); only locales with a catalogue are chosen. */
export function resolveLocale(
  userLocale?: string | null,
  tenantDefault?: string | null,
  available: readonly string[] = LOCALES,
): string {
  for (const candidate of [userLocale, tenantDefault]) {
    if (!candidate) continue;
    if (available.includes(candidate)) return candidate;
    const language = candidate.split('-')[0];
    if (language && available.includes(language)) return language;
  }
  return 'en';
}

/**
 * A translator for one locale, from its catalogue as loaded now (`loadCatalog`) or the one given. Missing keys — and
 * every key while the catalogue is still loading — fall back to en; formats are cached per key.
 */
export function createTranslator(
  locale: string,
  catalog: Partial<Catalog> = loaded.get(locale) ?? {},
): Translate {
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
