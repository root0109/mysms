import { useI18n, useSession } from '@mysms/core';
import { Card } from '@mysms/ui-web';

/** The persona home. Dashboards (ADM-01, teacher today, parent summary) replace this as they are built. */
export function HomeScreen() {
  const { t } = useI18n();
  const name = useSession((s) => s.me?.user?.username ?? '');
  return (
    <Card title={t('shell.home.welcome', { name })}>
      <p>{t('shell.home.body')}</p>
    </Card>
  );
}
