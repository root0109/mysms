import type { Intent } from '@mysms/tokens';

export interface StatusChipProps {
  /** Unknown values render as neutral (the contract's enums are extensible, ui/05 §14). */
  intent?: Intent | undefined;
  /** Visible text — status is never conveyed by colour alone (ui/04 §6). */
  label: string;
}

const INTENTS: readonly Intent[] = ['success', 'warning', 'danger', 'info', 'neutral', 'special'];

export function StatusChip({ intent, label }: StatusChipProps) {
  const safe = intent && INTENTS.includes(intent) ? intent : 'neutral';
  return <span className={`sms-chip sms-chip--${safe}`}>{label}</span>;
}
