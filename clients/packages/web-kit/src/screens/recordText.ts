import { isApiError, type FieldDef, type useI18n } from '@mysms/core';

type Translate = ReturnType<typeof useI18n>['t'];
type Format = ReturnType<typeof useI18n>['format'];

/** A field's value as words: dates in the reader's format, switches as yes/no, enum values by their label. */
export function display(t: Translate, field: FieldDef, value: unknown, format?: Format): string {
  if (value === undefined || value === null || value === '') return '—';
  if (format && field.kind === 'date' && /^\d{4}-\d{2}-\d{2}/.test(String(value)))
    return format.day(String(value));
  if (format && field.kind === 'datetime' && !Number.isNaN(Date.parse(String(value))))
    return format.dateTime(String(value));
  if (field.kind === 'boolean') return value === true ? t('adm.records.yes') : t('adm.records.no');
  if (field.kind === 'enum' && field.values) {
    const index = field.values.indexOf(String(value));
    return index >= 0 ? (field.valueLabels?.[index] ?? String(value)) : t('enum.common.unknown');
  }
  return String(value);
}

/** What a failed record read or write tells the reader (field errors, refusal, a stale copy, a conflict). */
export const problemText = (t: Translate, error: unknown): string | undefined => {
  if (!error) return undefined;
  if (isApiError(error) && error.status === 422)
    return error.errors.map((e) => `${e.field}: ${e.message}`).join(' ') || t('common.validation.pattern');
  if (isApiError(error) && error.status === 403) return t('adm.records.forbidden');
  if (isApiError(error) && error.status === 412) return t('common.conflict.body');
  if (isApiError(error) && error.status === 409) return error.detail ?? t('common.error.generic');
  return t('common.error.generic');
};
