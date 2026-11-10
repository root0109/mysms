import {
  isApiError,
  PermissionGate,
  settingLabel,
  settingScopeLabel,
  settingValueLabel,
  useCan,
  useCreateSettingValue,
  useDeleteSettingValue,
  useI18n,
  useSettingDefinitions,
  useInfiniteSettingValues,
  useUpdateSettingValue,
  type SettingDefinition,
  type SettingValue,
  type SettingValueCreate,
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
import { useState } from 'react';
import { UUID } from './ids';

/** The value control for one definition: a list for choices and booleans, text otherwise. */
function ValueField({
  definition,
  value,
  onChange,
  error,
}: {
  definition: SettingDefinition | undefined;
  value: string;
  onChange: (value: string) => void;
  error: string | undefined;
}) {
  const { t } = useI18n();
  const label = t('adm.settings.value');
  const choices =
    definition?.valueType === 'choice'
      ? (definition.allowedValues ?? [])
      : definition?.valueType === 'boolean'
        ? ['true', 'false']
        : null;
  if (choices) {
    return (
      <SelectField
        label={label}
        error={error}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        options={choices.map((v) => ({ value: v, label: settingValueLabel(t, v) }))}
      />
    );
  }
  const range =
    definition?.valueType === 'integer' &&
    definition.minValue !== undefined &&
    definition.maxValue !== undefined
      ? `${definition.minValue}–${definition.maxValue}`
      : undefined;
  return (
    <TextInput
      label={label}
      hint={range ?? definition?.description}
      error={error}
      value={value}
      inputMode={definition?.valueType === 'integer' ? 'numeric' : undefined}
      maxLength={500}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

const problemText = (t: ReturnType<typeof useI18n>['t'], error: unknown): string | undefined =>
  error
    ? isApiError(error) && error.status === 422
      ? error.errors.map((e) => e.message).join(' ') || t('common.validation.pattern')
      : isApiError(error) && error.status === 403
        ? t('adm.settings.forbidden')
        : isApiError(error) && error.code === 'DUPLICATE'
          ? (error.detail ?? t('common.error.generic'))
          : t('common.error.generic')
    : undefined;

/**
 * ADM-16 Settings by scope: the values administrators set for the whole school, an institution, a department, a
 * class section or one user, with locks (ui/04 §9). The server lists and accepts only the scopes of the caller's
 * own grants.
 */
export function SettingValuesScreen() {
  const { t } = useI18n();
  const values = useInfiniteSettingValues({ size: 100 });
  const definitions = useSettingDefinitions();
  const [editing, setEditing] = useState<SettingValue | null>(null);
  const [creating, setCreating] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const canUpdate = useCan('iam.setting_value.update');
  const byKey = new Map((definitions.data?.items ?? []).map((d) => [d.settingKey, d]));
  const rows = values.data?.pages.flatMap((p) => p.data) ?? [];
  const columns: Column<SettingValue>[] = [
    {
      key: 'setting',
      header: t('adm.settings.setting'),
      render: (v) => settingLabel(t, v.settingKey, byKey.get(v.settingKey)?.description),
    },
    {
      key: 'scope',
      header: t('adm.settings.scope'),
      render: (v) => `${settingScopeLabel(t, v.scopeType)}${v.scopeId ? ` · ${v.scopeId.slice(0, 8)}` : ''}`,
    },
    {
      key: 'value',
      header: t('adm.settings.value'),
      render: (v) => (v.settingKey === 'ui.textScale' ? `${v.value} %` : settingValueLabel(t, v.value)),
    },
    {
      key: 'locked',
      header: t('adm.settings.locked'),
      render: (v) => (v.isLocked ? <StatusChip intent="warning" label={t('adm.settings.locked')} /> : null),
    },
  ];
  const done = (message: string) => {
    setNotice(message);
    setEditing(null);
    setCreating(false);
  };
  return (
    <>
      <Card
        title={t('adm.settings.title')}
        actions={
          <PermissionGate code="iam.setting_value.create">
            <Button onClick={() => setCreating(true)} disabled={!definitions.data}>
              {t('adm.settings.new')}
            </Button>
          </PermissionGate>
        }
      >
        <p className="hint">{t('adm.settings.intro')}</p>
        {notice ? <Banner intent="success">{notice}</Banner> : null}
        {values.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void values.refetch()}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {t('common.error.generic')}
          </Banner>
        ) : (
          <DataTable
            caption={t('common.list.count', { count: rows.length })}
            columns={columns}
            rows={rows}
            rowKey={(v) => v.id ?? `${v.scopeType}-${v.scopeId ?? ''}-${v.settingKey}`}
            loading={values.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('common.state.empty')}
            onRowClick={canUpdate ? (v) => setEditing(v) : undefined}
          />
        )}
        {values.hasNextPage ? (
          <Button
            variant="ghost"
            loading={values.isFetchingNextPage}
            onClick={() => void values.fetchNextPage()}
          >
            {t('common.list.more')}
          </Button>
        ) : null}
      </Card>
      {creating && definitions.data ? (
        <CreateValueDialog
          definitions={definitions.data.items}
          onClose={() => setCreating(false)}
          onDone={done}
        />
      ) : null}
      {editing ? (
        <EditValueDialog
          value={editing}
          definition={byKey.get(editing.settingKey)}
          onClose={() => setEditing(null)}
          onDone={done}
        />
      ) : null}
    </>
  );
}

function CreateValueDialog({
  definitions,
  onClose,
  onDone,
}: {
  definitions: readonly SettingDefinition[];
  onClose: () => void;
  onDone: (message: string) => void;
}) {
  const { t } = useI18n();
  const create = useCreateSettingValue();
  const [settingKey, setSettingKey] = useState(definitions[0]?.settingKey ?? '');
  const definition = definitions.find((d) => d.settingKey === settingKey);
  const scopes = definition?.allowedScopes ?? [];
  const [scopeType, setScopeType] = useState<string>(scopes[0] ?? 'tenant');
  const [scopeId, setScopeId] = useState('');
  const [value, setValue] = useState(definition?.defaultValue ?? '');
  const [locked, setLocked] = useState(false);
  const [scopeIdError, setScopeIdError] = useState<string | undefined>();
  const pick = (key: string) => {
    const next = definitions.find((d) => d.settingKey === key);
    setSettingKey(key);
    setValue(next?.defaultValue ?? '');
    if (next && !(next.allowedScopes ?? []).includes(scopeType as never))
      setScopeType(next.allowedScopes?.[0] ?? 'tenant');
  };
  const submit = () => {
    const needsUnit = scopeType !== 'tenant';
    if (needsUnit && !UUID.test(scopeId.trim())) {
      setScopeIdError(t('common.validation.pattern'));
      return;
    }
    setScopeIdError(undefined);
    const body = {
      scopeType,
      settingKey,
      value,
      isLocked: scopeType === 'user' ? false : locked,
      ...(needsUnit ? { scopeId: scopeId.trim() } : {}),
    } as SettingValueCreate;
    create.mutate(body, { onSuccess: () => onDone(t('adm.settings.saved')) });
  };
  return (
    <Dialog
      open
      title={t('adm.settings.new')}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button type="submit" form="setting-value-new" loading={create.isPending}>
            {t('common.action.save')}
          </Button>
        </>
      }
    >
      {create.error && !(isApiError(create.error) && create.error.status === 422) ? (
        <Banner intent="danger">{problemText(t, create.error)}</Banner>
      ) : null}
      <form
        id="setting-value-new"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <SelectField
          label={t('adm.settings.setting')}
          value={settingKey}
          onChange={(e) => pick(e.target.value)}
          options={definitions.map((d) => ({
            value: d.settingKey,
            label: settingLabel(t, d.settingKey, d.description),
          }))}
        />
        <SelectField
          label={t('adm.settings.scope')}
          value={scopeType}
          onChange={(e) => setScopeType(e.target.value)}
          options={scopes.map((s) => ({ value: s, label: settingScopeLabel(t, s) }))}
        />
        {scopeType !== 'tenant' ? (
          <TextInput
            label={t('adm.settings.scopeId')}
            hint={t('adm.settings.scopeIdHint')}
            error={scopeIdError}
            value={scopeId}
            spellCheck={false}
            onChange={(e) => setScopeId(e.target.value)}
          />
        ) : null}
        <ValueField
          definition={definition}
          value={value}
          onChange={setValue}
          error={
            isApiError(create.error) && create.error.status === 422 ? problemText(t, create.error) : undefined
          }
        />
        {scopeType !== 'user' ? (
          <Switch
            label={t('adm.settings.locked')}
            hint={t('adm.settings.lockHint')}
            checked={locked}
            onChange={setLocked}
          />
        ) : null}
      </form>
    </Dialog>
  );
}

function EditValueDialog({
  value,
  definition,
  onClose,
  onDone,
}: {
  value: SettingValue;
  definition: SettingDefinition | undefined;
  onClose: () => void;
  onDone: (message: string) => void;
}) {
  const { t } = useI18n();
  const update = useUpdateSettingValue();
  const remove = useDeleteSettingValue();
  const canDelete = useCan('iam.setting_value.delete');
  const [draft, setDraft] = useState(value.value);
  const [locked, setLocked] = useState(value.isLocked === true);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const etag = value.etag ?? null;
  const conflict = [update.error, remove.error].some((e) => isApiError(e) && e.isConflict);
  const failure = [update.error, remove.error].find(
    (e) => e && !(isApiError(e) && (e.isConflict || e.status === 422)),
  );
  const label = settingLabel(t, value.settingKey, definition?.description);
  const save = () => {
    if (!etag || !value.id) return;
    const changes = {
      ...(draft !== value.value ? { value: draft } : {}),
      ...(locked !== (value.isLocked === true) ? { isLocked: locked } : {}),
    };
    if (Object.keys(changes).length === 0) return onClose();
    update.mutate(
      { settingValueId: value.id, etag, changes },
      { onSuccess: () => onDone(t('adm.settings.saved')) },
    );
  };
  return (
    <Dialog
      open
      title={t('adm.settings.edit.title', { setting: label })}
      onClose={onClose}
      footer={
        <>
          {canDelete && !confirmDelete ? (
            <Button variant="ghost" onClick={() => setConfirmDelete(true)} disabled={!etag}>
              {t('common.action.delete')}
            </Button>
          ) : null}
          <Button variant="secondary" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button
            type="submit"
            form="setting-value-edit"
            loading={update.isPending}
            disabled={!etag || conflict}
          >
            {t('common.action.save')}
          </Button>
        </>
      }
    >
      {conflict ? (
        <Banner intent="warning" title={t('common.conflict.title')}>
          {t('common.conflict.body')}
        </Banner>
      ) : null}
      {failure ? <Banner intent="danger">{problemText(t, failure)}</Banner> : null}
      {confirmDelete && etag && value.id ? (
        <Banner
          intent="danger"
          action={
            <Button
              variant="danger"
              loading={remove.isPending}
              onClick={() =>
                remove.mutate(
                  { settingValueId: value.id as string, etag },
                  { onSuccess: () => onDone(t('adm.settings.deleted')) },
                )
              }
            >
              {t('common.action.delete')}
            </Button>
          }
        >
          {t('adm.settings.delete.confirm')}
        </Banner>
      ) : null}
      <p className="hint">
        {settingScopeLabel(t, value.scopeType)}
        {value.scopeId ? ` · ${value.scopeId}` : ''}
      </p>
      <form
        id="setting-value-edit"
        noValidate
        onSubmit={(e) => {
          e.preventDefault();
          save();
        }}
      >
        <ValueField
          definition={definition}
          value={draft}
          onChange={setDraft}
          error={
            isApiError(update.error) && update.error.status === 422 ? problemText(t, update.error) : undefined
          }
        />
        {value.scopeType !== 'user' ? (
          <Switch
            label={t('adm.settings.locked')}
            hint={t('adm.settings.lockHint')}
            checked={locked}
            onChange={setLocked}
          />
        ) : null}
      </form>
    </Dialog>
  );
}
