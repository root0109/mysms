import type { Intent } from '@mysms/tokens';
import type { ReactNode } from 'react';
import { View } from 'react-native';
import { Text } from './Text';
import { useTheme } from './themeContext';

/** Same name and intents as the web Banner; container colour pairs (AA by construction). */
export function Banner({ intent = 'info', children }: { intent?: Intent; children: ReactNode }) {
  const t = useTheme();
  const c = t.intents[intent];
  return (
    <View
      accessibilityRole={intent === 'danger' || intent === 'warning' ? 'alert' : 'summary'}
      style={{
        backgroundColor: c.container,
        padding: t.space[5],
        borderRadius: t.radius.md,
        marginBottom: t.space[5],
      }}
    >
      <Text style={{ color: c.onContainer }}>{children}</Text>
    </View>
  );
}
