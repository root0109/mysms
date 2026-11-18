import { useI18n } from '@mysms/core';
import { ResourceScreen } from '../ResourceScreen';
import { RESOURCE_SCREENS } from '../resourceScreens';
import { CycleTimeline } from './CycleTimeline';

/**
 * ADMN-01 Admission cycles: the timeline every cycle follows (draft → confirmed → open for applications → admission
 * → closed; cancel and reset aside) as the header, over the list manager of cycles, whose rows offer the steps the
 * role may take (api/standard-resources.json transitions) and show where each cycle stands.
 */
export function AdmissionCyclesScreen() {
  const { t } = useI18n();
  const screen = RESOURCE_SCREENS.find((s) => s.id === 'ADMN-01');
  return (
    <div className="page">
      <section className="admission-header" aria-label={t('admission.cycle.timeline')}>
        <h2 className="sr-only">{t('admission.cycle.timeline')}</h2>
        <CycleTimeline />
        <p className="hint">{t('admission.cycle.hint.cancelled')}</p>
      </section>
      {screen ? <ResourceScreen screen={screen} /> : null}
    </div>
  );
}
