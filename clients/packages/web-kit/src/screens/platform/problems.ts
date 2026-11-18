import { isApiError } from '@mysms/core';
import type { MessageKey, Translate } from '@mysms/i18n';

const PROBLEMS: Readonly<Record<number, MessageKey>> = {
  403: 'plt.problem.forbidden',
  404: 'common.state.notAvailable',
  409: 'plt.problem.conflict',
  412: 'common.conflict.body',
  503: 'plt.problem.unavailable',
};

/**
 * A refused write in words the user can act on: field errors arrive localised from the server and are shown as
 * they are; the rest by status (a stale version, a state that no longer allows the step, a provider not configured).
 */
export function problemText(t: Translate, error: unknown): string {
  if (!isApiError(error)) return t('common.error.generic');
  if (error.status === 422 && error.errors.length > 0) return error.errors.map((e) => e.message).join(' ');
  if (error.code === 'NOT_CONFIGURED') return t('plt.problem.notConfigured');
  const known = PROBLEMS[error.status];
  return known ? t(known) : (error.detail ?? t('common.error.generic'));
}
