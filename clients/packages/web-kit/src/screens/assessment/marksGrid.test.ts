import { describe, expect, it } from 'vitest';
import type { ExamResult } from '@mysms/core';
import { mergeRows, rowOf } from './marksGrid';

const read = (studentId: string, marks: string, etag: string): ExamResult => ({
  studentId,
  examId: 'exam-1',
  attendanceStatus: 'present',
  marks,
  etag,
});

describe('the marks grid after a read', () => {
  it('keeps what the teacher typed and the version it started from; every other row shows the server', () => {
    const typed = { ...rowOf(read('ann', '40', '"1"')), marksObtained: '64' };
    const untouched = rowOf(read('ben', '50', '"1"'));
    const merged = mergeRows([typed, untouched], [read('ann', '41', '"2"'), read('ben', '55', '"2"')]);
    expect(merged[0]).toMatchObject({ marksObtained: '64', existing: { etag: '"1"' } });
    expect(merged[1]).toMatchObject({ marksObtained: '55', existing: { etag: '"2"' } });
  });

  it('takes the server row once it holds what the teacher saved', () => {
    const saved = { ...rowOf(read('ann', '40', '"1"')), marksObtained: '64' };
    const [row] = mergeRows([saved], [read('ann', '64', '"2"')]);
    expect(row).toMatchObject({ marksObtained: '64', existing: { etag: '"2"' } });
  });

  it('adds attendees registered meanwhile and drops removed ones', () => {
    const merged = mergeRows([rowOf(read('gone', '1', '"1"'))], [read('new', '2', '"1"')]);
    expect(merged.map((row) => row.studentId)).toEqual(['new']);
  });
});
