import type { ResourceDef } from '@mysms/core';
import type { MessageKey } from '@mysms/i18n';

/** Fields that name a row, in the order a reader looks for them. */
const NAMING = [
  'code',
  'name',
  'title',
  'displayName',
  'firstName',
  'lastName',
  'admissionNumber',
  'subject',
  'accessionNumber',
];

/** Columns when the screen names none: the naming fields first, then the status, then dates, at most five. */
export function defaultColumns(def: ResourceDef): string[] {
  const shown = def.fields.filter((f) => f.kind !== 'id' && !f.writeOnly);
  const naming = NAMING.flatMap((name) => shown.filter((f) => f.name === name));
  const status = shown.filter((f) => f.kind === 'enum' && /status|state/i.test(f.name));
  const dates = shown.filter((f) => f.kind === 'date' || f.kind === 'datetime');
  const rest = shown.filter((f) => f.kind === 'enum' || f.kind === 'boolean' || f.kind === 'text');
  const ordered = [...naming, ...status, ...dates, ...rest];
  return [...new Set(ordered.map((f) => f.name))].slice(0, 5);
}

/** The status-like enum a list is most often narrowed by, when the record type has one. */
export function defaultFilter(def: ResourceDef): string | undefined {
  return def.fields.find((f) => f.kind === 'enum' && /status|state/i.test(f.name))?.name;
}

/** The heading of an inventory screen, from the catalogue (`screen.<ID without the hyphen>.title`, e.g. `screen.SIS01.title`). */
export const screenTitleKey = (id: string) => `screen.${id.replace('-', '')}.title` as MessageKey;
