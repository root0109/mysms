import {
  resourceDef,
  useAllRecords,
  useCan,
  useCreateRecord,
  useDeleteRecord,
  useI18n,
  usePermissionCatalogue,
  type Permission,
  type RecordRow,
} from '@mysms/core';
import { Banner, Button, Card, SelectField, Skeleton, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { ResourceScreen } from '../ResourceScreen';
import { RESOURCE_SCREENS } from '../resourceScreens';
import { problemText } from '../recordText';

const GRANTS = resourceDef('iam.RolePermission');

/**
 * The permissions of one role, granted and taken away one at a time (iam.CreateRolePermission /
 * DeleteRolePermission under If-Match), grouped by module and searchable by code or name. A system role and a
 * permission the platform marks not modifiable are shown read-only; so is everything for a role without the write
 * permissions.
 */
function PermissionEditor({ role }: { role: RecordRow }) {
  const { t } = useI18n();
  const grants = GRANTS as NonNullable<typeof GRANTS>;
  const catalogue = usePermissionCatalogue();
  const held = useAllRecords('iam.RolePermission', { parentId: role.id });
  const grant = useCreateRecord(grants);
  const revoke = useDeleteRecord(grants);
  const mayGrant = useCan('iam.role_permission.create');
  const mayRevoke = useCan('iam.role_permission.delete');
  const [needle, setNeedle] = useState('');
  if (catalogue.isPending || held.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (catalogue.isError || held.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button
            variant="secondary"
            onClick={() => {
              void catalogue.refetch();
              void held.refetch();
            }}
          >
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const grantOf = new Map(held.data.items.map((row) => [String(row.permissionId), row]));
  const text = needle.trim().toLowerCase();
  const shown = catalogue.data.filter(
    (p) => !text || p.code.toLowerCase().includes(text) || (p.displayName ?? '').toLowerCase().includes(text),
  );
  const modules = [...new Set(shown.map((p) => p.moduleCode ?? p.code.split('.')[0] ?? ''))].sort();
  const busy = grant.isPending || revoke.isPending;
  const toggle = (permission: Permission, on: boolean) => {
    const existing = permission.id ? grantOf.get(permission.id) : undefined;
    if (on && !existing && permission.id)
      grant.mutate({ values: { permissionId: permission.id }, parentId: role.id });
    if (!on && existing?.etag) revoke.mutate({ id: existing.id, etag: existing.etag, parentId: role.id });
  };
  const locked = role.isSystem === true;
  return (
    <>
      <p className="hint">{t('org.roles.held', { count: grantOf.size })}</p>
      {locked ? <Banner intent="info">{t('org.roles.system')}</Banner> : null}
      {grant.error || revoke.error ? (
        <Banner intent="danger">{problemText(t, grant.error ?? revoke.error)}</Banner>
      ) : null}
      <TextInput
        label={t('org.roles.search')}
        type="search"
        value={needle}
        onChange={(event) => setNeedle(event.target.value)}
      />
      {modules.map((module) => (
        <fieldset key={module} className="permission-group">
          <legend>{module}</legend>
          {shown
            .filter((p) => (p.moduleCode ?? p.code.split('.')[0]) === module)
            .map((p) => {
              const on = p.id !== undefined && grantOf.has(p.id);
              const editable = !locked && p.isModifiable !== false && (on ? mayRevoke : mayGrant);
              return (
                <label key={p.code} className="permission-group__item">
                  <input
                    type="checkbox"
                    checked={on}
                    disabled={!editable || busy}
                    onChange={(event) => toggle(p, event.target.checked)}
                  />
                  <span>
                    {p.displayName ?? p.code}
                    <span className="hint"> {p.code}</span>
                  </span>
                </label>
              );
            })}
        </fieldset>
      ))}
    </>
  );
}

/**
 * ADM-10 Roles & permissions: the tenant's roles (list manager: create, edit, delete) and, for the role chosen above
 * the editor (`?role=` in the address), its permissions from iam.ListPermissions. Roles are tenant-level: only a
 * tenant administrator's grants list or change them.
 */
export function RolesScreen() {
  const { t } = useI18n();
  const [search, setSearch] = useSearchParams();
  const mayRead = useCan('iam.role.read');
  const mayReadGrants = useCan('iam.role_permission.read');
  const mayCatalogue = useCan('iam.permission.read');
  const mayGrants = mayReadGrants && mayCatalogue;
  const roles = useAllRecords('iam.Role', {}, mayRead);
  const role = roles.data?.items.find((row) => row.id === search.get('role'));
  const screen = RESOURCE_SCREENS.find((each) => each.id === 'ADM-10');
  return (
    <div className="page">
      {screen ? <ResourceScreen screen={screen} /> : null}
      {mayRead && mayGrants ? (
        <Card title={t('org.roles.permissions')}>
          <SelectField
            label={t('org.roles.pick')}
            value={role?.id ?? ''}
            options={[
              { value: '', label: t('org.roles.choose') },
              ...(roles.data?.items ?? []).map((row) => ({
                value: row.id,
                label: String(row.displayName || row.code),
              })),
            ]}
            onChange={(event) => {
              const chosen = event.target.value;
              setSearch(
                (previous) => {
                  const next = new URLSearchParams(previous);
                  if (chosen) next.set('role', chosen);
                  else next.delete('role');
                  return next;
                },
                { replace: true },
              );
            }}
          />
          {role ? <PermissionEditor key={role.id} role={role} /> : null}
        </Card>
      ) : null}
    </div>
  );
}
