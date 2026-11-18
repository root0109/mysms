import { useI18n } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { StatusChip, type StatusChipProps } from '@mysms/ui-web';

export { RetryBanner } from '../payroll/shared';

type Intent = StatusChipProps['intent'];

/** The states of every finance record a screen shows, with their label and colour. */
const STATES: Readonly<Record<string, { label: MessageKey; intent: Intent }>> = {
  draft: { label: 'fin.status.draft', intent: 'neutral' },
  invoiced: { label: 'fin.status.invoiced', intent: 'info' },
  posted: { label: 'fin.status.posted', intent: 'warning' },
  partially_paid: { label: 'fin.status.partiallyPaid', intent: 'warning' },
  paid: { label: 'fin.status.paid', intent: 'success' },
  cancelled: { label: 'fin.status.cancelled', intent: 'neutral' },
  initiated: { label: 'fin.status.initiated', intent: 'info' },
  received: { label: 'fin.status.received', intent: 'success' },
  failed: { label: 'fin.status.failed', intent: 'danger' },
  refunded: { label: 'fin.status.refunded', intent: 'neutral' },
  processed: { label: 'fin.status.processed', intent: 'success' },
  published: { label: 'fin.status.published', intent: 'success' },
  archived: { label: 'fin.status.archived', intent: 'neutral' },
};

/** A record's state as a chip (an unknown state shows as it is). */
export function FeeStatus({ status }: { status: string | undefined }) {
  const { t } = useI18n();
  const state = STATES[status ?? ''];
  return <StatusChip intent={state?.intent} label={state ? t(state.label) : (status ?? '')} />;
}
