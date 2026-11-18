import { isApiError } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';

/** The server's minimum password length (iam password policy). */
export const MIN_PASSWORD = 12;

/**
 * The catalogue sentence for a failed account call: a code that no longer works, e-mail that cannot be sent here,
 * too many requests, a closed sign-up; anything else is the generic error.
 */
export function accountProblem(error: unknown): MessageKey {
  if (!isApiError(error)) return 'common.error.generic';
  if (error.errors.some((e) => e.code === 'INVALID_CODE')) return 'acc.code.invalid';
  if (error.code === 'NOT_CONFIGURED') return 'acc.code.notConfigured';
  if (error.status === 429) return 'acc.code.tooMany';
  if (error.status === 403) return 'acc.code.notAllowed';
  if (error.status === 409) return 'acc.code.alreadyDone';
  if (error.status === 422) return 'acc.code.checkFields';
  return 'common.error.generic';
}
