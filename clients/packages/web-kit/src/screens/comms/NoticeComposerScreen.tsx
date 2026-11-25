import { useCan, useCloseNoticeReplies, useI18n, type RecordRow } from '@mysms/core';
import { Banner, Button } from '@mysms/ui-web';
import { useParams } from 'react-router';
import { RecordDetailPage } from '../RecordDetailPage';
import { problemText } from '../recordText';
import { RelatedRecords } from '../RelatedRecords';

/** Ends replies to a notice that takes them (comms.CloseNoticeReplies), under the notice's etag. */
function CloseReplies({ row, etag }: { row: RecordRow; etag: string | null }) {
  const { t } = useI18n();
  const close = useCloseNoticeReplies();
  if (row.allowReplies !== true || row.isClosed === true) return null;
  return (
    <>
      {close.error ? <Banner intent="danger">{problemText(t, close.error)}</Banner> : null}
      <Button
        variant="secondary"
        loading={close.isPending}
        onClick={() => close.mutate({ noticeId: row.id, etag })}
      >
        {t('comms.notices.closeReplies')}
      </Button>
    </>
  );
}

/**
 * COM-02 Notice composer: one notice — its text, dates and settings in its editor; publish, unpublish, deactivate
 * and reactivate as the notice's state allows (each under its etag); closing replies; the roles and institutions it
 * is addressed to beyond its audience; and the versions kept of each published text. A cohort's announcement
 * (LMS-28) is composed here too.
 */
export function NoticeComposerScreen() {
  const { t } = useI18n();
  const { noticeId = '' } = useParams();
  const mayClose = useCan('comms.notice.close_notice_replies');
  return (
    <RecordDetailPage
      resource="comms.Notice"
      id={noticeId}
      titleField="title"
      back="/app/comms/notices"
      headerActions={(row, etag) => (mayClose ? <CloseReplies row={row} etag={etag} /> : null)}
      tabs={[
        {
          key: 'audiences',
          label: t('comms.notices.audiences'),
          render: (row) => (
            <RelatedRecords
              resource="comms.NoticeAudience"
              title={t('comms.notices.audiences')}
              intro={t('comms.notices.audiencesIntro')}
              parentId={row.id}
            />
          ),
        },
        {
          key: 'versions',
          label: t('comms.notices.versions'),
          render: (row) => (
            <RelatedRecords
              resource="comms.NoticeVersion"
              title={t('comms.notices.versions')}
              parentId={row.id}
            />
          ),
        },
      ]}
    />
  );
}
