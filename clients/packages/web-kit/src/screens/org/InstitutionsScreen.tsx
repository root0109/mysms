import { useAllRecords, useCan, useI18n } from '@mysms/core';
import { Banner, Button, Card, Skeleton } from '@mysms/ui-web';
import { Link } from 'react-router';
import { ResourceScreen } from '../ResourceScreen';
import { RESOURCE_SCREENS } from '../resourceScreens';
import { institutionTree, type Branch } from './institutionTree';

function Tree({ branches }: { branches: readonly Branch[] }) {
  const { t } = useI18n();
  return (
    <ul className="tree">
      {branches.map(({ row, children }) => (
        <li key={row.id}>
          <Link to={`/app/org/institutions/${row.id}`}>{String(row.name ?? row.code ?? row.id)}</Link>
          {row.code ? <span className="hint"> {String(row.code)}</span> : null}
          {row.status === 'inactive' ? (
            <span className="hint"> · {t('org.institutions.inactive')}</span>
          ) : null}
          {children.length > 0 ? <Tree branches={children} /> : null}
        </li>
      ))}
    </ul>
  );
}

/**
 * ADM-02 Institutions: the tenant's institutions as a tree by parent (each opens ADM-03), above the list manager
 * that searches, creates and edits them. The server lists only the institutions in the caller's scope.
 */
export function InstitutionsScreen() {
  const { t } = useI18n();
  const mayRead = useCan('org.institution.read');
  const rows = useAllRecords('org.Institution', {}, mayRead);
  const screen = RESOURCE_SCREENS.find((each) => each.id === 'ADM-02');
  return (
    <div className="page">
      <Card title={t('org.institutions.tree')}>
        {!mayRead ? (
          <Banner intent="info">{t('adm.resource.noAccess')}</Banner>
        ) : rows.isPending ? (
          <Skeleton label={t('common.state.loading')} lines={4} />
        ) : rows.isError ? (
          <Banner
            intent="danger"
            action={
              <Button variant="secondary" onClick={() => void rows.refetch()}>
                {t('common.action.retry')}
              </Button>
            }
          >
            {t('common.error.generic')}
          </Banner>
        ) : rows.data.items.length === 0 ? (
          <p className="hint">{t('org.institutions.empty')}</p>
        ) : (
          <Tree branches={institutionTree(rows.data.items)} />
        )}
      </Card>
      {screen && mayRead ? <ResourceScreen screen={screen} /> : null}
    </div>
  );
}
