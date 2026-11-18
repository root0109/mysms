import {
  lmsFailureText,
  useCan,
  useDriveItems,
  useDriveItemShares,
  useI18n,
  useMyStorageUsage,
  useSession,
  useShareDriveItem,
  type DriveItem,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Dialog, ProgressBar, Skeleton, TextInput } from '@mysms/ui-web';
import { useState } from 'react';
import { UUID } from '../ids';

/**
 * LMS-54 Drive (file cabinet): the caller's own files and links with the quota meter, and sharing — the list of
 * users an item is seen by, replaced in one step. Files themselves are managed through the platform file store.
 */
export function DriveScreen() {
  const { t } = useI18n();
  const mayRead = useCan('lms.drive_item.read');
  const items = useDriveItems();
  const quota = useMyStorageUsage();
  const title = t('screen.LMS54.title');
  if (!mayRead)
    return (
      <Card title={title}>
        <p className="hint">{t('lms.common.forbidden')}</p>
      </Card>
    );
  if (items.isPending) return <Skeleton label={t('common.state.loading')} lines={6} />;
  if (items.isError)
    return (
      <Banner
        intent="danger"
        action={
          <Button variant="secondary" onClick={() => void items.refetch()}>
            {t('common.action.retry')}
          </Button>
        }
      >
        {t('common.error.generic')}
      </Banner>
    );
  const used = Number(quota.data?.usedBytes ?? 0);
  const limit = Number(quota.data?.quotaBytes ?? 0);
  return (
    <Card title={title}>
      {limit > 0 && (
        <ProgressBar
          value={(used / limit) * 100}
          label={t('lms.drive.quota', {
            used: Math.round(used / 1_048_576),
            limit: Math.round(limit / 1_048_576),
          })}
        />
      )}
      <DriveTable items={items.data.items} />
    </Card>
  );
}

function DriveTable({ items }: { items: DriveItem[] }) {
  const { t } = useI18n();
  const me = useSession((s) => s.me?.user?.id);
  const mayShare = useCan('lms.drive_item.share_drive_item');
  const [sharing, setSharing] = useState<DriveItem | null>(null);
  return (
    <>
      <DataTable
        caption={t('screen.LMS54.title')}
        columns={[
          { key: 'title', header: t('lms.drive.item'), render: (row: DriveItem) => row.title ?? '' },
          {
            key: 'kind',
            header: t('lms.drive.kind'),
            render: (row: DriveItem) =>
              row.itemType ?? (row.linkUrl !== undefined && row.linkUrl !== null ? t('lms.drive.link') : ''),
          },
          {
            key: 'shared',
            header: t('lms.drive.shared'),
            render: (row: DriveItem) => (row.isShared ? t('lms.drive.sharedYes') : t('lms.drive.sharedNo')),
          },
          {
            key: 'actions',
            header: t('lms.bank.actions'),
            render: (row: DriveItem) =>
              mayShare && row.ownerUserId === me ? (
                <Button variant="secondary" onClick={() => setSharing(row)}>
                  {t('lms.drive.share')}
                </Button>
              ) : null,
          },
        ]}
        rows={items}
        rowKey={(row) => row.id ?? ''}
        loadingLabel={t('common.state.loading')}
        emptyText={t('lms.drive.empty')}
      />
      <ShareDialog item={sharing} onClose={() => setSharing(null)} />
    </>
  );
}

/** Replaces who sees the item: the current shares shown, a user added by id, one removed, or all cleared. */
function ShareDialog({ item, onClose }: { item: DriveItem | null; onClose: () => void }) {
  const { t } = useI18n();
  const shares = useDriveItemShares(item?.id ?? null);
  const share = useShareDriveItem();
  const [adding, setAdding] = useState('');
  const current = (shares.data?.items ?? [])
    .map((row) => row.userId)
    .filter((userId): userId is string => Boolean(userId));
  const apply = (userIds: string[]) => {
    if (item?.id) share.mutate({ driveItemId: item.id, userIds }, { onSuccess: () => setAdding('') });
  };
  return (
    <Dialog open={item !== undefined && item !== null} title={t('lms.drive.share')} onClose={onClose}>
      <p>{item?.title}</p>
      {share.isError && <Banner intent="danger">{lmsFailureText(t, share.error)}</Banner>}
      {shares.isPending && item !== undefined && item !== null ? (
        <Skeleton label={t('common.state.loading')} lines={2} />
      ) : (
        <ul aria-label={t('lms.drive.sharedWith')}>
          {current.map((userId) => (
            <li key={userId}>
              {userId}{' '}
              <Button
                variant="secondary"
                onClick={() => apply(current.filter((kept) => kept !== userId))}
                disabled={share.isPending}
              >
                {t('lms.drive.unshare')}
              </Button>
            </li>
          ))}
          {current.length === 0 && <li className="hint">{t('lms.drive.nobody')}</li>}
        </ul>
      )}
      <TextInput
        label={t('lms.drive.addUser')}
        value={adding}
        onChange={(event) => setAdding(event.target.value)}
        error={adding !== '' && !UUID.test(adding) ? t('common.validation.pattern') : undefined}
      />
      <Button
        onClick={() => apply([...current, adding])}
        disabled={share.isPending || !UUID.test(adding) || current.includes(adding)}
      >
        {t('lms.drive.add')}
      </Button>
    </Dialog>
  );
}
