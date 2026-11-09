import { describe, expect, it } from 'vitest';
import { AA_TEXT, contrastRatio, meetsAA, parseHex, relativeLuminance } from './contrast';
import { cssVariables, tokensCss } from './css';
import { nativeTheme } from './native';
import { resolveTheme } from './theme';
import { intents, palette, type Mode } from './tokens';

const MODES: Mode[] = ['light', 'dark', 'highContrast'];

describe('contrast maths', () => {
  it('matches WCAG reference values', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 5);
    expect(contrastRatio('#FFFFFF', '#FFFFFF')).toBeCloseTo(1, 5);
    expect(relativeLuminance('#FFFFFF')).toBeCloseTo(1, 5);
    expect(contrastRatio('#767676', '#FFFFFF')).toBeCloseTo(4.54, 2);
    expect(parseHex('#0a0B0c')).toEqual([10, 11, 12]);
    expect(() => parseHex('red')).toThrow('not a #RRGGBB');
  });
});

describe('AA guaranteed by the token set (ui/04 §2.2, §6)', () => {
  for (const mode of MODES) {
    const p = palette[mode];
    it(`${mode}: text, muted text and on-primary pairs`, () => {
      for (const bg of [p.bg, p.surface, p.surface2]) {
        expect(contrastRatio(p.text, bg)).toBeGreaterThanOrEqual(AA_TEXT);
      }
      expect(contrastRatio(p.textMuted, p.surface)).toBeGreaterThanOrEqual(AA_TEXT);
      expect(contrastRatio(p.onPrimary, p.primary)).toBeGreaterThanOrEqual(AA_TEXT);
      expect(contrastRatio(p.text, p.primaryContainer)).toBeGreaterThanOrEqual(AA_TEXT);
    });
    for (const [name, c] of Object.entries(intents[mode])) {
      it(`${mode}: intent ${name} on/base and on-container/container`, () => {
        expect(contrastRatio(c.on, c.base)).toBeGreaterThanOrEqual(AA_TEXT);
        expect(contrastRatio(c.onContainer, c.container)).toBeGreaterThanOrEqual(AA_TEXT);
      });
    }
  }
});

describe('high contrast (ui/04 §9)', () => {
  it('reaches AAA 7:1 for every text pair', () => {
    const p = palette.highContrast;
    for (const bg of [p.bg, p.surface, p.surface2, p.primaryContainer]) {
      expect(contrastRatio(p.text, bg)).toBeGreaterThanOrEqual(7);
    }
    expect(contrastRatio(p.textMuted, p.surface)).toBeGreaterThanOrEqual(7);
    expect(contrastRatio(p.onPrimary, p.primary)).toBeGreaterThanOrEqual(7);
    for (const c of Object.values(intents.highContrast)) {
      expect(contrastRatio(c.on, c.base)).toBeGreaterThanOrEqual(7);
      expect(contrastRatio(c.onContainer, c.container)).toBeGreaterThanOrEqual(7);
    }
  });
});

describe('tenant theming (ui/04 §2.4)', () => {
  it('accepts an accessible brand override', () => {
    const t = resolveTheme('light', {
      primary: '#1D4ED8',
      onPrimary: '#FFFFFF',
      primaryContainer: '#DBEAFE',
      secondary: '#0F766E',
    });
    expect(t.rejected).toEqual([]);
    expect(t.palette.primary).toBe('#1D4ED8');
    expect(t.palette.primaryContainer).toBe('#DBEAFE');
    expect(t.palette.secondary).toBe('#0F766E');
  });

  it('falls back to the default pair when an override fails AA, and never lets the focus ring change', () => {
    const t = resolveTheme('light', {
      primary: '#FDE047',
      primaryContainer: '#334155',
      secondary: '#F8FAFC',
    });
    expect(t.rejected).toEqual(['primary', 'primaryContainer', 'secondary']);
    expect(t.palette.primary).toBe(palette.light.primary);
    expect(t.palette.focus).toBe(palette.light.focus);
    expect(resolveTheme('dark', { onPrimary: '#818CF8' }).rejected).toEqual(['onPrimary']);
    expect(meetsAA('#000000', '#FFFFFF')).toBe(true);
  });
});

describe('outputs', () => {
  it('emits --sms-* variables for both modes', () => {
    const light = cssVariables('light');
    expect(light['--sms-color-primary']).toBe('#4F46E5');
    expect(light['--sms-intent-on-success-container']).toBe('#14532D');
    expect(light['--sms-space-5']).toBe('16px');
    const scaled = (w: number, size: number, line: number, family = 'ui') =>
      `${w} calc(${size}px * var(--sms-text-scale)) / calc(${line}px * var(--sms-text-scale)) var(--sms-font-family-${family})`;
    expect(light['--sms-font-body']).toBe(scaled(400, 14, 22));
    expect(light['--sms-font-h2']).toBe(scaled(600, 20, 28));
    expect(light['--sms-font-body-lg']).toBe(scaled(400, 16, 24));
    expect(light['--sms-font-mono']).toBe(scaled(400, 13, 20, 'mono'));
    expect(light['--sms-text-scale']).toBe('1');
    expect(light['--sms-row-height']).toBe('48px');
    expect(light['--sms-color-surface-2']).toBe('#F1F5F9');
    expect(light['--sms-color-text-muted']).toBe('#64748B');
    expect(cssVariables('dark')['--sms-color-bg']).toBe('#0B1220');
    const css = tokensCss();
    expect(css).toContain(':root {');
    expect(css).toContain('@media (prefers-color-scheme: dark)');
    expect(css).toContain(':root[data-theme="dark"] {');
    expect(css).toContain('prefers-reduced-motion');
    expect(css).toContain(':root[data-theme="high-contrast"] {');
    expect(css).toContain(':root[data-density="compact"] {\n  --sms-row-height: 40px;');
    expect(css).toContain(':root[data-reduced-motion="true"] { --sms-motion-micro: 0ms;');
    // the OS dark preference applies only while the user keeps the system theme
    expect(css).toContain(':root:not([data-theme]) {');
    expect(cssVariables('highContrast')['--sms-color-text']).toBe('#000000');
  });

  it('builds the native theme with the phone +1 px body bump', () => {
    const t = nativeTheme('dark');
    expect(t.colors.bg).toBe('#0B1220');
    expect(t.text.body).toEqual({ fontSize: 15, lineHeight: 23, fontWeight: '400' });
    expect(t.text.h1.fontSize).toBe(24);
    expect(t.touchTarget).toBe(44);
  });
});
