import type { ExamResult, MarkEntry } from '@mysms/core';

export interface MarkRow extends Omit<MarkEntry, 'attendanceStatus' | 'etag'> {
  attendanceStatus: string;
  existing: ExamResult;
  error?: string;
}

/** The attendance values a teacher may record (expected is where every attendee starts). */
export const RECORDED = ['present', 'absent', 'exempted'];

export function rowOf(item: ExamResult): MarkRow {
  return {
    studentId: item.studentId,
    existing: item,
    attendanceStatus: item.attendanceStatus ?? 'expected',
    ...(item.marks !== undefined ? { marksObtained: item.marks } : {}),
    ...(item.note ? { note: item.note } : {}),
  };
}

/** Whether the row's values are the given result's (the row then carries nothing of the teacher's own). */
export function sameAs(row: MarkRow, item: ExamResult): boolean {
  return (
    row.attendanceStatus === (item.attendanceStatus ?? 'expected') &&
    (row.marksObtained ?? '') === (item.marks ?? '') &&
    (row.note ?? '') === (item.note ?? '')
  );
}

/**
 * The grid after a read: a row the teacher changed and has not saved keeps their values and the version they
 * started from (saving it then answers 412 if someone else changed it meanwhile); every other row shows the server.
 */
export function mergeRows(current: readonly MarkRow[], items: readonly ExamResult[]): MarkRow[] {
  const edited = new Map(
    current.filter((row) => !sameAs(row, row.existing)).map((row) => [row.studentId, row]),
  );
  return items.map((item) => {
    const mine = edited.get(item.studentId);
    return mine && !sameAs(mine, item) ? mine : rowOf(item);
  });
}
