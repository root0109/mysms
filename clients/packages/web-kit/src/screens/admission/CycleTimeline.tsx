import {
  CYCLE_STAGES,
  buildPath,
  cycleStage,
  screenById,
  useI18n,
  type CycleStatus,
  type RecordRow,
} from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import { Link } from 'react-router';
import { CYCLE_STATUS } from './status';

/** What each stage of a cycle means, in the order the steps move it. */
const STAGE_HINT: Readonly<Record<CycleStatus, MessageKey>> = {
  draft: 'admission.cycle.hint.draft',
  confirmed: 'admission.cycle.hint.confirmed',
  open: 'admission.cycle.hint.open',
  admission: 'admission.cycle.hint.admission',
  closed: 'admission.cycle.hint.closed',
  cancelled: 'admission.cycle.hint.cancelled',
};

/**
 * The stages of a cycle; with a status, that cycle's place on them (the current stage marked, a cancelled cycle
 * said so instead), without one the legend of what each stage means.
 */
export function CycleTimeline({ status, compact }: { status?: CycleStatus | undefined; compact?: boolean }) {
  const { t } = useI18n();
  if (status === 'cancelled') return <span className="hint">{t('admission.cycle.stage.cancelled')}</span>;
  const current = status ? cycleStage(status) : -1;
  return (
    <ol className={compact ? 'admission-timeline admission-timeline--compact' : 'admission-timeline'}>
      {CYCLE_STAGES.map((stage, index) => (
        <li
          key={stage}
          className={
            index < current
              ? 'admission-timeline__stage admission-timeline__stage--past'
              : index === current
                ? 'admission-timeline__stage admission-timeline__stage--current'
                : 'admission-timeline__stage'
          }
          aria-current={index === current ? 'step' : undefined}
        >
          <span className="admission-timeline__name">{t(CYCLE_STATUS[stage])}</span>
          {compact ? null : <span className="hint">{t(STAGE_HINT[stage])}</span>}
        </li>
      ))}
    </ol>
  );
}

/**
 * The row tool of a cycle: where it stands, and — while it takes applications — the address of its application
 * form (ADMN-05) to publish.
 */
export function CycleRowTimeline({ row }: { row: RecordRow }) {
  const { t } = useI18n();
  const status = row.status as CycleStatus | undefined;
  const form = screenById('ADMN-05')?.routes[0];
  return (
    <div className="admission-cycle-row">
      <CycleTimeline status={status} compact />
      {status === 'open' && form ? (
        <Link to={`/app${buildPath(form, { admissionCycleId: row.id })}`}>{t('admission.cycle.form')}</Link>
      ) : null}
    </div>
  );
}
