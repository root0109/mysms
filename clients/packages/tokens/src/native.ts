import { resolveTheme, type TenantOverride } from './theme';
import { fonts, radius, space, typeScale, type Mode } from './tokens';

/** The React Native theme object (same tokens; phones add +1 px to body sizes, ui/04 §2.3). */
export function nativeTheme(mode: Mode, override?: TenantOverride) {
  const t = resolveTheme(mode, override);
  const text = (name: keyof typeof typeScale, phoneBump = false) => {
    const [size, line, weight] = typeScale[name];
    return {
      fontSize: size + (phoneBump ? 1 : 0),
      lineHeight: line + (phoneBump ? 1 : 0),
      fontWeight: String(weight) as '400' | '600',
    };
  };
  return {
    mode,
    colors: t.palette,
    intents: t.intents,
    space,
    radius,
    fontFamily: { ui: fonts.ui.split(',')[0] ?? 'Inter', mono: 'JetBrains Mono' },
    text: {
      display: text('display'),
      h1: text('h1'),
      h2: text('h2'),
      h3: text('h3'),
      body: text('body', true),
      bodyLg: text('bodyLg', true),
      caption: text('caption'),
    },
    /** Minimum touch target (ui/04 §3). */
    touchTarget: 44,
  } as const;
}

export type NativeTheme = ReturnType<typeof nativeTheme>;
