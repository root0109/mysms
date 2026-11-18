import { useCan, useClearDemoData, useI18n, type RecordRow } from '@mysms/core';
import { Banner, Button, Dialog } from '@mysms/ui-web';
import { useState } from 'react';
import { problemText } from './problems';

/**
 * PLT-01 row tool (platform staff): clear a trial tenant's demo data. Demo data lives in the modules that loaded it,
 * so platform.ClearDemoData announces the clearance to them; the answer is the finished operation that records it.
 */
export function ClearDemoDataTool({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const mayClear = useCan('platform.tenant.clear_demo_data');
  const clear = useClearDemoData();
  const [open, setOpen] = useState(false);
  if (!mayClear || row.status === 'closed') return null;
  return (
    <>
      <Button variant="ghost" onClick={() => setOpen(true)}>
        {t('plt.demo.clear')}
      </Button>
      <Dialog
        open={open}
        title={t('plt.demo.clear')}
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t('common.action.close')}
            </Button>
            {clear.isSuccess ? null : (
              <Button variant="danger" loading={clear.isPending} onClick={() => clear.mutate(row.id)}>
                {t('plt.demo.clear')}
              </Button>
            )}
          </>
        }
      >
        <p>{t('plt.demo.confirm', { name: String(row.name ?? row.code ?? row.id) })}</p>
        {clear.isSuccess ? <Banner intent="success">{t('plt.demo.announced')}</Banner> : null}
        {clear.isError ? <Banner intent="danger">{problemText(t, clear.error)}</Banner> : null}
      </Dialog>
    </>
  );
}
