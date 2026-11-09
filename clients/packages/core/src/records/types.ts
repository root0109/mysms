/** One field of a resource as the generic record screens show and edit it (from the contract). */
export interface FieldDef {
  name: string;
  /** English label derived from the contract field name. */
  label: string;
  kind: 'text' | 'integer' | 'decimal' | 'boolean' | 'date' | 'datetime' | 'time' | 'id' | 'enum';
  values?: readonly string[];
  /** Display words for `values`, index for index. */
  valueLabels?: readonly string[];
  maxLength?: number;
  required?: boolean;
  /** Set only by the server (workflows, defaults); shown, never sent. */
  readOnly?: boolean;
  /** A secret: sent, never returned (the server shows a masked form in another field). */
  writeOnly?: boolean;
  /** Where an omitted switch lands in the database (booleans only). */
  defaultValue?: boolean;
  /** Can be changed after creation (the contract's Update schema). */
  writable?: boolean;
}

export interface OperationRef {
  path: string;
  permission: string;
}

/** A resource whose standard operations the backend serves (api/standard-resources.json). */
export interface ResourceDef {
  /** `<module>.<Resource>`, e.g. `sis.StudentCategory`. */
  key: string;
  module: string;
  label: string;
  /** Path parameter of the parent row for nested resources, e.g. `studentId`. */
  parent?: string;
  /** How rows belong to institutions: their own column, a parent row, the row itself (institutions), tenant-level, or global (platform tables shared by all tenants). */
  scope: 'direct' | 'chain' | 'self' | 'tenant' | 'global';
  ops: Partial<Record<'List' | 'Get' | 'Create' | 'Update' | 'Delete', OperationRef>>;
  fields: readonly FieldDef[];
  /** The generated state transitions of the resource (api/state-machines.md). */
  actions?: readonly ActionDef[];
  /** A partitioned table: every list names a range of this field (at most 366 days), or the server answers 400. */
  partition?: { field: string; kind: 'date' | 'datetime' };
}

/** One state transition: a POST on the row, allowed only while the row's fields hold one of the guard values. */
export interface ActionDef extends OperationRef {
  /** `<module>.<OperationId>`, e.g. `comms.PublishNotice`. */
  operation: string;
  label: string;
  /** Field name → the values the row must have (all fields must match); empty: any state. */
  guard: Readonly<Record<string, readonly (string | boolean)[]>>;
  /** The request carries the row's etag (412 when stale). */
  etag: boolean;
  /** The step is sent with If-Match (428 without, 412 when stale). */
  ifMatch: boolean;
  /** The request carries an optional reason, kept in the audit record. */
  reason: boolean;
  /** Further request fields. */
  params: readonly FieldDef[];
}

/** A row as the API returns it. */
export type RecordRow = Record<string, unknown> & { id: string; etag?: string };

export interface RecordPage {
  items: RecordRow[];
  nextPageToken?: string;
  totalSize?: number;
}
