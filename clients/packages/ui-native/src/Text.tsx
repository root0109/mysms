import type { ReactNode } from 'react';
import { Text as RNText, type TextProps as RNTextProps } from 'react-native';
import { useTheme } from './themeContext';

export interface TextProps extends RNTextProps {
  variant?: 'display' | 'h1' | 'h2' | 'h3' | 'body' | 'bodyLg' | 'caption';
  muted?: boolean;
  children: ReactNode;
}

/** Text in the type scale; scales with the OS font size (allowFontScaling stays on, ui/04 §6). */
export function Text({ variant = 'body', muted = false, style, ...rest }: TextProps) {
  const t = useTheme();
  return (
    <RNText
      {...rest}
      style={[t.text[variant], { color: muted ? t.colors.textMuted : t.colors.text }, style]}
    />
  );
}
