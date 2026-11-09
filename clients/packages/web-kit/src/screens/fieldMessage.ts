import { en, type MessageKey, type Translate } from '@mysms/i18n';

/**
 * Text for a form field error. Client-side schemas use catalogue keys as messages; server problem+json
 * field errors arrive already localised (Accept-Language) and are shown as they are.
 */
export function fieldMessage(
  t: Translate,
  error: { message?: string | undefined } | undefined,
  max?: number,
): string | undefined {
  const message = error?.message;
  if (!message) return undefined;
  return message in en ? t(message as MessageKey, max === undefined ? undefined : { max }) : message;
}
