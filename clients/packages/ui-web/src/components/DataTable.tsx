import type { ReactNode } from 'react';
import { Skeleton } from './Skeleton';

export interface Column<Row> {
  key: string;
  header: string;
  render: (row: Row) => ReactNode;
  align?: 'start' | 'end';
}

export interface DataTableProps<Row> {
  /** Accessible table name (rendered as the caption). */
  caption: string;
  columns: readonly Column<Row>[];
  rows: readonly Row[] | undefined;
  rowKey: (row: Row) => string;
  loading?: boolean;
  loadingLabel: string;
  emptyText: string;
  /** Fixed row height; omitted = the user's density setting. */
  density?: 'compact' | 'comfortable';
  onRowClick?: (row: Row) => void;
}

/** Server-paged table shell: header cells are scoped, loading shows a skeleton, empty shows the empty state. */
export function DataTable<Row>({
  caption,
  columns,
  rows,
  rowKey,
  loading,
  loadingLabel,
  emptyText,
  density,
  onRowClick,
}: DataTableProps<Row>) {
  if (loading) return <Skeleton label={loadingLabel} lines={5} />;
  return (
    <table className={density ? `sms-table sms-table--${density}` : 'sms-table'}>
      <caption className="sms-table__caption">{caption}</caption>
      <thead>
        <tr>
          {columns.map((c) => (
            <th key={c.key} scope="col" className={c.align === 'end' ? 'sms-table__cell--end' : undefined}>
              {c.header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {!rows || rows.length === 0 ? (
          <tr>
            <td colSpan={columns.length} className="sms-table__empty">
              {emptyText}
            </td>
          </tr>
        ) : (
          rows.map((row) => (
            <tr
              key={rowKey(row)}
              className={onRowClick ? 'sms-table__row--interactive' : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              onKeyDown={
                onRowClick
                  ? (e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onRowClick(row);
                      }
                    }
                  : undefined
              }
              tabIndex={onRowClick ? 0 : undefined}
            >
              {columns.map((c) => (
                <td key={c.key} className={c.align === 'end' ? 'sms-table__cell--end' : undefined}>
                  {c.render(row)}
                </td>
              ))}
            </tr>
          ))
        )}
      </tbody>
    </table>
  );
}
