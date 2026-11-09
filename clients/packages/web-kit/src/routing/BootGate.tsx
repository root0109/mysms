import { useRuntime, useI18n } from '@mysms/core';
import { Skeleton } from '@mysms/ui-web';
import { useEffect, useState, type ReactNode } from 'react';

/** Restores a session from the refresh-token vault once at start-up before any route renders. */
export function BootGate({ children }: { children: ReactNode }) {
  const { session } = useRuntime();
  const { t } = useI18n();
  const [ready, setReady] = useState(false);
  useEffect(() => {
    let live = true;
    session
      .restore()
      .catch(() => false)
      .finally(() => live && setReady(true));
    return () => {
      live = false;
    };
  }, [session]);
  return ready ? children : <Skeleton label={t('common.state.loading')} lines={4} />;
}
