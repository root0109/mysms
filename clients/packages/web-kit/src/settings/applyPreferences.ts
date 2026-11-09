import type { UiPreferences } from '@mysms/core';

/**
 * Applies the interface preferences to the document root, where the token stylesheet reads them (ui/04 §9):
 * `data-theme` (absent = follow the device), `data-density`, `data-reduced-motion`, `--sms-text-scale` and `lang`.
 */
export function applyPreferences(preferences: UiPreferences, locale: string, root: HTMLElement): void {
  if (preferences.theme === 'system') root.removeAttribute('data-theme');
  else
    root.setAttribute(
      'data-theme',
      preferences.theme === 'high_contrast' ? 'high-contrast' : preferences.theme,
    );
  root.setAttribute('data-density', preferences.density);
  if (preferences.reducedMotion) root.setAttribute('data-reduced-motion', 'true');
  else root.removeAttribute('data-reduced-motion');
  root.style.setProperty('--sms-text-scale', String(preferences.textScale / 100));
  root.setAttribute('lang', locale);
}
