import { useI18n } from '@mysms/core';
import { useParams } from 'react-router';
import { RecordDetailPage } from '../RecordDetailPage';
import { RelatedRecords } from '../RelatedRecords';

/**
 * LIB-02 Title detail: one title of the library — its record, its copies (accession numbers and their state), the
 * loans of its copies newest first (its loan history) and its reservations, each as the role may read them.
 */
export function TitleDetailScreen() {
  const { t } = useI18n();
  const { mediaId = '' } = useParams();
  return (
    <RecordDetailPage
      resource="library.Media"
      id={mediaId}
      titleField="title"
      back="/app/library/media"
      tabs={[
        {
          key: 'copies',
          label: t('lib.title.copies'),
          render: (row) => (
            <RelatedRecords
              resource="library.MediaUnit"
              title={t('lib.title.copies')}
              parentId={row.id}
              columns={['accessionNumber', 'barcode', 'status']}
              filterField="status"
            />
          ),
        },
        {
          key: 'loans',
          label: t('lib.title.loans'),
          render: (row) => (
            <RelatedRecords
              resource="library.Loan"
              title={t('lib.title.loans')}
              scope={`mediaId = "${row.id}"`}
              presets={{ mediaId: row.id }}
              columns={['issuedOn', 'dueOn', 'returnedOn', 'status']}
              filterField="status"
            />
          ),
        },
        {
          key: 'reservations',
          label: t('lib.title.reservations'),
          render: (row) => (
            <RelatedRecords
              resource="library.Reservation"
              title={t('lib.title.reservations')}
              scope={`mediaId = "${row.id}"`}
              presets={{ mediaId: row.id }}
              columns={['dateFrom', 'dateTo', 'status']}
              filterField="status"
            />
          ),
        },
      ]}
    />
  );
}
