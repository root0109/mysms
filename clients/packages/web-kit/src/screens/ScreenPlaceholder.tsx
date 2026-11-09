import { useI18n } from '@mysms/core';
import { Card } from '@mysms/ui-web';

/** Stands in for an inventory screen that is not built yet, naming it so the gap is visible. */
export function ScreenPlaceholder({ screenId, title }: { screenId: string; title: string }) {
  const { t } = useI18n();
  return (
    <Card title={t('shell.placeholder.title', { screenId, title })}>
      <p>{t('shell.placeholder.body')}</p>
    </Card>
  );
}
