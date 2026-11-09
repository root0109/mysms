import { zodResolver } from '@hookform/resolvers/zod';
import {
  isApiError,
  PermissionGate,
  useCan,
  useCountry,
  useCreateCountry,
  useDeleteCountry,
  useI18n,
  useInfiniteCountries,
  useUpdateCountry,
  type Country,
  type CountryUpdate,
} from '@mysms/core';
import { enumLabel } from '@mysms/i18n';
import { Banner, Button, Card, DataTable, Dialog, StatusChip, TextInput, type Column } from '@mysms/ui-web';
import { fieldMessage } from '@mysms/web-kit';
import { useState } from 'react';
import { useForm, type FieldValues, type Path, type UseFormReturn } from 'react-hook-form';
import { z } from 'zod';

const REQUIRED = 'common.validation.required';
const MAX = 'common.validation.maxLength';
const PATTERN = 'common.validation.pattern';
const LIMITS = { name: 120, shortName: 60, phonePrefix: 8 } as const;

const editSchema = z.object({
  name: z.string().trim().min(1, REQUIRED).max(LIMITS.name, MAX),
  shortName: z.string().trim().max(LIMITS.shortName, MAX),
  phonePrefix: z.union([
    z.literal(''),
    z
      .string()
      .trim()
      .regex(/^\+\d{1,7}$/, PATTERN),
  ]),
});
const createSchema = editSchema.extend({
  iso2: z
    .string()
    .trim()
    .regex(/^[A-Z]{2}$/, PATTERN),
  iso3: z.union([
    z.literal(''),
    z
      .string()
      .trim()
      .regex(/^[A-Z]{3}$/, PATTERN),
  ]),
});
type EditValues = z.infer<typeof editSchema>;
type CreateValues = z.infer<typeof createSchema>;

/** Server-side 422 field errors land on the matching inputs (ui/05 §4). */
function applyServerErrors<T extends FieldValues>(form: UseFormReturn<T>, error: unknown): boolean {
  if (!isApiError(error) || error.status !== 422 || error.errors.length === 0) return false;
  for (const [field, message] of Object.entries(error.fieldErrors()))
    form.setError(field as Path<T>, { message });
  return true;
}

/** PLT-07 Master data — countries: paged list, create, edit under If-Match, delete. */
export function CountriesScreen() {
  const { t } = useI18n();
  const list = useInfiniteCountries({ pageSize: 25 });
  const [editing, setEditing] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const rows = list.data?.pages.flatMap((p) => p.items) ?? [];
  const columns: Column<Country>[] = [
    { key: 'iso2', header: t('master.countries.iso2'), render: (c) => c.iso2 },
    { key: 'iso3', header: t('master.countries.iso3'), render: (c) => c.iso3 },
    { key: 'name', header: t('master.countries.name'), render: (c) => c.name },
    { key: 'phonePrefix', header: t('master.countries.phonePrefix'), render: (c) => c.phonePrefix },
    {
      key: 'active',
      header: t('master.countries.active'),
      render: (c) => (
        <StatusChip
          intent={c.isActive ? 'success' : 'neutral'}
          label={enumLabel(t, 'status', c.isActive ? 'active' : 'inactive')}
        />
      ),
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
        title={t('master.countries.title')}
        actions={
          <PermissionGate code="master.country.create">
            <Button onClick={() => setCreating(true)}>{t('master.countries.new')}</Button>
          </PermissionGate>
        }
      >
        {notice ? <Banner intent="success">{notice}</Banner> : null}
        {list.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void list.refetch()}>
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
            rowKey={(c) => c.id ?? c.iso2 ?? ''}
            loading={list.isPending}
            loadingLabel={t('common.state.loading')}
            emptyText={t('common.state.empty')}
            onRowClick={(c) => c.id && setEditing(c.id)}
          />
        )}
        {list.hasNextPage ? (
          <Button variant="ghost" loading={list.isFetchingNextPage} onClick={() => void list.fetchNextPage()}>
            {t('common.list.more')}
          </Button>
        ) : null}
      </Card>
      {editing ? (
        <EditCountryDialog countryId={editing} onClose={() => setEditing(null)} onDone={done} />
      ) : null}
      {creating ? <CreateCountryDialog onClose={() => setCreating(false)} onDone={done} /> : null}
    </>
  );
}

function EditCountryDialog({
  countryId,
  onClose,
  onDone,
}: {
  countryId: string;
  onClose: () => void;
  onDone: (message: string) => void;
}) {
  const { t } = useI18n();
  const country = useCountry(countryId);
  const update = useUpdateCountry();
  const remove = useDeleteCountry();
  const canDelete = useCan('master.country.delete');
  const [conflict, setConflict] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const current = country.data?.data;
  const etag = country.data?.etag ?? null;
  const form = useForm<EditValues>({
    resolver: zodResolver(editSchema),
    values: current
      ? {
          name: current.name ?? '',
          shortName: current.shortName ?? '',
          phonePrefix: current.phonePrefix ?? '',
        }
      : undefined,
  });
  const onConflictOrError = (error: unknown) => {
    if (isApiError(error) && error.isConflict) setConflict(true);
    else applyServerErrors(form, error);
  };
  const save = form.handleSubmit((values) => {
    if (!etag) return;
    const dirty = Object.keys(form.formState.dirtyFields) as (keyof EditValues)[];
    if (dirty.length === 0) return onClose();
    const changes: CountryUpdate = Object.fromEntries(dirty.map((k) => [k, values[k]]));
    update.mutate(
      { countryId, etag, changes },
      {
        onSuccess: (r) => onDone(t('master.countries.saved', { name: r.data.name ?? '' })),
        onError: onConflictOrError,
      },
    );
  });
  const reload = async () => {
    setConflict(false);
    await country.refetch();
  };
  const name = current?.name ?? '';
  return (
    <Dialog
      open
      title={t('master.countries.edit.title', { name })}
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
          <Button type="submit" form="country-edit" loading={update.isPending} disabled={!etag || conflict}>
            {t('common.action.save')}
          </Button>
        </>
      }
    >
      {conflict ? (
        <Banner
          intent="warning"
          title={t('common.conflict.title')}
          action={
            <Button variant="secondary" onClick={() => void reload()}>
              {t('common.conflict.reload')}
            </Button>
          }
        >
          {t('common.conflict.body')}
        </Banner>
      ) : null}
      {confirmDelete && etag ? (
        <Banner
          intent="danger"
          action={
            <Button
              variant="danger"
              loading={remove.isPending}
              onClick={() =>
                remove.mutate(
                  { countryId, etag },
                  {
                    onSuccess: () => onDone(t('master.countries.deleted', { name })),
                    onError: onConflictOrError,
                  },
                )
              }
            >
              {t('common.action.delete')}
            </Button>
          }
        >
          {t('master.countries.delete.confirm', { name })}
        </Banner>
      ) : null}
      <form id="country-edit" noValidate onSubmit={save}>
        <TextInput
          label={t('master.countries.name')}
          error={fieldMessage(t, form.formState.errors.name, LIMITS.name)}
          {...form.register('name')}
        />
        <TextInput
          label={t('master.countries.shortName')}
          error={fieldMessage(t, form.formState.errors.shortName, LIMITS.shortName)}
          {...form.register('shortName')}
        />
        <TextInput
          label={t('master.countries.phonePrefix')}
          error={fieldMessage(t, form.formState.errors.phonePrefix, LIMITS.phonePrefix)}
          {...form.register('phonePrefix')}
        />
      </form>
    </Dialog>
  );
}

function CreateCountryDialog({
  onClose,
  onDone,
}: {
  onClose: () => void;
  onDone: (message: string) => void;
}) {
  const { t } = useI18n();
  const create = useCreateCountry();
  const form = useForm<CreateValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { iso2: '', iso3: '', name: '', shortName: '', phonePrefix: '' },
  });
  const submit = form.handleSubmit((v) => {
    const body: Country = {
      iso2: v.iso2,
      name: v.name,
      ...(v.iso3 ? { iso3: v.iso3 } : {}),
      ...(v.shortName ? { shortName: v.shortName } : {}),
      ...(v.phonePrefix ? { phonePrefix: v.phonePrefix } : {}),
    };
    create.mutate(body, {
      onSuccess: (r) => onDone(t('master.countries.saved', { name: r.data.name ?? v.name })),
      onError: (e) => applyServerErrors(form, e),
    });
  });
  const failed = create.isError && !(isApiError(create.error) && create.error.status === 422);
  return (
    <Dialog
      open
      title={t('master.countries.new')}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button type="submit" form="country-create" loading={create.isPending}>
            {t('common.action.save')}
          </Button>
        </>
      }
    >
      {failed ? <Banner intent="danger">{t('common.error.generic')}</Banner> : null}
      <form id="country-create" noValidate onSubmit={submit}>
        <TextInput
          label={t('master.countries.iso2')}
          maxLength={2}
          error={fieldMessage(t, form.formState.errors.iso2)}
          {...form.register('iso2')}
        />
        <TextInput
          label={t('master.countries.iso3')}
          maxLength={3}
          error={fieldMessage(t, form.formState.errors.iso3)}
          {...form.register('iso3')}
        />
        <TextInput
          label={t('master.countries.name')}
          error={fieldMessage(t, form.formState.errors.name, LIMITS.name)}
          {...form.register('name')}
        />
        <TextInput
          label={t('master.countries.shortName')}
          error={fieldMessage(t, form.formState.errors.shortName, LIMITS.shortName)}
          {...form.register('shortName')}
        />
        <TextInput
          label={t('master.countries.phonePrefix')}
          error={fieldMessage(t, form.formState.errors.phonePrefix, LIMITS.phonePrefix)}
          {...form.register('phonePrefix')}
        />
      </form>
    </Dialog>
  );
}
