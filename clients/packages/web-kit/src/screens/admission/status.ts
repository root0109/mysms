import { isApiError, type ApplicationStep, type CycleStatus } from '@mysms/core';
import type { MessageKey, Translate } from '@mysms/i18n';
import type { StatusChipProps } from '@mysms/ui-web';

type Intent = StatusChipProps['intent'];

/** How each application status reads and looks (one place for the pipeline, the detail and the form). */
export const APPLICATION_STATUS: Readonly<Record<string, { label: MessageKey; intent: Intent }>> = {
  draft: { label: 'admission.status.draft', intent: 'neutral' },
  submitted: { label: 'admission.status.submitted', intent: 'info' },
  pending: { label: 'admission.status.pending', intent: 'warning' },
  confirmed: { label: 'admission.status.confirmed', intent: 'info' },
  approved: { label: 'admission.status.approved', intent: 'success' },
  admitted: { label: 'admission.status.admitted', intent: 'success' },
  done: { label: 'admission.status.done', intent: 'special' },
  rejected: { label: 'admission.status.rejected', intent: 'danger' },
  cancelled: { label: 'admission.status.cancelled', intent: 'neutral' },
};

/** The stages of a cycle as they read. */
export const CYCLE_STATUS: Readonly<Record<CycleStatus, MessageKey>> = {
  draft: 'admission.cycle.stage.draft',
  confirmed: 'admission.cycle.stage.confirmed',
  open: 'admission.cycle.stage.open',
  admission: 'admission.cycle.stage.admission',
  closed: 'admission.cycle.stage.closed',
  cancelled: 'admission.cycle.stage.cancelled',
};

/** The button of each decision step, and what is said once it is taken. */
export const STEP_LABEL: Readonly<Record<ApplicationStep, { action: MessageKey; done: MessageKey }>> = {
  submit: { action: 'admission.step.submit', done: 'admission.done.submit' },
  confirm: { action: 'admission.step.confirm', done: 'admission.done.confirm' },
  markPending: { action: 'admission.step.markPending', done: 'admission.done.markPending' },
  approve: { action: 'admission.step.approve', done: 'admission.done.approve' },
  confirmAdmission: { action: 'admission.step.confirmAdmission', done: 'admission.done.confirmAdmission' },
  reject: { action: 'admission.step.reject', done: 'admission.done.reject' },
  cancel: { action: 'admission.step.cancel', done: 'admission.done.cancel' },
  reset: { action: 'admission.step.reset', done: 'admission.done.reset' },
};

/**
 * What a refused write tells the user (ui/05 §6): a stale copy (412), a state or rule conflict with the server's own
 * sentence (409), the fields refused (422), a missing permission (403), a dependency that is down (503).
 */
export function failureText(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return error.errors.map((e) => `${e.field}: ${e.message}`).join(' ') || t('common.validation.pattern');
    if (error.status === 403) return t('admission.common.forbidden');
    if (error.status === 404) return t('common.state.notAvailable');
    if (error.status === 503) return t('admission.common.unavailable');
  }
  return t('common.error.generic');
}
