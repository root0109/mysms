import { isApiError, type useI18n } from '@mysms/core';
import { useEffect, useState } from 'react';

type Translate = ReturnType<typeof useI18n>['t'];

/** Whether the browser is online, following its online/offline events. */
export function useOnline(): boolean {
  const [online, setOnline] = useState(() => typeof navigator === 'undefined' || navigator.onLine);
  useEffect(() => {
    if (typeof window === 'undefined') return;
    const on = () => setOnline(true);
    const off = () => setOnline(false);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    return () => {
      window.removeEventListener('online', on);
      window.removeEventListener('offline', off);
    };
  }, []);
  return online;
}

/**
 * The sentence for a failed assessment read or write (ui/05 §6): a stale version is a conflict, 403 and 404 are
 * "not allowed" / "not available", a refused state change shows the server's own sentence, and a validation error
 * its field messages (they arrive localised).
 */
export function assessmentErrorText(error: unknown, t: Translate): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 403) return t('adm.records.forbidden');
    if (error.status === 404) return t('common.state.notAvailable');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return error.errors.map((item) => item.message).join(' ') || t('common.validation.pattern');
  }
  return t('common.error.generic');
}
