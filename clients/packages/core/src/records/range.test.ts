import { describe, expect, it } from 'vitest';
import { allOf, rangeFilter } from './api';
import type { ResourceDef } from './types';

const base: ResourceDef = { key: 'x.Y', module: 'x', label: 'Y', scope: 'tenant', ops: {}, fields: [] };

describe('partition ranges', () => {
  it('bounds a date field inclusively', () => {
    const def = { ...base, partition: { field: 'attendanceDate', kind: 'date' as const } };
    expect(rangeFilter(def, '2026-01-01', '2026-01-31')).toBe(
      'attendanceDate >= "2026-01-01" AND attendanceDate <= "2026-01-31"',
    );
  });

  it('bounds a timestamp from the start of the first day to before the day after the last, in local time', () => {
    const def = { ...base, partition: { field: 'createTime', kind: 'datetime' as const } };
    const start = new Date('2026-02-01T00:00:00').toISOString();
    const after = new Date('2026-03-01T00:00:00').toISOString();
    expect(rangeFilter(def, '2026-02-01', '2026-02-28')).toBe(
      `createTime >= "${start}" AND createTime < "${after}"`,
    );
    expect(rangeFilter(def, '2026-12-01', '2026-12-31')).toContain(
      new Date('2027-01-01T00:00:00').toISOString(),
    );
  });

  it('adds nothing for other resources', () => {
    expect(rangeFilter(base, '2026-01-01', '2026-01-31')).toBeUndefined();
  });

  it('keeps a range at the top level when joined with a search', () => {
    expect(allOf(undefined, undefined)).toBeUndefined();
    expect(allOf('name : "a"', undefined)).toBe('name : "a"');
    expect(allOf('name : "a"', 'd >= "1" AND d <= "2"')).toBe('name : "a" AND d >= "1" AND d <= "2"');
    expect(allOf('a = 1 OR b = 2', 'd >= "1" AND d <= "2"')).toBe(
      '(a = 1 OR b = 2) AND d >= "1" AND d <= "2"',
    );
  });
});
