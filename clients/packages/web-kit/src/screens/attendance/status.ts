import type { AttendanceStatus, AttendanceSummary } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import type { StatusChipProps } from '@mysms/ui-web';

type Intent = NonNullable<StatusChipProps['intent']>;

/** How each mark is coloured wherever it is shown; its label always accompanies the colour (ui/04 §6). */
export const STATUS_INTENT: Readonly<Record<AttendanceStatus, Intent>> = {
  present: 'success',
  absent: 'danger',
  late: 'warning',
  excused: 'info',
};

/** A sheet's workflow state, as the catalogue names it and as it is coloured. */
export const SHEET_STATUS: Readonly<Record<string, { label: MessageKey; intent: Intent }>> = {
  draft: { label: 'att.sheet.status.draft', intent: 'neutral' },
  in_progress: { label: 'att.sheet.status.inProgress', intent: 'info' },
  done: { label: 'att.sheet.status.done', intent: 'success' },
  cancelled: { label: 'att.sheet.status.cancelled', intent: 'warning' },
};

/** A reason is recorded only for a student who was not there. */
export const takesReason = (status: AttendanceStatus | undefined): boolean =>
  status === 'absent' || status === 'excused';

/**
 * The attendance percentage of a summary row (0–100), from its counts: present and late count as attended. The
 * counts are used rather than the server's rounded figure, so 74.995 % is still below a minimum of 75.
 */
export function percentOf(summary: AttendanceSummary): number {
  const total = summary.present + summary.absent + summary.late + summary.excused;
  return total === 0 ? 0 : ((summary.present + summary.late) * 100) / total;
}
