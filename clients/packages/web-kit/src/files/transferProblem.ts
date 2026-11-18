import { isApiError, UploadTransferError } from '@mysms/core';
import type { MessageKey, Translate } from '@mysms/i18n';

/** The server's answers an uploader can do something about, by problem code. */
const UPLOAD_PROBLEMS: Readonly<Record<string, MessageKey>> = {
  QUOTA_EXCEEDED: 'files.upload.quota',
  PLAN_LIMIT_REACHED: 'files.upload.planLimit',
  NOT_CONFIGURED: 'files.store.notConfigured',
  VALIDATION_FAILED: 'files.upload.refused',
  PERMISSION_DENIED: 'files.upload.forbidden',
};

/** What went wrong with a transfer, in words the user can act on (the reference is kept for support). */
export function transferProblem(t: Translate, error: unknown): string {
  if (error instanceof UploadTransferError) return t('files.upload.interrupted');
  if (isApiError(error)) {
    const known = UPLOAD_PROBLEMS[error.code];
    const detail = error.errors.map((e) => e.message).join(' ');
    if (known) return detail && error.code === 'VALIDATION_FAILED' ? `${t(known)} ${detail}` : t(known);
  }
  return t('common.error.generic');
}
