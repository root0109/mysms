import { useI18n } from '@mysms/core';
import { Banner, Button } from '@mysms/ui-web';

/** A failed read with a retry. */
export function RetryBanner({ onRetry }: { onRetry: () => void }) {
  const { t } = useI18n();
  return (
    <Banner
      intent="danger"
      action={
        <Button variant="secondary" onClick={onRetry}>
          {t('common.action.retry')}
        </Button>
      }
    >
      {t('common.error.generic')}
    </Banner>
  );
}
