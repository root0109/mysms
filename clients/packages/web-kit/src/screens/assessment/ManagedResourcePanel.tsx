import { RESOURCES, can, useI18n, useSession, type RecordRow, type ResourceDef } from '@mysms/core';
import { Card } from '@mysms/ui-web';
import { useState, type ComponentType } from 'react';
import { RecordsOf } from '../RecordsScreen';

export function ManagedResourcePanel({
  resource,
  title,
  parentId: initialParentId = '',
  rowTool: Tool,
  columns,
}: {
  resource: string;
  title: string;
  parentId?: string;
  rowTool?: ComponentType<{ row: RecordRow }>;
  columns?: readonly string[];
}) {
  const { t } = useI18n();
  const permissions = useSession((state) => state.me?.permissions);
  const def = RESOURCES.find((item) => item.key === resource);
  const mayRead = Boolean(def?.ops.List?.permission && can(permissions, def.ops.List.permission as never));
  const [parentId, setParentId] = useState(initialParentId);
  const [search, setSearch] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState<RecordRow | null>(null);
  const [creating, setCreating] = useState(false);
  if (!def) {
    return (
      <Card title={title}>
        <p className="hint">{t('asm.web.resourceUnavailable')}</p>
      </Card>
    );
  }
  if (!mayRead) {
    return (
      <Card title={title}>
        <p className="hint">{t('adm.records.forbidden')}</p>
      </Card>
    );
  }
  return (
    <RecordsOf
      def={def as ResourceDef}
      title={title}
      intro={t(def.scope === 'global' ? 'adm.resource.introGlobal' : 'adm.resource.intro', {
        label: def.label,
      })}
      columns={columns}
      rowTool={Tool ? (row) => <Tool row={row} /> : undefined}
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
