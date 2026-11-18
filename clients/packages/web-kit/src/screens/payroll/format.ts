import {
  isApiError,
  moneyText,
  useCan,
  useCurrencyCode,
  useI18n,
  useInstitutions,
  type PaymentMethod,
  type Revision,
} from '@mysms/core';
import type { MessageKey, Translate } from '@mysms/i18n';

/** The sentence for a refused write: the server's own detail for a conflict, every field error for a 422. */
export function failureText(t: Translate, error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 412) return t('common.conflict.body');
    if (error.status === 409) return error.detail ?? t('common.conflict.body');
    if (error.status === 422)
      return error.errors.map((e) => `${e.field}: ${e.message}`).join(' ') || t('common.validation.pattern');
    if (error.status === 403) return t('pay.common.forbidden');
  }
  return t('common.error.generic');
}

/**
 * Amounts of an institution for reading, through the kits' money formatting in the institution's currency
 * (org.institution → master.currency). Without a currency to read (no permission, none set) amounts show as plain
 * decimals rather than in a guessed currency.
 */
export function useMoney(institutionId: string | null | undefined): (amount: string | undefined) => string {
  const { format, formatLocale } = useI18n();
  const mayInstitutions = useCan('org.institution.read');
  const institutions = useInstitutions(mayInstitutions && Boolean(institutionId));
  const currencyId = institutions.data?.items.find((item) => item.id === institutionId)?.currencyId;
  const code = useCurrencyCode(currencyId);
  return (amount) => moneyText(format, formatLocale, amount, code);
}

/** The label of each payment method. */
export const METHOD: Readonly<Record<PaymentMethod, MessageKey>> = {
  bank_transfer: 'pay.method.bankTransfer',
  cash: 'pay.method.cash',
  cheque: 'pay.method.cheque',
  other: 'pay.method.other',
};

const LINE_TYPE: Readonly<Record<string, MessageKey>> = {
  earning: 'pay.component.type.earning',
  deduction: 'pay.component.type.deduction',
  defined: 'pay.component.type.defined',
};

/** The label of a pay component type (earning, deduction, employer-defined). */
export const componentTypeLabel = (t: Translate, type: string | undefined): string => {
  const key = LINE_TYPE[type ?? ''];
  return key ? t(key) : (type ?? '');
};

/**
 * How a write names its row: nothing for a new one, the id with the etag it was read at for a stored one, and null
 * for a stored row read without an etag — the action is then not offered, since the server refuses an update or
 * delete without If-Match (428).
 */
export function revisionOf(row: { id?: string; etag?: string } | null | undefined): Revision | null {
  if (!row?.id) return {};
  return row.etag ? { id: row.id, etag: row.etag } : null;
}
