import type { iam, master, people, platform } from '@mysms/sdk';
import {
  countries as seedCountries,
  operations as seedOperations,
  platformAdmin,
  schoolAdmin,
  schoolAdminPerson,
  settingDefinitions,
  TENANT_CODE,
  tokens,
} from './data';
import { json, problem, type Handler } from './fakeApi';
import { pageOf } from './paging';
import { patched } from './patch';

type Country = master.Country;
type Operation = platform.Operation;
type SettingValue = iam.SettingValue;
type EffectiveSetting = iam.EffectiveSetting;

/** The catalogue's value rules, as the server checks them; returns a message or null. */
export function settingValueError(definition: iam.SettingDefinition, value: unknown): string | null {
  if (typeof value !== 'string') return 'A value is required';
  switch (definition.valueType) {
    case 'boolean':
      return value === 'true' || value === 'false' ? null : 'Must be true or false';
    case 'integer': {
      const n = Number(value);
      if (!/^-?(0|[1-9]\d*)$/.test(value)) return 'Must be a whole number';
      if ((definition.minValue ?? n) > n || (definition.maxValue ?? n) < n)
        return `Must be between ${definition.minValue} and ${definition.maxValue}`;
      return null;
    }
    case 'choice':
      return definition.allowedValues?.includes(value)
        ? null
        : `Must be one of ${definition.allowedValues?.join(', ')}`;
    default:
      return value.length <= 500 ? null : 'Use at most 500 characters';
  }
}

export interface FakeBackendOptions {
  /** Who signs in; defaults to the platform operator. */
  me?: iam.Me;
  /** The signed-in user's person record (people.GetMyPerson); defaults to the school admin's when it is theirs. */
  person?: people.Person;
  /** Password accepted for every user. */
  password?: string;
}

const etagOf = (version: number) => `"${version}"`;

/**
 * A stateful fake of the endpoints the skeleton screens use, with the contract's semantics:
 * bearer auth, If-Match/ETag on writes (412 when stale), problem+json errors, per-module operations.
 */
export function fakeBackend(options: FakeBackendOptions = {}): Record<string, Handler> {
  const me = options.me ?? platformAdmin;
  const password = options.password ?? 'password';
  const rows = new Map<string, { row: Country; version: number }>(
    seedCountries.map((c) => [c.id as string, { row: { ...c }, version: 1 }]),
  );
  const ops = new Map<string, Operation>(seedOperations.map((o) => [o.operationId, { ...o }]));
  let issued = 0;
  const userId = me.user?.id ?? '';
  const defaultPerson = me.user?.personId === schoolAdmin.user?.personId ? schoolAdminPerson : undefined;
  const initialPerson = options.person ?? defaultPerson;
  let person: { row: people.Person; version: number } | undefined = initialPerson
    ? { row: { ...initialPerson }, version: 1 }
    : undefined;
  const values = new Map<string, { row: SettingValue; version: number }>();
  let valueSeq = 0;
  /** Administrator-only settings exist only for setting administrators (the fake has tenant-wide roles only). */
  const administrator = (me.permissions ?? []).includes('iam.setting_value.read');
  const visible = (d: iam.SettingDefinition) =>
    administrator || (d.userEditable === true && (d.allowedScopes ?? []).includes('user'));
  const visibleDefinitions = () => settingDefinitions.filter(visible);
  const definitionOf = (key: string) => visibleDefinitions().find((d) => d.settingKey === key);
  const valueAt = (key: string, scopeType: string, scopeId?: string) =>
    [...values.values()].find(
      (v) =>
        v.row.settingKey === key && v.row.scopeType === scopeType && (v.row.scopeId ?? undefined) === scopeId,
    );
  /** Tenant and user scopes only (the fake has no institutions or departments); a tenant lock wins. */
  const effective = (definition: iam.SettingDefinition): EffectiveSetting => {
    const tenant = valueAt(definition.settingKey, 'tenant');
    const own = valueAt(definition.settingKey, 'user', userId);
    const userEditable =
      definition.userEditable === true && (definition.allowedScopes ?? []).includes('user');
    if (tenant?.row.isLocked)
      return {
        settingKey: definition.settingKey,
        value: tenant.row.value,
        sourceScope: 'tenant',
        locked: true,
        userEditable,
      };
    if (own)
      return {
        settingKey: definition.settingKey,
        value: own.row.value,
        sourceScope: 'user',
        sourceScopeId: userId,
        locked: false,
        userEditable,
      };
    if (tenant)
      return {
        settingKey: definition.settingKey,
        value: tenant.row.value,
        sourceScope: 'tenant',
        locked: false,
        userEditable,
      };
    return {
      settingKey: definition.settingKey,
      value: definition.defaultValue ?? '',
      sourceScope: 'default',
      locked: false,
      userEditable,
    };
  };
  const invalid = (field: string, message: string) =>
    problem(422, 'VALIDATION_FAILED', 'Check the highlighted fields', { errors: [{ field, message }] });

  const authed = (req: Request) => req.headers.get('Authorization')?.startsWith('Bearer ') === true;
  const unauthenticated = () => problem(401, 'UNAUTHENTICATED', 'Sign in required');
  const pair = () => ({ ...tokens, accessToken: `${tokens.accessToken}-${++issued}` });
  const withEtag = (row: Country, version: number, status = 200) =>
    json(row, { status, headers: { ETag: etagOf(version) } });

  const readCountry = (id: string) => rows.get(id);
  const ifMatch = (req: Request, version: number) => {
    const header = req.headers.get('If-Match');
    if (!header) return problem(428, 'PRECONDITION_REQUIRED', 'If-Match is required');
    if (header !== etagOf(version))
      return problem(412, 'STALE_ROW_VERSION', 'This record changed since you loaded it');
    return null;
  };

  return {
    'POST /v1/iam/auth:login': async (req) => {
      const body = (await req.json()) as iam.LoginRequest;
      if (
        body.tenantCode !== TENANT_CODE ||
        body.identifier !== me.user?.username ||
        body.password !== password
      ) {
        return problem(401, 'INVALID_CREDENTIALS', 'Those details did not match');
      }
      return json({ tokens: pair(), user: me.user });
    },
    'POST /v1/iam/auth:refresh': () => json(pair()),
    'POST /v1/iam/auth:logout': (req) =>
      authed(req) ? new Response(null, { status: 204 }) : unauthenticated(),
    'GET /v1/iam/me': (req) =>
      authed(req) ? json(person ? { ...me, person: person.row } : me) : unauthenticated(),

    'GET /v1/iam/me/settings': (req) =>
      authed(req) ? json({ items: visibleDefinitions().map(effective) }) : unauthenticated(),
    'PUT /v1/iam/me/settings/{settingKey}': async (req, p) => {
      if (!authed(req)) return unauthenticated();
      const definition = definitionOf(p.settingKey ?? '');
      if (!definition) return problem(404, 'NOT_FOUND', 'No such setting');
      const { value } = (await req.json()) as { value?: unknown };
      if (!effective(definition).userEditable)
        return invalid('settingKey', 'Users cannot change this setting');
      const error = settingValueError(definition, value);
      if (error) return invalid('value', error);
      if (effective(definition).locked)
        return problem(409, 'SETTING_LOCKED', 'A broader scope locks this setting');
      const own = valueAt(definition.settingKey, 'user', userId);
      if (own) {
        own.row = { ...own.row, value: value as string };
        own.version += 1;
      } else {
        const id = `01960000-0000-7000-8000-${String(++valueSeq).padStart(12, '0')}`;
        values.set(id, {
          row: {
            id,
            scopeType: 'user',
            scopeId: userId,
            settingKey: definition.settingKey,
            value: value as string,
            isLocked: false,
          },
          version: 1,
        });
      }
      return json(effective(definition));
    },
    'DELETE /v1/iam/me/settings/{settingKey}': (req, p) => {
      if (!authed(req)) return unauthenticated();
      const definition = definitionOf(p.settingKey ?? '');
      if (!definition) return problem(404, 'NOT_FOUND', 'No such setting');
      const own = valueAt(definition.settingKey, 'user', userId);
      if (own?.row.id) values.delete(own.row.id);
      return new Response(null, { status: 204 });
    },
    'GET /v1/iam/setting-definitions': (req) =>
      authed(req) ? pageOf(req, visibleDefinitions()) : unauthenticated(),
    'GET /v1/iam/setting-values': (req) => {
      if (!authed(req)) return unauthenticated();
      return pageOf(
        req,
        [...values.values()].map((v) => ({ ...v.row, etag: etagOf(v.version) })),
      );
    },
    'POST /v1/iam/setting-values': async (req) => {
      if (!authed(req)) return unauthenticated();
      const body = (await req.json()) as SettingValue;
      const definition = definitionOf(body.settingKey);
      if (!definition) return invalid('settingKey', 'Not a setting of the catalogue');
      if (!(definition.allowedScopes ?? []).includes(body.scopeType))
        return invalid('scopeType', `This setting cannot be set at ${body.scopeType} scope`);
      if ((body.scopeType === 'tenant') !== !body.scopeId)
        return invalid(
          'scopeId',
          body.scopeType === 'tenant' ? 'Must be absent for the tenant scope' : 'Is required for this scope',
        );
      if (body.scopeType === 'user' && body.isLocked)
        return invalid('isLocked', 'A user-level value cannot be locked');
      const error = settingValueError(definition, body.value);
      if (error) return invalid('value', error);
      if (valueAt(body.settingKey, body.scopeType, body.scopeId ?? undefined))
        return problem(409, 'DUPLICATE', 'This setting already has a value at this scope');
      const id = `01960000-0000-7000-8000-${String(++valueSeq).padStart(12, '0')}`;
      const row: SettingValue = { ...body, id, isLocked: body.isLocked === true };
      values.set(id, { row, version: 1 });
      return json({ ...row, etag: etagOf(1) }, { status: 201, headers: { ETag: etagOf(1) } });
    },
    'PATCH /v1/iam/setting-values/{settingValueId}': async (req, p) => {
      if (!authed(req)) return unauthenticated();
      const found = values.get(p.settingValueId ?? '');
      if (!found) return problem(404, 'NOT_FOUND', 'Setting value not found');
      const precondition = ifMatch(req, found.version);
      if (precondition) return precondition;
      const changes = (await req.json()) as iam.SettingValueUpdate;
      const next = { ...found.row, ...changes };
      const definition = definitionOf(next.settingKey);
      const error = definition ? settingValueError(definition, next.value) : 'Not a setting of the catalogue';
      if (error) return invalid('value', error);
      if (next.scopeType === 'user' && next.isLocked)
        return invalid('isLocked', 'A user-level value cannot be locked');
      found.row = next;
      found.version += 1;
      return json(
        { ...found.row, etag: etagOf(found.version) },
        { headers: { ETag: etagOf(found.version) } },
      );
    },
    'DELETE /v1/iam/setting-values/{settingValueId}': (req, p) => {
      if (!authed(req)) return unauthenticated();
      const found = values.get(p.settingValueId ?? '');
      if (!found) return problem(404, 'NOT_FOUND', 'Setting value not found');
      const precondition = ifMatch(req, found.version);
      if (precondition) return precondition;
      values.delete(p.settingValueId ?? '');
      return new Response(null, { status: 204 });
    },

    'GET /v1/people/me': (req) => {
      if (!authed(req)) return unauthenticated();
      return person
        ? json({ ...person.row, etag: etagOf(person.version) }, { headers: { ETag: etagOf(person.version) } })
        : problem(404, 'NOT_FOUND', 'No person record is linked to this account');
    },
    'PATCH /v1/people/me': async (req) => {
      if (!authed(req)) return unauthenticated();
      if (!person) return problem(404, 'NOT_FOUND', 'No person record is linked to this account');
      const precondition = ifMatch(req, person.version);
      if (precondition) return precondition;
      const changes = (await req.json()) as people.MyPersonUpdate;
      if (changes.websiteUrl && !/^https?:\/\/[^\s/]+/i.test(changes.websiteUrl))
        return invalid('websiteUrl', 'Must be an http or https address');
      if (changes.primaryEmail && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(changes.primaryEmail))
        return invalid('primaryEmail', 'Must be an e-mail address');
      person = { row: patched(person.row, changes), version: person.version + 1 };
      return json(
        { ...person.row, etag: etagOf(person.version) },
        { headers: { ETag: etagOf(person.version) } },
      );
    },

    'GET /v1/master/countries': (req) => {
      const all = [...rows.values()]
        .map((r) => r.row)
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
      return pageOf(req, all);
    },
    'GET /v1/master/countries/{countryId}': (_req, p) => {
      const found = readCountry(p.countryId ?? '');
      return found ? withEtag(found.row, found.version) : problem(404, 'NOT_FOUND', 'Country not found');
    },
    'POST /v1/master/countries': async (req) => {
      if (!authed(req)) return unauthenticated();
      const body = (await req.json()) as Country;
      if (!body.name || !body.iso2) {
        return problem(422, 'VALIDATION_FAILED', 'Check the highlighted fields', {
          errors: [
            ...(body.name ? [] : [{ field: 'name', message: 'Name is required' }]),
            ...(body.iso2 ? [] : [{ field: 'iso2', message: 'ISO 2 is required' }]),
          ],
        });
      }
      const id = `01960000-0000-7000-8000-${String(rows.size + 1).padStart(12, '0')}`;
      const row: Country = { isActive: true, sortOrder: rows.size + 1, ...body, id };
      rows.set(id, { row, version: 1 });
      return withEtag(row, 1, 201);
    },
    'PATCH /v1/master/countries/{countryId}': async (req, p) => {
      if (!authed(req)) return unauthenticated();
      const found = readCountry(p.countryId ?? '');
      if (!found) return problem(404, 'NOT_FOUND', 'Country not found');
      const precondition = ifMatch(req, found.version);
      if (precondition) return precondition;
      const changes = (await req.json()) as master.CountryUpdate;
      found.row = patched(found.row, changes);
      found.version += 1;
      return withEtag(found.row, found.version);
    },
    'DELETE /v1/master/countries/{countryId}': (req, p) => {
      if (!authed(req)) return unauthenticated();
      const found = readCountry(p.countryId ?? '');
      if (!found) return problem(404, 'NOT_FOUND', 'Country not found');
      const precondition = ifMatch(req, found.version);
      if (precondition) return precondition;
      rows.delete(p.countryId ?? '');
      return new Response(null, { status: 204 });
    },

    'GET /v1/platform/operations': (req) =>
      authed(req) ? pageOf(req, [...ops.values()]) : unauthenticated(),
    'GET /v1/{module}/operations/{operationId}': (req, p) => {
      if (!authed(req)) return unauthenticated();
      const op = ops.get(p.operationId ?? '');
      return op && op.module === p.module ? json(op) : problem(404, 'NOT_FOUND', 'Operation not found');
    },
    'POST /v1/{module}/operations/{operationId}:cancel': (req, p) => {
      if (!authed(req)) return unauthenticated();
      const op = ops.get(p.operationId ?? '');
      if (!op || op.module !== p.module) return problem(404, 'NOT_FOUND', 'Operation not found');
      if (op.status !== 'running' && op.status !== 'queued')
        return problem(409, 'INVALID_STATE_TRANSITION', 'Only queued or running operations can be cancelled');
      const cancelled: Operation = { ...op, status: 'cancelled', doneTime: new Date().toISOString() };
      ops.set(op.operationId, cancelled);
      return json(cancelled);
    },
  };
}
