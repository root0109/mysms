import {
  lmsFailureText,
  useBadgeAwards,
  useCan,
  useCertificates,
  useDownloadCertificate,
  useI18n,
  useSession,
  useVerifyCertificate,
  type BadgeAward,
  type Certificate,
} from '@mysms/core';
import { Banner, Button, Card, DataTable, Dialog, StatusChip, type Column } from '@mysms/ui-web';
import { useState } from 'react';
import { LmsRetryBanner } from './lmsShared';
import { openDownload } from './lmsText';

/**
 * LMS-15 Certificates & badges: the signed-in member's certificates with their PDF download, their badges, and the
 * verification view anybody signed in can ask of a certificate number's id. The public (guest) verification page is
 * the catalogue's, outside this screen.
 */
export function CertificatesScreen() {
  const { t, format } = useI18n();
  const me = useSession((s) => s.me?.user?.id ?? '');
  const certificates = useCertificates(me || undefined);
  const badges = useBadgeAwards(me || undefined);
  const download = useDownloadCertificate();
  const mayDownload = useCan('lms.certificate.download_certificate');
  const [verifying, setVerifying] = useState<string | null>(null);
  const verification = useVerifyCertificate(verifying);
  const [problem, setProblem] = useState<string | null>(null);

  const certificateColumns: Column<Certificate>[] = [
    { key: 'number', header: t('lms.certificates.number'), render: (c) => c.certificateNumber ?? '' },
    {
      key: 'issued',
      header: t('lms.certificates.issued'),
      render: (c) => (c.issueDate ? format.day(c.issueDate) : ''),
    },
    {
      key: 'state',
      header: t('lms.certificates.state'),
      render: (c) =>
        c.isPublished ? (
          <StatusChip intent="success" label={t('lms.certificates.published')} />
        ) : (
          <StatusChip intent="neutral" label={t('lms.certificates.unpublished')} />
        ),
    },
    {
      key: 'actions',
      header: t('lms.roster.actions'),
      render: (c) => (
        <span className="chip-row">
          {mayDownload ? (
            <Button
              onClick={() =>
                download.mutate(
                  { certificateId: String(c.id) },
                  { onSuccess: openDownload, onError: (error) => setProblem(lmsFailureText(t, error)) },
                )
              }
              disabled={download.isPending}
            >
              {t('lms.certificates.download')}
            </Button>
          ) : null}
          <Button onClick={() => setVerifying(String(c.id))}>{t('lms.certificates.verify')}</Button>
        </span>
      ),
    },
  ];
  const badgeColumns: Column<BadgeAward>[] = [
    { key: 'badge', header: t('lms.certificates.badge'), render: (b) => String(b.badgeId) },
    {
      key: 'on',
      header: t('lms.certificates.awardedOn'),
      render: (b) => (b.issuedOn ? format.day(b.issuedOn) : ''),
    },
  ];

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">{t('screen.LMS15.title')}</h1>
      </header>
      {problem ? <Banner intent="danger">{problem}</Banner> : null}
      <div className="page__grid">
        <Card title={t('lms.certificates.mine')}>
          {certificates.isError ? (
            <LmsRetryBanner onRetry={() => void certificates.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.certificates.mine')}
              columns={certificateColumns}
              rows={certificates.data?.items}
              rowKey={(c) => String(c.id)}
              loading={certificates.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.certificates.none')}
            />
          )}
        </Card>
        <Card title={t('lms.certificates.badges')}>
          {badges.isError ? (
            <LmsRetryBanner onRetry={() => void badges.refetch()} />
          ) : (
            <DataTable
              caption={t('lms.certificates.badges')}
              columns={badgeColumns}
              rows={badges.data?.items}
              rowKey={(b) => String(b.id)}
              loading={badges.isPending}
              loadingLabel={t('common.state.loading')}
              emptyText={t('lms.certificates.noBadges')}
            />
          )}
        </Card>
      </div>
      <Dialog
        open={verifying !== null}
        title={t('lms.certificates.verify')}
        onClose={() => setVerifying(null)}
        footer={<Button onClick={() => setVerifying(null)}>{t('common.action.close')}</Button>}
      >
        {verification.isPending ? (
          <p>{t('common.state.loading')}</p>
        ) : verification.isError ? (
          <Banner intent="danger">{t('common.error.generic')}</Banner>
        ) : verification.data?.valid ? (
          <>
            <StatusChip intent="success" label={t('lms.certificates.valid')} />
            <p>{verification.data.title}</p>
            <p className="hint">
              {t('lms.certificates.issued')}:{' '}
              {verification.data.issueDate ? format.day(verification.data.issueDate) : ''}
            </p>
          </>
        ) : (
          <StatusChip intent="warning" label={t('lms.certificates.invalid')} />
        )}
      </Dialog>
    </div>
  );
}
