import {
  actionAllowed,
  allOf,
  MAX_RANGE_DAYS,
  rangeFilter,
  RESOURCES,
  useCreateRecord,
  useDeleteRecord,
  useI18n,
  useInfiniteRecords,
  useRecord,
  useRecordAction,
  useSession,
  useUpdateRecord,
  type ActionDef,
  type FieldDef,
  type RecordRow,
  type ResourceDef,
} from '@mysms/core';
import {
  Banner,
  Button,
  Card,
  DataTable,
  Dialog,
  SelectField,
  StatusChip,
  Switch,
  TextInput,
  type Column,
} from '@mysms/ui-web';
import { useMemo, useState, type ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router';
import { isoDay } from './dates';
import { display, problemText } from './recordText';
import { UUID } from './ids';

const SEARCH_FIELDS = ['name', 'title', 'code', 'displayName', 'subject'];

/** The record types the caller may list: the edge would answer 403 for the others. */
function useReadable(): ResourceDef[] {
  const permissions = useSession((s) => s.me?.permissions);
  return useMemo(
    () =>
      RESOURCES.filter((r) => {
        const code = r.ops.List?.permission;
        if (!code || !permissions) return false;
        // records that belong to one user account (sessions, devices…) are an administrator's view of other users;
        // everyone reaches their own through the account screens (ACC-08), so they need user administration here
        if (r.parent === 'userAccountId' && !hasPermission(permissions, 'iam.user_account.read'))
          return false;
        return hasPermission(permissions, code);
      }),
    [permissions],
  );
}

/** Mirrors the token's compacted wildcards (`*`, `module.*`, `module.resource.*`). */
function hasPermission(granted: readonly string[], code: string): boolean {
  if (granted.includes(code) || granted.includes('*')) return true;
  for (let dot = code.indexOf('.'); dot > 0; dot = code.indexOf('.', dot + 1)) {
    if (granted.includes(`${code.slice(0, dot + 1)}*`)) return true;
  }
  return false;
}

/**
 * ADM-20 Records: every resource whose standard operations the server serves, browsed and edited generically from
 * the contract (api/standard-resources.json). Only the record types the caller may read are offered; create, edit
 * and delete appear only with their permissions, and the server still decides which rows (institution scope,
 * tenant-level records) the caller may see or change.
 */
export function RecordsScreen() {
  const { t } = useI18n();
  const readable = useReadable();
  const [key, setKey] = useState<string>('');
  const def = readable.find((r) => r.key === key) ?? readable[0];
  const [parentId, setParentId] = useState('');
  const [search, setSearch] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState<RecordRow | null>(null);
  const [creating, setCreating] = useState(false);
  if (!def) {
    return (
      <Card title={t('adm.records.title')}>
        <p className="hint">{t('adm.records.none')}</p>
      </Card>
    );
  }
  return (
    <RecordsOf
      key={def.key}
      def={def}
      readable={readable}
      onPick={(next) => {
        setKey(next);
        setParentId('');
        setSearch('');
        setNotice(null);
      }}
      parentId={parentId}
      setParentId={setParentId}
      search={search}
      setSearch={setSearch}
      notice={notice}
      setNotice={setNotice}
      editing={editing}
      setEditing={setEditing}
      creating={creating}
      setCreating={setCreating}
    />
  );
}

interface RecordsOfProps {
  def: ResourceDef;
  /** The record types offered in the picker (ADM-11); a purpose-built screen shows one type and no picker. */
  readable?: ResourceDef[];
  onPick?: (key: string) => void;
  /** Heading and introduction; ADM-11's by default. */
  title?: string;
  intro?: string;
  /** Contract field names shown as columns, in order (default: the first four displayable fields). */
  columns?: readonly string[];
  /** An enum field offered as a filter above the list. */
  filterField?: string;
  /** Where a row's own screen is, when the record type has one; adds an "Open" link to every row. */
  rowLink?: ((row: RecordRow) => string) | undefined;
  /** Something a row can start besides its editor (e.g. taking attendance for a session); its own column. */
  rowTool?: ((row: RecordRow) => ReactNode) | undefined;
  /** Names of the ids a column holds, by field (a subject's name for `subjectId`), read by the page. */
  names?: Readonly<Record<string, ReadonlyMap<string, string>>> | undefined;
  parentId: string;
  setParentId: (v: string) => void;
  /** The parent is the page's own record (a tab of a detail page): it is not asked for. */
  fixedParent?: boolean;
  /** A condition every listed row meets, beside the reader's own (a title's copies, a class's sessions). */
  scope?: string | undefined;
  /** Values a new row starts with and keeps (the record the page is about); not editable in the dialog. */
  presets?: Readonly<Record<string, unknown>> | undefined;
  search: string;
  setSearch: (v: string) => void;
  notice: string | null;
  setNotice: (v: string | null) => void;
  editing: RecordRow | null;
  setEditing: (v: RecordRow | null) => void;
  creating: boolean;
  setCreating: (v: boolean) => void;
}

export function RecordsOf(p: RecordsOfProps) {
  const { t, format } = useI18n();
  const { def } = p;
  const filterDef = p.filterField
    ? def.fields.find((f) => f.name === p.filterField && f.kind === 'enum')
    : undefined;
  // the chosen filter lives in the address (?status=pending): a link or a reload opens the same list
  const [query, setQuery] = useSearchParams();
  const asked = filterDef ? (query.get(filterDef.name) ?? '') : '';
  const filterValue = filterDef?.values?.includes(asked) ? asked : '';
  const setFilterValue = (value: string) => {
    if (!filterDef) return;
    setQuery(
      (previous) => {
        const next = new URLSearchParams(previous);
        if (value) next.set(filterDef.name, value);
        else next.delete(filterDef.name);
        return next;
      },
      { replace: true },
    );
  };
  const permissions = useSession((s) => s.me?.permissions) ?? [];
  const may = (kind: keyof ResourceDef['ops']) => {
    const code = def.ops[kind]?.permission;
    return code !== undefined && hasPermission(permissions, code);
  };
  const parentOk = !def.parent || UUID.test(p.parentId.trim());
  const searchField = def.fields.find((f) => SEARCH_FIELDS.includes(f.name) && f.kind === 'text');
  const search =
    searchField && p.search.trim()
      ? `${searchField.name} : "${p.search.trim().replace(/["\\]/g, '')}"`
      : undefined;
  // partitioned record types are listed one date range at a time (the server reads only those partitions)
  const [from, setFrom] = useState(() => isoDay(-30));
  const [to, setTo] = useState(() => isoDay(0));
  const span = (Date.parse(to) - Date.parse(from)) / 86_400_000;
  const rangeOk = !def.partition || (span >= 0 && span <= MAX_RANGE_DAYS);
  const chosen = filterDef && filterValue ? `${filterDef.name} = "${filterValue}"` : undefined;
  const filter = allOf(p.scope, search, chosen, rangeOk ? rangeFilter(def, from, to) : undefined);
  const rows = useInfiniteRecords(def, {
    size: 50,
    parentId: parentOk && def.parent ? p.parentId.trim() : undefined,
    filter,
  });
  const items = rows.data?.pages.flatMap((page) => page.data) ?? [];
  const displayable = def.fields.filter((f) => f.kind !== 'id' && !f.writeOnly);
  const picked = (p.columns ?? [])
    .map((name) => def.fields.find((f) => f.name === name && !f.writeOnly))
    .filter((f): f is FieldDef => f !== undefined);
  const shown = picked.length ? picked : displayable.slice(0, 4);
  const columns: Column<RecordRow>[] = shown.map((field) => ({
    key: field.name,
    header: field.label,
    render: (row) =>
      field.kind === 'enum' ? (
        <StatusChip intent="neutral" label={display(t, field, row[field.name])} />
      ) : (
        (p.names?.[field.name]?.get(String(row[field.name])) ?? display(t, field, row[field.name], format))
      ),
  }));
  const { rowLink } = p;
  const nameField = shown[0];
  if (rowLink)
    columns.push({
      key: 'open',
      header: t('adm.resource.open'),
      render: (row) => (
        // the row itself opens the editor: the link keeps its own click and key presses
        <Link
          to={rowLink(row)}
          aria-label={t('adm.resource.openRow', {
            name: nameField ? display(t, nameField, row[nameField.name], format) : def.label,
          })}
          onClick={(event) => event.stopPropagation()}
          onKeyDown={(event) => event.stopPropagation()}
        >
          {t('adm.resource.open')}
        </Link>
      ),
    });
  const { rowTool } = p;
  if (rowTool)
    columns.push({
      key: 'tool',
      header: t('adm.resource.rowActions'),
      render: (row) => (
        // the row itself opens the editor: what the tool does must not also do that
        <span onClick={(event) => event.stopPropagation()} onKeyDown={(event) => event.stopPropagation()}>
          {rowTool(row)}
        </span>
      ),
    });
  const done = (message: string) => {
    p.setNotice(message);
    p.setEditing(null);
    p.setCreating(false);
  };
  const byModule = (p.readable ?? []).map((r) => ({ value: r.key, label: `${r.module} · ${r.label}` }));
  return (
    <>
      <Card
        title={p.title ?? t('adm.records.title')}
        actions={
          may('Create') ? (
            <Button onClick={() => p.setCreating(true)} disabled={!parentOk}>
              {t('adm.records.new')}
            </Button>
          ) : null
        }
      >
        <p className="hint">{p.intro ?? t('adm.records.intro')}</p>
        {p.readable && p.onPick ? (
          <SelectField
            label={t('adm.records.resource')}
            value={def.key}
            onChange={(e) => p.onPick?.(e.target.value)}
            options={byModule}
          />
        ) : null}
        {filterDef ? (
          <SelectField
            label={filterDef.label}
            value={filterValue}
            onChange={(e) => setFilterValue(e.target.value)}
            options={[
              { value: '', label: t('adm.records.all') },
              ...(filterDef.values ?? []).map((value, index) => ({
                value,
                label: filterDef.valueLabels?.[index] ?? value,
              })),
            ]}
          />
        ) : null}
        {def.parent && !p.fixedParent ? (
          <TextInput
            label={t('adm.records.parent')}
            hint={t('adm.records.parentHint')}
            value={p.parentId}
            error={p.parentId && !parentOk ? t('common.validation.pattern') : undefined}
            onChange={(e) => p.setParentId(e.target.value)}
          />
        ) : null}
        {def.partition ? (
          <>
            <TextInput
              label={t('adm.records.from')}
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
            <TextInput
              label={t('adm.records.to')}
              type="date"
              value={to}
              hint={t('adm.records.rangeHint', { days: MAX_RANGE_DAYS })}
              error={rangeOk ? undefined : t('adm.records.rangeInvalid', { days: MAX_RANGE_DAYS })}
              onChange={(e) => setTo(e.target.value)}
            />
          </>
        ) : null}
        {searchField ? (
          <TextInput
            label={t('adm.records.search')}
            type="search"
            value={p.search}
            maxLength={100}
            onChange={(e) => p.setSearch(e.target.value)}
          />
        ) : null}
        {p.notice ? <Banner intent="success">{p.notice}</Banner> : null}
        {rows.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void rows.refetch()}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {problemText(t, rows.error)}
          </Banner>
        ) : parentOk && rangeOk ? (
          <DataTable
            caption={t('common.list.count', { count: items.length })}
            columns={columns}
            rows={items}
            rowKey={(row) => row.id}
            loading={rows.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('common.state.empty')}
            onRowClick={(row) => p.setEditing(row)}
          />
        ) : null}
        {rows.hasNextPage ? (
          <Button variant="ghost" loading={rows.isFetchingNextPage} onClick={() => void rows.fetchNextPage()}>
            {t('common.list.more')}
          </Button>
        ) : null}
      </Card>
      {p.creating ? (
        <RecordDialog
          def={def}
          parentId={p.parentId.trim()}
          presets={p.presets}
          onClose={() => p.setCreating(false)}
          onDone={done}
        />
      ) : null}
      {p.editing ? (
        <RecordDialog
          def={def}
          parentId={p.parentId.trim()}
          row={p.editing}
          canUpdate={may('Update')}
          canDelete={may('Delete')}
          actions={(def.actions ?? []).filter((a) => hasPermission(permissions, a.permission))}
          onClose={() => p.setEditing(null)}
          onDone={done}
        />
      ) : null}
    </>
  );
}

function initialValues(
  def: ResourceDef,
  row: RecordRow | undefined,
  presets: Readonly<Record<string, unknown>> = {},
): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const f of def.fields)
    out[f.name] = row?.[f.name] ?? presets[f.name] ?? (f.kind === 'boolean' ? (f.defaultValue ?? false) : '');
  return out;
}

/** The editor of one record: its fields, its state steps the role may use and the row allows, and delete. */
export function RecordDialog({
  def,
  parentId,
  row,
  presets,
  canUpdate = false,
  canDelete = false,
  actions = [],
  onClose,
  onDone,
}: {
  def: ResourceDef;
  parentId: string;
  row?: RecordRow;
  presets?: Readonly<Record<string, unknown>> | undefined;
  canUpdate?: boolean;
  canDelete?: boolean;
  /** The state transitions the caller holds the permission for; offered only where the row's state allows. */
  actions?: readonly ActionDef[];
  onClose: () => void;
  /** After a save, a step or delete (`deleted`: the record is gone). */
  onDone: (message: string, deleted?: boolean) => void;
}) {
  const { t } = useI18n();
  const fresh = useRecord(def, row ? row.id : null, parentId || undefined);
  const current = fresh.data?.data ?? row;
  const etag = fresh.data?.etag ?? (row?.etag as string | undefined) ?? null;
  const [values, setValues] = useState<Record<string, unknown>>(() => initialValues(def, row, presets));
  const [missing, setMissing] = useState<string[]>([]);
  const create = useCreateRecord(def);
  const update = useUpdateRecord(def);
  const remove = useDeleteRecord(def);
  const act = useRecordAction(def);
  const [acting, setActing] = useState<ActionDef | null>(null);
  const [reason, setReason] = useState('');
  const [params, setParams] = useState<Record<string, unknown>>({});
  const [confirming, setConfirming] = useState(false);
  const isNew = row === undefined;
  const available = !isNew && current ? actions.filter((a) => actionAllowed(a, current)) : [];
  const runAction = (action: ActionDef) => {
    if (!current) return;
    const absent = action.params
      .filter((f) => f.required && (params[f.name] === '' || params[f.name] === undefined))
      .map((f) => f.name);
    setMissing(absent);
    if (absent.length > 0) return;
    act.mutate(
      { action, id: current.id, etag, reason, params, parentId: parentId || undefined },
      { onSuccess: () => onDone(t('adm.records.actionDone', { action: action.label })) },
    );
  };
  const editable = (f: FieldDef) =>
    !f.readOnly &&
    (isNew
      ? f.name !== def.parent && !(presets && f.name in presets)
      : canUpdate && f.writable === true && f.name !== def.parent);
  const pending = create.isPending || update.isPending || remove.isPending || act.isPending;
  const error = create.error ?? update.error ?? remove.error ?? act.error;
  const submit = () => {
    const absent = def.fields
      .filter((f) => isNew && f.required && editable(f) && f.kind !== 'boolean')
      .filter((f) => values[f.name] === '' || values[f.name] === undefined)
      .map((f) => f.name);
    setMissing(absent);
    if (absent.length > 0) return;
    if (isNew) {
      create.mutate(
        { values, parentId: parentId || undefined },
        { onSuccess: () => onDone(t('adm.records.saved')) },
      );
    } else if (etag && current) {
      update.mutate(
        { id: current.id, etag, values, parentId: parentId || undefined },
        { onSuccess: () => onDone(t('adm.records.saved')) },
      );
    }
  };
  const formId = `record-${def.key.replace(/\W/g, '-')}`;
  const showSave = isNew || canUpdate;
  return (
    <Dialog
      open
      title={isNew ? t('adm.records.new') : canUpdate ? t('adm.records.edit') : t('adm.records.view')}
      onClose={onClose}
      footer={
        <>
          {!isNew && canDelete && etag && current ? (
            confirming ? (
              <Button
                variant="danger"
                loading={remove.isPending}
                onClick={() =>
                  remove.mutate(
                    { id: current.id, etag, parentId: parentId || undefined },
                    { onSuccess: () => onDone(t('adm.records.deleted'), true) },
                  )
                }
              >
                {t('adm.records.confirmDelete')}
              </Button>
            ) : (
              <Button variant="secondary" onClick={() => setConfirming(true)}>
                {t('common.action.delete')}
              </Button>
            )
          ) : null}
          <Button variant="secondary" onClick={onClose}>
            {showSave ? t('common.action.cancel') : t('common.action.close')}
          </Button>
          {showSave ? (
            <Button type="submit" form={formId} loading={pending} disabled={!isNew && !etag}>
              {t('common.action.save')}
            </Button>
          ) : null}
        </>
      }
    >
      <form
        id={formId}
        className="form-grid"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        {error ? <Banner intent="danger">{problemText(t, error)}</Banner> : null}
        {!isNew && current ? (
          <TextInput label={t('adm.records.id')} value={current.id} readOnly onChange={() => undefined} />
        ) : null}
        {available.length > 0 ? (
          <fieldset className="record-actions">
            <legend>{t('adm.records.actions')}</legend>
            <div className="record-actions__buttons">
              {available.map((action) => (
                <Button
                  key={action.operation}
                  type="button"
                  variant={acting?.operation === action.operation ? 'primary' : 'secondary'}
                  onClick={() => {
                    setActing(action);
                    setReason('');
                    setParams({});
                    setMissing([]);
                  }}
                >
                  {action.label}
                </Button>
              ))}
            </div>
            {acting ? (
              <>
                {acting.reason ? (
                  <TextInput
                    label={t('adm.records.reason')}
                    value={reason}
                    maxLength={500}
                    onChange={(e) => setReason(e.target.value)}
                  />
                ) : null}
                {acting.params.map((field) => (
                  <FieldInput
                    key={field.name}
                    field={field}
                    value={params[field.name]}
                    disabled={false}
                    error={missing.includes(field.name) ? t('common.validation.required') : undefined}
                    onChange={(v) => setParams((old) => ({ ...old, [field.name]: v }))}
                  />
                ))}
                <Button type="button" loading={act.isPending} onClick={() => runAction(acting)}>
                  {t('adm.records.confirmAction', { action: acting.label })}
                </Button>
              </>
            ) : null}
          </fieldset>
        ) : null}
        {def.fields.map((field) => (
          <FieldInput
            key={field.name}
            field={field}
            value={values[field.name]}
            disabled={!editable(field)}
            error={missing.includes(field.name) ? t('common.validation.required') : undefined}
            onChange={(v) => setValues((old) => ({ ...old, [field.name]: v }))}
          />
        ))}
      </form>
    </Dialog>
  );
}

function FieldInput({
  field,
  value,
  disabled,
  error,
  onChange,
}: {
  field: FieldDef;
  value: unknown;
  disabled: boolean;
  error: string | undefined;
  onChange: (value: unknown) => void;
}) {
  const { t } = useI18n();
  if (field.kind === 'boolean') {
    return (
      <Switch
        label={field.label}
        checked={value === true}
        disabled={disabled}
        onChange={(checked) => onChange(checked)}
      />
    );
  }
  if (field.kind === 'enum' && field.values) {
    return (
      <SelectField
        label={field.label}
        value={String(value ?? '')}
        disabled={disabled}
        error={error}
        onChange={(e) => onChange(e.target.value)}
        options={[
          { value: '', label: '—' },
          ...field.values.map((v, i) => ({ value: v, label: field.valueLabels?.[i] ?? v })),
        ]}
      />
    );
  }
  const type =
    field.kind === 'date'
      ? 'date'
      : field.kind === 'time'
        ? 'time'
        : field.kind === 'datetime'
          ? 'text'
          : 'text';
  return (
    <TextInput
      label={field.label}
      type={field.writeOnly ? 'password' : type}
      autoComplete={field.writeOnly ? 'new-password' : undefined}
      value={String(value ?? '')}
      disabled={disabled}
      error={error}
      maxLength={field.maxLength}
      inputMode={field.kind === 'integer' || field.kind === 'decimal' ? 'decimal' : undefined}
      hint={
        field.writeOnly
          ? t('adm.records.secretHint')
          : field.kind === 'datetime'
            ? '2026-01-31T09:00:00Z'
            : field.kind === 'id'
              ? t('adm.records.id')
              : undefined
      }
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
