import { ActivityIndicator, Pressable, StyleSheet, Text } from 'react-native';
import { useTheme } from './themeContext';

export interface ButtonProps {
  children: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  loading?: boolean;
  disabled?: boolean;
  accessibilityHint?: string;
}

/** Same name and variants as the web Button; ≥ 44 pt touch target (ui/04 §3). */
export function Button({
  children,
  onPress,
  variant = 'primary',
  loading = false,
  disabled = false,
  accessibilityHint,
}: ButtonProps) {
  const t = useTheme();
  const palette = {
    primary: { bg: t.colors.primary, fg: t.colors.onPrimary, border: t.colors.primary },
    secondary: { bg: t.colors.surface, fg: t.colors.text, border: t.colors.border },
    ghost: { bg: 'transparent', fg: t.colors.primary, border: 'transparent' },
    danger: { bg: t.intents.danger.base, fg: t.intents.danger.on, border: t.intents.danger.base },
  }[variant];
  const inactive = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: inactive, busy: loading }}
      accessibilityHint={accessibilityHint}
      disabled={inactive}
      onPress={onPress}
      style={({ pressed }) => [
        styles.base,
        {
          minHeight: t.touchTarget,
          borderRadius: t.radius.sm,
          backgroundColor: palette.bg,
          borderColor: palette.border,
          opacity: inactive ? 0.6 : pressed ? 0.85 : 1,
        },
      ]}
    >
      {loading ? (
        <ActivityIndicator color={palette.fg} />
      ) : (
        <Text style={[t.text.body, styles.label, { color: palette.fg }]}>{children}</Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: { alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16, borderWidth: 1 },
  label: { fontWeight: '600' },
});
