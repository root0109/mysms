import { useI18n } from '@mysms/core';
import { Card } from '@mysms/ui-web';
import { Link } from 'react-router';

/** Polite dead end for unknown or unavailable paths (ui/05 §2). */
export function NotFoundScreen() {
  const { t } = useI18n();
  return (
    <Card>
      <p>{t('common.state.notAvailable')}</p>
      <Link to="/app">{t('shell.notFound.home')}</Link>
    </Card>
  );
}
