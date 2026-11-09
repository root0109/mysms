import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useTheme } from './themeContext';

/** A screen body with the phone gutter (16) or tablet gutter (24) from ui/04 §2.3. */
export function Screen({ children, tablet = false }: { children: ReactNode; tablet?: boolean }) {
  const t = useTheme();
  return (
    <View style={[styles.fill, { backgroundColor: t.colors.bg }]}>
      <ScrollView
        contentContainerStyle={{ padding: tablet ? t.space[7] : t.space[5] }}
        keyboardShouldPersistTaps="handled"
      >
        {children}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({ fill: { flex: 1 } });
