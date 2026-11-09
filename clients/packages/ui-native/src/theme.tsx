import { nativeTheme, type Mode, type TenantOverride } from '@mysms/tokens';
import { useMemo, type ReactNode } from 'react';
import { useColorScheme } from 'react-native';
import { ThemeContext } from './themeContext';

/**
 * Light/dark follows the OS unless the user picked a theme (light, dark or high contrast, ui/04 §9); the text-size
 * setting multiplies the type scale on top of the OS font size; tenant brand overrides pass the AA check.
 */
export function ThemeProvider({
  mode,
  textScale = 1,
  brand,
  children,
}: {
  mode?: Mode | undefined;
  /** 1 = 100 %, 1.5 = 150 %. */
  textScale?: number;
  brand?: TenantOverride;
  children: ReactNode;
}) {
  const scheme = useColorScheme();
  const resolved: Mode = mode ?? (scheme === 'dark' ? 'dark' : 'light');
  const theme = useMemo(() => {
    const base = nativeTheme(resolved, brand);
    if (textScale === 1) return base;
    const scaled = Object.fromEntries(
      Object.entries(base.text).map(([name, style]) => [
        name,
        {
          ...style,
          fontSize: Math.round(style.fontSize * textScale),
          lineHeight: Math.round(style.lineHeight * textScale),
        },
      ]),
    ) as typeof base.text;
    return { ...base, text: scaled };
  }, [resolved, brand, textScale]);
  return <ThemeContext.Provider value={theme}>{children}</ThemeContext.Provider>;
}
