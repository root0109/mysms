import { useI18n, type Cohort, lmsFailureText } from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, StatusChip } from '@mysms/ui-web';

/**
 * The error banner every lms screen shows for a failed query, with the retry the five states ask for: the failure's
 * own sentence when the error is given, else the generic one.
 */
export function LmsRetryBanner({ error, onRetry }: { error?: unknown; onRetry: () => void }) {
  const { t } = useI18n();
  return (
    <Banner intent="danger" action={<Button onClick={onRetry}>{t('common.action.retry')}</Button>}>
      {lmsFailureText(t, error)}
    </Banner>
  );
}

/** The cohort's lifecycle and catalogue state as chips. */
export function CohortStatus({ cohort }: { cohort: Cohort }) {
  const { t } = useI18n();
  const status = cohort.lifecycleStatus ?? 'draft';
  const intent =
    status === 'active' ? 'success' : status === 'archived' || status === 'suspended' ? 'warning' : 'neutral';
  return (
    <span className="chip-row">
      <StatusChip intent={intent} label={enumLabel(t, 'lmsCohort', status)} />
      {cohort.isPublished ? <StatusChip intent="info" label={t('lms.cohort.published')} /> : null}
    </span>
  );
}
