/**
 * The generic record building blocks of the native screens: a record's fields, its editor and delete, its state
 * transitions, and the loading, error and offline states of a query or mutation.
 */
import {
  actionAllowed,
  can,
  useCreateRecord,
  useDeleteRecord,
  useRecordAction,
  useUpdateRecord,
  isApiError,
  useI18n,
  useSession,
  type PermissionCode,
  type RecordRow,
  type ResourceDef,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, ChoiceGroup, SwitchRow, Text, TextInput, useTheme } from '@mysms/ui-native';
import { useState } from 'react';
import { View } from 'react-native';
import { actionLabelKey, textAreaHeight } from './recordText';

export function RecordFields({ row, def }: { row: RecordRow; def: ResourceDef }) {
  const { t } = useI18n();
  return (
    <View>
      {def.fields
        .filter((field) => !field.writeOnly && field.name in row && field.kind !== 'id')
        .map((field) => {
          const value = row[field.name];
          if (value === null || value === undefined || typeof value === 'object') return null;
          const display =
            field.kind === 'enum'
              ? (field.valueLabels?.[field.values?.indexOf(String(value)) ?? -1] ??
                enumLabel(t, `${def.key}.${field.name}`, String(value)))
              : field.name === 'status' && def.module === 'assessment'
                ? enumLabel(t, 'assessment.status', String(value))
                : String(value);
          return (
            <Text key={field.name}>
              {field.label}: {display}
            </Text>
          );
        })}
    </View>
  );
}

export function WorkflowActions({
  def,
  row,
  etag,
  onReload,
}: {
  def: ResourceDef;
  row: RecordRow;
  etag: string | null;
  onReload: () => void;
}) {
  const { t } = useI18n();
  const permissions = useSession((state) => state.me?.permissions);
  const theme = useTheme();
  const actionMutation = useRecordAction(def);
  const [reason, setReason] = useState('');
  const available = (def.actions ?? []).flatMap((action) => {
    const label = actionLabelKey(action.operation);
    return label && can(permissions, action.permission as PermissionCode) && actionAllowed(action, row)
      ? [{ action, label }]
      : [];
  });
  const conflict = isApiError(actionMutation.error) && actionMutation.error.isConflict;
  return (
    <View style={{ marginTop: theme.space[5], flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[3] }}>
      {available.some(({ action }) => action.reason) ? (
        <TextInput
          label={t('adm.records.reason')}
          value={reason}
          onChangeText={setReason}
          multiline
          style={{ minHeight: textAreaHeight(theme, 3), width: '100%' }}
        />
      ) : null}
      {available.map(({ action, label }) => (
        <Button
          key={action.operation}
          variant="secondary"
          loading={actionMutation.isPending}
          disabled={action.ifMatch && !etag}
          onPress={() =>
            actionMutation.mutate(
              {
                action,
                id: row.id,
                etag,
                ...(action.reason && reason.trim() ? { reason: reason.trim() } : {}),
              },
              { onSuccess: () => setReason('') },
            )
          }
        >
          {t(label)}
        </Button>
      ))}
      {actionMutation.isError ? (
        <>
          <Banner intent="danger">
            {conflict
              ? t('common.conflict.body')
              : isApiError(actionMutation.error)
                ? actionMutation.error.title
                : t('common.error.generic')}
          </Banner>
          <Button
            variant="secondary"
            onPress={() => {
              actionMutation.reset();
              onReload();
            }}
          >
            {conflict ? t('common.conflict.reload') : t('common.action.retry')}
          </Button>
        </>
      ) : null}
    </View>
  );
}

export function QueryFeedback({
  query,
  hasCachedRows,
  onRetry,
}: {
  query: { isPending: boolean; isError: boolean; error: unknown };
  hasCachedRows: boolean;
  onRetry: () => void;
}) {
  const { t } = useI18n();
  if (!query.isError) return null;
  const offline = isNetworkFailure(query.error);
  const error = query.error;
  return (
    <>
      {offline ? (
        <Banner>{hasCachedRows ? t('common.state.offline') : t('common.error.generic')}</Banner>
      ) : null}
      {!offline ? (
        <Banner intent="danger">
          {isApiError(error) ? `${error.title} (${error.code})` : t('common.error.generic')}
        </Banner>
      ) : null}
      <Button variant="secondary" onPress={onRetry}>
        {t('common.action.retry')}
      </Button>
    </>
  );
}

export function WorkflowNotice({ mutation }: { mutation: MutationFeedback }) {
  const { t } = useI18n();
  if (!mutation.isError) return null;
  const conflict = isApiError(mutation.error) && mutation.error.isConflict;
  return (
    <View>
      <Banner intent="danger">
        {conflict
          ? t('common.conflict.body')
          : isApiError(mutation.error)
            ? `${mutation.error.title} (${mutation.error.code})`
            : t('common.error.generic')}
      </Banner>
      <Button variant="secondary" onPress={mutation.reset}>
        {t('common.action.close')}
      </Button>
    </View>
  );
}

interface MutationFeedback {
  isError: boolean;
  isPending: boolean;
  error: unknown;
  reset: () => void;
}

export function RecordEditor({
  def,
  row,
  etag,
  initialValues,
  tablet = false,
  onDone,
  onCancel,
}: {
  def: ResourceDef;
  row?: RecordRow;
  etag?: string | null;
  initialValues?: Record<string, unknown>;
  tablet?: boolean;
  onDone: () => void;
  onCancel: () => void;
}) {
  const { t } = useI18n();
  const theme = useTheme();
  const create = useCreateRecord(def);
  const update = useUpdateRecord(def);
  const [values, setValues] = useState<Record<string, unknown>>(() =>
    Object.fromEntries(
      def.fields.map((field) => [
        field.name,
        row?.[field.name] ??
          initialValues?.[field.name] ??
          (field.kind === 'boolean' ? (field.defaultValue ?? false) : ''),
      ]),
    ),
  );
  const [missing, setMissing] = useState<string[]>([]);
  const pending = create.isPending || update.isPending;
  const error = create.error ?? update.error;
  const isNew = !row;
  const editable = (field: ResourceDef['fields'][number]) =>
    !field.readOnly &&
    (isNew ? field.name !== def.parent : field.writable === true && field.name !== def.parent);
  const save = () => {
    const absent = def.fields
      .filter((field) => isNew && field.required && editable(field) && field.kind !== 'boolean')
      .filter((field) => values[field.name] === '' || values[field.name] === undefined)
      .map((field) => field.name);
    setMissing(absent);
    if (absent.length) return;
    if (isNew) create.mutate({ values }, { onSuccess: onDone });
    else if (row && etag) update.mutate({ id: row.id, etag, values }, { onSuccess: onDone });
  };
  return (
    <View
      style={{
        padding: theme.space[5],
        marginTop: theme.space[4],
        borderWidth: 1,
        borderColor: theme.colors.border,
        borderRadius: theme.radius.md,
      }}
    >
      <Text variant="h3">{t(isNew ? 'adm.records.new' : 'adm.records.edit')}</Text>
      {error ? (
        <Banner intent="danger">
          {isApiError(error) && error.isConflict
            ? t('common.conflict.body')
            : isApiError(error)
              ? `${error.title} (${error.code})`
              : t('common.error.generic')}
        </Banner>
      ) : null}
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', columnGap: theme.space[4] }}>
        {def.fields.filter(editable).map((field) => {
          const fieldError = missing.includes(field.name) ? t('common.validation.required') : undefined;
          const width = tablet ? '48%' : '100%';
          if (field.kind === 'boolean')
            return (
              <View key={field.name} style={{ width }}>
                <SwitchRow
                  label={field.label}
                  value={values[field.name] === true}
                  onChange={(value) => setValues((old) => ({ ...old, [field.name]: value }))}
                />
              </View>
            );
          if (field.kind === 'enum' && field.values)
            return (
              <View key={field.name} style={{ width }}>
                <ChoiceGroup
                  label={field.label}
                  value={String(values[field.name] ?? '')}
                  choices={[
                    { value: '', label: t('common.state.notAvailable') },
                    ...field.values.map((value, index) => ({
                      value,
                      label: field.valueLabels?.[index] ?? enumLabel(t, `${def.key}.${field.name}`, value),
                    })),
                  ]}
                  onChange={(value) => setValues((old) => ({ ...old, [field.name]: value }))}
                />
                {fieldError ? <Text variant="caption">{fieldError}</Text> : null}
              </View>
            );
          return (
            <View key={field.name} style={{ width }}>
              <TextInput
                label={field.label}
                value={String(values[field.name] ?? '')}
                onChangeText={(value) => setValues((old) => ({ ...old, [field.name]: value }))}
                editable={!field.readOnly}
                maxLength={field.maxLength}
                keyboardType={
                  field.kind === 'integer' || field.kind === 'decimal' ? 'decimal-pad' : 'default'
                }
                autoCapitalize="none"
                error={fieldError}
              />
            </View>
          );
        })}
      </View>
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[3] }}>
        <Button loading={pending} disabled={!isNew && !etag} onPress={save}>
          {t('common.action.save')}
        </Button>
        <Button variant="secondary" onPress={onCancel}>
          {t('common.action.cancel')}
        </Button>
      </View>
    </View>
  );
}

export function RecordDelete({
  def,
  row,
  etag,
  onDeleted,
}: {
  def: ResourceDef;
  row: RecordRow;
  etag: string | null;
  onDeleted: () => void;
}) {
  const { t } = useI18n();
  const remove = useDeleteRecord(def);
  const [confirm, setConfirm] = useState(false);
  return (
    <View>
      {!confirm ? (
        <Button variant="secondary" onPress={() => setConfirm(true)}>
          {t('common.action.delete')}
        </Button>
      ) : (
        <>
          <Text>{t('adm.records.confirmDelete')}</Text>
          <Button
            loading={remove.isPending}
            disabled={!etag}
            onPress={() => remove.mutate({ id: row.id, etag: etag ?? '' }, { onSuccess: onDeleted })}
          >
            {t('common.action.delete')}
          </Button>
          <Button variant="secondary" onPress={() => setConfirm(false)}>
            {t('common.action.cancel')}
          </Button>
        </>
      )}
      <WorkflowNotice mutation={remove} />
    </View>
  );
}

export function LoadingRows({ tablet }: { tablet: boolean }) {
  const { t } = useI18n();
  const theme = useTheme();
  return (
    <View
      accessibilityRole="progressbar"
      accessibilityLabel={t('common.state.loading')}
      style={{ flexDirection: 'row', flexWrap: 'wrap', gap: theme.space[4], marginTop: theme.space[5] }}
    >
      {Array.from({ length: tablet ? 4 : 2 }, (_, index) => (
        <View
          key={index}
          style={{
            // a card's heading and two (tablet: three) lines of text, inside its padding
            height:
              theme.text.h3.lineHeight + theme.text.body.lineHeight * (tablet ? 3 : 2) + theme.space[5] * 2,
            width: tablet ? '48%' : '100%',
            borderRadius: theme.radius.md,
            backgroundColor: theme.colors.surface2,
          }}
        />
      ))}
    </View>
  );
}

function isNetworkFailure(error: unknown): boolean {
  return error instanceof TypeError;
}
