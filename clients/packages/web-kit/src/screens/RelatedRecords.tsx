import { can, RESOURCES, useI18n, useSession, type PermissionCode, type RecordRow } from '@mysms/core';
import { Banner } from '@mysms/ui-web';
import { useState, type ReactNode } from 'react';
import { RecordsOf } from './RecordsScreen';

/**
 * A tab of a detail page: the records of one record type that belong to the page's record (ui/02 §1.6), on the
 * shared list manager — a nested record type under its parent row, or a flat one narrowed by `scope` with the
 * page's record preset on new rows. Create, edit, delete and state steps follow the role's permissions as in every
 * list; a role that may not list the record type is told so instead of shown a list the server would refuse.
 */
export function RelatedRecords({
  resource,
  title,
  intro,
  parentId = '',
  scope,
  presets,
  columns,
  filterField,
  rowLink,
  rowTool,
  names,
}: {
  /** `<module>.<Resource>` of api/standard-resources.json. */
  resource: string;
  title: string;
  intro?: string;
  /** The page's record, when the record type is nested under it. */
  parentId?: string;
  scope?: string;
  presets?: Readonly<Record<string, unknown>>;
  columns?: readonly string[];
  filterField?: string;
  rowLink?: (row: RecordRow) => string;
  rowTool?: (row: RecordRow) => ReactNode;
  names?: Readonly<Record<string, ReadonlyMap<string, string>>>;
}) {
  const { t } = useI18n();
  const permissions = useSession((s) => s.me?.permissions);
  const def = RESOURCES.find((r) => r.key === resource);
  const [search, setSearch] = useState('');
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState<RecordRow | null>(null);
  const [creating, setCreating] = useState(false);
  const list = def?.ops.List?.permission;
  if (!def || !list || !can(permissions, list as PermissionCode))
    return <Banner intent="info">{t('adm.resource.noAccess')}</Banner>;
  return (
    <RecordsOf
      def={def}
      title={title}
      intro={intro ?? ''}
      columns={columns}
      filterField={filterField}
      rowLink={rowLink}
      rowTool={rowTool}
      names={names}
      parentId={parentId}
      setParentId={() => undefined}
      fixedParent
      scope={scope}
      presets={presets}
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
