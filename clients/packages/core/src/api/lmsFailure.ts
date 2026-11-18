import type { MessageKey, Translate } from '@mysms/i18n';
import { isApiError } from './problem';

/** The codes the LMS operations answer that have a sentence of their own, in the reader's language. */
const KNOWN: Readonly<Record<string, MessageKey>> = {
  NOT_ENROLLED: 'lms.error.notEnrolled',
  VIDEO_NOT_WATCHED: 'lms.error.videoNotWatched',
  QUIZ_NOT_COMPLETED: 'lms.error.quizNotCompleted',
  PAYMENT_REQUIRED: 'lms.error.paymentRequired',
  SELF_LEARNING_DISABLED: 'lms.error.selfLearningDisabled',
  DUPLICATE: 'lms.error.duplicate',
  NOT_CONFIGURED: 'lms.error.notConfigured',
  INVALID_STATE_TRANSITION: 'lms.error.invalidState',
  NOT_FOUND: 'common.state.notAvailable',
};

/**
 * The sentence a refused lms call shows: a code with a sentence of its own first, then the stale-version line, the
 * server's own word for a conflict or an unavailable provider, the field messages, the access and not-served lines,
 * else the generic one.
 */
export function lmsFailureText(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    const known = KNOWN[error.code];
    if (known) return t(known);
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409 || error.status === 503) return error.detail ?? t('common.error.generic');
    if (error.status === 422)
      return error.errors.map((e) => e.message).join(' ') || t('common.validation.pattern');
    if (error.status === 403) return t('lms.common.forbidden');
    if (error.status === 501) return t('lms.common.notServed');
  }
  return t('common.error.generic');
}
