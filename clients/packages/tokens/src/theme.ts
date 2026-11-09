import { meetsAA } from './contrast';
import { intents, palette, type Mode, type Palette } from './tokens';

/** What a tenant may override (ui/04 §2.4): brand colours only, never intents, spacing, type or focus ring. */
export interface TenantOverride {
  primary?: string;
  onPrimary?: string;
  primaryContainer?: string;
  secondary?: string;
}

export interface ResolvedTheme {
  mode: Mode;
  palette: Palette;
  intents: (typeof intents)[Mode];
  /** Overrides that were rejected because they fail AA; the default pair is used instead. */
  rejected: (keyof TenantOverride)[];
}

/** Applies a tenant override, falling back per pair when it would break AA contrast. */
export function resolveTheme(mode: Mode, override: TenantOverride = {}): ResolvedTheme {
  const base = palette[mode];
  const p: Palette = { ...base };
  const rejected: (keyof TenantOverride)[] = [];
  const primary = override.primary ?? base.primary;
  const onPrimary = override.onPrimary ?? base.onPrimary;
  if (override.primary || override.onPrimary) {
    if (meetsAA(onPrimary, primary)) {
      p.primary = primary;
      p.onPrimary = onPrimary;
    } else {
      if (override.primary) rejected.push('primary');
      if (override.onPrimary) rejected.push('onPrimary');
    }
  }
  if (override.primaryContainer) {
    if (meetsAA(p.text, override.primaryContainer)) p.primaryContainer = override.primaryContainer;
    else rejected.push('primaryContainer');
  }
  if (override.secondary) {
    if (meetsAA(override.secondary, p.surface) || meetsAA(p.surface, override.secondary))
      p.secondary = override.secondary;
    else rejected.push('secondary');
  }
  // focus ring is never tenant-controlled
  p.focus = base.focus;
  return { mode, palette: p, intents: intents[mode], rejected };
}
