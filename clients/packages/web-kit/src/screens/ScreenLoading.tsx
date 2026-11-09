import { useI18n } from '@mysms/core';
import { Skeleton } from '@mysms/ui-web';

/** Loading state while a lazily loaded screen arrives (skeleton, never a spinner — ui/02 §1.4). */
export function ScreenLoading() {
  const { t } = useI18n();
  return <Skeleton label={t('common.state.loading')} lines={5} />;
}
