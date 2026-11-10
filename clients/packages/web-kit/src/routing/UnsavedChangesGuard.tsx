import { useI18n } from '@mysms/core';
import { Button, Dialog } from '@mysms/ui-web';
import { useEffect } from 'react';
import { useBlocker } from 'react-router';

/**
 * Keeps unsaved work from being lost by accident: while `when` holds, moving to another screen asks first, and
 * closing or reloading the tab raises the browser's own warning.
 */
export function UnsavedChangesGuard({ when }: { when: boolean }) {
  const { t } = useI18n();
  const blocker = useBlocker(when);
  useEffect(() => {
    if (!when) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [when]);
  // the work was saved while the question was open: nothing is left to lose, so the move goes ahead
  useEffect(() => {
    if (!when && blocker.state === 'blocked') blocker.proceed();
  }, [when, blocker]);
  if (blocker.state !== 'blocked') return null;
  return (
    <Dialog
      open
      title={t('common.unsaved.title')}
      onClose={() => blocker.reset()}
      footer={
        <>
          <Button variant="secondary" onClick={() => blocker.reset()}>
            {t('common.unsaved.stay')}
          </Button>
          <Button variant="danger" onClick={() => blocker.proceed()}>
            {t('common.unsaved.leave')}
          </Button>
        </>
      }
    >
      <p>{t('common.unsaved.body')}</p>
    </Dialog>
  );
}
