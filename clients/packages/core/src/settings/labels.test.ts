import { createTranslator } from '@mysms/i18n';
import { describe, expect, it } from 'vitest';
import {
  settingCategoryLabel,
  settingLabel,
  settingScopeLabel,
  settingSourceLabel,
  settingValueLabel,
} from './labels';

const t = createTranslator('en');

describe('setting labels', () => {
  it('names settings, values, sources, categories and scopes', () => {
    expect(settingLabel(t, 'ui.theme')).toBe('Theme');
    expect(settingLabel(t, 'ui.newThing', 'A new thing.')).toBe('A new thing.');
    expect(settingLabel(t, 'ui.newThing')).toBe('ui.newThing');
    expect(settingValueLabel(t, 'high_contrast')).toBe('High contrast');
    expect(settingValueLabel(t, 'dd-MM-yyyy')).toBe('dd-MM-yyyy');
    expect(settingValueLabel(t, '')).toBe('');
    expect(settingSourceLabel(t, 'class_section')).toBe('Set by your class');
    expect(settingSourceLabel(t, 'galaxy')).toBe('galaxy');
    expect(settingCategoryLabel(t, 'appearance')).toBe('Appearance');
    expect(settingCategoryLabel(t, 'unknown')).toBe('Other');
    expect(settingCategoryLabel(t, undefined)).toBe('Other');
    expect(settingScopeLabel(t, 'tenant')).toBe('Whole school');
    expect(settingScopeLabel(t, 'class_section')).toBe('Class section');
    expect(settingScopeLabel(t, 'moon')).toBe('moon');
  });

  it('uses the language of the translator', () => {
    expect(settingLabel(createTranslator('hi'), 'ui.theme')).toBe('थीम');
    expect(settingLabel(createTranslator('hi'), 'ui.density')).toBe('Density');
  });
});
