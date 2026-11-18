import { buildPath, isApiError, screenById, type StaffEmploymentType, type StaffStatus } from '@mysms/core';
import type { MessageKey, Translate } from '@mysms/i18n';
import type { StatusChipProps } from '@mysms/ui-web';

/** How each staff status reads and is coloured (the label always carries the meaning, ui/04 §6). */
export const STAFF_STATUS: Readonly<
  Record<StaffStatus, { label: MessageKey; intent: StatusChipProps['intent'] }>
> = {
  active: { label: 'hr.staff.status.active', intent: 'success' },
  inactive: { label: 'hr.staff.status.inactive', intent: 'warning' },
  exited: { label: 'hr.staff.status.exited', intent: 'neutral' },
};

export const EMPLOYMENT_TYPE: Readonly<Record<StaffEmploymentType, MessageKey>> = {
  full_time: 'hr.staff.type.fullTime',
  part_time: 'hr.staff.type.partTime',
  contract: 'hr.staff.type.contract',
  visiting: 'hr.staff.type.visiting',
};

/** The step that moves a staff member to a status, as a button reads. */
export const STATUS_MOVE: Readonly<Record<StaffStatus, MessageKey>> = {
  active: 'hr.staff.move.active',
  inactive: 'hr.staff.move.inactive',
  exited: 'hr.staff.move.exited',
};

/**
 * What a failed staff write tells the user (ui/05 §6): a stale copy (412) asks for a reload, a conflict (409) says
 * what the server said, field errors (422) are listed, a refusal (403) and an unavailable iam (503) are named.
 */
export function staffProblem(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return (
        error.errors.map((e) => (e.field ? `${e.field}: ${e.message}` : e.message)).join(' ') ||
        error.detail ||
        t('common.validation.pattern')
      );
    if (error.status === 403) return t('hr.staff.forbidden');
    if (error.status === 503) return t('hr.staff.unavailable');
  }
  return t('common.error.generic');
}

/** The web address of an inventory screen (ui/02 §3), with its route parameters. */
export function screenLink(screenId: string, params: Record<string, string> = {}): string {
  const route = screenById(screenId)?.routes[0];
  if (!route) throw new Error(`no route for screen ${screenId}`);
  return `/app${buildPath(route, params)}`;
}

/** The web address of a staff member's record (HR-02). */
export const staffLink = (staffId: string) => screenLink('HR-02', { staffId });
