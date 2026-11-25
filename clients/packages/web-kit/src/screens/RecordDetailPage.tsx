import {
  can,
  isApiError,
  resourceDef,
  useI18n,
  useRecord,
  useSession,
  type PermissionCode,
  type RecordRow,
} from '@mysms/core';
import { Banner, Button, Card, SegmentedControl, Skeleton, StatusChip } from '@mysms/ui-web';
import { Fragment, useState, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router';
import { UUID } from './ids';
import { RecordDialog } from './RecordsScreen';
import { display, problemText } from './recordText';

/** One tab of a detail page: what it is called and what it shows for the record. */
export interface DetailTab {
  key: string;
  label: string;
  /** Shown only when this holds (the role may read the tab's records); the profile tab is always there. */
  when?: boolean;
  render: (row: RecordRow) => ReactNode;
}

/**
 * The detail page of one record (ui/02 §1.6), the Template Method of every "row opens a page" screen: the header
 * names the record and its state and carries its actions (edit, state steps and delete in the record's editor,
 * each with its permission), the first tab lists the record's own fields, and the page's own tabs show the records
 * that belong to it. A malformed id, a row the server does not show the caller (404) and a refusal each have their
 * own state; after delete the page goes back to `back`.
 */
export function RecordDetailPage({
  resource,
  id,
  titleField,
  title,
  back,
  tabs = [],
  extraFacts,
  headerActions,
}: {
  resource: string;
  id: string;
  /** The field that names the record in the heading. */
  titleField: string;
  /** The heading when the record's name is held by another module (a guardian's person). */
  title?: string | undefined;
  /** Where the page goes after the record is deleted (its list). */
  back: string;
  tabs?: readonly DetailTab[];
  /** Facts the record's own fields do not carry (names held by another module). */
  extraFacts?: (row: RecordRow) => readonly [string, ReactNode][];
  /** Steps of the record's own workflow besides its editor. */
  headerActions?: (row: RecordRow, etag: string | null) => ReactNode;
}) {
  const { t, format } = useI18n();
  const navigate = useNavigate();
  const permissions = useSession((s) => s.me?.permissions);
  const def = resourceDef(resource);
  const may = (code: string | undefined) => code !== undefined && can(permissions, code as PermissionCode);
  const mayRead = may(def?.ops.Get?.permission);
  const valid = UUID.test(id);
  const read = useRecord(def, mayRead && valid ? id : null);
  const [tab, setTab] = useState('profile');
  const [editing, setEditing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  if (!def || !mayRead) return <Banner intent="info">{t('adm.resource.noAccess')}</Banner>;
  if (!valid) return <Banner intent="warning">{t('common.state.notAvailable')}</Banner>;
  if (read.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (read.isError)
    return isApiError(read.error) && read.error.status === 404 ? (
      <Banner intent="warning">{t('common.state.notAvailable')}</Banner>
    ) : (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void read.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {problemText(t, read.error)}
      </Banner>
    );
  const row = read.data.data;
  const etag = read.data.etag ?? (row.etag as string | undefined) ?? null;
  const name = row[titleField];
  const status = def.fields.find((f) => f.name === 'status' && f.kind === 'enum');
  const shownTabs = tabs.filter((each) => each.when !== false);
  const facts: [string, ReactNode][] = [
    ...def.fields
      .filter((f) => !f.writeOnly && f.kind !== 'id' && f.name !== titleField)
      .map((f): [string, ReactNode] => [f.label, display(t, f, row[f.name], format)]),
    ...(extraFacts?.(row) ?? []),
  ];
  const actions = (def.actions ?? []).filter((a) => may(a.permission));
  const mayEdit = may(def.ops.Update?.permission) || may(def.ops.Delete?.permission) || actions.length > 0;
  return (
    <div className="page">
      <header className="page__header">
        <p className="hint">
          <Link to={back}>{t('common.action.back')}</Link>
        </p>
        <h1 className="page__title">{title ?? (typeof name === 'string' && name ? name : def.label)}</h1>
        {status ? (
          <p>
            <StatusChip intent="neutral" label={display(t, status, row.status)} />
          </p>
        ) : null}
        <div className="page__actions">
          {headerActions?.(row, etag)}
          {mayEdit ? (
            <Button variant="secondary" onClick={() => setEditing(true)}>
              {t('common.action.edit')}
            </Button>
          ) : null}
        </div>
      </header>
      {notice ? <Banner intent="success">{notice}</Banner> : null}
      {shownTabs.length > 0 ? (
        <SegmentedControl
          label={t('adm.detail.tabs')}
          options={[
            { value: 'profile', label: t('adm.detail.profile') },
            ...shownTabs.map((each) => ({ value: each.key, label: each.label })),
          ]}
          value={tab}
          onChange={setTab}
        />
      ) : null}
      {tab === 'profile' || !shownTabs.some((each) => each.key === tab) ? (
        <Card title={t('adm.detail.profile')}>
          <dl className="facts">
            {facts.map(([label, value]) => (
              <Fragment key={label}>
                <dt>{label}</dt>
                <dd>{value}</dd>
              </Fragment>
            ))}
          </dl>
        </Card>
      ) : (
        shownTabs.find((each) => each.key === tab)?.render(row)
      )}
      {editing ? (
        <RecordDialog
          def={def}
          parentId=""
          row={{ ...row, ...(etag ? { etag } : {}) }}
          canUpdate={may(def.ops.Update?.permission)}
          canDelete={may(def.ops.Delete?.permission)}
          actions={actions}
          onClose={() => setEditing(false)}
          onDone={(message, deleted) => {
            setEditing(false);
            if (deleted) navigate(back, { replace: true });
            else setNotice(message);
          }}
        />
      ) : null}
    </div>
  );
}
