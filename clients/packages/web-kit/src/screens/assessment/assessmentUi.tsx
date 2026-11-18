import { useI18n } from '@mysms/core';
import { Banner, Button } from '@mysms/ui-web';
import { assessmentErrorText, useOnline } from './assessmentText';

/** The offline warning of the assessment screens; nothing while online. */
export function OfflineBanner() {
  const online = useOnline();
  const { t } = useI18n();
  return online ? null : <Banner intent="warning">{t('common.state.offline')}</Banner>;
}

/** A failed read with its retry. */
export function ErrorState({ error, retry }: { error: unknown; retry: () => void }) {
  const { t } = useI18n();
  return (
    <Banner
      intent="danger"
      action={
        <Button variant="secondary" onClick={retry}>
          {t('common.action.retry')}
        </Button>
      }
    >
      {assessmentErrorText(error, t)}
    </Banner>
  );
}

/** A failed row action, under the row's controls; nothing without an error. */
export function RowError({ error }: { error: unknown }) {
  const { t } = useI18n();
  if (!error) return null;
  return (
    <p className="sms-field__error" role="alert">
      {assessmentErrorText(error, t)}
    </p>
  );
}
