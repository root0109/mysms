import type { Application } from '@mysms/core';
import type { Translate } from '@mysms/i18n';
import { StatusChip } from '@mysms/ui-web';
import { APPLICATION_STATUS } from './status';

/** The chip of an application's status; an unknown (newer) status reads as itself, neutral. */
export function ApplicationStatusChip({ t, application }: { t: Translate; application: Application }) {
  const state = APPLICATION_STATUS[application.status ?? 'draft'];
  return (
    <StatusChip
      intent={state?.intent ?? 'neutral'}
      label={state ? t(state.label) : (application.status ?? '')}
    />
  );
}
