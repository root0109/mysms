import { describe, expect, it } from 'vitest';
import type { EffectiveSetting } from '../auth/session';
import { DEFAULT_PREFERENCES, explicitSetting, preferencesFrom, settingValue } from './preferences';

const s = (settingKey: string, value: string, sourceScope: EffectiveSetting['sourceScope'] = 'user') =>
  ({ settingKey, value, sourceScope, locked: false, userEditable: true }) as EffectiveSetting;

describe('preferences from effective settings (ui/04 §9)', () => {
  it('uses the defaults without settings', () => {
    expect(preferencesFrom(null)).toEqual(DEFAULT_PREFERENCES);
    expect(preferencesFrom(undefined)).toEqual(DEFAULT_PREFERENCES);
    expect(preferencesFrom([])).toEqual(DEFAULT_PREFERENCES);
  });

  it('reads every interface setting', () => {
    expect(
      preferencesFrom([
        s('ui.theme', 'high_contrast'),
        s('ui.density', 'compact'),
        s('ui.textScale', '125'),
        s('ui.reducedMotion', 'true'),
        s('ui.locale', 'hi'),
        s('ui.timezone', 'Europe/London'),
        s('ui.dateFormat', 'yyyy-MM-dd'),
        s('ui.landingScreen', 'ACC-07'),
      ]),
    ).toEqual({
      theme: 'high_contrast',
      density: 'compact',
      textScale: 125,
      reducedMotion: true,
      locale: 'hi',
      timezone: 'Europe/London',
      dateFormat: 'yyyy-MM-dd',
      landingScreen: 'ACC-07',
    });
  });

  it('falls back on values this client does not know', () => {
    const p = preferencesFrom([
      s('ui.theme', 'neon'),
      s('ui.density', 'cosy'),
      s('ui.textScale', '300'),
      s('ui.reducedMotion', 'yes'),
      s('ui.locale', ''),
    ]);
    expect(p.theme).toBe('system');
    expect(p.density).toBe('comfortable');
    expect(p.textScale).toBe(100);
    expect(p.reducedMotion).toBe(false);
    expect(p.locale).toBe('en');
    expect(preferencesFrom([s('ui.textScale', '12.5')]).textScale).toBe(100);
  });

  it('tells set values from catalogue defaults', () => {
    const settings = [s('ui.locale', 'en', 'default'), s('ui.timezone', 'Asia/Dubai', 'tenant')];
    expect(settingValue(settings, 'ui.locale')).toBe('en');
    expect(explicitSetting(settings, 'ui.locale')).toBeUndefined();
    expect(explicitSetting(settings, 'ui.timezone')).toBe('Asia/Dubai');
    expect(settingValue(settings, 'missing')).toBeUndefined();
    expect(explicitSetting(null, 'ui.locale')).toBeUndefined();
  });
});
