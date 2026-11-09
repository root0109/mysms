/** Design tokens from ui/04 §2. Values are the single source for web CSS variables and the native theme. */

/** Colour modes: light, dark and high contrast (a user setting, ui/04 §9). */
export type Mode = 'light' | 'dark' | 'highContrast';
export type Intent = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'special';

export interface IntentColors {
  base: string;
  on: string;
  container: string;
  onContainer: string;
}

export interface Palette {
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  secondary: string;
  bg: string;
  surface: string;
  surface2: string;
  border: string;
  text: string;
  textMuted: string;
  focus: string;
}

/** ui/04 §2.1 base palette. */
export const palette: Record<Mode, Palette> = {
  light: {
    primary: '#4F46E5',
    onPrimary: '#FFFFFF',
    primaryContainer: '#EEF2FF',
    secondary: '#0D9488',
    bg: '#F8FAFC',
    surface: '#FFFFFF',
    surface2: '#F1F5F9',
    border: '#E2E8F0',
    text: '#0F172A',
    textMuted: '#64748B',
    focus: '#4F46E5',
  },
  dark: {
    primary: '#818CF8',
    onPrimary: '#1E1B4B',
    primaryContainer: '#312E81',
    secondary: '#2DD4BF',
    bg: '#0B1220',
    surface: '#111A2C',
    surface2: '#1A2538',
    border: '#27334A',
    text: '#E6EAF2',
    textMuted: '#94A3B8',
    focus: '#818CF8',
  },
  /** Black on white with strong borders: every text pair reaches WCAG AAA 7:1 (checked by the tests). */
  highContrast: {
    primary: '#1A1A8C',
    onPrimary: '#FFFFFF',
    primaryContainer: '#E6E6FF',
    secondary: '#00594F',
    bg: '#FFFFFF',
    surface: '#FFFFFF',
    surface2: '#F2F2F2',
    border: '#000000',
    text: '#000000',
    textMuted: '#262626',
    focus: '#000000',
  },
};

/**
 * ui/04 §2.2 semantic intents. The light base colours are the spec's; `on`/container pairs are chosen so every
 * text pair meets WCAG AA 4.5:1 (checked by the tests). Intents are never tenant-overridable.
 */
export const intents: Record<Mode, Record<Intent, IntentColors>> = {
  light: {
    success: { base: '#16A34A', on: '#052E16', container: '#DCFCE7', onContainer: '#14532D' },
    warning: { base: '#D97706', on: '#1C0A00', container: '#FEF3C7', onContainer: '#78350F' },
    danger: { base: '#DC2626', on: '#FFFFFF', container: '#FEE2E2', onContainer: '#7F1D1D' },
    info: { base: '#0284C7', on: '#020617', container: '#E0F2FE', onContainer: '#0C4A6E' },
    neutral: { base: '#64748B', on: '#FFFFFF', container: '#F1F5F9', onContainer: '#334155' },
    special: { base: '#9333EA', on: '#FFFFFF', container: '#F3E8FF', onContainer: '#581C87' },
  },
  dark: {
    success: { base: '#4ADE80', on: '#052E16', container: '#14532D', onContainer: '#DCFCE7' },
    warning: { base: '#FBBF24', on: '#451A03', container: '#78350F', onContainer: '#FEF3C7' },
    danger: { base: '#F87171', on: '#450A0A', container: '#7F1D1D', onContainer: '#FEE2E2' },
    info: { base: '#38BDF8', on: '#082F49', container: '#0C4A6E', onContainer: '#E0F2FE' },
    neutral: { base: '#94A3B8', on: '#0F172A', container: '#334155', onContainer: '#F1F5F9' },
    special: { base: '#C084FC', on: '#3B0764', container: '#581C87', onContainer: '#F3E8FF' },
  },
  highContrast: {
    success: { base: '#005A00', on: '#FFFFFF', container: '#E6F4E6', onContainer: '#002E00' },
    warning: { base: '#7A4100', on: '#FFFFFF', container: '#FFF2DC', onContainer: '#3D2000' },
    danger: { base: '#9E0000', on: '#FFFFFF', container: '#FFE6E6', onContainer: '#4D0000' },
    info: { base: '#00457A', on: '#FFFFFF', container: '#E3F0FF', onContainer: '#002642' },
    neutral: { base: '#333333', on: '#FFFFFF', container: '#EEEEEE', onContainer: '#111111' },
    special: { base: '#5B1A8C', on: '#FFFFFF', container: '#F2E6FF', onContainer: '#2E0D47' },
  },
};

/** ui/04 §2.3 type scale: [font size px, line height px, weight]. Phones add +1 px to body sizes. */
export const typeScale = {
  display: [32, 40, 600],
  h1: [24, 32, 600],
  h2: [20, 28, 600],
  h3: [16, 24, 600],
  body: [14, 22, 400],
  bodyLg: [16, 24, 400],
  caption: [12, 16, 400],
  mono: [13, 20, 400],
} as const;

export const fonts = {
  ui: "Inter, system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans', 'Noto Sans Devanagari', sans-serif",
  mono: "'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Consolas, monospace",
} as const;

/** Table and list row heights per density (ui/04 §3); the user's density setting picks one. */
export const rowHeight = { compact: 40, comfortable: 48 } as const;
export type Density = keyof typeof rowHeight;

/** 4-px grid steps. */
export const space = [0, 2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64] as const;
export const radius = { sm: 6, md: 10, lg: 16, full: 9999 } as const;
export const motion = { micro: 120, overlay: 200, page: 300, easing: 'cubic-bezier(0.2, 0, 0, 1)' } as const;
export const breakpoints = { sm: 640, md: 960, lg: 1280, xl: 1440 } as const;
/** Elevation 0..4 (dark mode raises surface tone instead of shadow). */
export const elevation = [
  'none',
  '0 1px 2px rgb(15 23 42 / 0.06), 0 1px 3px rgb(15 23 42 / 0.10)',
  '0 2px 6px rgb(15 23 42 / 0.10)',
  '0 8px 24px rgb(15 23 42 / 0.16)',
  '0 12px 32px rgb(15 23 42 / 0.20)',
] as const;
