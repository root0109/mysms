import { moneyText, useCurrencyCode, useI18n } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';

export { failureText } from '../payroll/format';

/**
 * Amounts in a currency of master.currency, for reading: the kits' money formatting once the code is known, plain
 * decimals until then (never a guessed currency).
 */
export function useFeeMoney(currencyId: string | null | undefined): (amount: string | undefined) => string {
  const { format, formatLocale } = useI18n();
  const code = useCurrencyCode(currencyId);
  return (amount) => moneyText(format, formatLocale, amount, code);
}

/** The label of a payment or refund method. */
export const METHOD_LABEL: Readonly<Record<string, MessageKey>> = {
  gateway: 'fin.method.gateway',
  cash: 'fin.method.cash',
  cheque: 'fin.method.cheque',
  dd: 'fin.method.dd',
  bank_transfer: 'fin.method.bankTransfer',
  card_pos: 'fin.method.cardPos',
  adjustment: 'fin.method.adjustment',
};

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Whether the text is one record id. */
export const isId = (text: string): boolean => UUID.test(text);

/** The ids typed one per line (or separated by commas or spaces); null when one of them is not an id. */
export function idList(text: string): string[] | null {
  const ids = text.split(/[\s,]+/).filter(Boolean);
  return ids.every(isId) ? [...new Set(ids)] : null;
}
