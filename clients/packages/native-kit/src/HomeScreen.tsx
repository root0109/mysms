import { useI18n, useSession, useSignOut, type RouteMatch } from '@mysms/core';
import { Button, Screen, Text } from '@mysms/ui-native';

/** The app home until the per-persona dashboards are built; shows where a deep link pointed. */
export function HomeScreen({
  linked,
  tablet = false,
  onOpen,
}: {
  linked: RouteMatch | null;
  tablet?: boolean;
  onOpen: (page: 'profile' | 'settings') => void;
}) {
  const { t } = useI18n();
  const name = useSession((s) => s.me?.user?.username ?? '');
  const signOut = useSignOut();
  return (
    <Screen tablet={tablet}>
      <Text variant="h1">{t('shell.home.welcome', { name })}</Text>
      <Text muted>{t('shell.home.body')}</Text>
      {linked ? (
        <Text>
          {t('shell.placeholder.title', { screenId: linked.screen.id, title: linked.screen.title })}
        </Text>
      ) : null}
      <Button variant="secondary" onPress={() => onOpen('profile')}>
        {t('shell.nav.profile')}
      </Button>
      <Button variant="secondary" onPress={() => onOpen('settings')}>
        {t('shell.nav.settings')}
      </Button>
      <Button variant="secondary" loading={signOut.isPending} onPress={() => signOut.mutate()}>
        {t('shell.user.signOut')}
      </Button>
    </Screen>
  );
}
