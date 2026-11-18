import { isApiError } from '@mysms/core';
import type { Translate } from '@mysms/i18n';

/** A failed step as a sentence: the server's own for a conflict, the field messages for a validation failure. */
export function sisFailure(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return error.errors.map((e) => e.message).join(' ') || t('common.validation.pattern');
    if (error.status === 403) return t('sis.student.forbidden');
    if (error.status === 503) return t('sis.student.unavailable');
  }
  return t('common.error.generic');
}
