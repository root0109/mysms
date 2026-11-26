import type { ApiClient } from '../api/client';
import { ApiError } from '../api/problem';
import type { WithEtag } from '../api/unwrap';
import type { ActionDef, FieldDef, RecordPage, RecordRow, ResourceDef } from './types';

export interface RecordListParams {
  /** The parent row's id, for nested resources. */
  parentId?: string;
  /** One-based page number (default 1). */
  page?: number;
  size?: number;
  /** The cursor of the previous page's next link. */
  cursor?: string;
  filter?: string;
  /** Sort criteria `field,asc|desc`, in priority order. */
  sort?: string[];
}

function path(template: string, parentId: string | undefined, id?: string): string {
  let out = template;
  const params = [...template.matchAll(/\{(\w+)\}/g)].map((m) => m[1] as string);
  for (const [index, name] of params.entries()) {
    const value = id !== undefined && index === params.length - 1 ? id : parentId;
    if (value === undefined) throw new Error(`no value for {${name}} in ${template}`);
    out = out.replace(`{${name}}`, encodeURIComponent(value));
  }
  return out;
}

/** One call of a contract path through the API client's transport (token, locale, retries); a failure is an ApiError. */
export async function send(
  api: ApiClient,
  method: string,
  url: string,
  body?: unknown,
  headers: Record<string, string> = {},
) {
  const fetchApi = api.configuration.fetchApi ?? globalThis.fetch;
  const init: RequestInit = {
    method,
    headers: { Accept: 'application/json, application/problem+json', ...headers },
  };
  if (body !== undefined) {
    init.body = JSON.stringify(body);
    (init.headers as Record<string, string>)['Content-Type'] = 'application/json';
  }
  const response = await fetchApi(`${api.configuration.basePath}${url}`, init);
  if (!response.ok) {
    const problem: unknown = await response
      .clone()
      .json()
      .catch(() => undefined);
    throw ApiError.fromResponse(response, problem);
  }
  return response;
}

function op(def: ResourceDef, kind: keyof ResourceDef['ops']) {
  const ref = def.ops[kind];
  if (!ref) throw new Error(`${def.key} has no ${kind} operation`);
  return ref;
}

/** Only the fields a create may carry, with empty inputs left out (the server keeps its defaults). */
export function toPayload(def: ResourceDef, values: Record<string, unknown>) {
  const out: Record<string, unknown> = {};
  for (const field of def.fields) {
    if (field.readOnly) continue;
    const value = values[field.name];
    if (value === undefined || value === null || value === '') continue;
    out[field.name] = coerce(field, value);
  }
  return out;
}

/**
 * A PATCH body (JAVA-CODING-RULES §7.2): only the fields the caller passes are sent — a value sets the field, an empty
 * one is sent as null and clears it — and a field left out keeps its value on the server. A secret is never returned,
 * so an empty one means "keep it" and is left out too.
 */
export function toPatch(def: ResourceDef, values: Record<string, unknown>) {
  const out: Record<string, unknown> = {};
  for (const field of def.fields) {
    if (field.readOnly || !field.writable || !(field.name in values)) continue;
    const value = values[field.name];
    const empty = value === undefined || value === null || value === '';
    if (empty && field.writeOnly) continue;
    out[field.name] = empty ? null : coerce(field, value);
  }
  return out;
}

function coerce(field: FieldDef, value: unknown): unknown {
  if (field.kind === 'integer') {
    if (typeof value === 'number') return value;
    const n = Number(value);
    // not a whole number: sent as typed, so the server's validation names the field (never a silent null)
    return Number.isInteger(n) ? n : value;
  }
  if (field.kind === 'boolean') return value === true || value === 'true';
  return value;
}

/** The longest range a list over a partitioned table may name (the server's limit). */
export const MAX_RANGE_DAYS = 366;

/**
 * The filter that bounds a partitioned resource's list to the days `from`..`to` (both `YYYY-MM-DD`, inclusive), or
 * undefined for other resources. Timestamps are bounded in the viewer's own time zone: from the start of `from` to
 * before the start of the day after `to`, sent as UTC instants.
 */
export function rangeFilter(def: ResourceDef, from: string, to: string): string | undefined {
  if (!def.partition) return undefined;
  const { field, kind } = def.partition;
  if (kind === 'date') return `${field} >= "${from}" AND ${field} <= "${to}"`;
  const start = new Date(`${from}T00:00:00`);
  const after = new Date(`${to}T00:00:00`);
  after.setDate(after.getDate() + 1);
  return `${field} >= "${start.toISOString()}" AND ${field} < "${after.toISOString()}"`;
}

/**
 * Joins filters with AND; undefined when none is given. A filter with OR is put in parentheses; one without is left
 * bare, so a partition range stays at the top level, where the server looks for it.
 */
export function allOf(...filters: (string | undefined)[]): string | undefined {
  const given = filters.filter((f): f is string => !!f);
  if (given.length <= 1) return given[0];
  return given.map((f) => (/\bOR\b/.test(f) ? `(${f})` : f)).join(' AND ');
}

export async function listRecords(api: ApiClient, def: ResourceDef, params: RecordListParams = {}) {
  const parts: string[] = [];
  const add = (name: string, value: string | number | undefined) => {
    if (value !== undefined && value !== '') parts.push(`${name}=${encodeURIComponent(String(value))}`);
  };
  add('page', params.page);
  add('size', params.size);
  add('cursor', params.cursor);
  add('filter', params.filter);
  for (const criterion of params.sort ?? []) add('sort', criterion);
  const qs = parts.join('&');
  const response = await send(api, 'GET', path(op(def, 'List').path, params.parentId) + (qs ? `?${qs}` : ''));
  return (await response.json()) as RecordPage;
}

export async function getRecord(api: ApiClient, def: ResourceDef, id: string, parentId?: string) {
  const response = await send(api, 'GET', path(op(def, 'Get').path, parentId, id));
  return {
    data: (await response.json()) as RecordRow,
    etag: response.headers.get('ETag'),
  } as WithEtag<RecordRow>;
}

export async function createRecord(
  api: ApiClient,
  def: ResourceDef,
  values: Record<string, unknown>,
  parentId?: string,
) {
  const body = toPayload(def, values);
  if (def.parent && parentId) body[def.parent] = parentId;
  const response = await send(api, 'POST', path(op(def, 'Create').path, parentId), body);
  return (await response.json()) as RecordRow;
}

export async function updateRecord(
  api: ApiClient,
  def: ResourceDef,
  id: string,
  etag: string,
  values: Record<string, unknown>,
  parentId?: string,
) {
  const item = path(op(def, 'Update').path, parentId, id);
  const body = toPatch(def, values);
  const response = await send(api, 'PATCH', item, body, { 'If-Match': etag });
  return (await response.json()) as RecordRow;
}

export async function deleteRecord(
  api: ApiClient,
  def: ResourceDef,
  id: string,
  etag: string,
  parentId?: string,
) {
  await send(api, 'DELETE', path(op(def, 'Delete').path, parentId, id), undefined, { 'If-Match': etag });
}

/** Whether a row is in a state the action allows (a missing boolean counts as false, as on the server). */
export function actionAllowed(action: ActionDef, row: RecordRow): boolean {
  return Object.entries(action.guard).every(([field, allowed]) => {
    const value = row[field];
    return allowed.some((v) => (typeof v === 'boolean' ? v === (value === true) : v === value));
  });
}

/** Runs a state transition on one row; the answer is the changed row (409 when the state no longer allows it). */
export async function runAction(
  api: ApiClient,
  action: ActionDef,
  id: string,
  etag: string | null,
  values: { reason?: string; params?: Record<string, unknown> },
  parentId?: string,
) {
  const body: Record<string, unknown> = {};
  if (action.etag && etag) body.etag = etag;
  if (action.reason && values.reason?.trim()) body.reason = values.reason.trim();
  for (const field of action.params) {
    const value = values.params?.[field.name];
    if (value !== undefined && value !== null && value !== '') body[field.name] = coerce(field, value);
  }
  const headers: Record<string, string> = action.ifMatch && etag ? { 'If-Match': etag } : {};
  const response = await send(api, 'POST', path(action.path, parentId, id), body, headers);
  return (await response.json()) as RecordRow;
}

/** A filter that holds for rows whose `field` is one of `values` (OR of equals); undefined for no values. */
export function fieldIn(field: string, values: readonly string[]): string | undefined {
  if (values.length === 0) return undefined;
  return values.map((value) => `${field} = "${value.replace(/["\\]/g, '')}"`).join(' OR ');
}
