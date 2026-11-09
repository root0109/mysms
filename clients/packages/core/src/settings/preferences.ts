import type { EffectiveSetting } from '../auth/session';

/** The interface preferences the clients apply from the effective settings (ui/04 §9). */
export interface UiPreferences {
  theme: 'system' | 'light' | 'dark' | 'high_contrast';
  density: 'comfortable' | 'compact';
  /** Text size in percent, 100–150. */
  textScale: number;
  reducedMotion: boolean;
  /** Catalogue language, e.g. `en` or `hi`. */
  locale: string;
  /** IANA time zone. */
  timezone: string;
  dateFormat: string;
  /** Screen id to open after sign-in; empty = the role's home. */
  landingScreen: string;
}

/** What applies when the server sent no settings (the catalogue defaults of iam V1_030_003). */
export const DEFAULT_PREFERENCES: UiPreferences = {
  theme: 'system',
  density: 'comfortable',
  textScale: 100,
  reducedMotion: false,
  locale: 'en',
  timezone: 'Asia/Kolkata',
  dateFormat: 'dd-MM-yyyy',
  landingScreen: '',
};

const THEMES: readonly UiPreferences['theme'][] = ['system', 'light', 'dark', 'high_contrast'];
const DENSITIES: readonly UiPreferences['density'][] = ['comfortable', 'compact'];

/** A setting's effective value, whatever its source (default included). */
export function settingValue(
  settings: readonly EffectiveSetting[] | null | undefined,
  key: string,
): string | undefined {
  return settings?.find((s) => s.settingKey === key)?.value;
}

/** A setting's value only when some scope set it (not the catalogue default). */
export function explicitSetting(
  settings: readonly EffectiveSetting[] | null | undefined,
  key: string,
): string | undefined {
  const found = settings?.find((s) => s.settingKey === key);
  return found && found.sourceScope !== 'default' ? found.value : undefined;
}

function oneOf<T extends string>(value: string | undefined, allowed: readonly T[], fallback: T): T {
  return value !== undefined && (allowed as readonly string[]).includes(value) ? (value as T) : fallback;
}

/**
 * Typed preferences from the effective settings. Anything missing or out of range (an older server, a newer
 * catalogue value this client does not know) falls back to the default, so the interface always renders.
 */
export function preferencesFrom(settings: readonly EffectiveSetting[] | null | undefined): UiPreferences {
  const get = (key: string) => settingValue(settings, key);
  const scale = Number(get('ui.textScale'));
  return {
    theme: oneOf(get('ui.theme'), THEMES, DEFAULT_PREFERENCES.theme),
    density: oneOf(get('ui.density'), DENSITIES, DEFAULT_PREFERENCES.density),
    textScale:
      Number.isInteger(scale) && scale >= 100 && scale <= 150 ? scale : DEFAULT_PREFERENCES.textScale,
    reducedMotion: get('ui.reducedMotion') === 'true',
    locale: get('ui.locale') || DEFAULT_PREFERENCES.locale,
    timezone: get('ui.timezone') || DEFAULT_PREFERENCES.timezone,
    dateFormat: get('ui.dateFormat') || DEFAULT_PREFERENCES.dateFormat,
    landingScreen: get('ui.landingScreen') ?? DEFAULT_PREFERENCES.landingScreen,
  };
}
