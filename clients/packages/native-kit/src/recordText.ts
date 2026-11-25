/** Plain helpers of the native record screens: action labels, text-area heights and a row's first text field. */
import type { RecordRow } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';
import type { useTheme } from '@mysms/ui-native';

/** The catalogue label of a state transition, by the verb of its operation (`assessment.PublishHomework` → publish). */
const ACTION_LABELS: readonly [RegExp, MessageKey][] = [
  [/^Publish/, 'asm.native.publish'],
  [/^Approve/, 'asm.native.approve'],
  [/^Accept/, 'asm.native.approve'],
  [/^Reject/, 'asm.native.reject'],
  [/^Schedule/, 'asm.native.schedule'],
  [/^Mark[A-Za-z]*Held$/, 'asm.native.markHeld'],
  [/^Complete/, 'asm.native.complete'],
  [/^Finish/, 'asm.native.finish'],
  [/^Validate/, 'asm.native.validate'],
  [/^Submit/, 'asm.native.submit'],
  [/^Cancel/, 'common.action.cancel'],
  [/^Reset/, 'asm.native.reset'],
  [/^Unpublish/, 'native.action.unpublish'],
  [/^Deactivate/, 'native.action.deactivate'],
  [/^Reactivate/, 'native.action.reactivate'],
  [/^Activate/, 'native.action.activate'],
  [/^Close/, 'native.action.close'],
  [/^Reopen/, 'native.action.reopen'],
  [/^Snooze/, 'native.action.snooze'],
];

/** The label key of an action; none for a transition the assessment screens do not offer through this list. */
export function actionLabelKey(operation: string): MessageKey | undefined {
  const verb = operation.slice(operation.indexOf('.') + 1);
  return ACTION_LABELS.find(([pattern]) => pattern.test(verb))?.[1];
}

/** The height of a multi-line text input showing the given number of body lines (tokens only, ui/04 §2). */
export function textAreaHeight(theme: ReturnType<typeof useTheme>, lines: number): number {
  return theme.text.body.lineHeight * lines + theme.space[4] * 2;
}

export function firstText(row: RecordRow, fields: readonly string[]): string | undefined {
  for (const field of fields) if (typeof row[field] === 'string' && row[field]) return row[field] as string;
  return undefined;
}
