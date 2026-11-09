import { can, RESOURCES, useI18n, useSession, type PermissionCode, type RecordRow } from '@mysms/core';
import { Card } from '@mysms/ui-web';
import { useState } from 'react';
import { RecordsOf } from './RecordsScreen';
import { defaultColumns, defaultFilter, screenTitleKey } from './resourceColumns';
import type { ResourceScreenDef } from './resourceScreens';

/**
 * One inventory screen over one record type: its own heading and route, the columns that identify a row, a status
 * filter, create/edit/delete and the state transitions the role may use. The server decides which rows the caller
 * sees (institution scope, tenant-level records); a role without the list permission is told so, never shown a
 * broken list.
 */
export function ResourceScreen({ screen }: { screen: ResourceScreenDef }) {
  const { t } = useI18n();
  const permissions = useSession((s) => s.me?.permissions);
  const def = RESOURCES.find((r) => r.key === screen.resource);
  const [parentId, setParentId] = useState('');
  const [search, setSearch] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState<RecordRow | null>(null);
  const [creating, setCreating] = useState(false);
  const title = t(screenTitleKey(screen.id));
  const listPermission = def?.ops.List?.permission;
  if (!def || !listPermission || !can(permissions, listPermission as PermissionCode)) {
    return (
      <Card title={title}>
        <p className="hint">{t('adm.resource.noAccess')}</p>
      </Card>
    );
  }
  return (
    <RecordsOf
      def={def}
      title={title}
      intro={t('adm.resource.intro', { label: def.label })}
      columns={screen.columns ?? defaultColumns(def)}
      filterField={screen.filter ?? defaultFilter(def)}
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
