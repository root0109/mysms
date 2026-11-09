import { forwardRef } from 'react';
import { TextInput as RNTextInput, View, type TextInputProps as RNTextInputProps } from 'react-native';
import { Text } from './Text';
import { useTheme } from './themeContext';

export interface TextInputProps extends Omit<RNTextInputProps, 'accessibilityLabel'> {
  label: string;
  hint?: string;
  error?: string | undefined;
}

/** Same name and props as the web TextInput: label, hint, error (announced). */
export const TextInput = forwardRef<RNTextInput, TextInputProps>(function TextInput(
  { label, hint, error, style, ...rest },
  ref,
) {
  const t = useTheme();
  return (
    <View style={{ marginBottom: t.space[5] }}>
      <Text variant="body" style={{ fontWeight: '600', marginBottom: t.space[2] }}>
        {label}
      </Text>
      <RNTextInput
        ref={ref}
        {...rest}
        accessibilityLabel={label}
        accessibilityHint={error ?? hint}
        placeholderTextColor={t.colors.textMuted}
        style={[
          t.text.body,
          {
            minHeight: t.touchTarget,
            paddingHorizontal: t.space[4],
            borderWidth: 1,
            borderRadius: t.radius.sm,
            borderColor: error ? t.intents.danger.base : t.colors.border,
            backgroundColor: t.colors.surface,
            color: t.colors.text,
          },
          style,
        ]}
      />
      {hint ? (
        <Text variant="caption" muted>
          {hint}
        </Text>
      ) : null}
      {error ? (
        <Text variant="caption" accessibilityLiveRegion="polite" style={{ color: t.intents.danger.base }}>
          {error}
        </Text>
      ) : null}
    </View>
  );
});
