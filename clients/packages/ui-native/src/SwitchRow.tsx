import { Switch, View } from 'react-native';
import { Text } from './Text';
import { useTheme } from './themeContext';

/** An on/off setting with its label and hint; the whole row is one accessible switch (ui/04 §6). */
export function SwitchRow({
  label,
  hint,
  value,
  disabled = false,
  onChange,
}: {
  label: string;
  hint?: string | undefined;
  value: boolean;
  disabled?: boolean;
  onChange: (value: boolean) => void;
}) {
  const t = useTheme();
  return (
    <View
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        minHeight: t.touchTarget,
        marginBottom: t.space[5],
      }}
    >
      <View style={{ flex: 1, marginRight: t.space[4] }}>
        <Text style={{ fontWeight: '600' }}>{label}</Text>
        {hint ? (
          <Text variant="caption" muted>
            {hint}
          </Text>
        ) : null}
      </View>
      <Switch
        accessibilityLabel={label}
        accessibilityHint={hint}
        value={value}
        disabled={disabled}
        onValueChange={onChange}
        trackColor={{ true: t.colors.primary, false: t.colors.border }}
      />
    </View>
  );
}
