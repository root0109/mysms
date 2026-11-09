import { Pressable, View } from 'react-native';
import { Text } from './Text';
import { useTheme } from './themeContext';

export interface Choice {
  value: string;
  label: string;
}

/** One of several values as large touch targets (radio group semantics), e.g. a theme or language. */
export function ChoiceGroup({
  label,
  hint,
  choices,
  value,
  disabled = false,
  onChange,
}: {
  label: string;
  hint?: string | undefined;
  choices: readonly Choice[];
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
}) {
  const t = useTheme();
  return (
    <View accessibilityRole="radiogroup" accessibilityLabel={label} style={{ marginBottom: t.space[5] }}>
      <Text style={{ fontWeight: '600', marginBottom: t.space[2] }}>{label}</Text>
      {hint ? (
        <Text variant="caption" muted style={{ marginBottom: t.space[3] }}>
          {hint}
        </Text>
      ) : null}
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: t.space[3] }}>
        {choices.map((c) => {
          const selected = c.value === value;
          return (
            <Pressable
              key={c.value}
              accessibilityRole="radio"
              accessibilityState={{ checked: selected, disabled }}
              disabled={disabled}
              onPress={() => onChange(c.value)}
              style={{
                minHeight: t.touchTarget,
                justifyContent: 'center',
                paddingHorizontal: t.space[5],
                borderRadius: t.radius.full,
                borderWidth: 1,
                borderColor: selected ? t.colors.primary : t.colors.border,
                backgroundColor: selected ? t.colors.primaryContainer : t.colors.surface,
                opacity: disabled ? 0.6 : 1,
              }}
            >
              <Text style={{ fontWeight: selected ? '600' : '400' }}>{c.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}
