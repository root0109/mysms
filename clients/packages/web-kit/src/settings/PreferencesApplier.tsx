import { useI18n, usePreferences } from '@mysms/core';
import { useLayoutEffect } from 'react';
import { applyPreferences } from './applyPreferences';

/** Keeps the document in step with the signed-in user's effective settings; defaults while signed out. */
export function PreferencesApplier() {
  const preferences = usePreferences();
  const { locale } = useI18n();
  useLayoutEffect(() => {
    applyPreferences(preferences, locale, document.documentElement);
  }, [preferences, locale]);
  return null;
}
